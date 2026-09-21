package com.auralis.app.playback.audiofx

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.auralis.app.data.local.preferences.AudioPreferences
import com.auralis.app.data.local.preferences.AudioPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle, hardware binding, and parameter configuration of Android audio effects
 * ([Equalizer], [BassBoost], [Virtualizer]) tied directly to ExoPlayer's active audio session.
 *
 * All operations are defensively guarded against OEM HAL crashes, vendor-specific exceptions,
 * and audio session transitions.
 */
@Singleton
class AudioEffectsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val capabilityDetector: AudioEffectCapabilityDetector,
    private val preferencesRepository: AudioPreferencesRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _effectsState = MutableStateFlow(AudioEffectsState())
    val effectsState: StateFlow<AudioEffectsState> = _effectsState.asStateFlow()

    private var nativeEqualizer: Equalizer? = null
    private var nativeBassBoost: BassBoost? = null
    private var nativeVirtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    init {
        // Initial capability detection
        val caps = capabilityDetector.detectCapabilities()
        _effectsState.update { current ->
            current.copy(
                equalizer = current.equalizer.copy(isSupported = caps.isEqualizerSupported),
                bassBoost = current.bassBoost.copy(isSupported = caps.isBassBoostSupported),
                virtualizer = current.virtualizer.copy(isSupported = caps.isVirtualizerSupported)
            )
        }
    }

    /**
     * Binds effects to a new player audio session, releasing any previously active effects.
     */
    fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentAudioSessionId) return

        releaseEffects()
        currentAudioSessionId = audioSessionId

        scope.launch {
            val savedPrefs = preferencesRepository.preferencesFlow.first()
            initializeEffectsForSession(audioSessionId, savedPrefs)
        }
    }

    private fun initializeEffectsForSession(audioSessionId: Int, savedPrefs: AudioPreferences) {
        val caps = capabilityDetector.detectCapabilities()

        // 1. Initialize Equalizer
        var eqState = EqualizerState(isSupported = caps.isEqualizerSupported)
        if (caps.isEqualizerSupported) {
            runCatching {
                val eq = Equalizer(0, audioSessionId)
                val numBands = eq.numberOfBands.toInt()
                val bandLevelRange = eq.bandLevelRange // [min, max] in milliBels
                val minLevel = bandLevelRange.getOrNull(0)?.toInt() ?: -1500
                val maxLevel = bandLevelRange.getOrNull(1)?.toInt() ?: 1500

                val bands = mutableListOf<EqualizerBand>()
                for (i in 0 until numBands) {
                    val centerFreq = eq.getCenterFreq(i.toShort())
                    // Check if user has a saved level for this band, otherwise read hardware level
                    val savedLevel = savedPrefs.equalizerBandLevels[i]
                    val level = if (savedLevel != null) {
                        val clamped = EqualizerState.clampLevel(savedLevel, minLevel, maxLevel)
                        runCatching { eq.setBandLevel(i.toShort(), clamped.toShort()) }
                        clamped
                    } else {
                        eq.getBandLevel(i.toShort()).toInt()
                    }
                    bands.add(EqualizerBand(index = i, centerFreqMilliHertz = centerFreq, levelMilliBels = level))
                }

                val presets = mutableListOf<EqualizerPreset>()
                val numPresets = eq.numberOfPresets.toInt()
                for (p in 0 until numPresets) {
                    val name = eq.getPresetName(p.toShort()) ?: "Preset $p"
                    presets.add(EqualizerPreset(index = p, name = name))
                }

                // Apply enabled state and preset if saved
                eq.enabled = savedPrefs.equalizerEnabled
                if (savedPrefs.equalizerPreset in 0 until numPresets) {
                    runCatching { eq.usePreset(savedPrefs.equalizerPreset.toShort()) }
                }

                nativeEqualizer = eq
                eqState = EqualizerState(
                    isSupported = true,
                    isEnabled = eq.enabled,
                    minLevelMilliBels = minLevel,
                    maxLevelMilliBels = maxLevel,
                    bands = bands,
                    presets = presets,
                    currentPresetIndex = savedPrefs.equalizerPreset
                )
            }.onFailure { e ->
                Log.e(TAG, "Equalizer initialization failed on session $audioSessionId: ${e.message}", e)
                eqState = EqualizerState(isSupported = false)
            }
        }

        // 2. Initialize BassBoost
        var bbState = BassBoostState(isSupported = caps.isBassBoostSupported)
        if (caps.isBassBoostSupported) {
            runCatching {
                val bb = BassBoost(0, audioSessionId)
                val isStrengthSupported = bb.strengthSupported
                if (isStrengthSupported) {
                    bb.enabled = savedPrefs.bassBoostEnabled
                    val clampedStrength = BassBoostState.clampStrength(savedPrefs.bassBoostStrength)
                    bb.setStrength(clampedStrength.toShort())
                    nativeBassBoost = bb
                    bbState = BassBoostState(
                        isSupported = true,
                        isEnabled = bb.enabled,
                        strength = bb.roundedStrength.toInt()
                    )
                } else {
                    bbState = BassBoostState(isSupported = false)
                }
            }.onFailure { e ->
                Log.e(TAG, "BassBoost initialization failed on session $audioSessionId: ${e.message}", e)
                bbState = BassBoostState(isSupported = false)
            }
        }

        // 3. Initialize Virtualizer
        var virtState = VirtualizerState(isSupported = caps.isVirtualizerSupported)
        if (caps.isVirtualizerSupported) {
            runCatching {
                val virt = Virtualizer(0, audioSessionId)
                val isStrengthSupported = virt.strengthSupported
                if (isStrengthSupported) {
                    virt.enabled = savedPrefs.virtualizerEnabled
                    val clampedStrength = VirtualizerState.clampStrength(savedPrefs.virtualizerStrength)
                    virt.setStrength(clampedStrength.toShort())
                    nativeVirtualizer = virt
                    virtState = VirtualizerState(
                        isSupported = true,
                        isEnabled = virt.enabled,
                        strength = virt.roundedStrength.toInt()
                    )
                } else {
                    virtState = VirtualizerState(isSupported = false)
                }
            }.onFailure { e ->
                Log.e(TAG, "Virtualizer initialization failed on session $audioSessionId: ${e.message}", e)
                virtState = VirtualizerState(isSupported = false)
            }
        }

        _effectsState.update {
            it.copy(
                equalizer = eqState,
                bassBoost = bbState,
                virtualizer = virtState,
                activeAudioSessionId = audioSessionId
            )
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        runCatching {
            nativeEqualizer?.enabled = enabled
        }.onFailure { Log.e(TAG, "Failed to toggle equalizer: ${it.message}") }

        _effectsState.update { current ->
            current.copy(equalizer = current.equalizer.copy(isEnabled = enabled))
        }
        scope.launch { preferencesRepository.setEqualizerEnabled(enabled) }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val currentEq = _effectsState.value.equalizer
        val clamped = EqualizerState.clampLevel(
            levelMilliBels,
            currentEq.minLevelMilliBels,
            currentEq.maxLevelMilliBels
        )

        runCatching {
            nativeEqualizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        }.onFailure { Log.e(TAG, "Failed to set band $bandIndex level: ${it.message}") }

        val updatedBands = currentEq.bands.map { band ->
            if (band.index == bandIndex) band.copy(levelMilliBels = clamped) else band
        }

        _effectsState.update { current ->
            current.copy(
                equalizer = current.equalizer.copy(
                    bands = updatedBands,
                    currentPresetIndex = EqualizerState.PRESET_CUSTOM
                )
            )
        }

        scope.launch {
            val levelMap = updatedBands.associate { it.index to it.levelMilliBels }
            preferencesRepository.setEqualizerBandLevels(levelMap, EqualizerState.PRESET_CUSTOM)
        }
    }

    fun useEqualizerPreset(presetIndex: Int) {
        val currentEq = _effectsState.value.equalizer
        if (presetIndex < 0 || presetIndex >= currentEq.presets.size) return

        runCatching {
            nativeEqualizer?.usePreset(presetIndex.toShort())
        }.onFailure { Log.e(TAG, "Failed to use preset $presetIndex: ${it.message}") }

        // Read updated levels from hardware
        val updatedBands = currentEq.bands.map { band ->
            val hwLevel = runCatching {
                nativeEqualizer?.getBandLevel(band.index.toShort())?.toInt()
            }.getOrNull() ?: band.levelMilliBels
            band.copy(levelMilliBels = hwLevel)
        }

        _effectsState.update { current ->
            current.copy(
                equalizer = current.equalizer.copy(
                    bands = updatedBands,
                    currentPresetIndex = presetIndex
                )
            )
        }

        scope.launch {
            val levelMap = updatedBands.associate { it.index to it.levelMilliBels }
            preferencesRepository.setEqualizerBandLevels(levelMap, presetIndex)
        }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        runCatching {
            nativeBassBoost?.enabled = enabled
        }.onFailure { Log.e(TAG, "Failed to toggle bass boost: ${it.message}") }

        _effectsState.update { current ->
            current.copy(bassBoost = current.bassBoost.copy(isEnabled = enabled))
        }
        scope.launch { preferencesRepository.setBassBoostEnabled(enabled) }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = BassBoostState.clampStrength(strength)
        runCatching {
            nativeBassBoost?.setStrength(clamped.toShort())
        }.onFailure { Log.e(TAG, "Failed to set bass boost strength: ${it.message}") }

        _effectsState.update { current ->
            current.copy(bassBoost = current.bassBoost.copy(strength = clamped))
        }
        scope.launch { preferencesRepository.setBassBoostStrength(clamped) }
    }

    fun setVirtualizerEnabled(enabled: Boolean) {
        runCatching {
            nativeVirtualizer?.enabled = enabled
        }.onFailure { Log.e(TAG, "Failed to toggle virtualizer: ${it.message}") }

        _effectsState.update { current ->
            current.copy(virtualizer = current.virtualizer.copy(isEnabled = enabled))
        }
        scope.launch { preferencesRepository.setVirtualizerEnabled(enabled) }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = VirtualizerState.clampStrength(strength)
        runCatching {
            nativeVirtualizer?.setStrength(clamped.toShort())
        }.onFailure { Log.e(TAG, "Failed to set virtualizer strength: ${it.message}") }

        _effectsState.update { current ->
            current.copy(virtualizer = current.virtualizer.copy(strength = clamped))
        }
        scope.launch { preferencesRepository.setVirtualizerStrength(clamped) }
    }

    fun release() {
        releaseEffects()
        currentAudioSessionId = 0
        _effectsState.update { it.copy(activeAudioSessionId = 0) }
    }

    private fun releaseEffects() {
        runCatching {
            nativeEqualizer?.release()
        }
        nativeEqualizer = null

        runCatching {
            nativeBassBoost?.release()
        }
        nativeBassBoost = null

        runCatching {
            nativeVirtualizer?.release()
        }
        nativeVirtualizer = null
    }

    companion object {
        private const val TAG = "AudioEffectsManager"
    }
}
