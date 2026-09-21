package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.auralis.app.data.local.db.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for albums.
 */
@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    fun getAlbumById(id: Long): Flow<AlbumEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()

    @Transaction
    suspend fun syncAlbums(albums: List<AlbumEntity>) {
        deleteAllAlbums()
        if (albums.isNotEmpty()) {
            insertAlbums(albums)
        }
    }
}
