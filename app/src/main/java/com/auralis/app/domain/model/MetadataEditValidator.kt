package com.auralis.app.domain.model

data class EditableMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String? = null,
    val composer: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null
)

data class MetadataValidationResult(
    val isValid: Boolean,
    val sanitizedMetadata: EditableMetadata? = null,
    val errorMessage: String? = null
)

/**
 * Validator for song metadata edits.
 * Enforces sanitization, bounds checking, and capability detection.
 */
object MetadataEditValidator {

    const val MAX_FIELD_LENGTH = 500

    private val EDITABLE_FORMATS = setOf(
        "mp3", "flac", "m4a", "aac", "ogg", "opus"
    )

    /**
     * Verifies whether a given MIME type or file path supports metadata editing.
     * Audio containers such as WAV or AIFF have limited tag support and are flagged as unsupported.
     */
    fun isFormatEditable(mimeType: String?, filePathOrName: String?): Boolean {
        val format = AudioFormatClassifier.getFormatName(mimeType, filePathOrName).lowercase()
        return EDITABLE_FORMATS.contains(format)
    }

    /**
     * Validates and sanitizes user input for metadata editing.
     */
    fun validate(
        title: String,
        artist: String,
        album: String,
        albumArtist: String? = null,
        genre: String? = null,
        composer: String? = null,
        yearText: String? = null,
        trackNumberText: String? = null,
        discNumberText: String? = null
    ): MetadataValidationResult {
        val cleanTitle = title.trim().take(MAX_FIELD_LENGTH)
        if (cleanTitle.isEmpty()) {
            return MetadataValidationResult(
                isValid = false,
                errorMessage = "Song title cannot be empty."
            )
        }

        val cleanArtist = artist.trim().take(MAX_FIELD_LENGTH).ifEmpty { "Unknown Artist" }
        val cleanAlbum = album.trim().take(MAX_FIELD_LENGTH).ifEmpty { "Unknown Album" }
        val cleanAlbumArtist = albumArtist?.trim()?.take(MAX_FIELD_LENGTH)?.ifEmpty { null }
        val cleanGenre = genre?.trim()?.take(MAX_FIELD_LENGTH)?.ifEmpty { null }
        val cleanComposer = composer?.trim()?.take(MAX_FIELD_LENGTH)?.ifEmpty { null }

        val parsedYear = if (!yearText.isNullOrBlank()) {
            val y = yearText.trim().toIntOrNull()
            if (y == null || y < 1000 || y > 2100) {
                return MetadataValidationResult(
                    isValid = false,
                    errorMessage = "Year must be a valid 4-digit number (1000–2100)."
                )
            }
            y
        } else {
            null
        }

        val parsedTrack = if (!trackNumberText.isNullOrBlank()) {
            val t = trackNumberText.trim().toIntOrNull()
            if (t == null || t < 1 || t > 999) {
                return MetadataValidationResult(
                    isValid = false,
                    errorMessage = "Track number must be between 1 and 999."
                )
            }
            t
        } else {
            null
        }

        val parsedDisc = if (!discNumberText.isNullOrBlank()) {
            val d = discNumberText.trim().toIntOrNull()
            if (d == null || d < 1 || d > 99) {
                return MetadataValidationResult(
                    isValid = false,
                    errorMessage = "Disc number must be between 1 and 99."
                )
            }
            d
        } else {
            null
        }

        return MetadataValidationResult(
            isValid = true,
            sanitizedMetadata = EditableMetadata(
                title = cleanTitle,
                artist = cleanArtist,
                album = cleanAlbum,
                albumArtist = cleanAlbumArtist,
                genre = cleanGenre,
                composer = cleanComposer,
                year = parsedYear,
                trackNumber = parsedTrack,
                discNumber = parsedDisc
            )
        )
    }
}
