package com.auralis.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.LibraryBrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlbumDetailUiState {
    data object Loading : AlbumDetailUiState
    data class Success(
        val album: Album,
        val songs: List<Song>
    ) : AlbumDetailUiState
    data class Error(val message: String) : AlbumDetailUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val browserRepository: LibraryBrowserRepository,
    private val favoriteRepository: FavoriteRepository,
    val localArtworkRepository: com.auralis.app.data.repository.LocalArtworkRepository
) : ViewModel() {

    private val _albumId = MutableStateFlow<Long?>(null)

    val favoriteSongIds: StateFlow<Set<Long>> = favoriteRepository.getFavoriteSongIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val uiState: StateFlow<AlbumDetailUiState> = _albumId.flatMapLatest { id ->
        if (id == null) {
            flowOf(AlbumDetailUiState.Loading)
        } else {
            browserRepository.getAlbumById(id).flatMapLatest { album ->
                if (album == null) {
                    flowOf(AlbumDetailUiState.Error("Album not found."))
                } else {
                    browserRepository.getSongsByAlbum(album.id, album.name).map { songs ->
                        AlbumDetailUiState.Success(album, songs)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumDetailUiState.Loading
    )

    fun loadAlbum(albumId: Long) {
        _albumId.value = albumId
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(songId)
        }
    }

    fun setCustomArtwork(uri: android.net.Uri) {
        val album = (uiState.value as? AlbumDetailUiState.Success)?.album ?: return
        viewModelScope.launch {
            localArtworkRepository.setCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.ALBUM, album.id.toString(), uri)
            _albumId.value = null
            _albumId.value = album.id
        }
    }

    fun clearCustomArtwork() {
        val album = (uiState.value as? AlbumDetailUiState.Success)?.album ?: return
        viewModelScope.launch {
            localArtworkRepository.clearCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.ALBUM, album.id.toString())
            _albumId.value = null
            _albumId.value = album.id
        }
    }
}
