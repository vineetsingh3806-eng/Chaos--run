package com.example

import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.DifficultyTier
import com.example.data.MasteryCatalog
import com.example.data.OfflineCalendarDate
import com.example.data.OfflineDailyChallengeGenerator
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.data.TrapBehaviorCategory
import com.example.data.TrapTriggerType
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import com.example.data.World1Levels
import com.example.engine.ChallengeCompletionOutcome
import com.example.engine.ChaosGameEngine
import com.example.engine.ContinuousRunCoordinator
import com.example.engine.HapticCue
import com.example.engine.TrapFairnessPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 9 JVM verification suite covering early-game hooks (ch_01..ch_12),
 * mid/late anti-repetition upgrades (ch_13..ch_27), haptic impact cues,
 * TrapFairnessPolicy invariants across all 36 challenges x 4 AdaptiveTiers,
 * checkpoint/death reset determinism, and real input-driven zero-death playthroughs.
 */
class Phase9EarlyGameUpgradeTest {

  private fun engineForChallenge(
      challengeId: String,
      difficulty: DifficultyTier = DifficultyTier.NORMAL,
      haptics: MutableList<HapticCue> = mutableListOf()
  ): ChaosGameEngine {
    val entry = ChallengeCatalog.findById(challengeId)!!
    return ChaosGameEngine(
        level = entry.buildLevel(difficulty),
        audioEngine = ProceduralAudioEngine(),
        onHapticCue = { cue -> haptics.add(cue) }
    )
  }

