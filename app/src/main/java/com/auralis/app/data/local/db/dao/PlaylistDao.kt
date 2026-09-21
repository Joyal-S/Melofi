package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.auralis.app.data.local.db.entity.PlaylistEntity
import com.auralis.app.data.local.db.entity.PlaylistSongCrossRef
import com.auralis.app.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight projection for playlists with active member song count.
 */
data class PlaylistWithCountEntity(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int
)

/**
 * Room DAO for playlists, membership, and deterministic ordering.
 */
@Dao
interface PlaylistDao {

    @Query("""
        SELECT p.id, p.name, p.createdAt, p.updatedAt,
               COUNT(ps.songId) AS songCount
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        GROUP BY p.id
        ORDER BY p.updatedAt DESC
    """)
    fun getPlaylistsWithCount(): Flow<List<PlaylistWithCountEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchPlaylist(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Query("SELECT position FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun getSongPosition(playlistId: Long, songId: Long): Int?

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deletePlaylistSong(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE songId IN (:songIds)")
    suspend fun deletePlaylistSongsBySongIds(songIds: List<Long>)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    @Query("""
        UPDATE playlist_songs
        SET position = position - 1
        WHERE playlistId = :playlistId AND position > :deletedPosition
    """)
    suspend fun compactPositionsAfterDelete(playlistId: Long, deletedPosition: Int)

    @Transaction
    suspend fun removeSongAndCompact(playlistId: Long, songId: Long) {
        val position = getSongPosition(playlistId, songId) ?: return
        deletePlaylistSong(playlistId, songId)
        compactPositionsAfterDelete(playlistId, position)
        touchPlaylist(playlistId)
    }

    @Transaction
    suspend fun addSongToEnd(playlistId: Long, songId: Long): Boolean {
        val currentMax = getMaxPosition(playlistId)
        val nextPosition = if (currentMax != null) currentMax + 1 else 0
        val crossRef = PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = songId,
            position = nextPosition
        )
        insertPlaylistSong(crossRef)
        touchPlaylist(playlistId)
        return true
    }

    @Transaction
    suspend fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>) {
        for ((index, songId) in orderedSongIds.withIndex()) {
            updateSongPosition(playlistId, songId, index)
        }
        touchPlaylist(playlistId)
    }
}
