package com.auralis.app.domain.model

import java.util.Locale

/**
 * Factual classifier for audio formats, codecs, and resolution specifications.
 * Strictly adheres to verified objective characteristics without marketing enhancement claims.
 */
object AudioFormatClassifier {

    private val LOSSLESS_MIME_TYPES = setOf(
        "audio/flac",
        "audio/x-flac",
        "audio/wav",
        "audio/x-wav",
        "audio/alac",
        "audio/x-alac",
        "audio/x-aiff",
        "audio/aiff"
    )

    private val LOSSLESS_EXTENSIONS = setOf(
        "flac",
        "wav",
        "alac",
        "aiff",
        "aif"
    )

    /**
     * Determines whether an audio file is lossless based on MIME type or file extension.
     */
    fun isLossless(mimeType: String?, filePathOrName: String?): Boolean {
        if (mimeType != null && LOSSLESS_MIME_TYPES.contains(mimeType.lowercase(Locale.ROOT))) {
            return true
        }
        val extension = getExtension(filePathOrName)
        return extension != null && LOSSLESS_EXTENSIONS.contains(extension.lowercase(Locale.ROOT))
    }

    /**
     * Extracts a clean, human-readable format name (e.g. "FLAC", "MP3", "AAC", "WAV", "OGG", "Opus").
     */
    fun getFormatName(mimeType: String?, filePathOrName: String?): String {
        val ext = getExtension(filePathOrName)?.uppercase(Locale.ROOT)
        if (ext == "M4A" || ext == "MP4") {
            return if (mimeType?.lowercase(Locale.ROOT)?.contains("alac") == true) "ALAC" else "AAC"
        }
        if (ext == "OPUS") return "Opus"
        if (!ext.isNullOrEmpty()) {
            return ext
        }
        return when (mimeType?.lowercase(Locale.ROOT)) {
            "audio/mpeg", "audio/mp3" -> "MP3"
            "audio/flac", "audio/x-flac" -> "FLAC"
            "audio/wav", "audio/x-wav" -> "WAV"
            "audio/aac", "audio/mp4", "audio/m4a" -> "AAC"
            "audio/ogg", "application/ogg" -> "OGG"
            "audio/opus" -> "Opus"
            "audio/alac", "audio/x-alac" -> "ALAC"
            else -> "Audio"
        }
    }

    /**
     * Formats bitrate in standard kbps (e.g. "320 kbps", "1,411 kbps").
     */
    fun formatBitrate(bitrateKbps: Int?): String {
        return if (bitrateKbps != null && bitrateKbps > 0) {
            String.format(Locale.US, "%,d kbps", bitrateKbps)
        } else {
            "Unknown"
        }
    }

    /**
     * Formats sample rate in kHz (e.g. "44.1 kHz", "48.0 kHz", "96.0 kHz").
     */
    fun formatSampleRate(sampleRateHz: Int?): String {
        return if (sampleRateHz != null && sampleRateHz > 0) {
            val khz = sampleRateHz / 1000.0
            String.format(Locale.US, "%.1f kHz", khz)
        } else {
            "Unknown"
        }
    }

    /**
     * Formats bit depth (e.g. "16-bit", "24-bit", "32-bit").
     */
    fun formatBitDepth(bitsPerSample: Int?): String {
        return if (bitsPerSample != null && bitsPerSample > 0) {
            "$bitsPerSample-bit"
        } else {
            "Unknown"
        }
    }

    /**
     * Formats channel count into mono/stereo/surround description.
     */
    fun formatChannels(channelCount: Int?): String {
        return when (channelCount) {
            1 -> "Mono (1 ch)"
            2 -> "Stereo (2 ch)"
            6 -> "5.1 Surround (6 ch)"
            8 -> "7.1 Surround (8 ch)"
            null -> "Unknown"
            else -> "$channelCount channels"
        }
    }

    /**
     * Formats file size into human-readable string (e.g. "8.4 MB").
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            String.format(Locale.US, "%.1f KB", kb)
        }
    }

    private fun getExtension(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex in 1 until path.length - 1) {
            path.substring(dotIndex + 1)
        } else {
            null
        }
    }
}
