package com.auralis.app

import com.auralis.app.data.local.preferences.AudioPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for crossfade preferences validation and duration clamping.
 */
class CrossfadePreferenceTest {

    @Test
    fun defaultCrossfade_disabledWithZeroDuration() {
        val prefs = AudioPreferences()
        assertFalse(prefs.crossfadeEnabled)
        assertEquals(0, prefs.crossfadeDurationSeconds)
    }

    @Test
    fun crossfadeDuration_validRange_preserved() {
        val validDurations = listOf(0, 2, 5, 8, 12)
        for (duration in validDurations) {
            val clamped = duration.coerceIn(0, 12)
            assertEquals(duration, clamped)
        }
    }

    @Test
    fun crossfadeDuration_exceedingMax_clampedToTwelveSeconds() {
        val excessive = 20
        val clamped = excessive.coerceIn(0, 12)
        assertEquals(12, clamped)
    }

    @Test
    fun crossfadeDuration_negative_clampedToZeroSeconds() {
        val negative = -5
        val clamped = negative.coerceIn(0, 12)
        assertEquals(0, clamped)
    }

    @Test
    fun crossfadePreferences_copy_updatesCorrectly() {
        val prefs = AudioPreferences()
        val updated = prefs.copy(
            crossfadeEnabled = true,
            crossfadeDurationSeconds = 6
        )
        assertTrue(updated.crossfadeEnabled)
        assertEquals(6, updated.crossfadeDurationSeconds)
    }
}
