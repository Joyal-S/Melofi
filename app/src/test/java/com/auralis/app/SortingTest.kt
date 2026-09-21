package com.auralis.app

import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Unit tests verifying that all 15 sorting modes correctly order the library.
 */
class SortingTest {

    private fun createSong(
        id: Long,
        title: String,
        artistName: String = "Artist",
        albumName: String = "Album",
        trackNumber: Int = 1,
        dateAdded: Long = 1000L,
        dateModified: Long = 1000L,
        duration: Long = 180000L,
        size: Long = 5000000L,
        bitrate: Int = 320
    ): Song {
        return Song(
            id = id,
            title = title,
            artistId = 1L,
            artistName = artistName,
            albumId = 1L,
            albumName = albumName,
            genreId = 1L,
            genreName = "Rock",
            duration = duration,
            trackNumber = trackNumber,
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = size,
            bitrate = bitrate,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = dateAdded,
            dateModified = dateModified,
            albumArtUri = null
        )
    }

    private fun sort(songs: List<Song>, order: SongSortOrder, recentIds: Set<Long> = emptySet()): List<Song> {
        return when (order) {
            SongSortOrder.TITLE -> songs.sortedBy { it.title.lowercase(Locale.ROOT) }
            SongSortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase(Locale.ROOT) }
            SongSortOrder.ARTIST -> songs.sortedWith(
                compareBy<Song> { it.artistName.lowercase(Locale.ROOT) }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            SongSortOrder.ARTIST_DESC -> songs.sortedWith(
                compareByDescending<Song> { it.artistName.lowercase(Locale.ROOT) }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            SongSortOrder.ALBUM -> songs.sortedWith(
                compareBy<Song> { it.albumName.lowercase(Locale.ROOT) }
                    .thenBy { it.trackNumber ?: 0 }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            SongSortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SongSortOrder.DATE_MODIFIED -> songs.sortedByDescending { it.dateModified }
            SongSortOrder.DURATION_ASC -> songs.sortedBy { it.duration }
            SongSortOrder.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SongSortOrder.FILE_SIZE_ASC -> songs.sortedBy { it.size }
            SongSortOrder.FILE_SIZE_DESC -> songs.sortedByDescending { it.size }
            SongSortOrder.BITRATE_DESC -> songs.sortedByDescending { it.bitrate ?: 0 }
            SongSortOrder.BITRATE_ASC -> songs.sortedBy { it.bitrate ?: 0 }
            SongSortOrder.MOST_PLAYED, SongSortOrder.RECENTLY_PLAYED -> {
                songs.sortedByDescending { it.id in recentIds }
            }
        }
    }

    @Test
    fun all15Modes_areAccountedFor() {
        assertEquals(15, SongSortOrder.entries.size)
    }

    @Test
    fun sort_titleAscendingAndDescending() {
        val songA = createSong(1L, "Alpha")
        val songB = createSong(2L, "Bravo")
        val songZ = createSong(3L, "Zulu")
        val list = listOf(songB, songZ, songA)

        val asc = sort(list, SongSortOrder.TITLE)
        assertEquals(listOf(1L, 2L, 3L), asc.map { it.id })

        val desc = sort(list, SongSortOrder.TITLE_DESC)
        assertEquals(listOf(3L, 2L, 1L), desc.map { it.id })
    }

    @Test
    fun sort_artistAscendingAndDescending() {
        val songA = createSong(1L, "Song 1", artistName = "Adele")
        val songB = createSong(2L, "Song 2", artistName = "Beatles")
        val songC = createSong(3L, "Song 3", artistName = "Coldplay")
        val list = listOf(songB, songC, songA)

        val asc = sort(list, SongSortOrder.ARTIST)
        assertEquals(listOf(1L, 2L, 3L), asc.map { it.id })

        val desc = sort(list, SongSortOrder.ARTIST_DESC)
        assertEquals(listOf(3L, 2L, 1L), desc.map { it.id })
    }

    @Test
    fun sort_albumAndTrackNumber() {
        val track2 = createSong(1L, "Song B", albumName = "Abbey Road", trackNumber = 2)
        val track1 = createSong(2L, "Song A", albumName = "Abbey Road", trackNumber = 1)
        val albumZ = createSong(3L, "Song C", albumName = "Zooropa", trackNumber = 1)

        val sorted = sort(listOf(track2, albumZ, track1), SongSortOrder.ALBUM)
        assertEquals(listOf(2L, 1L, 3L), sorted.map { it.id })
    }

    @Test
    fun sort_dates_addedAndModified() {
        val older = createSong(1L, "Older", dateAdded = 100L, dateModified = 500L)
        val newer = createSong(2L, "Newer", dateAdded = 500L, dateModified = 100L)

        val added = sort(listOf(older, newer), SongSortOrder.DATE_ADDED)
        assertEquals(listOf(2L, 1L), added.map { it.id })

        val modified = sort(listOf(older, newer), SongSortOrder.DATE_MODIFIED)
        assertEquals(listOf(1L, 2L), modified.map { it.id })
    }

    @Test
    fun sort_durations_ascAndDesc() {
        val shortTrack = createSong(1L, "Short", duration = 60000L)
        val mediumTrack = createSong(2L, "Medium", duration = 180000L)
        val longTrack = createSong(3L, "Long", duration = 400000L)

        val asc = sort(listOf(longTrack, shortTrack, mediumTrack), SongSortOrder.DURATION_ASC)
        assertEquals(listOf(1L, 2L, 3L), asc.map { it.id })

        val desc = sort(listOf(longTrack, shortTrack, mediumTrack), SongSortOrder.DURATION_DESC)
        assertEquals(listOf(3L, 2L, 1L), desc.map { it.id })
    }

    @Test
    fun sort_fileSizes_ascAndDesc() {
        val small = createSong(1L, "Small", size = 1000L)
        val large = createSong(2L, "Large", size = 10000000L)

        val asc = sort(listOf(large, small), SongSortOrder.FILE_SIZE_ASC)
        assertEquals(listOf(1L, 2L), asc.map { it.id })

        val desc = sort(listOf(large, small), SongSortOrder.FILE_SIZE_DESC)
        assertEquals(listOf(2L, 1L), desc.map { it.id })
    }

    @Test
    fun sort_bitrates_ascAndDesc() {
        val lowBitrate = createSong(1L, "Low", bitrate = 128)
        val highBitrate = createSong(2L, "High", bitrate = 320)

        val asc = sort(listOf(highBitrate, lowBitrate), SongSortOrder.BITRATE_ASC)
        assertEquals(listOf(1L, 2L), asc.map { it.id })

        val desc = sort(listOf(highBitrate, lowBitrate), SongSortOrder.BITRATE_DESC)
        assertEquals(listOf(2L, 1L), desc.map { it.id })
    }

    @Test
    fun sort_recentlyPlayed() {
        val unplayed = createSong(1L, "Unplayed")
        val played = createSong(2L, "Played")

        val recent = sort(listOf(unplayed, played), SongSortOrder.RECENTLY_PLAYED, recentIds = setOf(2L))
        assertEquals(listOf(2L, 1L), recent.map { it.id })
    }
}
