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
import com.auralis.app.domain.model.SmartPlaylistType
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
import com.auralis.app.ui.playlists.smart.SmartPlaylistDetailScreen
import com.auralis.app.ui.playlists.smart.SmartPlaylistDetailViewModel
import com.auralis.app.ui.settings.AudioSettingsScreen
import com.auralis.app.ui.stats.StatisticsScreen

enum class MainTab {
    SONGS,
    FAVORITES,
    PLAYLISTS,
    STATS,
    AUDIO
}

/**
 * Top-level navigation host coordinating library tabs, playlist details,
 * listening statistics, smart playlists, and the persistent Media3 playback controls bar.
 */
@Composable
fun MainNavigationScreen(
    songsViewModel: SongsViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    playlistDetailViewModel: PlaylistDetailViewModel = hiltViewModel(),
    smartPlaylistDetailViewModel: SmartPlaylistDetailViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.SONGS) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedSmartPlaylistType by remember { mutableStateOf<SmartPlaylistType?>(null) }

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()

    BackHandler(enabled = selectedPlaylistId != null || selectedSmartPlaylistType != null) {
        selectedPlaylistId = null
        selectedSmartPlaylistType = null
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
                if (selectedPlaylistId == null && selectedSmartPlaylistType == null) {
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
                        NavigationBarItem(
                            selected = selectedTab == MainTab.STATS,
                            onClick = { selectedTab = MainTab.STATS },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_insights),
                                    contentDescription = "Stats"
                                )
                            },
                            label = { Text("Stats") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.AUDIO,
                            onClick = { selectedTab = MainTab.AUDIO },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_tune),
                                    contentDescription = "Audio"
                                )
                            },
                            label = { Text("Audio") }
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
        val activeSmartPlaylistType = selectedSmartPlaylistType

        if (activeSmartPlaylistType != null) {
            SmartPlaylistDetailScreen(
                playlistType = activeSmartPlaylistType,
                viewModel = smartPlaylistDetailViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { selectedSmartPlaylistType = null },
                modifier = screenModifier
            )
        } else if (activePlaylistId != null) {
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
                        onSmartPlaylistClick = { selectedSmartPlaylistType = it },
                        modifier = screenModifier
                    )
                }
                MainTab.STATS -> {
                    StatisticsScreen(
                        modifier = screenModifier
                    )
                }
                MainTab.AUDIO -> {
                    AudioSettingsScreen(
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
