package com.auralis.app

import com.auralis.app.domain.model.MetadataEditValidator
import com.auralis.app.playback.audiofx.ReplayGainMetadata
import com.auralis.app.playback.audiofx.ReplayGainMode
import com.auralis.app.playback.audiofx.ReplayGainParser
import com.auralis.app.playback.audiofx.SleepTimerManager
import com.auralis.app.playback.audiofx.SleepTimerState
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security, Privacy, and Data Integrity verification test suite for Melofy Phase 12.
 * Validates resilience against malformed metadata, extreme numeric values,
 * unbounded input strings, and timer overflows.
 */
class SecurityAuditTest {

    // =========================================================================
    // 1. REPLAYGAIN SECURITY & MALFORMED DATA RESILIENCE
    // =========================================================================

    @Test
    fun replayGainParser_gainString_rejectsMalformedAndInfiniteValues() {
        assertNull("NaN should be rejected", ReplayGainParser.parseGainString("NaN"))
        assertNull("Infinity should be rejected", ReplayGainParser.parseGainString("Infinity"))
        assertNull("+Infinity should be rejected", ReplayGainParser.parseGainString("+Infinity"))
        assertNull("-Infinity should be rejected", ReplayGainParser.parseGainString("-Infinity"))
        assertNull("Garbage string should be rejected", ReplayGainParser.parseGainString("not_a_number dB"))
        assertNull("Gain exceeding +100dB should be rejected", ReplayGainParser.parseGainString("+120.0 dB"))
        assertNull("Gain below -100dB should be rejected", ReplayGainParser.parseGainString("-150.0 dB"))
        assertNull("Blank string should be null", ReplayGainParser.parseGainString("   "))

        val validGain = ReplayGainParser.parseGainString("-6.20 dB")
        assertNotNull(validGain)
        assertEquals(-6.20f, validGain!!, 0.001f)
    }

    @Test
    fun replayGainParser_peakString_rejectsNegativeAndInfiniteValues() {
        assertNull("Negative peak should be rejected", ReplayGainParser.parsePeakString("-0.5"))
        assertNull("NaN peak should be rejected", ReplayGainParser.parsePeakString("NaN"))
        assertNull("Infinite peak should be rejected", ReplayGainParser.parsePeakString("Infinity"))
        assertNull("Extreme peak > 100 should be rejected", ReplayGainParser.parsePeakString("500.0"))

        val validPeak = ReplayGainParser.parsePeakString("0.9856")
        assertNotNull(validPeak)
        assertEquals(0.9856f, validPeak!!, 0.001f)
    }

    @Test
    fun replayGainParser_calculateVolumeMultiplier_fallsBackToUnityOnNanOrInfinity() {
        // Gain is NaN
        val nanMetadata = ReplayGainMetadata(trackGainDb = Float.NaN, trackPeak = 0.98f)
        val nanResult = ReplayGainParser.calculateVolumeMultiplier(nanMetadata, ReplayGainMode.TRACK)
        assertEquals("NaN gain must safely fall back to unity 1.0f", 1.0f, nanResult, 0.0001f)
        assertTrue(nanResult.isFinite())

        // Gain is Positive Infinity
        val infMetadata = ReplayGainMetadata(trackGainDb = Float.POSITIVE_INFINITY, trackPeak = 1.0f)
        val infResult = ReplayGainParser.calculateVolumeMultiplier(infMetadata, ReplayGainMode.TRACK)
        assertEquals("Infinite gain must safely fall back to unity 1.0f", 1.0f, infResult, 0.0001f)

        // Peak is NaN
        val nanPeakMetadata = ReplayGainMetadata(trackGainDb = -3.0f, trackPeak = Float.NaN)
        val nanPeakResult = ReplayGainParser.calculateVolumeMultiplier(nanPeakMetadata, ReplayGainMode.TRACK)
        assertTrue("Multiplier with NaN peak must be finite", nanPeakResult.isFinite())
        assertTrue("Multiplier must be within 0.0..1.0", nanPeakResult in 0.0f..1.0f)

        // ReplayGainMode.OFF must always be 1.0f
        assertEquals(1.0f, ReplayGainParser.calculateVolumeMultiplier(nanMetadata, ReplayGainMode.OFF), 0.0001f)
    }

    @Test
    fun replayGainParser_calculateVolumeMultiplier_boundsAlwaysWithinZeroAndOne() {
        // Huge positive gain (attempting volume amplification beyond 1.0)
        val hugePositive = ReplayGainMetadata(trackGainDb = 50.0f, trackPeak = 0.5f)
        val positiveResult = ReplayGainParser.calculateVolumeMultiplier(hugePositive, ReplayGainMode.TRACK)
        assertTrue("Multiplier must never exceed 1.0f", positiveResult <= 1.0f)
        assertTrue("Multiplier must be >= 0.0f", positiveResult >= 0.0f)

        // Extreme negative gain
        val hugeNegative = ReplayGainMetadata(trackGainDb = -90.0f, trackPeak = 0.5f)
        val negativeResult = ReplayGainParser.calculateVolumeMultiplier(hugeNegative, ReplayGainMode.TRACK)
        assertTrue("Multiplier must never be negative", negativeResult >= 0.0f)
        assertTrue("Multiplier must never exceed 1.0f", negativeResult <= 1.0f)
    }

