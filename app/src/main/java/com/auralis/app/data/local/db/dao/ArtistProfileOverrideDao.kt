package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.auralis.app.data.local.db.entity.ArtistProfileOverrideEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user-customized artist profile display names.
 */
@Dao
interface ArtistProfileOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(override: ArtistProfileOverrideEntity)

    @Query("SELECT * FROM artist_profile_overrides WHERE artistId = :artistId LIMIT 1")
    suspend fun getOverride(artistId: Long): ArtistProfileOverrideEntity?

    @Query("SELECT * FROM artist_profile_overrides WHERE artistId = :artistId LIMIT 1")
    fun getOverrideFlow(artistId: Long): Flow<ArtistProfileOverrideEntity?>

    @Query("SELECT * FROM artist_profile_overrides")
    suspend fun getAllOverrides(): List<ArtistProfileOverrideEntity>

    @Query("SELECT * FROM artist_profile_overrides")
    fun getAllOverridesFlow(): Flow<List<ArtistProfileOverrideEntity>>

    @Query("DELETE FROM artist_profile_overrides WHERE artistId = :artistId")
    suspend fun deleteOverride(artistId: Long)
}
