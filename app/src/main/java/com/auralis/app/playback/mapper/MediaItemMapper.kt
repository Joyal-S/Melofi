package com.auralis.app.playback.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.auralis.app.domain.model.Song

/**
 * Deterministic mapper between domain [Song] and AndroidX Media3 [MediaItem].
 */
object MediaItemMapper {

    /**
     * Converts a domain [Song] to a Media3 [MediaItem] with content URI and rich metadata.
     */
    fun toMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artistName)
            .setAlbumTitle(song.albumName)
            .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(Uri.parse(song.contentUri))
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Extracts the deterministic song ID from a Media3 [MediaItem].
     */
    fun extractSongId(mediaItem: MediaItem?): Long? {
        return mediaItem?.mediaId?.toLongOrNull()
    }

    /**
     * Extracts the deterministic song ID from a mediaId string.
     */
    fun extractSongId(mediaId: String?): Long? {
        return mediaId?.toLongOrNull()
    }

    /**
     * Converts a list of [Song]s to a list of [MediaItem]s.
     */
    fun toMediaItemList(songs: List<Song>): List<MediaItem> {
        return songs.map { toMediaItem(it) }
    }
}
