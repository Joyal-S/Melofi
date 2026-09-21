package com.auralis.app

import com.auralis.app.playback.audiofx.SleepTimerManager
import com.auralis.app.playback.audiofx.SleepTimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unit tests for SleepTimerManager countdown, cancellation, reset, and expiration.
 */
class SleepTimerManagerTest {

    @Test
    fun initialState_isInactive() {
        val manager = SleepTimerManager(onExpireAction = {})
        assertEquals(SleepTimerState.Inactive, manager.timerState.value)
    }

    @Test
    fun startSeconds_transitionsToRunningState() {
        val manager = SleepTimerManager(
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 50L,
            onExpireAction = {}
        )

        manager.startSeconds(120L)
        val state = manager.timerState.value
        assertTrue(state is SleepTimerState.Running)
        val running = state as SleepTimerState.Running
        assertEquals(120L, running.totalDurationSeconds)
    }

    @Test
    fun cancel_resetsToInactive() {
        val manager = SleepTimerManager(
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 50L,
            onExpireAction = {}
        )

        manager.startSeconds(300L)
        assertTrue(manager.timerState.value is SleepTimerState.Running)

        manager.cancel()
        assertEquals(SleepTimerState.Inactive, manager.timerState.value)
    }

    @Test
    fun reset_resetsToInactive() {
        val manager = SleepTimerManager(
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 50L,
            onExpireAction = {}
        )

        manager.startSeconds(60L)
        manager.reset()
        assertEquals(SleepTimerState.Inactive, manager.timerState.value)
    }

    @Test
    fun timerExpiration_transitionsToCompletedAndTriggersAction() = runBlocking {
        val expired = AtomicBoolean(false)
        val manager = SleepTimerManager(
            dispatcher = Dispatchers.Default,
            tickIntervalMs = 15L,
            onExpireAction = { expired.set(true) }
        )

        // Run a short 2-second timer (with 15ms ticks = ~30ms total test time)
        manager.startSeconds(2L)

        // Wait up to 1 second for completion
        var attempts = 0
        while (manager.timerState.value !is SleepTimerState.Completed && attempts < 50) {
            kotlinx.coroutines.delay(20L)
            attempts++
        }

        assertEquals(SleepTimerState.Completed, manager.timerState.value)
        assertTrue(expired.get())
    }

    @Test
    fun restartTimer_cancelsPreviousTimerCleanly() {
        val manager = SleepTimerManager(
            dispatcher = Dispatchers.Unconfined,
            tickIntervalMs = 50L,
            onExpireAction = {}
        )

        manager.startSeconds(100L)
        val firstState = manager.timerState.value as SleepTimerState.Running
        assertEquals(100L, firstState.totalDurationSeconds)

        manager.startSeconds(500L)
        val secondState = manager.timerState.value as SleepTimerState.Running
        assertEquals(500L, secondState.totalDurationSeconds)
    }
}
