package com.auralis.app.domain.usecase.favorites

import com.auralis.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the reactive set of favorite song IDs for O(1) in-memory checks.
 */
class GetFavoriteSongIdsUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<Set<Long>> {
        return favoriteRepository.getFavoriteSongIds()
    }
}
