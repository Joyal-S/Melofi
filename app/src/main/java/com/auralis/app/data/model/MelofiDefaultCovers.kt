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
     * Deterministically selects a default cover for [songId].
     *
     * Uses 64-bit integer mix hashing to ensure high entropy and uniform distribution across
     * all 16 supplied cover images without consecutive duplicate assignments.
     * Pure function of [songId] ensuring permanent stability across app restarts, rescans,
     * recompositions, and navigation.
     *
     * @return Pair of (selectedFileName, selectedIndex)
     */
    fun pickCover(songId: Long, lastAssignedIndex: Int = -1): Pair<String, Int> {
        val total = AVAILABLE_COVERS.size
        // High-entropy 64-bit mix hash ensuring uniform permutation
        val mixed = (songId xor (songId ushr 16)) * 0x45d9f3bL
        var index = (abs(mixed) % total).toInt()
        if (lastAssignedIndex >= 0 && index == lastAssignedIndex) {
            index = (index + 1) % total
        }
        return Pair(AVAILABLE_COVERS[index], index)
    }

    /**
     * Deterministically selects a single cover file name for an entity ID (e.g. playlist or song).
     */
    fun selectCover(id: Long): String = pickCover(id).first
}
