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

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun findSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE (albumId IS NOT NULL AND albumId = :albumId) OR albumName = :albumName ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByAlbum(albumId: Long, albumName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE (artistId IS NOT NULL AND artistId = :artistId) OR (artistId IS NULL AND (artistName = :artistName OR (:originalName IS NOT NULL AND artistName = :originalName))) ORDER BY year DESC, albumName COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByArtistWithOriginal(artistId: Long, artistName: String, originalName: String?): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY year DESC, albumName COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByArtistId(artistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE (artistId IS NOT NULL AND artistId = :artistId) OR (artistId IS NULL AND artistName = :artistName) ORDER BY year DESC, albumName COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByArtist(artistId: Long, artistName: String): Flow<List<SongEntity>>

    @Query("UPDATE songs SET artistId = :targetArtistId, artistName = :targetArtistName WHERE artistId = :sourceArtistId")
    suspend fun reassignSongsArtist(sourceArtistId: Long, targetArtistId: Long, targetArtistName: String): Int

    @Query("UPDATE songs SET artistId = :artistId, artistName = :artistName, dateModified = :dateModified WHERE id = :songId")
    suspend fun updateSongArtist(songId: Long, artistId: Long, artistName: String, dateModified: Long): Int

    @Query("SELECT * FROM songs WHERE genreName = :genreName ORDER BY artistName COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    fun getSongsByGenre(genreName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%' OR albumName LIKE '%' || :query || '%' OR genreName LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<SongEntity>

    @Query("SELECT id, dateModified FROM songs")
    suspend fun getAllSongSyncInfo(): List<SongSyncInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>
}
