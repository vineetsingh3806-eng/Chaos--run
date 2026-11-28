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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase6ContentExpansion24ChallengesTest {

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
      tier: AdaptiveTier = AdaptiveTier.STANDARD,
      modifiers: Set<ChaosModifier> = emptySet()
  ): ChaosGameEngine {
    val cfg = ChallengeCatalog.createLockedConfig(challengeId, tier)!!
    return ChaosGameEngine(
        level = cfg.levelDefinition,
        audioEngine = createSilentAudio(),
        lockedChallengeConfig = cfg,
        initialLaunchMode = RunLaunchMode.SINGLE_STAGE,
        initialModifiers = modifiers
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
  fun test01_catalogHas24DistinctValidChallengesWithProperSpawnsExitsAndCheckpoints() {
    assertEquals(200, ChallengeCatalog.entries.size)
    val expectedIds = (1..200).map { "ch_${it.toString().padStart(2, '0')}" }
    assertEquals(expectedIds, ChallengeCatalog.entries.map { it.challengeId })
    assertEquals((1..200).toList(), ChallengeCatalog.entries.map { it.orderIndex })
    assertEquals((1001L..1200L).toList(), ChallengeCatalog.entries.map { it.challengeSeed })
    assertEquals(200, ChallengeCatalog.entries.map { it.title }.toSet().size)

    for (entry in ChallengeCatalog.entries) {
      val lvl = entry.buildLevel()
      val exitTiles = lvl.tiles.filter { it.type == TileType.SUN_GATE }
      assertTrue("${entry.challengeId} must contain SUN_GATE", exitTiles.isNotEmpty())

      // Verify spawn overlaps solid support below and no initial lethal trap at spawn
      val spawnHasSupport = lvl.tiles.any { tile ->
        tile.isSolid &&
            lvl.spawnX + 16f >= tile.x - 12f &&
            lvl.spawnX + 16f <= tile.x + tile.width + 12f &&
            tile.y >= lvl.spawnY
      }
      assertTrue("${entry.challengeId} spawn must sit above solid floor", spawnHasSupport)

      val spawnHitsTrap = lvl.trollTraps.any { trap ->
        trap.killsPlayer &&
            trap.isVisible &&
            rectsOverlap(
                lvl.spawnX, lvl.spawnY, 32f, 54f,
                trap.initialX, trap.initialY, trap.width, trap.height
            )
      }
      assertFalse("${entry.challengeId} spawn must not start inside lethal trap", spawnHitsTrap)

      // Check ch_13..ch_36 checkpoints & exit support
      if (entry.orderIndex >= 13) {
        val bells = lvl.tiles.filter { it.type == TileType.CHECKPOINT_BELL }
        if (entry.orderIndex in 21..36) {
          assertEquals("${entry.challengeId} (Extreme) must have 2 checkpoints", 2, bells.size)
        } else {
          assertTrue("${entry.challengeId} must have at least 1 checkpoint", bells.isNotEmpty())
        }
        for (bell in bells) {
          val bellOverlapsTrap = lvl.trollTraps.any { trap ->
            rectsOverlap(
                bell.x, bell.y, bell.width, bell.height,
                trap.initialX, trap.initialY, trap.width, trap.height
            )
          }
          assertFalse("${entry.challengeId} checkpoint bell must not overlap initial trap", bellOverlapsTrap)
        }
      }
    }
  }

  @Test
  fun test02_all24ChallengesPassTrapFairnessAndChainIntegrityAcrossAll4AdaptiveTiers() {
    for (entry in ChallengeCatalog.entries) {
      val lvl = entry.buildLevel()
      val trapById = lvl.trollTraps.associateBy { it.id }
      val coinIds = lvl.coins.map { it.id }.toSet()

      for (trap in lvl.trollTraps) {
        if (trap.triggerType == TrapTriggerType.ON_COIN_COLLECTED && trap.triggerCoinId != null) {
          assertTrue(
              "${entry.challengeId} trap #${trap.id} references missing coin #${trap.triggerCoinId}",
              coinIds.contains(trap.triggerCoinId)
          )
        }
        if (trap.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED) {
          val hasSource = trap.triggerSourceTrapId?.let { trapById.containsKey(it) } == true ||
              lvl.trollTraps.any { it.linkedTrapId == trap.id }
          assertTrue(
              "${entry.challengeId} ON_TRAP_ACTIVATED trap #${trap.id} must have a valid upstream source",
              hasSource
          )
        }
        if (trap.linkedTrapId != null) {
          assertTrue(
              "${entry.challengeId} trap #${trap.id} linkedTrapId #${trap.linkedTrapId} missing",
              trapById.containsKey(trap.linkedTrapId)
          )
          assertNotEquals(
              "${entry.challengeId} trap #${trap.id} cannot link to itself",
              trap.id,
              trap.linkedTrapId
          )
        }
      }

      for (tier in AdaptiveTier.entries) {
        val report = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
        for (item in report) {
          assertTrue(
              "Fairness failure in ${entry.challengeId} ($tier) trap #${item.trapId} (${item.trapType}): ${item.reason}",
              item.isFair
          )
        }
      }
    }
  }

  @Test
  fun test03_all5NewTrapMechanicsBehaveTelegraphCollideAndResetCleanly() {
    // 1. REVERSE_CONVEYOR_BELT (ch_13) — grounded carry, telegraphed reversal, and clean reset
    val ch13 = createEngine("ch_13")
    val belt = ch13.trollTraps.first { it.type == TrollTrapType.REVERSE_CONVEYOR_BELT }
    val initialGustSign = belt.gustSign
    ch13.readWorldStateLocked {
      ch13.player.x = belt.x + belt.width * 0.5f
      ch13.player.y = belt.y - ch13.player.collisionHeight
      ch13.player.vx = 0f
      ch13.player.vy = 0f
      ch13.player.isGrounded = true
    }
    val startX = ch13.player.x
    repeat(3) { ch13.update() }
    assertNotEquals("Conveyor must move grounded player horizontally", startX, ch13.player.x, 0.5f)

    // Advance until conveyor telegraphs and flips direction
    var sawTelegraph = false
    var flipped = false
    repeat(160) {
      ch13.update()
      if (belt.state == TrollTrapState.TELEGRAPH) sawTelegraph = true
      if (belt.gustSign != initialGustSign) flipped = true
    }
    assertTrue("Conveyor must enter TELEGRAPH warning before reversal", sawTelegraph)
    assertTrue("Conveyor must flip gustSign after telegraph", flipped)

    ch13.resetCurrentChallenge()
    assertEquals(initialGustSign, belt.gustSign, 0.001f)
    assertEquals(TrollTrapState.ARMED, belt.state)

    // 2. LAUNCH_BETRAYAL_PAD (ch_14) — immediate launch impulse, non-lethal telegraph, then spikes + reset
    val ch14 = createEngine("ch_14")
    val pad = ch14.trollTraps.first { it.type == TrollTrapType.LAUNCH_BETRAYAL_PAD }
    ch14.readWorldStateLocked {
      ch14.player.x = pad.x + 8f
      ch14.player.y = pad.y - ch14.player.collisionHeight - 2f
      ch14.player.vx = 0f
      ch14.player.vy = 2.0f
    }
    repeat(2) { ch14.update() }
    assertTrue("Launch betrayal pad must fire upward impulse", ch14.player.vy <= -12f)
    assertEquals("Pad must enter TELEGRAPH warning phase on launch", TrollTrapState.TELEGRAPH, pad.state)
    assertFalse("Telegraph phase must be non-lethal", ch14.isDying)

    repeat(pad.telegraphDurationFrames + 8) { ch14.update() }
    assertTrue("Pad spikes must sprout after telegraph", pad.morphSpikeHeight > 8f)

    ch14.resetCurrentChallenge()
    assertEquals(TrollTrapState.ARMED, pad.state)
    assertEquals(0f, pad.morphSpikeHeight, 0.01f)

    // 3. RISING_EMBER_SURGE (ch_14) — telegraph shimmer -> surge upward -> recede -> reset
    val ember = ch14.trollTraps.first { it.type == TrollTrapType.RISING_EMBER_SURGE }
    val emberStartY = ember.initialY
    ch14.readWorldStateLocked {
      ch14.player.x = ember.triggerMinX + 20f
      ch14.player.y = 8f * World1Levels.TILE_SIZE
      ch14.player.vy = 0f
      ch14.player.isGrounded = true
    }
    ch14.update()
    assertEquals("Ember surge must telegraph first", TrollTrapState.TELEGRAPH, ember.state)
    repeat(ember.telegraphDurationFrames + 20) {
      ch14.readWorldStateLocked {
        ch14.player.vy = 0f
        ch14.player.isGrounded = true
      }
      ch14.update()
    }
    assertTrue("Ember surge must rise upward toward targetY", ember.y < emberStartY)
    ch14.resetCurrentChallenge()
    assertEquals(emberStartY, ember.y, 0.01f)
    assertEquals(TrollTrapState.ARMED, ember.state)

    // Verify pad re-arms after cooldown even past 3 activations (prevents climb softlock)
    repeat(3) {
      ch14.resetCurrentChallenge()
      ch14.readWorldStateLocked {
        ch14.player.x = pad.x + 8f
        ch14.player.y = pad.y - ch14.player.collisionHeight - 2f
        ch14.player.vy = 2.0f
      }
      ch14.update()
    }
    ch14.resetCurrentChallenge()
    repeat(4) { cycleIdx ->
      ch14.readWorldStateLocked {
        ch14.player.x = pad.x + 8f
        ch14.player.y = pad.y - ch14.player.collisionHeight - 2f
        ch14.player.vy = 2.0f
      }
      ch14.update()
      assertEquals("Bounce #$cycleIdx must launch cleanly", TrollTrapState.TELEGRAPH, pad.state)
      repeat(pad.lockedTelegraphDurationFrames + pad.activeDurationFrames + 4) {
        ch14.readWorldStateLocked {
          ch14.player.x = 2f * World1Levels.TILE_SIZE
          ch14.player.y = 11f * World1Levels.TILE_SIZE
          ch14.player.vy = 0f
        }
        ch14.update()
      }
      assertEquals("Launch pad must re-arm rather than permanently retiring", TrollTrapState.ARMED, pad.state)
    }

    // 4. TELEGRAPHED_RUNE_BEAM (ch_16) — non-lethal aiming sightline -> lethal active beam -> reset
    val ch16 = createEngine("ch_16")
    val beam = ch16.trollTraps.first { it.type == TrollTrapType.TELEGRAPHED_RUNE_BEAM }
    var beamTelegraphed = false
    var beamActivated = false
    repeat(150) {
      ch16.update()
      if (beam.state == TrollTrapState.TELEGRAPH) {
        beamTelegraphed = true
      }
      if (beam.state == TrollTrapState.ACTIVE) {
        beamActivated = true
      }
    }
    assertTrue("Rune beam must telegraph sightline before firing", beamTelegraphed)
    assertTrue("Rune beam must fire ACTIVE beam after telegraph", beamActivated)

    // Verify beam hitbox kills player only during ACTIVE (not TELEGRAPH)
    ch16.resetCurrentChallenge()
    assertEquals(TrollTrapState.ARMED, beam.state)
    beam.state = TrollTrapState.TELEGRAPH
    beam.stateTimerFrames = 10
    ch16.readWorldStateLocked {
      ch16.player.x = beam.x + beam.width * 0.5f - 10f
      ch16.player.y = beam.y + beam.height * 0.5f - 20f
      ch16.player.vx = 0f
      ch16.player.vy = 0f
    }
    ch16.update()
    assertFalse("Telegraph sightline must NOT kill player", ch16.isDying)
    beam.state = TrollTrapState.ACTIVE
    beam.stateTimerFrames = 20
    ch16.readWorldStateLocked {
      ch16.player.x = beam.x + beam.width * 0.5f - 10f
      ch16.player.y = beam.y + beam.height * 0.5f - 20f
    }
    ch16.update()
    assertTrue("Active rune beam must kill overlapping player", ch16.isDying)

    // 5. LOCKED_RUNE_GATE (ch_15) — initially blocks passage, unlocks via linked coin/switch & allows passthrough
    val ch15 = createEngine("ch_15")
    val gate1 = ch15.trollTraps.first { it.type == TrollTrapType.LOCKED_RUNE_GATE }
    assertTrue("Locked rune gate must start solid", gate1.isSolid)
    val keyCoin = ch15.coins.first { it.id == gate1.triggerCoinId }
    ch15.readWorldStateLocked {
      ch15.player.x = keyCoin.x - 10f
      ch15.player.y = keyCoin.y - 10f
    }
    repeat(30) { ch15.update() }
    assertFalse("Collecting key coin must unlock rune gate", gate1.isSolid)
    assertEquals(TrollTrapState.RETIRED, gate1.state)

    // Walk horizontally straight through the opened gate corridor without being blocked
    ch15.readWorldStateLocked {
      ch15.player.x = gate1.initialX - 40f
      ch15.player.y = 13f * World1Levels.TILE_SIZE - ch15.player.collisionHeight
      ch15.player.isGrounded = true
    }
    ch15.inputRight = true
    repeat(20) { ch15.update() }
    ch15.inputRight = false
    assertTrue(
        "Player must run unobstructed past unlocked rune gate",
        ch15.player.x > gate1.initialX + gate1.width
    )

    ch15.resetCurrentChallenge()
    assertTrue("Resetting challenge must re-lock rune gate", gate1.isSolid)
  }

  @Test
  fun test04_checkpointRollbackAndMultiTrapChainsWorkCleanlyInCh13To24() {
    // ch_13: Coin Bait #1 triggers Falling Ceiling #2 with telegraph before slam; reset restores
    val ch13 = createEngine("ch_13")
    val baitCoin = ch13.coins.first { it.id == 1 }
    val baitBlock = ch13.trollTraps.first { it.id == 2 }
    ch13.readWorldStateLocked {
      ch13.player.x = baitCoin.x - 10f
      ch13.player.y = baitCoin.y - 10f
    }
    repeat(baitBlock.chainDelayFrames + 2) { ch13.update() }
    assertTrue(
        "Coin #1 must queue & trigger bait ceiling block",
        baitBlock.state == TrollTrapState.TELEGRAPH || baitBlock.state == TrollTrapState.ACTIVE
    )

    // ch_15: Activate Gate 1 before checkpoint -> touch checkpoint -> trigger post-checkpoint ambush -> die -> verify rollback
    val ch15 = createEngine("ch_15")
    val gate1 = ch15.trollTraps.first { it.type == TrollTrapType.LOCKED_RUNE_GATE }
    val keyCoin1 = ch15.coins.first { it.id == gate1.triggerCoinId }
    ch15.readWorldStateLocked {
      ch15.player.x = keyCoin1.x - 10f
      ch15.player.y = keyCoin1.y - 10f
    }
    repeat(25) { ch15.update() }
    assertFalse("Gate 1 unlocked before checkpoint", gate1.isSolid)

    // Touch checkpoint bell
    val bell = ch15.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    ch15.readWorldStateLocked {
      ch15.player.x = bell.x + 4f
      ch15.player.y = bell.y + 4f
    }
    ch15.update()
    assertTrue("Checkpoint must activate", ch15.hasCheckpointActivated)

    // Activate post-checkpoint floor/trap then die
    val postCheckpointTrap = ch15.trollTraps.last { it.x > bell.x }
    postCheckpointTrap.state = TrollTrapState.RETIRED
    postCheckpointTrap.isSolid = false
    ch15.handlePlayerDeath()
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) {
      if (ch15.isDying) ch15.update()
    }

    assertFalse("Pre-checkpoint unlocked gate must remain unlocked on checkpoint restore", gate1.isSolid)
    assertEquals("Post-checkpoint trap must restore to ARMED on checkpoint rollback", TrollTrapState.ARMED, postCheckpointTrap.state)

    // ch_20: Domino Cavern chain reaction propagates safely across chain traps
    val ch20 = createEngine("ch_20")
    val dominoCoin = ch20.coins.first { it.id == 1 }
    ch20.readWorldStateLocked {
      ch20.player.x = dominoCoin.x - 10f
      ch20.player.y = dominoCoin.y - 10f
    }
    repeat(40) { ch20.update() }
    val triggeredDominoCount = ch20.trollTraps.count { it.state != TrollTrapState.ARMED }
    assertTrue("Domino chain must propagate across linked traps", triggeredDominoCount >= 2)
  }

  @Test
  fun test05_progressionMastery72DailySaveV3CompatibilityAndRenderAll29TrapTypes() {
    // 1. ContinuousRunCoordinator covers all 200 challenges end-to-end
    val coord = ContinuousRunCoordinator()
    coord.startNewRun()
    for (idx in 0 until 199) {
      val chId = ChallengeCatalog.entries[idx].challengeId
      val outcome = coord.onChallengeCompleted(
          challengeId = chId,
          attemptToken = coord.currentAttemptToken,
          timeMs = 15_000L,
          score = 800,
          coinsCollected = 2,
          totalCoins = 5,
          parTimeSeconds = 30,
          isTrollLevel = true,
          challengeDeaths = 1
      )
      assertTrue("$chId must advance to next challenge", outcome is ChallengeCompletionOutcome.NextChallenge)
    }
    assertEquals("ch_200", coord.activeChallengeId)
    val finalOutcome = coord.onChallengeCompleted(
        challengeId = "ch_200",
        attemptToken = coord.currentAttemptToken,
        timeMs = 18_000L,
        score = 1000,
        coinsCollected = 3,
        totalCoins = 5,
        parTimeSeconds = 44,
        isTrollLevel = true,
        challengeDeaths = 2
    )
    assertTrue(finalOutcome is ChallengeCompletionOutcome.RunCompleted)
    val summary = (finalOutcome as ChallengeCompletionOutcome.RunCompleted).summary
    assertEquals(200, summary.completedCount)
    assertEquals(200, summary.totalChallenges)

    // 2. MasteryCatalog: 200 specs, 600 total stars, NO_COINS levels never require key coins for LOCKED_RUNE_GATE
    assertEquals(200, MasteryCatalog.specs.size)
    assertEquals(600, MasteryCatalog.totalPossibleStars)
    for (spec in MasteryCatalog.specs) {
      val third = spec.objectives[2].kind
      if (third == MasteryObjectiveKind.NO_COINS) {
        val lvl = ChallengeCatalog.findById(spec.challengeId)!!.buildLevel()
        val hasCoinLockedGate = lvl.trollTraps.any {
          it.type == TrollTrapType.LOCKED_RUNE_GATE && it.triggerType == TrapTriggerType.ON_COIN_COLLECTED
        }
        assertFalse("${spec.challengeId} uses NO_COINS mastery and must not gate progress on a coin", hasCoinLockedGate)
      }
    }

    // 3. OfflineDailyChallengeGenerator spans expanded 24-challenge pool cleanly
    val sampledChallengeIds = (1..24).map { day ->
      OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate(2026, 11, day)).challengeId
    }.toSet()
    assertTrue("Daily generator should pick from both 1..12 and 13..24 pools", sampledChallengeIds.any { it.removePrefix("ch_").toInt() > 12 })

    // 4. Schema v3 backward compatibility: existing ch_01..ch_12 completions persist alongside ch_13..ch_24
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    val repo = SaveRepository(app)
    val legacyCompleted12 = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = false,
        isRunInForeground = false,
        isRunCompleted = false,
        activeChallengeId = "ch_12",
        activeChallengeSeed = 1012L,
        activeConfigVersion = ChallengeCatalog.CONFIG_VERSION,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 0,
        completedChallengeIds = (1..12).map { "ch_${it.toString().padStart(2, '0')}" }.toSet(),
        challengeRecords = (1..12).associate { idx ->
          val id = "ch_${idx.toString().padStart(2, '0')}"
          id to ChallengeRecord(id, completed = true, bestTimeMs = 14_000L, bestScore = 700, stars = 3, leastDeaths = 0)
        },
        cumulativeRunTimerMs = 168_000L,
        cumulativeDeaths = 4,
        cumulativeScore = 8_400,
        bestRunTimeMs = 0L,
        bestRunScore = 0,
        bestRunLeastDeaths = -1
    )
    assertTrue(repo.saveContinuousRunState(legacyCompleted12, syncCommit = true))
    val loaded = repo.loadContinuousRunState()
    assertEquals(12, loaded.completedChallengeIds.size)
    assertEquals(14_000L, loaded.challengeRecords["ch_12"]?.bestTimeMs)

    // Save a ch_13 replay record cleanly without disturbing ch_01..ch_12 records
    repo.saveReplayChallengeRecordIfImproved(
        "ch_13",
        ChallengeRecord("ch_13", completed = true, bestTimeMs = 21_000L, bestScore = 950, stars = 3, leastDeaths = 1),
        syncCommit = true
    )
    val reloaded = repo.loadContinuousRunState()
    assertEquals(14_000L, reloaded.challengeRecords["ch_12"]?.bestTimeMs)
    assertEquals(21_000L, reloaded.challengeRecords["ch_13"]?.bestTimeMs)

    // 5. Render smoke test across all 29 TrollTrapTypes in ARMED, TELEGRAPH, ACTIVE without exceptions
    val tileRenderer = TileAtlasRenderer()
    val bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    for (trapType in TrollTrapType.entries) {
      for (state in listOf(TrollTrapState.ARMED, TrollTrapState.TELEGRAPH, TrollTrapState.ACTIVE)) {
        val trap = TrollTrap(
            id = 900 + trapType.ordinal,
            type = trapType,
            triggerType = TrapTriggerType.ON_PROXIMITY_X,
            initialX = 120f,
            initialY = 180f,
            width = 96f,
            height = 48f,
            targetX = 240f,
            targetY = 140f,
            telegraphDurationFrames = 16
        ).apply {
          this.state = state
          stateTimerFrames = 8
          morphSpikeHeight = if (state == TrollTrapState.ACTIVE) 20f else 0f
          auxProgress = 0.5f
        }
        tileRenderer.drawTrollTrap(canvas, trap, cameraX = 0f, cameraY = 0f, animTick = 12)
      }
    }
    bmp.recycle()
  }

  @Test
  fun test06_reachableExitsRecoveryPathsAndSoftlockPreventionAcrossCh13ToCh24() {
    val tile = World1Levels.TILE_SIZE

    for (entry in ChallengeCatalog.entries.filter { it.orderIndex in 13..36 }) {
      val lvl = entry.buildLevel()

      // 1. Every contiguous high platform > 3.0T (unreachable by single jump) must have a re-arming LAUNCH_BETRAYAL_PAD or YO_YO_PLATFORM lift nearby
      val highLedges = lvl.tiles.filter { tileObj ->
        tileObj.isSolid && tileObj.y < 10.1f * tile &&
            lvl.tiles.none { leftNeighbor ->
              leftNeighbor.isSolid &&
                  kotlin.math.abs(leftNeighbor.y - tileObj.y) < 4f &&
                  kotlin.math.abs((leftNeighbor.x + leftNeighbor.width) - tileObj.x) < 4f
            }
      }
      for (ledge in highLedges) {
        val climbTiles = (13f * tile - ledge.y) / tile
        if (climbTiles > 3.05f) {
          val nearbyPad = lvl.trollTraps.firstOrNull {
            it.type == TrollTrapType.LAUNCH_BETRAYAL_PAD &&
                it.initialX < ledge.x &&
                ledge.x - it.initialX <= 4.5f * tile
          }
          val nearbyYoYo = lvl.trollTraps.firstOrNull {
            it.type == TrollTrapType.YO_YO_PLATFORM &&
                it.initialX < ledge.x &&
                ledge.x - (it.initialX + it.width) <= 4.5f * tile
          }
          assertTrue(
              "${entry.challengeId} high ledge at x=${ledge.x / tile}T (y=${ledge.y / tile}T) needs a launch pad or yo-yo lift",
              nearbyPad != null || nearbyYoYo != null
          )
          if (nearbyPad != null) {
            assertTrue(
                "${entry.challengeId} launch pad #${nearbyPad.id} must set cycleFrames > 0 so it never softlocks",
                nearbyPad.cycleFrames > 0
            )
          }
        }
      }

      // 2. Every sunken Ember Basin span (y >= 14T) must allow <= 1.5T hop recovery onto solid exit/entry terrain
      val basinTiles = lvl.tiles
          .filter { it.isSolid && it.y >= 14.0f * tile }
          .sortedBy { it.x }
      var idx = 0
      while (idx < basinTiles.size) {
        val spanStartX = basinTiles[idx].x
        val spanY = basinTiles[idx].y
        var spanEndX = basinTiles[idx].x + basinTiles[idx].width
        idx++
        while (idx < basinTiles.size &&
            kotlin.math.abs(basinTiles[idx].y - spanY) < 4f &&
            basinTiles[idx].x <= spanEndX + 4f) {
          spanEndX = maxOf(spanEndX, basinTiles[idx].x + basinTiles[idx].width)
          idx++
        }
        val hasRecoverStep = lvl.tiles.any {
          it.isSolid &&
              it.y < spanY &&
              spanY - it.y <= 1.5f * tile &&
              (kotlin.math.abs(it.x - spanEndX) <= 2.0f * tile ||
                  kotlin.math.abs((it.x + it.width) - spanStartX) <= 2.0f * tile)
        }
        assertTrue(
            "${entry.challengeId} ember basin span [${spanStartX / tile}T..${spanEndX / tile}T] must have reachable recovery footing",
            hasRecoverStep
        )
      }

      // 3. Engine victory verification: resolve gates/fleeing traps/slam cages -> reaching exit wins cleanly
      val engine = createEngine(entry.challengeId)
      for (trap in engine.trollTraps) {
        when (trap.type) {
          TrollTrapType.FLEEING_SUN_GATE -> {
            trap.activationCount = trap.maxActivations
            trap.state = TrollTrapState.RETIRED
          }
          TrollTrapType.EXIT_SLAM_CAGE -> {
            trap.isSolid = false
            trap.state = TrollTrapState.RETIRED
          }
          TrollTrapType.LOCKED_RUNE_GATE -> {
            trap.isSolid = false
            trap.state = TrollTrapState.RETIRED
          }
          else -> Unit
        }
      }
      val gateTile = engine.tiles.first { it.type == TileType.SUN_GATE }
      val gateFloorSupport = engine.tiles.any {
        it.isSolid &&
            gateTile.x + gateTile.width * 0.5f >= it.x - 16f &&
            gateTile.x + gateTile.width * 0.5f <= it.x + it.width + 16f &&
            it.y >= gateTile.y + gateTile.height - 8f
      }
      assertTrue("${entry.challengeId} Sun Gate must rest on solid floor", gateFloorSupport)

      engine.readWorldStateLocked {
        engine.player.x = gateTile.x + gateTile.width * 0.5f - engine.player.collisionWidth * 0.5f
        engine.player.y = gateTile.y + gateTile.height - engine.player.collisionHeight
        engine.player.isGrounded = true
      }
      engine.update()
      if (!engine.isVictorious) {
        // Handle RELOCATING_EXIT_DOOR hop: verify new destination has solid floor support & stepping there wins
        val relocatedFloorSupport = engine.tiles.any {
          it.isSolid &&
              gateTile.x + gateTile.width * 0.5f >= it.x - 16f &&
              gateTile.x + gateTile.width * 0.5f <= it.x + it.width + 16f &&
              it.y >= gateTile.y + gateTile.height - 8f
        }
        assertTrue("${entry.challengeId} relocated Sun Gate must rest on solid floor", relocatedFloorSupport)
        engine.readWorldStateLocked {
          engine.player.x = gateTile.x + gateTile.width * 0.5f - engine.player.collisionWidth * 0.5f
          engine.player.y = gateTile.y + gateTile.height - engine.player.collisionHeight
          engine.player.isGrounded = true
        }
        engine.update()
      }
      assertTrue("${entry.challengeId} exit must cleanly trigger victory when reached", engine.isVictorious)
    }
  }

  @Test
  fun test07_adaptiveDifficultyAndChaosModifierIsolationAcrossCh13ToCh24() {
    // 1. AdaptiveDifficultyController on ch_13..ch_24: retries trigger ASSISTED; strong clears escalate safely
    val adaptive = AdaptiveDifficultyController()
    adaptive.resetForNewRun(startingChallengeId = "ch_16")
    assertEquals(AdaptiveTier.STANDARD, adaptive.currentTier)

    // 3 deaths on same ch_16 trap triggers ASSISTED at retry boundary
    repeat(3) {
      adaptive.recordDeath("ch_16", causeKey = "trap_1_TELEGRAPHED_RUNE_BEAM")
    }
    val retryTier = adaptive.evaluateRetryBoundary("ch_16", isAlreadyCompletedChallenge = false)
    assertEquals("ch_16 should step down to ASSISTED after repeated deaths", AdaptiveTier.ASSISTED, retryTier)

    // Assisted clear steps back to STANDARD (with 1-challenge escalation cooldown),
    // then strong zero-death clears on ch_17..ch_19 escalate cleanly to ESCALATED
    val postCh16 = adaptive.evaluateTransitionBoundary("ch_16", "ch_17", deathsInChallenge = 3, timeMs = 30_000L, parTimeSeconds = 32, lockedTierUsedForAttempt = AdaptiveTier.ASSISTED)
    assertEquals(AdaptiveTier.STANDARD, postCh16)
    val postCh17 = adaptive.evaluateTransitionBoundary("ch_17", "ch_18", deathsInChallenge = 0, timeMs = 15_000L, parTimeSeconds = 34, lockedTierUsedForAttempt = postCh16)
    assertEquals(AdaptiveTier.STANDARD, postCh17)
    val postCh18 = adaptive.evaluateTransitionBoundary("ch_18", "ch_19", deathsInChallenge = 0, timeMs = 14_000L, parTimeSeconds = 34, lockedTierUsedForAttempt = postCh17)
    val postCh19 = adaptive.evaluateTransitionBoundary("ch_19", "ch_20", deathsInChallenge = 0, timeMs = 15_000L, parTimeSeconds = 35, lockedTierUsedForAttempt = postCh18)
    assertTrue("Consecutive strong clears must allow escalation", postCh19.tierIndex >= AdaptiveTier.ESCALATED.tierIndex)

    // 2. ChaosModifiers on ch_21..ch_24: NO_CHECKPOINT keeps checkpoint dormant; SPEED_RUN sets finite clock;
    //    TRAP_RUSH honors TrapFairnessPolicy; saving modifier/mastery records preserves normal progression.
    val ch21NoCp = createEngine("ch_21", modifiers = setOf(ChaosModifier.NO_CHECKPOINT))
    val bell = ch21NoCp.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    ch21NoCp.readWorldStateLocked {
      ch21NoCp.player.x = bell.x + 4f
      ch21NoCp.player.y = bell.y + 4f
    }
    ch21NoCp.update()
    assertFalse("NO_CHECKPOINT modifier must suppress shrine bell activation", ch21NoCp.hasCheckpointActivated)

    for (entry in ChallengeCatalog.entries.filter { it.orderIndex >= 13 }) {
      val lvl = entry.buildLevel()
      val speedLimitMs = ChaosModifier.computeSpeedRunLimitMs(lvl.parTimeSeconds)
      assertTrue("${entry.challengeId} SPEED_RUN clock must stay >= 15s", speedLimitMs >= 15_000L)
      assertTrue("${entry.challengeId} TRAP_RUSH fairness check", TrapFairnessPolicy.validateChallengeTraps(entry, AdaptiveTier.RELENTLESS).all { it.isFair })
    }

    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    val repo = SaveRepository(app)
    val baselineState = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = true,
        isRunInForeground = true,
        isRunCompleted = false,
        activeChallengeId = "ch_18",
        activeChallengeSeed = 1018L,
        activeConfigVersion = ChallengeCatalog.CONFIG_VERSION,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 1,
        completedChallengeIds = (1..17).map { "ch_${it.toString().padStart(2, '0')}" }.toSet(),
        challengeRecords = mapOf(
            "ch_17" to ChallengeRecord("ch_17", completed = true, bestTimeMs = 20_000L, bestScore = 750, stars = 2, leastDeaths = 1)
        ),
        cumulativeRunTimerMs = 250_000L,
        cumulativeDeaths = 9,
        cumulativeScore = 14_000,
        bestRunTimeMs = 0L,
        bestRunScore = 0,
        bestRunLeastDeaths = -1
    )
    assertTrue(repo.saveContinuousRunState(baselineState, syncCommit = true))

    // Save Chaos Modifier record on ch_17 & Mastery stars on ch_24 -> normal run & ch_17 normal record stay untouched!
    val mods = setOf(ChaosModifier.SPEED_RUN, ChaosModifier.TRAP_RUSH)
    val modKey = ChaosModifier.canonicalKey(mods)
    assertTrue(
        repo.saveModifierChallengeRecord(
            challengeId = "ch_17",
            modifiers = mods,
            timeMs = 11_000L,
            score = 1500,
            deaths = 0,
            syncCommit = true
        )
    )
    val earnedMastery = MasteryCatalog.evaluateAttempt(
        ChallengeAttemptMetrics(
            challengeId = "ch_24",
            completed = true,
            timeMs = 22_000L,
            deaths = 0,
            coinsCollected = 5,
            totalCoins = 5,
            usedCheckpoint = true,
            score = 1200
        )
    )
    assertEquals(3, earnedMastery.size)
    assertTrue(repo.saveMasteryObjectivesEarned("ch_24", earnedMastery, syncCommit = true))

    val restoredState = repo.loadContinuousRunState()
    assertEquals("ch_18", restoredState.activeChallengeId)
    assertEquals(20_000L, restoredState.challengeRecords["ch_17"]?.bestTimeMs)
    assertEquals(750, restoredState.challengeRecords["ch_17"]?.bestScore)
    val retention = repo.loadPhase4RetentionSnapshot()
    assertEquals(11_000L, retention.modifierRecords["ch_17::$modKey"]?.bestTimeMs)
    assertEquals(3, retention.totalMasteryStars)
  }
}
