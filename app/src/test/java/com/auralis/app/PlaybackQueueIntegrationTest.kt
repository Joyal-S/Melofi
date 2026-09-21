package com.auralis.app

import com.auralis.app.domain.model.Song
import com.auralis.app.playback.mapper.MediaItemMapper
import com.auralis.app.playback.queue.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying playback queue integration with playlist and favorite collections.
 */
class PlaybackQueueIntegrationTest {

    private fun createSampleSong(id: Long, title: String): Song {
        return Song(
            id = id,
            title = title,
            artistId = 1L,
            artistName = "Test Artist",
            albumId = 2L,
            albumName = "Test Album",
            genreId = null,
            genreName = null,
            duration = 180000L,
            trackNumber = id.toInt(),
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = 5000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 2000L,
            albumArtUri = null
        )
    }

    @Test
    fun playPlaylist_setsQueueWithOrderedSongs() {
        val playlistSongs = listOf(
            createSampleSong(10L, "Track 1"),
            createSampleSong(20L, "Track 2"),
            createSampleSong(30L, "Track 3")
        )

        val queue = PlaybackQueue()
        queue.setQueue(playlistSongs, startIndex = 0)

        assertEquals(3, queue.size())
        assertEquals("Track 1", queue.currentSong()?.title)
        assertEquals(0, queue.currentIndex())

        val next = queue.nextSong()
        assertEquals("Track 2", next?.title)
        assertEquals(1, queue.currentIndex())
    }

    @Test
    fun addPlaylistToQueue_appendsSongsCorrectly() {
        val initialSongs = listOf(createSampleSong(1L, "Initial Track"))
        val queue = PlaybackQueue()
        queue.setQueue(initialSongs, startIndex = 0)

        val playlistSongs = listOf(
            createSampleSong(2L, "Playlist Track 1"),
            createSampleSong(3L, "Playlist Track 2")
        )

        queue.addToQueue(playlistSongs)

        assertEquals(3, queue.size())
        assertEquals(0, queue.currentIndex())
        assertEquals("Initial Track", queue.currentSong()?.title)

        val allSongs = queue.getSongs()
        assertEquals(1L, allSongs[0].id)
        assertEquals(2L, allSongs[1].id)
        assertEquals(3L, allSongs[2].id)
    }

    @Test
    fun emptyPlaylist_setQueueHandlesGracefullyWithoutCrash() {
        val emptyList = emptyList<Song>()
        val queue = PlaybackQueue()

        queue.setQueue(emptyList, startIndex = 0)

        assertTrue(queue.isEmpty())
        assertEquals(0, queue.size())
        assertEquals(-1, queue.currentIndex())
        assertNull(queue.currentSong())
    }

    @Test
    fun playlistSongs_extractSongIdMaintainsMapping() {
        val song1 = createSampleSong(42L, "Track 42")
        val song2 = createSampleSong(99L, "Track 99")

        val id1 = MediaItemMapper.extractSongId(song1.id.toString())
        val id2 = MediaItemMapper.extractSongId(song2.id.toString())

        assertEquals(42L, id1)
        assertEquals(99L, id2)
    }
}
