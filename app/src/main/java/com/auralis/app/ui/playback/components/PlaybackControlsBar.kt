package com.auralis.app.ui.playback.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.playback.model.PlaybackState

/**
 * Functional bottom playback control surface for Phase 3 verification.
 */
@Composable
fun PlaybackControlsBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isUserSeeking) {
        seekProgress
    } else {
        playbackState.progress
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Seek Slider
            Slider(
                value = currentProgress,
                onValueChange = {
                    isUserSeeking = true
                    seekProgress = it
                },
                onValueChangeFinished = {
                    val targetMs = (seekProgress * playbackState.durationMs).toLong()
                    onSeekTo(targetMs)
                    isUserSeeking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Duration labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val positionMs = if (isUserSeeking) {
                    (seekProgress * playbackState.durationMs).toLong()
                } else {
                    playbackState.currentPositionMs
                }
                Text(
                    text = TimeUtils.formatDuration(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeUtils.formatDuration(playbackState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Track info and controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Artwork for ${song.title}",
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_music_placeholder),
                        error = painterResource(R.drawable.ic_music_placeholder),
                        fallback = painterResource(R.drawable.ic_music_placeholder)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Track Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
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

                // Transport controls
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle),
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(20.dp),
                        tint = if (playbackState.shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(42.dp),
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

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(36.dp)
                ) {
                    val iconRes = if (playbackState.repeatMode == PlaybackRepeatMode.ONE) {
                        R.drawable.ic_repeat_one
                    } else {
                        R.drawable.ic_repeat
                    }
                    val tint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = "Repeat mode: ${playbackState.repeatMode}",
                        modifier = Modifier.size(20.dp),
                        tint = tint
                    )
                }
            }
        }
    }
}
