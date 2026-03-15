package com.mergeblast.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.*

/**
 * SoundManager — all sounds generated programmatically via AudioTrack.
 * No audio asset files needed.
 *
 * Sound events:
 *  playDrop()        — soft thud when block lands (no match)
 *  playBlast()       — punchy impact when block lands ON matching neighbours
 *  playMerge()       — clean rising ding on merge result (1st merge)
 *  playDoubleMerge() — two-note chime on 2nd cascade
 *  playTripleMerge() — triumphant chord on 3rd+ cascade
 *  playCombo()       — full arpeggio on big combo (4+ blocks)
 *  playGameOver()    — descending sad tones
 *  playButton()      — quick UI click
 */
class SoundManager(private val context: Context) {

    private val sampleRate = 44100
    var soundEnabled     = true
    var vibrationEnabled = true

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // ── PCM generation helpers ────────────────────────────────────────────────

    /** Sine wave with attack/release envelope */
    private fun sineWave(
        freq: Double, durationMs: Int,
        volume: Float = 0.6f,
        attackMs: Int = 5, releaseMs: Int = 40
    ): ShortArray {
        val samples        = (sampleRate * durationMs / 1000.0).toInt()
        val attackSamples  = (sampleRate * attackMs  / 1000.0).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleRate * releaseMs / 1000.0).toInt().coerceAtLeast(1)
        return ShortArray(samples) { i ->
            val t   = i.toDouble() / sampleRate
            var amp = volume
            if (i < attackSamples)           amp *= i.toFloat() / attackSamples
            if (i > samples - releaseSamples) amp *= (samples - i).toFloat() / releaseSamples
            (amp * Short.MAX_VALUE * sin(2.0 * PI * freq * t)).toInt().toShort()
        }
    }

    /** Noise burst — punch/impact character */
    private fun noiseWave(durationMs: Int, volume: Float = 0.3f, releaseMs: Int = 40): ShortArray {
        val samples        = (sampleRate * durationMs / 1000.0).toInt()
        val releaseSamples = (sampleRate * releaseMs / 1000.0).toInt().coerceAtLeast(1)
        return ShortArray(samples) { i ->
            var amp = volume
            if (i > samples - releaseSamples) amp *= (samples - i).toFloat() / releaseSamples
            (amp * Short.MAX_VALUE * (Math.random() * 2 - 1)).toInt().toShort()
        }
    }

    /** Mix multiple PCM arrays sample-by-sample (auto-pads shorter arrays) */
    private fun mix(vararg bufs: ShortArray): ShortArray {
        val len = bufs.maxOf { it.size }
        return ShortArray(len) { i ->
            val sum = bufs.sumOf { if (i < it.size) it[i].toInt() else 0 }
            (sum / bufs.size).toShort()
        }
    }

    /** Concatenate PCM arrays */
    private fun concat(vararg bufs: ShortArray): ShortArray {
        val total  = bufs.sumOf { it.size }
        val result = ShortArray(total)
        var pos    = 0
        for (b in bufs) { b.copyInto(result, pos); pos += b.size }
        return result
    }

    /** Play PCM on a background thread — fire and forget */
    private fun playPcm(pcm: ShortArray) {
        if (!soundEnabled) return
        Thread {
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(pcm.size * 2, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((pcm.size.toLong() * 1000L / sampleRate) + 80L)
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }.also { it.isDaemon = true; it.start() }
    }

    // ── Public sound events ───────────────────────────────────────────────────

    /**
     * Block lands but NO match with neighbours.
     * Soft low thud — simple placement.
     */
    fun playDrop() {
        val thud = mix(
            sineWave(160.0, 80, 0.40f, attackMs = 2, releaseMs = 70),
            noiseWave(40, 0.15f, releaseMs = 35)
        )
        playPcm(thud)
        vibrateShort(15)
    }

    /**
     * Block lands ON matching neighbours — "BLAST" moment.
     * Punchy pop + rising note to signal "something is about to happen".
     */
    fun playBlast() {
        val pop  = noiseWave(35, 0.45f, releaseMs = 30)
        val tone = sineWave(440.0, 120, 0.55f, attackMs = 3, releaseMs = 100)
        val pcm  = mix(
            concat(pop, ShortArray(tone.size - pop.size)),
            tone
        )
        playPcm(pcm)
        vibrateShort(30)
    }

    /**
     * 1st merge result — clean rising ding.
     * C5 → E5 two-note lift.
     */
    fun playMerge() {
        val pcm = concat(
            sineWave(523.25, 40,  0.60f, attackMs = 3, releaseMs = 30),  // C5 short hit
            sineWave(659.25, 140, 0.65f, attackMs = 4, releaseMs = 110)  // E5 sustain
        )
        playPcm(pcm)
        vibrateShort(28)
    }

    /**
     * 2nd cascade merge — two-note rising chime, higher and brighter.
     * E5 → G5 → C6
     */
    fun playDoubleMerge() {
        val pcm = concat(
            sineWave(659.25, 40,  0.62f, attackMs = 2, releaseMs = 30),  // E5
            sineWave(783.99, 40,  0.62f, attackMs = 2, releaseMs = 30),  // G5
            sineWave(1046.5, 160, 0.70f, attackMs = 4, releaseMs = 130)  // C6
        )
        playPcm(pcm)
        vibrateShort(40)
    }

    /**
     * 3rd+ cascade — triumphant major chord C+E+G mixed together.
     */
    fun playTripleMerge() {
        val len = (sampleRate * 0.22).toInt()
        val pcm = mix(
            sineWave(523.25, 220, 0.55f, attackMs = 3, releaseMs = 160),  // C5
            sineWave(659.25, 220, 0.55f, attackMs = 3, releaseMs = 160),  // E5
            sineWave(783.99, 220, 0.55f, attackMs = 3, releaseMs = 160)   // G5
        )
        playPcm(pcm)
        vibrateMerge()
    }

    /**
     * Big combo (4+ blocks merged) — 5-note ascending arpeggio.
     */
    fun playCombo() {
        val notes = listOf(523.25, 659.25, 783.99, 1046.5, 1318.5)  // C5 E5 G5 C6 E6
        val pcm   = concat(*notes.map { f ->
            sineWave(f, 75, 0.62f, attackMs = 3, releaseMs = 55)
        }.toTypedArray())
        playPcm(pcm)
        vibrateCombo()
    }

    /**
     * Game over — descending A→F→C sad tones.
     */
    fun playGameOver() {
        val pcm = concat(
            sineWave(440.0,  180, 0.65f, attackMs = 5, releaseMs = 40),   // A4
            sineWave(349.23, 180, 0.65f, attackMs = 5, releaseMs = 40),   // F4
            sineWave(261.63, 340, 0.75f, attackMs = 5, releaseMs = 300)   // C4
        )
        playPcm(pcm)
    }

    /**
     * Quick UI click for buttons / toggles.
     */
    fun playButton() {
        playPcm(sineWave(900.0, 45, 0.28f, attackMs = 2, releaseMs = 35))
    }

    // ── Vibration helpers ─────────────────────────────────────────────────────

    private fun vibrateShort(ms: Long) {
        if (!vibrationEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun vibrateMerge() {
        if (!vibrationEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun vibrateCombo() {
        if (!vibrationEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 30, 80, 30, 120),
                    intArrayOf(0, 120, 0, 200, 0, 255),
                    -1
                )
            )
        }
    }

    fun release() {}
}