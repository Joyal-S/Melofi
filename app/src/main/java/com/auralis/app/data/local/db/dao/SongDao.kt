package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.auralis.app.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight projection for differential library synchronization.
 */
data class SongSyncInfo(
    val id: Long,
    val dateModified: Long
)

/**
 * Room DAO for songs.
 */
@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getSongsSortedByTitle(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY artistName COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    fun getSongsSortedByArtist(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY albumName COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsSortedByAlbum(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getSongsSortedByDateAdded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongById(id: Long): Flow<SongEntity?>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<SongEntity>

    @Query("SELECT id, dateModified FROM songs")
    suspend fun getAllSongSyncInfo(): List<SongSyncInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>
}
