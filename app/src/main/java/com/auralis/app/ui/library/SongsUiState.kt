package com.auralis.app.ui.library

import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder

/**
 * UI State for the Songs library screen.
 */
sealed interface SongsUiState {
    data object Loading : SongsUiState

    data class PermissionRequired(
        val isPermanentlyDenied: Boolean = false
    ) : SongsUiState

    data class Empty(
        val isPermissionGranted: Boolean
    ) : SongsUiState

    data class Success(
        val songs: List<Song>,
        val sortOrder: SongSortOrder,
        val isScanning: Boolean = false
    ) : SongsUiState

    data class Error(
        val message: String
    ) : SongsUiState
}
