package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.PI
import kotlin.math.sin

enum class AudioLifecycleState {
  IDLE,
  PLAYING,
  PAUSED,
  STOPPED,
  RELEASED
}

interface AudioStreamHandle {
  fun play()
  fun pause()
  fun stop()
  fun release()
  fun writeBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int
  fun writeNonBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int
}

interface AudioTrackBackend {
  fun createMusicTrack(sampleRate: Int): AudioStreamHandle?
  fun createSfxTrack(sampleRate: Int): AudioStreamHandle?
}

interface AudioFocusController {
  fun requestFocus(): Boolean
  fun abandonFocus()
}

private class SystemAudioStreamHandle(private val track: AudioTrack) : AudioStreamHandle {
  override fun play() {
    track.play()
  }

  override fun pause() {
    track.pause()
  }

  override fun stop() {
    track.stop()
  }

  override fun release() {
    track.release()
  }

  override fun writeBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
    return track.write(buffer, offsetInShorts, sizeInShorts, AudioTrack.WRITE_BLOCKING)
  }

  override fun writeNonBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
    return track.write(buffer, offsetInShorts, sizeInShorts, AudioTrack.WRITE_NON_BLOCKING)
  }
}

class SystemAudioTrackBackend : AudioTrackBackend {
  override fun createMusicTrack(sampleRate: Int): AudioStreamHandle? {
    return try {
      val minBufSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
      )
      val bufSize = minBufSize.coerceAtLeast(sampleRate / 2)
      val track = AudioTrack.Builder()
          .setAudioAttributes(
              AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
          )
          .setAudioFormat(
              AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
          )
          .setBufferSizeInBytes(bufSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      SystemAudioStreamHandle(track)
    } catch (_: Exception) {
      null
    }
  }

  override fun createSfxTrack(sampleRate: Int): AudioStreamHandle? {
    return try {
      val minBufSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
      )
      val bufSize = minBufSize.coerceAtLeast(sampleRate / 4)
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
          .setBufferSizeInBytes(bufSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      SystemAudioStreamHandle(track)
    } catch (_: Exception) {
      null
    }
  }
}

private class PlatformAudioFocusController(
    context: Context,
    private val listener: (Int) -> Unit
) : AudioFocusController {
  private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> listener(change) }
  private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(focusListener)
        .build()
  } else {
    null
  }

  override fun requestFocus(): Boolean {
    val am = audioManager ?: return true
    return try {
      val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
        am.requestAudioFocus(focusRequest)
      } else {
        @Suppress("DEPRECATION")
        am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
      }
      res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } catch (_: Exception) {
      true
    }
  }

  override fun abandonFocus() {
    val am = audioManager ?: return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
        am.abandonAudioFocusRequest(focusRequest)
      } else {
        @Suppress("DEPRECATION")
        am.abandonAudioFocus(focusListener)
      }
    } catch (_: Exception) {}
  }
}

/**
 * Real-time Indian chiptune & instrumental procedural audio synthesizer.
 * Uses native AudioTrack in 16-bit PCM Mono @ 22,050 Hz without external recordings.
 * Thread-safe, idempotent AudioTrack lifecycle & AudioFocus ownership.
 */
