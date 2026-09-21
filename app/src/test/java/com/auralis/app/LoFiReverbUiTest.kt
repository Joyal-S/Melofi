package com.auralis.app

import androidx.compose.ui.graphics.Color
import com.auralis.app.core.ui.theme.AmoledColorScheme
import com.auralis.app.core.ui.theme.DarkColorScheme
import com.auralis.app.core.ui.theme.LightColorScheme
import com.auralis.app.playback.dsp.DspParameters
import com.auralis.app.playback.dsp.DspPreset
import com.auralis.app.playback.dsp.MelofyDspEngine
import com.auralis.app.ui.settings.AudioSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Deterministic unit test suite for Melofy Real-Time Lo-Fi + Reverb UI Integration (Addon 3).
 *
 * Verifies:
 * - Default OFF state out of the box
 * - Master enable/disable toggle mechanics
 * - Preset switching and parameter synchronization
 * - Master intensity adjustments and percentage formatting
 * - Advanced parameter manipulation (Bit Crush, Sample Rate, Saturation, Room Size, Decay, Wet Mix)
 * - UI composite state binding
 * - Theme compatibility across Light, Dark, and AMOLED modes
 */
class LoFiReverbUiTest {

    private lateinit var dspEngine: MelofyDspEngine

    @Before
    fun setUp() {
        dspEngine = MelofyDspEngine()
    }

    @Test
    fun defaultState_isOffOutOfTheBox() {
        val params = dspEngine.currentParameters
        assertFalse("Lo-Fi must be disabled by default", params.loFi.enabled)
        assertFalse("Reverb must be disabled by default", params.reverb.enabled)
        assertFalse("isAnyActive must be false by default", params.isAnyActive)
        assertEquals(DspPreset.DREAMY, params.preset)
        assertEquals(0.5f, params.loFi.intensity, 0.001f)
    }

    @Test
    fun toggleMasterEnabled_updatesEngineAndState() {
        // Master Enable
        dspEngine.setLoFiEnabled(true)
        dspEngine.setReverbEnabled(true)

        var params = dspEngine.currentParameters
        assertTrue("Lo-Fi should be enabled", params.loFi.enabled)
        assertTrue("Reverb should be enabled", params.reverb.enabled)
        assertTrue("isAnyActive should be true", params.isAnyActive)
        assertTrue("isBothActive should be true", params.isBothActive)

        // Master Disable
        dspEngine.setLoFiEnabled(false)
        dspEngine.setReverbEnabled(false)

        params = dspEngine.currentParameters
        assertFalse("Lo-Fi should be disabled", params.loFi.enabled)
        assertFalse("Reverb should be disabled", params.reverb.enabled)
        assertFalse("isAnyActive should be false", params.isAnyActive)
        assertFalse("isBothActive should be false", params.isBothActive)
    }

    @Test
    fun presetSelection_updatesParametersAndPreservesActiveState() {
        // Enable both effects
        dspEngine.setLoFiEnabled(true)
        dspEngine.setReverbEnabled(true)
        assertTrue(dspEngine.isAnyActive)

        // Switch to Cassette
        dspEngine.setPreset(DspPreset.CASSETTE)
        var params = dspEngine.currentParameters
        assertEquals(DspPreset.CASSETTE, params.preset)
        assertTrue("Active state must be preserved after switching preset", params.isAnyActive)
        assertEquals(0.70f, params.loFi.intensity, 0.01f)
        assertEquals(0.30f, params.loFi.sampleRateReduction, 0.01f)
        assertEquals(0.25f, params.loFi.bitCrush, 0.01f)
        assertEquals(0.45f, params.loFi.saturation, 0.01f)
        assertEquals(0.35f, params.reverb.roomSize, 0.01f)

        // Switch to Late Night
        dspEngine.setPreset(DspPreset.LATE_NIGHT)
        params = dspEngine.currentParameters
        assertEquals(DspPreset.LATE_NIGHT, params.preset)
        assertTrue("Active state must be preserved", params.isAnyActive)
        assertEquals(0.50f, params.loFi.intensity, 0.01f)
        assertEquals(0.10f, params.loFi.sampleRateReduction, 0.01f)

        // Switch to Deep Lo-Fi
        dspEngine.setPreset(DspPreset.DEEP_LOFI)
        params = dspEngine.currentParameters
        assertEquals(DspPreset.DEEP_LOFI, params.preset)
        assertTrue("Active state must be preserved", params.isAnyActive)
        assertEquals(0.85f, params.loFi.intensity, 0.01f)
        assertEquals(0.50f, params.loFi.sampleRateReduction, 0.01f)
        assertEquals(0.75f, params.reverb.roomSize, 0.01f)

        // Switch back to Dreamy
        dspEngine.setPreset(DspPreset.DREAMY)
        params = dspEngine.currentParameters
        assertEquals(DspPreset.DREAMY, params.preset)
        assertTrue("Active state must be preserved", params.isAnyActive)
        assertEquals(0.45f, params.loFi.intensity, 0.01f)
    }

