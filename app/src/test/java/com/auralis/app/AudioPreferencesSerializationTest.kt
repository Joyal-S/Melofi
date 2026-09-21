package com.auralis.app

import com.auralis.app.data.local.preferences.AudioPreferences
import com.auralis.app.data.local.preferences.AudioPreferencesRepository
import com.auralis.app.playback.audiofx.EqualizerState
import com.auralis.app.playback.audiofx.ReplayGainMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for audio preferences serialization, band levels encoding/decoding,
 * and preference defaults.
 */
class AudioPreferencesSerializationTest {

    @Test
    fun defaultPreferences_haveSensibleDefaults() {
        val prefs = AudioPreferences()
        assertFalse(prefs.equalizerEnabled)
        assertEquals(EqualizerState.PRESET_CUSTOM, prefs.equalizerPreset)
        assertTrue(prefs.equalizerBandLevels.isEmpty())
        assertFalse(prefs.bassBoostEnabled)
        assertEquals(0, prefs.bassBoostStrength)
        assertFalse(prefs.virtualizerEnabled)
        assertEquals(0, prefs.virtualizerStrength)
        assertEquals(ReplayGainMode.OFF, prefs.replayGainMode)
        assertFalse(prefs.crossfadeEnabled)
        assertEquals(0, prefs.crossfadeDurationSeconds)
    }

    @Test
    fun serializeBandLevels_producesExpectedFormat() {
        val map = mapOf(0 to 150, 1 to -300, 2 to 0, 3 to 450)
        val serialized = AudioPreferencesRepository.serializeBandLevels(map)
        assertEquals("0:150,1:-300,2:0,3:450", serialized)
    }

    @Test
    fun parseBandLevels_validString_reconstructsMap() {
        val raw = "0:150,1:-300,2:0,3:450"
        val parsed = AudioPreferencesRepository.parseBandLevels(raw)
        assertEquals(4, parsed.size)
        assertEquals(150, parsed[0])
        assertEquals(-300, parsed[1])
        assertEquals(0, parsed[2])
        assertEquals(450, parsed[3])
    }

    @Test
    fun parseBandLevels_nullOrBlank_returnsEmptyMap() {
        assertTrue(AudioPreferencesRepository.parseBandLevels(null).isEmpty())
        assertTrue(AudioPreferencesRepository.parseBandLevels("").isEmpty())
        assertTrue(AudioPreferencesRepository.parseBandLevels("   ").isEmpty())
    }

    @Test
    fun parseBandLevels_malformedEntries_ignoresMalformedAndPreservesValid() {
        val raw = "0:100,corrupted_token,1:-200,bad:number,2:300"
        val parsed = AudioPreferencesRepository.parseBandLevels(raw)
        assertEquals(3, parsed.size)
        assertEquals(100, parsed[0])
        assertEquals(-200, parsed[1])
        assertEquals(300, parsed[2])
    }
}
