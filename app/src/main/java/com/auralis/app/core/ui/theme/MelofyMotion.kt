package com.auralis.app.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf

/**
 * User-configurable animation preference supporting accessibility.
 */
enum class MelofyAnimationMode(val displayName: String) {
    FULL("Full Motion"),
    REDUCED("Reduced Motion"),
    OFF("Animations Off")
}

val LocalAnimationMode = compositionLocalOf { MelofyAnimationMode.FULL }

/**
 * Centralized motion tokens and animation specifications for Melofy.
 * Guarantees consistent motion language across screens and micro-interactions.
 */
object MelofyMotion {
    // Duration tokens (ms)
    const val DURATION_MICRO = 150
    const val DURATION_FAST = 200
    const val DURATION_NORMAL = 280
    const val DURATION_MEDIUM = 350
    const val DURATION_SLOW = 450

    // Easing curves
    val standardEasing = FastOutSlowInEasing
    val enterEasing = LinearOutSlowInEasing
    val exitEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val emphasizeEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Spring specs for micro-interactions
    fun <T> springSubtle(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springBouncy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springSmooth(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springSnappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Adaptive tween respecting user animation preference
    fun <T> adaptiveTween(
        mode: MelofyAnimationMode,
        durationMillis: Int = DURATION_NORMAL
    ): FiniteAnimationSpec<T> {
        return when (mode) {
            MelofyAnimationMode.OFF -> snap()
            MelofyAnimationMode.REDUCED -> tween(durationMillis = durationMillis / 2, easing = standardEasing)
            MelofyAnimationMode.FULL -> tween(durationMillis = durationMillis, easing = standardEasing)
        }
    }
}

