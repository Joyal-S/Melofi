package com.auralis.app.domain.usecase

import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve the list of songs from the repository with the specified sort order.
 */
class GetSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(sortOrder: SongSortOrder = SongSortOrder.TITLE): Flow<List<Song>> =
        songRepository.getSongs(sortOrder)
}
