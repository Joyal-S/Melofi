package com.auralis.app

import com.auralis.app.data.local.scanner.AudioMetadataNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying local artwork URI resolution and normalizer bounds.
 */
class ArtworkCacheTest {

    @Test
    fun buildAlbumArtUri_positiveAlbumId_returnsMediaStoreAlbumArtPrefix() {
        val uri = AudioMetadataNormalizer.buildAlbumArtUri(250L)
        assertEquals("content://media/external/audio/albumart/250", uri)
    }

    @Test
    fun buildAlbumArtUri_nullOrZeroAlbumId_returnsNull() {
        assertNull(AudioMetadataNormalizer.buildAlbumArtUri(null))
        assertNull(AudioMetadataNormalizer.buildAlbumArtUri(0L))
        assertNull(AudioMetadataNormalizer.buildAlbumArtUri(-5L))
    }

    @Test
    fun buildContentUri_mediaId_returnsAudioMediaUri() {
        val uri = AudioMetadataNormalizer.buildContentUri(1001L)
        assertEquals("content://media/external/audio/media/1001", uri)
    }
}
