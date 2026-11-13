package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time Indian chiptune & instrumental procedural audio synthesizer.
 * Uses native AudioTrack in 16-bit PCM Mono @ 22,050 Hz without external recordings.
 */
class ProceduralAudioEngine {
  companion object {
    private const val SAMPLE_RATE = 22050
    private const val TWO_PI = 2.0 * PI
  }

  private var musicTrack: AudioTrack? = null
  private var sfxTrack: AudioTrack? = null
  private val isPlayingMusic = AtomicBoolean(false)
  private var musicThread: Thread? = null

  var isMusicEnabled = true
  var isSfxEnabled = true
  var musicVolume = 0.8f
  var sfxVolume = 0.9f

  // Bilawal Mode frequencies (Hz)
  private val noteC4 = 261.63f
  private val noteD4 = 293.66f
  private val noteE4 = 329.63f
  private val noteF4 = 349.23f
  private val noteG4 = 392.00f
  private val noteA4 = 440.00f
  private val noteB4 = 493.88f
  private val noteC5 = 523.25f
  private val noteD5 = 587.33f
  private val noteE5 = 659.25f
  private val noteG5 = 783.99f

  // 64-step Bilawal melodic adventure phrase (4 bars of 16 steps)
  private val melodySequence = floatArrayOf(
      noteC4, 0f, noteE4, 0f, noteG4, 0f, noteA4, noteG4,
      noteE4, 0f, noteG4, 0f, noteC5, 0f, noteB4, noteA4,
      noteG4, 0f, noteE4, 0f, noteD4, 0f, noteC4, noteD4,
      noteE4, noteG4, noteA4, noteB4, noteC5, 0f, noteG4, 0f,

      noteC5, 0f, noteB4, 0f, noteA4, 0f, noteG4, noteE4,
      noteF4, 0f, noteA4, 0f, noteG4, 0f, noteE4, noteD4,
      noteC4, 0f, noteD4, noteE4, noteG4, 0f, noteA4, 0f,
      noteC5, noteD5, noteC5, noteG4, noteE4, 0f, noteC4, 0f
  )

  // Sitar pluck off-beat counterpoint notes
  private val sitarSequence = floatArrayOf(
      0f, noteG4, 0f, noteE4, 0f, noteC5, 0f, noteG4,
      0f, noteC5, 0f, noteA4, 0f, noteE5, 0f, noteC5,
      0f, noteB4, 0f, noteG4, 0f, noteD5, 0f, noteB4,
      0f, noteC5, 0f, noteG4, 0f, noteE5, 0f, noteC5,

      0f, noteE5, 0f, noteC5, 0f, noteG5, 0f, noteE5,
      0f, noteD5, 0f, noteB4, 0f, noteA4, 0f, noteF4,
      0f, noteG4, 0f, noteE4, 0f, noteC5, 0f, noteG4,
      0f, noteC5, 0f, noteG4, 0f, noteE4, 0f, noteC4
  )

  init {
    initSfxTrack()
  }

