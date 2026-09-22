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
 * 1. Deep black / AMOLED-compatible surface (or light surface if light theme).
 * 2. Musical emblem appears with subtle fade + physics-based scale.
 * 3. MELOFI wordmark smoothly reveals with slight upward settling.
 * 4. Tagline "Your music. Your device. Your control." fades in.
 * 5. Smooth continuous dissolve into Home screen.
 *
 * Target duration: ~850-1000ms (or ~350ms in Reduced Motion, 0ms when Off).
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
    // Prevent back-press navigation from interrupting the 0.9s brand sequence
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
    val logoScale = remember { Animatable(if (animationMode == MelofyAnimationMode.REDUCED) 1f else 0.82f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(if (animationMode == MelofyAnimationMode.REDUCED) 0f else 14f) }
    val taglineAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(animationMode) {
        if (animationMode == MelofyAnimationMode.REDUCED) {
            // Reduced motion: simple fade, zero scale/translation, quick ~380ms total
            launch { logoAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
            launch { wordmarkAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
            launch { taglineAlpha.animateTo(0.85f, tween(180, easing = FastOutSlowInEasing)) }
            delay(260)
            exitAlpha.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
            onLaunchComplete()
        } else {
            // Full motion: coordinated brand reveal sequence (~920ms total)

            // Step 1: Musical emblem appears with subtle spring scale and fade (0ms -> 340ms)
            launch {
                logoAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                logoScale.animateTo(1f, MelofyMotion.springSubtle())
            }

            // Step 2: MELOFI wordmark reveals with slight vertical translation (140ms -> 460ms)
            delay(140)
            launch {
                wordmarkAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkOffsetY.animateTo(0f, MelofyMotion.springSubtle())
            }

            // Step 3: Tagline fades in smoothly (280ms -> 560ms)
            delay(140)
            launch {
                taglineAlpha.animateTo(0.85f, tween(260, easing = LinearOutSlowInEasing))
            }

            // Step 4: Settle and hold brand identity briefly
            delay(360)

            // Step 5: Continuous cinematic dissolve into Home (reveal underlying screen)
            launch {
                exitScale.animateTo(1.03f, tween(200, easing = LinearOutSlowInEasing))
            }
            exitAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))

            onLaunchComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(exitScale.value)
            .alpha(exitAlpha.value)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
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
                    tint = MaterialTheme.colorScheme.primary,
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
