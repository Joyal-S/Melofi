package com.auralis.app.data.repository

import com.auralis.app.data.local.db.dao.HistoryDao
import com.auralis.app.data.local.db.entity.PlaybackHistoryEntity
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.domain.model.ListeningStats
import com.auralis.app.domain.model.PlaybackHistoryItem
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.TimeRange
import com.auralis.app.domain.model.TopAlbumItem
import com.auralis.app.domain.model.TopArtistItem
import com.auralis.app.domain.model.TopSongItem
import com.auralis.app.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [HistoryRepository] reading and writing to Room.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override suspend fun recordPlay(
        songId: Long,
        playedAt: Long,
        durationMs: Long,
        completed: Boolean
    ): Long {
        return historyDao.insertHistory(
            PlaybackHistoryEntity(
                songId = songId,
                playedAt = playedAt,
                durationMs = durationMs,
                completed = completed
            )
        )
    }

    override suspend fun updatePlay(id: Long, durationMs: Long, completed: Boolean) {
        historyDao.updateHistoryDurationAndCompletion(id, durationMs, completed)
    }

    override fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryItem>> {
        return historyDao.getRecentHistoryEntries(limit).map { entities ->
            entities.map { entity ->
                PlaybackHistoryItem(
                    id = entity.id,
                    songId = entity.songId,
                    playedAt = entity.playedAt,
                    durationMs = entity.durationMs,
                    completed = entity.completed
                )
            }
        }
    }

    override fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>> {
        return historyDao.getRecentlyPlayedSongs(limit).map { entities ->
            SongMapper.toDomainList(entities)
        }
    }

    override fun getListeningStats(timeRange: TimeRange): Flow<ListeningStats> {
        val since = timeRange.getSinceTimestamp()
        return combine(
            historyDao.getTotalPlayCount(since),
            historyDao.getTotalListeningDurationMs(since),
            historyDao.getUniqueSongsPlayedCount(since),
            historyDao.getUniqueArtistsPlayedCount(since),
            historyDao.getUniqueAlbumsPlayedCount(since)
        ) { totalPlays, totalDurationMs, uniqueSongs, uniqueArtists, uniqueAlbums ->
            ListeningStats(
                totalPlays = totalPlays,
                totalDurationMs = totalDurationMs,
                uniqueSongs = uniqueSongs,
                uniqueArtists = uniqueArtists,
                uniqueAlbums = uniqueAlbums
            )
        }
    }

    override fun getTopSongs(timeRange: TimeRange, limit: Int): Flow<List<TopSongItem>> {
        return historyDao.getTopSongs(timeRange.getSinceTimestamp(), limit).map { items ->
            items.map { item ->
                TopSongItem(
                    id = item.id,
                    title = item.title,
                    artistName = item.artistName,
                    albumName = item.albumName,
                    albumArtUri = item.albumArtUri,
                    duration = item.duration,
                    playCount = item.playCount,
                    totalDurationMs = item.totalDurationMs
                )
            }
        }
    }

    override fun getTopArtists(timeRange: TimeRange, limit: Int): Flow<List<TopArtistItem>> {
        return historyDao.getTopArtists(timeRange.getSinceTimestamp(), limit).map { items ->
            items.map { item ->
                TopArtistItem(
                    artistName = item.artistName,
                    playCount = item.playCount,
                    totalDurationMs = item.totalDurationMs
                )
            }
        }
    }

    override fun getTopAlbums(timeRange: TimeRange, limit: Int): Flow<List<TopAlbumItem>> {
        return historyDao.getTopAlbums(timeRange.getSinceTimestamp(), limit).map { items ->
            items.map { item ->
                TopAlbumItem(
                    albumName = item.albumName,
                    artistName = item.artistName,
                    albumArtUri = item.albumArtUri,
                    playCount = item.playCount,
                    totalDurationMs = item.totalDurationMs
                )
            }
        }
    }

    override suspend fun pruneHistory(maxEntries: Int) {
        historyDao.pruneHistory(maxEntries)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
