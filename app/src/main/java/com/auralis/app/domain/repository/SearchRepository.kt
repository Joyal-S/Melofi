package com.auralis.app.domain.repository

import com.auralis.app.domain.model.SearchResultGroup
import com.auralis.app.domain.model.SearchSuggestion
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for local multi-field search, history, and suggestions.
 */
interface SearchRepository {
    fun getRecentSearches(limit: Int = 20): Flow<List<String>>
    suspend fun recordSearch(query: String)
    suspend fun deleteSearch(query: String)
    suspend fun clearSearchHistory()
    suspend fun searchCategorized(query: String): SearchResultGroup
    suspend fun getSearchSuggestions(query: String): List<SearchSuggestion>
}
