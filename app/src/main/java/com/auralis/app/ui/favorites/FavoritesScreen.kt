package com.auralis.app.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.library.components.AddToPlaylistDialog
import com.auralis.app.ui.library.components.SongListItem
import com.auralis.app.ui.playback.PlaybackViewModel

/**
 * Screen displaying the user's favorite songs with playback controls and playlist integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoritesViewModel: FavoritesViewModel,
    playbackViewModel: PlaybackViewModel,
    playlists: List<Playlist>,
    onAddSongToPlaylist: (playlistId: Long, songId: Long) -> Unit,
    onCreatePlaylistAndAdd: (name: String, songId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val favoriteSongs by favoritesViewModel.favoriteSongs.collectAsStateWithLifecycle()
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val count = if (favoriteSongs.isNotEmpty()) " (${favoriteSongs.size})" else ""
                    Text(
                        text = "Favorites$count",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (favoriteSongs.isNotEmpty()) {
                        IconButton(
                            onClick = { playbackViewModel.playSongs(favoriteSongs, 0) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = "Play all favorites",
                                tint = MaterialTheme.colorScheme.primary
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (favoriteSongs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_favorite_border),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the heart icon on any song in your library to add it to your favorites.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = favoriteSongs,
                        key = { it.id }
                    ) { song ->
                        SongListItem(
                            song = song,
                            onClick = { playbackViewModel.playSong(song, favoriteSongs) },
                            isFavorite = true,
                            onToggleFavorite = { favoritesViewModel.removeFavorite(song.id) },
                            onAddToPlaylist = { songForPlaylistDialog = song },
                            onPlayNext = { playbackViewModel.addSongsToQueue(listOf(song)) }
                        )
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    songForPlaylistDialog?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { songForPlaylistDialog = null },
            onAddToPlaylist = { playlistId ->
                onAddSongToPlaylist(playlistId, song.id)
                songForPlaylistDialog = null
            },
            onCreateAndAdd = { name ->
                onCreatePlaylistAndAdd(name, song.id)
                songForPlaylistDialog = null
            }
        )
    }
}
