package com.auralis.app.ui.duplicates

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.domain.model.AudioFormatClassifier
import com.auralis.app.domain.model.DuplicateConfidence
import com.auralis.app.domain.model.DuplicateGroup
import com.auralis.app.domain.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateDetectionScreen(
    viewModel: DuplicateDetectionViewModel,
    onBackClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Detection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanDuplicates() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = "Rescan Duplicates"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DuplicateUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Analyzing local library for duplicates...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is DuplicateUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_copy),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Duplicates Detected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your music library has no matching duplicate tracks. All titles and audio signatures appear unique.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is DuplicateUiState.Success -> {
                LaunchedEffect(state.message) {
                    state.message?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearMessage()
                    }
                }

                if (songToDelete != null) {
                    val target = songToDelete!!
                    AlertDialog(
                        onDismissRequest = { songToDelete = null },
                        title = { Text("Delete Duplicate Track?", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Are you sure you want to delete \"${target.title}\"?\n\n" +
                                "This will permanently remove this file from your device storage. This action cannot be undone."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val id = target.id
                                    songToDelete = null
                                    viewModel.deleteTrack(id)
                                }
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { songToDelete = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${state.groups.size} Duplicate Groups (${state.totalDuplicateTracks} tracks)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Melofy never deletes files automatically. Review duplicate candidates, listen to compare audio quality, and manually choose which file to keep.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(state.groups, key = { it.id }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            onPlaySong = onPlaySong,
                            onDeleteSong = { songToDelete = it }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onPlaySong: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = group.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val confidenceText = when (group.confidence) {
                    DuplicateConfidence.EXACT -> "Exact Match"
                    DuplicateConfidence.VERY_LIKELY -> "Very Likely"
                    DuplicateConfidence.POSSIBLE -> "Possible Match"
                }

                AssistChip(
                    onClick = {},
                    label = { Text(confidenceText, fontWeight = FontWeight.SemiBold) }
                )
            }

            HorizontalDivider()

            group.songs.forEach { song ->
                DuplicateSongRow(
                    song = song,
                    onPlay = { onPlaySong(song) },
                    onDelete = { onDeleteSong(song) }
                )
            }
        }
    }
}

@Composable
private fun DuplicateSongRow(
    song: Song,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_music_placeholder),
                error = painterResource(R.drawable.ic_music_placeholder),
                fallback = painterResource(R.drawable.ic_music_placeholder)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val formatName = AudioFormatClassifier.getFormatName(song.mimeType, song.contentUri)
            val bitrateStr = AudioFormatClassifier.formatBitrate(song.bitrate)
            val sizeStr = AudioFormatClassifier.formatFileSize(song.size)
            val durationStr = TimeUtils.formatDuration(song.duration)

            Text(
                text = "${song.albumName} • $formatName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$durationStr  •  $sizeStr  •  $bitrateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onPlay) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = "Play and Compare",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Delete Track",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
