package com.auralis.app.ui.playback.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.playback.model.PlaybackState
import com.auralis.app.ui.components.MelofyArtwork

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyDimensions

/**
 * Premium persistent playback controls bar (mini player).
 * Features rounded artwork with deterministic fallback, draw-phase decoupled progress indicator,
 * animated play/pause state morph, spring-animated favorite interaction, subtle border stroke,
 * and natural expansion to Now Playing.
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
    val animationMode = LocalAnimationMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.99f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "miniPlayerPressScale"
    )

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite && animationMode != MelofyAnimationMode.OFF) 1.2f else 1.0f,
        animationSpec = MelofyMotion.springBouncy(),
        label = "miniPlayerFavoriteBounce"
    )

    val playPauseRotation by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 0f else -90f,
        animationSpec = MelofyMotion.springBouncy(),
        label = "miniPlayerPlayPauseRotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.xs)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = onClick != null
            ) { onClick?.invoke() },
        shape = MelofyShapes.card,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.medium)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle top progress indicator (rendered in draw phase via lambda provider)
            LinearProgressIndicator(
                progress = progressProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail with deterministic gradient fallback
                MelofyArtwork(
                    artworkUri = song.albumArtUri,
                    title = song.title,
                    artist = song.artistName,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 10.dp
                )

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

                // Favorite Toggle (Accessible 48dp touch target)
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(MelofyDimensions.minTouchTarget)
                            .scale(heartScale)
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

                // Play / Pause Button with animated state transition
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    AnimatedContent(
                        targetState = playbackState.isPlaying,
                        transitionSpec = {
                            if (animationMode == MelofyAnimationMode.OFF) {
                                androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                            } else {
                                (scaleIn(initialScale = 0.80f, animationSpec = MelofyMotion.springSubtle<Float>()) + fadeIn(tween(140))).togetherWith(
                                    scaleOut(targetScale = 0.80f, animationSpec = MelofyMotion.springSubtle<Float>()) + fadeOut(tween(100))
                                )
                            }
                        },
                        label = "miniPlayerPlayPauseMorph"
                    ) { isPlaying ->
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Next Button (Accessible 44dp target)
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(44.dp)
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
