package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a genre aggregated from local media tracks.
 */
@Entity(
    tableName = "genres",
    indices = [
        Index("name")
    ]
)
data class GenreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val songCount: Int,
)
