package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.model.Song
import com.auralis.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the ordered songs of a playlist.
 */
class GetPlaylistSongsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(playlistId: Long): Flow<List<Song>> {
        return playlistRepository.getPlaylistSongs(playlistId)
    }
}
