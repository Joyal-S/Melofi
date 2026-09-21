package com.auralis.app.domain.usecase.favorites

import com.auralis.app.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Use case to toggle the favorite status of a song.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(songId: Long): Boolean {
        return favoriteRepository.toggleFavorite(songId)
    }
}
