package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Permanent local mapping between a stable song ID and its assigned Melofi default cover image.
 *
 * Decoupled from foreign key cascades to ensure assignments permanently survive library rescans,
 * temporary file removals, and differential synchronizations.
 */
@Entity(tableName = "song_default_covers")
data class SongDefaultCoverEntity(
    @PrimaryKey
    val songId: Long,
    val coverFileName: String,
    val assignedAt: Long
)
