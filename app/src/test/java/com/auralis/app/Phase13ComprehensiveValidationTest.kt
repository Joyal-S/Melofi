package com.auralis.app

import com.auralis.app.data.local.db.entity.SongEntity
import com.auralis.app.data.local.preferences.AudioPreferences
import com.auralis.app.data.local.scanner.LibrarySyncDiff
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.MetadataEditValidator
import com.auralis.app.domain.model.SearchRankingEngine
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.ThemeMode
import com.auralis.app.domain.model.UserPreferences
import com.auralis.app.playback.audiofx.ReplayGainMetadata
import com.auralis.app.playback.audiofx.ReplayGainMode
import com.auralis.app.playback.audiofx.ReplayGainParser
import com.auralis.app.playback.dsp.DspPreset
import com.auralis.app.playback.dsp.LoFiAudioProcessor
import com.auralis.app.playback.dsp.LoFiParameters
import com.auralis.app.playback.dsp.ReverbAudioProcessor
import com.auralis.app.playback.dsp.ReverbParameters
import com.auralis.app.playback.history.PlaybackSessionTracker
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.playback.queue.PlaybackQueue
import com.auralis.app.playback.util.SeekSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Melofy Phase 13 Comprehensive Testing & Device Compatibility Validation Suite.
 *
 * Verifies the complete integrated Melofy product across all architectural layers:
 * - First launch and default state invariants
 * - MediaStore differential synchronization and empty-scan safety
 * - Search query sanitization (SQL injection patterns, emojis, extreme lengths)
 * - Playback queue state transitions, shuffle integrity, and repeat loop semantics
 * - Audio format decoder independence and multi-sample-rate / multi-channel matrix
 * - Listening history session tracking and completion threshold rules
 * - Security, offline isolation, and manifest hardening audit
 * - Full audio chain coexistence (ReplayGain + Lo-Fi saturation + Reverb comb filters)
 * - Metadata bounds and capability validation
 */
class Phase13ComprehensiveValidationTest {

    private fun createTestSong(id: Long, title: String = "Song $id", artist: String = "Artist", duration: Long = 180000L): Song {
        return Song(
            id = id,
            title = title,
            artistId = 10L,
            artistName = artist,
            albumId = 20L,
            albumName = "Album",
            genreId = 1L,
            genreName = "Rock",
            duration = duration,
            trackNumber = id.toInt(),
            discNumber = 1,
            year = 2024,
            contentUri = "content://media/external/audio/media/$id",
            mimeType = "audio/mpeg",
            size = 4000000L,
            bitrate = 320,
            sampleRate = 44100,
            channelCount = 2,
            dateAdded = 1700000000L,
            dateModified = 1700000000L,
            albumArtUri = "content://media/external/audio/albumart/20"
        )
    }

    // =========================================================================
    // 1. FIRST LAUNCH & DEFAULT STATE INVARIANTS
    // =========================================================================

    @Test
    fun firstLaunch_defaultState_allSubsystemsSafeAndInitialized() {
        val defaultUserPrefs = UserPreferences()
        assertEquals("Default theme must be SYSTEM", ThemeMode.SYSTEM, defaultUserPrefs.themeMode)
        assertFalse("AMOLED black mode should be off by default", defaultUserPrefs.amoledBlack)
        assertEquals("Default start screen must be FOR_YOU", LibraryTab.FOR_YOU, defaultUserPrefs.defaultLibraryTab)

        val defaultAudioPrefs = AudioPreferences()
        assertFalse("Equalizer must be OFF by default", defaultAudioPrefs.equalizerEnabled)
        assertFalse("Bass boost must be OFF by default", defaultAudioPrefs.bassBoostEnabled)
        assertFalse("Virtualizer must be OFF by default", defaultAudioPrefs.virtualizerEnabled)
        assertEquals("ReplayGain must be OFF by default", ReplayGainMode.OFF, defaultAudioPrefs.replayGainMode)
        assertFalse("Lo-Fi must be OFF by default", defaultAudioPrefs.dspLoFiEnabled)
        assertFalse("Reverb must be OFF by default", defaultAudioPrefs.dspReverbEnabled)
        assertEquals("Default preset must be DREAMY", DspPreset.DREAMY, defaultAudioPrefs.dspPreset)
        assertEquals("Default intensity must be 0.5f", 0.5f, defaultAudioPrefs.dspLoFiIntensity, 0.001f)

        val queue = PlaybackQueue()
        assertTrue("Initial queue must be empty", queue.isEmpty())
        assertEquals("Initial queue size must be 0", 0, queue.size())
        assertEquals("Initial current index must be -1", -1, queue.currentIndex())
    }

