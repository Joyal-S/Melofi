package com.auralis.app.data.mapper

import com.auralis.app.data.local.db.entity.AlbumEntity
import com.auralis.app.data.local.db.entity.ArtistEntity
import com.auralis.app.data.local.db.entity.GenreEntity
import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.Genre

/**
 * Mapper between Room entities and domain models for Albums, Artists, and Genres.
 */
object LibraryMapper {

    fun toDomain(entity: AlbumEntity): Album {
        return Album(
            id = entity.id,
            name = entity.name,
            artistId = entity.artistId,
            artistName = entity.artistName,
            year = entity.year,
            songCount = entity.songCount,
            artUri = entity.artUri
        )
    }

    fun toDomain(entity: ArtistEntity): Artist {
        return Artist(
            id = entity.id,
            name = entity.name,
            albumCount = entity.albumCount,
            songCount = entity.songCount,
            artUri = entity.customArtworkUri
        )
    }

    fun toDomain(entity: GenreEntity): Genre {
        return Genre(
            id = entity.id,
            name = entity.name,
            songCount = entity.songCount
        )
    }

    fun toAlbumList(entities: List<AlbumEntity>): List<Album> = entities.map { toDomain(it) }
    fun toArtistList(entities: List<ArtistEntity>): List<Artist> = entities.map { toDomain(it) }
    fun toGenreList(entities: List<GenreEntity>): List<Genre> = entities.map { toDomain(it) }
}
