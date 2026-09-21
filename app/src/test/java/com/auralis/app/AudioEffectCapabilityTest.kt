package com.auralis.app

import com.auralis.app.playback.audiofx.AudioEffectCapability
import com.auralis.app.playback.audiofx.AudioEffectsState
import com.auralis.app.playback.audiofx.BassBoostState
import com.auralis.app.playback.audiofx.EqualizerState
import com.auralis.app.playback.audiofx.VirtualizerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests for audio effect capability detection fallback and unsupported UI states.
 */
class AudioEffectCapabilityTest {

    @Test
    fun defaultCapabilities_allUnsupported() {
        val caps = AudioEffectCapability()
        assertFalse(caps.isEqualizerSupported)
        assertFalse(caps.isBassBoostSupported)
        assertFalse(caps.isVirtualizerSupported)
    }

    @Test
    fun unsupportedCapabilities_produceUnsupportedStateModels() {
        val caps = AudioEffectCapability(
            isEqualizerSupported = false,
            isBassBoostSupported = false,
            isVirtualizerSupported = false
        )

        val state = AudioEffectsState(
            equalizer = EqualizerState(isSupported = caps.isEqualizerSupported),
            bassBoost = BassBoostState(isSupported = caps.isBassBoostSupported),
            virtualizer = VirtualizerState(isSupported = caps.isVirtualizerSupported)
        )

        assertFalse(state.equalizer.isSupported)
        assertFalse(state.bassBoost.isSupported)
        assertFalse(state.virtualizer.isSupported)
        assertEquals(0, state.activeAudioSessionId)
    }

    @Test
    fun partialCapabilities_reflectedAccurately() {
        val caps = AudioEffectCapability(
            isEqualizerSupported = true,
            isBassBoostSupported = false,
            isVirtualizerSupported = true
        )

        val state = AudioEffectsState(
            equalizer = EqualizerState(isSupported = caps.isEqualizerSupported),
            bassBoost = BassBoostState(isSupported = caps.isBassBoostSupported),
            virtualizer = VirtualizerState(isSupported = caps.isVirtualizerSupported)
        )

        assert(state.equalizer.isSupported)
        assertFalse(state.bassBoost.isSupported)
        assert(state.virtualizer.isSupported)
    }
}
