package com.auralis.app

import com.auralis.app.domain.model.Song
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.playback.queue.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PlaybackQueue].
 * Verifies queue state management, repeat modes (OFF/ONE/ALL), index bounds,
 * and dynamic queue modifications.
 */
class PlaybackQueueTest {

    private lateinit var queue: PlaybackQueue

    private fun createSong(id: Long, title: String): Song {
        return Song(
            id = id,
            title = title,
            artistId = 10L,
            artistName = "Artist",
            albumId = 20L,
            albumName = "Album",
            genreId = null,
            genreName = null,
            duration = 180000L,
            trackNumber = id.toInt(),
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = 4000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 2000L,
            albumArtUri = null
        )
    }

    @Before
    fun setUp() {
        queue = PlaybackQueue()
    }

    @Test
    fun initialQueue_isEmpty() {
        assertTrue(queue.isEmpty())
        assertEquals(0, queue.size())
        assertEquals(-1, queue.currentIndex())
        assertNull(queue.currentSong())
    }

    @Test
    fun setQueue_initializesCorrectly() {
        val songs = listOf(
            createSong(1L, "Song 1"),
            createSong(2L, "Song 2"),
            createSong(3L, "Song 3")
        )

        queue.setQueue(songs, startIndex = 1)

        assertEquals(3, queue.size())
        assertFalse(queue.isEmpty())
        assertEquals(1, queue.currentIndex())
        assertEquals("Song 2", queue.currentSong()?.title)
    }

