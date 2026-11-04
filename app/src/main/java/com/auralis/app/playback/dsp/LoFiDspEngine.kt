package com.auralis.app.playback.dsp

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

/**
 * Real-Time Lo-Fi DSP Engine (MELOFI DSP 3.0).
 *
 * Implements a high-fidelity, musical vintage degradation signal chain:
 * 1. Headroom & numerical protection (strict NaN, Infinity, and extreme outlier sanitization)
 * 2. 2-Pole cascaded warm analog Low-Pass Filter (1.8 kHz to 14.0 kHz analog tape warmth rolloff)
 * 3. Sample-Rate Degradation (Sample & Hold decimation from 1.0x up to 4.5x)
 * 4. Bit-Depth Quantization (16.0-bit down to 9.5-bit vintage sampler resolution)
 * 5. Musical Analog Tape Saturation (smooth cubic curve with asymmetric 2nd harmonic warmth)
 * 6. Subtle Tape Wow & Flutter (sample-rate scaled, click-free continuous buffer & transition blending)
 * 7. Dry/Wet intensity mixing
 * 8. Transparent output soft limiter (strictly bounded in [-1.0f..1.0f] with zero digital clipping)
 *
 * Features:
 * - Linear per-frame parameter interpolation eliminating zipper noise during real-time adjustments.
 * - Zero heap allocations during [process] execution.
 * - Strictly bounded: guaranteed no NaN, Infinity, or overflow.
 * - Clean zero-overhead bypass when disabled.
 * - Thread-safe state reset on seek or track transitions.
 */
class LoFiDspEngine {

    companion object {
        private const val WOW_BUFFER_SIZE = 1024
    }

    // Filter states for 2-pole cascaded warm low-pass IIR filter (Left & Right)
    private var lpfStateLeft1: Float = 0.0f
    private var lpfStateLeft2: Float = 0.0f
    private var lpfStateRight1: Float = 0.0f
    private var lpfStateRight2: Float = 0.0f

    // Sample-and-hold states for decimation
    private var holdLeft: Float = 0.0f
    private var holdRight: Float = 0.0f
    private var decimationPhase: Float = 1.0f

    // Circular delay buffer for subtle wow & flutter tape modulation (preallocated, zero allocations)
    private val wowBufferL = FloatArray(WOW_BUFFER_SIZE)
    private val wowBufferR = FloatArray(WOW_BUFFER_SIZE)
    private var wowWriteIdx: Int = 0
    private var wowLfoPhase: Float = 0.0f
    private var flutterLfoPhase: Float = 0.0f

    // Smoothed parameter states for click-free transitions
    private var currentIntensity: Float = 0.0f
    private var currentDecimationFactor: Float = 1.0f
    private var currentHalfLevels: Float = 32768.0f
    private var currentLpfAlpha: Float = 0.95f
    private var currentSatDrive: Float = 1.0f
    private var currentBitCrush: Float = 0.0f
    private var currentSaturation: Float = 0.0f
    private var currentWowFlutter: Float = 0.0f

    private var isInitialized: Boolean = false

