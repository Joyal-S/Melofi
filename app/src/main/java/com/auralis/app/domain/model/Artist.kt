package com.auralis.app.domain.model

/**
 * Domain model representing an artist aggregated from local library tracks.
 */
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val artUri: String? = null
)
