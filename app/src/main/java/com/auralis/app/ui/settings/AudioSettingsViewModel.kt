package com.auralis.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.data.local.preferences.AudioPreferencesRepository
import com.auralis.app.playback.audiofx.AudioEffectsManager
import com.auralis.app.playback.audiofx.ReplayGainManager
import com.auralis.app.playback.audiofx.ReplayGainMode
import com.auralis.app.playback.audiofx.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel coordinating audio effects, ReplayGain normalization, sleep timer,
 * and user preferences for the Audio Settings screen.
 */
@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
    private val audioEffectsManager: AudioEffectsManager,
    private val replayGainManager: ReplayGainManager,
    private val sleepTimerManager: SleepTimerManager,
    private val preferencesRepository: AudioPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AudioSettingsUiState> = combine(
        audioEffectsManager.effectsState,
        replayGainManager.replayGainState,
        sleepTimerManager.timerState,
        preferencesRepository.preferencesFlow
    ) { effects, replayGain, sleepTimer, prefs ->
        AudioSettingsUiState(
            effectsState = effects,
            replayGainState = replayGain,
            sleepTimerState = sleepTimer,
            crossfadeEnabled = prefs.crossfadeEnabled,
            crossfadeDurationSeconds = prefs.crossfadeDurationSeconds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AudioSettingsUiState()
    )

    fun toggleEqualizer(enabled: Boolean) {
        audioEffectsManager.setEqualizerEnabled(enabled)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        audioEffectsManager.setEqualizerBandLevel(bandIndex, levelMilliBels)
    }

    fun selectEqualizerPreset(presetIndex: Int) {
        audioEffectsManager.useEqualizerPreset(presetIndex)
    }

    fun toggleBassBoost(enabled: Boolean) {
        audioEffectsManager.setBassBoostEnabled(enabled)
    }

    fun setBassBoostStrength(strength: Int) {
        audioEffectsManager.setBassBoostStrength(strength)
    }

    fun toggleVirtualizer(enabled: Boolean) {
        audioEffectsManager.setVirtualizerEnabled(enabled)
    }

    fun setVirtualizerStrength(strength: Int) {
        audioEffectsManager.setVirtualizerStrength(strength)
    }

    fun setReplayGainMode(mode: ReplayGainMode) {
        replayGainManager.setMode(mode)
    }

    fun startSleepTimerMinutes(minutes: Int) {
        sleepTimerManager.startMinutes(minutes)
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancel()
    }
}
