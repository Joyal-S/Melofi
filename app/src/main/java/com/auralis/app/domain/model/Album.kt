package com.auralis.app.domain.model

/**
 * Domain model representing an album aggregated from local library tracks.
 */
data class Album(
    val id: Long,
    val name: String,
    val artistId: Long?,
    val artistName: String?,
    val year: Int?,
    val songCount: Int,
    val artUri: String?
)
