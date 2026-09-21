package com.auralis.app

import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.LibraryFilter
import com.auralis.app.domain.model.MetadataFilter
import com.auralis.app.domain.model.PlaybackFilter
import com.auralis.app.domain.model.QualityFilter
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongFilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the advanced multi-filter engine with strict AND semantics.
 */
class AdvancedFilterTest {

    private fun createSong(
        id: Long,
        title: String = "Test Track",
        mimeType: String = "audio/mpeg",
        duration: Long = 200000L,
        bitrate: Int = 320,
        albumName: String = "Test Album",
        artistName: String = "Test Artist",
        albumArtUri: String? = "content://media/art/1"
    ): Song {
        return Song(
            id = id,
            title = title,
            artistId = 1L,
            artistName = artistName,
            albumId = 1L,
            albumName = albumName,
            genreId = 1L,
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
            albumArtUri = albumArtUri
        )
    }

    @Test
    fun activeFilterCount_reflectsOnlyNonDefaultFilters() {
        val defaultState = SongFilterState()
        assertEquals(0, defaultState.activeFilterCount)

        val oneFilter = defaultState.copy(formatFilter = FormatFilter.FLAC)
        assertEquals(1, oneFilter.activeFilterCount)

        val twoFilters = oneFilter.copy(qualityFilter = QualityFilter.LOSSLESS)
        assertEquals(2, twoFilters.activeFilterCount)

        val threeFilters = twoFilters.copy(playbackFilter = PlaybackFilter.FAVORITES)
        assertEquals(3, threeFilters.activeFilterCount)

        val fourFilters = threeFilters.copy(libraryFilter = LibraryFilter.RECENTLY_ADDED)
        assertEquals(4, fourFilters.activeFilterCount)

        val resetState = fourFilters.reset()
        assertEquals(0, resetState.activeFilterCount)
    }

    @Test
    fun formatFilter_matchesExpectedFormats() {
        val mp3 = createSong(1L, mimeType = "audio/mpeg")
        val flac = createSong(2L, mimeType = "audio/flac")
        val wav = createSong(3L, mimeType = "audio/x-wav")
        val aac = createSong(4L, mimeType = "audio/aac")
        val ogg = createSong(5L, mimeType = "audio/ogg")

        assertTrue(SongFilterState(formatFilter = FormatFilter.MP3).matches(mp3, false))
        assertFalse(SongFilterState(formatFilter = FormatFilter.MP3).matches(flac, false))

        assertTrue(SongFilterState(formatFilter = FormatFilter.FLAC).matches(flac, false))
        assertTrue(SongFilterState(formatFilter = FormatFilter.WAV).matches(wav, false))
        assertTrue(SongFilterState(formatFilter = FormatFilter.AAC).matches(aac, false))
        assertTrue(SongFilterState(formatFilter = FormatFilter.OGG).matches(ogg, false))

        // Lossless matches both flac and wav
        assertTrue(SongFilterState(formatFilter = FormatFilter.LOSSLESS).matches(flac, false))
        assertTrue(SongFilterState(formatFilter = FormatFilter.LOSSLESS).matches(wav, false))
        assertFalse(SongFilterState(formatFilter = FormatFilter.LOSSLESS).matches(mp3, false))
    }

    @Test
    fun qualityFilter_matchesLosslessHighAndLossy() {
        val flac = createSong(1L, mimeType = "audio/flac", bitrate = 0)
        val highMp3 = createSong(2L, mimeType = "audio/mpeg", bitrate = 320)
        val standardMp3 = createSong(3L, mimeType = "audio/mpeg", bitrate = 192)

        val losslessFilter = SongFilterState(qualityFilter = QualityFilter.LOSSLESS)
        assertTrue(losslessFilter.matches(flac, false))
        assertFalse(losslessFilter.matches(highMp3, false))

        val highFilter = SongFilterState(qualityFilter = QualityFilter.HIGH_QUALITY)
        assertTrue(highFilter.matches(highMp3, false))
        assertTrue(highFilter.matches(flac, false))
        assertFalse(highFilter.matches(standardMp3, false))

        val lossyFilter = SongFilterState(qualityFilter = QualityFilter.LOSSY)
        assertTrue(lossyFilter.matches(standardMp3, false))
        assertTrue(lossyFilter.matches(highMp3, false))
        assertFalse(lossyFilter.matches(flac, false))
    }

