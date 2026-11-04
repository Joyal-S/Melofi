package com.auralis.app.data.local.importer

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.content.Intent
import androidx.room.withTransaction
import com.auralis.app.data.local.db.AuralisDatabase
import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.ImportedFolderDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.local.db.entity.ImportedFolderEntity
import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.data.local.scanner.SafAudioScanner
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.DuplicateConfidence
import com.auralis.app.domain.model.DuplicateDetectionEngine
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Candidate audio file identified during an import operation.
 */
data class ImportCandidate(
    val sourceUri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val bitrateKbps: Int?,
    val isDuplicate: Boolean = false,
    val existingSongMatch: Song? = null,
    val duplicateConfidence: DuplicateConfidence? = null
)

/**
 * Live status of the music import operation.
 */
sealed interface ImportState {
    data object Idle : ImportState
    data class Validating(val current: Int, val total: Int) : ImportState
    data class DuplicatesPending(
        val duplicates: List<ImportCandidate>,
        val cleanCandidates: List<ImportCandidate>
    ) : ImportState
    data class Importing(
        val current: Int,
        val total: Int,
        val percent: Int,
        val currentFileName: String
    ) : ImportState
    data class Completed(
        val addedCount: Int,
        val skippedCount: Int,
        val failedCount: Int,
        val message: String? = null
    ) : ImportState
}

/**
 * 100% offline, local music import manager.
 *
 * Implements Android Storage Access Framework (SAF) single/multiple file and folder import,
 * strict format capability validation, pre-import duplicate detection, byte-perfect copying
 * into standard MediaStore shared music storage (Music/Melofy/) for selected files, and
 * direct persistent tree indexing for folders.
 *
 * Guarantees:
 * - Zero audio transcoding, resampling, or re-encoding.
 * - Preserves original codec, bitrate, sample rate, bit depth, channels, and metadata.
 * - Source files are never modified or deleted.
 * - Scoped Storage compliant without MANAGE_EXTERNAL_STORAGE.
 */
