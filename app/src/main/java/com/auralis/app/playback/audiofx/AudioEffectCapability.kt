package com.auralis.app.playback.audiofx

/**
 * Immutable representation of audio effect capabilities detected on the current device.
 */
data class AudioEffectCapability(
    val isEqualizerSupported: Boolean = false,
    val isBassBoostSupported: Boolean = false,
    val isVirtualizerSupported: Boolean = false
)
