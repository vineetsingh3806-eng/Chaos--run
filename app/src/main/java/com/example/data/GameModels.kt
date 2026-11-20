package com.example.data

/**
 * Core data models and snapshot representations for CHAOS RUN.
 */

enum class TileType {
  SOLID_EARTH,
  MOSSY_TIMBER,
  CARVED_STONE,
  CRUMBLING_WOOD,
  VANISHING_SLAB,
  DISAPPEARING_FLOOR,
  BRAMBLE_SPIKES,
  CHECKPOINT_BELL,
  SUN_GATE,
  ECHO_BRIDGE,
  SOUND_GATE,
  FALSE_PATH_WALL,
  SLIPPERY_STONE
}

enum class LevelTheme {
  WHISPERING_WOODS,
  HOLLOW_OF_ECHOES,
  RUINS_OF_LOST_KING,
  MONSOON_RUN,
  TEMPLE_OF_LAST_LIGHT
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

enum class DifficultyTier(
    val label: String,
    val subtitle: String,
    val startingHealth: Int,
    val crumbleFrames: Int,
    val vanishingFrames: Int,
    val hazardWarningFrames: Int
) {
  NORMAL(
      label = "NORMAL",
      subtitle = "Sharp hazards • 3 Hearts",
      startingHealth = 3,
      crumbleFrames = 45,
      vanishingFrames = 34,
      hazardWarningFrames = 34
  ),
  HARD(
      label = "HARD",
      subtitle = "Tight timing • 2-Hit Guardian",
      startingHealth = 3,
      crumbleFrames = 34,
      vanishingFrames = 24,
      hazardWarningFrames = 24
  ),
  NIGHTMARE(
      label = "NIGHTMARE",
      subtitle = "2 Hearts • Relentless traps",
      startingHealth = 2,
      crumbleFrames = 26,
      vanishingFrames = 18,
      hazardWarningFrames = 18
  )
}

/**
 * Exactly four permanent story clues for World 1 Level 1 ("The Forest Is Watching").
 */
enum class StoryClueId(
    val number: Int,
    val title: String,
    val locationHint: String,
    val whisperText: String,
    val fieldNote: String
) {
  ASH_PRINTS(
      number = 1,
      title = "Ash-Prints",
      locationHint = "Beat 1 • Mossy Clearing",
      whisperText = "Warm ash-prints lead deeper into the woods... someone passed here before you.",
      fieldNote = "Smoldering footprints pressed into the damp timber curve toward the river gap. Whoever left them crossed shortly before dawn—moving with quiet intent, not panic."
  ),
  WATCHER(
      number = 2,
      title = "Watcher",
      locationHint = "Beat 2 • Canopy Clearing",
      whisperText = "A quiet silhouette watches from the canopy... then melts into the leaves.",
      fieldNote = "High among the cedar boughs, a cloaked figure with pale amber eyes observed Aarav cross the trembling bridge before vanishing without breaking a twig."
  ),
  CHECKPOINT_BELL_GLYPH(
      number = 3,
      title = "Checkpoint Bell Glyph",
      locationHint = "Beat 3 • Shrine Terrace",
      whisperText = "A teal binding glyph awakens on the shrine bell — the forest remembers this moment.",
      fieldNote = "Etched inside the weathered brass bell is a twin-spiral sigil. When struck, it anchors Aarav's footing in memory, letting him return if the trails give way."
  ),
  ANCIENT_CARVING(
      number = 4,
      title = "Ancient Carving",
      locationHint = "Beat 5 • Overlook Ruins",
      whisperText = "Carved into the stone: 'The gate answers those who look above the trail.'",
      fieldNote = "A moss-draped stele overlooking the final chasm bears a faded relief of the Sun Gate and an arrow pointing toward a high ruin alcove above the stone path."
  )
}

enum class NotificationCategory {
  STORY_CLUE,
  EVENT_WARNING,
  SECRET_DISCOVERY,
  TRIUMPH
}

enum class NotificationPriority(
    val rank: Int,
    val defaultDurationFrames: Int,
    val minVisibleFrames: Int
) {
  INFO(rank = 1, defaultDurationFrames = 90, minVisibleFrames = 30),
  STORY(rank = 2, defaultDurationFrames = 120, minVisibleFrames = 40),
  DISCOVERY(rank = 3, defaultDurationFrames = 120, minVisibleFrames = 45),
  CRITICAL(rank = 4, defaultDurationFrames = 150, minVisibleFrames = 85)
}

enum class NotificationVisualKind {
  STORY,
  DISCOVERY,
  FALLING_HAZARD,
  LIGHTNING_HAZARD,
  BOSS_WARNING,
  HAZARD_WARNING,
  TRIUMPH,
  INFO
}

data class GameplayNotification(
    val eventKey: String,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val priority: NotificationPriority,
    val visualKind: NotificationVisualKind,
    val durationFrames: Int = priority.defaultDurationFrames,
    val minVisibleFrames: Int = priority.minVisibleFrames,
    val worldFocusX: Float? = null,
    val worldFocusY: Float? = null,
    val supersedesKeys: Set<String> = emptySet()
)

/**
 * All 7 approved gameplay events (E1-E7) for World 1 Level 1.
 */
enum class GameplayEventId {
  E1_ASH_PRINTS,
  E2_WATCHER_CANOPY,
  E3_CHECKPOINT_BELL_GLYPH,
  E4_RESONANCE_STONE_BASIN,
  E5_CANOPY_ECHO_SEAL,
  E6_ANCIENT_CARVING,
  E7_SUN_GATE_AWAKENING
}

data class ClueMarker(
    val clueId: StoryClueId,
    val x: Float,
    val y: Float,
    val triggerRadius: Float = 52f
)

/**
 * Optional bonus environmental discovery in the lower basin (NOT counted as a story clue).
 */
data class ResonanceStone(
    val x: Float,
    val y: Float,
    val radius: Float = 18f,
    var isCollected: Boolean = false,
    var animTimer: Float = 0f
)

/**
 * Temporary puzzle trigger node (rolls back deterministically on checkpoint death).
 */
data class EchoPuzzleNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val triggerRadius: Float = 44f,
    var isActivated: Boolean = false
)

