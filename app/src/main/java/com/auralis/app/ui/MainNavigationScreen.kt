package com.auralis.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.ui.favorites.FavoritesScreen
import com.auralis.app.ui.favorites.FavoritesViewModel
import com.auralis.app.ui.library.SongsScreen
import com.auralis.app.ui.library.SongsViewModel
import com.auralis.app.ui.playback.PlaybackViewModel
import com.auralis.app.ui.playback.components.PlaybackControlsBar
import com.auralis.app.ui.playlists.PlaylistsScreen
import com.auralis.app.ui.playlists.PlaylistsViewModel
import com.auralis.app.ui.playlists.detail.PlaylistDetailScreen
import com.auralis.app.ui.playlists.detail.PlaylistDetailViewModel

enum class MainTab {
    SONGS,
    FAVORITES,
    PLAYLISTS
}

/**
 * Top-level navigation host coordinating library tabs, playlist details,
 * and the persistent Media3 playback controls bar.
 */
@Composable
fun MainNavigationScreen(
    songsViewModel: SongsViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    playlistDetailViewModel: PlaylistDetailViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.SONGS) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()

    BackHandler(enabled = selectedPlaylistId != null) {
        selectedPlaylistId = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Persistent Playback Controls Bar
                if (playbackState.currentSong != null) {
                    PlaybackControlsBar(
                        playbackState = playbackState,
                        onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                        onSeekTo = { playbackViewModel.seekTo(it) },
                        onNext = { playbackViewModel.next() },
                        onPrevious = { playbackViewModel.previous() },
                        onToggleShuffle = { playbackViewModel.toggleShuffle() },
                        onCycleRepeat = { playbackViewModel.cycleRepeatMode() }
                    )
                }

                // Root Navigation Bar (hidden on sub-details)
                if (selectedPlaylistId == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == MainTab.SONGS,
                            onClick = { selectedTab = MainTab.SONGS },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music_placeholder),
                                    contentDescription = "Songs"
                                )
                            },
                            label = { Text("Songs") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.FAVORITES,
                            onClick = { selectedTab = MainTab.FAVORITES },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_favorite),
                                    contentDescription = "Favorites"
                                )
                            },
                            label = { Text("Favorites") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.PLAYLISTS,
                            onClick = { selectedTab = MainTab.PLAYLISTS },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_playlist_play),
                                    contentDescription = "Playlists"
                                )
                            },
                            label = { Text("Playlists") }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        val activePlaylistId = selectedPlaylistId
        if (activePlaylistId != null) {
            PlaylistDetailScreen(
                playlistId = activePlaylistId,
                viewModel = playlistDetailViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { selectedPlaylistId = null },
                modifier = screenModifier
            )
        } else {
            when (selectedTab) {
                MainTab.SONGS -> {
                    SongsScreen(
                        viewModel = songsViewModel,
                        playbackViewModel = playbackViewModel,
                        playlists = playlists,
                        onCreatePlaylistAndAdd = { name, songId ->
                            playlistsViewModel.createPlaylist(name) { newId ->
                                songsViewModel.addSongToPlaylist(newId, songId)
                            }
                        },
                        showBottomBar = false,
                        modifier = screenModifier
                    )
                }
                MainTab.FAVORITES -> {
                    FavoritesScreen(
                        favoritesViewModel = favoritesViewModel,
                        playbackViewModel = playbackViewModel,
                        playlists = playlists,
                        onAddSongToPlaylist = { playlistId, songId ->
                            songsViewModel.addSongToPlaylist(playlistId, songId)
                        },
                        onCreatePlaylistAndAdd = { name, songId ->
                            playlistsViewModel.createPlaylist(name) { newId ->
                                songsViewModel.addSongToPlaylist(newId, songId)
                            }
                        },
                        modifier = screenModifier
                    )
                }
                MainTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlistsViewModel = playlistsViewModel,
                        onPlaylistClick = { selectedPlaylistId = it },
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
