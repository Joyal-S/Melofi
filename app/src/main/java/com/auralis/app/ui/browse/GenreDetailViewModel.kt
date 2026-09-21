package com.auralis.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.domain.model.Genre
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.FavoriteRepository
import com.auralis.app.domain.repository.LibraryBrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GenreDetailUiState {
    data object Loading : GenreDetailUiState
    data class Success(
        val genre: Genre,
        val songs: List<Song>
    ) : GenreDetailUiState
    data class Error(val message: String) : GenreDetailUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val browserRepository: LibraryBrowserRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _genreId = MutableStateFlow<Long?>(null)

    val favoriteSongIds: StateFlow<Set<Long>> = favoriteRepository.getFavoriteSongIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val uiState: StateFlow<GenreDetailUiState> = _genreId.flatMapLatest { id ->
        if (id == null) {
            flowOf(GenreDetailUiState.Loading)
        } else {
            browserRepository.getGenreById(id).flatMapLatest { genre ->
                if (genre == null) {
                    flowOf(GenreDetailUiState.Error("Genre not found."))
                } else {
                    browserRepository.getSongsByGenre(genre.name).map { songs ->
                        GenreDetailUiState.Success(genre, songs)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GenreDetailUiState.Loading
    )

    fun loadGenre(genreId: Long) {
        _genreId.value = genreId
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(songId)
        }
    }
}
