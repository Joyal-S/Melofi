package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auralis.app.data.local.db.entity.SongDefaultCoverEntity

@Dao
interface SongDefaultCoverDao {

    @Query("SELECT * FROM song_default_covers WHERE songId = :songId")
    suspend fun getCoverBySongId(songId: Long): SongDefaultCoverEntity?

    @Query("SELECT * FROM song_default_covers")
    suspend fun getAllCovers(): List<SongDefaultCoverEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCover(entity: SongDefaultCoverEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCovers(entities: List<SongDefaultCoverEntity>)

    @Query("DELETE FROM song_default_covers WHERE songId = :songId")
    suspend fun deleteCover(songId: Long)
}
