package com.auralis.app

import com.auralis.app.domain.model.RecommendationEngine
import com.auralis.app.domain.model.RecommendationProfile
import com.auralis.app.domain.model.Song
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Synthetic performance benchmark evaluating recommendation candidate scoring,
 * diversity filtering, and section assembly across a simulated 5,000-song library.
 */
class RecommendationPerformanceTest {

    private fun generateSyntheticLibrary(count: Int): List<Song> {
        val artists = (1..50).map { "Artist $it" }
        val albums = (1..100).map { "Album $it" }
        val genres = listOf("Rock", "Pop", "Jazz", "Classical", "Hip Hop", "Electronic", "Metal", "R&B")
        val formats = listOf("audio/mpeg", "audio/flac", "audio/x-wav", "audio/aac")

        return (1..count).map { i ->
            val artist = artists[i % artists.size]
            val album = albums[i % albums.size]
            val genre = genres[i % genres.size]
            val mime = formats[i % formats.size]

            Song(
                id = i.toLong(),
                title = "Track $i - Synthetic Sound",
                artistId = (i % artists.size).toLong() + 1,
                artistName = artist,
                albumId = (i % albums.size).toLong() + 1,
                albumName = album,
                genreId = (i % genres.size).toLong() + 1,
                genreName = genre,
                duration = 180000L + (i % 120) * 1000L,
                trackNumber = (i % 12) + 1,
                discNumber = 1,
                year = 1980 + (i % 45),
                contentUri = "content://media/external/audio/media/$i",
                mimeType = mime,
                size = 5000000L,
                bitrate = if (mime == "audio/flac") 0 else 320,
                sampleRate = 44100,
                channelCount = 2,
                dateAdded = 1600000000L + i * 100,
                dateModified = 1600000000L + i * 100,
                albumArtUri = if (i % 2 == 0) "content://art/$i" else null
            )
        }
    }

    @Test
    fun recommendationGeneration_performance_underBudget() {
        val library = generateSyntheticLibrary(5000)

        val profile = RecommendationProfile(
            artistPlayCounts = (1..20).associate { "Artist $it" to (25 - it) },
            genrePlayCounts = mapOf("rock" to 100, "pop" to 80, "jazz" to 50),
            songPlayCounts = (1L..200L).associateWith { (200 - it).toInt() },
            songCompletionRatios = (1L..200L).associateWith { 0.85f },
            recentSongTimestamps = (1L..50L).associateWith { System.currentTimeMillis() - it * 3600 * 1000L },
            favoriteSongIds = (1L..50L).toSet(),
            favoriteArtistNames = setOf("Artist 1", "Artist 2", "Artist 3"),
            forgottenFavoriteIds = (40L..50L).toSet(),
            playlistSongPairs = (1L..30L).associateWith { setOf(it + 1, it + 2) },
            unplayedSongIds = (1000L..5000L).toSet(),
            totalPlays = 500
        )

        // Warmup
        RecommendationEngine.generateSections(library.take(200), profile)

        val durationMs = measureTimeMillis {
            val sections = RecommendationEngine.generateSections(library, profile)
            assertTrue("Expected generated recommendation sections", sections.isNotEmpty())
            assertTrue("Expected Made For You section", sections.any { it.title == "Made For You" })
        }

        println("Full recommendation generation for 5,000 tracks took: ${durationMs}ms")
        assertTrue("Recommendation generation took too long: ${durationMs}ms (budget is 250ms)", durationMs < 250)
    }
}
