package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.example.data.AdaptiveTier
import com.example.data.LevelTheme
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

enum class AudioLifecycleState {
  IDLE,
  PLAYING,
  PAUSED,
  STOPPED,
  RELEASED
}

enum class SfxCue(val minCooldownMs: Long, val bypassTransitionSuppression: Boolean = false) {
  JUMP(65L),
  LAND(85L),
  COIN(45L),
  CRUMBLE(160L),
  DISAPPEAR(150L),
  CHECKPOINT(300L, bypassTransitionSuppression = true),
  DAMAGE(180L),
  DEATH(240L, bypassTransitionSuppression = true),
  FAST_TROLL_DEATH(200L, bypassTransitionSuppression = true),
  VICTORY(500L, bypassTransitionSuppression = true),
  CHALLENGE_COMPLETE(420L, bypassTransitionSuppression = true),
  TRAP_WARNING(170L),
  TRAP_SNAP(110L),
  CEILING_SLAM(150L),
  DOOR_ESCAPE(160L),
  FAKE_DOOR_POOF(180L),
  MECHANISM_SHIFT(150L),
  CLUE(250L),
  RESONANCE(240L),
  DISTANT_BELL(220L),
  BOUNCE(85L),
  ENEMY_BONK(95L),
  DIYA(180L),
  TREE_RUMBLE(260L),
  BEAST_ROAR(260L),
  GUARDIAN_GONG(320L),
  GUARDIAN_DEFEAT(360L),
  ECHO_SWITCH(190L),
  LIGHTNING(220L),
  NEAR_MISS(480L),
  RETRY(200L, bypassTransitionSuppression = true),
  UI_CLICK(45L, bypassTransitionSuppression = true)
}

enum class AudioIntensity(val level: Int, val tempoDeltaBpm: Int, val bayanLift: Double, val bansuriAccent: Double) {
  CALM(level = 0, tempoDeltaBpm = -4, bayanLift = 0.90, bansuriAccent = 0.96),
  STANDARD(level = 1, tempoDeltaBpm = 0, bayanLift = 1.00, bansuriAccent = 1.00),
  ESCALATED(level = 2, tempoDeltaBpm = 4, bayanLift = 1.12, bansuriAccent = 1.08)
}

data class ThemeMusicProfile(
    val theme: LevelTheme,
    val ragaName: String,
    val bpm: Int,
    val droneRootHz: Float,
    val droneFifthHz: Float,
    val bayanBaseHz: Double,
    val bayanSweepHz: Double,
    val bayanGain: Double,
    val dayanFreqHz: Double,
    val dayanGain: Double,
    val bansuriGain: Double,
    val sitarGain: Double,
    val melodySequence: FloatArray,
    val sitarSequence: FloatArray
)

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
 * Thread-safe, idempotent AudioTrack lifecycle, anti-spam cue cooldowns,
 * music headroom balancing, click-free de-clicked SFX envelopes, and AudioFocus ownership.
 */
