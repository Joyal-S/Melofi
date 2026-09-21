package com.auralis.app.ui.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.DuplicateGroup
import com.auralis.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DuplicateUiState {
    data object Loading : DuplicateUiState
    data object Empty : DuplicateUiState
    data class Success(
        val groups: List<DuplicateGroup>,
        val totalDuplicateTracks: Int,
        val message: String? = null
    ) : DuplicateUiState
}

@HiltViewModel
class DuplicateDetectionViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DuplicateUiState>(DuplicateUiState.Loading)
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    init {
        scanDuplicates()
    }

    fun scanDuplicates() {
        viewModelScope.launch {
            _uiState.value = DuplicateUiState.Loading
            val duplicates = songRepository.findDuplicates()
            if (duplicates.isEmpty()) {
                _uiState.value = DuplicateUiState.Empty
            } else {
                val totalTracks = duplicates.sumOf { it.songs.size }
                _uiState.value = DuplicateUiState.Success(
                    groups = duplicates,
                    totalDuplicateTracks = totalTracks
                )
            }
        }
    }

    fun deleteTrack(songId: Long) {
        viewModelScope.launch {
            val result = songRepository.deleteSong(songId)
            result.onSuccess {
                scanDuplicates()
            }.onFailure { error ->
                val current = _uiState.value
                if (current is DuplicateUiState.Success) {
                    _uiState.value = current.copy(message = "Delete failed: ${error.message}")
                }
            }
        }
    }

    fun clearMessage() {
        val current = _uiState.value
        if (current is DuplicateUiState.Success) {
            _uiState.value = current.copy(message = null)
        }
    }
}
