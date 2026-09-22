package com.auralis.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import com.auralis.app.data.local.db.dao.ArtworkOverrideDao
import com.auralis.app.data.local.db.entity.ArtworkOverrideEntity
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Universal local-only repository for artwork management, embedded extraction,
 * persistent custom overrides across all entities (Songs, Albums, Artists, Genres, Playlists),
 * dynamic 2x2 playlist collage generation, and memory-bounded disk caching.
 *
 * 100% offline — zero network calls.
 *
 * Artwork Priority:
 * CUSTOM OVERRIDE -> EMBEDDED ARTWORK -> MEDIASTORE ARTWORK -> AUTOMATIC LOCAL FALLBACK
 */
@Singleton
class LocalArtworkRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val artworkOverrideDao: ArtworkOverrideDao
) {

    companion object {
        private const val TAG = "LocalArtworkRepo"
        private const val MAX_THUMBNAIL_DIMENSION = 512
        private const val MAX_CUSTOM_DIMENSION = 1024
        private const val COLLAGE_SIZE = 512
    }

    private val cacheDir = File(context.cacheDir, "artwork_cache").apply { mkdirs() }
    private val customArtDir = File(context.filesDir, "custom_artwork").apply { mkdirs() }
    private val collageDir = File(context.cacheDir, "collage_cache").apply { mkdirs() }

    // Fast in-memory cache mapping "${entityType.name}_${entityKey}" -> absolutePath
    // Guarantees O(1) synchronous lookups during LazyColumn/Grid scrolling with zero disk/DB latency.
    private val overrideCache = ConcurrentHashMap<String, String>()

    init {
        CoroutineScope(ioDispatcher).launch {
            try {
                val overrides = artworkOverrideDao.getAllOverrides()
                for (override in overrides) {
                    val file = File(override.localPath)
                    if (file.exists() && file.length() > 0) {
                        overrideCache["${override.entityType}_${override.entityKey}"] = override.localPath
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to warm artwork override cache: ${e.message}")
            }
        }
    }

    /**
     * Synchronously resolves a custom artwork override URI if present.
     */
    fun getCustomArtworkUri(entityType: ArtworkEntityType, entityKey: String): String? {
        val path = overrideCache["${entityType.name}_$entityKey"]
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                return Uri.fromFile(file).toString()
            } else {
                overrideCache.remove("${entityType.name}_$entityKey")
            }
        }
        val diskFile = getCustomArtworkFile(entityType, entityKey)
        if (diskFile.exists() && diskFile.length() > 0) {
            overrideCache["${entityType.name}_$entityKey"] = diskFile.absolutePath
            return Uri.fromFile(diskFile).toString()
        }
        return null
    }

    /**
     * Resolves the best available local artwork URI for a song with full priority:
     * 1. Song custom override
     * 2. Album custom override
     * 3. App-private extracted thumbnail cache
     * 4. System ContentResolver.loadThumbnail (API 29+)
     * 5. MediaMetadataRetriever embedded picture extraction
     * 6. MediaStore album art URI
     * 7. Fallback (null)
     */
    suspend fun getArtworkUri(song: Song): String? = withContext(ioDispatcher) {
        // 1. Song-level custom override
        val songCustom = getCustomArtworkUri(ArtworkEntityType.SONG, song.id.toString())
        if (songCustom != null) return@withContext songCustom

        // 2. Album-level custom override
        if (song.albumId != null) {
            val albumCustom = getCustomArtworkUri(ArtworkEntityType.ALBUM, song.albumId.toString())
            if (albumCustom != null) return@withContext albumCustom
        }

        // 3. Check cached thumbnail
        val cachedFile = File(cacheDir, "${song.id}.webp")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext Uri.fromFile(cachedFile).toString()
        }

        // 4. Extract & cache embedded artwork
        val extractedUri = extractAndCacheArtwork(song, cachedFile)
        if (extractedUri != null) {
            return@withContext extractedUri
        }

        // 5. Fallback to standard MediaStore album art URI if available
        song.albumArtUri
    }

    /**
     * Resolves the best artwork URI for an album:
     * 1. Album custom override
     * 2. MediaStore / embedded album art URI
     * 3. Fallback (null)
     */
    fun getAlbumArtworkUri(albumId: Long, fallbackArtUri: String? = null): String? {
        val custom = getCustomArtworkUri(ArtworkEntityType.ALBUM, albumId.toString())
        if (custom != null) return custom
        return fallbackArtUri
    }

    /**
     * Resolves artwork for an artist:
     * 1. Artist custom override
     * 2. Automatic local fallback (null)
     */
    fun getArtistArtworkUri(artistId: Long): String? {
        return getCustomArtworkUri(ArtworkEntityType.ARTIST, artistId.toString())
    }

    /**
     * Resolves artwork for a genre:
     * 1. Genre custom override
     * 2. Automatic local fallback (null)
     */
    fun getGenreArtworkUri(genreId: Long): String? {
        return getCustomArtworkUri(ArtworkEntityType.GENRE, genreId.toString())
    }

    /**
     * Resolves artwork for a playlist:
     * 1. Playlist custom override
     * 2. Cached 2x2 collage
     * 3. null (triggers dynamic collage generation or monogram fallback)
     */
    fun getPlaylistArtworkUri(playlistId: Long, explicitCustomUri: String? = null): String? {
        val custom = explicitCustomUri ?: getCustomArtworkUri(ArtworkEntityType.PLAYLIST, playlistId.toString())
        if (custom != null) return custom

        val collageFile = File(collageDir, "collage_$playlistId.png")
        if (collageFile.exists() && collageFile.length() > 0) {
            return Uri.fromFile(collageFile).toString()
        }
        return null
    }

    /**
     * Generates and caches a 2x2 album art collage locally from member track artworks.
     * Adapts gracefully when fewer than 4 tracks have artwork.
     */
    suspend fun generatePlaylistCollage(
        playlistId: Long,
        memberArtUris: List<String>
    ): String? = withContext(ioDispatcher) {
        val distinctArts = memberArtUris.filter { it.isNotBlank() }.distinct().take(4)
        if (distinctArts.isEmpty()) return@withContext null
        if (distinctArts.size == 1) return@withContext distinctArts[0]

        try {
            val destination = File(collageDir, "collage_$playlistId.png")
            val collageBitmap = Bitmap.createBitmap(COLLAGE_SIZE, COLLAGE_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(collageBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val halfSize = COLLAGE_SIZE / 2

            val quadrantRects = listOf(
                Rect(0, 0, halfSize, halfSize),
                Rect(halfSize, 0, COLLAGE_SIZE, halfSize),
                Rect(0, halfSize, halfSize, COLLAGE_SIZE),
                Rect(halfSize, halfSize, COLLAGE_SIZE, COLLAGE_SIZE)
            )

            for (i in distinctArts.indices) {
                val artUri = distinctArts[i]
                val bitmap = loadBitmapSampled(artUri, halfSize, halfSize)
                if (bitmap != null) {
                    val destRect = quadrantRects[i]
                    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                    canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                    bitmap.recycle()
                }
            }

            // If 2 arts, repeat them into bottom quadrants for visual balance
            if (distinctArts.size == 2) {
                val bmp0 = loadBitmapSampled(distinctArts[0], halfSize, halfSize)
                val bmp1 = loadBitmapSampled(distinctArts[1], halfSize, halfSize)
                if (bmp1 != null) {
                    canvas.drawBitmap(bmp1, Rect(0, 0, bmp1.width, bmp1.height), quadrantRects[2], paint)
                    bmp1.recycle()
                }
                if (bmp0 != null) {
                    canvas.drawBitmap(bmp0, Rect(0, 0, bmp0.width, bmp0.height), quadrantRects[3], paint)
                    bmp0.recycle()
                }
            } else if (distinctArts.size == 3) {
                // Repeat first art in 4th quadrant
                val bmp0 = loadBitmapSampled(distinctArts[0], halfSize, halfSize)
                if (bmp0 != null) {
                    canvas.drawBitmap(bmp0, Rect(0, 0, bmp0.width, bmp0.height), quadrantRects[3], paint)
                    bmp0.recycle()
                }
            }

            FileOutputStream(destination).use { out ->
                collageBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            collageBitmap.recycle()

            Uri.fromFile(destination).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate playlist collage for playlist $playlistId: ${e.message}")
            null
        }
    }

    /**
     * Invalidates any cached 2x2 collage for the given playlist.
     */
    fun invalidatePlaylistCollage(playlistId: Long) {
        val collageFile = File(collageDir, "collage_$playlistId.png")
        if (collageFile.exists()) {
            collageFile.delete()
        }
    }

    /**
     * Universal method to persist a custom artwork override for any supported entity.
     * Enforces MIME validation, dimension bounds checking, and downsampling to prevent OOM/disk exhaustion.
     */
    suspend fun setCustomArtwork(
        entityType: ArtworkEntityType,
        entityKey: String,
        sourceUri: Uri
    ): Result<String> = withContext(ioDispatcher) {
        try {
            // 1. Validate MIME type if available
            val mimeType = context.contentResolver.getType(sourceUri)
            if (mimeType != null && !mimeType.startsWith("image/")) {
                return@withContext Result.failure(IllegalArgumentException("Selected file is not an image ($mimeType)"))
            }

            // 2. Inspect image dimensions with inJustDecodeBounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read image stream"))

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight
            if (outWidth <= 0 || outHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid or corrupted image format"))
            }

            // 3. Downsample if image exceeds max allowed dimension (1024x1024)
            var sampleSize = 1
            if (outHeight > MAX_CUSTOM_DIMENSION || outWidth > MAX_CUSTOM_DIMENSION) {
                val halfHeight = outHeight / 2
                val halfWidth = outWidth / 2
                while ((halfHeight / sampleSize) >= MAX_CUSTOM_DIMENSION && (halfWidth / sampleSize) >= MAX_CUSTOM_DIMENSION) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to decode image bitmap"))

            val destination = getCustomArtworkFile(entityType, entityKey)
            FileOutputStream(destination).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
            }
            bitmap.recycle()

            val localPath = destination.absolutePath

            // 4. Atomically persist mapping in Room database
            artworkOverrideDao.insertOrUpdate(
                ArtworkOverrideEntity(
                    entityType = entityType.name,
                    entityKey = entityKey,
                    localPath = localPath,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // 5. Update fast in-memory cache
            overrideCache["${entityType.name}_$entityKey"] = localPath

            // 6. Invalidate thumbnail and collage caches
            val cachedThumb = File(cacheDir, "$entityKey.webp")
            if (cachedThumb.exists()) {
                cachedThumb.delete()
            }
            if (entityType == ArtworkEntityType.PLAYLIST) {
                invalidatePlaylistCollage(entityKey.toLongOrNull() ?: 0L)
            }

            Result.success(Uri.fromFile(destination).toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom artwork for ${entityType.name} $entityKey: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Removes custom artwork override for the given entity.
     * Immediately reverts entity back to embedded, MediaStore, or automatic local fallback.
     */
    suspend fun clearCustomArtwork(
        entityType: ArtworkEntityType,
        entityKey: String
    ): Boolean = withContext(ioDispatcher) {
        val destination = getCustomArtworkFile(entityType, entityKey)
        val path = overrideCache.remove("${entityType.name}_$entityKey")
        artworkOverrideDao.deleteOverride(entityType.name, entityKey)

        val cachedThumb = File(cacheDir, "$entityKey.webp")
        if (cachedThumb.exists()) {
            cachedThumb.delete()
        }
        if (entityType == ArtworkEntityType.PLAYLIST) {
            invalidatePlaylistCollage(entityKey.toLongOrNull() ?: 0L)
        }

        var deleted = false
        if (destination.exists()) {
            deleted = destination.delete()
        }
        if (path != null) {
            val oldFile = File(path)
            if (oldFile.exists()) {
                deleted = oldFile.delete() || deleted
            }
        }
        deleted
    }

    /**
     * Checks whether an entity currently has an active custom artwork override.
     */
    fun hasCustomArtwork(entityType: ArtworkEntityType, entityKey: String): Boolean {
        if (overrideCache.containsKey("${entityType.name}_$entityKey")) return true
        val file = getCustomArtworkFile(entityType, entityKey)
        return file.exists() && file.length() > 0
    }

    // --- Backward-Compatible Overloads for Long IDs ---

    suspend fun setCustomArtwork(targetId: Long, sourceUri: Uri): Result<String> {
        return setCustomArtwork(ArtworkEntityType.SONG, targetId.toString(), sourceUri)
    }

    suspend fun clearCustomArtwork(targetId: Long): Boolean {
        return clearCustomArtwork(ArtworkEntityType.SONG, targetId.toString())
    }

    fun hasCustomArtwork(targetId: Long): Boolean {
        return hasCustomArtwork(ArtworkEntityType.SONG, targetId.toString())
    }

    private fun getCustomArtworkFile(entityType: ArtworkEntityType, entityKey: String): File {
        return File(customArtDir, "art_${entityType.name}_$entityKey.png")
    }

    private fun extractAndCacheArtwork(song: Song, targetFile: File): String? {
        val uri = Uri.parse(song.contentUri)

        // Try ContentResolver.loadThumbnail first on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(MAX_THUMBNAIL_DIMENSION, MAX_THUMBNAIL_DIMENSION), null)
                saveBitmapToCache(bitmap, targetFile)
                bitmap.recycle()
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
                val bitmap = decodeSampledBitmap(pictureBytes, MAX_THUMBNAIL_DIMENSION, MAX_THUMBNAIL_DIMENSION)
                if (bitmap != null) {
                    saveBitmapToCache(bitmap, targetFile)
                    bitmap.recycle()
                    return Uri.fromFile(targetFile).toString()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Embedded artwork extraction failed for song id ${song.id}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Safe ignore
            }
        }

        return null
    }

    private fun loadBitmapSampled(uriStr: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriStr)
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path, boundsOpts)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
            }

            var sampleSize = 1
            if (boundsOpts.outHeight > reqHeight || boundsOpts.outWidth > reqWidth) {
                val halfH = boundsOpts.outHeight / 2
                val halfW = boundsOpts.outWidth / 2
                while ((halfH / sampleSize) >= reqHeight && (halfW / sampleSize) >= reqWidth) {
                    sampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path, decodeOpts)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
        } catch (e: Exception) {
            null
        }
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
