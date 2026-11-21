package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioFocusController
import com.example.audio.AudioLifecycleState
import com.example.audio.AudioStreamHandle
import com.example.audio.AudioTrackBackend
import com.example.audio.ProceduralAudioEngine
import com.example.audio.SfxCue
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.engine.ChaosGameEngine
import com.example.engine.HapticCue
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase5HudAudioOnboardingTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
  }

  private class FakeStreamHandle : AudioStreamHandle {
    val isPlaying = AtomicBoolean(false)
    val writes = AtomicInteger(0)
    val releaseCalls = AtomicInteger(0)

    override fun play() {
      isPlaying.set(true)
    }

    override fun pause() {
      isPlaying.set(false)
    }

    override fun stop() {
      isPlaying.set(false)
    }

    override fun release() {
      isPlaying.set(false)
      releaseCalls.incrementAndGet()
    }

    override fun writeBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
      writes.incrementAndGet()
      try {
        Thread.sleep(6L)
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        return -1
      }
      return sizeInShorts
    }

    override fun writeNonBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
      writes.incrementAndGet()
      return sizeInShorts
    }
  }

  private class FakeAudioBackend : AudioTrackBackend {
    val musicTracks = mutableListOf<FakeStreamHandle>()
    val sfxTracks = mutableListOf<FakeStreamHandle>()

    override fun createMusicTrack(sampleRate: Int): AudioStreamHandle {
      return FakeStreamHandle().also { musicTracks.add(it) }
    }

    override fun createSfxTrack(sampleRate: Int): AudioStreamHandle {
      return FakeStreamHandle().also { sfxTracks.add(it) }
    }
  }

  private class FakeFocusController : AudioFocusController {
    override fun requestFocus(): Boolean = true
    override fun abandonFocus() {}
  }

  @Test
  fun test01_musicHeadroomLowerThanSfxAndMuteVolumePreferencesRespected() {
    val backend = FakeAudioBackend()
    var fakeTimeMs = 1_000L
    val audio = ProceduralAudioEngine(
        trackBackend = backend,
        context = null,
        focusControllerOverride = FakeFocusController(),
        timeProviderMs = { fakeTimeMs }
    )

    // At equal user volume, background music headroom stays clearly below SFX headroom
    audio.musicVolume = 0.8f
    audio.sfxVolume = 0.8f
    assertTrue(
        "Music effective gain must stay cleanly below SFX gain so music never overpowers SFX",
        audio.computeEffectiveMusicGain() < audio.computeEffectiveSfxGain()
    )

    // Muting SFX suppresses playback writes cleanly
    val sfxTrack = backend.sfxTracks.single()
    val beforeMuteWrites = sfxTrack.writes.get()
    audio.isSfxEnabled = false
    audio.playJump()
    assertEquals(beforeMuteWrites, sfxTrack.writes.get())

    audio.isSfxEnabled = true
    fakeTimeMs += 500L
    audio.playJump()
    assertEquals(beforeMuteWrites + 1, sfxTrack.writes.get())

    // Ducking reduces both gains without silencing them
    val fullMusicGain = audio.computeEffectiveMusicGain()
    audio.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
    assertTrue(audio.computeEffectiveMusicGain() < fullMusicGain)
    audio.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
    assertEquals(fullMusicGain, audio.computeEffectiveMusicGain(), 0.001f)

    audio.release()
    assertEquals(AudioLifecycleState.RELEASED, audio.currentLifecycleState)
  }

  @Test
  fun test02_antiOverlapCooldownsAndBurstSuppressionPreventNoisyAudio() {
    val backend = FakeAudioBackend()
    var fakeTimeMs = 5_000L
    val audio = ProceduralAudioEngine(
        trackBackend = backend,
        context = null,
        focusControllerOverride = FakeFocusController(),
        timeProviderMs = { fakeTimeMs }
    )

    // Rapid duplicate jump calls within cooldown must write only once
    repeat(6) {
      audio.playJump()
      fakeTimeMs += 5L
    }
    assertEquals(1, audio.totalSfxPlayed)
    assertEquals(5, audio.totalSfxSuppressed)

    // After cooldown expires, next jump plays cleanly
    fakeTimeMs += 100L
    audio.playJump()
    assertEquals(2, audio.totalSfxPlayed)

    // Simultaneous trap storm within 100ms burst window caps excessive noisy writes
    fakeTimeMs += 500L
    val playedBeforeStorm = audio.totalSfxPlayed
    audio.playTrapWarning()
    audio.playTrapSnap()
    audio.playCrumble()
    audio.playMechanismShift()
    audio.playDisappear()
    audio.playCeilingSlam()
    assertTrue(
        "Burst limiter must cap overlapping SFX spam in a single 100ms window",
        audio.totalSfxPlayed - playedBeforeStorm <= 4
    )
    assertTrue(audio.totalSfxSuppressed > 5)

    audio.release()
  }

  @Test
  fun test03_retryAndChallengeTransitionAudioSelectionAndDuplicateSuppression() {
    val backend = FakeAudioBackend()
    var fakeTimeMs = 10_000L
    val audio = ProceduralAudioEngine(
        trackBackend = backend,
        context = null,
        focusControllerOverride = FakeFocusController(),
        timeProviderMs = { fakeTimeMs }
    )

    // Transition/retry suppression drops ambient land/crumble/warning bursts right at reset
    audio.onTransitionOrRetry(suppressAmbientMs = 150L, playRetryCue = true)
    assertEquals(SfxCue.RETRY, audio.lastPlayedCue)
    val playedAtRetry = audio.totalSfxPlayed

    audio.playLand()
    audio.playCrumble()
    audio.playTrapWarning()
    assertEquals("Ambient cues during 150ms post-retry window must be suppressed", playedAtRetry, audio.totalSfxPlayed)

    fakeTimeMs += 160L
    audio.playTrapWarning()
    assertEquals(playedAtRetry + 1, audio.totalSfxPlayed)
    assertEquals(SfxCue.TRAP_WARNING, audio.lastPlayedCue)

    // Continuous intermediate challenge completion triggers CHALLENGE_COMPLETE, final challenge triggers VICTORY
    val ch01 = ChallengeCatalog.createLockedConfig("ch_01")!!
    val continuousEngine = ChaosGameEngine(
        level = ch01.levelDefinition,
        audioEngine = audio,
        lockedChallengeConfig = ch01,
        initialLaunchMode = RunLaunchMode.CONTINUOUS_PROGRESSION
    )
    var attempts1 = 0
    while (!continuousEngine.isVictorious && attempts1 < 12) {
      fakeTimeMs += 1_000L
      val gate1 = continuousEngine.tiles.first { it.type == TileType.SUN_GATE }
      continuousEngine.player.x = gate1.x + 10f
      continuousEngine.player.y = gate1.y + 8f
      continuousEngine.update()
      repeat(24) { if (!continuousEngine.isVictorious) continuousEngine.update() }
      attempts1++
    }
    assertTrue(continuousEngine.isVictorious)
    assertEquals(SfxCue.CHALLENGE_COMPLETE, audio.lastPlayedCue)

    val ch12 = ChallengeCatalog.createLockedConfig("ch_12")!!
    val finalEngine = ChaosGameEngine(
        level = ch12.levelDefinition,
        audioEngine = audio,
        lockedChallengeConfig = ch12,
        initialLaunchMode = RunLaunchMode.CONTINUOUS_PROGRESSION
    )
    var attempts12 = 0
    while (!finalEngine.isVictorious && attempts12 < 12) {
      fakeTimeMs += 1_000L
      val gate12 = finalEngine.tiles.first { it.type == TileType.SUN_GATE }
      finalEngine.player.x = gate12.x + 10f
      finalEngine.player.y = gate12.y + 8f
      finalEngine.update()
      repeat(24) { if (!finalEngine.isVictorious) finalEngine.update() }
      attempts12++
    }
    assertTrue(finalEngine.isVictorious)
    assertEquals(SfxCue.VICTORY, audio.lastPlayedCue)

    audio.release()
  }

  @Test
  fun test04_gameplayHudShowsProgressBarTimerMirrorCurseAndPauseQuickToggles() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startNewContinuousRun()
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("hud_top_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_challenge_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_progress_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_timer_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_coins_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_score_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_pause").assertIsDisplayed()

    // Trigger Mirror Curse frames in engine & verify HUD mirrors active gameplay state cleanly
    composeTestRule.activityRule.scenario.onActivity {
      val engine = vm.gameEngine!!
      engine.mirrorCurseFrames = 90
      engine.update()
    }
    composeTestRule.waitUntil(timeoutMillis = 2_000L) {
      composeTestRule.onAllNodesWithTag("hud_mirror_badge").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("hud_mirror_badge").assertIsDisplayed()

    // Ensure top HUD does not overlap virtual jump button
    val hudRect = composeTestRule.onNodeWithTag("hud_top_bar").fetchSemanticsNode().boundsInRoot
    val jumpRect = composeTestRule.onNodeWithTag("control_jump").fetchSemanticsNode().boundsInRoot
    assertTrue("Top HUD must stay cleanly above touch controls", hudRect.bottom <= jumpRect.top)

    // Open Pause Dialog and verify quick audio/vibration toggles work
    composeTestRule.onNodeWithTag("button_pause").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_pause").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_pause_music").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_pause_sfx").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_pause_vibration").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_resume").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_restart").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_return_menu").assertIsDisplayed()

    composeTestRule.onNodeWithTag("button_pause_vibration").performClick()
    composeTestRule.waitForIdle()
    assertFalse(vm.isVibrationEnabled.value)

    composeTestRule.onNodeWithTag("button_resume").performClick()
    composeTestRule.waitForIdle()
    assertFalse(vm.isUserPaused.value)
  }

  @Test
  fun test05_firstLaunchShowsSkippableOnboardingAndReturningPlayerIsNotInterrupted() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.waitForIdle()

    // Brand-new player sees non-blocking Quick Guide card while main menu CTAs remain accessible
    assertFalse(vm.isOnboardingCompleted.value)
    composeTestRule.onNodeWithTag("card_first_launch_onboarding").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_play_level_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_continuous_run").assertIsDisplayed()

    // Step through quick card or Skip -> persists onboarding completion and hides card
    composeTestRule.onNodeWithTag("button_first_launch_next").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_first_launch_skip").performClick()
    composeTestRule.waitForIdle()

    assertTrue(vm.isOnboardingCompleted.value)
    assertTrue(SaveRepository(app).isOnboardingCompleted())
    assertEquals(0, composeTestRule.onAllNodesWithTag("card_first_launch_onboarding").fetchSemanticsNodes().size)

    // Returning player launching fresh ViewModel must NOT be interrupted by onboarding
    val returningVm = GameStateViewModel(app)
    assertTrue(returningVm.isOnboardingCompleted.value)
    assertFalse(returningVm.isOnboardingModalOpen.value)
    assertEquals(AppScreen.MAIN_MENU, returningVm.currentScreen.value)
  }

  @Test
  fun test06_onboardingTourReaccessibleFromHowToPlayAndSettings() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.completeOnboarding(syncCommit = true)
    }
    composeTestRule.waitForIdle()

    // Open How To Play -> launch visual onboarding tour
    composeTestRule.onNodeWithTag("button_how_to_play").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_how_to_play").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_how_to_play_onboarding").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("modal_onboarding").assertIsDisplayed()
    composeTestRule.onNodeWithText("RUN & JUMP TOGETHER").assertIsDisplayed()

    // Step through all 4 steps
    composeTestRule.onNodeWithTag("button_onboarding_next").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("OUTSMART TRICK TILES").assertIsDisplayed()

    composeTestRule.onNodeWithTag("button_onboarding_prev").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("RUN & JUMP TOGETHER").assertIsDisplayed()

    composeTestRule.onNodeWithTag("button_onboarding_next").performClick()
    composeTestRule.onNodeWithTag("button_onboarding_next").performClick()
    composeTestRule.onNodeWithTag("button_onboarding_next").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FAST DEATH, INSTANT RETRY").assertIsDisplayed()

    composeTestRule.onNodeWithTag("button_onboarding_done").performClick()
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("modal_onboarding").fetchSemanticsNodes().size)

    // Navigate back to menu -> open Settings -> Replay Guide -> Skip works cleanly
    composeTestRule.onNodeWithTag("button_how_to_play_back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    // Ensure RemoveDifficultySelection contract holds
    assertEquals(0, composeTestRule.onAllNodesWithText("Difficulty", substring = true, ignoreCase = true).fetchSemanticsNodes().size)

    composeTestRule.onNodeWithTag("button_replay_onboarding").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("modal_onboarding").assertIsDisplayed()

    composeTestRule.onNodeWithTag("button_onboarding_skip").performClick()
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("modal_onboarding").fetchSemanticsNodes().size)
  }

  @Test
  fun test07_audioAndVibrationSettingsPersistAcrossRestartAndSafeHaptics() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("switch_music").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("switch_sfx").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("switch_vibration").assertIsDisplayed().performClick()
    composeTestRule.activityRule.scenario.onActivity {
      vm.setMusicVolume(0.42f)
      vm.setSfxVolume(0.66f)
    }
    composeTestRule.waitForIdle()

    // Save & verify persistence across fresh ViewModel + SaveRepository
    composeTestRule.onNodeWithTag("button_settings_save").performClick()
    composeTestRule.waitForIdle()

    val freshRepo = SaveRepository(app)
    assertFalse(freshRepo.isMusicEnabled())
    assertFalse(freshRepo.isSfxEnabled())
    assertFalse(freshRepo.isVibrationEnabled())
    assertEquals(0.42f, freshRepo.getMusicVolume(), 0.01f)
    assertEquals(0.66f, freshRepo.getSfxVolume(), 0.01f)

    val freshVm = GameStateViewModel(app)
    assertFalse(freshVm.isMusicEnabled.value)
    assertFalse(freshVm.isSfxEnabled.value)
    assertFalse(freshVm.isVibrationEnabled.value)
    assertEquals(0.42f, freshVm.musicVolume.value, 0.01f)
    assertEquals(0.66f, freshVm.sfxVolume.value, 0.01f)

    // Safe vibration triggers when enabled or disabled must never throw
    freshVm.triggerHaptic(HapticCue.DEATH)
    freshVm.setVibrationEnabled(true)
    freshVm.triggerHaptic(HapticCue.VICTORY)
  }
}
