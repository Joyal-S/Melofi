package com.auralis.app.data.repository

import androidx.room.withTransaction
import com.auralis.app.data.local.db.AuralisDatabase
import com.auralis.app.data.local.db.dao.AlbumDao
import com.auralis.app.data.local.db.dao.ArtistDao
import com.auralis.app.data.local.db.dao.GenreDao
import com.auralis.app.data.local.db.dao.SongDao
import com.auralis.app.data.local.scanner.LibrarySyncDiff
import com.auralis.app.data.local.scanner.MediaStoreAudioScanner
import com.auralis.app.data.mapper.SongMapper
import com.auralis.app.di.IoDispatcher
import com.auralis.app.domain.model.ScanResult
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.auralis.app.data.local.metadata.SongMetadataInspector
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.domain.model.DuplicateDetectionEngine
import com.auralis.app.domain.model.DuplicateGroup
import com.auralis.app.domain.model.SongDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [SongRepository] orchestrating MediaStore discovery,
 * Room persistence, and differential synchronization.
 */
@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val genreDao: GenreDao,
    private val database: AuralisDatabase,
    private val scanner: MediaStoreAudioScanner,
    private val metadataInspector: SongMetadataInspector,
    private val artworkRepository: LocalArtworkRepository,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SongRepository {

    override fun getSongs(sortOrder: SongSortOrder): Flow<List<Song>> {
        val flow = when (sortOrder) {
            SongSortOrder.TITLE -> songDao.getSongsSortedByTitle()
            SongSortOrder.ARTIST -> songDao.getSongsSortedByArtist()
            SongSortOrder.ALBUM -> songDao.getSongsSortedByAlbum()
            SongSortOrder.DATE_ADDED -> songDao.getSongsSortedByDateAdded()
            else -> songDao.getSongsSortedByTitle()
        }
        return kotlinx.coroutines.flow.combine(flow, artworkRepository.artworkUpdates) { list, _ ->
            list.map { entity ->
                val domain = SongMapper.toDomain(entity)
                val resolvedArt = artworkRepository.resolveSongArtworkFast(domain)
                domain.copy(albumArtUri = resolvedArt)
            }
        }
    }

    override fun getSongById(id: Long): Flow<Song?> {
        return kotlinx.coroutines.flow.combine(songDao.getSongById(id), artworkRepository.artworkUpdates) { entity, _ ->
            entity?.let {
                val domain = SongMapper.toDomain(it)
                val resolvedArt = artworkRepository.resolveSongArtworkFast(domain)
                domain.copy(albumArtUri = resolvedArt)
            }
        }
    }

    override suspend fun scanLibrary(): Result<ScanResult> = withContext(ioDispatcher) {
        try {
            // 1. Scan external MediaStore via ContentResolver
            val scanOutput = scanner.scanAudioFiles()

            // 2. Fetch existing songs sync info (ID and dateModified)
            val existingList = songDao.getAllSongSyncInfo()
            val existingMap = existingList.associate { it.id to it.dateModified }

            // 3. Compute deterministic differential
            val diff = LibrarySyncDiff.computeDiff(existingMap, scanOutput.songs)

            // 4. Atomically persist changes in a single Room transaction
            database.withTransaction {
                if (diff.toDeleteIds.isNotEmpty()) {
                    songDao.deleteSongsByIds(diff.toDeleteIds)
                }
                if (diff.toInsert.isNotEmpty()) {
                    songDao.insertSongs(diff.toInsert)
                }
                if (diff.toUpdate.isNotEmpty()) {
                    songDao.updateSongs(diff.toUpdate)
                }

                // Synchronize aggregated album, artist, and genre groupings
                albumDao.syncAlbums(scanOutput.albums)
                artistDao.syncArtists(scanOutput.artists)
                genreDao.syncGenres(scanOutput.genres)
            }

            // 5. Ensure newly discovered songs have permanent default covers assigned
            if (diff.toInsert.isNotEmpty()) {
                for (newSong in diff.toInsert) {
                    val domain = SongMapper.toDomain(newSong)
                    artworkRepository.resolveSongArtworkFast(domain)
                }
            }

            val result = ScanResult(
                totalFound = scanOutput.songs.size,
                inserted = diff.toInsert.size,
                updated = diff.toUpdate.size,
                deleted = diff.toDeleteIds.size
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSongDetails(id: Long): Flow<SongDetails?> {
        return songDao.getSongById(id).map { entity ->
            entity?.let {
                val domain = SongMapper.toDomain(it)
                val baseDetails = metadataInspector.inspectSong(domain)
                val resolvedArt = artworkRepository.getArtworkUri(domain)
                if (resolvedArt != null) {
                    baseDetails.copy(
                        technicalInfo = baseDetails.technicalInfo.copy(albumArtUri = resolvedArt)
                    )
                } else {
                    baseDetails
                }
            }
        }
    }

    override suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        albumArtist: String?,
        genre: String?,
        composer: String?,
        year: Int?,
        trackNumber: Int?,
        discNumber: Int?
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Defensive input validation and length bounding
            val validation = com.auralis.app.domain.model.MetadataEditValidator.validate(
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                composer = composer,
                yearText = year?.toString(),
                trackNumberText = trackNumber?.toString(),
                discNumberText = discNumber?.toString()
            )
            if (!validation.isValid || validation.sanitizedMetadata == null) {
                return@withContext Result.failure(
                    IllegalArgumentException(validation.errorMessage ?: "Invalid metadata values")
                )
            }
            val clean = validation.sanitizedMetadata

            val entity = songDao.getAllSongsList().find { it.id == songId }
                ?: return@withContext Result.failure(IllegalArgumentException("Song with ID $songId not found"))

            // Safely attempt to update MediaStore record
            try {
                val uri = Uri.parse(entity.contentUri)
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, clean.title)
                    put(MediaStore.Audio.Media.ARTIST, clean.artist)
                    put(MediaStore.Audio.Media.ALBUM, clean.album)
                    if (clean.year != null && clean.year > 0) {
                        put(MediaStore.Audio.Media.YEAR, clean.year)
                    }
                    if (clean.trackNumber != null && clean.trackNumber > 0) {
                        put(MediaStore.Audio.Media.TRACK, clean.trackNumber)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        if (!clean.genre.isNullOrBlank()) {
                            put(MediaStore.Audio.AudioColumns.GENRE, clean.genre)
                        }
                        if (!clean.albumArtist.isNullOrBlank()) {
                            put(MediaStore.Audio.AudioColumns.ALBUM_ARTIST, clean.albumArtist)
                        }
                        if (!clean.composer.isNullOrBlank()) {
                            put(MediaStore.Audio.AudioColumns.COMPOSER, clean.composer)
                        }
                    }
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (se: SecurityException) {
                Log.w("SongRepository", "MediaStore ContentResolver update caught SecurityException: ${se.message}")
            } catch (e: Exception) {
                Log.w("SongRepository", "MediaStore update failed: ${e.message}")
            }

            // Atomically update local Room database so UI and library immediately reflect changes
            val updatedEntity = entity.copy(
                title = clean.title,
                artistName = clean.artist,
                albumName = clean.album,
                genreName = clean.genre ?: entity.genreName,
                year = clean.year ?: entity.year,
                trackNumber = clean.trackNumber ?: entity.trackNumber,
                discNumber = clean.discNumber ?: entity.discNumber,
                dateModified = System.currentTimeMillis() / 1000L
            )
            songDao.updateSong(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Error updating song metadata: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun findDuplicates(): List<DuplicateGroup> = withContext(ioDispatcher) {
        val entities = songDao.getAllSongsList()
        val songs = entities.map { SongMapper.toDomain(it) }
        DuplicateDetectionEngine.findDuplicates(songs)
    }

    override suspend fun deleteSong(songId: Long): Result<Unit> = withContext(ioDispatcher) {
        try {
            val entity = songDao.getAllSongsList().find { it.id == songId }
                ?: return@withContext Result.failure(IllegalArgumentException("Song with ID $songId not found"))

            // Safely attempt MediaStore deletion
            try {
                val uri = Uri.parse(entity.contentUri)
                context.contentResolver.delete(uri, null, null)
            } catch (se: SecurityException) {
                Log.w("SongRepository", "MediaStore deletion SecurityException: ${se.message}")
            } catch (e: Exception) {
                Log.w("SongRepository", "MediaStore deletion error: ${e.message}")
            }

            // Remove from Room database (cascading removes favorites and playlist links without deleting playlists)
            songDao.deleteSongById(songId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongRepository", "Error deleting song: ${e.message}", e)
            Result.failure(e)
        }
    }
}
