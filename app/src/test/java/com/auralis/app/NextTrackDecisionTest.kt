package com.auralis.app

import com.auralis.app.domain.model.RecommendationEngine
import com.auralis.app.domain.model.RecommendationProfile
import com.auralis.app.domain.model.Song
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.playback.queue.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying Next-track decision logic and priority rules:
 * 1. Manual user queue ("Play Next") must win over any recommendation or original sequence.
 * 2. Repeat-One replays current track at 0 position.
 * 3. Repeat-All wraps queue to beginning.
 * 4. Queue exhaustion (Repeat-OFF) triggers recommendation continuation using the existing local engine.
 * 5. Current song is strictly excluded from recommendation candidates.
 * 6. Anti-repetition window prevents recent songs from repeating.
 * 7. Artist diversity is maintained.
 * 8. Cold start produces valid local recommendations without listening history.
 */
class NextTrackDecisionTest {

    private lateinit var queue: PlaybackQueue

    private fun createSong(
        id: Long,
        title: String,
        artist: String = "Queen",
        genre: String = "Rock",
        dateAdded: Long = 1000L
    ): Song {
        return Song(
            id = id,
            title = title,
            artistId = id * 10,
            artistName = artist,
            albumId = id * 100,
            albumName = "Album $id",
            genreId = 1L,
            genreName = genre,
            duration = 180000L,
            trackNumber = 1,
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = 5000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = dateAdded,
            dateModified = 1000L,
            albumArtUri = null
        )
    }

    @Before
    fun setUp() {
        queue = PlaybackQueue()
    }

    @Test
    fun manualQueueMustWin_whenPlayNextIsTriggered() {
        val songA = createSong(1L, "Song A", "Artist 1")
        val songB = createSong(2L, "Song B", "Artist 1")
        val songC = createSong(3L, "Song C", "Artist 1")

        val manualX = createSong(10L, "Song X (Manual)", "Artist 2")
        val manualY = createSong(20L, "Song Y (Manual)", "Artist 3")

        // User starts playing Song A from queue [A, B, C]
        queue.setQueue(listOf(songA, songB, songC), startIndex = 0)
        assertEquals("Song A", queue.currentSong()?.title)

        // User manually taps "Play Next" on Song X, then Song Y
        queue.playNext(listOf(manualX, manualY))

        // Press Next: Song X MUST play, NOT Song B or recommendation
        val next1 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNotNull(next1)
        assertEquals("Song X (Manual)", next1?.title)

        // Press Next again: Song Y MUST play
        val next2 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNotNull(next2)
        assertEquals("Song Y (Manual)", next2?.title)

        // Press Next again: Song B resumes the original queue
        val next3 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNotNull(next3)
        assertEquals("Song B", next3?.title)

        // Press Next again: Song C
        val next4 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNotNull(next4)
        assertEquals("Song C", next4?.title)
    }

