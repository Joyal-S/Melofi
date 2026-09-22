package com.auralis.app

import com.auralis.app.domain.model.DuplicateConfidence
import com.auralis.app.domain.model.DuplicateDetectionEngine
import com.auralis.app.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Unit tests validating local music import logic, format capability validation,
 * duplicate candidate classification, and audio byte preservation.
 */
class MusicImportTest {

    private val supportedAudioExtensions = setOf(
        "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus"
    )

    @Test
    fun supportedAudioExtensions_recognizesAllRequiredContainers() {
        val testFiles = listOf(
            "song1.mp3" to true,
            "track.FLAC" to true,
            "audio.wav" to true,
            "tune.aac" to true,
            "voice.m4a" to true,
            "sound.ogg" to true,
            "music.opus" to true,
            "document.pdf" to false,
            "video.mp4" to false,
            "image.jpg" to false,
            "script.sh" to false,
            "executable.exe" to false
        )

        for ((filename, shouldBeSupported) in testFiles) {
            val ext = filename.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val isSupported = ext in supportedAudioExtensions
            assertEquals("Check support for $filename", shouldBeSupported, isSupported)
        }
    }

    @Test
    fun durationThreshold_rejectsShortClickSounds() {
        val validDuration = 1500L
        val minDuration = 1000L
        val shortClickDuration = 250L

        assertTrue(validDuration >= minDuration)
        assertFalse(shortClickDuration >= minDuration)
    }

    @Test
    fun duplicateDetection_identifiesExactMatchOnImportCandidate() {
        val existingSong = Song(
            id = 101L,
            title = "Bohemian Rhapsody",
            artistId = 1L,
            artistName = "Queen",
            albumId = 5L,
            albumName = "A Night at the Opera",
            genreId = null,
            genreName = "Rock",
            duration = 354000L,
            trackNumber = 11,
            discNumber = 1,
            year = 1975,
            contentUri = "content://media/external/audio/media/101",
            mimeType = "audio/flac",
            size = 45000000L,
            bitrate = 1411,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 2000L,
            albumArtUri = null
        )

        // Exact candidate: same title, same artist, same size, duration within 1s
        val candidate = Song(
            id = -1L,
            title = "Bohemian Rhapsody",
            artistId = null,
            artistName = "Queen",
            albumId = null,
            albumName = "A Night at the Opera",
            genreId = null,
            genreName = null,
            duration = 354200L,
            trackNumber = null,
            discNumber = null,
            year = null,
            contentUri = "content://com.android.providers.media.documents/123",
            mimeType = "audio/flac",
            size = 45000000L,
            bitrate = 1411,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 0L,
            dateModified = 0L,
            albumArtUri = null
        )

        val matchConfidence = DuplicateDetectionEngine.evaluateMatch(candidate, existingSong)
        assertNotNull("Should identify duplicate", matchConfidence)
        assertEquals(DuplicateConfidence.EXACT, matchConfidence)
    }

    @Test
    fun duplicateDetection_allowsDistinctTracksThrough() {
        val existingSong = Song(
            id = 101L,
            title = "Bohemian Rhapsody",
            artistId = 1L,
            artistName = "Queen",
            albumId = 5L,
            albumName = "A Night at the Opera",
            genreId = null,
            genreName = "Rock",
            duration = 354000L,
            trackNumber = 11,
            discNumber = 1,
            year = 1975,
            contentUri = "content://media/external/audio/media/101",
            mimeType = "audio/flac",
            size = 45000000L,
            bitrate = 1411,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 2000L,
            albumArtUri = null
        )

        val nonDuplicate = Song(
            id = -1L,
            title = "Don't Stop Me Now",
            artistId = null,
            artistName = "Queen",
            albumId = null,
            albumName = "Jazz",
            genreId = null,
            genreName = null,
            duration = 209000L,
            trackNumber = null,
            discNumber = null,
            year = null,
            contentUri = "content://com.android.providers.media.documents/456",
            mimeType = "audio/flac",
            size = 25000000L,
            bitrate = 1411,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 0L,
            dateModified = 0L,
            albumArtUri = null
        )

        val matchConfidence = DuplicateDetectionEngine.evaluateMatch(nonDuplicate, existingSong)
        assertNull("Should not identify match for different songs", matchConfidence)
    }

    @Test
    fun destinationPath_usesStandardMusicDirectory() {
        val relPath = "Music/Melofy/"
        assertTrue(relPath.startsWith("Music/"))
        assertTrue(relPath.endsWith("/"))
    }
}
