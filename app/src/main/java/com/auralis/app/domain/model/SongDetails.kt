package com.auralis.app.domain.model

/**
 * Rich domain model for inspecting detailed local song information.
 * All fields represent verified device/file metadata without invented values.
 */
data class SongDetails(
    val id: Long,
    val basicInfo: BasicInfo,
    val audioInfo: AudioInfo,
    val fileInfo: FileInfo,
    val technicalInfo: TechnicalInfo
) {
    data class BasicInfo(
        val title: String,
        val artist: String,
        val album: String,
        val albumArtist: String?,
        val genre: String?,
        val composer: String?,
        val trackNumber: Int?,
        val discNumber: Int?,
        val year: Int?,
        val durationMs: Long
    )

    data class AudioInfo(
        val codec: String?,
        val mimeType: String?,
        val bitrateKbps: Int?,
        val sampleRateHz: Int?,
        val channelCount: Int?,
        val bitDepth: Int?,
        val isLossless: Boolean,
        val formatName: String
    )

    data class FileInfo(
        val fileName: String,
        val filePath: String?,
        val contentUri: String,
        val sizeBytes: Long,
        val dateModifiedMs: Long
    )

    data class TechnicalInfo(
        val mediaStoreId: Long,
        val contentUri: String,
        val albumArtUri: String?,
        val isWritable: Boolean
    )
}