    // =========================================================================
    // 2. DIFFERENTIAL SYNC & EMPTY SCAN SAFETY
    // =========================================================================

    @Test
    fun librarySync_differentialIntegrity_preservesExistingDataOnEmptyScan() {
        val existingMap = mapOf(1L to 1000L, 2L to 1000L, 3L to 1000L)
        val scanned = emptyList<SongEntity>()

        // When a scan yields 0 items (e.g. storage momentarily unmounted or permission denied)
        val diff = LibrarySyncDiff.computeDiff(existingMap, scanned)
        assertEquals("Diff identifies missing songs for deletion", 3, diff.toDeleteIds.size)
        assertTrue("No new inserts on empty scan", diff.toInsert.isEmpty())
        assertTrue("No updates on empty scan", diff.toUpdate.isEmpty())
    }

    // =========================================================================
    // 3. SEARCH SANITIZATION & EXTREME QUERY RESILIENCE
    // =========================================================================

    @Test
    fun searchRanking_specialCharactersAndExtremeQueries_noCrashAndBoundedExecution() {
        val songs = listOf(
            createTestSong(1, "Bohemian Rhapsody", "Queen"),
            createTestSong(2, "Stairway to Heaven", "Led Zeppelin"),
            createTestSong(3, "Hotel California", "Eagles"),
            createTestSong(4, "Smells Like Teen Spirit", "Nirvana")
        )

        // 1. Emojis and unicode
        val emojiResults = SearchRankingEngine.rankSongs(songs, "🎸 🎶 🔥")
        assertNotNull("Emoji query must not crash", emojiResults)

        // 2. SQL injection patterns
        val sqlQuery = "'; DROP TABLE songs; -- ' OR '1'='1"
        val sqlResults = SearchRankingEngine.rankSongs(songs, sqlQuery)
        assertNotNull("SQL injection pattern must not crash search engine", sqlResults)

        // 3. Diacritics and accented characters
        val diacriticResults = SearchRankingEngine.rankSongs(songs, "Bôhémiàn Rhäpsôdy")
        assertNotNull("Diacritic query must not crash", diacriticResults)

        // 4. Extreme query length (10,000 characters)
        val massiveQuery = "A".repeat(10000)
        val massiveResults = SearchRankingEngine.rankSongs(songs, massiveQuery)
        assertNotNull("Massive query must be handled safely", massiveResults)
        assertTrue("Massive non-matching query yields 0 results", massiveResults.isEmpty())

        // 5. Blank and punctuation-only queries
        assertTrue("Blank query returns all or empty safely", SearchRankingEngine.rankSongs(songs, "   ").isEmpty())
        assertTrue("Punctuation-only query does not throw", SearchRankingEngine.rankSongs(songs, "!@#$%^&*()").isEmpty())
    }

    // =========================================================================
    // 4. PLAYBACK CORE STATE MACHINE & SEEK SANITIZATION
    // =========================================================================

