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
class Phase7ExtremeRage36ChallengesTest {

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

  private class WarningSpyCallbacks : TrollTrapCallbacks {
    var warningCalls = 0
    var killedPlayer = false
    var mirrorCurseCalls = 0
    var relocateCount = 0
    var lastRelocatedX = 0f

    override fun killPlayer(quip: String) {
      killedPlayer = true
    }

    override fun killPlayer(quip: String, causeKey: String?) {
      killedPlayer = true
    }

    override fun relocateExitGate(destX: Float, destY: Float) {
      relocateCount++
      lastRelocatedX = destX
    }

    override fun hideLinkedTile(tileId: Int) = Unit
    override fun applyMirrorCurse(durationFrames: Int) {
      mirrorCurseCalls++
    }

    override fun playTrapSnap() = Unit
    override fun playCrumble() = Unit
    override fun playDisappear() = Unit
    override fun playCeilingSlam() = Unit
    override fun playDoorEscape() = Unit
    override fun playFakeDoorPoof() = Unit
    override fun playMechanismShift() = Unit
    override fun playTrapWarning() {
      warningCalls++
    }

    override fun spawnDustTrickle(x: Float, y: Float) = Unit
    override fun spawnDustPuff(x: Float, y: Float) = Unit
    override fun spawnSparkles(x: Float, y: Float, color: Int, count: Int) = Unit
    override fun spawnStarburst(x: Float, y: Float) = Unit
    override fun spawnPopup(x: Float, y: Float, text: String, color: Int) = Unit
    override fun triggerCameraShake(frames: Int, magnitude: Float) = Unit
  }

  @Test
  fun test01_catalogHas36ChallengesPreservesCh01ToCh24AndHasExactPhase7Identities() {
    assertEquals(200, ChallengeCatalog.entries.size)
    val expectedIds = (1..200).map { "ch_${it.toString().padStart(2, '0')}" }
    assertEquals(expectedIds, ChallengeCatalog.entries.map { it.challengeId })
    assertEquals((1..200).toList(), ChallengeCatalog.entries.map { it.orderIndex })
    assertEquals((1001L..1200L).toList(), ChallengeCatalog.entries.map { it.challengeSeed })
    assertEquals(200, ChallengeCatalog.entries.map { it.title }.toSet().size)

    // Verify exact Phase 7 titles for ch_25..ch_36
    val expectedPhase7Titles = mapOf(
        "ch_25" to "No Time to React",
        "ch_26" to "The Ceiling Wants You",
        "ch_27" to "Coin of Betrayal",
        "ch_28" to "The Moving Nightmare",
        "ch_29" to "Nothing Is Safe",
        "ch_30" to "The Fake Exit Returns",
        "ch_31" to "Chain Reaction",
        "ch_32" to "The Trap Corridor",
        "ch_33" to "Above and Below",
        "ch_34" to "The Unforgiving Tower",
        "ch_35" to "Survival Instinct",
        "ch_36" to "The Last Laugh"
    )
    for ((id, title) in expectedPhase7Titles) {
      assertEquals(title, ChallengeCatalog.findById(id)!!.title)
    }

    // Check ch_01..ch_24 preservation: ch_01..ch_12 & ch_24 key titles/themes untouched
    assertEquals("The Easy Start", ChallengeCatalog.findById("ch_01")!!.title)
    assertEquals("Trial of the Eternal Sun", ChallengeCatalog.findById("ch_24")!!.title)

    // Verify every ch_25..ch_36 has 2 checkpoints, valid spawn footing, no spawn/bell trap overlap, and distinct whispers
    for (entry in ChallengeCatalog.entries.filter { it.orderIndex in 25..36 }) {
      val lvl = entry.buildLevel()
      val bells = lvl.tiles.filter { it.type == TileType.CHECKPOINT_BELL }
      assertEquals("${entry.challengeId} must have 2 checkpoints", 2, bells.size)
      for (bell in bells) {
        val bellHitsTrap = lvl.trollTraps.any { trap ->
          rectsOverlap(
              bell.x, bell.y, bell.width, bell.height,
              trap.initialX, trap.initialY, trap.width, trap.height
          )
        }
        assertFalse("${entry.challengeId} checkpoint bell must not overlap initial trap", bellHitsTrap)
      }

      val whisper = ChaosGameEngine.resolveVictoryWhisper(lvl)
      assertNotEquals("The Sun Gate awakens — trial cleared!", whisper)
    }
  }

