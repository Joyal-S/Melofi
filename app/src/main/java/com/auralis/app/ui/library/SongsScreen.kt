package com.auralis.app.ui.library

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.ui.library.components.EmptyStateView
import com.auralis.app.ui.library.components.SongGridItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.util.PermissionUtils
import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.PlaybackFilter
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.QualityFilter
import com.auralis.app.domain.model.RecommendationSectionType
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.ui.components.ArtworkCustomizationDialog
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.ui.library.components.AddToPlaylistDialog
import com.auralis.app.ui.library.components.FilterBottomSheet
import com.auralis.app.ui.library.components.SongListItem
import com.auralis.app.ui.library.importmusic.ImportMusicDialog
import com.auralis.app.ui.playback.PlaybackViewModel
import com.auralis.app.ui.recommendations.RecommendationSectionRow
import com.auralis.app.ui.recommendations.RecommendationUiState
import com.auralis.app.ui.recommendations.RecommendationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    playbackViewModel: PlaybackViewModel,
    recommendationViewModel: RecommendationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    playlists: List<Playlist> = emptyList(),
    onCreatePlaylistAndAdd: ((String, Long) -> Unit)? = null,
    onSongDetailsClick: ((Song) -> Unit)? = null,
    onCheckDuplicatesClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onAlbumClick: ((Long) -> Unit)? = null,
    onArtistClick: ((Long) -> Unit)? = null,
    onGenreClick: ((Long) -> Unit)? = null,
    onRecommendationSectionClick: ((RecommendationSectionType) -> Unit)? = null,
    showBottomBar: Boolean = true
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()

    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()

    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    var songForArtworkDialog by remember { mutableStateOf<Song?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val requiredPermission = remember { PermissionUtils.getRequiredStoragePermission() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val activity = context as? Activity
        val permanentlyDenied = if (!isGranted && activity != null) {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, requiredPermission)
        } else {
            false
        }
        viewModel.onPermissionStatus(granted = isGranted, permanentlyDenied = permanentlyDenied)
    }

    LaunchedEffect(Unit) {
        val hasPermission = PermissionUtils.hasStoragePermission(context)
        viewModel.onPermissionStatus(granted = hasPermission)
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            sheetState = sheetState,
            onDismiss = { showFilterSheet = false },
            onApply = { viewModel.setFilterState(it) },
            onClearAll = { viewModel.resetFilters() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Melofi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        // Add Music Button
                        IconButton(onClick = { showImportDialog = true }) {
                            Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Add Music")
                        }

                        // Search Button
                        IconButton(onClick = { onSearchClick?.invoke() }) {
                            Icon(painter = painterResource(R.drawable.ic_search), contentDescription = "Search Library")
                        }

                        // Rescan Button
                        IconButton(onClick = { viewModel.scanLibrary() }) {
                            Icon(painter = painterResource(R.drawable.ic_refresh), contentDescription = "Scan Library")
                        }

                        // Duplicate Detection Button
                        IconButton(onClick = { onCheckDuplicatesClick?.invoke() }) {
                            Icon(painter = painterResource(R.drawable.ic_file_copy), contentDescription = "Check Duplicates")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Browsing Tabs (Tracks, Albums, Artists, Genres) - Smooth scrollable row
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    LibraryTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { viewModel.setLibraryTab(tab) },
                            text = {
                                Text(
                                    tab.displayName,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val animationMode = LocalAnimationMode.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (animationMode == MelofyAnimationMode.OFF) {
                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    } else if (animationMode == MelofyAnimationMode.REDUCED) {
                        fadeIn(MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_FAST)) togetherWith
                                fadeOut(MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_FAST))
                    } else {
                        val direction = targetState.ordinal > initialState.ordinal
                        if (direction) {
                            (slideInHorizontally(MelofyMotion.springSubtle<IntOffset>()) { it / 3 } + fadeIn(MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_FAST)))
                                .togetherWith(slideOutHorizontally(MelofyMotion.springSubtle<IntOffset>()) { -it / 3 } + fadeOut(MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_FAST)))
                        } else {
                            (slideInHorizontally(MelofyMotion.springSubtle<IntOffset>()) { -it / 3 } + fadeIn(MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_FAST)))
                                .togetherWith(slideOutHorizontally(MelofyMotion.springSubtle<IntOffset>()) { it / 3 } + fadeOut(MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_FAST)))
                        }
                    }
                },
                label = "libraryTabTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentTab ->
                when (currentTab) {
                    LibraryTab.FOR_YOU -> {
                        RecommendationsTabContent(
                            recommendationViewModel = recommendationViewModel,
                            playbackViewModel = playbackViewModel,
                            onRecommendationSectionClick = onRecommendationSectionClick,
                            onSongDetailsClick = onSongDetailsClick,
                            onAddToPlaylistClick = { songForPlaylistDialog = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    LibraryTab.TRACKS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Tracks Controls Bar (Filters button with badge, Sort button, ViewMode switcher)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Filter Sheet Launcher Button with Badge
                                BadgedBox(
                                    badge = {
                                        if (filterState.activeFilterCount > 0) {
                                            Badge { Text("${filterState.activeFilterCount}") }
                                        }
                                    }
                                ) {
                                    OutlinedButton(
                                        onClick = { showFilterSheet = true }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_tune),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (filterState.activeFilterCount > 0)
                                                "Filters (${filterState.activeFilterCount})"
                                            else
                                                "Filters"
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Sort Dropdown
                                    Box {
                                        OutlinedButton(onClick = { showSortMenu = true }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_insights),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sort")
                                        }

                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false }
                                        ) {
                                            SongSortOrder.entries.forEach { order ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = order.displayName,
                                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.setSortOrder(order)
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // View Mode Toggle
                                    IconButton(
                                        onClick = {
                                            val nextMode = when (viewMode) {
                                                LibraryViewMode.LIST -> LibraryViewMode.COMPACT
                                                LibraryViewMode.COMPACT -> LibraryViewMode.GRID
                                                LibraryViewMode.GRID -> LibraryViewMode.LIST
                                            }
                                            viewModel.setViewMode(nextMode)
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                when (viewMode) {
                                                    LibraryViewMode.LIST -> R.drawable.ic_queue_music
                                                    LibraryViewMode.COMPACT -> R.drawable.ic_playlist_play
                                                    LibraryViewMode.GRID -> R.drawable.ic_music_placeholder
                                                }
                                            ),
                                            contentDescription = "Change view mode (${viewMode.displayName})"
                                        )
                                    }
                                }
                            }

                            // Smart Filter Chips Horizontal Scroll
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = filterState.isDefault,
                                        onClick = { viewModel.resetFilters() },
                                        label = { Text("All") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.playbackFilter == PlaybackFilter.FAVORITES || filterState.favoritesOnly,
                                        onClick = {
                                            val isSelected = filterState.playbackFilter == PlaybackFilter.FAVORITES || filterState.favoritesOnly
                                            viewModel.setPlaybackFilter(if (isSelected) PlaybackFilter.ALL else PlaybackFilter.FAVORITES)
                                            viewModel.setFavoritesOnly(!isSelected)
                                        },
                                        label = { Text("Favorites") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.playbackFilter == PlaybackFilter.RECENTLY_PLAYED,
                                        onClick = {
                                            val isSelected = filterState.playbackFilter == PlaybackFilter.RECENTLY_PLAYED
                                            viewModel.setPlaybackFilter(if (isSelected) PlaybackFilter.ALL else PlaybackFilter.RECENTLY_PLAYED)
                                        },
                                        label = { Text("Recently Played") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.playbackFilter == PlaybackFilter.NEVER_PLAYED,
                                        onClick = {
                                            val isSelected = filterState.playbackFilter == PlaybackFilter.NEVER_PLAYED
                                            viewModel.setPlaybackFilter(if (isSelected) PlaybackFilter.ALL else PlaybackFilter.NEVER_PLAYED)
                                        },
                                        label = { Text("Never Played") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.formatFilter == FormatFilter.LOSSLESS || filterState.qualityFilter == QualityFilter.LOSSLESS,
                                        onClick = {
                                            val isSelected = filterState.qualityFilter == QualityFilter.LOSSLESS
                                            viewModel.setQualityFilter(if (isSelected) QualityFilter.ALL else QualityFilter.LOSSLESS)
                                        },
                                        label = { Text("Lossless") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.formatFilter == FormatFilter.FLAC,
                                        onClick = {
                                            val isSelected = filterState.formatFilter == FormatFilter.FLAC
                                            viewModel.setFormatFilter(if (isSelected) FormatFilter.ALL else FormatFilter.FLAC)
                                        },
                                        label = { Text("FLAC") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.qualityFilter == QualityFilter.HIGH_QUALITY,
                                        onClick = {
                                            val isSelected = filterState.qualityFilter == QualityFilter.HIGH_QUALITY
                                            viewModel.setQualityFilter(if (isSelected) QualityFilter.ALL else QualityFilter.HIGH_QUALITY)
                                        },
                                        label = { Text("320+ kbps") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = filterState.durationFilter == DurationFilter.LONG,
                                        onClick = {
                                            val isSelected = filterState.durationFilter == DurationFilter.LONG
                                            viewModel.setDurationFilter(if (isSelected) DurationFilter.ALL else DurationFilter.LONG)
                                        },
                                        label = { Text("Long (> 6m)") }
                                    )
                                }
                            }

                            // Content State
                            when (val state = uiState) {
                                is SongsUiState.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                is SongsUiState.PermissionRequired -> {
                                    EmptyStateView(
                                        iconRes = R.drawable.ic_tune,
                                        title = "Storage Permission Required",
                                        description = "Melofi needs permission to discover audio files on your device. Everything remains 100% offline.",
                                        actionText = "Grant Permission",
                                        onAction = { permissionLauncher.launch(requiredPermission) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is SongsUiState.Error -> {
                                    EmptyStateView(
                                        iconRes = R.drawable.ic_refresh,
                                        title = "Scan Error",
                                        description = state.message,
                                        actionText = "Retry Scan",
                                        onAction = { viewModel.scanLibrary() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is SongsUiState.Empty -> {
                                    EmptyStateView(
                                        iconRes = R.drawable.ic_music_placeholder,
                                        title = if (filterState.isDefault) "No Music Found" else "No matching tracks",
                                        description = if (filterState.isDefault)
                                            "No audio files discovered on your device storage."
                                        else
                                            "Try changing or resetting your active filters.",
                                        actionText = if (filterState.isDefault) "Add Music" else "Reset Filters",
                                        onAction = {
                                            if (filterState.isDefault) showImportDialog = true else viewModel.resetFilters()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is SongsUiState.Success -> {
                                    val songs = state.songs

                                    if (viewMode == LibraryViewMode.GRID) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            contentPadding = PaddingValues(MelofySpacing.md),
                                            horizontalArrangement = Arrangement.spacedBy(MelofySpacing.md),
                                            verticalArrangement = Arrangement.spacedBy(MelofySpacing.md),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(songs, key = { it.id }) { song ->
                                                SongGridItem(
                                                    song = song,
                                                    onClick = { playbackViewModel.playSong(song, songs) },
                                                    isFavorite = song.id in favoriteSongIds,
                                                    onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                                                    onAddToPlaylist = { songForPlaylistDialog = song },
                                                    onPlay = { playbackViewModel.playSong(song, songs) },
                                                    onAddToQueue = { playbackViewModel.addSongsToQueue(listOf(song)) },
                                                    onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) },
                                                    onChangeArtwork = { songForArtworkDialog = song },
                                                    onDelete = { viewModel.deleteSong(song.id) },
                                                    onShowDetails = { onSongDetailsClick?.invoke(song) },
                                                    onHideFromLibrary = {
                                                        viewModel.hideSong(song) {
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("\"${song.title}\" hidden from Music Library")
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(songs, key = { it.id }) { song ->
                                                SongListItem(
                                                    song = song,
                                                    onClick = { playbackViewModel.playSong(song, songs) },
                                                    isFavorite = song.id in favoriteSongIds,
                                                    isCompact = viewMode == LibraryViewMode.COMPACT,
                                                    onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                                                    onAddToPlaylist = { songForPlaylistDialog = song },
                                                    onPlay = { playbackViewModel.playSong(song, songs) },
                                                    onAddToQueue = { playbackViewModel.addSongsToQueue(listOf(song)) },
                                                    onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) },
                                                    onChangeArtwork = { songForArtworkDialog = song },
                                                    onDelete = { viewModel.deleteSong(song.id) },
                                                    onShowDetails = { onSongDetailsClick?.invoke(song) },
                                                    onHideFromLibrary = {
                                                        viewModel.hideSong(song) {
                                                            coroutineScope.launch {
                                                                snackbarHostState.showSnackbar("\"${song.title}\" hidden from Music Library")
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.ALBUMS -> {
                        if (albums.isEmpty()) {
                            EmptyStateView(
                                iconRes = R.drawable.ic_music_placeholder,
                                title = "No Albums Found",
                                description = "Albums discovered from your audio files will appear here.",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(albums, key = { it.id }) { album ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAlbumClick?.invoke(album.id) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            MelofyArtwork(
                                                artworkUri = album.artUri,
                                                title = album.name,
                                                artist = album.artistName,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f),
                                                cornerRadius = 10.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = album.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = album.artistName ?: "Unknown Artist",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${album.songCount} tracks",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.ARTISTS -> {
                        if (artists.isEmpty()) {
                            EmptyStateView(
                                iconRes = R.drawable.ic_music_placeholder,
                                title = "No Artists Found",
                                description = "Artists discovered from your audio files will appear here.",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(artists, key = { it.id }) { artist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onArtistClick?.invoke(artist.id) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MelofyArtwork(
                                            artworkUri = artist.artUri,
                                            title = artist.name,
                                            entityType = ArtworkEntityType.ARTIST,
                                            modifier = Modifier.size(52.dp),
                                            cornerRadius = 26.dp
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${artist.songCount} songs • ${artist.albumCount} albums",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LibraryTab.GENRES -> {
                        if (genres.isEmpty()) {
                            EmptyStateView(
                                iconRes = R.drawable.ic_music_placeholder,
                                title = "No Genres Found",
                                description = "Genres tagged in your audio files will appear here.",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(genres, key = { it.id }) { genre ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onGenreClick?.invoke(genre.id) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            MelofyArtwork(
                                                artworkUri = genre.artUri,
                                                title = genre.name,
                                                entityType = ArtworkEntityType.GENRE,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1.2f),
                                                cornerRadius = 10.dp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = genre.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${genre.songCount} songs",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportMusicDialog(
            importManager = viewModel.importManager,
            onDismiss = { showImportDialog = false },
            onImportComplete = {
                showImportDialog = false
                viewModel.scanLibrary()
            }
        )
    }

    songForArtworkDialog?.let { song ->
        ArtworkCustomizationDialog(
            entityType = ArtworkEntityType.SONG,
            entityId = song.id.toString(),
            entityName = song.title,
            currentArtworkUri = song.albumArtUri,
            onDismiss = { songForArtworkDialog = null },
            onSave = { uri ->
                viewModel.setCustomArtwork(ArtworkEntityType.SONG, song.id.toString(), uri)
                songForArtworkDialog = null
            },
            onReset = {
                viewModel.clearCustomArtwork(ArtworkEntityType.SONG, song.id.toString())
                songForArtworkDialog = null
            }
        )
    }

    songForPlaylistDialog?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { songForPlaylistDialog = null },
            onAddToPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                songForPlaylistDialog = null
            },
            onCreateAndAdd = { name ->
                onCreatePlaylistAndAdd?.invoke(name, song.id)
                songForPlaylistDialog = null
            }
        )
    }
}

@Composable
private fun RecommendationsTabContent(
    recommendationViewModel: RecommendationViewModel,
    playbackViewModel: PlaybackViewModel,
    onRecommendationSectionClick: ((RecommendationSectionType) -> Unit)?,
    onSongDetailsClick: ((Song) -> Unit)?,
    onAddToPlaylistClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by recommendationViewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "For You",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "100% on-device & private",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { recommendationViewModel.refresh() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = "Refresh Recommendations"
                    )
                }
            }
        }

        when (val state = uiState) {
            is RecommendationUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is RecommendationUiState.Empty -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_music_placeholder),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Build your listening history",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recommendations will appear as you play songs and add favorites to your collection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            is RecommendationUiState.Success -> {
                items(state.sections, key = { it.type.id }) { section ->
                    RecommendationSectionRow(
                        section = section,
                        onItemClick = { item ->
                            playbackViewModel.playSong(item.song, section.items.map { it.song })
                        },
                        onItemPlayClick = { item ->
                            playbackViewModel.playSong(item.song, section.items.map { it.song })
                        },
                        onSeeAllClick = {
                            recommendationViewModel.selectSection(section.type)
                            onRecommendationSectionClick?.invoke(section.type)
                        },
                        onPlayAllClick = {
                            val songs = section.items.map { it.song }
                            if (songs.isNotEmpty()) {
                                playbackViewModel.playSong(songs.first(), songs)
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}