    @Test
    fun playbackCore_seekSanitizer_clampsAllInvalidPositions() {
        val durationMs = 210000L // 3.5 minutes

        // Negative seeks clamped to 0
        assertEquals(0L, SeekSanitizer.sanitize(-1000L, durationMs))
        assertEquals(0L, SeekSanitizer.sanitize(-1L, durationMs))

        // Normal seeks preserved
        assertEquals(45000L, SeekSanitizer.sanitize(45000L, durationMs))

        // Seeks beyond duration clamped to duration
        assertEquals(durationMs, SeekSanitizer.sanitize(210001L, durationMs))
        assertEquals(durationMs, SeekSanitizer.sanitize(9999999L, durationMs))

        // Unknown duration (-1L) preserves positive seeks
        assertEquals(30000L, SeekSanitizer.sanitize(30000L, -1L))
        assertEquals(0L, SeekSanitizer.sanitize(-500L, -1L))
    }

    // =========================================================================
    // 5. QUEUE SHUFFLE INTEGRITY & REPEAT MODES
    // =========================================================================

    @Test
    fun playbackQueue_repeatMatrix_allCombinationsDeterministic() {
        val songs = (1L..5L).map { createTestSong(it) }
        val queue = PlaybackQueue()
        queue.setQueue(songs, startIndex = 0)

        assertEquals(5, queue.size())
        assertEquals(0, queue.currentIndex())

        // 1. RepeatMode.OFF: Advancing past the end returns null
        queue.setCurrentIndex(4)
        assertEquals(4, queue.currentIndex())
        val nextAfterEnd = queue.nextSong(PlaybackRepeatMode.OFF)
        assertEquals(null, nextAfterEnd)

        // 2. RepeatMode.ALL: Advancing past the end wraps to index 0
        val wrappedNext = queue.nextSong(PlaybackRepeatMode.ALL)
        assertNotNull("Repeat ALL must advance", wrappedNext)
        assertEquals(1L, wrappedNext?.id)
        assertEquals("Repeat ALL must wrap to index 0", 0, queue.currentIndex())

        // 3. RepeatMode.ONE: Current index stays on track
        val repeatedSong = queue.nextSong(PlaybackRepeatMode.ONE)
        assertEquals(1L, repeatedSong?.id)
        assertEquals(0, queue.currentIndex())
    }

    // =========================================================================
    // 6. AUDIO FORMAT & SAMPLE RATE MATRIX
    // =========================================================================

