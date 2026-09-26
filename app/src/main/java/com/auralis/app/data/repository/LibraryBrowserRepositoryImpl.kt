package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.ArtistProfileOverrideDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.local.db.entity.ArtistProfileOverrideEntity
import com.auralis.app.data.mapper.LibraryMapper
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.Album
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.Genre
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.LibraryBrowserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LibraryBrowserRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val songDao: SongDao,
    private val artistProfileOverrideDao: ArtistProfileOverrideDao,
    private val localArtworkRepository: LocalArtworkRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LibraryBrowserRepository {

    override fun getAlbums(): Flow<List<Album>> {
        return combine(
            albumDao.getAllAlbums(),
            localArtworkRepository.artworkUpdates
        ) { list, _ ->
            list.map { entity ->
                val domain = LibraryMapper.toDomain(entity)
                val custom = localArtworkRepository.getAlbumArtworkUri(domain.id, domain.artUri)
                domain.copy(artUri = custom)
            }
        }.flowOn(ioDispatcher)
    }

    override fun getAlbumById(id: Long): Flow<Album?> {
        return combine(
            albumDao.getAlbumById(id),
            localArtworkRepository.artworkUpdates
        ) { entity, _ ->
            entity?.let {
                val domain = LibraryMapper.toDomain(it)
                val custom = localArtworkRepository.getAlbumArtworkUri(domain.id, domain.artUri)
                domain.copy(artUri = custom)
            }
        }.flowOn(ioDispatcher)
    }

    private fun mapSongs(entities: List<com.auralis.app.data.local.db.entity.SongEntity>): List<Song> {
        return entities.map { entity ->
            val domain = SongMapper.toDomain(entity)
            val art = localArtworkRepository.resolveSongArtworkFast(domain)
            domain.copy(albumArtUri = art)
        }
    }

    override fun getSongsByAlbum(albumId: Long, albumName: String): Flow<List<Song>> {
        return combine(
            songDao.getSongsByAlbum(albumId, albumName),
            localArtworkRepository.artworkUpdates
        ) { list, _ ->
            mapSongs(list)
        }.flowOn(ioDispatcher)
    }

    override fun getArtists(): Flow<List<Artist>> {
        return combine(
            artistDao.getAllArtists(),
            artistProfileOverrideDao.getAllOverridesFlow(),
            localArtworkRepository.artworkUpdates
        ) { artists, overrides, _ ->
            val overrideMap = overrides.associateBy { it.artistId }
            artists.map { entity ->
                val domain = LibraryMapper.toDomain(entity)
                val custom = localArtworkRepository.getArtistArtworkUri(domain.id)
                val customName = overrideMap[domain.id]?.customName
                domain.copy(
                    name = customName ?: domain.name,
                    artUri = custom
                )
            }
        }.flowOn(ioDispatcher)
    }

    override fun getArtistById(id: Long): Flow<Artist?> {
        return combine(
            artistDao.getArtistById(id),
            artistProfileOverrideDao.getOverrideFlow(id),
            localArtworkRepository.artworkUpdates
        ) { entity, override, _ ->
            entity?.let {
                val domain = LibraryMapper.toDomain(it)
                val custom = localArtworkRepository.getArtistArtworkUri(domain.id)
                val customName = override?.customName
                domain.copy(
                    name = customName ?: domain.name,
                    artUri = custom
                )
            }
        }.flowOn(ioDispatcher)
    }

    override fun getSongsByArtist(artistId: Long, artistName: String): Flow<List<Song>> {
        return combine(
            songDao.getSongsByArtistId(artistId),
            localArtworkRepository.artworkUpdates
        ) { list, _ ->
            if (list.isNotEmpty()) {
                mapSongs(list)
            } else {
                // Defensive fallback to matching songs by artistName if artistId was not yet migrated
                val fallback = songDao.getSongsByArtist(artistId, artistName).first()
                mapSongs(fallback)
            }
        }.flowOn(ioDispatcher)
    }

    override fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> {
        return combine(
            albumDao.getAllAlbums(),
            localArtworkRepository.artworkUpdates
        ) { albums, _ ->
            albums.filter { it.artistId == artistId }
                .map { entity ->
                    val domain = LibraryMapper.toDomain(entity)
                    val custom = localArtworkRepository.getAlbumArtworkUri(domain.id, domain.artUri)
                    domain.copy(artUri = custom)
                }
        }.flowOn(ioDispatcher)
    }

    override fun getGenres(): Flow<List<Genre>> {
        return combine(
            genreDao.getAllGenres(),
            localArtworkRepository.artworkUpdates
        ) { list, _ ->
            list.map { entity ->
                val domain = LibraryMapper.toDomain(entity)
                val custom = localArtworkRepository.getGenreArtworkUri(domain.id)
                domain.copy(artUri = custom)
            }
        }.flowOn(ioDispatcher)
    }

    override fun getGenreById(id: Long): Flow<Genre?> {
        return combine(
            genreDao.getGenreById(id),
            localArtworkRepository.artworkUpdates
        ) { entity, _ ->
            entity?.let {
                val domain = LibraryMapper.toDomain(it)
                val custom = localArtworkRepository.getGenreArtworkUri(domain.id)
                domain.copy(artUri = custom)
            }
        }.flowOn(ioDispatcher)
    }

    override fun getSongsByGenre(genreName: String): Flow<List<Song>> {
        return combine(
            songDao.getSongsByGenre(genreName),
            localArtworkRepository.artworkUpdates
        ) { list, _ ->
            mapSongs(list)
        }.flowOn(ioDispatcher)
    }

    override suspend fun createArtist(name: String): Result<Artist> = withContext(ioDispatcher) {
        try {
            val clean = com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(name)
            val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(name)
            if (norm.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Artist name cannot be blank"))
            }

            val existing = artistDao.findArtistByNormalizedName(norm)
            if (existing != null) {
                val domain = LibraryMapper.toDomain(existing)
                return@withContext Result.failure(
                    com.auralis.app.domain.model.DuplicateArtistException(domain)
                )
            }

            val now = System.currentTimeMillis()
            val newEntity = com.auralis.app.data.local.db.entity.ArtistEntity(
                name = clean,
                normalizedName = norm,
                albumCount = 0,
                songCount = 0,
                createdAt = now,
                updatedAt = now
            )
            val generatedId = artistDao.insertArtist(newEntity)
            val created = artistDao.findArtistById(generatedId)
                ?: newEntity.copy(id = generatedId)
            Result.success(LibraryMapper.toDomain(created))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findArtistByNormalizedName(normalizedName: String): Artist? = withContext(ioDispatcher) {
        val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(normalizedName)
        artistDao.findArtistByNormalizedName(norm)?.let {
            LibraryMapper.toDomain(it)
        }
    }

    override suspend fun findArtistByName(name: String): Artist? = withContext(ioDispatcher) {
        val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(name)
        val entity = artistDao.findArtistByNormalizedName(norm) ?: artistDao.findArtistByName(name.trim())
        entity?.let { LibraryMapper.toDomain(it) }
    }

    override suspend fun mergeArtists(sourceArtistId: Long, targetArtistId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (sourceArtistId == targetArtistId) {
                return@withContext Result.success(Unit)
            }

            val targetArtist = artistDao.findArtistById(targetArtistId)
                ?: return@withContext Result.failure(IllegalArgumentException("Target artist not found"))
            val sourceArtist = artistDao.findArtistById(sourceArtistId)

            // 1. Atomically reassign all songs from sourceArtist to targetArtist
            songDao.reassignSongsArtist(sourceArtistId, targetArtistId, targetArtist.name)

            // 2. Transfer custom artwork if target has none and source has one
            val targetArt = localArtworkRepository.getArtistArtworkUri(targetArtistId)
            val sourceArt = localArtworkRepository.getArtistArtworkUri(sourceArtistId)
            if (targetArt == null && sourceArt != null) {
                localArtworkRepository.setCustomArtwork(
                    com.auralis.app.domain.model.ArtworkEntityType.ARTIST,
                    targetArtistId.toString(),
                    android.net.Uri.parse(sourceArt)
                )
            }

            // 3. Clean up source artist override if present
            artistProfileOverrideDao.deleteOverride(sourceArtistId)

            // 4. Delete source artist profile
            artistDao.deleteArtistById(sourceArtistId)

            // 5. Recalculate artist counts for target artist
            artistDao.recalculateArtistCounts(targetArtistId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateArtistProfileName(artistId: Long, customName: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val clean = com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(customName)
            val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(customName)
            if (norm.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Artist name cannot be blank"))
            }

            val currentArtist = artistDao.findArtistById(artistId)
                ?: return@withContext Result.failure(IllegalArgumentException("Artist with ID $artistId not found"))

            // Check if another artist already has this normalized name
            val duplicate = artistDao.findArtistByNormalizedName(norm)
            if (duplicate != null && duplicate.id != artistId) {
                val domain = LibraryMapper.toDomain(duplicate)
                return@withContext Result.failure(
                    com.auralis.app.domain.model.DuplicateArtistException(domain)
                )
            }

            val now = System.currentTimeMillis()
            // Update persistent artist entity
            artistDao.updateArtistName(artistId, clean, norm, now)

            // Update associated songs' artistName display attribute
            songDao.reassignSongsArtist(artistId, artistId, clean)

            // Update override record for backward-compatibility
            artistProfileOverrideDao.insertOrUpdate(
                ArtistProfileOverrideEntity(
                    artistId = artistId,
                    originalName = currentArtist.name,
                    customName = clean,
                    updatedAt = now
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetArtistProfileName(artistId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val override = artistProfileOverrideDao.getOverride(artistId)
            if (override != null && override.originalName.isNotBlank()) {
                val orig = override.originalName
                val clean = com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(orig)
                val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(orig)
                val duplicate = artistDao.findArtistByNormalizedName(norm)
                if (duplicate == null || duplicate.id == artistId) {
                    artistDao.updateArtistName(artistId, clean, norm, System.currentTimeMillis())
                    songDao.reassignSongsArtist(artistId, artistId, clean)
                }
            }
            artistProfileOverrideDao.deleteOverride(artistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
