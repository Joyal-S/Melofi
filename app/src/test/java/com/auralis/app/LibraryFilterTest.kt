package com.auralis.app

import com.auralis.app.domain.model.BitrateFilter
import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongFilterState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying library quality and specification filters.
 */
class LibraryFilterTest {

    private fun createSong(
        id: Long,
        title: String,
        mimeType: String,
        duration: Long,
        bitrate: Int
    ): Song {
        return Song(
            id = id,
            title = title,
            artistId = 1L,
            artistName = "Artist",
            albumId = 1L,
            albumName = "Album",
            genreId = null,
            genreName = "Pop",
            duration = duration,
            trackNumber = 1,
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = mimeType,
            size = 5000000L,
            bitrate = bitrate,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 1000L,
            albumArtUri = null
        )
    }

    @Test
    fun filter_losslessOnly_matchesOnlyLosslessFormats() {
        val flacSong = createSong(1L, "Flac Track", "audio/flac", 200000L, 900)
        val mp3Song = createSong(2L, "Mp3 Track", "audio/mpeg", 200000L, 320)

        val filter = SongFilterState(formatFilter = FormatFilter.LOSSLESS)

        assertTrue(filter.matches(flacSong, isFavorite = false))
        assertFalse(filter.matches(mp3Song, isFavorite = false))
    }

    @Test
    fun filter_formatSpecific_matchesExactFormats() {
        val flacSong = createSong(1L, "Flac Track", "audio/flac", 200000L, 900)
        val mp3Song = createSong(2L, "Mp3 Track", "audio/mpeg", 200000L, 320)

        val mp3Filter = SongFilterState(formatFilter = FormatFilter.MP3)
        assertFalse(mp3Filter.matches(flacSong, isFavorite = false))
        assertTrue(mp3Filter.matches(mp3Song, isFavorite = false))

        val flacFilter = SongFilterState(formatFilter = FormatFilter.FLAC)
        assertTrue(flacFilter.matches(flacSong, isFavorite = false))
        assertFalse(flacFilter.matches(mp3Song, isFavorite = false))
    }

    @Test
    fun filter_durationShortAndLong_filtersCorrectRanges() {
        val shortSong = createSong(1L, "Short", "audio/mpeg", 120000L, 320) // 2 min
        val mediumSong = createSong(2L, "Medium", "audio/mpeg", 240000L, 320) // 4 min
        val longSong = createSong(3L, "Long", "audio/mpeg", 420000L, 320) // 7 min

        val shortFilter = SongFilterState(durationFilter = DurationFilter.SHORT)
        assertTrue(shortFilter.matches(shortSong, isFavorite = false))
        assertFalse(shortFilter.matches(mediumSong, isFavorite = false))
        assertFalse(shortFilter.matches(longSong, isFavorite = false))

        val longFilter = SongFilterState(durationFilter = DurationFilter.LONG)
        assertFalse(longFilter.matches(shortSong, isFavorite = false))
        assertFalse(longFilter.matches(mediumSong, isFavorite = false))
        assertTrue(longFilter.matches(longSong, isFavorite = false))
    }

    @Test
    fun filter_highBitrate_matchesHighBitrateAndLossless() {
        val lowBitrateSong = createSong(1L, "Low", "audio/mpeg", 200000L, 128)
        val highBitrateMp3 = createSong(2L, "High", "audio/mpeg", 200000L, 320)
        val losslessSong = createSong(3L, "Lossless", "audio/flac", 200000L, 0) // Flac with unread bitrate

        val filter = SongFilterState(bitrateFilter = BitrateFilter.HIGH_QUALITY)

        assertFalse(filter.matches(lowBitrateSong, isFavorite = false))
        assertTrue(filter.matches(highBitrateMp3, isFavorite = false))
        assertTrue(filter.matches(losslessSong, isFavorite = false))
    }

    @Test
    fun filter_favoritesOnly_filtersUnfavorited() {
        val song = createSong(1L, "Song", "audio/mpeg", 200000L, 320)

        val filter = SongFilterState(favoritesOnly = true)

        assertFalse(filter.matches(song, isFavorite = false))
        assertTrue(filter.matches(song, isFavorite = true))
    }

    @Test
    fun filter_searchQuery_matchesTitleArtistAlbumAndGenre() {
        val song = createSong(1L, "Starboy", "audio/mpeg", 200000L, 320)

        // Matching title
        val titleFilter = SongFilterState(searchQuery = "star")
        assertTrue(titleFilter.matches(song, isFavorite = false))

        // Matching artist
        val artistFilter = SongFilterState(searchQuery = "artist")
        assertTrue(artistFilter.matches(song, isFavorite = false))

        // Matching album
        val albumFilter = SongFilterState(searchQuery = "album")
        assertTrue(albumFilter.matches(song, isFavorite = false))

        // Matching genre
        val genreFilter = SongFilterState(searchQuery = "pop")
        assertTrue(genreFilter.matches(song, isFavorite = false))

        // Non-matching query
        val noMatchFilter = SongFilterState(searchQuery = "nonexistent")
        assertFalse(noMatchFilter.matches(song, isFavorite = false))
    }
}
