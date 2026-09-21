package com.auralis.app

import com.auralis.app.domain.model.MetadataEditValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying metadata edit validation and format capability checks.
 */
class MetadataEditValidatorTest {

    @Test
    fun validate_emptyTitle_returnsInvalid() {
        val result = MetadataEditValidator.validate(
            title = "   ",
            artist = "Artist",
            album = "Album",
            genre = null,
            yearText = "2024",
            trackNumberText = "1",
            discNumberText = "1"
        )
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun validate_blankArtistAndAlbum_substitutesCleanDefaults() {
        val result = MetadataEditValidator.validate(
            title = "Valid Title",
            artist = "   ",
            album = "",
            genre = "Rock",
            yearText = null,
            trackNumberText = null,
            discNumberText = null
        )
        assertTrue(result.isValid)
        val sanitized = result.sanitizedMetadata!!
        assertEquals("Unknown Artist", sanitized.artist)
        assertEquals("Unknown Album", sanitized.album)
        assertEquals("Rock", sanitized.genre)
    }

    @Test
    fun validate_invalidYear_returnsInvalid() {
        val lowYearResult = MetadataEditValidator.validate(
            title = "Title",
            artist = "Artist",
            album = "Album",
            genre = null,
            yearText = "500",
            trackNumberText = null,
            discNumberText = null
        )
        assertFalse(lowYearResult.isValid)

        val textYearResult = MetadataEditValidator.validate(
            title = "Title",
            artist = "Artist",
            album = "Album",
            genre = null,
            yearText = "abcd",
            trackNumberText = null,
            discNumberText = null
        )
        assertFalse(textYearResult.isValid)
    }

    @Test
    fun validate_invalidTrackNumber_returnsInvalid() {
        val negativeTrackResult = MetadataEditValidator.validate(
            title = "Title",
            artist = "Artist",
            album = "Album",
            genre = null,
            yearText = "2024",
            trackNumberText = "-5",
            discNumberText = null
        )
        assertFalse(negativeTrackResult.isValid)

        val outOfBoundsTrack = MetadataEditValidator.validate(
            title = "Title",
            artist = "Artist",
            album = "Album",
            genre = null,
            yearText = "2024",
            trackNumberText = "1500",
            discNumberText = null
        )
        assertFalse(outOfBoundsTrack.isValid)
    }

    @Test
    fun isFormatEditable_supportedAndUnsupportedFormats_returnsCorrectCapability() {
        assertTrue(MetadataEditValidator.isFormatEditable("audio/mpeg", "song.mp3"))
        assertTrue(MetadataEditValidator.isFormatEditable("audio/flac", "song.flac"))
        assertTrue(MetadataEditValidator.isFormatEditable("audio/aac", "song.m4a"))
        assertTrue(MetadataEditValidator.isFormatEditable("audio/ogg", "song.ogg"))
        assertFalse(MetadataEditValidator.isFormatEditable("audio/x-wav", "song.wav"))
        assertFalse(MetadataEditValidator.isFormatEditable("audio/midi", "song.mid"))
    }

    @Test
    fun validate_albumArtistAndComposer_sanitizesAndPreservesValues() {
        val result = MetadataEditValidator.validate(
            title = "Symphony No. 5",
            artist = "Vienna Philharmonic",
            album = "Beethoven Masterpieces",
            albumArtist = "   Ludwig van Beethoven   ",
            genre = "Classical",
            composer = "  Ludwig van Beethoven  ",
            yearText = "2020",
            trackNumberText = "1",
            discNumberText = "1"
        )
        assertTrue(result.isValid)
        val sanitized = result.sanitizedMetadata!!
        assertEquals("Ludwig van Beethoven", sanitized.albumArtist)
        assertEquals("Ludwig van Beethoven", sanitized.composer)
    }
}