data class WatcherAppearance(
    var isActive: Boolean = false,
    var worldX: Float = 0f,
    var worldY: Float = 0f,
    var remainingFrames: Int = 0,
    var totalFrames: Int = 150
)

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
    var isCrumbling: Boolean = false,
    val groupId: Int = 0
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
    val maxX: Float = 4128f,
    val centerX: Float = 4032f,
    val amplitude: Float = 96f,
    val periodFrames: Int = 192, // Verified 192-frame period (3.2s at 60 FPS)
    var currentFrame: Int = 0,
    var vx: Float = 0f,
    val centerY: Float = y,
    val amplitudeY: Float = 0f,
    var vy: Float = 0f
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

/**
 * Interactive Indian Dholak / Lotus Bounce Drum that launches Aarav upward and chains combos.
 */
data class BounceDrum(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float = 52f,
    val height: Float = 20f,
    val launchVy: Float = -17.6f,
    var squashFrames: Int = 0
)

enum class CritterBehavior {
  PATROL,
  LUNGE_ON_PROXIMITY,
  HOPPING
}

/**
 * Stompable armored forest critter (Kanta Beetle) patrolling flat platforms.
 * Stomping top stuns it and bounces Aarav; side contact deals 1 heart damage.
 */
data class ForestCritter(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float = 36f,
    val height: Float = 26f,
    val minX: Float,
    val maxX: Float,
    var vx: Float = 1.6f,
    var stunFrames: Int = 0,
    var walkTick: Int = 0,
    val behavior: CritterBehavior = CritterBehavior.PATROL,
    var alertFrames: Int = 0,
    var lungeFrames: Int = 0,
    var hopTimer: Int = 0,
    var vy: Float = 0f,
    val baseY: Float = y
)

