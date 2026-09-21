package com.auralis.app

import com.auralis.app.domain.model.AudioFormatClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying factual audio format classification, resolution detection,
 * and display formatting without marketing enhancement claims.
 */
class SongMetadataInspectionTest {

    @Test
    fun isLossless_flacAndWav_returnsTrue() {
        assertTrue(AudioFormatClassifier.isLossless("audio/flac", "content://media/1"))
        assertTrue(AudioFormatClassifier.isLossless("audio/x-wav", "track.wav"))
        assertTrue(AudioFormatClassifier.isLossless("audio/alac", "track.alac"))
    }

    @Test
    fun isLossless_mp3AndAac_returnsFalse() {
        assertFalse(AudioFormatClassifier.isLossless("audio/mpeg", "track.mp3"))
        assertFalse(AudioFormatClassifier.isLossless("audio/aac", "track.m4a"))
        assertFalse(AudioFormatClassifier.isLossless("audio/ogg", "track.ogg"))
    }

    @Test
    fun getFormatName_resolvesCorrectAcronym() {
        assertEquals("FLAC", AudioFormatClassifier.getFormatName("audio/flac", "song.flac"))
        assertEquals("MP3", AudioFormatClassifier.getFormatName("audio/mpeg", "song.mp3"))
        assertEquals("WAV", AudioFormatClassifier.getFormatName("audio/x-wav", "song.wav"))
        assertEquals("AAC", AudioFormatClassifier.getFormatName("audio/aac", "song.m4a"))
    }

    @Test
    fun formatBitrate_validAndNull_formattedCorrectly() {
        assertEquals("320 kbps", AudioFormatClassifier.formatBitrate(320))
        assertEquals("1,411 kbps", AudioFormatClassifier.formatBitrate(1411))
        assertEquals("Unknown", AudioFormatClassifier.formatBitrate(null))
        assertEquals("Unknown", AudioFormatClassifier.formatBitrate(0))
    }

    @Test
    fun formatSampleRate_validAndNull_formattedCorrectly() {
        assertEquals("44.1 kHz", AudioFormatClassifier.formatSampleRate(44100))
        assertEquals("48.0 kHz", AudioFormatClassifier.formatSampleRate(48000))
        assertEquals("96.0 kHz", AudioFormatClassifier.formatSampleRate(96000))
        assertEquals("Unknown", AudioFormatClassifier.formatSampleRate(null))
    }

    @Test
    fun formatBitDepth_validAndNull_formattedCorrectly() {
        assertEquals("16-bit", AudioFormatClassifier.formatBitDepth(16))
        assertEquals("24-bit", AudioFormatClassifier.formatBitDepth(24))
        assertEquals("Unknown", AudioFormatClassifier.formatBitDepth(null))
    }

    @Test
    fun formatChannels_standardConfigs_returnsCleanDescription() {
        assertEquals("Mono (1 ch)", AudioFormatClassifier.formatChannels(1))
        assertEquals("Stereo (2 ch)", AudioFormatClassifier.formatChannels(2))
        assertEquals("5.1 Surround (6 ch)", AudioFormatClassifier.formatChannels(6))
        assertEquals("Unknown", AudioFormatClassifier.formatChannels(null))
    }

    @Test
    fun formatFileSize_megabytesAndKilobytes_formattedCorrectly() {
        assertEquals("8.4 MB", AudioFormatClassifier.formatFileSize((8.4 * 1024 * 1024).toLong()))
        assertEquals("500.0 KB", AudioFormatClassifier.formatFileSize((500 * 1024).toLong()))
        assertEquals("0 B", AudioFormatClassifier.formatFileSize(0))
    }
}
