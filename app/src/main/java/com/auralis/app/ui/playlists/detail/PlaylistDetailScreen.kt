package com.auralis.app.ui.playlists.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.ui.components.PlaylistCover
import com.auralis.app.ui.playback.PlaybackViewModel
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.ui.components.ArtworkCustomizationDialog

/**
 * Screen displaying the ordered songs in a specific playlist with playback,
 * custom cover selection, description editing, and reordering actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlaylistDetailViewModel,
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(playlistId) {
        viewModel.setPlaylistId(playlistId)
    }

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    var showEditDescriptionDialog by remember { mutableStateOf(false) }
    var showArtworkDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist?.name ?: "Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Cover") },
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.ic_image), contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showArtworkDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Description") },
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showEditDescriptionDialog = true
                                }
                            )
                            if (songs.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Add to Queue") },
                                    leadingIcon = {
                                        Icon(painter = painterResource(R.drawable.ic_queue_music), contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        playbackViewModel.addSongsToQueue(songs)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Playlist") },
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog = true
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Playlist Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PlaylistCover(
                            customCoverUri = playlist?.customCoverUri,
                            memberArtUris = songs.mapNotNull { it.albumArtUri }.take(4),
                            title = playlist?.name ?: "Playlist",
                            modifier = Modifier
                                .size(160.dp)
                                .clickable { showArtworkDialog = true },
                            cornerRadius = 16.dp
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { showArtworkDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = "Change cover",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = playlist?.name ?: "Playlist",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    if (!playlist?.description.isNullOrBlank()) {
                        Text(
                            text = playlist?.description.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }

                    val totalDurationMs = songs.sumOf { it.duration }
                    Text(
                        text = buildString {
                            append("${songs.size} ${if (songs.size == 1) "track" else "tracks"}")
                            if (totalDurationMs > 0) {
                                append(" • ${TimeUtils.formatDuration(totalDurationMs)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play & Shuffle controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    playbackViewModel.playSongs(songs, 0)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = songs.isNotEmpty()
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
                            modifier = Modifier.weight(1f),
                            enabled = songs.isNotEmpty()
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_shuffle), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle")
                        }
                    }
                }
            }

            if (songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_playlist_play),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Playlist is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add songs to this playlist from the Songs or Favorites screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    PlaylistSongItem(
                        index = index,
                        song = song,
                        totalCount = songs.size,
                        onClick = { playbackViewModel.playSong(song, songs) },
                        onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) },
                        onRemove = { viewModel.removeSong(song.id) },
                        onMoveUp = { if (index > 0) viewModel.moveSong(index, index - 1) },
                        onMoveDown = { if (index < songs.lastIndex) viewModel.moveSong(index, index + 1) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showEditDescriptionDialog) {
        var descriptionText by remember(playlist?.description) { mutableStateOf(playlist?.description.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showEditDescriptionDialog = false },
            title = { Text("Playlist Description") },
            text = {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description") },
                    placeholder = { Text("Add notes, mood, or context...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDescription(descriptionText.trim().ifEmpty { null })
                        showEditDescriptionDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDescriptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showArtworkDialog) {
        val currentPlaylist = playlist
        if (currentPlaylist != null) {
            ArtworkCustomizationDialog(
                entityType = ArtworkEntityType.PLAYLIST,
                entityId = currentPlaylist.id.toString(),
                entityName = currentPlaylist.name,
                currentArtworkUri = currentPlaylist.customCoverUri,
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

    if (showDeleteDialog) {
        val currentPlaylist = playlist
        if (currentPlaylist != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Playlist") },
                text = { Text("Delete \"${currentPlaylist.name}\"? Tracks in your device library will not be deleted.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deletePlaylist {
                                onBackClick()
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaylistSongItem(
    index: Int,
    song: Song,
    totalCount: Int,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track order position index label
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )

        // MelofyArtwork
        MelofyArtwork(
            artworkUri = song.albumArtUri,
            title = song.title,
            artist = song.artistName,
            modifier = Modifier.size(44.dp),
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and artist
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = TimeUtils.formatDuration(song.duration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp)
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "Song options",
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next") },
                    leadingIcon = {
                        Icon(painter = painterResource(R.drawable.ic_queue_music), contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onPlayNext()
                    }
                )
                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onMoveUp()
                        }
                    )
                }
                if (index < totalCount - 1) {
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.ic_expand_more), contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onMoveDown()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove from Playlist") },
                    leadingIcon = {
                        Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onRemove()
                    }
                )
            }
        }
    }
}
