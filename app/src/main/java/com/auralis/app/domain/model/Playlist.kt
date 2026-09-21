package com.auralis.app.domain.model

/**
 * Domain representation of a playlist with custom local cover and optional description.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int = 0,
    val customCoverUri: String? = null,
    val description: String? = null
)
