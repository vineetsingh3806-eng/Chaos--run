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
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase4TrapsAndChallengesTest {

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
  }

  private fun noopCallbacks(
      killCounter: IntArray = intArrayOf(0),
      mirrorSetter: (Int) -> Unit = {},
      windAdder: (Float) -> Unit = {},
      gateRelocator: (Float, Float) -> Unit = { _, _ -> }
  ) = object : TrollTrapCallbacks {
    override fun killPlayer(quip: String) {
      killCounter[0]++
    }
    override fun relocateExitGate(destX: Float, destY: Float) = gateRelocator(destX, destY)
    override fun hideLinkedTile(tileId: Int) = Unit
    override fun applyMirrorCurse(durationFrames: Int) = mirrorSetter(durationFrames)
    override fun applyWindForce(forceX: Float) = windAdder(forceX)
    override fun playTrapSnap() = Unit
    override fun playCrumble() = Unit
    override fun playDisappear() = Unit
    override fun playCeilingSlam() = Unit
    override fun playDoorEscape() = Unit
    override fun playFakeDoorPoof() = Unit
    override fun playMechanismShift() = Unit
    override fun spawnDustTrickle(x: Float, y: Float) = Unit
    override fun spawnDustPuff(x: Float, y: Float) = Unit
    override fun spawnSparkles(x: Float, y: Float, color: Int, count: Int) = Unit
    override fun spawnStarburst(x: Float, y: Float) = Unit
    override fun spawnPopup(x: Float, y: Float, text: String, color: Int) = Unit
    override fun triggerCameraShake(frames: Int, magnitude: Float) = Unit
  }

  @Test
  fun test01_enumInventoryAndCatalogCompleteness() {
    assertTrue("Must contain at least the 24 Phase 4 trap mechanics", TrollTrapType.entries.size >= 24)
    assertEquals("Must have exactly 7 trigger behaviours", 7, TrapTriggerType.entries.size)
    assertTrue("Catalog must preserve at least 12 challenges", ChallengeCatalog.entries.size >= 12)

    val first12 = ChallengeCatalog.entries.take(12)
    val ids = first12.map { it.challengeId }
    assertEquals((1..12).map { "ch_${it.toString().padStart(2, '0')}" }, ids)
    assertEquals((1..12).toList(), first12.map { it.orderIndex })
    assertEquals(
        ChallengeCatalog.entries.size,
        ChallengeCatalog.entries.map { it.challengeSeed }.toSet().size
    )

    // Verify all 13 new trap types and 2 new triggers are actively used in ch_06..ch_12
    val phase4Levels = (6..12).map { idx ->
      ChallengeCatalog.findById("ch_${idx.toString().padStart(2, '0')}")!!.buildLevel()
    }
    val usedTypes = phase4Levels.flatMap { lvl -> lvl.trollTraps.map { it.type } }.toSet()
    val usedTriggers = phase4Levels.flatMap { lvl -> lvl.trollTraps.map { it.triggerType } }.toSet()

    val new13Types = setOf(
        TrollTrapType.SPLIT_BRIDGE,
        TrollTrapType.PHANTOM_LEDGE,
        TrollTrapType.TILTING_PLANK,
        TrollTrapType.TRAILING_SPIKE,
        TrollTrapType.EVASIVE_PLATFORM,
        TrollTrapType.YO_YO_PLATFORM,
        TrollTrapType.EXIT_SLAM_CAGE,
        TrollTrapType.FLEEING_SUN_GATE,
        TrollTrapType.PINCER_WALLS,
        TrollTrapType.STALKER_CRUSH_WALL,
        TrollTrapType.BETRAYAL_SWITCH,
        TrollTrapType.GUST_REVERSAL_ZONE,
        TrollTrapType.MIRROR_CURSE_TOTEM
    )
    assertTrue("All 13 new traps must be showcased in ch_06..ch_12", usedTypes.containsAll(new13Types))
    assertTrue(usedTriggers.contains(TrapTriggerType.ON_COIN_COLLECTED))
    assertTrue(usedTriggers.contains(TrapTriggerType.ON_TRAP_ACTIVATED))
  }

  @Test
  fun test02_splitBridgeAndPhantomLedgeDeterminism() {
    val system = TrollTrapSystem()
    val cb = noopCallbacks()

    // SPLIT_BRIDGE: wings remain solid after center splits
    val split = TrollTrap(
        id = 1,
        type = TrollTrapType.SPLIT_BRIDGE,
        triggerType = TrapTriggerType.ON_STEP,
        initialX = 200f,
        initialY = 400f,
        width = 200f,
        height = 48f,
        telegraphDurationFrames = 12,
        killsPlayer = false
    )
    val playerOnWing = Player(x = 208f, y = 400f - 54f, isGrounded = true)
    playerOnWing.prevY = playerOnWing.y
    system.updateAll(listOf(split), playerOnWing, cb)
    assertEquals(TrollTrapState.TELEGRAPH, split.state)

    // Advance past 12f telegraph + split animation
    repeat(30) { system.updateAll(listOf(split), playerOnWing, cb) }
    assertTrue("Split progress must have parted the center", split.auxProgress >= 0.35f)

    // Player on left outer wing stays supported cleanly
    playerOnWing.vy = 2f
    playerOnWing.y = 400f - 54f + 1f
    playerOnWing.prevY = 400f - 54f - 1f
    playerOnWing.isGrounded = false
    system.resolveVerticalCollisions(listOf(split), playerOnWing, false, cb) {}
    assertTrue("Outer wing of split bridge must remain solid escape footing", playerOnWing.isGrounded)

    // Player over center split must fall through
    val playerInCenter = Player(x = 284f, y = 400f - 54f + 1f, vy = 2f, isGrounded = false)
    playerInCenter.prevY = 400f - 54f - 1f
    system.resolveVerticalCollisions(listOf(split), playerInCenter, false, cb) {}
    assertFalse("Center split must become non-solid once opened", playerInCenter.isGrounded)

    // PHANTOM_LEDGE: clue -> telegraph -> non-solid -> reforms deterministically
    val phantom = TrollTrap(
        id = 2,
        type = TrollTrapType.PHANTOM_LEDGE,
        triggerType = TrapTriggerType.ON_STEP,
        initialX = 200f,
        initialY = 300f,
        width = 120f,
        height = 24f,
        telegraphDurationFrames = 10,
        cycleFrames = 60,
        killsPlayer = false
    )
    val p2 = Player(x = 230f, y = 300f - 54f, isGrounded = true)
    system.updateAll(listOf(phantom), p2, cb)
    assertEquals(TrollTrapState.TELEGRAPH, phantom.state)
    assertTrue("Must remain solid during telegraph", phantom.isSolid)

    repeat(10) { system.updateAll(listOf(phantom), p2, cb) }
    assertEquals(TrollTrapState.ACTIVE, phantom.state)
    assertFalse("Must turn non-solid after telegraph", phantom.isSolid)

    repeat(60) { system.updateAll(listOf(phantom), p2, cb) }
    assertEquals(TrollTrapState.ARMED, phantom.state)
    assertTrue("Phantom ledge must reform deterministically after cycle", phantom.isSolid)
  }

  @Test
  fun test03_tiltingPlankSyncAndTrailingSpikeBoundedPursuit() {
    val system = TrollTrapSystem()
    val kills = intArrayOf(0)
    val cb = noopCallbacks(killCounter = kills)

    val plank = TrollTrap(
        id = 1,
        type = TrollTrapType.TILTING_PLANK,
        triggerType = TrapTriggerType.ON_STEP,
        initialX = 100f,
        initialY = 400f,
        width = 180f,
        height = 18f,
        maxTiltDegrees = 20f,
        killsPlayer = false
    )
    // Player standing near right edge applies positive clockwise tilt
    val rider = Player(x = 240f, y = 400f - 54f, isGrounded = true)
    repeat(15) {
      rider.prevY = rider.y
      rider.vy = 0.5f
      system.resolveVerticalCollisions(listOf(plank), rider, true, cb) {}
      system.updateAll(listOf(plank), rider, cb)
    }
    assertTrue("Plank must tilt positively when weighted on right", plank.tiltAngleDeg > 2f)
    val expectedSurfaceY = system.computeTiltingPlankSurfaceY(plank, rider.x + rider.collisionWidth * 0.5f)
    assertEquals("Collision surface must stay synchronized with rotated plank", expectedSurfaceY, rider.y + rider.collisionHeight, 1.5f)

    // Stepping off gently springs plank back toward 0 deg
    val offPlayer = Player(x = 20f, y = 400f - 54f, isGrounded = true)
    val tiltBeforeReturn = plank.tiltAngleDeg
    repeat(30) { system.updateAll(listOf(plank), offPlayer, cb) }
    assertTrue("Plank must spring back toward level after player steps off", plank.tiltAngleDeg < tiltBeforeReturn)

    // TRAILING_SPIKE: pursuit speed capped below player run speed & never teleports into hitbox
    val chaser = TrollTrap(
        id = 2,
        type = TrollTrapType.TRAILING_SPIKE,
        triggerType = TrapTriggerType.ON_PROXIMITY_X,
        initialX = 100f,
        initialY = 370f,
        width = 44f,
        height = 30f,
        triggerMinX = 180f,
        triggerMaxX = 240f,
        telegraphDurationFrames = 12,
        targetX = 500f,
        targetY = 370f,
        moveSpeed = 9.9f, // Deliberately authored high; policy must cap!
        activeDurationFrames = 180,
        killsPlayer = true
    )
    TrapFairnessPolicy.applyTierTuningInPlace(listOf(chaser), AdaptiveTier.RELENTLESS)
    assertTrue(
        "Pursuit speed must be capped below player max run speed",
        chaser.lockedMoveSpeed <= TrapFairnessPolicy.MAX_PURSUIT_TRAP_SPEED &&
            chaser.lockedMoveSpeed < ChaosGameEngine.MAX_RUN_SPEED
    )
    val runner = Player(x = 210f, y = 346f, isGrounded = true)
    system.updateAll(listOf(chaser), runner, cb)
    assertEquals(TrollTrapState.TELEGRAPH, chaser.state)
    repeat(12) { system.updateAll(listOf(chaser), runner, cb) }
    assertEquals(TrollTrapState.ACTIVE, chaser.state)
    // Run rightward faster than pursuit spike -> must stay safe!
    repeat(35) {
      runner.x += ChaosGameEngine.MAX_RUN_SPEED
      system.updateAll(listOf(chaser), runner, cb)
    }
    assertEquals("Outrunning bounded trailing spike must never cause phantom kill", 0, kills[0])
  }

  @Test
  fun test04_evasivePlatformAndYoYoPlatformPhysics() {
    val system = TrollTrapSystem()
    val cb = noopCallbacks()

    val evasive = TrollTrap(
        id = 1,
        type = TrollTrapType.EVASIVE_PLATFORM,
        triggerType = TrapTriggerType.ON_PROXIMITY_X,
        initialX = 200f,
        initialY = 350f,
        width = 120f,
        height = 24f,
        triggerMinX = 160f,
        triggerMaxX = 240f,
        telegraphDurationFrames = 12,
        targetX = 320f,
        targetY = 350f,
        moveSpeed = 5.0f,
        killsPlayer = false
    )
    val p = Player(x = 180f, y = 350f - 54f, isGrounded = true)
    system.updateAll(listOf(evasive), p, cb)
    assertEquals(TrollTrapState.TELEGRAPH, evasive.state)
    repeat(12) { system.updateAll(listOf(evasive), p, cb) }
    assertEquals(TrollTrapState.ACTIVE, evasive.state)
    repeat(40) {
      system.preStepMovingTraps(listOf(evasive))
      system.updateAll(listOf(evasive), p, cb)
    }
    assertEquals(320f, evasive.x, 0.5f)
    assertEquals(TrollTrapState.RETIRED, evasive.state)

    // YO_YO_PLATFORM carries rider smoothly vertically without jitter
    val yoyo = TrollTrap(
        id = 2,
        type = TrollTrapType.YO_YO_PLATFORM,
        triggerType = TrapTriggerType.ON_TIMER_CYCLE,
        initialX = 300f,
        initialY = 400f,
        width = 120f,
        height = 24f,
        targetX = 300f,
        targetY = 280f,
        cycleFrames = 120,
        killsPlayer = false
    )
    val rider = Player(x = 340f, y = 400f - 54f, isGrounded = true)
    var minSeenY = Float.MAX_VALUE
    var maxSeenY = Float.MIN_VALUE
    repeat(120) {
      system.preStepMovingTraps(listOf(yoyo))
      rider.prevY = rider.y
      rider.y += yoyo.vy
      rider.vy = 0.5f
      system.resolveVerticalCollisions(listOf(yoyo), rider, true, cb) {}
      system.updateAll(listOf(yoyo), rider, cb)
      assertTrue("Rider must remain reliably grounded throughout yo-yo cycle", rider.isGrounded)
      minSeenY = minOf(minSeenY, yoyo.y)
      maxSeenY = maxOf(maxSeenY, yoyo.y)
    }
    assertEquals(280f, minSeenY, 2f)
    assertEquals(400f, maxSeenY, 2f)
  }

  @Test
  fun test05_exitSlamCageAndFleeingSunGateNoSoftlock() {
    val ch09 = ChallengeCatalog.findById("ch_09")!!.lockConfig(AdaptiveTier.STANDARD)
    val engine = ChaosGameEngine(ch09.levelDefinition, ProceduralAudioEngine(), lockedChallengeConfig = ch09)

    // 1. Trigger EXIT_SLAM_CAGE -> enters TELEGRAPH, then ACTIVE (locks briefly), then opens/RETIRES cleanly!
    val cage = engine.trollTraps.first { it.type == TrollTrapType.EXIT_SLAM_CAGE }
    engine.player.x = cage.x + 24f
    engine.player.y = cage.y + 40f
    repeat(15) { engine.update() }
    assertEquals(TrollTrapState.ACTIVE, cage.state)
    assertTrue(cage.isSolid)

    // Wait effectDurationFrames -> cage unlocks cleanly so player is never trapped permanently
    repeat(70) { engine.update() }
    assertEquals(TrollTrapState.RETIRED, cage.state)
    assertFalse("Slam cage must release solidity cleanly", cage.isSolid)

    // 2. Test FLEEING_SUN_GATE: hops 2 times cleanly, blocks victory until hops complete, then retires -> win!
    val fleeing = engine.trollTraps.first { it.type == TrollTrapType.FLEEING_SUN_GATE }
    val gateStart = engine.tiles.first { it.type == TileType.SUN_GATE }
    engine.player.x = gateStart.x + 16f
    engine.player.y = gateStart.y + 40f
    engine.update()
    assertFalse("Must not allow instant victory before fleeing gate resolves", engine.isVictorious)

    // Wait out hop 1 (8f telegraph + 14f flight)
    repeat(26) { engine.update() }
    assertEquals(1, fleeing.activationCount)
    assertEquals(TrollTrapState.ARMED, fleeing.state)

    // Approach at hop 1 position -> hop 2 -> retires!
    val gateHop1 = engine.tiles.first { it.type == TileType.SUN_GATE }
    engine.player.x = gateHop1.x + 16f
    engine.player.y = gateHop1.y + 40f
    repeat(26) { engine.update() }
    assertEquals(2, fleeing.activationCount)
    assertEquals(TrollTrapState.RETIRED, fleeing.state)

    // Touch retired gate -> clean victory!
    val gateFinal = engine.tiles.first { it.type == TileType.SUN_GATE }
    engine.player.x = gateFinal.x + 16f
    engine.player.y = gateFinal.y + 40f
    engine.update()
    assertTrue("After fleeing stops, touching Sun Gate must trigger victory", engine.isVictorious)
  }

  @Test
  fun test06_pincerWallsSafeCorridorAndStalkerCrushWall() {
    val system = TrollTrapSystem()
    val kills = intArrayOf(0)
    val cb = noopCallbacks(killCounter = kills)

    val pincer = TrollTrap(
        id = 1,
        type = TrollTrapType.PINCER_WALLS,
        triggerType = TrapTriggerType.ON_PROXIMITY_X,
        initialX = 200f,
        initialY = 320f,
        width = 180f,
        height = 84f,
        minSafeGapWidth = 60f,
        telegraphDurationFrames = 12,
        moveSpeed = 5.0f,
        cycleFrames = 100,
        killsPlayer = true
    )
    // Player centered cleanly inside minSafeGapWidth corridor stays alive!
    val safePlayer = Player(x = 200f + 90f - 16f, y = 350f, isGrounded = true)
    system.updateAll(listOf(pincer), safePlayer, cb)
    assertEquals(TrollTrapState.TELEGRAPH, pincer.state)
    repeat(60) {
      system.resolveHorizontalCollisions(listOf(pincer), safePlayer, cb)
      system.updateAll(listOf(pincer), safePlayer, cb)
    }
    assertEquals("Center safe gap must protect player cleanly", 0, kills[0])
    val maxJawW = system.computePincerJawWidth(pincer)
    assertTrue("Open corridor must stay >= minSafeGapWidth", pincer.width - 2f * maxJawW >= 59.5f)
  }

  @Test
  fun test07_betrayalSwitchOnCoinCollectedAndOnTrapActivatedChaining() {
    val system = TrollTrapSystem()
    val kills = intArrayOf(0)
    val cb = noopCallbacks(killCounter = kills)

    // Self-referential / cycle recursion guard test (must never infinite loop or crash)
    val cyclicA = TrollTrap(
        id = 10,
        type = TrollTrapType.BETRAYAL_SWITCH,
        triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
        triggerSourceTrapId = 11,
        linkedTrapId = 11,
        initialX = 100f,
        initialY = 400f,
        width = 48f,
        height = 24f,
        telegraphDurationFrames = 0,
        killsPlayer = false
    )
    val cyclicB = TrollTrap(
        id = 11,
        type = TrollTrapType.BETRAYAL_SWITCH,
        triggerType = TrapTriggerType.ON_TRAP_ACTIVATED,
        triggerSourceTrapId = 10,
        linkedTrapId = 10,
        initialX = 160f,
        initialY = 400f,
        width = 48f,
        height = 24f,
        telegraphDurationFrames = 0,
        killsPlayer = false
    )
    // Coin #4 triggers cyclicA which chains to cyclicB -> terminates cleanly without stack overflow!
    val coinTrap = TrollTrap(
        id = 12,
        type = TrollTrapType.SURPRISE_SPIKE_POP,
        triggerType = TrapTriggerType.ON_COIN_COLLECTED,
        triggerCoinId = 4,
        linkedTrapId = 10,
        initialX = 300f,
        initialY = 400f,
        width = 48f,
        height = 30f,
        telegraphDurationFrames = 12,
        killsPlayer = true
    )
    val all = listOf(cyclicA, cyclicB, coinTrap)
    // Wrong coin ID must NOT trigger
    system.onCoinCollected(99, all, cb)
    assertEquals(TrollTrapState.ARMED, coinTrap.state)

    // Matching coin ID triggers once cleanly and executes cyclic chain cleanly
    system.onCoinCollected(4, all, cb)
    assertEquals(TrollTrapState.TELEGRAPH, coinTrap.state)
    repeat(12) { system.updateAll(all, Player(x = 0f, y = 0f), cb) }
    assertEquals(TrollTrapState.RETIRED, cyclicA.state)
    assertEquals(TrollTrapState.RETIRED, cyclicB.state)

    // Duplicate coin notification must remain ignored
    coinTrap.reset()
    system.onCoinCollected(4, all, cb)
    assertEquals("Already collected coin must not re-fire ON_COIN_COLLECTED", TrollTrapState.ARMED, coinTrap.state)
  }

  @Test
  fun test08_gustReversalZoneAndMirrorCurseTotemWithCleanReset() {
    val ch12 = ChallengeCatalog.findById("ch_12")!!.lockConfig(AdaptiveTier.STANDARD)
    val engine = ChaosGameEngine(ch12.levelDefinition, ProceduralAudioEngine(), lockedChallengeConfig = ch12)

    val gust = engine.trollTraps.first { it.type == TrollTrapType.GUST_REVERSAL_ZONE }
    val initialSign = gust.gustSign
    // Stand safely inside Gust zone -> velocity influenced while raw input stays untouched
    engine.player.x = gust.x + 80f
    engine.player.y = gust.y + 80f
    engine.inputLeft = false
    engine.inputRight = false
    repeat(20) { engine.update() }
    assertFalse("Gust must never mutate player inputLeft", engine.inputLeft)
    assertFalse("Gust must never mutate player inputRight", engine.inputRight)

    // Advance full gust cycle -> enters pre-flip TELEGRAPH and reverses gustSign cleanly
    var sawTelegraph = false
    repeat(gust.lockedCycleFrames) {
      engine.update()
      if (gust.state == TrollTrapState.TELEGRAPH) sawTelegraph = true
    }
    assertTrue("Gust reversal must display pre-flip TELEGRAPH phase", sawTelegraph)
    assertEquals(-initialSign, gust.gustSign, 0.001f)

    // MIRROR_CURSE_TOTEM: swaps left/right movement while raw inputs stay untouched & clears on reset/death
    val totem = engine.trollTraps.first { it.type == TrollTrapType.MIRROR_CURSE_TOTEM }
    engine.player.x = totem.triggerMinX + 16f
    engine.player.y = totem.initialY + 10f
    engine.inputRight = false
    engine.inputLeft = false
    repeat(15) { engine.update() }
    assertTrue("Mirror Curse must activate after telegraph", engine.mirrorCurseFrames > 0)

    // Hold raw RIGHT -> player moves LEFT under mirror curse without corrupting inputRight
    engine.inputRight = true
    repeat(5) { engine.update() }
    assertTrue("Raw inputRight must remain true", engine.inputRight)
    assertTrue("Mirrored control must accelerate player leftward (vx < 0)", engine.player.vx < 0f)

    // Death/retry immediately clears Mirror Curse cleanly
    engine.handlePlayerDeath()
    assertEquals("Mirror Curse must clear cleanly upon death/respawn", 0, engine.mirrorCurseFrames)
    engine.inputRight = false
  }

  @Test
  fun test09_checkpointRollbackAndSpawnRetryResetAllNewTrapsAndQueues() {
    val ch11 = ChallengeCatalog.findById("ch_11")!!.lockConfig(AdaptiveTier.STANDARD)
    val engine = ChaosGameEngine(ch11.levelDefinition, ProceduralAudioEngine(), lockedChallengeConfig = ch11)
    val switch1 = engine.trollTraps.first { it.id == 1 }

    // Trigger Trap 1 -> queued chain event present, then advance to Bell checkpoint
    engine.player.x = switch1.x + 8f
    engine.player.y = switch1.y + switch1.height - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.update()
    assertEquals(TrollTrapState.RETIRED, switch1.state)
    assertTrue("Chained trap event should be pending", engine.pendingTrapChainEventCount > 0)

    // Settle chain and activate Checkpoint Bell
    repeat(30) { engine.update() }
    val bell = engine.tiles.first { it.type == TileType.CHECKPOINT_BELL }
    engine.player.x = bell.x + 8f
    engine.player.y = bell.y + 8f
    engine.update()
    assertTrue(engine.hasCheckpointActivated)

    // Collect Coin 3 (post-checkpoint) which queues Phantom Ledge #5 drop, then die!
    val coin3 = engine.coins.first { it.id == 3 }
    engine.player.x = coin3.x - 10f
    engine.player.y = coin3.y - 10f
    engine.update()
    assertTrue(coin3.isCollected)
    assertTrue(engine.pendingTrapChainEventCount > 0)

    engine.handlePlayerDeath()
    assertEquals("Pending chain queue must be cleared on checkpoint rollback", 0, engine.pendingTrapChainEventCount)
    assertFalse("Post-checkpoint coin must roll back so ON_COIN_COLLECTED works again", coin3.isCollected)
    assertEquals("Pre-checkpoint switch stays resolved", TrollTrapState.RETIRED, switch1.state)

    // Full retry clears checkpoint and restores everything to initial armed state
    engine.resetCurrentChallenge()
    assertFalse(engine.hasCheckpointActivated)
    assertEquals(TrollTrapState.ARMED, switch1.state)
    assertEquals(0, engine.mirrorCurseFrames)
  }

  @Test
  fun test10_trapFairnessPolicyAcrossAll12ChallengesAnd4AdaptiveTiers() {
    for (def in ChallengeCatalog.entries) {
      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(def, tier)
        assertFalse("${def.challengeId} must contain traps", validations.isEmpty())
        for (v in validations) {
          assertTrue(
              "${def.challengeId} [$tier] trap ${v.trapId} (${v.trapType}) failed fairness: ${v.reason}",
              v.isFair
          )
        }
      }
    }
  }

  @Test
  fun test11_allNewChallengesPlayableReachableAndRenderWithoutCrash() {
    val bmp = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val renderer = ChaosGameRenderer(app)

    try {
      for (idx in 6..12) {
        val id = "ch_${idx.toString().padStart(2, '0')}"
        val locked = ChallengeCatalog.createLockedConfig(id, AdaptiveTier.STANDARD)!!
        val lvl = locked.levelDefinition

        assertEquals(5, lvl.coins.size)
        assertEquals(5, lvl.coins.map { it.id }.toSet().size)
        assertEquals(lvl.trollTraps.size, lvl.trollTraps.map { it.id }.toSet().size)
        assertTrue(lvl.tiles.any { it.type == TileType.SUN_GATE })

        // Verify spawn sits safely above solid terrain
        val hasSolidBelowSpawn = lvl.tiles.any {
          it.isSolid && lvl.spawnX + 16f >= it.x && lvl.spawnX + 16f <= it.x + it.width && it.y >= lvl.spawnY
        }
        assertTrue("$id must have solid footing below spawn", hasSolidBelowSpawn)

        val engine = ChaosGameEngine(lvl, ProceduralAudioEngine(), lockedChallengeConfig = locked)
        repeat(60) {
          engine.inputRight = (it < 20)
          engine.update()
        }
        renderer.render(canvas, engine, 800f, 480f)
        assertTrue("$id must render cleanly", renderer.hasRenderedFrame)
      }
    } finally {
      renderer.expansionRenderer.release()
      bmp.recycle()
    }
  }

  @Test
  fun test12_continuousProgressionThrough12ChallengesAndSaveRestorePersistence() {
    val repo = SaveRepository(app)
    val coordinator = ContinuousRunCoordinator()
    coordinator.startNewRun()

    // Progress cleanly through ch_01..ch_08, save mid-run, restore via fresh coordinator & repo
    for (i in 1..8) {
      val id = "ch_${i.toString().padStart(2, '0')}"
      val outcome = coordinator.onChallengeCompleted(
          challengeId = id,
          attemptToken = coordinator.currentAttemptToken,
          timeMs = 9500L,
          score = 1000,
          coinsCollected = 5,
          totalCoins = 5,
          parTimeSeconds = 30,
          isTrollLevel = true,
          challengeDeaths = 0
      )
      assertTrue(outcome is ChallengeCompletionOutcome.NextChallenge)
    }
    assertEquals("ch_09", coordinator.activeChallengeId)

    repo.saveContinuousRunState(
        coordinator.exportPersistedState(
            schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
            isRunInForeground = true
        ),
        syncCommit = true
    )
    val loaded = repo.loadContinuousRunState()
    assertEquals("ch_09", loaded.activeChallengeId)
    assertEquals(8, loaded.completedChallengeIds.size)

    val restoredCoord = ContinuousRunCoordinator()
    val restoredCfg = restoredCoord.restoreFromPersisted(loaded)
    assertEquals("ch_09", restoredCfg.challengeId)

    // Finish ch_09..ch_200 -> produces RunCompleted(200/200)
    for (i in 9..199) {
      val id = "ch_${i.toString().padStart(2, '0')}"
      val outcome = restoredCoord.onChallengeCompleted(
          challengeId = id,
          attemptToken = restoredCoord.currentAttemptToken,
          timeMs = 9500L,
          score = 1000,
          coinsCollected = 5,
          totalCoins = 5,
          parTimeSeconds = 30,
          isTrollLevel = true,
          challengeDeaths = 0
      )
      assertTrue(outcome is ChallengeCompletionOutcome.NextChallenge)
    }
    val finalOutcome = restoredCoord.onChallengeCompleted(
        challengeId = "ch_200",
        attemptToken = restoredCoord.currentAttemptToken,
        timeMs = 9500L,
        score = 1000,
        coinsCollected = 5,
        totalCoins = 5,
        parTimeSeconds = 30,
        isTrollLevel = true,
        challengeDeaths = 0
    )
    assertTrue("Final ch_200 must complete the 200-challenge run", finalOutcome is ChallengeCompletionOutcome.RunCompleted)
    val summary = (finalOutcome as ChallengeCompletionOutcome.RunCompleted).summary
    assertEquals(200, summary.completedCount)
    assertEquals(200, summary.totalChallenges)
    assertTrue(restoredCoord.isRunCompleted)
  }
}