@Singleton
class MusicImportManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val songDao: SongDao,
    private val songRepository: SongRepository,
    private val safAudioScanner: SafAudioScanner,
    private val importedFolderDao: ImportedFolderDao,
    private val database: AuralisDatabase,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    private val genreDao: GenreDao
) {

    companion object {
        private const val TAG = "MusicImportManager"
        private const val MELOFY_MUSIC_DIRECTORY = "Music/Melofy/"
        private const val MIN_AUDIO_DURATION_MS = 1000L

        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "m4a", "ogg", "opus"
        )
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private var activeImportJob: Job? = null
    private var pendingDestUri: Uri? = null

    /**
     * Resets import state back to Idle.
     */
    fun resetState() {
        activeImportJob?.cancel()
        _importState.value = ImportState.Idle
    }

    /**
     * Cancels an ongoing import operation and cleans up any pending partially-copied MediaStore file.
     */
    fun cancelImport() {
        activeImportJob?.cancel()
        activeImportJob = null
        pendingDestUri?.let { uri ->
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean up pending MediaStore URI on cancel: ${e.message}")
            }
            pendingDestUri = null
        }
        _importState.value = ImportState.Idle
    }

    /**
     * Initiates inspection and validation for user-selected file URIs.
     */
    fun processSelectedUris(uris: List<Uri>) {
        activeImportJob?.cancel()
        activeImportJob = CoroutineScope(ioDispatcher).launch {
            try {
                if (uris.isEmpty()) {
                    _importState.value = ImportState.Completed(0, 0, 0, "No files selected.")
                    return@launch
                }

                val candidates = mutableListOf<ImportCandidate>()
                val existingEntities = songDao.getAllSongsList()
                val existingSongs = existingEntities.map { com.auralis.app.data.mapper.SongMapper.toDomain(it) }

                for (i in uris.indices) {
                    _importState.value = ImportState.Validating(i + 1, uris.size)
                    val uri = uris[i]
                    val candidate = inspectAudioUri(uri, existingSongs)
                    if (candidate != null) {
                        candidates.add(candidate)
                    }
                }

                if (candidates.isEmpty()) {
                    _importState.value = ImportState.Completed(
                        addedCount = 0,
                        skippedCount = 0,
                        failedCount = uris.size,
                        message = "No valid audio files found in selection."
                    )
                    return@launch
                }

                val duplicates = candidates.filter { it.isDuplicate }
                val clean = candidates.filter { !it.isDuplicate }

                if (duplicates.isNotEmpty()) {
                    _importState.value = ImportState.DuplicatesPending(
                        duplicates = duplicates,
                        cleanCandidates = clean
                    )
                } else {
                    executeImport(candidates)
                }
            } catch (e: CancellationException) {
                _importState.value = ImportState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error processing selected URIs: ${e.message}", e)
                _importState.value = ImportState.Completed(0, 0, 0, "Import preparation failed: ${e.message}")
            }
        }
    }

    /**
     * Recursively traverses a SAF Document Tree URI and inspects all audio files found,
     * persisting the folder URI and indexing tracks into the Melofi library without copying.
     */
    fun processFolderTreeUri(treeUri: Uri) {
        activeImportJob?.cancel()
        activeImportJob = CoroutineScope(ioDispatcher).launch {
            try {
                // 1. Take persistable URI permission
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed taking persistable permission for $treeUri: ${e.message}")
                }

                _importState.value = ImportState.Validating(0, 1)

                val rootDoc = try {
                    androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                } catch (e: Exception) {
                    null
                }

                if (rootDoc == null || !rootDoc.exists() || !rootDoc.canRead()) {
                    _importState.value = ImportState.Completed(
                        addedCount = 0,
                        skippedCount = 0,
                        failedCount = 0,
                        message = "Selected folder is not accessible or permission was not granted."
                    )
                    return@launch
                }

                var resolvedFolderName: String? = rootDoc.name
                if (resolvedFolderName.isNullOrBlank()) {
                    try {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            DocumentsContract.getTreeDocumentId(treeUri)
                        )
                        context.contentResolver.query(
                            docUri,
                            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                            null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val colIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                                if (colIdx >= 0) {
                                    resolvedFolderName = cursor.getString(colIdx)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not query folder display name: ${e.message}")
                    }
                }

                val folderName = resolvedFolderName?.takeIf { it.isNotBlank() }
                    ?: treeUri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                    ?: "Music Folder"

                // 2. Persist folder access in database
                val existingFolder = importedFolderDao.findFolderByUri(treeUri.toString())
                val folderId = if (existingFolder != null) {
                    importedFolderDao.updatePermissionStatus(existingFolder.id, true)
                    existingFolder.id
                } else {
                    val entity = ImportedFolderEntity(
                        treeUri = treeUri.toString(),
                        displayName = folderName,
                        dateAdded = System.currentTimeMillis(),
                        isPermissionValid = true,
                        songCount = 0
                    )
                    importedFolderDao.insertFolder(entity)
                }

                // 3. Recursively inspect supported audio files with live progress
                val discoveredSongs = safAudioScanner.scanTree(treeUri) { current, total, name ->
                    val percent = if (total > 0) ((current.toFloat() / total) * 100).toInt() else 0
                    _importState.value = ImportState.Importing(
                        current = current,
                        total = total,
                        percent = percent,
                        currentFileName = name
                    )
                }

                if (discoveredSongs.isEmpty()) {
                    importedFolderDao.updateSongCount(folderId, 0)
                    _importState.value = ImportState.Completed(
                        addedCount = 0,
                        skippedCount = 0,
                        failedCount = 0,
                        message = "No supported audio files found in chosen folder."
                    )
                    return@launch
                }

                // 4. Deduplicate against existing library and intra-batch
                val existingEntities = songDao.getAllSongsList()
                var addedCount = 0
                var skippedCount = 0
                val songsToInsert = mutableListOf<SongEntity>()

                for (song in discoveredSongs) {
                    if (SafAudioScanner.isDuplicateOfExisting(song, existingEntities) ||
                        SafAudioScanner.isDuplicateOfExisting(song, songsToInsert)
                    ) {
                        skippedCount++
                    } else {
                        songsToInsert.add(song)
                        addedCount++
                    }
                }

                // 5. Persist newly discovered songs atomically with canonical artists and albums
                if (songsToInsert.isNotEmpty()) {
                    database.withTransaction {
                        val preparedInserts = songsToInsert.map { song ->
                            val norm = com.auralis.app.core.util.ArtistNameNormalizer.normalize(song.artistName)
                            val existingArtist = if (norm.isNotEmpty()) artistDao.findArtistByNormalizedName(norm) else null
                            val targetArtistId = if (existingArtist != null) {
                                existingArtist.id
                            } else if (norm.isNotEmpty()) {
                                val clean = com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(song.artistName)
                                val now = System.currentTimeMillis()
                                val newEntity = com.auralis.app.data.local.db.entity.ArtistEntity(
                                    name = clean,
                                    normalizedName = norm,
                                    albumCount = 0,
                                    songCount = 0,
                                    createdAt = now,
                                    updatedAt = now
                                )
                                artistDao.insertArtist(newEntity)
                            } else {
                                null
                            }
                            val targetName = existingArtist?.name ?: com.auralis.app.core.util.ArtistNameNormalizer.cleanDisplayName(song.artistName)
                            song.copy(artistId = targetArtistId, artistName = targetName)
                        }

                        songDao.insertSongs(preparedInserts)
                        artistDao.recalculateAllArtistCounts()

                        val currentAllSongs = songDao.getAllSongsList()
                        val albums = com.auralis.app.data.local.scanner.LibraryAggregator.aggregateAlbums(currentAllSongs)
                        val genres = com.auralis.app.data.local.scanner.LibraryAggregator.aggregateGenres(currentAllSongs)
                        albumDao.insertAlbums(albums)
                        genreDao.insertGenres(genres)
                    }
                }

                importedFolderDao.updateSongCount(folderId, discoveredSongs.size)

                val msg = when {
                    addedCount == 0 && skippedCount > 0 -> "All $skippedCount tracks are already in library."
                    skippedCount > 0 -> "Added $addedCount tracks ($skippedCount already in library)."
                    else -> "Added $addedCount tracks to library."
                }

                _importState.value = ImportState.Completed(
                    addedCount = addedCount,
                    skippedCount = skippedCount,
                    failedCount = 0,
                    message = msg
                )
            } catch (e: CancellationException) {
                _importState.value = ImportState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error processing folder tree: ${e.message}", e)
                _importState.value = ImportState.Completed(0, 0, 0, "Failed to read folder contents: ${e.message}")
            }
        }
    }

    /**
     * Confirms execution of import with user-chosen duplicate handling strategy.
     * @param importDuplicates If true, duplicates are imported anyway; if false, they are skipped.
     */
    fun confirmDuplicatesResolution(importDuplicates: Boolean) {
        val currentState = _importState.value
        if (currentState !is ImportState.DuplicatesPending) return

        val candidatesToImport = if (importDuplicates) {
            currentState.cleanCandidates + currentState.duplicates
        } else {
            currentState.cleanCandidates
        }

        val skippedCount = if (importDuplicates) 0 else currentState.duplicates.size

        activeImportJob?.cancel()
        activeImportJob = CoroutineScope(ioDispatcher).launch {
            executeImport(candidatesToImport, initialSkipped = skippedCount)
        }
    }

    /**
     * Executes the byte-perfect copy of audio candidates into MediaStore shared storage.
     */
    private suspend fun executeImport(
        candidates: List<ImportCandidate>,
        initialSkipped: Int = 0
    ) = withContext(ioDispatcher) {
        var addedCount = 0
        var failedCount = 0
        val total = candidates.size

        if (total == 0) {
            _importState.value = ImportState.Completed(
                addedCount = 0,
                skippedCount = initialSkipped,
                failedCount = 0
            )
            return@withContext
        }

        for (i in candidates.indices) {
            val candidate = candidates[i]
            val percent = (((i.toFloat()) / total) * 100).toInt()

            _importState.value = ImportState.Importing(
                current = i + 1,
                total = total,
                percent = percent,
                currentFileName = candidate.fileName
            )

            val success = copyCandidateToMediaStore(candidate)
            if (success) {
                addedCount++
            } else {
                failedCount++
            }
        }

        // Trigger comprehensive library re-scan so newly added MediaStore tracks populate Room database
        try {
            songRepository.scanLibrary()
        } catch (e: Exception) {
            Log.w(TAG, "Library rescan after import caught error: ${e.message}")
        }

        _importState.value = ImportState.Completed(
            addedCount = addedCount,
            skippedCount = initialSkipped,
            failedCount = failedCount
        )
    }

    /**
     * Copies raw bytes into standard Android MediaStore without modifying or re-encoding audio.
     */
    private suspend fun copyCandidateToMediaStore(candidate: ImportCandidate): Boolean = withContext(ioDispatcher) {
        var destUri: Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, candidate.fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, candidate.mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, MELOFY_MUSIC_DIRECTORY)
                put(MediaStore.Audio.Media.IS_PENDING, 1)

                if (candidate.title.isNotBlank()) {
                    put(MediaStore.Audio.Media.TITLE, candidate.title)
                }
                if (candidate.artist.isNotBlank()) {
                    put(MediaStore.Audio.Media.ARTIST, candidate.artist)
                }
                if (candidate.album.isNotBlank()) {
                    put(MediaStore.Audio.Media.ALBUM, candidate.album)
                }
                if (candidate.durationMs > 0) {
                    put(MediaStore.Audio.Media.DURATION, candidate.durationMs)
                }
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
            }

            destUri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            pendingDestUri = destUri

            // Stream raw bytes directly from source URI into destination MediaStore output stream
            context.contentResolver.openInputStream(candidate.sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                } ?: return@withContext false
            } ?: return@withContext false

            // Mark file as fully written and ready in MediaStore
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(destUri, values, null, null)
            pendingDestUri = null

            // Request MediaScannerConnection scan to index tags immediately
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destUri.toString()),
                    arrayOf(candidate.mimeType),
                    null
                )
            } catch (e: Exception) {
                // Non-fatal
            }

            true
        } catch (e: CancellationException) {
            // Clean up incomplete pending file
            destUri?.let { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (delEx: Exception) {
                    // Safe ignore
                }
            }
            pendingDestUri = null
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy audio bytes for ${candidate.fileName}: ${e.message}", e)
            destUri?.let { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (delEx: Exception) {
                    // Safe ignore
                }
            }
            pendingDestUri = null
            false
        }
    }

    /**
     * Inspects a candidate audio file via ContentResolver and MediaMetadataRetriever.
     * Evaluates whether it constitutes a duplicate of any existing song.
     */
    private fun inspectAudioUri(uri: Uri, existingSongs: List<Song>): ImportCandidate? {
        var displayName = "audio_track"
        var sizeBytes = 0L

        // Read name and size
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read OpenableColumns: ${e.message}")
        }

        val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (extension.isNotEmpty() && extension !in SUPPORTED_AUDIO_EXTENSIONS) {
            return null
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val rawDurationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val rawMimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val rawBitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

            val durationMs = rawDurationStr?.toLongOrNull() ?: 0L
            if (durationMs < MIN_AUDIO_DURATION_MS) {
                // Skip sounds under 1 second (notifications, UI clicks, corrupt headers)
                return null
            }

            val title = if (!rawTitle.isNullOrBlank()) rawTitle else displayName.substringBeforeLast('.')
            val artist = if (!rawArtist.isNullOrBlank()) rawArtist else "Unknown Artist"
            val album = if (!rawAlbum.isNullOrBlank()) rawAlbum else "Unknown Album"
            val mimeType = if (!rawMimeType.isNullOrBlank()) rawMimeType else getMimeTypeFromExtension(extension)
            val bitrateKbps = rawBitrateStr?.toIntOrNull()?.let { if (it > 0) it / 1000 else null }

            // Evaluate duplicate match against existing library
            var matchedSong: Song? = null
            var matchedConfidence: DuplicateConfidence? = null

            val candidateAsSong = Song(
                id = -1L,
                title = title,
                artistId = null,
                artistName = artist,
                albumId = null,
                albumName = album,
                genreId = null,
                genreName = null,
                duration = durationMs,
                trackNumber = null,
                discNumber = null,
                year = null,
                contentUri = uri.toString(),
                mimeType = mimeType,
                size = sizeBytes,
                bitrate = bitrateKbps,
                sampleRate = null,
                channelCount = null,
                dateAdded = 0L,
                dateModified = 0L,
                albumArtUri = null
            )

            for (existing in existingSongs) {
                val confidence = DuplicateDetectionEngine.evaluateMatch(candidateAsSong, existing)
                if (confidence != null) {
                    matchedSong = existing
                    matchedConfidence = confidence
                    if (confidence == DuplicateConfidence.EXACT) break
                }
            }

            return ImportCandidate(
                sourceUri = uri,
                fileName = displayName,
                sizeBytes = sizeBytes,
                mimeType = mimeType,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                bitrateKbps = bitrateKbps,
                isDuplicate = (matchedConfidence != null),
                existingSongMatch = matchedSong,
                duplicateConfidence = matchedConfidence
            )
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed to read $displayName: ${e.message}")
            return null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Safe ignore
            }
        }
    }

    /**
     * Traverses a SAF Document Tree URI and collects supported audio file document URIs.
     */
    private fun findAudioFilesFromTreeUri(treeUri: Uri): List<Uri> {
        val results = mutableListOf<Uri>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val queue = ArrayDeque<String>()
        queue.add(rootDocId)

        while (queue.isNotEmpty()) {
            val currentDocId = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )

            try {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val childId = cursor.getString(idIdx)
                        val mime = cursor.getString(mimeIdx)
                        val name = cursor.getString(nameIdx)

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(childId)
                        } else if (isSupportedAudioFile(name, mime)) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                            results.add(docUri)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying child documents for $currentDocId: ${e.message}")
            }
        }

        return results
    }

    private fun isSupportedAudioFile(fileName: String, mimeType: String?): Boolean {
        if (mimeType != null && mimeType.startsWith("audio/")) return true
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in SUPPORTED_AUDIO_EXTENSIONS
    }

    private fun getMimeTypeFromExtension(ext: String): String {
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            else -> "audio/*"
        }
    }
}
