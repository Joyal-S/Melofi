package com.auralis.app.data.local.preferences

import com.auralis.app.playback.audiofx.EqualizerState
import com.auralis.app.playback.audiofx.ReplayGainMode

/**
 * Persisted user configuration for audio processing and playback behavior.
 */
data class AudioPreferences(
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Int = EqualizerState.PRESET_CUSTOM,
    val equalizerBandLevels: Map<Int, Int> = emptyMap(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 0
)
