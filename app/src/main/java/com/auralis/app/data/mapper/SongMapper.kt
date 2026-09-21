package com.auralis.app.data.mapper

import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.domain.model.Song

/**
 * Mapper between Room persistence [SongEntity] and clean domain [Song].
 */
object SongMapper {

    fun toDomain(entity: SongEntity): Song {
        return Song(
            id = entity.id,
            title = entity.title,
            artistId = entity.artistId,
            artistName = entity.artistName,
            albumId = entity.albumId,
            albumName = entity.albumName,
            genreId = entity.genreId,
            genreName = entity.genreName,
            duration = entity.duration,
            trackNumber = entity.trackNumber,
            discNumber = entity.discNumber,
            year = entity.year,
            contentUri = entity.contentUri,
            mimeType = entity.mimeType,
            size = entity.size,
            bitrate = entity.bitrate,
            sampleRate = entity.sampleRate,
            channelCount = entity.channelCount,
            dateAdded = entity.dateAdded,
            dateModified = entity.dateModified,
            albumArtUri = entity.albumArtUri
        )
    }

    fun toEntity(domain: Song, filePath: String? = null): SongEntity {
        return SongEntity(
            id = domain.id,
            title = domain.title,
            artistId = domain.artistId,
            artistName = domain.artistName,
            albumId = domain.albumId,
            albumName = domain.albumName,
            genreId = domain.genreId,
            genreName = domain.genreName,
            duration = domain.duration,
            trackNumber = domain.trackNumber,
            discNumber = domain.discNumber,
            year = domain.year,
            filePath = filePath,
            contentUri = domain.contentUri,
            mimeType = domain.mimeType,
            size = domain.size,
            bitrate = domain.bitrate,
            sampleRate = domain.sampleRate,
            channelCount = domain.channelCount,
            dateAdded = domain.dateAdded,
            dateModified = domain.dateModified,
            albumArtUri = domain.albumArtUri
        )
    }

    fun toDomainList(entities: List<SongEntity>): List<Song> =
        entities.map { toDomain(it) }
}
