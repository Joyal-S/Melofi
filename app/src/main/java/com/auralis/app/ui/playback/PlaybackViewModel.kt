package com.auralis.app.ui.playback

import androidx.lifecycle.ViewModel
import com.auralis.app.domain.model.Song
import com.auralis.app.playback.client.PlaybackController
import com.auralis.app.playback.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel exposing reactive playback state and user actions to Jetpack Compose UI.
 */
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackController: PlaybackController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState

    fun playSong(song: Song, allSongs: List<Song>) {
        val index = allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackController.playSongs(allSongs, index)
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        playbackController.playSongs(songs, startIndex)
    }

    fun addSongsToQueue(songs: List<Song>) {
        playbackController.addSongsToQueue(songs)
    }

    fun play() {
        playbackController.play()
    }

    fun pause() {
        playbackController.pause()
    }

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun next() {
        playbackController.skipNext()
    }

    fun previous() {
        playbackController.skipPrevious()
    }

    fun toggleShuffle() {
        playbackController.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playbackController.cycleRepeatMode()
    }
}
