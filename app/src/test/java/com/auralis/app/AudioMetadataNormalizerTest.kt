package com.auralis.app

import com.auralis.app.data.local.scanner.AudioMetadataNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AudioMetadataNormalizer].
 * Verifies robust null-handling, edge-case cleanup, and track/URI formatting.
 */
class AudioMetadataNormalizerTest {

    @Test
    fun normalizeTitle_validTitle_returnsTrimmedTitle() {
        val result = AudioMetadataNormalizer.normalizeTitle("  Midnight City  ", "track.mp3")
        assertEquals("Midnight City", result)
    }

    @Test
    fun normalizeTitle_nullOrUnknownTitle_fallsBackToDisplayNameWithoutExtension() {
        val result1 = AudioMetadataNormalizer.normalizeTitle(null, "Bohemian_Rhapsody.flac")
        assertEquals("Bohemian_Rhapsody", result1)

        val result2 = AudioMetadataNormalizer.normalizeTitle("<unknown>", "01 Starboy.mp3")
        assertEquals("01 Starboy", result2)

        val result3 = AudioMetadataNormalizer.normalizeTitle("", "SongTitle.wav")
        assertEquals("SongTitle", result3)
    }

    @Test
    fun normalizeTitle_nullTitleAndNullDisplayName_returnsUnknownTitle() {
        val result = AudioMetadataNormalizer.normalizeTitle(null, null)
        assertEquals(AudioMetadataNormalizer.UNKNOWN_TITLE, result)
    }

    @Test
    fun normalizeArtist_handlesNullAndUnknown() {
        assertEquals("Queen", AudioMetadataNormalizer.normalizeArtist(" Queen "))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ARTIST, AudioMetadataNormalizer.normalizeArtist(null))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ARTIST, AudioMetadataNormalizer.normalizeArtist(""))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ARTIST, AudioMetadataNormalizer.normalizeArtist("<unknown>"))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ARTIST, AudioMetadataNormalizer.normalizeArtist(" <UNKNOWN> "))
    }

    @Test
    fun normalizeAlbum_handlesNullAndUnknown() {
        assertEquals("A Night at the Opera", AudioMetadataNormalizer.normalizeAlbum(" A Night at the Opera "))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ALBUM, AudioMetadataNormalizer.normalizeAlbum(null))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ALBUM, AudioMetadataNormalizer.normalizeAlbum("   "))
        assertEquals(AudioMetadataNormalizer.UNKNOWN_ALBUM, AudioMetadataNormalizer.normalizeAlbum("<unknown>"))
    }

    @Test
    fun normalizeGenre_handlesNullAndUnknown() {
        assertEquals("Rock", AudioMetadataNormalizer.normalizeGenre(" Rock "))
        assertNull(AudioMetadataNormalizer.normalizeGenre(null))
        assertNull(AudioMetadataNormalizer.normalizeGenre(""))
        assertNull(AudioMetadataNormalizer.normalizeGenre("<unknown>"))
    }

    @Test
    fun normalizeDuration_negativeOrZero_returnsZero() {
        assertEquals(180000L, AudioMetadataNormalizer.normalizeDuration(180000L))
        assertEquals(0L, AudioMetadataNormalizer.normalizeDuration(0L))
        assertEquals(0L, AudioMetadataNormalizer.normalizeDuration(-500L))
        assertEquals(0L, AudioMetadataNormalizer.normalizeDuration(null))
    }

    @Test
    fun parseTrackAndDiscNumber_encodesDiscAndTrackCorrectly() {
        // Standard track
        val (disc1, track1) = AudioMetadataNormalizer.parseTrackAndDiscNumber(5)
        assertNull(disc1)
        assertEquals(5, track1)

        // Combined disc + track (1004 = Disc 1, Track 4)
        val (disc2, track2) = AudioMetadataNormalizer.parseTrackAndDiscNumber(1004)
        assertEquals(1, disc2)
        assertEquals(4, track2)

        // Disc 2, Track 12
        val (disc3, track3) = AudioMetadataNormalizer.parseTrackAndDiscNumber(2012)
        assertEquals(2, disc3)
        assertEquals(12, track3)

        // Invalid / null
        val (disc4, track4) = AudioMetadataNormalizer.parseTrackAndDiscNumber(null)
        assertNull(disc4)
        assertNull(track4)

        val (disc5, track5) = AudioMetadataNormalizer.parseTrackAndDiscNumber(-1)
        assertNull(disc5)
        assertNull(track5)
    }

    @Test
    fun buildContentUri_createsStableUri() {
        assertEquals(
            "content://media/external/audio/media/98765",
            AudioMetadataNormalizer.buildContentUri(98765L)
        )
    }

    @Test
    fun buildAlbumArtUri_createsStableUri() {
        assertEquals(
            "content://media/external/audio/albumart/4321",
            AudioMetadataNormalizer.buildAlbumArtUri(4321L)
        )
        assertNull(AudioMetadataNormalizer.buildAlbumArtUri(null))
        assertNull(AudioMetadataNormalizer.buildAlbumArtUri(-1L))
        assertNull(AudioMetadataNormalizer.buildAlbumArtUri(0L))
    }
}
