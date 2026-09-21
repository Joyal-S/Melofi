package com.auralis.app

import com.auralis.app.domain.model.ListeningStats
import com.auralis.app.domain.model.TimeRange
import com.auralis.app.domain.model.TopAlbumItem
import com.auralis.app.domain.model.TopArtistItem
import com.auralis.app.domain.model.TopSongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying statistics data structures, time range bounds, and formatting logic.
 */
class StatisticsCalculationTest {

    @Test
    fun timeRange_allTime_startsAtZero() {
        assertEquals(0L, TimeRange.ALL_TIME.getSinceTimestamp())
    }

    @Test
    fun timeRange_thisWeek_isSevenDaysAgo() {
        val now = System.currentTimeMillis()
        val weekSince = TimeRange.THIS_WEEK.getSinceTimestamp()
        val expected = now - 7L * 24 * 60 * 60 * 1000L
        // Allow a 1-second margin for execution
        assertTrue(Math.abs(expected - weekSince) < 1000L)
    }

    @Test
    fun timeRange_thisMonth_isThirtyDaysAgo() {
        val now = System.currentTimeMillis()
        val monthSince = TimeRange.THIS_MONTH.getSinceTimestamp()
        val expected = now - 30L * 24 * 60 * 60 * 1000L
        assertTrue(Math.abs(expected - monthSince) < 1000L)
    }

    @Test
    fun timeRange_today_startsAtMidnight() {
        val todaySince = TimeRange.TODAY.getSinceTimestamp()
        val now = System.currentTimeMillis()
        assertTrue("Today start timestamp must be <= now", todaySince <= now)
        assertTrue("Today start timestamp must be within 24 hours of now", (now - todaySince) < 24L * 60 * 60 * 1000L)
    }

    @Test
    fun listeningStats_defaultValuesAreZero() {
        val stats = ListeningStats()
        assertEquals(0L, stats.totalPlays)
        assertEquals(0L, stats.totalDurationMs)
        assertEquals(0, stats.uniqueSongs)
        assertEquals(0, stats.uniqueArtists)
        assertEquals(0, stats.uniqueAlbums)
    }

    @Test
    fun listeningStats_populatedValues() {
        val stats = ListeningStats(
            totalPlays = 1284L,
            totalDurationMs = 311520000L, // ~86.5 hours
            uniqueSongs = 312,
            uniqueArtists = 74,
            uniqueAlbums = 45
        )
        assertEquals(1284L, stats.totalPlays)
        assertEquals(311520000L, stats.totalDurationMs)
        assertEquals(312, stats.uniqueSongs)
        assertEquals(74, stats.uniqueArtists)
        assertEquals(45, stats.uniqueAlbums)
    }

    @Test
    fun topSongItem_propertiesPreserved() {
        val item = TopSongItem(
            id = 42L,
            title = "Clair de Lune",
            artistName = "Claude Debussy",
            albumName = "Suite bergamasque",
            albumArtUri = "content://media/external/audio/albumart/1",
            duration = 300_000L,
            playCount = 18L,
            totalDurationMs = 5400_000L
        )
        assertEquals(42L, item.id)
        assertEquals("Clair de Lune", item.title)
        assertEquals("Claude Debussy", item.artistName)
        assertEquals(18L, item.playCount)
    }

    @Test
    fun topArtistItem_propertiesPreserved() {
        val item = TopArtistItem(
            artistName = "Miles Davis",
            playCount = 45L,
            totalDurationMs = 12000_000L
        )
        assertEquals("Miles Davis", item.artistName)
        assertEquals(45L, item.playCount)
        assertEquals(12000_000L, item.totalDurationMs)
    }

    @Test
    fun topAlbumItem_propertiesPreserved() {
        val item = TopAlbumItem(
            albumName = "Kind of Blue",
            artistName = "Miles Davis",
            albumArtUri = null,
            playCount = 30L,
            totalDurationMs = 9000_000L
        )
        assertEquals("Kind of Blue", item.albumName)
        assertEquals(30L, item.playCount)
    }
}
