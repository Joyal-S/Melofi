package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Use case to add a song to a playlist.
 */
class AddSongToPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long): Result<Unit> {
        return playlistRepository.addSongToPlaylist(playlistId, songId)
    }
}
