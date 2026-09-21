package com.auralis.app.playback.audiofx

import com.auralis.app.playback.client.PlaybackController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages countdown sleep timer using structured concurrency.
 *
 * Automatically pauses playback via [PlaybackController] when the timer reaches zero.
 */
@Singleton
class SleepTimerManager(
    private val playbackController: PlaybackController? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val tickIntervalMs: Long = 1000L,
    private val onExpireAction: (() -> Unit)? = null
) {

    @Inject
    constructor(playbackController: PlaybackController) : this(
        playbackController = playbackController,
        dispatcher = Dispatchers.Default,
        tickIntervalMs = 1000L,
        onExpireAction = null
    )

    companion object {
        const val MAX_DURATION_SECONDS = 86_400L // 24 hours maximum
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var timerJob: Job? = null

    private val _timerState = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    fun startMinutes(minutes: Int) {
        if (minutes <= 0) return
        val clampedMinutes = minutes.coerceAtMost(1440)
        startSeconds(clampedMinutes.toLong() * 60L)
    }

    fun startSeconds(totalSeconds: Long) {
        if (totalSeconds <= 0L) return
        val clampedDuration = totalSeconds.coerceAtMost(MAX_DURATION_SECONDS)

        cancel()

        timerJob = scope.launch {
            var remaining = clampedDuration
            _timerState.value = SleepTimerState.Running(
                remainingSeconds = remaining,
                totalDurationSeconds = clampedDuration
            )

            while (isActive && remaining > 0) {
                delay(tickIntervalMs)
                remaining--
                if (remaining > 0) {
                    _timerState.value = SleepTimerState.Running(
                        remainingSeconds = remaining,
                        totalDurationSeconds = totalSeconds
                    )
                }
            }

            if (isActive && remaining <= 0) {
                _timerState.value = SleepTimerState.Completed
                onExpireAction?.invoke() ?: playbackController?.pause()
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = SleepTimerState.Inactive
    }

    fun reset() {
        cancel()
    }
}
