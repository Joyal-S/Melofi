package com.auralis.app

import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.data.local.scanner.SafAudioScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying SAF audio scanner logic:
 * - Supported audio container and mime type detection (MP3, AAC, M4A, FLAC, WAV, OGG, Opus, ALAC)
 * - Deterministic, non-colliding positive ID generation from document tree URIs
 * - Deduplication against existing library songs
 */
class SafAudioScannerTest {

    @Test
    fun supportedAudioExtensions_recognizesAllRequiredFormats() {
        val requiredExtensions = listOf("mp3", "aac", "m4a", "flac", "wav", "ogg", "opus", "alac")
        for (ext in requiredExtensions) {
            assertTrue("Extension $ext must be supported", ext in SafAudioScanner.SUPPORTED_EXTENSIONS)
            assertTrue("Filename song.$ext must be recognized", SafAudioScanner.isSupportedAudio("song.$ext", null))
            assertTrue("Filename uppercase SONG.${ext.uppercase()} must be recognized", SafAudioScanner.isSupportedAudio("SONG.${ext.uppercase()}", null))
        }
    }

    @Test
    fun supportedAudio_rejectsUnsupportedFiles() {
        val nonAudioFiles = listOf("document.pdf", "movie.mp4", "photo.jpg", "notes.txt", "program.exe", "archive.zip")
        for (file in nonAudioFiles) {
            assertFalse("File $file must not be recognized as supported audio", SafAudioScanner.isSupportedAudio(file, null))
        }
    }

    @Test
    fun supportedAudio_acceptsAudioMimeTypesEvenWithoutExtension() {
        assertTrue(SafAudioScanner.isSupportedAudio("stream", "audio/mpeg"))
        assertTrue(SafAudioScanner.isSupportedAudio("stream", "audio/flac"))
        assertTrue(SafAudioScanner.isSupportedAudio("stream", "audio/ogg"))
        assertTrue(SafAudioScanner.isSupportedAudio("stream", "application/ogg"))
    }

    @Test
    fun generateSafSongId_producesDeterministicPositiveHighRangeIds() {
        val uri1 = "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2Ftrack1.mp3"
        val uri2 = "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2Ftrack2.flac"

        val id1 = SafAudioScanner.generateSafSongId(uri1)
        val id1Repeat = SafAudioScanner.generateSafSongId(uri1)
        val id2 = SafAudioScanner.generateSafSongId(uri2)

        // Must be positive
        assertTrue("SAF ID must be positive", id1 > 0)
        assertTrue("SAF ID must be positive", id2 > 0)

        // Must be deterministic
        assertEquals("SAF ID must be deterministic across scans", id1, id1Repeat)

        // Must be distinct for different URIs
        assertNotEquals("Different URIs must yield different IDs", id1, id2)

        // High partition bit set
        assertTrue("SAF ID must be partitioned into high space (>= 2^60)", id1 >= (1L shl 60))
    }

    @Test
    fun isDuplicateOfExisting_detectsMatchingContentUri() {
        val existing = listOf(
            createTestSongEntity(id = 100L, title = "Song A", artist = "Artist A", uri = "content://media/external/audio/100", size = 5000000L, duration = 200000L)
        )
        val candidate = createTestSongEntity(id = 999L, title = "Different Title", artist = "Different Artist", uri = "content://media/external/audio/100", size = 999L, duration = 999L)

        assertTrue(SafAudioScanner.isDuplicateOfExisting(candidate, existing))
    }

    @Test
    fun isDuplicateOfExisting_detectsMatchingMetadataAndDuration() {
        val existing = listOf(
            createTestSongEntity(id = 100L, title = "Midnight City", artist = "M83", uri = "content://media/1", size = 12000000L, duration = 243000L)
        )
        // Same title and artist, duration within 1s tolerance, different URI/id
        val candidate = createTestSongEntity(id = 200L, title = "midnight city", artist = "m83", uri = "content://saf/doc/2", size = 12050000L, duration = 243500L)

        assertTrue(SafAudioScanner.isDuplicateOfExisting(candidate, existing))
    }

    @Test
    fun isDuplicateOfExisting_detectsMatchingTitleAndSize() {
        val existing = listOf(
            createTestSongEntity(id = 100L, title = "Track 01", artist = "Unknown", uri = "content://media/1", size = 8500123L, duration = 180000L)
        )
        val candidate = createTestSongEntity(id = 200L, title = "track 01", artist = "Various Artists", uri = "content://saf/doc/2", size = 8500123L, duration = 250000L)

        assertTrue(SafAudioScanner.isDuplicateOfExisting(candidate, existing))
    }

    @Test
    fun isDuplicateOfExisting_returnsFalseForDifferentSong() {
        val existing = listOf(
            createTestSongEntity(id = 100L, title = "Song 1", artist = "Artist A", uri = "content://media/1", size = 4000000L, duration = 180000L)
        )
        val candidate = createTestSongEntity(id = 200L, title = "Song 2", artist = "Artist B", uri = "content://saf/doc/2", size = 7000000L, duration = 210000L)

        assertFalse(SafAudioScanner.isDuplicateOfExisting(candidate, existing))
    }

    private fun createTestSongEntity(
        id: Long,
        title: String,
        artist: String,
        uri: String,
        size: Long,
        duration: Long
    ): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artistId = 1L,
            artistName = artist,
            albumId = 1L,
            albumName = "Test Album",
            genreId = null,
            genreName = null,
            duration = duration,
            trackNumber = 1,
            discNumber = 1,
            year = 2024,
            filePath = null,
            contentUri = uri,
            mimeType = "audio/mpeg",
            size = size,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 2000L,
            albumArtUri = null
        )
    }
}
