package com.auralis.app.playback

import com.auralis.app.data.model.MelofiDefaultCovers
import com.auralis.app.domain.model.SmartPlaylistType
import com.auralis.app.playback.dsp.DspParameters
import com.auralis.app.playback.dsp.DspPreset
import com.auralis.app.playback.dsp.LoFiDspEngine
import com.auralis.app.playback.dsp.MelofyDspEngine
import com.auralis.app.playback.dsp.ReverbDspEngine
import com.auralis.app.ui.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Unit test suite validating the final consolidated Melofi refinements:
 * 1. 5-Destination Main Navigation hierarchy (Songs, Favorites, Playlists, Artists, Settings)
 * 2. Smart Playlists deduplication (Favorites excluded from Playlists screen)
 * 3. Lo-Fi DSP warmth tuning invariants & soft saturation bounds
 * 4. 2-Pole analog cascaded low-pass filter & tape saturation stability
 * 5. Instantaneous master DSP toggle logic
 * 6. 16 default cover distribution & fallback invariants
 */
class ComprehensiveEnhancementTest {

    private lateinit var loFiEngine: LoFiDspEngine
    private lateinit var reverbEngine: ReverbDspEngine
    private lateinit var unifiedEngine: MelofyDspEngine

    @Before
    fun setUp() {
        loFiEngine = LoFiDspEngine()
        reverbEngine = ReverbDspEngine()
        unifiedEngine = MelofyDspEngine(loFiEngine, reverbEngine)
    }

    // ========================================================================
    // 1. BOTTOM NAVIGATION ARCHITECTURE & TAB ORDER
    // ========================================================================

    @Test
    fun `MainTab enum contains exactly 5 destinations in correct order`() {
        val tabs = MainTab.entries
        assertEquals(5, tabs.size)
        assertEquals(MainTab.SONGS, tabs[0])
        assertEquals(MainTab.FAVORITES, tabs[1])
        assertEquals(MainTab.PLAYLISTS, tabs[2])
        assertEquals(MainTab.ARTISTS, tabs[3])
        assertEquals(MainTab.SETTINGS, tabs[4])
    }

    // ========================================================================
    // 2. SMART PLAYLIST DEDUPLICATION
    // ========================================================================

    @Test
    fun `Filtering out FAVORITES leaves exactly 5 smart playlist categories without gaps`() {
        val filtered = SmartPlaylistType.entries.filter { it != SmartPlaylistType.FAVORITES }
        assertEquals(5, filtered.size)
        assertFalse(filtered.contains(SmartPlaylistType.FAVORITES))
        assertTrue(filtered.contains(SmartPlaylistType.RECENTLY_PLAYED))
        assertTrue(filtered.contains(SmartPlaylistType.MOST_PLAYED))
        assertTrue(filtered.contains(SmartPlaylistType.NEVER_PLAYED))
        assertTrue(filtered.contains(SmartPlaylistType.RECENTLY_ADDED))
        assertTrue(filtered.contains(SmartPlaylistType.FORGOTTEN_FAVORITES))
    }

    // ========================================================================
    // 3. DSP PRESETS WARMTH & STABILITY INVARIANTS
    // ========================================================================

    @Test
    fun `All presets define warm musical parameters within safe bounds`() {
        for (preset in DspPreset.entries) {
            val params = DspParameters.createPreset(preset, enabled = true)
            assertTrue("Preset $preset intensity in range", params.loFi.intensity in 0.0f..1.0f)
            assertTrue("Preset $preset SR reduction in range", params.loFi.sampleRateReduction in 0.0f..1.0f)
            assertTrue("Preset $preset bit crush in range", params.loFi.bitCrush in 0.0f..1.0f)
            assertTrue("Preset $preset cutoff in range", params.loFi.lowPassCutoff in 0.0f..1.0f)
            assertTrue("Preset $preset saturation in range", params.loFi.saturation in 0.0f..1.0f)

            // BIBO stability guarantees for reverb
            assertTrue("Preset $preset reverb decay in safe range", params.reverb.decay in 0.0f..0.85f)
            assertTrue("Preset $preset reverb wetLevel in safe range", params.reverb.wetLevel in 0.0f..0.7f)
        }
    }

