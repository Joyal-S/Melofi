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
import com.auralis.app.playback.history.PlaybackSessionTracker
import com.auralis.app.playback.mapper.MediaItemMapper
import com.auralis.app.widget.MelofyWidgetProvider
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
 * - Playback session tracking and listening history recording
 * - Home screen AppWidget event-driven state updates
 */
@AndroidEntryPoint
class AuralisPlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioEffectsManager: AudioEffectsManager

    @Inject
    lateinit var replayGainManager: ReplayGainManager

    @Inject
    lateinit var playbackSessionTracker: PlaybackSessionTracker

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
                val songId = MediaItemMapper.extractSongId(mediaItem)
                if (songId != null) {
                    val duration = if (player.duration > 0) player.duration else 0L
                    playbackSessionTracker.onTrackStarted(songId, duration, player.isPlaying)
                } else {
                    playbackSessionTracker.finalizeCurrentSession()
                }
                updateWidget(player)
            }

            override fun onIsPlayingChanged(
                eventTime: AnalyticsListener.EventTime,
                isPlaying: Boolean
            ) {
                playbackSessionTracker.onPlayingChanged(isPlaying)
                updateWidget(player)
            }

            override fun onPlaybackStateChanged(
                eventTime: AnalyticsListener.EventTime,
                state: Int
            ) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    playbackSessionTracker.onTrackEnded()
                }
                updateWidget(player)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MelofyWidgetProvider.ACTION_WIDGET_PLAY_PAUSE -> {
                val player = mediaSession?.player
                if (player != null) {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
            MelofyWidgetProvider.ACTION_WIDGET_NEXT -> {
                val player = mediaSession?.player
                if (player?.hasNextMediaItem() == true) {
                    player.seekToNextMediaItem()
                }
            }
            MelofyWidgetProvider.ACTION_WIDGET_PREVIOUS -> {
                val player = mediaSession?.player
                if (player != null) {
                    if (player.currentPosition > 3000L) {
                        player.seekTo(0L)
                    } else if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    } else {
                        player.seekTo(0L)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
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
        playbackSessionTracker.finalizeCurrentSession()
        MelofyWidgetProvider.updateAllWidgets(this, null, null, false, null)
        audioEffectsManager.release()
        replayGainManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun updateWidget(player: androidx.media3.common.Player) {
        val mediaItem = player.currentMediaItem
        val title = mediaItem?.mediaMetadata?.title?.toString()
        val artist = mediaItem?.mediaMetadata?.artist?.toString()
        val artworkUri = mediaItem?.mediaMetadata?.artworkUri
        MelofyWidgetProvider.updateAllWidgets(
            context = this,
            title = title,
            artist = artist,
            isPlaying = player.isPlaying,
            artworkUri = artworkUri
        )
    }
}
