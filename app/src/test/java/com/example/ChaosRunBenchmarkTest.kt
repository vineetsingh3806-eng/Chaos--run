package com.example

import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ChaosRunBenchmarkTest {

  private lateinit var level: LevelDefinition
  private lateinit var audioEngine: ProceduralAudioEngine
  private lateinit var engine: ChaosGameEngine

  @Before
  fun setUp() {
    level = World1Levels.createLevel1()
    audioEngine = ProceduralAudioEngine()
    engine = ChaosGameEngine(level, audioEngine) { _, _, _ -> }
  }

  @Test
  fun testPlayerKinematicsAndJumpImpulse() {
    val player = engine.player
    player.isGrounded = true

    // Horizontal acceleration test
    engine.inputRight = true
    engine.update()
    assertTrue("Player vx should accelerate right", player.vx > 0f)
    assertTrue("Player vx must not exceed MAX_RUN_SPEED", player.vx <= ChaosGameEngine.MAX_RUN_SPEED)

    // Ground jump impulse test
    engine.inputJump = true
    engine.update()
    val expectedVy = ChaosGameEngine.JUMP_IMPULSE + ChaosGameEngine.GRAVITY
    assertEquals("Jump impulse plus gravity", expectedVy, player.vy, 0.1f)
    assertFalse("Player must not be grounded after jump impulse", player.isGrounded)
  }

  @Test
  fun testShortHopVariableJumpCut() {
    val player = engine.player
    player.isGrounded = true

    // Trigger jump
    engine.inputJump = true
    engine.update()
    val initialVy = player.vy // approx -13.2 + gravity

    // Release jump while ascending
    engine.inputJump = false
    engine.update()

    // vy should be cut by approx 0.42
    assertTrue("Variable jump cut must reduce upward velocity", player.vy > initialVy * 0.6f)
    assertTrue("Jump cut flag must be set to prevent repeated cuts", player.hasJumpCutApplied)
  }

  @Test
  fun testCoyoteTimeAllowsLateJump() {
    val player = engine.player
    player.isGrounded = true
    engine.update()

    // Step off platform into air
    player.isGrounded = false
    player.coyoteTimer = ChaosGameEngine.COYOTE_FRAMES

    // Press jump within coyote window
    engine.inputJump = true
    engine.update()

    assertEquals("Jump must trigger during coyote time", ChaosGameEngine.JUMP_IMPULSE, player.vy, 0.6f)
    assertEquals("Coyote timer should be consumed", 0, player.coyoteTimer)
  }

  @Test
  fun testDisappearingFloorTopContactOnly() {
    val floorTile = GameTile(
        id = 999,
        x = 100f,
        y = 200f,
        width = 48f,
        height = 48f,
        type = TileType.DISAPPEARING_FLOOR,
        isVisible = true,
        isSolid = true,
        isTriggered = false
    )
    val player = engine.player

    // CASE 1: Valid Downward Landing (vy > 0.1, was above, crossed top) -> MUST TRIGGER
    player.x = 108f // Centered horizontally
    player.prevY = 140f // Bottom was at 140 + 54 = 194 (above tile.y = 200)
    player.y = 150f     // Bottom is at 150 + 54 = 204 (crossed surface)
    player.vy = 4.0f    // Downward motion

    val triggered = engine.evaluateDisappearingFloor(floorTile)
    assertTrue("Valid downward landing MUST trigger disappearing floor", triggered)
    assertTrue("Tile must be marked triggered", floorTile.isTriggered)
    assertFalse("Tile must become non-solid immediately", floorTile.isSolid)
    assertFalse("Tile must become invisible immediately", floorTile.isVisible)

    // Reset tile
    floorTile.isTriggered = false
    floorTile.isSolid = true
    floorTile.isVisible = true

    // CASE 2: Stationary standing (vy == 0) -> MUST NOT TRIGGER
    player.vy = 0f
    val stationaryTriggered = engine.evaluateDisappearingFloor(floorTile)
    assertFalse("Stationary contact must NOT trigger disappearing floor", stationaryTriggered)
    assertTrue("Tile must remain solid", floorTile.isSolid)

    // CASE 3: Underside contact (vy < 0, jumping up through bottom) -> MUST NOT TRIGGER
    player.vy = -6.0f
    player.prevY = 260f
    player.y = 250f
    val undersideTriggered = engine.evaluateDisappearingFloor(floorTile)
    assertFalse("Underside contact must NOT trigger disappearing floor", undersideTriggered)
    assertTrue("Tile must remain solid ceiling", floorTile.isSolid)

    // CASE 4: Side contact (grazing wall horizontally) -> MUST NOT TRIGGER
    player.vy = 3.0f
    player.prevY = 210f // Bottom was already at 264 (below tile top 200)
    player.y = 213f
    player.x = 50f // Horizontally outside left edge
    val sideTriggered = engine.evaluateDisappearingFloor(floorTile)
    assertFalse("Side contact must NOT trigger disappearing floor", sideTriggered)
    assertTrue("Tile must remain solid", floorTile.isSolid)
  }

  @Test
  fun testCheckpointSnapshotAndDeathRollback() {
    val t = 48f
    val cpLevel = LevelDefinition(
        worldId = 1,
        levelId = 1,
        title = "Checkpoint Benchmark",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 13f * t, width = t * 20f, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 10f * t, y = 12f * t, width = t, height = t, type = TileType.CHECKPOINT_BELL, isSolid = false),
            GameTile(id = 3, x = 14f * t, y = 13f * t, width = t, height = t, type = TileType.DISAPPEARING_FLOOR)
        ),
        coins = listOf(
            Coin(id = 1, x = 5f * t, y = 11.5f * t),
            Coin(id = 2, x = 13f * t, y = 11.5f * t)
        ),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 11f * t,
        exitX = 18f * t,
        exitY = 10f * t
    )
    val cpEngine = ChaosGameEngine(cpLevel, audioEngine) { _, _, _ -> }

    // 1. Collect coin #1 before checkpoint
    val coin1 = cpEngine.coins.first { it.id == 1 }
    coin1.isCollected = true

    // 2. Trigger Checkpoint bell at Tile 10
    cpEngine.player.x = 10f * t + 8f
    cpEngine.player.y = 12f * t - cpEngine.player.collisionHeight / 2f
    cpEngine.update()

    assertNotNull("Checkpoint snapshot must be captured", cpEngine.checkpointSnapshot)
    assertTrue(
        "Checkpoint snapshot must remember coin #1",
        cpEngine.checkpointSnapshot!!.coinsCollectedAtCheckpoint.contains(1)
    )

    // 3. Collect coin #2 AFTER checkpoint
    val coin2 = cpEngine.coins.first { it.id == 2 }
    coin2.isCollected = true

    // 4. Trigger a disappearing floor after checkpoint
    val disappearTile = cpEngine.tiles.first { it.type == TileType.DISAPPEARING_FLOOR }
    disappearTile.isTriggered = true
    disappearTile.isSolid = false
    disappearTile.isVisible = false

    // 5. Trigger Player Death
    cpEngine.handlePlayerDeath()

    // 6. Verify Rollback
    assertEquals("Player must respawn at checkpoint X", cpEngine.checkpointSnapshot!!.playerX, cpEngine.player.x, 1.0f)
    assertTrue("Coin #1 collected BEFORE checkpoint must remain collected", coin1.isCollected)
    assertFalse("Coin #2 collected AFTER checkpoint must be respawned (uncollected)", coin2.isCollected)
  }

  @Test
  fun testLevel1JumpGapsReachability() {
    // Kinematics verification
    val airTimeFrames = (2.0 * Math.abs(ChaosGameEngine.JUMP_IMPULSE) / ChaosGameEngine.GRAVITY).toFloat()
    val runningReachGU = ChaosGameEngine.MAX_RUN_SPEED * airTimeFrames // ~295 GU = ~6.1 tiles

    // Mandatory Gap 1: 3 tiles = 144 GU
    val gap1Distance = 3 * 48f
    assertTrue("Gap 1 must be reachable by running jump", runningReachGU > gap1Distance)
    val margin1 = (runningReachGU - gap1Distance) / gap1Distance * 100f
    assertTrue("Gap 1 safety margin must exceed 50%", margin1 > 50f)

    // Mandatory Gap 2: River gap = 144 GU
    val gap2Distance = 3 * 48f
    assertTrue("Gap 2 must be reachable", runningReachGU > gap2Distance)

    // Secret Ledge: Y difference is 2 tiles (96 GU)
    val secretRiseGU = 96f
    val maxJumpHeightGU = (ChaosGameEngine.JUMP_IMPULSE * ChaosGameEngine.JUMP_IMPULSE) / (2f * ChaosGameEngine.GRAVITY)
    assertTrue("Max jump height (~150 GU) must clear secret rise (96 GU)", maxJumpHeightGU > secretRiseGU)
    val secretMargin = maxJumpHeightGU - secretRiseGU
    assertTrue("Secret ledge must have at least 40 GU clearance", secretMargin >= 40f)
  }
}