    @Test
    fun `Instantaneous DSP toggle preserves all internal parameters`() {
        val original = DspParameters.createPreset(DspPreset.CASSETTE, enabled = false)
        val toggled = original.copy(
            loFi = original.loFi.copy(enabled = true),
            reverb = original.reverb.copy(enabled = true)
        )

        assertTrue(toggled.isAnyActive)
        assertEquals(original.preset, toggled.preset)
        assertEquals(original.loFi.intensity, toggled.loFi.intensity, 0.001f)
        assertEquals(original.reverb.roomSize, toggled.reverb.roomSize, 0.001f)

        val toggledBack = toggled.copy(
            loFi = toggled.loFi.copy(enabled = false),
            reverb = toggled.reverb.copy(enabled = false)
        )
        assertFalse(toggledBack.isAnyActive)
        assertEquals(original, toggledBack)
    }

    // ========================================================================
    // 4. LO-FI DSP 2-POLE FILTER & SOFT SATURATION PROCESSING
    // ========================================================================

    @Test
    fun `Lo-Fi DSP bypass is bit-for-bit identical when disabled`() {
        val frameCount = 512
        val channels = 2
        val input = FloatArray(frameCount * channels) { (it % 100) / 100.0f }
        val output = FloatArray(frameCount * channels)

        val disabledParams = DspParameters.createPreset(DspPreset.DREAMY, enabled = false)
        unifiedEngine.updateParameters(disabledParams)
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        for (i in input.indices) {
            assertEquals("Sample $i must match exactly in bypass", input[i], output[i], 0.0f)
        }
    }

    @Test
    fun `Lo-Fi DSP handles extreme overdrive without clipping overflow or NaN`() {
        val frameCount = 1024
        val channels = 2
        // Extreme input: 3x full scale sine wave (amplitude 3.0f)
        val input = FloatArray(frameCount * channels) { i ->
            3.0f * sin(2.0 * PI * 440.0 * (i / channels) / 44100.0).toFloat()
        }
        val output = FloatArray(frameCount * channels)

        val heavyParams = DspParameters.createPreset(DspPreset.DEEP_LOFI, enabled = true)
        unifiedEngine.updateParameters(heavyParams)
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        for (i in output.indices) {
            val s = output[i]
            assertFalse("Sample $i must not be NaN", s.isNaN())
            assertFalse("Sample $i must not be infinite", s.isInfinite())
            assertTrue("Sample $i must be strictly clamped [-1.0, 1.0]", s in -1.0f..1.0f)
        }
    }

    @Test
    fun `Lo-Fi DSP 2-pole analog filter attenuates high frequencies smoothly`() {
        val frameCount = 1024
        val channels = 1
        // High-frequency test tone (12 kHz)
        val input = FloatArray(frameCount) { i ->
            sin(2.0 * PI * 12000.0 * i / 44100.0).toFloat()
        }
        val output = FloatArray(frameCount)

        val cassetteParams = DspParameters.createPreset(DspPreset.CASSETTE, enabled = true)
        unifiedEngine.updateParameters(cassetteParams)
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        // Peak output of high frequencies should be significantly attenuated due to 2-pole LPF
        val maxOutput = output.drop(200).maxOf { kotlin.math.abs(it) }
        assertTrue("High frequencies must be attenuated (maxOutput=$maxOutput)", maxOutput < 0.65f)
    }

    // ========================================================================
    // 5. DEFAULT COVER ARTWORK REGISTRY
    // ========================================================================

    @Test
    fun `MelofiDefaultCovers has exactly 16 images and shifts consecutive collisions`() {
        val covers = MelofiDefaultCovers.AVAILABLE_COVERS
        assertEquals(16, covers.size)

        // Verify png and jpg set
        assertTrue(covers.contains("1.png"))
        assertTrue(covers.contains("10.png"))
        assertTrue(covers.contains("11.jpg"))
        assertTrue(covers.contains("16.jpg"))

        // Verify collision shifting
        val (firstCover, firstIndex) = MelofiDefaultCovers.pickCover(100L)
        val (secondCover, secondIndex) = MelofiDefaultCovers.pickCover(100L, lastAssignedIndex = firstIndex)

        // Must shift away from lastAssignedIndex
        assertTrue("Second index must differ from first index", secondIndex != firstIndex)
        assertTrue("Second cover must differ from first cover", secondCover != firstCover)
    }
}
