package com.auralis.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.auralis.app.data.local.db.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for managing local search history.
 * Automatically deduplicates and keeps recent queries sorted by timestamp.
 */
@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSearch(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchByQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    @Query("SELECT * FROM search_history WHERE query = :query LIMIT 1")
    suspend fun getSearchByQuery(query: String): SearchHistoryEntity?

    @Query("""
        DELETE FROM search_history
        WHERE id NOT IN (
            SELECT id FROM search_history
            ORDER BY searchedAt DESC
            LIMIT :maxEntries
        )
    """)
    suspend fun pruneSearchHistory(maxEntries: Int = 20)

    @Transaction
    suspend fun recordSearch(query: String, maxEntries: Int = 20) {
        val clean = query.trim().take(200)
        if (clean.isEmpty()) return
        val existing = getSearchByQuery(clean)
        if (existing != null) {
            insertOrUpdateSearch(existing.copy(searchedAt = System.currentTimeMillis()))
        } else {
            insertOrUpdateSearch(
                SearchHistoryEntity(
                    query = clean,
                    searchedAt = System.currentTimeMillis()
                )
            )
        }
        pruneSearchHistory(maxEntries)
    }
}
