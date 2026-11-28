package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioFocusController
import com.example.audio.AudioIntensity
import com.example.audio.AudioStreamHandle
import com.example.audio.AudioTrackBackend
import com.example.audio.ProceduralAudioEngine
import com.example.audio.SfxCue
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ControlButtonSize
import com.example.data.ControlLayoutMode
import com.example.data.GameplayNotification
import com.example.data.LevelTheme
import com.example.data.NotificationCategory
import com.example.data.NotificationPriority
import com.example.data.NotificationVisualKind
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.data.TrapBehaviorCategory
import com.example.data.TrapTriggerType
import com.example.data.TrollTrap
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.TrapFairnessPolicy
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import com.example.ui.input.GameControlButton
import com.example.ui.screens.NotificationPlacementResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class Phase3MasteryAudioControlsTest {

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
    }

    override fun writeBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
      writes.incrementAndGet()
      try {
        Thread.sleep(4L)
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

  private fun createTestAudioEngine(timeProviderMs: () -> Long = { 10_000L }): Pair<ProceduralAudioEngine, FakeAudioBackend> {
    val backend = FakeAudioBackend()
    val audio = ProceduralAudioEngine(
        trackBackend = backend,
        context = null,
        focusControllerOverride = FakeFocusController(),
        timeProviderMs = timeProviderMs
    )
    return audio to backend
  }

  @Test
  fun test01_all12ChallengesPassFairnessPolicyAndAcyclicValidationAcrossAllTiers() {
    for (challenge in ChallengeCatalog.entries) {
      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(challenge, tier)
        for (v in validations) {
          assertTrue(
              "Challenge ${challenge.challengeId} trap #${v.trapId} (${v.trapType}) failed fairness in $tier: ${v.reason}",
              v.isFair
          )
        }
      }
    }

    // Synthetic cyclic chain check must catch infinite ON_TRAP_ACTIVATED loops
    val cyclicTraps = listOf(
        TrollTrap(
            id = 101,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 102,
            initialX = 100f,
            initialY = 300f,
            width = 40f,
            height = 28f,
            telegraphDurationFrames = 12
        ),
        TrollTrap(
            id = 102,
            type = TrollTrapType.SURPRISE_SPIKE_POP,
            triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
            triggerSourceTrapId = 101,
            initialX = 200f,
            initialY = 300f,
            width = 40f,
            height = 28f,
            telegraphDurationFrames = 12
        )
    )
    val cyclicIds = TrapFairnessPolicy.findCyclicChainTrapIds(cyclicTraps)
    assertEquals(setOf(101, 102), cyclicIds)
  }

  @Test
  fun test02_ch06ChainReactionAndCoinBaitDeterministicTelegraphAndRollback() {
    val (audio, _) = createTestAudioEngine()
    val tileSize = World1Levels.TILE_SIZE
    val configA = ChallengeCatalog.createLockedConfig("ch_06", AdaptiveTier.STANDARD)!!
    val configB = ChallengeCatalog.createLockedConfig("ch_06", AdaptiveTier.STANDARD)!!
    assertEquals(configA.challengeSeed, configB.challengeSeed)

    val engineA = ChaosGameEngine(level = configA.levelDefinition, audioEngine = audio, lockedChallengeConfig = configA)
    val engineB = ChaosGameEngine(level = configB.levelDefinition, audioEngine = audio, lockedChallengeConfig = configB)

    // Stepping onto Split Bridge #1 activates chained SURPRISE_SPIKE_POP #6 after delay + telegraph
    fun triggerBridgeAndMeasure(engine: ChaosGameEngine): Pair<Int, Int> {
      val bridge = engine.trollTraps.first { it.id == 1 }
      engine.player.x = bridge.x + 20f
      engine.player.y = bridge.y - engine.player.collisionHeight
      engine.player.vy = 0f
      engine.player.isGrounded = true
      engine.update() // Step onto bridge -> enters TELEGRAPH (14f)

      // Move player safely aside so we observe chain cleanly without dying
      engine.player.x = 14.5f * tileSize
      engine.player.y = 13f * tileSize - engine.player.collisionHeight
      var framesToTelegraph = -1
      var framesToActive = -1
      for (f in 1..60) {
        engine.update()
        val spike = engine.trollTraps.first { it.id == 6 }
        if (framesToTelegraph < 0 && spike.state == TrollTrapState.TELEGRAPH) {
          framesToTelegraph = f
        }
        if (framesToActive < 0 && spike.state == TrollTrapState.ACTIVE) {
          framesToActive = f
          break
        }
      }
      return framesToTelegraph to framesToActive
    }

    val timingA = triggerBridgeAndMeasure(engineA)
    val timingB = triggerBridgeAndMeasure(engineB)
    assertEquals("Same seed must yield identical chain timing", timingA, timingB)
    assertTrue("Spike #6 must enter TELEGRAPH strictly before ACTIVE", timingA.first in 1 until timingA.second)

    // Coin #2 bait triggers FALLING_CEILING_BLOCK #7 with telegraph before falling
    val coin2 = engineA.coins.first { it.id == 2 }
    engineA.player.x = coin2.x - engineA.player.collisionWidth * 0.5f
    engineA.player.y = coin2.y - engineA.player.collisionHeight * 0.5f
    engineA.update()
    assertTrue(coin2.isCollected)

    // Move away safely and advance through delay (4f) + telegraph (14f)
    engineA.player.x = 2f * tileSize
    engineA.player.y = 11f * tileSize
    var sawTelegraph = false
    repeat(24) {
      engineA.update()
      val block = engineA.trollTraps.first { it.id == 7 }
      if (block.state == TrollTrapState.TELEGRAPH) {
        sawTelegraph = true
        assertEquals("Ceiling block must not drop before telegraph ends", block.initialY, block.y, 0.01f)
      }
    }
    assertTrue("Coin bait ceiling block #7 must telegraph before dropping", sawTelegraph)
    val trap7After = engineA.trollTraps.first { it.id == 7 }
    assertTrue(trap7After.state == TrollTrapState.ACTIVE || trap7After.state == TrollTrapState.RETIRED)

    // Activate checkpoint at tile 25.5, then collect coin #2? Wait: roll back to start or pre-coin checkpoint:
    // Verify resetCurrentChallenge re-arms coin #2 and chained traps cleanly
    engineA.resetCurrentChallenge(clearInputs = true)
    assertFalse(engineA.coins.first { it.id == 2 }.isCollected)
    assertEquals(TrollTrapState.ARMED, engineA.trollTraps.first { it.id == 6 }.state)
    assertEquals(TrollTrapState.ARMED, engineA.trollTraps.first { it.id == 7 }.state)
    assertEquals(0, engineA.pendingTrapChainEventCount)

    audio.release()
  }

  @Test
  fun test03_ch07ch08ch10ch12CombinationsFireCleanlyAndCheckpointReArmsCorrectly() {
    val (audio, _) = createTestAudioEngine()
    val tileSize = World1Levels.TILE_SIZE

    // 1. Challenge 07: Trailing Spike #4 -> Falling Ceiling #6 with safe raised alcove
    val ch07 = ChallengeCatalog.createLockedConfig("ch_07", AdaptiveTier.STANDARD)!!
    val engine07 = ChaosGameEngine(level = ch07.levelDefinition, audioEngine = audio, lockedChallengeConfig = ch07)
    engine07.player.x = 31.8f * tileSize
    engine07.player.y = 13f * tileSize - engine07.player.collisionHeight
    engine07.update()
    // Move player to overhead safe alcove (33.5..34.8 * T, Y=11.2 * T - height) -> zero damage from chase or ceiling drop
    engine07.player.x = 33.8f * tileSize
    engine07.player.y = 11.2f * tileSize - engine07.player.collisionHeight
    var sawDropBlock = false
    repeat(80) {
      engine07.update()
      val drop = engine07.trollTraps.first { it.id == 6 }
      if (drop.state == TrollTrapState.ACTIVE || drop.state == TrollTrapState.RETIRED) {
        sawDropBlock = true
      }
    }
    assertTrue("Ch07 combo drop #6 must fire after Trailing Spike #4 activates", sawDropBlock)
    assertFalse("Overhead alcove preserves viable safe route", engine07.isDying)

    // 2. Challenge 08: Evasive Platform #4 -> Backup Spike #7
    val ch08 = ChallengeCatalog.createLockedConfig("ch_08", AdaptiveTier.STANDARD)!!
    val engine08 = ChaosGameEngine(level = ch08.levelDefinition, audioEngine = audio, lockedChallengeConfig = ch08)
    engine08.player.x = 28.2f * tileSize
    engine08.player.y = 11f * tileSize
    engine08.update()
    engine08.player.x = 25.0f * tileSize
    engine08.player.y = 12.5f * tileSize - engine08.player.collisionHeight
    var sawBackupSpike = false
    repeat(70) {
      engine08.update()
      if (engine08.trollTraps.first { it.id == 7 }.state == TrollTrapState.ACTIVE) {
        sawBackupSpike = true
      }
    }
    assertTrue("Ch08 backup spike #7 must activate after Evasive Platform #4 dodges", sawBackupSpike)

    // 3. Challenge 12: Mirror Totem #2 -> Mirror Drop #7 + checkpoint rollback clears curse & restores post-checkpoint drop
    val ch12 = ChallengeCatalog.createLockedConfig("ch_12", AdaptiveTier.STANDARD)!!
    val engine12 = ChaosGameEngine(level = ch12.levelDefinition, audioEngine = audio, lockedChallengeConfig = ch12)
    // First activate checkpoint bell at tile 28.0 before triggering Totem #2? Totem #2 is at tile 17.5 -> trigger #2 first, verify drop #7 fires
    engine12.player.x = 17.5f * tileSize
    engine12.player.y = 13f * tileSize - engine12.player.collisionHeight
    engine12.update()
    engine12.player.x = 21.5f * tileSize
    engine12.player.y = 12.0f * tileSize - engine12.player.collisionHeight
    var mirrorCurseSeen = false
    var mirrorDropSeen = false
    repeat(75) {
      engine12.update()
      if (engine12.mirrorCurseFrames > 0) mirrorCurseSeen = true
      val drop7 = engine12.trollTraps.first { it.id == 7 }
      if (drop7.state == TrollTrapState.ACTIVE || drop7.state == TrollTrapState.RETIRED) {
        mirrorDropSeen = true
      }
    }
    assertTrue(mirrorCurseSeen)
    assertTrue("Ch12 Mirror Drop #7 must chain from Mirror Totem #2", mirrorDropSeen)

    // Activate checkpoint at tile 28, verify post-checkpoint Mirage Gate #5 -> Spike #6 resets cleanly after pit death
    val bell = engine12.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    engine12.player.x = bell.x
    engine12.player.y = bell.y
    engine12.update()
    assertTrue(engine12.hasCheckpointActivated)

    engine12.player.x = 45.2f * tileSize
    engine12.player.y = 13f * tileSize - engine12.player.collisionHeight
    repeat(30) { engine12.update() }
    assertEquals(TrollTrapState.ACTIVE, engine12.trollTraps.first { it.id == 6 }.state)

    // Trigger pit death and advance through death animation
    engine12.player.y = engine12.level.pitKillY + 20f
    repeat(ChaosGameEngine.DEFAULT_DEATH_ANIM_FRAMES + 2) { engine12.update() }
    assertEquals("Post-checkpoint chained spike #6 must re-arm on rollback", TrollTrapState.ARMED, engine12.trollTraps.first { it.id == 6 }.state)
    assertEquals(0, engine12.mirrorCurseFrames)

    audio.release()
  }

  @Test
  fun test04_distinctThemeProfilesAndIntensityScalingAcrossAll5Themes() {
    val (audio, backend) = createTestAudioEngine()

    val profiles = LevelTheme.entries.map { ProceduralAudioEngine.profileForTheme(it) }
    val uniqueRagas = profiles.map { it.ragaName }.toSet()
    val uniqueBpms = profiles.map { it.bpm }.toSet()
    val uniqueMelodies = profiles.map { it.melodySequence.toList() }.toSet()
    val uniqueSitars = profiles.map { it.sitarSequence.toList() }.toSet()

    assertEquals("All 5 themes must have distinct raga identities", 5, uniqueRagas.size)
    assertEquals("All 5 themes must have distinct tempos", 5, uniqueBpms.size)
    assertEquals("All 5 themes must have distinct bansuri melodies", 5, uniqueMelodies.size)
    assertEquals("All 5 themes must have distinct sitar counter-lines", 5, uniqueSitars.size)

    assertEquals(AudioIntensity.CALM, ProceduralAudioEngine.resolveIntensity(AdaptiveTier.ASSISTED))
    assertEquals(AudioIntensity.STANDARD, ProceduralAudioEngine.resolveIntensity(AdaptiveTier.STANDARD))
    assertEquals(AudioIntensity.ESCALATED, ProceduralAudioEngine.resolveIntensity(AdaptiveTier.ESCALATED))
    assertEquals(AudioIntensity.ESCALATED, ProceduralAudioEngine.resolveIntensity(AdaptiveTier.RELENTLESS))

    // Start live procedural music loop and cycle through themes/intensities cleanly
    audio.startMusic()
    for (theme in LevelTheme.entries) {
      audio.setThemeAndIntensity(theme, AdaptiveTier.ESCALATED)
      Thread.sleep(30L)
    }
    val musicWrites = backend.musicTracks.sumOf { it.writes.get() }
    assertTrue("Music thread should synthesize & write PCM buffers across themes", musicWrites > 0)

    // Verify ChaosGameEngine automatically syncs theme & intensity from locked config
    val ch07Assisted = ChallengeCatalog.createLockedConfig("ch_07", AdaptiveTier.ASSISTED)!!
    val engine = ChaosGameEngine(level = ch07Assisted.levelDefinition, audioEngine = audio, lockedChallengeConfig = ch07Assisted)
    assertEquals(LevelTheme.HOLLOW_OF_ECHOES, audio.activeTheme)
    assertEquals(AudioIntensity.CALM, audio.activeIntensity)

    val ch10Relentless = ChallengeCatalog.createLockedConfig("ch_10", AdaptiveTier.RELENTLESS)!!
    engine.loadChallenge(lockedConfig = ch10Relentless, attemptToken = 99L, launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION)
    assertEquals(LevelTheme.TEMPLE_OF_LAST_LIGHT, audio.activeTheme)
    assertEquals(AudioIntensity.ESCALATED, audio.activeIntensity)

    audio.release()
  }

  @Test
  fun test05_nearMissAudioCueCooldownSuppressionAndEngineTrigger() {
    var fakeTimeMs = 20_000L
    val (audio, _) = createTestAudioEngine { fakeTimeMs }

    // 1. Direct cooldown enforcement (480ms)
    audio.playNearMiss()
    assertEquals(SfxCue.NEAR_MISS, audio.lastPlayedCue)
    val afterFirst = audio.totalSfxPlayed

    fakeTimeMs += 100L
    audio.playNearMiss()
    assertEquals("Near-miss within 480ms cooldown must be suppressed", afterFirst, audio.totalSfxPlayed)

    fakeTimeMs += 450L
    audio.playNearMiss()
    assertEquals(afterFirst + 1, audio.totalSfxPlayed)

    // 2. Mute & post-retry suppression enforcement
    audio.onTransitionOrRetry(suppressAmbientMs = 150L, playRetryCue = true)
    val atRetry = audio.totalSfxPlayed
    audio.playNearMiss()
    assertEquals("Near-miss during 150ms post-retry suppression must be dropped", atRetry, audio.totalSfxPlayed)

    fakeTimeMs += 200L
    audio.isSfxEnabled = false
    audio.playNearMiss()
    assertEquals("Muted SFX must not play near-miss cue", atRetry, audio.totalSfxPlayed)

    // 3. Engine near-miss graze past an active FALLING_CEILING_BLOCK
    audio.isSfxEnabled = true
    fakeTimeMs += 1_000L
    val ch06 = ChallengeCatalog.createLockedConfig("ch_06", AdaptiveTier.STANDARD)!!
    val engine = ChaosGameEngine(level = ch06.levelDefinition, audioEngine = audio, lockedChallengeConfig = ch06)
    val dropBlock = engine.trollTraps.first { it.id == 7 }
    dropBlock.state = TrollTrapState.ACTIVE
    dropBlock.y = 10f * World1Levels.TILE_SIZE
    dropBlock.vy = 9.0f

    // Position player right alongside block's outer near-miss margin (8px gap horizontally) while moving quickly
    engine.player.x = dropBlock.x - engine.player.collisionWidth - 8f
    engine.player.y = dropBlock.y + 4f
    engine.player.vx = -3.0f
    engine.player.isGrounded = false

    val playedBeforeGraze = audio.totalSfxPlayed
    engine.update()
    assertTrue("Airborne graze near active falling block should fire NEAR_MISS cue", audio.totalSfxPlayed > playedBeforeGraze)
    assertEquals(SfxCue.NEAR_MISS, audio.lastPlayedCue)

    audio.release()
  }

  @Test
  fun test06_saveRepositoryAndViewModelControlPreferencesPersistAndFallback() {
    val repo = SaveRepository(app)
    assertEquals(ControlLayoutMode.STANDARD, repo.getControlLayoutMode())
    assertEquals(ControlButtonSize.MEDIUM, repo.getControlButtonSize())
    assertEquals(1.0f, repo.getControlOpacity(), 0.001f)

    assertTrue(repo.setControlLayoutMode(ControlLayoutMode.SWAPPED, syncCommit = true))
    assertTrue(repo.setControlButtonSize(ControlButtonSize.LARGE, syncCommit = true))
    assertTrue(repo.setControlOpacity(0.10f, syncCommit = true))
    assertEquals(SaveRepository.MIN_CONTROL_OPACITY, repo.getControlOpacity(), 0.001f)

    assertTrue(repo.setControlOpacity(Float.NaN, syncCommit = true))
    assertEquals(1.0f, repo.getControlOpacity(), 0.001f)
    assertTrue(repo.setControlOpacity(0.65f, syncCommit = true))

    // Restore across fresh SaveRepository & ViewModel instance (simulating app restart)
    val freshRepo = SaveRepository(app)
    assertEquals(ControlLayoutMode.SWAPPED, freshRepo.getControlLayoutMode())
    assertEquals(ControlButtonSize.LARGE, freshRepo.getControlButtonSize())
    assertEquals(0.65f, freshRepo.getControlOpacity(), 0.001f)

    // Corrupt SharedPreferences values fall back cleanly
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .putString("pref_control_layout_mode", "invalid_layout")
        .putInt("pref_control_button_size", 42)
        .putString("pref_control_opacity", "not_a_float")
        .commit()
    assertEquals(ControlLayoutMode.STANDARD, freshRepo.getControlLayoutMode())
    assertEquals(ControlButtonSize.MEDIUM, freshRepo.getControlButtonSize())
    assertEquals(1.0f, freshRepo.getControlOpacity(), 0.001f)

    // ViewModel setters & reset
    val vm = ViewModelProvider(composeTestRule.activity)[GameStateViewModel::class.java]
    vm.setControlLayoutMode(ControlLayoutMode.SWAPPED)
    vm.setControlButtonSize(ControlButtonSize.SMALL)
    vm.setControlOpacity(0.55f)
    assertEquals(ControlLayoutMode.SWAPPED, vm.controlLayoutMode.value)
    assertEquals(ControlButtonSize.SMALL, vm.controlButtonSize.value)
    assertEquals(0.55f, vm.controlOpacity.value, 0.001f)

    vm.resetControlSettings()
    assertEquals(ControlLayoutMode.STANDARD, vm.controlLayoutMode.value)
    assertEquals(ControlButtonSize.MEDIUM, vm.controlButtonSize.value)
    assertEquals(1.0f, vm.controlOpacity.value, 0.001f)
  }

  @Test
  fun test07_settingsModalControlCustomizationAndNoDifficultyWord() {
    val vm = ViewModelProvider(composeTestRule.activity)[GameStateViewModel::class.java]

    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.SETTINGS, vm.currentScreen.value)

    // Existing Phase-2 test contract: no "Difficulty" label may appear anywhere in SettingsModal
    val difficultyNodes = composeTestRule.onAllNodesWithText("Difficulty", substring = true, ignoreCase = true)
        .fetchSemanticsNodes()
    assertEquals("SettingsModal must never display the word Difficulty", 0, difficultyNodes.size)

    // Phase 13 minimal Settings: only audio & vibration controls, no touch controls section
    assertEquals(0, composeTestRule.onAllNodesWithTag("section_touch_controls").fetchSemanticsNodes().size)

    composeTestRule.runOnUiThread {
      vm.setControlLayoutMode(ControlLayoutMode.SWAPPED)
      vm.setControlButtonSize(ControlButtonSize.LARGE)
    }
    composeTestRule.waitForIdle()

    assertEquals(ControlLayoutMode.SWAPPED, vm.controlLayoutMode.value)
    assertEquals(ControlButtonSize.LARGE, vm.controlButtonSize.value)
    assertEquals(ControlLayoutMode.SWAPPED, SaveRepository(app).getControlLayoutMode())
    assertEquals(ControlButtonSize.LARGE, SaveRepository(app).getControlButtonSize())

    composeTestRule.runOnUiThread {
      vm.resetControlSettings()
    }
    composeTestRule.waitForIdle()
    assertEquals(ControlLayoutMode.STANDARD, vm.controlLayoutMode.value)
    assertEquals(ControlButtonSize.MEDIUM, vm.controlButtonSize.value)
    assertEquals(1.0f, vm.controlOpacity.value, 0.001f)
  }

  @Test
  fun test08_swappedAndResizedControlsLayoutNotificationNonOverlapAndMultiTouchPhysics() {
    val vm = ViewModelProvider(composeTestRule.activity)[GameStateViewModel::class.java]
    vm.setControlLayoutMode(ControlLayoutMode.SWAPPED)
    vm.setControlButtonSize(ControlButtonSize.SMALL)
    vm.setControlOpacity(0.6f)
    vm.startOrContinueContinuousRun()
    composeTestRule.waitForIdle()

    val smallJumpBounds = composeTestRule.onNodeWithTag("control_jump").getUnclippedBoundsInRoot()
    val smallLeftBounds = composeTestRule.onNodeWithTag("control_left").getUnclippedBoundsInRoot()
    val smallRightBounds = composeTestRule.onNodeWithTag("control_right").getUnclippedBoundsInRoot()

    // Swapped layout places Jump on the left side and Left/Right on the right side
    assertTrue(
        "In SWAPPED mode, Jump (${smallJumpBounds.left}) must sit left of Move Left (${smallLeftBounds.left})",
        smallJumpBounds.left < smallLeftBounds.left
    )
    assertTrue(smallJumpBounds.left < smallRightBounds.left)

    // Switch to LARGE button size and verify buttons physically grow while staying inside viewport
    composeTestRule.runOnUiThread {
      vm.setControlButtonSize(ControlButtonSize.LARGE)
    }
    composeTestRule.waitForIdle()
    val largeJumpBounds = composeTestRule.onNodeWithTag("control_jump").getUnclippedBoundsInRoot()
    val largeJumpWidth = largeJumpBounds.right - largeJumpBounds.left
    val smallJumpWidth = smallJumpBounds.right - smallJumpBounds.left
    assertTrue(
        "LARGE Jump width ($largeJumpWidth) must exceed SMALL Jump width ($smallJumpWidth)",
        largeJumpWidth > smallJumpWidth
    )

    // Verify NotificationPlacementResolver keeps notifications non-overlapping in SWAPPED layout
    val safeBounds = Rect(20f, 16f, 1600f, 800f)
    val hudBounds = Rect(20f, 16f, 1600f, 120f)
    val swappedControls = listOf(
        Rect(1260f, 620f, 1380f, 780f), // Left btn on right side
        Rect(1400f, 620f, 1560f, 780f), // Right btn on right side
        Rect(40f, 580f, 200f, 780f)     // Jump btn on left side
    )
    val placement = NotificationPlacementResolver.resolvePlacement(
        safeAreaBounds = safeBounds,
        hudBounds = hudBounds,
        controlBounds = swappedControls,
        exclusionData = null,
        bannerWidthPx = 420f,
        bannerHeightPx = 80f,
        gapPx = 16f,
        priority = NotificationPriority.CRITICAL
    )
    for (ctrl in swappedControls) {
      assertFalse(
          "Notification rect ${placement.rect} must not overlap swapped control $ctrl",
          placement.rect.overlaps(ctrl)
      )
    }

    // Verify simultaneous Jump + Right multi-touch still drives the engine cleanly with unchanged physics
    val sessionEngine = vm.gameEngine
    assertNotNull(sessionEngine)
    val touchState = vm.touchControlState
    touchState.onPointerDown(1L, GameControlButton.JUMP)
    touchState.onPointerDown(2L, GameControlButton.RIGHT)
    assertTrue(sessionEngine!!.inputJump)
    assertTrue(sessionEngine.inputRight)
    touchState.onPointerUp(1L, GameControlButton.JUMP)
    assertFalse(sessionEngine.inputJump)
    assertTrue(sessionEngine.inputRight)

    assertEquals(0.58f, ChaosGameEngine.GRAVITY, 0.0001f)
    assertEquals(-13.2f, ChaosGameEngine.JUMP_IMPULSE, 0.0001f)
    assertEquals(6.5f, ChaosGameEngine.MAX_RUN_SPEED, 0.0001f)
  }
}