enum class TrapPhase {
  ARMED,
  WARNING,
  ACTIVE,
  COOLDOWN
}

/**
 * Telegraphed falling boulder / temple keystone with ground warning shadow prior to drop.
 */
data class FallingHazard(
    val id: Int,
    val x: Float,
    val startY: Float,
    val groundY: Float,
    val triggerMinX: Float,
    val triggerMaxX: Float,
    val radius: Float = 14f,
    val warningDurationFrames: Int = 34,
    var y: Float = startY,
    var vy: Float = 0f,
    var warningFrames: Int = 0,
    var isWarning: Boolean = false,
    var isFalling: Boolean = false,
    var hasTriggered: Boolean = false
)

/**
 * Pressure-triggered thorn/spike mechanism with glowing seam telegraph before snapping upward.
 */
data class AmbushSpikeTrap(
    val id: Int,
    val x: Float,
    val groundY: Float,
    val width: Float = 44f,
    val maxHeight: Float = 26f,
    val triggerPadding: Float = 54f,
    val warningDurationFrames: Int = 28,
    val activeDurationFrames: Int = 55,
    var phase: TrapPhase = TrapPhase.ARMED,
    var phaseTimerFrames: Int = 0,
    var currentHeight: Float = 0f
)

/**
 * Periodic bamboo / monsoon breeze updraft lifting Aarav gently across chasms & secret lines.
 */
data class WindDraft(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val liftAccel: Float = 1.05f,
    val maxRiseSpeed: Float = -6.8f
)

/**
 * Rhythmic swinging brass Temple Ghanta / vine pendulum with telegraphed sinusoidal arc.
 */
data class SwingingPendulum(
    val id: Int,
    val anchorX: Float,
    val anchorY: Float,
    val length: Float,
    val maxAngleRad: Float = 0.62f,
    val periodFrames: Int = 150,
    val bobRadius: Float = 17f,
    var currentFrame: Int = 0,
    var bobX: Float = anchorX,
    var bobY: Float = anchorY + length
)

/**
 * Optional suspended Brass Festival Diya that ignites on contact for bonus score & celebratory sparks.
 */
data class ForestDiya(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 18f,
    var isLit: Boolean = false
)

/**
 * Zero-allocation pre-pooled Canvas popup indicator (+100, x2!, BOUNCE!, etc.).
 */
data class FloatingPopup(
    var x: Float = 0f,
    var y: Float = 0f,
    var vy: Float = -1.2f,
    var text: String = "",
    var color: Int = 0xFFFDE047.toInt(),
    var life: Float = 0f,
    var maxLife: Float = 0.9f,
    var active: Boolean = false
)

// --- Set Piece A: The Awakening Banyan Tree ---

enum class TreePhase {
  IDLE,
  WARNING,
  ACTIVE,
  RESOLVED
}

data class RootPillar(
    val id: Int,
    val x: Float,
    val baseY: Float,
    val width: Float,
    val maxHeight: Float,
    var currentHeight: Float = 0f
)

data class FallingAcorn(
    var x: Float = 0f,
    var y: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 10f,
    var active: Boolean = false
)

data class AwakeningTreeState(
    val trunkX: Float = 2040f, // Tile 42.5
    val groundY: Float = 13 * 48f, // 624f
    var phase: TreePhase = TreePhase.IDLE,
    var timerFrames: Int = 0,
    val roots: List<RootPillar> = listOf(
        RootPillar(id = 1, x = 1974f, baseY = 624f, width = 34f, maxHeight = 52f),   // Step root on Tile 41.1
        RootPillar(id = 2, x = 2036f, baseY = 624f, width = 44f, maxHeight = 104f),  // Tall blocking trunk on Tile 42.4
        RootPillar(id = 3, x = 2104f, baseY = 624f, width = 34f, maxHeight = 44f)    // Descent root step on Tile 43.8
    ),
    val acorns: Array<FallingAcorn> = Array(4) { FallingAcorn() }
)

