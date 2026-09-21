package com.auralis.app.domain.model

/**
 * Explainable reason for a recommended song.
 * Guaranteed to reflect an actual calculated scoring signal without synthetic or fake explanations.
 */
sealed class RecommendationReason {
    abstract val displayText: String

    data class BecauseYouListenTo(val artistName: String) : RecommendationReason() {
        override val displayText: String = "Because you listen to $artistName often"
    }

    data class BasedOnGenre(val genreName: String) : RecommendationReason() {
        override val displayText: String = "Based on your interest in $genreName"
    }

    data object ForgottenFavorite : RecommendationReason() {
        override val displayText: String = "Forgotten favorite • Not played recently"
    }

    data object FavoriteTrack : RecommendationReason() {
        override val displayText: String = "From your favorites"
    }

    data object DiscoverNew : RecommendationReason() {
        override val displayText: String = "Unplayed gem from your library"
    }

    data object SimilarToFavorites : RecommendationReason() {
        override val displayText: String = "Similar to your favorite tracks"
    }

    data object PlaylistCoOccurrence : RecommendationReason() {
        override val displayText: String = "Often grouped in your playlists"
    }

    data object FrequentlyPlayed : RecommendationReason() {
        override val displayText: String = "One of your most played tracks"
    }

    data object RecentlyEnjoyed : RecommendationReason() {
        override val displayText: String = "Recently played & enjoyed"
    }

    data object ExploreLibrary : RecommendationReason() {
        override val displayText: String = "Explore from your local collection"
    }
}

/**
 * An individual recommendation item with its computed relevance score,
 * primary reason, and granular scoring breakdown for transparency.
 */
data class RecommendationItem(
    val song: Song,
    val score: Float,
    val reason: RecommendationReason,
    val scoreBreakdown: Map<String, Float> = emptyMap()
)

/**
 * Types of recommendation sections.
 */
enum class RecommendationSectionType(val id: String) {
    MADE_FOR_YOU("made_for_you"),
    BECAUSE_YOU_LISTEN_TO("because_you_listen_to"),
    DISCOVER_NEW("discover_new"),
    FORGOTTEN_FAVORITES("forgotten_favorites"),
    FROM_PLAYLISTS("from_playlists"),
    RECENTLY_ENJOYED("recently_enjoyed"),
    EXPLORE_COLLECTION("explore_collection")
}

/**
 * A categorized collection of recommendations displayed to the user.
 */
data class RecommendationSection(
    val type: RecommendationSectionType,
    val title: String,
    val subtitle: String,
    val items: List<RecommendationItem>
)

/**
 * Transient user taste profile aggregated on-the-fly from local database signals.
 * Never stored as a permanent personal profile entity, adhering strictly to Melofy's
 * privacy-first offline architecture.
 */
data class RecommendationProfile(
    val artistPlayCounts: Map<String, Int> = emptyMap(),
    val genrePlayCounts: Map<String, Int> = emptyMap(),
    val songPlayCounts: Map<Long, Int> = emptyMap(),
    val songCompletionRatios: Map<Long, Float> = emptyMap(),
    val recentSongTimestamps: Map<Long, Long> = emptyMap(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val favoriteArtistNames: Set<String> = emptySet(),
    val forgottenFavoriteIds: Set<Long> = emptySet(),
    val playlistSongPairs: Map<Long, Set<Long>> = emptyMap(),
    val unplayedSongIds: Set<Long> = emptySet(),
    val totalPlays: Int = 0
)
