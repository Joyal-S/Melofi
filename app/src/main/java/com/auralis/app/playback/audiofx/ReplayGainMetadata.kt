package com.auralis.app.playback.audiofx

/**
 * Normalized ReplayGain metadata extracted from audio container tags or stream headers.
 *
 * All values are nullable as tracks may contain only track gain, album gain, or no tags at all.
 */
data class ReplayGainMetadata(
    val trackGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumGainDb: Float? = null,
    val albumPeak: Float? = null
) {
    val hasMetadata: Boolean
        get() = trackGainDb != null || albumGainDb != null

    val displaySummary: String
        get() = when {
            trackGainDb != null && albumGainDb != null -> "Track: ${formatGain(trackGainDb)}, Album: ${formatGain(albumGainDb)}"
            trackGainDb != null -> "Track: ${formatGain(trackGainDb)}"
            albumGainDb != null -> "Album: ${formatGain(albumGainDb)}"
            else -> "No ReplayGain tags found"
        }

    companion object {
        private fun formatGain(gain: Float): String = String.format(java.util.Locale.US, "%+.1f dB", gain)
    }
}
