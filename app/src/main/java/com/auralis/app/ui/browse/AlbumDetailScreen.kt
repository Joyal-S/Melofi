package com.auralis.app.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.R
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.library.components.SongListItem
import com.auralis.app.ui.playback.PlaybackViewModel

import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.ui.components.ArtworkCustomizationDialog

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofySpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: AlbumDetailViewModel,
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongDetailsClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    var showArtworkDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Album", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(painter = painterResource(R.drawable.ic_more_vert), contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Cover") },
                                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_image), contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showArtworkDialog = true
                                }
                            )
                            val songs = (uiState as? AlbumDetailUiState.Success)?.songs.orEmpty()
                            if (songs.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Play All") },
                                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_play_arrow), contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        playbackViewModel.playSong(songs.first(), songs)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Shuffle") },
                                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_shuffle), contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        playbackViewModel.playSong(songs.shuffled().first(), songs.shuffled())
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add to Queue") },
                                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_queue_music), contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        playbackViewModel.addSongsToQueue(songs)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is AlbumDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AlbumDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is AlbumDetailUiState.Success -> {
                val album = state.album
                val songs = state.songs

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Header
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.BottomEnd,
                                    modifier = Modifier.clickable { showArtworkDialog = true }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(190.dp)
                                            .shadow(
                                                elevation = MelofyElevation.high,
                                                shape = RoundedCornerShape(20.dp),
                                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            )
                                    )
                                    MelofyArtwork(
                                        artworkUri = album.artUri,
                                        title = album.name,
                                        artist = album.artistName,
                                        modifier = Modifier.size(190.dp),
                                        cornerRadius = 20.dp
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .padding(2.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_image),
                                                contentDescription = "Edit Album Cover",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(MelofySpacing.md))

                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(MelofySpacing.xxs))

                                Text(
                                    text = album.artistName ?: "Unknown Artist",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(MelofySpacing.xs))

                                val totalDurationMs = songs.sumOf { it.duration }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = buildString {
                                            if (album.year != null && album.year > 0) {
                                                append("${album.year} • ")
                                            }
                                            append("${songs.size} tracks")
                                            if (totalDurationMs > 0) {
                                                append(" • ${TimeUtils.formatDuration(totalDurationMs)}")
                                            }
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.xxs)
                                    )
                                }

                                Spacer(modifier = Modifier.height(MelofySpacing.lg))

                                // Playback Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (songs.isNotEmpty()) {
                                                playbackViewModel.playSong(songs.first(), songs)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(painter = painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            if (songs.isNotEmpty()) {
                                                playbackViewModel.playSong(songs.shuffled().first(), songs.shuffled())
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(painter = painterResource(R.drawable.ic_shuffle), contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Shuffle")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            playbackViewModel.addSongsToQueue(songs)
                                        }
                                    ) {
                                        Icon(painter = painterResource(R.drawable.ic_queue_music), contentDescription = "Add all to queue")
                                    }
                                }
                            }
                        }
                    }

                    // Song List
                    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        SongListItem(
                            song = song,
                            onClick = { onSongClick(song, songs) },
                            isFavorite = song.id in favoriteSongIds,
                            onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                            onAddToPlaylist = { onAddToPlaylistClick(song) },
                            onPlay = { onSongClick(song, songs) },
                            onAddToQueue = { playbackViewModel.addSongsToQueue(listOf(song)) },
                            onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) },
                            onShowDetails = { onSongDetailsClick(song) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showArtworkDialog) {
        val currentAlbum = (uiState as? AlbumDetailUiState.Success)?.album
        if (currentAlbum != null) {
            ArtworkCustomizationDialog(
                entityType = ArtworkEntityType.ALBUM,
                entityId = currentAlbum.id.toString(),
                entityName = currentAlbum.name,
                currentArtworkUri = currentAlbum.artUri,
                onDismiss = { showArtworkDialog = false },
                onSave = { uri ->
                    showArtworkDialog = false
                    viewModel.setCustomArtwork(uri)
                },
                onReset = {
                    showArtworkDialog = false
                    viewModel.clearCustomArtwork()
                }
            )
        }
    }
}