    @Test
    fun durationFilter_matchesDefinedThresholds() {
        val shortTrack = createSong(1L, duration = 120000L) // 2m (< 3m)
        val mediumTrack = createSong(2L, duration = 240000L) // 4m (3-6m)
        val longTrack = createSong(3L, duration = 420000L) // 7m (> 6m)

        assertTrue(SongFilterState(durationFilter = DurationFilter.SHORT).matches(shortTrack, false))
        assertFalse(SongFilterState(durationFilter = DurationFilter.SHORT).matches(mediumTrack, false))
        assertFalse(SongFilterState(durationFilter = DurationFilter.SHORT).matches(longTrack, false))

        assertTrue(SongFilterState(durationFilter = DurationFilter.MEDIUM).matches(mediumTrack, false))
        assertFalse(SongFilterState(durationFilter = DurationFilter.MEDIUM).matches(shortTrack, false))
        assertFalse(SongFilterState(durationFilter = DurationFilter.MEDIUM).matches(longTrack, false))

        assertTrue(SongFilterState(durationFilter = DurationFilter.LONG).matches(longTrack, false))
        assertFalse(SongFilterState(durationFilter = DurationFilter.LONG).matches(mediumTrack, false))
    }

    @Test
    fun playbackFilter_matchesPlayCountsAndFavorites() {
        val track = createSong(1L)

        val favoritesFilter = SongFilterState(playbackFilter = PlaybackFilter.FAVORITES)
        assertTrue(favoritesFilter.matches(track, isFavorite = true))
        assertFalse(favoritesFilter.matches(track, isFavorite = false))

        val playedFilter = SongFilterState(playbackFilter = PlaybackFilter.PLAYED)
        assertTrue(playedFilter.matches(track, isFavorite = false, isPlayed = true))
        assertFalse(playedFilter.matches(track, isFavorite = false, isPlayed = false))

        val neverPlayedFilter = SongFilterState(playbackFilter = PlaybackFilter.NEVER_PLAYED)
        assertTrue(neverPlayedFilter.matches(track, isFavorite = false, isPlayed = false))
        assertFalse(neverPlayedFilter.matches(track, isFavorite = false, isPlayed = true))

        val recentlyPlayedFilter = SongFilterState(playbackFilter = PlaybackFilter.RECENTLY_PLAYED)
        assertTrue(recentlyPlayedFilter.matches(track, isFavorite = false, isRecentlyPlayed = true))
        assertFalse(recentlyPlayedFilter.matches(track, isFavorite = false, isRecentlyPlayed = false))
    }

    @Test
    fun metadataFilter_matchesArtworkAndAlbumAvailability() {
        val complete = createSong(1L, albumArtUri = "art://1", albumName = "Album A", artistName = "Artist A")
        val noArt = createSong(2L, albumArtUri = null, albumName = "Album B", artistName = "Artist B")
        val noAlbum = createSong(3L, albumArtUri = null, albumName = "Unknown Album", artistName = "Artist C")
        val noArtist = createSong(4L, albumArtUri = null, albumName = "Album D", artistName = "Unknown Artist")

        val hasArtFilter = SongFilterState(metadataFilter = MetadataFilter.HAS_ARTWORK)
        assertTrue(hasArtFilter.matches(complete, false))
        assertFalse(hasArtFilter.matches(noArt, false))

        val missingArtFilter = SongFilterState(metadataFilter = MetadataFilter.MISSING_ARTWORK)
        assertTrue(missingArtFilter.matches(noArt, false))
        assertFalse(missingArtFilter.matches(complete, false))

        val missingAlbumFilter = SongFilterState(metadataFilter = MetadataFilter.MISSING_ALBUM)
        assertTrue(missingAlbumFilter.matches(noAlbum, false))
        assertFalse(missingAlbumFilter.matches(complete, false))

        val missingArtistFilter = SongFilterState(metadataFilter = MetadataFilter.MISSING_ARTIST)
        assertTrue(missingArtistFilter.matches(noArtist, false))
        assertFalse(missingArtistFilter.matches(complete, false))
    }

    @Test
    fun multiFilter_strictAndSemantics_allCriteriaMustPass() {
        val targetSong = createSong(
            id = 1L,
            title = "Bohemian Rhapsody",
            mimeType = "audio/flac",
            duration = 360000L, // 6 min (MEDIUM)
            bitrate = 0,
            albumArtUri = "art://queen"
        )

        val filterPass = SongFilterState(
            formatFilter = FormatFilter.FLAC,
            qualityFilter = QualityFilter.LOSSLESS,
            durationFilter = DurationFilter.MEDIUM,
            playbackFilter = PlaybackFilter.PLAYED,
            metadataFilter = MetadataFilter.HAS_ARTWORK,
            searchQuery = "Bohemian"
        )
        assertTrue(filterPass.matches(targetSong, isFavorite = true, isPlayed = true))

        // Failing just one criteria causes match to be false
        val filterFailDuration = filterPass.copy(durationFilter = DurationFilter.SHORT)
        assertFalse(filterFailDuration.matches(targetSong, isFavorite = true, isPlayed = true))

        val filterFailFormat = filterPass.copy(formatFilter = FormatFilter.MP3)
        assertFalse(filterFailFormat.matches(targetSong, isFavorite = true, isPlayed = true))

        val filterFailPlayback = filterPass.copy(playbackFilter = PlaybackFilter.NEVER_PLAYED)
        assertFalse(filterFailPlayback.matches(targetSong, isFavorite = true, isPlayed = true))
    }
}
