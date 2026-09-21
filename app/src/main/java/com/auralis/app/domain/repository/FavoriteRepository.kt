package com.auralis.app.domain.repository

import com.auralis.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing favorite tracks.
 */
interface FavoriteRepository {

    /**
     * Emits the reactive set of all favorited song IDs for O(1) in-memory check.
     */
    fun getFavoriteSongIds(): Flow<Set<Long>>

    /**
     * Emits the list of favorited songs ordered by addition date descending.
     */
    fun getFavoriteSongs(): Flow<List<Song>>

    /**
     * Checks whether a specific song is favorited.
     */
    suspend fun isFavorite(songId: Long): Boolean

    /**
     * Toggles the favorite status of a song, returning the new state.
     */
    suspend fun toggleFavorite(songId: Long): Boolean

    /**
     * Adds a song to favorites.
     */
    suspend fun addFavorite(songId: Long)

    /**
     * Removes a song from favorites.
     */
    suspend fun removeFavorite(songId: Long)
}
