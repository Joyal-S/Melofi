package com.auralis.app.domain.model

import java.util.Calendar

/**
 * Filter time ranges for listening statistics.
 */
enum class TimeRange(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time");

    fun getSinceTimestamp(): Long {
        val now = System.currentTimeMillis()
        return when (this) {
            TODAY -> {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            }
            THIS_WEEK -> now - 7L * 24 * 60 * 60 * 1000L
            THIS_MONTH -> now - 30L * 24 * 60 * 60 * 1000L
            ALL_TIME -> 0L
        }
    }
}

/**
 * High-level listening statistics overview for a given time period.
 */
data class ListeningStats(
    val totalPlays: Long = 0L,
    val totalDurationMs: Long = 0L,
    val uniqueSongs: Int = 0,
    val uniqueArtists: Int = 0,
    val uniqueAlbums: Int = 0
)

/**
 * Ranked top song with playback counts.
 */
data class TopSongItem(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumArtUri: String?,
    val duration: Long,
    val playCount: Long,
    val totalDurationMs: Long
)

/**
 * Ranked top artist with playback counts.
 */
data class TopArtistItem(
    val artistName: String,
    val playCount: Long,
    val totalDurationMs: Long
)

/**
 * Ranked top album with playback counts.
 */
data class TopAlbumItem(
    val albumName: String,
    val artistName: String,
    val albumArtUri: String?,
    val playCount: Long,
    val totalDurationMs: Long
)

/**
 * Listening history event item.
 */
data class PlaybackHistoryItem(
    val id: Long,
    val songId: Long,
    val playedAt: Long,
    val durationMs: Long,
    val completed: Boolean
)
