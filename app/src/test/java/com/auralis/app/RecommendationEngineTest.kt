package com.auralis.app

import com.auralis.app.domain.model.RecommendationEngine
import com.auralis.app.domain.model.RecommendationProfile
import com.auralis.app.domain.model.RecommendationReason
import com.auralis.app.domain.model.RecommendationSectionType
import com.auralis.app.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the deterministic, local recommendation engine.
 */
class RecommendationEngineTest {

    private fun createSong(
        id: Long,
        title: String = "Track $id",
        artistName: String = "Queen",
        albumName: String = "A Night at the Opera",
        genreName: String = "Rock",
        mimeType: String = "audio/mpeg",
        dateAdded: Long = 1000L
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
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = mimeType,
            size = 5000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = dateAdded,
            dateModified = 1000L,
            albumArtUri = null
        )
    }

    @Test
    fun coldStart_zeroPlays_returnsLibraryExploration() {
        val songs = (1L..20L).map { createSong(it, dateAdded = it * 100) }
        val emptyProfile = RecommendationProfile()

        val sections = RecommendationEngine.generateSections(songs, emptyProfile)

        assertTrue(sections.isNotEmpty())
        val exploreSection = sections.firstOrNull { it.type == RecommendationSectionType.EXPLORE_COLLECTION }
        assertTrue("Expected Explore Your Library section in cold start", exploreSection != null)
        assertEquals("Recommendations adapt as you listen to music and favorite tracks", exploreSection?.subtitle)
        assertTrue(exploreSection?.items?.first()?.reason is RecommendationReason.ExploreLibrary)
    }

    @Test
    fun artistAffinity_boostsTopArtistTracks() {
        val song1 = createSong(1L, "Radio Ga Ga", artistName = "Queen")
        val song2 = createSong(2L, "Let It Be", artistName = "The Beatles")

        val profile = RecommendationProfile(
            artistPlayCounts = mapOf("Queen" to 15, "The Beatles" to 2),
            totalPlays = 17
        )

        val item1 = RecommendationEngine.scoreCandidate(song1, profile)
        val item2 = RecommendationEngine.scoreCandidate(song2, profile)

        assertTrue("Queen track should score significantly higher than Beatles track", item1.score > item2.score)
        val artistScore1 = item1.scoreBreakdown["artistAffinity"] ?: 0f
        val artistScore2 = item2.scoreBreakdown["artistAffinity"] ?: 0f
        assertTrue(artistScore1 > artistScore2)
        assertEquals(20.0f, artistScore1, 0.01f) // Max ratio * 20
    }

    @Test
    fun genreAffinity_boostsFavoriteGenreTracks() {
        val rockSong = createSong(1L, "Rock Track", genreName = "Rock")
        val jazzSong = createSong(2L, "Jazz Track", genreName = "Jazz")

        val profile = RecommendationProfile(
            genrePlayCounts = mapOf("rock" to 20, "jazz" to 1),
            totalPlays = 21
        )

        val itemRock = RecommendationEngine.scoreCandidate(rockSong, profile)
        val itemJazz = RecommendationEngine.scoreCandidate(jazzSong, profile)

        assertTrue(itemRock.score > itemJazz.score)
        assertTrue((itemRock.scoreBreakdown["genreAffinity"] ?: 0f) > (itemJazz.scoreBreakdown["genreAffinity"] ?: 0f))
    }

    @Test
    fun recencyDecay_piecewiseDecayDecreasesWithTime() {
        val now = 1_000_000_000L
        val song = createSong(1L)

        // 1 hour ago
        val profile1h = RecommendationProfile(recentSongTimestamps = mapOf(1L to now - 3600 * 1000L))
        val score1h = RecommendationEngine.scoreCandidate(song, profile1h, now).scoreBreakdown["recency"] ?: 0f
        assertEquals(15.0f, score1h, 0.01f)

        // 2 days ago (48 hours)
        val profile2d = RecommendationProfile(recentSongTimestamps = mapOf(1L to now - 48L * 3600 * 1000L))
        val score2d = RecommendationEngine.scoreCandidate(song, profile2d, now).scoreBreakdown["recency"] ?: 0f
        assertEquals(12.0f, score2d, 0.01f)

        // 5 days ago (120 hours)
        val profile5d = RecommendationProfile(recentSongTimestamps = mapOf(1L to now - 120L * 3600 * 1000L))
        val score5d = RecommendationEngine.scoreCandidate(song, profile5d, now).scoreBreakdown["recency"] ?: 0f
        assertEquals(9.0f, score5d, 0.01f)

        // 40 days ago (> 720 hours)
        val profile40d = RecommendationProfile(recentSongTimestamps = mapOf(1L to now - 40L * 24 * 3600 * 1000L))
        val score40d = RecommendationEngine.scoreCandidate(song, profile40d, now).scoreBreakdown["recency"] ?: 0f
        assertEquals(0.0f, score40d, 0.01f)
    }

    @Test
    fun playFrequency_logarithmicNormalizationIsBounded() {
        val song = createSong(1L)

        val profile1Play = RecommendationProfile(songPlayCounts = mapOf(1L to 1))
        val profile10Plays = RecommendationProfile(songPlayCounts = mapOf(1L to 10))
        val profile1000Plays = RecommendationProfile(songPlayCounts = mapOf(1L to 1000))

        val score1 = RecommendationEngine.scoreCandidate(song, profile1Play).scoreBreakdown["playFrequency"] ?: 0f
        val score10 = RecommendationEngine.scoreCandidate(song, profile10Plays).scoreBreakdown["playFrequency"] ?: 0f
        val score1000 = RecommendationEngine.scoreCandidate(song, profile1000Plays).scoreBreakdown["playFrequency"] ?: 0f

        assertTrue(score1 in 3.0f..4.0f)
        assertTrue(score10 > score1)
        assertTrue(score1000 <= 15.0f) // Hard upper bound
    }

    @Test
    fun completionSignal_highCompletionRanksHigher() {
        val song = createSong(1L)

        val profileCompleted = RecommendationProfile(songCompletionRatios = mapOf(1L to 1.0f))
        val profileSkipped = RecommendationProfile(songCompletionRatios = mapOf(1L to 0.1f))

        val scoreComp = RecommendationEngine.scoreCandidate(song, profileCompleted).scoreBreakdown["completion"] ?: 0f
        val scoreSkip = RecommendationEngine.scoreCandidate(song, profileSkipped).scoreBreakdown["completion"] ?: 0f

        assertEquals(10.0f, scoreComp, 0.01f)
        assertEquals(1.0f, scoreSkip, 0.01f)
    }

    @Test
    fun forgottenFavorite_receivesDedicatedBoostAndReason() {
        val song = createSong(1L)
        val profile = RecommendationProfile(
            favoriteSongIds = setOf(1L),
            forgottenFavoriteIds = setOf(1L),
            totalPlays = 10
        )

        val item = RecommendationEngine.scoreCandidate(song, profile)

        assertTrue(item.score >= 35.0f) // 20 forgotten + 15 fav
        assertEquals(RecommendationReason.ForgottenFavorite, item.reason)
        assertEquals("Forgotten favorite • Not played recently", item.reason.displayText)
    }

    @Test
    fun playlistCoOccurrence_boostsAssociatedTracks() {
        val songTarget = createSong(1L)
        val profile = RecommendationProfile(
            favoriteSongIds = setOf(2L, 3L),
            playlistSongPairs = mapOf(1L to setOf(2L, 3L)),
            totalPlays = 10
        )

        val item = RecommendationEngine.scoreCandidate(songTarget, profile)
        val playlistScore = item.scoreBreakdown["playlistCoOccurrence"] ?: 0f

        assertEquals(10.0f, playlistScore, 0.01f)
    }

    @Test
    fun unplayedDiscovery_boostsNeverPlayedSongsMatchingTaste() {
        val unplayedSong = createSong(1L, artistName = "Queen")
        val profile = RecommendationProfile(
            unplayedSongIds = setOf(1L),
            artistPlayCounts = mapOf("Queen" to 10),
            totalPlays = 10
        )

        val item = RecommendationEngine.scoreCandidate(unplayedSong, profile)
        val discScore = item.scoreBreakdown["discovery"] ?: 0f

        assertEquals(15.0f, discScore, 0.01f) // 5 base + 10 taste
    }

    @Test
    fun repetitionPenalty_appliesOnlyToVeryRecentlyPlayed() {
        val song = createSong(1L)
        val now = 1_000_000_000L

        // Played 1 hour ago (penalty should apply: -10.0)
        val profileRecent = RecommendationProfile(recentSongTimestamps = mapOf(1L to now - 3600 * 1000L))
        val itemRecent = RecommendationEngine.scoreCandidate(song, profileRecent, now)
        assertEquals(-10.0f, itemRecent.scoreBreakdown["repetitionPenalty"] ?: 0f, 0.01f)

        // Played 10 hours ago (no penalty: 0.0)
        val profileOld = RecommendationProfile(recentSongTimestamps = mapOf(1L to now - 10L * 3600 * 1000L))
        val itemOld = RecommendationEngine.scoreCandidate(song, profileOld, now)
        assertEquals(0.0f, itemOld.scoreBreakdown["repetitionPenalty"] ?: 0f, 0.01f)
    }

    @Test
    fun diversityEnforcement_limitsTracksPerArtistAndAlbum() {
        val songs = (1L..10L).map {
            createSong(it, title = "Queen Song $it", artistName = "Queen", albumName = "Opera")
        } + (11L..15L).map {
            createSong(it, title = "Beatles Song $it", artistName = "The Beatles", albumName = "Abbey Road")
        }

        val profile = RecommendationProfile(
            artistPlayCounts = mapOf("Queen" to 50, "The Beatles" to 10),
            totalPlays = 60
        )

        val scored = songs.map { RecommendationEngine.scoreCandidate(it, profile) }
        val diversified = RecommendationEngine.enforceDiversity(scored, maxPerArtist = 2, maxPerAlbum = 2, targetCount = 10)

        val queenCount = diversified.count { it.song.artistName == "Queen" }
        assertEquals("Should be capped at 2 Queen tracks", 2, queenCount)
    }

    @Test
    fun determinism_identicalInputsProduceExactSameRecommendations() {
        val songs = (1L..50L).map {
            createSong(
                it,
                title = "Song $it",
                artistName = if (it % 2 == 0L) "Queen" else "The Beatles",
                genreName = if (it % 3 == 0L) "Rock" else "Pop"
            )
        }

        val profile = RecommendationProfile(
            artistPlayCounts = mapOf("Queen" to 20, "The Beatles" to 10),
            genrePlayCounts = mapOf("rock" to 15, "pop" to 8),
            favoriteSongIds = setOf(2L, 4L, 6L),
            totalPlays = 30
        )

        val fixedTime = 1700000000000L

        val run1 = RecommendationEngine.generateSections(songs, profile, fixedTime)
        val run2 = RecommendationEngine.generateSections(songs, profile, fixedTime)

        assertEquals(run1.size, run2.size)
        for (i in run1.indices) {
            val sec1 = run1[i]
            val sec2 = run2[i]
            assertEquals(sec1.type, sec2.type)
            assertEquals(sec1.title, sec2.title)
            assertEquals(sec1.items.size, sec2.items.size)
            for (j in sec1.items.indices) {
                assertEquals(sec1.items[j].song.id, sec2.items[j].song.id)
                assertEquals(sec1.items[j].score, sec2.items[j].score, 0.001f)
                assertEquals(sec1.items[j].reason, sec2.items[j].reason)
            }
        }
    }
}
