package com.auralis.app

import com.auralis.app.domain.model.SearchRankingEngine
import com.auralis.app.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the deterministic, multi-field local search ranking engine.
 */
class SearchRankingEngineTest {

    private fun createSong(
        id: Long,
        title: String,
        artistName: String = "Queen",
        albumName: String = "A Night at the Opera",
        genreName: String? = "Rock",
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
            genreName = genreName,
            duration = 180000L,
            trackNumber = 1,
            discNumber = 1,
            year = 1975,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
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
    fun blankOrEmptyQuery_returnsEmptyList() {
        val songs = listOf(createSong(1L, "Bohemian Rhapsody"))
        assertTrue(SearchRankingEngine.rankSongs(songs, "").isEmpty())
        assertTrue(SearchRankingEngine.rankSongs(songs, "   ").isEmpty())
    }

    @Test
    fun exactTitleMatch_ranksHigherThanPrefixMatch() {
        val prefixSong = createSong(1L, "Bohemian Rhapsody Live")
        val exactSong = createSong(2L, "Bohemian Rhapsody")
        val containsSong = createSong(3L, "The Bohemian Rhapsody")

        val results = SearchRankingEngine.rankSongs(listOf(prefixSong, exactSong, containsSong), "Bohemian Rhapsody")

        assertEquals(3, results.size)
        assertEquals(exactSong.id, results[0].id)
        assertEquals(prefixSong.id, results[1].id)
        assertEquals(containsSong.id, results[2].id)
    }

    @Test
    fun fieldWeighting_titleRanksHigherThanArtistAndAlbum() {
        val albumMatchSong = createSong(1L, "Radio Ga Ga", artistName = "Queen", albumName = "Starboy")
        val artistMatchSong = createSong(2L, "Ordinary", artistName = "Starboy", albumName = "Other Album")
        val titleMatchSong = createSong(3L, "Starboy", artistName = "The Weeknd", albumName = "Other Album")

        val results = SearchRankingEngine.rankSongs(listOf(albumMatchSong, artistMatchSong, titleMatchSong), "Starboy")

        assertEquals(3, results.size)
        assertEquals("Title match should have highest priority", titleMatchSong.id, results[0].id)
        assertEquals("Artist match should rank second", artistMatchSong.id, results[1].id)
        assertEquals("Album match should rank third", albumMatchSong.id, results[2].id)
    }

    @Test
    fun accentInsensitiveMatching_matchesCorrectly() {
        val cafeSong = createSong(1L, "Café del Mar")
        val dvorakSong = createSong(2L, "Symphony No. 9", artistName = "Antonín Dvořák")
        val beyonceSong = createSong(3L, "Halo", artistName = "Beyoncé")

        val cafeResults = SearchRankingEngine.rankSongs(listOf(cafeSong), "cafe")
        assertEquals(1, cafeResults.size)
        assertEquals(cafeSong.id, cafeResults[0].id)

        val dvorakResults = SearchRankingEngine.rankSongs(listOf(dvorakSong), "Dvorak")
        assertEquals(1, dvorakResults.size)
        assertEquals(dvorakSong.id, dvorakResults[0].id)

        val beyonceResults = SearchRankingEngine.rankSongs(listOf(beyonceSong), "beyonce")
        assertEquals(1, beyonceResults.size)
        assertEquals(beyonceSong.id, beyonceResults[0].id)
    }

    @Test
    fun caseInsensitiveMatching_matchesAllCases() {
        val song = createSong(1L, "Bohemian Rhapsody")

        val upperResults = SearchRankingEngine.rankSongs(listOf(song), "BOHEMIAN")
        val mixedResults = SearchRankingEngine.rankSongs(listOf(song), "bOhEmIaN")

        assertEquals(1, upperResults.size)
        assertEquals(1, mixedResults.size)
    }

    @Test
    fun tieBreaking_breaksAlphabeticallyByTitle() {
        val songB = createSong(1L, "Track B")
        val songA = createSong(2L, "Track A")
        val songC = createSong(3L, "Track C")

        val results = SearchRankingEngine.rankSongs(listOf(songB, songA, songC), "Track")

        assertEquals(3, results.size)
        assertEquals("Track A should win alphabetical tie-break", songA.id, results[0].id)
        assertEquals(songB.id, results[1].id)
        assertEquals(songC.id, results[2].id)
    }
}
