package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.auralis.app.data.local.db.entity.GenreEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for genres.
 */
@Dao
interface GenreDao {

    @Query("SELECT * FROM genres ORDER BY name COLLATE NOCASE ASC")
    fun getAllGenres(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM genres WHERE id = :id")
    fun getGenreById(id: Long): Flow<GenreEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenres(genres: List<GenreEntity>)

    @Query("DELETE FROM genres")
    suspend fun deleteAllGenres()

    @Transaction
    suspend fun syncGenres(genres: List<GenreEntity>) {
        deleteAllGenres()
        if (genres.isNotEmpty()) {
            insertGenres(genres)
        }
    }
}
