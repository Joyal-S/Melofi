package com.auralis.app.domain.repository

import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.Genre
import com.auralis.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for browsing albums, artists, and genres in the local library.
 */
interface LibraryBrowserRepository {
    fun getAlbums(): Flow<List<Album>>
    fun getAlbumById(id: Long): Flow<Album?>
    fun getSongsByAlbum(albumId: Long, albumName: String): Flow<List<Song>>

    fun getArtists(): Flow<List<Artist>>
    fun getArtistById(id: Long): Flow<Artist?>
    fun getSongsByArtist(artistId: Long, artistName: String): Flow<List<Song>>
    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>>

    fun getGenres(): Flow<List<Genre>>
    fun getGenreById(id: Long): Flow<Genre?>
    fun getSongsByGenre(genreName: String): Flow<List<Song>>

    suspend fun createArtist(name: String): Result<Artist>
    suspend fun findArtistByNormalizedName(normalizedName: String): Artist?
    suspend fun findArtistByName(name: String): Artist?
    suspend fun mergeArtists(sourceArtistId: Long, targetArtistId: Long): Result<Unit>
    suspend fun updateArtistProfileName(artistId: Long, customName: String): Result<Unit>
    suspend fun resetArtistProfileName(artistId: Long): Result<Unit>
}
