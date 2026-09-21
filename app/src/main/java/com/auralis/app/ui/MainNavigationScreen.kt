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
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.browse.AlbumDetailScreen
import com.auralis.app.ui.browse.AlbumDetailViewModel
import com.auralis.app.ui.browse.ArtistDetailScreen
import com.auralis.app.ui.browse.ArtistDetailViewModel
import com.auralis.app.ui.browse.GenreDetailScreen
import com.auralis.app.ui.browse.GenreDetailViewModel
import com.auralis.app.ui.details.SongDetailsScreen
import com.auralis.app.ui.details.SongDetailsViewModel
import com.auralis.app.ui.duplicates.DuplicateDetectionScreen
import com.auralis.app.ui.duplicates.DuplicateDetectionViewModel
import com.auralis.app.ui.favorites.FavoritesScreen
import com.auralis.app.ui.favorites.FavoritesViewModel
import com.auralis.app.ui.library.SongsScreen
import com.auralis.app.ui.library.SongsViewModel
import com.auralis.app.ui.playback.NowPlayingScreen
import com.auralis.app.ui.playback.PlaybackViewModel
import com.auralis.app.ui.playback.components.PlaybackControlsBar
import com.auralis.app.ui.playlists.PlaylistsScreen
import com.auralis.app.ui.playlists.PlaylistsViewModel
import com.auralis.app.ui.playlists.detail.PlaylistDetailScreen
import com.auralis.app.ui.playlists.detail.PlaylistDetailViewModel
import com.auralis.app.ui.playlists.smart.SmartPlaylistDetailScreen
import com.auralis.app.ui.playlists.smart.SmartPlaylistDetailViewModel
import com.auralis.app.ui.recommendations.RecommendationDetailScreen
import com.auralis.app.ui.recommendations.RecommendationViewModel
import com.auralis.app.ui.search.SearchScreen
import com.auralis.app.ui.search.SearchViewModel
import com.auralis.app.ui.settings.AudioSettingsViewModel
import com.auralis.app.ui.settings.SettingsScreen
import com.auralis.app.ui.settings.SettingsViewModel
import com.auralis.app.ui.stats.StatisticsScreen

enum class MainTab {
    SONGS,
    FAVORITES,
    PLAYLISTS,
    STATS,
    SETTINGS
}

/**
 * Top-level navigation host coordinating library tabs, playlist details,
 * listening statistics, smart playlists, metadata inspection, duplicates,
 * advanced search, category browsing (albums, artists, genres),
 * on-device recommendations, settings, and the persistent Media3 playback controls bar.
 */
