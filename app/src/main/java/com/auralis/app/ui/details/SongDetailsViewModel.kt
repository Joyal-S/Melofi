package com.auralis.app.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.auralis.app.data.repository.LocalArtworkRepository
import com.auralis.app.domain.model.EditableMetadata
import com.auralis.app.domain.model.SongDetails
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SongDetailsUiState {
    data object Loading : SongDetailsUiState
    data class Success(
        val details: SongDetails,
        val isFavorite: Boolean,
        val hasCustomArtwork: Boolean = false,
        val message: String? = null
    ) : SongDetailsUiState
    data class Error(val message: String) : SongDetailsUiState
}

@HiltViewModel
class SongDetailsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val favoriteRepository: FavoriteRepository,
    private val artworkRepository: LocalArtworkRepository,
    private val libraryBrowserRepository: com.auralis.app.domain.repository.LibraryBrowserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongDetailsUiState>(SongDetailsUiState.Loading)
    val uiState: StateFlow<SongDetailsUiState> = _uiState.asStateFlow()

    val allArtists: StateFlow<List<com.auralis.app.domain.model.Artist>> = libraryBrowserRepository.getArtists()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun createArtist(name: String): Result<com.auralis.app.domain.model.Artist> {
        return libraryBrowserRepository.createArtist(name)
    }

    private var currentSongId: Long? = null

    fun loadSong(songId: Long) {
        currentSongId = songId
        viewModelScope.launch {
            _uiState.value = SongDetailsUiState.Loading
            combine(
                songRepository.getSongDetails(songId),
                favoriteRepository.getFavoriteSongIds()
            ) { details: SongDetails?, favIds: Set<Long> ->
                if (details != null) {
                    val hasCustom = artworkRepository.hasCustomArtwork(songId)
                    SongDetailsUiState.Success(
                        details = details,
                        isFavorite = songId in favIds,
                        hasCustomArtwork = hasCustom
                    )
                } else {
                    SongDetailsUiState.Error("Song not found or inaccessible.")
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleFavorite() {
        val songId = currentSongId ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(songId)
        }
    }

    fun updateMetadata(metadata: EditableMetadata) {
        val songId = currentSongId ?: return
        viewModelScope.launch {
            val result = songRepository.updateSongMetadata(
                songId = songId,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                albumArtist = metadata.albumArtist,
                genre = metadata.genre,
                composer = metadata.composer,
                year = metadata.year,
                trackNumber = metadata.trackNumber,
                discNumber = metadata.discNumber,
                artistId = metadata.artistId
            )
            result.onSuccess {
                // Re-fetch details to update UI
                loadSong(songId)
            }.onFailure { error ->
                val current = _uiState.value
                if (current is SongDetailsUiState.Success) {
                    _uiState.value = current.copy(message = "Update failed: ${error.message}")
                }
            }
        }
    }

    fun setCustomArtwork(sourceUri: Uri) {
        val songId = currentSongId ?: return
        viewModelScope.launch {
            val result = artworkRepository.setCustomArtwork(songId, sourceUri)
            result.onSuccess {
                loadSong(songId)
            }.onFailure { error ->
                val current = _uiState.value
                if (current is SongDetailsUiState.Success) {
                    _uiState.value = current.copy(message = "Artwork update failed: ${error.message}")
                }
            }
        }
    }

    fun clearCustomArtwork() {
        val songId = currentSongId ?: return
        viewModelScope.launch {
            artworkRepository.clearCustomArtwork(songId)
            loadSong(songId)
        }
    }

    fun refreshMetadata() {
        val songId = currentSongId ?: return
        loadSong(songId)
    }

    fun clearMessage() {
        val current = _uiState.value
        if (current is SongDetailsUiState.Success) {
            _uiState.value = current.copy(message = null)
        }
    }
}
