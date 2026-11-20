package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase1ChallengeResetAndFoundationTest {

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
  }

  private fun finishDeathAnimation(engine: ChaosGameEngine, expectedFrames: Int = ChaosGameEngine.DEFAULT_DEATH_ANIM_FRAMES) {
    assertTrue("Engine should enter dying state", engine.isDying)
    repeat(expectedFrames) {
      if (engine.isDying) engine.update()
    }
    assertFalse("Engine must complete instant respawn after $expectedFrames frames", engine.isDying)
  }

  @Test
  fun test01_challengeCatalogMappingAndLockedConfigParity() {
    assertEquals(5, ChallengeCatalog.entries.size)
    val expectedIds = listOf("ch_01", "ch_02", "ch_03", "ch_04", "ch_05")
    assertEquals(expectedIds, ChallengeCatalog.entries.map { it.challengeId })
    assertEquals(listOf(1, 2, 3, 4, 5), ChallengeCatalog.entries.map { it.orderIndex })
    assertEquals(5, ChallengeCatalog.entries.map { it.challengeSeed }.toSet().size)

    for (levelId in 1..5) {
      val challengeId = "ch_0$levelId"
      val byId = ChallengeCatalog.findById(challengeId)
      val byLegacy = ChallengeCatalog.findByLegacyLevel(1, levelId)
      assertNotNull(byId)
      assertEquals(byId, byLegacy)

      val locked = ChallengeCatalog.createLockedConfig(challengeId, AdaptiveTier.STANDARD)!!
      val fromLegacy = ChallengeCatalog.fromLegacyLevel(1, levelId, AdaptiveTier.STANDARD)!!
      assertEquals(locked.challengeId, fromLegacy.challengeId)
      assertEquals(locked.challengeSeed, fromLegacy.challengeSeed)
      assertEquals(ChallengeCatalog.CONFIG_VERSION, locked.configVersion)
      assertEquals(AdaptiveTier.STANDARD, locked.selectedAdaptiveTier)

      val legacyLevel = World1Levels.createLevel(levelId, DifficultyTier.NORMAL)
      assertEquals(legacyLevel.title, locked.levelDefinition.title)
      assertEquals(legacyLevel.spawnX, locked.levelDefinition.spawnX, 0.001f)
      assertEquals(legacyLevel.spawnY, locked.levelDefinition.spawnY, 0.001f)
      assertEquals(legacyLevel.exitX, locked.levelDefinition.exitX, 0.001f)
      assertEquals(legacyLevel.exitY, locked.levelDefinition.exitY, 0.001f)
      assertEquals(legacyLevel.tiles.size, locked.levelDefinition.tiles.size)
      assertEquals(legacyLevel.coins.size, locked.levelDefinition.coins.size)
      assertEquals(legacyLevel.trollTraps.size, locked.levelDefinition.trollTraps.size)
    }

    // Verify AdaptiveTier progression metadata ordering
    assertEquals(0, AdaptiveTier.ASSISTED.tierIndex)
    assertEquals(1, AdaptiveTier.STANDARD.tierIndex)
    assertEquals(2, AdaptiveTier.ESCALATED.tierIndex)
    assertEquals(3, AdaptiveTier.RELENTLESS.tierIndex)
    assertNull(ChallengeCatalog.findById("ch_99"))
  }

  @Test
  fun test02_challengeSpawnRetryResetsAllMutableStateAndPreservesCumulativeCounters() {
    val t = World1Levels.TILE_SIZE
    val level = LevelDefinition(
        worldId = 1,
        levelId = 2,
        title = "Synthetic Reset Validation",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 12f * t, width = 20f * t, height = t, type = TileType.SOLID_EARTH),
            GameTile(id = 2, x = 20f * t, y = 12f * t, width = t, height = t, type = TileType.DISAPPEARING_FLOOR),
            GameTile(id = 3, x = 21f * t, y = 12f * t, width = t, height = t, type = TileType.CRUMBLING_WOOD),
            GameTile(id = 4, x = 22f * t, y = 12f * t, width = t, height = t, type = TileType.ECHO_BRIDGE, isSolid = false, groupId = 7),
            GameTile(id = 5, x = 25f * t, y = 10f * t, width = t, height = 2f * t, type = TileType.SOUND_GATE, isSolid = true, groupId = 7),
            GameTile(id = 6, x = 30f * t, y = 9f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = listOf(Coin(id = 1, x = 6f * t, y = 11f * t)),
        movingPlatforms = listOf(
            MovingPlatform(id = 1, x = 10f * t, y = 10f * t, minX = 9f * t, maxX = 12f * t, centerX = 10.5f * t, amplitude = 48f, periodFrames = 120)
        ),
        spawnX = 2f * t,
        spawnY = 10f * t,
        exitX = 30f * t,
        exitY = 9f * t,
        bounceDrums = listOf(BounceDrum(id = 1, x = 7f * t, y = 11.5f * t)),
        critters = listOf(ForestCritter(id = 1, x = 14f * t, y = 12f * t - 26f, minX = 12f * t, maxX = 16f * t)),
        pendulums = listOf(SwingingPendulum(id = 1, anchorX = 16f * t, anchorY = 4f * t, length = 120f)),
        diyas = listOf(ForestDiya(id = 1, x = 8f * t, y = 11f * t)),
        fallingHazards = listOf(FallingHazard(id = 1, x = 11f * t, startY = 2f * t, groundY = 12f * t, triggerMinX = 10f * t, triggerMaxX = 12f * t)),
        ambushTraps = listOf(AmbushSpikeTrap(id = 1, x = 13f * t, groundY = 12f * t)),
        soundSwitches = listOf(SoundSwitchNode(id = 1, groupId = 7, x = 5f * t, y = 11f * t)),
        orbitingPlatforms = listOf(OrbitingPlatform(id = 1, pivotX = 18f * t, pivotY = 8f * t, orbitRadius = 60f)),
        collapsingPillars = listOf(CollapsingPillar(id = 1, x = 19f * t, topY = 9f * t)),
        lightningHazards = listOf(LightningHazard(id = 1, x = 15f * t, topY = 2f * t, groundY = 12f * t, cycleFrames = 120, phaseOffsetFrames = 10)),
        relics = listOf(LevelRelic(id = "rel_test", levelId = 2, title = "Test Relic", hint = "Hint", x = 4f * t, y = 11f * t)),
        trollTraps = listOf(
            TrollTrap(
                id = 1,
                type = TrollTrapType.DELAYED_CRUMBLE,
                triggerType = TrapTriggerType.ON_STEP,
                initialX = 9f * t,
                initialY = 11f * t,
                width = 2f * t,
                height = t
            ),
            TrollTrap(
                id = 2,
                type = TrollTrapType.RELOCATING_EXIT_DOOR,
                triggerType = TrapTriggerType.ON_APPROACH_EXIT,
                initialX = 29f * t,
                initialY = 9f * t,
                width = 2f * t,
                height = 3f * t,
                linkedDoorTargetX = 36f * t,
                linkedDoorTargetY = 9f * t
            )
        ),
        isTrollLevel = true
    )

    val locked = ChallengeDefinition(
        challengeId = "ch_02",
        orderIndex = 2,
        title = level.title,
        subtitle = "Test",
        theme = level.theme,
        challengeSeed = 2002L,
        levelFactory = { level }
    ).lockConfig(AdaptiveTier.STANDARD)

    val engine = ChaosGameEngine(
        level = level,
        audioEngine = ProceduralAudioEngine(),
        lockedChallengeConfig = locked,
        initialCompletedChallengeIds = setOf("ch_01"),
        initialCumulativeRunTimerMs = 12_500L,
        initialCumulativeDeathCount = 3
    )

    // Advance engine & mutate every mutable entity
    engine.inputRight = true
    repeat(12) { engine.update() }
    engine.coins.first().isCollected = true
    engine.diyas.first().isLit = true
    engine.relics.first().isCollected = true
    engine.bounceDrums.first().squashFrames = 9
    engine.soundSwitches.first().apply { isActivated = true; remainingFrames = 120; pulseAnimFrames = 10 }
    engine.tiles.first { it.type == TileType.ECHO_BRIDGE }.apply { isSolid = true; isTriggered = true }
    engine.tiles.first { it.type == TileType.SOUND_GATE }.apply { isSolid = false; isVisible = false; isTriggered = true }
    engine.tiles.first { it.type == TileType.DISAPPEARING_FLOOR }.apply { isSolid = false; isVisible = false; isTriggered = true }
    engine.tiles.first { it.type == TileType.CRUMBLING_WOOD }.apply { isCrumbling = true; crumbleTimerFrames = 4 }
    engine.collapsingPillars.first().apply { currentDropY = 48f; phase = TrapPhase.ACTIVE; timerFrames = 12 }
    engine.fallingHazards.first().apply { y = 400f; isFalling = true; hasTriggered = true }
    engine.ambushTraps.first().apply { phase = TrapPhase.ACTIVE; currentHeight = 20f; phaseTimerFrames = 15 }
    engine.critters.first().apply { stunFrames = 50; x = 15f * t }
    engine.trollTraps[0].apply { state = TrollTrapState.RETIRED; isSolid = false; isVisible = false }
    engine.trollTraps[1].apply { state = TrollTrapState.RETIRED; hasRelocated = true }
    engine.tiles.first { it.type == TileType.SUN_GATE }.apply { x = 36f * t; y = 9f * t }
    engine.player.coyoteTimer = 4
    engine.player.jumpBufferTimer = 5
    engine.player.hasJumpCutApplied = true
    engine.postNotification("TEST_NOTIF", "Warning", "Pre-death alert", NotificationCategory.EVENT_WARNING, NotificationPriority.CRITICAL)
    val preDeathChallengeTimer = engine.challengeTimerMs
    assertTrue(preDeathChallengeTimer > 0L)

    // Trigger pit death (clearInputs = false) to verify challenge reset with persistent cumulative counters
    engine.handlePlayerDeath()

    // Player & attempt state fully reset
    assertEquals(level.spawnX, engine.player.x, 0.001f)
    assertEquals(level.spawnY, engine.player.y, 0.001f)
    assertEquals(level.spawnX, engine.player.prevX, 0.001f)
    assertEquals(level.spawnY, engine.player.prevY, 0.001f)
    assertEquals(0f, engine.player.vx, 0.001f)
    assertEquals(0f, engine.player.vy, 0.001f)
    assertFalse(engine.player.isGrounded)
    assertEquals(0, engine.player.coyoteTimer)
    assertEquals(0, engine.player.jumpBufferTimer)
    assertFalse(engine.player.hasJumpCutApplied)
    assertEquals(AaravState.IDLE, engine.player.state)

    // Attempt timer & camera shake MUST reset even when clearInputs = false
    assertEquals(0L, engine.challengeTimerMs)
    assertEquals(0L, engine.levelTimerMs)
    assertEquals(0, engine.cameraShakeFrames)
    assertEquals(0f, engine.cameraShakeMagnitude, 0.001f)
    assertTrue(engine.hasPendingCameraSnap())
    assertEquals(0, engine.score)
    assertEquals(1, engine.comboMultiplier)
    assertNull(engine.activeNotification)

    // World tiles, entities, hazards, switches & troll traps MUST reset
    assertTrue(engine.coins.none { it.isCollected })
    assertTrue(engine.diyas.none { it.isLit })
    assertTrue(engine.relics.none { it.isCollected })
    assertEquals(0, engine.bounceDrums.first().squashFrames)
    assertFalse(engine.soundSwitches.first().isActivated)
    assertEquals(0, engine.soundSwitches.first().remainingFrames)
    assertFalse(engine.tiles.first { it.type == TileType.ECHO_BRIDGE }.isSolid)
    assertTrue(engine.tiles.first { it.type == TileType.SOUND_GATE }.isSolid)
    assertTrue(engine.tiles.first { it.type == TileType.DISAPPEARING_FLOOR }.isSolid)
    assertFalse(engine.tiles.first { it.type == TileType.CRUMBLING_WOOD }.isCrumbling)
    assertEquals(0f, engine.collapsingPillars.first().currentDropY, 0.001f)
    assertEquals(TrapPhase.ARMED, engine.collapsingPillars.first().phase)
    assertFalse(engine.fallingHazards.first().hasTriggered)
    assertEquals(TrapPhase.ARMED, engine.ambushTraps.first().phase)
    assertEquals(0, engine.critters.first().stunFrames)
    assertEquals(0, engine.movingPlatforms.first().currentFrame)
    assertEquals(0, engine.orbitingPlatforms.first().currentFrame)
    assertEquals(0, engine.pendulums.first().currentFrame)
    assertEquals(10, engine.lightningHazards.first().currentFrame)
    assertTrue(engine.trollTraps.all { it.state == TrollTrapState.ARMED && !it.hasRelocated && it.isVisible })
    assertEquals(level.exitX, engine.currentExitX, 0.001f)
    assertEquals(level.exitX, engine.tiles.first { it.type == TileType.SUN_GATE }.x, 0.001f)

    // Cumulative progress & completed challenges MUST survive spawn retry
    assertTrue(engine.completedChallengeIds.contains("ch_01"))
    assertTrue(engine.cumulativeRunTimerMs > 12_500L)
    assertEquals(4, engine.cumulativeDeathCount)
    assertEquals(1, engine.deathCount)
  }

  @Test
  fun test03_genuineCheckpointRetryRestoresTrapSnapshotsAndRollsBackPostCheckpointChanges() {
    val t = World1Levels.TILE_SIZE
    val level = LevelDefinition(
        worldId = 1,
        levelId = 3,
        title = "Genuine Checkpoint Snapshot",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 12f * t, width = 35f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 12f * t, y = 10f * t, width = t, height = 2f * t, type = TileType.CHECKPOINT_BELL, isSolid = false),
            GameTile(id = 3, x = 28f * t, y = 9f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = listOf(
            Coin(id = 1, x = 6f * t, y = 11f * t),
            Coin(id = 2, x = 18f * t, y = 11f * t)
        ),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 10f * t,
        exitX = 28f * t,
        exitY = 9f * t,
        orbitingPlatforms = listOf(OrbitingPlatform(id = 1, pivotX = 16f * t, pivotY = 8f * t, orbitRadius = 48f)),
        pendulums = listOf(SwingingPendulum(id = 1, anchorX = 20f * t, anchorY = 5f * t, length = 96f)),
        lightningHazards = listOf(LightningHazard(id = 1, x = 22f * t, topY = 2f * t, groundY = 12f * t, cycleFrames = 140)),
        trollTraps = listOf(
            TrollTrap(
                id = 1,
                type = TrollTrapType.DISAPPEARING_FLOOR,
                triggerType = TrapTriggerType.ON_STEP,
                initialX = 8f * t,
                initialY = 11f * t,
                width = t,
                height = t,
                telegraphDurationFrames = 0
            ),
            TrollTrap(
                id = 2,
                type = TrollTrapType.SURPRISE_SPIKE_POP,
                triggerType = TrapTriggerType.ON_PROXIMITY_X,
                initialX = 16f * t,
                initialY = 11f * t,
                width = t,
                height = t,
                triggerMinX = 15f * t,
                triggerMaxX = 17f * t,
                telegraphDurationFrames = 6
            ),
            TrollTrap(
                id = 3,
                type = TrollTrapType.RELOCATING_EXIT_DOOR,
                triggerType = TrapTriggerType.ON_APPROACH_EXIT,
                initialX = 27f * t,
                initialY = 9f * t,
                width = 2f * t,
                height = 3f * t,
                triggerMinX = 25f * t,
                triggerMaxX = 29f * t,
                telegraphDurationFrames = 0,
                linkedDoorTargetX = 33f * t,
                linkedDoorTargetY = 9f * t
            )
        ),
        isTrollLevel = true
    )

    val engine = ChaosGameEngine(level, ProceduralAudioEngine())

    // Pre-checkpoint actions: solve/retire trap #1 and collect coin #1
    engine.coins[0].isCollected = true
    engine.trollTraps[0].apply {
      state = TrollTrapState.RETIRED
      isSolid = false
      isVisible = false
    }

    // Advance onto CHECKPOINT_BELL to record snapshot
    engine.player.x = 12f * t + 6f
    engine.player.y = 12f * t - engine.player.collisionHeight
    engine.update()

    assertTrue(engine.hasCheckpointActivated)
    val cpSnap = engine.checkpointSnapshot!!
    val cpTimer = cpSnap.challengeTimerMs
    val cpOrbFrame = cpSnap.orbitingPlatformSnapshots.first().currentFrame
    val cpPendFrame = cpSnap.pendulumSnapshots.first().currentFrame
    val cpLightFrame = cpSnap.lightningSnapshots.first().currentFrame
    assertEquals(2, cpSnap.trollTrapSnapshots.let { snaps ->
      assertEquals(3, snaps.size)
      snaps.count { it.state == TrollTrapState.ARMED }
    })

    // Post-checkpoint mutations: collect coin #2, trigger trap #2 & trap #3 (exit relocation)
    repeat(10) { engine.update() }
    engine.coins[1].isCollected = true
    engine.player.x = 16f * t
    engine.update() // Triggers trap #2 telegraph
    engine.player.x = 26f * t
    engine.update() // Triggers trap #3 exit relocation
    assertTrue(engine.trollTraps[2].hasRelocated)
    assertEquals(33f * t, engine.currentExitX, 0.01f)

    // Die via lethal death spin and wait out instant retry
    engine.startDeathSequence("OOPS")
    finishDeathAnimation(engine)

    // Verify rollback to checkpoint snapshot
    assertTrue(engine.hasCheckpointActivated)
    assertEquals(cpSnap.playerX, engine.player.x, 0.01f)
    assertEquals(cpSnap.playerY, engine.player.y, 0.01f)
    assertEquals(cpTimer, engine.challengeTimerMs)
    assertEquals(cpOrbFrame, engine.orbitingPlatforms.first().currentFrame)
    assertEquals(cpPendFrame, engine.pendulums.first().currentFrame)
    assertEquals(cpLightFrame, engine.lightningHazards.first().currentFrame)

    // Pre-checkpoint state stays preserved; post-checkpoint state rolls back
    assertTrue("Coin #1 before checkpoint stays collected", engine.coins[0].isCollected)
    assertFalse("Coin #2 after checkpoint rolls back", engine.coins[1].isCollected)
    assertEquals(
        "Trap #1 retired before checkpoint stays retired",
        TrollTrapState.RETIRED,
        engine.trollTraps[0].state
    )
    assertEquals("Trap #2 triggered after checkpoint resets to ARMED", TrollTrapState.ARMED, engine.trollTraps[1].state)
    assertEquals("Trap #3 triggered after checkpoint resets to ARMED", TrollTrapState.ARMED, engine.trollTraps[2].state)
    assertFalse(engine.trollTraps[2].hasRelocated)
    assertEquals(level.exitX, engine.currentExitX, 0.01f)
    assertEquals(level.exitX, engine.tiles.first { it.type == TileType.SUN_GATE }.x, 0.01f)
    assertTrue("Death starburst particles cleared after rollback", engine.particles.none { it.active })
    assertTrue("Death popups cleared after rollback", engine.popups.none { it.active })
  }

  @Test
  fun test04_fakeCheckpointNeverSavesSnapshotAndRespawnsAtSpawn() {
    val t = World1Levels.TILE_SIZE
    val level = LevelDefinition(
        worldId = 1,
        levelId = 3,
        title = "Fake Checkpoint Trap",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 12f * t, width = 24f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 22f * t, y = 9f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = emptyList(),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 10f * t,
        exitX = 22f * t,
        exitY = 9f * t,
        trollTraps = listOf(
            TrollTrap(
                id = 10,
                type = TrollTrapType.FAKE_CHECKPOINT,
                triggerType = TrapTriggerType.ON_PROXIMITY_X,
                initialX = 10f * t,
                initialY = 10f * t,
                width = t,
                height = 2f * t,
                triggerMinX = 9.5f * t,
                triggerMaxX = 11.5f * t,
                telegraphDurationFrames = 2,
                killsPlayer = true,
                quipPopup = "FAKE BELL!"
            )
        ),
        isTrollLevel = true
    )

    val engine = ChaosGameEngine(level, ProceduralAudioEngine())

    // Step directly into FAKE_CHECKPOINT
    engine.player.x = 10f * t + 4f
    engine.player.y = 12f * t - engine.player.collisionHeight
    repeat(4) {
      if (!engine.isDying) engine.update()
    }

    assertFalse("FAKE_CHECKPOINT must never activate checkpoint", engine.hasCheckpointActivated)
    assertNull("FAKE_CHECKPOINT must never create checkpointSnapshot", engine.checkpointSnapshot)
    assertTrue("Lethal FAKE_CHECKPOINT triggers death sequence", engine.isDying)

    finishDeathAnimation(engine)

    // Respawn MUST return strictly to challenge spawn
    assertFalse(engine.hasCheckpointActivated)
    assertNull(engine.checkpointSnapshot)
    assertEquals(level.spawnX, engine.player.x, 0.01f)
    assertEquals(level.spawnY, engine.player.y, 0.01f)
    assertEquals(TrollTrapState.ARMED, engine.trollTraps.first().state)
  }

  @Test
  fun test05_pendingTrapChainEventsClearedAcrossSpawnAndCheckpointRetry() {
    val t = World1Levels.TILE_SIZE
    val level = LevelDefinition(
        worldId = 1,
        levelId = 4,
        title = "Trap Chain Queue Cancellation",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 12f * t, width = 25f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 10f * t, y = 10f * t, width = t, height = 2f * t, type = TileType.CHECKPOINT_BELL, isSolid = false),
            GameTile(id = 3, x = 22f * t, y = 9f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = emptyList(),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 10f * t,
        exitX = 22f * t,
        exitY = 9f * t,
        trollTraps = listOf(
            TrollTrap(
                id = 20,
                type = TrollTrapType.SURPRISE_SPIKE_POP,
                triggerType = TrapTriggerType.ON_PROXIMITY_X,
                initialX = 18f * t,
                initialY = 11f * t,
                width = t,
                height = t,
                triggerMinX = 17.5f * t,
                triggerMaxX = 18.5f * t,
                telegraphDurationFrames = 4
            )
        ),
        isTrollLevel = true
    )

    // 1. Spawn retry clears pending chain queue
    val spawnEngine = ChaosGameEngine(level, ProceduralAudioEngine())
    spawnEngine.enqueueTrapChainEvent(targetTrapId = 20, delayFrames = 5)
    assertEquals(1, spawnEngine.pendingTrapChainEventCount)
    spawnEngine.startDeathSequence("CHAINED")
    finishDeathAnimation(spawnEngine)
    assertEquals(0, spawnEngine.pendingTrapChainEventCount)
    repeat(15) { spawnEngine.update() }
    assertEquals(
        "Target trap must remain ARMED when pre-death chain event was cancelled on spawn retry",
        TrollTrapState.ARMED,
        spawnEngine.trollTraps.first().state
    )

    // 2. Checkpoint retry clears pending chain queue
    val cpEngine = ChaosGameEngine(level, ProceduralAudioEngine())
    cpEngine.player.x = 10f * t + 4f
    cpEngine.player.y = 12f * t - cpEngine.player.collisionHeight
    cpEngine.update()
    assertTrue(cpEngine.hasCheckpointActivated)

    cpEngine.enqueueTrapChainEvent(targetTrapId = 20, delayFrames = 5)
    assertEquals(1, cpEngine.pendingTrapChainEventCount)
    cpEngine.startDeathSequence("CHAINED_CP")
    finishDeathAnimation(cpEngine)
    assertEquals(0, cpEngine.pendingTrapChainEventCount)
    repeat(15) { cpEngine.update() }
    assertEquals(
        "Target trap must remain ARMED when pre-death chain event was cancelled on checkpoint retry",
        TrollTrapState.ARMED,
        cpEngine.trollTraps.first().state
    )
  }

  @Test
  fun test06_pendingCameraSnapEliminatesLerpDragOnDeathAndReset() {
    val level = World1Levels.createLevel(1, DifficultyTier.NORMAL)
    val engine = ChaosGameEngine(level, ProceduralAudioEngine())
    val renderer = ChaosGameRenderer(app)
    val bitmap = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Move player far right and let camera track far right
    engine.player.x = 2400f
    engine.player.y = 500f
    renderer.render(canvas, engine, 800f, 480f) // consumes initial snap cleanly to ~2156f
    assertTrue("Camera should be far right before death", renderer.cameraX > 1800f)
    assertFalse(engine.hasPendingCameraSnap())

    // Trigger pit death -> respawns at spawnX = 96f and sets pendingCameraSnap
    engine.handlePlayerDeath()
    assertTrue("Spawn retry must request immediate camera snap", engine.hasPendingCameraSnap())

    // Render ONE frame and assert zero LERP lag
    renderer.render(canvas, engine, 800f, 480f)
    val expectedSpawnCamX = (engine.player.x + 60f - 800f * 0.38f).coerceAtLeast(0f)
    val expectedSpawnCamY = (engine.player.y - 480f * 0.55f).coerceIn(level.minCamY, level.maxCamY)
    assertEquals(expectedSpawnCamX, renderer.cameraX, 0.01f)
    assertEquals(expectedSpawnCamY, renderer.cameraY, 0.01f)
    assertFalse(engine.hasPendingCameraSnap())

    // Also test checkpoint snap using a level with a genuine CHECKPOINT_BELL
    val t = World1Levels.TILE_SIZE
    val cpLevel = LevelDefinition(
        worldId = 1,
        levelId = 2,
        title = "Checkpoint Camera Snap",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 12f * t, width = 100f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 25f * t, y = 10f * t, width = t, height = 2f * t, type = TileType.CHECKPOINT_BELL, isSolid = false),
            GameTile(id = 3, x = 90f * t, y = 9f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = emptyList(),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 10f * t,
        exitX = 90f * t,
        exitY = 9f * t
    )
    val cpEngine = ChaosGameEngine(cpLevel, ProceduralAudioEngine())
    val cpRenderer = ChaosGameRenderer(app)
    val bell = cpEngine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    cpEngine.player.x = bell.x + 6f
    cpEngine.player.y = bell.y
    cpEngine.update()
    assertTrue(cpEngine.hasCheckpointActivated)

    cpEngine.player.x = 4200f
    repeat(20) {
      cpRenderer.render(canvas, cpEngine, 800f, 480f)
    }
    assertTrue("Camera should follow player far past checkpoint", cpRenderer.cameraX > 3200f)

    cpEngine.handlePlayerDeath()
    assertTrue(cpEngine.hasPendingCameraSnap())
    cpRenderer.render(canvas, cpEngine, 800f, 480f)
    val expectedCpCamX = (cpEngine.player.x + 60f - 800f * 0.38f).coerceAtLeast(0f)
    assertEquals(expectedCpCamX, cpRenderer.cameraX, 0.01f)
  }

  @Test
  fun test07_heldJumpGuardPreventsFrame1JumpWhilePreservingHeldRunDirection() {
    val t = World1Levels.TILE_SIZE
    val flatLevel = LevelDefinition(
        worldId = 1,
        levelId = 1,
        title = "Flat Floor Jump Guard",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 12f * t, width = 30f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 25f * t, y = 9f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = emptyList(),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 12f * t - 54f,
        exitX = 25f * t,
        exitY = 9f * t,
        isTrollLevel = true
    )
    val engine = ChaosGameEngine(flatLevel, ProceduralAudioEngine())

    // Land player cleanly at spawn
    repeat(5) { engine.update() }
    assertTrue(engine.player.isGrounded)

    // Hold BOTH Jump and Right while dying through the death animation
    engine.inputRight = true
    engine.inputJump = true
    engine.startDeathSequence("SPIKE")
    finishDeathAnimation(engine)

    // Frames after respawn with inputJump STILL held down:
    // Must NOT execute an accidental jump impulse, while still moving Right!
    val startX = engine.player.x
    repeat(6) {
      engine.update()
      assertTrue("Held Jump must NOT cause negative jump vy on frame ${it + 1}", engine.player.vy >= 0f)
    }
    assertTrue("Held Right must carry through death retry cleanly", engine.player.x > startX)
    assertTrue("Player must land and remain grounded instead of jumping off held pre-death Jump", engine.player.isGrounded)

    // Releasing Jump and pressing Jump cleanly afterward DOES trigger normal jump
    engine.inputJump = false
    engine.update()
    engine.inputJump = true
    engine.update()
    assertEquals(ChaosGameEngine.JUMP_IMPULSE + ChaosGameEngine.GRAVITY, engine.player.vy, 0.05f)
    assertFalse(engine.player.isGrounded)
  }

  @Test
  fun test08_deathAnimationTimingEvaluation8Vs10Vs12Frames() {
    assertEquals(10, ChaosGameEngine.DEFAULT_DEATH_ANIM_FRAMES)
    assertEquals(10, ChaosGameEngine.DEATH_ANIM_FRAMES)

    for (targetFrames in listOf(8, 10, 12)) {
      val engine = ChaosGameEngine(
          level = World1Levels.createLevel(1, DifficultyTier.NORMAL),
          audioEngine = ProceduralAudioEngine(),
          deathAnimDurationFrames = targetFrames
      )
      engine.startDeathSequence("TEST_$targetFrames")
      assertTrue(engine.isDying)
      assertEquals(targetFrames, engine.deathAnimFrames)

      // Remains dying through (targetFrames - 1) frames
      repeat(targetFrames - 1) {
        engine.update()
        assertTrue("Frame ${it + 1}/$targetFrames should still be dying", engine.isDying)
      }
      // Completes respawn cleanly on exact targetFrames-th update
      engine.update()
      assertFalse("Should finish death spin and respawn on frame $targetFrames", engine.isDying)
      assertEquals(1, engine.deathCount)
      assertEquals(engine.level.spawnX, engine.player.x, 0.01f)
    }
  }
}
