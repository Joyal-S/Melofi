package com.auralis.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auralis.app.playback.audiofx.EqualizerState
import com.auralis.app.playback.audiofx.ReplayGainMode
import com.auralis.app.playback.dsp.DspPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "audio_preferences")

/**
 * DataStore repository managing persistent user preferences for audio effects and playback behaviors.
 */
@Singleton
class AudioPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    val preferencesFlow: Flow<AudioPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EQUALIZER_ENABLED] = enabled }
    }

    suspend fun setEqualizerPreset(preset: Int) {
        context.dataStore.edit { it[KEY_EQUALIZER_PRESET] = preset }
    }

    suspend fun setEqualizerBandLevels(levels: Map<Int, Int>, preset: Int = EqualizerState.PRESET_CUSTOM) {
        context.dataStore.edit {
            it[KEY_EQUALIZER_BAND_LEVELS] = serializeBandLevels(levels)
            it[KEY_EQUALIZER_PRESET] = preset
        }
    }

    suspend fun setBassBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BASS_BOOST_ENABLED] = enabled }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        context.dataStore.edit { it[KEY_BASS_BOOST_STRENGTH] = strength }
    }

    suspend fun setVirtualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIRTUALIZER_ENABLED] = enabled }
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        context.dataStore.edit { it[KEY_VIRTUALIZER_STRENGTH] = strength }
    }

    suspend fun setReplayGainMode(mode: ReplayGainMode) {
        context.dataStore.edit { it[KEY_REPLAY_GAIN_MODE] = mode.name }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setCrossfadeDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_CROSSFADE_DURATION] = seconds }
    }

    suspend fun setDspLoFiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DSP_LOFI_ENABLED] = enabled }
    }

    suspend fun setDspReverbEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DSP_REVERB_ENABLED] = enabled }
    }

    suspend fun setDspPreset(preset: DspPreset) {
        context.dataStore.edit { it[KEY_DSP_PRESET] = preset.name }
    }

    suspend fun setDspLoFiIntensity(intensity: Float) {
        context.dataStore.edit { it[KEY_DSP_LOFI_INTENSITY] = intensity }
    }

    suspend fun setDspLoFiBitCrush(bitCrush: Float) {
        context.dataStore.edit { it[KEY_DSP_LOFI_BIT_CRUSH] = bitCrush }
    }

    suspend fun setDspLoFiSampleReduction(sampleReduction: Float) {
        context.dataStore.edit { it[KEY_DSP_LOFI_SAMPLE_REDUCTION] = sampleReduction }
    }

    suspend fun setDspLoFiSaturation(saturation: Float) {
        context.dataStore.edit { it[KEY_DSP_LOFI_SATURATION] = saturation }
    }

    suspend fun setDspLoFiWowFlutter(wowFlutter: Float) {
        context.dataStore.edit { it[KEY_DSP_LOFI_WOW_FLUTTER] = wowFlutter }
    }

    suspend fun setDspReverbRoomSize(roomSize: Float) {
        context.dataStore.edit { it[KEY_DSP_REVERB_ROOM_SIZE] = roomSize }
    }

    suspend fun setDspReverbDecay(decay: Float) {
        context.dataStore.edit { it[KEY_DSP_REVERB_DECAY] = decay }
    }

    suspend fun setDspReverbWetLevel(wetLevel: Float) {
        context.dataStore.edit { it[KEY_DSP_REVERB_WET_LEVEL] = wetLevel }
    }

    private fun mapPreferences(preferences: Preferences): AudioPreferences {
        val eqEnabled = preferences[KEY_EQUALIZER_ENABLED] ?: false
        val eqPreset = preferences[KEY_EQUALIZER_PRESET] ?: EqualizerState.PRESET_CUSTOM
        val rawBandLevels = preferences[KEY_EQUALIZER_BAND_LEVELS]
        val bandLevels = parseBandLevels(rawBandLevels)

        val bbEnabled = preferences[KEY_BASS_BOOST_ENABLED] ?: false
        val bbStrength = preferences[KEY_BASS_BOOST_STRENGTH] ?: 0

        val virtEnabled = preferences[KEY_VIRTUALIZER_ENABLED] ?: false
        val virtStrength = preferences[KEY_VIRTUALIZER_STRENGTH] ?: 0

        val rawReplayGain = preferences[KEY_REPLAY_GAIN_MODE]
        val replayGainMode = try {
            rawReplayGain?.let { ReplayGainMode.valueOf(it) } ?: ReplayGainMode.OFF
        } catch (e: IllegalArgumentException) {
            ReplayGainMode.OFF
        }

        val crossfadeEnabled = preferences[KEY_CROSSFADE_ENABLED] ?: false
        val crossfadeDuration = preferences[KEY_CROSSFADE_DURATION] ?: 0

        val dspLoFiEnabled = preferences[KEY_DSP_LOFI_ENABLED] ?: false
        val dspReverbEnabled = preferences[KEY_DSP_REVERB_ENABLED] ?: false
        val rawDspPreset = preferences[KEY_DSP_PRESET]
        val dspPreset = DspPreset.fromString(rawDspPreset)
        val dspLoFiIntensity = preferences[KEY_DSP_LOFI_INTENSITY] ?: 0.5f
        val dspLoFiBitCrush = preferences[KEY_DSP_LOFI_BIT_CRUSH] ?: 0.0f
        val dspLoFiSampleReduction = preferences[KEY_DSP_LOFI_SAMPLE_REDUCTION] ?: 0.0f
        val dspLoFiSaturation = preferences[KEY_DSP_LOFI_SATURATION] ?: 0.2f
        val dspLoFiWowFlutter = preferences[KEY_DSP_LOFI_WOW_FLUTTER] ?: 0.0f
        val dspReverbRoomSize = preferences[KEY_DSP_REVERB_ROOM_SIZE] ?: 0.5f
        val dspReverbDecay = preferences[KEY_DSP_REVERB_DECAY] ?: 0.5f
        val dspReverbWetLevel = preferences[KEY_DSP_REVERB_WET_LEVEL] ?: 0.3f

        return AudioPreferences(
            equalizerEnabled = eqEnabled,
            equalizerPreset = eqPreset,
            equalizerBandLevels = bandLevels,
            bassBoostEnabled = bbEnabled,
            bassBoostStrength = bbStrength,
            virtualizerEnabled = virtEnabled,
            virtualizerStrength = virtStrength,
            replayGainMode = replayGainMode,
            crossfadeEnabled = crossfadeEnabled,
            crossfadeDurationSeconds = crossfadeDuration,
            dspLoFiEnabled = dspLoFiEnabled,
            dspReverbEnabled = dspReverbEnabled,
            dspPreset = dspPreset,
            dspLoFiIntensity = dspLoFiIntensity,
            dspLoFiBitCrush = dspLoFiBitCrush,
            dspLoFiSampleReduction = dspLoFiSampleReduction,
            dspLoFiSaturation = dspLoFiSaturation,
            dspLoFiWowFlutter = dspLoFiWowFlutter,
            dspReverbRoomSize = dspReverbRoomSize,
            dspReverbDecay = dspReverbDecay,
            dspReverbWetLevel = dspReverbWetLevel
        )
    }

    companion object {
        val KEY_EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val KEY_EQUALIZER_PRESET = intPreferencesKey("equalizer_preset")
        val KEY_EQUALIZER_BAND_LEVELS = stringPreferencesKey("equalizer_band_levels")
        val KEY_BASS_BOOST_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        val KEY_BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val KEY_VIRTUALIZER_ENABLED = booleanPreferencesKey("virtualizer_enabled")
        val KEY_VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val KEY_REPLAY_GAIN_MODE = stringPreferencesKey("replay_gain_mode")
        val KEY_CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val KEY_CROSSFADE_DURATION = intPreferencesKey("crossfade_duration_seconds")

        val KEY_DSP_LOFI_ENABLED = booleanPreferencesKey("dsp_lofi_enabled")
        val KEY_DSP_REVERB_ENABLED = booleanPreferencesKey("dsp_reverb_enabled")
        val KEY_DSP_PRESET = stringPreferencesKey("dsp_preset")
        val KEY_DSP_LOFI_INTENSITY = androidx.datastore.preferences.core.floatPreferencesKey("dsp_lofi_intensity")
        val KEY_DSP_LOFI_BIT_CRUSH = androidx.datastore.preferences.core.floatPreferencesKey("dsp_lofi_bit_crush")
        val KEY_DSP_LOFI_SAMPLE_REDUCTION = androidx.datastore.preferences.core.floatPreferencesKey("dsp_lofi_sample_reduction")
        val KEY_DSP_LOFI_SATURATION = androidx.datastore.preferences.core.floatPreferencesKey("dsp_lofi_saturation")
        val KEY_DSP_LOFI_WOW_FLUTTER = androidx.datastore.preferences.core.floatPreferencesKey("dsp_lofi_wow_flutter")
        val KEY_DSP_REVERB_ROOM_SIZE = androidx.datastore.preferences.core.floatPreferencesKey("dsp_reverb_room_size")
        val KEY_DSP_REVERB_DECAY = androidx.datastore.preferences.core.floatPreferencesKey("dsp_reverb_decay")
        val KEY_DSP_REVERB_WET_LEVEL = androidx.datastore.preferences.core.floatPreferencesKey("dsp_reverb_wet_level")

        fun serializeBandLevels(levels: Map<Int, Int>): String {
            return levels.entries.joinToString(",") { "${it.key}:${it.value}" }
        }

        fun parseBandLevels(raw: String?): Map<Int, Int> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(",")
                .mapNotNull { token ->
                    val parts = token.split(":")
                    if (parts.size == 2) {
                        val band = parts[0].trim().toIntOrNull()
                        val level = parts[1].trim().toIntOrNull()
                        if (band != null && level != null) band to level else null
                    } else null
                }
                .toMap()
        }
    }
}