class ProceduralAudioEngine(
    private val trackBackend: AudioTrackBackend = SystemAudioTrackBackend(),
    context: Context? = null,
    focusControllerOverride: AudioFocusController? = null,
    private val timeProviderMs: () -> Long = { System.currentTimeMillis() }
) {
  companion object {
    private const val SAMPLE_RATE = 22050
    private const val TWO_PI = 2.0 * PI
    private const val THREAD_JOIN_TIMEOUT_MS = 600L

    // Keeps atmospheric music cleanly below gameplay SFX at equal user slider positions.
    internal const val MUSIC_HEADROOM_GAIN = 0.44f
    internal const val SFX_HEADROOM_GAIN = 0.92f
    private const val BURST_WINDOW_MS = 100L
    private const val MAX_SFX_PER_BURST_WINDOW = 4
    private const val MIN_THEME_BPM = 84

    // Base frequency palette across Bilawal, Kafi, Bhairav, Megh & Purvi ragas
    private const val NOTE_A3 = 220.00f
    private const val NOTE_B3 = 246.94f
    private const val NOTE_C4 = 261.63f
    private const val NOTE_DB4 = 277.18f
    private const val NOTE_D4 = 293.66f
    private const val NOTE_EB4 = 311.13f
    private const val NOTE_E4 = 329.63f
    private const val NOTE_F4 = 349.23f
    private const val NOTE_FS4 = 369.99f
    private const val NOTE_G4 = 392.00f
    private const val NOTE_AB4 = 415.30f
    private const val NOTE_A4 = 440.00f
    private const val NOTE_BB4 = 466.16f
    private const val NOTE_B4 = 493.88f
    private const val NOTE_C5 = 523.25f
    private const val NOTE_DB5 = 554.37f
    private const val NOTE_D5 = 587.33f
    private const val NOTE_EB5 = 622.25f
    private const val NOTE_E5 = 659.25f
    private const val NOTE_F5 = 698.46f
    private const val NOTE_FS5 = 739.99f
    private const val NOTE_G5 = 783.99f

    fun resolveIntensity(tier: AdaptiveTier): AudioIntensity = when (tier) {
      AdaptiveTier.ASSISTED -> AudioIntensity.CALM
      AdaptiveTier.STANDARD -> AudioIntensity.STANDARD
      AdaptiveTier.ESCALATED, AdaptiveTier.RELENTLESS -> AudioIntensity.ESCALATED
    }

    private fun buildWoodsProfile(): ThemeMusicProfile {
      val melody = floatArrayOf(
          NOTE_C4, 0f, NOTE_E4, 0f, NOTE_G4, 0f, NOTE_A4, NOTE_G4,
          NOTE_E4, 0f, NOTE_G4, 0f, NOTE_C5, 0f, NOTE_B4, NOTE_A4,
          NOTE_G4, 0f, NOTE_E4, 0f, NOTE_D4, 0f, NOTE_C4, 0f,
          NOTE_E4, NOTE_G4, NOTE_A4, 0f, NOTE_G4, 0f, 0f, 0f,

          NOTE_C5, 0f, NOTE_B4, 0f, NOTE_A4, 0f, NOTE_G4, NOTE_E4,
          NOTE_F4, 0f, NOTE_A4, 0f, NOTE_G4, 0f, NOTE_E4, NOTE_D4,
          NOTE_C4, 0f, NOTE_D4, NOTE_E4, NOTE_G4, 0f, NOTE_A4, 0f,
          NOTE_C5, 0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C4, 0f,

          NOTE_G4, 0f, NOTE_A4, 0f, NOTE_C5, 0f, NOTE_D5, NOTE_E5,
          NOTE_D5, 0f, NOTE_C5, 0f, NOTE_A4, 0f, NOTE_G4, 0f,
          NOTE_F4, 0f, NOTE_A4, 0f, NOTE_C5, 0f, NOTE_B4, NOTE_A4,
          NOTE_G4, 0f, NOTE_E4, 0f, NOTE_D4, 0f, 0f, 0f,

          NOTE_E5, 0f, NOTE_D5, NOTE_C5, NOTE_D5, 0f, NOTE_C5, NOTE_A4,
          NOTE_G4, 0f, NOTE_F4, 0f, NOTE_E4, 0f, NOTE_D4, 0f,
          NOTE_C4, 0f, NOTE_E4, 0f, NOTE_G4, 0f, NOTE_C5, 0f,
          NOTE_B4, 0f, NOTE_G4, 0f, NOTE_C4, 0f, 0f, 0f
      )
      val sitar = floatArrayOf(
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_C5, 0f, NOTE_A4, 0f, NOTE_E5, 0f, NOTE_C5,
          0f, NOTE_B4, 0f, NOTE_G4, 0f, NOTE_D5, 0f, NOTE_B4,
          0f, NOTE_C5, 0f, 0f, 0f, NOTE_E5, 0f, NOTE_C5,

          0f, NOTE_E5, 0f, NOTE_C5, 0f, NOTE_G5, 0f, NOTE_E5,
          0f, NOTE_D5, 0f, NOTE_B4, 0f, NOTE_A4, 0f, NOTE_F4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_E4, 0f, 0f,

          0f, NOTE_D5, 0f, NOTE_B4, 0f, NOTE_G5, 0f, NOTE_D5,
          0f, NOTE_F5, 0f, NOTE_D5, 0f, NOTE_C5, 0f, NOTE_A4,
          0f, NOTE_C5, 0f, NOTE_A4, 0f, NOTE_E5, 0f, NOTE_C5,
          0f, NOTE_D5, 0f, NOTE_B4, 0f, NOTE_G4, 0f, 0f,

          0f, NOTE_G5, 0f, NOTE_E5, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_B4, 0f, NOTE_G4, 0f, NOTE_A4, 0f, NOTE_F4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_E4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, 0f
      )
      return ThemeMusicProfile(
          theme = LevelTheme.WHISPERING_WOODS,
          ragaName = "Bilawal",
          bpm = 104,
          droneRootHz = NOTE_C4,
          droneFifthHz = NOTE_G4,
          bayanBaseHz = 48.0,
          bayanSweepHz = 62.0,
          bayanGain = 0.28,
          dayanFreqHz = 920.0,
          dayanGain = 0.12,
          bansuriGain = 0.28,
          sitarGain = 0.18,
          melodySequence = melody,
          sitarSequence = sitar
      )
    }

    private fun buildHollowProfile(): ThemeMusicProfile {
      // Kafi-flavoured hollow cavern mood (Eb, Bb, gentle echoing plucks)
      val melody = floatArrayOf(
          NOTE_C4, 0f, NOTE_EB4, 0f, NOTE_G4, 0f, 0f, NOTE_F4,
          NOTE_EB4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, NOTE_BB4, 0f,
          NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4, 0f,
          NOTE_EB4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, 0f, 0f,

          NOTE_G4, 0f, NOTE_BB4, 0f, NOTE_C5, 0f, NOTE_EB5, 0f,
          NOTE_D5, 0f, NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f,
          NOTE_F4, 0f, NOTE_G4, 0f, NOTE_EB4, 0f, NOTE_D4, 0f,
          NOTE_C4, 0f, NOTE_G4, 0f, NOTE_C4, 0f, 0f, 0f,

          NOTE_EB5, 0f, NOTE_D5, 0f, NOTE_C5, 0f, NOTE_BB4, 0f,
          NOTE_A4, 0f, NOTE_G4, 0f, NOTE_F4, 0f, NOTE_EB4, 0f,
          NOTE_D4, 0f, NOTE_F4, 0f, NOTE_G4, 0f, NOTE_BB4, 0f,
          NOTE_C5, 0f, NOTE_G4, 0f, NOTE_EB4, 0f, 0f, 0f,

          NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4, 0f,
          NOTE_EB4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, NOTE_B3, 0f,
          NOTE_C4, 0f, NOTE_EB4, 0f, NOTE_G4, 0f, NOTE_C5, 0f,
          NOTE_G4, 0f, NOTE_EB4, 0f, NOTE_C4, 0f, 0f, 0f
      )
      val sitar = floatArrayOf(
          0f, NOTE_G4, 0f, NOTE_EB4, 0f, NOTE_BB4, 0f, NOTE_G4,
          0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_EB5, 0f, NOTE_C5,
          0f, NOTE_D5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4,
          0f, NOTE_EB4, 0f, 0f, 0f, NOTE_G4, 0f, NOTE_C4,

          0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_EB5, 0f, NOTE_C5,
          0f, NOTE_D5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4,
          0f, NOTE_EB4, 0f, NOTE_C4, 0f, NOTE_G4, 0f, NOTE_D4,
          0f, NOTE_C4, 0f, NOTE_G4, 0f, NOTE_EB4, 0f, 0f,

          0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_EB5, 0f, NOTE_BB4,
          0f, NOTE_A4, 0f, NOTE_F4, 0f, NOTE_G4, 0f, NOTE_EB4,
          0f, NOTE_D4, 0f, NOTE_F4, 0f, NOTE_BB4, 0f, NOTE_G4,
          0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_EB4, 0f, 0f,

          0f, NOTE_EB5, 0f, NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4,
          0f, NOTE_F4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, NOTE_A3,
          0f, NOTE_G4, 0f, NOTE_EB4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_EB4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, 0f
      )
      return ThemeMusicProfile(
          theme = LevelTheme.HOLLOW_OF_ECHOES,
          ragaName = "Kafi",
          bpm = 96,
          droneRootHz = NOTE_C4,
          droneFifthHz = NOTE_G4,
          bayanBaseHz = 44.0,
          bayanSweepHz = 54.0,
          bayanGain = 0.25,
          dayanFreqHz = 780.0,
          dayanGain = 0.10,
          bansuriGain = 0.24,
          sitarGain = 0.23,
          melodySequence = melody,
          sitarSequence = sitar
      )
    }

    private fun buildRuinsProfile(): ThemeMusicProfile {
      // Bhairav regal ruins motif (Db, E, Ab, B)
      val melody = floatArrayOf(
          NOTE_C4, 0f, NOTE_DB4, NOTE_E4, NOTE_G4, 0f, NOTE_AB4, NOTE_G4,
          NOTE_E4, 0f, NOTE_F4, 0f, NOTE_E4, NOTE_DB4, NOTE_C4, 0f,
          NOTE_G4, 0f, NOTE_AB4, 0f, NOTE_C5, 0f, NOTE_B4, NOTE_AB4,
          NOTE_G4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, 0f, 0f,

          NOTE_C5, 0f, NOTE_DB5, 0f, NOTE_C5, NOTE_B4, NOTE_AB4, NOTE_G4,
          NOTE_F4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_C4, 0f,
          NOTE_E4, 0f, NOTE_G4, 0f, NOTE_AB4, 0f, NOTE_C5, 0f,
          NOTE_B4, 0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C4, 0f,

          NOTE_G4, 0f, NOTE_E4, NOTE_G4, NOTE_C5, 0f, NOTE_DB5, NOTE_E5,
          NOTE_DB5, 0f, NOTE_C5, 0f, NOTE_AB4, 0f, NOTE_G4, 0f,
          NOTE_F4, 0f, NOTE_G4, 0f, NOTE_AB4, 0f, NOTE_B4, NOTE_C5,
          NOTE_G4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, 0f, 0f,

          NOTE_E5, 0f, NOTE_DB5, NOTE_C5, NOTE_B4, 0f, NOTE_AB4, NOTE_G4,
          NOTE_F4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_C4, 0f,
          NOTE_C4, 0f, NOTE_E4, 0f, NOTE_G4, 0f, NOTE_C5, 0f,
          NOTE_AB4, 0f, NOTE_G4, 0f, NOTE_C4, 0f, 0f, 0f
      )
      val sitar = floatArrayOf(
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C5, 0f, NOTE_AB4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_C4,
          0f, NOTE_B4, 0f, NOTE_AB4, 0f, NOTE_G4, 0f, NOTE_E4,
          0f, NOTE_C5, 0f, 0f, 0f, NOTE_E5, 0f, NOTE_C5,

          0f, NOTE_DB5, 0f, NOTE_C5, 0f, NOTE_G5, 0f, NOTE_E5,
          0f, NOTE_AB4, 0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_DB4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_B4, 0f, NOTE_G4, 0f, NOTE_E4, 0f, 0f,

          0f, NOTE_E5, 0f, NOTE_C5, 0f, NOTE_G5, 0f, NOTE_DB5,
          0f, NOTE_C5, 0f, NOTE_AB4, 0f, NOTE_G4, 0f, NOTE_E4,
          0f, NOTE_F4, 0f, NOTE_AB4, 0f, NOTE_C5, 0f, NOTE_B4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, 0f,

          0f, NOTE_E5, 0f, NOTE_DB5, 0f, NOTE_C5, 0f, NOTE_AB4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_C4,
          0f, NOTE_E4, 0f, NOTE_G4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_AB4, 0f, NOTE_E4, 0f, NOTE_C4, 0f, 0f
      )
      return ThemeMusicProfile(
          theme = LevelTheme.RUINS_OF_LOST_KING,
          ragaName = "Bhairav",
          bpm = 108,
          droneRootHz = NOTE_C4,
          droneFifthHz = NOTE_G4,
          bayanBaseHz = 50.0,
          bayanSweepHz = 66.0,
          bayanGain = 0.30,
          dayanFreqHz = 960.0,
          dayanGain = 0.13,
          bansuriGain = 0.25,
          sitarGain = 0.21,
          melodySequence = melody,
          sitarSequence = sitar
      )
    }

    private fun buildMonsoonProfile(): ThemeMusicProfile {
      // Megh brisk monsoon energy (C, D, F, G, Bb)
      val melody = floatArrayOf(
          NOTE_C4, NOTE_D4, NOTE_F4, 0f, NOTE_G4, 0f, NOTE_BB4, NOTE_G4,
          NOTE_F4, 0f, NOTE_G4, 0f, NOTE_C5, 0f, NOTE_BB4, NOTE_G4,
          NOTE_F4, 0f, NOTE_D4, 0f, NOTE_C4, 0f, NOTE_F4, 0f,
          NOTE_G4, NOTE_BB4, NOTE_C5, 0f, NOTE_G4, 0f, 0f, 0f,

          NOTE_C5, 0f, NOTE_D5, 0f, NOTE_F5, 0f, NOTE_D5, NOTE_C5,
          NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4, 0f, NOTE_D4, 0f,
          NOTE_C4, NOTE_D4, NOTE_F4, NOTE_G4, NOTE_BB4, 0f, NOTE_C5, 0f,
          NOTE_D5, 0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_F4, 0f,

          NOTE_G4, 0f, NOTE_BB4, 0f, NOTE_C5, 0f, NOTE_D5, NOTE_F5,
          NOTE_D5, 0f, NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f,
          NOTE_F4, 0f, NOTE_G4, 0f, NOTE_BB4, 0f, NOTE_C5, 0f,
          NOTE_G4, 0f, NOTE_F4, 0f, NOTE_D4, 0f, 0f, 0f,

          NOTE_F5, 0f, NOTE_D5, NOTE_C5, NOTE_BB4, 0f, NOTE_C5, NOTE_G4,
          NOTE_F4, 0f, NOTE_D4, 0f, NOTE_F4, 0f, NOTE_G4, 0f,
          NOTE_BB4, 0f, NOTE_C5, 0f, NOTE_D5, 0f, NOTE_C5, 0f,
          NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_C4, 0f, 0f, 0f
      )
      val sitar = floatArrayOf(
          0f, NOTE_G4, 0f, NOTE_F4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F5, 0f, NOTE_D5,
          0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_F4, 0f, NOTE_D4,
          0f, NOTE_G4, 0f, 0f, 0f, NOTE_C5, 0f, NOTE_G4,

          0f, NOTE_D5, 0f, NOTE_C5, 0f, NOTE_F5, 0f, NOTE_D5,
          0f, NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4,
          0f, NOTE_D4, 0f, NOTE_F4, 0f, NOTE_G4, 0f, NOTE_BB4,
          0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_F4, 0f, 0f,

          0f, NOTE_D5, 0f, NOTE_BB4, 0f, NOTE_F5, 0f, NOTE_D5,
          0f, NOTE_C5, 0f, NOTE_BB4, 0f, NOTE_G4, 0f, NOTE_F4,
          0f, NOTE_G4, 0f, NOTE_BB4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_D4, 0f, NOTE_F4, 0f, NOTE_G4, 0f, 0f,

          0f, NOTE_F5, 0f, NOTE_D5, 0f, NOTE_C5, 0f, NOTE_BB4,
          0f, NOTE_G4, 0f, NOTE_F4, 0f, NOTE_D4, 0f, NOTE_C4,
          0f, NOTE_F4, 0f, NOTE_G4, 0f, NOTE_C5, 0f, NOTE_BB4,
          0f, NOTE_G4, 0f, NOTE_F4, 0f, NOTE_C4, 0f, 0f
      )
      return ThemeMusicProfile(
          theme = LevelTheme.MONSOON_RUN,
          ragaName = "Megh",
          bpm = 114,
          droneRootHz = NOTE_C4,
          droneFifthHz = NOTE_G4,
          bayanBaseHz = 52.0,
          bayanSweepHz = 70.0,
          bayanGain = 0.30,
          dayanFreqHz = 990.0,
          dayanGain = 0.14,
          bansuriGain = 0.27,
          sitarGain = 0.19,
          melodySequence = melody,
          sitarSequence = sitar
      )
    }

    private fun buildTempleProfile(): ThemeMusicProfile {
      // Purvi / Marwa mystic finale (Db, E, F#, Ab, B)
      val melody = floatArrayOf(
          NOTE_C4, 0f, NOTE_E4, 0f, NOTE_FS4, 0f, NOTE_G4, NOTE_AB4,
          NOTE_B4, 0f, NOTE_C5, 0f, NOTE_DB5, 0f, NOTE_B4, NOTE_G4,
          NOTE_FS4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_C4, 0f,
          NOTE_E4, NOTE_FS4, NOTE_G4, 0f, NOTE_B4, 0f, 0f, 0f,

          NOTE_C5, 0f, NOTE_B4, 0f, NOTE_AB4, 0f, NOTE_G4, NOTE_FS4,
          NOTE_E4, 0f, NOTE_FS4, 0f, NOTE_G4, 0f, NOTE_E4, NOTE_DB4,
          NOTE_C4, 0f, NOTE_DB4, NOTE_E4, NOTE_FS4, 0f, NOTE_G4, 0f,
          NOTE_C5, 0f, NOTE_B4, 0f, NOTE_G4, 0f, NOTE_C4, 0f,

          NOTE_G4, 0f, NOTE_B4, 0f, NOTE_C5, 0f, NOTE_DB5, NOTE_E5,
          NOTE_DB5, 0f, NOTE_C5, 0f, NOTE_B4, 0f, NOTE_G4, 0f,
          NOTE_FS4, 0f, NOTE_G4, 0f, NOTE_B4, 0f, NOTE_C5, 0f,
          NOTE_AB4, 0f, NOTE_G4, 0f, NOTE_E4, 0f, 0f, 0f,

          NOTE_E5, 0f, NOTE_DB5, NOTE_C5, NOTE_B4, 0f, NOTE_G4, NOTE_FS4,
          NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_E4, 0f, NOTE_G4, 0f,
          NOTE_B4, 0f, NOTE_C5, 0f, NOTE_G4, 0f, NOTE_C5, 0f,
          NOTE_B4, 0f, NOTE_G4, 0f, NOTE_C4, 0f, 0f, 0f
      )
      val sitar = floatArrayOf(
          0f, NOTE_G4, 0f, NOTE_FS4, 0f, NOTE_C5, 0f, NOTE_B4,
          0f, NOTE_E5, 0f, NOTE_C5, 0f, NOTE_B4, 0f, NOTE_G4,
          0f, NOTE_FS4, 0f, NOTE_E4, 0f, NOTE_DB4, 0f, NOTE_C4,
          0f, NOTE_G4, 0f, 0f, 0f, NOTE_B4, 0f, NOTE_G4,

          0f, NOTE_E5, 0f, NOTE_C5, 0f, NOTE_FS5, 0f, NOTE_E5,
          0f, NOTE_B4, 0f, NOTE_G4, 0f, NOTE_FS4, 0f, NOTE_E4,
          0f, NOTE_G4, 0f, NOTE_E4, 0f, NOTE_C5, 0f, NOTE_B4,
          0f, NOTE_G4, 0f, NOTE_FS4, 0f, NOTE_E4, 0f, 0f,

          0f, NOTE_DB5, 0f, NOTE_B4, 0f, NOTE_E5, 0f, NOTE_C5,
          0f, NOTE_B4, 0f, NOTE_AB4, 0f, NOTE_G4, 0f, NOTE_FS4,
          0f, NOTE_E4, 0f, NOTE_G4, 0f, NOTE_B4, 0f, NOTE_C5,
          0f, NOTE_G4, 0f, NOTE_FS4, 0f, NOTE_E4, 0f, 0f,

          0f, NOTE_E5, 0f, NOTE_DB5, 0f, NOTE_C5, 0f, NOTE_B4,
          0f, NOTE_G4, 0f, NOTE_FS4, 0f, NOTE_E4, 0f, NOTE_DB4,
          0f, NOTE_G4, 0f, NOTE_B4, 0f, NOTE_C5, 0f, NOTE_G4,
          0f, NOTE_FS4, 0f, NOTE_E4, 0f, NOTE_C4, 0f, 0f
      )
      return ThemeMusicProfile(
          theme = LevelTheme.TEMPLE_OF_LAST_LIGHT,
          ragaName = "Purvi",
          bpm = 118,
          droneRootHz = NOTE_C4,
          droneFifthHz = NOTE_G4,
          bayanBaseHz = 50.0,
          bayanSweepHz = 72.0,
          bayanGain = 0.29,
          dayanFreqHz = 1080.0,
          dayanGain = 0.14,
          bansuriGain = 0.25,
          sitarGain = 0.22,
          melodySequence = melody,
          sitarSequence = sitar
      )
    }

    private val THEME_PROFILES: Map<LevelTheme, ThemeMusicProfile> = mapOf(
        LevelTheme.WHISPERING_WOODS to buildWoodsProfile(),
        LevelTheme.HOLLOW_OF_ECHOES to buildHollowProfile(),
        LevelTheme.RUINS_OF_LOST_KING to buildRuinsProfile(),
        LevelTheme.MONSOON_RUN to buildMonsoonProfile(),
        LevelTheme.TEMPLE_OF_LAST_LIGHT to buildTempleProfile()
    )

    fun profileForTheme(theme: LevelTheme): ThemeMusicProfile {
      return THEME_PROFILES[theme] ?: THEME_PROFILES.getValue(LevelTheme.WHISPERING_WOODS)
    }
  }

  private val transitionLock = Any()
  private val lifecycleLock = Any()
  private val sfxMixLock = Any()
  private val sfxBufferLock = Any()
  private val pauseSignalLock = Object()
  // Pre-allocated zero-allocation SFX buffer (up to 1 second @ 22.05 kHz)
  private val sfxSampleBuffer = ShortArray(SAMPLE_RATE)

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
  @Volatile var activeTheme: LevelTheme = LevelTheme.WHISPERING_WOODS
    private set
  @Volatile var activeIntensity: AudioIntensity = AudioIntensity.STANDARD
    private set

  // Anti-overlap & transition de-duplication state
  private val lastCuePlayedAtMs = EnumMap<SfxCue, Long>(SfxCue::class.java)
  private val recentWriteTimestamps = LongArray(MAX_SFX_PER_BURST_WINDOW)
  private var recentWriteHead = 0
  private var suppressAmbientUntilMs = 0L

  // Diagnostic telemetry for deterministic automated verification
  private val _totalSfxPlayed = AtomicInteger(0)
  private val _totalSfxSuppressed = AtomicInteger(0)
  @Volatile var lastPlayedCue: SfxCue? = null
    private set

  val totalSfxPlayed: Int get() = _totalSfxPlayed.get()
  val totalSfxSuppressed: Int get() = _totalSfxSuppressed.get()

  val currentLifecycleState: AudioLifecycleState
    get() = synchronized(lifecycleLock) { lifecycleState }

  fun setThemeAndIntensity(
      theme: LevelTheme,
      tier: AdaptiveTier = AdaptiveTier.STANDARD
  ) {
    activeTheme = theme
    activeIntensity = resolveIntensity(tier)
  }

  fun currentThemeProfile(): ThemeMusicProfile = profileForTheme(activeTheme)

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

  fun computeEffectiveMusicGain(): Float {
    if (!isMusicEnabled) return 0f
    val duckFactor = if (isFocusDucked) 0.25f else 1.0f
    return musicVolume.coerceIn(0f, 1f) * MUSIC_HEADROOM_GAIN * duckFactor
  }

  fun computeEffectiveSfxGain(): Float {
    if (!isSfxEnabled) return 0f
    val duckFactor = if (isFocusDucked) 0.35f else 1.0f
    return sfxVolume.coerceIn(0f, 1f) * SFX_HEADROOM_GAIN * duckFactor
  }

  /**
   * Call on retry or challenge transition to prevent duplicate ambient/landing/trap audio bursts.
   */
  fun onTransitionOrRetry(suppressAmbientMs: Long = 150L, playRetryCue: Boolean = false) {
    synchronized(sfxMixLock) {
      val now = timeProviderMs()
      suppressAmbientUntilMs = now + suppressAmbientMs.coerceAtLeast(0L)
      // Reset transient platform/trap cooldowns so new attempt starts clean after suppression window
      lastCuePlayedAtMs.remove(SfxCue.LAND)
      lastCuePlayedAtMs.remove(SfxCue.CRUMBLE)
      lastCuePlayedAtMs.remove(SfxCue.DISAPPEAR)
      lastCuePlayedAtMs.remove(SfxCue.TRAP_SNAP)
      lastCuePlayedAtMs.remove(SfxCue.TRAP_WARNING)
      lastCuePlayedAtMs.remove(SfxCue.MECHANISM_SHIFT)
      lastCuePlayedAtMs.remove(SfxCue.NEAR_MISS)
    }
    if (playRetryCue) {
      playRetryCue()
    }
  }

  fun startMusic() {
    synchronized(transitionLock) {
      startMusicLocked()
    }
  }

  private fun startMusicLocked() {
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
      // Pre-allocate single buffer large enough for slowest theme/intensity BPM (zero GC in loop)
      val maxStepSamples = (SAMPLE_RATE * 60f / (MIN_THEME_BPM * 4f)).toInt() + 16
      val buffer = ShortArray(maxStepSamples)

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

        val profile = profileForTheme(activeTheme)
        val intensity = activeIntensity
        val effectiveBpm = (profile.bpm + intensity.tempoDeltaBpm).coerceAtLeast(MIN_THEME_BPM)
        val stepSamples = (SAMPLE_RATE * 60f / (effectiveBpm * 4f)).toInt().coerceIn(256, maxStepSamples)
        val vol = computeEffectiveMusicGain().toDouble()

        val seqLen = profile.melodySequence.size
        val seqStep = stepIndex % seqLen
        val melodyFreq = profile.melodySequence[seqStep]
        val sitarFreq = profile.sitarSequence[seqStep]
        val isBayanBeat = (seqStep % 8 == 0) ||
            (seqStep % 16 == 12) ||
            (profile.theme == LevelTheme.MONSOON_RUN && seqStep % 16 == 10)
        val isDayanClick = (seqStep % 8 == 2) ||
            (seqStep % 8 == 6) ||
            (intensity == AudioIntensity.ESCALATED && seqStep % 16 == 14)
        // Subtle 4-bar breathing dynamics so loop feels musical & non-fatiguing
        val phraseSwell = 0.85 + 0.15 * sin(TWO_PI * seqStep / seqLen.toDouble())
        val bansuriMul = profile.bansuriGain * intensity.bansuriAccent * phraseSwell
        val bayanMul = profile.bayanGain * intensity.bayanLift

        for (i in 0 until stepSamples) {
          val t = (sampleCount + i).toDouble() / SAMPLE_RATE
          val stepT = i.toDouble() / stepSamples
          val attack = (stepT / 0.06).coerceIn(0.0, 1.0)

          val drone1 = sin(TWO_PI * profile.droneRootHz * t)
          val drone2 = sin(TWO_PI * (profile.droneFifthHz + 0.30) * t)
          val tanpura = (drone1 * 0.082 + drone2 * 0.052)

          var bansuri = 0.0
          if (melodyFreq > 0f) {
            val vibrato = 1.0 + 0.011 * sin(TWO_PI * 5.0 * t)
            val envelope = attack * (1.0 - stepT * 0.45).coerceAtLeast(0.0)
            val fund = sin(TWO_PI * (melodyFreq * vibrato) * t)
            val harm = 0.20 * sin(TWO_PI * (melodyFreq * 2.0 * vibrato) * t)
            bansuri = (fund + harm) * envelope * bansuriMul
          }

          var sitar = 0.0
          if (sitarFreq > 0f) {
            val decay = exp(-stepT * 6.8)
            val pluck = sin(TWO_PI * sitarFreq * t) + 0.32 * sin(TWO_PI * sitarFreq * 3.0 * t)
            sitar = pluck * attack * decay * profile.sitarGain
          }

          var bayan = 0.0
          if (isBayanBeat) {
            val sweepFreq = profile.bayanBaseHz + profile.bayanSweepHz * exp(-stepT * 8.5)
            val bayanEnv = exp(-stepT * 5.2)
            bayan = sin(TWO_PI * sweepFreq * t) * attack * bayanEnv * bayanMul
          }

          var dayan = 0.0
          if (isDayanClick && stepT < 0.14) {
            val clickEnv = (1.0 - stepT / 0.14).coerceAtLeast(0.0)
            dayan = sin(TWO_PI * profile.dayanFreqHz * t) * attack * clickEnv * profile.dayanGain
          }

          // Smooth tanh soft-clipper prevents harsh digital clipping
          val rawSum = (tanpura + bansuri + sitar + bayan + dayan) * vol
          val softLimited = tanh(rawSum * 1.15) * 0.92
          buffer[i] = (softLimited.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
        }

        val written = track.writeBlocking(buffer, 0, stepSamples)
        if (written < 0) {
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
    synchronized(sfxMixLock) {
      lastCuePlayedAtMs.clear()
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

  private fun tryAcquireSfxSlot(cue: SfxCue): Boolean {
    synchronized(sfxMixLock) {
      val now = timeProviderMs()
      if (!cue.bypassTransitionSuppression && now < suppressAmbientUntilMs) {
        _totalSfxSuppressed.incrementAndGet()
        return false
      }
      val lastPlayed = lastCuePlayedAtMs[cue]
      if (lastPlayed != null && (now - lastPlayed) < cue.minCooldownMs) {
        _totalSfxSuppressed.incrementAndGet()
        return false
      }
      if (!cue.bypassTransitionSuppression) {
        var recentInWindow = 0
        for (ts in recentWriteTimestamps) {
          if (ts > 0L && (now - ts) < BURST_WINDOW_MS) {
            recentInWindow++
          }
        }
        if (recentInWindow >= MAX_SFX_PER_BURST_WINDOW) {
          _totalSfxSuppressed.incrementAndGet()
          return false
        }
      }
      lastCuePlayedAtMs[cue] = now
      recentWriteTimestamps[recentWriteHead] = now
      recentWriteHead = (recentWriteHead + 1) % recentWriteTimestamps.size
      return true
    }
  }

  private fun playRawSfx(
      cue: SfxCue,
      durationMs: Int,
      generator: (t: Double, progress: Double) -> Double
  ) {
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
    if (!tryAcquireSfxSlot(cue)) {
      return
    }

    val samples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceIn(16, sfxSampleBuffer.size)
    val vol = computeEffectiveSfxGain().toDouble()
    if (vol <= 0.0001) return

    // Click-free 4ms fade-in & 8ms fade-out ramps on every procedural cue
    val fadeInSamples = (SAMPLE_RATE * 0.004).toInt().coerceAtLeast(4).coerceAtMost(samples / 3)
    val fadeOutSamples = (SAMPLE_RATE * 0.008).toInt().coerceAtLeast(8).coerceAtMost(samples / 2)

    synchronized(sfxBufferLock) {
      for (i in 0 until samples) {
        val t = i.toDouble() / SAMPLE_RATE
        val progress = i.toDouble() / samples
        val fadeIn = if (i < fadeInSamples) i.toDouble() / fadeInSamples else 1.0
        val distFromEnd = samples - 1 - i
        val fadeOut = if (distFromEnd < fadeOutSamples) distFromEnd.toDouble() / fadeOutSamples else 1.0
        val raw = generator(t, progress) * fadeIn * fadeOut * vol
        val limited = tanh(raw * 1.1)
        sfxSampleBuffer[i] = (limited.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
      }

      try {
        activeTrack.writeNonBlocking(sfxSampleBuffer, 0, samples)
        _totalSfxPlayed.incrementAndGet()
        lastPlayedCue = cue
      } catch (_: Exception) {}
    }
  }

  // Jump: Clean snappy 210 Hz -> 560 Hz elastic pop over 105 ms
  fun playJump() {
    playRawSfx(SfxCue.JUMP, 105) { t, progress ->
      val freq = 210.0 + 350.0 * sqrt(progress)
      val env = (1.0 - progress) * exp(-progress * 1.4)
      (sin(TWO_PI * freq * t) + 0.18 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.44
    }
  }

  // Land: Warm grounded low thud (115 Hz -> 48 Hz) over 68 ms
  fun playLand() {
    playRawSfx(SfxCue.LAND, 68) { t, progress ->
      val freq = 115.0 - 67.0 * progress
      val env = exp(-progress * 5.8)
      sin(TWO_PI * freq * t) * env * 0.36
    }
  }

  // Coin Collect: Bright harmonic chime with gentle combo pitch lift
  fun playCoin(comboMultiplier: Int = 1) {
    val pitchLift = (1.0 + ((comboMultiplier - 1).coerceIn(0, 4) * 0.055))
    playRawSfx(SfxCue.COIN, 135) { t, progress ->
      val baseFreq = if (progress < 0.42) 987.77 else 1318.51
      val freq = baseFreq * pitchLift
      val localEnv = exp(-((progress % 0.42) * 5.2)) * (1.0 - progress * 0.25)
      (sin(TWO_PI * freq * t) + 0.28 * sin(TWO_PI * freq * 2.0 * t)) * localEnv * 0.44
    }
  }

  // Trap Warning: Distinct non-grating double telegraph blip for approaching/triggered traps
  fun playTrapWarning() {
    playRawSfx(SfxCue.TRAP_WARNING, 95) { t, progress ->
      val freq = if (progress < 0.48) 466.16 else 622.25
      val pulse = if (progress in 0.40..0.52) 0.18 else 1.0
      val env = exp(-progress * 3.0) * pulse
      (sin(TWO_PI * freq * t) + 0.22 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.34
    }
  }

  // Near-Miss Rush: Crisp airy whoosh-glint when narrowly slipping past an active lethal trap
  fun playNearMiss() {
    playRawSfx(SfxCue.NEAR_MISS, 85) { t, progress ->
      val freq = 620.0 + 360.0 * progress
      val env = sin(PI * progress) * exp(-progress * 1.8)
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.28
    }
  }

  // Crumble Bridge: Filtered wooden/earthen crumble rumble over 220 ms
  fun playCrumble() {
    playRawSfx(SfxCue.CRUMBLE, 220) { t, progress ->
      val freq = 86.0 - 48.0 * progress
      val env = (1.0 - progress) * (sin(TWO_PI * 16.0 * t) * 0.4 + 0.6)
      sin(TWO_PI * freq * t) * env * 0.46
    }
  }

  // Disappearing Floor: Resonant crystalline vanish chirp over 130 ms
  fun playDisappear() {
    playRawSfx(SfxCue.DISAPPEAR, 130) { t, progress ->
      val freq = 700.0 - 440.0 * progress
      val env = exp(-progress * 4.8)
      sin(TWO_PI * freq * t) * env * 0.40
    }
  }

  // Checkpoint Bell: Resonant 4-note ascending chime (C5, E5, G5, C6) with temple shimmer over 440 ms
  fun playCheckpoint() {
    playRawSfx(SfxCue.CHECKPOINT, 440) { t, progress ->
      val noteIndex = (progress * 4).toInt().coerceIn(0, 3)
      val freq = when (noteIndex) {
        0 -> 523.25
        1 -> 659.25
        2 -> 783.99
        else -> 1046.50
      }
      val env = exp(-progress * 2.2)
      (sin(TWO_PI * freq * t) + 0.24 * sin(TWO_PI * freq * 2.0 * t) + 0.10 * sin(TWO_PI * freq * 3.0 * t)) * env * 0.46
    }
  }

  // Damage: Soft-filtered low brass impact buzz (no harsh digital square clip) over 170 ms
  fun playDamage() {
    playRawSfx(SfxCue.DAMAGE, 170) { t, progress ->
      val freq = 205.0 - 95.0 * progress
      val env = exp(-progress * 3.4) * (1.0 - progress * 0.3)
      val warmBuzz = sin(TWO_PI * freq * t) + 0.45 * sin(TWO_PI * freq * 2.0 * t) + 0.2 * sin(TWO_PI * freq * 3.0 * t)
      warmBuzz * 0.55 * env * 0.48
    }
  }

  // Death: Crisp comedic descending slide (360 Hz -> 85 Hz) over 320 ms
  fun playDeath() {
    playRawSfx(SfxCue.DEATH, 320) { t, progress ->
      val freq = 360.0 - 275.0 * progress
      val env = exp(-progress * 3.4)
      (sin(TWO_PI * freq * t) + 0.2 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.52
    }
  }

  // Retry Feedback: Quick crisp reset spring cue (240 Hz -> 420 Hz) over 95 ms
  fun playRetryCue() {
    playRawSfx(SfxCue.RETRY, 95) { t, progress ->
      val freq = 240.0 + 180.0 * progress
      val env = sin(PI * progress)
      (sin(TWO_PI * freq * t) + 0.15 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.34
    }
  }

  // UI Tap Feedback: Gentle woody percussion click over 55 ms
  fun playUiClick() {
    playRawSfx(SfxCue.UI_CLICK, 55) { t, progress ->
      val freq = 520.0 - 160.0 * progress
      val env = exp(-progress * 6.5)
      sin(TWO_PI * freq * t) * env * 0.26
    }
  }

  // Mid-run Challenge Clear: Clean 4-note ascending chime (G4 -> B4 -> D5 -> G5) over 350 ms
  fun playChallengeComplete() {
    playRawSfx(SfxCue.CHALLENGE_COMPLETE, 350) { t, progress ->
      val noteIndex = (progress * 4).toInt().coerceIn(0, 3)
      val freq = when (noteIndex) {
        0 -> 392.00
        1 -> 493.88
        2 -> 587.33
        else -> 783.99
      }
      val env = exp(-progress * 1.9) * (1.0 - progress * 0.22)
      (sin(TWO_PI * freq * t) + 0.26 * sin(TWO_PI * freq * 2.0 * t) + 0.10 * sin(TWO_PI * freq * 3.0 * t)) * env * 0.48
    }
  }

  // Final Victory: Celebratory 5-note Bilawal fanfare with harmonic sheen (C5-D5-E5-G5-C6) over 600 ms
  fun playVictory() {
    playRawSfx(SfxCue.VICTORY, 600) { t, progress ->
      val noteIndex = (progress * 5).toInt().coerceIn(0, 4)
      val freq = when (noteIndex) {
        0 -> 523.25
        1 -> 587.33
        2 -> 659.25
        3 -> 783.99
        else -> 1046.50
      }
      val env = (1.0 - progress * 0.25) * exp(-progress * 1.05)
      (sin(TWO_PI * freq * t) + 0.28 * sin(TWO_PI * freq * 2.0 * t) + 0.14 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.50
    }
  }

  // Clue Discovery: Soft, warm wooden-bell harmonic chime (E5 -> B5) over 240 ms
  fun playClueDiscovery() {
    playRawSfx(SfxCue.CLUE, 240) { t, progress ->
      val freq = if (progress < 0.5) 659.25 else 987.77
      val env = exp(-progress * 4.0) * (1.0 - progress * 0.2)
      (sin(TWO_PI * freq * t) + 0.2 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.30
    }
  }

  // Resonance Hum: Gentle crystalline breath chime (G4 + D5 fifth) over 260 ms
  fun playResonanceHum() {
    playRawSfx(SfxCue.RESONANCE, 260) { t, progress ->
      val env = sin(PI * progress) * 0.28
      (sin(TWO_PI * 392.0 * t) + 0.35 * sin(TWO_PI * 587.33 * t)) * env
    }
  }

  // Distant Bell Cue: Muffled far-off shrine chime (D5 + A4) over 280 ms
  fun playDistantBellCue() {
    playRawSfx(SfxCue.DISTANT_BELL, 280) { t, progress ->
      val env = exp(-progress * 3.8) * (1.0 - progress * 0.25)
      (sin(TWO_PI * 587.33 * t) * 0.6 + sin(TWO_PI * 440.0 * t) * 0.4) * env * 0.26
    }
  }

  // Dholak Bounce Drum: Punchy pitch-lift thump (95 Hz -> 260 Hz) over 160 ms
  fun playBounce() {
    playRawSfx(SfxCue.BOUNCE, 160) { t, progress ->
      val freq = 95.0 + 165.0 * sqrt(progress)
      val env = exp(-progress * 3.6)
      (sin(TWO_PI * freq * t) + 0.35 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.50
    }
  }

  // Kanta Beetle Stomp Bonk: Playful percussive woodblock + ascending pop over 140 ms
  fun playEnemyBonk() {
    playRawSfx(SfxCue.ENEMY_BONK, 140) { t, progress ->
      val freq = if (progress < 0.35) 310.0 else 620.0
      val env = exp(-progress * 5.0)
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 2.5 * t)) * env * 0.46
    }
  }

  // Brass Diya Ignition: Warm festive tri-tone sparkle (G5 -> B5 -> D6) over 260 ms
  fun playDiyaLit() {
    playRawSfx(SfxCue.DIYA, 260) { t, progress ->
      val freq = when {
        progress < 0.33 -> 783.99
        progress < 0.66 -> 987.77
        else -> 1174.66
      }
      val env = exp(-progress * 2.8)
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.42
    }
  }

  // Awakening Tree Rumble: Deep earthen growl + woody snap over 320 ms
  fun playTreeRumble() {
    playRawSfx(SfxCue.TREE_RUMBLE, 320) { t, progress ->
      val freq = 68.0 - 24.0 * progress
      val pulse = 0.65 + 0.35 * sin(TWO_PI * 14.0 * t)
      val env = (1.0 - progress) * pulse
      (sin(TWO_PI * freq * t) + 0.4 * sin(TWO_PI * freq * 1.5 * t)) * env * 0.52
    }
  }

  // Vanar Stalker Snarl: Tense descending modulated growl (210 Hz -> 95 Hz) over 280 ms
  fun playBeastRoar() {
    playRawSfx(SfxCue.BEAST_ROAR, 280) { t, progress ->
      val freq = 210.0 - 115.0 * progress + 18.0 * sin(TWO_PI * 22.0 * t)
      val env = sin(PI * progress) * 0.50
      val raw = sin(TWO_PI * freq * t) + 0.45 * sin(TWO_PI * freq * 2.0 * t)
      raw.coerceIn(-1.0, 1.0) * env
    }
  }

  // Stone Guardian Temple Gong: Resonant deep bronze bell + shimmer over 440 ms
  fun playGuardianGong() {
    playRawSfx(SfxCue.GUARDIAN_GONG, 440) { t, progress ->
      val env = exp(-progress * 2.2)
      (sin(TWO_PI * 146.83 * t) * 0.65 + sin(TWO_PI * 220.0 * t) * 0.35 + sin(TWO_PI * 369.99 * t) * 0.2) * env * 0.50
    }
  }

  // Guardian Crystal Shatter: Bright ascending triumph chime (E5 -> G#5 -> B5 -> E6) over 400 ms
  fun playGuardianDefeat() {
    playRawSfx(SfxCue.GUARDIAN_DEFEAT, 400) { t, progress ->
      val freq = when {
        progress < 0.25 -> 659.25
        progress < 0.50 -> 830.61
        progress < 0.75 -> 987.77
        else -> 1318.51
      }
      val env = exp(-progress * 2.4)
      (sin(TWO_PI * freq * t) + 0.3 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.48
    }
  }

  // Sound-Triggered Mechanism: Echoing cavern harmonic ring (D5 -> A5 -> D6) over 320 ms
  fun playEchoSwitch() {
    playRawSfx(SfxCue.ECHO_SWITCH, 320) { t, progress ->
      val freq = when {
        progress < 0.33 -> 587.33
        progress < 0.66 -> 880.00
        else -> 1174.66
      }
      val env = exp(-progress * 3.0) * (0.75 + 0.25 * sin(TWO_PI * 9.0 * t))
      (sin(TWO_PI * freq * t) + 0.28 * sin(TWO_PI * freq * 2.0 * t)) * env * 0.42
    }
  }

  // Monsoon Lightning Strike: Electric crackle + low thunder roll over 300 ms
  fun playLightningStrike() {
    playRawSfx(SfxCue.LIGHTNING, 300) { t, progress ->
      val crack = if (progress < 0.18) sin(TWO_PI * (1450.0 - progress * 4000.0) * t) * (1.0 - progress / 0.18) else 0.0
      val thunder = sin(TWO_PI * (92.0 - 48.0 * progress) * t) * exp(-progress * 3.2)
      (crack * 0.42 + thunder * 0.62).coerceIn(-1.0, 1.0) * 0.50
    }
  }

  // Ruins Mechanism Shift / Pillar Sink: Low stone grind over 200 ms
  fun playMechanismShift() {
    playRawSfx(SfxCue.MECHANISM_SHIFT, 200) { t, progress ->
      val freq = 105.0 - 45.0 * progress
      val grit = sin(TWO_PI * 26.0 * t) * 0.35 + 0.65
      sin(TWO_PI * freq * t) * grit * (1.0 - progress) * 0.42
    }
  }

  // Troll Trap Snap: Sharp spring-loaded spike/trap snap over 95 ms
  fun playTrapSnap() {
    playRawSfx(SfxCue.TRAP_SNAP, 95) { t, progress ->
      val freq = if (progress < 0.35) 640.0 - 220.0 * progress else 340.0 + 160.0 * progress
      val env = exp(-progress * 5.2)
      sin(TWO_PI * freq * t) * env * 0.45
    }
  }

  // Ceiling Block Slam: Punchy heavy thud over 150 ms
  fun playCeilingSlam() {
    playRawSfx(SfxCue.CEILING_SLAM, 150) { t, progress ->
      val freq = 92.0 - 62.0 * progress
      val env = exp(-progress * 4.5)
      (sin(TWO_PI * freq * t) + 0.3 * sin(TWO_PI * freq * 0.5 * t)) * env * 0.56
    }
  }

  // Cheeky Relocating Exit Door Boing/Whoosh over 160 ms
  fun playDoorEscape() {
    playRawSfx(SfxCue.DOOR_ESCAPE, 160) { t, progress ->
      val freq = 360.0 + 420.0 * sin(PI * progress)
      val env = (1.0 - progress * 0.7) * 0.42
      sin(TWO_PI * freq * t) * env
    }
  }

  // Fake Exit Door Comedic "Wah-Wah" Descending Slide over 210 ms
  fun playFakeDoorPoof() {
    playRawSfx(SfxCue.FAKE_DOOR_POOF, 210) { t, progress ->
      val freq = if (progress < 0.45) 440.0 - 80.0 * progress else 340.0 - 160.0 * progress
      val env = (1.0 - progress * 0.5) * 0.42
      (sin(TWO_PI * freq * t) + 0.25 * sin(TWO_PI * freq * 2.0 * t)) * env
    }
  }

  // Fast Troll Death Pop-Splat over 180 ms (snappy, playful, never grating)
  fun playFastTrollDeath() {
    playRawSfx(SfxCue.FAST_TROLL_DEATH, 180) { t, progress ->
      val freq = if (progress < 0.22) 520.0 else 350.0 - 240.0 * progress
      val env = exp(-progress * 3.8)
      sin(TWO_PI * freq * t) * env * 0.50
    }
  }
}
