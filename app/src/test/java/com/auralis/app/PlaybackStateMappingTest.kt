package com.auralis.app

import androidx.media3.common.Player
import com.auralis.app.playback.client.PlaybackController
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.playback.model.PlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for mapping logic between Media3 player states and domain [PlaybackStatus] / [PlaybackRepeatMode].
 */
class PlaybackStateMappingTest {

    @Test
    fun mapPlaybackStatus_hasError_returnsError() {
        val status = PlaybackController.mapPlaybackStatus(
            playbackState = Player.STATE_READY,
            isPlaying = true,
            hasError = true
        )
        assertEquals(PlaybackStatus.ERROR, status)
    }

    @Test
    fun mapPlaybackStatus_idle_returnsIdle() {
        val status = PlaybackController.mapPlaybackStatus(
            playbackState = Player.STATE_IDLE,
            isPlaying = false,
            hasError = false
        )
        assertEquals(PlaybackStatus.IDLE, status)
    }

    @Test
    fun mapPlaybackStatus_buffering_returnsBuffering() {
        val status = PlaybackController.mapPlaybackStatus(
            playbackState = Player.STATE_BUFFERING,
            isPlaying = false,
            hasError = false
        )
        assertEquals(PlaybackStatus.BUFFERING, status)
    }

    @Test
    fun mapPlaybackStatus_readyAndPlaying_returnsPlaying() {
        val status = PlaybackController.mapPlaybackStatus(
            playbackState = Player.STATE_READY,
            isPlaying = true,
            hasError = false
        )
        assertEquals(PlaybackStatus.PLAYING, status)
    }

    @Test
    fun mapPlaybackStatus_readyAndPaused_returnsReady() {
        val status = PlaybackController.mapPlaybackStatus(
            playbackState = Player.STATE_READY,
            isPlaying = false,
            hasError = false
        )
        assertEquals(PlaybackStatus.READY, status)
    }

    @Test
    fun mapPlaybackStatus_ended_returnsEnded() {
        val status = PlaybackController.mapPlaybackStatus(
            playbackState = Player.STATE_ENDED,
            isPlaying = false,
            hasError = false
        )
        assertEquals(PlaybackStatus.ENDED, status)
    }

    @Test
    fun mapMedia3RepeatMode_correctlyMapsAllModes() {
        assertEquals(
            PlaybackRepeatMode.OFF,
            PlaybackController.mapMedia3RepeatMode(Player.REPEAT_MODE_OFF)
        )
        assertEquals(
            PlaybackRepeatMode.ONE,
            PlaybackController.mapMedia3RepeatMode(Player.REPEAT_MODE_ONE)
        )
        assertEquals(
            PlaybackRepeatMode.ALL,
            PlaybackController.mapMedia3RepeatMode(Player.REPEAT_MODE_ALL)
        )
    }

    @Test
    fun toMedia3RepeatMode_correctlyMapsAllModes() {
        assertEquals(
            Player.REPEAT_MODE_OFF,
            PlaybackController.toMedia3RepeatMode(PlaybackRepeatMode.OFF)
        )
        assertEquals(
            Player.REPEAT_MODE_ONE,
            PlaybackController.toMedia3RepeatMode(PlaybackRepeatMode.ONE)
        )
        assertEquals(
            Player.REPEAT_MODE_ALL,
            PlaybackController.toMedia3RepeatMode(PlaybackRepeatMode.ALL)
        )
    }
}