  private fun initSfxTrack() {
    try {
      val minBufSize = AudioTrack.getMinBufferSize(
          SAMPLE_RATE,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
      )
      val bufSize = minBufSize.coerceAtLeast(SAMPLE_RATE / 4)
      sfxTrack = AudioTrack.Builder()
          .setAudioAttributes(
              AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                  .build()
          )
          .setAudioFormat(
              AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(SAMPLE_RATE)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
          )
          .setBufferSizeInBytes(bufSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      sfxTrack?.play()
    } catch (_: Exception) {
      // AudioTrack init fallback
    }
  }

  fun startMusic() {
    if (isPlayingMusic.get()) return
    isPlayingMusic.set(true)

    musicThread = Thread({
      try {
        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = minBufSize.coerceAtLeast(SAMPLE_RATE / 2)
        musicTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        musicTrack?.play()

        val tempoBpm = 108
        val stepSamples = (SAMPLE_RATE * 60f / (tempoBpm * 4f)).toInt()
        val buffer = ShortArray(stepSamples)

        var stepIndex = 0
        var sampleCount = 0L

        while (isPlayingMusic.get()) {
          val activeMusic = isMusicEnabled
          val vol = if (activeMusic) musicVolume else 0f

          val melodyFreq = melodySequence[stepIndex % melodySequence.size]
          val sitarFreq = sitarSequence[stepIndex % sitarSequence.size]
          val isBayanBeat = (stepIndex % 8 == 0) || (stepIndex % 8 == 4) // Tabla Bayan bass on beats 1 & 5
          val isDayanClick = (stepIndex % 8 == 2) || (stepIndex % 8 == 6) // Tabla Dayan rim on beats 3 & 7

          for (i in 0 until stepSamples) {
            val t = (sampleCount + i).toDouble() / SAMPLE_RATE
            val stepT = i.toDouble() / stepSamples

            // 1. Tanpura continuous drone (Root C4 & Fifth G4 with subtle detune)
            val drone1 = sin(TWO_PI * noteC4 * t)
            val drone2 = sin(TWO_PI * (noteG4 + 0.35) * t)
            val tanpura = (drone1 * 0.12 + drone2 * 0.08)

            // 2. Bansuri Flute lead with 5.2 Hz vibrato LFO and soft envelope
            var bansuri = 0.0
            if (melodyFreq > 0f) {
              val vibrato = 1.0 + 0.015 * sin(TWO_PI * 5.2 * t)
              val envelope = (1.0 - stepT * 0.4).coerceAtLeast(0.0)
              val fund = sin(TWO_PI * (melodyFreq * vibrato) * t)
              val harm = 0.25 * sin(TWO_PI * (melodyFreq * 2.0 * vibrato) * t)
              bansuri = (fund + harm) * envelope * 0.32
            }

            // 3. Sitar pluck with fast exponential decay
            var sitar = 0.0
            if (sitarFreq > 0f) {
              val decay = Math.exp(-stepT * 6.5)
              val pluck = sin(TWO_PI * sitarFreq * t) + 0.4 * sin(TWO_PI * sitarFreq * 3.0 * t)
              sitar = pluck * decay * 0.22
            }

            // 4. Tabla Bayan (bass pitch sweep from 110 Hz down to 45 Hz)
            var bayan = 0.0
            if (isBayanBeat) {
              val sweepFreq = 45.0 + 65.0 * Math.exp(-stepT * 8.0)
              val bayanEnv = Math.exp(-stepT * 5.0)
              bayan = sin(TWO_PI * sweepFreq * t) * bayanEnv * 0.35
            }

            // 5. Tabla Dayan (crisp wooden high click)
            var dayan = 0.0
            if (isDayanClick && stepT < 0.15) {
              val clickEnv = (1.0 - stepT / 0.15).coerceAtLeast(0.0)
              dayan = sin(TWO_PI * 980.0 * t) * clickEnv * 0.18
            }

            val mixed = (tanpura + bansuri + sitar + bayan + dayan) * vol
            val clamped = mixed.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767).toInt().toShort()
          }

          musicTrack?.write(buffer, 0, stepSamples)
          sampleCount += stepSamples
          stepIndex++
        }
      } catch (_: Exception) {
        // Safe exit on thread interrupt
      }
    }, "ChaosRunMusicThread")
    musicThread?.priority = Thread.NORM_PRIORITY - 1
    musicThread?.start()
  }

  fun pause() {
    isPlayingMusic.set(false)
    try {
      musicTrack?.pause()
    } catch (_: Exception) {}
  }

  fun resume() {
    if (!isPlayingMusic.get()) {
      startMusic()
    }
  }

  fun stop() {
    isPlayingMusic.set(false)
    try {
      musicThread?.interrupt()
      musicThread = null
      musicTrack?.stop()
      musicTrack?.release()
      musicTrack = null
    } catch (_: Exception) {}
  }

  fun release() {
    stop()
    try {
      sfxTrack?.stop()
      sfxTrack?.release()
      sfxTrack = null
    } catch (_: Exception) {}
  }

