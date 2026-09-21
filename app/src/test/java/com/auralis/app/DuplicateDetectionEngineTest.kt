package com.auralis.app

import com.auralis.app.domain.model.DuplicateConfidence
import com.auralis.app.domain.model.DuplicateDetectionEngine
import com.auralis.app.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying algorithmic duplicate detection, confidence tiers,
 * and string normalization.
 */
class DuplicateDetectionEngineTest {

    private fun createSong(
        id: Long,
        title: String,
        artist: String,
        duration: Long,
        size: Long,
        mimeType: String = "audio/mpeg"
    ): Song {
        return Song(
            id = id,
            title = title,
            artistId = 1L,
            artistName = artist,
            albumId = 1L,
            albumName = "Album",
            genreId = null,
            genreName = null,
            duration = duration,
            trackNumber = 1,
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = mimeType,
            size = size,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 1000L,
            albumArtUri = null
        )
    }

    @Test
    fun normalizeTitle_removesBracketsAndNoise() {
        assertEquals("blinding lights", DuplicateDetectionEngine.normalizeTitle("Blinding Lights (Official Music Video)"))
        assertEquals("hotel california", DuplicateDetectionEngine.normalizeTitle("Hotel California [Remastered 2013]"))
        assertEquals("stay", DuplicateDetectionEngine.normalizeTitle("Stay (feat. Justin Bieber)"))
    }

    @Test
    fun normalizeArtist_removesFeatCollaborations() {
        assertEquals("the weeknd", DuplicateDetectionEngine.normalizeArtist("The Weeknd feat. Daft Punk"))
        assertEquals("post malone", DuplicateDetectionEngine.normalizeArtist("Post Malone with Swae Lee"))
    }

    @Test
    fun evaluateMatch_identicalSizeAndDuration_returnsExactMatch() {
        val song1 = createSong(1L, "Starboy", "The Weeknd", 230000L, 5500000L)
        val song2 = createSong(2L, "Starboy (Official Audio)", "The Weeknd", 230000L, 5500000L)

        val confidence = DuplicateDetectionEngine.evaluateMatch(song1, song2)
        assertEquals(DuplicateConfidence.EXACT, confidence)
    }

    @Test
    fun evaluateMatch_sameDurationDifferentSize_returnsVeryLikely() {
        val song1 = createSong(1L, "Starboy", "The Weeknd", 230000L, 5500000L)
        val song2 = createSong(2L, "Starboy", "The Weeknd", 231000L, 25000000L, mimeType = "audio/flac")

        val confidence = DuplicateDetectionEngine.evaluateMatch(song1, song2)
        assertEquals(DuplicateConfidence.VERY_LIKELY, confidence)
    }

    @Test
    fun evaluateMatch_slightDurationVariance_returnsPossibleMatch() {
        val song1 = createSong(1L, "Starboy", "The Weeknd", 230000L, 5500000L)
        val song2 = createSong(2L, "Starboy", "The Weeknd", 235000L, 5700000L) // 5s delta (e.g. edit)

        val confidence = DuplicateDetectionEngine.evaluateMatch(song1, song2)
        assertEquals(DuplicateConfidence.POSSIBLE, confidence)
    }

    @Test
    fun evaluateMatch_completelyDifferentSong_returnsNull() {
        val song1 = createSong(1L, "Starboy", "The Weeknd", 230000L, 5500000L)
        val song2 = createSong(2L, "Blinding Lights", "The Weeknd", 200000L, 5000000L)

        val confidence = DuplicateDetectionEngine.evaluateMatch(song1, song2)
        assertNull(confidence)
    }

    @Test
    fun findDuplicates_groupedAcrossLibrary_returnsCategorizedGroups() {
        val library = listOf(
            createSong(1L, "Blinding Lights", "The Weeknd", 200000L, 5000000L),
            createSong(2L, "Blinding Lights (Remastered)", "The Weeknd", 200000L, 5000000L), // Exact
            createSong(3L, "Shape of You", "Ed Sheeran", 233000L, 6000000L),
            createSong(4L, "Shape of You (Official Video)", "Ed Sheeran", 234000L, 24000000L), // Very likely
            createSong(5L, "Levitating", "Dua Lipa", 203000L, 5100000L) // Unique
        )

        val duplicates = DuplicateDetectionEngine.findDuplicates(library)

        assertEquals(2, duplicates.size)

        val blindingLightsGroup = duplicates.find { it.title.contains("Blinding", ignoreCase = true) }
        assertNotNull(blindingLightsGroup)
        assertEquals(2, blindingLightsGroup!!.songs.size)
        assertEquals(DuplicateConfidence.EXACT, blindingLightsGroup.confidence)

        val shapeOfYouGroup = duplicates.find { it.title.contains("Shape", ignoreCase = true) }
        assertNotNull(shapeOfYouGroup)
        assertEquals(2, shapeOfYouGroup!!.songs.size)
        assertEquals(DuplicateConfidence.VERY_LIKELY, shapeOfYouGroup.confidence)
    }

    @Test
    fun findDuplicates_emptyAndSingleSong_returnsEmptyList() {
        assertTrue(DuplicateDetectionEngine.findDuplicates(emptyList()).isEmpty())
        val single = listOf(createSong(1L, "Alone", "Artist", 180000L, 4000000L))
        assertTrue(DuplicateDetectionEngine.findDuplicates(single).isEmpty())
    }
}
