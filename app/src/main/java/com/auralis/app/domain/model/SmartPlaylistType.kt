package com.auralis.app.domain.model

import androidx.annotation.DrawableRes
import com.auralis.app.R

/**
 * Deterministic rule-based smart playlists dynamically generated from local library and history data.
 */
enum class SmartPlaylistType(
    val id: String,
    val title: String,
    val description: String,
    @get:DrawableRes val iconRes: Int
) {
    RECENTLY_PLAYED(
        id = "recently_played",
        title = "Recently Played",
        description = "Songs you have listened to recently",
        iconRes = R.drawable.ic_history
    ),
    MOST_PLAYED(
        id = "most_played",
        title = "Most Played",
        description = "Your most frequently played tracks",
        iconRes = R.drawable.ic_trending_up
    ),
    NEVER_PLAYED(
        id = "never_played",
        title = "Never Played",
        description = "Songs in your library you haven't played yet",
        iconRes = R.drawable.ic_queue_music
    ),
    RECENTLY_ADDED(
        id = "recently_added",
        title = "Recently Added",
        description = "Songs recently added to your device",
        iconRes = R.drawable.ic_refresh
    ),
    FAVORITES(
        id = "smart_favorites",
        title = "Favorites",
        description = "All tracks you have marked as favorite",
        iconRes = R.drawable.ic_favorite
    ),
    FORGOTTEN_FAVORITES(
        id = "forgotten_favorites",
        title = "Forgotten Favorites",
        description = "Favorited songs you haven't played in over 30 days",
        iconRes = R.drawable.ic_favorite_border
    );

    companion object {
        fun fromId(id: String): SmartPlaylistType? {
            return entries.firstOrNull { it.id == id }
        }
    }
}
