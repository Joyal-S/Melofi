package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.auralis.app.data.local.db.entity.ImportedFolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for managing persisted user-imported SAF music folders.
 */
@Dao
interface ImportedFolderDao {

    @Query("SELECT * FROM imported_folders ORDER BY dateAdded DESC")
    fun getAllFoldersFlow(): Flow<List<ImportedFolderEntity>>

    @Query("SELECT * FROM imported_folders ORDER BY dateAdded DESC")
    suspend fun getAllFolders(): List<ImportedFolderEntity>

    @Query("SELECT * FROM imported_folders WHERE treeUri = :treeUri LIMIT 1")
    suspend fun findFolderByUri(treeUri: String): ImportedFolderEntity?

    @Query("SELECT * FROM imported_folders WHERE id = :id LIMIT 1")
    suspend fun findFolderById(id: Long): ImportedFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ImportedFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: ImportedFolderEntity)

    @Query("UPDATE imported_folders SET isPermissionValid = :isValid WHERE id = :id")
    suspend fun updatePermissionStatus(id: Long, isValid: Boolean)

    @Query("UPDATE imported_folders SET songCount = :songCount WHERE id = :id")
    suspend fun updateSongCount(id: Long, songCount: Int)

    @Query("DELETE FROM imported_folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}
