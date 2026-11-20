package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveMetricsState
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ChallengePerformanceRecord
import com.example.data.ChallengeRecord
import com.example.data.ContinuousRunPersistedState
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TrapTriggerType
import com.example.data.TrollTrap
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import com.example.data.World1Levels
import com.example.engine.AdaptiveDifficultyController
import com.example.engine.ChallengeCompletionOutcome
import com.example.engine.ChaosGameEngine
import com.example.engine.ContinuousRunCoordinator
import com.example.engine.TrapClueQualification
import com.example.engine.TrapFairnessClass
import com.example.engine.TrapFairnessPolicy
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase3AdaptiveDifficultyAndFairnessTest {

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
  }

  private fun completeWithStats(
      coordinator: ContinuousRunCoordinator,
      challengeId: String,
      deaths: Int,
      timeMs: Long,
      score: Int = 1500
  ): ChallengeCompletionOutcome {
    val def = ChallengeCatalog.findById(challengeId)!!
    val level = def.buildLevel()
    return coordinator.onChallengeCompleted(
        challengeId = challengeId,
        attemptToken = coordinator.currentAttemptToken,
        timeMs = timeMs,
        score = score,
        coinsCollected = level.coins.size,
        totalCoins = level.coins.size,
        parTimeSeconds = level.parTimeSeconds,
        isTrollLevel = level.isTrollLevel,
        challengeDeaths = deaths,
        engineCumulativeRunTimerMs = coordinator.cumulativeRunTimerMs + timeMs,
        engineCumulativeDeaths = coordinator.cumulativeDeaths
    )
  }

  private fun finishDeathAnimation(engine: ChaosGameEngine) {
    repeat(engine.deathAnimDurationFrames + 2) {
      if (engine.isDying) engine.update()
    }
    assertFalse("Death animation must finish cleanly", engine.isDying)
  }

  @Test
  fun test01_freshRunStartsInStandardTierAndLocksCh01() {
    val coordinator = ContinuousRunCoordinator()
    val firstLocked = coordinator.startNewRun()
    assertEquals("ch_01", firstLocked.challengeId)
    assertEquals(AdaptiveTier.STANDARD, firstLocked.selectedAdaptiveTier)
    assertEquals(AdaptiveTier.STANDARD, coordinator.currentAdaptiveTier)
    assertEquals(0, coordinator.currentChallengeDeaths)

    val vm = GameStateViewModel(app)
    vm.startNewContinuousRun()
    val engine = vm.gameEngine!!
    assertEquals(RunLaunchMode.CONTINUOUS_PROGRESSION, engine.launchMode)
    assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(AdaptiveTier.STANDARD, engine.hudSnapshot.activeAdaptiveTier)
  }

  @Test
  fun test02_assistedEligibleAfterFourChallengeDeathsAtRetryBoundary() {
    val coordinator = ContinuousRunCoordinator()
    val initial = coordinator.startNewRun()
    val expectedSeed = initial.challengeSeed
    val expectedVersion = initial.configVersion

    // Deaths 1..3 from distinct causes must NOT enter ASSISTED yet
    val causes = listOf("pit", "trap_4_FALLING_CEILING_BLOCK", "trap_9_FAKE_SAFE_PLATFORM")
    causes.forEachIndexed { idx, cause ->
      coordinator.recordDeath("ch_01", trapCauseKey = cause)
      val retryCfg = coordinator.prepareRetryCurrentChallenge()
      assertEquals("Death ${idx + 1} (<4 total, <3 same trap) must keep STANDARD", AdaptiveTier.STANDARD, retryCfg.selectedAdaptiveTier)
    }

    // 4th challenge death (still <3 on any single trap) -> eligible for ASSISTED on retry boundary
    coordinator.recordDeath("ch_01", trapCauseKey = "trap_3_SURPRISE_SPIKE_POP")
    // Before prepareRetryCurrentChallenge(), activeLockedConfig remains immutable at STANDARD
    assertEquals(AdaptiveTier.STANDARD, coordinator.activeLockedConfig.selectedAdaptiveTier)

    val assistedCfg = coordinator.prepareRetryCurrentChallenge()
    assertEquals(AdaptiveTier.ASSISTED, assistedCfg.selectedAdaptiveTier)
    assertEquals("ch_01", assistedCfg.challengeId)
    assertEquals(expectedSeed, assistedCfg.challengeSeed)
    assertEquals(expectedVersion, assistedCfg.configVersion)
  }

  @Test
  fun test03_assistedEligibleAfterThreeSameTrapDeathsAtRetryBoundary() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()

    val trapCause = "trap_3_SURPRISE_SPIKE_POP"
    coordinator.recordDeath("ch_01", trapCauseKey = trapCause)
    assertEquals(AdaptiveTier.STANDARD, coordinator.prepareRetryCurrentChallenge().selectedAdaptiveTier)

    coordinator.recordDeath("ch_01", trapCauseKey = trapCause)
    assertEquals(AdaptiveTier.STANDARD, coordinator.prepareRetryCurrentChallenge().selectedAdaptiveTier)

    // 3rd death to exact same trap triggers ASSISTED even though total challenge deaths == 3 (<4)
    coordinator.recordDeath("ch_01", trapCauseKey = trapCause)
    assertEquals(3, coordinator.currentChallengeDeaths)
    assertEquals(AdaptiveTier.STANDARD, coordinator.activeLockedConfig.selectedAdaptiveTier)

    val retryCfg = coordinator.prepareRetryCurrentChallenge()
    assertEquals(AdaptiveTier.ASSISTED, retryCfg.selectedAdaptiveTier)
  }

  @Test
  fun test04_lockedAttemptNeverMutatesMidAttemptOrDuringDeathSpin() {
    val vm = GameStateViewModel(app)
    vm.startNewContinuousRun()
    val engine = vm.gameEngine!!
    val initialAttemptConfig = engine.lockedChallengeConfig!!
    val spikeTrap = engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    val baselineTelegraph = spikeTrap.lockedTelegraphDurationFrames
    assertEquals(AdaptiveTier.STANDARD, initialAttemptConfig.selectedAdaptiveTier)

    // Trigger 2 prior deaths on same spike trap -> attempt #3 still locked at STANDARD
    repeat(2) {
      engine.startDeathSequence("SURPRISE!", "trap_3_SURPRISE_SPIKE_POP")
      finishDeathAnimation(engine)
      assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    }

    // Attempt #3: hit the spike trap a 3rd time (crossing threshold!)
    engine.startDeathSequence("SURPRISE!", "trap_3_SURPRISE_SPIKE_POP")
    assertTrue(engine.isDying)
    // Mid-death-spin before respawn boundary: locked attempt MUST NOT mutate!
    assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(baselineTelegraph, spikeTrap.lockedTelegraphDurationFrames)
    assertEquals(AdaptiveTier.STANDARD, initialAttemptConfig.selectedAdaptiveTier)

    // Finish death spin -> retry boundary transitions attempt #4 to ASSISTED
    finishDeathAnimation(engine)
    assertEquals(AdaptiveTier.ASSISTED, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(AdaptiveTier.STANDARD, initialAttemptConfig.selectedAdaptiveTier)
    assertTrue("ASSISTED attempt must extend spike telegraph window", spikeTrap.lockedTelegraphDurationFrames > baselineTelegraph)
  }

  @Test
  fun test05_tierAppliesAtRetryBoundaryViaBothAutoRespawnAndManualRetry() {
    val vm = GameStateViewModel(app)
    vm.startNewContinuousRun()
    val engine = vm.gameEngine!!
    val seed = engine.lockedChallengeConfig!!.challengeSeed

    // Reach 3 pit deaths (still STANDARD), then 4th pit death via manual ViewModel retry
    repeat(3) {
      engine.player.y = ChaosGameEngine.PIT_KILL_Y + 25f
      engine.update()
      assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    }
    assertEquals(3, engine.deathCount)

    // Record 4th pit death via auto-respawn
    engine.player.y = ChaosGameEngine.PIT_KILL_Y + 25f
    engine.update()
    assertEquals(4, engine.deathCount)
    assertEquals(AdaptiveTier.ASSISTED, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(seed, engine.lockedChallengeConfig!!.challengeSeed)

    // Persisted tier must already be updated on retry boundary
    assertEquals(AdaptiveTier.ASSISTED, vm.saveRepository.loadContinuousRunState().activeAdaptiveTier)

    // Manual retry via pause/retry keeps ASSISTED consistently without advancing tier improperly
    val tokenBeforeManualRetry = vm.continuousCoordinator.currentAttemptToken
    vm.retryCurrentChallenge()
    assertEquals(AdaptiveTier.ASSISTED, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(seed, engine.lockedChallengeConfig!!.challengeSeed)
    assertTrue(vm.continuousCoordinator.currentAttemptToken > tokenBeforeManualRetry)
  }

  @Test
  fun test06_tierEvaluatedAndLockedAtChallengeTransitionBoundary() {
    val vm = GameStateViewModel(app)
    vm.startNewContinuousRun()
    val engine = vm.gameEngine!!
    val ch1AttemptConfig = engine.lockedChallengeConfig!!
    assertEquals("ch_01", ch1AttemptConfig.challengeId)
    assertEquals(AdaptiveTier.STANDARD, ch1AttemptConfig.selectedAdaptiveTier)

    // 4 deaths on ch_01 -> auto-respawn locks attempt in ASSISTED, then complete ch_01
    repeat(4) {
      engine.startDeathSequence("OOPS!", "pit")
      finishDeathAnimation(engine)
    }
    assertEquals(AdaptiveTier.ASSISTED, engine.lockedChallengeConfig!!.selectedAdaptiveTier)

    // Relocate exit once & cross Sun Gate on ch_01
    val reloc = engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    engine.player.x = (reloc.triggerMinX + reloc.triggerMaxX) / 2f
    engine.player.y = 11f * World1Levels.TILE_SIZE
    engine.update()
    engine.player.x = engine.currentExitX + 24f
    engine.player.y = engine.currentExitY + 60f
    engine.update()

    // Transition to ch_02 MUST recover conservatively from ASSISTED back to STANDARD
    assertEquals("ch_02", engine.activeChallengeId)
    assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(AdaptiveTier.STANDARD, vm.saveRepository.loadContinuousRunState().activeAdaptiveTier)
    assertEquals(AdaptiveTier.STANDARD, ch1AttemptConfig.selectedAdaptiveTier)
  }

  @Test
  fun test07_escalatedPromotionRequiresMultiChallengeEvidenceNotOneLuckyWin() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()

    // Clear ch_01 with 0 deaths fast -> single lucky win stays STANDARD
    val afterCh1 = completeWithStats(coordinator, "ch_01", deaths = 0, timeMs = 15_000L)
        as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.STANDARD, afterCh1.nextConfig.selectedAdaptiveTier)

    // Clear ch_02 with 0 deaths fast -> 2 consecutive strong wins promote to ESCALATED for ch_03
    val afterCh2 = completeWithStats(coordinator, "ch_02", deaths = 0, timeMs = 18_000L)
        as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.ESCALATED, afterCh2.nextConfig.selectedAdaptiveTier)
  }

  @Test
  fun test08_relentlessRequiresSustainedFourChallengeEvidenceElseRemainsEscalated() {
    // Path A: 4 consecutive dominant completions (ch_01..ch_04) -> ch_05 promotes to RELENTLESS
    val eliteCoordinator = ContinuousRunCoordinator()
    eliteCoordinator.startNewRun()
    val o1 = completeWithStats(eliteCoordinator, "ch_01", deaths = 0, timeMs = 16_000L) as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.STANDARD, o1.nextConfig.selectedAdaptiveTier)
    val o2 = completeWithStats(eliteCoordinator, "ch_02", deaths = 0, timeMs = 18_000L) as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.ESCALATED, o2.nextConfig.selectedAdaptiveTier)
    val o3 = completeWithStats(eliteCoordinator, "ch_03", deaths = 0, timeMs = 20_000L) as ChallengeCompletionOutcome.NextChallenge
    assertEquals("3rd completion still below 4-challenge RELENTLESS window", AdaptiveTier.ESCALATED, o3.nextConfig.selectedAdaptiveTier)
    val o4 = completeWithStats(eliteCoordinator, "ch_04", deaths = 0, timeMs = 22_000L) as ChallengeCompletionOutcome.NextChallenge
    assertEquals("4 sustained strong clears qualify ch_05 for RELENTLESS", AdaptiveTier.RELENTLESS, o4.nextConfig.selectedAdaptiveTier)

    // Path B: Strong ch_01/02 -> ESCALATED on ch_03, but 2 total deaths in window (1 on ch_01, 1 on ch_03) -> stays ESCALATED
    val mixedCoordinator = ContinuousRunCoordinator()
    mixedCoordinator.startNewRun()
    completeWithStats(mixedCoordinator, "ch_01", deaths = 1, timeMs = 20_000L)
    val m2 = completeWithStats(mixedCoordinator, "ch_02", deaths = 0, timeMs = 20_000L) as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.ESCALATED, m2.nextConfig.selectedAdaptiveTier)
    completeWithStats(mixedCoordinator, "ch_03", deaths = 1, timeMs = 22_000L)
    val m4 = completeWithStats(mixedCoordinator, "ch_04", deaths = 0, timeMs = 22_000L) as ChallengeCompletionOutcome.NextChallenge
    assertEquals("Insufficient dominant mastery across 4-challenge window keeps ESCALATED", AdaptiveTier.ESCALATED, m4.nextConfig.selectedAdaptiveTier)
  }

  @Test
  fun test09_hysteresisAndCooldownPreventTierThrashingAcrossRetriesAndTransitions() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()

    // Enter ASSISTED on ch_01 at 4 deaths -> subsequent 5th & 6th deaths stay smoothly in ASSISTED
    repeat(4) { coordinator.recordDeath("ch_01", trapCauseKey = "pit") }
    assertEquals(AdaptiveTier.ASSISTED, coordinator.prepareRetryCurrentChallenge().selectedAdaptiveTier)
    repeat(2) {
      coordinator.recordDeath("ch_01", trapCauseKey = "trap_3_SURPRISE_SPIKE_POP")
      assertEquals(AdaptiveTier.ASSISTED, coordinator.prepareRetryCurrentChallenge().selectedAdaptiveTier)
    }

    // Completing ch_01 in ASSISTED recovers conservatively to STANDARD (with cooldown=1)
    val afterCh1 = completeWithStats(coordinator, "ch_01", deaths = 6, timeMs = 30_000L)
        as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.STANDARD, afterCh1.nextConfig.selectedAdaptiveTier)

    // Clearing ch_02 cleanly right after recovery decrements cooldown & builds streak to 1 -> still STANDARD (not thrashing!)
    val afterCh2 = completeWithStats(coordinator, "ch_02", deaths = 0, timeMs = 18_000L)
        as ChallengeCompletionOutcome.NextChallenge
    assertEquals("Cooldown after ASSISTED recovery prevents jumping straight to ESCALATED", AdaptiveTier.STANDARD, afterCh2.nextConfig.selectedAdaptiveTier)

    // Clearing ch_03 cleanly -> now 2 strong clears after recovery -> promotes to ESCALATED
    val afterCh3 = completeWithStats(coordinator, "ch_03", deaths = 0, timeMs = 19_000L)
        as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.ESCALATED, afterCh3.nextConfig.selectedAdaptiveTier)

    // Struggling (2 deaths) on ch_04 steps down ESCALATED -> STANDARD at retry boundary with cooldown
    repeat(2) { coordinator.recordDeath("ch_04", trapCauseKey = "pit") }
    assertEquals(AdaptiveTier.STANDARD, coordinator.prepareRetryCurrentChallenge().selectedAdaptiveTier)
    // Clearing ch_04 immediately after step-down stays STANDARD (no thrashing back to ESCALATED)
    val afterCh4 = completeWithStats(coordinator, "ch_04", deaths = 2, timeMs = 32_000L)
        as ChallengeCompletionOutcome.NextChallenge
    assertEquals(AdaptiveTier.STANDARD, afterCh4.nextConfig.selectedAdaptiveTier)
  }

  @Test
  fun test10_assistedNeverForcedOnAlreadyCompletedChallengeOrReplay() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()
    completeWithStats(coordinator, "ch_01", deaths = 0, timeMs = 14_000L)

    // Simulate a state where an already-completed challenge is checked for retry eligibility -> must NOT force ASSISTED
    val evalCompleted = coordinator.adaptiveController.evaluateRetryBoundary(
        challengeId = "ch_01",
        isAlreadyCompletedChallenge = true
    )
    assertNotEquals(AdaptiveTier.ASSISTED, evalCompleted)

    // Also test full ViewModel REPLAY mode: dying 5 times in Replay of ch_01 never activates ASSISTED or alters run state
    val vm = GameStateViewModel(app)
    vm.startNewContinuousRun()
    val ch1Engine = vm.gameEngine!!
    val reloc = ch1Engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    ch1Engine.player.x = (reloc.triggerMinX + reloc.triggerMaxX) / 2f
    ch1Engine.player.y = 11f * World1Levels.TILE_SIZE
    ch1Engine.update()
    ch1Engine.player.x = ch1Engine.currentExitX + 24f
    ch1Engine.player.y = ch1Engine.currentExitY + 60f
    ch1Engine.update()
    assertEquals("ch_02", vm.continuousCoordinator.activeChallengeId)

    vm.replayCompletedChallenge("ch_01")
    val replayEngine = vm.gameEngine!!
    assertEquals(RunLaunchMode.REPLAY, replayEngine.launchMode)
    assertEquals(AdaptiveTier.STANDARD, replayEngine.lockedChallengeConfig!!.selectedAdaptiveTier)

    repeat(5) {
      replayEngine.startDeathSequence("REPLAY_FAIL", "trap_3_SURPRISE_SPIKE_POP")
      finishDeathAnimation(replayEngine)
      assertEquals(AdaptiveTier.STANDARD, replayEngine.lockedChallengeConfig!!.selectedAdaptiveTier)
    }
    assertEquals("ch_02", vm.continuousCoordinator.activeChallengeId)
    assertEquals(AdaptiveTier.STANDARD, vm.continuousCoordinator.currentAdaptiveTier)
    assertEquals(0, vm.continuousCoordinator.currentChallengeDeaths)
  }

  @Test
  fun test11_allAuthoredChallengeTrapsPassCentralFairnessPolicyAcrossAllTiers() {
    for (challengeDef in ChallengeCatalog.entries) {
      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(challengeDef, tier)
        assertTrue("Challenge ${challengeDef.challengeId} must contain authored traps", validations.isNotEmpty())
        for (v in validations) {
          assertTrue(
              "Fairness check failed on ${challengeDef.challengeId} trap #${v.trapId} (${v.trapType}) @ $tier: ${v.reason}",
              v.isFair
          )
          assertTrue(
              "Warning must precede lethal hitbox on ${challengeDef.challengeId} trap #${v.trapId}",
              v.warningOccursBeforeActiveHitbox
          )
          if (v.fairnessClass == TrapFairnessClass.STANDARD_LETHAL_FLOOR) {
            assertTrue(
                "Standard lethal floor trap #${v.trapId} @ $tier must have >= 12f warning (actual=${v.effectiveTelegraphFrames})",
                v.effectiveTelegraphFrames >= TrapFairnessPolicy.STANDARD_LETHAL_FLOOR_MIN_FRAMES
            )
          } else if (v.fairnessClass == TrapFairnessClass.QUALIFIED_STATIC_CLUE_LETHAL) {
            assertTrue(v.qualification.isQualified)
            assertTrue(
                "Qualified static-clue trap #${v.trapId} @ $tier must have >= 6f warning (actual=${v.effectiveTelegraphFrames})",
                v.effectiveTelegraphFrames >= TrapFairnessPolicy.QUALIFIED_SHORT_WARNING_MIN_FRAMES
            )
          }
        }
      }
    }
  }

  @Test
  fun test12_adaptiveTiersExtendAssistedWarningsAndNeverShortenBelowFairnessMinimum() {
    val ch1FloorSpike = ChallengeCatalog.findById("ch_01")!!.buildLevel()
        .trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    val generousSpike = ch1FloorSpike.copy(telegraphDurationFrames = 16)
    val ceilingTrap = ChallengeCatalog.findById("ch_01")!!.buildLevel()
        .trollTraps.first { it.type == TrollTrapType.FALLING_CEILING_BLOCK }
    val blinkTrap = ChallengeCatalog.findById("ch_04")!!.buildLevel()
        .trollTraps.first { it.type == TrollTrapType.TIMED_BLINK_PLATFORM }

    // ASSISTED extends warnings meaningfully & slows moving hazards / widens blink cycle
    val assistedFloor = TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ch1FloorSpike, AdaptiveTier.ASSISTED)
    val standardFloor = TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ch1FloorSpike, AdaptiveTier.STANDARD)
    val escalatedFloor = TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ch1FloorSpike, AdaptiveTier.ESCALATED)
    val relentlessFloor = TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ch1FloorSpike, AdaptiveTier.RELENTLESS)

    assertTrue("ASSISTED must extend telegraph (${assistedFloor} > ${standardFloor})", assistedFloor > standardFloor)
    assertEquals(12, standardFloor)
    assertEquals("ESCALATED must clamp at 12f floor minimum", 12, escalatedFloor)
    assertEquals("RELENTLESS must clamp at 12f floor minimum", 12, relentlessFloor)

    // Generous 16f floor trap trims gently under higher tiers but stays >= 12f
    assertEquals(20, TrapFairnessPolicy.resolveEffectiveTelegraphFrames(generousSpike, AdaptiveTier.ASSISTED))
    assertEquals(14, TrapFairnessPolicy.resolveEffectiveTelegraphFrames(generousSpike, AdaptiveTier.ESCALATED))
    assertEquals(13, TrapFairnessPolicy.resolveEffectiveTelegraphFrames(generousSpike, AdaptiveTier.RELENTLESS))

    // Unqualified trap claiming short warning (missing static clue / spacing) cannot use 6..11f exception
    val badClueQual = TrapClueQualification(
        hasStaticVisualClue = false,
        hasMinimumReactionSpacing = false,
        hasEscapeOpportunity = true
    )
    assertEquals(
        12,
        TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ceilingTrap, AdaptiveTier.STANDARD, badClueQual)
    )

    // Speed & blink cycle tuning
    assertTrue(
        TrapFairnessPolicy.resolveEffectiveMoveSpeed(ceilingTrap, AdaptiveTier.ASSISTED) <
            TrapFairnessPolicy.resolveEffectiveMoveSpeed(ceilingTrap, AdaptiveTier.STANDARD)
    )
    assertTrue(
        TrapFairnessPolicy.resolveEffectiveCycleFrames(blinkTrap, AdaptiveTier.ASSISTED) >
            TrapFairnessPolicy.resolveEffectiveCycleFrames(blinkTrap, AdaptiveTier.STANDARD)
    )
    assertTrue(
        TrapFairnessPolicy.resolveEffectiveCycleFrames(blinkTrap, AdaptiveTier.RELENTLESS) >=
            TrapFairnessPolicy.MIN_BLINK_CYCLE_FRAMES
    )
  }

  @Test
  fun test13_persistedAdaptiveMetricsAndProcessDeathRecoveryRestoreExactTier() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()
    completeWithStats(coordinator, "ch_01", deaths = 0, timeMs = 15_000L)
    completeWithStats(coordinator, "ch_02", deaths = 0, timeMs = 16_000L)
    assertEquals(AdaptiveTier.ESCALATED, coordinator.currentAdaptiveTier)
    coordinator.recordDeath("ch_03", trapCauseKey = "trap_2_MOVING_CRUSH_WALL")

    val repo = SaveRepository(app)
    val state = coordinator.exportPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunInForeground = true
    )
    repo.saveContinuousRunState(state, syncCommit = true)

    // Simulate OS process death and fresh launch
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      scenario.onActivity { activity ->
        val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
        assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
        assertEquals(RunLaunchMode.CONTINUOUS_PROGRESSION, vm.runLaunchMode.value)
        val engine = vm.gameEngine!!
        assertEquals("ch_03", engine.activeChallengeId)
        assertEquals(AdaptiveTier.ESCALATED, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
        assertEquals(AdaptiveTier.ESCALATED, vm.continuousCoordinator.currentAdaptiveTier)
        assertEquals(1, vm.continuousCoordinator.currentChallengeDeaths)
        assertEquals(1, vm.continuousCoordinator.adaptiveController.getTrapDeathsMap()["trap_2_MOVING_CRUSH_WALL"])
        assertEquals(2, vm.continuousCoordinator.adaptiveController.getRecentCompletions().size)
      }
    } finally {
      scenario.close()
    }
  }

  @Test
  fun test14_malformedAdaptiveSaveDataSafelyFallsBackToStandardWithoutCrash() {
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("cr_schema_version", 2)
        .putBoolean("cr_run_active", true)
        .putBoolean("cr_run_in_foreground", false)
        .putBoolean("cr_run_completed", false)
        .putString("cr_active_challenge_id", "ch_02")
        .putString("cr_completed_challenge_ids", "ch_01")
        .putString("cr_active_adaptive_tier", "CORRUPTED_TIER_NAME")
        .putString("cr_adaptive_trap_deaths", "{not-valid-json!!")
        .putString("cr_adaptive_recent_outcomes", "[{bad_entry:true}]")
        .putString("cr_adaptive_total_deaths", "WRONG_TYPE_NAN")
        .putBoolean("cr_rec_ch_01_completed", true)
        .putInt("cr_rec_ch_01_stars", 3)
        .putLong("cr_rec_ch_01_best_time", 12345L)
        .commit()

    val repo = SaveRepository(app)
    val loaded = repo.loadContinuousRunState()
    assertEquals(AdaptiveTier.STANDARD, loaded.activeAdaptiveTier)
    assertTrue(loaded.adaptiveMetrics.currentChallengeTrapDeaths.isEmpty())
    assertTrue(loaded.adaptiveMetrics.recentCompletions.isEmpty())
    assertEquals(setOf("ch_01"), loaded.completedChallengeIds)
    assertEquals(3, loaded.challengeRecords["ch_01"]!!.stars)

    val coordinator = ContinuousRunCoordinator()
    val restored = coordinator.restoreFromPersisted(loaded)
    assertEquals("ch_02", restored.challengeId)
    assertEquals(AdaptiveTier.STANDARD, restored.selectedAdaptiveTier)
  }

  @Test
  fun test15_realEngineMultiAttemptFairnessAndAssistedClearSimulation() {
    val vm = GameStateViewModel(app)
    vm.startNewContinuousRun()
    val engine = vm.gameEngine!!
    val spikeTrap = engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    val t = World1Levels.TILE_SIZE

    // Attempt 1..3: trigger Surprise Spike directly; verify >=12f readable telegraph occurs BEFORE hitbox kills player
    for (attempt in 1..3) {
      assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
      assertEquals(12, spikeTrap.lockedTelegraphDurationFrames)

      engine.player.x = 17.6f * t
      engine.player.y = 13f * t - engine.player.collisionHeight
      engine.player.isGrounded = true
      engine.inputRight = true

      // Trigger entry into TELEGRAPH
      var enteredTelegraph = false
      for (f in 0..10) {
        engine.update()
        if (spikeTrap.state == TrollTrapState.TELEGRAPH) {
          enteredTelegraph = true
          break
        }
      }
      assertTrue("Attempt $attempt must enter TELEGRAPH first", enteredTelegraph)
      assertFalse("Player must stay alive on telegraph start frame", engine.isDying)

      // Advance 11 frames inside telegraph -> still in TELEGRAPH and NOT dying!
      repeat(11) {
        engine.update()
        assertFalse("Attempt $attempt must remain non-lethal throughout 12-frame telegraph window", engine.isDying)
      }

      // Continue walking until spike activates and kills player -> finishes respawn cleanly
      var died = false
      for (f in 0..20) {
        engine.update()
        if (engine.isDying) {
          died = true
          break
        }
      }
      assertTrue("Active spike must kill stationary/continuing player on attempt $attempt", died)
      finishDeathAnimation(engine)
      assertEquals(attempt, engine.deathCount)
    }

    // Attempt 4 now automatically locked into ASSISTED (+4f -> 16f warning) with completed ch_01 untouched!
    assertEquals(AdaptiveTier.ASSISTED, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
    assertEquals(16, spikeTrap.lockedTelegraphDurationFrames)

    // Complete ch_01 on attempt 4 -> recovers to STANDARD for ch_02 and records ch_01 completed
    val reloc = engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    engine.player.x = (reloc.triggerMinX + reloc.triggerMaxX) / 2f
    engine.player.y = 11f * t
    engine.update()
    engine.player.x = engine.currentExitX + 24f
    engine.player.y = engine.currentExitY + 60f
    engine.update()

    assertTrue(vm.continuousCoordinator.completedChallengeIds.contains("ch_01"))
    assertEquals("ch_02", vm.continuousCoordinator.activeChallengeId)
    assertEquals(AdaptiveTier.STANDARD, engine.lockedChallengeConfig!!.selectedAdaptiveTier)
  }
}
