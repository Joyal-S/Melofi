package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an album aggregated from local media tracks.
 */
@Entity(
    tableName = "albums",
    indices = [
        Index("artistId"),
        Index("name")
    ]
)
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val artistId: Long?,
    val artistName: String?,
    val year: Int?,
    val songCount: Int,
    val artUri: String?,
)
