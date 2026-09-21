package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.auralis.app.data.local.db.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for artists.
 */
@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun getArtistById(id: Long): Flow<ArtistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists()

    @Transaction
    suspend fun syncArtists(artists: List<ArtistEntity>) {
        deleteAllArtists()
        if (artists.isNotEmpty()) {
            insertArtists(artists)
        }
    }
}
