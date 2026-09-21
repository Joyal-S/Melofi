package com.auralis.app

import com.auralis.app.playback.audiofx.VirtualizerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests for VirtualizerState, percentage conversion, and strength clamping.
 */
class VirtualizerStateTest {

    @Test
    fun defaultState_isUnsupportedAndDisabled() {
        val state = VirtualizerState()
        assertFalse(state.isSupported)
        assertFalse(state.isEnabled)
        assertEquals(0, state.strength)
        assertEquals(0, state.strengthPercentage)
    }

    @Test
    fun strengthPercentage_convertsMilliBelsToPercentage() {
        assertEquals(0, VirtualizerState(strength = 0).strengthPercentage)
        assertEquals(30, VirtualizerState(strength = 300).strengthPercentage)
        assertEquals(60, VirtualizerState(strength = 600).strengthPercentage)
        assertEquals(100, VirtualizerState(strength = 1000).strengthPercentage)
    }

    @Test
    fun percentageToStrength_convertsPercentageToMilliBels() {
        assertEquals(0, VirtualizerState.percentageToStrength(0))
        assertEquals(300, VirtualizerState.percentageToStrength(30))
        assertEquals(1000, VirtualizerState.percentageToStrength(100))
        assertEquals(1000, VirtualizerState.percentageToStrength(120)) // clamped
        assertEquals(0, VirtualizerState.percentageToStrength(-5)) // clamped
    }

    @Test
    fun clampStrength_restrictsWithinZeroAndThousand() {
        assertEquals(0, VirtualizerState.clampStrength(-100))
        assertEquals(0, VirtualizerState.clampStrength(0))
        assertEquals(750, VirtualizerState.clampStrength(750))
        assertEquals(1000, VirtualizerState.clampStrength(1000))
        assertEquals(1000, VirtualizerState.clampStrength(2000))
    }
}
