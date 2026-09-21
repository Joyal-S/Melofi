package com.auralis.app.domain.usecase.favorites

import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the list of favorite songs.
 */
class GetFavoriteSongsUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return favoriteRepository.getFavoriteSongs()
    }
}
