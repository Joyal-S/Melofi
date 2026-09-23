package com.auralis.app.ui.playlists.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.PlaylistRepository
import com.auralis.app.domain.usecase.playlists.GetPlaylistSongsUseCase
import com.auralis.app.domain.usecase.playlists.RemoveSongFromPlaylistUseCase
import com.auralis.app.domain.usecase.playlists.ReorderPlaylistSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.auralis.app.data.repository.LocalArtworkRepository
import com.auralis.app.domain.usecase.playlists.DeletePlaylistUseCase

/**
 * ViewModel managing the contents and actions of an active playlist.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val getPlaylistSongsUseCase: GetPlaylistSongsUseCase,
    private val removeSongFromPlaylistUseCase: RemoveSongFromPlaylistUseCase,
    private val reorderPlaylistSongsUseCase: ReorderPlaylistSongsUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    val localArtworkRepository: LocalArtworkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _playlistId = MutableStateFlow<Long?>(savedStateHandle.get<Long>("playlistId"))

    val playlist: StateFlow<Playlist?> = _playlistId
        .filterNotNull()
        .flatMapLatest { id -> playlistRepository.getPlaylist(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val songs: StateFlow<List<Song>> = _playlistId
        .filterNotNull()
        .flatMapLatest { id -> getPlaylistSongsUseCase(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setPlaylistId(id: Long) {
        _playlistId.value = id
    }

    fun removeSong(songId: Long) {
        val playlistId = _playlistId.value ?: return
        viewModelScope.launch {
            removeSongFromPlaylistUseCase(playlistId, songId)
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        val playlistId = _playlistId.value ?: return
        val currentSongs = songs.value
        if (fromIndex !in currentSongs.indices || toIndex !in currentSongs.indices) return

        val mutableList = currentSongs.map { it.id }.toMutableList()
        val movedId = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, movedId)

        viewModelScope.launch {
            reorderPlaylistSongsUseCase(playlistId, mutableList)
        }
    }

    fun updateCover(customCoverUri: String?) {
        val playlistId = _playlistId.value ?: return
        viewModelScope.launch {
            playlistRepository.updatePlaylistCover(playlistId, customCoverUri)
        }
    }

    fun updateDescription(description: String?) {
        val playlistId = _playlistId.value ?: return
        viewModelScope.launch {
            playlistRepository.updatePlaylistDescription(playlistId, description)
        }
    }

    fun setCustomArtwork(uri: android.net.Uri) {
        val playlistId = _playlistId.value ?: return
        viewModelScope.launch {
            val result = localArtworkRepository.setCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.PLAYLIST, playlistId.toString(), uri)
            result.getOrNull()?.let { localUri ->
                playlistRepository.updatePlaylistCover(playlistId, localUri)
            }
        }
    }

    fun clearCustomArtwork() {
        val playlistId = _playlistId.value ?: return
        viewModelScope.launch {
            localArtworkRepository.clearCustomArtwork(com.auralis.app.domain.model.ArtworkEntityType.PLAYLIST, playlistId.toString())
            playlistRepository.updatePlaylistCover(playlistId, null)
        }
    }

    fun getPlaylistArtworkUri(playlist: com.auralis.app.domain.model.Playlist): String? {
        return localArtworkRepository.getPlaylistArtworkUri(playlist.id, playlist.customCoverUri)
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        val playlistId = _playlistId.value ?: return
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId).onSuccess {
                onDeleted()
            }
        }
    }
}
