package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.mapper.LibraryMapper
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.Genre
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.LibraryBrowserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryBrowserRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val songDao: SongDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LibraryBrowserRepository {

    override fun getAlbums(): Flow<List<Album>> {
        return albumDao.getAllAlbums()
            .map { LibraryMapper.toAlbumList(it) }
            .flowOn(ioDispatcher)
    }

    override fun getAlbumById(id: Long): Flow<Album?> {
        return albumDao.getAlbumById(id)
            .map { it?.let { LibraryMapper.toDomain(it) } }
            .flowOn(ioDispatcher)
    }

    override fun getSongsByAlbum(albumId: Long, albumName: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(albumId, albumName)
            .map { SongMapper.toDomainList(it) }
            .flowOn(ioDispatcher)
    }

    override fun getArtists(): Flow<List<Artist>> {
        return artistDao.getAllArtists()
            .map { LibraryMapper.toArtistList(it) }
            .flowOn(ioDispatcher)
    }

    override fun getArtistById(id: Long): Flow<Artist?> {
        return artistDao.getArtistById(id)
            .map { it?.let { LibraryMapper.toDomain(it) } }
            .flowOn(ioDispatcher)
    }

    override fun getSongsByArtist(artistId: Long, artistName: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artistId, artistName)
            .map { SongMapper.toDomainList(it) }
            .flowOn(ioDispatcher)
    }

    override fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> {
        return albumDao.getAllAlbums()
            .map { albums ->
                albums.filter { it.artistId == artistId }
                    .map { LibraryMapper.toDomain(it) }
            }
            .flowOn(ioDispatcher)
    }

    override fun getGenres(): Flow<List<Genre>> {
        return genreDao.getAllGenres()
            .map { LibraryMapper.toGenreList(it) }
            .flowOn(ioDispatcher)
    }

    override fun getGenreById(id: Long): Flow<Genre?> {
        return genreDao.getGenreById(id)
            .map { it?.let { LibraryMapper.toDomain(it) } }
            .flowOn(ioDispatcher)
    }

    override fun getSongsByGenre(genreName: String): Flow<List<Song>> {
        return songDao.getSongsByGenre(genreName)
            .map { SongMapper.toDomainList(it) }
            .flowOn(ioDispatcher)
    }
}
