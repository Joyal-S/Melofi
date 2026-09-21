package com.auralis.app

import com.auralis.app.playback.mapper.MediaItemMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [MediaItemMapper].
 */
class MediaItemMapperTest {

    @Test
    fun extractSongId_validStringId_returnsLong() {
        assertEquals(12345L, MediaItemMapper.extractSongId("12345"))
    }

    @Test
    fun extractSongId_invalidString_returnsNull() {
        assertNull(MediaItemMapper.extractSongId("not-a-number"))
    }

    @Test
    fun extractSongId_nullString_returnsNull() {
        assertNull(MediaItemMapper.extractSongId(null as String?))
    }

    @Test
    fun extractSongId_zeroAndNegative_parsedCorrectly() {
        assertEquals(0L, MediaItemMapper.extractSongId("0"))
        assertEquals(-1L, MediaItemMapper.extractSongId("-1"))
    }
}
