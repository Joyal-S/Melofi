package com.auralis.app.ui.library

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.util.PermissionUtils
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.ui.library.components.AddToPlaylistDialog
import com.auralis.app.ui.library.components.SongListItem
import com.auralis.app.ui.playback.PlaybackViewModel
import com.auralis.app.ui.playback.components.PlaybackControlsBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
    playlists: List<Playlist> = emptyList(),
    onCreatePlaylistAndAdd: ((String, Long) -> Unit)? = null,
    onSongDetailsClick: ((Song) -> Unit)? = null,
    onCheckDuplicatesClick: (() -> Unit)? = null,
    showBottomBar: Boolean = true
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    val requiredPermission = remember { PermissionUtils.getRequiredStoragePermission() }

    // Launcher for runtime media permissions
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

    // Check permission state on composition
    LaunchedEffect(Unit) {
        val hasPermission = PermissionUtils.hasStoragePermission(context)
        viewModel.onPermissionStatus(granted = hasPermission)
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            androidx.compose.material3.TextField(
                                value = filterState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search songs, artists, albums...") },
                                singleLine = true,
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                viewModel.setSearchQuery("")
                                isSearchActive = false
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = "Close Search"
                                )
                            }
                        },
                        actions = {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = "Clear Search"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            val subtitle = when (val state = uiState) {
                                is SongsUiState.Success -> " (${state.songs.size})"
                                else -> ""
                            }
                            Text(
                                text = stringResource(R.string.nav_songs) + subtitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            // Search action
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Search library"
                                )
                            }

                            // Rescan action
                            IconButton(onClick = { viewModel.scanLibrary() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_refresh),
                                    contentDescription = stringResource(R.string.action_rescan)
                                )
                            }

                            // Sort options menu
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more_vert),
                                        contentDescription = stringResource(R.string.action_sort)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_title)) },
                                        onClick = {
                                            viewModel.setSortOrder(SongSortOrder.TITLE)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_artist)) },
                                        onClick = {
                                            viewModel.setSortOrder(SongSortOrder.ARTIST)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_album)) },
                                        onClick = {
                                            viewModel.setSortOrder(SongSortOrder.ALBUM)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_date_added)) },
                                        onClick = {
                                            viewModel.setSortOrder(SongSortOrder.DATE_ADDED)
                                            showSortMenu = false
                                        }
                                    )
                                    androidx.compose.material3.HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Check for Duplicates") },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_file_copy),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showSortMenu = false
                                            onCheckDuplicatesClick?.invoke()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Library Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.ALL,
                            onClick = { viewModel.setFormatFilter(com.auralis.app.domain.model.FormatFilter.ALL) },
                            label = { Text("All Formats") }
                        )
                    }
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.LOSSLESS,
                            onClick = {
                                val next = if (filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.LOSSLESS)
                                    com.auralis.app.domain.model.FormatFilter.ALL
                                else
                                    com.auralis.app.domain.model.FormatFilter.LOSSLESS
                                viewModel.setFormatFilter(next)
                            },
                            label = { Text("Lossless") }
                        )
                    }
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.FLAC,
                            onClick = {
                                val next = if (filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.FLAC)
                                    com.auralis.app.domain.model.FormatFilter.ALL
                                else
                                    com.auralis.app.domain.model.FormatFilter.FLAC
                                viewModel.setFormatFilter(next)
                            },
                            label = { Text("FLAC") }
                        )
                    }
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.MP3,
                            onClick = {
                                val next = if (filterState.formatFilter == com.auralis.app.domain.model.FormatFilter.MP3)
                                    com.auralis.app.domain.model.FormatFilter.ALL
                                else
                                    com.auralis.app.domain.model.FormatFilter.MP3
                                viewModel.setFormatFilter(next)
                            },
                            label = { Text("MP3") }
                        )
                    }
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = filterState.bitrateFilter == com.auralis.app.domain.model.BitrateFilter.HIGH_QUALITY,
                            onClick = {
                                val next = if (filterState.bitrateFilter == com.auralis.app.domain.model.BitrateFilter.HIGH_QUALITY)
                                    com.auralis.app.domain.model.BitrateFilter.ALL
                                else
                                    com.auralis.app.domain.model.BitrateFilter.HIGH_QUALITY
                                viewModel.setBitrateFilter(next)
                            },
                            label = { Text("≥ 320 kbps") }
                        )
                    }
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = filterState.favoritesOnly,
                            onClick = { viewModel.setFavoritesOnly(!filterState.favoritesOnly) },
                            label = { Text("Favorites") }
                        )
                    }
                    if (!filterState.isDefault) {
                        item {
                            androidx.compose.material3.AssistChip(
                                onClick = { viewModel.resetFilters() },
                                label = { Text("Reset Filters") }
                            )
                        }
                    }
                }

                if (uiState is SongsUiState.Success && (uiState as SongsUiState.Success).isScanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            if (showBottomBar && playbackState.currentSong != null) {
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SongsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SongsUiState.PermissionRequired -> {
                    PermissionCard(
                        isPermanentlyDenied = state.isPermanentlyDenied,
                        onRequestPermission = { permissionLauncher.launch(requiredPermission) },
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                is SongsUiState.Empty -> {
                    EmptySongsView(
                        onScanClick = { viewModel.scanLibrary() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SongsUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.scanLibrary() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SongsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (recentlyPlayed.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Text(
                                        text = "RECENTLY PLAYED",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(recentlyPlayed, key = { "recent_${it.id}" }) { song ->
                                            RecentlyPlayedCard(
                                                song = song,
                                                onClick = { playbackViewModel.playSong(song, state.songs) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "ALL SONGS",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        items(
                            items = state.songs,
                            key = { it.id }
                        ) { song ->
                            SongListItem(
                                song = song,
                                onClick = {
                                    playbackViewModel.playSong(song, state.songs)
                                },
                                isFavorite = favoriteSongIds.contains(song.id),
                                onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                                onAddToPlaylist = { songForPlaylistDialog = song },
                                onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) },
                                onShowDetails = { onSongDetailsClick?.invoke(song) }
                            )
                        }
                    }
                }
            }
        }
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
private fun PermissionCard(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_placeholder),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permission_storage_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPermanentlyDenied) {
                    stringResource(R.string.permission_denied_permanent_description)
                } else {
                    stringResource(R.string.permission_storage_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (isPermanentlyDenied) {
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            } else {
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.permission_grant_button))
                }
            }
        }
    }
}

@Composable
private fun EmptySongsView(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_placeholder),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_songs_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_songs_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onScanClick) {
            Text(stringResource(R.string.action_scan))
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading library",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun RecentlyPlayedCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_music_placeholder),
                error = painterResource(R.drawable.ic_music_placeholder),
                fallback = painterResource(R.drawable.ic_music_placeholder)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artistName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

