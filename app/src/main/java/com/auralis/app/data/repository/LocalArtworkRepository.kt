package com.auralis.app.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Size
import com.auralis.app.data.local.db.dao.ArtworkOverrideDao
import com.auralis.app.data.local.db.dao.SongDefaultCoverDao
import com.auralis.app.data.local.db.entity.ArtworkOverrideEntity
import com.auralis.app.data.local.db.entity.SongDefaultCoverEntity
import com.auralis.app.data.model.MelofiDefaultCovers
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Universal local-only repository for artwork management, embedded extraction,
 * permanent default covers from images/, persistent custom overrides across all entities
 * (Songs, Albums, Artists, Genres, Playlists), dynamic 2x2 playlist collage generation,
 * and memory-bounded disk caching.
 *
 * 100% offline — zero network calls.
 *
 * Artwork Priority:
 * CUSTOM OVERRIDE -> EMBEDDED ARTWORK -> MEDIASTORE ARTWORK -> PERSISTED MELOFI DEFAULT COVER -> FALLBACK
 */
@Singleton
class LocalArtworkRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val artworkOverrideDao: ArtworkOverrideDao,
    private val songDefaultCoverDao: SongDefaultCoverDao
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
    private val defaultCoversDir = File(context.filesDir, MelofiDefaultCovers.FILES_FOLDER).apply { mkdirs() }

    // Fast in-memory cache mapping "${entityType.name}_${entityKey}" -> absolutePath
    // Guarantees O(1) synchronous lookups during LazyColumn/Grid scrolling with zero disk/DB latency.
    private val overrideCache = ConcurrentHashMap<String, String>()

    // Fast in-memory cache mapping songId -> default cover file URI string
    private val defaultCoverCache = ConcurrentHashMap<Long, String>()

    // Fast in-memory cache mapping playlistId -> default cover file URI string
    private val defaultPlaylistCoverCache = ConcurrentHashMap<Long, String>()

    // Set of song IDs known not to have embedded audio artwork (prevents repeated retriever calls)
    private val noEmbeddedArtCache = java.util.Collections.newSetFromMap(ConcurrentHashMap<Long, Boolean>())

    // Reactive stream of custom artwork overrides for dynamic UI updates
    val artworkUpdates: kotlinx.coroutines.flow.Flow<List<ArtworkOverrideEntity>> = artworkOverrideDao.getAllOverridesFlow()


    @Volatile
    private var lastAssignedIndex: Int = -1

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

            // Unpack 16 supplied default cover assets to app-private filesDir on first launch
            ensureDefaultCoversUnpacked()

            // Warm default cover assignments from Room database
            try {
                val defaultCovers = songDefaultCoverDao.getAllCovers()
                for (cover in defaultCovers) {
                    val file = File(defaultCoversDir, cover.coverFileName)
                    defaultCoverCache[cover.songId] = Uri.fromFile(file).toString()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to warm default cover cache: ${e.message}")
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
     * Unpacks all 16 supplied default cover assets to app-private storage.
     */
    private fun ensureDefaultCoversUnpacked() {
        for (fileName in MelofiDefaultCovers.AVAILABLE_COVERS) {
            ensureCoverUnpacked(fileName)
        }
    }

    private fun ensureCoverUnpacked(fileName: String) {
        val dest = File(defaultCoversDir, fileName)
        if (!dest.exists() || dest.length() == 0L) {
            try {
                context.assets.open("${MelofiDefaultCovers.ASSET_FOLDER}/$fileName").use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy default cover asset $fileName: ${e.message}")
            }
        }
    }

    /**
     * Returns the permanently assigned default cover URI for [songId].
     *
     * Once assigned, this mapping is permanently preserved in Room database (song_default_covers)
     * and cached in memory for O(1) synchronous lookup.
     */
    @Synchronized
    fun getOrAssignDefaultCover(songId: Long): String {
        val cached = defaultCoverCache[songId]
        if (cached != null) return cached

        val (fileName, index) = MelofiDefaultCovers.pickCover(songId, lastAssignedIndex)
        lastAssignedIndex = index

        ensureCoverUnpacked(fileName)
        val file = File(defaultCoversDir, fileName)
        val uriStr = Uri.fromFile(file).toString()

        defaultCoverCache[songId] = uriStr

        CoroutineScope(ioDispatcher).launch {
            try {
                songDefaultCoverDao.insertCover(
                    SongDefaultCoverEntity(
                        songId = songId,
                        coverFileName = fileName,
                        assignedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist default cover for song $songId: ${e.message}")
            }
        }

        return uriStr
    }

    /**
     * Validates whether a given MediaStore content or file URI is actually readable.
     */
    fun isValidMediaStoreArt(uriStr: String?): Boolean {
        if (uriStr.isNullOrBlank()) return false
        val uri = Uri.parse(uriStr)
        return try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                f.exists() && f.length() > 0
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Centralized artwork resolver implementing Melofi's strict 5-tier priority chain:
     * 1. USER CUSTOM ARTWORK (Song override -> Album override)
     * 2. EMBEDDED AUDIO ARTWORK (Cached / Extracted via MediaMetadataRetriever)
     * 3. MEDIASTORE ARTWORK (if openable and accessible)
     * 4. PERSISTED MELOFI DEFAULT COVER (Permanently assigned from images/)
     * 5. FINAL MELOFI FALLBACK (Null)
     */
    suspend fun resolveSongArtwork(song: Song): String? = withContext(ioDispatcher) {
        // 1. User custom override (Song level)
        val songCustom = getCustomArtworkUri(ArtworkEntityType.SONG, song.id.toString())
        if (songCustom != null) return@withContext songCustom

        // 1b. User custom override (Album level)
        if (song.albumId != null) {
            val albumCustom = getCustomArtworkUri(ArtworkEntityType.ALBUM, song.albumId.toString())
            if (albumCustom != null) return@withContext albumCustom
        }

        // 2. Embedded audio artwork (Check disk cache first)
        val cachedFile = File(cacheDir, "${song.id}.webp")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext Uri.fromFile(cachedFile).toString()
        }

        // Try embedded extraction if not previously identified as having none
        if (!noEmbeddedArtCache.contains(song.id)) {
            val extracted = extractAndCacheArtwork(song, cachedFile)
            if (extracted != null) {
                return@withContext extracted
            } else {
                noEmbeddedArtCache.add(song.id)
            }
        }

        // 3. MediaStore artwork (Validate content URI accessibility)
        if (isValidMediaStoreArt(song.albumArtUri)) {
            return@withContext song.albumArtUri
        }

        // 4. Persisted Melofi default cover
        val memCover = defaultCoverCache[song.id]
        if (memCover != null) return@withContext memCover

        val dbCover = songDefaultCoverDao.getCoverBySongId(song.id)
        if (dbCover != null) {
            ensureCoverUnpacked(dbCover.coverFileName)
            val file = File(defaultCoversDir, dbCover.coverFileName)
            val uriStr = Uri.fromFile(file).toString()
            defaultCoverCache[song.id] = uriStr
            return@withContext uriStr
        }

        getOrAssignDefaultCover(song.id)
    }

    /**
     * Fast non-suspending artwork resolution for UI list scrolling and immediate domain mapping.
     */
    fun resolveSongArtworkFast(song: Song): String {
        // 1. Song custom override
        val songCustom = getCustomArtworkUri(ArtworkEntityType.SONG, song.id.toString())
        if (songCustom != null) return songCustom

        // 1b. Album custom override
        if (song.albumId != null) {
            val albumCustom = getCustomArtworkUri(ArtworkEntityType.ALBUM, song.albumId.toString())
            if (albumCustom != null) return albumCustom
        }

        // 2. Cached embedded artwork
        val cachedFile = File(cacheDir, "${song.id}.webp")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return Uri.fromFile(cachedFile).toString()
        }

        // 3. Persisted default cover
        return getOrAssignDefaultCover(song.id)
    }

    /**
     * Resolves the best available local artwork URI for a song with full priority.
     * Backwards-compatible alias for [resolveSongArtwork].
     */
    suspend fun getArtworkUri(song: Song): String? = resolveSongArtwork(song)

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
     * Deterministically resolves or assigns a permanent Melofi default cover for a user playlist.
     * Uses the 16 supplied default covers in images/ and guarantees the same playlist retains
     * the exact same cover across app restarts, rescans, navigation, and configuration changes.
     */
    fun getOrAssignDefaultPlaylistCover(playlistId: Long): String {
        return defaultPlaylistCoverCache.getOrPut(playlistId) {
            val fileName = MelofiDefaultCovers.selectCover(playlistId)
            ensureCoverUnpacked(fileName)
            val file = File(defaultCoversDir, fileName)
            Uri.fromFile(file).toString()
        }
    }

    /**
     * Resolves artwork for a playlist:
     * 1. Playlist custom override
     * 2. Cached 2x2 collage
     * 3. Stable deterministic Melofi default cover from images/
     */
    fun getPlaylistArtworkUri(playlistId: Long, explicitCustomUri: String? = null): String? {
        val custom = explicitCustomUri ?: getCustomArtworkUri(ArtworkEntityType.PLAYLIST, playlistId.toString())
        if (custom != null) return custom

        val collageFile = File(collageDir, "collage_$playlistId.png")
        if (collageFile.exists() && collageFile.length() > 0) {
            return Uri.fromFile(collageFile).toString()
        }

        return getOrAssignDefaultPlaylistCover(playlistId)
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
     * Safely opens an InputStream from a given Uri.
     * Robust against modern Android content provider edge cases:
     * - Tries persistable URI permission grant where supported
     * - Attempts ContentResolver.openInputStream
     * - Falls back to ContentResolver.openFileDescriptor("r") via AutoCloseInputStream
     * - Falls back to direct FileInputStream for file:// schemes
     */
    private fun openSafeInputStream(uri: Uri): InputStream? {
        return ArtworkStagingHelper.openSafeInputStream(context, uri)
    }

    /**
     * Universal method to persist a custom artwork override for any supported entity.
     * Enforces MIME validation, single-pass temporary cache streaming, bounds checking,
     * memory-bounded downsampling (max 1024x1024), and persistent app-private file storage.
     * NEVER modifies the user's original audio file.
     */
    suspend fun setCustomArtwork(
        entityType: ArtworkEntityType,
        entityKey: String,
        sourceUri: Uri
    ): Result<String> = withContext(ioDispatcher) {
        val tempImportFile = File(cacheDir, "temp_art_import_${System.currentTimeMillis()}_${entityType.name}_$entityKey.tmp")
        try {
            // 1. Validate MIME type if available from ContentResolver
            val mimeType = try { context.contentResolver.getType(sourceUri) } catch (_: Exception) { null }
            if (mimeType != null && mimeType != "application/octet-stream" && !mimeType.startsWith("image/")) {
                return@withContext Result.failure(IllegalArgumentException("Selected file is not an image ($mimeType)"))
            }

            // 2. Single-pass copy into local temp file in cacheDir
            val inputStream = openSafeInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to read image stream"))

            tempImportFile.outputStream().use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }

            if (!tempImportFile.exists() || tempImportFile.length() == 0L) {
                tempImportFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Unable to read image stream"))
            }

            // 3. Inspect image dimensions and decoded MIME with inJustDecodeBounds on the local temp file
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempImportFile.absolutePath, boundsOptions)

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight
            val decodedMime = boundsOptions.outMimeType
            if (outWidth <= 0 || outHeight <= 0) {
                tempImportFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Invalid or corrupted image format"))
            }
            if (decodedMime != null && !decodedMime.startsWith("image/")) {
                tempImportFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Selected file is not an image ($decodedMime)"))
            }

            // 4. Downsample if image exceeds max allowed dimension (1024x1024)
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

            val bitmap = BitmapFactory.decodeFile(tempImportFile.absolutePath, decodeOptions)
            tempImportFile.delete() // Clean up temp file immediately after decoding into memory

            if (bitmap == null) {
                return@withContext Result.failure(IllegalStateException("Unable to use this image: bitmap decode failed"))
            }

            val destination = getCustomArtworkFile(entityType, entityKey)
            FileOutputStream(destination).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, output)
            }
            bitmap.recycle()

            val localPath = destination.absolutePath

            // 5. Atomically persist mapping in Room database
            artworkOverrideDao.insertOrUpdate(
                ArtworkOverrideEntity(
                    entityType = entityType.name,
                    entityKey = entityKey,
                    localPath = localPath,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // 6. Update fast in-memory cache
            overrideCache["${entityType.name}_$entityKey"] = localPath

            // 7. Invalidate thumbnail and collage caches
            val cachedThumb = File(cacheDir, "$entityKey.webp")
            if (cachedThumb.exists()) {
                cachedThumb.delete()
            }
            if (entityType == ArtworkEntityType.PLAYLIST) {
                invalidatePlaylistCollage(entityKey.toLongOrNull() ?: 0L)
            }

            Result.success(Uri.fromFile(destination).toString())
        } catch (e: Exception) {
            if (tempImportFile.exists()) {
                tempImportFile.delete()
            }
            Log.e(TAG, "Failed to save custom artwork for ${entityType.name} $entityKey: ${e.message}", e)
            Result.failure(IllegalArgumentException("Unable to use this image: ${e.message ?: "unknown error"}"))
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
                openSafeInputStream(uri)?.use {
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
                openSafeInputStream(uri)?.use {
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
