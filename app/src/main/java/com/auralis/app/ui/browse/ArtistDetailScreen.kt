package com.auralis.app.ui.browse

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.components.MelofyArtwork
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
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.ui.theme.MelofyElevation
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    viewModel: ArtistDetailViewModel,
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongDetailsClick: (Song) -> Unit,
    onAddToPlaylistClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    var showArtworkDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Artist", fontWeight = FontWeight.Bold) },
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
                                text = { Text("Change Artist Image") },
                                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_image), contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showArtworkDialog = true
                                }
                            )
                            val songs = (uiState as? ArtistDetailUiState.Success)?.songs.orEmpty()
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
            is ArtistDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ArtistDetailUiState.Error -> {
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
            is ArtistDetailUiState.Success -> {
                val artist = state.artist
                val songs = state.songs
                val albums = state.albums
                val popularSongs = remember(songs) { songs.take(5) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 1. Immersive Hero Header
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
                                            .size(150.dp)
                                            .shadow(
                                                elevation = MelofyElevation.high,
                                                shape = CircleShape,
                                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            )
                                    )
                                    MelofyArtwork(
                                        artworkUri = artist.artUri,
                                        title = artist.name,
                                        entityType = ArtworkEntityType.ARTIST,
                                        modifier = Modifier.size(150.dp),
                                        cornerRadius = 75.dp
                                    )
                                    // Custom artwork badge indicator
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
                                                contentDescription = "Edit Artist Artwork",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(MelofySpacing.md))

                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(MelofySpacing.xxs))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${artist.songCount} songs • ${artist.albumCount} albums",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.xxs)
                                    )
                                }

                                Spacer(modifier = Modifier.height(MelofySpacing.lg))

                                // Playback Actions Row
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
                                        Icon(painter = painterResource(R.drawable.ic_queue_music), contentDescription = "Queue")
                                    }
                                }
                            }
                        }
                    }

                    // 2. Popular Tracks Section (Top 5 tracks with rank indicators)
                    if (popularSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Popular Tracks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = MelofySpacing.lg, end = MelofySpacing.lg, top = MelofySpacing.lg, bottom = MelofySpacing.xs)
                            )
                        }

                        itemsIndexed(popularSongs, key = { _, s -> "pop_${s.id}" }) { index, song ->
                            PopularTrackRow(
                                rank = index + 1,
                                song = song,
                                onClick = { onSongClick(song, songs) },
                                isFavorite = song.id in favoriteSongIds,
                                onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                                onAddToPlaylist = { onAddToPlaylistClick(song) },
                                onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) },
                                onShowDetails = { onSongDetailsClick(song) }
                            )
                        }
                    }

                    // 3. Albums Horizontal Carousel
                    if (albums.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = MelofySpacing.md, bottom = MelofySpacing.xs)) {
                                Text(
                                    text = "Albums (${albums.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.xs)
                                )

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = MelofySpacing.lg),
                                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.md)
                                ) {
                                    items(albums, key = { it.id }) { album ->
                                        Card(
                                            modifier = Modifier
                                                .width(136.dp)
                                                .clickable { onAlbumClick(album.id) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(MelofySpacing.sm)) {
                                                MelofyArtwork(
                                                    artworkUri = album.artUri,
                                                    title = album.name,
                                                    artist = artist.name,
                                                    modifier = Modifier.size(120.dp),
                                                    cornerRadius = 10.dp
                                                )

                                                Spacer(modifier = Modifier.height(MelofySpacing.xs))

                                                Text(
                                                    text = album.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = "${album.songCount} songs",
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

                    // 4. All Tracks Header & List
                    item {
                        Text(
                            text = "All Tracks (${songs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = MelofySpacing.lg, end = MelofySpacing.lg, top = MelofySpacing.lg, bottom = MelofySpacing.xs)
                        )
                    }

                    items(songs, key = { it.id }) { song ->
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
                }
            }
        }
    }

    if (showArtworkDialog) {
        ArtworkCustomizationDialog(
            entityType = ArtworkEntityType.ARTIST,
            entityId = artistId.toString(),
            entityName = (uiState as? ArtistDetailUiState.Success)?.artist?.name.orEmpty(),
            currentArtworkUri = (uiState as? ArtistDetailUiState.Success)?.artist?.artUri,
            onDismiss = { showArtworkDialog = false },
            onSave = { uri ->
                viewModel.setCustomArtwork(uri)
                showArtworkDialog = false
            },
            onReset = {
                viewModel.clearCustomArtwork()
                showArtworkDialog = false
            }
        )
    }
}

@Composable
private fun PopularTrackRow(
    rank: Int,
    song: Song,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )

        MelofyArtwork(
            artworkUri = song.albumArtUri,
            title = song.title,
            artist = song.artistName,
            modifier = Modifier.size(44.dp),
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.width(MelofySpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.albumName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                ),
                contentDescription = "Favorite",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