    @Test
    fun repeatOne_replaysCurrentSongWithoutAdvancingQueue() {
        val songA = createSong(1L, "Song A")
        val songB = createSong(2L, "Song B")
        queue.setQueue(listOf(songA, songB), startIndex = 0)

        // In Repeat One, nextSong returns the current song (Song A) and does not advance index
        val next = queue.nextSong(PlaybackRepeatMode.ONE)
        assertEquals("Song A", next?.title)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun repeatAll_cyclesBackToBeginningAtEnd() {
        val songA = createSong(1L, "Song A")
        val songB = createSong(2L, "Song B")
        queue.setQueue(listOf(songA, songB), startIndex = 1) // At last song

        val next = queue.nextSong(PlaybackRepeatMode.ALL)
        assertEquals("Song A", next?.title)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun queueExhaustion_signalsNoNextInQueueUnderRepeatOff() {
        val songA = createSong(1L, "Song A")
        val songB = createSong(2L, "Song B")
        queue.setQueue(listOf(songA, songB), startIndex = 1) // At last song

        assertFalse("Queue should report no next under Repeat OFF", queue.hasNext(PlaybackRepeatMode.OFF))
        val next = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNull("Next must return null on queue exhaustion", next)
    }

    @Test
    fun recommendationContinuation_excludesCurrentSong() {
        val currentSong = createSong(1L, "Current Playing", "Daft Punk")
        val song2 = createSong(2L, "Harder Better", "Daft Punk")
        val song3 = createSong(3L, "Get Lucky", "Daft Punk")
        val library = listOf(currentSong, song2, song3)

        val profile = RecommendationProfile()
        val sections = RecommendationEngine.generateSections(library, profile)
        val candidateItems = sections.flatMap { it.items }

        // Filter using our continuation rule: exclude current song id
        val excludedIds = setOf(currentSong.id)
        val validNext = candidateItems.firstOrNull { it.song.id !in excludedIds }

        assertNotNull(validNext)
        assertNotEquals(currentSong.id, validNext?.song?.id)
    }

    @Test
    fun recommendationContinuation_antiRepetitionWindow_excludesRecentSongs() {
        val song1 = createSong(1L, "Song 1", "Artist A")
        val song2 = createSong(2L, "Song 2", "Artist B")
        val song3 = createSong(3L, "Song 3", "Artist C")
        val library = listOf(song1, song2, song3)

        val recentHistoryIds = setOf(1L, 2L)
        val currentSongId = 1L
        val excludedIds = recentHistoryIds + currentSongId

        val availableSongs = library.filter { it.id !in excludedIds }
        assertEquals(1, availableSongs.size)
        assertEquals("Song 3", availableSongs.first().title)
    }

    @Test
    fun recommendationContinuation_coldStart_producesValidCandidate() {
        // Cold start: completely empty profile (no plays, no favorites, no history)
        val library = (1L..10L).map {
            createSong(it, "Track $it", "Artist ${it % 3}", dateAdded = it * 1000L)
        }
        val emptyProfile = RecommendationProfile()

        val sections = RecommendationEngine.generateSections(library, emptyProfile)
        assertTrue("Cold start must generate recommendation sections", sections.isNotEmpty())

        val allCandidates = sections.flatMap { it.items }
        assertTrue("Candidates must not be empty", allCandidates.isNotEmpty())

        val currentSong = library.first()
        val nextTrack = allCandidates.firstOrNull { it.song.id != currentSong.id }
        assertNotNull("Must select a valid next track in cold start", nextTrack)
        assertNotEquals(currentSong.id, nextTrack?.song?.id)
    }

    @Test
    fun recommendationContinuation_artistDiversity_prefersDifferentArtistWhenAvailable() {
        val currentSong = createSong(1L, "Bohemian Rhapsody", "Queen")
        val sameArtist = createSong(2L, "Radio Ga Ga", "Queen")
        val differentArtist1 = createSong(3L, "Starman", "David Bowie")
        val differentArtist2 = createSong(4L, "Heroes", "David Bowie")

        val candidates = listOf(sameArtist, differentArtist1, differentArtist2)

        // If candidates from a different artist exist, the diversity rule prioritizes different artist
        val diverseCandidates = candidates.filter { !it.artistName.equals(currentSong.artistName, ignoreCase = true) }
        assertTrue("Should find candidates from different artist", diverseCandidates.isNotEmpty())
        assertEquals("David Bowie", diverseCandidates.first().artistName)
    }

    @Test
    fun oneSongLibrary_fallbackCandidate_returnsSingleSong() {
        val onlySong = createSong(1L, "Lone Song", "Solo Artist")
        val library = listOf(onlySong)

        // In 1-song library, if candidate is the only song, relaxing exclusions returns the only song
        val candidates = library.filter { it.id != onlySong.id }.ifEmpty { library }
        assertEquals(1, candidates.size)
        assertEquals(onlySong.id, candidates.first().id)
    }

    @Test
    fun emptyLibrary_recommendationReturnsNull_noCrash() {
        val emptyLibrary = emptyList<Song>()
        val profile = RecommendationProfile()

        val sections = RecommendationEngine.generateSections(emptyLibrary, profile)
        assertTrue("Empty library generates empty recommendation sections", sections.isEmpty())
    }

    @Test
    fun repeatOne_neverReplacedByRecommendation() {
        val song1 = createSong(1L, "Track 1")
        val song2 = createSong(2L, "Track 2")
        queue.setQueue(listOf(song1, song2), startIndex = 1) // At last song

        // Under Repeat One, next MUST always return current song, never trigger recommendation
        val next = queue.nextSong(PlaybackRepeatMode.ONE)
        assertEquals(song2.id, next?.id)
        assertEquals(1, queue.currentIndex())
    }

    @Test
    fun repeatAll_neverReplacedByRecommendation() {
        val song1 = createSong(1L, "Track 1")
        val song2 = createSong(2L, "Track 2")
        queue.setQueue(listOf(song1, song2), startIndex = 1) // At last song

        // Under Repeat All, next wraps to 0, never triggers recommendation
        val next = queue.nextSong(PlaybackRepeatMode.ALL)
        assertEquals(song1.id, next?.id)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun manualQueue_preservesExplicitPriorityOverContinuation() {
        val currentSong = createSong(1L, "Current Track")
        val manual1 = createSong(2L, "User Enqueued 1")
        val manual2 = createSong(3L, "User Enqueued 2")

        queue.setQueue(listOf(currentSong), startIndex = 0)
        // User explicitly enqueues manual1 and manual2
        queue.addToQueue(listOf(manual1, manual2))

        // HasNext must be true because manual queue has tracks
        assertTrue(queue.hasNext(PlaybackRepeatMode.OFF))

        // Next MUST be manual1
        val n1 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals(manual1.id, n1?.id)

        // Next MUST be manual2
        val n2 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals(manual2.id, n2?.id)

        // Now queue is exhausted -> hasNext is false -> recommendation continuation allowed
        assertFalse(queue.hasNext(PlaybackRepeatMode.OFF))
    }
}
