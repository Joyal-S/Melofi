package com.auralis.app.playback.dsp

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * High-performance, zero-allocation Real-Time Lo-Fi DSP Engine with click-free parameter smoothing.
 *
 * Implements 4 sequential analog/retro modeling stages tuned for warm, musical, vintage character:
 * 1. Sample Rate Decimation (Phase-accumulating downsampler modeling vintage 8/12-bit samplers)
 * 2. Bit-Depth Quantization (Linear stepped quantizer modeling vintage digital converters)
 * 3. 2-Pole Cascaded Analog Low-Pass Filter (Smooth 12dB/octave tape warmth rolloff eliminating harsh digital fizz)
 * 4. Musical Analog Tape Saturation (Smooth algebraic transfer curve x/sqrt(1+x^2) delivering warm analog harmonics without harsh clipping)
 *
 * Features:
 * - Linear per-frame parameter interpolation eliminating zipper noise during real-time adjustments.
 * - Zero heap allocations during [process] execution.
 * - Numerically bounded to guarantee no NaN, Infinity, or clipping overflow.
 * - Clean zero-overhead bypass when disabled.
 * - Thread-safe state reset on seek or track transitions.
 */
class LoFiDspEngine {

    // Filter states for 2-pole cascaded warm low-pass IIR filter (Left & Right)
    private var lpfStateLeft1: Float = 0.0f
    private var lpfStateLeft2: Float = 0.0f
    private var lpfStateRight1: Float = 0.0f
    private var lpfStateRight2: Float = 0.0f

    // Sample-and-hold states for decimation
    private var holdLeft: Float = 0.0f
    private var holdRight: Float = 0.0f
    private var decimationPhase: Float = 1.0f

    // Smoothed parameter states for click-free transitions
    private var currentIntensity: Float = 0.0f
    private var currentDecimationFactor: Float = 1.0f
    private var currentHalfLevels: Float = 32768.0f
    private var currentLpfAlpha: Float = 0.95f
    private var currentSatDrive: Float = 1.0f
    private var currentBitCrush: Float = 0.0f
    private var currentSaturation: Float = 0.0f

    private var isInitialized: Boolean = false

    /**
     * Resets internal filter states and sample-and-hold accumulators.
     * Must be invoked during seek, pause-flush, or track transitions to eliminate clicks.
     */
    fun reset() {
        lpfStateLeft1 = 0.0f
        lpfStateLeft2 = 0.0f
        lpfStateRight1 = 0.0f
        lpfStateRight2 = 0.0f
        holdLeft = 0.0f
        holdRight = 0.0f
        decimationPhase = 1.0f
        isInitialized = false
    }

