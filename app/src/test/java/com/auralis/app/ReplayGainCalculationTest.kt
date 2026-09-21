package com.auralis.app

import com.auralis.app.playback.audiofx.ReplayGainMetadata
import com.auralis.app.playback.audiofx.ReplayGainMode
import com.auralis.app.playback.audiofx.ReplayGainParser
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

/**
 * Unit tests for ReplayGain volume multiplier computation, anti-clipping protection,
 * and track/album fallback policies.
 */
class ReplayGainCalculationTest {

    @Test
    fun calculateMultiplier_modeOff_alwaysReturnsUnityGain() {
        val meta = ReplayGainMetadata(trackGainDb = -10.0f, trackPeak = 0.5f)
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.OFF)
        assertEquals(1.0f, multiplier, 0.0001f)
    }

    @Test
    fun calculateMultiplier_nullOrEmptyMetadata_returnsUnityGain() {
        assertEquals(1.0f, ReplayGainParser.calculateVolumeMultiplier(null, ReplayGainMode.TRACK), 0.0001f)
        assertEquals(1.0f, ReplayGainParser.calculateVolumeMultiplier(ReplayGainMetadata(), ReplayGainMode.TRACK), 0.0001f)
        assertEquals(1.0f, ReplayGainParser.calculateVolumeMultiplier(null, ReplayGainMode.ALBUM), 0.0001f)
    }

    @Test
    fun calculateMultiplier_zeroGain_returnsUnityGain() {
        val meta = ReplayGainMetadata(trackGainDb = 0.0f)
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.TRACK)
        assertEquals(1.0f, multiplier, 0.0001f)
    }

    @Test
    fun calculateMultiplier_negativeGain_computesAttenuation() {
        val gainDb = -6.0206f // Half power (-6 dB is approx 0.5x amplitude)
        val meta = ReplayGainMetadata(trackGainDb = gainDb)
        val expected = 10.0.pow(gainDb.toDouble() / 20.0).toFloat()

        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.TRACK)
        assertEquals(expected, multiplier, 0.001f)
        assertEquals(0.50f, multiplier, 0.01f)
    }

    @Test
    fun calculateMultiplier_positiveGain_clampedToUnityVolume() {
        val meta = ReplayGainMetadata(trackGainDb = 6.0f) // ~2.0x linear
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.TRACK)
        // Standard Android player volume maximum is 1.0f
        assertEquals(1.0f, multiplier, 0.0001f)
    }

    @Test
    fun calculateMultiplier_antiClipping_reducesMultiplierWhenPeakExceedsHeadroom() {
        // gain = -1 dB -> linear ~ 0.8913
        // peak = 1.25 -> 0.8913 * 1.25 = 1.114 > 1.0 (clipping risk!)
        // anti-clipping target = 1.0 / 1.25 = 0.8000
        val meta = ReplayGainMetadata(trackGainDb = -1.0f, trackPeak = 1.25f)
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.TRACK)

        assertEquals(0.80f, multiplier, 0.001f)
    }

    @Test
    fun calculateMultiplier_albumMode_usesAlbumGainWhenAvailable() {
        val meta = ReplayGainMetadata(
            trackGainDb = -8.0f,
            albumGainDb = -3.0f
        )
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.ALBUM)
        val expectedAlbumMultiplier = 10.0.pow(-3.0 / 20.0).toFloat()
        assertEquals(expectedAlbumMultiplier, multiplier, 0.001f)
    }

    @Test
    fun calculateMultiplier_albumMode_fallsBackToTrackGainWhenAlbumGainMissing() {
        val meta = ReplayGainMetadata(
            trackGainDb = -4.0f,
            albumGainDb = null
        )
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.ALBUM)
        val expectedTrackMultiplier = 10.0.pow(-4.0 / 20.0).toFloat()
        assertEquals(expectedTrackMultiplier, multiplier, 0.001f)
    }

    @Test
    fun calculateMultiplier_trackMode_ignoresAlbumGain() {
        val meta = ReplayGainMetadata(
            trackGainDb = -5.0f,
            albumGainDb = -2.0f
        )
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(meta, ReplayGainMode.TRACK)
        val expectedTrackMultiplier = 10.0.pow(-5.0 / 20.0).toFloat()
        assertEquals(expectedTrackMultiplier, multiplier, 0.001f)
    }
}
