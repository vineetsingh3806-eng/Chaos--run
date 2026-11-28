package com.example

import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-JVM Phase 11 verification suite:
 * - 200 playable challenges (ch_01..ch_200), unique titles/IDs/seeds, continuous run completion
 * - 800 TrapFairnessPolicy validations (200 challenges x 4 AdaptiveTiers)
 * - Double-jump edge-trigger physics, 2-jump limit, ground/death/checkpoint/reset restoration
 * - All 6 new Phase 11 TrollTrapTypes: mechanics, carry physics, reversal, opening walls, reform, rollback
 * - Zero tall vertical jump-only spike-gate barriers across all 200 challenges
 * - MasteryCatalog (600 stars), OfflineDailyChallengeGenerator span, and zero-warning SURPRISE rules
 */
class Phase11ExpansionDoubleJumpTest {

  private fun engineForChallenge(
      challengeId: String,
      tier: AdaptiveTier = AdaptiveTier.STANDARD
  ): ChaosGameEngine {
    val cfg = ChallengeCatalog.createLockedConfig(challengeId, tier)
        ?: error("Missing challenge $challengeId")
    return ChaosGameEngine(
        level = cfg.levelDefinition,
        audioEngine = ProceduralAudioEngine(context = null),
        lockedChallengeConfig = cfg,
        initialLaunchMode = RunLaunchMode.MASTERY_CHALLENGE
    )
  }

  private fun rectsOverlap(
      ax: Float,
      ay: Float,
      aw: Float,
      ah: Float,
      bx: Float,
      by: Float,
      bw: Float,
      bh: Float
  ): Boolean {
    return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by
  }

  @Test
  fun test01_catalogHas200UniquePlayableChallengesAndPreservesFirst36() {
    assertEquals(200, ChallengeCatalog.entries.size)
    assertEquals((1..200).map { "ch_${it.toString().padStart(2, '0')}" }, ChallengeCatalog.entries.map { it.challengeId })
    assertEquals((1..200).toList(), ChallengeCatalog.entries.map { it.orderIndex })
    assertEquals((1001L..1200L).toList(), ChallengeCatalog.entries.map { it.challengeSeed })
    assertEquals(200, ChallengeCatalog.entries.map { it.title }.toSet().size)

    // Milestone identities
    assertEquals("The Easy Start", ChallengeCatalog.findById("ch_01")!!.title)
    assertEquals("The Last Laugh", ChallengeCatalog.findById("ch_36")!!.title)
    assertEquals("Slip-Slide Clearing", ChallengeCatalog.findById("ch_37")!!.title)
    assertEquals("Centurion Trial", ChallengeCatalog.findById("ch_100")!!.title)
    assertEquals("Crown of Chaos", ChallengeCatalog.findById("ch_200")!!.title)

    // ContinuousRunCoordinator runs seamlessly through all 200 stages
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()
    val visited = mutableListOf<String>()
    while (true) {
      val currentId = coordinator.activeChallengeId
      visited += currentId
      val outcome = coordinator.onChallengeCompleted(
          challengeId = currentId,
          attemptToken = coordinator.currentAttemptToken,
          timeMs = 14_000L,
          score = 800,
          coinsCollected = 2,
          totalCoins = 4,
          parTimeSeconds = 48,
          isTrollLevel = true,
          challengeDeaths = 0
      )
      when (outcome) {
        is ChallengeCompletionOutcome.NextChallenge -> Unit
        is ChallengeCompletionOutcome.RunCompleted -> {
          assertEquals(200, outcome.summary.completedCount)
          assertEquals(200, outcome.summary.totalChallenges)
          break
        }
        ChallengeCompletionOutcome.IgnoredDuplicate -> error("Unexpected duplicate on $currentId")
      }
    }
    assertEquals(200, visited.size)
    assertEquals("ch_200", visited.last())
  }

