package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for user-customized artist profile display names.
 *
 * Persists custom artist names across app restarts, rescans, and updates
 * without modifying original audio files or MediaStore records.
 * Ensures all songs and albums remain permanently associated with the artist.
 */
@Entity(tableName = "artist_profile_overrides")
data class ArtistProfileOverrideEntity(
    @PrimaryKey
    val artistId: Long,
    val originalName: String,
    val customName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
