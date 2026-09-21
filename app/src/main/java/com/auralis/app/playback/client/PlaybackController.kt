package com.auralis.app.playback.client

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.app.domain.model.Song
import com.auralis.app.playback.mapper.MediaItemMapper
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.playback.model.PlaybackState
import com.auralis.app.playback.model.PlaybackStatus
import com.auralis.app.playback.queue.PlaybackQueue
import com.auralis.app.playback.service.AuralisPlaybackService
import com.auralis.app.playback.util.SeekSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authoritative client managing connection to [AuralisPlaybackService] via [MediaController].
 *
 * Translates Media3 callbacks into domain [PlaybackState], tracks active queue,
 * and executes playback commands off the UI thread.
 */
@Singleton
class PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playbackQueue: PlaybackQueue
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaController: MediaController? = null
    private var positionUpdateJob: Job? = null

    init {
        connectToService()
    }

    private fun connectToService(onConnected: ((MediaController) -> Unit)? = null) {
        if (mediaController != null) {
            onConnected?.invoke(mediaController!!)
            return
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, AuralisPlaybackService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    controller.addListener(PlayerListener())
                    syncStateFromPlayer(controller)
                    onConnected?.invoke(controller)
                } catch (e: Exception) {
                    _playbackState.update {
                        it.copy(
                            status = PlaybackStatus.ERROR,
                            errorMessage = e.localizedMessage ?: "Failed to connect to playback service."
                        )
                    }
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        playbackQueue.setQueue(songs, startIndex)
        val targetSong = playbackQueue.currentSong()
        val mediaItems = MediaItemMapper.toMediaItemList(songs)

        ensureController { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()

            _playbackState.update {
                it.copy(
                    currentSong = targetSong,
                    status = PlaybackStatus.LOADING,
                    errorMessage = null
                )
            }
        }
    }

    fun play() {
        ensureController { it.play() }
    }

    fun pause() {
        ensureController { it.pause() }
    }

    fun togglePlayPause() {
        ensureController { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        ensureController { controller ->
            val duration = if (controller.duration > 0) controller.duration else _playbackState.value.durationMs
            val sanitized = SeekSanitizer.sanitize(positionMs, duration)
            controller.seekTo(sanitized)
            _playbackState.update { it.copy(currentPositionMs = sanitized) }
        }
    }

    fun skipNext() {
        ensureController { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (_playbackState.value.repeatMode == PlaybackRepeatMode.ALL) {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun skipPrevious() {
        ensureController { controller ->
            if (controller.currentPosition > 3000L) {
                // Restart track if more than 3 seconds in
                controller.seekTo(0L)
            } else if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else if (_playbackState.value.repeatMode == PlaybackRepeatMode.ALL) {
                val lastIndex = controller.mediaItemCount - 1
                if (lastIndex >= 0) {
                    controller.seekTo(lastIndex, 0L)
                }
            } else {
                controller.seekTo(0L)
            }
        }
    }

    fun toggleShuffle() {
        ensureController { controller ->
            val newShuffleState = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newShuffleState
            _playbackState.update { it.copy(shuffleEnabled = newShuffleState) }
        }
    }

    fun cycleRepeatMode() {
        ensureController { controller ->
            val nextMode = _playbackState.value.repeatMode.next()
            controller.repeatMode = toMedia3RepeatMode(nextMode)
            _playbackState.update { it.copy(repeatMode = nextMode) }
        }
    }

    private fun ensureController(action: (MediaController) -> Unit) {
        mediaController?.let {
            action(it)
        } ?: run {
            connectToService { controller ->
                action(controller)
            }
        }
    }

    private fun syncStateFromPlayer(player: Player) {
        val currentMediaItem = player.currentMediaItem
        val songId = MediaItemMapper.extractSongId(currentMediaItem)
        val currentSong = songId?.let { playbackQueue.findSongById(it) }

        val status = mapPlaybackStatus(player.playbackState, player.isPlaying, player.playerError != null)
        val duration = if (player.duration > 0) player.duration else currentSong?.duration ?: 0L
        val repeatMode = mapMedia3RepeatMode(player.repeatMode)

        _playbackState.update {
            it.copy(
                status = status,
                currentSong = currentSong ?: it.currentSong,
                isPlaying = player.isPlaying,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = repeatMode,
                errorMessage = player.playerError?.localizedMessage
            )
        }

        if (player.isPlaying) {
            startPositionTracker()
        } else {
            stopPositionTracker()
        }
    }

    private fun startPositionTracker() {
        if (positionUpdateJob?.isActive == true) return
        positionUpdateJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playbackState.update {
                            it.copy(
                                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                                bufferedPositionMs = controller.bufferedPosition.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionTracker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun release() {
        stopPositionTracker()
        mediaController?.release()
        mediaController = null
    }

    private inner class PlayerListener : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            mediaController?.let { syncStateFromPlayer(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            mediaController?.let { syncStateFromPlayer(it) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaController?.let { syncStateFromPlayer(it) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playbackState.update { it.copy(repeatMode = mapMedia3RepeatMode(repeatMode)) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = error.localizedMessage ?: "Playback error occurred."
                )
            }
        }
    }

    companion object {
        fun mapPlaybackStatus(playbackState: Int, isPlaying: Boolean, hasError: Boolean): PlaybackStatus {
            if (hasError) return PlaybackStatus.ERROR
            return when (playbackState) {
                Player.STATE_IDLE -> PlaybackStatus.IDLE
                Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
                Player.STATE_READY -> if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.READY
                Player.STATE_ENDED -> PlaybackStatus.ENDED
                else -> PlaybackStatus.IDLE
            }
        }

        fun mapMedia3RepeatMode(media3Mode: Int): PlaybackRepeatMode = when (media3Mode) {
            Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
            Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
            else -> PlaybackRepeatMode.OFF
        }

        fun toMedia3RepeatMode(mode: PlaybackRepeatMode): Int = when (mode) {
            PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
            PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }
}