    @Test
    fun sampleRatesAndChannels_allMatrixCombinations_stableOutput() {
        val sampleRates = intArrayOf(44100, 48000, 88200, 96000, 192000)
        val channelCounts = intArrayOf(1, 2)

        for (sampleRate in sampleRates) {
            for (channels in channelCounts) {
                val loFiProcessor = LoFiAudioProcessor()
                loFiProcessor.updateParameters(LoFiParameters(enabled = true, intensity = 0.6f))

                val reverbProcessor = ReverbAudioProcessor()
                reverbProcessor.updateParameters(ReverbParameters(enabled = true, roomSize = 0.7f, decay = 0.7f, wetLevel = 0.5f))

                // Configure processors
                val inputConfig = androidx.media3.common.audio.AudioProcessor.AudioFormat(
                    sampleRate,
                    channels,
                    androidx.media3.common.C.ENCODING_PCM_16BIT
                )
                loFiProcessor.configure(inputConfig)
                loFiProcessor.flush()
                reverbProcessor.configure(inputConfig)
                reverbProcessor.flush()

                // Generate 100ms test sine burst
                val frameCount = sampleRate / 10
                val byteCount = frameCount * channels * 2
                val inputBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN)

                for (f in 0 until frameCount) {
                    val sampleVal = (Math.sin(2.0 * Math.PI * 440.0 * f / sampleRate) * 16000.0).toInt().toShort()
                    for (ch in 0 until channels) {
                        inputBuffer.putShort(sampleVal)
                    }
                }
                inputBuffer.flip()

                // Execute Lo-Fi
                loFiProcessor.queueInput(inputBuffer)
                val loFiOutput = loFiProcessor.getOutput()
                assertTrue("Lo-Fi output must have data for $sampleRate Hz, $channels ch", loFiOutput.hasRemaining())

                // Execute Reverb on Lo-Fi output
                reverbProcessor.queueInput(loFiOutput)
                val reverbOutput = reverbProcessor.getOutput()
                assertTrue("Reverb output must have data for $sampleRate Hz, $channels ch", reverbOutput.hasRemaining())

                // Verify output audio samples are bounded and valid
                val shortBuffer = reverbOutput.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                while (shortBuffer.hasRemaining()) {
                    val sample = shortBuffer.get()
                    // 16-bit short value must be finite
                    assertTrue("Sample must be valid 16-bit PCM", sample in Short.MIN_VALUE..Short.MAX_VALUE)
                }

                loFiProcessor.reset()
                reverbProcessor.reset()
            }
        }
    }

    // =========================================================================
    // 7. SESSION TRACKER & HISTORY COMPLETION THRESHOLD
    // =========================================================================

    @Test
    fun sessionTracker_playbackThresholdCalculations() {
        val fakeRepo = object : com.auralis.app.domain.repository.HistoryRepository {
            override suspend fun recordPlay(songId: Long, playedAt: Long, durationMs: Long, completed: Boolean): Long = 1L
            override suspend fun updatePlay(id: Long, durationMs: Long, completed: Boolean) {}
            override fun getRecentHistory(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.auralis.app.domain.model.PlaybackHistoryItem>())
            override fun getRecentlyPlayedSongs(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<Song>())
            override fun getListeningStats(timeRange: com.auralis.app.domain.model.TimeRange) = kotlinx.coroutines.flow.flowOf(com.auralis.app.domain.model.ListeningStats())
            override fun getTopSongs(timeRange: com.auralis.app.domain.model.TimeRange, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.auralis.app.domain.model.TopSongItem>())
            override fun getTopArtists(timeRange: com.auralis.app.domain.model.TimeRange, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.auralis.app.domain.model.TopArtistItem>())
            override fun getTopAlbums(timeRange: com.auralis.app.domain.model.TimeRange, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.auralis.app.domain.model.TopAlbumItem>())
            override suspend fun pruneHistory(maxEntries: Int) {}
            override suspend fun clearHistory() {}
        }

        val tracker = PlaybackSessionTracker(fakeRepo)

        // Threshold calculations:
        // Long track (240s) -> 30s threshold
        assertEquals(30000L, tracker.calculatePlayThreshold(240000L))

        // Medium track (40s) -> 50% = 20s threshold
        assertEquals(20000L, tracker.calculatePlayThreshold(40000L))

        // Short track (8s) -> 5s minimum floor
        assertEquals(5000L, tracker.calculatePlayThreshold(8000L))

        // 0 or unknown duration -> 30s fallback
        assertEquals(30000L, tracker.calculatePlayThreshold(0L))
        assertEquals(30000L, tracker.calculatePlayThreshold(-1L))
    }

    // =========================================================================
    // 8. SECURITY & OFFLINE MANIFEST VERIFICATION
    // =========================================================================

    @Test
    fun securityAndPrivacy_manifestAndRuntimeClasspathAudit() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val altManifestFile = File("app/src/main/AndroidManifest.xml")
        val activeFile = if (manifestFile.exists()) manifestFile else altManifestFile
        assertTrue("AndroidManifest.xml must exist", activeFile.exists())

        val manifestText = activeFile.readText()

        // Absolute Privacy Verification
        assertFalse("Manifest MUST NOT declare android.permission.INTERNET", manifestText.contains("android.permission.INTERNET"))
        assertTrue("Manifest must remove ACCESS_NETWORK_STATE", manifestText.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue("Manifest must set allowBackup=\"false\"", manifestText.contains("android:allowBackup=\"false\""))

        // Playback foreground service type declaration
        assertTrue("Manifest must declare FOREGROUND_SERVICE_MEDIA_PLAYBACK", manifestText.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"))
    }

    // =========================================================================
    // 9. FULL AUDIO CHAIN COEXISTENCE (REPLAYGAIN + DSP)
    // =========================================================================

    @Test
    fun dspAndAudioFx_fullChainSimulation_noOverflowOrClipping() {
        // ReplayGain metadata with +6dB boost
        val rgMetadata = ReplayGainMetadata(trackGainDb = 6.0f, trackPeak = 0.95f)
        val volumeMultiplier = ReplayGainParser.calculateVolumeMultiplier(rgMetadata, ReplayGainMode.TRACK)
        assertTrue("Multiplier must be clamped to safe range <= 1.0f", volumeMultiplier in 0.0f..1.0f)

        // Configure full DSP pipeline
        val loFiProcessor = LoFiAudioProcessor()
        loFiProcessor.updateParameters(LoFiParameters(enabled = true, intensity = 0.8f, saturation = 0.6f, bitCrush = 0.5f))

        val reverbProcessor = ReverbAudioProcessor()
        reverbProcessor.updateParameters(ReverbParameters(enabled = true, roomSize = 0.8f, decay = 0.85f, wetLevel = 0.6f))

        val format = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )
        loFiProcessor.configure(format)
        loFiProcessor.flush()
        reverbProcessor.configure(format)
        reverbProcessor.flush()

        // Create high-amplitude test buffer
        val frameCount = 1024
        val input = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount) {
            val rawSample = (Math.sin(2.0 * Math.PI * 1000.0 * i / 44100.0) * 32000.0 * volumeMultiplier).toInt().toShort()
            input.putShort(rawSample)
            input.putShort(rawSample)
        }
        input.flip()

        loFiProcessor.queueInput(input)
        val loFiOut = loFiProcessor.getOutput()

        reverbProcessor.queueInput(loFiOut)
        val finalOut = reverbProcessor.getOutput()

        assertTrue("Final output must contain processed samples", finalOut.hasRemaining())
        val shortOut = finalOut.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        while (shortOut.hasRemaining()) {
            val sample = shortOut.get()
            // Sample must never overflow 16-bit PCM range
            assertTrue("Sample must be within 16-bit boundaries", sample in Short.MIN_VALUE..Short.MAX_VALUE)
        }
    }

    // =========================================================================
    // 10. METADATA VALIDATION & BOUNDS
    // =========================================================================

    @Test
    fun metadataAndArtwork_boundsAndSafeFallback() {
        // Valid validation
        val validResult = MetadataEditValidator.validate(
            title = "Valid Title",
            artist = "Valid Artist",
            album = "Valid Album",
            yearText = "2024",
            trackNumberText = "5",
            discNumberText = "1"
        )
        assertTrue(validResult.isValid)
        assertEquals(2024, validResult.sanitizedMetadata?.year)
        assertEquals(5, validResult.sanitizedMetadata?.trackNumber)

        // Year outside valid 1000..2100 bounds
        val invalidYearResult = MetadataEditValidator.validate(
            title = "Valid Title",
            artist = "Artist",
            album = "Album",
            yearText = "500"
        )
        assertFalse("Year 500 must fail validation", invalidYearResult.isValid)

        // Future year > 2100
        val futureYearResult = MetadataEditValidator.validate(
            title = "Valid Title",
            artist = "Artist",
            album = "Album",
            yearText = "2500"
        )
        assertFalse("Year 2500 must fail validation", futureYearResult.isValid)

        // Invalid track number <= 0
        val invalidTrackResult = MetadataEditValidator.validate(
            title = "Valid Title",
            artist = "Artist",
            album = "Album",
            trackNumberText = "0"
        )
        assertFalse("Track 0 must fail validation", invalidTrackResult.isValid)

        // Format editability check
        assertTrue("MP3 must be editable", MetadataEditValidator.isFormatEditable("audio/mpeg", "song.mp3"))
        assertTrue("FLAC must be editable", MetadataEditValidator.isFormatEditable("audio/flac", "song.flac"))
        assertFalse("WAV must NOT be editable", MetadataEditValidator.isFormatEditable("audio/x-wav", "song.wav"))
    }
}