@Composable
fun MainNavigationScreen(
    songsViewModel: SongsViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    playlistDetailViewModel: PlaylistDetailViewModel = hiltViewModel(),
    smartPlaylistDetailViewModel: SmartPlaylistDetailViewModel = hiltViewModel(),
    songDetailsViewModel: SongDetailsViewModel = hiltViewModel(),
    duplicateDetectionViewModel: DuplicateDetectionViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    albumDetailViewModel: AlbumDetailViewModel = hiltViewModel(),
    artistDetailViewModel: ArtistDetailViewModel = hiltViewModel(),
    genreDetailViewModel: GenreDetailViewModel = hiltViewModel(),
    recommendationViewModel: RecommendationViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    audioSettingsViewModel: AudioSettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainTab.SONGS) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedSmartPlaylistType by remember { mutableStateOf<SmartPlaylistType?>(null) }
    var selectedSongDetailsId by remember { mutableStateOf<Long?>(null) }
    var showDuplicateScreen by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var showRecommendationDetail by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var selectedAlbumId by remember { mutableStateOf<Long?>(null) }
    var selectedArtistId by remember { mutableStateOf<Long?>(null) }
    var selectedGenreId by remember { mutableStateOf<Long?>(null) }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongIds by songsViewModel.favoriteSongIds.collectAsStateWithLifecycle()

    val isSubScreen = showNowPlaying ||
            selectedPlaylistId != null ||
            selectedSmartPlaylistType != null ||
            selectedSongDetailsId != null ||
            showDuplicateScreen ||
            showSearchScreen ||
            showRecommendationDetail ||
            selectedAlbumId != null ||
            selectedArtistId != null ||
            selectedGenreId != null

    BackHandler(enabled = isSubScreen) {
        when {
            showNowPlaying -> showNowPlaying = false
            selectedSongDetailsId != null -> selectedSongDetailsId = null
            showDuplicateScreen -> showDuplicateScreen = false
            showRecommendationDetail -> showRecommendationDetail = false
            selectedAlbumId != null -> selectedAlbumId = null
            selectedArtistId != null -> selectedArtistId = null
            selectedGenreId != null -> selectedGenreId = null
            showSearchScreen -> showSearchScreen = false
            else -> {
                selectedPlaylistId = null
                selectedSmartPlaylistType = null
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Persistent Playback Controls Bar (hidden when full-screen Now Playing is open)
                if (playbackState.currentSong != null && !showNowPlaying) {
                    PlaybackControlsBar(
                        playbackState = playbackState,
                        onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                        onSeekTo = { playbackViewModel.seekTo(it) },
                        onNext = { playbackViewModel.next() },
                        onPrevious = { playbackViewModel.previous() },
                        onToggleShuffle = { playbackViewModel.toggleShuffle() },
                        onCycleRepeat = { playbackViewModel.cycleRepeatMode() },
                        onClick = { showNowPlaying = true },
                        isFavorite = playbackState.currentSong?.id?.let { it in favoriteSongIds } == true,
                        onToggleFavorite = { playbackState.currentSong?.let { favoritesViewModel.toggleFavorite(it.id) } }
                    )
                }

                // Root Navigation Bar (hidden on sub-details and Now Playing)
                if (!isSubScreen) {
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
                            selected = selectedTab == MainTab.SETTINGS,
                            onClick = { selectedTab = MainTab.SETTINGS },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") }
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
        val activeSongDetailsId = selectedSongDetailsId
        val activeAlbumId = selectedAlbumId
        val activeArtistId = selectedArtistId
        val activeGenreId = selectedGenreId

        if (showNowPlaying) {
            NowPlayingScreen(
                playbackViewModel = playbackViewModel,
                onCollapse = { showNowPlaying = false },
                isFavorite = playbackState.currentSong?.id?.let { it in favoriteSongIds } == true,
                onToggleFavorite = { favoritesViewModel.toggleFavorite(it) },
                onOpenAudioSettings = {
                    showNowPlaying = false
                    selectedTab = MainTab.SETTINGS
                },
                modifier = screenModifier
            )
        } else if (activeSongDetailsId != null) {
            SongDetailsScreen(
                songId = activeSongDetailsId,
                viewModel = songDetailsViewModel,
                onBackClick = { selectedSongDetailsId = null },
                onPlaySong = { song -> playbackViewModel.playSong(song, listOf(song)) },
                onAddToQueue = { song -> playbackViewModel.addSongsToQueue(listOf(song)) },
                onAddToPlaylist = { song -> songForPlaylistDialog = song },
                onFindDuplicates = {
                    selectedSongDetailsId = null
                    showDuplicateScreen = true
                },
                modifier = screenModifier
            )
        } else if (showDuplicateScreen) {
            DuplicateDetectionScreen(
                viewModel = duplicateDetectionViewModel,
                onBackClick = { showDuplicateScreen = false },
                onPlaySong = { song -> playbackViewModel.playSong(song, listOf(song)) },
                modifier = screenModifier
            )
        } else if (activeAlbumId != null) {
            AlbumDetailScreen(
                albumId = activeAlbumId,
                viewModel = albumDetailViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { selectedAlbumId = null },
                onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                modifier = screenModifier
            )
        } else if (activeArtistId != null) {
            ArtistDetailScreen(
                artistId = activeArtistId,
                viewModel = artistDetailViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { selectedArtistId = null },
                onAlbumClick = { albumId -> selectedAlbumId = albumId },
                onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                modifier = screenModifier
            )
        } else if (activeGenreId != null) {
            GenreDetailScreen(
                genreId = activeGenreId,
                viewModel = genreDetailViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { selectedGenreId = null },
                onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                modifier = screenModifier
            )
        } else if (showRecommendationDetail) {
            RecommendationDetailScreen(
                viewModel = recommendationViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { showRecommendationDetail = false },
                onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                modifier = screenModifier
            )
        } else if (showSearchScreen) {
            SearchScreen(
                viewModel = searchViewModel,
                playbackViewModel = playbackViewModel,
                onBackClick = { showSearchScreen = false },
                onAlbumClick = { albumId -> selectedAlbumId = albumId },
                onArtistClick = { artistId -> selectedArtistId = artistId },
                onGenreClick = { genreId -> selectedGenreId = genreId },
                onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                modifier = screenModifier
            )
        } else if (activeSmartPlaylistType != null) {
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
                        recommendationViewModel = recommendationViewModel,
                        playlists = playlists,
                        onCreatePlaylistAndAdd = { name, songId ->
                            playlistsViewModel.createPlaylist(name) { newId ->
                                songsViewModel.addSongToPlaylist(newId, songId)
                            }
                        },
                        onSongDetailsClick = { selectedSongDetailsId = it.id },
                        onCheckDuplicatesClick = { showDuplicateScreen = true },
                        onSearchClick = { showSearchScreen = true },
                        onAlbumClick = { albumId -> selectedAlbumId = albumId },
                        onArtistClick = { artistId -> selectedArtistId = artistId },
                        onGenreClick = { genreId -> selectedGenreId = genreId },
                        onRecommendationSectionClick = { showRecommendationDetail = true },
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
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        audioViewModel = audioSettingsViewModel,
                        modifier = screenModifier
                    )
                }
            }
        }

        songForPlaylistDialog?.let { song ->
            com.auralis.app.ui.library.components.AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onDismiss = { songForPlaylistDialog = null },
                onAddToPlaylist = { playlistId ->
                    songsViewModel.addSongToPlaylist(playlistId, song.id)
                    songForPlaylistDialog = null
                },
                onCreateAndAdd = { name ->
                    playlistsViewModel.createPlaylist(name) { newId ->
                        songsViewModel.addSongToPlaylist(newId, song.id)
                    }
                    songForPlaylistDialog = null
                }
            )
        }
    }
}
