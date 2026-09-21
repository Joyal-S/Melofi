package com.auralis.app

import com.auralis.app.domain.model.ListeningStats
import com.auralis.app.domain.model.PlaybackHistoryItem
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.TimeRange
import com.auralis.app.domain.model.TopAlbumItem
import com.auralis.app.domain.model.TopArtistItem
import com.auralis.app.domain.model.TopSongItem
import com.auralis.app.domain.repository.HistoryRepository
import com.auralis.app.playback.history.PlaybackSessionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying playback session tracking, play-count thresholds,
 * active duration accumulation, pause/resume tracking, and duplicate prevention.
 */
class PlaybackSessionTrackerTest {

    private lateinit var fakeRepository: FakeHistoryRepository
    private lateinit var tracker: PlaybackSessionTracker
    private var currentTime = 1_000_000L

    @Before
    fun setUp() {
        fakeRepository = FakeHistoryRepository()
        tracker = PlaybackSessionTracker(fakeRepository).apply {
            timeProvider = { currentTime }
            // Run synchronously on immediate Dispatchers.Unconfined
            scope = CoroutineScope(Dispatchers.Unconfined)
        }
    }

    @Test
    fun thresholdCalculation_variousDurations() {
        // Long track: 4 minutes -> 30s threshold
        assertEquals(30_000L, tracker.calculatePlayThreshold(240_000L))

        // Medium track: 40 seconds -> 50% = 20s threshold
        assertEquals(20_000L, tracker.calculatePlayThreshold(40_000L))

        // Short track: 8 seconds -> 50% = 4s, but clamped to 5s minimum floor
        assertEquals(5_000L, tracker.calculatePlayThreshold(8_000L))

        // Unknown / 0 duration -> 30s default
        assertEquals(30_000L, tracker.calculatePlayThreshold(0L))
        assertEquals(30_000L, tracker.calculatePlayThreshold(-1L))
    }

    @Test
    fun skipBeforeThreshold_doesNotRecordPlay() {
        val songId = 101L
        val duration = 180_000L // 3 minutes

        tracker.onTrackStarted(songId, duration, isPlaying = true)

        // Advance by 15 seconds (less than 30s threshold)
        currentTime += 15_000L

        // Skip to next track
        tracker.onTrackStarted(songId = 102L, durationMs = 180_000L, isPlaying = true)

        // Verify song 101 was NOT recorded
        assertTrue("Skipped track before threshold must not be recorded", fakeRepository.recordedPlays.isEmpty())
    }

    @Test
    fun listenPastThreshold_recordsPlay() {
        val songId = 101L
        val duration = 180_000L

        tracker.onTrackStarted(songId, duration, isPlaying = true)

        // Advance by 32 seconds (exceeds 30s threshold)
        currentTime += 32_000L

        // Check progress or pause
        tracker.onPlayingChanged(isPlaying = false)

        assertEquals(1, fakeRepository.recordedPlays.size)
        val recorded = fakeRepository.recordedPlays.first()
        assertEquals(songId, recorded.songId)
        assertTrue(recorded.durationMs >= 30_000L)
        assertFalse(recorded.completed)
    }

    @Test
    fun pauseDoesNotAccumulateActiveListeningTime() {
        val songId = 101L
        val duration = 180_000L

        tracker.onTrackStarted(songId, duration, isPlaying = true)

        // Play for 10 seconds
        currentTime += 10_000L
        tracker.onPlayingChanged(isPlaying = false)

        // Paused for 5 minutes (300 seconds)
        currentTime += 300_000L

        // Resume playback for 5 seconds
        tracker.onPlayingChanged(isPlaying = true)
        currentTime += 5_000L

        // Finalize / transition
        tracker.finalizeCurrentSession()

        // Total active playing time is 10s + 5s = 15s (less than 30s threshold)
        assertTrue("Paused duration must not count towards threshold", fakeRepository.recordedPlays.isEmpty())
    }

