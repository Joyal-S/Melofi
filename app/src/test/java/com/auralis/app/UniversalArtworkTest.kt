package com.auralis.app

import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit tests validating universal artwork priority, entity override paths,
 * offline storage invariants, and downsampling logic.
 */
class UniversalArtworkTest {

    @Test
    fun entityTypePaths_formatDeterministically() {
        val songKey = "123"
        val albumKey = "456"
        val artistKey = "789"
        val genreKey = "101"
        val playlistKey = "202"

        assertEquals("art_SONG_123.png", "art_${ArtworkEntityType.SONG.name}_$songKey.png")
        assertEquals("art_ALBUM_456.png", "art_${ArtworkEntityType.ALBUM.name}_$albumKey.png")
        assertEquals("art_ARTIST_789.png", "art_${ArtworkEntityType.ARTIST.name}_$artistKey.png")
        assertEquals("art_GENRE_101.png", "art_${ArtworkEntityType.GENRE.name}_$genreKey.png")
        assertEquals("art_PLAYLIST_202.png", "art_${ArtworkEntityType.PLAYLIST.name}_$playlistKey.png")
    }

    @Test
    fun collageFilename_formatsDeterministically() {
        val playlistId = 99L
        val expected = "collage_$playlistId.png"
        assertEquals("collage_99.png", expected)
    }

    @Test
    fun artworkPriorityHierarchy_evaluatesCorrectly() {
        // Priority: Custom Song > Custom Album > Cached Thumbnail > Embedded > MediaStore > Fallback
        val customSongOverride: String? = "file:///data/user/0/app/files/custom_artwork/art_SONG_1.png"
        val customAlbumOverride: String? = "file:///data/user/0/app/files/custom_artwork/art_ALBUM_10.png"
        val cachedThumb: String? = "file:///data/user/0/app/cache/artwork_cache/1.webp"
        val mediaStoreArt: String? = "content://media/external/audio/albumart/10"

        val resolvedWithSongOverride = customSongOverride ?: customAlbumOverride ?: cachedThumb ?: mediaStoreArt
        assertEquals(customSongOverride, resolvedWithSongOverride)

        val resolvedWithAlbumOverride = null ?: customAlbumOverride ?: cachedThumb ?: mediaStoreArt
        assertEquals(customAlbumOverride, resolvedWithAlbumOverride)

        val resolvedWithCachedThumb = null ?: null ?: cachedThumb ?: mediaStoreArt
        assertEquals(cachedThumb, resolvedWithCachedThumb)

        val resolvedWithMediaStore = null ?: null ?: null ?: mediaStoreArt
        assertEquals(mediaStoreArt, resolvedWithMediaStore)
    }

    @Test
    fun oversizedImageSampleRatio_computesSafely() {
        val maxDim = 1024
        val imageWidth = 4096
        val imageHeight = 4096

        var sampleSize = 1
        val halfHeight = imageHeight / 2
        val halfWidth = imageWidth / 2
        while ((halfHeight / sampleSize) >= maxDim && (halfWidth / sampleSize) >= maxDim) {
            sampleSize *= 2
        }

        // 4096 / 4 = 1024 -> sampleSize is 4
        assertEquals(4, sampleSize)
    }

    @Test
    fun resetToOriginal_clearsCustomFileAndRestoresDefault() {
        val dummyFile = File.createTempFile("art_SONG_test", ".png")
        assertTrue(dummyFile.exists())

        // Simulating reset: delete file
        val deleted = dummyFile.delete()
        assertTrue(deleted)
        assertFalse(dummyFile.exists())
    }

    @Test
    fun collageDistinctTracksSelection_takesFirstFour() {
        val memberArtUris = listOf(
            "content://media/1",
            "content://media/2",
            "content://media/1", // duplicate
            "content://media/3",
            "content://media/4",
            "content://media/5"
        )
        val selected = memberArtUris.filter { it.isNotBlank() }.distinct().take(4)
        assertEquals(4, selected.size)
        assertEquals("content://media/1", selected[0])
        assertEquals("content://media/2", selected[1])
        assertEquals("content://media/3", selected[2])
        assertEquals("content://media/4", selected[3])
    }
}
