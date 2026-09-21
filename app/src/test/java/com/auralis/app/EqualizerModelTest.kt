package com.auralis.app

import com.auralis.app.playback.audiofx.EqualizerBand
import com.auralis.app.playback.audiofx.EqualizerState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for Equalizer models, frequency formatting, millibels formatting,
 * and band gain level clamping.
 */
class EqualizerModelTest {

    @Test
    fun frequencyFormatting_subMegaHertz_formatsAsHz() {
        assertEquals("60 Hz", EqualizerState.formatFrequency(60_000))
        assertEquals("230 Hz", EqualizerState.formatFrequency(230_000))
        assertEquals("910 Hz", EqualizerState.formatFrequency(910_000))
    }

    @Test
    fun frequencyFormatting_exactKilohertz_formatsWithoutDecimal() {
        assertEquals("1 kHz", EqualizerState.formatFrequency(1_000_000))
        assertEquals("4 kHz", EqualizerState.formatFrequency(4_000_000))
        assertEquals("14 kHz", EqualizerState.formatFrequency(14_000_000))
    }

    @Test
    fun frequencyFormatting_fractionalKilohertz_formatsWithOneDecimal() {
        assertEquals("2.5 kHz", EqualizerState.formatFrequency(2_500_000))
        assertEquals("3.6 kHz", EqualizerState.formatFrequency(3_600_000))
    }

    @Test
    fun frequencyFormatting_zeroOrNegative_returnsZeroHz() {
        assertEquals("0 Hz", EqualizerState.formatFrequency(0))
        assertEquals("0 Hz", EqualizerState.formatFrequency(-1000))
    }

    @Test
    fun milliBelsFormatting_positive_formatsWithPlusAndDb() {
        assertEquals("+3 dB", EqualizerState.formatMilliBels(300))
        assertEquals("+6 dB", EqualizerState.formatMilliBels(600))
        assertEquals("+4.5 dB", EqualizerState.formatMilliBels(450))
    }

    @Test
    fun milliBelsFormatting_zero_formatsAsZeroDb() {
        assertEquals("0 dB", EqualizerState.formatMilliBels(0))
    }

    @Test
    fun milliBelsFormatting_negative_formatsWithMinusAndDb() {
        assertEquals("-3 dB", EqualizerState.formatMilliBels(-300))
        assertEquals("-6 dB", EqualizerState.formatMilliBels(-600))
        assertEquals("-2.5 dB", EqualizerState.formatMilliBels(-250))
    }

    @Test
    fun clampLevel_withinBounds_returnsSameValue() {
        val clamped = EqualizerState.clampLevel(levelMilliBels = 200, min = -1500, max = 1500)
        assertEquals(200, clamped)
    }

    @Test
    fun clampLevel_belowMin_clampedToMin() {
        val clamped = EqualizerState.clampLevel(levelMilliBels = -2000, min = -1500, max = 1500)
        assertEquals(-1500, clamped)
    }

    @Test
    fun clampLevel_aboveMax_clampedToMax() {
        val clamped = EqualizerState.clampLevel(levelMilliBels = 3000, min = -1500, max = 1500)
        assertEquals(1500, clamped)
    }

    @Test
    fun clampLevel_invertedBounds_handlesGracefully() {
        val clamped = EqualizerState.clampLevel(levelMilliBels = 2000, min = 1500, max = -1500)
        assertEquals(1500, clamped)
    }

    @Test
    fun bandProperties_computeFormattedValuesCorrectly() {
        val band = EqualizerBand(index = 0, centerFreqMilliHertz = 60_000, levelMilliBels = 300)
        assertEquals("60 Hz", band.centerFreqFormatted)
        assertEquals("+3 dB", band.levelFormatted)
    }
}
