package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a song in the local database.
 *
 * Uses MediaStore ID as primary key.
 * Stores content URI as stable reference for Scoped Storage (Android 10+).
 * Denormalizes artistName and albumName for high-performance single-table queries.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index("artistId"),
        Index("albumId"),
        Index("genreId"),
        Index("title"),
        Index("artistName"),
        Index("dateAdded"),
        Index(value = ["contentUri"], unique = true)
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artistId: Long?,
    val artistName: String,
    val albumId: Long?,
    val albumName: String,
    val genreId: Long?,
    val genreName: String?,
    val duration: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val filePath: String?,
    val contentUri: String,
    val mimeType: String?,
    val size: Long,
    val bitrate: Int?,
    val sampleRate: Int?,
    val channelCount: Int?,
    val dateAdded: Long,
    val dateModified: Long,
    val albumArtUri: String?,
)