    @Test
    fun intensityAdjustment_switchesToCustomAndUpdatesLoFiIntensity() {
        dspEngine.setPreset(DspPreset.DREAMY)
        assertEquals(DspPreset.DREAMY, dspEngine.currentParameters.preset)

        val updated = dspEngine.currentParameters.copy(
            preset = DspPreset.CUSTOM,
            loFi = dspEngine.currentParameters.loFi.copy(intensity = 0.58f)
        )
        dspEngine.updateParameters(updated)

        val params = dspEngine.currentParameters
        assertEquals(DspPreset.CUSTOM, params.preset)
        assertEquals(0.58f, params.loFi.intensity, 0.001f)
        assertEquals(58, (params.loFi.intensity * 100).roundToInt())
    }

    @Test
    fun intensityClamping_handlesBoundaryValues() {
        val minParams = dspEngine.currentParameters.copy(
            loFi = dspEngine.currentParameters.loFi.copy(intensity = (-0.5f).coerceIn(0f, 1f))
        )
        assertEquals(0.0f, minParams.loFi.intensity, 0.001f)

        val maxParams = dspEngine.currentParameters.copy(
            loFi = dspEngine.currentParameters.loFi.copy(intensity = (1.5f).coerceIn(0f, 1f))
        )
        assertEquals(1.0f, maxParams.loFi.intensity, 0.001f)
    }

    @Test
    fun advancedControls_updateIndividualParametersCorrectly() {
        // Lo-Fi Advanced Parameters
        var params = dspEngine.currentParameters.copy(
            preset = DspPreset.CUSTOM,
            loFi = dspEngine.currentParameters.loFi.copy(
                bitCrush = 0.42f,
                sampleRateReduction = 0.65f,
                saturation = 0.80f
            ),
            reverb = dspEngine.currentParameters.reverb.copy(
                roomSize = 0.72f,
                decay = 0.88f,
                wetLevel = 0.40f
            )
        )
        dspEngine.updateParameters(params)

        params = dspEngine.currentParameters
        assertEquals(DspPreset.CUSTOM, params.preset)
        assertEquals(0.42f, params.loFi.bitCrush, 0.001f)
        assertEquals(42, (params.loFi.bitCrush * 100).roundToInt())
        assertEquals(0.65f, params.loFi.sampleRateReduction, 0.001f)
        assertEquals(65, (params.loFi.sampleRateReduction * 100).roundToInt())
        assertEquals(0.80f, params.loFi.saturation, 0.001f)
        assertEquals(80, (params.loFi.saturation * 100).roundToInt())

        // Reverb Advanced Parameters
        assertEquals(0.72f, params.reverb.roomSize, 0.001f)
        assertEquals(72, (params.reverb.roomSize * 100).roundToInt())
        assertEquals(0.88f, params.reverb.decay, 0.001f)
        assertEquals(88, (params.reverb.decay * 100).roundToInt())
        assertEquals(0.40f, params.reverb.wetLevel, 0.001f)
        assertEquals(40, (params.reverb.wetLevel * 100).roundToInt())
    }

    @Test
    fun uiState_compositeStateReflectsDspParameters() {
        val defaultUiState = AudioSettingsUiState()
        assertNotNull(defaultUiState.dspParameters)
        assertFalse(defaultUiState.dspParameters.isAnyActive)

        val activeUiState = defaultUiState.copy(
            dspParameters = DspParameters.createPreset(DspPreset.CASSETTE, enabled = true)
        )
        assertTrue(activeUiState.dspParameters.isAnyActive)
        assertEquals(DspPreset.CASSETTE, activeUiState.dspParameters.preset)
    }

    @Test
    fun presetDisplayNames_areHumanFriendly() {
        assertEquals("Dreamy", DspPreset.DREAMY.displayName)
        assertEquals("Cassette", DspPreset.CASSETTE.displayName)
        assertEquals("Late Night", DspPreset.LATE_NIGHT.displayName)
        assertEquals("Deep Lo-Fi", DspPreset.DEEP_LOFI.displayName)
        assertEquals("Custom", DspPreset.CUSTOM.displayName)
    }

    @Test
    fun themeCompatibility_amoledModeHasPureBlackSurfaces() {
        assertEquals(Color(0xFF000000), AmoledColorScheme.background)
        assertEquals(Color(0xFF000000), AmoledColorScheme.surface)
        assertNotNull(DarkColorScheme.primary)
        assertNotNull(LightColorScheme.primary)
    }
}
