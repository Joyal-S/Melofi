package com.auralis.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only repository for album artwork retrieval, embedded extraction, and disk caching.
 * Ensures zero network usage and prevents UI thread jank by caching thumbnails in app-private storage.
 */
@Singleton
class LocalArtworkRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "LocalArtworkRepo"
        private const val MAX_DIMENSION = 512
    }

    private val cacheDir = File(context.cacheDir, "artwork_cache").apply { mkdirs() }
    private val customArtDir = File(context.filesDir, "custom_artwork").apply { mkdirs() }

    /**
     * Resolves the best available local artwork URI for a song.
     * Checks:
     * 1. App-private custom artwork override
     * 2. App-private extracted thumbnail cache
     * 3. System ContentResolver.loadThumbnail (API 29+)
     * 4. MediaMetadataRetriever embedded picture extraction
     * 5. MediaStore album art URI
     */
    suspend fun getArtworkUri(song: Song): String? = withContext(ioDispatcher) {
        // 1. Check custom artwork override
        val customFile = getCustomArtworkFile(song.albumId ?: song.id)
        if (customFile.exists() && customFile.length() > 0) {
            return@withContext Uri.fromFile(customFile).toString()
        }

        // 2. Check cached thumbnail
        val cachedFile = File(cacheDir, "${song.id}.webp")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext Uri.fromFile(cachedFile).toString()
        }

        // 3. Extract & cache embedded artwork
        val extractedUri = extractAndCacheArtwork(song, cachedFile)
        if (extractedUri != null) {
            return@withContext extractedUri
        }

        // 4. Fallback to standard MediaStore album art URI if available
        song.albumArtUri
    }

    /**
     * Saves a custom artwork image provided by the user for an album or track.
     */
    suspend fun setCustomArtwork(targetId: Long, sourceUri: Uri): Result<String> = withContext(ioDispatcher) {
        try {
            val destination = getCustomArtworkFile(targetId)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            // Invalidate thumbnail cache so the new image is re-read
            val cachedFile = File(cacheDir, "$targetId.webp")
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            Result.success(Uri.fromFile(destination).toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom artwork: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Removes any custom artwork override for the given ID.
     */
    suspend fun clearCustomArtwork(targetId: Long): Boolean = withContext(ioDispatcher) {
        val file = getCustomArtworkFile(targetId)
        val cachedFile = File(cacheDir, "$targetId.webp")
        if (cachedFile.exists()) {
            cachedFile.delete()
        }
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Checks whether a custom artwork override exists for the given ID.
     */
    fun hasCustomArtwork(targetId: Long): Boolean {
        val file = getCustomArtworkFile(targetId)
        return file.exists() && file.length() > 0
    }

    private fun getCustomArtworkFile(targetId: Long): File {
        return File(customArtDir, "art_$targetId.png")
    }

    private fun extractAndCacheArtwork(song: Song, targetFile: File): String? {
        val uri = Uri.parse(song.contentUri)

        // Try ContentResolver.loadThumbnail first on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(MAX_DIMENSION, MAX_DIMENSION), null)
                saveBitmapToCache(bitmap, targetFile)
                return Uri.fromFile(targetFile).toString()
            } catch (e: Exception) {
                // Ignore and fall back to MediaMetadataRetriever
            }
        }

        // Try MediaMetadataRetriever embedded picture
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                val bitmap = decodeSampledBitmap(pictureBytes, MAX_DIMENSION, MAX_DIMENSION)
                if (bitmap != null) {
                    saveBitmapToCache(bitmap, targetFile)
                    return Uri.fromFile(targetFile).toString()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Embedded artwork extraction failed for ${song.title}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Safe ignore
            }
        }

        return null
    }

    private fun saveBitmapToCache(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        }
    }

    private fun decodeSampledBitmap(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(data, 0, data.size, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        return BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
    }
}
