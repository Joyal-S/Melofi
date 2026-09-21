package com.auralis.app.playback.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auralis.app.MainActivity
import com.auralis.app.playback.audiofx.AudioEffectsManager
import com.auralis.app.playback.audiofx.ReplayGainManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Long-lived Media3 [MediaSessionService] managing the ExoPlayer engine and MediaSession.
 *
 * Configured for 100% offline playback with:
 * - Scoped storage ContentResolver URI playback
 * - Automatic audio focus handling
 * - Automatic audio becoming noisy (headphone disconnect) handling
 * - WakeLock for screen-off playback
 * - Foreground notification and lock screen controls
 * - Hardware audio effects (Equalizer, Bass Boost, Virtualizer) bound to audioSessionId
 * - ReplayGain metadata decoding and runtime volume normalization
 */
@AndroidEntryPoint
class AuralisPlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioEffectsManager: AudioEffectsManager

    @Inject
    lateinit var replayGainManager: ReplayGainManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        replayGainManager.bindPlayer(player)

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    audioEffectsManager.onAudioSessionIdChanged(audioSessionId)
                }
            }

            override fun onMetadata(
                eventTime: AnalyticsListener.EventTime,
                metadata: Metadata
            ) {
                replayGainManager.onMetadata(metadata, player)
            }

            override fun onMediaItemTransition(
                eventTime: AnalyticsListener.EventTime,
                mediaItem: MediaItem?,
                reason: Int
            ) {
                replayGainManager.onMediaItemTransition(player)
            }
        })

        val initialSessionId = player.audioSessionId
        if (initialSessionId != C.AUDIO_SESSION_ID_UNSET && initialSessionId > 0) {
            audioEffectsManager.onAudioSessionIdChanged(initialSessionId)
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioEffectsManager.release()
        replayGainManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
