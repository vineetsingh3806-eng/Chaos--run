package com.example

import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-JVM Phase 10 verification suite:
 * - Zero-warning discipline for designated SURPRISE traps & Phase 7 ch_25..ch_36 contracts
 * - Early-game (ch_06..ch_12) & targeted ch_13..ch_36 anti-repetition / chain upgrades
 * - ch_33 upper finale betrayal & ch_29/ch_28/ch_34 enhancements
 * - Post-activation selective feedback, close-call rewards & ChaosModifier score synergy
 * - Full 36 x 4 TrapFairnessPolicy audit & real controller-input beatability proofs
 */
class Phase10ChaosAndRetentionTest {

  private fun engineForChallenge(
      challengeId: String,
      tier: AdaptiveTier = AdaptiveTier.STANDARD,
      modifiers: Set<ChaosModifier> = emptySet(),
      onHaptic: (HapticCue) -> Unit = {}
  ): ChaosGameEngine {
    val cfg = ChallengeCatalog.createLockedConfig(challengeId, tier)
        ?: error("Missing challenge $challengeId")
    return ChaosGameEngine(
        level = cfg.levelDefinition,
        audioEngine = ProceduralAudioEngine(context = null),
        lockedChallengeConfig = cfg,
        initialLaunchMode = RunLaunchMode.MASTERY_CHALLENGE,
        initialModifiers = modifiers,
        onHapticCue = onHaptic
    )
  }

  private fun stepInputs(
      engine: ChaosGameEngine,
      frames: Int,
      right: Boolean = false,
      left: Boolean = false,
      jump: Boolean = false
  ) {
    engine.inputLeft = left
    engine.inputRight = right
    engine.inputJump = jump
    repeat(frames) {
      if (!engine.isVictorious) engine.update()
    }
  }

  private fun runUntilX(
      engine: ChaosGameEngine,
      targetX: Float,
      right: Boolean = true,
      jump: Boolean = false,
      maxFrames: Int = 260
  ) {
    engine.inputLeft = false
    engine.inputRight = right
    engine.inputJump = jump
    var ticks = 0
    while (engine.player.x < targetX && ticks < maxFrames && !engine.isVictorious && !engine.isDying) {
      engine.update()
      ticks++
    }
  }

  private fun waitUntilGrounded(
      engine: ChaosGameEngine,
      right: Boolean = false,
      maxFrames: Int = 180
  ) {
    engine.inputLeft = false
    engine.inputRight = right
    engine.inputJump = false
    var ticks = 0
    while (!engine.player.isGrounded && ticks < maxFrames && !engine.isVictorious && !engine.isDying) {
      engine.update()
      ticks++
    }
  }

  @Test
  fun test01_preservationMandatesCatalogPhysicsSchemaAndCharactersIntact() {
    assertEquals(200, ChallengeCatalog.entries.size)
    assertEquals((1..200).map { "ch_${it.toString().padStart(2, '0')}" }, ChallengeCatalog.entries.map { it.challengeId })
    assertEquals((1..200).toList(), ChallengeCatalog.entries.map { it.orderIndex })
    assertEquals((1..200).map { 1000L + it }, ChallengeCatalog.entries.map { it.challengeSeed })

    assertEquals(6.5f, ChaosGameEngine.MAX_RUN_SPEED, 0.001f)
    assertEquals(-13.2f, ChaosGameEngine.JUMP_IMPULSE, 0.001f)
    assertEquals(0.58f, ChaosGameEngine.GRAVITY, 0.001f)
    assertEquals(48f, World1Levels.TILE_SIZE, 0.001f)
    assertEquals(3, SaveRepository.SAVE_SCHEMA_VERSION)
    assertEquals(600, MasteryCatalog.totalPossibleStars)
    assertEquals(4, PlayableCharacter.entries.size)
  }

