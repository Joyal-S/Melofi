package com.auralis.app.domain.model

/**
 * Domain representation of a playlist.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int = 0
)