class ProceduralAudioEngine(
    private val trackBackend: AudioTrackBackend = SystemAudioTrackBackend(),
    context: Context? = null,
    focusControllerOverride: AudioFocusController? = null
) {
  companion object {
    private const val SAMPLE_RATE = 22050
    private const val TWO_PI = 2.0 * PI
    private const val THREAD_JOIN_TIMEOUT_MS = 600L
  }

  private val transitionLock = Any()
  private val lifecycleLock = Any()
  private val pauseSignalLock = Object()

  private var lifecycleState = AudioLifecycleState.IDLE
  private var musicTrack: AudioStreamHandle? = null
  private var sfxTrack: AudioStreamHandle? = null
  private var musicThread: Thread? = null
  private var musicGeneration = 0L

  @Volatile private var isHostBackgrounded = false
  @Volatile private var hasAudioFocus = true
  @Volatile private var isFocusDucked = false

  private val focusController: AudioFocusController? =
      focusControllerOverride ?: context?.let { PlatformAudioFocusController(it) { change -> onAudioFocusChange(change) } }

  @Volatile var isMusicEnabled = true
  @Volatile var isSfxEnabled = true
  @Volatile var musicVolume = 0.8f
  @Volatile var sfxVolume = 0.9f

  val currentLifecycleState: AudioLifecycleState
    get() = synchronized(lifecycleLock) { lifecycleState }

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
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED || sfxTrack != null) return
      val track = trackBackend.createSfxTrack(SAMPLE_RATE) ?: return
      sfxTrack = track
      try {
        if (!isHostBackgrounded && hasAudioFocus) {
          track.play()
        }
      } catch (_: Exception) {
        // Fallback if track fails to enter play state in test/headless environment
      }
    }
  }

  fun startMusic() {
    synchronized(transitionLock) {
      startMusicLocked()
    }
  }

  private fun startMusicLocked() {
    // Ensure any stale/stopped music thread has exited cleanly before checking/allocating.
    val staleThreadToJoin: Thread?
    val staleTrackToRelease: AudioStreamHandle?
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED) return
      isHostBackgrounded = false

      if (musicThread?.isAlive == true && musicTrack != null) {
        lifecycleState = AudioLifecycleState.PLAYING
        hasAudioFocus = focusController?.requestFocus() ?: true
        if (hasAudioFocus) {
          try {
            musicTrack?.play()
          } catch (_: Exception) {}
          try {
            sfxTrack?.play()
          } catch (_: Exception) {}
        }
        synchronized(pauseSignalLock) { pauseSignalLock.notifyAll() }
        return
      }

      staleThreadToJoin = musicThread.also { musicThread = null }
      staleTrackToRelease = musicTrack.also { musicTrack = null }
      musicGeneration++
    }

    cleanupMusicResources(staleThreadToJoin, staleTrackToRelease)

    val newThread: Thread?
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED) return
      if (musicThread?.isAlive == true && musicTrack != null) return

      hasAudioFocus = focusController?.requestFocus() ?: true
      val createdTrack = trackBackend.createMusicTrack(SAMPLE_RATE) ?: return
      musicTrack = createdTrack
      if (hasAudioFocus && !isHostBackgrounded) {
        try {
          createdTrack.play()
        } catch (_: Exception) {}
      }
      if (hasAudioFocus && !isHostBackgrounded) {
        try {
          sfxTrack?.play()
        } catch (_: Exception) {}
      }
      lifecycleState = AudioLifecycleState.PLAYING
      val gen = ++musicGeneration
      newThread = Thread({ runMusicLoop(gen, createdTrack) }, "ChaosRunMusicThread").apply {
        priority = Thread.NORM_PRIORITY - 1
      }
      musicThread = newThread
    }
    newThread?.start()
  }

  private fun runMusicLoop(generation: Long, track: AudioStreamHandle) {
    try {
      val tempoBpm = 108
      val stepSamples = (SAMPLE_RATE * 60f / (tempoBpm * 4f)).toInt()
      val buffer = ShortArray(stepSamples)

      var stepIndex = 0
      var sampleCount = 0L

      while (!Thread.currentThread().isInterrupted) {
        var shouldPauseWait = false
        synchronized(lifecycleLock) {
          if (lifecycleState == AudioLifecycleState.RELEASED ||
              lifecycleState == AudioLifecycleState.STOPPED ||
              musicGeneration != generation ||
              musicTrack !== track) {
            return
          }
          if (lifecycleState == AudioLifecycleState.PAUSED || isHostBackgrounded || !hasAudioFocus) {
            shouldPauseWait = true
          }
        }

        if (shouldPauseWait) {
          synchronized(pauseSignalLock) {
            val stillPaused = synchronized(lifecycleLock) {
              lifecycleState == AudioLifecycleState.PAUSED ||
                  ((isHostBackgrounded || !hasAudioFocus) &&
                      lifecycleState != AudioLifecycleState.STOPPED &&
                      lifecycleState != AudioLifecycleState.RELEASED &&
                      musicGeneration == generation)
            }
            if (stillPaused) {
              try {
                pauseSignalLock.wait(200L)
              } catch (_: InterruptedException) {
                return
              }
            }
          }
          continue
        }

        val activeMusic = isMusicEnabled
        val duckFactor = if (isFocusDucked) 0.25f else 1.0f
        val vol = if (activeMusic) musicVolume * duckFactor else 0f

        val melodyFreq = melodySequence[stepIndex % melodySequence.size]
        val sitarFreq = sitarSequence[stepIndex % sitarSequence.size]
        val isBayanBeat = (stepIndex % 8 == 0) || (stepIndex % 8 == 4)
        val isDayanClick = (stepIndex % 8 == 2) || (stepIndex % 8 == 6)

        for (i in 0 until stepSamples) {
          val t = (sampleCount + i).toDouble() / SAMPLE_RATE
          val stepT = i.toDouble() / stepSamples

          val drone1 = sin(TWO_PI * noteC4 * t)
          val drone2 = sin(TWO_PI * (noteG4 + 0.35) * t)
          val tanpura = (drone1 * 0.12 + drone2 * 0.08)

          var bansuri = 0.0
          if (melodyFreq > 0f) {
            val vibrato = 1.0 + 0.015 * sin(TWO_PI * 5.2 * t)
            val envelope = (1.0 - stepT * 0.4).coerceAtLeast(0.0)
            val fund = sin(TWO_PI * (melodyFreq * vibrato) * t)
            val harm = 0.25 * sin(TWO_PI * (melodyFreq * 2.0 * vibrato) * t)
            bansuri = (fund + harm) * envelope * 0.32
          }

          var sitar = 0.0
          if (sitarFreq > 0f) {
            val decay = Math.exp(-stepT * 6.5)
            val pluck = sin(TWO_PI * sitarFreq * t) + 0.4 * sin(TWO_PI * sitarFreq * 3.0 * t)
            sitar = pluck * decay * 0.22
          }

          var bayan = 0.0
          if (isBayanBeat) {
            val sweepFreq = 45.0 + 65.0 * Math.exp(-stepT * 8.0)
            val bayanEnv = Math.exp(-stepT * 5.0)
            bayan = sin(TWO_PI * sweepFreq * t) * bayanEnv * 0.35
          }

          var dayan = 0.0
          if (isDayanClick && stepT < 0.15) {
            val clickEnv = (1.0 - stepT / 0.15).coerceAtLeast(0.0)
            dayan = sin(TWO_PI * 980.0 * t) * clickEnv * 0.18
          }

          val mixed = (tanpura + bansuri + sitar + bayan + dayan) * vol
          val clamped = mixed.coerceIn(-1.0, 1.0)
          buffer[i] = (clamped * 32767).toInt().toShort()
        }

        val written = track.writeBlocking(buffer, 0, stepSamples)
        if (written < 0) {
          // Track stopped or invalid; exit loop safely.
          break
        }
        sampleCount += stepSamples
        stepIndex++
      }
    } catch (_: InterruptedException) {
      // Normal termination
    } catch (_: Exception) {
      // Safe exit on AudioTrack interruption/stop
    }
  }

  fun pause() {
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED ||
          lifecycleState == AudioLifecycleState.STOPPED ||
          lifecycleState == AudioLifecycleState.PAUSED) {
        return
      }
      isHostBackgrounded = true
      lifecycleState = AudioLifecycleState.PAUSED
      try {
        musicTrack?.pause()
      } catch (_: Exception) {}
      try {
        sfxTrack?.pause()
      } catch (_: Exception) {}
      focusController?.abandonFocus()
    }
    synchronized(pauseSignalLock) { pauseSignalLock.notifyAll() }
  }

  fun resume() {
    val shouldStartFresh: Boolean
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED) return
      isHostBackgrounded = false
      shouldStartFresh = (musicThread?.isAlive != true || musicTrack == null ||
          lifecycleState == AudioLifecycleState.STOPPED ||
          lifecycleState == AudioLifecycleState.IDLE)
      if (!shouldStartFresh) {
        hasAudioFocus = focusController?.requestFocus() ?: true
        isFocusDucked = false
        lifecycleState = AudioLifecycleState.PLAYING
        if (hasAudioFocus) {
          try {
            musicTrack?.play()
          } catch (_: Exception) {}
          try {
            sfxTrack?.play()
          } catch (_: Exception) {}
        }
      }
    }
    if (shouldStartFresh) {
      startMusic()
    } else {
      synchronized(pauseSignalLock) { pauseSignalLock.notifyAll() }
    }
  }

  fun onAudioFocusChange(focusChange: Int) {
    when (focusChange) {
      AudioManager.AUDIOFOCUS_GAIN -> {
        synchronized(lifecycleLock) {
          if (lifecycleState == AudioLifecycleState.RELEASED) return
          hasAudioFocus = true
          isFocusDucked = false
          if (!isHostBackgrounded && lifecycleState == AudioLifecycleState.PLAYING) {
            try {
              musicTrack?.play()
            } catch (_: Exception) {}
            try {
              sfxTrack?.play()
            } catch (_: Exception) {}
          }
        }
        synchronized(pauseSignalLock) { pauseSignalLock.notifyAll() }
      }
      AudioManager.AUDIOFOCUS_LOSS,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        synchronized(lifecycleLock) {
          if (lifecycleState == AudioLifecycleState.RELEASED) return
          hasAudioFocus = false
          isFocusDucked = false
          try {
            musicTrack?.pause()
          } catch (_: Exception) {}
          try {
            sfxTrack?.pause()
          } catch (_: Exception) {}
        }
        synchronized(pauseSignalLock) { pauseSignalLock.notifyAll() }
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        isFocusDucked = true
      }
    }
  }

  fun stop() {
    synchronized(transitionLock) {
      stopLocked()
    }
  }

  private fun stopLocked() {
    val threadToJoin: Thread?
    val trackToRelease: AudioStreamHandle?
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED) return
      musicGeneration++
      lifecycleState = AudioLifecycleState.STOPPED
      threadToJoin = musicThread.also { musicThread = null }
      trackToRelease = musicTrack.also { musicTrack = null }
      focusController?.abandonFocus()
    }
    cleanupMusicResources(threadToJoin, trackToRelease)
  }

  fun release() {
    synchronized(transitionLock) {
      releaseLocked()
    }
  }

  private fun releaseLocked() {
    val threadToJoin: Thread?
    val trackToRelease: AudioStreamHandle?
    val sfxToRelease: AudioStreamHandle?
    synchronized(lifecycleLock) {
      if (lifecycleState == AudioLifecycleState.RELEASED) return
      musicGeneration++
      lifecycleState = AudioLifecycleState.RELEASED
      threadToJoin = musicThread.also { musicThread = null }
      trackToRelease = musicTrack.also { musicTrack = null }
      sfxToRelease = sfxTrack.also { sfxTrack = null }
      focusController?.abandonFocus()
    }
    cleanupMusicResources(threadToJoin, trackToRelease)
    if (sfxToRelease != null) {
      try {
        sfxToRelease.pause()
      } catch (_: Exception) {}
      try {
        sfxToRelease.stop()
      } catch (_: Exception) {}
      try {
        sfxToRelease.release()
      } catch (_: Exception) {}
    }
  }

  private fun cleanupMusicResources(thread: Thread?, track: AudioStreamHandle?) {
    if (track != null) {
      try {
        track.pause()
      } catch (_: Exception) {}
      try {
        track.stop()
      } catch (_: Exception) {}
    }
    thread?.interrupt()
    synchronized(pauseSignalLock) { pauseSignalLock.notifyAll() }
    if (thread != null && thread !== Thread.currentThread()) {
      try {
        thread.join(THREAD_JOIN_TIMEOUT_MS)
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }
    if (track != null) {
      try {
        track.release()
      } catch (_: Exception) {}
    }
  }

  fun isMusicThreadAlive(): Boolean = synchronized(lifecycleLock) { musicThread?.isAlive == true }

  fun hasActiveMusicTrack(): Boolean = synchronized(lifecycleLock) { musicTrack != null }

  fun hasActiveSfxTrack(): Boolean = synchronized(lifecycleLock) { sfxTrack != null }

  private fun playRawSfx(durationMs: Int, generator: (t: Double, progress: Double) -> Double) {
    val activeTrack: AudioStreamHandle
    synchronized(lifecycleLock) {
      if (!isSfxEnabled ||
          lifecycleState == AudioLifecycleState.RELEASED ||
          isHostBackgrounded ||
          !hasAudioFocus) {
        return
      }
      activeTrack = sfxTrack ?: return
    }
    val samples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
    val buffer = ShortArray(samples)
    val duckFactor = if (isFocusDucked) 0.35f else 1.0f
    val vol = sfxVolume * duckFactor

    for (i in 0 until samples) {
      val t = i.toDouble() / SAMPLE_RATE
      val progress = i.toDouble() / samples
      val sample = generator(t, progress) * vol
      buffer[i] = (sample.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
    }

    try {
      activeTrack.writeNonBlocking(buffer, 0, samples)
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

  // Clue Discovery: Soft, warm wooden-bell harmonic chime (E5 -> B5) over 240 ms
  fun playClueDiscovery() {
    playRawSfx(240) { t, progress ->
      val freq = if (progress < 0.5) 659.25 else 987.77
      val env = Math.exp(-progress * 4.0) * (1.0 - progress * 0.2)
      (sin(TWO_PI * freq * t) + 0.2 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.28
    }
  }

  // Resonance Hum: Gentle crystalline breath chime (G4 + D5 fifth) over 280 ms
  fun playResonanceHum() {
    playRawSfx(280) { t, progress ->
      val env = sin(PI * progress) * 0.26
      (sin(TWO_PI * 392.0 * t) + 0.35 * sin(TWO_PI * 587.33 * t)) * env
    }
  }

  // Distant Bell Cue: Muffled far-off shrine chime (D5 + A4) for Echoing Bridge warning over 320 ms
  fun playDistantBellCue() {
    playRawSfx(320) { t, progress ->
      val env = Math.exp(-progress * 3.8) * (1.0 - progress * 0.25)
      (sin(TWO_PI * 587.33 * t) * 0.6 + sin(TWO_PI * 440.0 * t) * 0.4) * env * 0.22
    }
  }

  // Dholak Bounce Drum: Punchy Tabla/Dholak pitch-lift thump (95 Hz -> 260 Hz) over 170 ms
  fun playBounce() {
    playRawSfx(170) { t, progress ->
      val freq = 95.0 + 165.0 * Math.sqrt(progress)
      val env = Math.exp(-progress * 3.6)
      (sin(TWO_PI * freq * t) + 0.35 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.55
    }
  }

  // Kanta Beetle Stomp Bonk: Playful percussive woodblock + ascending pop over 150 ms
  fun playEnemyBonk() {
    playRawSfx(150) { t, progress ->
      val freq = if (progress < 0.35) 310.0 else 620.0
      val env = Math.exp(-progress * 5.0)
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 2.5 * t)) * env * 0.48
    }
  }

  // Brass Diya Ignition: Warm festive tri-tone sparkle (G5 -> B5 -> D6) over 280 ms
  fun playDiyaLit() {
    playRawSfx(280) { t, progress ->
      val freq = when {
        progress < 0.33 -> 783.99
        progress < 0.66 -> 987.77
        else -> 1174.66
      }
      val env = Math.exp(-progress * 2.8)
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.42
    }
  }

  // Awakening Tree Rumble: Deep earthen growl + woody snap over 340 ms
  fun playTreeRumble() {
    playRawSfx(340) { t, progress ->
      val freq = 68.0 - 24.0 * progress
      val pulse = 0.65 + 0.35 * sin(TWO_PI * 14.0 * t)
      val env = (1.0 - progress) * pulse
      (sin(TWO_PI * freq * t) + 0.4 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.58
    }
  }

  // Vanar Stalker Snarl: Tense descending modulated growl (210 Hz -> 95 Hz) over 300 ms
  fun playBeastRoar() {
    playRawSfx(300) { t, progress ->
      val freq = 210.0 - 115.0 * progress + 18.0 * sin(TWO_PI * 22.0 * t)
      val env = sin(PI * progress) * 0.55
      val raw = sin(TWO_PI * freq * t) + 0.45 * sin(TWO_PI * freq * 2.0 * t)
      raw.coerceIn(-1.0, 1.0) * env
    }
  }

  // Stone Guardian Temple Gong: Resonant deep bronze bell + shimmer over 460 ms
  fun playGuardianGong() {
    playRawSfx(460) { t, progress ->
      val env = Math.exp(-progress * 2.2)
      (sin(TWO_PI * 146.83 * t) * 0.65 + sin(TWO_PI * 220.0 * t) * 0.35 + sin(TWO_PI * 369.99 * t) * 0.2) * env * 0.55
    }
  }

  // Guardian Crystal Shatter: Bright ascending triumph chime (E5 -> G#5 -> B5 -> E6) over 420 ms
  fun playGuardianDefeat() {
    playRawSfx(420) { t, progress ->
      val freq = when {
        progress < 0.25 -> 659.25
        progress < 0.50 -> 830.61
        progress < 0.75 -> 987.77
        else -> 1318.51
      }
      val env = Math.exp(-progress * 2.4)
      (sin(TWO_PI * freq * t) + 0.3 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.52
    }
  }

  // Sound-Triggered Mechanism: Echoing cavern harmonic ring (D5 -> A5 -> D6) over 340 ms
  fun playEchoSwitch() {
    playRawSfx(340) { t, progress ->
      val freq = when {
        progress < 0.33 -> 587.33
        progress < 0.66 -> 880.00
        else -> 1174.66
      }
      val env = Math.exp(-progress * 3.0) * (0.75 + 0.25 * sin(TWO_PI * 9.0 * t))
      (sin(TWO_PI * freq * t) + 0.28 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.44
    }
  }

  // Monsoon Lightning Strike: Sharp electric crackle + low thunder roll over 320 ms
  fun playLightningStrike() {
    playRawSfx(320) { t, progress ->
      val crack = if (progress < 0.18) sin(TWO_PI * (1450.0 - progress * 4000.0) * t) * (1.0 - progress / 0.18) else 0.0
      val thunder = sin(TWO_PI * (92.0 - 48.0 * progress) * t) * Math.exp(-progress * 3.2)
      (crack * 0.45 + thunder * 0.65).coerceIn(-1.0, 1.0) * 0.55
    }
  }

  // Ruins Mechanism Shift / Pillar Sink: Low stone grind over 220 ms
  fun playMechanismShift() {
    playRawSfx(220) { t, progress ->
      val freq = 105.0 - 45.0 * progress
      val grit = sin(TWO_PI * 26.0 * t) * 0.35 + 0.65
      sin(TWO_PI * freq * t) * grit * (1.0 - progress) * 0.48
    }
  }

  // Troll Trap Snap: Sharp spring-loaded spike/trap snap over 95 ms
  fun playTrapSnap() {
    playRawSfx(95) { t, progress ->
      val freq = if (progress < 0.35) 680.0 - 240.0 * progress else 340.0 + 180.0 * progress
      val env = Math.exp(-progress * 5.2)
      sin(TWO_PI * freq * t) * env * 0.48
    }
  }

  // Ceiling Block Slam: Punchy heavy thud over 160 ms
  fun playCeilingSlam() {
    playRawSfx(160) { t, progress ->
      val freq = 92.0 - 62.0 * progress
      val env = Math.exp(-progress * 4.5)
      (sin(TWO_PI * freq * t) + 0.3 * sin(TWO_PI * freq * 0.5 * t)) * env * 0.62
    }
  }

  // Cheeky Relocating Exit Door Boing/Whoosh over 170 ms
  fun playDoorEscape() {
    playRawSfx(170) { t, progress ->
      val freq = 360.0 + 440.0 * sin(PI * progress)
      val env = (1.0 - progress * 0.7) * 0.44
      sin(TWO_PI * freq * t) * env
    }
  }

  // Fake Exit Door Comedic "Wah-Wah" Descending Slide over 220 ms
  fun playFakeDoorPoof() {
    playRawSfx(220) { t, progress ->
      val freq = if (progress < 0.45) 440.0 - 80.0 * progress else 340.0 - 160.0 * progress
      val env = (1.0 - progress * 0.5) * 0.45
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 2.0 * t)) * env
    }
  }

  // Fast Troll Death Pop-Splat over 190 ms (snappy, playful, never grating)
  fun playFastTrollDeath() {
    playRawSfx(190) { t, progress ->
      val freq = if (progress < 0.22) 540.0 else 360.0 - 250.0 * progress
      val env = Math.exp(-progress * 3.8)
      sin(TWO_PI * freq * t) * env * 0.55
    }
  }
}
