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
            crossfadeDurationSeconds = crossfadeDuration
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
