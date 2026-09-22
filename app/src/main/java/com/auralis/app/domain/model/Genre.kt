package com.auralis.app.domain.model

/**
 * Domain model representing an audio genre aggregated from local library tracks.
 */
data class Genre(
    val id: Long,
    val name: String,
    val songCount: Int,
    val artUri: String? = null
)
