package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auralis.app.data.local.db.entity.FavoriteEntity
import com.auralis.app.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for user favorites.
 */
@Dao
interface FavoriteDao {

    @Query("SELECT songId FROM favorites")
    fun getFavoriteSongIds(): Flow<List<Long>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN favorites f ON s.id = f.songId
        ORDER BY f.addedAt DESC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun deleteFavorite(songId: Long)

    @Query("DELETE FROM favorites WHERE songId IN (:songIds)")
    suspend fun deleteBySongIds(songIds: List<Long>)
}
