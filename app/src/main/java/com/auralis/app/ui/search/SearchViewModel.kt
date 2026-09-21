package com.auralis.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.SearchResultGroup
import com.auralis.app.domain.model.SearchSuggestion
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val group: SearchResultGroup) : SearchUiState
    data class Empty(val query: String) : SearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestion>> = _suggestions.asStateFlow()

    val recentSearches: StateFlow<List<String>> = searchRepository.getRecentSearches(20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteSongIds: StateFlow<Set<Long>> = favoriteRepository.getFavoriteSongIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        // Debounced search trigger (250ms)
        viewModelScope.launch {
            _query
                .debounce(250L)
                .distinctUntilChanged()
                .collect { q ->
                    val clean = q.trim()
                    if (clean.isEmpty()) {
                        _uiState.value = SearchUiState.Idle
                        loadSuggestions("")
                    } else {
                        executeSearch(clean, saveHistory = false)
                        loadSuggestions(clean)
                    }
                }
        }
        loadSuggestions("")
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onSearchExplicit(queryToSearch: String) {
        _query.value = queryToSearch
        val clean = queryToSearch.trim()
        if (clean.isNotEmpty()) {
            executeSearch(clean, saveHistory = true)
        }
    }

    fun clearQuery() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
        loadSuggestions("")
    }

    fun deleteHistoryQuery(queryToDelete: String) {
        viewModelScope.launch {
            searchRepository.deleteSearch(queryToDelete)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchRepository.clearSearchHistory()
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(songId)
        }
    }

    private fun executeSearch(cleanQuery: String, saveHistory: Boolean) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            if (saveHistory) {
                searchRepository.recordSearch(cleanQuery)
            }
            val results = searchRepository.searchCategorized(cleanQuery)
            if (results.isEmpty) {
                _uiState.value = SearchUiState.Empty(cleanQuery)
            } else {
                _uiState.value = SearchUiState.Results(results)
            }
        }
    }

    private fun loadSuggestions(cleanQuery: String) {
        viewModelScope.launch {
            _suggestions.value = searchRepository.getSearchSuggestions(cleanQuery)
        }
    }
}