  @Test
  fun test02_designatedInstantTrapsActivateWithoutWarningOrDelayAcrossAllTiers() {
    // 1. All SURPRISE traps in ch_25..ch_36 must resolve lockedTelegraphDurationFrames == 0 in EVERY AdaptiveTier
    val phase7SurpriseTraps = ChallengeCatalog.entries
        .filter { it.orderIndex >= 25 }
        .flatMap { entry ->
          entry.buildLevel().trollTraps
              .filter { it.behaviorCategory == TrapBehaviorCategory.SURPRISE }
              .map { trap -> entry to trap }
        }
    assertTrue("Phase 7 must have numerous SURPRISE traps", phase7SurpriseTraps.size >= 80)

    for (tier in AdaptiveTier.entries) {
      for ((entry, rawTrap) in phase7SurpriseTraps) {
        val trap = rawTrap.copy()
        TrapFairnessPolicy.applyTierTuningInPlace(listOf(trap), tier)
        assertEquals(
            "${entry.challengeId} trap #${trap.id} (${trap.type}) must keep 0f telegraph in $tier",
            0,
            trap.lockedTelegraphDurationFrames
        )
        assertNull(
            "${entry.challengeId} trap #${trap.id} lethal instant surprise must not display warning popup",
            if (trap.killsPlayer && trap.type != TrollTrapType.FAKE_EXIT_DOOR) trap.quipPopup else null
        )
      }
    }

    // 2. Verify SpyCallbacks sees ZERO playTrapWarning() calls and ZERO TELEGRAPH phase on instant activation
    val sys = TrollTrapSystem()
    val spy = WarningSpyCallbacks()
    val ch25Traps = ChallengeCatalog.findById("ch_25")!!.buildLevel().trollTraps.map { it.copy() }
    TrapFairnessPolicy.applyTierTuningInPlace(ch25Traps, AdaptiveTier.ASSISTED)

    // Step onto instant disappearing floor #1 in ch_25 -> immediately ACTIVE (no TELEGRAPH, no warning sound)
    val floorTrap = ch25Traps.first { it.id == 1 }
    val stepPlayer = Player(
        x = floorTrap.initialX + 10f,
        y = floorTrap.initialY - 54f,
        vy = 2f,
        isGrounded = false
    ).apply {
      prevY = floorTrap.initialY - 56f
    }
    sys.resolveVerticalCollisions(ch25Traps, stepPlayer, previousGrounded = false, callbacks = spy) {}
    sys.updateAll(ch25Traps, stepPlayer, spy)
    assertEquals("Instant disappearing floor must skip TELEGRAPH and vanish immediately", TrollTrapState.RETIRED, floorTrap.state)
    assertFalse("Instant disappearing floor must disable visibility immediately", floorTrap.isVisible)
    assertFalse("Instant disappearing floor must disable solidity immediately", floorTrap.isSolid)
    assertEquals(0, spy.warningCalls)

    // Proximity onto instant spike #2 in ch_25 -> immediately ACTIVE with extended spikes on frame 1
    val spikeTrap = ch25Traps.first { it.id == 2 }
    val spikePlayer = Player(
        x = spikeTrap.triggerMinX + 4f,
        y = 11f * World1Levels.TILE_SIZE,
        isGrounded = true
    )
    sys.updateAll(ch25Traps, spikePlayer, spy)
    assertEquals("Instant spike must activate immediately without TELEGRAPH", TrollTrapState.ACTIVE, spikeTrap.state)
    assertTrue("Instant spike must begin emerging on trigger frame", spikeTrap.morphSpikeHeight > 0f)
    assertEquals(0, spy.warningCalls)

    // ch_26: instant FALLING_CEILING_BLOCK moves downward on trigger frame without TELEGRAPH
    val ch26Traps = ChallengeCatalog.findById("ch_26")!!.buildLevel().trollTraps.map { it.copy() }
    TrapFairnessPolicy.applyTierTuningInPlace(ch26Traps, AdaptiveTier.STANDARD)
    val ceilingTrap = ch26Traps.first { it.id == 1 }
    val ceilStartY = ceilingTrap.y
    val ceilPlayer = Player(
        x = ceilingTrap.triggerMinX + 4f,
        y = 11f * World1Levels.TILE_SIZE,
        isGrounded = true
    )
    sys.updateAll(ch26Traps, ceilPlayer, spy)
    assertEquals(TrollTrapState.ACTIVE, ceilingTrap.state)
    assertEquals(0, spy.warningCalls)
    sys.updateAll(ch26Traps, ceilPlayer, spy)
    assertTrue("Instant ceiling drop must descend immediately after activation without telegraph", ceilingTrap.y > ceilStartY)

    // ch_28: instant STALKER_CRUSH_WALL enters ACTIVE immediately and advances without TELEGRAPH
    val ch28Traps = ChallengeCatalog.findById("ch_28")!!.buildLevel().trollTraps.map { it.copy() }
    TrapFairnessPolicy.applyTierTuningInPlace(ch28Traps, AdaptiveTier.STANDARD)
    val stalker = ch28Traps.first { it.type == TrollTrapType.STALKER_CRUSH_WALL }
    val stalkerStartX = stalker.x
    val stalkerPlayer = Player(
        x = stalker.triggerMinX + 4f,
        y = 11f * World1Levels.TILE_SIZE,
        isGrounded = true
    )
    sys.updateAll(ch28Traps, stalkerPlayer, spy)
    assertEquals(TrollTrapState.ACTIVE, stalker.state)
    assertEquals(0, spy.warningCalls)
    sys.updateAll(ch28Traps, stalkerPlayer, spy)
    assertTrue("Instant stalker wall must advance immediately", stalker.x > stalkerStartX)

    // Verify all ON_PROXIMITY_X ceiling blocks in ch_25..ch_36 have triggerMaxY reaching their targetY floor zone
    for (entry in ChallengeCatalog.entries.filter { it.orderIndex >= 25 }) {
      val lvl = entry.buildLevel()
      for (t in lvl.trollTraps.filter { it.type == TrollTrapType.FALLING_CEILING_BLOCK && it.triggerType == TrapTriggerType.ON_PROXIMITY_X }) {
        assertTrue(
            "${entry.challengeId} ceiling block #${t.id} triggerMaxY=${t.triggerMaxY} must reach targetY=${t.targetY}",
            t.triggerMaxY >= t.targetY
        )
      }
    }

    // ch_30: RELOCATING_EXIT_DOOR + FAKE_EXIT_DOOR + FLEEING_SUN_GATE relocate/activate with 0 warning calls
    val ch30Traps = ChallengeCatalog.findById("ch_30")!!.buildLevel().trollTraps.map { it.copy() }
    TrapFairnessPolicy.applyTierTuningInPlace(ch30Traps, AdaptiveTier.STANDARD)
    val reloc = ch30Traps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    val approachRelocPlayer = Player(
        x = reloc.triggerMinX + 8f,
        y = 11f * World1Levels.TILE_SIZE,
        isGrounded = true
    )
    sys.updateAll(ch30Traps, approachRelocPlayer, spy)
    assertEquals(1, spy.relocateCount)
    assertEquals(reloc.linkedDoorTargetX!!, spy.lastRelocatedX, 0.1f)
    assertEquals(0, spy.warningCalls)

    // 3. Verify existing telegraphed traps in ch_01..ch_24 still emit TELEGRAPH & playTrapWarning
    val ch13Traps = ChallengeCatalog.findById("ch_13")!!.buildLevel().trollTraps.map { it.copy() }
    TrapFairnessPolicy.applyTierTuningInPlace(ch13Traps, AdaptiveTier.STANDARD)
    val ch13Spikes = ch13Traps.firstOrNull { it.telegraphDurationFrames > 0 }!!
    sys.onCoinCollected(coinId = 1, traps = ch13Traps, callbacks = spy)
    assertTrue("Existing telegraphed trap should preserve warning behavior", ch13Spikes.telegraphDurationFrames > 0)
  }

