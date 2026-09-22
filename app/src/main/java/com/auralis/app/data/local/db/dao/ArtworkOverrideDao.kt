package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auralis.app.data.local.db.entity.ArtworkOverrideEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for managing persistent custom artwork overrides across entities.
 */
@Dao
interface ArtworkOverrideDao {

    @Query("SELECT * FROM artwork_overrides WHERE entityType = :entityType AND entityKey = :entityKey LIMIT 1")
    fun getOverride(entityType: String, entityKey: String): Flow<ArtworkOverrideEntity?>

    @Query("SELECT * FROM artwork_overrides WHERE entityType = :entityType AND entityKey = :entityKey LIMIT 1")
    suspend fun getOverrideSync(entityType: String, entityKey: String): ArtworkOverrideEntity?

    @Query("SELECT * FROM artwork_overrides")
    fun getAllOverridesFlow(): Flow<List<ArtworkOverrideEntity>>

    @Query("SELECT * FROM artwork_overrides")
    suspend fun getAllOverrides(): List<ArtworkOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(override: ArtworkOverrideEntity): Long

    @Query("DELETE FROM artwork_overrides WHERE entityType = :entityType AND entityKey = :entityKey")
    suspend fun deleteOverride(entityType: String, entityKey: String): Int

    @Query("DELETE FROM artwork_overrides WHERE id = :id")
    suspend fun deleteOverrideById(id: Long): Int

    @Query("DELETE FROM artwork_overrides")
    suspend fun deleteAllOverrides()
}
