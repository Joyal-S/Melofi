package com.auralis.app.data.local.scanner

import com.auralis.app.data.local.db.entity.AlbumEntity
import com.auralis.app.data.local.db.entity.ArtistEntity
import com.auralis.app.data.local.db.entity.GenreEntity
import com.auralis.app.data.local.db.entity.SongEntity
import kotlin.math.abs

/**
 * Pure Kotlin aggregation engine for deriving album, artist, and genre groupings
 * from active library songs.
 *
 * Guarantees that deleting songs (or the last song in an album/artist/genre)
 * prevents orphaned aggregate entities in the database.
 */
object LibraryAggregator {

    /**
     * Aggregates album entities from a collection of songs.
     * Songs without an albumId are grouped by albumName if available.
     */
    fun aggregateAlbums(songs: List<SongEntity>): List<AlbumEntity> {
        return songs
            .groupBy { song ->
                song.albumId ?: (abs(song.albumName.hashCode().toLong()) + 1_000_000_000L)
            }
            .map { (albumId, albumSongs) ->
                val first = albumSongs.first()
                AlbumEntity(
                    id = albumId,
                    name = first.albumName,
                    artistId = first.artistId,
                    artistName = first.artistName,
                    year = albumSongs.mapNotNull { it.year }.maxOrNull(),
                    songCount = albumSongs.size,
                    artUri = first.albumArtUri
                )
            }
    }

    /**
     * Aggregates artist entities from a collection of songs.
     * Songs without an artistId are grouped by artistName if available.
     */
    fun aggregateArtists(songs: List<SongEntity>): List<ArtistEntity> {
        return songs
            .groupBy { song ->
                song.artistId ?: (abs(song.artistName.hashCode().toLong()) + 1_000_000_000L)
            }
            .map { (artistId, artistSongs) ->
                val first = artistSongs.first()
                val distinctAlbumCount = artistSongs.mapNotNull { it.albumId }.distinct().size
                ArtistEntity(
                    id = artistId,
                    name = first.artistName,
                    albumCount = distinctAlbumCount,
                    songCount = artistSongs.size
                )
            }
    }

    /**
     * Aggregates genre entities from a collection of songs.
     * Omits null or blank genre values.
     */
    fun aggregateGenres(songs: List<SongEntity>): List<GenreEntity> {
        return songs
            .filter { !it.genreName.isNullOrBlank() }
            .groupBy { it.genreName!! }
            .map { (genreName, genreSongs) ->
                GenreEntity(
                    id = abs(genreName.hashCode().toLong()),
                    name = genreName,
                    songCount = genreSongs.size
                )
            }
    }
}
