package com.auralis.app.playback.audiofx

/**
 * Immutable state of the Bass Boost audio effect.
 *
 * Android BassBoost strength ranges from 0 to 1000 (0% to 100%).
 */
data class BassBoostState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val strength: Int = 0
) {
    val strengthPercentage: Int
        get() = (strength / 10).coerceIn(0, 100)

    companion object {
        const val MIN_STRENGTH = 0
        const val MAX_STRENGTH = 1000

        fun clampStrength(strength: Int): Int = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)

        fun percentageToStrength(percentage: Int): Int = (percentage.coerceIn(0, 100) * 10)
    }
}
