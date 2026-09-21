package com.auralis.app.playback.dsp

import com.auralis.app.data.local.preferences.AudioPreferences
import com.auralis.app.data.local.preferences.AudioPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton coordinator bridging DataStore persisted [AudioPreferences] with
 * the real-time [MelofyDspAudioProcessor] executing inside ExoPlayer's audio thread.
 *
 * All parameter mutations are dispatched asynchronously to persistent storage while
 * instantaneously updating the active audio processor via lock-free atomic swaps.
 */
@Singleton
class DspEffectsManager @Inject constructor(
    private val preferencesRepository: AudioPreferencesRepository,
    val audioProcessor: MelofyDspAudioProcessor
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _parametersState = MutableStateFlow(DspParameters.DEFAULT)
    val parametersState: StateFlow<DspParameters> = _parametersState.asStateFlow()

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                applyPreferencesToProcessor(prefs)
            }
        }
    }

    private fun applyPreferencesToProcessor(prefs: AudioPreferences) {
        val baseParams = if (prefs.dspPreset != DspPreset.CUSTOM) {
            DspParameters.createPreset(prefs.dspPreset)
        } else {
            DspParameters.DEFAULT.copy(
                loFi = DspParameters.DEFAULT.loFi.copy(
                    bitCrush = prefs.dspLoFiBitCrush,
                    sampleRateReduction = prefs.dspLoFiSampleReduction,
                    saturation = prefs.dspLoFiSaturation
                )
            )
        }

        val updated = baseParams.copy(
            preset = prefs.dspPreset,
            loFi = baseParams.loFi.copy(
                enabled = prefs.dspLoFiEnabled,
                intensity = prefs.dspLoFiIntensity
            ),
            reverb = baseParams.reverb.copy(
                enabled = prefs.dspReverbEnabled,
                roomSize = prefs.dspReverbRoomSize,
                decay = prefs.dspReverbDecay,
                wetLevel = prefs.dspReverbWetLevel
            )
        )

        _parametersState.update { updated }
        audioProcessor.updateParameters(updated)
    }

    fun setMasterEnabled(enabled: Boolean) {
        audioProcessor.engine.setLoFiEnabled(enabled)
        audioProcessor.engine.setReverbEnabled(enabled)
        val updated = audioProcessor.getParameters().copy(
            loFi = audioProcessor.getParameters().loFi.copy(enabled = enabled),
            reverb = audioProcessor.getParameters().reverb.copy(enabled = enabled)
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspLoFiEnabled(enabled)
            preferencesRepository.setDspReverbEnabled(enabled)
        }
    }

    fun setLoFiEnabled(enabled: Boolean) {
        audioProcessor.engine.setLoFiEnabled(enabled)
        val updated = audioProcessor.getParameters().copy(
            loFi = audioProcessor.getParameters().loFi.copy(enabled = enabled)
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch { preferencesRepository.setDspLoFiEnabled(enabled) }
    }

    fun setReverbEnabled(enabled: Boolean) {
        audioProcessor.engine.setReverbEnabled(enabled)
        val updated = audioProcessor.getParameters().copy(
            reverb = audioProcessor.getParameters().reverb.copy(enabled = enabled)
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch { preferencesRepository.setDspReverbEnabled(enabled) }
    }

    fun setPreset(preset: DspPreset) {
        val currentActive = _parametersState.value.isAnyActive
        audioProcessor.engine.setPreset(preset)
        if (currentActive) {
            audioProcessor.engine.setLoFiEnabled(true)
            audioProcessor.engine.setReverbEnabled(true)
        }
        val newParams = audioProcessor.getParameters().let {
            if (currentActive) {
                it.copy(
                    loFi = it.loFi.copy(enabled = true),
                    reverb = it.reverb.copy(enabled = true)
                )
            } else it
        }
        audioProcessor.updateParameters(newParams)
        _parametersState.update { newParams }
        scope.launch {
            preferencesRepository.setDspPreset(preset)
            preferencesRepository.setDspLoFiIntensity(newParams.loFi.intensity)
            preferencesRepository.setDspLoFiBitCrush(newParams.loFi.bitCrush)
            preferencesRepository.setDspLoFiSampleReduction(newParams.loFi.sampleRateReduction)
            preferencesRepository.setDspLoFiSaturation(newParams.loFi.saturation)
            preferencesRepository.setDspReverbRoomSize(newParams.reverb.roomSize)
            preferencesRepository.setDspReverbDecay(newParams.reverb.decay)
            preferencesRepository.setDspReverbWetLevel(newParams.reverb.wetLevel)
        }
    }

    fun setLoFiIntensity(intensity: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            loFi = current.loFi.copy(intensity = intensity.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspLoFiIntensity(updated.loFi.intensity)
        }
    }

    fun setLoFiBitCrush(bitCrush: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            loFi = current.loFi.copy(bitCrush = bitCrush.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspLoFiBitCrush(updated.loFi.bitCrush)
        }
    }

    fun setLoFiSampleReduction(sampleReduction: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            loFi = current.loFi.copy(sampleRateReduction = sampleReduction.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspLoFiSampleReduction(updated.loFi.sampleRateReduction)
        }
    }

    fun setLoFiSaturation(saturation: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            loFi = current.loFi.copy(saturation = saturation.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspLoFiSaturation(updated.loFi.saturation)
        }
    }

    fun setReverbRoomSize(roomSize: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            reverb = current.reverb.copy(roomSize = roomSize.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspReverbRoomSize(updated.reverb.roomSize)
        }
    }

    fun setReverbDecay(decay: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            reverb = current.reverb.copy(decay = decay.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspReverbDecay(updated.reverb.decay)
        }
    }

    fun setReverbWetLevel(wetLevel: Float) {
        val current = audioProcessor.getParameters()
        val updated = current.copy(
            preset = DspPreset.CUSTOM,
            reverb = current.reverb.copy(wetLevel = wetLevel.coerceIn(0.0f, 1.0f))
        )
        audioProcessor.updateParameters(updated)
        _parametersState.update { updated }
        scope.launch {
            preferencesRepository.setDspPreset(DspPreset.CUSTOM)
            preferencesRepository.setDspReverbWetLevel(updated.reverb.wetLevel)
        }
    }

    /**
     * Resets the DSP engine's internal delay lines, accumulators, and smoothing ramps.
     * Invoked during seek operations and track transitions.
     */
    fun reset() {
        audioProcessor.engine.reset()
    }
}