  @Test
  fun test02_all200ChallengesPass800FairnessChecksSafeSpawnsExitsAndNoVerticalSpikeGates() {
    val t = World1Levels.TILE_SIZE
    for (entry in ChallengeCatalog.entries) {
      val level = entry.buildLevel()

      // Safe ground beneath spawn and exit gate
      val spawnSupported = level.tiles.any { tile ->
        tile.isSolid &&
            level.spawnX + 16f >= tile.x - 10f &&
            level.spawnX + 16f <= tile.x + tile.width + 10f &&
            tile.y >= level.spawnY
      }
      assertTrue("${entry.challengeId} spawn must sit above solid floor", spawnSupported)

      val gate = level.tiles.firstOrNull { it.type == TileType.SUN_GATE }
      assertNotNull("${entry.challengeId} must have SUN_GATE", gate)
      gate!!
      val gateSupported = level.tiles.any { tile ->
        tile.isSolid &&
            gate.x + gate.width * 0.5f >= tile.x - 16f &&
            gate.x + gate.width * 0.5f <= tile.x + tile.width + 16f &&
            tile.y >= gate.y + gate.height - 8f
      }
      assertTrue("${entry.challengeId} SUN_GATE must rest on solid floor", gateSupported)

      // Spawn must not overlap lethal traps
      val spawnTrapHit = level.trollTraps.any { trap ->
        trap.killsPlayer &&
            trap.isVisible &&
            rectsOverlap(level.spawnX, level.spawnY, 32f, 54f, trap.initialX, trap.initialY, trap.width, trap.height)
      }
      assertFalse("${entry.challengeId} spawn must not start inside lethal trap", spawnTrapHit)

      // No tall vertical precision-jump rune-beam spike gates across any challenge
      val tallVerticalBeams = level.trollTraps.filter {
        it.type == TrollTrapType.TELEGRAPHED_RUNE_BEAM && it.height > it.width * 2f && it.height >= 2.8f * t
      }
      assertTrue(
          "${entry.challengeId} must not contain tall vertical jump-only beam gates",
          tallVerticalBeams.isEmpty()
      )

      // Checkpoints must not overlap initial traps
      val bells = level.tiles.filter { it.type == TileType.CHECKPOINT_BELL }
      assertTrue("${entry.challengeId} (ch_06+) should include checkpoint or ch_01..ch_05 rules", entry.orderIndex <= 5 || bells.isNotEmpty())
      for (bell in bells) {
        val bellOverlap = level.trollTraps.any { trap ->
          rectsOverlap(bell.x, bell.y, bell.width, bell.height, trap.initialX, trap.initialY, trap.width, trap.height)
        }
        assertFalse("${entry.challengeId} checkpoint bell overlaps initial trap", bellOverlap)
      }

      // Pass TrapFairnessPolicy across all 4 adaptive tiers (200 x 4 = 800 checks)
      for (tier in AdaptiveTier.entries) {
        val report = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
        assertTrue("${entry.challengeId} must contain traps", report.isNotEmpty())
        val failed = report.filterNot { it.isFair }
        assertTrue(
            "${entry.challengeId} ($tier) failed fairness: ${failed.joinToString { "#${it.trapId}:${it.reason}" }}",
            failed.isEmpty()
        )
      }
    }
  }

  @Test
  fun test03_doubleJumpEdgeTriggerLimitAndStateResetsWorkReliably() {
    val engine = engineForChallenge("ch_01")
    repeat(12) { engine.update() }
    assertTrue("Player starts grounded", engine.player.isGrounded)
    assertEquals(1, engine.player.airJumpsRemaining)

    // 1. Ground jump (jump #1) with held button must NOT auto-trigger double jump
    engine.inputJump = true
    engine.update()
    assertEquals(ChaosGameEngine.JUMP_IMPULSE + ChaosGameEngine.GRAVITY, engine.player.vy, 0.05f)
    assertFalse(engine.player.isGrounded)
    assertEquals("Held jump button must not consume air jump", 1, engine.player.airJumpsRemaining)

    repeat(10) { engine.update() }
    assertEquals("Still 1 air jump remaining while holding jump button", 1, engine.player.airJumpsRemaining)
    assertFalse(engine.player.hasDoubleJumpedThisAirborne)

    // 2. Release & press again in mid-air -> executes double jump (jump #2)
    engine.inputJump = false
    engine.update()
    engine.inputJump = true
    engine.update()
    assertTrue("Air jump must apply upward DOUBLE_JUMP_IMPULSE", engine.player.vy <= ChaosGameEngine.DOUBLE_JUMP_IMPULSE + 1.0f)
    assertEquals(0, engine.player.airJumpsRemaining)
    assertTrue(engine.player.hasDoubleJumpedThisAirborne)

    // 3. Trying a 3rd jump in the same airborne sequence must be forbidden
    repeat(8) { engine.update() }
    engine.inputJump = false
    engine.update()
    val vyBeforeThirdTap = engine.player.vy
    engine.inputJump = true
    engine.update()
    assertTrue("3rd jump in mid-air must not launch upward again", engine.player.vy >= vyBeforeThirdTap - 0.1f)
    assertEquals(0, engine.player.airJumpsRemaining)

    // 4. Landing resets double jump cleanly
    engine.inputJump = false
    var ticks = 0
    while (!engine.player.isGrounded && ticks < 120) {
      engine.update()
      ticks++
    }
    assertTrue("Player must land back on ground", engine.player.isGrounded)
    assertEquals(1, engine.player.airJumpsRemaining)
    assertFalse(engine.player.hasDoubleJumpedThisAirborne)

    // 5. Death & respawn / reset restores double jump cleanly
    engine.inputJump = true
    engine.update()
    engine.inputJump = false
    engine.update()
    engine.inputJump = true
    engine.update()
    assertEquals(0, engine.player.airJumpsRemaining)
    engine.inputJump = false

    engine.handlePlayerDeath("test_double_jump_reset")
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 4) { engine.update() }
    assertFalse(engine.isDying)
    assertEquals(1, engine.player.airJumpsRemaining)
    assertFalse(engine.player.hasDoubleJumpedThisAirborne)

