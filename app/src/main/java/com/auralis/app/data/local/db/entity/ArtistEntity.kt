package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an artist aggregated from local media tracks.
 */
@Entity(
    tableName = "artists",
    indices = [
        Index("name")
    ]
)
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)
