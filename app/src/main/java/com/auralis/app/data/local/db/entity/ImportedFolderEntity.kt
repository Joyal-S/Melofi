package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted entity representing a user-selected music folder imported via Android Storage Access Framework (SAF).
 *
 * Stores the persistable tree URI, user-facing folder name, permission validity, and track count.
 * Enables persistent folder scans across application restarts without re-prompting the user.
 */
@Entity(
    tableName = "imported_folders",
    indices = [
        Index(value = ["treeUri"], unique = true)
    ]
)
data class ImportedFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treeUri: String,
    val displayName: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val isPermissionValid: Boolean = true,
    val songCount: Int = 0
)
