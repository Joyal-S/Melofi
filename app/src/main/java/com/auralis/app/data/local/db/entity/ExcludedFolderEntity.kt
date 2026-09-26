package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted entity representing a folder pattern excluded from the Melofi Music Library.
 *
 * Audio files residing within or beneath these folders are excluded from automatic library scans
 * unless explicitly imported into Melofi's deliberate music directory (Music/Melofy/).
 *
 * Configurable under Settings -> Library -> Music Sources.
 */
@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderName: String,
    val folderPath: String,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false
)
