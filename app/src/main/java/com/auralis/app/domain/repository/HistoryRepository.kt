package com.auralis.app.domain.repository

import com.auralis.app.domain.model.ListeningStats
import com.auralis.app.domain.model.PlaybackHistoryItem
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.TimeRange
import com.auralis.app.domain.model.TopAlbumItem
import com.auralis.app.domain.model.TopArtistItem
import com.auralis.app.domain.model.TopSongItem
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository for listening history, playback tracking, and listening statistics.
 */
interface HistoryRepository {

    suspend fun recordPlay(songId: Long, playedAt: Long, durationMs: Long, completed: Boolean): Long

    suspend fun updatePlay(id: Long, durationMs: Long, completed: Boolean)

    fun getRecentHistory(limit: Int = 50): Flow<List<PlaybackHistoryItem>>

    fun getRecentlyPlayedSongs(limit: Int = 50): Flow<List<Song>>

    fun getListeningStats(timeRange: TimeRange): Flow<ListeningStats>

    fun getTopSongs(timeRange: TimeRange, limit: Int = 10): Flow<List<TopSongItem>>

    fun getTopArtists(timeRange: TimeRange, limit: Int = 10): Flow<List<TopArtistItem>>

    fun getTopAlbums(timeRange: TimeRange, limit: Int = 10): Flow<List<TopAlbumItem>>

    suspend fun pruneHistory(maxEntries: Int = 10000)

    suspend fun clearHistory()
}
