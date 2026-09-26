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

import com.auralis.app.playback.dsp.DspEffectsManager
import com.auralis.app.playback.dsp.DspPreset

/**
 * ViewModel coordinating audio effects, ReplayGain normalization, sleep timer,
 * real-time Lo-Fi + Reverb DSP processing, and user preferences for the Audio Settings screen.
 */
@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
    private val audioEffectsManager: AudioEffectsManager,
    private val replayGainManager: ReplayGainManager,
    private val sleepTimerManager: SleepTimerManager,
    private val preferencesRepository: AudioPreferencesRepository,
    private val dspEffectsManager: DspEffectsManager
) : ViewModel() {

    val uiState: StateFlow<AudioSettingsUiState> = combine(
        audioEffectsManager.effectsState,
        replayGainManager.replayGainState,
        sleepTimerManager.timerState,
        preferencesRepository.preferencesFlow,
        dspEffectsManager.parametersState
    ) { effects, replayGain, sleepTimer, prefs, dspParams ->
        AudioSettingsUiState(
            effectsState = effects,
            replayGainState = replayGain,
            sleepTimerState = sleepTimer,
            crossfadeEnabled = prefs.crossfadeEnabled,
            crossfadeDurationSeconds = prefs.crossfadeDurationSeconds,
            dspParameters = dspParams
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

    // Real-Time Lo-Fi + Reverb DSP Controls

    fun toggleLoFiReverbMaster(enabled: Boolean) {
        dspEffectsManager.setMasterEnabled(enabled)
    }

    fun setLoFiReverbPreset(preset: DspPreset) {
        dspEffectsManager.setPreset(preset)
    }

    fun setLoFiReverbIntensity(intensity: Float) {
        dspEffectsManager.setLoFiIntensity(intensity)
    }

    fun setLoFiBitCrush(bitCrush: Float) {
        dspEffectsManager.setLoFiBitCrush(bitCrush)
    }

    fun setLoFiSampleReduction(sampleReduction: Float) {
        dspEffectsManager.setLoFiSampleReduction(sampleReduction)
    }

    fun setLoFiSaturation(saturation: Float) {
        dspEffectsManager.setLoFiSaturation(saturation)
    }

    fun setLoFiWowFlutter(wowFlutter: Float) {
        dspEffectsManager.setLoFiWowFlutter(wowFlutter)
    }

    fun setReverbRoomSize(roomSize: Float) {
        dspEffectsManager.setReverbRoomSize(roomSize)
    }

    fun setReverbDecay(decay: Float) {
        dspEffectsManager.setReverbDecay(decay)
    }

    fun setReverbWetLevel(wetLevel: Float) {
        dspEffectsManager.setReverbWetLevel(wetLevel)
    }
}
