package com.auralis.app.ui.playback.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.ui.playback.PlaybackViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium, minimal waveform playback progress control.
 *
 * Implements the layout:
 * Elapsed time                         Remaining/total time
 * 2:18 ─────── waveform/progress ─────── 6:44
 *
 * Preserves Phase 11 performance optimizations:
 * - High-frequency updates from [playbackViewModel] are confined to this isolated composable.
 * - Waveform heights are precomputed once per track and cached across ticks.
 * - Draw operations allocate zero objects on tick or drag.
 * - Seek thumb uses subtle spring scaling during interaction.
 */
@Composable
fun MelofiWaveformScrubber(
    playbackViewModel: PlaybackViewModel,
    durationMs: Long,
    songId: Long,
    modifier: Modifier = Modifier
) {
    val animationMode = LocalAnimationMode.current
    val progress by playbackViewModel.playbackProgress.collectAsStateWithLifecycle()
    val positionMsState by playbackViewModel.playbackPositionMs.collectAsStateWithLifecycle()

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var showRemainingTime by remember { mutableStateOf(false) }

    val currentProgress = if (isUserSeeking) seekProgress else progress

    val currentPositionMs = if (isUserSeeking) {
        (seekProgress * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    } else {
        positionMsState.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }

    val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)

    // Precompute 44 normalized waveform bar heights for this song
    val barAmplitudes = remember(songId) {
        val totalBars = 44
        val seed = abs(songId.hashCode())
        FloatArray(totalBars) { i ->
            val norm = i.toFloat() / (totalBars - 1).toFloat()
            // Window envelope tapering at the start and end
            val envelope = sin(norm * Math.PI).toFloat().coerceIn(0.15f, 1.0f)
            val wave1 = sin(norm * Math.PI * 4.0 + (seed % 7)).toFloat()
            val wave2 = cos(norm * Math.PI * 8.5 + (seed % 11)).toFloat()
            val combined = 0.40f + 0.35f * wave1 + 0.25f * wave2
            (combined * envelope).coerceIn(0.18f, 1.0f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val thumbGlowColor = primaryColor.copy(alpha = 0.35f)

    // Seek thumb radius spring animation
    val thumbRadiusPx by animateFloatAsState(
        targetValue = if (isUserSeeking && animationMode != MelofyAnimationMode.OFF) 9f else 5.5f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "waveformThumbRadius"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MelofySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Elapsed time
        Text(
            text = TimeUtils.formatDuration(currentPositionMs),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.width(MelofySpacing.xs))

        // Center: Interactive Waveform & Progress Track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .pointerInput(durationMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val trackWidth = size.width.toFloat()
                        if (trackWidth <= 0f) return@awaitEachGesture

                        var fraction = (down.position.x / trackWidth).coerceIn(0f, 1f)
                        isUserSeeking = true
                        seekProgress = fraction
                        down.consume()

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                isUserSeeking = false
                                val targetMs = (fraction * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
                                playbackViewModel.seekTo(targetMs)
                                break
                            } else {
                                fraction = (change.position.x / trackWidth).coerceIn(0f, 1f)
                                seekProgress = fraction
                                change.consume()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                val totalWidth = size.width
                val totalHeight = size.height
                val totalBars = barAmplitudes.size
                val barSpacing = 2.5.dp.toPx()
                val totalSpacing = barSpacing * (totalBars - 1)
                val barWidth = ((totalWidth - totalSpacing) / totalBars).coerceAtLeast(1.5f)
                val maxBarHeight = totalHeight * 0.78f
                val minBarHeight = 4.dp.toPx()
                val centerY = totalHeight / 2f
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                val clampedProgress = currentProgress.coerceIn(0f, 1f)
                val thumbX = totalWidth * clampedProgress

                // Draw waveform bars
                for (i in 0 until totalBars) {
                    val barX = i * (barWidth + barSpacing)
                    val barMidX = barX + barWidth / 2f
                    val amp = barAmplitudes[i]
                    val barHeight = (maxBarHeight * amp).coerceAtLeast(minBarHeight)
                    val top = centerY - barHeight / 2f

                    val isPlayed = barMidX <= thumbX
                    val barColor = if (isPlayed) primaryColor else inactiveTrackColor

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barX, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Draw central seek thumb
                val thumbRadius = thumbRadiusPx.dp.toPx()

                // Ambient glow when seeking
                if (isUserSeeking) {
                    drawCircle(
                        color = thumbGlowColor,
                        radius = thumbRadius * 2.2f,
                        center = Offset(thumbX, centerY)
                    )
                }

                // Solid thumb circle
                drawCircle(
                    color = primaryColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY)
                )

                // Inner contrast dot on thumb
                drawCircle(
                    color = Color.White,
                    radius = (thumbRadius * 0.4f).coerceAtLeast(1.5f),
                    center = Offset(thumbX, centerY)
                )
            }
        }

        Spacer(modifier = Modifier.width(MelofySpacing.xs))

        // Right: Total or Remaining duration (tap to toggle)
        Text(
            text = if (showRemainingTime) {
                "-${TimeUtils.formatDuration(remainingMs)}"
            } else {
                TimeUtils.formatDuration(durationMs)
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(48.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                showRemainingTime = !showRemainingTime
                                break
                            }
                        }
                    }
                },
            textAlign = TextAlign.End
        )
    }
}
