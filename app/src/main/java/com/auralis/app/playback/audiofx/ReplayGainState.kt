package com.auralis.app.playback.audiofx

/**
 * Observable UI state for ReplayGain loudness normalization.
 */
data class ReplayGainState(
    val mode: ReplayGainMode = ReplayGainMode.OFF,
    val currentMetadata: ReplayGainMetadata? = null,
    val appliedVolumeMultiplier: Float = 1.0f
)
