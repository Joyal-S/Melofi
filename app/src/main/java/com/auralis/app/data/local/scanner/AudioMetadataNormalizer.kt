package com.auralis.app.data.local.scanner

/**
 * Pure Kotlin normalization utility for audio metadata extracted from MediaStore.
 *
 * Ensures no null/blank string anomalies or corrupted values enter the database,
 * and standardizes URIs and track/disc numbers.
 */
object AudioMetadataNormalizer {

    const val UNKNOWN_TITLE = "Unknown Title"
    const val UNKNOWN_ARTIST = "Unknown Artist"
    const val UNKNOWN_ALBUM = "Unknown Album"
    private const val MEDIASTORE_ALBUM_ART_PREFIX = "content://media/external/audio/albumart"
    private const val MEDIASTORE_AUDIO_MEDIA_PREFIX = "content://media/external/audio/media"

    /**
     * Normalizes a song title. Falls back to stripped display name, or [UNKNOWN_TITLE].
     */
    fun normalizeTitle(rawTitle: String?, rawDisplayName: String?): String {
        val trimmedTitle = rawTitle?.trim()
        if (!trimmedTitle.isNullOrEmpty() && !trimmedTitle.equals("<unknown>", ignoreCase = true)) {
            return trimmedTitle
        }
        val trimmedDisplay = rawDisplayName?.trim()
        if (!trimmedDisplay.isNullOrEmpty()) {
            val withoutExtension = trimmedDisplay.substringBeforeLast('.')
            if (withoutExtension.isNotBlank()) {
                return withoutExtension
            }
        }
        return UNKNOWN_TITLE
    }

    /**
     * Normalizes an artist name. Falls back to [UNKNOWN_ARTIST].
     */
    fun normalizeArtist(rawArtist: String?): String {
        val trimmed = rawArtist?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) {
            return UNKNOWN_ARTIST
        }
        return trimmed
    }

    /**
     * Normalizes an album name. Falls back to [UNKNOWN_ALBUM].
     */
    fun normalizeAlbum(rawAlbum: String?): String {
        val trimmed = rawAlbum?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) {
            return UNKNOWN_ALBUM
        }
        return trimmed
    }

    /**
     * Normalizes genre name. Returns null if unknown or empty.
     */
    fun normalizeGenre(rawGenre: String?): String? {
        val trimmed = rawGenre?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) {
            return null
        }
        return trimmed
    }

    /**
     * Normalizes duration in milliseconds. Negative or zero values become 0L.
     */
    fun normalizeDuration(rawDuration: Long?): Long {
        return if (rawDuration != null && rawDuration > 0L) rawDuration else 0L
    }

    /**
     * Parses MediaStore raw track number which may encode both disc and track
     * (e.g., 1004 for Disc 1, Track 4).
     *
     * @return Pair(discNumber, trackNumber)
     */
    fun parseTrackAndDiscNumber(rawTrack: Int?): Pair<Int?, Int?> {
        if (rawTrack == null || rawTrack <= 0) {
            return Pair(null, null)
        }
        return if (rawTrack >= 1000) {
            val disc = rawTrack / 1000
            val track = rawTrack % 1000
            Pair(if (disc > 0) disc else null, if (track > 0) track else null)
        } else {
            Pair(null, rawTrack)
        }
    }

    /**
     * Constructs a stable content:// URI for an audio track ID.
     */
    fun buildContentUri(mediaId: Long): String {
        return "$MEDIASTORE_AUDIO_MEDIA_PREFIX/$mediaId"
    }

    /**
     * Constructs a content:// URI for album artwork based on albumId.
     */
    fun buildAlbumArtUri(albumId: Long?): String? {
        return if (albumId != null && albumId > 0) {
            "$MEDIASTORE_ALBUM_ART_PREFIX/$albumId"
        } else {
            null
        }
    }
}
