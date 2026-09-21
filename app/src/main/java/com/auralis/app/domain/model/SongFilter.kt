package com.auralis.app.domain.model

import java.util.Locale

enum class FormatFilter(val displayName: String) {
    ALL("All Formats"),
    LOSSLESS("Lossless"),
    FLAC("FLAC"),
    MP3("MP3"),
    WAV("WAV"),
    AAC("AAC")
}

enum class DurationFilter(val displayName: String) {
    ALL("All Durations"),
    SHORT("< 3 min"),
    MEDIUM("3–6 min"),
    LONG("> 6 min")
}

enum class BitrateFilter(val displayName: String) {
    ALL("All Bitrates"),
    HIGH_QUALITY("≥ 320 kbps")
}

/**
 * Composite filtering state for songs library.
 */
data class SongFilterState(
    val formatFilter: FormatFilter = FormatFilter.ALL,
    val durationFilter: DurationFilter = DurationFilter.ALL,
    val bitrateFilter: BitrateFilter = BitrateFilter.ALL,
    val searchQuery: String = "",
    val favoritesOnly: Boolean = false
) {
    val isDefault: Boolean
        get() = formatFilter == FormatFilter.ALL &&
                durationFilter == DurationFilter.ALL &&
                bitrateFilter == BitrateFilter.ALL &&
                searchQuery.isEmpty() &&
                !favoritesOnly

    /**
     * Evaluates whether a given track satisfies the filter criteria.
     */
    fun matches(song: Song, isFavorite: Boolean): Boolean {
        // 1. Favorites filter
        if (favoritesOnly && !isFavorite) return false

        // 2. Format filter
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
        }

        // 3. Duration filter
        when (durationFilter) {
            DurationFilter.ALL -> Unit
            DurationFilter.SHORT -> if (song.duration >= 180_000L) return false
            DurationFilter.MEDIUM -> if (song.duration < 180_000L || song.duration > 360_000L) return false
            DurationFilter.LONG -> if (song.duration <= 360_000L) return false
        }

        // 4. Bitrate filter
        when (bitrateFilter) {
            BitrateFilter.ALL -> Unit
            BitrateFilter.HIGH_QUALITY -> {
                val br = song.bitrate ?: 0
                val isLossless = AudioFormatClassifier.isLossless(song.mimeType, song.contentUri)
                // Lossless formats are inherently high bitrate (usually >= 700 kbps), lossy must be >= 320
                if (!isLossless && br < 320) return false
            }
        }

        // 5. Search query filter
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
