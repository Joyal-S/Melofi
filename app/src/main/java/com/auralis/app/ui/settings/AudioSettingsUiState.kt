package com.auralis.app.ui.settings

import com.auralis.app.playback.audiofx.AudioEffectsState
import com.auralis.app.playback.audiofx.ReplayGainState
import com.auralis.app.playback.audiofx.SleepTimerState

/**
 * Composite UI state for the Audio Settings screen.
 */
data class AudioSettingsUiState(
    val effectsState: AudioEffectsState = AudioEffectsState(),
    val replayGainState: ReplayGainState = ReplayGainState(),
    val sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 0
)
