package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeRecord
import com.example.data.ContinuousRunPersistedState
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.data.TrollTrapState
import com.example.engine.ChallengeCompletionOutcome
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.engine.ContinuousRunCoordinator
import com.example.engine.GameSurfaceView
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase2ContinuousRunAndRecoveryTest {

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

  private fun findGameSurfaceViews(root: View): List<GameSurfaceView> {
    val result = mutableListOf<GameSurfaceView>()
    if (root is GameSurfaceView) {
      result.add(root)
    } else if (root is ViewGroup) {
      for (i in 0 until root.childCount) {
        result.addAll(findGameSurfaceViews(root.getChildAt(i)))
      }
    }
    return result
  }

  private fun completeCurrentEngineChallenge(engine: ChaosGameEngine) {
    // Advance at least a few frames so timer/score move and defeat boss/clear trap hops cleanly
    repeat(10) { engine.update() }
    if (engine.level.guardianConfig != null) {
      engine.stoneGuardian.phase = com.example.data.GuardianPhase.DEFEATED
      engine.stoneGuardian.hitsRemaining = 0
    }
    val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
    engine.player.x = gate.x + 12f
    engine.player.y = gate.y + 8f
    engine.update()
    // On 1-1 / 1-3, RELOCATING_EXIT_DOOR may move gate on first approach; approach new location if still same challenge
    if (!engine.isVictorious) {
      val movedGate = engine.tiles.first { it.type == TileType.SUN_GATE }
      engine.player.x = movedGate.x + 12f
      engine.player.y = movedGate.y + 8f
      engine.update()
    }
  }

  @Test
  fun test01_continuousProgressionAcrossAllFiveChallengesAndSingleSurfaceViewPreserved() {
    lateinit var vm: GameStateViewModel
    lateinit var initialSurfaceView: GameSurfaceView
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.waitForIdle()

    // Start Continuous Run via MainMenuView CTA
    composeTestRule.onNodeWithTag("button_continuous_run").performClick()
    composeTestRule.waitForIdle()

    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertEquals(RunLaunchMode.CONTINUOUS_PROGRESSION, vm.runLaunchMode.value)
    val initialSession = vm.gameSession.value!!
    val initialSessionId = initialSession.sessionId
    val initialEngine = initialSession.engine
    val initialRenderer = initialSession.renderer
    assertEquals("ch_01", initialEngine.activeChallengeId)

    composeTestRule.activityRule.scenario.onActivity { activity ->
      val svs = findGameSurfaceViews(activity.window.decorView)
      assertEquals("Must mount exactly one GameSurfaceView", 1, svs.size)
      initialSurfaceView = svs.single()
    }

    val expectedOrder = listOf("ch_01", "ch_02", "ch_03", "ch_04", "ch_05")
    for (idx in 0 until 4) {
      val currentId = expectedOrder[idx]
      val nextId = expectedOrder[idx + 1]
      assertEquals(currentId, initialEngine.activeChallengeId)

      completeCurrentEngineChallenge(initialEngine)
      composeTestRule.waitForIdle()

      assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
      assertEquals(nextId, initialEngine.activeChallengeId)
      assertSame("Session must remain identical in continuous run", initialSession, vm.gameSession.value)
      assertEquals("sessionId must remain unchanged across transitions", initialSessionId, vm.gameSession.value!!.sessionId)
      assertSame("Engine must remain same instance in-place", initialEngine, vm.gameEngine)
      assertSame("Renderer must remain same instance in-place", initialRenderer, vm.gameRenderer)

      composeTestRule.activityRule.scenario.onActivity { activity ->
        val svs = findGameSurfaceViews(activity.window.decorView)
        assertEquals("Only 1 GameSurfaceView must exist after transition to $nextId", 1, svs.size)
        assertSame("Same GameSurfaceView instance must be preserved", initialSurfaceView, svs.single())
      }
    }

    // Complete ch_05 -> must transition to RUN_COMPLETION screen (no auto-restart into ch_01)
    assertEquals("ch_05", initialEngine.activeChallengeId)
    completeCurrentEngineChallenge(initialEngine)
    composeTestRule.waitForIdle()

    assertEquals(AppScreen.RUN_COMPLETION, vm.currentScreen.value)
    composeTestRule.onNodeWithTag("screen_run_completion").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_run_completion_count").assertIsDisplayed()
    val summary = vm.runCompletionSummary.value!!
    assertEquals(5, summary.completedCount)
    assertEquals(5, summary.totalChallenges)
    assertTrue(summary.challengeRecords.all { it.completed })
  }

  @Test
  fun test02_duplicateCompletionCallbacksAreSafelyIgnored() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()
    val firstToken = coordinator.currentAttemptToken

    val firstOutcome = coordinator.onChallengeCompleted(
        challengeId = "ch_01",
        attemptToken = firstToken,
        timeMs = 5000L,
        score = 800,
        coinsCollected = 6,
        totalCoins = 10,
        parTimeSeconds = 60,
        isTrollLevel = false,
        challengeDeaths = 0
    )
    assertTrue(firstOutcome is ChallengeCompletionOutcome.NextChallenge)
    assertEquals("ch_02", coordinator.activeChallengeId)
    assertEquals(5000L, coordinator.cumulativeRunTimerMs)
    assertEquals(800, coordinator.cumulativeScore)

    // Duplicate callback with same token or wrong challengeId must be ignored
    val dupSameToken = coordinator.onChallengeCompleted(
        challengeId = "ch_01",
        attemptToken = firstToken,
        timeMs = 5000L,
        score = 800,
        coinsCollected = 6,
        totalCoins = 10,
        parTimeSeconds = 60,
        isTrollLevel = false,
        challengeDeaths = 0
    )
    val wrongChallenge = coordinator.onChallengeCompleted(
        challengeId = "ch_01",
        attemptToken = coordinator.currentAttemptToken,
        timeMs = 5000L,
        score = 800,
        coinsCollected = 6,
        totalCoins = 10,
        parTimeSeconds = 60,
        isTrollLevel = false,
        challengeDeaths = 0
    )
    assertEquals(ChallengeCompletionOutcome.IgnoredDuplicate, dupSameToken)
    assertEquals(ChallengeCompletionOutcome.IgnoredDuplicate, wrongChallenge)
    assertEquals("ch_02", coordinator.activeChallengeId)
    assertEquals(5000L, coordinator.cumulativeRunTimerMs)
    assertEquals(800, coordinator.cumulativeScore)
  }

  @Test
  fun test03_cumulativeStatsPreservedAcrossDeathsAndChallengeTransitions() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startNewContinuousRun()
    }
    composeTestRule.waitForIdle()

    val engine = vm.gameEngine!!
    // Run a few frames, die once on ch_01, run more frames + collect a coin, then complete ch_01
    repeat(15) { engine.update() }
    engine.handlePlayerDeath()
    assertEquals(1, engine.deathCount)
    assertEquals(1, engine.cumulativeDeathCount)
    assertEquals(1, vm.continuousCoordinator.currentChallengeDeaths)

    // Pick up first coin & advance frames
    val c0 = engine.coins.first()
    engine.player.x = c0.x - engine.player.collisionWidth / 2f
    engine.player.y = c0.y - engine.player.collisionHeight / 2f
    repeat(10) { engine.update() }
    val scoreCh1 = engine.score
    assertTrue("Score must be positive after collecting coin", scoreCh1 > 0)

    completeCurrentEngineChallenge(engine)
    composeTestRule.waitForIdle()

    assertEquals("ch_02", engine.activeChallengeId)
    assertEquals("Per-challenge deaths reset for ch_02", 0, engine.deathCount)
    assertEquals("Cumulative deaths must persist into ch_02", 1, engine.cumulativeDeathCount)
    assertTrue("Cumulative timer must remain > 0 across ch_02 load", engine.cumulativeRunTimerMs > 0L)
    assertEquals("Per-challenge score resets to 0 on ch_02", 0, engine.score)
    assertEquals("Coordinator cumulativeScore retains ch_01 score", scoreCh1, vm.continuousCoordinator.cumulativeScore)

    // Die twice on ch_02 -> cumulative deaths becomes 3, timer keeps accumulating
    repeat(5) { engine.update() }
    engine.handlePlayerDeath()
    engine.handlePlayerDeath()
    assertEquals(2, engine.deathCount)
    assertEquals(3, engine.cumulativeDeathCount)
    assertEquals(3, vm.continuousCoordinator.cumulativeDeaths)
  }

  @Test
  fun test04_runCompletionScreenDisplaysSummaryControlsAndStartsFreshRunWithoutAutoLooping() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startNewContinuousRun()
    }
    composeTestRule.waitForIdle()
    val engine = vm.gameEngine!!

    repeat(5) {
      completeCurrentEngineChallenge(engine)
      composeTestRule.waitForIdle()
    }

    assertEquals(AppScreen.RUN_COMPLETION, vm.currentScreen.value)
    composeTestRule.onNodeWithTag("screen_run_completion").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_run_completion_count").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_run_total_time").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_run_total_deaths").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_run_total_score").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_run_best_records").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_start_new_run").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_run_completion_main_menu").assertIsDisplayed()
    for (id in listOf("ch_01", "ch_02", "ch_03", "ch_04", "ch_05")) {
      composeTestRule.onNodeWithTag("button_replay_$id").assertIsDisplayed()
    }

    // Must not auto-restart; clicking START NEW RUN launches a fresh ch_01 run
    composeTestRule.onNodeWithTag("button_start_new_run").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertEquals("ch_01", vm.gameEngine!!.activeChallengeId)
    assertEquals(0, vm.continuousCoordinator.completedChallengeIds.size)
    assertEquals(0, vm.continuousCoordinator.cumulativeDeaths)
  }

  @Test
  fun test05_nonDestructiveReplayPreservesBetterRecordsAndCumulativeStats() {
    val repo = SaveRepository(app)
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()

    // Complete ch_01..ch_05 with strong ch_02 score=1200, fast time=8000ms, 0 deaths
    for (i in 1..5) {
      val id = "ch_0$i"
      coordinator.onChallengeCompleted(
          challengeId = id,
          attemptToken = coordinator.currentAttemptToken,
          timeMs = if (id == "ch_02") 8000L else 10000L,
          score = if (id == "ch_02") 1200 else 500,
          coinsCollected = if (id == "ch_02") 8 else 5,
          totalCoins = 10,
          parTimeSeconds = 60,
          isTrollLevel = false,
          challengeDeaths = if (id == "ch_02") 0 else 1
      )
    }
    assertTrue(coordinator.isRunCompleted)
    assertEquals(48000L, coordinator.cumulativeRunTimerMs)
    assertEquals(3200, coordinator.cumulativeScore)
    val preReplayCh2 = coordinator.challengeRecords["ch_02"]!!

    // Replay ch_02 with WORSE performance -> existing better records & cumulative stats must not degrade
    val worseMerged = coordinator.recordReplayCompletionIfImproved(
        challengeId = "ch_02",
        timeMs = 45000L,
        score = 100,
        coinsCollected = 1,
        totalCoins = 10,
        parTimeSeconds = 60,
        isTrollLevel = false,
        challengeDeaths = 9
    )
    assertEquals(preReplayCh2.bestTimeMs, worseMerged.bestTimeMs)
    assertEquals(preReplayCh2.bestScore, worseMerged.bestScore)
    assertEquals(preReplayCh2.stars, worseMerged.stars)
    assertEquals(preReplayCh2.leastDeaths, worseMerged.leastDeaths)
    assertEquals(48000L, coordinator.cumulativeRunTimerMs)
    assertEquals(3200, coordinator.cumulativeScore)
    assertTrue(coordinator.isRunCompleted)

    // Verify ViewModel UI replay mode flow is non-destructive and marked REPLAY in HUD
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startNewContinuousRun()
    }
    composeTestRule.waitForIdle()
    val runEngine = vm.gameEngine!!
    repeat(5) {
      completeCurrentEngineChallenge(runEngine)
      composeTestRule.waitForIdle()
    }
    assertEquals(AppScreen.RUN_COMPLETION, vm.currentScreen.value)
    val summaryBeforeReplay = vm.runCompletionSummary.value!!

    composeTestRule.onNodeWithTag("button_replay_ch_02").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertEquals(RunLaunchMode.REPLAY, vm.runLaunchMode.value)
    composeTestRule.onNodeWithTag("hud_replay_badge").assertIsDisplayed()

    completeCurrentEngineChallenge(vm.gameEngine!!)
    composeTestRule.waitForIdle()
    // Return to run completion summary cleanly
    composeTestRule.onNodeWithTag("button_victory_main_menu").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.RUN_COMPLETION, vm.currentScreen.value)
    val summaryAfterReplay = vm.runCompletionSummary.value!!
    assertEquals(summaryBeforeReplay.completedCount, summaryAfterReplay.completedCount)
    assertEquals(summaryBeforeReplay.totalRunTimeMs, summaryAfterReplay.totalRunTimeMs)
    assertEquals(summaryBeforeReplay.totalDeaths, summaryAfterReplay.totalDeaths)
    assertEquals(summaryBeforeReplay.totalScore, summaryAfterReplay.totalScore)
    assertFalse("Replay must not mark run as active unfinished run", repo.loadContinuousRunState().isRunActive)
  }

  @Test
  fun test06_legacySaveMigrationPreservesAllLegacyKeysAndProgress() {
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit()
        .putLong("best_time_1_1", 21450L)
        .putInt("stars_1_1", 3)
        .putInt("best_score_1_1", 1850)
        .putLong("best_time_1_2", 34900L)
        .putInt("stars_1_2", 2)
        .putInt("best_score_1_2", 1420)
        .putBoolean("clue_discovered_1", true)
        .putBoolean("relic_discovered_relic_1_2", true)
        .commit()

    val repo = SaveRepository(app)
    assertEquals(1, repo.getSchemaVersion())
    repo.migrateIfNeeded()
    assertEquals(SaveRepository.SAVE_SCHEMA_VERSION, repo.getSchemaVersion())

    // Legacy keys remain completely intact
    assertEquals(21450L, repo.getBestTime(1, 1))
    assertEquals(3, repo.getStars(1, 1))
    assertEquals(1850, repo.getBestScore(1, 1))
    assertEquals(34900L, repo.getBestTime(1, 2))
    assertEquals(2, repo.getStars(1, 2))
    assertEquals(1420, repo.getBestScore(1, 2))
    assertTrue(repo.isRelicDiscovered("relic_1_2"))

    // Continuous state absorbed migrated completions cleanly and starts next unfinished challenge (ch_03)
    val state = repo.loadContinuousRunState()
    assertTrue(state.completedChallengeIds.contains("ch_01"))
    assertTrue(state.completedChallengeIds.contains("ch_02"))
    assertEquals("ch_03", state.activeChallengeId)
    assertFalse("Legacy v1 migration must not falsely trigger foreground crash recovery", state.isRunInForeground)
    assertEquals(21450L, state.challengeRecords["ch_01"]!!.bestTimeMs)
    assertEquals(1850, state.challengeRecords["ch_01"]!!.bestScore)
    assertEquals(3, state.challengeRecords["ch_01"]!!.stars)
  }

  @Test
  fun test07_migrationIsStrictlyIdempotentAcrossRepeatedInvocations() {
    val repo = SaveRepository(app)
    repo.saveBestTime(1, 1, 25000L)
    repo.saveStars(1, 1, 2)
    repo.saveBestScore(1, 1, 900)

    repo.migrateIfNeeded()
    // Now save an even better v2 record and start an active run on ch_02
    val v2State = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = true,
        isRunInForeground = true,
        isRunCompleted = false,
        activeChallengeId = "ch_02",
        activeChallengeSeed = 1002L,
        activeConfigVersion = 1,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 2,
        completedChallengeIds = setOf("ch_01"),
        challengeRecords = mapOf(
            "ch_01" to ChallengeRecord("ch_01", true, bestTimeMs = 15000L, bestScore = 2400, stars = 3, leastDeaths = 0)
        ),
        cumulativeRunTimerMs = 18000L,
        cumulativeDeaths = 2,
        cumulativeScore = 2400,
        bestRunTimeMs = 0L,
        bestRunScore = 2400,
        bestRunLeastDeaths = 0
    )
    repo.saveContinuousRunState(v2State, syncCommit = true)

    // Repeat migrateIfNeeded multiple times -> nothing may downgrade or change
    repeat(3) { repo.migrateIfNeeded() }

    val loaded = repo.loadContinuousRunState()
    assertEquals(SaveRepository.SAVE_SCHEMA_VERSION, loaded.schemaVersion)
    assertTrue(loaded.isRunActive)
    assertEquals("ch_02", loaded.activeChallengeId)
    assertEquals(15000L, loaded.challengeRecords["ch_01"]!!.bestTimeMs)
    assertEquals(2400, loaded.challengeRecords["ch_01"]!!.bestScore)
    assertEquals(3, loaded.challengeRecords["ch_01"]!!.stars)
  }

  @Test
  fun test08_09_10_processDeathRecoveryRestoresExactConfigStatsAndNeverMarksUnfinishedCompleted() {
    val repo = SaveRepository(app)
    val ch3Def = ChallengeCatalog.findById("ch_03")!!

    val persisted = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = true,
        isRunInForeground = true,
        isRunCompleted = false,
        activeChallengeId = "ch_03",
        activeChallengeSeed = ch3Def.challengeSeed,
        activeConfigVersion = ch3Def.configVersion,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 2,
        completedChallengeIds = setOf("ch_01", "ch_02"),
        challengeRecords = mapOf(
            "ch_01" to ChallengeRecord("ch_01", true, bestTimeMs = 11000L, bestScore = 1100, stars = 2, leastDeaths = 1),
            "ch_02" to ChallengeRecord("ch_02", true, bestTimeMs = 14000L, bestScore = 1350, stars = 3, leastDeaths = 0)
        ),
        cumulativeRunTimerMs = 28500L,
        cumulativeDeaths = 3,
        cumulativeScore = 2450,
        bestRunTimeMs = 0L,
        bestRunScore = 0,
        bestRunLeastDeaths = -1
    )
    repo.saveContinuousRunState(persisted, syncCommit = true)

    // Launch a fresh Activity/ViewModel process after simulated OS kill
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      scenario.onActivity { activity ->
        val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
        assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
        assertEquals(RunLaunchMode.CONTINUOUS_PROGRESSION, vm.runLaunchMode.value)

        val engine = vm.gameEngine!!
        val locked = engine.lockedChallengeConfig!!
        // Test 8 & 9: Exact challengeId, seed, configVersion, tier, completedIds, cumulative stats preserved, at spawn
        assertEquals("ch_03", engine.activeChallengeId)
        assertEquals(ch3Def.challengeSeed, locked.challengeSeed)
        assertEquals(ch3Def.configVersion, locked.configVersion)
        assertEquals(AdaptiveTier.STANDARD, locked.selectedAdaptiveTier)
        assertEquals(engine.level.spawnX, engine.player.x, 0.01f)
        assertEquals(engine.level.spawnY, engine.player.y, 0.01f)
        assertEquals(28500L, engine.cumulativeRunTimerMs)
        assertEquals(3, engine.cumulativeDeathCount)
        assertEquals(2, engine.deathCount)
        assertEquals(2450, vm.continuousCoordinator.cumulativeScore)
        assertEquals(setOf("ch_01", "ch_02"), vm.continuousCoordinator.completedChallengeIds)

        // Test 10: Active unfinished challenge ch_03 MUST NOT be falsely marked completed
        assertFalse(vm.continuousCoordinator.completedChallengeIds.contains("ch_03"))
        assertFalse(engine.completedChallengeIds.contains("ch_03"))
        assertFalse(repo.loadContinuousRunState().completedChallengeIds.contains("ch_03"))
      }
    } finally {
      scenario.close()
    }
  }

  @Test
  fun test11_ordinaryBackgroundResumePreservesLiveInMemoryStateWithoutResetToSpawn() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startNewContinuousRun()
    }
    composeTestRule.waitForIdle()

    val engine = vm.gameEngine!!
    val initialSessionId = vm.gameSession.value!!.sessionId
    engine.player.x = 640f
    engine.player.y = 420f
    engine.inputRight = true
    repeat(10) { engine.update() }
    val posXBeforePause = engine.player.x
    assertTrue(posXBeforePause > 640f)

    // Simulate ordinary app background -> resume (process NOT killed)
    composeTestRule.activityRule.scenario.onActivity {
      vm.onHostPaused()
      assertTrue(vm.isEffectivePaused.value)
      vm.onHostResumed()
      assertFalse(vm.isEffectivePaused.value)
    }
    composeTestRule.waitForIdle()

    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertEquals(initialSessionId, vm.gameSession.value!!.sessionId)
    assertSame(engine, vm.gameEngine)
    assertEquals("Player position must stay intact after normal background/resume", posXBeforePause, engine.player.x, 0.01f)
  }

  @Test
  fun test12_invalidMalformedAndIncompleteSavesFallBackGracefullyWithoutCrashing() {
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    // Inject mixed wrong types + out-of-range + bogus challengeId + bogus completed flag with missing challenges
    prefs.edit()
        .putInt("cr_schema_version", 2)
        .putBoolean("cr_run_active", true)
        .putBoolean("cr_run_in_foreground", true)
        .putBoolean("cr_run_completed", true) // Incomplete save: marked completed with only ch_999!
        .putString("cr_active_challenge_id", "ch_999_does_not_exist")
        .putLong("cr_active_challenge_seed", -99999L)
        .putInt("cr_active_config_version", -5)
        .putString("cr_active_adaptive_tier", "NOT_A_REAL_TIER")
        .putInt("cr_current_challenge_deaths", -42)
        .putString("cr_completed_challenge_ids", "ch_01,bogus_id,ch_999")
        .putLong("cr_cumulative_timer_ms", -1000L)
        .putInt("cr_cumulative_deaths", -7)
        .putInt("cr_cumulative_score", -300)
        .putString("cr_best_run_score", "WRONG_TYPE_STRING") // Triggers ClassCastException in getInt!
        .commit()

    val repo = SaveRepository(app)
    val sanitized = repo.loadContinuousRunState()
    assertEquals(SaveRepository.SAVE_SCHEMA_VERSION, sanitized.schemaVersion)
    assertFalse("Incomplete save must not remain marked runCompleted", sanitized.isRunCompleted)
    assertEquals("ch_02", sanitized.activeChallengeId) // ch_01 completed, so ch_02 is first unfinished
    assertEquals(1002L, sanitized.activeChallengeSeed)
    assertEquals(ChallengeCatalog.CONFIG_VERSION, sanitized.activeConfigVersion)
    assertEquals(AdaptiveTier.STANDARD, sanitized.activeAdaptiveTier)
    assertEquals(setOf("ch_01"), sanitized.completedChallengeIds)
    assertTrue(sanitized.currentChallengeDeaths >= 0)
    assertTrue(sanitized.cumulativeRunTimerMs >= 0L)
    assertTrue(sanitized.cumulativeDeaths >= 0)
    assertTrue(sanitized.cumulativeScore >= 0)
    assertEquals(0, sanitized.bestRunScore)
  }

  @Test
  fun test13_inPlaceChallengeLoadingCleansPendingTrapEventsAndIsThreadSafeUnderConcurrentRender() {
    val engine = ChaosGameEngine(
        level = ChallengeCatalog.createLockedConfig("ch_01")!!.levelDefinition,
        audioEngine = ProceduralAudioEngine(),
        lockedChallengeConfig = ChallengeCatalog.createLockedConfig("ch_01")!!,
        initialLaunchMode = RunLaunchMode.CONTINUOUS_PROGRESSION
    )
    val renderer = ChaosGameRenderer(app)

    // Dirty up ch_01 trap state, pending chain queue, timers, and reentrant trigger during update()
    engine.enqueueTrapChainEvent(targetTrapId = 999, delayFrames = 5)
    assertEquals(1, engine.pendingTrapChainEventCount)
    engine.trollTraps.firstOrNull()?.state = TrollTrapState.ACTIVE
    repeat(12) { engine.update() }
    val timerBeforeLoad = engine.cumulativeRunTimerMs
    assertTrue(timerBeforeLoad > 0L)

    // Verify reentrant loadChallenge from inside update() via onLevelCompleted works atomically
    val ch1Again = ChallengeCatalog.createLockedConfig("ch_01")!!
    val ch2Config = ChallengeCatalog.createLockedConfig("ch_02")!!
    lateinit var reentrantEngine: ChaosGameEngine
    reentrantEngine = ChaosGameEngine(
        level = ch1Again.levelDefinition,
        audioEngine = ProceduralAudioEngine(),
        lockedChallengeConfig = ch1Again,
        initialLaunchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
        onLevelCompleted = { _, _, _ ->
          reentrantEngine.loadChallenge(ch2Config, attemptToken = 77L)
        }
    )
    completeCurrentEngineChallenge(reentrantEngine)
    assertEquals("Reentrant loadChallenge inside update() must transition to ch_02", "ch_02", reentrantEngine.activeChallengeId)
    assertEquals(1L, reentrantEngine.challengeEpoch)
    assertFalse("New challenge must not stay victorious after in-place load", reentrantEngine.isVictorious)

    // Test direct loadChallenge cleanup & concurrent render stress
    val ch2 = ChallengeCatalog.createLockedConfig("ch_02")!!
    engine.loadChallenge(
        lockedConfig = ch2,
        attemptToken = 42L,
        launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
        preserveCumulativeStats = true
    )

    assertEquals("ch_02", engine.activeChallengeId)
    assertEquals(1L, engine.challengeEpoch)
    assertEquals(0, engine.pendingTrapChainEventCount)
    assertEquals(0L, engine.levelTimerMs)
    assertEquals(timerBeforeLoad, engine.cumulativeRunTimerMs)
    assertTrue(engine.trollTraps.all { it.state == TrollTrapState.ARMED || it.state == TrollTrapState.RETIRED || it.state == TrollTrapState.ACTIVE })

    // Stress-test concurrent update() + loadChallenge() + renderer.render() across threads
    val failure = AtomicReference<Throwable?>(null)
    val doneLatch = CountDownLatch(2)
    val configs = (1..5).map { ChallengeCatalog.createLockedConfig("ch_0$it")!! }

    val simThread = Thread {
      try {
        for (step in 0 until 60) {
          engine.inputRight = (step % 2 == 0)
          engine.update()
          if (step % 12 == 0) {
            engine.loadChallenge(configs[(step / 12) % configs.size], attemptToken = 100L + step)
          }
        }
      } catch (t: Throwable) {
        failure.compareAndSet(null, t)
      } finally {
        doneLatch.countDown()
      }
    }

    val renderThread = Thread {
      val bmp = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      try {
        repeat(45) {
          renderer.render(canvas, engine, 800f, 480f)
        }
      } catch (t: Throwable) {
        failure.compareAndSet(null, t)
      } finally {
        renderer.expansionRenderer.release()
        doneLatch.countDown()
      }
    }

    simThread.start()
    renderThread.start()
    assertTrue("Concurrent render/update/load stress timed out", doneLatch.await(15, TimeUnit.SECONDS))
    val err = failure.get()
    if (err != null) {
      throw AssertionError("Concurrent render/load failed: ${err.message}", err)
    }
  }
}
