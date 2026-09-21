package com.auralis.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.BitrateFilter
import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongFilterState
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.repository.HistoryRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val favoriteSongIds: StateFlow<Set<Long>> = getFavoriteSongIdsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val recentlyPlayed: StateFlow<List<Song>> = historyRepository.getRecentlyPlayedSongs(limit = 10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sortOrder = MutableStateFlow(SongSortOrder.TITLE)
    private val _syncState = MutableStateFlow(SyncState())
    private val _filterState = MutableStateFlow(com.auralis.app.domain.model.SongFilterState())
    val filterState: StateFlow<com.auralis.app.domain.model.SongFilterState> = _filterState.asStateFlow()

    private val _songsFlow = _sortOrder.flatMapLatest { order ->
        getSongsUseCase(order)
    }

    val uiState: StateFlow<SongsUiState> = combine(
        _songsFlow,
        _sortOrder,
        _syncState,
        _filterState,
        favoriteSongIds
    ) { songs, sortOrder, syncState, filter, favIds ->
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
                    filter.matches(song, isFavorite = song.id in favIds)
                }
                SongsUiState.Success(
                    songs = filtered,
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

    fun onPermissionStatus(granted: Boolean, permanentlyDenied: Boolean = false) {
        val wasPreviouslyGranted = _syncState.value.permissionGranted == true
        _syncState.update {
            it.copy(permissionGranted = granted, isPermanentlyDenied = permanentlyDenied)
        }
        if (granted && !wasPreviouslyGranted) {
            // Automatically run initial scan upon first grant
            scanLibrary()
        }
    }

    fun setSortOrder(sortOrder: SongSortOrder) {
        _sortOrder.value = sortOrder
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

    fun setFormatFilter(formatFilter: com.auralis.app.domain.model.FormatFilter) {
        _filterState.update { it.copy(formatFilter = formatFilter) }
    }

    fun setDurationFilter(durationFilter: com.auralis.app.domain.model.DurationFilter) {
        _filterState.update { it.copy(durationFilter = durationFilter) }
    }

    fun setBitrateFilter(bitrateFilter: com.auralis.app.domain.model.BitrateFilter) {
        _filterState.update { it.copy(bitrateFilter = bitrateFilter) }
    }

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        _filterState.update { it.copy(favoritesOnly = favoritesOnly) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun resetFilters() {
        _filterState.value = com.auralis.app.domain.model.SongFilterState()
    }
}
