package com.auralis.app.ui.browse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.ui.library.SongsViewModel
import com.auralis.app.ui.library.components.EmptyStateView

/**
 * Dedicated Artists library destination.
 *
 * Displays all discovered local library artists with:
 * - Circular artist artwork avatar
 * - Artist name
 * - Song count and album count
 * - Direct navigation to Artist Profile
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: SongsViewModel,
    onArtistClick: (Long) -> Unit,
    onSearchClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val animationMode = LocalAnimationMode.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Artists",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (onSearchClick != null) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = "Search artists"
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
        if (artists.isEmpty()) {
            EmptyStateView(
                iconRes = R.drawable.ic_person,
                title = "No Artists Found",
                description = "Artists discovered from your local audio library will appear here.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = MelofySpacing.sm)
            ) {
                item {
                    Text(
                        text = "${artists.size} ARTISTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.xs)
                    )
                }

                items(artists, key = { it.id }) { artist ->
                    ArtistListItem(
                        artist = artist,
                        onClick = { onArtistClick(artist.id) },
                        animationMode = animationMode
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit,
    animationMode: MelofyAnimationMode,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.98f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "artistItemScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple()
            ) { onClick() }
            .padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Artist Artwork
        MelofyArtwork(
            artworkUri = artist.artUri,
            title = artist.name,
            entityType = ArtworkEntityType.ARTIST,
            modifier = Modifier.size(54.dp),
            cornerRadius = 27.dp
        )

        Spacer(modifier = Modifier.width(MelofySpacing.md))

        // Artist Metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val songsCountText = if (artist.songCount == 1) "1 song" else "${artist.songCount} songs"
            val albumsCountText = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"
            Text(
                text = "$songsCountText • $albumsCountText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
