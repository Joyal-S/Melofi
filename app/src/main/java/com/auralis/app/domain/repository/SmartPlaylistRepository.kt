package com.auralis.app.domain.repository

import com.auralis.app.domain.model.SmartPlaylistType
import com.auralis.app.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository managing dynamically computed smart playlists.
 */
interface SmartPlaylistRepository {

    fun getSmartPlaylists(): List<SmartPlaylistType>

    fun getSongsForSmartPlaylist(type: SmartPlaylistType): Flow<List<Song>>
}
