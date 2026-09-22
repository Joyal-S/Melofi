package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an offline custom artwork override for any supported entity
 * (Song, Album, Artist, Genre, or Playlist).
 *
 * Persisted in a dedicated table to ensure custom artwork assignments survive
 * dynamic MediaStore scans and library aggregations.
 */
@Entity(
    tableName = "artwork_overrides",
    indices = [
        Index(value = ["entityType", "entityKey"], unique = true)
    ]
)
data class ArtworkOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityKey: String,
    val localPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
