package com.auralis.app.domain.model

/**
 * Domain representation of an audio track in Auralis.
 * Completely decoupled from Android framework and Room persistence annotations.
 */
data class Song(
    val id: Long,
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
