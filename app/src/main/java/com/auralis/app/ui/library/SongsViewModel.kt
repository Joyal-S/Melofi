package com.auralis.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.BitrateFilter
import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.Genre
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.PlaybackFilter
import com.auralis.app.domain.model.QualityFilter
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongFilterState
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.repository.HistoryRepository
import com.auralis.app.domain.repository.LibraryBrowserRepository
import com.auralis.app.domain.usecase.GetSongsUseCase
import com.auralis.app.domain.usecase.ScanLibraryUseCase
import com.auralis.app.domain.usecase.favorites.GetFavoriteSongIdsUseCase
import com.auralis.app.domain.usecase.favorites.ToggleFavoriteUseCase
import com.auralis.app.domain.usecase.playlists.AddSongToPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

import com.auralis.app.data.local.preferences.UserPreferencesRepository
import com.auralis.app.domain.model.LibraryTab
import kotlinx.coroutines.flow.firstOrNull

private data class SyncState(
    val isScanning: Boolean = false,
    val permissionGranted: Boolean? = null,
    val isPermanentlyDenied: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SongsViewModel @Inject constructor(
    private val getSongsUseCase: GetSongsUseCase,
    private val scanLibraryUseCase: ScanLibraryUseCase,
    private val getFavoriteSongIdsUseCase: GetFavoriteSongIdsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val addSongToPlaylistUseCase: AddSongToPlaylistUseCase,
    private val historyRepository: HistoryRepository,
    private val browserRepository: LibraryBrowserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val importManager: com.auralis.app.data.local.importer.MusicImportManager,
    val localArtworkRepository: com.auralis.app.data.repository.LocalArtworkRepository,
    private val songRepository: com.auralis.app.domain.repository.SongRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.FOR_YOU)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _viewMode = MutableStateFlow(LibraryViewMode.LIST)
    val viewMode: StateFlow<LibraryViewMode> = _viewMode.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferencesFlow.firstOrNull()?.let { prefs ->
                _selectedTab.value = prefs.defaultLibraryTab
                _viewMode.value = prefs.defaultViewMode
                _sortOrder.value = prefs.defaultSortOrder
            }
        }
    }

    val favoriteSongIds: StateFlow<Set<Long>> = getFavoriteSongIdsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val recentlyPlayed: StateFlow<List<Song>> = historyRepository.getRecentlyPlayedSongs(limit = 20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val recentlyPlayedIds: StateFlow<Set<Long>> = recentlyPlayed
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val albums: StateFlow<List<Album>> = browserRepository.getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val artists: StateFlow<List<Artist>> = browserRepository.getArtists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val genres: StateFlow<List<Genre>> = browserRepository.getGenres()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sortOrder = MutableStateFlow(SongSortOrder.TITLE)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    private val _filterState = MutableStateFlow(SongFilterState())
    val filterState: StateFlow<SongFilterState> = _filterState.asStateFlow()

    // Base query sorting delegation (falls back to Title query if custom sort is applied in-memory)
    private val _songsFlow = _sortOrder.flatMapLatest { order ->
        val baseOrder = when (order) {
            SongSortOrder.TITLE, SongSortOrder.TITLE_DESC -> SongSortOrder.TITLE
            SongSortOrder.ARTIST, SongSortOrder.ARTIST_DESC -> SongSortOrder.ARTIST
            SongSortOrder.ALBUM -> SongSortOrder.ALBUM
            SongSortOrder.DATE_ADDED -> SongSortOrder.DATE_ADDED
            else -> SongSortOrder.TITLE
        }
        getSongsUseCase(baseOrder)
    }

    val uiState: StateFlow<SongsUiState> = combine(
        combine(_songsFlow, _sortOrder, _syncState) { songs, sortOrder, syncState ->
            Triple(songs, sortOrder, syncState)
        },
        combine(_filterState, favoriteSongIds, recentlyPlayedIds) { filter, favIds, recentIds ->
            Triple(filter, favIds, recentIds)
        }
    ) { (songs, sortOrder, syncState), (filter, favIds, recentIds) ->
        when {
            syncState.permissionGranted == false -> {
                SongsUiState.PermissionRequired(isPermanentlyDenied = syncState.isPermanentlyDenied)
            }
            syncState.errorMessage != null -> {
                SongsUiState.Error(message = syncState.errorMessage)
            }
            syncState.permissionGranted == null -> {
                SongsUiState.Loading
            }
            songs.isEmpty() && !syncState.isScanning -> {
                SongsUiState.Empty(isPermissionGranted = true)
            }
            else -> {
                val filtered = songs.filter { song ->
                    filter.matches(
                        song = song,
                        isFavorite = song.id in favIds,
                        isPlayed = song.id in recentIds,
                        isRecentlyPlayed = song.id in recentIds
                    )
                }

                val sorted = sortSongs(filtered, sortOrder, recentIds)

                SongsUiState.Success(
                    songs = sorted,
                    sortOrder = sortOrder,
                    isScanning = syncState.isScanning
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SongsUiState.Loading
    )

    private fun sortSongs(songs: List<Song>, sortOrder: SongSortOrder, recentIds: Set<Long>): List<Song> {
        return when (sortOrder) {
            SongSortOrder.TITLE -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOrder.TITLE_DESC -> songs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOrder.ARTIST -> songs.sortedWith(
                compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artistName }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            SongSortOrder.ARTIST_DESC -> songs.sortedWith(
                compareByDescending<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artistName }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            SongSortOrder.ALBUM -> songs.sortedWith(
                compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.albumName }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            SongSortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SongSortOrder.DATE_MODIFIED -> songs.sortedByDescending { it.dateModified }
            SongSortOrder.DURATION_ASC -> songs.sortedBy { it.duration }
            SongSortOrder.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE_ASC -> songs.sortedBy { it.size }
            SongSortOrder.FILE_SIZE_DESC -> songs.sortedByDescending { it.size }
            SongSortOrder.BITRATE_DESC -> songs.sortedByDescending { it.bitrate ?: 0 }
            SongSortOrder.BITRATE_ASC -> songs.sortedBy { it.bitrate ?: 0 }
            SongSortOrder.MOST_PLAYED, SongSortOrder.RECENTLY_PLAYED -> {
                // Rank recently played tracks first
                songs.sortedByDescending { it.id in recentIds }
            }
        }
    }

    fun onPermissionStatus(granted: Boolean, permanentlyDenied: Boolean = false) {
        val wasPreviouslyGranted = _syncState.value.permissionGranted == true
        _syncState.update {
            it.copy(permissionGranted = granted, isPermanentlyDenied = permanentlyDenied)
        }
        if (granted && !wasPreviouslyGranted) {
            scanLibrary()
        }
    }

    fun setLibraryTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun setViewMode(mode: LibraryViewMode) {
        _viewMode.value = mode
        viewModelScope.launch { userPreferencesRepository.setDefaultViewMode(mode) }
    }

    fun setSortOrder(sortOrder: SongSortOrder) {
        _sortOrder.value = sortOrder
        viewModelScope.launch { userPreferencesRepository.setDefaultSortOrder(sortOrder) }
    }

    fun setFilterState(newFilter: SongFilterState) {
        _filterState.value = newFilter
    }

    fun scanLibrary() {
        if (_syncState.value.isScanning) return
        viewModelScope.launch {
            _syncState.update { it.copy(isScanning = true, errorMessage = null) }
            val result = scanLibraryUseCase()
            result.fold(
                onSuccess = {
                    _syncState.update { it.copy(isScanning = false) }
                },
                onFailure = { error ->
                    _syncState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = error.localizedMessage ?: "Failed to scan media library."
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _syncState.update { it.copy(errorMessage = null) }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            toggleFavoriteUseCase(songId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            addSongToPlaylistUseCase(playlistId, songId)
        }
    }

    fun setFormatFilter(formatFilter: FormatFilter) {
        _filterState.update { it.copy(formatFilter = formatFilter) }
    }

    fun setQualityFilter(qualityFilter: QualityFilter) {
        _filterState.update { it.copy(qualityFilter = qualityFilter) }
    }

    fun setDurationFilter(durationFilter: DurationFilter) {
        _filterState.update { it.copy(durationFilter = durationFilter) }
    }

    fun setPlaybackFilter(playbackFilter: PlaybackFilter) {
        _filterState.update { it.copy(playbackFilter = playbackFilter) }
    }

    fun setBitrateFilter(bitrateFilter: BitrateFilter) {
        _filterState.update { it.copy(bitrateFilter = bitrateFilter) }
    }

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        _filterState.update { it.copy(favoritesOnly = favoritesOnly) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun resetFilters() {
        _filterState.value = SongFilterState()
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            songRepository.deleteSong(songId)
        }
    }

    fun setCustomArtwork(entityType: com.auralis.app.domain.model.ArtworkEntityType, entityKey: String, uri: android.net.Uri) {
        viewModelScope.launch {
            localArtworkRepository.setCustomArtwork(entityType, entityKey, uri)
        }
    }

    fun clearCustomArtwork(entityType: com.auralis.app.domain.model.ArtworkEntityType, entityKey: String) {
        viewModelScope.launch {
            localArtworkRepository.clearCustomArtwork(entityType, entityKey)
        }
    }
}
