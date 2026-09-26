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
        discNumber: Int? = null,
        artistId: Long? = null
    ): Result<Unit>

    /**
     * Explicitly reassigns a song to an existing artist identity.
     */
    suspend fun reassignSongArtist(songId: Long, newArtistId: Long): Result<Unit>

    /**
     * Identifies duplicate groups across the local song library.
     */
    suspend fun findDuplicates(): List<com.auralis.app.domain.model.DuplicateGroup>

    /**
     * Safely deletes a song file from MediaStore and Room with explicit user confirmation.
     */
    suspend fun deleteSong(songId: Long): Result<Unit>

    /**
     * Hides an audio file from the Melofi Music Library without deleting it from disk.
     */
    suspend fun hideSongFromLibrary(song: Song): Result<Unit>

    /**
     * Restores a previously hidden audio file back into the Music Library.
     */
    suspend fun restoreHiddenAudio(id: Long): Result<Unit>

    /**
     * Observes all user-hidden audio records.
     */
    fun getHiddenAudio(): Flow<List<com.auralis.app.data.local.db.entity.HiddenAudioEntity>>

    /**
     * Observes all folder exclusions.
     */
    fun getExcludedFolders(): Flow<List<com.auralis.app.data.local.db.entity.ExcludedFolderEntity>>

    /**
     * Toggles whether an excluded folder pattern is currently active.
     */
    suspend fun toggleExcludedFolder(id: Long, isEnabled: Boolean)

    /**
     * Adds a new custom folder exclusion pattern.
     */
    suspend fun addExcludedFolder(folderName: String, folderPath: String): Result<Unit>

    /**
     * Deletes a custom folder exclusion pattern.
     */
    suspend fun deleteExcludedFolder(id: Long): Result<Unit>
}