// --- Set Piece B: The Great Forest Chase (Vanar Shadow Stalker) ---

enum class StalkerPhase {
  IDLE,
  WARNING,
  CHASING,
  REPELLED
}

data class ForestStalkerState(
    var phase: StalkerPhase = StalkerPhase.IDLE,
    var x: Float = 3000f,
    var y: Float = 12 * 48f - 42f,
    var vx: Float = 0f,
    val width: Float = 54f,
    val height: Float = 42f,
    var phaseTimerFrames: Int = 0,
    var strideTick: Int = 0,
    var stunPauseFrames: Int = 0
)

// --- Set Piece D + C: The Vanishing Causeway & Sleeping Stone Guardian (Shila-Rakshak) ---

enum class GuardianPhase {
  SLEEPING,
  AWAKENING,
  ACTIVE,
  DEFEATED
}

data class GuardianShockwave(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = -5.4f,
    val radius: Float = 15f,
    var active: Boolean = false,
    var isRebound: Boolean = false,
    var hasReversed: Boolean = false,
    var reboundX: Float = 5080f
)

data class StoneGuardianState(
    val x: Float = 5392f, // Tile 112.33, blocking approach before Sun Gate (Tile 115)
    val groundY: Float = 7 * 48f, // 336f
    val width: Float = 66f,
    val height: Float = 94f,
    val crystalX: Float = 5425f, // Glowing Sun Crown Prism above Guardian head
    val crystalY: Float = 7 * 48f - 122f,
    val crystalRadius: Float = 24f,
    var phase: GuardianPhase = GuardianPhase.SLEEPING,
    var phaseTimerFrames: Int = 0,
    var attackCooldownFrames: Int = 65,
    var windupFrames: Int = 0,
    var hitsRemaining: Int = 1,
    var maxHits: Int = 1,
    var crystalInvulnFrames: Int = 0,
    val shockwaves: Array<GuardianShockwave> = Array(4) { GuardianShockwave() },
    val triggerMinX: Float = 5060f,
    val triggerMaxX: Float = 5380f,
    val reboundX: Float = 5150f,
    val despawnMinX: Float = 4880f,
    val isTempleBoss: Boolean = false
)

// --- World 1 Expansion Mechanics (1-2 to 1-5) ---

/**
 * Sound-triggered resonant bell / stone chime that activates matching groupId
 * ECHO_BRIDGE tiles (becoming solid) and SOUND_GATE barriers (opening passage).
 * durationFrames < 0 makes the switch permanent once struck (until checkpoint rollback).
 */
data class SoundSwitchNode(
    val id: Int,
    val groupId: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 28f,
    val durationFrames: Int = 340,
    var isActivated: Boolean = false,
    var remainingFrames: Int = 0,
    var pulseAnimFrames: Int = 0
)

/**
 * Rotating stone platform or spiked clockwork gear orbiting around a carved astrolabe pivot.
 */
data class OrbitingPlatform(
    val id: Int,
    val pivotX: Float,
    val pivotY: Float,
    val orbitRadius: Float,
    val width: Float = 104f,
    val height: Float = 22f,
    val periodFrames: Int = 240,
    val phaseOffsetRad: Float = 0f,
    val clockwise: Boolean = true,
    val isHazardGear: Boolean = false,
    var currentFrame: Int = 0,
    var x: Float = pivotX + orbitRadius - width / 2f,
    var y: Float = pivotY - height / 2f,
    var vx: Float = 0f,
    var vy: Float = 0f
)

/**
 * Tall carved temple pillar that rumbles on footfall then sinks downward.
 */
