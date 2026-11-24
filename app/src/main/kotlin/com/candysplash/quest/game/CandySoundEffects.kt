package com.candysplash.quest.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class CandySoundEffects {
  private val scope = CoroutineScope(Dispatchers.Default)
  var isMuted: Boolean = false

  fun playPop(comboStep: Int = 1) {
    if (isMuted) return
    // Ascending pentatonic notes for cascading combos!
    val scaleFreqs = listOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.50, 1174.66)
    val freq = scaleFreqs[(comboStep - 1).coerceIn(0, scaleFreqs.lastIndex)]
    playChime(listOf(freq to 85, (freq * 1.25) to 110))
  }

  fun playSpecialBlast() {
    if (isMuted) return
    playChime(listOf(587.33 to 70, 783.99 to 70, 987.77 to 80, 1174.66 to 150))
  }

  fun playInvalidMove() {
    if (isMuted) return
    playChime(listOf(280.0 to 80, 220.0 to 110))
  }

  fun playLevelWin() {
    if (isMuted) return
    playChime(
      listOf(
        523.25 to 100,
        659.25 to 100,
        783.99 to 100,
        1046.50 to 240,
      )
    )
  }

  private fun playChime(notes: List<Pair<Double, Int>>) {
    scope.launch {
      try {
        val sampleRate = 22050
        val totalSamples = notes.sumOf { (it.second * sampleRate) / 1000 }
        if (totalSamples <= 0) return@launch

        val buffer = ShortArray(totalSamples)
        var offset = 0
        for ((freq, durationMs) in notes) {
          val count = (durationMs * sampleRate) / 1000
          for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-4.5 * i.toDouble() / count)
            val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)
            val sample = (wave * envelope * 9000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[offset + i] = sample.toShort()
          }
          offset += count
        }

        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(buffer.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(buffer, 0, buffer.size)
        track.play()
      } catch (_: Throwable) {
        // Ignore audio errors on headless/preview renders
      }
    }
  }
}