    // =========================================================================
    // 2. METADATA INPUT VALIDATION & STRING BOUNDING
    // =========================================================================

    @Test
    fun metadataEditValidator_boundsStringLengthsToMaxFieldLength() {
        val massiveString = "A".repeat(5000)
        val result = MetadataEditValidator.validate(
            title = massiveString,
            artist = massiveString,
            album = massiveString,
            albumArtist = massiveString,
            genre = massiveString,
            composer = massiveString,
            yearText = "2024",
            trackNumberText = "1",
            discNumberText = "1"
        )

        assertTrue("Validation should succeed with sanitized bounded strings", result.isValid)
        val sanitized = result.sanitizedMetadata!!
        assertEquals(MetadataEditValidator.MAX_FIELD_LENGTH, sanitized.title.length)
        assertEquals(MetadataEditValidator.MAX_FIELD_LENGTH, sanitized.artist.length)
        assertEquals(MetadataEditValidator.MAX_FIELD_LENGTH, sanitized.album.length)
        assertEquals(MetadataEditValidator.MAX_FIELD_LENGTH, sanitized.albumArtist!!.length)
        assertEquals(MetadataEditValidator.MAX_FIELD_LENGTH, sanitized.genre!!.length)
        assertEquals(MetadataEditValidator.MAX_FIELD_LENGTH, sanitized.composer!!.length)
    }

    @Test
    fun metadataEditValidator_rejectsEmptyTitleAndInvalidNumbers() {
        // Empty title
        assertFalse(
            MetadataEditValidator.validate(
                title = "   ",
                artist = "Artist",
                album = "Album"
            ).isValid
        )

        // Year out of range
        assertFalse(
            MetadataEditValidator.validate(
                title = "Title",
                artist = "Artist",
                album = "Album",
                yearText = "800"
            ).isValid
        )
        assertFalse(
            MetadataEditValidator.validate(
                title = "Title",
                artist = "Artist",
                album = "Album",
                yearText = "3000"
            ).isValid
        )

        // Track number out of range
        assertFalse(
            MetadataEditValidator.validate(
                title = "Title",
                artist = "Artist",
                album = "Album",
                trackNumberText = "-1"
            ).isValid
        )
        assertFalse(
            MetadataEditValidator.validate(
                title = "Title",
                artist = "Artist",
                album = "Album",
                trackNumberText = "1000"
            ).isValid
        )

        // Disc number out of range
        assertFalse(
            MetadataEditValidator.validate(
                title = "Title",
                artist = "Artist",
                album = "Album",
                discNumberText = "0"
            ).isValid
        )
        assertFalse(
            MetadataEditValidator.validate(
                title = "Title",
                artist = "Artist",
                album = "Album",
                discNumberText = "100"
            ).isValid
        )
    }

    @Test
    fun metadataEditValidator_unsupportedFormatsRemainBlocked() {
        assertFalse("WAV containers must be blocked from tag edits", MetadataEditValidator.isFormatEditable("audio/wav", "song.wav"))
        assertFalse("AIFF containers must be blocked from tag edits", MetadataEditValidator.isFormatEditable("audio/aiff", "song.aiff"))
        assertTrue("MP3 must be editable", MetadataEditValidator.isFormatEditable("audio/mpeg", "song.mp3"))
        assertTrue("FLAC must be editable", MetadataEditValidator.isFormatEditable("audio/flac", "song.flac"))
        assertTrue("M4A must be editable", MetadataEditValidator.isFormatEditable("audio/mp4", "song.m4a"))
        assertTrue("OGG must be editable", MetadataEditValidator.isFormatEditable("audio/ogg", "song.ogg"))
    }

    // =========================================================================
    // 3. SLEEP TIMER SECURITY & BOUNDARY CLAMPING
    // =========================================================================

    @Test
    fun sleepTimerManager_clampsExcessiveDurationAndRejectsNegative() {
        val testDispatcher = Dispatchers.Unconfined
        val timer = SleepTimerManager(
            playbackController = null,
            dispatcher = testDispatcher,
            tickIntervalMs = 1000L
        )

        // Zero or negative minutes are safely ignored
        timer.startMinutes(0)
        assertTrue(timer.timerState.value is SleepTimerState.Inactive)
        timer.startMinutes(-10)
        assertTrue(timer.timerState.value is SleepTimerState.Inactive)

        // Zero or negative seconds are safely ignored
        timer.startSeconds(0L)
        assertTrue(timer.timerState.value is SleepTimerState.Inactive)
        timer.startSeconds(-500L)
        assertTrue(timer.timerState.value is SleepTimerState.Inactive)

        // Excessive duration (e.g. 5,000 minutes = >3 days) is clamped to 1,440 minutes (24h = 86,400s)
        timer.startMinutes(5000)
        val state = timer.timerState.value
        assertTrue("Timer state should be Running", state is SleepTimerState.Running)
        val running = state as SleepTimerState.Running
        assertEquals(86_400L, running.totalDurationSeconds)
        assertEquals(86_400L, running.remainingSeconds)

        timer.cancel()
        assertTrue(timer.timerState.value is SleepTimerState.Inactive)
    }
}
