package com.auralis.app.domain.model

/**
 * Categorized container for multi-entity local search results.
 */
data class SearchResultGroup(
    val query: String,
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val genres: List<Genre> = emptyList()
) {
    val totalResultsCount: Int
        get() = songs.size + artists.size + albums.size + genres.size

    val isEmpty: Boolean
        get() = totalResultsCount == 0
}

/**
 * Type categorization for search suggestion chips and autocomplete entries.
 */
enum class SearchSuggestionType {
    HISTORY,
    ARTIST,
    ALBUM,
    GENRE,
    SONG
}

/**
 * Single search suggestion derived strictly from local library metadata or history.
 */
data class SearchSuggestion(
    val text: String,
    val type: SearchSuggestionType,
    val subtitle: String? = null
)
