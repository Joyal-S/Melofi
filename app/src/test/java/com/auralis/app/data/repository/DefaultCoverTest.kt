package com.auralis.app.data.repository

import com.auralis.app.data.model.MelofiDefaultCovers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Melofi's permanent default cover artwork system.
 */
class DefaultCoverTest {

    @Test
    fun `verify catalog integrity - exactly 16 supplied images discovered`() {
        val covers = MelofiDefaultCovers.AVAILABLE_COVERS
        assertEquals("Catalog must contain exactly 16 supplied images", 16, covers.size)

        val expectedCovers = listOf(
            "1.png", "2.png", "3.png", "4.png", "5.png",
            "6.png", "7.png", "8.png", "9.png", "10.png",
            "11.jpg", "12.jpg", "13.jpg", "14.jpg", "15.jpg", "16.jpg"
        )
        assertEquals(expectedCovers, covers)
    }

    @Test
    fun `deterministic selection - same song ID always yields same primary choice`() {
        val songId = 42891L
        val (firstPick, firstIdx) = MelofiDefaultCovers.pickCover(songId)
        val (secondPick, secondIdx) = MelofiDefaultCovers.pickCover(songId)

        assertEquals("Same song ID must deterministically choose the same cover", firstPick, secondPick)
        assertEquals("Same song ID must yield the same cover index", firstIdx, secondIdx)
    }

    @Test
    fun `variety and distribution - sequential song IDs distribute across available covers`() {
        val assignedCovers = mutableListOf<String>()
        var lastIdx = -1

        for (id in 1L..16L) {
            val (cover, idx) = MelofiDefaultCovers.pickCover(id, lastIdx)
            assignedCovers.add(cover)
            lastIdx = idx
        }

        // Verify that sequential songs don't all receive the same cover
        val distinctCovers = assignedCovers.distinct()
        assertTrue(
            "Sequential songs should be distributed across multiple covers (got ${distinctCovers.size} distinct)",
            distinctCovers.size >= 8
        )
    }

    @Test
    fun `consecutive duplicate prevention - shifts index when identical to previous`() {
        // Find an ID and its index
        val songId = 100L
        val (_, naturalIdx) = MelofiDefaultCovers.pickCover(songId, -1)

        // When passing lastAssignedIndex = naturalIdx, it must shift to prevent consecutive duplicate
        val (shiftedCover, shiftedIdx) = MelofiDefaultCovers.pickCover(songId, naturalIdx)

        assertNotEquals("Consecutive duplicate should be prevented", naturalIdx, shiftedIdx)
        assertEquals("Index should shift by 1 modulo total", (naturalIdx + 1) % 16, shiftedIdx)
        assertEquals(MelofiDefaultCovers.AVAILABLE_COVERS[shiftedIdx], shiftedCover)
    }

    @Test
    fun `stability priority - once persisted, assigned cover remains unchanged`() {
        // Simulate local assignment persistence repository map
        val persistedDefaultCovers = mutableMapOf<Long, String>()

        val songA = 1001L
        val (initialCoverA, _) = MelofiDefaultCovers.pickCover(songA)
        persistedDefaultCovers[songA] = initialCoverA

        val songB = 1002L
        val (initialCoverB, _) = MelofiDefaultCovers.pickCover(songB)
        persistedDefaultCovers[songB] = initialCoverB

        // Simulate app restart / memory wipe / rescan
        // Resolver must always check persisted mapping first
        val resolvedCoverA = persistedDefaultCovers[songA]
            ?: MelofiDefaultCovers.pickCover(songA).first
        val resolvedCoverB = persistedDefaultCovers[songB]
            ?: MelofiDefaultCovers.pickCover(songB).first

        assertEquals(initialCoverA, resolvedCoverA)
        assertEquals(initialCoverB, resolvedCoverB)
    }

    @Test
    fun `custom artwork override and reset behavior`() {
        val songId = 5001L
        val persistedDefaultCovers = mutableMapOf<Long, String>()
        val customArtworkOverrides = mutableMapOf<Long, String>()

        // 1. Initial discovery -> assigned default cover
        val (defaultCover, _) = MelofiDefaultCovers.pickCover(songId)
        persistedDefaultCovers[songId] = defaultCover

        fun resolveArtwork(id: Long): String {
            return customArtworkOverrides[id]
                ?: persistedDefaultCovers[id]
                ?: "fallback"
        }

        // Initially displays default cover
        assertEquals(defaultCover, resolveArtwork(songId))

        // 2. User sets custom artwork from Gallery
        val userGalleryUri = "file:///data/user/0/com.auralis.app/files/custom_artwork/art_SONG_5001.png"
        customArtworkOverrides[songId] = userGalleryUri

        // Custom artwork takes priority
        assertEquals(userGalleryUri, resolveArtwork(songId))
        // Default cover assignment remains stored underneath
        assertEquals(defaultCover, persistedDefaultCovers[songId])

        // 3. User taps "Reset Artwork" -> only custom override is removed
        customArtworkOverrides.remove(songId)

        // Song immediately reverts to its original default cover (NOT a new random cover)
        assertEquals(defaultCover, resolveArtwork(songId))
    }

    @Test
    fun `artwork URI path discrimination - correctly identifies custom override vs default cover`() {
        val defaultCoverUri = "file:///data/user/0/com.auralis.app/files/default_covers/15.jpg"
        val customOverrideUri = "file:///data/user/0/com.auralis.app/files/custom_artwork/art_SONG_57.png"
        val embeddedCacheUri = "file:///data/user/0/com.auralis.app/cache/artwork_cache/57.webp"
        val mediaStoreUri = "content://media/external/audio/albumart/12"

        fun isCustomOverride(uri: String?): Boolean = uri?.contains("custom_artwork") == true
        fun isDefaultCover(uri: String?): Boolean = uri?.contains("default_covers") == true

        assertTrue(isCustomOverride(customOverrideUri))
        assertFalse(isCustomOverride(defaultCoverUri))
        assertFalse(isCustomOverride(embeddedCacheUri))
        assertFalse(isCustomOverride(mediaStoreUri))

        assertTrue(isDefaultCover(defaultCoverUri))
        assertFalse(isDefaultCover(customOverrideUri))
    }
}
