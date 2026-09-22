package com.auralis.app.ui.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyDimensions
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.ui.theme.MelofyTypographyTokens
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.ui.playback.components.PlaybackQueueSheet

/**
 * Polished, dedicated full-screen Now Playing screen.
 * Reuses existing Media3 PlaybackController without creating duplicate engines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    onCollapse: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: (Long) -> Unit,
    onOpenAudioSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animationMode = LocalAnimationMode.current
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val activeQueue by playbackViewModel.activeQueue.collectAsStateWithLifecycle()
    val dspParams by playbackViewModel.dspParameters.collectAsStateWithLifecycle()
    val song = playbackState.currentSong ?: return

    DisposableEffect(Unit) {
        playbackViewModel.setHighFrequencyTracking(true)
        onDispose {
            playbackViewModel.setHighFrequencyTracking(false)
        }
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite && animationMode != MelofyAnimationMode.OFF) 1.2f else 1.0f,
        animationSpec = MelofyMotion.springBouncy(),
        label = "heartScale"
    )

    // Compute audio quality tag
    val qualityTag = remember(song.mimeType, song.bitrate, song.sampleRate) {
        val mime = song.mimeType
        when {
            mime != null && mime.contains("flac", ignoreCase = true) -> {
                if (song.sampleRate != null && song.sampleRate >= 96000) "HI-RES • FLAC ${song.sampleRate / 1000}kHz"
                else "LOSSLESS • FLAC"
            }
            mime != null && mime.contains("wav", ignoreCase = true) -> "LOSSLESS • WAV"
            mime != null && (mime.contains("ogg", ignoreCase = true) || mime.contains("opus", ignoreCase = true)) -> "OGG OPUS"
            song.bitrate != null && song.bitrate >= 320 -> "320 KBPS • MP3"
            song.bitrate != null && song.bitrate in 256..319 -> "256 KBPS • AAC"
            song.sampleRate != null && song.sampleRate >= 96000 -> "HI-RES • ${song.sampleRate / 1000}kHz"
            else -> null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PLAYING FROM LIBRARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = song.albumName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_expand_more),
                            contentDescription = "Collapse Player",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenAudioSettings?.invoke() },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = "Audio Effects & Equalizer",
                            tint = if (dspParams.isAnyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MelofySpacing.xl, vertical = MelofySpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(MelofySpacing.sm))

            // 1. Large Hero Album Artwork with Ambient Glow & Dynamic Elevation
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Ambient tonal back-glow
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .shadow(
                            elevation = MelofyElevation.high,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        )
                )

                MelofyArtwork(
                    artworkUri = song.albumArtUri,
                    title = song.title,
                    artist = song.artistName,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(MelofyElevation.medium, RoundedCornerShape(24.dp)),
                    cornerRadius = 24.dp
                )
            }

            Spacer(modifier = Modifier.height(MelofySpacing.xl))

            // 2. Title, Artist & Favorite Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = MelofySpacing.sm)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(song.id) },
                    modifier = Modifier
                        .size(MelofyDimensions.minTouchTarget)
                        .scale(heartScale)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                        ),
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Audio Format Quality & Active DSP Badges Row
            if (qualityTag != null || dspParams.isAnyActive) {
                Spacer(modifier = Modifier.height(MelofySpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    qualityTag?.let { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MelofyTypographyTokens.qualityBadge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = MelofySpacing.sm, vertical = 3.dp)
                            )
                        }
                    }

                    if (dspParams.isAnyActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onOpenAudioSettings?.invoke() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = MelofySpacing.sm, vertical = 3.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_waves),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(MelofySpacing.xs))
                                Text(
                                    text = "Lo-Fi • ${dspParams.preset.displayName}",
                                    style = MelofyTypographyTokens.qualityBadge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MelofySpacing.md))

            // 3. Scrub Slider & Time Row (isolated high-frequency recomposition)
            NowPlayingScrubber(
                playbackViewModel = playbackViewModel,
                durationMs = playbackState.durationMs
            )

            Spacer(modifier = Modifier.height(MelofySpacing.md))

            // 4. Primary Transport Controls Row with tactile spring feedback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                TactileIconButton(
                    onClick = { playbackViewModel.toggleShuffle() },
                    modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                    animationMode = animationMode
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle),
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                TactileIconButton(
                    onClick = { playbackViewModel.previous() },
                    modifier = Modifier.size(52.dp),
                    animationMode = animationMode
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Button with animated morph and tactile press feedback
                val playPauseInteraction = remember { MutableInteractionSource() }
                val isPlayPausePressed by playPauseInteraction.collectIsPressedAsState()
                val playPauseScale by animateFloatAsState(
                    targetValue = if (isPlayPausePressed && animationMode != MelofyAnimationMode.OFF) 0.92f else 1.0f,
                    animationSpec = MelofyMotion.springSubtle(),
                    label = "playPauseTactile"
                )
                val playPauseRotation by animateFloatAsState(
                    targetValue = if (playbackState.isPlaying) 0f else -90f,
                    animationSpec = MelofyMotion.springBouncy(),
                    label = "nowPlayingPlayPauseRotation"
                )

                FilledIconButton(
                    onClick = { playbackViewModel.togglePlayPause() },
                    interactionSource = playPauseInteraction,
                    modifier = Modifier
                        .size(68.dp)
                        .graphicsLayer {
                            scaleX = playPauseScale
                            scaleY = playPauseScale
                        },
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
                                (scaleIn(initialScale = 0.65f, animationSpec = MelofyMotion.springBouncy<Float>()) + fadeIn(tween(180))).togetherWith(
                                    scaleOut(targetScale = 0.65f, animationSpec = MelofyMotion.springSubtle<Float>()) + fadeOut(tween(140))
                                )
                            }
                        },
                        label = "nowPlayingPlayPauseMorph"
                    ) { isPlaying ->
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    rotationZ = if (animationMode != MelofyAnimationMode.OFF) playPauseRotation else 0f
                                }
                        )
                    }
                }

                // Next
                TactileIconButton(
                    onClick = { playbackViewModel.next() },
                    modifier = Modifier.size(52.dp),
                    animationMode = animationMode
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat
                TactileIconButton(
                    onClick = { playbackViewModel.cycleRepeatMode() },
                    modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                    animationMode = animationMode
                ) {
                    val repeatIcon = if (playbackState.repeatMode == PlaybackRepeatMode.ONE) {
                        R.drawable.ic_repeat_one
                    } else {
                        R.drawable.ic_repeat
                    }
                    val repeatTint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = "Repeat Mode: ${playbackState.repeatMode}",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MelofySpacing.md))

            // 5. Bottom Queue Shortcut Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TactileIconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                    animationMode = animationMode
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_queue_music),
                        contentDescription = "Open Play Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MelofySpacing.sm))
        }

        // Active Playback Queue Bottom Sheet
        if (showQueueSheet) {
            PlaybackQueueSheet(
                queue = activeQueue,
                currentSong = song,
                sheetState = queueSheetState,
                onDismiss = { showQueueSheet = false },
                onSongSelect = { selectedSong, queue ->
                    playbackViewModel.playSong(selectedSong, queue)
                }
            )
        }
    }
}

@Composable
private fun TactileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationMode: MelofyAnimationMode = MelofyAnimationMode.FULL,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.88f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "tactileScale"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}

/**
 * Isolated scrubber composable that observes playbackProgress and playbackPositionMs.
 * Confines high-frequency 500ms recompositions strictly to the slider and duration readouts,
 * leaving artwork, top bar, titles, and transport controls undisturbed.
 */
@Composable
private fun NowPlayingScrubber(
    playbackViewModel: PlaybackViewModel,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val progress by playbackViewModel.playbackProgress.collectAsStateWithLifecycle()
    val positionMsState by playbackViewModel.playbackPositionMs.collectAsStateWithLifecycle()

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isUserSeeking) seekProgress else progress

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = currentProgress.coerceIn(0f, 1f),
            onValueChange = {
                isUserSeeking = true
                seekProgress = it
            },
            onValueChangeFinished = {
                val targetMs = (seekProgress * durationMs).toLong()
                playbackViewModel.seekTo(targetMs)
                isUserSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val positionMs = if (isUserSeeking) {
                (seekProgress * durationMs).toLong()
            } else {
                positionMsState
            }
            val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)

            Text(
                text = TimeUtils.formatDuration(positionMs),
                style = MelofyTypographyTokens.durationText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "-${TimeUtils.formatDuration(remainingMs)}",
                style = MelofyTypographyTokens.durationText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

