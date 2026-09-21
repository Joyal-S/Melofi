package com.auralis.app.domain.repository

import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing playlists, membership, and track ordering.
 */
interface PlaylistRepository {

    /**
     * Emits all playlists with updated track counts, ordered by most recently modified.
     */
    fun getPlaylists(): Flow<List<Playlist>>

    /**
     * Emits details for a specific playlist by ID.
     */
    fun getPlaylist(id: Long): Flow<Playlist?>

    /**
     * Emits the ordered list of songs belonging to a playlist.
     */
    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>>

    /**
     * Creates a new playlist with the validated name.
     */
    suspend fun createPlaylist(name: String): Result<Long>

    /**
     * Renames an existing playlist with the validated name.
     */
    suspend fun renamePlaylist(id: Long, name: String): Result<Unit>

    /**
     * Deletes a playlist and its memberships (does not delete library songs).
     */
    suspend fun deletePlaylist(id: Long): Result<Unit>

    /**
     * Appends a song to the end of the playlist.
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Result<Unit>

    /**
     * Removes a song from the playlist and compacts remaining positions.
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Result<Unit>

    /**
     * Reorders the playlist according to the provided list of song IDs.
     */
    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>): Result<Unit>
}