data class CollapsingPillar(
    val id: Int,
    val x: Float,
    val topY: Float,
    val width: Float = 52f,
    val height: Float = 156f,
    val sinkDistance: Float = 128f,
    val warningDurationFrames: Int = 30,
    var currentDropY: Float = 0f,
    var phase: TrapPhase = TrapPhase.ARMED,
    var timerFrames: Int = 0
)

/**
 * Rushing monsoon floodwater channel pushing Aarav horizontally (positive = surge, negative = counter-current).
 */
data class WaterCurrentZone(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val flowVx: Float,
    val flowVy: Float = 0f
)

/**
 * Telegraphed vertical storm lightning column that flashes warning beams before striking.
 */
data class LightningHazard(
    val id: Int,
    val x: Float,
    val topY: Float,
    val groundY: Float,
    val width: Float = 36f,
    val cycleFrames: Int = 170,
    val phaseOffsetFrames: Int = 0,
    val warningFrames: Int = 44,
    val strikeFrames: Int = 16,
    var currentFrame: Int = phaseOffsetFrames,
    var phase: TrapPhase = TrapPhase.ARMED
)

/**
 * Hidden secret relic collectible per expansion level (1-2 to 1-5), saved permanently on discovery.
 */
data class LevelRelic(
    val id: String,
    val levelId: Int,
    val title: String,
    val hint: String,
    val x: Float,
    val y: Float,
    val radius: Float = 20f,
    var isCollected: Boolean = false,
    var animTimer: Float = 0f
)