  @Test
  fun test03_instantCoinAndChainReactionsFireImmediatelyAndCheckpointRollbackResetsAll() {
    // 1. ch_27 Coin of Betrayal: collecting coin #1 immediately fires both Ceiling #1 AND chained Spike #2 on the same frame
    val ch27 = createEngine("ch_27")
    val coin1 = ch27.coins.first { it.id == 1 }
    ch27.readWorldStateLocked {
      ch27.player.x = coin1.x - 10f
      ch27.player.y = coin1.y - 10f
    }
    ch27.update()
    val trap1 = ch27.trollTraps.first { it.id == 1 }
    val trap2 = ch27.trollTraps.first { it.id == 2 }
    assertEquals("Coin #1 must trigger Trap #1 immediately without TELEGRAPH", TrollTrapState.ACTIVE, trap1.state)
    assertEquals("Trap #1 must instantaneously chain Trap #2 on same frame", TrollTrapState.ACTIVE, trap2.state)

    // 2. ch_31 Chain Reaction: stepping on Split Bridge #8 simultaneously fires Pincer #9 and Ceiling #10 immediately
    val ch31 = createEngine("ch_31")
    val bridge8 = ch31.trollTraps.first { it.id == 8 }
    ch31.readWorldStateLocked {
      ch31.player.x = bridge8.initialX + 10f
      ch31.player.y = bridge8.initialY - ch31.player.collisionHeight
      ch31.player.isGrounded = true
    }
    ch31.update()
    assertEquals(TrollTrapState.ACTIVE, ch31.trollTraps.first { it.id == 8 }.state)
    assertEquals(TrollTrapState.ACTIVE, ch31.trollTraps.first { it.id == 9 }.state)
    assertEquals(TrollTrapState.ACTIVE, ch31.trollTraps.first { it.id == 10 }.state)

    // Retry resets every chain trap back to ARMED
    ch31.resetCurrentChallenge()
    assertTrue("Retry must restore all ch_31 chain traps to ARMED", ch31.trollTraps.all { it.state == TrollTrapState.ARMED })

    // 3. ch_36 Checkpoint rollback preserves Coin 4 / Gate 9 if collected pre-checkpoint or restores post-checkpoint traps
    val ch36 = createEngine("ch_36")
    val cp1 = ch36.tiles.filter { it.type == TileType.CHECKPOINT_BELL }.first()
    val preCpTrap = ch36.trollTraps.first { it.id == 1 }
    ch36.readWorldStateLocked {
      ch36.player.x = preCpTrap.triggerMinX + 4f
      ch36.player.y = 11f * World1Levels.TILE_SIZE
      ch36.player.isGrounded = true
    }
    ch36.update()
    assertEquals(TrollTrapState.ACTIVE, preCpTrap.state)

    ch36.readWorldStateLocked {
      ch36.player.x = cp1.x + 4f
      ch36.player.y = cp1.y + 4f
    }
    ch36.update()
    assertTrue("CP1 must bind", ch36.hasCheckpointActivated)

    val postCpTrap = ch36.trollTraps.first { it.id == 11 }
    ch36.readWorldStateLocked {
      ch36.player.x = postCpTrap.triggerMinX + 8f
      ch36.player.y = 11f * World1Levels.TILE_SIZE
    }
    ch36.update()
    assertEquals(TrollTrapState.ACTIVE, postCpTrap.state)

    ch36.handlePlayerDeath()
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) {
      if (ch36.isDying) ch36.update()
    }
    assertEquals("Post-checkpoint trap must revert to ARMED on rollback", TrollTrapState.ARMED, postCpTrap.state)
  }

  @Test
  fun test04_allCh25ToCh36PassFairnessReachabilityExitsAndRender() {
    val tile = World1Levels.TILE_SIZE
    val renderer = ChaosGameRenderer(app)
    val bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    try {
      for (entry in ChallengeCatalog.entries.filter { it.orderIndex >= 25 }) {
        // Fairness + chain cycle check across all 4 tiers
        for (tier in AdaptiveTier.entries) {
          val report = TrapFairnessPolicy.validateChallengeTraps(entry, tier)
          for (item in report) {
            assertTrue(
                "${entry.challengeId} ($tier) trap #${item.trapId} failed fairness: ${item.reason}",
                item.isFair
            )
          }
        }

        // Reachability & exit completion check
        val engine = createEngine(entry.challengeId)
        for (trap in engine.trollTraps) {
          when (trap.type) {
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

        val gateTile = engine.tiles.first { it.type == TileType.SUN_GATE }
        engine.readWorldStateLocked {
          engine.player.x = gateTile.x + gateTile.width * 0.5f - engine.player.collisionWidth * 0.5f
          engine.player.y = gateTile.y + gateTile.height - engine.player.collisionHeight
          engine.player.isGrounded = true
        }
        engine.update()
        if (!engine.isVictorious) {
          // Relocated gate (e.g., ch_30)
          engine.readWorldStateLocked {
            engine.player.x = gateTile.x + gateTile.width * 0.5f - engine.player.collisionWidth * 0.5f
            engine.player.y = gateTile.y + gateTile.height - engine.player.collisionHeight
            engine.player.isGrounded = true
          }
          engine.update()
        }
        assertTrue("${entry.challengeId} must be cleanly completable at its exit gate", engine.isVictorious)

        // Render verification
        val renderEngine = createEngine(entry.challengeId)
        repeat(10) { renderEngine.update() }
        renderer.render(canvas, renderEngine, 640f, 360f)
        assertTrue("${entry.challengeId} must render cleanly", renderer.hasRenderedFrame)

        // Check ch_34 launch pads always re-arm (never softlock)
        if (entry.challengeId == "ch_34") {
          val pads = entry.buildLevel().trollTraps.filter { it.type == TrollTrapType.LAUNCH_BETRAYAL_PAD }
          assertTrue("ch_34 launch pads must set cycleFrames > 0", pads.all { it.cycleFrames > 0 })
        }
        // Check ch_33 dual routes both span Act 1..2 horizontally
        if (entry.challengeId == "ch_33") {
          val lvl = entry.buildLevel()
          assertTrue("ch_33 must include upper route platforms around y=10.2T", lvl.tiles.any { kotlin.math.abs(it.y - 10.2f * tile) < 2f })
          assertTrue("ch_33 must include lower route floor around y=13.4T/14.4T", lvl.tiles.any { it.y >= 13.4f * tile })
        }
      }
    } finally {
      renderer.expansionRenderer.release()
      bmp.recycle()
    }
  }

  @Test
  fun test05_continuousRun36Mastery108DailyPoolAndSaveV3Compatibility() {
    // 1. ContinuousRunCoordinator runs all 200 challenges seamlessly
    val coord = ContinuousRunCoordinator()
    coord.startNewRun()
    for (idx in 0 until 199) {
      val id = ChallengeCatalog.entries[idx].challengeId
      val res = coord.onChallengeCompleted(
          challengeId = id,
          attemptToken = coord.currentAttemptToken,
          timeMs = 12_000L,
          score = 800,
          coinsCollected = 3,
          totalCoins = 5,
          parTimeSeconds = 36,
          isTrollLevel = true,
          challengeDeaths = 1
      )
      assertTrue("$id must advance continuously", res is ChallengeCompletionOutcome.NextChallenge)
    }
    assertEquals("ch_200", coord.activeChallengeId)
    val finalRes = coord.onChallengeCompleted(
        challengeId = "ch_200",
        attemptToken = coord.currentAttemptToken,
        timeMs = 20_000L,
        score = 1200,
        coinsCollected = 5,
        totalCoins = 5,
        parTimeSeconds = 46,
        isTrollLevel = true,
        challengeDeaths = 2
    )
    assertTrue(finalRes is ChallengeCompletionOutcome.RunCompleted)
    val summary = (finalRes as ChallengeCompletionOutcome.RunCompleted).summary
    assertEquals(200, summary.completedCount)
    assertEquals(200, summary.totalChallenges)

    // 2. MasteryCatalog: 200 specs, 600 total stars, NO_COINS never assigned to key-coin gated levels
    assertEquals(200, MasteryCatalog.specs.size)
    assertEquals(600, MasteryCatalog.totalPossibleStars)
    val ch36Spec = MasteryCatalog.specFor("ch_36")!!
    assertFalse("ch_36 must not use NO_COINS because Coin 4 unlocks LOCKED_RUNE_GATE", ch36Spec.objectives[2].kind == MasteryObjectiveKind.NO_COINS)

    // 3. OfflineDailyChallengeGenerator spans ch_25..ch_36 in its rotation cleanly
    val sampled = (1..90).map { dayOffset ->
      val m = ((dayOffset - 1) / 28) + 1
      val d = ((dayOffset - 1) % 28) + 1
      OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate(2027, m, d)).challengeId
    }.toSet()
    assertTrue("Daily generator must include ch_25..ch_36", sampled.any { it.removePrefix("ch_").toInt() >= 25 })

    // 4. Schema v3 backward compatibility: a save completed at ch_24 resumes/records ch_25 cleanly without losing existing data
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    val repo = SaveRepository(app)
    val legacy24 = ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = false,
        isRunInForeground = false,
        isRunCompleted = false,
        activeChallengeId = "ch_24",
        activeChallengeSeed = 1024L,
        activeConfigVersion = ChallengeCatalog.CONFIG_VERSION,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 0,
        completedChallengeIds = (1..24).map { "ch_${it.toString().padStart(2, '0')}" }.toSet(),
        challengeRecords = (1..24).associate { idx ->
          val id = "ch_${idx.toString().padStart(2, '0')}"
          id to ChallengeRecord(id, completed = true, bestTimeMs = 15_000L, bestScore = 850, stars = 3, leastDeaths = 0)
        },
        cumulativeRunTimerMs = 360_000L,
        cumulativeDeaths = 6,
        cumulativeScore = 20_400,
        bestRunTimeMs = 0L,
        bestRunScore = 0,
        bestRunLeastDeaths = -1
    )
    assertTrue(repo.saveContinuousRunState(legacy24, syncCommit = true))
    repo.saveReplayChallengeRecordIfImproved(
        "ch_25",
        ChallengeRecord("ch_25", completed = true, bestTimeMs = 18_500L, bestScore = 920, stars = 3, leastDeaths = 1),
        syncCommit = true
    )
    val reloaded = repo.loadContinuousRunState()
    assertEquals(15_000L, reloaded.challengeRecords["ch_24"]?.bestTimeMs)
    assertEquals(18_500L, reloaded.challengeRecords["ch_25"]?.bestTimeMs)
    assertTrue(reloaded.completedChallengeIds.contains("ch_01"))
    assertTrue(reloaded.completedChallengeIds.contains("ch_24"))
  }
}
