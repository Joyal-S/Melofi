package com.auralis.app.domain.model

import java.util.Locale
import kotlin.math.abs

/**
 * Confidence level of a duplicate match between tracks.
 */
enum class DuplicateConfidence {
    EXACT,       // Identical title, artist, file size, and duration
    VERY_LIKELY, // Identical title and artist, duration within 2s, differing bitrate/encoding
    POSSIBLE     // Matching title and artist with minor duration variance (up to 15s) or matching title/duration with different artist
}

/**
 * A group of potential duplicate audio tracks detected in the local library.
 */
data class DuplicateGroup(
    val id: String,
    val title: String,
    val artist: String,
    val confidence: DuplicateConfidence,
    val songs: List<Song>
)

/**
 * Multi-signal algorithmic duplicate detection engine.
 *
 * Employs normalized string parsing, audio duration tolerance, and file size checks
 * to group duplicate candidates without relying on cloud services or destructive actions.
 */
object DuplicateDetectionEngine {

    private val TITLE_CLEANUP_REGEX = Regex(
        """(?i)\s*[\(\[\{](?:official\s*(?:video|audio|music\s*video|lyric\s*video)?|remaster(?:ed)?(?:\s*\d{4})?|live|deluxe|bonus(?:\s*track)?|radio\s*edit|extended(?:\s*mix)?|feat\.?.*?)[\)\]\}]"""
    )
    private val FEAT_ARTIST_REGEX = Regex("""(?i)\s+(?:feat\.?|ft\.?|featuring|with|vs\.?)\s+.*""")
    private val PUNCTUATION_REGEX = Regex("""[^\p{L}\p{Nd}\s]""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    /**
     * Finds potential duplicate tracks in the provided song library.
     * Uses candidate grouping to achieve O(N log N) performance suitable for large libraries.
     */
    fun findDuplicates(songs: List<Song>): List<DuplicateGroup> {
        if (songs.size < 2) return emptyList()

        // 1. Group candidates by normalized base title to avoid O(N^2) search
        val candidateBuckets = HashMap<String, MutableList<Song>>()
        for (song in songs) {
            val normalizedTitle = normalizeTitle(song.title)
            if (normalizedTitle.isNotEmpty()) {
                candidateBuckets.getOrPut(normalizedTitle) { mutableListOf() }.add(song)
            }
        }

        val duplicateGroups = mutableListOf<DuplicateGroup>()

        // 2. Evaluate candidate buckets with 2 or more songs
        for ((normTitle, bucket) in candidateBuckets) {
            if (bucket.size < 2) continue

            val visited = BooleanArray(bucket.size)

            for (i in 0 until bucket.size) {
                if (visited[i]) continue
                val base = bucket[i]
                val matchedSongs = mutableListOf(base)
                var highestConfidence = DuplicateConfidence.POSSIBLE

                for (j in i + 1 until bucket.size) {
                    if (visited[j]) continue
                    val candidate = bucket[j]

                    val confidence = evaluateMatch(base, candidate, normTitle)
                    if (confidence != null) {
                        matchedSongs.add(candidate)
                        visited[j] = true
                        if (confidence.ordinal < highestConfidence.ordinal) {
                            highestConfidence = confidence
                        }
                    }
                }

                if (matchedSongs.size > 1) {
                    visited[i] = true
                    duplicateGroups.add(
                        DuplicateGroup(
                            id = "${base.id}_${matchedSongs.size}",
                            title = base.title,
                            artist = base.artistName,
                            confidence = highestConfidence,
                            songs = matchedSongs.sortedByDescending { it.size }
                        )
                    )
                }
            }
        }

        // Sort groups: EXACT first, then VERY_LIKELY, then POSSIBLE; secondary sort by title
        return duplicateGroups.sortedWith(
            compareBy<DuplicateGroup> { it.confidence.ordinal }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )
    }

    /**
     * Evaluates whether two songs constitute duplicates and returns their match confidence.
     */
    fun evaluateMatch(songA: Song, songB: Song, precomputedNormTitle: String? = null): DuplicateConfidence? {
        if (songA.id == songB.id) return null

        val normTitleA = precomputedNormTitle ?: normalizeTitle(songA.title)
        val normTitleB = precomputedNormTitle ?: normalizeTitle(songB.title)
        if (normTitleA != normTitleB) return null

        val normArtistA = normalizeArtist(songA.artistName)
        val normArtistB = normalizeArtist(songB.artistName)
        val sameArtist = normArtistA.isNotEmpty() && normArtistA == normArtistB
        val durationDiffMs = abs(songA.duration - songB.duration)
        val sameSize = songA.size > 0 && songA.size == songB.size

        // Exact Match: Identical size + duration within 1s + same artist
        if (sameSize && durationDiffMs <= 1000L && sameArtist) {
            return DuplicateConfidence.EXACT
        }

        // Very Likely: Same artist + duration within 2s (differing encoding / bitrates)
        if (sameArtist && durationDiffMs <= 2000L) {
            return DuplicateConfidence.VERY_LIKELY
        }

        // Possible Match: Same artist with duration variance up to 15s (e.g. edit / radio cut)
        if (sameArtist && durationDiffMs <= 15000L) {
            return DuplicateConfidence.POSSIBLE
        }

        // Possible Match: Same title + identical duration (within 1s) even if artist metadata differs
        if (durationDiffMs <= 1000L && songA.duration > 10000L) {
            return DuplicateConfidence.POSSIBLE
        }

        return null
    }

    /**
     * Normalizes a song title by removing bracketed noise, punctuation, and extra whitespace.
     */
    fun normalizeTitle(title: String): String {
        return title
            .replace(TITLE_CLEANUP_REGEX, "")
            .replace(PUNCTUATION_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Normalizes artist name by stripping featured collaborations and punctuation.
     */
    fun normalizeArtist(artist: String): String {
        return artist
            .replace(FEAT_ARTIST_REGEX, "")
            .replace(PUNCTUATION_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
            .lowercase(Locale.ROOT)
    }
}
