package com.auralis.app

import com.auralis.app.playback.audiofx.BassBoostState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BassBoostState, percentage conversion, and strength clamping.
 */
class BassBoostStateTest {

    @Test
    fun defaultState_isUnsupportedAndDisabled() {
        val state = BassBoostState()
        assertFalse(state.isSupported)
        assertFalse(state.isEnabled)
        assertEquals(0, state.strength)
        assertEquals(0, state.strengthPercentage)
    }

    @Test
    fun strengthPercentage_convertsMilliBelsToPercentage() {
        assertEquals(0, BassBoostState(strength = 0).strengthPercentage)
        assertEquals(25, BassBoostState(strength = 250).strengthPercentage)
        assertEquals(50, BassBoostState(strength = 500).strengthPercentage)
        assertEquals(75, BassBoostState(strength = 750).strengthPercentage)
        assertEquals(100, BassBoostState(strength = 1000).strengthPercentage)
    }

    @Test
    fun percentageToStrength_convertsPercentageToMilliBels() {
        assertEquals(0, BassBoostState.percentageToStrength(0))
        assertEquals(500, BassBoostState.percentageToStrength(50))
        assertEquals(1000, BassBoostState.percentageToStrength(100))
        assertEquals(1000, BassBoostState.percentageToStrength(150)) // clamped
        assertEquals(0, BassBoostState.percentageToStrength(-10)) // clamped
    }

    @Test
    fun clampStrength_restrictsWithinZeroAndThousand() {
        assertEquals(0, BassBoostState.clampStrength(-50))
        assertEquals(0, BassBoostState.clampStrength(0))
        assertEquals(450, BassBoostState.clampStrength(450))
        assertEquals(1000, BassBoostState.clampStrength(1000))
        assertEquals(1000, BassBoostState.clampStrength(1500))
    }
}
