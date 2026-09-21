package com.auralis.app.domain.model

import java.util.Locale

/**
 * Deterministic local search ranking engine.
 *
 * Implements weighted scoring based on exact matching, prefix matching, word boundary
 * matching, and metadata field relevance without remote queries or machine learning.
 */
object SearchRankingEngine {

    private val PUNCTUATION_REGEX = Regex("""[^\p{L}\p{Nd}\s]""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    /**
     * Normalizes an input string for resilient matching: lowercases, replaces punctuation with space,
     * collapses whitespace, and trims.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val decomposed = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{M}"""), "")
        return decomposed
            .replace(PUNCTUATION_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Evaluates a relevance score for a given [Song] against the query string.
     * Returns 0 if the track does not match any searchable metadata field.
     */
    fun scoreSong(song: Song, query: String, precomputedNormQuery: String? = null): Int {
        val q = precomputedNormQuery ?: normalize(query)
        if (q.isEmpty()) return 0

        var score = 0

        val title = normalize(song.title)
        val artist = normalize(song.artistName)
        val album = normalize(song.albumName)
        val genre = normalize(song.genreName)

        // 1. Title Scoring (Highest relevance)
        when {
            title == q -> score += 1000
            title.startsWith(q) -> score += 500
            title.contains(" $q") -> score += 300
            title.contains(q) -> score += 200
        }

        // 2. Artist Scoring
        when {
            artist == q -> score += 400
            artist.startsWith(q) -> score += 250
            artist.contains(" $q") -> score += 150
            artist.contains(q) -> score += 100
        }

        // 3. Album Scoring
        when {
            album == q -> score += 250
            album.startsWith(q) -> score += 150
            album.contains(" $q") -> score += 100
            album.contains(q) -> score += 80
        }

        // 4. Genre Scoring
        if (genre.isNotEmpty()) {
            when {
                genre == q -> score += 100
                genre.startsWith(q) -> score += 70
                genre.contains(q) -> score += 40
            }
        }

        return score
    }

    /**
     * Filters and ranks songs according to relevance against the query.
     */
    fun rankSongs(songs: List<Song>, query: String): List<Song> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()

        return songs
            .mapNotNull { song ->
                val score = scoreSong(song, query, q)
                if (score > 0) song to score else null
            }
            .sortedWith(
                compareByDescending<Pair<Song, Int>> { it.second }
                    .thenBy { it.first.title.lowercase(Locale.ROOT) }
            )
            .map { it.first }
    }

    /**
     * Filters and ranks albums according to relevance against the query.
     */
    fun rankAlbums(albums: List<Album>, query: String): List<Album> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()

        return albums
            .mapNotNull { album ->
                val name = normalize(album.name)
                val artist = normalize(album.artistName)
                var score = 0

                when {
                    name == q -> score += 1000
                    name.startsWith(q) -> score += 500
                    name.contains(" $q") -> score += 300
                    name.contains(q) -> score += 200
                }

                when {
                    artist == q -> score += 300
                    artist.startsWith(q) -> score += 150
                    artist.contains(q) -> score += 80
                }

                if (score > 0) album to score else null
            }
            .sortedWith(
                compareByDescending<Pair<Album, Int>> { it.second }
                    .thenBy { it.first.name.lowercase(Locale.ROOT) }
            )
            .map { it.first }
    }

    /**
     * Filters and ranks artists according to relevance against the query.
     */
    fun rankArtists(artists: List<Artist>, query: String): List<Artist> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()

        return artists
            .mapNotNull { artist ->
                val name = normalize(artist.name)
                var score = 0

                when {
                    name == q -> score += 1000
                    name.startsWith(q) -> score += 500
                    name.contains(" $q") -> score += 300
                    name.contains(q) -> score += 200
                }

                if (score > 0) artist to score else null
            }
            .sortedWith(
                compareByDescending<Pair<Artist, Int>> { it.second }
                    .thenBy { it.first.name.lowercase(Locale.ROOT) }
            )
            .map { it.first }
    }

    /**
     * Filters and ranks genres according to relevance against the query.
     */
    fun rankGenres(genres: List<Genre>, query: String): List<Genre> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()

        return genres
            .mapNotNull { genre ->
                val name = normalize(genre.name)
                var score = 0

                when {
                    name == q -> score += 1000
                    name.startsWith(q) -> score += 500
                    name.contains(" $q") -> score += 300
                    name.contains(q) -> score += 200
                }

                if (score > 0) genre to score else null
            }
            .sortedWith(
                compareByDescending<Pair<Genre, Int>> { it.second }
                    .thenBy { it.first.name.lowercase(Locale.ROOT) }
            )
            .map { it.first }
    }
}
