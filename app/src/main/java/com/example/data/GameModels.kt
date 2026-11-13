package com.example.data

/**
 * Core data models and snapshot representations for CHAOS RUN.
 */

enum class TileType {
  SOLID_EARTH,
  MOSSY_TIMBER,
  CARVED_STONE,
  CRUMBLING_WOOD,
  DISAPPEARING_FLOOR,
  BRAMBLE_SPIKES,
  CHECKPOINT_BELL,
  SUN_GATE
}

enum class AaravState {
  IDLE,
  RUN,
  JUMP_UP,
  FALL_DOWN,
  LAND,
  DAMAGE,
  DEATH,
  VICTORY
}

data class GameTile(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val type: TileType,
    var isVisible: Boolean = true,
    var isSolid: Boolean = true,
    var isTriggered: Boolean = false,
    var crumbleTimerFrames: Int = 0,
    var isCrumbling: Boolean = false
)

data class Coin(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 14f,
    var isCollected: Boolean = false,
    var animTimer: Float = 0f
)

data class MovingPlatform(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float = 144f, // 3 tiles wide (48 * 3)
    val height: Float = 24f, // half tile height
    val minX: Float = 3936f,
    val maxX: Float = 4224f,
    val centerX: Float = 4080f,
    val amplitude: Float = 144f,
    val periodFrames: Int = 192, // 3.2 seconds at 60 FPS
    var currentFrame: Int = 0,
    var vx: Float = 0f
)

data class Player(
    var x: Float,
    var y: Float,
    var prevX: Float = x,
    var prevY: Float = y,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val collisionWidth: Float = 32f,
    val collisionHeight: Float = 54f,
    var isGrounded: Boolean = false,
    var facingRight: Boolean = true,
    var state: AaravState = AaravState.IDLE,
    var stateTimerFrames: Int = 0,
    var coyoteTimer: Int = 0,
    var jumpBufferTimer: Int = 0,
    var hasJumpCutApplied: Boolean = false,
    var health: Int = 3,
    var maxHealth: Int = 3,
    var invulnerableFrames: Int = 0
)

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 1f,
    var color: Int = 0,
    var size: Float = 4f,
    var active: Boolean = false
)

// Checkpoint Snapshot Models for deterministic rollback
data class TileSnapshot(
    val tileId: Int,
    val isVisible: Boolean,
    val isSolid: Boolean,
    val isTriggered: Boolean,
    val crumbleTimerFrames: Int,
    val isCrumbling: Boolean
)

data class MovingPlatformSnapshot(
    val platformId: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val currentFrame: Int
)

data class LevelCheckpointSnapshot(
    val playerX: Float,
    val playerY: Float,
    val playerFacingRight: Boolean,
    val playerHealth: Int,
    val levelTimerMs: Long,
    val coinsCollectedAtCheckpoint: Set<Int>,
    val tileSnapshots: List<TileSnapshot>,
    val movingPlatformSnapshots: List<MovingPlatformSnapshot>
)

data class LevelDefinition(
    val worldId: Int,
    val levelId: Int,
    val title: String,
    val tiles: List<GameTile>,
    val coins: List<Coin>,
    val movingPlatforms: List<MovingPlatform>,
    val spawnX: Float,
    val spawnY: Float,
    val exitX: Float,
    val exitY: Float,
    val parTimeSeconds: Int = 60
)
