package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.DifficultyTier
import com.example.data.GameTile
import com.example.data.LevelDefinition
import com.example.data.LevelTheme
import com.example.data.SaveRepository
import com.example.data.TileType
import com.example.data.TrapTriggerType
import com.example.data.TrollTrap
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class World1TrollPart2Test {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
  }

  private fun engineFor(levelId: Int, difficulty: DifficultyTier = DifficultyTier.NORMAL): ChaosGameEngine {
    return ChaosGameEngine(
        level = World1Levels.createLevel(levelId, difficulty),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
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
      maxFrames: Int = 220
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
        "Player died while running toward ${targetX / t}T (stopped at x=${engine.player.x / t}T, deaths=${engine.deathCount})",
        engine.isDying || engine.deathCount > startDeaths
    )
    assertTrue(
        "Timed out after $maxFrames frames running toward ${targetX / t}T (stuck at x=${engine.player.x / t}T)",
        engine.player.x >= targetX || engine.isVictorious
    )
  }

  private fun waitUntilGrounded(
      engine: ChaosGameEngine,
      right: Boolean = true,
      left: Boolean = false,
      maxFrames: Int = 120
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
        "Player died before landing (x=${engine.player.x / t}T, y=${engine.player.y / t}T, deaths=${engine.deathCount})",
        engine.isDying || engine.deathCount > startDeaths
    )
    assertTrue(
        "Timed out waiting to land after $maxFrames frames (x=${engine.player.x / t}T, y=${engine.player.y / t}T, vy=${engine.player.vy})",
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
        "Player died waiting for ${description()} at x=${engine.player.x / t}T (deaths=${engine.deathCount})",
        engine.isDying || engine.deathCount > startDeaths
    )
    assertTrue(
        "Timed out after $maxFrames frames waiting for ${description()} at x=${engine.player.x / t}T",
        condition()
    )
  }

  // --- 1. Level Loading & Metadata ---

  @Test
  fun test01_allFiveWorld1LevelsLoadDistinctlyAcrossDifficulties() {
    val expectedTitles = mapOf(
        1 to "The Easy Start",
        2 to "The Fake Safe Path",
        3 to "The Exit Is Lying",
        4 to "The Floor Has Plans",
        5 to "Almost There"
    )
    val expectedThemes = mapOf(
        1 to LevelTheme.WHISPERING_WOODS,
        2 to LevelTheme.HOLLOW_OF_ECHOES,
        3 to LevelTheme.RUINS_OF_LOST_KING,
        4 to LevelTheme.MONSOON_RUN,
        5 to LevelTheme.TEMPLE_OF_LAST_LIGHT
    )

    assertEquals(5, World1Levels.catalog.size)
    for (tier in DifficultyTier.entries) {
      for (id in 1..5) {
        val level = World1Levels.createLevel(id, tier)
        assertTrue("Level 1-$id must be flagged as troll level", level.isTrollLevel)
        assertEquals(expectedTitles[id], level.title)
        assertEquals(expectedThemes[id], level.theme)
        assertTrue("Level 1-$id must have troll traps", level.trollTraps.isNotEmpty())
        assertTrue("Level 1-$id must have collectible coins", level.coins.size >= 4)
        val sunGate = level.tiles.firstOrNull { it.type == TileType.SUN_GATE }
        assertNotNull("Level 1-$id must contain SUN_GATE", sunGate)
        assertEquals(level.exitX, sunGate!!.x, 0.01f)
        assertEquals(level.exitY, sunGate.y, 0.01f)
      }
    }
  }

  // --- 2. Individual Trap Mechanics, Collisions & Deterministic Resets ---

  @Test
  fun test02_disappearingFloorAndDelayedCrumbleTriggerAndReset() {
    val t = World1Levels.TILE_SIZE
    val e2 = engineFor(2)

    // Disappearing Floor (trap id 9 at Tile 38, Y=13)
    val vanish = e2.trollTraps.first { it.type == TrollTrapType.DISAPPEARING_FLOOR }
    e2.player.x = vanish.initialX + 8f
    e2.player.y = vanish.initialY - e2.player.collisionHeight
    e2.player.isGrounded = true
    repeat(8) { e2.update() }
    assertFalse("Disappearing floor must turn non-solid after telegraph", vanish.isSolid)
    assertFalse("Disappearing floor must become invisible", vanish.isVisible)
    assertEquals(TrollTrapState.RETIRED, vanish.state)

    // Delayed Crumble (trap id 7 at Tile 27.2, Y=10.4) stays solid during telegraph then drops
    val crumble = e2.trollTraps.first { it.type == TrollTrapType.DELAYED_CRUMBLE }
    e2.player.x = crumble.initialX + 12f
    e2.player.y = crumble.initialY - e2.player.collisionHeight
    e2.player.vy = 0f
    e2.player.isGrounded = true
    e2.update()
    assertEquals(TrollTrapState.TELEGRAPH, crumble.state)
    assertTrue("Crumble remains solid on first frame of telegraph", crumble.isSolid)
    repeat(crumble.telegraphDurationFrames + 2) { e2.update() }
    assertFalse("Crumble becomes non-solid once dropped", crumble.isSolid)

    // Reset restores both traps cleanly
    e2.resetLevel()
    assertTrue(vanish.isSolid && vanish.isVisible)
    assertEquals(TrollTrapState.ARMED, vanish.state)
    assertTrue(crumble.isSolid && crumble.isVisible)
    assertEquals(TrollTrapState.ARMED, crumble.state)
    assertEquals(0f, crumble.dropOffsetY, 0.01f)
    assertEquals(1f, crumble.fadeAlpha, 0.01f)
  }

  @Test
  fun test03_surpriseSpikesAndFakeSafePlatformsSpikedAndTrapdoor() {
    val e2 = engineFor(2)
    // Trapdoor Fake Safe Platform (id 1, killsPlayer = false)
    val trapdoor = e2.trollTraps.first { it.type == TrollTrapType.FAKE_SAFE_PLATFORM && !it.killsPlayer }
    e2.player.x = trapdoor.initialX + 16f
    e2.player.y = trapdoor.initialY - e2.player.collisionHeight
    e2.player.isGrounded = true
    repeat(trapdoor.telegraphDurationFrames + 4) { e2.update() }
    assertFalse("Trapdoor FAKE_SAFE_PLATFORM must drop out from under feet", trapdoor.isSolid)
    assertFalse("Trapdoor does not instant-kill on touch — gravity drops player into pit", e2.isDying)

    // Spiked Fake Safe Platform (id 4, killsPlayer = true)
    e2.resetLevel()
    val spikedSafe = e2.trollTraps.first { it.type == TrollTrapType.FAKE_SAFE_PLATFORM && it.killsPlayer }
    e2.player.x = spikedSafe.initialX + 16f
    e2.player.y = spikedSafe.initialY - e2.player.collisionHeight
    e2.player.isGrounded = true
    repeat(spikedSafe.telegraphDurationFrames + 3) { e2.update() }
    assertTrue("Spiked FAKE_SAFE_PLATFORM must kill player standing atop it", e2.isDying)
    finishDeathAnimation(e2)
    assertEquals(1, e2.deathCount)
    assertEquals(TrollTrapState.ARMED, spikedSafe.state)
    assertEquals(0f, spikedSafe.morphSpikeHeight, 0.01f)

    // Surprise Spike Pop in Level 1-3 (trap id 8)
    val e3 = engineFor(3)
    val popSpike = e3.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    e3.player.x = popSpike.initialX + 8f
    e3.player.y = popSpike.initialY + popSpike.height - e3.player.collisionHeight
    e3.player.isGrounded = true
    repeat(popSpike.telegraphDurationFrames + 4) { e3.update() }
    assertTrue("SURPRISE_SPIKE_POP must kill player in active zone", e3.isDying)
    finishDeathAnimation(e3)
    assertEquals(TrollTrapState.ARMED, popSpike.state)
    assertEquals(0f, popSpike.morphSpikeHeight, 0.01f)
  }

  @Test
  fun test04_movingSpikesOscillatingAndProximityAmbushAndMovingCrushWall() {
    // 1-2 Oscillating Moving Spike moves cyclically back and forth & kills on contact
    val e2 = engineFor(2)
    val oscSpike = e2.trollTraps.first {
      it.type == TrollTrapType.MOVING_SPIKE && it.triggerType == TrapTriggerType.ON_TIMER_CYCLE
    }
    val startX = oscSpike.x
    repeat(oscSpike.cycleFrames / 2) { e2.update() }
    assertTrue("Oscillating spike must sweep horizontally across its track", oscSpike.x > startX + 40f)
    e2.player.x = oscSpike.x + 8f
    e2.player.y = oscSpike.y + oscSpike.height - e2.player.collisionHeight
    e2.player.isGrounded = true
    e2.update()
    assertTrue("Contact with oscillating MOVING_SPIKE must kill player", e2.isDying)
    finishDeathAnimation(e2)
    assertEquals(startX, oscSpike.initialX, 0.1f)

    // 1-5 Proximity Ambush Moving Spike charges toward player on approach
    val e5 = engineFor(5)
    val ambushSpike = e5.trollTraps.first {
      it.type == TrollTrapType.MOVING_SPIKE && it.triggerType == TrapTriggerType.ON_PROXIMITY_X
    }
    val ambushInitX = ambushSpike.x
    e5.player.x = ambushSpike.triggerMinX + 16f
    e5.player.y = ambushSpike.initialY + ambushSpike.height - e5.player.collisionHeight
    e5.player.isGrounded = true
    repeat(ambushSpike.telegraphDurationFrames + 5) { e5.update() }
    assertTrue("Ambush spike must rush toward targetX", ambushSpike.x < ambushInitX)
    e5.resetLevel()
    assertEquals(ambushInitX, ambushSpike.x, 0.01f)
    assertEquals(TrollTrapState.ARMED, ambushSpike.state)

    // 1-3 Moving Crush Wall slides to targetX, kills if caught inside, and stops at 80px height so jumpable afterward
    val e3 = engineFor(3)
    val wall = e3.trollTraps.first { it.type == TrollTrapType.MOVING_CRUSH_WALL }
    val wallStartX = wall.x
    e3.player.x = wall.triggerMinX + 20f
    e3.player.y = wall.initialY + wall.height - e3.player.collisionHeight
    e3.player.isGrounded = true
    for (f in 0 until wall.telegraphDurationFrames + 40) {
      e3.update()
      if (e3.isDying) break
    }
    assertTrue("Standing in crusher lane must squish Aarav", e3.isDying)
    finishDeathAnimation(e3)
    assertEquals(wallStartX, wall.x, 0.01f)
    assertEquals(TrollTrapState.ARMED, wall.state)

    // Trigger crush wall then step back so it finishes sliding to targetX -> stays jump-clearable (< 156px jump height)
    e3.player.x = wall.triggerMinX + 4f
    e3.player.y = wall.initialY + wall.height - e3.player.collisionHeight
    e3.update()
    e3.player.x = wall.targetX - 96f
    repeat(wall.telegraphDurationFrames + 40) { e3.update() }
    assertEquals(TrollTrapState.RETIRED, wall.state)
    assertEquals(wall.targetX, wall.x, 0.5f)
    assertTrue("Resting crush wall height (80px) must remain well below 156px max jump", wall.height <= 90f)
  }

  @Test
  fun test05_fallingCeilingBlockOnJumpAboveAndOnProximityX() {
    // 1-2 Coin #2 has ON_JUMP_ABOVE trap: walking straight underneath MUST NOT drop it; jumping up DOES drop it!
    val e2 = engineFor(2)
    val jumpTrap = e2.trollTraps.first { it.triggerType == TrapTriggerType.ON_JUMP_ABOVE }
    val initialCeilingY = jumpTrap.y
    e2.player.x = jumpTrap.triggerMinX + 8f
    e2.player.y = 13f * World1Levels.TILE_SIZE - e2.player.collisionHeight
    e2.player.isGrounded = true
    runUntilX(e2, jumpTrap.triggerMaxX + 24f, jump = false, maxFrames = 30)
    assertEquals("Walking calmly under ON_JUMP_ABOVE trap must keep it ARMED", TrollTrapState.ARMED, jumpTrap.state)
    assertEquals(initialCeilingY, jumpTrap.y, 0.01f)

    // Now jump upward underneath the trigger window -> drops!
    e2.player.x = (jumpTrap.triggerMinX + jumpTrap.triggerMaxX) / 2f
    e2.player.y = 13f * World1Levels.TILE_SIZE - e2.player.collisionHeight
    e2.player.isGrounded = true
    stepInputs(e2, frames = 40, right = false, jump = true)
    assertTrue("Jumping under ON_JUMP_ABOVE block must release and kill/slam", e2.isDying || jumpTrap.y > initialCeilingY)
    e2.resetLevel()
    assertEquals(initialCeilingY, jumpTrap.y, 0.01f)
    assertEquals(TrollTrapState.ARMED, jumpTrap.state)

    // 1-4 ON_PROXIMITY_X falling ceiling block drops down to stone deck and rests there without locking path
    val e4 = engineFor(4)
    val t4 = World1Levels.TILE_SIZE
    val oscSpike = e4.trollTraps.first {
      it.type == TrollTrapType.MOVING_SPIKE && it.triggerType == TrapTriggerType.ON_TIMER_CYCLE
    }
    val proxCeiling = e4.trollTraps.first {
      it.type == TrollTrapType.FALLING_CEILING_BLOCK && it.triggerType == TrapTriggerType.ON_PROXIMITY_X
    }
    e4.player.x = 36.2f * t4
    e4.player.y = 12.5f * t4 - e4.player.collisionHeight
    e4.player.isGrounded = true
    waitUntil(e4, maxFrames = 80, description = {
      "oscSpike sweep right >= 41.2T (oscX=${oscSpike.x / t4}T)"
    }) { oscSpike.x >= 41.2f * t4 }
    runUntilX(e4, 38.9f * t4, jump = false, maxFrames = 45)
    assertEquals("Approaching ceiling trigger must enter TELEGRAPH", TrollTrapState.TELEGRAPH, proxCeiling.state)
    stepInputs(e4, frames = 28, left = true)
    waitUntil(e4, maxFrames = proxCeiling.telegraphDurationFrames + 40, description = {
      "proxCeiling RETIRED (state=${proxCeiling.state}, y=${proxCeiling.y})"
    }) { proxCeiling.state == TrollTrapState.RETIRED }
    assertFalse("Aarav must survive baiting ON_PROXIMITY_X ceiling block", e4.isDying)
    assertEquals("Aarav must have zero deaths", 0, e4.deathCount)
    assertEquals("Oscillating spike must remain active", TrollTrapState.ACTIVE, oscSpike.state)
    assertEquals(proxCeiling.targetY, proxCeiling.y, 0.5f)
    assertEquals(TrollTrapState.RETIRED, proxCeiling.state)
    assertTrue("Landed block height must remain easily jumpable", proxCeiling.height <= 64f)
  }

  @Test
  fun test06_fakeExitDoorLethalAndHarmlessRetirePlusRelocatingExitChainAndFakeCheckpoint() {
    // 1-3 Fake Exit Door kills on approach
    val e3 = engineFor(3)
    val fakeDoor = e3.trollTraps.first { it.type == TrollTrapType.FAKE_EXIT_DOOR }
    e3.player.x = fakeDoor.initialX + 12f
    e3.player.y = fakeDoor.initialY + fakeDoor.height - e3.player.collisionHeight
    e3.player.isGrounded = true
    repeat(fakeDoor.telegraphDurationFrames + 3) { e3.update() }
    assertTrue("Approaching lethal FAKE_EXIT_DOOR must trigger death", e3.isDying)
    finishDeathAnimation(e3)
    assertEquals(TrollTrapState.ARMED, fakeDoor.state)

    // Harmless decoy FAKE_EXIT_DOOR (killsPlayer = false, activeDurationFrames = 8) auto-retires & hides
    val t = World1Levels.TILE_SIZE
    val harmlessDoorLevel = LevelDefinition(
        worldId = 1,
        levelId = 3,
        title = "Harmless Door Test",
        tiles = listOf(
            GameTile(1, 0f, 13f * t, t * 20f, t, TileType.CARVED_STONE),
            GameTile(2, 16f * t, 10f * t, t * 2f, t * 3f, TileType.SUN_GATE, isSolid = false)
        ),
        coins = emptyList(),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 11f * t,
        exitX = 16f * t,
        exitY = 10f * t,
        trollTraps = listOf(
            TrollTrap(
                id = 99,
                type = TrollTrapType.FAKE_EXIT_DOOR,
                triggerType = TrapTriggerType.ON_APPROACH_EXIT,
                initialX = 8f * t,
                initialY = 10f * t,
                width = t * 2f,
                height = t * 3f,
                telegraphDurationFrames = 0,
                activeDurationFrames = 8,
                killsPlayer = false
            )
        )
    )
    val harmlessEngine = ChaosGameEngine(harmlessDoorLevel, ProceduralAudioEngine()) { _, _, _ -> }
    val harmlessTrap = harmlessEngine.trollTraps.single()
    harmlessEngine.player.x = 8.2f * t
    harmlessEngine.player.y = 11f * t
    repeat(10) { harmlessEngine.update() }
    assertFalse("Non-lethal decoy door must vanish after activeDurationFrames", harmlessTrap.isVisible)
    assertEquals(TrollTrapState.RETIRED, harmlessTrap.state)
    assertFalse("Non-lethal decoy door must not kill player", harmlessEngine.isDying)

    // 1-3 Chained 2-Hop Relocating Exit Door (39.5T -> 45.5T -> 52.5T) + Reset
    e3.resetLevel()
    assertEquals(39.5f * t, e3.currentExitX, 0.5f)
    e3.player.x = 39.5f * t
    e3.player.y = 11f * t
    e3.update()
    assertEquals("Hop 1 must relocate exit to Sky Terrace (45.5T)", 45.5f * t, e3.currentExitX, 0.5f)
    assertEquals(7.0f * t, e3.currentExitY, 0.5f)
    assertFalse("Hop 1 must not trigger premature victory", e3.isVictorious)

    e3.player.x = 45.5f * t
    e3.player.y = 8.0f * t
    e3.update()
    assertEquals("Hop 2 must relocate exit to Final Terrace (52.5T)", 52.5f * t, e3.currentExitX, 0.5f)
    assertEquals(10.0f * t, e3.currentExitY, 0.5f)
    assertFalse(e3.isVictorious)

    e3.resetLevel()
    assertEquals("Reset must restore original exitX", 39.5f * t, e3.currentExitX, 0.5f)
    assertTrue(e3.trollTraps.filter { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }.all { !it.hasRelocated })

    // 1-2 Fake Checkpoint Bell betrays player without saving checkpoint
    val e2 = engineFor(2)
    val fakeBell = e2.trollTraps.first { it.type == TrollTrapType.FAKE_CHECKPOINT }
    e2.player.x = fakeBell.initialX + 6f
    e2.player.y = fakeBell.initialY + fakeBell.height - e2.player.collisionHeight
    e2.player.isGrounded = true
    repeat(fakeBell.telegraphDurationFrames + 3) { e2.update() }
    assertFalse("Fake checkpoint must NEVER bind real checkpoint", e2.hasCheckpointActivated)
    assertTrue("Fake checkpoint must kill on approach", e2.isDying)
    finishDeathAnimation(e2)
    assertEquals(TrollTrapState.ARMED, fakeBell.state)
  }

  @Test
  fun test07_timedBlinkPlatformsAlternatingCyclesAndDeterministicReset() {
    val e4 = engineFor(4)
    val blinks = e4.trollTraps.filter { it.type == TrollTrapType.TIMED_BLINK_PLATFORM }
    assertEquals(2, blinks.size)
    val padA = blinks[0]
    val padB = blinks[1]

    e4.update()
    assertTrue("Pad A starts solid at cycle frame 1", padA.isSolid)
    assertFalse("Pad B starts ghosted (180 deg offset)", padB.isSolid)

    // Advance half a cycle -> roles swap cleanly
    repeat(padA.cycleFrames / 2) { e4.update() }
    assertFalse("Pad A ghosts out on second half of cycle", padA.isSolid)
    assertTrue("Pad B solidifies on second half of cycle", padB.isSolid)

    // Verify reset restores exact phase offsets
    e4.resetLevel()
    assertEquals(0, padA.stateTimerFrames)
    assertEquals(padA.cycleFrames / 2, padB.stateTimerFrames)
    e4.update()
    assertTrue(padA.isSolid)
    assertFalse(padB.isSolid)
  }

  // --- 3. Real Input-Driven Zero-Softlock Playthroughs (Levels 1-2 to 1-5) ---

  @Test
  fun test08_level12FakeSafePathIsWinnableWithRealControlsAndZeroSoftlocks() {
    val engine = engineFor(2, DifficultyTier.NORMAL)
    val t = World1Levels.TILE_SIZE

    // Jump OVER opening Disappearing Floor (7..8T) onto perch stone (9T), then leap over Trapdoor (10..12T) onto clearing (13T)
    runUntilX(engine, 6.1f * t)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 9.1f * t)
    stepInputs(engine, frames = 26, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Walk calmly across Bait Coin stone (no jump!) so ON_JUMP_ABOVE ceiling stays harmlessly armed above,
    // then jump over new hidden floor spike (18.6T) onto 20..21T
    runUntilX(engine, 16.6f * t, jump = false)
    stepInputs(engine, frames = 22, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Jump over Fake Checkpoint Bell (Tile 22) and land on Upper Timber Step (24.5T, Y=11T)
    runUntilX(engine, 20.6f * t, jump = false)
    stepInputs(engine, frames = 30, right = true, jump = true)
    waitUntilGrounded(engine)
    // Cross step -> Delayed Crumble 1 (27.2T) -> jump -> Delayed Crumble 2 (31T) -> land on Rejoin stone (34..37T)
    runUntilX(engine, 25.4f * t)
    stepInputs(engine, frames = 10, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 28.4f * t)
    stepInputs(engine, frames = 18, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 32.3f * t)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Clear Disappearing Floor (38..39T) onto Island (40..41T)
    runUntilX(engine, 36.8f * t)
    stepInputs(engine, frames = 22, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Clear second Trapdoor (42..43T) onto Final Terrace (44..50T)
    runUntilX(engine, 40.8f * t)
    stepInputs(engine, frames = 22, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Trigger relocating Sun Gate (46.2T -> 51.2T) cleanly on flat approach, then jump over Disappearing Gap (48..49T) into 51.2T!
    runUntilX(engine, 46.5f * t, jump = false)
    assertEquals(51.2f * t, engine.currentExitX, 1f)
    runUntilX(engine, 47.0f * t, jump = false)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine)
    runUntilX(engine, 52.0f * t, jump = false, maxFrames = 80)

    assertFalse("Playthrough 1-2 must not die: x=${engine.player.x / t}", engine.isDying)
    assertEquals(0, engine.deathCount)
    assertTrue("Level 1-2 must be cleanly winnable via real controller inputs", engine.isVictorious)
  }

  @Test
  fun test09_level13ExitIsLyingIsWinnableWithRealControlsAndZeroSoftlocks() {
    val engine = engineFor(3, DifficultyTier.NORMAL)
    val t = World1Levels.TILE_SIZE

    // Jump OVER opening Disappearing Floor (5..6T) onto arena floor (7..9T)
    runUntilX(engine, 4.1f * t)
    stepInputs(engine, frames = 18, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Controlled short hop onto raised step (10..11T, Y=11.5T) so we don't overshoot into Fake Exit Door
    runUntilX(engine, 8.6f * t)
    stepInputs(engine, frames = 7, right = true, jump = true)
    waitUntilGrounded(engine)
    assertEquals(0, engine.deathCount)
    // Jump cleanly OVER Fake Exit Door (13..15T) without overshooting into Pit 18T
    runUntilX(engine, 11.0f * t)
    stepInputs(engine, frames = 14, right = true, jump = true)
    waitUntilGrounded(engine)
    // Clear pit Tile 18 onto start of Crusher corridor (before trigger window 22.4..25.2T)
    runUntilX(engine, 16.9f * t)
    stepInputs(engine, frames = 10, right = true, jump = true)
    waitUntilGrounded(engine)
    assertEquals(0, engine.deathCount)
    // Bait Crusher & Surprise Spike corridor at 22.5T, then step back clear left (< 22T)
    runUntilX(engine, 22.5f * t)
    stepInputs(engine, frames = 20, left = true, right = false, jump = false)
    // Let crusher slam safely onto 22T, then hop cleanly over crusher without hitting 26T surprise spike
    stepInputs(engine, frames = 55, right = false, left = false, jump = false)
    stepInputs(engine, frames = 22, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Jump over Surprise Spike (26T) onto corridor end, then hop onto Delayed Crumble (31..32T)
    runUntilX(engine, 24.9f * t)
    stepInputs(engine, frames = 28, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 28.9f * t)
    stepInputs(engine, frames = 26, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Clear Delayed Crumble -> jump to Threshold (34..42T)
    runUntilX(engine, 32.1f * t)
    stepInputs(engine, frames = 22, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    assertEquals(0, engine.deathCount)
    // Bait Door Guard Ceiling Drop (id=10 at 37.6T) then retreat left and jump over it
    val doorGuard = engine.trollTraps.first { it.id == 10 }
    runUntilX(engine, 36.3f * t)
    stepInputs(engine, frames = 18, left = true)
    waitUntil(engine, maxFrames = 90, description = {
      "doorGuard ceiling RETIRED (state=${doorGuard.state}, y=${doorGuard.y})"
    }) { doorGuard.state == TrollTrapState.RETIRED }
    runUntilX(engine, 36.4f * t)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Approach Initial Sun Gate (39.5T) -> Door Hop #1 to Upper Sky Terrace (45.5T, Y=7T)
    runUntilX(engine, 39.2f * t)
    assertEquals(45.5f * t, engine.currentExitX, 1f)

    // Climb stepping slab (42.3T, Y=11.5T) -> Upper Terrace (44..48T, Y=10T) -> Hop #2!
    runUntilX(engine, 41.2f * t)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 42.8f * t)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 45.2f * t)
    assertEquals(52.5f * t, engine.currentExitX, 1f)

    // Drop to Final Terrace and leap over final pre-gate spike (51.2T) into true Exit (52.5T)
    runUntilX(engine, 48.0f * t)
    stepInputs(engine, frames = 14, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 50.2f * t)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine)
    runUntilX(engine, 53.5f * t, jump = false, maxFrames = 60)

    assertFalse("Playthrough 1-3 must not die: x=${engine.player.x / t}", engine.isDying)
    assertEquals(0, engine.deathCount)
    assertTrue("Level 1-3 must be cleanly winnable via real controller inputs", engine.isVictorious)
  }

  @Test
  fun test10_level14FloorHasPlansIsWinnableWithRealControlsAndZeroSoftlocks() {
    val engine = engineFor(4, DifficultyTier.NORMAL)
    val t = World1Levels.TILE_SIZE

    // Jump OVER Trapdoor edge (6..7T) onto Stair 1 (8..10T, Y=13T), then hop up Stairs 2 -> 3 -> Breather
    runUntilX(engine, 5.0f * t)
    stepInputs(engine, frames = 20, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 9.8f * t)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine)
    runUntilX(engine, 14.8f * t)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine)
    runUntilX(engine, 19.8f * t)
    stepInputs(engine, frames = 10, right = true, jump = true)
    waitUntilGrounded(engine)
    assertEquals(0, engine.deathCount)

    // Settle on Breather (22..25T), wait for Pad A's early solid window (cycle=124), land atop Pad A
    val padA = engine.trollTraps.first { it.id == 4 }
    val padB = engine.trollTraps.first { it.id == 5 }
    runUntilX(engine, 25.0f * t)
    stepInputs(engine, frames = 6)
    waitUntil(engine, maxFrames = 180, description = {
      "Pad A early solid window (state=${padA.state}, timer=${padA.stateTimerFrames})"
    }) { padA.isSolid && padA.stateTimerFrames in 5..22 }
    stepInputs(engine, frames = 14, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // Jump near end of Pad A's solid phase (44..58 of 124) so Pad B solidifies just as Aarav lands on it
    waitUntil(engine, maxFrames = 120, description = {
      "Pad A late solid window before swap (timerA=${padA.stateTimerFrames}, timerB=${padB.stateTimerFrames})"
    }) { padA.isSolid && padA.stateTimerFrames in 44..58 }
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine, right = true)

    // Hop cleanly over Tile 35 disappearing edge onto Stone Patrol Deck safe left edge (36..37T)
    runUntilX(engine, 33.1f * t)
    stepInputs(engine, frames = 20, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    assertEquals(0, engine.deathCount)

    // Wait safely left of oscillating spike until it sweeps past 40.5T, bait proximity ceiling block, then retreat left
    val deckCeiling = engine.trollTraps.first { it.id == 8 }
    val oscSpike = engine.trollTraps.first { it.id == 7 }
    waitUntil(engine, maxFrames = 180, description = {
      "oscSpike sweep right >= 40.5T before bait (oscX=${oscSpike.x / t}T)"
    }) { oscSpike.x >= 40.5f * t }
    runUntilX(engine, 39.55f * t)
    stepInputs(engine, frames = 28, left = true)
    stepInputs(engine, frames = 6)
    waitUntil(engine, maxFrames = 180, description = {
      "deckCeiling RETIRED and oscSpike >= 40.5T (ceil=${deckCeiling.state}, oscX=${oscSpike.x / t}T)"
    }) { deckCeiling.state == TrollTrapState.RETIRED && oscSpike.x >= 40.5f * t }
    waitUntil(engine, maxFrames = 180, description = {
      "oscSpike return left <= 39.2T (oscX=${oscSpike.x / t}T)"
    }) { oscSpike.x <= 39.2f * t }

    // Leap cleanly over both the returning oscillating spike and the landed ceiling block
    stepInputs(engine, frames = 34, right = true, jump = true)
    waitUntilGrounded(engine, right = true)
    assertEquals(0, engine.deathCount)

    // Jump OVER Disappearing bridge (46..47T) onto Bait Island (48..50T)
    runUntilX(engine, 44.0f * t)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // Walk calmly across Island (do NOT jump under ON_JUMP_ABOVE thwomp at 48.8T!), then jump Trapdoor 51.4T -> Sanctum edge!
    runUntilX(engine, 50.0f * t, jump = false)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    // Trigger relocating exit (55.6T -> 61.5T) & chained disappearing gap (58..59T), then jump cleanly onto 60..64T!
    runUntilX(engine, 56.2f * t, jump = false)
    assertEquals(61.5f * t, engine.currentExitX, 1f)
    runUntilX(engine, 56.8f * t, jump = false)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine)
    if (!engine.isVictorious) {
      runUntilX(engine, 62.2f * t, jump = false, maxFrames = 60)
    }

    assertFalse("Playthrough 1-4 must not die: x=${engine.player.x / t}", engine.isDying)
    assertEquals(0, engine.deathCount)
    assertTrue("Level 1-4 must be cleanly winnable via real controller inputs", engine.isVictorious)
  }

  @Test
  fun test11_level15AlmostThereIsWinnableWithRealControlsAndZeroSoftlocks() {
    val engine = engineFor(5, DifficultyTier.NORMAL)
    val t = World1Levels.TILE_SIZE

    // 1. Jump cleanly OVER disappearing planks (7..8T) onto start of Ambush terrace (before 9.8T trigger)
    runUntilX(engine, 5.8f * t)
    stepInputs(engine, frames = 20, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // 2. Trigger proximity Moving Spike (9.8T) and jump cleanly over it while running right
    runUntilX(engine, 9.35f * t)
    stepInputs(engine, frames = 26, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // 3. Synchronize at edge 13.8T so Blink Platform (20..22.5T, cycle=122, solid 0..60) is solid on landing
    runUntilX(engine, 13.8f * t)
    val blink = engine.trollTraps.first { it.type == TrollTrapType.TIMED_BLINK_PLATFORM }
    waitUntil(engine, maxFrames = 180, description = {
      "blink platform sync window 70..85 (timer=${blink.stateTimerFrames}, state=${blink.state})"
    }) { blink.stateTimerFrames in 70..85 }

    // Jump pit -> Delayed Crumble (16..17T, Y=12.2T) -> Blink Platform (20..22.5T, Y=12.0T)
    stepInputs(engine, frames = 16, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 17.0f * t)
    stepInputs(engine, frames = 24, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // 5. Hop across onto start of Crusher terrace (23..24.1T) -> bait crusher & retreat safely left
    runUntilX(engine, 21.8f * t)
    stepInputs(engine, frames = 10, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 24.3f * t)
    stepInputs(engine, frames = 20, left = true)
    val crush = engine.trollTraps.first { it.type == TrollTrapType.MOVING_CRUSH_WALL }
    waitUntil(engine, maxFrames = 120, description = {
      "crush wall RETIRED (state=${crush.state}, x=${crush.x / t}T)"
    }) { crush.state == TrollTrapState.RETIRED }

    // Jump OVER stopped 80px crush wall (24.5T) -> land cleanly -> jump OVER 0-telegraph Fake Bell (29.8T) + Pit (31T) onto 32T
    stepInputs(engine, frames = 28, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 28.1f * t)
    stepInputs(engine, frames = 28, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // 6. Walk flat under ON_JUMP_ABOVE ceiling block (33.8T) to edge 36.0T
    runUntilX(engine, 36.0f * t, jump = false)
    // Jump over Spiked Fake Safe (38..39.8T) onto Delayed Crumble (40.2..41.8T) -> jump onto Lower Arena / Step (43..44T, Y=11.5T)
    stepInputs(engine, frames = 26, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 40.9f * t)
    stepInputs(engine, frames = 20, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // 7. Jump from raised step (44T, Y=11.5T) cleanly OVER Fake Exit Door (46T) -> lands ~48.4T, then bait Ambush Ceiling #14 (49T)!
    runUntilX(engine, 44.0f * t)
    stepInputs(engine, frames = 30, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    val ceil14 = engine.trollTraps.first { it.id == 14 }
    if (ceil14.state == TrollTrapState.ARMED) {
      runUntilX(engine, 48.3f * t)
    }
    if (engine.player.x > 48.28f * t) {
      stepInputs(engine, frames = 2, left = true)
    }
    waitUntil(engine, maxFrames = 90, description = {
      "ceil14 RETIRED (state=${ceil14.state}, y=${ceil14.y})"
    }) { ceil14.state == TrollTrapState.RETIRED }
    stepInputs(engine, frames = 22, right = true, jump = true)
    waitUntilGrounded(engine, right = false)

    // 8. Trigger Final Relocating Door (51.5T -> High Sanctum 60.5T, Y=6.5T)
    runUntilX(engine, 51.0f * t)
    assertEquals(60.5f * t, engine.currentExitX, 1f)

    // 9. Climb stepping stone (54.5T, Y=11.2T) -> High Sanctum edge (57T, Y=9.5T) -> jump over spike (58.4T) + chained gap (59T) into Sun Gate
    runUntilX(engine, 53.0f * t)
    stepInputs(engine, frames = 18, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 55.1f * t)
    stepInputs(engine, frames = 10, right = true, jump = true)
    waitUntilGrounded(engine, right = false)
    runUntilX(engine, 57.1f * t)
    stepInputs(engine, frames = 26, right = true, jump = true)
    waitUntilGrounded(engine)
    if (!engine.isVictorious) {
      runUntilX(engine, 61.5f * t, jump = false, maxFrames = 60)
    }

    assertFalse("Playthrough 1-5 must not die: x=${engine.player.x / t}", engine.isDying)
    assertEquals(0, engine.deathCount)
    assertTrue("Level 1-5 must be cleanly winnable via real controller inputs", engine.isVictorious)
  }

  // --- 4. Repeated Retries, Progression, Persistence & UI Workflows ---

  @Test
  fun test12_repeatedRetriesAndTrapResetAcrossTwentyFiveDeaths() {
    val engine = engineFor(5)
    val pop = engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    val reloc = engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }

    repeat(25) { attempt ->
      // Trigger exit hop + surprise spike death on every single attempt
      engine.player.x = 51.5f * World1Levels.TILE_SIZE
      engine.player.y = 11f * World1Levels.TILE_SIZE
      engine.update()
      assertEquals(60.5f * World1Levels.TILE_SIZE, engine.currentExitX, 1f)
      assertTrue(reloc.hasRelocated)

      engine.player.x = pop.initialX + 8f
      engine.player.y = pop.initialY + pop.height - engine.player.collisionHeight
      engine.player.isGrounded = true
      repeat(pop.telegraphDurationFrames + 3) {
        if (!engine.isDying) engine.update()
      }
      assertTrue("Attempt $attempt must die cleanly", engine.isDying)
      finishDeathAnimation(engine)

      assertEquals("Exit must reset to initialX after attempt $attempt", engine.level.exitX, engine.currentExitX, 0.5f)
      assertFalse("Relocating trap must re-arm after attempt $attempt", reloc.hasRelocated)
      assertEquals(TrollTrapState.ARMED, pop.state)
      assertEquals(0f, pop.morphSpikeHeight, 0.01f)
      assertEquals(engine.level.spawnX, engine.player.x, 1f)
    }
    assertEquals(25, engine.deathCount)
  }

  @Test
  fun test13_sequentialUnlockingLockRejectionAndPersistentProgress() {
    val vm = GameStateViewModel(app)
    val repo = vm.saveRepository

    // Initial state: only 1-1 unlocked
    assertTrue(repo.isLevelUnlocked(1, 1))
    for (id in 2..5) {
      assertFalse("Level 1-$id must start locked", repo.isLevelUnlocked(1, id))
      vm.startLevel(levelId = id, enforceUnlock = true)
      assertEquals("Launching locked level 1-$id must remain on MAIN_MENU", AppScreen.MAIN_MENU, vm.currentScreen.value)
    }

    // Sequential clear of 1-1 -> unlocks 1-2 -> ... -> unlocks 1-5 -> completes World 1
    for (id in 1..5) {
      vm.startLevel(levelId = id, enforceUnlock = true)
      assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
      val e = vm.gameEngine!!
      val firstCoin = e.coins.first()
      e.player.x = firstCoin.x - e.player.collisionWidth / 2f
      e.player.y = firstCoin.y - e.player.collisionHeight / 2f
      e.update()
      assertTrue("Collecting coin through ChaosGameEngine on 1-$id must raise score", e.score > 0)
      e.coins.forEach { it.isCollected = true }
      var hops = 0
      while (hops < 3) {
        val reloc = e.trollTraps.firstOrNull {
          it.type == TrollTrapType.RELOCATING_EXIT_DOOR && !it.hasRelocated &&
              kotlin.math.abs(it.initialX - e.currentExitX) < 12f
        } ?: break
        e.player.x = (reloc.triggerMinX + reloc.triggerMaxX) / 2f - e.player.collisionWidth / 2f
        e.player.y = (reloc.triggerMinY + reloc.triggerMaxY) / 2f - e.player.collisionHeight / 2f
        e.update()
        hops++
      }
      e.player.x = e.currentExitX + 24f
      e.player.y = e.currentExitY + 60f
      e.update()

      val victory = vm.victoryResult.value
      assertNotNull("Victory result must emit on 1-$id", victory)
      assertEquals(3, victory!!.stars)
      if (id < 5) {
        assertTrue("Clearing 1-$id must unlock 1-${id + 1}", repo.isLevelUnlocked(1, id + 1))
        assertTrue(victory.nextLevelUnlocked)
      }
    }

    // Re-create ViewModel / SaveRepository -> verify persistent completion, stars, times, scores
    val freshVm = GameStateViewModel(app)
    val progress = freshVm.levelsProgress.value
    assertEquals(5, progress.size)
    for (item in progress) {
      assertTrue("1-${item.levelId} must persist unlocked", item.isUnlocked)
      assertTrue("1-${item.levelId} must persist completed", item.isCompleted)
      assertEquals(3, item.stars)
      assertTrue(item.bestTimeMs > 0L)
      assertTrue(item.bestScore > 0)
    }
  }

  @Test
  fun test14_pauseResumeVictoryNextLevelUiAndCanvasRenderAllFiveLevels() {
    // 1. Unlock levels via SaveRepository and check ChapterTrail + Pause/Resume UI flows
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.saveRepository.saveBestTime(1, 1, 12000L)
      vm.saveRepository.saveStars(1, 1, 3)
      vm.saveRepository.saveBestScore(1, 1, 400)
      vm.refreshSavedProgress()
    }
    composeTestRule.waitForIdle()

    // Verify minimal Home Screen hides Chapter Trail while internal progression unlocks 1-2 cleanly
    composeTestRule.onNodeWithTag("button_tap_to_start").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      assertTrue(vm.levelsProgress.value.first { it.levelId == 2 }.isUnlocked)
      assertFalse(vm.levelsProgress.value.first { it.levelId == 3 }.isUnlocked)
      vm.startLevel(2, enforceUnlock = true)
    }
    composeTestRule.waitForIdle()

    // Pause -> verify simulation freezes -> Resume -> verify simulation advances
    composeTestRule.onNodeWithTag("button_pause").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_pause").assertIsDisplayed()
    var engineBeforePauseTime = 0L
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      val e = vm.gameEngine!!
      assertTrue(e.isPaused)
      engineBeforePauseTime = e.levelTimerMs
      repeat(20) { e.update() }
      assertEquals("Paused engine must not advance timer", engineBeforePauseTime, e.levelTimerMs)
    }

    composeTestRule.onNodeWithTag("button_resume").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      val e = vm.gameEngine!!
      assertFalse(e.isPaused)
      repeat(5) { e.update() }
      assertTrue("Resumed engine advances cleanly", e.levelTimerMs > engineBeforePauseTime)
    }

    // Complete 1-2 -> click Next Trial -> verify 1-3 opens
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      val e = vm.gameEngine!!
      e.player.x = e.currentExitX + 24f
      e.player.y = e.currentExitY + 60f
      e.update()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_victory").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_victory_next_level").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      assertEquals(3, vm.activeLevelId.value)
      assertEquals("The Exit Is Lying", vm.gameEngine?.level?.title)
    }

    // Render every level 1-1..1-5 via ChaosGameRenderer onto an offscreen Bitmap without throwing
    val renderer = ChaosGameRenderer(app)
    val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    for (id in 1..5) {
      val e = engineFor(id)
      // Step several frames including an active/telegraph trap
      repeat(10) { e.update() }
      renderer.render(canvas, e, 1280f, 720f)
    }
  }
}