    // 6. Paused engine must not consume air jump
    engine.isPaused = true
    engine.readWorldStateLocked {
      engine.player.isGrounded = false
      engine.player.coyoteTimer = 0
      engine.player.airJumpsRemaining = 1
    }
    engine.inputJump = true
    engine.update()
    assertEquals(1, engine.player.airJumpsRemaining)
  }

  @Test
  fun test04_all6NewTrollTrapsBehaveCollideReverseReformAndRollbackCleanly() {
    val t = World1Levels.TILE_SIZE

    // 1. SLIDING_FLOOR_SHIFT + REVERSING_HAZARD + TEMP_VANISH_REFORM_FLOOR + SPLITTING_TWIN_FLOOR (ch_37)
    val ch37 = engineForChallenge("ch_37")
    val slider = ch37.trollTraps.first { it.type == TrollTrapType.SLIDING_FLOOR_SHIFT }
    val initialSliderX = slider.x
    ch37.readWorldStateLocked {
      ch37.player.x = slider.x + 20f
      ch37.player.y = slider.y - ch37.player.collisionHeight
      ch37.player.isGrounded = true
      ch37.player.vy = 0f
    }
    val playerStartX = ch37.player.x
    repeat(slider.lockedTelegraphDurationFrames + 8) { ch37.update() }
    assertTrue("SLIDING_FLOOR_SHIFT must move toward targetX", slider.x > initialSliderX)
    assertTrue("Grounded player must be carried smoothly on SLIDING_FLOOR_SHIFT", ch37.player.x > playerStartX)

    // TEMP_VANISH_REFORM_FLOOR vanishes and then reforms solid
    val tempFloor = ch37.trollTraps.first { it.type == TrollTrapType.TEMP_VANISH_REFORM_FLOOR }
    ch37.readWorldStateLocked {
      ch37.player.x = tempFloor.x + 20f
      ch37.player.y = tempFloor.y - ch37.player.collisionHeight
      ch37.player.isGrounded = true
      ch37.player.vy = 0f
    }
    repeat(tempFloor.lockedTelegraphDurationFrames + 2) { ch37.update() }
    assertFalse("TEMP_VANISH_REFORM_FLOOR must drop solidity after trigger", tempFloor.isSolid)
    // Move player away safely and let timer reform floor
    ch37.readWorldStateLocked {
      ch37.player.x = 2f * t
      ch37.player.y = 11f * t
    }
    repeat(tempFloor.lockedCycleFrames + 4) { ch37.update() }
    assertTrue("TEMP_VANISH_REFORM_FLOOR must reform solid automatically", tempFloor.isSolid)

    // SPLITTING_TWIN_FLOOR opens center gap on step
    val splitter = ch37.trollTraps.first { it.type == TrollTrapType.SPLITTING_TWIN_FLOOR }
    ch37.readWorldStateLocked {
      ch37.player.x = splitter.x + 24f
      ch37.player.y = splitter.y - ch37.player.collisionHeight
      ch37.player.isGrounded = true
      ch37.player.vy = 0f
    }
    repeat(splitter.lockedTelegraphDurationFrames + 8) { ch37.update() }
    assertTrue("SPLITTING_TWIN_FLOOR must widen split auxProgress", splitter.auxProgress > 0.1f)

    // 2. REVERSING_HAZARD reverses direction mid-lane (ch_37)
    ch37.resetCurrentChallenge()
    val saw = ch37.trollTraps.first { it.type == TrollTrapType.REVERSING_HAZARD }
    ch37.readWorldStateLocked {
      ch37.player.x = saw.triggerMinX + 10f
      ch37.player.y = 9.8f * t - ch37.player.collisionHeight // safely on upper ledge above saw
      ch37.player.isGrounded = true
    }
    var sawFlippedBack = false
    repeat(140) {
      ch37.update()
      if (saw.gustSign < 0f) sawFlippedBack = true
    }
    assertTrue("REVERSING_HAZARD must reverse direction unexpectedly mid-lane", sawFlippedBack)

    // 3. REVERSING_PLATFORM switches travel direction on step (ch_37)
    ch37.resetCurrentChallenge()
    val revPlat = ch37.trollTraps.first { it.type == TrollTrapType.REVERSING_PLATFORM }
    ch37.readWorldStateLocked {
      ch37.player.x = revPlat.x + 20f
      ch37.player.y = revPlat.y - ch37.player.collisionHeight
      ch37.player.isGrounded = true
      ch37.player.vy = 0f
    }
    val initialSign = revPlat.gustSign
    repeat(revPlat.lockedTelegraphDurationFrames + 3) { ch37.update() }
    assertEquals("REVERSING_PLATFORM must flip direction on step", -initialSign, revPlat.gustSign, 0.001f)

    // 4. OPENING_AMBUSH_WALL opens & chains hazard in ch_40, and resets cleanly
    val ch40 = engineForChallenge("ch_40")
    val openWall = ch40.trollTraps.first { it.type == TrollTrapType.OPENING_AMBUSH_WALL }
    val chainedSaw = ch40.trollTraps.first { it.id == openWall.linkedTrapId }
    val wallStartY = openWall.y
    ch40.readWorldStateLocked {
      ch40.player.x = openWall.triggerMinX + 8f
      ch40.player.y = 13f * t - ch40.player.collisionHeight
      ch40.player.isGrounded = true
    }
    repeat(openWall.lockedTelegraphDurationFrames + 12) { ch40.update() }
    assertTrue("OPENING_AMBUSH_WALL must slide open", openWall.y < wallStartY)
    assertNotEquals("Opening wall must unleash chained hazard", TrollTrapState.ARMED, chainedSaw.state)

    ch40.resetCurrentChallenge()
    assertEquals(wallStartY, openWall.y, 0.1f)
    assertEquals(TrollTrapState.ARMED, openWall.state)
    assertEquals(TrollTrapState.ARMED, chainedSaw.state)
  }

  @Test
  fun test05_mastery600StarsDailyPoolAcross200AndZeroWarningSurpriseContracts() {
    assertEquals(200, MasteryCatalog.specs.size)
    assertEquals(600, MasteryCatalog.totalPossibleStars)

    // Ensure NO_COINS is never paired with coin-locked gates
    for (spec in MasteryCatalog.specs) {
      if (spec.objectives[2].kind == MasteryObjectiveKind.NO_COINS) {
        val lvl = ChallengeCatalog.findById(spec.challengeId)!!.buildLevel()
        val hasCoinGate = lvl.trollTraps.any {
          it.type == TrollTrapType.LOCKED_RUNE_GATE && it.triggerType == TrapTriggerType.ON_COIN_COLLECTED
        }
        assertFalse("${spec.challengeId} cannot have NO_COINS if a coin unlocks LOCKED_RUNE_GATE", hasCoinGate)
      }
    }

    // Daily Challenge generator samples cleanly across 200-challenge range
    val sampledOrderIndices = (1..31).map { day ->
      OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate(2026, 12, day)).challengeOrderIndex
    }.toSet()
    assertTrue("Daily challenge pool must include expansion challenges (> 36)", sampledOrderIndices.any { it > 36 })

    // Designated SURPRISE traps in ch_161..ch_200 keep 0 telegraph & null quip on lethal non-FakeExit
    for (entry in ChallengeCatalog.entries.filter { it.orderIndex >= 161 }) {
      val traps = entry.buildLevel().trollTraps
      for (tier in AdaptiveTier.entries) {
        val tuned = traps.map { it.copy() }
        TrapFairnessPolicy.applyTierTuningInPlace(tuned, tier)
        for (trap in tuned) {
          if (trap.behaviorCategory == TrapBehaviorCategory.SURPRISE) {
            assertEquals(0, trap.lockedTelegraphDurationFrames)
            if (trap.killsPlayer && trap.type != TrollTrapType.FAKE_EXIT_DOOR) {
              assertNull(trap.quipPopup)
            }
          }
        }
      }
    }
  }
}
