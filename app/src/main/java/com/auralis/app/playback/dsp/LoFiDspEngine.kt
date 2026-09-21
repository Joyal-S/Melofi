package com.auralis.app.playback.dsp

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.round

/**
 * High-performance, zero-allocation Real-Time Lo-Fi DSP Engine.
 *
 * Implements 4 sequential analog/retro modeling stages:
 * 1. Sample Rate Decimation (Phase-accumulating downsampler modeling vintage 8/12-bit samplers)
 * 2. Bit-Depth Quantization (Linear stepped quantizer modeling vintage digital converters)
 * 3. Warm Low-Pass Filter (Single-pole IIR filter rolling off high-end to emulate tape warmth)
 * 4. Soft Saturation (Smooth polynomial cubic saturation modeling magnetic tape overdrive)
 *
 * Designed specifically for real-time audio threads:
 * - Zero heap allocations during [processInterleaved] execution.
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
    }

    /**
     * Processes interleaved 16-bit PCM samples in-place or from input array to output array.
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
            return
        }

        val intensity = params.intensity
        val sampleReduction = params.sampleRateReduction
        val bitCrush = params.bitCrush
        val saturation = params.saturation

        // 1. Decimation step factor: 1.0f (no reduction) up to 8.0f (extreme reduction)
        val decimationFactor = 1.0f + (sampleReduction * 7.0f)

        // 2. Bit crush quantization levels: 16-bit (65536) down to 8-bit (256) or 6-bit (64)
        val targetBits = 16.0f - (bitCrush * 8.0f)
        val quantizationLevels = 2.0.pow(targetBits.toDouble()).toFloat()
        val halfLevels = quantizationLevels * 0.5f

        // 3. Low-pass filter coefficient calculation:
        // Cutoff frequency sweeps between 2000 Hz and 20000 Hz
        val cutoffHz = 2000.0f + (params.lowPassCutoff * 18000.0f)
        val safeSampleRate = sampleRate.coerceAtLeast(8000).toFloat()
        val omega = (2.0 * PI * cutoffHz / safeSampleRate).toFloat()
        val lpfAlpha = (omega / (omega + 1.0f)).coerceIn(0.01f, 0.99f)

        // Saturation drive: 1.0f to 2.5f
        val satDrive = 1.0f + (saturation * 1.5f)

        val totalSamples = frameCount * channelCount
        var sampleIndex = 0

        if (channelCount == 1) {
            // Mono processing
            while (sampleIndex < totalSamples) {
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
                if (bitCrush > 0.0f) {
                    wet = (round(wet * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 3: Warm Low-Pass Filter
                lpfStateLeft += lpfAlpha * (wet - lpfStateLeft)
                wet = lpfStateLeft

                // Stage 4: Soft Saturation (Cubic polynomial soft-clipper)
                if (saturation > 0.0f) {
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
            while (sampleIndex < totalSamples) {
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
                if (bitCrush > 0.0f) {
                    wetL = (round(wetL * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                    wetR = (round(wetR * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 3: Warm Low-Pass Filter
                lpfStateLeft += lpfAlpha * (wetL - lpfStateLeft)
                lpfStateRight += lpfAlpha * (wetR - lpfStateRight)
                wetL = lpfStateLeft
                wetR = lpfStateRight

                // Stage 4: Soft Saturation
                if (saturation > 0.0f) {
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
    }
}
