package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auralis.app.data.local.db.entity.PlaybackHistoryEntity
import com.auralis.app.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight projection for top song statistics.
 */
data class TopSongItemEntity(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumArtUri: String?,
    val duration: Long,
    val playCount: Long,
    val totalDurationMs: Long
)

/**
 * Lightweight projection for top artist statistics.
 */
data class TopArtistItemEntity(
    val artistName: String,
    val playCount: Long,
    val totalDurationMs: Long
)

/**
 * Lightweight projection for top album statistics.
 */
data class TopAlbumItemEntity(
    val albumName: String,
    val artistName: String,
    val albumArtUri: String?,
    val playCount: Long,
    val totalDurationMs: Long
)

/**
 * Room DAO for listening history, playback session tracking, and high-performance SQL statistics.
 */
@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: PlaybackHistoryEntity): Long

    @Query("UPDATE playback_history SET durationMs = :durationMs, completed = :completed WHERE id = :id")
    suspend fun updateHistoryDurationAndCompletion(id: Long, durationMs: Long, completed: Boolean)

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistoryEntries(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecentHistoryEntriesList(limit: Int): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history WHERE songId = :songId ORDER BY playedAt DESC")
    fun getHistoryForSong(songId: Long): Flow<List<PlaybackHistoryEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT songId, MAX(playedAt) AS maxPlayedAt
            FROM playback_history
            GROUP BY songId
        ) latest ON s.id = latest.songId
        ORDER BY latest.maxPlayedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedSongs(limit: Int): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT songId, COUNT(id) AS playCount
            FROM playback_history
            WHERE playedAt >= :sinceTimestamp
            GROUP BY songId
        ) counts ON s.id = counts.songId
        ORDER BY counts.playCount DESC, s.title COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun getMostPlayedSongs(sinceTimestamp: Long = 0L, limit: Int = 50): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        LEFT JOIN playback_history h ON s.id = h.songId
        WHERE h.id IS NULL
        ORDER BY s.title COLLATE NOCASE ASC
        LIMIT :limit
    """)
    fun getNeverPlayedSongs(limit: Int = 100): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN favorites f ON s.id = f.songId
        LEFT JOIN (
            SELECT songId, MAX(playedAt) AS maxPlayedAt
            FROM playback_history
            GROUP BY songId
        ) h ON s.id = h.songId
        WHERE h.maxPlayedAt IS NULL OR h.maxPlayedAt < :cutoffTimestamp
        ORDER BY f.addedAt DESC
        LIMIT :limit
    """)
    fun getForgottenFavorites(cutoffTimestamp: Long, limit: Int = 50): Flow<List<SongEntity>>

    // ============================================================
    // Statistics SQL Aggregations
    // ============================================================

    @Query("SELECT COUNT(*) FROM playback_history WHERE playedAt >= :sinceTimestamp")
    fun getTotalPlayCount(sinceTimestamp: Long = 0L): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM playback_history WHERE playedAt >= :sinceTimestamp")
    fun getTotalListeningDurationMs(sinceTimestamp: Long = 0L): Flow<Long>

    @Query("SELECT COUNT(DISTINCT songId) FROM playback_history WHERE playedAt >= :sinceTimestamp")
    fun getUniqueSongsPlayedCount(sinceTimestamp: Long = 0L): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT s.artistName) FROM playback_history h
        INNER JOIN songs s ON h.songId = s.id
        WHERE h.playedAt >= :sinceTimestamp
    """)
    fun getUniqueArtistsPlayedCount(sinceTimestamp: Long = 0L): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT s.albumName) FROM playback_history h
        INNER JOIN songs s ON h.songId = s.id
        WHERE h.playedAt >= :sinceTimestamp
    """)
    fun getUniqueAlbumsPlayedCount(sinceTimestamp: Long = 0L): Flow<Int>

    @Query("""
        SELECT s.id, s.title, s.artistName, s.albumName, s.albumArtUri, s.duration,
               COUNT(h.id) AS playCount,
               SUM(h.durationMs) AS totalDurationMs
        FROM songs s
        INNER JOIN playback_history h ON s.id = h.songId
        WHERE h.playedAt >= :sinceTimestamp
        GROUP BY s.id
        ORDER BY playCount DESC, totalDurationMs DESC
        LIMIT :limit
    """)
    fun getTopSongs(sinceTimestamp: Long = 0L, limit: Int = 10): Flow<List<TopSongItemEntity>>

    @Query("""
        SELECT s.artistName,
               COUNT(h.id) AS playCount,
               SUM(h.durationMs) AS totalDurationMs
        FROM songs s
        INNER JOIN playback_history h ON s.id = h.songId
        WHERE h.playedAt >= :sinceTimestamp
        GROUP BY s.artistName
        ORDER BY playCount DESC, totalDurationMs DESC
        LIMIT :limit
    """)
    fun getTopArtists(sinceTimestamp: Long = 0L, limit: Int = 10): Flow<List<TopArtistItemEntity>>

    @Query("""
        SELECT s.albumName, s.artistName, s.albumArtUri,
               COUNT(h.id) AS playCount,
               SUM(h.durationMs) AS totalDurationMs
        FROM songs s
        INNER JOIN playback_history h ON s.id = h.songId
        WHERE h.playedAt >= :sinceTimestamp
        GROUP BY s.albumName, s.artistName
        ORDER BY playCount DESC, totalDurationMs DESC
        LIMIT :limit
    """)
    fun getTopAlbums(sinceTimestamp: Long = 0L, limit: Int = 10): Flow<List<TopAlbumItemEntity>>

    // ============================================================
    // Retention & Cleanup
    // ============================================================

    @Query("""
        DELETE FROM playback_history
        WHERE id NOT IN (
            SELECT id FROM playback_history
            ORDER BY playedAt DESC
            LIMIT :maxEntries
        )
    """)
    suspend fun pruneHistory(maxEntries: Int = 10000)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)
}
