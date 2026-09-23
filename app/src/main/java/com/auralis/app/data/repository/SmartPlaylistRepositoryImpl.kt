package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.FavoriteDao
import com.auralis.app.data.local.db.dao.HistoryDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.domain.model.SmartPlaylistType
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.SmartPlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [SmartPlaylistRepository] executing targeted SQL queries.
 */
@Singleton
class SmartPlaylistRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val localArtworkRepository: LocalArtworkRepository
) : SmartPlaylistRepository {

    override fun getSmartPlaylists(): List<SmartPlaylistType> {
        return SmartPlaylistType.entries
    }

    private fun mapSongs(entities: List<com.auralis.app.data.local.db.entity.SongEntity>): List<Song> {
        return entities.map { entity ->
            val domain = SongMapper.toDomain(entity)
            val art = localArtworkRepository.resolveSongArtworkFast(domain)
            domain.copy(albumArtUri = art)
        }
    }

    override fun getSongsForSmartPlaylist(type: SmartPlaylistType): Flow<List<Song>> {
        return when (type) {
            SmartPlaylistType.RECENTLY_PLAYED -> {
                historyDao.getRecentlyPlayedSongs(limit = 50).map {
                    mapSongs(it)
                }
            }
            SmartPlaylistType.MOST_PLAYED -> {
                historyDao.getMostPlayedSongs(sinceTimestamp = 0L, limit = 50).map {
                    mapSongs(it)
                }
            }
            SmartPlaylistType.NEVER_PLAYED -> {
                historyDao.getNeverPlayedSongs(limit = 100).map {
                    mapSongs(it)
                }
            }
            SmartPlaylistType.RECENTLY_ADDED -> {
                songDao.getSongsSortedByDateAdded().map { entities ->
                    mapSongs(entities.take(50))
                }
            }
            SmartPlaylistType.FAVORITES -> {
                favoriteDao.getFavoriteSongs().map {
                    mapSongs(it)
                }
            }
            SmartPlaylistType.FORGOTTEN_FAVORITES -> {
                val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
                historyDao.getForgottenFavorites(cutoffTimestamp = thirtyDaysAgo, limit = 50).map {
                    mapSongs(it)
                }
            }
        }
    }
}
