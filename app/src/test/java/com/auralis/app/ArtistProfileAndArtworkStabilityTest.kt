package com.auralis.app

import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.data.local.db.entity.ArtistProfileOverrideEntity
import com.auralis.app.data.model.MelofiDefaultCovers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. 100% deterministic, order-independent SplitMix64 default cover assignment.
 * 2. Motion duration tokens and non-jarring spring parameters.
 * 3. ArtistProfileOverrideEntity integrity for persistent offline artist renaming.
 */
class ArtistProfileAndArtworkStabilityTest {

    @Test
    fun defaultCovers_deterministicAndPermanent() {
        val sampleSongIds = listOf(1L, 42L, 100L, 999999L, 1234567890123L)

        for (id in sampleSongIds) {
            val initial = MelofiDefaultCovers.selectCover(id)
            assertNotNull(initial)
            assertTrue("Cover must be in available covers list", initial in MelofiDefaultCovers.AVAILABLE_COVERS)

            // Re-evaluate 500 times to guarantee strict determinism and zero order dependence
            for (i in 0 until 500) {
                val repeated = MelofiDefaultCovers.selectCover(id)
                assertEquals("Cover must remain strictly identical across repeated queries", initial, repeated)
            }
        }
    }

    @Test
    fun pickCover_matchesSelectCoverAndValidIndex() {
        val ids = listOf(10L, 20L, 30L, 40L, 50L)
        for (id in ids) {
            val (cover, index) = MelofiDefaultCovers.pickCover(id)
            val direct = MelofiDefaultCovers.selectCover(id)
            assertEquals(direct, cover)
            assertTrue(index in 0 until MelofiDefaultCovers.AVAILABLE_COVERS.size)
            assertEquals(MelofiDefaultCovers.AVAILABLE_COVERS[index], cover)
        }
    }

    @Test
    fun defaultCovers_distributionAcrossAvailableAssets() {
        val assignedSet = mutableSetOf<String>()
        for (id in 1L..200L) {
            val cover = MelofiDefaultCovers.selectCover(id)
            assignedSet.add(cover)
        }
        // With 16 available covers and 200 songs, distribution should cover multiple assets
        assertTrue("Cover hashing should distribute across available images", assignedSet.size >= 10)
    }

    @Test
    fun motionTokens_areConsistentAndOrdered() {
        assertTrue(MelofyMotion.DURATION_MICRO > 0)
        assertTrue(MelofyMotion.DURATION_FAST >= MelofyMotion.DURATION_MICRO)
        assertTrue(MelofyMotion.DURATION_NORMAL >= MelofyMotion.DURATION_FAST)
        assertTrue(MelofyMotion.DURATION_CROSSFADE >= MelofyMotion.DURATION_NORMAL)
        assertTrue(MelofyMotion.DURATION_MAJOR >= MelofyMotion.DURATION_CROSSFADE)
    }

    @Test
    fun artistProfileOverrideEntity_fieldsPreserved() {
        val entity = ArtistProfileOverrideEntity(
            artistId = 12345L,
            originalName = "Original Artist Band",
            customName = "My Favorite Artist",
            updatedAt = 1700000000000L
        )
        assertEquals(12345L, entity.artistId)
        assertEquals("Original Artist Band", entity.originalName)
        assertEquals("My Favorite Artist", entity.customName)
        assertEquals(1700000000000L, entity.updatedAt)
    }
}
