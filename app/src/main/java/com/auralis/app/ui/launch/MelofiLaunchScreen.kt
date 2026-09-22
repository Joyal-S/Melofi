package com.auralis.app.ui.launch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium, calm, brand-authentic app launch experience for Melofi.
 *
 * Sequence:
 * 0ms: Deep black / AMOLED-compatible surface (or light surface if light theme).
 * ~100–250ms: Melofi musical emblem appears with subtle fade + physics-based scale.
 * ~250–480ms: MELOFI wordmark smoothly reveals with slight upward settling.
 * ~400–620ms: Tagline "Your music. Your device. Your control." fades in.
 * ~550–900ms: Home content smoothly emerges through the brand transition (continuous alpha dissolve while brand subtly drifts upward).
 * ~920ms: Home becomes fully interactive with zero abrupt cuts or layout shifts.
 *
 * Completely non-blocking to underlying library/database initialization.
 */
@Composable
fun MelofiLaunchScreen(
    isDark: Boolean,
    isAmoled: Boolean,
    animationMode: MelofyAnimationMode,
    onLaunchComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Prevent back-press navigation from interrupting the brand sequence
    BackHandler { /* consume back press during initial launch */ }

    // Instant bypass if animations are disabled
    if (animationMode == MelofyAnimationMode.OFF) {
        LaunchedEffect(Unit) {
            onLaunchComplete()
        }
        return
    }

    // Determine surface background color matching active theme
    val backgroundColor = when {
        isAmoled -> Color.Black
        isDark -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.background
    }

    // Animation drivers
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (animationMode == MelofyAnimationMode.REDUCED) 1f else 0.84f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(if (animationMode == MelofyAnimationMode.REDUCED) 0f else 12f) }
    val taglineAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val contentDissolveOffsetY = remember { Animatable(0f) }
    val haloExpansion = remember { Animatable(1f) }

    LaunchedEffect(animationMode) {
        if (animationMode == MelofyAnimationMode.REDUCED) {
            // Reduced motion: simple fade, zero scale/translation, quick ~360ms total
            launch { logoAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
            launch { wordmarkAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
            launch { taglineAlpha.animateTo(0.85f, tween(160, easing = FastOutSlowInEasing)) }
            delay(220)
            exitAlpha.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
            onLaunchComplete()
        } else {
            // Full motion: coordinated brand reveal and seamless blending into Home (~920ms total)

            // Step 1: Musical emblem appears (0ms -> 280ms)
            launch {
                logoAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
            launch {
                logoScale.animateTo(1f, MelofyMotion.springSubtle())
            }

            // Step 2: MELOFI wordmark reveals (140ms -> 420ms)
            delay(140)
            launch {
                wordmarkAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkOffsetY.animateTo(0f, MelofyMotion.springSubtle())
            }

            // Step 3: Tagline fades in smoothly (280ms -> 520ms)
            delay(140)
            launch {
                taglineAlpha.animateTo(0.85f, tween(240, easing = LinearOutSlowInEasing))
            }

            // Step 4: Settle briefly (520ms -> 580ms)
            delay(240)

            // Step 5: Continuous blend into Home:
            // The overlay smoothly fades from 1f to 0f over 340ms, while the brand mark
            // subtly drifts upward by -16dp, naturally unveiling the Home screen underneath.
            launch {
                contentDissolveOffsetY.animateTo(-16f, tween(340, easing = FastOutSlowInEasing))
            }
            launch {
                haloExpansion.animateTo(1.25f, tween(340, easing = LinearOutSlowInEasing))
            }
            exitAlpha.animateTo(0f, tween(340, easing = FastOutSlowInEasing))

            onLaunchComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = contentDissolveOffsetY.value.dp)
        ) {
            // Ambient subtle back-glow behind the logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                // Soft radial ambient illumination
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(haloExpansion.value)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Melofi Musical Emblem
                Icon(
                    painter = painterResource(R.drawable.ic_melofi_brand_mark),
                    contentDescription = "Melofi Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(MelofySpacing.lg))

            // MELOFI Wordmark
            Text(
                text = "MELOFI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .offset(y = wordmarkOffsetY.value.dp)
                    .alpha(wordmarkAlpha.value)
            )

            Spacer(modifier = Modifier.height(MelofySpacing.sm))

            // Brand Tagline
            Text(
                text = "Your music. Your device. Your control.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
