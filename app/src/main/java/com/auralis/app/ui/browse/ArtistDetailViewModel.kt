package com.auralis.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.LibraryBrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ArtistDetailUiState {
    data object Loading : ArtistDetailUiState
    data class Success(
        val artist: Artist,
        val songs: List<Song>,
        val albums: List<Album>
    ) : ArtistDetailUiState
    data class Error(val message: String) : ArtistDetailUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val browserRepository: LibraryBrowserRepository,
    private val favoriteRepository: FavoriteRepository,
    val localArtworkRepository: com.auralis.app.data.repository.LocalArtworkRepository
) : ViewModel() {

    private val _artistId = MutableStateFlow<Long?>(null)

    val favoriteSongIds: StateFlow<Set<Long>> = favoriteRepository.getFavoriteSongIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val uiState: StateFlow<ArtistDetailUiState> = _artistId.flatMapLatest { id ->
        if (id == null) {
            flowOf(ArtistDetailUiState.Loading)
        } else {
            browserRepository.getArtistById(id).flatMapLatest { artist ->
                if (artist == null) {
                    flowOf(ArtistDetailUiState.Error("Artist not found."))
                } else {
                    combine(
                        browserRepository.getSongsByArtist(artist.id, artist.name),
                        browserRepository.getAlbumsByArtist(artist.id)
                    ) { songs, albums ->
                        ArtistDetailUiState.Success(artist, songs, albums)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArtistDetailUiState.Loading
    )

    fun loadArtist(artistId: Long) {
        _artistId.value = artistId
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(songId)
        }
    }

    fun setCustomArtwork(uri: android.net.Uri) {
        val artist = (uiState.value as? ArtistDetailUiState.Success)?.artist ?: return
        viewModelScope.launch {
            localArtworkRepository.setCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.ARTIST, artist.id.toString(), uri)
            _artistId.value = null
            _artistId.value = artist.id
        }
    }

    fun clearCustomArtwork() {
        val artist = (uiState.value as? ArtistDetailUiState.Success)?.artist ?: return
        viewModelScope.launch {
            localArtworkRepository.clearCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.ARTIST, artist.id.toString())
            _artistId.value = null
            _artistId.value = artist.id
        }
    }
}