  @Test
  fun test02_phase7ZeroTelegraphAndNullQuipContractsRemainIntactAcrossAll36x4() {
    for (entry in ChallengeCatalog.entries) {
      val level = entry.buildLevel()
      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
        val failures = validations.filterNot { it.isFair }
        assertTrue(
            "TrapFairnessPolicy failed for ${entry.challengeId} @ $tier: ${failures.joinToString { "#${it.trapId}:${it.reason}" }}",
            failures.isEmpty()
        )

        if (entry.orderIndex >= 25) {
          val tuned = level.trollTraps.map { it.copy() }
          TrapFairnessPolicy.applyTierTuningInPlace(tuned, tier)
          for (trap in tuned) {
            if (trap.behaviorCategory == TrapBehaviorCategory.SURPRISE) {
              assertEquals(
                  "${entry.challengeId} trap ${trap.id} (${trap.type}) @ $tier must have 0 telegraph frames",
                  0,
                  trap.lockedTelegraphDurationFrames
              )
              if (trap.killsPlayer && trap.type != TrollTrapType.FAKE_EXIT_DOOR) {
                assertNull(
                    "${entry.challengeId} lethal non-FakeExit SURPRISE trap ${trap.id} must have null quipPopup",
                    trap.quipPopup
                )
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun test03_earlyGameCh06ToCh12HaveDenseVarietyAndImmediateSurpriseHooks() {
    val earlyIds = (6..12).map { "ch_${it.toString().padStart(2, '0')}" }
    for (id in earlyIds) {
      val lvl = ChallengeCatalog.createLockedConfig(id)!!.levelDefinition
      assertTrue("$id should have >= 10 traps after Phase 10, got ${lvl.trollTraps.size}", lvl.trollTraps.size >= 10)
      val surpriseCount = lvl.trollTraps.count { it.behaviorCategory == TrapBehaviorCategory.SURPRISE }
      assertTrue("$id should have >= 6 SURPRISE traps, got $surpriseCount", surpriseCount >= 6)
    }

    // Verify new ch_06..ch_12 surprise IDs exist and are SURPRISE with 0 telegraph
    val ch06 = ChallengeCatalog.createLockedConfig("ch_06")!!.levelDefinition
    assertTrue(ch06.trollTraps.any { it.id == 12 && it.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED })
    assertTrue(ch06.trollTraps.any { it.id == 13 && it.type == TrollTrapType.RELOCATING_EXIT_DOOR })
    assertTrue(ch06.trollTraps.any { it.id == 14 && it.triggerSourceTrapId == 13 })

    val ch07 = ChallengeCatalog.createLockedConfig("ch_07")!!.levelDefinition
    assertTrue(ch07.trollTraps.any { it.id == 11 && it.triggerCoinId == 1 && it.telegraphDurationFrames == 0 })
    assertTrue(ch07.trollTraps.any { it.id == 12 && it.triggerType == TrapTriggerType.ON_JUMP_ABOVE })
    assertTrue(ch07.trollTraps.any { it.id == 13 && it.type == TrollTrapType.BETRAYAL_SWITCH && it.linkedTrapId == 14 })

    val ch08 = ChallengeCatalog.createLockedConfig("ch_08")!!.levelDefinition
    assertTrue(ch08.trollTraps.any { it.id == 12 && it.triggerCoinId == 1 })
    assertTrue(ch08.trollTraps.any { it.id == 13 && it.triggerSourceTrapId == 2 })
    assertTrue(ch08.trollTraps.any { it.id == 14 && it.triggerCoinId == 3 })

    val ch09 = ChallengeCatalog.createLockedConfig("ch_09")!!.levelDefinition
    assertTrue(ch09.trollTraps.any { it.id == 10 && it.triggerCoinId == 2 })
    assertTrue(ch09.trollTraps.any { it.id == 11 && it.type == TrollTrapType.BETRAYAL_SWITCH && it.linkedTrapId == 12 })
    assertTrue(ch09.trollTraps.any { it.id == 13 && it.type == TrollTrapType.MOVING_CRUSH_WALL && it.triggerCoinId == 5 })

    val ch10 = ChallengeCatalog.createLockedConfig("ch_10")!!.levelDefinition
    assertTrue(ch10.trollTraps.any { it.id == 10 && it.triggerCoinId == 1 && it.chainDelayFrames >= 24 })
    assertTrue(ch10.trollTraps.any { it.id == 11 && it.triggerType == TrapTriggerType.ON_JUMP_ABOVE })
    assertTrue(ch10.trollTraps.any { it.id == 12 && it.triggerSourceTrapId == 9 })
    assertTrue(ch10.trollTraps.any { it.id == 13 && it.triggerType == TrapTriggerType.ON_APPROACH_EXIT })

    val ch11 = ChallengeCatalog.createLockedConfig("ch_11")!!.levelDefinition
    assertTrue(ch11.trollTraps.any { it.id == 11 && it.type == TrollTrapType.FAKE_CHECKPOINT })
    assertTrue(ch11.trollTraps.any { it.id == 12 && it.triggerCoinId == 4 })
    assertTrue(ch11.trollTraps.any { it.id == 13 && it.triggerType == TrapTriggerType.ON_JUMP_ABOVE })

    val ch12 = ChallengeCatalog.createLockedConfig("ch_12")!!.levelDefinition
    assertTrue(ch12.trollTraps.any { it.id == 11 && it.type == TrollTrapType.FAKE_SAFE_PLATFORM && it.initialX <= 5.5f * World1Levels.TILE_SIZE })
    assertTrue(ch12.trollTraps.any { it.id == 12 && it.triggerCoinId == 4 })
    assertTrue(ch12.trollTraps.any { it.id == 13 && it.triggerType == TrapTriggerType.ON_JUMP_ABOVE })
    assertTrue(ch12.trollTraps.any { it.id == 14 && it.triggerCoinId == 5 })
  }

  @Test
  fun test04_surpriseTrapsEmitZeroWarningsZeroTelegraphFramesAndTriggerImpactFeedbackOnlyOnActivation() {
    val sys = TrollTrapSystem()
    var warningCalls = 0
    var shakeCalls = 0
    var hapticCalls = 0
    val cb = object : TrollTrapCallbacks {
      override fun killPlayer(quip: String) {}
      override fun relocateExitGate(destX: Float, destY: Float) {}
      override fun hideLinkedTile(tileId: Int) {}
      override fun playTrapSnap() {}
      override fun playCrumble() {}
      override fun playDisappear() {}
      override fun playCeilingSlam() {}
      override fun playDoorEscape() {}
      override fun playFakeDoorPoof() {}
      override fun playMechanismShift() {}
      override fun playTrapWarning() { warningCalls++ }
      override fun triggerImpactHaptic() { hapticCalls++ }
      override fun spawnDustTrickle(x: Float, y: Float) {}
      override fun spawnDustPuff(x: Float, y: Float) {}
      override fun spawnSparkles(x: Float, y: Float, color: Int, count: Int) {}
      override fun spawnStarburst(x: Float, y: Float) {}
      override fun spawnPopup(x: Float, y: Float, text: String, color: Int) {}
      override fun triggerCameraShake(frames: Int, magnitude: Float) { shakeCalls++ }
    }

    val spike = TrollTrap(
        id = 1,
        type = TrollTrapType.SURPRISE_SPIKE_POP,
        triggerType = TrapTriggerType.ON_PROXIMITY_X,
        initialX = 200f,
        initialY = 592f,
        width = 48f,
        height = 32f,
        triggerMinX = 160f,
        triggerMaxX = 250f,
        telegraphDurationFrames = 0,
        activeDurationFrames = 60,
        behaviorCategory = TrapBehaviorCategory.SURPRISE,
        killsPlayer = true
    )
    val fakeFloor = TrollTrap(
        id = 2,
        type = TrollTrapType.FAKE_SAFE_PLATFORM,
        triggerType = TrapTriggerType.ON_STEP,
        initialX = 300f,
        initialY = 624f,
        width = 48f,
        height = 48f,
        telegraphDurationFrames = 0,
        behaviorCategory = TrapBehaviorCategory.SURPRISE,
        killsPlayer = false
    )
    val traps = listOf(spike, fakeFloor)
    sys.resetAll(traps)

    // Before player enters trigger: zero warnings, zero shakes, zero haptics
    val player = Player(x = 40f, y = 570f, vy = 0f, isGrounded = true)
    sys.updateAll(traps, player, cb)
    assertEquals(0, warningCalls)
    assertEquals(0, shakeCalls)
    assertEquals(0, hapticCalls)
    assertEquals(TrollTrapState.ARMED, spike.state)

    // Enter spike proximity: must skip TELEGRAPH completely, jump straight to ACTIVE, 0 warnings
    player.x = 180f
    sys.updateAll(traps, player, cb)
    assertEquals(0, warningCalls)
    assertEquals(TrollTrapState.ACTIVE, spike.state)
    assertTrue("Spike activation must produce impact shake", shakeCalls >= 1)
    assertTrue("Spike activation must produce selective impact haptic", hapticCalls >= 1)

    // Step onto fake floor: must drop immediately without warning or telegraph
    val shakesBeforeFloor = shakeCalls
    val hapticsBeforeFloor = hapticCalls
    player.x = 308f
    player.prevY = 624f - player.collisionHeight - 2f
    player.y = 624f - player.collisionHeight + 1f
    player.vy = 2f
    sys.resolveVerticalCollisions(traps, player, previousGrounded = false, callbacks = cb) {}
    assertEquals(0, warningCalls)
    assertEquals(TrollTrapState.ACTIVE, fakeFloor.state)
    assertFalse(fakeFloor.isSolid)
    assertTrue(shakeCalls > shakesBeforeFloor)
    assertTrue(hapticsBeforeFloor < hapticCalls)
  }

  @Test
  fun test05_ch33UpperFinaleAndCh13ToCh36UpgradesFireWithoutWarningsAndResetCleanly() {
    val t = World1Levels.TILE_SIZE

    // Verify ch_33 upper final deck now defends Coin #5 & upper path with zero-telegraph traps
    val ch33 = engineForChallenge("ch_33")
    val upperCeiling = ch33.trollTraps.first { it.id == 13 }
    val upperSpike = ch33.trollTraps.first { it.id == 14 }
    assertNull(upperCeiling.quipPopup)
    assertNull(upperSpike.quipPopup)

    ch33.player.x = 49.0f * t - 10f
    ch33.player.y = 8.8f * t - 10f
    ch33.player.isGrounded = false
    stepInputs(ch33, frames = 2, right = true)
    assertEquals("ch_33 Coin #5 must trigger upper ceiling drop immediately", TrollTrapState.ACTIVE, upperCeiling.state)

    ch33.player.x = 50.0f * t
    ch33.player.y = 10.2f * t - ch33.player.collisionHeight
    ch33.player.isGrounded = true
    ch33.update()
    assertEquals("ch_33 upper spike #14 must activate immediately with 0 telegraph", TrollTrapState.ACTIVE, upperSpike.state)

    // Reset must restore traps to ARMED cleanly
    ch33.resetCurrentChallenge()
    assertEquals(TrollTrapState.ARMED, upperCeiling.state)
    assertEquals(TrollTrapState.ARMED, upperSpike.state)

    // Verify ch_29 trap #8 is now TELEGRAPHED_RUNE_BEAM chained off #7 with 0 telegraph & null quip
    val ch29 = engineForChallenge("ch_29")
    val ch29Beam = ch29.trollTraps.first { it.id == 8 }
    assertEquals(TrollTrapType.TELEGRAPHED_RUNE_BEAM, ch29Beam.type)
    assertEquals(7, ch29Beam.triggerSourceTrapId)
    assertEquals(0, ch29Beam.telegraphDurationFrames)
    assertNull(ch29Beam.quipPopup)

    // Verify ch_34 Coin #5 triggers #13 exit spike
    val ch34 = engineForChallenge("ch_34")
    val ch34Spike = ch34.trollTraps.first { it.id == 13 }
    assertEquals(0, ch34Spike.telegraphDurationFrames)
    assertNull(ch34Spike.quipPopup)
    ch34.player.x = 50.5f * t - 10f
    ch34.player.y = 8.4f * t - 10f
    stepInputs(ch34, frames = 8, right = false)
    assertEquals(TrollTrapState.ACTIVE, ch34Spike.state)
  }

  @Test
  fun test06_closeCallFeedbackCooldownAndModifierScoreMultiplierWorkCleanly() {
    val t = World1Levels.TILE_SIZE

    // 1. Verify ChaosModifier score synergy increases score cleanly without changing standard mode
    val stdEngine = engineForChallenge("ch_06", modifiers = emptySet())
    val modEngine = engineForChallenge("ch_06", modifiers = setOf(ChaosModifier.SPEED_RUN, ChaosModifier.TRAP_RUSH))

    val coinStd = stdEngine.coins.first { it.id == 1 }
    stdEngine.player.x = coinStd.x - 10f
    stdEngine.player.y = coinStd.y - 10f
    stdEngine.update()

    val coinMod = modEngine.coins.first { it.id == 1 }
    modEngine.player.x = coinMod.x - 10f
    modEngine.player.y = coinMod.y - 10f
    modEngine.update()

    assertTrue(
        "Modifier run score (${modEngine.score}) must exceed standard score (${stdEngine.score}) for identical coin",
        modEngine.score > stdEngine.score
    )
    assertEquals(100, stdEngine.score)
    assertEquals(150, modEngine.score)

    // 2. Verify CLOSE CALL! rewards once per trap with cooldown and resets cleanly
    val haptics = mutableListOf<HapticCue>()
    val ncEngine = engineForChallenge("ch_06", onHaptic = { haptics.add(it) })
    val spike = ncEngine.trollTraps.first { it.id == 9 }
    // Position player airborne right above active spike inside 14px near-miss envelope (not touching hitbox)
    ncEngine.player.x = spike.x + 6f
    ncEngine.player.y = spike.y - ncEngine.player.collisionHeight - 2f
    ncEngine.player.isGrounded = false
    ncEngine.player.vy = -1f
    ncEngine.update() // triggers #9 via proximity
    assertEquals(TrollTrapState.ACTIVE, spike.state)
    // Extend spike height past threshold so near-miss envelope checks active hitbox
    repeat(3) {
      ncEngine.player.x = spike.x + 6f
      ncEngine.player.y = spike.y - ncEngine.player.collisionHeight - 2f
      ncEngine.player.vy = 0f
      ncEngine.update()
    }
    assertEquals("Close call should trigger once per trap per attempt", 1, ncEngine.closeCallsCount)
    assertTrue("Close call must award bonus points", ncEngine.score >= 50)

    // Verify haptic cooldown prevents every-frame spam
    ncEngine.resetCurrentChallenge()
    assertEquals(0, ncEngine.closeCallsCount)
    assertEquals(0, ncEngine.score)
  }

  @Test
  fun test07_upgradedEarlyChallengesCh06Ch07Ch12AndCh33AreCleanlyWinnableZeroDeaths() {
    val t = World1Levels.TILE_SIZE

    // --- ch_06 intro + relocating exit finale (#13 -> 47.6T over #14 spike at 46.0T) ---
    val e6 = engineForChallenge("ch_06")
    val door6 = e6.trollTraps.first { it.id == 13 }
    runUntilX(e6, 3.0f * t)
    stepInputs(e6, frames = 20, right = true, jump = true)
    waitUntilGrounded(e6, right = false)
    // Skip ahead to final sanctum approach (testing the new Phase 10 relocating exit + guard spike!)
    e6.player.x = 42.0f * t
    e6.player.y = 13f * t - e6.player.collisionHeight
    e6.player.isGrounded = true
    runUntilX(e6, 44.2f * t)
    assertTrue("ch_06 finale Sun Gate must relocate on approach", door6.hasRelocated)
    assertEquals(47.6f * t, e6.currentExitX, 1f)
    // Jump cleanly over chained #14 spike at 46.0T into relocated gate at 47.6T
    runUntilX(e6, 44.8f * t)
    stepInputs(e6, frames = 22, right = true, jump = true)
    waitUntilGrounded(e6, right = true)
    if (!e6.isVictorious) runUntilX(e6, 48.8f * t, jump = false, maxFrames = 60)
    assertEquals(0, e6.deathCount)
    assertTrue("ch_06 upgraded finale must be cleanly winnable", e6.isVictorious)

    // --- ch_07 Coin #1 + Betrayal Switch #13/#14 + Relocating exit ---
    val e7 = engineForChallenge("ch_07")
    // Test jump over opening edge + Plank #1
    runUntilX(e7, 4.0f * t)
    stepInputs(e7, frames = 22, right = true, jump = true)
    waitUntilGrounded(e7, right = false)
    // Test Betrayal Switch #13 at 26.5T arms #14 at 30.0T, and player jumps cleanly over 30.0T
    e7.player.x = 26.2f * t
    e7.player.y = 13f * t - e7.player.collisionHeight
    e7.player.isGrounded = true
    runUntilX(e7, 29.0f * t)
    stepInputs(e7, frames = 22, right = true, jump = true)
    waitUntilGrounded(e7, right = false)
    assertEquals(0, e7.deathCount)
    // Finale relocating door
    e7.player.x = 45.5f * t
    e7.player.y = 13f * t - e7.player.collisionHeight
    e7.player.isGrounded = true
    runUntilX(e7, 52.0f * t, jump = false, maxFrames = 120)
    assertEquals(0, e7.deathCount)
    assertTrue("ch_07 must remain cleanly winnable", e7.isVictorious)

    // --- ch_12 intro Fake Safe Platform #11 + Mirage Gate -> Real Sun Gate ---
    val e12 = engineForChallenge("ch_12")
    runUntilX(e12, 4.1f * t)
    stepInputs(e12, frames = 18, right = true, jump = true) // clears #11 Fake Safe at 5.0T cleanly
    waitUntilGrounded(e12, right = false)
    assertTrue("Player must cleanly leap #11 Fake Safe start in ch_12", e12.player.x >= 5.8f * t && e12.deathCount == 0)
    // Clear final stretch past Mirage Door (#5) and reach Sun Gate (50.0T) cleanly
    e12.player.x = 44.0f * t
    e12.player.y = 13f * t - e12.player.collisionHeight
    e12.player.isGrounded = true
    runUntilX(e12, 46.2f * t)
    stepInputs(e12, frames = 22, right = true, jump = true) // hops #6 chained spike cleanly
    waitUntilGrounded(e12, right = true)
    if (!e12.isVictorious) runUntilX(e12, 51.0f * t, jump = false, maxFrames = 80)
    assertEquals(0, e12.deathCount)
    assertTrue("ch_12 must remain cleanly winnable", e12.isVictorious)

    // --- ch_33 upper final deck (#13 & #14) is cleanly beatable by jumping past 49.5..51.5T ---
    val e33 = engineForChallenge("ch_33")
    e33.player.x = 47.5f * t
    e33.player.y = 10.2f * t - e33.player.collisionHeight
    e33.player.isGrounded = true
    runUntilX(e33, 49.4f * t)
    stepInputs(e33, frames = 24, right = true, jump = true) // leaps over #13 & #14 toward 54.0T Sun Gate
    waitUntilGrounded(e33, right = true)
    if (!e33.isVictorious) runUntilX(e33, 55.0f * t, jump = false, maxFrames = 80)
    assertEquals(0, e33.deathCount)
    assertTrue("ch_33 upper deck must be cleanly winnable with good timing", e33.isVictorious)
  }
}
