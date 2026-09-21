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
 * Unit tests verifying PlaybackQueue state, active queue retrieval, and navigation bounds.
 */
class PlaybackQueueFlowTest {

    private lateinit var queue: PlaybackQueue

    private fun createSong(id: Long, title: String) = Song(
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

    @Before
    fun setUp() {
        queue = PlaybackQueue()
    }

    @Test
    fun initialQueue_isEmpty() {
        assertTrue(queue.isEmpty())
        assertEquals(0, queue.size())
        assertNull(queue.currentSong())
        assertTrue(queue.getSongs().isEmpty())
    }

    @Test
    fun setQueue_populatesSongsAndSetsCurrentIndex() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"), createSong(3L, "Song 3"))
        queue.setQueue(songs, startIndex = 1)

        assertEquals(3, queue.size())
        assertFalse(queue.isEmpty())
        assertEquals("Song 2", queue.currentSong()?.title)
        assertEquals(1, queue.currentIndex())
        assertEquals(3, queue.getSongs().size)
    }

    @Test
    fun nextAndPrevious_traverseWithinBounds() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 0)

        val next = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals("Song 2", next?.title)

        val endNext = queue.nextSong(PlaybackRepeatMode.OFF)
        assertNull(endNext)

        val prev = queue.previousSong(PlaybackRepeatMode.OFF)
        assertEquals("Song 1", prev?.title)
    }

    @Test
    fun repeatAll_loopsToBeginning() {
        val songs = listOf(createSong(1L, "Song 1"), createSong(2L, "Song 2"))
        queue.setQueue(songs, startIndex = 1)

        val looped = queue.nextSong(PlaybackRepeatMode.ALL)
        assertEquals("Song 1", looped?.title)
    }

    @Test
    fun addToQueue_appendsItemsAndUpdatesQueueList() {
        val s1 = createSong(1L, "Song 1")
        val s2 = createSong(2L, "Song 2")
        queue.setQueue(listOf(s1), startIndex = 0)

        queue.addToQueue(s2)
        assertEquals(2, queue.size())
        assertEquals(listOf(s1, s2), queue.getSongs())
    }
}
