package com.auralis.app.domain.repository

import com.auralis.app.domain.model.DuplicateGroup
import com.auralis.app.domain.model.ScanResult
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongDetails
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

    /**
     * Observe detailed metadata and technical audio specifications for a song.
     */
    fun getSongDetails(id: Long): Flow<SongDetails?>

    /**
     * Updates song metadata safely in MediaStore and Room database.
     */
    suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        albumArtist: String? = null,
        genre: String? = null,
        composer: String? = null,
        year: Int? = null,
        trackNumber: Int? = null,
        discNumber: Int? = null
    ): Result<Unit>

    /**
     * Identifies duplicate groups across the local song library.
     */
    suspend fun findDuplicates(): List<com.auralis.app.domain.model.DuplicateGroup>

    /**
     * Safely deletes a song file from MediaStore and Room with explicit user confirmation.
     */
    suspend fun deleteSong(songId: Long): Result<Unit>
}
