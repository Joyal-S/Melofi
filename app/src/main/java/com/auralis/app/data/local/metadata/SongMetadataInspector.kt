package com.auralis.app.data.local.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.auralis.app.domain.model.AudioFormatClassifier
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-demand metadata inspector utilizing Android's [MediaMetadataRetriever].
 * Lazily extracts deep audio specs (exact sample rate, bit depth, composer, album artist)
 * without incurring scanning overhead across the broader library.
 */
@Singleton
class SongMetadataInspector @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SongMetadataInspector"
    }

    /**
     * Inspects the audio track and produces a comprehensive [SongDetails] model.
     */
    fun inspectSong(song: Song): SongDetails {
        var composer: String? = null
        var albumArtist: String? = null
        var extractedSampleRate: Int? = song.sampleRate
        var extractedBitDepth: Int? = null
        var extractedBitrate: Int? = song.bitrate
        var extractedCodec: String? = song.mimeType

        val uri = Uri.parse(song.contentUri)
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.trim()?.ifEmpty { null }
            albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim()?.ifEmpty { null }
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!mime.isNullOrBlank()) {
                extractedCodec = mime
            }

            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!brStr.isNullOrBlank()) {
                val br = brStr.toIntOrNull()
                if (br != null && br > 0) {
                    extractedBitrate = br / 1000 // Convert bps to kbps
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val srStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                if (!srStr.isNullOrBlank()) {
                    extractedSampleRate = srStr.toIntOrNull()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bdStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                if (!bdStr.isNullOrBlank()) {
                    extractedBitDepth = bdStr.toIntOrNull()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever inspection failed for song id ${song.id}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Safe ignore on older releases
            }
        }

        // Determine file writability safely
        val isWritable = checkWritability(uri)

        val isLossless = AudioFormatClassifier.isLossless(extractedCodec, song.contentUri)
        val formatName = AudioFormatClassifier.getFormatName(extractedCodec, song.contentUri)
        val fileName = getFileName(song)

        return SongDetails(
            id = song.id,
            basicInfo = SongDetails.BasicInfo(
                title = song.title,
                artist = song.artistName,
                album = song.albumName,
                albumArtist = albumArtist,
                genre = song.genreName,
                composer = composer,
                trackNumber = song.trackNumber,
                discNumber = song.discNumber,
                year = song.year,
                durationMs = song.duration
            ),
            audioInfo = SongDetails.AudioInfo(
                codec = extractedCodec,
                mimeType = song.mimeType,
                bitrateKbps = extractedBitrate,
                sampleRateHz = extractedSampleRate,
                channelCount = song.channelCount,
                bitDepth = extractedBitDepth,
                isLossless = isLossless,
                formatName = formatName
            ),
            fileInfo = SongDetails.FileInfo(
                fileName = fileName,
                filePath = null, // Protected by Scoped Storage
                contentUri = song.contentUri,
                sizeBytes = song.size,
                dateModifiedMs = song.dateModified * 1000L
            ),
            technicalInfo = SongDetails.TechnicalInfo(
                mediaStoreId = song.id,
                contentUri = song.contentUri,
                albumArtUri = song.albumArtUri,
                isWritable = isWritable
            )
        )
    }

    private fun checkWritability(uri: Uri): Boolean {
        return try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use {
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileName(song: Song): String {
        return "${song.artistName} - ${song.title}"
    }
}
