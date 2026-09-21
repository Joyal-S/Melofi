package com.auralis.app.playback.audiofx

import java.util.Locale

/**
 * Represents the lifecycle state of the playback sleep timer.
 */
sealed interface SleepTimerState {

    /** No active countdown. */
    data object Inactive : SleepTimerState

    /** Countdown is actively running. */
    data class Running(
        val remainingSeconds: Long,
        val totalDurationSeconds: Long
    ) : SleepTimerState {
        val formattedRemaining: String
            get() {
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val seconds = remainingSeconds % 60
                return if (hours > 0) {
                    String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.US, "%02d:%02d", minutes, seconds)
                }
            }

        val progress: Float
            get() = if (totalDurationSeconds > 0) {
                (remainingSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }

    /** Timer completed and triggered playback pause. */
    data object Completed : SleepTimerState
}
