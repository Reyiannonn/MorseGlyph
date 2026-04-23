package com.morseglyph.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class AudioController {

    private val sampleRate = 44100
    private val frequency = 700.0
    private val amplitude = 0.7

    var available = true
        private set

    suspend fun beep(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        withContext(Dispatchers.IO) {
            val numSamples = (sampleRate * durationMs / 1000.0).toInt()
            val buffer = buildBuffer(numSamples)
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs)
                track.stop()
                track.release()
            } catch (e: Exception) {
                available = false
            }
        }
    }

    private fun buildBuffer(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples) { i ->
            val angle = 2.0 * PI * i.toDouble() * frequency / sampleRate
            (sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
        }
        val fadeSamples = min((sampleRate * 0.005).toInt(), numSamples / 4)
        for (i in 0 until fadeSamples) {
            val fade = i.toFloat() / fadeSamples
            buffer[i] = (buffer[i] * fade).toInt().toShort()
            buffer[numSamples - 1 - i] = (buffer[numSamples - 1 - i] * fade).toInt().toShort()
        }
        return buffer
    }
}
