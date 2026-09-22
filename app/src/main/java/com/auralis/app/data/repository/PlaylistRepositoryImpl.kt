package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.PlaylistDao
import com.auralis.app.data.local.db.entity.PlaylistEntity
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PlaylistRepository] backed by Room [PlaylistDao].
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val localArtworkRepository: LocalArtworkRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsWithCount()
            .map { list ->
                list.map { entity ->
                    val cover = entity.customCoverUri ?: localArtworkRepository.getPlaylistArtworkUri(entity.id)
                    Playlist(
                        id = entity.id,
                        name = entity.name,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        songCount = entity.songCount,
                        customCoverUri = cover,
                        description = entity.description
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun getPlaylist(id: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistById(id)
            .map { entity ->
                entity?.let {
                    val cover = it.customCoverUri ?: localArtworkRepository.getPlaylistArtworkUri(it.id)
                    Playlist(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        customCoverUri = cover,
                        description = it.description
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId)
            .map { SongMapper.toDomainList(it) }
            .flowOn(ioDispatcher)
    }

    override suspend fun createPlaylist(name: String): Result<Long> = withContext(ioDispatcher) {
        try {
            val entity = PlaylistEntity(name = name)
            val id = playlistDao.insertPlaylist(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renamePlaylist(id: Long, name: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val existing = playlistDao.getPlaylistByName(name)
            val entity = PlaylistEntity(
                id = id,
                name = name,
                customCoverUri = existing?.customCoverUri,
                description = existing?.description,
                updatedAt = System.currentTimeMillis()
            )
            playlistDao.updatePlaylist(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(id: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            playlistDao.deletePlaylistById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            playlistDao.addSongToEnd(playlistId, songId)
            localArtworkRepository.invalidatePlaylistCollage(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            playlistDao.removeSongAndCompact(playlistId, songId)
            localArtworkRepository.invalidatePlaylistCollage(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>): Result<Unit> = withContext(ioDispatcher) {
        try {
            playlistDao.reorderSongs(playlistId, orderedSongIds)
            localArtworkRepository.invalidatePlaylistCollage(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePlaylistCover(playlistId: Long, coverUri: String?): Result<Unit> = withContext(ioDispatcher) {
        try {
            playlistDao.updatePlaylistCover(playlistId, coverUri)
            localArtworkRepository.invalidatePlaylistCollage(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePlaylistDescription(playlistId: Long, description: String?): Result<Unit> = withContext(ioDispatcher) {
        try {
            playlistDao.updatePlaylistDescription(playlistId, description)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
