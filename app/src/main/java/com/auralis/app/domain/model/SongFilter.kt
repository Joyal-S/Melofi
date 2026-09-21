package com.auralis.app.domain.model

import java.util.Locale

enum class FormatFilter(val displayName: String) {
    ALL("All Formats"),
    LOSSLESS("Lossless"),
    FLAC("FLAC"),
    MP3("MP3"),
    WAV("WAV"),
    AAC("AAC"),
    ALAC("ALAC"),
    OGG("OGG"),
    OPUS("Opus")
}

enum class QualityFilter(val displayName: String) {
    ALL("All Quality"),
    LOSSLESS("Lossless"),
    LOSSY("Lossy"),
    HIGH_QUALITY("≥ 320 kbps")
}

// Backward compatibility alias for Phase 7 tests
enum class BitrateFilter(val displayName: String) {
    ALL("All Bitrates"),
    HIGH_QUALITY("≥ 320 kbps")
}

enum class DurationFilter(val displayName: String) {
    ALL("All Durations"),
    SHORT("< 3 min"),
    MEDIUM("3–6 min"),
    LONG("> 6 min")
}

enum class PlaybackFilter(val displayName: String) {
    ALL("All Tracks"),
    FAVORITES("Favorites"),
    PLAYED("Played"),
    NEVER_PLAYED("Never Played"),
    RECENTLY_PLAYED("Recently Played")
}

enum class MetadataFilter(val displayName: String) {
    ALL("All Metadata"),
    HAS_ARTWORK("Has Artwork"),
    MISSING_ARTWORK("Missing Artwork"),
    MISSING_ALBUM("Missing Album"),
    MISSING_ARTIST("Missing Artist")
}

enum class LibraryFilter(val displayName: String) {
    ALL("All Time"),
    RECENTLY_ADDED("Added in last 30d"),
    RECENTLY_MODIFIED("Modified in last 30d")
}

/**
 * Composite filtering state for songs library with strict AND semantics.
 */
