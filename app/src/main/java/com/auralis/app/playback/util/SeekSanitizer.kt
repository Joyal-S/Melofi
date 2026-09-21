package com.auralis.app.playback.util

/**
 * Pure Kotlin utility for sanitizing seek operations across invalid boundaries.
 */
object SeekSanitizer {

    /**
     * Sanitizes requested seek position against known track duration.
     *
     * - Negative values are clamped to 0L.
     * - Values exceeding known positive duration are clamped to durationMs.
     * - Unknown or non-positive durations clamp negative values to 0L without upper bound.
     */
    fun sanitize(positionMs: Long, durationMs: Long): Long {
        if (positionMs <= 0L) return 0L
        if (durationMs > 0L && positionMs > durationMs) return durationMs
        return positionMs
    }
}
