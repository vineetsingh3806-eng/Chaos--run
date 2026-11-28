package com.example

import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-JVM verification for Phase 13 preservation guarantees:
 * - All 200 challenges (ch_01..ch_200), 6 Phase-11 trap mechanics, double jump, and 5 daylight themes remain intact
 * - ContinuousRunCoordinator advances directly from fresh start and saved mid-run states cleanly
 * - PlayableCharacter internal implementations, hitboxes, and save-schema v3 models stay preserved
 * - MasteryCatalog (600 stars), OfflineDailyChallengeGenerator, ChaosModifier, and all unlockable rewards remain intact
 */
class Phase13PreservationPureJvmTest {

  @Test
  fun test01_all200ChallengesThemesTrapsAndDoubleJumpPreserved() {
    assertEquals(200, ChallengeCatalog.entries.size)
    assertEquals(5, LevelTheme.entries.size)

    val phase11TrapsSeen = mutableSetOf<TrollTrapType>()
    for (entry in ChallengeCatalog.entries.filter { it.orderIndex >= 37 }) {
      for (trap in entry.buildLevel().trollTraps) {
        phase11TrapsSeen += trap.type
      }
    }
    assertTrue(phase11TrapsSeen.containsAll(listOf(
        TrollTrapType.SLIDING_FLOOR_SHIFT,
        TrollTrapType.SPLITTING_TWIN_FLOOR,
        TrollTrapType.REVERSING_PLATFORM,
        TrollTrapType.OPENING_AMBUSH_WALL,
        TrollTrapType.REVERSING_HAZARD,
        TrollTrapType.TEMP_VANISH_REFORM_FLOOR
    )))

    val cfg = ChallengeCatalog.createLockedConfig("ch_37", AdaptiveTier.STANDARD)!!
    val engine = ChaosGameEngine(
        level = cfg.levelDefinition,
        audioEngine = ProceduralAudioEngine(context = null),
        lockedChallengeConfig = cfg,
        initialLaunchMode = RunLaunchMode.CONTINUOUS_PROGRESSION
    )
    repeat(12) { engine.update() }
    assertTrue(engine.player.isGrounded)
    assertEquals(1, engine.player.airJumpsRemaining)

    engine.inputJump = true
    engine.update()
    assertFalse(engine.player.isGrounded)
    engine.inputJump = false
    engine.update()
    engine.inputJump = true
    engine.update()
    assertEquals(0, engine.player.airJumpsRemaining)
    assertTrue(engine.player.hasDoubleJumpedThisAirborne)
  }

  @Test
  fun test02_continuousRunCoordinatorResumesSavedMidRunAndPreservesAllInternalSystems() {
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()
    assertEquals("ch_01", coordinator.activeChallengeId)

    val outcome1 = coordinator.onChallengeCompleted(
        challengeId = "ch_01",
        attemptToken = coordinator.currentAttemptToken,
        timeMs = 11_500L,
        score = 650,
        coinsCollected = 2,
        totalCoins = 2,
        parTimeSeconds = 25,
        isTrollLevel = false,
        challengeDeaths = 0
    )
    assertTrue(outcome1 is ChallengeCompletionOutcome.NextChallenge)
    assertEquals("ch_02", coordinator.activeChallengeId)

    val exported = coordinator.exportPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunInForeground = true
    )
    assertEquals(3, SaveRepository.SAVE_SCHEMA_VERSION)
    assertEquals("ch_02", exported.activeChallengeId)
    assertTrue(exported.completedChallengeIds.contains("ch_01"))

    val restored = ContinuousRunCoordinator()
    restored.restoreFromPersisted(exported)
    assertEquals("ch_02", restored.activeChallengeId)
    assertTrue(restored.completedChallengeIds.contains("ch_01"))

    // Preserve PlayableCharacter, Rewards, Mastery, ChaosModifiers, Daily Generator internally
    assertEquals(4, PlayableCharacter.entries.size)
    assertEquals(PlayableCharacter.AARAV, PlayableCharacter.DEFAULT)
    assertEquals(600, MasteryCatalog.totalPossibleStars)
    assertEquals(4, ChaosModifier.entries.size)
    assertTrue(CosmeticTrail.entries.size >= 5)
    assertTrue(VictoryEffect.entries.size >= 4)
    assertTrue(PlayerTitle.entries.size >= 7)

    val daily = OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate(2026, 11, 28))
    assertTrue(daily.challengeOrderIndex in 1..200)
  }
}
