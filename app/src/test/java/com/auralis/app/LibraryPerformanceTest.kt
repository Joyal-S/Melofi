package com.auralis.app

import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.QualityFilter
import com.auralis.app.domain.model.SearchRankingEngine
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongFilterState
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.system.measureTimeMillis

/**
 * Synthetic performance benchmark testing search ranking, filtering, and sorting
 * on a simulated local library of 1,000 to 5,000 tracks.
 */
class LibraryPerformanceTest {

    private fun generateSyntheticLibrary(count: Int): List<Song> {
        val artists = listOf("The Beatles", "Queen", "Pink Floyd", "Led Zeppelin", "Taylor Swift", "Radiohead", "Kendrick Lamar")
        val albums = listOf("Abbey Road", "A Night at the Opera", "The Dark Side of the Moon", "IV", "1989", "OK Computer", "DAMN.")
        val genres = listOf("Rock", "Pop", "Hip Hop", "Electronic", "Classical", "Jazz")
        val formats = listOf("audio/mpeg", "audio/flac", "audio/x-wav", "audio/aac")

        return (1..count).map { i ->
            val artist = artists[i % artists.size]
            val album = albums[i % albums.size]
            val genre = genres[i % genres.size]
            val mime = formats[i % formats.size]

            Song(
                id = i.toLong(),
                title = "Track $i - Melody of the Night",
                artistId = (i % artists.size).toLong() + 1,
                artistName = artist,
                albumId = (i % albums.size).toLong() + 1,
                albumName = album,
                genreId = (i % genres.size).toLong() + 1,
                genreName = genre,
                duration = 120000L + (i % 300) * 1000L,
                trackNumber = (i % 12) + 1,
                discNumber = 1,
                year = 1970 + (i % 55),
                contentUri = "content://media/external/audio/media/$i",
                mimeType = mime,
                size = 3000000L + (i % 10000000),
                bitrate = if (mime == "audio/flac") 0 else 320,
                sampleRate = 44100,
                channelCount = 2,
                dateAdded = 1600000000L + i * 100,
                dateModified = 1600000000L + i * 100,
                albumArtUri = if (i % 3 == 0) null else "content://art/$i"
            )
        }
    }

    @Test
    fun searchRanking_performance_underBudget() {
        val library = generateSyntheticLibrary(5000)

        // Warmup
        SearchRankingEngine.rankSongs(library.take(500), "Night")

        val durationMs = measureTimeMillis {
            val results = SearchRankingEngine.rankSongs(library, "Night")
            assertTrue("Expected matching results", results.isNotEmpty())
        }

        println("Search ranking for 5,000 songs took: ${durationMs}ms")
        assertTrue("Search ranking took too long: ${durationMs}ms", durationMs < 500)
    }

    @Test
    fun filtering_performance_underBudget() {
        val library = generateSyntheticLibrary(5000)
        val filter = SongFilterState(
            formatFilter = FormatFilter.MP3,
            qualityFilter = QualityFilter.HIGH_QUALITY,
            durationFilter = DurationFilter.MEDIUM
        )

        // Warmup
        library.take(500).filter { filter.matches(it, false) }

        val durationMs = measureTimeMillis {
            val filtered = library.filter { filter.matches(it, false) }
            assertTrue("Expected filtered results", filtered.isNotEmpty())
        }

        println("Filtering 5,000 songs took: ${durationMs}ms")
        assertTrue("Filtering took too long: ${durationMs}ms", durationMs < 200)
    }

    @Test
    fun sorting_performance_underBudget() {
        val library = generateSyntheticLibrary(5000)

        val durationMs = measureTimeMillis {
            val sorted = library.sortedWith(
                compareBy<Song> { it.artistName.lowercase(Locale.ROOT) }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            assertTrue("Expected sorted list", sorted.size == library.size)
        }

        println("Sorting 5,000 songs took: ${durationMs}ms")
        assertTrue("Sorting took too long: ${durationMs}ms", durationMs < 200)
    }
}