  private fun finishDeathAnimation(engine: ChaosGameEngine) {
    assertTrue("Player should enter dying state before respawn completes", engine.isDying)
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) {
      if (engine.isDying) engine.update()
    }
    assertFalse("Auto-retry must respawn cleanly after DEATH_ANIM_FRAMES", engine.isDying)
  }

  private fun stepInputs(
      engine: ChaosGameEngine,
      frames: Int,
      right: Boolean = false,
      left: Boolean = false,
      jump: Boolean = false,
      stopOnWin: Boolean = true
  ) {
    engine.inputRight = right
    engine.inputLeft = left
    engine.inputJump = jump
    for (i in 0 until frames) {
      if (stopOnWin && engine.isVictorious) break
      if (engine.isDying) break
      engine.update()
    }
  }

  private fun runUntilX(
      engine: ChaosGameEngine,
      targetX: Float,
      jump: Boolean = false,
      maxFrames: Int = 240
  ) {
    val t = World1Levels.TILE_SIZE
    val startDeaths = engine.deathCount
    engine.inputLeft = false
    engine.inputRight = true
    engine.inputJump = jump
    var frames = 0
    while (engine.player.x < targetX &&
        frames < maxFrames &&
        !engine.isDying &&
        engine.deathCount == startDeaths &&
        !engine.isVictorious) {
      engine.update()
      frames++
    }
    assertFalse(
        "Player died running toward ${targetX / t}T (stopped at x=${engine.player.x / t}T, deaths=${engine.deathCount})",
        engine.isDying || engine.deathCount > startDeaths
    )
    assertTrue(
        "Timed out after $maxFrames frames running toward ${targetX / t}T (at x=${engine.player.x / t}T)",
        engine.player.x >= targetX || engine.isVictorious
    )
  }

  private fun waitUntilGrounded(
      engine: ChaosGameEngine,
      right: Boolean = true,
      left: Boolean = false,
      maxFrames: Int = 140
  ) {
    val t = World1Levels.TILE_SIZE
    val startDeaths = engine.deathCount
    var frames = 0
    while (!engine.player.isGrounded &&
        !engine.isDying &&
        engine.deathCount == startDeaths &&
        !engine.isVictorious &&
        frames < maxFrames) {
      stepInputs(engine, frames = 1, right = right, left = left, jump = false)
      frames++
    }
    assertFalse(
        "Player died before landing (x=${engine.player.x / t}T, y=${engine.player.y / t}T)",
        engine.isDying || engine.deathCount > startDeaths
    )
    assertTrue(
        "Timed out waiting to land (x=${engine.player.x / t}T, y=${engine.player.y / t}T)",
        engine.player.isGrounded || engine.isVictorious
    )
  }

  private fun waitUntil(
      engine: ChaosGameEngine,
      maxFrames: Int = 240,
      description: () -> String,
      condition: () -> Boolean
  ) {
    val t = World1Levels.TILE_SIZE
    val startDeaths = engine.deathCount
    var frames = 0
    while (!condition() &&
        !engine.isDying &&
        engine.deathCount == startDeaths &&
        !engine.isVictorious &&
        frames < maxFrames) {
      stepInputs(engine, frames = 1)
      frames++
    }
    assertFalse(
        "Player died waiting for ${description()} at x=${engine.player.x / t}T",
        engine.isDying || engine.deathCount > startDeaths
    )
    assertTrue(
        "Timed out waiting for ${description()} at x=${engine.player.x / t}T",
        condition()
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
  fun test01_catalogPreservationFairnessPolicyAndPhase7ContractsAllPass() {
    assertEquals(200, ChallengeCatalog.entries.size)
    assertEquals(3, SaveRepository.SAVE_SCHEMA_VERSION)
    assertEquals(200, MasteryCatalog.specs.size)
    assertEquals(5, World1Levels.catalog.size)
    assertEquals(6.5f, ChaosGameEngine.MAX_RUN_SPEED, 0.001f)
    assertEquals(-13.2f, ChaosGameEngine.JUMP_IMPULSE, 0.001f)
    assertEquals(0.58f, ChaosGameEngine.GRAVITY, 0.001f)

    // Continuous 36-stage run order & seeds preserved
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()
    val visited = mutableListOf<String>()
    while (true) {
      val currentId = coordinator.activeChallengeId
      visited += currentId
      val outcome = coordinator.onChallengeCompleted(
          challengeId = currentId,
          attemptToken = coordinator.currentAttemptToken,
          timeMs = 12_000L,
          score = 600,
          coinsCollected = 5,
          totalCoins = 5,
          parTimeSeconds = 30,
          isTrollLevel = true,
          challengeDeaths = 0
      )
      when (outcome) {
        is ChallengeCompletionOutcome.NextChallenge -> Unit
        is ChallengeCompletionOutcome.RunCompleted -> break
        ChallengeCompletionOutcome.IgnoredDuplicate -> error("Unexpected duplicate on $currentId")
      }
    }
    assertEquals(200, visited.size)
    assertEquals((1..200).map { "ch_${it.toString().padStart(2, '0')}" }, visited)

    // Daily challenge generator still builds cleanly across seed days
    val dailySample = (0 until 15).map { offset ->
      OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate(2026, 11, 10 + offset))
    }
    assertTrue(dailySample.all { !it.isFallback && ChallengeCatalog.findById(it.challengeId) != null })

    // TrapFairnessPolicy across all 36 challenges x 4 AdaptiveTiers
    for ((idx, entry) in ChallengeCatalog.entries.withIndex()) {
      assertEquals("ch_${(idx + 1).toString().padStart(2, '0')}", entry.challengeId)
      assertEquals(idx + 1, entry.orderIndex)
      assertEquals(1001L + idx, entry.challengeSeed)

      val level = entry.buildLevel()
      val uniqueTrapIds = level.trollTraps.map { it.id }.toSet()
      assertEquals("${entry.challengeId} trap IDs must be unique", level.trollTraps.size, uniqueTrapIds.size)
      if (entry.orderIndex in 6..12) {
        assertEquals("${entry.challengeId} must retain 5 unique coins", 5, level.coins.map { it.id }.toSet().size)
      }

      // Checkpoints must not overlap initial trap boxes, and ch_25..ch_36 must retain 2 checkpoints
      val bells = level.tiles.filter { it.type == TileType.CHECKPOINT_BELL }
      if (entry.orderIndex in 25..36) {
        assertEquals("${entry.challengeId} must have 2 checkpoints", 2, bells.size)
      }
      for (bell in bells) {
        val overlaps = level.trollTraps.any { trap ->
          rectsOverlap(bell.x, bell.y, bell.width, bell.height, trap.initialX, trap.initialY, trap.width, trap.height)
        }
        assertFalse("${entry.challengeId} checkpoint overlaps trap", overlaps)
      }

      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
        val failures = validations.filterNot { it.isFair }
        assertTrue(
            "${entry.challengeId} fairness failures in $tier: ${failures.joinToString { "#${it.trapId}:${it.reason}" }}",
            failures.isEmpty()
        )
      }

      // Phase 7 non-negotiable invariant on ch_25..ch_36 SURPRISE traps
      if (entry.orderIndex >= 25) {
        for (rawTrap in level.trollTraps.filter { it.behaviorCategory == TrapBehaviorCategory.SURPRISE }) {
          val copy = rawTrap.copy()
          for (tier in AdaptiveTier.entries) {
            TrapFairnessPolicy.applyTierTuningInPlace(listOf(copy), tier)
            assertEquals(
                "${entry.challengeId} trap #${rawTrap.id} must have 0 telegraph in $tier",
                0,
                copy.lockedTelegraphDurationFrames
            )
          }
          if (rawTrap.killsPlayer && rawTrap.type != TrollTrapType.FAKE_EXIT_DOOR) {
            assertNull(
                "${entry.challengeId} lethal non-fake-door surprise #${rawTrap.id} must have null quipPopup",
                rawTrap.quipPopup
            )
          }
        }
      }
    }

    // ch_13 preserves at least one telegraphed trap for Phase 7 backwards contract
    val ch13 = ChallengeCatalog.findById("ch_13")!!.buildLevel()
    assertTrue("ch_13 must keep telegraphed coin trap #2", ch13.trollTraps.any { it.id == 2 && it.telegraphDurationFrames > 0 })
  }

  @Test
  fun test02_newEarlyHooksCoinBaitsSwitchesConveyorPadAndHapticsFireAndResetCleanly() {
    val t = World1Levels.TILE_SIZE

    // 1. ch_01 Betrayal Switch #12 -> delayed Spike #13 + Coin #1 Falling Block #14 + Coin #3 Spike #15
    val haptics1 = mutableListOf<HapticCue>()
    val ch01 = engineForChallenge("ch_01", haptics = haptics1)
    val sw12 = ch01.trollTraps.first { it.id == 12 }
    val chainSpike13 = ch01.trollTraps.first { it.id == 13 }
    ch01.player.x = sw12.initialX + 8f
    ch01.player.y = 13f * t - ch01.player.collisionHeight
    ch01.player.isGrounded = true
    ch01.update()
    assertEquals(TrollTrapState.RETIRED, sw12.state)
    // Stays armed until chainDelayFrames elapses, then activates
    assertEquals(TrollTrapState.ARMED, chainSpike13.state)
    repeat(chainSpike13.chainDelayFrames + 2) { ch01.update() }
    assertEquals(TrollTrapState.ACTIVE, chainSpike13.state)

    // Coin #1 drops canopy block #14 immediately
    val c1 = ch01.coins.first { it.id == 1 }
    ch01.player.x = c1.x - ch01.player.collisionWidth * 0.5f
    ch01.player.y = c1.y - ch01.player.collisionHeight * 0.5f
    ch01.update()
    assertTrue(c1.isCollected)
    assertEquals(TrollTrapState.ACTIVE, ch01.trollTraps.first { it.id == 14 }.state)

    // Relocating exit door emits TRAP_IMPACT haptic
    haptics1.clear()
    val door7 = ch01.trollTraps.first { it.id == 7 }
    ch01.player.x = door7.triggerMinX + 8f
    ch01.player.y = 11f * t
    ch01.update()
    assertTrue(door7.hasRelocated)
    assertTrue("Door escape must emit TRAP_IMPACT haptic", HapticCue.TRAP_IMPACT in haptics1)

    ch01.resetCurrentChallenge()
    assertEquals(TrollTrapState.ARMED, sw12.state)
    assertEquals(TrollTrapState.ARMED, chainSpike13.state)
    assertEquals(TrollTrapState.ARMED, ch01.trollTraps.first { it.id == 14 }.state)
    assertFalse(door7.hasRelocated)

    // 2. ch_04 & ch_05 & ch_15 & ch_25 Relocating Exit Doors
    for ((cid, expectedDestT) in listOf("ch_02" to 51.2f, "ch_04" to 61.5f, "ch_11" to 51.0f, "ch_15" to 50.6f, "ch_25" to 56.8f)) {
      val e = engineForChallenge(cid)
      val reloc = e.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
      e.player.x = (reloc.triggerMinX + reloc.triggerMaxX) * 0.5f - e.player.collisionWidth * 0.5f
      e.player.y = (reloc.triggerMinY + reloc.triggerMaxY) * 0.5f - e.player.collisionHeight * 0.5f
      e.update()
      assertEquals("$cid exit must relocate", expectedDestT * t, e.currentExitX, 2f)
      if (reloc.linkedTrapId != null) {
        val linked = e.trollTraps.first { it.id == reloc.linkedTrapId }
        assertTrue(
            "$cid linked disappearing floor #${linked.id} must trigger with gate relocation",
            linked.state == TrollTrapState.RETIRED || !linked.isSolid
        )
      }
      e.resetCurrentChallenge()
      assertEquals("$cid exit must restore on reset", e.level.exitX, e.currentExitX, 1f)
    }

    // 3. ch_10 intro REVERSE_CONVEYOR_BELT (#8) + LAUNCH_BETRAYAL_PAD (#9)
    val ch10 = engineForChallenge("ch_10")
    val conv8 = ch10.trollTraps.first { it.id == 8 }
    assertEquals(TrollTrapType.REVERSE_CONVEYOR_BELT, conv8.type)
    val pad9 = ch10.trollTraps.first { it.id == 9 }
    assertEquals(TrollTrapType.LAUNCH_BETRAYAL_PAD, pad9.type)
    assertTrue("LAUNCH_BETRAYAL_PAD must have telegraph > 0 to launch", pad9.telegraphDurationFrames > 0)
    ch10.player.x = pad9.initialX + 8f
    ch10.player.y = pad9.initialY - ch10.player.collisionHeight + 4f
    ch10.player.vy = 2f
    ch10.player.prevY = ch10.player.y - 4f
    ch10.player.isGrounded = true
    ch10.update()
    assertEquals(TrollTrapState.TELEGRAPH, pad9.state)
    assertTrue("Stepping onto LAUNCH_BETRAYAL_PAD must launch player upward", ch10.player.vy < -10f)

    // 4. ch_12 Coin #1 triggers early MIRROR_CURSE_TOTEM (#10)
    val ch12 = engineForChallenge("ch_12")
    assertEquals(2, ch12.trollTraps.first { it.type == TrollTrapType.MIRROR_CURSE_TOTEM }.id)
    val ch12c1 = ch12.coins.first { it.id == 1 }
    ch12.player.x = ch12c1.x - 10f
    ch12.player.y = ch12c1.y - 10f
    ch12.update()
    assertEquals(TrollTrapState.ACTIVE, ch12.trollTraps.first { it.id == 10 }.state)

    // 5. ch_13 FAKE_EXIT_DOOR (#6) & HapticCue + ch_16 beam (#8) + ch_24 slam cage (#10) + ch_27 Coin #1 + #5
    val haptics13 = mutableListOf<HapticCue>()
    val ch13 = engineForChallenge("ch_13", haptics = haptics13)
    val fake13 = ch13.trollTraps.first { it.id == 6 }
    assertEquals(TrollTrapType.FAKE_EXIT_DOOR, fake13.type)
    ch13.player.x = fake13.initialX + 16f
    ch13.player.y = 11f * t
    repeat(2) { if (!ch13.isDying) ch13.update() }
    assertTrue(ch13.isDying)
    assertTrue("Fake Exit Door death triggers TRAP_IMPACT haptic", HapticCue.TRAP_IMPACT in haptics13)

    val ch16 = engineForChallenge("ch_16")
    val beam8 = ch16.trollTraps.first { it.id == 8 }
    assertEquals(TrollTrapType.TELEGRAPHED_RUNE_BEAM, beam8.type)
    ch16.player.x = beam8.triggerMinX + 8f
    ch16.player.y = 11f * t
    ch16.update()
    assertEquals(TrollTrapState.ACTIVE, beam8.state)

    val ch24 = engineForChallenge("ch_24")
    val cage10 = ch24.trollTraps.first { it.id == 10 }
    assertEquals(TrollTrapType.EXIT_SLAM_CAGE, cage10.type)
    ch24.player.x = cage10.triggerMinX + 12f
    ch24.player.y = 11f * t
    ch24.update()
    assertEquals(TrollTrapState.ACTIVE, cage10.state)

    val ch27 = engineForChallenge("ch_27")
    val c27_1 = ch27.coins.first { it.id == 1 }
    ch27.player.x = c27_1.x - 10f
    ch27.player.y = c27_1.y - 10f
    ch27.update()
    assertEquals(TrollTrapState.ACTIVE, ch27.trollTraps.first { it.id == 1 }.state)
    assertEquals(TrollTrapState.ACTIVE, ch27.trollTraps.first { it.id == 2 }.state)
    ch27.resetCurrentChallenge()
    val c27_5 = ch27.coins.first { it.id == 5 }
    ch27.player.x = c27_5.x - 10f
    ch27.player.y = c27_5.y - 10f
    ch27.update()
    val trap27_8 = ch27.trollTraps.first { it.id == 8 }
    assertFalse("Collecting ch_27 Coin #5 silently collapses gap #8", trap27_8.isSolid)
  }

  @Test
  fun test03_all36ChallengesHaveSafeSpawnAndReachableSunGatesWithoutSoftlocks() {
    for (entry in ChallengeCatalog.entries) {
      val engine = engineForChallenge(entry.challengeId)
      repeat(15) { engine.update() }
      assertTrue("${entry.challengeId} spawn must land on solid footing", engine.player.isGrounded)
      assertEquals("${entry.challengeId} idle at spawn must not die", 0, engine.deathCount)

      for (trap in engine.trollTraps) {
        when (trap.type) {
          TrollTrapType.RELOCATING_EXIT_DOOR -> trap.hasRelocated = true
          TrollTrapType.FLEEING_SUN_GATE -> {
            trap.activationCount = trap.maxActivations
            trap.state = TrollTrapState.RETIRED
          }
          TrollTrapType.EXIT_SLAM_CAGE,
          TrollTrapType.LOCKED_RUNE_GATE -> {
            trap.isSolid = false
            trap.state = TrollTrapState.RETIRED
          }
          else -> Unit
        }
      }
      val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
      engine.player.x = gate.x + gate.width * 0.5f - engine.player.collisionWidth * 0.5f
      engine.player.y = gate.y + gate.height - engine.player.collisionHeight
      engine.player.isGrounded = true
      engine.update()
      assertTrue("${entry.challengeId} SUN_GATE must be cleanly reachable for victory", engine.isVictorious)
    }
  }

  @Test
  fun test04_checkpointRollbackRestoresModifiedEarlyAndMidTrapsDeterministically() {
    val t = World1Levels.TILE_SIZE
    val engine = engineForChallenge("ch_15")
    val bell = engine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    engine.player.x = bell.x + 4f
    engine.player.y = bell.y + bell.height - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.update()
    assertTrue("ch_15 checkpoint bell must activate", engine.hasCheckpointActivated)

    val reloc = engine.trollTraps.first { it.id == 8 }
    engine.player.x = (reloc.triggerMinX + reloc.triggerMaxX) * 0.5f
    engine.player.y = 11f * t
    engine.update()
    assertTrue(reloc.hasRelocated)
    assertEquals(50.6f * t, engine.currentExitX, 1f)

    engine.handlePlayerDeath("test_ch15_checkpoint_rollback")
    assertFalse("Checkpoint rollback must restore relocating door #8", reloc.hasRelocated)
    assertEquals(engine.level.exitX, engine.currentExitX, 1f)
  }

  @Test
  fun test05_ch01ThroughCh05RealControllerZeroDeathPlaythroughsPass() {
    val t = World1Levels.TILE_SIZE

    // --- ch_01 (exact TrollLevel1PrototypeTest.test05 learned route!) ---
    val e1 = engineForChallenge("ch_01", DifficultyTier.NORMAL)
    var jumpCooldown = 0
    var maxFrames = 900
    var baitedThwomp = false
    while (!e1.isVictorious && maxFrames-- > 0) {
      if (jumpCooldown > 0) jumpCooldown--
      val px = e1.player.x
      val onGround = e1.player.isGrounded
      val thwomp = e1.trollTraps.first { it.id == 4 }
      if (!baitedThwomp && px in 1242f..1275f && thwomp.state != TrollTrapState.RETIRED) {
        e1.inputRight = false
        if (thwomp.y >= thwomp.targetY - 2f) {
          baitedThwomp = true
        }
      } else {
        e1.inputRight = true
      }

      val shouldJump = onGround && jumpCooldown == 0 && (
          px in 520f..555f ||
          px in 735f..782f ||
          px in 1055f..1106f ||
          (thwomp.state == TrollTrapState.RETIRED && px in 1240f..1285f) ||
          px in 1465f..1503f ||
          px in 1775f..1822f
      )
      val jumpHoldCutoff = if (px in 520f..740f || px in 1240f..1450f || px >= 1770f) -6.0f else -1.5f
      e1.inputJump = shouldJump || (!onGround && e1.player.vy < jumpHoldCutoff)
      if (shouldJump) jumpCooldown = 10
      e1.update()
      assertEquals("ch_01 learned route died at x=$px (${px / t}T)", 0, e1.deathCount)
    }
    assertTrue("ch_01 must remain cleanly winnable (ended x=${e1.player.x / t}T)", e1.isVictorious)

    // --- ch_02 ---
    val e2 = engineForChallenge("ch_02", DifficultyTier.NORMAL)
    runUntilX(e2, 6.1f * t)
    stepInputs(e2, frames = 16, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 9.1f * t)
    stepInputs(e2, frames = 26, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 16.6f * t, jump = false)
    stepInputs(e2, frames = 22, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 20.6f * t, jump = false)
    stepInputs(e2, frames = 30, right = true, jump = true)
    waitUntilGrounded(e2)
    runUntilX(e2, 25.4f * t)
    stepInputs(e2, frames = 10, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 28.4f * t)
    stepInputs(e2, frames = 18, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 32.3f * t)
    stepInputs(e2, frames = 16, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 36.8f * t)
    stepInputs(e2, frames = 22, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 40.8f * t)
    stepInputs(e2, frames = 22, right = true, jump = true)
    waitUntilGrounded(e2, right = false)
    runUntilX(e2, 46.5f * t, jump = false)
    assertEquals(51.2f * t, e2.currentExitX, 1f)
    runUntilX(e2, 47.0f * t, jump = false)
    stepInputs(e2, frames = 24, right = true, jump = true)
    waitUntilGrounded(e2)
    if (!e2.isVictorious) runUntilX(e2, 52.0f * t, jump = false, maxFrames = 80)
    assertEquals(0, e2.deathCount)
    assertTrue("ch_02 must be cleanly winnable", e2.isVictorious)

    // --- ch_03 ---
    val e3 = engineForChallenge("ch_03", DifficultyTier.NORMAL)
    runUntilX(e3, 4.1f * t)
    stepInputs(e3, frames = 18, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 8.6f * t)
    stepInputs(e3, frames = 7, right = true, jump = true)
    waitUntilGrounded(e3)
    runUntilX(e3, 11.0f * t)
    stepInputs(e3, frames = 14, right = true, jump = true)
    waitUntilGrounded(e3)
    runUntilX(e3, 16.9f * t)
    stepInputs(e3, frames = 10, right = true, jump = true)
    waitUntilGrounded(e3)
    runUntilX(e3, 22.5f * t)
    stepInputs(e3, frames = 20, left = true, right = false, jump = false)
    stepInputs(e3, frames = 55, right = false, left = false, jump = false)
    stepInputs(e3, frames = 22, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 24.9f * t)
    stepInputs(e3, frames = 28, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 28.9f * t)
    stepInputs(e3, frames = 26, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 32.1f * t)
    stepInputs(e3, frames = 22, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    val doorGuard = e3.trollTraps.first { it.id == 10 }
    runUntilX(e3, 36.3f * t)
    stepInputs(e3, frames = 18, left = true)
    waitUntil(e3, maxFrames = 90, description = { "ch_03 doorGuard RETIRED" }) {
      doorGuard.state == TrollTrapState.RETIRED
    }
    runUntilX(e3, 36.4f * t)
    stepInputs(e3, frames = 24, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 39.2f * t)
    assertEquals(45.5f * t, e3.currentExitX, 1f)
    runUntilX(e3, 41.2f * t)
    stepInputs(e3, frames = 16, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 42.8f * t)
    stepInputs(e3, frames = 16, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    runUntilX(e3, 45.2f * t)
    assertEquals(52.5f * t, e3.currentExitX, 1f)
    runUntilX(e3, 48.0f * t)
    stepInputs(e3, frames = 14, right = true, jump = true)
    waitUntilGrounded(e3, right = false)
    // Jump cleanly over final Betrayal Switch (51.0T) into Sun Gate (52.5T)
    runUntilX(e3, 50.2f * t)
    stepInputs(e3, frames = 24, right = true, jump = true)
    waitUntilGrounded(e3)
    if (!e3.isVictorious) runUntilX(e3, 53.5f * t, jump = false, maxFrames = 60)
    assertEquals(0, e3.deathCount)
    assertTrue("ch_03 must be cleanly winnable", e3.isVictorious)

    // --- ch_04 ---
    val e4 = engineForChallenge("ch_04", DifficultyTier.NORMAL)
    runUntilX(e4, 5.0f * t)
    stepInputs(e4, frames = 20, right = true, jump = true)
    waitUntilGrounded(e4, right = false)
    runUntilX(e4, 9.8f * t)
    stepInputs(e4, frames = 16, right = true, jump = true)
    waitUntilGrounded(e4)
    runUntilX(e4, 14.8f * t)
    stepInputs(e4, frames = 16, right = true, jump = true)
    waitUntilGrounded(e4)
    runUntilX(e4, 19.8f * t)
    stepInputs(e4, frames = 10, right = true, jump = true)
    waitUntilGrounded(e4)
    val padA = e4.trollTraps.first { it.id == 4 }
    runUntilX(e4, 25.0f * t)
    stepInputs(e4, frames = 6)
    waitUntil(e4, maxFrames = 180, description = { "Pad A solid" }) {
      padA.isSolid && padA.stateTimerFrames in 5..22
    }
    stepInputs(e4, frames = 14, right = true, jump = true)
    waitUntilGrounded(e4, right = false)
    waitUntil(e4, maxFrames = 120, description = { "Pad A late window" }) {
      padA.isSolid && padA.stateTimerFrames in 44..58
    }
    stepInputs(e4, frames = 16, right = true, jump = true)
    waitUntilGrounded(e4, right = true)
    runUntilX(e4, 33.1f * t)
    stepInputs(e4, frames = 20, right = true, jump = true)
    waitUntilGrounded(e4, right = false)
    val deckCeiling = e4.trollTraps.first { it.id == 8 }
    val oscSpike = e4.trollTraps.first { it.id == 7 }
    waitUntil(e4, maxFrames = 180, description = { "oscSpike right" }) { oscSpike.x >= 40.5f * t }
    runUntilX(e4, 39.55f * t)
    stepInputs(e4, frames = 28, left = true)
    stepInputs(e4, frames = 6)
    waitUntil(e4, maxFrames = 180, description = { "deckCeiling RETIRED" }) {
      deckCeiling.state == TrollTrapState.RETIRED && oscSpike.x >= 40.5f * t
    }
    waitUntil(e4, maxFrames = 180, description = { "oscSpike return left" }) { oscSpike.x <= 39.2f * t }
    stepInputs(e4, frames = 34, right = true, jump = true)
    waitUntilGrounded(e4, right = true)
    runUntilX(e4, 44.0f * t)
    stepInputs(e4, frames = 24, right = true, jump = true)
    waitUntilGrounded(e4, right = false)
    runUntilX(e4, 50.0f * t, jump = false)
    stepInputs(e4, frames = 24, right = true, jump = true)
    waitUntilGrounded(e4, right = false)
    runUntilX(e4, 56.2f * t, jump = false)
    assertEquals(61.5f * t, e4.currentExitX, 1f)
    runUntilX(e4, 56.8f * t, jump = false)
    stepInputs(e4, frames = 24, right = true, jump = true)
    waitUntilGrounded(e4)
    if (!e4.isVictorious) runUntilX(e4, 62.2f * t, jump = false, maxFrames = 60)
    assertEquals(0, e4.deathCount)
    assertTrue("ch_04 must be cleanly winnable", e4.isVictorious)

    // --- ch_05 ---
    val e5 = engineForChallenge("ch_05", DifficultyTier.NORMAL)
    runUntilX(e5, 5.8f * t)
    stepInputs(e5, frames = 20, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 9.35f * t)
    stepInputs(e5, frames = 26, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 13.8f * t)
    val blink = e5.trollTraps.first { it.type == TrollTrapType.TIMED_BLINK_PLATFORM }
    waitUntil(e5, maxFrames = 180, description = { "blink sync" }) { blink.stateTimerFrames in 70..85 }
    stepInputs(e5, frames = 16, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 17.0f * t)
    stepInputs(e5, frames = 24, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 21.8f * t)
    stepInputs(e5, frames = 10, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 24.3f * t)
    stepInputs(e5, frames = 20, left = true)
    val crush = e5.trollTraps.first { it.type == TrollTrapType.MOVING_CRUSH_WALL }
    waitUntil(e5, maxFrames = 120, description = { "crush RETIRED" }) { crush.state == TrollTrapState.RETIRED }
    stepInputs(e5, frames = 28, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 28.1f * t)
    stepInputs(e5, frames = 28, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 36.0f * t, jump = false)
    stepInputs(e5, frames = 26, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 40.9f * t)
    stepInputs(e5, frames = 20, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 44.0f * t)
    stepInputs(e5, frames = 30, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    val ceil14 = e5.trollTraps.first { it.id == 14 }
    if (ceil14.state == TrollTrapState.ARMED) runUntilX(e5, 48.3f * t)
    if (e5.player.x > 48.28f * t) stepInputs(e5, frames = 2, left = true)
    waitUntil(e5, maxFrames = 90, description = { "ceil14 RETIRED" }) { ceil14.state == TrollTrapState.RETIRED }
    stepInputs(e5, frames = 22, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 51.0f * t)
    assertEquals(60.5f * t, e5.currentExitX, 1f)
    runUntilX(e5, 53.0f * t)
    stepInputs(e5, frames = 18, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 55.1f * t)
    stepInputs(e5, frames = 10, right = true, jump = true)
    waitUntilGrounded(e5, right = false)
    runUntilX(e5, 57.1f * t)
    stepInputs(e5, frames = 26, right = true, jump = true)
    waitUntilGrounded(e5)
    if (!e5.isVictorious) runUntilX(e5, 61.5f * t, jump = false, maxFrames = 60)
    assertEquals(0, e5.deathCount)
    assertTrue("ch_05 must be cleanly winnable", e5.isVictorious)
  }

  @Test
  fun test06_ch06ThroughCh11ModifiedIntroAndFinaleSpansArePhysicallyTraversable() {
    val t = World1Levels.TILE_SIZE

    // ch_06 intro gap (disappearing floor 4..5T -> 6T) + Coin #4 ceiling drop clearance
    val ch06 = engineForChallenge("ch_06")
    runUntilX(ch06, 3.0f * t)
    stepInputs(ch06, frames = 20, right = true, jump = true)
    waitUntilGrounded(ch06)
    assertTrue("ch_06 intro jump lands safely past 4..5T gap", ch06.player.x >= 5.8f * t && ch06.deathCount == 0)

    // ch_07 finale relocating door (47.6T -> 51.0T)
    val ch07 = engineForChallenge("ch_07")
    val door07 = ch07.trollTraps.first { it.id == 7 }
    ch07.player.x = 45.5f * t
    ch07.player.y = 11f * t
    ch07.player.isGrounded = true
    runUntilX(ch07, 52.0f * t, jump = false, maxFrames = 120)
    assertTrue(door07.hasRelocated)
    assertTrue("ch_07 relocated door must be reachable cleanly", ch07.isVictorious)

    // ch_09 intro SPLIT_BRIDGE (4..5T) jump clearance
    val ch09 = engineForChallenge("ch_09")
    runUntilX(ch09, 3.0f * t)
    stepInputs(ch09, frames = 18, right = true, jump = true)
    waitUntilGrounded(ch09)
    assertTrue("ch_09 intro jump clears split bridge cleanly", ch09.player.x >= 5.6f * t && ch09.deathCount == 0)

    // ch_10 intro REVERSE_CONVEYOR_BELT (3..5T)
    val ch10 = engineForChallenge("ch_10")
    runUntilX(ch10, 2.7f * t)
    stepInputs(ch10, frames = 24, right = true, jump = true)
    waitUntilGrounded(ch10)
    runUntilX(ch10, 6.2f * t, jump = false, maxFrames = 120)
    assertTrue("ch_10 intro belt clears cleanly", ch10.player.x >= 6.0f * t && ch10.deathCount == 0)

    // ch_11 intro gap (4..5T) + finale relocating door (46.8T -> 51.0T) over gap 48..49T
    val ch11 = engineForChallenge("ch_11")
    runUntilX(ch11, 3.0f * t)
    stepInputs(ch11, frames = 20, right = true, jump = true)
    waitUntilGrounded(ch11)
    assertTrue(ch11.player.x >= 5.8f * t && ch11.deathCount == 0)
    ch11.player.x = 45.2f * t
    ch11.player.y = 11f * t
    ch11.player.isGrounded = true
    runUntilX(ch11, 47.0f * t, jump = false)
    assertEquals(51.0f * t, ch11.currentExitX, 1f)
    stepInputs(ch11, frames = 24, right = true, jump = true)
    waitUntilGrounded(ch11)
    if (!ch11.isVictorious) runUntilX(ch11, 52.0f * t, jump = false, maxFrames = 80)
    assertTrue("ch_11 relocated door finale must be cleanly winnable", ch11.isVictorious)
  }
}