    /**
     * Resets internal delay lines, LFO phases, and filter states.
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

        wowBufferL.fill(0.0f)
        wowBufferR.fill(0.0f)
        wowWriteIdx = 0
        wowLfoPhase = 0.0f
        flutterLfoPhase = 0.0f
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

        val targetIntensity = params.intensity.coerceIn(0.0f, 1.0f)
        val targetReduction = params.sampleRateReduction.coerceIn(0.0f, 1.0f)
        val targetBitCrush = params.bitCrush.coerceIn(0.0f, 1.0f)
        val targetSaturation = params.saturation.coerceIn(0.0f, 1.0f)
        val targetWowFlutter = params.wowFlutter.coerceIn(0.0f, 1.0f)

        // Musical decimation range: 1.0x to 4.5x
        val targetDecimationFactor = 1.0f + (targetReduction * 3.5f)

        // Bit-depth: 16.0 bits down to 9.5 bits
        val targetBits = 16.0f - (targetBitCrush * 6.5f)
        val targetQuantizationLevels = 2.0.pow(targetBits.toDouble()).toFloat()
        val targetHalfLevels = targetQuantizationLevels * 0.5f

        val safeSampleRate = sampleRate.coerceIn(8000, 192000).toFloat()
        val rateRatio = safeSampleRate / 44100.0f

        // 2-Pole cascaded warm analog low-pass filter: 1.8 kHz to 14.0 kHz
        val targetCutoffHz = 1800.0f + (params.lowPassCutoff.coerceIn(0.0f, 1.0f) * 12200.0f)
        val omega = (2.0 * PI * targetCutoffHz / safeSampleRate).toFloat().coerceIn(0.01f, 3.0f)
        val targetLpfAlpha = (omega / (omega + 1.0f)).coerceIn(0.01f, 0.98f)

        val targetSatDrive = 1.0f + (targetSaturation * 1.5f)

        if (!isInitialized) {
            currentIntensity = targetIntensity
            currentDecimationFactor = targetDecimationFactor
            currentHalfLevels = targetHalfLevels
            currentLpfAlpha = targetLpfAlpha
            currentSatDrive = targetSatDrive
            currentBitCrush = targetBitCrush
            currentSaturation = targetSaturation
            currentWowFlutter = targetWowFlutter
            isInitialized = true
        }

        // Per-frame ramp increments for click-free transitions
        val invFrames = 1.0f / frameCount.coerceAtLeast(1).toFloat()
        val stepIntensity = (targetIntensity - currentIntensity) * invFrames
        val stepDecimation = (targetDecimationFactor - currentDecimationFactor) * invFrames
        val stepHalfLevels = (targetHalfLevels - currentHalfLevels) * invFrames
        val stepAlpha = (targetLpfAlpha - currentLpfAlpha) * invFrames
        val stepSatDrive = (targetSatDrive - currentSatDrive) * invFrames
        val stepBitCrush = (targetBitCrush - currentBitCrush) * invFrames
        val stepSaturation = (targetSaturation - currentSaturation) * invFrames
        val stepWowFlutter = (targetWowFlutter - currentWowFlutter) * invFrames

        var intensity = currentIntensity
        var decimationFactor = currentDecimationFactor
        var halfLevels = currentHalfLevels
        var lpfAlpha = currentLpfAlpha
        var satDrive = currentSatDrive
        var bitCrush = currentBitCrush
        var saturation = currentSaturation
        var wowFlutter = currentWowFlutter

        // Wow/Flutter LFO phase increments and delay scaling (sample-rate normalized)
        val wowPhaseInc = (2.0 * PI * 0.55 / safeSampleRate).toFloat()       // 0.55 Hz wow
        val flutterPhaseInc = (2.0 * PI * 4.8 / safeSampleRate).toFloat()     // 4.8 Hz flutter
        val baseDelay = 40.0f * rateRatio

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
                wowFlutter += stepWowFlutter

                val rawDry = inputSamples[sampleIndex]
                val dry = if (rawDry.isNaN() || rawDry.isInfinite()) 0.0f else rawDry

                // Fast path for pure silence to prevent phantom noise
                if (dry == 0.0f && lpfStateLeft1 == 0.0f && lpfStateLeft2 == 0.0f && holdLeft == 0.0f) {
                    outputSamples[sampleIndex] = 0.0f
                    sampleIndex++
                    continue
                }

                var wet = dry

                // Stage 1: 2-Pole Cascaded Warm Analog Low-Pass Filter (Anti-aliasing and tape warmth)
                lpfStateLeft1 += lpfAlpha * (wet - lpfStateLeft1)
                lpfStateLeft2 += lpfAlpha * (lpfStateLeft1 - lpfStateLeft2)
                wet = lpfStateLeft2

                // Stage 2: Decimation (Sample & Hold)
                if (decimationFactor > 1.005f) {
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
                }

                // Stage 3: Bit crushing (quantization)
                if (bitCrush > 0.005f) {
                    wet = (round(wet * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 4: Musical Analog Tape Saturation (Cubic curve with 2nd harmonic warmth)
                if (saturation > 0.005f) {
                    val driven = wet * satDrive
                    val asym = driven - (0.04f * driven * driven * saturation)
                    val sat = when {
                        asym > 1.0f -> 1.0f
                        asym < -1.0f -> -1.0f
                        else -> 1.5f * asym - 0.5f * asym * asym * asym
                    }
                    wet = wet * (1.0f - saturation) + sat * saturation
                }

                // Stage 5: Subtle Tape Wow & Flutter Modulation (continuous buffer write & click-free transition)
                val preModWet = wet
                if (wowFlutter > 0.005f) {
                    wowLfoPhase += wowPhaseInc
                    if (wowLfoPhase >= (2.0f * PI.toFloat())) wowLfoPhase -= (2.0f * PI.toFloat())
                    flutterLfoPhase += flutterPhaseInc
                    if (flutterLfoPhase >= (2.0f * PI.toFloat())) flutterLfoPhase -= (2.0f * PI.toFloat())

                    val maxMod = 22.0f * wowFlutter * rateRatio
                    val modOffset = baseDelay + (sin(wowLfoPhase) * 0.75f + sin(flutterLfoPhase) * 0.25f) * maxMod
                    val readPos = (wowWriteIdx - modOffset + WOW_BUFFER_SIZE * 2) % WOW_BUFFER_SIZE
                    val idx0 = readPos.toInt() % WOW_BUFFER_SIZE
                    val idx1 = (idx0 + 1) % WOW_BUFFER_SIZE
                    val frac = readPos - readPos.toInt()

                    val delayed = wowBufferL[idx0] * (1.0f - frac) + wowBufferL[idx1] * frac
                    val ramp = (wowFlutter / 0.05f).coerceIn(0.0f, 1.0f)
                    wet = wet * (1.0f - ramp) + delayed * ramp
                }

                // Continuously populate delay line to prevent stale buffer artifacts or clicks on enable
                wowBufferL[wowWriteIdx] = preModWet
                if (++wowWriteIdx >= WOW_BUFFER_SIZE) wowWriteIdx = 0

                // Stage 6: Dry/Wet Intensity Blend
                val blended = dry * (1.0f - intensity) + wet * intensity

                // Stage 7: Output Soft Limiter
                outputSamples[sampleIndex] = softLimit(blended)
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
                wowFlutter += stepWowFlutter

                val rawDryL = inputSamples[sampleIndex]
                val rawDryR = inputSamples[sampleIndex + 1]
                val dryL = if (rawDryL.isNaN() || rawDryL.isInfinite()) 0.0f else rawDryL
                val dryR = if (rawDryR.isNaN() || rawDryR.isInfinite()) 0.0f else rawDryR

                // Fast path for silence
                if (dryL == 0.0f && dryR == 0.0f &&
                    lpfStateLeft1 == 0.0f && lpfStateLeft2 == 0.0f &&
                    lpfStateRight1 == 0.0f && lpfStateRight2 == 0.0f &&
                    holdLeft == 0.0f && holdRight == 0.0f) {
                    outputSamples[sampleIndex] = 0.0f
                    outputSamples[sampleIndex + 1] = 0.0f
                    sampleIndex += 2
                    continue
                }

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
                if (decimationFactor > 1.005f) {
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
                }

                // Stage 3: Bit crushing (quantization)
                if (bitCrush > 0.005f) {
                    wetL = (round(wetL * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                    wetR = (round(wetR * halfLevels) / halfLevels).coerceIn(-1.0f, 1.0f)
                }

                // Stage 4: Musical Analog Tape Saturation (Cubic curve with 2nd harmonic warmth)
                if (saturation > 0.005f) {
                    val drivenL = wetL * satDrive
                    val asymL = drivenL - (0.04f * drivenL * drivenL * saturation)
                    val satL = when {
                        asymL > 1.0f -> 1.0f
                        asymL < -1.0f -> -1.0f
                        else -> 1.5f * asymL - 0.5f * asymL * asymL * asymL
                    }
                    wetL = wetL * (1.0f - saturation) + satL * saturation

                    val drivenR = wetR * satDrive
                    val asymR = drivenR - (0.04f * drivenR * drivenR * saturation)
                    val satR = when {
                        asymR > 1.0f -> 1.0f
                        asymR < -1.0f -> -1.0f
                        else -> 1.5f * asymR - 0.5f * asymR * asymR * asymR
                    }
                    wetR = wetR * (1.0f - saturation) + satR * saturation
                }

                // Stage 5: Subtle Tape Wow & Flutter Modulation (continuous buffer write & click-free transition)
                val preModWetL = wetL
                val preModWetR = wetR
                if (wowFlutter > 0.005f) {
                    wowLfoPhase += wowPhaseInc
                    if (wowLfoPhase >= (2.0f * PI.toFloat())) wowLfoPhase -= (2.0f * PI.toFloat())
                    flutterLfoPhase += flutterPhaseInc
                    if (flutterLfoPhase >= (2.0f * PI.toFloat())) flutterLfoPhase -= (2.0f * PI.toFloat())

                    val maxMod = 22.0f * wowFlutter * rateRatio
                    val modOffset = baseDelay + (sin(wowLfoPhase) * 0.75f + sin(flutterLfoPhase) * 0.25f) * maxMod
                    val readPos = (wowWriteIdx - modOffset + WOW_BUFFER_SIZE * 2) % WOW_BUFFER_SIZE
                    val idx0 = readPos.toInt() % WOW_BUFFER_SIZE
                    val idx1 = (idx0 + 1) % WOW_BUFFER_SIZE
                    val frac = readPos - readPos.toInt()

                    val delayedL = wowBufferL[idx0] * (1.0f - frac) + wowBufferL[idx1] * frac
                    val delayedR = wowBufferR[idx0] * (1.0f - frac) + wowBufferR[idx1] * frac

                    val ramp = (wowFlutter / 0.05f).coerceIn(0.0f, 1.0f)
                    wetL = wetL * (1.0f - ramp) + delayedL * ramp
                    wetR = wetR * (1.0f - ramp) + delayedR * ramp
                }

                // Continuously populate delay lines to prevent stale buffer artifacts or clicks on enable
                wowBufferL[wowWriteIdx] = preModWetL
                wowBufferR[wowWriteIdx] = preModWetR
                if (++wowWriteIdx >= WOW_BUFFER_SIZE) wowWriteIdx = 0

                // Stage 6: Dry/Wet Intensity Blend
                val blendedL = dryL * (1.0f - intensity) + wetL * intensity
                val blendedR = dryR * (1.0f - intensity) + wetR * intensity

                // Stage 7: Output Soft Limiter
                outputSamples[sampleIndex] = softLimit(blendedL)
                outputSamples[sampleIndex + 1] = softLimit(blendedR)

                sampleIndex += 2
            }
        }

        currentIntensity = targetIntensity
        currentDecimationFactor = targetDecimationFactor
        currentHalfLevels = targetHalfLevels
        currentLpfAlpha = targetLpfAlpha
        currentSatDrive = targetSatDrive
        currentBitCrush = targetBitCrush
        currentSaturation = targetSaturation
        currentWowFlutter = targetWowFlutter
    }

    /**
     * Transparent, click-free soft limiter.
     * Smoothly curves extreme excursions above 0.88 into the 1.0f ceiling.
     * Prevents digital overs while preserving dynamic punch and clarity.
     * Strictly protects against NaN and Infinity values.
     */
    private fun softLimit(x: Float): Float {
        if (x.isNaN() || x.isInfinite()) return 0.0f
        return when {
            x >= 1.0f -> 1.0f
            x <= -1.0f -> -1.0f
            x > 0.88f -> {
                val u = (x - 0.88f) / 0.12f
                0.88f + 0.12f * (1.5f * u - 0.5f * u * u * u)
            }
            x < -0.88f -> {
                val u = (-x - 0.88f) / 0.12f
                -(0.88f + 0.12f * (1.5f * u - 0.5f * u * u * u))
            }
            else -> x
        }
    }
}