data class GuardianEncounterConfig(
    val name: String = "Shila-Rakshak",
    val x: Float = 5392f,
    val groundY: Float = 7 * 48f,
    val crystalX: Float = 5425f,
    val crystalY: Float = 7 * 48f - 122f,
    val triggerMinX: Float = 5060f,
    val triggerMaxX: Float = 5380f,
    val reboundX: Float = 5150f,
    val despawnMinX: Float = 4880f,
    val baseHitsNormal: Int = 1,
    val baseHitsHard: Int = 2,
    val baseHitsNightmare: Int = 3,
    val isTempleBoss: Boolean = false
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

data class CritterSnapshot(
    val critterId: Int,
    val x: Float,
    val vx: Float,
    val stunFrames: Int
)

data class SwitchSnapshot(
    val switchId: Int,
    val isActivated: Boolean,
    val remainingFrames: Int
)

data class PillarSnapshot(
    val pillarId: Int,
    val currentDropY: Float,
    val phase: TrapPhase,
    val timerFrames: Int
)

// --- Troll / Rage Platformer Data-Driven Trap System ---

enum class TrollTrapType {
  DISAPPEARING_FLOOR,
  DELAYED_CRUMBLE,
  SURPRISE_SPIKE_POP,
  MOVING_SPIKE,
  FAKE_SAFE_PLATFORM,
  FAKE_EXIT_DOOR,
  RELOCATING_EXIT_DOOR,
  MOVING_CRUSH_WALL,
  FALLING_CEILING_BLOCK,
  FAKE_CHECKPOINT,
  TIMED_BLINK_PLATFORM
}

enum class TrapTriggerType {
  ON_PROXIMITY_X,
  ON_STEP,
  ON_JUMP_ABOVE,
  ON_TIMER_CYCLE,
  ON_APPROACH_EXIT
}

enum class TrollTrapState {
  ARMED,
  TELEGRAPH,
  ACTIVE,
  RETIRED
}

/**
 * Deterministic, resettable trap unit powering all World 1 Troll Platformer gags.
 */
data class TrollTrap(
    val id: Int,
    val type: TrollTrapType,
    val triggerType: TrapTriggerType,
    val initialX: Float,
    val initialY: Float,
    val width: Float,
    val height: Float,
    val triggerMinX: Float = initialX - 36f,
    val triggerMaxX: Float = initialX + width + 36f,
    val triggerMinY: Float = initialY - 120f,
    val triggerMaxY: Float = initialY + height + 64f,
    val telegraphDurationFrames: Int = 10,
    val activeDurationFrames: Int = 0,
    val cycleFrames: Int = 0,
    val phaseOffsetFrames: Int = 0,
    val targetX: Float = initialX,
    val targetY: Float = initialY,
    val moveSpeed: Float = 8f,
    val killsPlayer: Boolean = true,
    val linkedTileId: Int? = null,
    val linkedDoorTargetX: Float? = null,
    val linkedDoorTargetY: Float? = null,
    val quipPopup: String? = null,
    var x: Float = initialX,
    var y: Float = initialY,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var state: TrollTrapState = TrollTrapState.ARMED,
    var stateTimerFrames: Int = phaseOffsetFrames,
    var isSolid: Boolean = (type == TrollTrapType.DISAPPEARING_FLOOR ||
        type == TrollTrapType.FAKE_SAFE_PLATFORM ||
        type == TrollTrapType.DELAYED_CRUMBLE ||
        type == TrollTrapType.MOVING_CRUSH_WALL ||
        type == TrollTrapType.FALLING_CEILING_BLOCK ||
        type == TrollTrapType.TIMED_BLINK_PLATFORM),
    var isVisible: Boolean = true,
    var morphSpikeHeight: Float = 0f,
    var hasRelocated: Boolean = false,
    var dropOffsetY: Float = 0f,
    var fadeAlpha: Float = 1f,
    var lockedTelegraphDurationFrames: Int = telegraphDurationFrames,
    var lockedMoveSpeed: Float = moveSpeed,
    var lockedCycleFrames: Int = cycleFrames
) {
  fun applyLockedTuning(
      telegraphFrames: Int,
      moveSpeedScaled: Float,
      cycleFramesScaled: Int
  ) {
    lockedTelegraphDurationFrames = telegraphFrames
    lockedMoveSpeed = moveSpeedScaled
    lockedCycleFrames = cycleFramesScaled
  }

  fun reset() {
    x = initialX
    y = initialY
    vx = 0f
    vy = 0f
    state = TrollTrapState.ARMED
    stateTimerFrames = phaseOffsetFrames
    isSolid = (type == TrollTrapType.DISAPPEARING_FLOOR ||
        type == TrollTrapType.FAKE_SAFE_PLATFORM ||
        type == TrollTrapType.DELAYED_CRUMBLE ||
        type == TrollTrapType.MOVING_CRUSH_WALL ||
        type == TrollTrapType.FALLING_CEILING_BLOCK ||
        type == TrollTrapType.TIMED_BLINK_PLATFORM)
    isVisible = true
    morphSpikeHeight = 0f
    hasRelocated = false
    dropOffsetY = 0f
    fadeAlpha = 1f
  }
}

data class TrapSnapshot(
    val trapId: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val state: TrollTrapState,
    val stateTimerFrames: Int,
    val isSolid: Boolean,
    val isVisible: Boolean,
    val morphSpikeHeight: Float,
    val hasRelocated: Boolean,
    val dropOffsetY: Float,
    val fadeAlpha: Float
)

data class OrbitingPlatformSnapshot(
    val platformId: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val currentFrame: Int
)

data class PendulumSnapshot(
    val pendulumId: Int,
    val currentFrame: Int,
    val bobX: Float,
    val bobY: Float
)

data class LightningSnapshot(
    val hazardId: Int,
    val currentFrame: Int,
    val phase: TrapPhase
)

data class LevelCheckpointSnapshot(
    val playerX: Float,
    val playerY: Float,
    val playerFacingRight: Boolean,
    val playerHealth: Int,
    val levelTimerMs: Long,
    val scoreAtCheckpoint: Int = 0,
    val coinsCollectedAtCheckpoint: Set<Int>,
    val litDiyasAtCheckpoint: Set<Int> = emptySet(),
    val tileSnapshots: List<TileSnapshot>,
    val movingPlatformSnapshots: List<MovingPlatformSnapshot>,
    val critterSnapshots: List<CritterSnapshot> = emptyList(),
    val echoSealActivatedAtCheckpoint: Boolean = false,
    val resonanceStoneCollectedAtCheckpoint: Boolean = false,
    val treeResolvedAtCheckpoint: Boolean = false,
    val stalkerRepelledAtCheckpoint: Boolean = false,
    val guardianDefeatedAtCheckpoint: Boolean = false,
    val triggeredFallingHazardIds: Set<Int> = emptySet(),
    val switchSnapshots: List<SwitchSnapshot> = emptyList(),
    val pillarSnapshots: List<PillarSnapshot> = emptyList(),
    val collectedRelicsAtCheckpoint: Set<String> = emptySet(),
    val challengeTimerMs: Long = levelTimerMs,
    val exitXAtCheckpoint: Float? = null,
    val exitYAtCheckpoint: Float? = null,
    val trollTrapSnapshots: List<TrapSnapshot> = emptyList(),
    val orbitingPlatformSnapshots: List<OrbitingPlatformSnapshot> = emptyList(),
    val pendulumSnapshots: List<PendulumSnapshot> = emptyList(),
    val lightningSnapshots: List<LightningSnapshot> = emptyList()
)

enum class AdaptiveTier(
    val tierIndex: Int,
    val label: String,
    val speedScale: Float,
    val telegraphBonusFrames: Int
) {
  ASSISTED(tierIndex = 0, label = "ASSISTED", speedScale = 0.85f, telegraphBonusFrames = 4),
  STANDARD(tierIndex = 1, label = "STANDARD", speedScale = 1.00f, telegraphBonusFrames = 0),
  ESCALATED(tierIndex = 2, label = "ESCALATED", speedScale = 1.08f, telegraphBonusFrames = -2),
  RELENTLESS(tierIndex = 3, label = "RELENTLESS", speedScale = 1.16f, telegraphBonusFrames = -3)
}

data class ChallengeDefinition(
    val challengeId: String,
    val orderIndex: Int,
    val title: String,
    val subtitle: String,
    val theme: LevelTheme,
    val challengeSeed: Long,
    val configVersion: Int = 1,
    val legacyWorldId: Int? = null,
    val legacyLevelId: Int? = null,
    val baseDifficulty: DifficultyTier = DifficultyTier.NORMAL,
    val levelFactory: (DifficultyTier) -> LevelDefinition
) {
  fun buildLevel(difficulty: DifficultyTier = baseDifficulty): LevelDefinition = levelFactory(difficulty)

  fun lockConfig(adaptiveTier: AdaptiveTier = AdaptiveTier.STANDARD): LockedChallengeConfig {
    return LockedChallengeConfig(
        challengeId = challengeId,
        orderIndex = orderIndex,
        challengeSeed = challengeSeed,
        selectedAdaptiveTier = adaptiveTier,
        configVersion = configVersion,
        levelDefinition = buildLevel(baseDifficulty)
    )
  }
}

/**
 * Immutable per-attempt configuration locked at attempt start so adaptive tier,
 * seed, and trap layout never mutate mid-attempt.
 */
data class LockedChallengeConfig(
    val challengeId: String,
    val orderIndex: Int,
    val challengeSeed: Long,
    val selectedAdaptiveTier: AdaptiveTier,
    val configVersion: Int,
    val levelDefinition: LevelDefinition
)

enum class RunLaunchMode {
  SINGLE_STAGE,
  CONTINUOUS_PROGRESSION,
  REPLAY
}

data class ChallengeRecord(
    val challengeId: String,
    val completed: Boolean = false,
    val bestTimeMs: Long = 0L,
    val bestScore: Int = 0,
    val stars: Int = 0,
    val leastDeaths: Int = -1
)

data class ChallengePerformanceRecord(
    val challengeId: String,
    val deaths: Int,
    val timeMs: Long,
    val parTimeSeconds: Int,
    val tierUsed: AdaptiveTier,
    val usedAssisted: Boolean
)

data class AdaptiveMetricsState(
    val currentTier: AdaptiveTier = AdaptiveTier.STANDARD,
    val activeChallengeId: String = "ch_01",
    val currentChallengeDeaths: Int = 0,
    val currentChallengeTrapDeaths: Map<String, Int> = emptyMap(),
    val totalRunDeaths: Int = 0,
    val totalRunCompletions: Int = 0,
    val consecutiveStrongCompletions: Int = 0,
    val escalationCooldownChallenges: Int = 0,
    val demotionCooldownRetries: Int = 0,
    val recentCompletions: List<ChallengePerformanceRecord> = emptyList()
)

data class ContinuousRunPersistedState(
    val schemaVersion: Int,
    val isRunActive: Boolean,
    val isRunInForeground: Boolean,
    val isRunCompleted: Boolean,
    val activeChallengeId: String,
    val activeChallengeSeed: Long,
    val activeConfigVersion: Int,
    val activeAdaptiveTier: AdaptiveTier,
    val currentChallengeDeaths: Int,
    val completedChallengeIds: Set<String>,
    val challengeRecords: Map<String, ChallengeRecord>,
    val cumulativeRunTimerMs: Long,
    val cumulativeDeaths: Int,
    val cumulativeScore: Int,
    val bestRunTimeMs: Long,
    val bestRunScore: Int,
    val bestRunLeastDeaths: Int,
    val adaptiveMetrics: AdaptiveMetricsState = AdaptiveMetricsState()
)

data class RunCompletionSummary(
    val completedCount: Int,
    val totalChallenges: Int,
    val totalRunTimeMs: Long,
    val totalDeaths: Int,
    val totalScore: Int,
    val bestRunTimeMs: Long,
    val bestRunScore: Int,
    val bestRunLeastDeaths: Int,
    val challengeRecords: List<ChallengeRecord>
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
    val parTimeSeconds: Int = 60,
    val clueMarkers: List<ClueMarker> = emptyList(),
    val resonanceStone: ResonanceStone? = null,
    val echoPuzzleNode: EchoPuzzleNode? = null,
    val bounceDrums: List<BounceDrum> = emptyList(),
    val critters: List<ForestCritter> = emptyList(),
    val windDrafts: List<WindDraft> = emptyList(),
    val pendulums: List<SwingingPendulum> = emptyList(),
    val diyas: List<ForestDiya> = emptyList(),
    val difficulty: DifficultyTier = DifficultyTier.NORMAL,
    val fallingHazards: List<FallingHazard> = emptyList(),
    val ambushTraps: List<AmbushSpikeTrap> = emptyList(),
    val subtitle: String = "",
    val description: String = "",
    val theme: LevelTheme = LevelTheme.WHISPERING_WOODS,
    val enableForestSetPieces: Boolean = false,
    val minCamY: Float = -48f,
    val maxCamY: Float = 240f,
    val pitKillY: Float = 760f,
    val soundSwitches: List<SoundSwitchNode> = emptyList(),
    val orbitingPlatforms: List<OrbitingPlatform> = emptyList(),
    val collapsingPillars: List<CollapsingPillar> = emptyList(),
    val waterCurrents: List<WaterCurrentZone> = emptyList(),
    val lightningHazards: List<LightningHazard> = emptyList(),
    val relics: List<LevelRelic> = emptyList(),
    val hasHeavyRain: Boolean = false,
    val hasDarknessLantern: Boolean = false,
    val guardianConfig: GuardianEncounterConfig? = null,
    val trollTraps: List<TrollTrap> = emptyList(),
    val isTrollLevel: Boolean = false
)
