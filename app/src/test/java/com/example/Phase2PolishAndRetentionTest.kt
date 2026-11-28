package com.example

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeRecord
import com.example.data.ContinuousRunPersistedState
import com.example.data.Level13ExitIsLying
import com.example.data.Level14FloorHasPlans
import com.example.data.Level15AlmostThere
import com.example.data.PlayableCharacter
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.data.TrapTriggerType
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.TrapFairnessPolicy
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase2PolishAndRetentionTest {

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

  private fun buildEngine(
      challengeId: String,
      character: PlayableCharacter = PlayableCharacter.DEFAULT
  ): ChaosGameEngine {
    val config = ChallengeCatalog.createLockedConfig(challengeId)!!
    return ChaosGameEngine(
        level = config.levelDefinition,
        audioEngine = ProceduralAudioEngine(),
        lockedChallengeConfig = config,
        initialLaunchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
        initialSelectedCharacter = character
    )
  }

  private fun settleDeath(engine: ChaosGameEngine) {
    repeat(ChaosGameEngine.DEFAULT_DEATH_ANIM_FRAMES + 2) {
      if (engine.isDying) engine.update()
    }
    assertFalse("Death animation must finish cleanly", engine.isDying)
  }

  // --- TASK A: Save Reliability, Non-Blocking Render Thread & Honest Failure Reporting ---

  @Test
  fun test01_saveRepositoryReturnsFalseHonestlyWhenCommitFails() {
    val basePrefs = app.getSharedPreferences("chaos_run_fail_test", Context.MODE_PRIVATE)
    basePrefs.edit().clear().commit()

    val failingContext = object : ContextWrapper(app) {
      override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return object : SharedPreferences by basePrefs {
          override fun edit(): SharedPreferences.Editor {
            val realEditor = basePrefs.edit()
            lateinit var proxy: SharedPreferences.Editor
            proxy = java.lang.reflect.Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)
            ) { _, method, args ->
              when (method.name) {
                "commit" -> false
                else -> {
                  val result = if (args == null) method.invoke(realEditor) else method.invoke(realEditor, *args)
                  if (result === realEditor) proxy else result
                }
              }
            } as SharedPreferences.Editor
            return proxy
          }
        }
      }
    }

    val failingRepo = SaveRepository(failingContext)
    val state = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = true,
        isRunInForeground = true,
        isRunCompleted = false,
        activeChallengeId = "ch_01",
        activeChallengeSeed = 101L,
        activeConfigVersion = 1,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 1,
        completedChallengeIds = emptySet(),
        challengeRecords = emptyMap(),
        cumulativeRunTimerMs = 1500L,
        cumulativeDeaths = 1,
        cumulativeScore = 100,
        bestRunTimeMs = 0L,
        bestRunScore = 0,
        bestRunLeastDeaths = -1
    )
    val rec = ChallengeRecord("ch_01", completed = true, bestTimeMs = 1500L, bestScore = 100, stars = 3, leastDeaths = 1)

    assertFalse(failingRepo.saveContinuousRunState(state, syncCommit = true))
    assertFalse(failingRepo.saveChallengeCompletionBoundary(state, "ch_01", rec, syncCommit = true))
    assertFalse(failingRepo.saveReplayChallengeRecordIfImproved("ch_01", rec, syncCommit = true))
    assertFalse(failingRepo.setContinuousRunInForeground(false, syncCommit = true))
    assertFalse(failingRepo.setOnboardingCompleted(true, syncCommit = true))
    assertFalse(failingRepo.setSelectedCharacter(PlayableCharacter.VEGA, syncCommit = true))
  }

  @Test
  fun test02_renderThreadPersistenceNeverBlocksRenderLoopAndPersistsReliably() {
    val vm = ViewModelProvider(
        composeTestRule.activity,
        ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )[GameStateViewModel::class.java]

    composeTestRule.runOnUiThread {
      vm.startNewContinuousRun()
    }
    composeTestRule.waitForIdle()
    val engine = vm.gameEngine!!

    val completedOnRenderThread = AtomicBoolean(false)
    val renderLatch = CountDownLatch(1)
    val renderSimThread = Thread({
      // Trigger both a trap death attribution and completion from ChaosRunRenderThread
      engine.handlePlayerDeath("trap_1_SIM")
      val startChallengeId = engine.activeChallengeId
      repeat(10) {
        if (engine.activeChallengeId != startChallengeId || engine.isVictorious) return@repeat
        val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
        engine.player.x = gate.x + 28f
        engine.player.y = gate.y + 40f
        engine.update()
      }
      completedOnRenderThread.set(true)
      renderLatch.countDown()
    }, "ChaosRunRenderThread")

    renderSimThread.start()
    assertTrue("Render thread simulation must finish without blocking", renderLatch.await(3, TimeUnit.SECONDS))
    assertTrue(completedOnRenderThread.get())

    // Flushing persistence guarantees serialized commit completed on disk
    vm.flushPendingPersistence()
    val persisted = SaveRepository(app).loadContinuousRunState()
    assertTrue("ch_01 must be persisted after render-thread completion", persisted.completedChallengeIds.contains("ch_01"))
    assertEquals("ch_02", persisted.activeChallengeId)
    assertFalse("Save error flag must remain false when commit succeeds", vm.lastSaveError.value)
  }

  @Test
  fun test03_hostPausePersistsForegroundRecoveryBoundaryCleanly() {
    val vm = ViewModelProvider(
        composeTestRule.activity,
        ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )[GameStateViewModel::class.java]

    composeTestRule.runOnUiThread {
      vm.startNewContinuousRun()
      vm.onHostPaused()
    }
    composeTestRule.waitForIdle()

    val persisted = SaveRepository(app).loadContinuousRunState()
    assertTrue(persisted.isRunActive)
    assertTrue(persisted.isRunInForeground)
    assertEquals(SaveRepository.SAVE_SCHEMA_VERSION, persisted.schemaVersion)
  }

  // --- TASK B: Early Challenge Checkpoints (ch_03, ch_04, ch_05; ch_02 evaluated separately) ---

  @Test
  fun test04_checkpointsExistAndRollBackSafelyInCh03Ch04Ch05() {
    val ch02 = ChallengeCatalog.findById("ch_02")!!.buildLevel()
    assertEquals("ch_02 stays punchy without a real bell next to Fake Bell", 0, ch02.tiles.count { it.type == TileType.CHECKPOINT_BELL })

    for (id in listOf("ch_03", "ch_04", "ch_05")) {
      val engine = buildEngine(id)
      val bells = engine.tiles.filter { it.type == TileType.CHECKPOINT_BELL }
      assertEquals("$id must contain exactly 1 CHECKPOINT_BELL", 1, bells.size)
      val bell = bells.first()
      assertFalse("$id CHECKPOINT_BELL must be non-solid", bell.isSolid)

      // Walk player directly over the checkpoint bell
      engine.player.x = bell.x + 4f
      engine.player.y = bell.y + bell.height - engine.player.collisionHeight
      engine.player.isGrounded = true
      engine.update()
      assertTrue("$id checkpoint must activate cleanly", engine.hasCheckpointActivated)

      // Die afterward and ensure instant respawn back at the checkpoint safely
      engine.handlePlayerDeath("test_post_checkpoint")
      assertEquals(bell.x + 8f, engine.player.x, 4f)
      // Step 20 frames standing still to prove checkpoint spot is trap-free & solid
      repeat(20) { engine.update() }
      assertFalse("$id checkpoint respawn spot must remain safe for idle player", engine.isDying)
      assertTrue(engine.player.isGrounded)
    }
  }

  // --- TASK C: First-Time Non-Intrusive Gameplay Control Hint ---

  @Test
  fun test05_firstRunControlHintAppearsOnceOnCh01AndDismissesOnInteraction() {
    composeTestRule.waitForIdle()
    // Home screen stays clean & minimal
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()

    // First ch_01 attempt shows inline control hint pill
    composeTestRule.onNodeWithTag("first_run_control_hint").assertIsDisplayed()
    composeTestRule.onNodeWithText("LEFT / RIGHT").assertIsDisplayed()
    composeTestRule.onNodeWithText("JUMP").assertIsDisplayed()

    // Player presses movement -> hint fades cleanly and persists onboarding completion
    val vm = ViewModelProvider(
        composeTestRule.activity,
        ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )[GameStateViewModel::class.java]
    vm.gameEngine!!.inputRight = true

    composeTestRule.waitUntil(timeoutMillis = 2_000L) {
      composeTestRule.onAllNodesWithTag("first_run_control_hint").fetchSemanticsNodes().isEmpty()
    }
    assertTrue(vm.hasDismissedFirstRunHint.value)
    assertTrue(SaveRepository(app).isOnboardingCompleted())

    // Returning user relaunch must NOT show the first-run control hint again
    vm.returnToMainMenu()
    composeTestRule.waitForIdle()
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      scenario.onActivity { act ->
        val reloadedVm = ViewModelProvider(
            act,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[GameStateViewModel::class.java]
        reloadedVm.startNewContinuousRun()
        assertEquals("ch_01", reloadedVm.gameEngine?.activeChallengeId)
        assertTrue(reloadedVm.isOnboardingCompleted.value)
        assertTrue(reloadedVm.hasDismissedFirstRunHint.value)
      }
    } finally {
      scenario.close()
    }
  }

  // --- TASK D: Character-Aware Victory Messages ---

  @Test
  fun test06_characterAwareVictoryMessagesMatchAllFourPlayableCharacters() {
    val ch03Level = Level13ExitIsLying.build()
    val whispers = PlayableCharacter.entries.map { character ->
      val engine = buildEngine("ch_03", character)
      var capturedWhisper = ""
      val customEngine = ChaosGameEngine(
          level = ch03Level,
          audioEngine = ProceduralAudioEngine(),
          lockedChallengeConfig = ChallengeCatalog.createLockedConfig("ch_03")!!,
          initialSelectedCharacter = character,
          onWhisperEvent = { _, msg -> capturedWhisper = msg }
      )
      repeat(10) {
        if (customEngine.isVictorious) return@repeat
        val gate = customEngine.tiles.first { it.type == TileType.SUN_GATE }
        customEngine.player.x = gate.x + 28f
        customEngine.player.y = gate.y + 30f
        customEngine.update()
      }

      assertTrue("Engine must win on gate touch", customEngine.isVictorious)
      assertTrue(
          "Victory whisper for ${character.displayName} must include character name: '$capturedWhisper'",
          capturedWhisper.contains(character.displayName)
      )
      assertEquals(ChaosGameEngine.resolveVictoryWhisper(ch03Level, character), capturedWhisper)
      assertEquals(character, engine.selectedCharacter)
      capturedWhisper
    }

    assertEquals("All 4 characters must produce distinct ch_03 victory messages", 4, whispers.toSet().size)
    assertFalse("Non-Aarav whispers must not mention Aarav", whispers.drop(1).any { it.contains("Aarav") })
  }

  // --- TASK E: Coin Betrayal (ch_08) & Fake Exit (ch_12) Combinations + Full Fairness Audit ---

  @Test
  fun test07_coinBetrayalInCh08IsOptionalSurvivableAndRollsBackAtCheckpoint() {
    val engine = buildEngine("ch_08")
    val t = World1Levels.TILE_SIZE
    val cp = engine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    val coin5 = engine.coins.first { it.id == 5 }
    val betrayalTrap = engine.trollTraps.first { it.id == 6 }
    assertEquals(TrapTriggerType.ON_COIN_COLLECTED, betrayalTrap.triggerType)
    assertEquals(5, betrayalTrap.triggerCoinId)

    // Activate checkpoint
    engine.player.x = cp.x + 4f
    engine.player.y = cp.y + cp.height - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.update()
    assertTrue(engine.hasCheckpointActivated)

    // Path A: Skipping optional Coin #5 walks straight to SUN_GATE safely with 0 deaths
    engine.player.x = 38.2f * t
    engine.player.y = 12.5f * t - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.inputRight = true
    repeat(40) {
      if (!engine.isVictorious) engine.update()
    }
    engine.inputRight = false
    assertFalse("Optional coin must not be collected while walking normally underneath", coin5.isCollected)
    assertEquals("Trap must stay ARMED when optional bait coin is skipped", TrollTrapState.ARMED, betrayalTrap.state)
    assertTrue("Main route without bait coin must reach SUN_GATE cleanly", engine.isVictorious)

    // Path B: Collecting optional Coin #5 triggers ceiling block -> death -> checkpoint rollback re-arms both
    val rollbackEngine = buildEngine("ch_08")
    val rCp = rollbackEngine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    val rCoin5 = rollbackEngine.coins.first { it.id == 5 }
    val rTrap = rollbackEngine.trollTraps.first { it.id == 6 }

    rollbackEngine.player.x = rCp.x + 4f
    rollbackEngine.player.y = rCp.y + rCp.height - rollbackEngine.player.collisionHeight
    rollbackEngine.player.isGrounded = true
    rollbackEngine.update()

    // Jump/touch Coin #5 intentionally (center player hitbox on coin center)
    rollbackEngine.player.x = rCoin5.x - rollbackEngine.player.collisionWidth / 2f
    rollbackEngine.player.y = rCoin5.y - rollbackEngine.player.collisionHeight / 2f
    rollbackEngine.update()
    assertTrue(rCoin5.isCollected)
    assertEquals(TrollTrapState.TELEGRAPH, rTrap.state)

    // Wait while standing beneath falling ceiling block -> dies cleanly -> checkpoint rolls back
    rollbackEngine.player.x = rTrap.initialX + 10f
    rollbackEngine.player.y = 12.5f * t - rollbackEngine.player.collisionHeight
    rollbackEngine.player.isGrounded = true
    repeat(45) {
      if (!rollbackEngine.isDying) rollbackEngine.update()
    }
    assertTrue("Standing under triggered Coin Betrayal ceiling block must kill", rollbackEngine.isDying)
    settleDeath(rollbackEngine)

    assertFalse("Checkpoint rollback must un-collect Coin #5", rCoin5.isCollected)
    assertEquals("Checkpoint rollback must re-arm Coin Betrayal trap", TrollTrapState.ARMED, rTrap.state)
    assertEquals(rTrap.initialY, rTrap.y, 0.5f)
  }

  @Test
  fun test08_fakeExitInCh12NeverSoftlocksAndRollsBackCleanly() {
    val engine = buildEngine("ch_12")
    val t = World1Levels.TILE_SIZE
    val cp = engine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    val fakeDoor = engine.trollTraps.first { it.type == TrollTrapType.FAKE_EXIT_DOOR }
    val chainedSpike = engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP && it.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED }

    // Activate ch_12 checkpoint
    engine.player.x = cp.x + 4f
    engine.player.y = cp.y + cp.height - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.update()
    assertTrue(engine.hasCheckpointActivated)

    // Approach Mirage Fake Exit Door -> activates, poofs invisible cleanly, and chains telegraphed spike
    engine.player.x = fakeDoor.initialX
    engine.player.y = 13f * t - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.update()

    assertEquals(TrollTrapState.ACTIVE, fakeDoor.state)
    assertEquals(TrollTrapState.TELEGRAPH, chainedSpike.state)
    repeat(fakeDoor.activeDurationFrames + 2) { engine.update() }
    assertEquals("Mirage door must retire non-destructively after reveal", TrollTrapState.RETIRED, fakeDoor.state)
    assertFalse("Mirage door must hide cleanly without blocking progression", fakeDoor.isVisible)
    assertFalse("Approaching mirage door alone must not kill player", engine.isDying)

    // Waiting out chained spike active window OR jumping over leads cleanly to real SUN_GATE
    repeat(chainedSpike.activeDurationFrames + 5) { engine.update() }
    assertEquals("Chained spike must retract after active window so path never softlocks", TrollTrapState.RETIRED, chainedSpike.state)

    val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
    engine.player.x = gate.x + 28f
    engine.player.y = gate.y + 30f
    engine.update()
    assertTrue("Real SUN_GATE in ch_12 must stay reachable", engine.isVictorious)

    // Verify rollback re-arms both Mirage Door and Chained Spike
    val rbEngine = buildEngine("ch_12")
    val rbCp = rbEngine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    val rbDoor = rbEngine.trollTraps.first { it.type == TrollTrapType.FAKE_EXIT_DOOR }
    val rbSpike = rbEngine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    rbEngine.player.x = rbCp.x + 4f
    rbEngine.player.y = rbCp.y + rbCp.height - rbEngine.player.collisionHeight
    rbEngine.player.isGrounded = true
    rbEngine.update()

    rbEngine.player.x = rbDoor.initialX
    rbEngine.player.y = 13f * t - rbEngine.player.collisionHeight
    rbEngine.update()
    rbEngine.handlePlayerDeath("test_fake_exit_rollback")
    assertEquals(TrollTrapState.ARMED, rbDoor.state)
    assertTrue(rbDoor.isVisible)
    assertEquals(TrollTrapState.ARMED, rbSpike.state)
    assertEquals(0f, rbSpike.morphSpikeHeight, 0.01f)
  }

  @Test
  fun test09_allTwelveChallengesPassTrapFairnessPolicyAcrossAllTiers() {
    for (entry in ChallengeCatalog.entries) {
      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
        val unfair = validations.filter { !it.isFair }
        assertTrue(
            "All traps in ${entry.challengeId} tier=$tier must satisfy TrapFairnessPolicy: $unfair",
            unfair.isEmpty()
        )
      }
    }
  }

  // --- TASK F: Snappy Death & Retry Loop Verification ---

  @Test
  fun test10_fastDeathRespawnResetsTimersAndRequestsImmediateCameraSnap() {
    val engine = buildEngine("ch_05")
    engine.consumePendingCameraSnap()
    engine.inputRight = true
    engine.inputJump = true
    engine.mirrorCurseFrames = 45
    engine.startDeathSequence("BONK!", "trap_test")
    assertTrue(engine.isDying)
    assertEquals(ChaosGameEngine.DEFAULT_DEATH_ANIM_FRAMES, engine.deathAnimFrames)

    settleDeath(engine)
    assertEquals(1, engine.deathCount)
    assertEquals(0, engine.mirrorCurseFrames)
    assertTrue("Camera snap must trigger immediately upon respawn", engine.consumePendingCameraSnap())
    assertTrue("Held horizontal movement input must remain responsive across retry", engine.inputRight)
    assertNotEquals(0, engine.challengeAttemptToken)
  }
}
