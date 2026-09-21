package com.auralis.app

import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.data.mapper.SongMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [SongMapper].
 * Verifies bidirectional mapping between Room entity and domain models.
 */
class SongMapperTest {

    @Test
    fun toDomain_preservesAllAttributes() {
        val entity = SongEntity(
            id = 42L,
            title = "Clair de Lune",
            artistId = 101L,
            artistName = "Claude Debussy",
            albumId = 202L,
            albumName = "Suite Bergamasque",
            genreId = 303L,
            genreName = "Classical",
            duration = 300000L,
            trackNumber = 3,
            discNumber = 1,
            year = 1905,
            filePath = "/storage/emulated/0/Music/debussy.flac",
            contentUri = "content://media/external/audio/media/42",
            mimeType = "audio/flac",
            size = 25000000L,
            bitrate = 900,
            sampleRate = 96000,
            channelCount = 2,
            dateAdded = 1600000000L,
            dateModified = 1600001000L,
            albumArtUri = "content://media/external/audio/albumart/202"
        )

        val domain = SongMapper.toDomain(entity)

        assertEquals(42L, domain.id)
        assertEquals("Clair de Lune", domain.title)
        assertEquals(101L, domain.artistId)
        assertEquals("Claude Debussy", domain.artistName)
        assertEquals(202L, domain.albumId)
        assertEquals("Suite Bergamasque", domain.albumName)
        assertEquals(303L, domain.genreId)
        assertEquals("Classical", domain.genreName)
        assertEquals(300000L, domain.duration)
        assertEquals(3, domain.trackNumber)
        assertEquals(1, domain.discNumber)
        assertEquals(1905, domain.year)
        assertEquals("content://media/external/audio/media/42", domain.contentUri)
        assertEquals("audio/flac", domain.mimeType)
        assertEquals(25000000L, domain.size)
        assertEquals(900, domain.bitrate)
        assertEquals(96000, domain.sampleRate)
        assertEquals(2, domain.channelCount)
        assertEquals(1600000000L, domain.dateAdded)
        assertEquals(1600001000L, domain.dateModified)
        assertEquals("content://media/external/audio/albumart/202", domain.albumArtUri)
    }

    @Test
    fun toEntity_preservesDomainAttributes() {
        val entity = SongEntity(
            id = 99L,
            title = "Test Title",
            artistId = null,
            artistName = "Unknown Artist",
            albumId = null,
            albumName = "Unknown Album",
            genreId = null,
            genreName = null,
            duration = 120000L,
            trackNumber = null,
            discNumber = null,
            year = null,
            filePath = null,
            contentUri = "content://media/external/audio/media/99",
            mimeType = "audio/mp3",
            size = 3000000L,
            bitrate = 128,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1700000000L,
            dateModified = 1700000000L,
            albumArtUri = null
        )

        val domain = SongMapper.toDomain(entity)
        val reconverted = SongMapper.toEntity(domain)

        assertEquals(domain.id, reconverted.id)
        assertEquals(domain.title, reconverted.title)
        assertNull(reconverted.artistId)
        assertEquals(domain.artistName, reconverted.artistName)
        assertNull(reconverted.albumId)
        assertEquals(domain.albumName, reconverted.albumName)
        assertNull(reconverted.albumArtUri)
    }
}
