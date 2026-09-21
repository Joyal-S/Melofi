package com.auralis.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.usecase.GetSongsUseCase
import com.auralis.app.domain.usecase.ScanLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class SyncState(
    val isScanning: Boolean = false,
    val permissionGranted: Boolean? = null,
    val isPermanentlyDenied: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SongsViewModel @Inject constructor(
    private val getSongsUseCase: GetSongsUseCase,
    private val scanLibraryUseCase: ScanLibraryUseCase
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SongSortOrder.TITLE)
    private val _syncState = MutableStateFlow(SyncState())

    private val _songsFlow = _sortOrder.flatMapLatest { order ->
        getSongsUseCase(order)
    }

    val uiState: StateFlow<SongsUiState> = combine(
        _songsFlow,
        _sortOrder,
        _syncState
    ) { songs, sortOrder, syncState ->
        when {
            syncState.permissionGranted == false -> {
                SongsUiState.PermissionRequired(isPermanentlyDenied = syncState.isPermanentlyDenied)
            }
            syncState.errorMessage != null -> {
                SongsUiState.Error(message = syncState.errorMessage)
            }
            syncState.permissionGranted == null -> {
                SongsUiState.Loading
            }
            songs.isEmpty() && !syncState.isScanning -> {
                SongsUiState.Empty(isPermissionGranted = true)
            }
            else -> {
                SongsUiState.Success(
                    songs = songs,
                    sortOrder = sortOrder,
                    isScanning = syncState.isScanning
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SongsUiState.Loading
    )

    fun onPermissionStatus(granted: Boolean, permanentlyDenied: Boolean = false) {
        val wasPreviouslyGranted = _syncState.value.permissionGranted == true
        _syncState.update {
            it.copy(permissionGranted = granted, isPermanentlyDenied = permanentlyDenied)
        }
        if (granted && !wasPreviouslyGranted) {
            // Automatically run initial scan upon first grant
            scanLibrary()
        }
    }

    fun setSortOrder(sortOrder: SongSortOrder) {
        _sortOrder.value = sortOrder
    }

    fun scanLibrary() {
        if (_syncState.value.isScanning) return
        viewModelScope.launch {
            _syncState.update { it.copy(isScanning = true, errorMessage = null) }
            val result = scanLibraryUseCase()
            result.fold(
                onSuccess = {
                    _syncState.update { it.copy(isScanning = false) }
                },
                onFailure = { error ->
                    _syncState.update {
                        it.copy(
                            isScanning = false,
                            errorMessage = error.localizedMessage ?: "Failed to scan media library."
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _syncState.update { it.copy(errorMessage = null) }
    }
}
