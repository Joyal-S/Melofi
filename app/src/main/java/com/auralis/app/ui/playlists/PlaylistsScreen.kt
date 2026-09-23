package com.auralis.app.ui.playlists

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyDimensions
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.SmartPlaylistType
import com.auralis.app.ui.components.ArtworkCustomizationDialog
import com.auralis.app.ui.components.PlaylistCover
import com.auralis.app.ui.playback.PlaybackViewModel

/**
 * Screen displaying the user's playlists alongside local rule-based smart playlists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlistsViewModel: PlaylistsViewModel,
    onPlaylistClick: (Long) -> Unit,
    onSmartPlaylistClick: ((SmartPlaylistType) -> Unit)? = null,
    playbackViewModel: PlaybackViewModel? = null,
    modifier: Modifier = Modifier
) {
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()
    val playlistArtworkMap by playlistsViewModel.playlistArtworkMap.collectAsStateWithLifecycle()
    val actionError by playlistsViewModel.actionError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistForArtwork by remember { mutableStateOf<Playlist?>(null) }

    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            playlistsViewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Create playlist"
                        )
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
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 1. User Playlists Section (Primary visual focus)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = if (playlists.isNotEmpty()) " (${playlists.size})" else ""
                    Text(
                        text = "YOUR PLAYLISTS$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "New custom playlist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (playlists.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No custom playlists yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showCreateDialog = true }) {
                                Text("Create Playlist")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = playlists,
                    key = { it.id }
                ) { playlist ->
                    PlaylistItemCard(
                        playlist = playlist,
                        coverUri = playlist.customCoverUri ?: playlistsViewModel.getCustomArtworkUri(playlist.id),
                        memberArtUris = playlistArtworkMap[playlist.id].orEmpty(),
                        defaultCoverUri = playlistsViewModel.getDefaultPlaylistCoverUri(playlist.id),
                        onClick = { onPlaylistClick(playlist.id) },
                        onPlay = {
                            playlistsViewModel.playPlaylist(playlist.id, shuffle = false) { songs ->
                                playbackViewModel?.playSong(songs.first(), songs)
                            }
                        },
                        onShuffle = {
                            playlistsViewModel.playPlaylist(playlist.id, shuffle = true) { songs ->
                                playbackViewModel?.playSong(songs.first(), songs)
                            }
                        },
                        onRename = { playlistToRename = playlist },
                        onChangeCover = { playlistForArtwork = playlist },
                        onDelete = { playlistToDelete = playlist }
                    )
                }
            }

            // 2. Smart Playlists Section (Secondary, Favorites removed with zero gap)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SMART PLAYLISTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(SmartPlaylistType.entries.filter { it != SmartPlaylistType.FAVORITES }) { type ->
                SmartPlaylistItemRow(
                    type = type,
                    onClick = { onSmartPlaylistClick?.invoke(type) }
                )
            }
        }
    }

    // Custom Artwork Dialog
    playlistForArtwork?.let { playlist ->
        ArtworkCustomizationDialog(
            entityType = ArtworkEntityType.PLAYLIST,
            entityId = playlist.id.toString(),
            entityName = playlist.name,
            currentArtworkUri = playlistsViewModel.getPlaylistArtworkUri(playlist),
            onDismiss = { playlistForArtwork = null },
            onSave = { uri ->
                playlistsViewModel.setCustomArtwork(playlist.id, uri)
                playlistForArtwork = null
            },
            onReset = {
                playlistsViewModel.clearCustomArtwork(playlist.id)
                playlistForArtwork = null
            }
        )
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "New Playlist",
            confirmButtonText = "Create",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                playlistsViewModel.createPlaylist(name) {
                    showCreateDialog = false
                }
            }
        )
    }

    // Rename Playlist Dialog
    playlistToRename?.let { playlist ->
        PlaylistNameDialog(
            title = "Rename Playlist",
            confirmButtonText = "Save",
            initialName = playlist.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                playlistsViewModel.renamePlaylist(playlist.id, newName) {
                    playlistToRename = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist") },
            text = {
                Text("Delete \"${playlist.name}\"? Tracks in your device library will not be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        playlistsViewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlaylistItemCard(
    playlist: Playlist,
    coverUri: String?,
    memberArtUris: List<String> = emptyList(),
    defaultCoverUri: String? = null,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    val animationMode = LocalAnimationMode.current
    var showMenu by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.985f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "playlistCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelofySpacing.lg, vertical = 6.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistCover(
                customCoverUri = coverUri,
                memberArtUris = memberArtUris,
                fallbackDefaultCoverUri = defaultCoverUri,
                title = playlist.name,
                modifier = Modifier.size(68.dp),
                cornerRadius = 10.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        if (!playlist.description.isNullOrBlank()) {
                            append("${playlist.description} • ")
                        }
                        append("${playlist.songCount} ${if (playlist.songCount == 1) "song" else "songs"}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(MelofyDimensions.minTouchTarget)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "Playlist options"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onPlay()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Shuffle") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.ic_shuffle), contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onShuffle()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Change Cover") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.ic_image), contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onChangeCover()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    confirmButtonText: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text("Playlist Name") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        error = "Playlist name cannot be empty"
                    } else if (trimmed.length > 100) {
                        error = "Name cannot exceed 100 characters"
                    } else {
                        onConfirm(trimmed)
                    }
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SmartPlaylistItemRow(
    type: SmartPlaylistType,
    onClick: () -> Unit
) {
    val animationMode = LocalAnimationMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.985f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "smartPlaylistScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelofySpacing.lg, vertical = 4.dp)
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(type.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MelofySpacing.md)
            ) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
