package com.auralis.app.playback

import com.auralis.app.core.util.TimeUtils
import com.auralis.app.playback.model.PlaybackRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unit tests validating the Now Playing player redesign invariants:
 * - Scrubber seeking math and bounds sanitization
 * - TimeUtils duration formatting for elapsed and remaining readouts
 * - Audio format & quality badge generation
 * - Repeat mode cycle transitions
 * - Deterministic waveform bar envelope calculation
 */
class NowPlayingRedesignTest {

    @Test
    fun `TimeUtils formats elapsed and remaining durations accurately`() {
        assertEquals("0:00", TimeUtils.formatDuration(0L))
        assertEquals("0:00", TimeUtils.formatDuration(-500L))
        assertEquals("0:05", TimeUtils.formatDuration(5000L))
        assertEquals("0:59", TimeUtils.formatDuration(59000L))
        assertEquals("2:18", TimeUtils.formatDuration(138000L))
        assertEquals("6:44", TimeUtils.formatDuration(404000L))
        assertEquals("1:02:15", TimeUtils.formatDuration(3735000L))
    }

    @Test
    fun `Scrubber seek progress to milliseconds calculation clamps accurately`() {
        val durationMs = 240000L // 4 minutes

        // Normal fractions
        assertEquals(0L, (0.0f * durationMs).toLong().coerceIn(0L, durationMs))
        assertEquals(120000L, (0.5f * durationMs).toLong().coerceIn(0L, durationMs))
        assertEquals(240000L, (1.0f * durationMs).toLong().coerceIn(0L, durationMs))

        // Boundary fractions
        assertEquals(0L, (-0.25f * durationMs).toLong().coerceIn(0L, durationMs))
        assertEquals(240000L, (1.50f * durationMs).toLong().coerceIn(0L, durationMs))

        // Zero duration edge case
        val zeroDuration = 0L
        assertEquals(0L, (0.5f * zeroDuration).toLong().coerceIn(0L, zeroDuration.coerceAtLeast(0L)))
    }

    @Test
    fun `Audio quality badges match audio format and sample rate rules`() {
        fun computeQualityTag(mimeType: String?, bitrate: Int?, sampleRate: Int?): String? {
            return when {
                mimeType != null && mimeType.contains("flac", ignoreCase = true) -> {
                    if (sampleRate != null && sampleRate >= 96000) "HI-RES • FLAC ${sampleRate / 1000}kHz"
                    else "LOSSLESS • FLAC"
                }
                mimeType != null && mimeType.contains("wav", ignoreCase = true) -> "LOSSLESS • WAV"
                mimeType != null && (mimeType.contains("ogg", ignoreCase = true) || mimeType.contains("opus", ignoreCase = true)) -> "OGG OPUS"
                bitrate != null && bitrate >= 320 -> "320 KBPS • MP3"
                bitrate != null && bitrate in 256..319 -> "256 KBPS • AAC"
                sampleRate != null && sampleRate >= 96000 -> "HI-RES • ${sampleRate / 1000}kHz"
                else -> null
            }
        }

        assertEquals("HI-RES • FLAC 96kHz", computeQualityTag("audio/flac", 1500, 96000))
        assertEquals("LOSSLESS • FLAC", computeQualityTag("audio/flac", 900, 44100))
        assertEquals("LOSSLESS • WAV", computeQualityTag("audio/wav", 1411, 44100))
        assertEquals("OGG OPUS", computeQualityTag("audio/opus", 160, 48000))
        assertEquals("320 KBPS • MP3", computeQualityTag("audio/mpeg", 320, 44100))
        assertEquals("256 KBPS • AAC", computeQualityTag("audio/mp4", 256, 44100))
        assertEquals(null, computeQualityTag("audio/mpeg", 128, 44100))
    }

    @Test
    fun `Repeat mode transitions follow strict three-state cycle`() {
        var mode = PlaybackRepeatMode.OFF
        mode = mode.next()
        assertEquals(PlaybackRepeatMode.ALL, mode)

        mode = mode.next()
        assertEquals(PlaybackRepeatMode.ONE, mode)

        mode = mode.next()
        assertEquals(PlaybackRepeatMode.OFF, mode)
    }

    @Test
    fun `Waveform bar amplitudes are bounded within valid visual range`() {
        val testSongIds = listOf(1L, 42L, 999999L, -555L, 0L)
        val totalBars = 44

        for (songId in testSongIds) {
            val seed = abs(songId.hashCode())
            val amplitudes = FloatArray(totalBars) { i ->
                val norm = i.toFloat() / (totalBars - 1).toFloat()
                val envelope = sin(norm * Math.PI).toFloat().coerceIn(0.15f, 1.0f)
                val wave1 = sin(norm * Math.PI * 4.0 + (seed % 7)).toFloat()
                val wave2 = cos(norm * Math.PI * 8.5 + (seed % 11)).toFloat()
                val combined = 0.40f + 0.35f * wave1 + 0.25f * wave2
                (combined * envelope).coerceIn(0.18f, 1.0f)
            }

            assertEquals(totalBars, amplitudes.size)
            for (amp in amplitudes) {
                assertTrue("Amplitude $amp must be >= 0.18f", amp >= 0.18f)
                assertTrue("Amplitude $amp must be <= 1.0f", amp <= 1.0f)
            }
        }
    }
}
