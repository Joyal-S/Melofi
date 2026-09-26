package com.auralis.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted entity representing an audio file hidden from the Melofi Music Library by the user.
 *
 * Excluded from all library views, search, playlists, and recommendations without deleting or
 * modifying the physical file on disk. Can be reviewed and restored from Settings -> Library -> Hidden Audio.
 */
@Entity(tableName = "hidden_audio")
data class HiddenAudioEntity(
    @PrimaryKey
    val id: Long,
    val contentUri: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Long,
    val filePath: String?,
    val albumArtUri: String?,
    val hiddenAt: Long = System.currentTimeMillis()
)
