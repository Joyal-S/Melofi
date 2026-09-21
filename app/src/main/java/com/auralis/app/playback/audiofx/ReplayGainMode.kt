package com.auralis.app.playback.audiofx

/**
 * ReplayGain loudness normalization playback modes.
 */
enum class ReplayGainMode {
    /** Loudness normalization disabled (unity gain 1.0x). */
    OFF,

    /** Normalizes each track independently to reference loudness (-18 LUFS / 89 dB SPL). */
    TRACK,

    /** Preserves relative loudness differences between tracks from the same album. */
    ALBUM
}
