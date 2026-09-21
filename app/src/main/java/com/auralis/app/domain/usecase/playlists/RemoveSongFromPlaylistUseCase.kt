package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case to remove a song from a playlist and compact ordering.
 */
class RemoveSongFromPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long): Result<Unit> {
        return playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }
}
