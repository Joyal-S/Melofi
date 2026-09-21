package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case to reorder playlist songs.
 */
class ReorderPlaylistSongsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, orderedSongIds: List<Long>): Result<Unit> {
        return playlistRepository.reorderPlaylistSongs(playlistId, orderedSongIds)
    }
}
