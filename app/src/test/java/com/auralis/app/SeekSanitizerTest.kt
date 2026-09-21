package com.auralis.app

import com.auralis.app.playback.util.SeekSanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SeekSanitizer].
 * Verifies clamping of seek requests across valid ranges and edge cases.
 */
class SeekSanitizerTest {

    @Test
    fun sanitize_negativePosition_clampedToZero() {
        val result = SeekSanitizer.sanitize(positionMs = -500L, durationMs = 180000L)
        assertEquals(0L, result)
    }

    @Test
    fun sanitize_zeroPosition_returnsZero() {
        val result = SeekSanitizer.sanitize(positionMs = 0L, durationMs = 180000L)
        assertEquals(0L, result)
    }

    @Test
    fun sanitize_validPositionWithinDuration_returnsPosition() {
        val result = SeekSanitizer.sanitize(positionMs = 60000L, durationMs = 180000L)
        assertEquals(60000L, result)
    }

    @Test
    fun sanitize_positionExceedingDuration_clampedToDuration() {
        val result = SeekSanitizer.sanitize(positionMs = 200000L, durationMs = 180000L)
        assertEquals(180000L, result)
    }

    @Test
    fun sanitize_unknownDuration_negativeClampedToZero() {
        val result = SeekSanitizer.sanitize(positionMs = -100L, durationMs = -1L)
        assertEquals(0L, result)
    }

    @Test
    fun sanitize_unknownDuration_positiveRetained() {
        val result = SeekSanitizer.sanitize(positionMs = 50000L, durationMs = -1L)
        assertEquals(50000L, result)
    }

    @Test
    fun sanitize_zeroDuration_clampedToZero() {
        val result = SeekSanitizer.sanitize(positionMs = 5000L, durationMs = 0L)
        assertEquals(5000L, result)
    }
}
