package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 8 Verification Suite: "Unexpected But Fair Gameplay Rebalancing" across all 36 challenges.
 *
 * Confirms:
 * 1. Physics constants, 36-challenge catalog IDs/seeds, and Save Schema v3 remain preserved.
 * 2. Early/mid challenges (ch_01..ch_24) introduce genuine unexpected SURPRISE traps and trim
 *    bloated flat intro runways while preserving a progressive difficulty curve to ch_36.
 * 3. Newly added/rebalanced Phase 8 traps trigger accurately, match hitboxes, never softlock,
 *    and reset cleanly on retry and checkpoint rollback.
 * 4. TrapFairnessPolicy validates all 36 challenges across all 4 AdaptiveTiers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase8GameplayRebalancingTest {

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
  }

  private fun createSilentAudio(): ProceduralAudioEngine =
      ProceduralAudioEngine(context = app).apply {
        isMusicEnabled = false
        isSfxEnabled = false
      }

  private fun createEngine(
      challengeId: String,
      tier: AdaptiveTier = AdaptiveTier.STANDARD
  ): ChaosGameEngine {
    val cfg = ChallengeCatalog.createLockedConfig(challengeId, tier)!!
    return ChaosGameEngine(
        level = cfg.levelDefinition,
        audioEngine = createSilentAudio(),
        lockedChallengeConfig = cfg,
        initialLaunchMode = RunLaunchMode.SINGLE_STAGE
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
  fun test01_all36ChallengesPreserveCatalogIdsPhysicsConstantsAndPassFairnessAcrossAllTiers() {
    assertEquals(200, ChallengeCatalog.entries.size)
    val expectedIds = (1..200).map { "ch_${it.toString().padStart(2, '0')}" }
    assertEquals(expectedIds, ChallengeCatalog.entries.map { it.challengeId })
    assertEquals((1001L..1200L).toList(), ChallengeCatalog.entries.map { it.challengeSeed })

    // Core movement physics constants must remain unaltered
    assertEquals(6.5f, ChaosGameEngine.MAX_RUN_SPEED, 0.001f)
    assertEquals(-13.2f, ChaosGameEngine.JUMP_IMPULSE, 0.001f)
    assertEquals(0.58f, ChaosGameEngine.GRAVITY, 0.001f)
    assertEquals(48f, World1Levels.TILE_SIZE, 0.001f)
    val defaultPlayer = Player(x = 0f, y = 0f)
    assertEquals(32f, defaultPlayer.collisionWidth, 0.001f)
    assertEquals(54f, defaultPlayer.collisionHeight, 0.001f)

    // Every single trap across all 36 challenges and all 4 adaptive tiers must pass TrapFairnessPolicy
    for (entry in ChallengeCatalog.entries) {
      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
        assertTrue("${entry.challengeId} must contain traps", validations.isNotEmpty())
        for (v in validations) {
          assertTrue(
              "${entry.challengeId} ($tier) trap #${v.trapId} (${v.trapType}) failed fairness: ${v.reason}",
              v.isFair
          )
        }
      }
    }
  }

  @Test
  fun test02_earlyChallengesRemoveBoringRunwaysFeatureGenuineSurprisesAndScaleCurve() {
    val t = World1Levels.TILE_SIZE

    // Every challenge ch_01..ch_36 must include genuine 0-frame SURPRISE traps
    for (entry in ChallengeCatalog.entries) {
      val traps = entry.buildLevel().trollTraps
      val surpriseCount = traps.count { it.behaviorCategory == TrapBehaviorCategory.SURPRISE }
      assertTrue(
          "${entry.challengeId} must include SURPRISE traps for genuine unexpected moments (found $surpriseCount)",
          surpriseCount >= 1
      )
    }

    // Early game ch_01..ch_05 engage players early (no long empty intro runway past Tile 12)
    for (idx in 1..5) {
      val id = "ch_${idx.toString().padStart(2, '0')}"
      val lvl = ChallengeCatalog.findById(id)!!.buildLevel()
      val earliestHazardX = lvl.trollTraps.minOf { trap ->
        if (trap.triggerMinX > 0f) trap.triggerMinX else trap.initialX
      }
      assertTrue(
          "$id must present an active trap engagement before Tile 12.5 (earliest=${earliestHazardX / t}T)",
          earliestHazardX <= 12.5f * t
      )
    }

    // Progressive difficulty curve across campaign tiers (trap density grows cleanly from World 1 -> Endgame)
    val tier1Avg = (1..5).map { ChallengeCatalog.findById("ch_0$it")!!.buildLevel().trollTraps.size }.average()
    val tier2Avg = (6..12).map { ChallengeCatalog.findById("ch_${it.toString().padStart(2, '0')}")!!.buildLevel().trollTraps.size }.average()
    val midHighAvg = (13..24).map { ChallengeCatalog.findById("ch_$it")!!.buildLevel().trollTraps.size }.average()
    val brutalAvg = (25..36).map { ChallengeCatalog.findById("ch_$it")!!.buildLevel().trollTraps.size }.average()

    assertTrue("Rebalanced Tier 1 avg ($tier1Avg) removes sparse intros (>= 10.0)", tier1Avg >= 10.0)
    assertTrue("Tier 2 avg ($tier2Avg) maintains dense challenge traps (>= 7.0)", tier2Avg >= 7.0)
    assertTrue("Mid-high avg ($midHighAvg) maintains challenging trap density (>= 7.5)", midHighAvg >= 7.5)
    assertTrue("Endgame avg ($brutalAvg) escalates above Tier 2 ($tier2Avg) and Mid-high ($midHighAvg)", brutalAvg > tier2Avg && brutalAvg > midHighAvg)
  }

  @Test
  fun test03_phase8AddedSurpriseTrapsTriggerAccuratelyRespectHitboxesAndResetCleanly() {
    val t = World1Levels.TILE_SIZE

    // 1. ch_01 id=10 ON_JUMP_ABOVE canopy ambush: safe when walking calmly, triggers instantly on jump under Coin #2
    val ch01Walk = createEngine("ch_01")
    val canopyWalk = ch01Walk.trollTraps.first { it.id == 10 }
    ch01Walk.readWorldStateLocked {
      ch01Walk.player.x = 7.5f * t
      ch01Walk.player.y = 13f * t - ch01Walk.player.collisionHeight
      ch01Walk.player.isGrounded = true
    }
    ch01Walk.inputRight = true
    repeat(16) { ch01Walk.update() }
    assertEquals("Calm walk under Coin #2 must leave canopy ambush ARMED", TrollTrapState.ARMED, canopyWalk.state)

    val ch01Jump = createEngine("ch_01")
    val canopyJump = ch01Jump.trollTraps.first { it.id == 10 }
    ch01Jump.readWorldStateLocked {
      ch01Jump.player.x = 7.9f * t
      ch01Jump.player.y = 11.4f * t
      ch01Jump.player.vy = -8.0f
      ch01Jump.player.isGrounded = false
    }
    ch01Jump.update()
    assertEquals("Jumping for Coin #2 must trigger canopy ambush immediately", TrollTrapState.ACTIVE, canopyJump.state)
    ch01Jump.resetCurrentChallenge(clearInputs = true)
    assertEquals(TrollTrapState.ARMED, canopyJump.state)
    assertEquals(canopyJump.initialY, canopyJump.y, 0.1f)

    // 2. ch_01 id=11 exit-chase spike: pops on approach, retracts after active window, resets cleanly
    val ch01Spike = createEngine("ch_01")
    val chaseSpike = ch01Spike.trollTraps.first { it.id == 11 }
    ch01Spike.readWorldStateLocked {
      ch01Spike.player.x = 39.8f * t
      ch01Spike.player.y = 13f * t - ch01Spike.player.collisionHeight
      ch01Spike.player.isGrounded = true
    }
    ch01Spike.update()
    assertEquals("Exit-chase spike must activate immediately without telegraph", TrollTrapState.ACTIVE, chaseSpike.state)
    // Stay safely left and confirm it retracts instead of softlocking
    ch01Spike.readWorldStateLocked { ch01Spike.player.x = 34.5f * t }
    repeat(chaseSpike.activeDurationFrames + 4) { ch01Spike.update() }
    assertEquals("Temporary chase spike must retire cleanly", TrollTrapState.RETIRED, chaseSpike.state)
    ch01Spike.resetCurrentChallenge(clearInputs = true)
    assertEquals(TrollTrapState.ARMED, chaseSpike.state)

    // 3. ch_02 id=13 instant opening disappearing floor drops immediately on footfall & resets cleanly
    val ch02 = createEngine("ch_02")
    val earlyFloor = ch02.trollTraps.first { it.id == 13 }
    ch02.readWorldStateLocked {
      ch02.player.x = earlyFloor.initialX + 12f
      ch02.player.y = earlyFloor.initialY - ch02.player.collisionHeight
      ch02.player.vy = 2f
      ch02.player.prevY = ch02.player.y - 4f
      ch02.player.isGrounded = true
    }
    ch02.update()
    assertFalse("Opening ch_02 disappearing floor must drop solidity immediately", earlyFloor.isSolid)
    assertFalse("Opening ch_02 disappearing floor must hide cleanly", earlyFloor.isVisible)
    ch02.resetCurrentChallenge(clearInputs = true)
    assertTrue(earlyFloor.isSolid)
    assertTrue(earlyFloor.isVisible)
    assertEquals(TrollTrapState.ARMED, earlyFloor.state)

    // 4. ch_06 checkpoint rollback restores new #8 disappearing threshold & #9 pier spike
    val ch06 = createEngine("ch_06")
    val bell = ch06.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    val pierSpike = ch06.trollTraps.first { it.id == 9 }
    val exitThreshold = ch06.trollTraps.first { it.id == 8 }
    ch06.readWorldStateLocked {
      ch06.player.x = bell.x + 4f
      ch06.player.y = bell.y + bell.height - ch06.player.collisionHeight
      ch06.player.isGrounded = true
    }
    ch06.update()
    assertTrue("ch_06 checkpoint must bind cleanly", ch06.hasCheckpointActivated)

    ch06.readWorldStateLocked {
      ch06.player.x = pierSpike.triggerMinX + 6f
      ch06.player.y = 12.5f * t - ch06.player.collisionHeight
      ch06.player.isGrounded = true
    }
    ch06.update()
    assertEquals(TrollTrapState.ACTIVE, pierSpike.state)

    ch06.readWorldStateLocked {
      ch06.player.x = exitThreshold.initialX + 8f
      ch06.player.y = exitThreshold.initialY - ch06.player.collisionHeight
      ch06.player.isGrounded = true
    }
    ch06.update()
    assertFalse(exitThreshold.isSolid)

    ch06.handlePlayerDeath("test_ch06_rollback")
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) {
      if (ch06.isDying) ch06.update()
    }
    assertEquals("Checkpoint rollback must re-arm #9 pier spike", TrollTrapState.ARMED, pierSpike.state)
    assertEquals("Checkpoint rollback must restore #8 disappearing floor", TrollTrapState.ARMED, exitThreshold.state)
    assertTrue(exitThreshold.isSolid)
    assertTrue(exitThreshold.isVisible)

    // 5. ch_14 balcony spike (#6), ch_24 eternal bite (#10), ch_35 final coin spike (#10) activate cleanly
    val ch14 = createEngine("ch_14")
    val ch14Balcony = ch14.trollTraps.first { it.id == 6 }
    ch14.readWorldStateLocked {
      ch14.player.x = ch14Balcony.triggerMinX + 4f
      ch14.player.y = 9.6f * t - ch14.player.collisionHeight
      ch14.player.isGrounded = true
    }
    ch14.update()
    assertEquals(TrollTrapState.ACTIVE, ch14Balcony.state)

    val ch35 = createEngine("ch_35")
    val coin5 = ch35.coins.first { it.id == 5 }
    val ch35Spike = ch35.trollTraps.first { it.id == 10 }
    ch35.readWorldStateLocked {
      ch35.player.x = coin5.x - ch35.player.collisionWidth * 0.5f
      ch35.player.y = coin5.y - ch35.player.collisionHeight * 0.5f
    }
    ch35.update()
    assertTrue(coin5.isCollected)
    assertEquals(TrollTrapState.ACTIVE, ch35Spike.state)
  }

  @Test
  fun test04_all36ChallengesHaveSafeSpawnCheckpointsReachableExitsAndRenderCleanly() {
    val renderer = ChaosGameRenderer(app)
    val bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    try {
      for (entry in ChallengeCatalog.entries) {
        val engine = createEngine(entry.challengeId)
        // Player must settle onto solid footing without falling out of bounds
        repeat(15) { engine.update() }
        assertTrue("${entry.challengeId} spawn must land on ground", engine.player.isGrounded)
        assertEquals("${entry.challengeId} idle at spawn must not die", 0, engine.deathCount)

        // Checkpoints must not sit inside initial trap bounding boxes
        val bells = engine.tiles.filter { it.type == TileType.CHECKPOINT_BELL }
        for (bell in bells) {
          val overlaps = engine.trollTraps.any { trap ->
            rectsOverlap(bell.x, bell.y, bell.width, bell.height, trap.initialX, trap.initialY, trap.width, trap.height)
          }
          assertFalse("${entry.challengeId} bell overlaps initial trap", overlaps)
        }

        // Render cleanly
        renderer.render(canvas, engine, 640f, 360f)
        assertTrue("${entry.challengeId} renderer must produce a frame", renderer.hasRenderedFrame)

        // Exit SUN_GATE must trigger victory when blockers/fleeing gates are cleared
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
        engine.readWorldStateLocked {
          engine.player.x = gate.x + gate.width * 0.5f - engine.player.collisionWidth * 0.5f
          engine.player.y = gate.y + gate.height - engine.player.collisionHeight
          engine.player.isGrounded = true
        }
        engine.update()
        assertTrue("${entry.challengeId} SUN_GATE must be reachable and winnable", engine.isVictorious)
      }
    } finally {
      renderer.expansionRenderer.release()
      bmp.recycle()
    }
  }

  @Test
  fun test05_continuousRun36MasteryAndSaveSchemaV3Preserved() {
    val coord = ContinuousRunCoordinator()
    coord.startNewRun()
    for (idx in 0 until 199) {
      val id = ChallengeCatalog.entries[idx].challengeId
      val outcome = coord.onChallengeCompleted(
          challengeId = id,
          attemptToken = coord.currentAttemptToken,
          timeMs = 15_000L,
          score = 750,
          coinsCollected = 3,
          totalCoins = 5,
          parTimeSeconds = 32,
          isTrollLevel = true,
          challengeDeaths = 1
      )
      assertTrue("$id must advance continuous run", outcome is ChallengeCompletionOutcome.NextChallenge)
    }
    assertEquals("ch_200", coord.activeChallengeId)
    val finalOutcome = coord.onChallengeCompleted(
        challengeId = "ch_200",
        attemptToken = coord.currentAttemptToken,
        timeMs = 22_000L,
        score = 1200,
        coinsCollected = 5,
        totalCoins = 5,
        parTimeSeconds = 46,
        isTrollLevel = true,
        challengeDeaths = 2
    )
    assertTrue(finalOutcome is ChallengeCompletionOutcome.RunCompleted)

    assertEquals(200, MasteryCatalog.specs.size)
    assertEquals(600, MasteryCatalog.totalPossibleStars)

    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    val repo = SaveRepository(app)
    val existing = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = false,
        isRunInForeground = false,
        isRunCompleted = false,
        activeChallengeId = "ch_13",
        activeChallengeSeed = 1013L,
        activeConfigVersion = ChallengeCatalog.CONFIG_VERSION,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 0,
        completedChallengeIds = (1..12).map { "ch_${it.toString().padStart(2, '0')}" }.toSet(),
        challengeRecords = (1..12).associate { idx ->
          val cid = "ch_${idx.toString().padStart(2, '0')}"
          cid to ChallengeRecord(cid, completed = true, bestTimeMs = 14_000L, bestScore = 800, stars = 3, leastDeaths = 0)
        },
        cumulativeRunTimerMs = 168_000L,
        cumulativeDeaths = 3,
        cumulativeScore = 9600,
        bestRunTimeMs = 0L,
        bestRunScore = 0,
        bestRunLeastDeaths = -1
    )
    assertTrue(repo.saveContinuousRunState(existing, syncCommit = true))
    val loaded = repo.loadContinuousRunState()
    assertEquals(SaveRepository.SAVE_SCHEMA_VERSION, loaded.schemaVersion)
    assertEquals(12, loaded.completedChallengeIds.size)
    assertEquals(14_000L, loaded.challengeRecords["ch_06"]?.bestTimeMs)
  }
}
