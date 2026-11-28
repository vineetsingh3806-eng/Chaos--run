package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveTier
import com.example.data.ChallengeAttemptMetrics
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeRecord
import com.example.data.ChaosModifier
import com.example.data.CosmeticTrail
import com.example.data.MasteryCatalog
import com.example.data.OfflineCalendarDate
import com.example.data.OfflineDailyChallengeGenerator
import com.example.data.PlayerTitle
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.data.VictoryEffect
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase4RetentionMasteryDailyTest {

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

  private fun reachExitGate(engine: ChaosGameEngine, coinsCollectedCount: Int = 0) {
    engine.readWorldStateLocked {
      var leftToCollect = coinsCollectedCount
      for (coin in engine.coins) {
        if (leftToCollect > 0) {
          coin.isCollected = true
          leftToCollect--
        } else {
          coin.isCollected = false
        }
      }
    }
    repeat(12) {
      if (!engine.isVictorious) {
        engine.readWorldStateLocked {
          val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
          engine.player.x = gate.x + 24f
          engine.player.y = gate.y + 4f
          engine.player.vx = 0f
          engine.player.vy = 0f
          engine.player.isGrounded = true
        }
        engine.update()
      }
    }
  }

  @Test
  fun test01_masteryCatalogEvaluatesAllObjectivesAdditivelyAndPersists() {
    assertEquals(200, MasteryCatalog.specs.size)
    assertEquals(600, MasteryCatalog.totalPossibleStars)

    val repo = SaveRepository(app)
    // Attempt 1 on ch_01: 0 deaths + fast time + collected coins -> earns ZERO_DEATHS + SPEED_PAR (2 stars)
    val attempt1 = ChallengeAttemptMetrics(
        challengeId = "ch_01",
        completed = true,
        timeMs = 11_500L,
        deaths = 0,
        coinsCollected = 2,
        totalCoins = 5,
        usedCheckpoint = false,
        score = 350
    )
    val earned1 = MasteryCatalog.evaluateAttempt(attempt1)
    assertEquals(setOf("ch_01_zero_deaths", "ch_01_speed_par"), earned1)
    assertTrue(repo.saveMasteryObjectivesEarned("ch_01", earned1, syncCommit = true))

    // Attempt 2 on ch_01: slower + died once, but 0 coins -> earns NO_COINS, keeps all 3 stars additively
    val attempt2 = ChallengeAttemptMetrics(
        challengeId = "ch_01",
        completed = true,
        timeMs = 28_000L,
        deaths = 1,
        coinsCollected = 0,
        totalCoins = 5,
        usedCheckpoint = false,
        score = 200
    )
    val earned2 = MasteryCatalog.evaluateAttempt(attempt2)
    assertEquals(setOf("ch_01_no_coins"), earned2)
    assertTrue(repo.saveMasteryObjectivesEarned("ch_01", earned2, syncCommit = true))

    val snapshot = repo.loadPhase4RetentionSnapshot()
    val ch1Progress = snapshot.masteryByChallenge["ch_01"]!!
    assertEquals(3, ch1Progress.masteryStars)
    assertEquals(3, snapshot.totalMasteryStars)

    // Verify ch_04 uses NO_CHECKPOINT and ch_05 uses MIN_SCORE
    val ch04Earned = MasteryCatalog.evaluateAttempt(
        ChallengeAttemptMetrics("ch_04", true, 12_000L, 0, 1, 4, usedCheckpoint = false, score = 200)
    )
    assertTrue(ch04Earned.contains("ch_04_no_checkpoint"))
    val ch05Earned = MasteryCatalog.evaluateAttempt(
        ChallengeAttemptMetrics("ch_05", true, 12_000L, 0, 3, 5, usedCheckpoint = true, score = 450)
    )
    assertTrue(ch05Earned.contains("ch_05_min_score"))
  }

  @Test
  fun test02_masteryAndMilestoneUnlocksGrantRewardsAndPersist() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.dailyDateProvider = { OfflineCalendarDate(2026, 8, 10) }
    }
    composeTestRule.waitForIdle()

    // Earn all 3 mastery stars + all coins in ch_01 via Mastery Challenge clear
    composeTestRule.activityRule.scenario.onActivity {
      vm.launchMasteryOrModifierChallenge("ch_01", emptySet())
      val engine = vm.gameEngine!!
      val totalCoins = engine.coins.size
      reachExitGate(engine, coinsCollectedCount = totalCoins)
      vm.flushPendingPersistence()
    }
    composeTestRule.waitForIdle()

    val victory = vm.victoryResult.value!!
    assertEquals(RunLaunchMode.MASTERY_CHALLENGE, victory.launchMode)
    // ch_01 with all coins collected and 0 deaths & fast time unlocks ZERO_DEATHS + SPEED_PAR (2 stars) + COIN_GREEDY title
    assertTrue(vm.retentionSnapshot.value.rewards.unlockedTitles.contains(PlayerTitle.COIN_GREEDY))

    // Complete ch_02 cleanly to reach 3+ Mastery Stars -> unlocks EMBER_SPARK trail & TRAP_SURVIVOR title
    composeTestRule.activityRule.scenario.onActivity {
      vm.launchMasteryOrModifierChallenge("ch_02", setOf(ChaosModifier.NO_CHECKPOINT))
      val engine = vm.gameEngine!!
      reachExitGate(engine, coinsCollectedCount = 0)
      vm.flushPendingPersistence()
    }
    composeTestRule.waitForIdle()

    val snap = vm.retentionSnapshot.value
    assertTrue("Total mastery stars >= 3", snap.totalMasteryStars >= 3)
    assertTrue(snap.rewards.unlockedTrails.contains(CosmeticTrail.EMBER_SPARK))
    assertTrue(snap.rewards.unlockedTitles.contains(PlayerTitle.TRAP_SURVIVOR))
    assertTrue(snap.rewards.unlockedTitles.contains(PlayerTitle.NO_MERCY))

    // Locked items must reject equip, unlocked items must persist equip
    assertFalse(vm.equipTrail(CosmeticTrail.SOLAR_GOLD))
    assertTrue(vm.equipTrail(CosmeticTrail.EMBER_SPARK))
    assertTrue(vm.equipTitle(PlayerTitle.NO_MERCY))

    val reloaded = SaveRepository(app).loadPhase4RetentionSnapshot()
    assertEquals(CosmeticTrail.EMBER_SPARK, reloaded.rewards.equippedTrail)
    assertEquals(PlayerTitle.NO_MERCY, reloaded.rewards.equippedTitle)
  }

  @Test
  fun test03_chaosModifiersFairnessNoCheckpointSpeedRunAndCosmeticRender() {
    // 1. All 12 challenges remain fair under TRAP_RUSH (AdaptiveTier.RELENTLESS)
    for (def in ChallengeCatalog.entries) {
      val results = TrapFairnessPolicy.validateChallengeTraps(def, AdaptiveTier.RELENTLESS)
      assertTrue(
          "Challenge ${def.challengeId} must remain fair under TRAP_RUSH",
          results.all { it.isFair }
      )
    }

    // 2. NO_CHECKPOINT keeps checkpoint bell dormant
    val ch03Config = ChallengeCatalog.createLockedConfig("ch_03", AdaptiveTier.STANDARD)!!
    val audio = ProceduralAudioEngine(context = app).apply { isMusicEnabled = false; isSfxEnabled = false }
    val noCpEngine = ChaosGameEngine(
        level = ch03Config.levelDefinition,
        audioEngine = audio,
        lockedChallengeConfig = ch03Config,
        initialLaunchMode = RunLaunchMode.MASTERY_CHALLENGE,
        initialModifiers = setOf(ChaosModifier.NO_CHECKPOINT)
    )
    val cpTile = noCpEngine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    noCpEngine.readWorldStateLocked {
      noCpEngine.player.x = cpTile.x
      noCpEngine.player.y = cpTile.y
    }
    noCpEngine.update()
    assertFalse("NO_CHECKPOINT must keep checkpoint inactive", noCpEngine.hasCheckpointActivated)
    assertFalse("usedCheckpointInAttempt must remain false", noCpEngine.usedCheckpointInAttempt)

    // 3. SPEED_RUN enforces par time limit cleanly
    val speedConfig = ChallengeCatalog.createLockedConfig("ch_01", AdaptiveTier.STANDARD)!!
    val speedEngine = ChaosGameEngine(
        level = speedConfig.levelDefinition,
        audioEngine = audio,
        lockedChallengeConfig = speedConfig,
        initialLaunchMode = RunLaunchMode.MASTERY_CHALLENGE,
        initialModifiers = setOf(ChaosModifier.SPEED_RUN, ChaosModifier.LANTERN_MIST),
        initialVictoryEffect = VictoryEffect.SAFFRON_FIREWORKS
    )
    assertTrue(speedEngine.speedRunLimitMs > 0L)
    // Physics & hitbox invariants remain strictly untouched
    assertEquals(0.58f, ChaosGameEngine.GRAVITY, 0.0001f)
    assertEquals(-13.2f, ChaosGameEngine.JUMP_IMPULSE, 0.0001f)
    assertEquals(6.5f, ChaosGameEngine.MAX_RUN_SPEED, 0.0001f)
    assertEquals(32f, speedEngine.player.collisionWidth, 0.0001f)
    assertEquals(54f, speedEngine.player.collisionHeight, 0.0001f)

    // Render with equipped cosmetic trail + LANTERN_MIST without exception
    val renderer = ChaosGameRenderer(app).apply {
      equippedTrail = CosmeticTrail.EMBER_SPARK
    }
    val bmp = Bitmap.createBitmap(480, 270, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    renderer.render(canvas, speedEngine, 480f, 270f)
    speedEngine.inputRight = true
    repeat(4) {
      speedEngine.update()
      renderer.render(canvas, speedEngine, 480f, 270f)
    }
    bmp.recycle()

    // Simulate Speed Run clock expiry -> triggers death with modifier_speed_run cause
    val framesToExpire = (speedEngine.speedRunLimitMs / 16L).toInt() + 10
    speedEngine.inputRight = false
    repeat(framesToExpire) {
      if (!speedEngine.isDying) {
        speedEngine.update()
      }
    }
    assertTrue("SPEED_RUN expiry must trigger attempt reset death", speedEngine.isDying)
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) {
      if (speedEngine.isDying) speedEngine.update()
    }
    assertEquals(1, speedEngine.deathCount)
    assertEquals("modifier_speed_run", speedEngine.lastDeathCauseKey)
    audio.release()
  }

  @Test
  fun test04_modifierRunsStayIsolatedFromNormalRecordsAndSurviveRetry() {
    val repo = SaveRepository(app)
    val normalRecord = ChallengeRecord(
        challengeId = "ch_01",
        completed = true,
        bestTimeMs = 15_000L,
        bestScore = 900,
        stars = 3,
        leastDeaths = 0
    )
    repo.saveReplayChallengeRecordIfImproved("ch_01", normalRecord, syncCommit = true)

    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.refreshSavedProgress()
    }
    composeTestRule.waitForIdle()

    composeTestRule.activityRule.scenario.onActivity {
      vm.launchMasteryOrModifierChallenge("ch_01", setOf(ChaosModifier.SPEED_RUN, ChaosModifier.NO_CHECKPOINT))
      val engine = vm.gameEngine!!
      assertEquals(setOf(ChaosModifier.SPEED_RUN, ChaosModifier.NO_CHECKPOINT), engine.activeModifiers)

      // Retry / restart preserves active modifiers
      vm.retryCurrentChallenge()
      assertEquals(setOf(ChaosModifier.SPEED_RUN, ChaosModifier.NO_CHECKPOINT), engine.activeModifiers)

      reachExitGate(engine, coinsCollectedCount = 0)
      vm.flushPendingPersistence()
    }
    composeTestRule.waitForIdle()

    val persistedNormal = repo.loadContinuousRunState().challengeRecords["ch_01"]!!
    assertEquals(15_000L, persistedNormal.bestTimeMs)
    assertEquals(900, persistedNormal.bestScore)
    assertEquals(3, persistedNormal.stars)

    val retention = repo.loadPhase4RetentionSnapshot()
    val modKey = ChaosModifier.canonicalKey(setOf(ChaosModifier.SPEED_RUN, ChaosModifier.NO_CHECKPOINT))
    val modRecord = retention.modifierRecords["ch_01::$modKey"]
    assertTrue("Modifier record must be recorded separately", modRecord != null && modRecord.clearsCount == 1)
  }

  @Test
  fun test05_offlineDailyChallengeDeterminismClockSafetyAndDuplicatePrevention() {
    val d1 = OfflineCalendarDate(2026, 9, 5)
    val d2 = OfflineCalendarDate(2026, 9, 6)
    val spec1A = OfflineDailyChallengeGenerator.forDate(d1)
    val spec1B = OfflineDailyChallengeGenerator.forDate(d1)
    val spec2 = OfflineDailyChallengeGenerator.forDate(d2)

    assertEquals(spec1A, spec1B)
    assertEquals("2026-09-05", spec1A.dateKey)
    assertEquals("2026-09-06", spec2.dateKey)
    assertNotEquals(spec1A.deterministicSeed, spec2.deterministicSeed)

    // Corrupt / invalid date falls back safely
    val badDateSpec = OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate(2026, 2, 30))
    assertTrue(badDateSpec.isFallback)
    assertEquals(OfflineCalendarDate.SAFE_FALLBACK.toIsoDateKey(), badDateSpec.dateKey)

    // Duplicate & backward clock safety
    val repo = SaveRepository(app)
    assertTrue(repo.recordDailyCompletionIfNew("2026-09-05", syncCommit = true))
    assertFalse("Same date must not claim reward twice", repo.recordDailyCompletionIfNew("2026-09-05", syncCommit = true))
    assertTrue(repo.recordDailyCompletionIfNew("2026-09-06", syncCommit = true))
    // Backward clock return to already completed 2026-09-05 must still not increment total clears
    assertFalse(repo.recordDailyCompletionIfNew("2026-09-05", syncCommit = true))
    assertFalse("Invalid date string rejected", repo.recordDailyCompletionIfNew("not-a-date", syncCommit = true))

    val snap = repo.loadPhase4RetentionSnapshot()
    assertEquals(2, snap.dailyTotalClears)
    assertTrue(snap.dailyCompletedDates.contains("2026-09-05"))
    assertTrue(snap.dailyCompletedDates.contains("2026-09-06"))
  }

  @Test
  fun test06_schemaV2ToV3MigrationPreservesContinuousRunAndDoesNotHijackRecovery() {
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("cr_schema_version", 2)
        .putBoolean("cr_run_active", true)
        .putBoolean("cr_run_in_foreground", true)
        .putBoolean("cr_run_completed", false)
        .putString("cr_active_challenge_id", "ch_04")
        .putString("cr_completed_challenge_ids", "ch_01,ch_02,ch_03")
        .putInt("cr_current_challenge_deaths", 2)
        .putLong("cr_cumulative_timer_ms", 42_500L)
        .putInt("cr_cumulative_deaths", 5)
        .putBoolean("cr_rec_ch_01_completed", true)
        .putLong("cr_rec_ch_01_best_time", 12_345L)
        .putInt("cr_rec_ch_01_stars", 3)
        .commit()

    val repo = SaveRepository(app)
    repo.migrateIfNeeded()
    repo.migrateIfNeeded() // idempotent

    assertEquals(SaveRepository.SAVE_SCHEMA_VERSION, prefs.getInt("cr_schema_version", 0))
    val state = repo.loadContinuousRunState()
    assertTrue(state.isRunActive)
    assertEquals("ch_04", state.activeChallengeId)
    assertEquals(setOf("ch_01", "ch_02", "ch_03"), state.completedChallengeIds)
    assertEquals(42_500L, state.cumulativeRunTimerMs)
    assertEquals(5, state.cumulativeDeaths)
    assertEquals(12_345L, state.challengeRecords["ch_01"]?.bestTimeMs)

    val p4 = repo.loadPhase4RetentionSnapshot()
    assertEquals(0, p4.totalMasteryStars)
    assertEquals(CosmeticTrail.NONE, p4.rewards.equippedTrail)
  }

  @Test
  fun test07_progressionHubUiNavigationAndDailyLaunchFlow() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.dailyDateProvider = { OfflineCalendarDate(2026, 10, 21) }
      vm.refreshDailyChallenge()
    }
    composeTestRule.waitForIdle()

    // Open Rewards from Home
    composeTestRule.onNodeWithTag("button_rewards").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_progression_hub").assertIsDisplayed()
    assertEquals(AppScreen.PROGRESSION_HUB, vm.currentScreen.value)

    // Verify Rewards tabs & absence of mode tabs; programmatic Daily & Modifiers remain intact internally
    composeTestRule.onNodeWithTag("tab_rewards_trails").performClick()
    composeTestRule.onNodeWithTag("reward_trail_none").performScrollTo().assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("button_play_daily").fetchSemanticsNodes().size)

    composeTestRule.activityRule.scenario.onActivity {
      vm.toggleModifierSelection(ChaosModifier.NO_CHECKPOINT)
      assertTrue(vm.selectedModifiers.value.contains(ChaosModifier.NO_CHECKPOINT))
      vm.launchDailyChallenge()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()
    assertEquals(RunLaunchMode.DAILY_CHALLENGE, vm.runLaunchMode.value)
    assertEquals("2026-10-21", vm.gameEngine?.dailyDateKey)
  }
}