    @Test
    fun songCompletion_marksCompleted() {
        val songId = 101L
        val duration = 50_000L // 50 seconds track (threshold = 25s)

        tracker.onTrackStarted(songId, duration, isPlaying = true)

        // Advance past threshold
        currentTime += 30_000L
        tracker.checkProgress()

        // Verify recorded
        assertEquals(1, fakeRepository.recordedPlays.size)
        val initialRecord = fakeRepository.recordedPlays.first()
        assertFalse(initialRecord.completed)

        // Finish track
        currentTime += 20_000L
        tracker.onTrackEnded()

        assertEquals(1, fakeRepository.updatedPlays.size)
        val updated = fakeRepository.updatedPlays.first()
        assertTrue("Completed track should have completed = true", updated.completed)
        assertEquals(50_000L, updated.durationMs)
    }

    @Test
    fun duplicatePrevention_continuousPlaybackDoesNotDuplicate() {
        val songId = 101L
        val duration = 200_000L

        tracker.onTrackStarted(songId, duration, isPlaying = true)

        // 35s in: crosses threshold
        currentTime += 35_000L
        tracker.checkProgress()
        assertEquals(1, fakeRepository.recordedPlays.size)

        // Continue playing to 70s
        currentTime += 35_000L
        tracker.checkProgress()
        // Should NOT record a second row
        assertEquals(1, fakeRepository.recordedPlays.size)

        // Continue playing to 100s and finalize
        currentTime += 30_000L
        tracker.finalizeCurrentSession()

        // Still only 1 record inserted, and 1 update
        assertEquals(1, fakeRepository.recordedPlays.size)
        assertEquals(1, fakeRepository.updatedPlays.size)
        assertEquals(100_000L, fakeRepository.updatedPlays.first().durationMs)
    }

    @Test
    fun pruneHistoryCalledOnRecording() {
        tracker.onTrackStarted(101L, 100_000L, isPlaying = true)
        currentTime += 35_000L
        tracker.finalizeCurrentSession()

        assertTrue(fakeRepository.pruneCalled)
    }

    private class FakeHistoryRepository : HistoryRepository {
        data class RecordedPlay(val songId: Long, val playedAt: Long, val durationMs: Long, val completed: Boolean)
        data class UpdatedPlay(val id: Long, val durationMs: Long, val completed: Boolean)

        val recordedPlays = mutableListOf<RecordedPlay>()
        val updatedPlays = mutableListOf<UpdatedPlay>()
        var pruneCalled = false
        private var nextId = 1L

        override suspend fun recordPlay(songId: Long, playedAt: Long, durationMs: Long, completed: Boolean): Long {
            val id = nextId++
            recordedPlays.add(RecordedPlay(songId, playedAt, durationMs, completed))
            return id
        }

        override suspend fun updatePlay(id: Long, durationMs: Long, completed: Boolean) {
            updatedPlays.add(UpdatedPlay(id, durationMs, completed))
        }

        override fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryItem>> = flowOf(emptyList())
        override fun getRecentlyPlayedSongs(limit: Int): Flow<List<Song>> = flowOf(emptyList())
        override fun getListeningStats(timeRange: TimeRange): Flow<ListeningStats> = flowOf(ListeningStats())
        override fun getTopSongs(timeRange: TimeRange, limit: Int): Flow<List<TopSongItem>> = flowOf(emptyList())
        override fun getTopArtists(timeRange: TimeRange, limit: Int): Flow<List<TopArtistItem>> = flowOf(emptyList())
        override fun getTopAlbums(timeRange: TimeRange, limit: Int): Flow<List<TopAlbumItem>> = flowOf(emptyList())

        override suspend fun pruneHistory(maxEntries: Int) {
            pruneCalled = true
        }

        override suspend fun clearHistory() {
            recordedPlays.clear()
            updatedPlays.clear()
        }
    }
}
