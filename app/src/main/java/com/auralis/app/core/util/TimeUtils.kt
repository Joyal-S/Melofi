package com.auralis.app.core.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Time formatting helpers for audio duration display.
 */
object TimeUtils {

    /**
     * Formats duration in milliseconds into "m:ss" or "h:mm:ss".
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0:00"

        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
}
