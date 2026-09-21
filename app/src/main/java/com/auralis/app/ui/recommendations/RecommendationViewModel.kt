package com.auralis.app.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.RecommendationSection
import com.auralis.app.domain.model.RecommendationSectionType
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecommendationUiState {
    data object Loading : RecommendationUiState
    data object Empty : RecommendationUiState
    data class Success(val sections: List<RecommendationSection>) : RecommendationUiState
}

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    val uiState: StateFlow<RecommendationUiState> = recommendationRepository.getRecommendationSections()
        .map { sections ->
            if (sections.isEmpty()) {
                RecommendationUiState.Empty
            } else {
                RecommendationUiState.Success(sections)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecommendationUiState.Loading
        )

    val favoriteSongIds: StateFlow<Set<Long>> = favoriteRepository.getFavoriteSongIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _selectedSectionType = MutableStateFlow<RecommendationSectionType?>(null)
    val selectedSectionType: StateFlow<RecommendationSectionType?> = _selectedSectionType.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedSectionDetail: StateFlow<RecommendationSection?> = _selectedSectionType.flatMapLatest { type ->
        if (type == null) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            recommendationRepository.getSectionDetail(type)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectSection(type: RecommendationSectionType?) {
        _selectedSectionType.value = type
    }

    fun refresh() {
        viewModelScope.launch {
            recommendationRepository.refreshRecommendations()
        }
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(songId)
        }
    }
}
