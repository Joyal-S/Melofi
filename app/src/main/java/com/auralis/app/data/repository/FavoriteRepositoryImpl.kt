package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.FavoriteDao
import com.auralis.app.data.local.db.entity.FavoriteEntity
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [FavoriteRepository] backed by Room [FavoriteDao].
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FavoriteRepository {

    override fun getFavoriteSongIds(): Flow<Set<Long>> {
        return favoriteDao.getFavoriteSongIds()
            .map { it.toSet() }
            .flowOn(ioDispatcher)
    }

    override fun getFavoriteSongs(): Flow<List<Song>> {
        return favoriteDao.getFavoriteSongs()
            .map { SongMapper.toDomainList(it) }
            .flowOn(ioDispatcher)
    }

    override suspend fun isFavorite(songId: Long): Boolean = withContext(ioDispatcher) {
        favoriteDao.isFavorite(songId)
    }

    override suspend fun toggleFavorite(songId: Long): Boolean = withContext(ioDispatcher) {
        val currentlyFav = favoriteDao.isFavorite(songId)
        if (currentlyFav) {
            favoriteDao.deleteFavorite(songId)
            false
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(songId = songId))
            true
        }
    }

    override suspend fun addFavorite(songId: Long) = withContext(ioDispatcher) {
        favoriteDao.insertFavorite(FavoriteEntity(songId = songId))
    }

    override suspend fun removeFavorite(songId: Long) = withContext(ioDispatcher) {
        favoriteDao.deleteFavorite(songId)
    }
}