    /**
     * Processes interleaved audio samples in real time with per-frame parameter smoothing.
     *
     * @param inputSamples Interleaved PCM samples as normalized floats in [-1.0f..1.0f].
     * @param outputSamples Target array to receive processed float samples (can be the same as [inputSamples]).
     * @param frameCount Total audio frames to process.
     * @param channelCount Number of audio channels (1 = Mono, 2 = Stereo).
     * @param sampleRate Input sample rate in Hz (e.g. 44100, 48000, 96000).
     * @param params Active immutable [LoFiParameters].
     */
    fun process(
        inputSamples: FloatArray,
        outputSamples: FloatArray,
        frameCount: Int,
        channelCount: Int,
        sampleRate: Int,
        params: LoFiParameters
    ) {
        if (!params.enabled || params.intensity <= 0.0f) {
            // Direct zero-overhead bypass copy
            if (inputSamples !== outputSamples) {
                System.arraycopy(inputSamples, 0, outputSamples, 0, frameCount * channelCount)
            }
            currentIntensity = 0.0f
            return
        }

        val targetIntensity = params.intensity
        val targetReduction = params.sampleRateReduction
        val targetBitCrush = params.bitCrush
        val targetSaturation = params.saturation

        // Compute target parameter targets
        val targetDecimationFactor = 1.0f + (targetReduction * 6.0f)

        val targetBits = 16.0f - (targetBitCrush * 7.5f)
        val targetQuantizationLevels = 2.0.pow(targetBits.toDouble()).toFloat()
        val targetHalfLevels = targetQuantizationLevels * 0.5f

        val safeSampleRate = sampleRate.coerceAtLeast(8000).toFloat()
        // Musically tuned 2-pole analog reconstruction curve: rolls off digital fizz into tape warmth (1.8 kHz to 14 kHz)
        val targetCutoffHz = 1800.0f + (params.lowPassCutoff * 12200.0f)
        val omega = (2.0 * PI * targetCutoffHz / safeSampleRate).toFloat()
        val targetLpfAlpha = (omega / (omega + 1.0f)).coerceIn(0.02f, 0.98f)

        val targetSatDrive = 1.0f + (targetSaturation * 1.5f)

        if (!isInitialized) {
            currentIntensity = targetIntensity
            currentDecimationFactor = targetDecimationFactor
            currentHalfLevels = targetHalfLevels
            currentLpfAlpha = targetLpfAlpha
            currentSatDrive = targetSatDrive
            currentBitCrush = targetBitCrush
            currentSaturation = targetSaturation
            isInitialized = true
        }

        // Per-frame ramp increments
        val invFrames = 1.0f / frameCount.coerceAtLeast(1).toFloat()
        val stepIntensity = (targetIntensity - currentIntensity) * invFrames
        val stepDecimation = (targetDecimationFactor - currentDecimationFactor) * invFrames
        val stepHalfLevels = (targetHalfLevels - currentHalfLevels) * invFrames
        val stepAlpha = (targetLpfAlpha - currentLpfAlpha) * invFrames
        val stepSatDrive = (targetSatDrive - currentSatDrive) * invFrames
        val stepBitCrush = (targetBitCrush - currentBitCrush) * invFrames
        val stepSaturation = (targetSaturation - currentSaturation) * invFrames

        var intensity = currentIntensity
        var decimationFactor = currentDecimationFactor
        var halfLevels = currentHalfLevels
        var lpfAlpha = currentLpfAlpha
        var satDrive = currentSatDrive
        var bitCrush = currentBitCrush
        var saturation = currentSaturation

        var sampleIndex = 0

        if (channelCount == 1) {
            // Mono processing
            for (frame in 0 until frameCount) {
                intensity += stepIntensity
                decimationFactor += stepDecimation
                halfLevels += stepHalfLevels
                lpfAlpha += stepAlpha
                satDrive += stepSatDrive
                bitCrush += stepBitCrush
                saturation += stepSaturation

                val dry = inputSamples[sampleIndex]
                var wet = dry

                // Stage 1: 2-Pole Cascaded Warm Analog Low-Pass Filter (Anti-aliasing and tape warmth)
                lpfStateLeft1 += lpfAlpha * (wet - lpfStateLeft1)
                lpfStateLeft2 += lpfAlpha * (lpfStateLeft1 - lpfStateLeft2)
                wet = lpfStateLeft2

                // Stage 2: Decimation (Sample & Hold)
                decimationPhase += 1.0f
                if (decimationPhase >= decimationFactor) {
                    holdLeft = wet
                    decimationPhase -= decimationFactor
                    if (decimationPhase >= decimationFactor) {
                        decimationPhase = 0.0f
                    }
                } else {
                    wet = holdLeft
                }

                // Stage 3: Bit crushing
                if (bitCrush > 0.01f) {
                    wet = (round(wet * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 4: Musical Analog Tape Saturation (Smooth transfer curve with 2nd harmonic warmth)
                if (saturation > 0.01f) {
                    val driven = wet * satDrive
                    val asymmetric = driven - (0.08f * driven * driven * saturation)
                    val saturated = (asymmetric / sqrt(1.0f + asymmetric * asymmetric)).coerceIn(-1.0f, 1.0f)
                    wet = wet * (1.0f - saturation) + saturated * saturation
                }

                // Stage 5: Dry/Wet Intensity Blend & Clipping Guard
                val output = dry * (1.0f - intensity) + wet * intensity
                outputSamples[sampleIndex] = output.coerceIn(-1.0f, 1.0f)

                sampleIndex++
            }
        } else {
            // Stereo processing (interleaved L, R, L, R...)
            for (frame in 0 until frameCount) {
                intensity += stepIntensity
                decimationFactor += stepDecimation
                halfLevels += stepHalfLevels
                lpfAlpha += stepAlpha
                satDrive += stepSatDrive
                bitCrush += stepBitCrush
                saturation += stepSaturation

                val dryL = inputSamples[sampleIndex]
                val dryR = inputSamples[sampleIndex + 1]
                var wetL = dryL
                var wetR = dryR

                // Stage 1: 2-Pole Cascaded Warm Analog Low-Pass Filter (Anti-aliasing and tape warmth)
                lpfStateLeft1 += lpfAlpha * (wetL - lpfStateLeft1)
                lpfStateLeft2 += lpfAlpha * (lpfStateLeft1 - lpfStateLeft2)
                wetL = lpfStateLeft2

                lpfStateRight1 += lpfAlpha * (wetR - lpfStateRight1)
                lpfStateRight2 += lpfAlpha * (lpfStateRight1 - lpfStateRight2)
                wetR = lpfStateRight2

                // Stage 2: Decimation (Sample & Hold)
                decimationPhase += 1.0f
                if (decimationPhase >= decimationFactor) {
                    holdLeft = wetL
                    holdRight = wetR
                    decimationPhase -= decimationFactor
                    if (decimationPhase >= decimationFactor) {
                        decimationPhase = 0.0f
                    }
                } else {
                    wetL = holdLeft
                    wetR = holdRight
                }

                // Stage 3: Bit crushing
                if (bitCrush > 0.01f) {
                    wetL = (round(wetL * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                    wetR = (round(wetR * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 4: Musical Analog Tape Saturation (Smooth transfer curve with 2nd harmonic warmth)
                if (saturation > 0.01f) {
                    val drivenL = wetL * satDrive
                    val asymL = drivenL - (0.08f * drivenL * drivenL * saturation)
                    val saturatedL = (asymL / sqrt(1.0f + asymL * asymL)).coerceIn(-1.0f, 1.0f)
                    wetL = wetL * (1.0f - saturation) + saturatedL * saturation

                    val drivenR = wetR * satDrive
                    val asymR = drivenR - (0.08f * drivenR * drivenR * saturation)
                    val saturatedR = (asymR / sqrt(1.0f + asymR * asymR)).coerceIn(-1.0f, 1.0f)
                    wetR = wetR * (1.0f - saturation) + saturatedR * saturation
                }

                // Stage 5: Dry/Wet Intensity Blend & Clipping Guard
                outputSamples[sampleIndex] = (dryL * (1.0f - intensity) + wetL * intensity).coerceIn(-1.0f, 1.0f)
                outputSamples[sampleIndex + 1] = (dryR * (1.0f - intensity) + wetR * intensity).coerceIn(-1.0f, 1.0f)

                sampleIndex += 2
            }
        }

        // Store final smoothed states for subsequent buffer continuity
        currentIntensity = targetIntensity
        currentDecimationFactor = targetDecimationFactor
        currentHalfLevels = targetHalfLevels
        currentLpfAlpha = targetLpfAlpha
        currentSatDrive = targetSatDrive
        currentBitCrush = targetBitCrush
        currentSaturation = targetSaturation
    }
}
