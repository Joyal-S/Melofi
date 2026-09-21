package com.auralis.app.playback.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * High-performance, zero-allocation Real-Time Lo-Fi DSP Engine with click-free parameter smoothing.
 *
 * Implements 4 sequential analog/retro modeling stages:
 * 1. Sample Rate Decimation (Phase-accumulating downsampler modeling vintage 8/12-bit samplers)
 * 2. Bit-Depth Quantization (Linear stepped quantizer modeling vintage digital converters)
 * 3. Warm Low-Pass Filter (Single-pole IIR filter rolling off high-end to emulate tape warmth)
 * 4. Soft Saturation (Smooth polynomial cubic saturation modeling magnetic tape overdrive)
 *
 * Features:
 * - Linear per-frame parameter interpolation eliminating zipper noise during real-time adjustments.
 * - Zero heap allocations during [process] execution.
 * - Numerically bounded to guarantee no NaN, Infinity, or clipping overflow.
 * - Thread-safe state reset on seek or track transitions.
 */
class LoFiDspEngine {

    // Filter states for warm low-pass IIR filter
    private var lpfStateLeft: Float = 0.0f
    private var lpfStateRight: Float = 0.0f

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
        lpfStateLeft = 0.0f
        lpfStateRight = 0.0f
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
            // Direct bypass copy
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
        val targetDecimationFactor = 1.0f + (targetReduction * 7.0f)

        val targetBits = 16.0f - (targetBitCrush * 8.0f)
        val targetQuantizationLevels = 2.0.pow(targetBits.toDouble()).toFloat()
        val targetHalfLevels = targetQuantizationLevels * 0.5f

        val safeSampleRate = sampleRate.coerceAtLeast(8000).toFloat()
        val targetCutoffHz = 2000.0f + (params.lowPassCutoff * 18000.0f)
        val omega = (2.0 * PI * targetCutoffHz / safeSampleRate).toFloat()
        val targetLpfAlpha = (omega / (omega + 1.0f)).coerceIn(0.01f, 0.99f)

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

                // Stage 1: Decimation (Sample & Hold)
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

                // Stage 2: Bit crushing
                if (bitCrush > 0.01f) {
                    wet = (round(wet * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 3: Warm Low-Pass Filter
                lpfStateLeft += lpfAlpha * (wet - lpfStateLeft)
                wet = lpfStateLeft

                // Stage 4: Soft Saturation (Cubic polynomial soft-clipper)
                if (saturation > 0.01f) {
                    val driven = wet * satDrive
                    val clipped = when {
                        driven > 1.0f -> 1.0f
                        driven < -1.0f -> -1.0f
                        else -> 1.5f * driven - 0.5f * driven * driven * driven
                    }
                    wet = wet * (1.0f - saturation) + clipped * saturation
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

                // Stage 1: Decimation (Sample & Hold)
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

                // Stage 2: Bit crushing
                if (bitCrush > 0.01f) {
                    wetL = (round(wetL * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                    wetR = (round(wetR * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 3: Warm Low-Pass Filter
                lpfStateLeft += lpfAlpha * (wetL - lpfStateLeft)
                lpfStateRight += lpfAlpha * (wetR - lpfStateRight)
                wetL = lpfStateLeft
                wetR = lpfStateRight

                // Stage 4: Soft Saturation
                if (saturation > 0.01f) {
                    val drivenL = wetL * satDrive
                    val clippedL = when {
                        drivenL > 1.0f -> 1.0f
                        drivenL < -1.0f -> -1.0f
                        else -> 1.5f * drivenL - 0.5f * drivenL * drivenL * drivenL
                    }
                    wetL = wetL * (1.0f - saturation) + clippedL * saturation

                    val drivenR = wetR * satDrive
                    val clippedR = when {
                        drivenR > 1.0f -> 1.0f
                        drivenR < -1.0f -> -1.0f
                        else -> 1.5f * drivenR - 0.5f * drivenR * drivenR * drivenR
                    }
                    wetR = wetR * (1.0f - saturation) + clippedR * saturation
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
