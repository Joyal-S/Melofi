package com.auralis.app.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID

/**
 * Robust helper to safely read, validate, and stage incoming gallery and content URIs
 * into Melofi app-private temporary storage.
 *
 * Ensures:
 * - Operates safely with transient Activity URI permissions
 * - Attempts persistable URI permissions where supported
 * - Multi-tier stream acquisition (openInputStream -> openAssetFileDescriptor -> openFileDescriptor -> FileInputStream)
 * - Single-pass copy into app-private staging file
 * - Dimension bounds inspection (inJustDecodeBounds)
 * - MIME type validation (supports image MIME types from ContentResolver and decoded outMimeType)
 * - Rejection of corrupted or zero-byte files with clear error messages
 * - Zero network calls, 100% offline
 */
object ArtworkStagingHelper {

    private const val TAG = "ArtworkStagingHelper"
    private const val STAGING_DIR_NAME = "artwork_staging"

    /**
     * Safely reads an image [sourceUri] using the provided [context]'s ContentResolver,
     * validates its format and dimensions, and stages it into app-private cache directory.
     *
     * @param context Context holding read permission (preferably Activity context).
     * @param sourceUri The selected image URI (content:// or file://).
     * @return Result containing the staged local [File] on success, or an informative [Exception] on failure.
     */
    suspend fun stageImageUri(context: Context, sourceUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, STAGING_DIR_NAME).apply { mkdirs() }
        val stagedFile = File(stagingDir, "stage_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.tmp")

        try {
            // 1. Safely attempt persistable URI permission if applicable
            try {
                context.contentResolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Ignore: transient grants or non-persistable providers do not support this
            }

            // 2. Open stream with multi-tiered fallback
            val inputStream = openSafeInputStream(context, sourceUri)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Unable to read image stream: source URI is inaccessible or permission was denied")
                )

            // 3. Stream copy into app-private staging file
            stagedFile.outputStream().use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }

            if (!stagedFile.exists() || stagedFile.length() == 0L) {
                stagedFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException("Unable to read image stream: selected image file is empty (0 bytes)")
                )
            }

            // 4. Inspect image dimensions and MIME type with inJustDecodeBounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(stagedFile.absolutePath, boundsOptions)

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight
            val decodedMime = boundsOptions.outMimeType

            if (outWidth <= 0 || outHeight <= 0) {
                stagedFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException("Unable to use this image: file is corrupted or not a valid image")
                )
            }

            // Validate MIME type: either ContentResolver type or decoded MIME must start with "image/"
            val crMime = try { context.contentResolver.getType(sourceUri) } catch (_: Exception) { null }
            val isImageMime = (crMime != null && crMime.startsWith("image/")) ||
                    (decodedMime != null && decodedMime.startsWith("image/"))

            if (!isImageMime && crMime != null && crMime != "application/octet-stream") {
                stagedFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException("Selected file is not an image ($crMime)")
                )
            }

            Result.success(stagedFile)
        } catch (e: Exception) {
            if (stagedFile.exists()) {
                stagedFile.delete()
            }
            Log.e(TAG, "Failed to stage image URI $sourceUri: ${e.message}", e)
            Result.failure(IllegalArgumentException("Unable to use this image: ${e.message ?: "read error"}"))
        }
    }

    /**
     * Cleans up all orphaned temporary files in the staging directory.
     */
    fun clearStagingDir(context: Context) {
        try {
            val stagingDir = File(context.cacheDir, STAGING_DIR_NAME)
            if (stagingDir.exists()) {
                stagingDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear staging dir: ${e.message}")
        }
    }

    /**
     * Safely opens an InputStream from a given Uri using multiple fallback strategies.
     */
    fun openSafeInputStream(context: Context, uri: Uri): InputStream? {
        // Strategy 1: ContentResolver.openInputStream
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) return stream
        } catch (e: Exception) {
            Log.w(TAG, "openInputStream failed for $uri: ${e.message}")
        }

        // Strategy 2: ContentResolver.openAssetFileDescriptor
        try {
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
            if (afd != null) {
                return afd.createInputStream()
            }
        } catch (e: Exception) {
            Log.w(TAG, "openAssetFileDescriptor failed for $uri: ${e.message}")
        }

        // Strategy 3: ContentResolver.openFileDescriptor via AutoCloseInputStream
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                return ParcelFileDescriptor.AutoCloseInputStream(pfd)
            }
        } catch (e: Exception) {
            Log.w(TAG, "openFileDescriptor failed for $uri: ${e.message}")
        }

        // Strategy 4: Direct file access for file:// or null scheme
        if (uri.scheme == "file" || uri.scheme == null) {
            try {
                val path = uri.path
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        return FileInputStream(file)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct FileInputStream failed for $uri: ${e.message}")
            }
        }

        return null
    }
}
