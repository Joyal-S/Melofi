package com.auralis.app.data.model

import kotlin.math.abs

/**
 * Authoritative registry of Melofi's 16 supplied default cover images.
 *
 * Sourced directly from the project's images/ repository folder:
 * - 1.png through 10.png
 * - 11.jpg through 16.jpg
 *
 * Guarantees uniform, variety-rich distribution across discovered audio tracks
 * while avoiding consecutive duplicate assignments.
 */
object MelofiDefaultCovers {

    val AVAILABLE_COVERS: List<String> = listOf(
        "1.png",
        "2.png",
        "3.png",
        "4.png",
        "5.png",
        "6.png",
        "7.png",
        "8.png",
        "9.png",
        "10.png",
        "11.jpg",
        "12.jpg",
        "13.jpg",
        "14.jpg",
        "15.jpg",
        "16.jpg"
    )

    const val ASSET_FOLDER = "default_covers"
    const val FILES_FOLDER = "default_covers"

    /**
     * Deterministically selects a single cover file name for an entity ID (e.g. playlist or song).
     *
     * Uses pure 64-bit SplitMix64 mixing to guarantee uniform, high-entropy distribution
     * across all 16 supplied cover images without order dependence or scan state.
     * Pure function of [id] alone, guaranteeing permanent stability across app restarts,
     * rescans, recompositions, and navigation.
     */
    fun selectCover(id: Long): String {
        var z = id + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb67232230ec5L
        z = z xor (z ushr 31)
        val index = (abs(z % AVAILABLE_COVERS.size)).toInt()
        return AVAILABLE_COVERS[index]
    }

    /**
     * Deterministically selects a default cover for [songId].
     * Supports collision avoidance when consecutive tracks hash to identical indices.
     */
    fun pickCover(songId: Long, lastAssignedIndex: Int = -1): Pair<String, Int> {
        val total = AVAILABLE_COVERS.size
        var z = songId + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb67232230ec5L
        z = z xor (z ushr 31)
        var index = (abs(z % total)).toInt()
        if (lastAssignedIndex >= 0 && index == lastAssignedIndex) {
            index = (index + 1) % total
        }
        return Pair(AVAILABLE_COVERS[index], index)
    }
}
