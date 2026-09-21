package com.auralis.app.playback.audiofx

import java.util.Locale

/**
 * Represents a single frequency band of the hardware Equalizer.
 *
 * @property index 0-based band index.
 * @property centerFreqMilliHertz Center frequency in milliHertz (e.g. 60000 mHz = 60 Hz).
 * @property levelMilliBels Band gain in milliBels (e.g. 300 mB = +3.0 dB).
 */
data class EqualizerBand(
    val index: Int,
    val centerFreqMilliHertz: Int,
    val levelMilliBels: Int
) {
    val centerFreqFormatted: String
        get() = EqualizerState.formatFrequency(centerFreqMilliHertz)

    val levelFormatted: String
        get() = EqualizerState.formatMilliBels(levelMilliBels)
}

/**
 * Predefined hardware equalizer preset (e.g., "Rock", "Pop", "Classical").
 */
data class EqualizerPreset(
    val index: Int,
    val name: String
)

/**
 * Immutable state of the Equalizer audio effect.
 */
data class EqualizerState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val minLevelMilliBels: Int = -1500,
    val maxLevelMilliBels: Int = 1500,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<EqualizerPreset> = emptyList(),
    val currentPresetIndex: Int = PRESET_CUSTOM
) {
    companion object {
        const val PRESET_CUSTOM = -1

        fun clampLevel(levelMilliBels: Int, min: Int, max: Int): Int {
            val lower = minOf(min, max)
            val upper = maxOf(min, max)
            return levelMilliBels.coerceIn(lower, upper)
        }

        fun formatFrequency(milliHertz: Int): String {
            if (milliHertz <= 0) return "0 Hz"
            return if (milliHertz < 1_000_000) {
                "${milliHertz / 1000} Hz"
            } else {
                val kHz = milliHertz / 1_000_000.0
                if (milliHertz % 1_000_000 == 0) {
                    "${milliHertz / 1_000_000} kHz"
                } else {
                    String.format(Locale.US, "%.1f kHz", kHz)
                }
            }
        }

        fun formatMilliBels(milliBels: Int): String {
            val db = milliBels / 100.0
            return when {
                milliBels > 0 -> {
                    val formatted = String.format(Locale.US, "+%.1f dB", db)
                    if (formatted.endsWith(".0 dB")) formatted.replace(".0 dB", " dB") else formatted
                }
                milliBels == 0 -> "0 dB"
                else -> {
                    val formatted = String.format(Locale.US, "%.1f dB", db)
                    if (formatted.endsWith(".0 dB")) formatted.replace(".0 dB", " dB") else formatted
                }
            }
        }
    }
}