data class SongFilterState(
    val formatFilter: FormatFilter = FormatFilter.ALL,
    val qualityFilter: QualityFilter = QualityFilter.ALL,
    val durationFilter: DurationFilter = DurationFilter.ALL,
    val playbackFilter: PlaybackFilter = PlaybackFilter.ALL,
    val metadataFilter: MetadataFilter = MetadataFilter.ALL,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val searchQuery: String = "",
    val favoritesOnly: Boolean = false,
    val bitrateFilter: BitrateFilter = BitrateFilter.ALL
) {
    val isDefault: Boolean
        get() = formatFilter == FormatFilter.ALL &&
                qualityFilter == QualityFilter.ALL &&
                durationFilter == DurationFilter.ALL &&
                playbackFilter == PlaybackFilter.ALL &&
                metadataFilter == MetadataFilter.ALL &&
                libraryFilter == LibraryFilter.ALL &&
                searchQuery.isEmpty() &&
                !favoritesOnly &&
                bitrateFilter == BitrateFilter.ALL

    fun reset(): SongFilterState = SongFilterState()

    /**
     * Number of active, non-default filter dimensions currently applied.
     */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (formatFilter != FormatFilter.ALL) count++
            if (qualityFilter != QualityFilter.ALL || bitrateFilter != BitrateFilter.ALL) count++
            if (durationFilter != DurationFilter.ALL) count++
            if (playbackFilter != PlaybackFilter.ALL || favoritesOnly) count++
            if (metadataFilter != MetadataFilter.ALL) count++
            if (libraryFilter != LibraryFilter.ALL) count++
            return count
        }

    /**
     * Evaluates whether a given track satisfies ALL active filter criteria (strict AND semantics).
     */
    fun matches(
        song: Song,
        isFavorite: Boolean = false,
        isPlayed: Boolean = false,
        isRecentlyPlayed: Boolean = false,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Boolean {
        // 1. Favorites flag (Phase 7 compatibility)
        if (favoritesOnly && !isFavorite) return false

        // 2. Playback / User State Filter
        when (playbackFilter) {
            PlaybackFilter.ALL -> Unit
            PlaybackFilter.FAVORITES -> if (!isFavorite) return false
            PlaybackFilter.PLAYED -> if (!isPlayed) return false
            PlaybackFilter.NEVER_PLAYED -> if (isPlayed) return false
            PlaybackFilter.RECENTLY_PLAYED -> if (!isRecentlyPlayed) return false
        }

        // 3. Format Filter
        when (formatFilter) {
            FormatFilter.ALL -> Unit
            FormatFilter.LOSSLESS -> {
                if (!AudioFormatClassifier.isLossless(song.mimeType, song.contentUri)) return false
            }
            FormatFilter.FLAC -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("FLAC", ignoreCase = true)) return false
            }
            FormatFilter.MP3 -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("MP3", ignoreCase = true)) return false
            }
            FormatFilter.WAV -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("WAV", ignoreCase = true)) return false
            }
            FormatFilter.AAC -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("AAC", ignoreCase = true)) return false
            }
            FormatFilter.ALAC -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("ALAC", ignoreCase = true)) return false
            }
            FormatFilter.OGG -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("OGG", ignoreCase = true)) return false
            }
            FormatFilter.OPUS -> {
                val name = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
                if (!name.equals("Opus", ignoreCase = true)) return false
            }
        }

        // 4. Quality Filter (and BitrateFilter compatibility)
        val effQuality = if (bitrateFilter == BitrateFilter.HIGH_QUALITY) QualityFilter.HIGH_QUALITY else qualityFilter
        when (effQuality) {
            QualityFilter.ALL -> Unit
            QualityFilter.LOSSLESS -> {
                if (!AudioFormatClassifier.isLossless(song.mimeType, song.contentUri)) return false
            }
            QualityFilter.LOSSY -> {
                if (AudioFormatClassifier.isLossless(song.mimeType, song.contentUri)) return false
            }
            QualityFilter.HIGH_QUALITY -> {
                val br = song.bitrate ?: 0
                val isLossless = AudioFormatClassifier.isLossless(song.mimeType, song.contentUri)
                if (!isLossless && br < 320) return false
            }
        }

        // 5. Duration Filter
        when (durationFilter) {
            DurationFilter.ALL -> Unit
            DurationFilter.SHORT -> if (song.duration >= 180_000L) return false
            DurationFilter.MEDIUM -> if (song.duration < 180_000L || song.duration > 360_000L) return false
            DurationFilter.LONG -> if (song.duration <= 360_000L) return false
        }

        // 6. Metadata Filter
        when (metadataFilter) {
            MetadataFilter.ALL -> Unit
            MetadataFilter.HAS_ARTWORK -> if (song.albumArtUri.isNullOrBlank()) return false
            MetadataFilter.MISSING_ARTWORK -> if (!song.albumArtUri.isNullOrBlank()) return false
            MetadataFilter.MISSING_ALBUM -> if (song.albumName.isNotBlank() && !song.albumName.equals("Unknown Album", ignoreCase = true)) return false
            MetadataFilter.MISSING_ARTIST -> if (song.artistName.isNotBlank() && !song.artistName.equals("Unknown Artist", ignoreCase = true)) return false
        }

        // 7. Library / Date Filter (30 days threshold = 30 * 86400 seconds)
        val thirtyDaysSeconds = 30L * 24L * 60L * 60L
        val currentSeconds = currentTimestamp / 1000L
        when (libraryFilter) {
            LibraryFilter.ALL -> Unit
            LibraryFilter.RECENTLY_ADDED -> {
                if (song.dateAdded <= 0 || (currentSeconds - song.dateAdded) > thirtyDaysSeconds) return false
            }
            LibraryFilter.RECENTLY_MODIFIED -> {
                if (song.dateModified <= 0 || (currentSeconds - song.dateModified) > thirtyDaysSeconds) return false
            }
        }

        // 8. Search query match
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            val inTitle = song.title.lowercase(Locale.ROOT).contains(q)
            val inArtist = song.artistName.lowercase(Locale.ROOT).contains(q)
            val inAlbum = song.albumName.lowercase(Locale.ROOT).contains(q)
            val inGenre = song.genreName?.lowercase(Locale.ROOT)?.contains(q) == true
            if (!inTitle && !inArtist && !inAlbum && !inGenre) return false
        }

        return true
    }
}
