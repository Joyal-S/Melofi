package com.auralis.app.ui.playlists.smart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.SmartPlaylistType
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.SmartPlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel managing dynamic song loading for a specific [SmartPlaylistType].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmartPlaylistDetailViewModel @Inject constructor(
    private val smartPlaylistRepository: SmartPlaylistRepository
) : ViewModel() {

    private val _playlistType = MutableStateFlow<SmartPlaylistType?>(null)
    val playlistType: StateFlow<SmartPlaylistType?> = _playlistType.asStateFlow()

    val songs: StateFlow<List<Song>> = _playlistType.flatMapLatest { type ->
        if (type != null) {
            smartPlaylistRepository.getSongsForSmartPlaylist(type)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setPlaylistType(type: SmartPlaylistType) {
        _playlistType.value = type
    }
}
