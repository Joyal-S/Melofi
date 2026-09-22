package com.auralis.app.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.usecase.playlists.CreatePlaylistUseCase
import com.auralis.app.domain.usecase.playlists.DeletePlaylistUseCase
import com.auralis.app.domain.usecase.playlists.GetPlaylistsUseCase
import com.auralis.app.domain.usecase.playlists.RenamePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.auralis.app.data.repository.LocalArtworkRepository
import com.auralis.app.domain.repository.PlaylistRepository
import com.auralis.app.domain.usecase.playlists.GetPlaylistSongsUseCase
import kotlinx.coroutines.flow.firstOrNull

/**
 * ViewModel managing user playlists collection.
 */
@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    getPlaylistsUseCase: GetPlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    val localArtworkRepository: LocalArtworkRepository,
    private val playlistRepository: PlaylistRepository,
    private val getPlaylistSongsUseCase: GetPlaylistSongsUseCase
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun createPlaylist(name: String, onSuccess: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val result = createPlaylistUseCase(name)
            result.fold(
                onSuccess = { id ->
                    _actionError.value = null
                    onSuccess?.invoke(id)
                },
                onFailure = { error ->
                    _actionError.value = error.localizedMessage ?: "Failed to create playlist"
                }
            )
        }
    }

    fun renamePlaylist(id: Long, name: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = renamePlaylistUseCase(id, name)
            result.fold(
                onSuccess = {
                    _actionError.value = null
                    onSuccess?.invoke()
                },
                onFailure = { error ->
                    _actionError.value = error.localizedMessage ?: "Failed to rename playlist"
                }
            )
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            val result = deletePlaylistUseCase(id)
            result.onFailure { error ->
                _actionError.value = error.localizedMessage ?: "Failed to delete playlist"
            }
        }
    }

    fun clearError() {
        _actionError.value = null
    }

    fun setCustomArtwork(playlistId: Long, uri: android.net.Uri) {
        viewModelScope.launch {
            val result = localArtworkRepository.setCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.PLAYLIST, playlistId.toString(), uri)
            result.getOrNull()?.let { localUri ->
                playlistRepository.updatePlaylistCover(playlistId, localUri)
            }
        }
    }

    fun clearCustomArtwork(playlistId: Long) {
        viewModelScope.launch {
            localArtworkRepository.clearCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.PLAYLIST, playlistId.toString())
            playlistRepository.updatePlaylistCover(playlistId, null)
        }
    }

    fun getPlaylistArtworkUri(playlist: Playlist): String? {
        return localArtworkRepository.getPlaylistArtworkUri(playlist.id, playlist.customCoverUri)
    }

    fun playPlaylist(playlistId: Long, shuffle: Boolean, onPlay: (List<com.auralis.app.domain.model.Song>) -> Unit) {
        viewModelScope.launch {
            val songs = getPlaylistSongsUseCase(playlistId).firstOrNull().orEmpty()
            if (songs.isNotEmpty()) {
                val ordered = if (shuffle) songs.shuffled() else songs
                onPlay(ordered)
            }
        }
    }
}
