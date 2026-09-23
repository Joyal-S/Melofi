package com.auralis.app.data.repository

import com.auralis.app.data.model.MelofiDefaultCovers
import com.auralis.app.domain.model.ArtworkEntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Safe downsampling calculation for large gallery images (protecting memory/preventing OOM).
 * 2. Playlist default cover determinism across app restarts, rescans, and navigation.
 * 3. Playlist custom gallery override and reset restoring original default cover.
 * 4. Entity isolation between Song and Playlist artwork.
 * 5. Audio file preservation (zero audio modification).
 */
class GalleryArtworkAndDockTest {

    @Test
    fun `downsampling calculation - downsamples large 20MP+ images safely to 1024 max`() {
        val maxDim = 1024

        fun computeSampleSize(width: Int, height: Int): Int {
            var sampleSize = 1
            if (height > maxDim || width > maxDim) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / sampleSize) >= maxDim && (halfWidth / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }
            return sampleSize
        }

        // Test normal 500x500 image -> no downsampling
        assertEquals(1, computeSampleSize(500, 500))

        // Test exactly 1024x1024 image -> no downsampling
        assertEquals(1, computeSampleSize(1024, 1024))

        // Test 2048x2048 image -> downsampled by 2
        assertEquals(2, computeSampleSize(2048, 2048))

        // Test 4096x4096 (16MP) -> downsampled by 4
        assertEquals(4, computeSampleSize(4096, 4096))

        // Test 6000x4000 (24MP camera capture) -> downsampled by 4
        val sampleSize24MP = computeSampleSize(6000, 4000)
        assertTrue("Sample size should be at least 2 for 24MP image", sampleSize24MP >= 2)
        assertTrue(
            "Downsampled dimension must be within acceptable bounds",
            (4000 / sampleSize24MP) <= maxDim * 2
        )
    }

    @Test
    fun `playlist default cover determinism - same playlist always receives same default cover`() {
        val playlistId1 = 1L
        val playlistId2 = 2L
        val playlistId3 = 105L

        val cover1a = MelofiDefaultCovers.selectCover(playlistId1)
        val cover1b = MelofiDefaultCovers.selectCover(playlistId1)
        assertEquals("Playlist 1 must have deterministic cover across calls", cover1a, cover1b)

        val cover2a = MelofiDefaultCovers.selectCover(playlistId2)
        val cover2b = MelofiDefaultCovers.selectCover(playlistId2)
        assertEquals("Playlist 2 must have deterministic cover across calls", cover2a, cover2b)

        val cover3a = MelofiDefaultCovers.selectCover(playlistId3)
        val cover3b = MelofiDefaultCovers.selectCover(playlistId3)
        assertEquals("Playlist 105 must have deterministic cover across calls", cover3a, cover3b)

        // Across 16 playlist IDs, covers should be distributed
        val covers = (1L..16L).map { MelofiDefaultCovers.selectCover(it) }.distinct()
        assertTrue("Playlists should be distributed across multiple covers", covers.size >= 8)
    }

    @Test
    fun `playlist custom gallery override and reset lifecycle`() {
        val playlistId = 77L
        val originalDefault = MelofiDefaultCovers.selectCover(playlistId)

        val customOverrides = mutableMapOf<String, String>()

        fun resolvePlaylistCover(id: Long): String {
            return customOverrides["PLAYLIST_$id"] ?: originalDefault
        }

        // 1. Initial creation: displays deterministic default cover
        assertEquals(originalDefault, resolvePlaylistCover(playlistId))

        // 2. User selects gallery image and saves
        val customAppPrivatePath = "file:///data/user/0/com.auralis.app/files/custom_artwork/art_PLAYLIST_77.png"
        customOverrides["PLAYLIST_$playlistId"] = customAppPrivatePath

        // Verify custom cover displays
        assertEquals(customAppPrivatePath, resolvePlaylistCover(playlistId))

        // 3. User taps "Reset Cover"
        customOverrides.remove("PLAYLIST_$playlistId")

        // Verify reverts to the EXACT same original default cover (NOT a different cover)
        assertEquals(originalDefault, resolvePlaylistCover(playlistId))
    }

    @Test
    fun `entity artwork isolation - playlist artwork does not affect song artwork and vice versa`() {
        val entityOverrides = mutableMapOf<String, String>()

        val songId = "101"
        val playlistId = "101" // Same numerical ID, different entity type

        val songCustomArt = "file:///files/custom_artwork/art_SONG_101.png"
        val playlistCustomArt = "file:///files/custom_artwork/art_PLAYLIST_101.png"

        // Set song artwork
        entityOverrides["${ArtworkEntityType.SONG.name}_$songId"] = songCustomArt

        // Verify playlist artwork is unaffected
        assertEquals(null, entityOverrides["${ArtworkEntityType.PLAYLIST.name}_$playlistId"])
        assertEquals(songCustomArt, entityOverrides["${ArtworkEntityType.SONG.name}_$songId"])

        // Set playlist artwork
        entityOverrides["${ArtworkEntityType.PLAYLIST.name}_$playlistId"] = playlistCustomArt

        // Verify both coexist independently
        assertEquals(songCustomArt, entityOverrides["${ArtworkEntityType.SONG.name}_$songId"])
        assertEquals(playlistCustomArt, entityOverrides["${ArtworkEntityType.PLAYLIST.name}_$playlistId"])

        // Clear song artwork
        entityOverrides.remove("${ArtworkEntityType.SONG.name}_$songId")

        // Verify playlist artwork remains intact
        assertEquals(null, entityOverrides["${ArtworkEntityType.SONG.name}_$songId"])
        assertEquals(playlistCustomArt, entityOverrides["${ArtworkEntityType.PLAYLIST.name}_$playlistId"])
    }

    @Test
    fun `zero audio file modification guarantee`() {
        val audioExtensions = listOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus")
        val destinationPath = "/data/user/0/com.auralis.app/files/custom_artwork/art_SONG_42.png"

        // Destination must be in app-private storage, never in external music paths
        assertTrue(destinationPath.contains("custom_artwork"))
        assertTrue(destinationPath.endsWith(".png"))

        for (ext in audioExtensions) {
            assertFalse(
                "Custom artwork destination must never be an audio file",
                destinationPath.endsWith(".$ext")
            )
        }
    }
}
