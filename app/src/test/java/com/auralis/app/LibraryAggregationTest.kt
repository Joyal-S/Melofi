package com.auralis.app

import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.data.local.scanner.LibraryAggregator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [LibraryAggregator].
 * Verifies that differential aggregation keeps albums, artists, and genres consistent,
 * updates metadata accurately, and cleans up stale/orphaned aggregates when their last song is removed.
 */
class LibraryAggregationTest {

    private fun createSong(
        id: Long,
        title: String = "Track $id",
        artistId: Long? = 10L,
        artistName: String = "Artist A",
        albumId: Long? = 100L,
        albumName: String = "Album X",
        genreName: String? = "Rock",
        year: Int? = 2020
    ): SongEntity {
        return SongEntity(
            id = id,
            title = title,
            artistId = artistId,
            artistName = artistName,
            albumId = albumId,
            albumName = albumName,
            genreId = null,
            genreName = genreName,
            duration = 180000L,
            trackNumber = id.toInt(),
            discNumber = 1,
            year = year,
            filePath = "/storage/emulated/0/Music/track_$id.mp3",
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = 4000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1000L,
            dateModified = 1000L,
            albumArtUri = "content://media/external/audio/albumart/$albumId"
        )
    }

    @Test
    fun aggregateAlbums_whenSongsAdded_aggregatesCorrectCounts() {
        val songs = listOf(
            createSong(id = 1L, albumId = 100L, albumName = "Album 1", year = 2021),
            createSong(id = 2L, albumId = 100L, albumName = "Album 1", year = 2022),
            createSong(id = 3L, albumId = 200L, albumName = "Album 2", year = 2024)
        )

        val albums = LibraryAggregator.aggregateAlbums(songs)

        assertEquals(2, albums.size)
        val album1 = albums.first { it.id == 100L }
        assertEquals(2, album1.songCount)
        assertEquals(2022, album1.year) // Highest year preserved

        val album2 = albums.first { it.id == 200L }
        assertEquals(1, album2.songCount)
    }

    @Test
    fun aggregateAlbums_whenLastSongRemoved_prunesAlbumPreventingOrphans() {
        // Initially 2 songs across 2 albums
        val initialSongs = listOf(
            createSong(id = 1L, albumId = 100L, albumName = "Album 1"),
            createSong(id = 2L, albumId = 200L, albumName = "Album 2")
        )
        val initialAlbums = LibraryAggregator.aggregateAlbums(initialSongs)
        assertEquals(2, initialAlbums.size)

        // Song 2 (the only track of Album 2) is deleted
        val remainingSongs = listOf(
            createSong(id = 1L, albumId = 100L, albumName = "Album 1")
        )
        val updatedAlbums = LibraryAggregator.aggregateAlbums(remainingSongs)

        // Album 2 must be completely pruned
        assertEquals(1, updatedAlbums.size)
        assertEquals(100L, updatedAlbums.first().id)
        assertTrue(updatedAlbums.none { it.id == 200L })
    }

    @Test
    fun aggregateArtists_whenLastSongRemoved_prunesArtistPreventingOrphans() {
        val initialSongs = listOf(
            createSong(id = 1L, artistId = 10L, artistName = "Artist 1"),
            createSong(id = 2L, artistId = 20L, artistName = "Artist 2")
        )
        val initialArtists = LibraryAggregator.aggregateArtists(initialSongs)
        assertEquals(2, initialArtists.size)

        // Song 2 is deleted
        val remainingSongs = listOf(
            createSong(id = 1L, artistId = 10L, artistName = "Artist 1")
        )
        val updatedArtists = LibraryAggregator.aggregateArtists(remainingSongs)

        assertEquals(1, updatedArtists.size)
        assertEquals(10L, updatedArtists.first().id)
        assertTrue(updatedArtists.none { it.id == 20L })
    }

    @Test
    fun aggregateGenres_whenLastSongRemoved_prunesGenrePreventingOrphans() {
        val initialSongs = listOf(
            createSong(id = 1L, genreName = "Jazz"),
            createSong(id = 2L, genreName = "Classical")
        )
        val initialGenres = LibraryAggregator.aggregateGenres(initialSongs)
        assertEquals(2, initialGenres.size)

        // Song 2 is deleted
        val remainingSongs = listOf(
            createSong(id = 1L, genreName = "Jazz")
        )
        val updatedGenres = LibraryAggregator.aggregateGenres(remainingSongs)

        assertEquals(1, updatedGenres.size)
        assertEquals("Jazz", updatedGenres.first().name)
        assertTrue(updatedGenres.none { it.name == "Classical" })
    }

    @Test
    fun aggregateAlbums_whenMetadataChanges_reflectsUpdatedAlbumNameAndYear() {
        val updatedSongs = listOf(
            createSong(id = 1L, albumId = 100L, albumName = "Album Remastered", year = 2025)
        )

        val albums = LibraryAggregator.aggregateAlbums(updatedSongs)

        assertEquals(1, albums.size)
        val album = albums.first()
        assertEquals("Album Remastered", album.name)
        assertEquals(2025, album.year)
    }

    @Test
    fun aggregateArtists_distinctAlbumCountCalculatedCorrectly() {
        val songs = listOf(
            createSong(id = 1L, artistId = 10L, albumId = 100L),
            createSong(id = 2L, artistId = 10L, albumId = 100L),
            createSong(id = 3L, artistId = 10L, albumId = 200L),
            createSong(id = 4L, artistId = 10L, albumId = 300L)
        )

        val artists = LibraryAggregator.aggregateArtists(songs)

        assertEquals(1, artists.size)
        val artist = artists.first()
        assertEquals(4, artist.songCount)
        assertEquals(3, artist.albumCount) // 3 distinct albums: 100, 200, 300
    }

    @Test
    fun aggregateGenres_nullOrBlankGenresIgnored() {
        val songs = listOf(
            createSong(id = 1L, genreName = "Ambient"),
            createSong(id = 2L, genreName = null),
            createSong(id = 3L, genreName = ""),
            createSong(id = 4L, genreName = "   ")
        )

        val genres = LibraryAggregator.aggregateGenres(songs)

        assertEquals(1, genres.size)
        assertEquals("Ambient", genres.first().name)
        assertEquals(1, genres.first().songCount)
    }
}
