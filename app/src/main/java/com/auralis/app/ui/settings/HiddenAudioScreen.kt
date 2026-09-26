package com.auralis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.data.local.db.entity.HiddenAudioEntity
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.ui.library.components.EmptyStateView
import kotlinx.coroutines.launch

/**
 * Screen displaying audio tracks hidden by the user from the Melofi Music Library.
 * Users can review all hidden tracks and restore any individual track back into the library.
 * Safe & offline: Audio files are never deleted or modified on device storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAudioScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hiddenAudioList by viewModel.hiddenAudioList.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hidden Audio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (hiddenAudioList.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_music_placeholder,
                title = "No Hidden Tracks",
                description = "Audio files hidden from your Music Library will appear here. You can hide unwanted voice notes or clips using the track menu (⋮) in your library.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(MelofySpacing.lg),
                verticalArrangement = Arrangement.spacedBy(MelofySpacing.md)
            ) {
                item {
                    // Informative Guidance Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MelofyShapes.card,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MelofySpacing.md),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(MelofySpacing.md))
                            Column {
                                Text(
                                    text = "${hiddenAudioList.size} Hidden Track${if (hiddenAudioList.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "These tracks are excluded from your music collection, search, and playlists. Tap 'Restore' to return them to your library at any time. Files remain safe on storage.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(hiddenAudioList, key = { it.id }) { item ->
                    HiddenAudioItem(
                        item = item,
                        onRestore = {
                            viewModel.restoreHiddenAudio(item.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("\"${item.title}\" restored to Music Library")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAudioItem(
    item: HiddenAudioEntity,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MelofyShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MelofySpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                MelofyArtwork(
                    artworkUri = item.albumArtUri,
                    title = item.title,
                    artist = item.artistName,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 8.dp
                )
                Spacer(modifier = Modifier.width(MelofySpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.artistName} • ${TimeUtils.formatDuration(item.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val path = item.filePath
                    if (!path.isNullOrBlank()) {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(MelofySpacing.sm))
            FilledTonalButton(
                onClick = onRestore,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Restore", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
