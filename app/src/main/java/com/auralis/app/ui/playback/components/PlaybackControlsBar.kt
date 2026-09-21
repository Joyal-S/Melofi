package com.auralis.app.ui.playback.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.playback.model.PlaybackState

/**
 * Polished, sleek persistent playback controls bar (mini player).
 * Tapping the bar expands into the full Now Playing screen.
 */
@Composable
fun PlaybackControlsBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    progressProvider: () -> Float = { playbackState.progress },
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MelofySpacing.sm, vertical = MelofySpacing.xxs)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = MelofyShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.medium)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sleek top progress indicator (rendered in draw phase via lambda provider)
            LinearProgressIndicator(
                progress = progressProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Artwork for ${song.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_music_placeholder),
                        error = painterResource(R.drawable.ic_music_placeholder),
                        fallback = painterResource(R.drawable.ic_music_placeholder)
                    )
                }

                Spacer(modifier = Modifier.width(MelofySpacing.md))

                // Track Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = MelofySpacing.xs)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artistName} • ${song.albumName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite Toggle (if provided)
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                            ),
                            contentDescription = if (isFavorite) "Favorite" else "Not favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Play / Pause Button
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        painter = painterResource(
                            if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
