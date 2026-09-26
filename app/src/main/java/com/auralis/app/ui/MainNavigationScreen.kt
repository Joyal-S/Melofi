package com.auralis.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.domain.model.SmartPlaylistType
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.browse.AlbumDetailScreen
import com.auralis.app.ui.browse.AlbumDetailViewModel
import com.auralis.app.ui.browse.ArtistDetailScreen
import com.auralis.app.ui.browse.ArtistDetailViewModel
import com.auralis.app.ui.browse.ArtistsScreen
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
    ARTISTS,
    SETTINGS
}

private sealed interface ScreenDestination {
    data class Main(val tab: MainTab) : ScreenDestination
    data object NowPlaying : ScreenDestination
    data class SongDetails(val id: Long) : ScreenDestination
    data object Duplicates : ScreenDestination
    data class Album(val id: Long) : ScreenDestination
    data class Artist(val id: Long) : ScreenDestination
    data class Genre(val id: Long) : ScreenDestination
    data object Recommendations : ScreenDestination
    data object Search : ScreenDestination
    data class SmartPlaylist(val type: SmartPlaylistType) : ScreenDestination
    data class Playlist(val id: Long) : ScreenDestination
    data object Statistics : ScreenDestination
}

/**
 * Top-level navigation host coordinating library tabs, playlist details,
 * smart playlists, metadata inspection, duplicates,
 * advanced search, category browsing (albums, artists, genres),
 * on-device recommendations, settings, listening statistics, and the persistent Media3 playback controls bar.
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
    val animationMode = LocalAnimationMode.current

    var selectedTab by remember { mutableStateOf(MainTab.SONGS) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedSmartPlaylistType by remember { mutableStateOf<SmartPlaylistType?>(null) }
    var selectedSongDetailsId by remember { mutableStateOf<Long?>(null) }
    var showDuplicateScreen by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var showRecommendationDetail by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showStatisticsScreen by remember { mutableStateOf(false) }
    var selectedAlbumId by remember { mutableStateOf<Long?>(null) }
    var selectedArtistId by remember { mutableStateOf<Long?>(null) }
    var selectedGenreId by remember { mutableStateOf<Long?>(null) }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongIds by songsViewModel.favoriteSongIds.collectAsStateWithLifecycle()

    val isSubScreen = showNowPlaying ||
            showStatisticsScreen ||
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
            showStatisticsScreen -> showStatisticsScreen = false
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

    val currentDestination: ScreenDestination = remember(
        showNowPlaying,
        showStatisticsScreen,
        selectedSongDetailsId,
        showDuplicateScreen,
        selectedAlbumId,
        selectedArtistId,
        selectedGenreId,
        showRecommendationDetail,
        showSearchScreen,
        selectedSmartPlaylistType,
        selectedPlaylistId,
        selectedTab
    ) {
        when {
            showNowPlaying -> ScreenDestination.NowPlaying
            showStatisticsScreen -> ScreenDestination.Statistics
            selectedSongDetailsId != null -> ScreenDestination.SongDetails(selectedSongDetailsId!!)
            showDuplicateScreen -> ScreenDestination.Duplicates
            selectedAlbumId != null -> ScreenDestination.Album(selectedAlbumId!!)
            selectedArtistId != null -> ScreenDestination.Artist(selectedArtistId!!)
            selectedGenreId != null -> ScreenDestination.Genre(selectedGenreId!!)
            showRecommendationDetail -> ScreenDestination.Recommendations
            showSearchScreen -> ScreenDestination.Search
            selectedSmartPlaylistType != null -> ScreenDestination.SmartPlaylist(selectedSmartPlaylistType!!)
            selectedPlaylistId != null -> ScreenDestination.Playlist(selectedPlaylistId!!)
            else -> ScreenDestination.Main(selectedTab)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            com.auralis.app.ui.components.MelofiBottomDock(
                playbackState = playbackState,
                showMiniPlayer = playbackState.currentSong != null && !showNowPlaying,
                showBottomNav = !isSubScreen && !showNowPlaying,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                favoriteSongIds = favoriteSongIds,
                onTogglePlayPause = { playbackViewModel.togglePlayPause() },
                onSeekTo = { playbackViewModel.seekTo(it) },
                onNext = { playbackViewModel.next() },
                onPrevious = { playbackViewModel.previous() },
                onToggleShuffle = { playbackViewModel.toggleShuffle() },
                onCycleRepeat = { playbackViewModel.cycleRepeatMode() },
                onMiniPlayerClick = { showNowPlaying = true },
                onToggleFavorite = { song -> favoritesViewModel.toggleFavorite(song.id) },
                progressProvider = { playbackViewModel.playbackProgress.value },
                animationMode = animationMode
            )
        }
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())

        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                if (animationMode == MelofyAnimationMode.OFF) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else if (animationMode == MelofyAnimationMode.REDUCED) {
                    fadeIn(MelofyMotion.adaptiveTween(animationMode, MelofyMotion.DURATION_FAST)) togetherWith
                            fadeOut(MelofyMotion.adaptiveTween(animationMode, MelofyMotion.DURATION_FAST))
                } else {
                    val isEnteringNowPlaying = targetState is ScreenDestination.NowPlaying
                    val isExitingNowPlaying = initialState is ScreenDestination.NowPlaying
                    val isTargetSubScreen = targetState !is ScreenDestination.Main && !isEnteringNowPlaying
                    val isInitialSubScreen = initialState !is ScreenDestination.Main && !isExitingNowPlaying

                    when {
                        isEnteringNowPlaying -> {
                            (slideInVertically(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_NORMAL,
                                    easing = MelofyMotion.emphasizeEasing
                                )
                            ) { it / 6 } +
                            scaleIn(
                                initialScale = 0.94f,
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_NORMAL,
                                    easing = MelofyMotion.emphasizeEasing
                                )
                            ) +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_FAST,
                                    easing = MelofyMotion.standardEasing
                                )
                            )).togetherWith(
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                )
                            )
                        }
                        isExitingNowPlaying -> {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_FAST,
                                    easing = MelofyMotion.standardEasing
                                )
                            ).togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                ) { it / 6 } +
                                scaleOut(
                                    targetScale = 0.94f,
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                ) +
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                )
                            )
                        }
                        isTargetSubScreen && !isInitialSubScreen -> {
                            (slideInHorizontally(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_NORMAL,
                                    easing = MelofyMotion.emphasizeEasing
                                )
                            ) { it / 5 } +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_FAST,
                                    easing = MelofyMotion.standardEasing
                                )
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                ) { -it / 5 } +
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                )
                            )
                        }
                        !isTargetSubScreen && isInitialSubScreen -> {
                            (slideInHorizontally(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_NORMAL,
                                    easing = MelofyMotion.emphasizeEasing
                                )
                            ) { -it / 5 } +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = MelofyMotion.DURATION_FAST,
                                    easing = MelofyMotion.standardEasing
                                )
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                ) { it / 5 } +
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = MelofyMotion.DURATION_FAST,
                                        easing = MelofyMotion.standardEasing
                                    )
                                )
                            )
                        }
                        else -> {
                            fadeIn(MelofyMotion.adaptiveTween(animationMode, MelofyMotion.DURATION_FAST))
                                .togetherWith(fadeOut(MelofyMotion.adaptiveTween(animationMode, MelofyMotion.DURATION_FAST)))
                        }
                    }
                }
            },
            label = "screenTransition",
            modifier = Modifier.fillMaxSize()
        ) { destination ->
            when (destination) {
                is ScreenDestination.NowPlaying -> {
                    NowPlayingScreen(
                        playbackViewModel = playbackViewModel,
                        onCollapse = { showNowPlaying = false },
                        isFavorite = playbackState.currentSong?.id?.let { it in favoriteSongIds } == true,
                        onToggleFavorite = { favoritesViewModel.toggleFavorite(it) },
                        onOpenAudioSettings = {
                            showNowPlaying = false
                            selectedTab = MainTab.SETTINGS
                        },
                        onAddToPlaylist = { song ->
                            songForPlaylistDialog = song
                        },
                        onSongDetailsClick = { song ->
                            showNowPlaying = false
                            selectedSongDetailsId = song.id
                        },
                        onArtistClick = { artistId, artistName ->
                            showNowPlaying = false
                            val resolvedId = artistId ?: songsViewModel.artists.value.firstOrNull {
                                it.name.equals(artistName, ignoreCase = true)
                            }?.id
                            if (resolvedId != null) {
                                selectedArtistId = resolvedId
                            } else {
                                selectedTab = MainTab.ARTISTS
                            }
                        },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.SongDetails -> {
                    SongDetailsScreen(
                        songId = destination.id,
                        viewModel = songDetailsViewModel,
                        onBackClick = { selectedSongDetailsId = null },
                        onPlaySong = { song -> playbackViewModel.playSong(song, listOf(song)) },
                        onAddToQueue = { song -> playbackViewModel.addSongsToQueue(listOf(song)) },
                        onAddToPlaylist = { song -> songForPlaylistDialog = song },
                        onFindDuplicates = {
                            selectedSongDetailsId = null
                            showDuplicateScreen = true
                        },
                        onArtistClick = { artistId, artistName ->
                            selectedSongDetailsId = null
                            val resolvedId = artistId ?: songsViewModel.artists.value.firstOrNull {
                                it.name.equals(artistName, ignoreCase = true)
                            }?.id
                            if (resolvedId != null) {
                                selectedArtistId = resolvedId
                            } else {
                                selectedTab = MainTab.ARTISTS
                            }
                        },
                        onAlbumClick = { albumName ->
                            val album = songsViewModel.albums.value.firstOrNull {
                                it.name.equals(albumName, ignoreCase = true)
                            }
                            if (album != null) {
                                selectedSongDetailsId = null
                                selectedAlbumId = album.id
                            }
                        },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Duplicates -> {
                    DuplicateDetectionScreen(
                        viewModel = duplicateDetectionViewModel,
                        onBackClick = { showDuplicateScreen = false },
                        onPlaySong = { song -> playbackViewModel.playSong(song, listOf(song)) },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Album -> {
                    AlbumDetailScreen(
                        albumId = destination.id,
                        viewModel = albumDetailViewModel,
                        playbackViewModel = playbackViewModel,
                        onBackClick = { selectedAlbumId = null },
                        onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                        onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                        onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Artist -> {
                    ArtistDetailScreen(
                        artistId = destination.id,
                        viewModel = artistDetailViewModel,
                        playbackViewModel = playbackViewModel,
                        onBackClick = { selectedArtistId = null },
                        onAlbumClick = { albumId -> selectedAlbumId = albumId },
                        onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                        onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                        onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Genre -> {
                    GenreDetailScreen(
                        genreId = destination.id,
                        viewModel = genreDetailViewModel,
                        playbackViewModel = playbackViewModel,
                        onBackClick = { selectedGenreId = null },
                        onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                        onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                        onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Recommendations -> {
                    RecommendationDetailScreen(
                        viewModel = recommendationViewModel,
                        playbackViewModel = playbackViewModel,
                        onBackClick = { showRecommendationDetail = false },
                        onSongClick = { song, list -> playbackViewModel.playSong(song, list) },
                        onSongDetailsClick = { song -> selectedSongDetailsId = song.id },
                        onAddToPlaylistClick = { song -> songForPlaylistDialog = song },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Search -> {
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
                }
                is ScreenDestination.SmartPlaylist -> {
                    SmartPlaylistDetailScreen(
                        playlistType = destination.type,
                        viewModel = smartPlaylistDetailViewModel,
                        playbackViewModel = playbackViewModel,
                        onBackClick = { selectedSmartPlaylistType = null },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Playlist -> {
                    PlaylistDetailScreen(
                        playlistId = destination.id,
                        viewModel = playlistDetailViewModel,
                        playbackViewModel = playbackViewModel,
                        onBackClick = { selectedPlaylistId = null },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Statistics -> {
                    StatisticsScreen(
                        onBackClick = { showStatisticsScreen = false },
                        modifier = screenModifier
                    )
                }
                is ScreenDestination.Main -> {
                    when (destination.tab) {
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
                                playbackViewModel = playbackViewModel,
                                modifier = screenModifier
                            )
                        }
                        MainTab.ARTISTS -> {
                            ArtistsScreen(
                                viewModel = songsViewModel,
                                onArtistClick = { artistId -> selectedArtistId = artistId },
                                onSearchClick = { showSearchScreen = true },
                                modifier = screenModifier
                            )
                        }
                        MainTab.SETTINGS -> {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                audioViewModel = audioSettingsViewModel,
                                onNavigateToStatistics = { showStatisticsScreen = true },
                                modifier = screenModifier
                            )
                        }
                    }
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

