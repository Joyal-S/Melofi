package com.auralis.app.domain.repository

import com.auralis.app.domain.model.ScanResult
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for accessing and synchronizing the local song library.
 */
interface SongRepository {
    /**
     * Observe the list of songs in the library, sorted by the requested sort order.
     */
    fun getSongs(sortOrder: SongSortOrder = SongSortOrder.TITLE): Flow<List<Song>>

    /**
     * Observe a single song by its ID.
     */
    fun getSongById(id: Long): Flow<Song?>

    /**
     * Run a full differential synchronization between MediaStore and local database.
     */
    suspend fun scanLibrary(): Result<ScanResult>
}
