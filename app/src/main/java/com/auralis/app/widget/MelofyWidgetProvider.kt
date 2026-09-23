package com.auralis.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.auralis.app.MainActivity
import com.auralis.app.R
import com.auralis.app.playback.service.AuralisPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Standard Android Home Screen AppWidget providing local playback status and controls.
 *
 * Architecture:
 * - Compatible with minSdk 29+ using Android platform [AppWidgetProvider] and [RemoteViews].
 * - Playback commands are dispatched via explicit Intents to [AuralisPlaybackService].
 * - Updates are 100% event-driven from Media3 player lifecycle callbacks.
 * - Artwork is safely downsampled from local storage Content URIs with zero network loading.
 */
class MelofyWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE,
            ACTION_WIDGET_NEXT,
            ACTION_WIDGET_PREVIOUS -> {
                val serviceIntent = Intent(context, AuralisPlaybackService::class.java).apply {
                    action = intent.action
                }
                context.startService(serviceIntent)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(
            context = context,
            title = null,
            artist = null,
            isPlaying = false,
            artworkUri = null
        )
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.auralis.app.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.auralis.app.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREVIOUS = "com.auralis.app.ACTION_WIDGET_PREVIOUS"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artworkUri: Uri?
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, MelofyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds == null || appWidgetIds.isEmpty()) return

            widgetScope.launch {
                val bitmap = artworkUri?.let { loadScaledArtwork(context, it) }
                val views = buildRemoteViews(context, title, artist, isPlaying, bitmap)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }

        internal fun buildRemoteViews(
            context: Context,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            artworkBitmap: Bitmap?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_playback)

            // Titles
            val displayTitle = if (!title.isNullOrBlank()) title else context.getString(R.string.app_name)
            val displayArtist = if (!artist.isNullOrBlank()) artist else context.getString(R.string.widget_no_music)
            views.setTextViewText(R.id.widget_title, displayTitle)
            views.setTextViewText(R.id.widget_artist, displayArtist)

            // Play/Pause icon
            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

            // Album artwork
            if (artworkBitmap != null) {
                views.setImageViewBitmap(R.id.widget_album_art, artworkBitmap)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_music_placeholder)
            }

            // Pending Intents
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val activityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, activityPendingIntent)

            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                createActionPendingIntent(context, ACTION_WIDGET_PREVIOUS, 1)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                createActionPendingIntent(context, ACTION_WIDGET_PLAY_PAUSE, 2)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                createActionPendingIntent(context, ACTION_WIDGET_NEXT, 3)
            )

            return views
        }

        private fun createActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MelofyWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        internal fun loadScaledArtwork(context: Context, uri: Uri): Bitmap? {
            return runCatching {
                fun openStream(): java.io.InputStream? {
                    return if (uri.scheme == "file") {
                        val path = uri.path ?: return null
                        val file = java.io.File(path)
                        if (file.exists()) file.inputStream() else null
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                }

                openStream()?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(stream, null, options)

                    val targetSize = 256
                    var sampleSize = 1
                    if (options.outHeight > targetSize || options.outWidth > targetSize) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while ((halfHeight / sampleSize) >= targetSize && (halfWidth / sampleSize) >= targetSize) {
                            sampleSize *= 2
                        }
                    }

                    openStream()?.use { secondStream ->
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                    }
                }
            }.getOrNull()
        }
    }
}
