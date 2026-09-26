package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.auralis.app.data.local.db.entity.ExcludedFolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for folder-based library exclusions.
 */
@Dao
interface ExcludedFolderDao {

    @Query("SELECT * FROM excluded_folders ORDER BY isDefault DESC, folderName ASC")
    fun getAllExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT * FROM excluded_folders WHERE isEnabled = 1")
    suspend fun getActiveExcludedFolders(): List<ExcludedFolderEntity>

    @Query("SELECT folderPath FROM excluded_folders WHERE isEnabled = 1")
    suspend fun getActiveExcludedFolderPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcludedFolder(entity: ExcludedFolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultFolders(folders: List<ExcludedFolderEntity>)

    @Update
    suspend fun updateExcludedFolder(entity: ExcludedFolderEntity)

    @Query("UPDATE excluded_folders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleFolder(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM excluded_folders WHERE id = :id")
    suspend fun deleteExcludedFolder(id: Long)

    @Query("SELECT COUNT(*) FROM excluded_folders")
    suspend fun getFolderCount(): Int
}
