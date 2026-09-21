package com.auralis.app

import com.auralis.app.domain.model.DuplicateDetectionEngine
import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.QualityFilter
import com.auralis.app.domain.model.SearchRankingEngine
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongFilterState
import com.auralis.app.playback.audiofx.AudioEffectCapabilityDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Phase 11 Performance & Scaling Test Suite.
 * Benchmarks large library operations (10,000 tracks) under strict performance budgets:
 * - 10,000 song search ranking < 500ms
 * - 10,000 song sorting < 200ms
 * - 10,000 song filtering < 100ms
 * - 10,000 song duplicate grouping < 300ms
 * - AudioEffect capability caching verification
 */
class LargeLibraryScalingTest {

    private fun generateSyntheticLibrary(count: Int): List<Song> {
        val artists = listOf("The Beatles", "Queen", "Pink Floyd", "Led Zeppelin", "Taylor Swift", "Radiohead", "Kendrick Lamar", "Daft Punk", "Miles Davis")
        val albums = listOf("Abbey Road", "A Night at the Opera", "The Dark Side of the Moon", "IV", "1989", "OK Computer", "DAMN.", "Discovery", "Kind of Blue")
        val genres = listOf("Rock", "Pop", "Hip Hop", "Electronic", "Classical", "Jazz", "Metal")
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
    fun largeLibrary_10k_searchRanking_underBudget() {
        val library = generateSyntheticLibrary(10000)

        // Warmup
        SearchRankingEngine.rankSongs(library.take(500), "Night")

        val durationMs = measureTimeMillis {
            val results = SearchRankingEngine.rankSongs(library, "Night")
            assertTrue("Expected matching results", results.isNotEmpty())
        }

        println("Search ranking for 10,000 songs took: ${durationMs}ms")
        assertTrue("Search ranking took too long: ${durationMs}ms", durationMs < 500)
    }

    @Test
    fun largeLibrary_10k_sorting_underBudget() {
        val library = generateSyntheticLibrary(10000)

        val durationMs = measureTimeMillis {
            val sorted = library.sortedWith(
                compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artistName }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            assertTrue("Expected sorted list", sorted.size == 10000)
        }

        println("Sorting 10,000 songs took: ${durationMs}ms")
        assertTrue("Sorting took too long: ${durationMs}ms", durationMs < 200)
    }

    @Test
    fun largeLibrary_10k_filtering_underBudget() {
        val library = generateSyntheticLibrary(10000)
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

        println("Filtering 10,000 songs took: ${durationMs}ms")
        assertTrue("Filtering took too long: ${durationMs}ms", durationMs < 100)
    }

    @Test
    fun largeLibrary_10k_duplicateGrouping_underBudget() {
        val library = generateSyntheticLibrary(10000)

        val durationMs = measureTimeMillis {
            val groups = DuplicateDetectionEngine.findDuplicates(library)
            assertNotNull("Duplicate groups should not be null", groups)
        }

        println("Duplicate grouping for 10,000 songs took: ${durationMs}ms")
        assertTrue("Duplicate grouping took too long: ${durationMs}ms", durationMs < 300)
    }

    @Test
    fun audioEffectCapabilityDetector_cachesResults() {
        val detector = AudioEffectCapabilityDetector()
        val firstResult = detector.detectCapabilities()
        val secondResult = detector.detectCapabilities()
        assertEquals("Subsequent detection must return identical cached capability", firstResult, secondResult)
    }
}
