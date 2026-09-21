package com.auralis.app.domain.usecase.favorites

import com.auralis.app.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Use case to check if a specific song is marked as favorite.
 */
class IsFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(songId: Long): Boolean {
        return favoriteRepository.isFavorite(songId)
    }
}
