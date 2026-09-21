package com.auralis.app

import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.auralis.app.playback.audiofx.ReplayGainMetadata
import com.auralis.app.playback.audiofx.ReplayGainParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ReplayGain metadata extraction, string parsing, and tag formatting.
 */
class ReplayGainParserTest {

    @Test
    fun parseGainString_validNegativeWithDb_parsedCorrectly() {
        val result = ReplayGainParser.parseGainString("-6.20 dB")
        assertNotNull(result)
        assertEquals(-6.20f, result!!, 0.001f)
    }

    @Test
    fun parseGainString_validPositiveWithDb_parsedCorrectly() {
        val result = ReplayGainParser.parseGainString("+2.50 dB")
        assertNotNull(result)
        assertEquals(2.50f, result!!, 0.001f)
    }

    @Test
    fun parseGainString_withoutDbSuffix_parsedCorrectly() {
        val result = ReplayGainParser.parseGainString("-3.15")
        assertNotNull(result)
        assertEquals(-3.15f, result!!, 0.001f)
    }

    @Test
    fun parseGainString_zeroDb_returnsZero() {
        val result = ReplayGainParser.parseGainString("0.0 dB")
        assertNotNull(result)
        assertEquals(0.0f, result!!, 0.001f)
    }

    @Test
    fun parseGainString_invalidOrBlank_returnsNull() {
        assertNull(ReplayGainParser.parseGainString(null))
        assertNull(ReplayGainParser.parseGainString(""))
        assertNull(ReplayGainParser.parseGainString("   "))
        assertNull(ReplayGainParser.parseGainString("invalid"))
        assertNull(ReplayGainParser.parseGainString("dB"))
    }

    @Test
    fun parsePeakString_validDecimal_parsedCorrectly() {
        val result = ReplayGainParser.parsePeakString("0.985672")
        assertNotNull(result)
        assertEquals(0.985672f, result!!, 0.0001f)
    }

    @Test
    fun parsePeakString_unityPeak_parsedCorrectly() {
        val result = ReplayGainParser.parsePeakString("1.0")
        assertNotNull(result)
        assertEquals(1.0f, result!!, 0.0001f)
    }

    @Test
    fun parsePeakString_negativeOrInvalid_returnsNull() {
        assertNull(ReplayGainParser.parsePeakString("-0.5"))
        assertNull(ReplayGainParser.parsePeakString("abc"))
        assertNull(ReplayGainParser.parsePeakString(null))
    }

    @Test
    fun metadataSummary_formatsAccurately() {
        val full = ReplayGainMetadata(trackGainDb = -6.2f, albumGainDb = -4.5f)
        assertTrue(full.hasMetadata)
        assertTrue(full.displaySummary.contains("Track: -6.2 dB"))
        assertTrue(full.displaySummary.contains("Album: -4.5 dB"))

        val trackOnly = ReplayGainMetadata(trackGainDb = -3.0f)
        assertTrue(trackOnly.hasMetadata)
        assertEquals("Track: -3.0 dB", trackOnly.displaySummary)

        val albumOnly = ReplayGainMetadata(albumGainDb = -1.5f)
        assertTrue(albumOnly.hasMetadata)
        assertEquals("Album: -1.5 dB", albumOnly.displaySummary)

        val empty = ReplayGainMetadata()
        assertFalse(empty.hasMetadata)
        assertEquals("No ReplayGain tags found", empty.displaySummary)
    }

    @Test
    fun extractFromMetadata_textInformationFrames_parsesAllFields() {
        val metadata = Metadata(
            TextInformationFrame("TXXX", "REPLAYGAIN_TRACK_GAIN", listOf("-6.20 dB")),
            TextInformationFrame("TXXX", "REPLAYGAIN_TRACK_PEAK", listOf("0.9850")),
            TextInformationFrame("TXXX", "REPLAYGAIN_ALBUM_GAIN", listOf("-5.10 dB")),
            TextInformationFrame("TXXX", "REPLAYGAIN_ALBUM_PEAK", listOf("0.9920"))
        )

        val extracted = ReplayGainParser.extractFromMetadata(metadata)
        assertEquals(-6.20f, extracted.trackGainDb!!, 0.001f)
        assertEquals(0.9850f, extracted.trackPeak!!, 0.001f)
        assertEquals(-5.10f, extracted.albumGainDb!!, 0.001f)
        assertEquals(0.9920f, extracted.albumPeak!!, 0.001f)
    }

    @Test
    fun extractFromMetadata_vorbisComments_parsesAllFields() {
        val metadata = Metadata(
            VorbisComment("replaygain_track_gain", "-4.8 dB"),
            VorbisComment("replaygain_track_peak", "0.950"),
            VorbisComment("replaygain_album_gain", "-3.9 dB"),
            VorbisComment("replaygain_album_peak", "0.975")
        )

        val extracted = ReplayGainParser.extractFromMetadata(metadata)
        assertEquals(-4.8f, extracted.trackGainDb!!, 0.001f)
        assertEquals(0.950f, extracted.trackPeak!!, 0.001f)
        assertEquals(-3.9f, extracted.albumGainDb!!, 0.001f)
        assertEquals(0.975f, extracted.albumPeak!!, 0.001f)
    }
}
