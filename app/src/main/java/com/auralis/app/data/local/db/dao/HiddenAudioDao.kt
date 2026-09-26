package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auralis.app.data.local.db.entity.HiddenAudioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for user-hidden audio records.
 */
@Dao
interface HiddenAudioDao {

    @Query("SELECT * FROM hidden_audio ORDER BY hiddenAt DESC")
    fun getAllHiddenAudio(): Flow<List<HiddenAudioEntity>>

    @Query("SELECT id FROM hidden_audio")
    suspend fun getAllHiddenIds(): List<Long>

    @Query("SELECT contentUri FROM hidden_audio")
    suspend fun getAllHiddenContentUris(): List<String>

    @Query("SELECT id FROM hidden_audio")
    fun getHiddenIdsFlow(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenAudio(entity: HiddenAudioEntity)

    @Query("DELETE FROM hidden_audio WHERE id = :id")
    suspend fun deleteHiddenAudio(id: Long)

    @Query("DELETE FROM hidden_audio")
    suspend fun clearAllHiddenAudio()

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_audio WHERE id = :id)")
    suspend fun isHidden(id: Long): Boolean
}
