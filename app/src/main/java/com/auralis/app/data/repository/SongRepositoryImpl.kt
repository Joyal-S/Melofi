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
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SongRepository {

    override fun getSongs(sortOrder: SongSortOrder): Flow<List<Song>> {
        val flow = when (sortOrder) {
            SongSortOrder.TITLE -> songDao.getSongsSortedByTitle()
            SongSortOrder.ARTIST -> songDao.getSongsSortedByArtist()
            SongSortOrder.ALBUM -> songDao.getSongsSortedByAlbum()
            SongSortOrder.DATE_ADDED -> songDao.getSongsSortedByDateAdded()
        }
        return flow.map { SongMapper.toDomainList(it) }
    }

    override fun getSongById(id: Long): Flow<Song?> {
        return songDao.getSongById(id).map { it?.let { entity -> SongMapper.toDomain(entity) } }
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
}