  private fun playRawSfx(durationMs: Int, generator: (t: Double, progress: Double) -> Double) {
    if (!isSfxEnabled || sfxTrack == null) return
    val samples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
    val buffer = ShortArray(samples)
    val vol = sfxVolume

    for (i in 0 until samples) {
      val t = i.toDouble() / SAMPLE_RATE
      val progress = i.toDouble() / samples
      val sample = generator(t, progress) * vol
      buffer[i] = (sample.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
    }

    try {
      sfxTrack?.write(buffer, 0, samples, AudioTrack.WRITE_NON_BLOCKING)
    } catch (_: Exception) {}
  }

  // Jump: 180 Hz to 520 Hz chirp over 110 ms
  fun playJump() {
    playRawSfx(110) { t, progress ->
      val freq = 180.0 + (520.0 - 180.0) * progress
      val env = 1.0 - progress
      sin(TWO_PI * freq * t) * env * 0.45
    }
  }

  // Land: 100 Hz to 40 Hz damped sine drop over 70 ms
  fun playLand() {
    playRawSfx(70) { t, progress ->
      val freq = 100.0 - 60.0 * progress
      val env = Math.exp(-progress * 5.0)
      sin(TWO_PI * freq * t) * env * 0.5
    }
  }

  // Coin Collect: Dual harmonic chime (B5 988 Hz -> E6 1319 Hz) over 140 ms
  fun playCoin() {
    playRawSfx(140) { t, progress ->
      val freq = if (progress < 0.45) 987.77 else 1318.51
      val env = Math.exp(-(progress % 0.45) * 6.0)
      (sin(TWO_PI * freq * t) + 0.3 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.45
    }
  }

  // Crumble Bridge: Filtered 80 Hz to 30 Hz brownian rumble over 260 ms
  fun playCrumble() {
    playRawSfx(260) { t, progress ->
      val freq = 80.0 - 50.0 * progress
      val env = (1.0 - progress) * (sin(TWO_PI * 18.0 * t) * 0.5 + 0.5)
      sin(TWO_PI * freq * t) * env * 0.6
    }
  }

  // Disappearing Floor: Resonant 720 Hz to 240 Hz crystalline chirp over 140 ms
  fun playDisappear() {
    playRawSfx(140) { t, progress ->
      val freq = 720.0 - 480.0 * progress
      val env = Math.exp(-progress * 4.5)
      sin(TWO_PI * freq * t) * env * 0.45
    }
  }

  // Checkpoint Bell: 4-note ascending chord arpeggio (C5, E5, G5, C6) over 450 ms
  fun playCheckpoint() {
    playRawSfx(450) { t, progress ->
      val noteIndex = (progress * 4).toInt().coerceIn(0, 3)
      val freq = when (noteIndex) {
        0 -> 523.25 // C5
        1 -> 659.25 // E5
        2 -> 783.99 // G5
        else -> 1046.50 // C6
      }
      val env = Math.exp(-progress * 2.5)
      (sin(TWO_PI * freq * t) + 0.2 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.5
    }
  }

  // Damage: 220 Hz to 110 Hz square/triangle buzz over 180 ms
  fun playDamage() {
    playRawSfx(180) { t, progress ->
      val freq = 220.0 - 110.0 * progress
      val env = 1.0 - progress
      val square = if (sin(TWO_PI * freq * t) >= 0) 0.6 else -0.6
      square * env * 0.5
    }
  }

  // Death: Descending sweep (380 Hz down to 80 Hz) over 380 ms
  fun playDeath() {
    playRawSfx(380) { t, progress ->
      val freq = 380.0 - 300.0 * progress
      val env = Math.exp(-progress * 3.5)
      sin(TWO_PI * freq * t) * env * 0.6
    }
  }

  // Victory: Celebratory 5-note fanfare (Sa-Re-Ga-Pa-Sa) over 650 ms
  fun playVictory() {
    playRawSfx(650) { t, progress ->
      val noteIndex = (progress * 5).toInt().coerceIn(0, 4)
      val freq = when (noteIndex) {
        0 -> noteC4.toDouble()
        1 -> noteD4.toDouble()
        2 -> noteE4.toDouble()
        3 -> noteG4.toDouble()
        else -> noteC5.toDouble()
      }
      val env = (1.0 - progress * 0.3)
      (sin(TWO_PI * freq * t) + 0.3 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.5
    }
  }
}