    @Test
    fun setQueue_outOfBoundsStartIndex_clamped() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))

        queue.setQueue(songs, startIndex = 10)
        assertEquals(1, queue.currentIndex())

        queue.setQueue(songs, startIndex = -5)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun nextSong_repeatOff_progressesUntilEndThenReturnsNull() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 0)

        val next1 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals("Song 2", next1?.title)
        assertEquals(1, queue.currentIndex())

        val next2 = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNull(next2)
        assertEquals(1, queue.currentIndex())
    }

    @Test
    fun nextSong_repeatAll_wrapsToBeginningAtEnd() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 1)

        val next = queue.nextSong(PlaybackRepeatMode.ALL)
        assertEquals("Song 1", next?.title)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun nextSong_repeatOne_repeatsCurrentSongWithoutAdvancing() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 0)

        val next = queue.nextSong(PlaybackRepeatMode.ONE)
        assertEquals("Song 1", next?.title)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun previousSong_repeatOff_movesBackOrStaysAtFirst() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 1)

        val prev1 = queue.previousSong(PlaybackRepeatMode.OFF)
        assertEquals("Song 1", prev1?.title)
        assertEquals(0, queue.currentIndex())

        val prev2 = queue.previousSong(PlaybackRepeatMode.OFF)
        assertEquals("Song 1", prev2?.title)
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun previousSong_repeatAll_wrapsToLastSongFromFirst() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 0)

        val prev = queue.previousSong(PlaybackRepeatMode.ALL)
        assertEquals("Song 2", prev?.title)
        assertEquals(1, queue.currentIndex())
    }

    @Test
    fun previousSong_repeatOne_repeatsCurrentSong() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 1)

        val prev = queue.previousSong(PlaybackRepeatMode.ONE)
        assertEquals("Song 2", prev?.title)
        assertEquals(1, queue.currentIndex())
    }

    @Test
    fun addToQueue_singleAndMultipleItems() {
        val s1 = createSong(1L, "Song 1")
        val s2 = createSong(2L, "Song 2")
        val s3 = createSong(3L, "Song 3")

        queue.addToQueue(s1)
        assertEquals(1, queue.size())
        assertEquals(0, queue.currentIndex())
        assertEquals(s1, queue.currentSong())

        queue.addToQueue(listOf(s2, s3))
        assertEquals(3, queue.size())
        assertEquals(0, queue.currentIndex())
    }

    @Test
    fun removeFromQueue_adjustsCurrentIndexCorrectly() {
        val songs = listOf(
            createSong(1L, "Song 1"),
            createSong(2L, "Song 2"),
            createSong(3L, "Song 3")
        )
        queue.setQueue(songs, startIndex = 1) // currentIndex = 1 (Song 2)

        // Remove item before current: currentIndex should decrement
        val removedFirst = queue.removeFromQueue(0)
        assertTrue(removedFirst)
        assertEquals(0, queue.currentIndex())
        assertEquals("Song 2", queue.currentSong()?.title)

        // Remove invalid index: returns false
        val removedInvalid = queue.removeFromQueue(99)
        assertFalse(removedInvalid)

        // Remove last item: currentIndex becomes 0
        queue.removeFromQueue(1)
        assertEquals(1, queue.size())
        assertEquals(0, queue.currentIndex())

        // Remove final remaining item: currentIndex becomes -1
        queue.removeFromQueue(0)
        assertTrue(queue.isEmpty())
        assertEquals(-1, queue.currentIndex())
    }

    @Test
    fun clearQueue_resetsState() {
        val songs = listOf(createSong(1L, "Song 1"))
        queue.setQueue(songs, 0)
        queue.clearQueue()

        assertTrue(queue.isEmpty())
        assertEquals(-1, queue.currentIndex())
        assertNull(queue.currentSong())
    }

    @Test
    fun findSongById_and_indexOfSongId() {
        val s1 = createSong(10L, "Song 10")
        val s2 = createSong(20L, "Song 20")
        queue.setQueue(listOf(s1, s2), 0)

        assertEquals(s2, queue.findSongById(20L))
        assertNull(queue.findSongById(999L))

        assertEquals(1, queue.indexOfSongId(20L))
        assertEquals(-1, queue.indexOfSongId(999L))
    }

    @Test
    fun playNext_insertsDirectlyAfterCurrentIndex() {
        val s1 = createSong(1L, "Song 1")
        val s2 = createSong(2L, "Song 2")
        val s3 = createSong(3L, "Song 3")
        val manualNext = createSong(99L, "Manual Next")

        // Queue: [Song 1, Song 2, Song 3], current index = 0 (Song 1)
        queue.setQueue(listOf(s1, s2, s3), startIndex = 0)

        queue.playNext(manualNext)

        // Queue must now be: [Song 1, Manual Next, Song 2, Song 3]
        assertEquals(4, queue.size())
        assertEquals(0, queue.currentIndex())
        assertEquals("Song 1", queue.currentSong()?.title)

        // Calling nextSong should immediately play Manual Next
        val next = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals("Manual Next", next?.title)
        assertEquals(1, queue.currentIndex())

        // Next again should play Song 2
        val nextAfterManual = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals("Song 2", nextAfterManual?.title)
        assertEquals(2, queue.currentIndex())
    }

    @Test
    fun playNext_multipleSongs_insertsDirectlyAfterCurrentIndexInOrder() {
        val s1 = createSong(1L, "Song 1")
        val s2 = createSong(2L, "Song 2")
        val manualX = createSong(10L, "Manual X")
        val manualY = createSong(20L, "Manual Y")

        queue.setQueue(listOf(s1, s2), startIndex = 0)
        queue.playNext(listOf(manualX, manualY))

        // Queue must be: [Song 1, Manual X, Manual Y, Song 2]
        assertEquals(4, queue.size())
        assertEquals("Song 1", queue.currentSong()?.title)

        assertEquals("Manual X", queue.nextSong(PlaybackRepeatMode.OFF)?.title)
        assertEquals("Manual Y", queue.nextSong(PlaybackRepeatMode.OFF)?.title)
        assertEquals("Song 2", queue.nextSong(PlaybackRepeatMode.OFF)?.title)
    }

    @Test
    fun playNext_whenQueueEmpty_initializesQueue() {
        val manual = createSong(1L, "Manual Solo")
        queue.playNext(manual)

        assertEquals(1, queue.size())
        assertEquals(0, queue.currentIndex())
        assertEquals("Manual Solo", queue.currentSong()?.title)
    }

    @Test
    fun hasNext_accuratelyReflectsAvailabilityAcrossRepeatModes() {
        val s1 = createSong(1L, "Song 1")
        val s2 = createSong(2L, "Song 2")

        queue.setQueue(listOf(s1, s2), startIndex = 0)
        // At index 0, has next item regardless of repeat
        assertTrue(queue.hasNext(PlaybackRepeatMode.OFF))
        assertTrue(queue.hasNext(PlaybackRepeatMode.ALL))
        assertTrue(queue.hasNext(PlaybackRepeatMode.ONE))

        // Advance to index 1 (last item)
        queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals(1, queue.currentIndex())

        // Repeat OFF -> no more items in queue
        assertFalse(queue.hasNext(PlaybackRepeatMode.OFF))
        // Repeat ALL -> wraps around, so has next
        assertTrue(queue.hasNext(PlaybackRepeatMode.ALL))
        // Repeat ONE -> replays current, so has next
        assertTrue(queue.hasNext(PlaybackRepeatMode.ONE))
    }
}
