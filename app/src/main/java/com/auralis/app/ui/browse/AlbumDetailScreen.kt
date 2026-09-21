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
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.library.components.SongListItem
import com.auralis.app.ui.playback.PlaybackViewModel

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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = album.artUri,
                                    contentDescription = album.name,
                                    modifier = Modifier.size(180.dp),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_music_placeholder),
                                    error = painterResource(R.drawable.ic_music_placeholder),
                                    fallback = painterResource(R.drawable.ic_music_placeholder)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = album.artistName ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = buildString {
                                    if (album.year != null && album.year > 0) {
                                        append("${album.year} • ")
                                    }
                                    append("${songs.size} tracks")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Playback Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    // Song List
                    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        SongListItem(
                            song = song,
                            onClick = { onSongClick(song, songs) },
                            isFavorite = song.id in favoriteSongIds,
                            onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                            onAddToPlaylist = { onAddToPlaylistClick(song) },
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
}
