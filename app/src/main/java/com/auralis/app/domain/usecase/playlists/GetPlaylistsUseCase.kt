package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe all playlists.
 */
class GetPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return playlistRepository.getPlaylists()
    }
}
