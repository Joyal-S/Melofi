package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent Room entity representing an artist profile with a stable identity.
 *
 * Guarantees that:
 * - [id] (artistId) serves as the persistent, immutable library identity
 * - [normalizedName] enforces uniqueness at the database level via a UNIQUE index
 * - Artist profile identity is decoupled from individual file metadata
 * - Songs link persistently via [SongEntity.artistId]
 */
@Entity(
    tableName = "artists",
    indices = [
        Index("name"),
        Index(value = ["normalizedName"], unique = true)
    ]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val customArtworkUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
