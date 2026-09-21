package com.auralis.app.domain.model

/**
 * Result statistics from a MediaStore library synchronization pass.
 */
data class ScanResult(
    val totalFound: Int,
    val inserted: Int,
    val updated: Int,
    val deleted: Int
)
