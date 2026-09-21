package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case to delete a playlist.
 */
class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return playlistRepository.deletePlaylist(id)
    }
}
