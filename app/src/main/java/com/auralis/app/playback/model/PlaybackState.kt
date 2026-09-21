package com.auralis.app.playback.model

import com.auralis.app.domain.model.Song

/**
 * High-level playback status.
 */
enum class PlaybackStatus {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    BUFFERING,
    ENDED,
    ERROR
}

/**
 * Repeat modes supported by Auralis playback engine.
 */
enum class PlaybackRepeatMode {
    OFF,
    ONE,
    ALL;

    fun next(): PlaybackRepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

/**
 * Domain-level playback state representation for UI and clients.
 */
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
