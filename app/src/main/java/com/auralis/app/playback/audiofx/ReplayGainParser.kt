package com.auralis.app.playback.audiofx

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.util.Locale
import kotlin.math.pow

/**
 * Pure parsing and gain computation engine for ReplayGain audio normalization.
 *
 * Implements the standard ReplayGain 1.0/2.0 specifications with peak-based anti-clipping protection.
 */
object ReplayGainParser {

    private const val TAG_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
    private const val TAG_TRACK_PEAK = "REPLAYGAIN_TRACK_PEAK"
    private const val TAG_ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
    private const val TAG_ALBUM_PEAK = "REPLAYGAIN_ALBUM_PEAK"

    /**
     * Extracts ReplayGain tags from Media3 container [Metadata].
     */
    fun extractFromMetadata(metadata: Metadata): ReplayGainMetadata {
        var trackGain: Float? = null
        var trackPeak: Float? = null
        var albumGain: Float? = null
        var albumPeak: Float? = null

        for (i in 0 until metadata.length()) {
            when (val entry = metadata.get(i)) {
                is TextInformationFrame -> {
                    val key = entry.description?.uppercase(Locale.US) ?: continue
                    val value = entry.values.firstOrNull() ?: continue
                    when (key) {
                        TAG_TRACK_GAIN -> trackGain = parseGainString(value) ?: trackGain
                        TAG_TRACK_PEAK -> trackPeak = parsePeakString(value) ?: trackPeak
                        TAG_ALBUM_GAIN -> albumGain = parseGainString(value) ?: albumGain
                        TAG_ALBUM_PEAK -> albumPeak = parsePeakString(value) ?: albumPeak
                    }
                }
                is VorbisComment -> {
                    val key = entry.key?.uppercase(Locale.US) ?: continue
                    val value = entry.value ?: continue
                    when (key) {
                        TAG_TRACK_GAIN -> trackGain = parseGainString(value) ?: trackGain
                        TAG_TRACK_PEAK -> trackPeak = parsePeakString(value) ?: trackPeak
                        TAG_ALBUM_GAIN -> albumGain = parseGainString(value) ?: albumGain
                        TAG_ALBUM_PEAK -> albumPeak = parsePeakString(value) ?: albumPeak
                    }
                }
            }
        }

        return ReplayGainMetadata(
            trackGainDb = trackGain,
            trackPeak = trackPeak,
            albumGainDb = albumGain,
            albumPeak = albumPeak
        )
    }

    /**
     * Parses a gain value string like "-6.20 dB", "+1.5 dB", or "-3.5" into a [Float] in decibels.
     */
    fun parseGainString(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .replace("dB", "", ignoreCase = true)
            .replace("db", "", ignoreCase = true)
            .trim()
        return cleaned.toFloatOrNull()?.takeIf { it.isFinite() && it in -100f..100f }
    }

    /**
     * Parses a peak value string like "0.9856" into a normalized [Float].
     */
    fun parsePeakString(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().toFloatOrNull()?.takeIf { it.isFinite() && it >= 0f && it <= 100f }
    }

    /**
     * Computes the linear volume multiplier for ExoPlayer (`0.0f` to `1.0f`).
     *
     * Formula:
     * multiplier = 10^(gainDb / 20)
     *
     * Anti-clipping:
     * if (multiplier * peak > 1.0f) multiplier = 1.0f / peak
     *
     * Clamped to [0.0f, 1.0f] to prevent exceeding standard Android AudioTrack maximum gain.
     */
    fun calculateVolumeMultiplier(
        metadata: ReplayGainMetadata?,
        mode: ReplayGainMode
    ): Float {
        if (mode == ReplayGainMode.OFF || metadata == null || !metadata.hasMetadata) {
            return 1.0f
        }

        val gainDb = when (mode) {
            ReplayGainMode.TRACK -> metadata.trackGainDb ?: return 1.0f
            ReplayGainMode.ALBUM -> metadata.albumGainDb ?: metadata.trackGainDb ?: return 1.0f
            ReplayGainMode.OFF -> return 1.0f
        }

        if (!gainDb.isFinite()) return 1.0f

        val peak = when (mode) {
            ReplayGainMode.TRACK -> metadata.trackPeak
            ReplayGainMode.ALBUM -> metadata.albumPeak ?: metadata.trackPeak
            ReplayGainMode.OFF -> null
        }

        var linearMultiplier = 10.0.pow(gainDb.toDouble() / 20.0).toFloat()
        if (!linearMultiplier.isFinite()) return 1.0f

        // Anti-clipping protection
        if (peak != null && peak.isFinite() && peak > 0f) {
            if (linearMultiplier * peak > 1.0f) {
                linearMultiplier = 1.0f / peak
            }
        }

        if (!linearMultiplier.isFinite()) return 1.0f

        return linearMultiplier.coerceIn(0.0f, 1.0f)
    }
}
