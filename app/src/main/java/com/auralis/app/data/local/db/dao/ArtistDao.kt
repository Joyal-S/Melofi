package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.auralis.app.data.local.db.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for artists.
 * Provides high-performance reactive queries, single-row lookups,
 * and atomic aggregation count synchronization.
 */
@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun getArtistById(id: Long): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE id = :id LIMIT 1")
    suspend fun findArtistById(id: Long): ArtistEntity?

    @Query("SELECT * FROM artists WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findArtistByNormalizedName(normalizedName: String): ArtistEntity?

    @Query("SELECT * FROM artists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findArtistByName(name: String): ArtistEntity?

    @Query("SELECT * FROM artists")
    suspend fun getAllArtistsList(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' OR normalizedName LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchArtists(query: String): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateArtist(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Update
    suspend fun updateArtist(artist: ArtistEntity)

    @Query("UPDATE artists SET name = :name, normalizedName = :normalizedName, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun updateArtistName(artistId: Long, name: String, normalizedName: String, updatedAt: Long): Int

    @Query("UPDATE artists SET customArtworkUri = :artworkUri, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun updateArtistArtwork(artistId: Long, artworkUri: String?, updatedAt: Long): Int

    @Query("DELETE FROM artists WHERE id = :id")
    suspend fun deleteArtistById(id: Long): Int

    @Query("""
        UPDATE artists SET
            songCount = (SELECT COUNT(*) FROM songs WHERE songs.artistId = artists.id),
            albumCount = (SELECT COUNT(DISTINCT albumId) FROM songs WHERE songs.artistId = artists.id AND albumId IS NOT NULL)
    """)
    suspend fun recalculateAllArtistCounts(): Int

    @Query("""
        UPDATE artists SET
            songCount = (SELECT COUNT(*) FROM songs WHERE songs.artistId = :artistId),
            albumCount = (SELECT COUNT(DISTINCT albumId) FROM songs WHERE songs.artistId = :artistId AND albumId IS NOT NULL)
        WHERE id = :artistId
    """)
    suspend fun recalculateArtistCounts(artistId: Long): Int

    @Transaction
    suspend fun syncArtistsPreservingIdentity(scannedArtists: List<ArtistEntity>) {
        for (scanned in scannedArtists) {
            val existing = findArtistByNormalizedName(scanned.normalizedName)
            if (existing == null) {
                insertArtist(scanned)
            }
        }
        recalculateAllArtistCounts()
    }
}
