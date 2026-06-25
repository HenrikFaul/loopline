package com.loopline.game.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates short, pleasant feedback tones at runtime with AudioTrack so the
 * app ships without any bundled audio assets. Each connection plays a blip whose
 * pitch rises along a pentatonic scale for a satisfying "building" feel.
 */
class SoundManager {

    var enabled: Boolean = true

    private val sampleRate = 44100
    private val executor = Executors.newSingleThreadExecutor()

    // Pentatonic-ish semitone offsets so consecutive connects sound musical.
    private val scale = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24)

    fun connect(step: Int) {
        if (!enabled) return
        val semis = scale[step % scale.size] + 12 * (step / scale.size).coerceAtMost(1)
        val freq = 440.0 * Math.pow(2.0, semis / 12.0)
        playTone(freq, 90, 0.35)
    }

    fun undo() {
        if (!enabled) return
        playTone(330.0, 70, 0.25)
    }

    fun win() {
        if (!enabled) return
        // a quick rising arpeggio
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        executor.execute {
            for ((i, f) in notes.withIndex()) {
                renderAndPlay(f, 130, 0.4, fadeFrom = i == notes.size - 1)
            }
        }
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Double) {
        executor.execute { renderAndPlay(freq, durationMs, volume, fadeFrom = true) }
    }

    private fun renderAndPlay(freq: Double, durationMs: Int, volume: Double, fadeFrom: Boolean) {
        val count = sampleRate * durationMs / 1000
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            // exponential decay envelope for a soft "pluck"
            val env = if (fadeFrom) exp(-3.5 * i / count) else 1.0
            // gentle attack to avoid clicks
            val attack = (i / (sampleRate * 0.004)).coerceAtMost(1.0)
            val v = sin(2.0 * PI * freq * t) * env * attack * volume
            samples[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            samples.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        try {
            track.write(samples, 0, samples.size)
            track.play()
            // Let the static buffer finish, then release.
            Thread.sleep(durationMs.toLong() + 30)
        } catch (_: Exception) {
            // Audio is best-effort; never crash gameplay because of it.
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
        }
    }

    fun release() {
        executor.shutdown()
    }
}
