package com.auralis.app.data.local.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.auralis.app.data.local.db.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Robust, recursive Android Storage Access Framework (SAF) folder scanner for local audio files.
 *
 * Traverses document trees via DocumentFile and ContentResolver without converting URIs
 * to fake filesystem paths. Handles nested directories, empty folders, permission anomalies,
 * and unsupported files safely.
 *
 * Supported formats: MP3, AAC, M4A, FLAC, WAV, OGG, Opus, ALAC.
 */
@Singleton
class SafAudioScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SafAudioScanner"
        private const val MIN_AUDIO_DURATION_MS = 1000L

        val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus", "alac"
        )

        /**
         * Generates a deterministic, positive 62-bit ID from a SAF document URI.
         * The ID is stable across rescans and partitioned into a high space to avoid
         * collisions with standard sequential MediaStore IDs.
         */
        fun generateSafSongId(contentUri: String): Long {
            var h = 1125899906842597L
            for (ch in contentUri) {
                h = 31 * h + ch.code
            }
            val positive = h and 0x3FFFFFFFFFFFFFFFL
            return positive or (1L shl 60)
        }

        fun isSupportedAudio(displayName: String, mimeType: String?): Boolean {
            if (mimeType != null && (mimeType.startsWith("audio/") || mimeType == "application/ogg")) {
                return true
            }
            val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return ext in SUPPORTED_EXTENSIONS
        }

        fun getMimeTypeFromExtension(ext: String): String {
            return when (ext.lowercase(Locale.ROOT)) {
                "mp3" -> "audio/mpeg"
                "flac" -> "audio/flac"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                "m4a" -> "audio/mp4"
                "ogg" -> "audio/ogg"
                "opus" -> "audio/opus"
                "alac" -> "audio/alac"
                else -> "audio/*"
            }
        }

        /**
         * Determines whether a candidate song matches an existing song in the library to prevent duplicates.
         */
        fun isDuplicateOfExisting(candidate: SongEntity, existingSongs: List<SongEntity>): Boolean {
            for (existing in existingSongs) {
                if (candidate.contentUri == existing.contentUri) return true
                if (candidate.id == existing.id) return true

                // Exact title & artist with matching duration (within 1.5s tolerance)
                val titleMatches = candidate.title.equals(existing.title, ignoreCase = true)
                val artistMatches = candidate.artistName.equals(existing.artistName, ignoreCase = true)
                val durationMatches = kotlin.math.abs(candidate.duration - existing.duration) < 1500L

                if (titleMatches && artistMatches && durationMatches) {
                    return true
                }

                // Exact file size and title match
                if (candidate.size > 0 && candidate.size == existing.size && titleMatches) {
                    return true
                }
            }
            return false
        }
    }

    data class DiscoveredDocument(
        val documentUri: Uri,
        val displayName: String,
        val sizeBytes: Long,
        val mimeType: String?,
        val lastModified: Long
    )

    /**
     * Recursively traverses a document tree URI via DocumentFile and ContentResolver
     * and returns all valid, supported audio files.
     */
    suspend fun scanTree(
        treeUri: Uri,
        onProgress: ((current: Int, total: Int, currentName: String) -> Unit)? = null
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val discoveredFiles = mutableListOf<DiscoveredDocument>()

        try {
            val rootDoc = try {
                DocumentFile.fromTreeUri(context, treeUri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Cannot create DocumentFile for tree $treeUri: ${e.message}")
                null
            }

            if (rootDoc == null || !rootDoc.exists() || !rootDoc.canRead()) {
                Log.w(TAG, "Tree root is inaccessible or permission missing: $treeUri")
                return@withContext emptyList()
            }

            val queue = ArrayDeque<DocumentFile>()
            queue.add(rootDoc)

            while (queue.isNotEmpty()) {
                val currentDir = queue.removeFirst()
                val children = try {
                    currentDir.listFiles()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed listing children of directory ${currentDir.uri}: ${e.message}")
                    emptyArray()
                }

                for (child in children) {
                    try {
                        if (child.isDirectory) {
                            queue.add(child)
                        } else if (child.isFile) {
                            val name = child.name ?: "audio_file"
                            val mime = child.type
                            if (isSupportedAudio(name, mime)) {
                                discoveredFiles.add(
                                    DiscoveredDocument(
                                        documentUri = child.uri,
                                        displayName = name,
                                        sizeBytes = child.length(),
                                        mimeType = mime,
                                        lastModified = child.lastModified()
                                    )
                                )
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Error inspecting child document ${child.uri}: ${e.message}")
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing tree $treeUri: ${e.message}", e)
            return@withContext emptyList()
        }

        // 2. Extract metadata and build SongEntity list
        val songs = mutableListOf<SongEntity>()
        val total = discoveredFiles.size

        for (i in discoveredFiles.indices) {
            val doc = discoveredFiles[i]
            onProgress?.invoke(i + 1, total, doc.displayName)

            val song = extractSongMetadata(doc)
            if (song != null) {
                songs.add(song)
            }
        }

        songs
    }

    private fun extractSongMetadata(doc: DiscoveredDocument): SongEntity? {
        val extension = doc.displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val defaultMime = if (!doc.mimeType.isNullOrBlank()) doc.mimeType else getMimeTypeFromExtension(extension)

        val retriever = MediaMetadataRetriever()
        return try {
            try {
                context.contentResolver.openFileDescriptor(doc.documentUri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                } ?: run {
                    retriever.setDataSource(context, doc.documentUri)
                }
            } catch (e: Exception) {
                retriever.setDataSource(context, doc.documentUri)
            }

            val rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (rawDuration < MIN_AUDIO_DURATION_MS) {
                return null
            }

            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val rawGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()
            val rawYear = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
            val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            val rawTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()
            val rawDisc = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.toIntOrNull()

            val title = AudioMetadataNormalizer.normalizeTitle(rawTitle, doc.displayName)
            val artist = AudioMetadataNormalizer.normalizeArtist(rawArtist)
            val album = AudioMetadataNormalizer.normalizeAlbum(rawAlbum)
            val genre = AudioMetadataNormalizer.normalizeGenre(rawGenre)
            val duration = AudioMetadataNormalizer.normalizeDuration(rawDuration)
            val bitrateKbps = if (rawBitrate != null && rawBitrate > 0) rawBitrate / 1000 else null

            val stableId = generateSafSongId(doc.documentUri.toString())

            SongEntity(
                id = stableId,
                title = title,
                artistId = null,
                artistName = artist,
                albumId = null,
                albumName = album,
                genreId = null,
                genreName = genre,
                duration = duration,
                trackNumber = rawTrack,
                discNumber = rawDisc,
                year = if (rawYear != null && rawYear > 0) rawYear else null,
                filePath = null, // SAF canonical access is via contentUri
                contentUri = doc.documentUri.toString(),
                mimeType = defaultMime,
                size = doc.sizeBytes,
                bitrate = bitrateKbps,
                sampleRate = null,
                channelCount = null,
                dateAdded = System.currentTimeMillis() / 1000L,
                dateModified = if (doc.lastModified > 0) doc.lastModified / 1000L else System.currentTimeMillis() / 1000L,
                albumArtUri = null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Skipping unreadable SAF audio file ${doc.displayName}: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
