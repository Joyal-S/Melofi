package com.auralis.app.playback.history

import com.auralis.app.domain.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * State machine managing playback session tracking and listening history recording.
 *
 * Implements the Melofy play-count policy:
 * - A play is counted when the user has listened for at least 30 seconds OR 50% of the track duration
 *   (whichever is reached first, with a 5-second floor for short clips).
 * - Only active playing time (while isPlaying == true) is accumulated. Pauses, buffering, and seeks
 *   do not inflate listening time.
 * - Skips before the threshold are discarded without recording history.
 * - Once the threshold is crossed, the play is recorded in Room.
 * - On track completion or transition, the record is updated with final duration and completion status.
 * - Entirely non-blocking and fault-tolerant: database operations run in background coroutines with
 *   fail-safe error handling so playback is never interrupted.
 */
@Singleton
class PlaybackSessionTracker @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    // Overridable for testing
    internal var timeProvider: () -> Long = { System.currentTimeMillis() }
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activeSongId: Long? = null

    @Volatile
    private var sessionStartTime: Long = 0L

    @Volatile
    private var trackDurationMs: Long = 0L

    @Volatile
    private var accumulatedPlayTimeMs: Long = 0L

    @Volatile
    private var lastResumeTimestamp: Long? = null

    @Volatile
    private var historyRecordId: Long? = null

    @Volatile
    private var hasRecordedPlay: Boolean = false

    @Volatile
    private var isCompleted: Boolean = false

    /**
     * Calculates the threshold needed to qualify as a play.
     */
    fun calculatePlayThreshold(durationMs: Long): Long {
        if (durationMs <= 0L) return 30_000L
        val halfDuration = (durationMs * 0.5).toLong().coerceAtLeast(5_000L)
        return min(30_000L, halfDuration)
    }

    /**
     * Called when a new track starts playing or player transitions to a new MediaItem.
     */
    @Synchronized
    fun onTrackStarted(songId: Long, durationMs: Long, isPlaying: Boolean) {
        // Finalize previous session if exists
        finalizeCurrentSession()

        val now = timeProvider()
        activeSongId = songId
        trackDurationMs = durationMs
        sessionStartTime = now
        accumulatedPlayTimeMs = 0L
        lastResumeTimestamp = if (isPlaying) now else null
        historyRecordId = null
        hasRecordedPlay = false
        isCompleted = false
    }

    /**
     * Called when isPlaying state changes.
     */
    @Synchronized
    fun onPlayingChanged(isPlaying: Boolean) {
        val songId = activeSongId ?: return
        val now = timeProvider()

        if (isPlaying) {
            if (lastResumeTimestamp == null) {
                lastResumeTimestamp = now
            }
        } else {
            lastResumeTimestamp?.let { start ->
                accumulatedPlayTimeMs += (now - start).coerceAtLeast(0L)
                lastResumeTimestamp = null
            }
            checkAndRecordThreshold(songId)
        }
    }

    /**
     * Called periodically or on state change to check if threshold has been crossed.
     */
    @Synchronized
    fun checkProgress() {
        val songId = activeSongId ?: return
        flushActiveTime()
        checkAndRecordThreshold(songId)
    }

    /**
     * Called when the player reaches STATE_ENDED (song played to the very end).
     */
    @Synchronized
    fun onTrackEnded() {
        isCompleted = true
        finalizeCurrentSession()
    }

    /**
     * Finalizes and persists the active session when track changes, service stops, or song ends.
     */
    @Synchronized
    fun finalizeCurrentSession() {
        val songId = activeSongId ?: return
        flushActiveTime()

        val threshold = calculatePlayThreshold(trackDurationMs)
        val finalDuration = accumulatedPlayTimeMs
        val completed = isCompleted || (trackDurationMs > 0 && finalDuration >= (trackDurationMs * 0.95))
        val recordId = historyRecordId
        val startTime = sessionStartTime

        if (finalDuration >= threshold) {
            scope.launch {
                runCatching {
                    if (recordId != null) {
                        historyRepository.updatePlay(
                            id = recordId,
                            durationMs = finalDuration,
                            completed = completed
                        )
                    } else {
                        val newId = historyRepository.recordPlay(
                            songId = songId,
                            playedAt = startTime,
                            durationMs = finalDuration,
                            completed = completed
                        )
                        historyRepository.pruneHistory(10000)
                    }
                }
            }
        }

        // Reset state
        activeSongId = null
        sessionStartTime = 0L
        trackDurationMs = 0L
        accumulatedPlayTimeMs = 0L
        lastResumeTimestamp = null
        historyRecordId = null
        hasRecordedPlay = false
        isCompleted = false
    }

    private fun flushActiveTime() {
        val now = timeProvider()
        lastResumeTimestamp?.let { start ->
            accumulatedPlayTimeMs += (now - start).coerceAtLeast(0L)
            lastResumeTimestamp = now
        }
    }

    private fun checkAndRecordThreshold(songId: Long) {
        val threshold = calculatePlayThreshold(trackDurationMs)
        if (accumulatedPlayTimeMs >= threshold && !hasRecordedPlay) {
            hasRecordedPlay = true
            val startTime = sessionStartTime
            val duration = accumulatedPlayTimeMs
            val completed = isCompleted

            scope.launch {
                runCatching {
                    val id = historyRepository.recordPlay(
                        songId = songId,
                        playedAt = startTime,
                        durationMs = duration,
                        completed = completed
                    )
                    historyRecordId = id
                }
            }
        }
    }

    // For test inspection
    internal fun getAccumulatedPlayTimeMs(): Long {
        var total = accumulatedPlayTimeMs
        val last = lastResumeTimestamp
        if (last != null) {
            total += (timeProvider() - last).coerceAtLeast(0L)
        }
        return total
    }

    internal fun isPlayRecorded(): Boolean = hasRecordedPlay
    internal fun getActiveSongId(): Long? = activeSongId
}
