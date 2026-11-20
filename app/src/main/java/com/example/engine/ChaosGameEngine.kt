package com.example.engine

import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

data class AttemptRetryTransition(
    val lockedConfig: LockedChallengeConfig,
    val attemptToken: Long
)

data class EngineHudSnapshot(
    val coinsCollected: Int,
    val health: Int,
    val timerTenths: Int,
    val cluesCount: Int,
    val resonanceDiscovered: Boolean,
    val score: Int,
    val comboMultiplier: Int,
    val diyasLit: Int,
    val relicsCollected: Int,
    val activeNotification: GameplayNotification?,
    val deathCount: Int = 0,
    val isDying: Boolean = false,
    val challengeEpoch: Long = 0L,
    val challengeId: String = "ch_01",
    val challengeOrderIndex: Int = 1,
    val launchMode: RunLaunchMode = RunLaunchMode.SINGLE_STAGE,
    val cumulativeDeathCount: Int = 0,
    val cumulativeRunTimerMs: Long = 0L,
    val activeAdaptiveTier: AdaptiveTier = AdaptiveTier.STANDARD
)

/**
 * 60 FPS Core Game Physics & Simulation Engine.
 * Implements deterministic kinematics, coyote time, jump buffering,
 * top-contact-only disappearing floor state machine, E1-E7 mystery events,
 * permanent story clue tracking, and deterministic checkpoint rollback.
 */
class ChaosGameEngine(
    level: LevelDefinition,
    val audioEngine: ProceduralAudioEngine,
    initialDiscoveredClues: Set<StoryClueId> = emptySet(),
    initialResonanceDiscovered: Boolean = false,
    val onClueDiscovered: (StoryClueId) -> Unit = {},
    val onResonanceDiscovered: () -> Unit = {},
    val onWhisperEvent: (eventId: GameplayEventId, message: String) -> Unit = { _, _ -> },
    initialDiscoveredRelics: Set<String> = emptySet(),
    val onRelicDiscovered: (LevelRelic) -> Unit = {},
    val onPlayerDied: (totalSessionDeaths: Int) -> Unit = {},
    lockedChallengeConfig: LockedChallengeConfig? = null,
    initialCompletedChallengeIds: Set<String> = emptySet(),
    initialCumulativeRunTimerMs: Long = 0L,
    initialCumulativeDeathCount: Int = 0,
    val deathAnimDurationFrames: Int = DEFAULT_DEATH_ANIM_FRAMES,
    initialAttemptToken: Long = 1L,
    initialLaunchMode: RunLaunchMode = RunLaunchMode.SINGLE_STAGE,
    initialChallengeDeaths: Int = 0,
    val onChallengeDeathUpdated: (challengeId: String, challengeDeaths: Int, cumulativeDeaths: Int) -> Unit = { _, _, _ -> },
    val onChallengeDeathAttributed: (challengeId: String, challengeDeaths: Int, cumulativeDeaths: Int, causeKey: String?) -> Unit = { _, _, _, _ -> },
    val onAttemptRetryConfigRequested: ((challengeId: String, challengeDeaths: Int, causeKey: String?) -> AttemptRetryTransition?)? = null,
    val onLevelCompleted: (timeMs: Long, coinsCollected: Int, totalCoins: Int) -> Unit = { _, _, _ -> }
) {
  companion object {
    const val GRAVITY = 0.58f
    const val JUMP_IMPULSE = -13.2f
    const val TERMINAL_VELOCITY = 14.0f
    const val MAX_RUN_SPEED = 6.5f
    const val RUN_ACCEL = 0.85f
    const val GROUND_FRICTION = 1.1f
    const val AIR_ACCEL = 0.68f
    const val TURN_ACCEL_MULTIPLIER = 1.65f
    const val COYOTE_FRAMES = 5 // ~80 ms
    const val JUMP_BUFFER_FRAMES = 6 // ~100 ms
    const val CRUMBLE_DELAY_FRAMES = 45 // 0.75 s
    const val VANISHING_SLAB_DELAY_FRAMES = 34 // ~0.57 s progressive bridge collapse (Normal)
    const val PIT_KILL_Y = 760f
    const val DEFAULT_DEATH_ANIM_FRAMES = 10 // ~167ms snappy comedic spin + instant auto-retry
    const val DEATH_ANIM_FRAMES = DEFAULT_DEATH_ANIM_FRAMES
    const val WHISPER_DURATION_FRAMES = 120 // ~2.0 seconds at 60 FPS (compact lower safe-area toast)
    const val COMBO_WINDOW_FRAMES = 135 // ~2.25 seconds at 60 FPS
  }

  var level: LevelDefinition = level
    private set

  var lockedChallengeConfig: LockedChallengeConfig? = lockedChallengeConfig
    private set

  val player = Player(
      x = this.level.spawnX,
      y = this.level.spawnY,
      health = this.level.difficulty.startingHealth,
      maxHealth = this.level.difficulty.startingHealth
  )
  var tiles: List<GameTile> = this.level.tiles.map { it.copy() }
    private set
  var coins: List<Coin> = this.level.coins.map { it.copy() }
    private set
  var movingPlatforms: List<MovingPlatform> = this.level.movingPlatforms.map { it.copy() }
    private set
  var bounceDrums: List<BounceDrum> = this.level.bounceDrums.map { it.copy() }
    private set
  var critters: List<ForestCritter> = this.level.critters.map { it.copy() }
    private set
  var windDrafts: List<WindDraft> = this.level.windDrafts.map { it.copy() }
    private set
  var pendulums: List<SwingingPendulum> = this.level.pendulums.map { it.copy() }
    private set
  var diyas: List<ForestDiya> = this.level.diyas.map { it.copy() }
    private set
  var fallingHazards: List<FallingHazard> = this.level.fallingHazards.map { it.copy() }
    private set
  var ambushTraps: List<AmbushSpikeTrap> = this.level.ambushTraps.map { it.copy() }
    private set
  var soundSwitches: List<SoundSwitchNode> = this.level.soundSwitches.map { it.copy() }
    private set
  var orbitingPlatforms: List<OrbitingPlatform> = this.level.orbitingPlatforms.map { it.copy() }
    private set
  var collapsingPillars: List<CollapsingPillar> = this.level.collapsingPillars.map { it.copy() }
    private set
  var waterCurrents: List<WaterCurrentZone> = this.level.waterCurrents.map { it.copy() }
    private set
  var lightningHazards: List<LightningHazard> = this.level.lightningHazards.map { it.copy() }
    private set
  var relics: List<LevelRelic> = this.level.relics.map { it.copy() }
    private set
  var trollTraps: List<TrollTrap> = this.level.trollTraps.map { it.copy() }
    private set

  init {
    lockedChallengeConfig?.let {
      TrapFairnessPolicy.applyTierTuningInPlace(trollTraps, it.selectedAdaptiveTier)
    }
  }

  var resonanceStone: ResonanceStone? = this.level.resonanceStone?.copy()
    private set
  var echoPuzzleNode: EchoPuzzleNode? = this.level.echoPuzzleNode?.copy()
    private set
  val watcherAppearance = WatcherAppearance()

  var deathCount: Int = initialChallengeDeaths
    private set
  var isDying: Boolean = false
    private set
  var deathAnimFrames: Int = 0
    private set
  var lastDeathCauseKey: String? = null
    private set
  private var pendingDeathCauseKey: String? = null
  var currentExitX: Float = this.level.exitX
    private set
  var currentExitY: Float = this.level.exitY
    private set

  // Set pieces (Awakening Tree, Vanar Forest Stalker, Vanishing Causeway + Stone Guardian)
  val awakeningTree = AwakeningTreeState()
  val forestStalker = ForestStalkerState()
  private var guardianCfg = this.level.guardianConfig
  var stoneGuardian: StoneGuardianState = buildStoneGuardianState(this.level)
    private set

  private fun buildStoneGuardianState(lvl: LevelDefinition): StoneGuardianState {
    val cfg = lvl.guardianConfig
    return if (cfg != null) {
      val hits = when (lvl.difficulty) {
        DifficultyTier.NORMAL -> cfg.baseHitsNormal
        DifficultyTier.HARD -> cfg.baseHitsHard
        DifficultyTier.NIGHTMARE -> cfg.baseHitsNightmare
      }
      StoneGuardianState(
          x = cfg.x,
          groundY = cfg.groundY,
          crystalX = cfg.crystalX,
          crystalY = cfg.crystalY,
          hitsRemaining = hits,
          maxHits = hits,
          triggerMinX = cfg.triggerMinX,
          triggerMaxX = cfg.triggerMaxX,
          reboundX = cfg.reboundX,
          despawnMinX = cfg.despawnMinX,
          isTempleBoss = cfg.isTempleBoss
      )
    } else {
      StoneGuardianState(hitsRemaining = 1, maxHits = 1)
    }
  }

  var isStandingOnSlippery: Boolean = false
    private set
  var lightningFlashFrames: Int = 0
    private set

  // Permanent discoveries (never lost on death)
  val discoveredClues: MutableSet<StoryClueId> = initialDiscoveredClues.toMutableSet()
  val discoveredRelics: MutableSet<String> = initialDiscoveredRelics.toMutableSet()
  var isResonanceStoneDiscovered: Boolean = initialResonanceDiscovered
    private set

  // Per-run event log & priority-ordered, deduplicated contextual notification controller
  val triggeredEvents: MutableSet<GameplayEventId> = mutableSetOf()
  val notificationController = GameplayNotificationController()
  val activeNotification: GameplayNotification?
    get() = notificationController.activeNotification
  val activeWhisperText: String?
    get() = notificationController.activeNotification?.message
  val activeWhisperCategory: NotificationCategory
    get() = notificationController.activeNotification?.category ?: NotificationCategory.STORY_CLUE
  val activeWhisperEventKey: String?
    get() = notificationController.activeNotification?.eventKey
  val activeWhisperFrames: Int
    get() = notificationController.remainingFrames
  val queuedNotificationCount: Int
    get() = notificationController.queuedCount
  fun peekQueuedNotifications(): List<GameplayNotification> = notificationController.peekQueued()
  var echoingBridgeCueCount: Int = 0
    private set

  // Adventure score, combo & game-feel feedback
  var score: Int = 0
    private set
  var comboMultiplier: Int = 1
    private set
  var comboTimerFrames: Int = 0
    private set
  var cameraShakeFrames: Int = 0
    private set
  var cameraShakeMagnitude: Float = 0f
    private set
  private var landingSquashFrames: Int = 0

  var activeChallengeId: String =
      this.lockedChallengeConfig?.challengeId ?: "ch_${this.level.levelId.toString().padStart(2, '0')}"
    private set
  val completedChallengeIds: MutableSet<String> = initialCompletedChallengeIds.toMutableSet()
  var challengeAttemptToken: Long = initialAttemptToken
    private set
  var challengeEpoch: Long = 0L
    private set
  var launchMode: RunLaunchMode = initialLaunchMode
    private set

  // Particle & popup pools (pre-allocated for zero-GC 60 FPS loop)
  val particles = Array(64) { Particle() }
  private var nextParticleIndex = 0
  val popups = Array(16) { FloatingPopup() }
  private var nextPopupIndex = 0

  var levelTimerMs = 0L
  var challengeTimerMs = 0L
    private set
  var cumulativeRunTimerMs = initialCumulativeRunTimerMs
    private set
  var cumulativeDeathCount = initialCumulativeDeathCount
    private set
  @Volatile var isPaused = false
  @Volatile var isGameOver = false
  @Volatile var isVictorious = false

  var checkpointSnapshot: LevelCheckpointSnapshot? = null
  var hasCheckpointActivated = false
    private set

  // Thread-safe controller inputs (written on UI thread, polled at 60 Hz on render thread)
  @Volatile var inputLeft = false
  @Volatile var inputRight = false
  @Volatile var inputJump = false
  private var previousInputJump = false

  private val pendingCameraSnap = AtomicBoolean(true)
  private val worldStateLock = Any()

  private data class PendingChallengeLoad(
      val lockedConfig: LockedChallengeConfig,
      val attemptToken: Long,
      val launchMode: RunLaunchMode,
      val discoveredRelics: Set<String>,
      val preserveCumulativeStats: Boolean,
      val initialDeathsInChallenge: Int,
      val cumulativeTimerMs: Long?,
      val cumulativeDeaths: Int?
  )

  private var isInsideUpdate = false
  private var pendingChallengeLoad: PendingChallengeLoad? = null

  private val trapSystem = TrollTrapSystem()

  fun consumePendingCameraSnap(): Boolean = pendingCameraSnap.getAndSet(false)
  fun hasPendingCameraSnap(): Boolean = pendingCameraSnap.get()

  fun enqueueTrapChainEvent(targetTrapId: Int, delayFrames: Int = 0) = synchronized(worldStateLock) {
    trapSystem.enqueueChainEvent(targetTrapId, delayFrames)
  }

  val pendingTrapChainEventCount: Int
    get() = trapSystem.pendingChainEventCount
  private val trapCallbacks = object : TrollTrapCallbacks {
    override fun killPlayer(quip: String) = startDeathSequence(quip, null)
    override fun killPlayer(quip: String, causeKey: String?) = startDeathSequence(quip, causeKey)

    override fun relocateExitGate(destX: Float, destY: Float) {
      currentExitX = destX
      currentExitY = destY
      for (tile in tiles) {
        if (tile.type == TileType.SUN_GATE) {
          tile.x = destX
          tile.y = destY
        }
      }
    }

    override fun hideLinkedTile(tileId: Int) {
      for (tile in tiles) {
        if (tile.id == tileId) {
          tile.isVisible = false
          tile.isSolid = false
          tile.isTriggered = true
        }
      }
    }

    override fun playTrapSnap() = audioEngine.playTrapSnap()
    override fun playCrumble() = audioEngine.playCrumble()
    override fun playDisappear() = audioEngine.playDisappear()
    override fun playCeilingSlam() = audioEngine.playCeilingSlam()
    override fun playDoorEscape() = audioEngine.playDoorEscape()
    override fun playFakeDoorPoof() = audioEngine.playFakeDoorPoof()
    override fun playMechanismShift() = audioEngine.playMechanismShift()
    override fun spawnDustTrickle(x: Float, y: Float) = this@ChaosGameEngine.spawnDustTrickle(x, y)
    override fun spawnDustPuff(x: Float, y: Float) = this@ChaosGameEngine.spawnDustPuff(x, y)
    override fun spawnSparkles(x: Float, y: Float, color: Int, count: Int) =
        this@ChaosGameEngine.spawnSparkles(x, y, color, count)
    override fun spawnStarburst(x: Float, y: Float) = spawnDeathStarburst(x, y)
    override fun spawnPopup(x: Float, y: Float, text: String, color: Int) =
        this@ChaosGameEngine.spawnPopup(x, y, text, color)
    override fun triggerCameraShake(frames: Int, magnitude: Float) =
        this@ChaosGameEngine.triggerCameraShake(frames, magnitude)
  }

  @Volatile
  var hudSnapshot: EngineHudSnapshot = buildCurrentHudSnapshot()
    private set

  private fun currentOrderIndex(): Int {
    return lockedChallengeConfig?.orderIndex ?: level.levelId
  }

  private fun buildCurrentHudSnapshot(): EngineHudSnapshot {
    var collectedCoins = 0
    for (i in coins.indices) {
      if (coins[i].isCollected) collectedCoins++
    }
    var litDiyas = 0
    for (i in diyas.indices) {
      if (diyas[i].isLit) litDiyas++
    }
    var collectedRelics = 0
    for (i in relics.indices) {
      if (relics[i].isCollected) collectedRelics++
    }
    val currentTier = lockedChallengeConfig?.selectedAdaptiveTier ?: AdaptiveTier.STANDARD
    return EngineHudSnapshot(
        coinsCollected = collectedCoins,
        health = player.health,
        timerTenths = (levelTimerMs / 100L).toInt(),
        cluesCount = discoveredClues.size,
        resonanceDiscovered = isResonanceStoneDiscovered,
        score = score,
        comboMultiplier = comboMultiplier,
        diyasLit = litDiyas,
        relicsCollected = collectedRelics,
        activeNotification = notificationController.activeNotification,
        deathCount = deathCount,
        isDying = isDying,
        challengeEpoch = challengeEpoch,
        challengeId = activeChallengeId,
        challengeOrderIndex = currentOrderIndex(),
        launchMode = launchMode,
        cumulativeDeathCount = cumulativeDeathCount,
        cumulativeRunTimerMs = cumulativeRunTimerMs,
        activeAdaptiveTier = currentTier
    )
  }

  private fun publishHudSnapshot() {
    var collectedCoins = 0
    for (i in coins.indices) {
      if (coins[i].isCollected) collectedCoins++
    }
    var litDiyas = 0
    for (i in diyas.indices) {
      if (diyas[i].isLit) litDiyas++
    }
    var collectedRelics = 0
    for (i in relics.indices) {
      if (relics[i].isCollected) collectedRelics++
    }
    val timerTenths = (levelTimerMs / 100L).toInt()
    val notif = notificationController.activeNotification
    val orderIdx = currentOrderIndex()
    val currentTier = lockedChallengeConfig?.selectedAdaptiveTier ?: AdaptiveTier.STANDARD
    val prev = hudSnapshot
    if (prev.coinsCollected == collectedCoins &&
        prev.health == player.health &&
        prev.timerTenths == timerTenths &&
        prev.cluesCount == discoveredClues.size &&
        prev.resonanceDiscovered == isResonanceStoneDiscovered &&
        prev.score == score &&
        prev.comboMultiplier == comboMultiplier &&
        prev.diyasLit == litDiyas &&
        prev.relicsCollected == collectedRelics &&
        prev.activeNotification == notif &&
        prev.deathCount == deathCount &&
        prev.isDying == isDying &&
        prev.challengeEpoch == challengeEpoch &&
        prev.challengeId == activeChallengeId &&
        prev.challengeOrderIndex == orderIdx &&
        prev.launchMode == launchMode &&
        prev.cumulativeDeathCount == cumulativeDeathCount &&
        prev.cumulativeRunTimerMs == cumulativeRunTimerMs &&
        prev.activeAdaptiveTier == currentTier) {
      return
    }
    hudSnapshot = EngineHudSnapshot(
        coinsCollected = collectedCoins,
        health = player.health,
        timerTenths = timerTenths,
        cluesCount = discoveredClues.size,
        resonanceDiscovered = isResonanceStoneDiscovered,
        score = score,
        comboMultiplier = comboMultiplier,
        diyasLit = litDiyas,
        relicsCollected = collectedRelics,
        activeNotification = notif,
        deathCount = deathCount,
        isDying = isDying,
        challengeEpoch = challengeEpoch,
        challengeId = activeChallengeId,
        challengeOrderIndex = orderIdx,
        launchMode = launchMode,
        cumulativeDeathCount = cumulativeDeathCount,
        cumulativeRunTimerMs = cumulativeRunTimerMs,
        activeAdaptiveTier = currentTier
    )
  }

  fun <T> readWorldStateLocked(block: (ChaosGameEngine) -> T): T = synchronized(worldStateLock) {
    block(this)
  }

  /**
   * Thread-safely loads a new challenge into this existing ChaosGameEngine instance,
   * preserving the live GameSurfaceView / GameRenderThread across continuous transitions.
   * Reentrant calls made from inside update() (e.g. via onLevelCompleted) are queued
   * and applied atomically right before update() exits worldStateLock.
   */
  fun loadChallenge(
      lockedConfig: LockedChallengeConfig,
      attemptToken: Long = challengeAttemptToken + 1L,
      launchMode: RunLaunchMode = this.launchMode,
      discoveredRelics: Set<String> = this.discoveredRelics,
      preserveCumulativeStats: Boolean = true,
      initialDeathsInChallenge: Int = 0,
      cumulativeTimerMs: Long? = null,
      cumulativeDeaths: Int? = null
  ) = synchronized(worldStateLock) {
    val request = PendingChallengeLoad(
        lockedConfig = lockedConfig,
        attemptToken = attemptToken,
        launchMode = launchMode,
        discoveredRelics = discoveredRelics.toSet(),
        preserveCumulativeStats = preserveCumulativeStats,
        initialDeathsInChallenge = initialDeathsInChallenge.coerceAtLeast(0),
        cumulativeTimerMs = cumulativeTimerMs,
        cumulativeDeaths = cumulativeDeaths
    )
    if (isInsideUpdate) {
      pendingChallengeLoad = request
      return@synchronized
    }
    applyChallengeLoadLocked(request)
    publishHudSnapshot()
  }

  private fun applyChallengeLoadLocked(request: PendingChallengeLoad) {
    trapSystem.clearPendingChainEvents()
    lockedChallengeConfig = request.lockedConfig
    level = request.lockedConfig.levelDefinition
    activeChallengeId = request.lockedConfig.challengeId
    challengeAttemptToken = request.attemptToken
    launchMode = request.launchMode
    pendingDeathCauseKey = null
    lastDeathCauseKey = null
    challengeEpoch++

    if (!request.preserveCumulativeStats) {
      cumulativeRunTimerMs = request.cumulativeTimerMs?.coerceAtLeast(0L) ?: 0L
      cumulativeDeathCount = request.cumulativeDeaths?.coerceAtLeast(0) ?: 0
    } else {
      request.cumulativeTimerMs?.let { cumulativeRunTimerMs = it.coerceAtLeast(0L) }
      request.cumulativeDeaths?.let { cumulativeDeathCount = it.coerceAtLeast(0) }
    }
    deathCount = request.initialDeathsInChallenge

    tiles = level.tiles.map { it.copy() }
    coins = level.coins.map { it.copy() }
    movingPlatforms = level.movingPlatforms.map { it.copy() }
    bounceDrums = level.bounceDrums.map { it.copy() }
    critters = level.critters.map { it.copy() }
    windDrafts = level.windDrafts.map { it.copy() }
    pendulums = level.pendulums.map { it.copy() }
    diyas = level.diyas.map { it.copy() }
    fallingHazards = level.fallingHazards.map { it.copy() }
    ambushTraps = level.ambushTraps.map { it.copy() }
    soundSwitches = level.soundSwitches.map { it.copy() }
    orbitingPlatforms = level.orbitingPlatforms.map { it.copy() }
    collapsingPillars = level.collapsingPillars.map { it.copy() }
    waterCurrents = level.waterCurrents.map { it.copy() }
    lightningHazards = level.lightningHazards.map { it.copy() }
    relics = level.relics.map { it.copy() }
    trollTraps = level.trollTraps.map { it.copy() }
    resonanceStone = level.resonanceStone?.copy()
    echoPuzzleNode = level.echoPuzzleNode?.copy()

    guardianCfg = level.guardianConfig
    stoneGuardian = buildStoneGuardianState(level)
    currentExitX = level.exitX
    currentExitY = level.exitY

    discoveredRelics.clear()
    discoveredRelics.addAll(request.discoveredRelics)

    resetCurrentChallengeInternal(clearInputs = true, invulnFrames = 0)
  }

  fun update() = synchronized(worldStateLock) {
    isInsideUpdate = true
    try {
      if (isPaused || isGameOver || isVictorious) {
        if (isVictorious) {
          player.state = AaravState.VICTORY
          player.stateTimerFrames++
          updateParticles()
          updateAtmosphereTimers()
          publishHudSnapshot()
        }
        return
      }

      if (isDying) {
        deathAnimFrames--
        player.prevX = player.x
        player.prevY = player.y
        player.x += player.vx
        player.y += player.vy
        player.vy = (player.vy + 0.52f).coerceAtMost(12f)
        player.state = AaravState.DEATH
        player.stateTimerFrames++
        updateParticles()
        updatePopups()
        updateAtmosphereTimers()
        if (deathAnimFrames <= 0) {
          completeDeathRespawn()
        }
        publishHudSnapshot()
        return
      }

    levelTimerMs += 16L
    challengeTimerMs = levelTimerMs
    cumulativeRunTimerMs += 16L

    // 1. Process Jump Buffer
    if (inputJump && !previousInputJump) {
      player.jumpBufferTimer = JUMP_BUFFER_FRAMES
    } else if (player.jumpBufferTimer > 0) {
      player.jumpBufferTimer--
    }

    // 2. Variable Jump Cut on Release
    if (!inputJump && previousInputJump) {
      if (player.vy < -2.0f && !player.hasJumpCutApplied) {
        player.vy *= 0.42f
        player.hasJumpCutApplied = true
      }
    }
    previousInputJump = inputJump

    // 3. Horizontal Kinematics (snappy turnarounds, responsive air control, slippery stone momentum)
    val baseAccelRaw = if (player.isGrounded) RUN_ACCEL else AIR_ACCEL
    val baseAccel = if (player.isGrounded && isStandingOnSlippery) baseAccelRaw * 0.52f else baseAccelRaw
    val friction = if (player.isGrounded && isStandingOnSlippery) GROUND_FRICTION * 0.18f else GROUND_FRICTION
    if (inputLeft && !inputRight) {
      val accel = if (player.vx > 0f && player.isGrounded && !isStandingOnSlippery) baseAccel * TURN_ACCEL_MULTIPLIER else baseAccel
      player.vx = (player.vx - accel).coerceAtLeast(-MAX_RUN_SPEED)
      player.facingRight = false
    } else if (inputRight && !inputLeft) {
      val accel = if (player.vx < 0f && player.isGrounded && !isStandingOnSlippery) baseAccel * TURN_ACCEL_MULTIPLIER else baseAccel
      player.vx = (player.vx + accel).coerceAtMost(MAX_RUN_SPEED)
      player.facingRight = true
    } else {
      // Ground or air friction
      if (player.vx > 0f) {
        player.vx = (player.vx - friction).coerceAtLeast(0f)
      } else if (player.vx < 0f) {
        player.vx = (player.vx + friction).coerceAtMost(0f)
      }
    }

    // 4. Coyote Time and Jump Trigger
    if (player.isGrounded) {
      player.coyoteTimer = COYOTE_FRAMES
      player.hasJumpCutApplied = false
    } else if (player.coyoteTimer > 0) {
      player.coyoteTimer--
    }

    if (player.jumpBufferTimer > 0 && player.coyoteTimer > 0) {
      // Execute ground/coyote jump
      player.vy = JUMP_IMPULSE
      player.isGrounded = false
      player.coyoteTimer = 0
      player.jumpBufferTimer = 0
      player.hasJumpCutApplied = false
      landingSquashFrames = 0
      player.state = AaravState.JUMP_UP
      audioEngine.playJump()
      spawnDustPuff(player.x + player.collisionWidth / 2, player.y + player.collisionHeight)
    }

    // 5. Apply Gravity (with crisp apex hang-time assist while jump is held)
    player.prevX = player.x
    player.prevY = player.y

    if (!player.isGrounded) {
      val nearApex = inputJump && kotlin.math.abs(player.vy) < 1.8f
      val gravityStep = if (nearApex) GRAVITY * 0.72f else GRAVITY
      player.vy = (player.vy + gravityStep).coerceAtMost(TERMINAL_VELOCITY)
      player.state = if (player.vy < 0f) AaravState.JUMP_UP else AaravState.FALL_DOWN
    } else if (landingSquashFrames > 0) {
      landingSquashFrames--
      player.state = AaravState.LAND
    } else {
      player.state = if (kotlin.math.abs(player.vx) > 0.4f) AaravState.RUN else AaravState.IDLE
    }

    // 6. Update Moving Platforms, Orbiting Mechanisms & Rhythmic Pendulums
    for (mp in movingPlatforms) {
      val prevMpX = mp.x
      val prevMpY = mp.y
      mp.currentFrame = (mp.currentFrame + 1) % mp.periodFrames
      val angle = (2.0 * PI * mp.currentFrame / mp.periodFrames).toFloat()
      mp.x = mp.centerX + mp.amplitude * sin(angle)
      mp.y = mp.centerY + mp.amplitudeY * kotlin.math.cos(angle)
      mp.vx = mp.x - prevMpX
      mp.vy = mp.y - prevMpY
    }
    updateOrbitingPlatforms()
    updatePendulums()

    // 7. Integrate Physics & Collision
    val wasGroundedBeforeMove = player.isGrounded
    integrateHorizontalMovement()
    integrateVerticalMovement()
    if (!wasGroundedBeforeMove && player.isGrounded) {
      landingSquashFrames = 6
      player.state = AaravState.LAND
      triggerCameraShake(2, 1.5f)
    }

    // 8. Update Crumbling Platforms, Collapsing Pillars, Water Currents & Monsoon Wind Updrafts
    updateCrumblingTiles()
    updateCollapsingPillars()
    updateWaterCurrents()
    updateWindDrafts()

    // 9. Update Interactive Entities, Expansion Mechanisms & Major Set Pieces
    updateSoundSwitches()
    updateFalsePathWalls()
    updateLightningHazards()
    updateBounceDrums()
    updateCritters()
    updateFallingHazards()
    updateAmbushTraps()
    updateTrollTraps()
    if (!isDying) {
      if (level.enableForestSetPieces) {
        updateAwakeningTreeSetPiece()
        updateForestStalkerSetPiece()
      }
      if (guardianCfg != null) {
        updateStoneGuardianSetPiece()
      }
      updateDiyas()
      updateCoins()
      updateRelics()
      updateCheckpoint()
      updateMysteryEvents()
      updateExitGate()
    }

      if (pendingChallengeLoad != null) {
        return
      }

      // 10. Pit Death Detection
      if (!isDying && player.y > level.pitKillY) {
        handlePlayerDeath(causeKey = "pit")
      }

      // 11. Invulnerability timer
      if (player.invulnerableFrames > 0) {
        player.invulnerableFrames--
      }

      // 12. Update Particles, Popups & Atmospheric Timers
      updateParticles()
      updatePopups()
      updateAtmosphereTimers()
      player.stateTimerFrames++
      publishHudSnapshot()
    } finally {
      val pending = pendingChallengeLoad
      if (pending != null) {
        pendingChallengeLoad = null
        applyChallengeLoadLocked(pending)
        publishHudSnapshot()
      }
      isInsideUpdate = false
    }
  }

  private fun updateAtmosphereTimers() {
    if (watcherAppearance.isActive) {
      watcherAppearance.remainingFrames--
      if (watcherAppearance.remainingFrames <= 0) {
        watcherAppearance.isActive = false
        watcherAppearance.remainingFrames = 0
      }
    }
    notificationController.tick()
    if (comboTimerFrames > 0) {
      comboTimerFrames--
      if (comboTimerFrames <= 0) {
        comboMultiplier = 1
      }
    }
    if (cameraShakeFrames > 0) {
      cameraShakeFrames--
      if (cameraShakeFrames <= 0) {
        cameraShakeMagnitude = 0f
      }
    }
    if (lightningFlashFrames > 0) {
      lightningFlashFrames--
    }
  }

  private fun integrateHorizontalMovement() {
    player.x += player.vx

    // Tile side collisions (Solid blocks only)
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val playerTop = player.y
    val playerBottom = player.y + player.collisionHeight

    for (tile in tiles) {
      if (!tile.isSolid || !tile.isVisible) continue
      if (tile.type == TileType.CHECKPOINT_BELL || tile.type == TileType.SUN_GATE || tile.type == TileType.BRAMBLE_SPIKES) continue

      val tileLeft = tile.x
      val tileRight = tile.x + tile.width
      val tileTop = tile.y
      val tileBottom = tile.y + tile.height

      // AABB overlap check
      if (playerRight > tileLeft && playerLeft < tileRight &&
          playerBottom > tileTop + 4f && playerTop < tileBottom - 4f) {
        if (player.vx > 0f) {
          player.x = tileLeft - player.collisionWidth
          player.vx = 0f
        } else if (player.vx < 0f) {
          player.x = tileRight
          player.vx = 0f
        }
      }
    }

    // Awakening Banyan Tree emerging root pillar side collisions
    if (level.enableForestSetPieces && (awakeningTree.phase == TreePhase.ACTIVE || awakeningTree.phase == TreePhase.RESOLVED)) {
      val pLeft = player.x
      val pRight = player.x + player.collisionWidth
      val pTop = player.y
      val pBottom = player.y + player.collisionHeight
      for (root in awakeningTree.roots) {
        if (root.currentHeight < 6f) continue
        val rTop = root.baseY - root.currentHeight
        if (pRight > root.x && pLeft < root.x + root.width &&
            pBottom > rTop + 5f && pTop < root.baseY - 4f) {
          if (player.vx > 0f) {
            player.x = root.x - player.collisionWidth
            player.vx = 0f
          } else if (player.vx < 0f) {
            player.x = root.x + root.width
            player.vx = 0f
          }
        }
      }
    }

    // Collapsing Pillar solid side collisions
    for (pillar in collapsingPillars) {
      val pillarTop = pillar.topY + pillar.currentDropY
      val pillarBottom = pillarTop + pillar.height
      if (playerRight > pillar.x && playerLeft < pillar.x + pillar.width &&
          playerBottom > pillarTop + 6f && playerTop < pillarBottom - 4f) {
        if (player.vx > 0f) {
          player.x = pillar.x - player.collisionWidth
          player.vx = 0f
        } else if (player.vx < 0f) {
          player.x = pillar.x + pillar.width
          player.vx = 0f
        }
      }
    }

    // Stone Guardian solid body barrier when actively guarding the Sun Gate
    if (guardianCfg != null && (stoneGuardian.phase == GuardianPhase.ACTIVE || stoneGuardian.phase == GuardianPhase.AWAKENING)) {
      val gLeft = stoneGuardian.x
      val gRight = stoneGuardian.x + stoneGuardian.width
      val gTop = stoneGuardian.groundY - stoneGuardian.height
      if (player.x + player.collisionWidth > gLeft && player.x < gRight &&
          player.y + player.collisionHeight > gTop + 6f && player.y < stoneGuardian.groundY - 4f) {
        if (player.vx > 0f) {
          player.x = gLeft - player.collisionWidth
          player.vx = 0f
        } else if (player.vx < 0f) {
          player.x = gRight
          player.vx = 0f
        }
      }
    }

    // Solid TrollTrap platforms & crushing walls
    trapSystem.resolveHorizontalCollisions(trollTraps, player, trapCallbacks)
  }

  private fun integrateVerticalMovement() {
    val previousGrounded = player.isGrounded
    player.isGrounded = false
    isStandingOnSlippery = false
    player.y += player.vy

    // 1. Moving Platform Contact
    for (mp in movingPlatforms) {
      val mpLeft = mp.x
      val mpRight = mp.x + mp.width
      val mpTop = mp.y

      val playerLeft = player.x
      val playerRight = player.x + player.collisionWidth
      val prevBottom = player.prevY + player.collisionHeight
      val currBottom = player.y + player.collisionHeight
      val downSlop = mp.vy.coerceAtLeast(0f) + 3f

      if (player.vy >= 0f && prevBottom <= (mpTop - mp.vy) + 8f && currBottom >= mpTop - downSlop &&
          playerRight > mpLeft + 8f && playerLeft < mpRight - 8f) {
        player.y = mpTop - player.collisionHeight
        player.vy = 0f
        player.isGrounded = true
        player.x += mp.vx // Carry player with platform
        if (!previousGrounded) {
          audioEngine.playLand()
          spawnDustPuff(player.x + player.collisionWidth / 2, mpTop)
        }
        return
      }
    }

    // 1b. Orbiting Stone Platform Contact (non-hazard platforms)
    for (op in orbitingPlatforms) {
      if (op.isHazardGear) continue
      val opLeft = op.x
      val opRight = op.x + op.width
      val opTop = op.y

      val playerLeft = player.x
      val playerRight = player.x + player.collisionWidth
      val prevBottom = player.prevY + player.collisionHeight
      val currBottom = player.y + player.collisionHeight
      val downSlop = op.vy.coerceAtLeast(0f) + 4f

      if (player.vy >= 0f && prevBottom <= (opTop - op.vy) + 10f && currBottom >= opTop - downSlop &&
          playerRight > opLeft + 6f && playerLeft < opRight - 6f) {
        player.y = opTop - player.collisionHeight
        player.vy = 0f
        player.isGrounded = true
        player.x += op.vx
        if (!previousGrounded) {
          audioEngine.playLand()
          spawnDustPuff(player.x + player.collisionWidth / 2, opTop)
        }
        return
      }
    }

    // 1c. Collapsing Pillar Top Surface Contact
    for (pillar in collapsingPillars) {
      val pillarTop = pillar.topY + pillar.currentDropY
      val playerLeft = player.x
      val playerRight = player.x + player.collisionWidth
      val prevBottom = player.prevY + player.collisionHeight
      val currBottom = player.y + player.collisionHeight

      if (player.vy >= 0f && prevBottom <= pillarTop + 9f && currBottom >= pillarTop - 2f &&
          playerRight > pillar.x + 4f && playerLeft < pillar.x + pillar.width - 4f) {
        player.y = pillarTop - player.collisionHeight
        player.vy = 0f
        player.isGrounded = true
        if (pillar.phase == TrapPhase.ARMED) {
          pillar.phase = TrapPhase.WARNING
          pillar.timerFrames = pillar.warningDurationFrames
          audioEngine.playMechanismShift()
          spawnDustTrickle(pillar.x + pillar.width / 2f, pillarTop + 16f)
        }
        if (!previousGrounded) {
          audioEngine.playLand()
          spawnDustPuff(player.x + player.collisionWidth / 2, pillarTop)
        }
        return
      }
    }

    // 2. Tile Floor & Ceiling Collisions
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val playerTop = player.y
    val playerBottom = player.y + player.collisionHeight

    for (tile in tiles) {
      if (!tile.isVisible) continue

      // A. Disappearing Floor Check (Strict Top-Contact Only)
      if (tile.type == TileType.DISAPPEARING_FLOOR) {
        if (evaluateDisappearingFloor(tile)) {
          audioEngine.playDisappear()
          spawnSparkles(tile.x + tile.width / 2, tile.y + tile.height / 2, 0xFF64DFDF.toInt(), 12)
          continue
        }
      }

      if (!tile.isSolid) continue

      // B. Bramble Spikes Check
      if (tile.type == TileType.BRAMBLE_SPIKES) {
        val tileLeft = tile.x
        val tileRight = tile.x + tile.width
        val tileTop = tile.y + 12f // Spikes hit area
        val tileBottom = tile.y + tile.height

        if (playerRight > tileLeft + 6f && playerLeft < tileRight - 6f &&
            playerBottom > tileTop && playerTop < tileBottom) {
          handlePlayerDamage()
        }
        continue
      }

      // C. Standard Solid Floor / Ceiling Collision
      val tileLeft = tile.x
      val tileRight = tile.x + tile.width
      val tileTop = tile.y
      val tileBottom = tile.y + tile.height

      if (playerRight > tileLeft + 4f && playerLeft < tileRight - 4f) {
        // Landing on floor
        if (player.vy >= 0f && player.prevY + player.collisionHeight <= tileTop + 8f &&
            playerBottom >= tileTop - 1f && playerTop < tileTop) {
          player.y = tileTop - player.collisionHeight
          player.vy = 0f
          player.isGrounded = true
          if (tile.type == TileType.SLIPPERY_STONE) {
            isStandingOnSlippery = true
          }

          // Trigger crumbling wood or progressive vanishing causeway slab
          if (tile.type == TileType.CRUMBLING_WOOD && !tile.isCrumbling && !tile.isTriggered) {
            tile.isCrumbling = true
            tile.crumbleTimerFrames = level.difficulty.crumbleFrames
            echoingBridgeCueCount++
            audioEngine.playCrumble()
            audioEngine.playDistantBellCue()
            spawnDustTrickle(tile.x + tile.width / 2, tile.y + tile.height)
          } else if (tile.type == TileType.VANISHING_SLAB && !tile.isCrumbling && !tile.isTriggered) {
            tile.isCrumbling = true
            tile.crumbleTimerFrames = level.difficulty.vanishingFrames
            audioEngine.playCrumble()
            spawnDustTrickle(tile.x + tile.width / 2, tile.y + tile.height)
            postNotification(
                eventKey = "SP_SLAB_CRACK",
                title = "Unstable Causeway",
                message = "Causeway fracturing — keep moving!",
                category = NotificationCategory.EVENT_WARNING,
                priority = NotificationPriority.CRITICAL,
                visualKind = NotificationVisualKind.HAZARD_WARNING,
                worldFocusX = tile.x + tile.width / 2f,
                worldFocusY = tile.y
            )
          }

          if (!previousGrounded) {
            audioEngine.playLand()
            spawnDustPuff(player.x + player.collisionWidth / 2, tileTop)
          }
        }
        // Ceiling bump
        else if (player.vy < 0f && player.prevY >= tileBottom - 6f && playerTop <= tileBottom) {
          player.y = tileBottom
          player.vy = 0f
        }
      }
    }

    // Emerged Root Pillar top surfaces (steppable terrain for Set Piece A counterplay)
    if (level.enableForestSetPieces && (awakeningTree.phase == TreePhase.ACTIVE || awakeningTree.phase == TreePhase.RESOLVED)) {
      val pLeft = player.x
      val pRight = player.x + player.collisionWidth
      val prevBottom = player.prevY + player.collisionHeight
      val currBottom = player.y + player.collisionHeight
      for (root in awakeningTree.roots) {
        if (root.currentHeight < 6f) continue
        val rTop = root.baseY - root.currentHeight
        if (player.vy >= 0f && prevBottom <= rTop + 8f && currBottom >= rTop - 1f &&
            pRight > root.x + 2f && pLeft < root.x + root.width - 2f) {
          player.y = rTop - player.collisionHeight
          player.vy = 0f
          player.isGrounded = true
          if (!previousGrounded) {
            audioEngine.playLand()
            spawnDustPuff(player.x + player.collisionWidth / 2, rTop)
          }
        }
      }
    }

    // 3. Solid TrollTrap Floor & Ceiling Collisions
    trapSystem.resolveVerticalCollisions(
        traps = trollTraps,
        player = player,
        previousGrounded = previousGrounded,
        callbacks = trapCallbacks
    ) { surfaceY ->
      audioEngine.playLand()
      spawnDustPuff(player.x + player.collisionWidth / 2, surfaceY)
    }
  }

  /**
   * Swept top-contact detection for instant disappearing floors.
   * Strictly requires vy > 0.1f and downward crossing of the top edge.
   */
  fun evaluateDisappearingFloor(tile: GameTile): Boolean {
    if (tile.isTriggered || !tile.isSolid) return false
    if (player.vy <= 0.1f) return false // Rejects vy <= 0 (stationary, ceiling bumps)

    val prevBottom = player.prevY + player.collisionHeight
    val currBottom = player.y + player.collisionHeight
    val tileTop = tile.y

    val wasAboveSurface = prevBottom <= tileTop + 2f
    val crossedSurface = currBottom >= tileTop - 1f

    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val tileLeft = tile.x
    val tileRight = tile.x + tile.width
    val horizontalOverlap = (playerRight > tileLeft + 8f) && (playerLeft < tileRight - 8f)

    if (wasAboveSurface && crossedSurface && horizontalOverlap) {
      tile.isTriggered = true
      tile.isVisible = false
      tile.isSolid = false
      player.isGrounded = false
      return true
    }
    return false
  }

  private fun updateCrumblingTiles() {
    for (tile in tiles) {
      if (tile.isCrumbling && !tile.isTriggered) {
        tile.crumbleTimerFrames--
        if (tile.crumbleTimerFrames > 0 && tile.crumbleTimerFrames % 6 == 0) {
          spawnDustTrickle(tile.x + tile.width * 0.5f, tile.y + tile.height - 2f)
        }
        if (tile.crumbleTimerFrames <= 0) {
          tile.isTriggered = true
          tile.isVisible = false
          tile.isSolid = false
          spawnDustPuff(tile.x + tile.width / 2, tile.y + tile.height / 2)
        }
      }
    }
  }

  private fun updateCoins() {
    val playerCenterX = player.x + player.collisionWidth / 2
    val playerCenterY = player.y + player.collisionHeight / 2

    for (coin in coins) {
      if (coin.isCollected) continue
      coin.animTimer += 0.08f

      val dx = playerCenterX - coin.x
      val dy = playerCenterY - coin.y
      val distSq = dx * dx + dy * dy
      val pickupDist = coin.radius + player.collisionWidth / 2

      if (distSq < pickupDist * pickupDist) {
        coin.isCollected = true
        audioEngine.playCoin()
        addComboScore(100, coin.x, coin.y - 12f, null, 0xFFFDE047.toInt())
        spawnSparkles(coin.x, coin.y, 0xFFFFD700.toInt(), 10)
      }
    }
  }

  private fun updateBounceDrums() {
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val prevBottom = player.prevY + player.collisionHeight
    val currBottom = player.y + player.collisionHeight

    for (drum in bounceDrums) {
      if (drum.squashFrames > 0) drum.squashFrames--
      val overlapX = playerRight > drum.x + 2f && playerLeft < drum.x + drum.width - 2f
      if (overlapX && player.vy >= 0f && prevBottom <= drum.y + 12f && currBottom >= drum.y - 2f) {
        player.y = drum.y - player.collisionHeight
        player.vy = drum.launchVy
        player.isGrounded = false
        player.hasJumpCutApplied = false
        player.state = AaravState.JUMP_UP
        drum.squashFrames = 14
        audioEngine.playBounce()
        triggerCameraShake(5, 3.2f)
        addComboScore(100, drum.x + drum.width / 2f, drum.y - 14f, "DHOLAK BOUNCE!", 0xFFF59E0B.toInt())
        spawnSparkles(drum.x + drum.width / 2f, drum.y, 0xFFF59E0B.toInt(), 12)
      }
    }
  }

  private fun updateWindDrafts() {
    val cx = player.x + player.collisionWidth / 2f
    val cy = player.y + player.collisionHeight / 2f
    for (draft in windDrafts) {
      if (cx in draft.x..(draft.x + draft.width) && cy in draft.y..(draft.y + draft.height)) {
        if (player.vy > draft.maxRiseSpeed) {
          player.vy = (player.vy - draft.liftAccel).coerceAtLeast(draft.maxRiseSpeed)
          player.isGrounded = false
          player.hasJumpCutApplied = false
        }
        if (player.stateTimerFrames % 5 == 0) {
          spawnSparkles(cx, player.y + player.collisionHeight, 0xFF67E8F9.toInt(), 2)
        }
      }
    }
  }

  private fun updateCritters() {
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val playerTop = player.y
    val playerBottom = player.y + player.collisionHeight
    val prevBottom = player.prevY + player.collisionHeight

    for (critter in critters) {
      critter.walkTick++
      if (critter.stunFrames > 0) {
        critter.stunFrames--
        critter.alertFrames = 0
        critter.lungeFrames = 0
        critter.y = critter.baseY
        critter.vy = 0f
        continue
      }

      when (critter.behavior) {
        CritterBehavior.PATROL -> {
          critter.x += critter.vx
        }
        CritterBehavior.LUNGE_ON_PROXIMITY -> {
          val distX = (player.x + player.collisionWidth / 2f) - (critter.x + critter.width / 2f)
          val distY = kotlin.math.abs((player.y + player.collisionHeight) - (critter.baseY + critter.height))
          if (critter.lungeFrames > 0) {
            critter.lungeFrames--
            val speed = kotlin.math.abs(critter.vx) * 2.1f
            val dir = if (critter.vx >= 0f) 1f else -1f
            critter.x += dir * speed
          } else if (critter.alertFrames > 0) {
            critter.alertFrames--
            if (critter.alertFrames == 0) {
              critter.vx = if (distX >= 0f) kotlin.math.abs(critter.vx) else -kotlin.math.abs(critter.vx)
              critter.lungeFrames = 26
            }
          } else {
            if (kotlin.math.abs(distX) < 160f && distY < 64f && critter.walkTick % 2 == 0) {
              critter.alertFrames = 16
              critter.vx = if (distX >= 0f) kotlin.math.abs(critter.vx) else -kotlin.math.abs(critter.vx)
            } else {
              critter.x += critter.vx
            }
          }
        }
        CritterBehavior.HOPPING -> {
          critter.x += critter.vx
          critter.hopTimer++
          if (critter.y >= critter.baseY && critter.hopTimer >= 68) {
            critter.hopTimer = 0
            critter.vy = -7.2f
          }
          if (critter.y < critter.baseY || critter.vy < 0f) {
            critter.vy += 0.45f
            critter.y += critter.vy
            if (critter.y >= critter.baseY) {
              critter.y = critter.baseY
              critter.vy = 0f
            }
          }
        }
      }

      if (critter.x <= critter.minX) {
        critter.x = critter.minX
        critter.vx = kotlin.math.abs(critter.vx)
      } else if (critter.x + critter.width >= critter.maxX) {
        critter.x = critter.maxX - critter.width
        critter.vx = -kotlin.math.abs(critter.vx)
      }

      if (playerRight > critter.x + 4f && playerLeft < critter.x + critter.width - 4f &&
          playerBottom > critter.y + 2f && playerTop < critter.y + critter.height - 2f) {
        if (player.vy >= 0f && prevBottom <= critter.y + 14f) {
          critter.stunFrames = 180
          critter.alertFrames = 0
          critter.lungeFrames = 0
          critter.y = critter.baseY
          critter.vy = 0f
          player.y = critter.y - player.collisionHeight
          player.vy = -11.6f
          player.isGrounded = false
          player.hasJumpCutApplied = false
          player.state = AaravState.JUMP_UP
          audioEngine.playEnemyBonk()
          triggerCameraShake(4, 2.8f)
          addComboScore(150, critter.x + critter.width / 2f, critter.y - 10f, "BONK!", 0xFF34D399.toInt())
          spawnSparkles(critter.x + critter.width / 2f, critter.y, 0xFF34D399.toInt(), 10)
        } else {
          handlePlayerDamage()
        }
      }
    }
  }

  private fun updateFallingHazards() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight

    for (hz in fallingHazards) {
      if (hz.hasTriggered) continue

      if (!hz.isWarning && !hz.isFalling) {
        val guardianReady = guardianCfg == null || hz.id != 3 || stoneGuardian.phase == GuardianPhase.DEFEATED
        if (guardianReady && player.x in hz.triggerMinX..hz.triggerMaxX) {
          hz.isWarning = true
          hz.warningFrames = hz.warningDurationFrames
          hz.y = hz.startY
          hz.vy = 0f
          audioEngine.playCrumble()
          spawnDustTrickle(hz.x, hz.groundY - 4f)
          postNotification(
              eventKey = "HAZARD_FALL_${hz.id}",
              title = "Falling Hazard",
              message = "Rockslide overhead — dodge clear!",
              category = NotificationCategory.EVENT_WARNING,
              priority = NotificationPriority.CRITICAL,
              visualKind = NotificationVisualKind.FALLING_HAZARD,
              worldFocusX = hz.x,
              worldFocusY = hz.groundY - 40f
          )
        }
      } else if (hz.isWarning) {
        hz.warningFrames--
        if (hz.warningFrames % 7 == 0) {
          spawnDustTrickle(hz.x, hz.startY + 12f)
        }
        if (hz.warningFrames <= 0) {
          hz.isWarning = false
          hz.isFalling = true
          hz.vy = 2.8f
        }
      } else if (hz.isFalling) {
        hz.vy = (hz.vy + 0.36f).coerceAtMost(11.5f)
        hz.y += hz.vy

        // Fair circle-AABB contact check
        val closestX = hz.x.coerceIn(pLeft, pRight)
        val closestY = hz.y.coerceIn(pTop, pBottom)
        val dx = hz.x - closestX
        val dy = hz.y - closestY
        val hitR = hz.radius - 2f
        if (dx * dx + dy * dy <= hitR * hitR) {
          hz.isFalling = false
          hz.hasTriggered = true
          spawnDustPuff(hz.x, hz.y)
          handlePlayerDamage()
          continue
        }

        if (hz.y + hz.radius >= hz.groundY) {
          hz.isFalling = false
          hz.hasTriggered = true
          spawnDustPuff(hz.x, hz.groundY)
          triggerCameraShake(4, 2.4f)
        }
      }
    }
  }

  private fun updateAmbushTraps() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight

    for (trap in ambushTraps) {
      when (trap.phase) {
        TrapPhase.ARMED -> {
          trap.currentHeight = 0f
          val nearX = pRight > trap.x - trap.triggerPadding && pLeft < trap.x + trap.width + trap.triggerPadding
          val nearY = kotlin.math.abs(pBottom - trap.groundY) < 96f
          if (nearX && nearY) {
            trap.phase = TrapPhase.WARNING
            trap.phaseTimerFrames = trap.warningDurationFrames
          }
        }
        TrapPhase.WARNING -> {
          trap.phaseTimerFrames--
          if (trap.phaseTimerFrames <= 0) {
            trap.phase = TrapPhase.ACTIVE
            trap.phaseTimerFrames = trap.activeDurationFrames
            trap.currentHeight = trap.maxHeight
            audioEngine.playCrumble()
            spawnDustPuff(trap.x + trap.width / 2f, trap.groundY)
          }
        }
        TrapPhase.ACTIVE -> {
          val tTop = trap.groundY - trap.currentHeight + 3f
          if (pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
              pBottom > tTop && pTop < trap.groundY) {
            handlePlayerDamage()
          }
          trap.phaseTimerFrames--
          if (trap.phaseTimerFrames <= 0) {
            trap.phase = TrapPhase.COOLDOWN
            trap.phaseTimerFrames = 52
            trap.currentHeight = 0f
          }
        }
        TrapPhase.COOLDOWN -> {
          trap.phaseTimerFrames--
          if (trap.phaseTimerFrames <= 0) {
            trap.phase = TrapPhase.ARMED
          }
        }
      }
    }
  }

  private fun updatePendulums() {
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val playerTop = player.y
    val playerBottom = player.y + player.collisionHeight

    for (pend in pendulums) {
      pend.currentFrame = (pend.currentFrame + 1) % pend.periodFrames
      val phase = (2.0 * PI * pend.currentFrame / pend.periodFrames)
      val angle = pend.maxAngleRad * sin(phase)
      pend.bobX = pend.anchorX + (pend.length * sin(angle)).toFloat()
      pend.bobY = pend.anchorY + (pend.length * kotlin.math.cos(angle)).toFloat()

      val closestX = pend.bobX.coerceIn(playerLeft, playerRight)
      val closestY = pend.bobY.coerceIn(playerTop, playerBottom)
      val dx = pend.bobX - closestX
      val dy = pend.bobY - closestY
      val hitR = pend.bobRadius - 2f
      if (dx * dx + dy * dy <= hitR * hitR) {
        handlePlayerDamage()
      }
    }
  }

  private fun updateDiyas() {
    val cx = player.x + player.collisionWidth / 2f
    val cy = player.y + player.collisionHeight / 2f
    for (diya in diyas) {
      if (diya.isLit) continue
      val dx = cx - diya.x
      val dy = cy - diya.y
      val r = diya.radius + player.collisionWidth / 2f + 4f
      if (dx * dx + dy * dy <= r * r) {
        diya.isLit = true
        audioEngine.playDiyaLit()
        addComboScore(250, diya.x, diya.y - 16f, "DIYA LIT!", 0xFFFDE047.toInt())
        spawnSparkles(diya.x, diya.y, 0xFFF59E0B.toInt(), 14)
        if (diyas.all { it.isLit }) {
          score += 500
          spawnPopup(diya.x, diya.y - 32f, "FESTIVAL FLAME +500!", 0xFF34D399.toInt())
          postNotification(
              eventKey = "INFO_ALL_DIYAS_LIT",
              title = "Festival Flame",
              message = "All ${diyas.size} brass diyas lit (+500)!",
              category = NotificationCategory.TRIUMPH,
              priority = NotificationPriority.INFO,
              visualKind = NotificationVisualKind.INFO
          )
        }
      }
    }
  }

  private fun addComboScore(
      basePoints: Int,
      popupX: Float,
      popupY: Float,
      label: String?,
      color: Int
  ) {
    val gained = basePoints * comboMultiplier
    score += gained
    val text = if (label != null) {
      if (comboMultiplier > 1) "$label +$gained (x$comboMultiplier)" else "$label +$gained"
    } else {
      if (comboMultiplier > 1) "+$gained x$comboMultiplier" else "+$gained"
    }
    spawnPopup(popupX, popupY, text, color)
    comboMultiplier = (comboMultiplier + 1).coerceAtMost(4)
    comboTimerFrames = COMBO_WINDOW_FRAMES
  }

  private fun triggerCameraShake(frames: Int, magnitude: Float) {
    if (frames > cameraShakeFrames) cameraShakeFrames = frames
    if (magnitude > cameraShakeMagnitude) cameraShakeMagnitude = magnitude
  }

  private fun updateCheckpoint() {
    val checkpointTile = tiles.firstOrNull { it.type == TileType.CHECKPOINT_BELL } ?: return
    val playerRight = player.x + player.collisionWidth
    val playerLeft = player.x
    val playerBottom = player.y + player.collisionHeight
    val playerTop = player.y

    if (playerRight > checkpointTile.x && playerLeft < checkpointTile.x + checkpointTile.width &&
        playerBottom > checkpointTile.y && playerTop < checkpointTile.y + checkpointTile.height) {
      if (!hasCheckpointActivated) {
        hasCheckpointActivated = true
        audioEngine.playCheckpoint()
        spawnSparkles(checkpointTile.x + checkpointTile.width / 2, checkpointTile.y + checkpointTile.height / 2, 0xFFFFCC00.toInt(), 16)
        spawnPopup(checkpointTile.x + 24f, checkpointTile.y - 12f, "SHRINE BOUND!", 0xFF67E8F9.toInt())

        // Capture immutable snapshot (including temporary puzzle & critter states)
        val tileSnaps = tiles.map {
          TileSnapshot(
              tileId = it.id,
              isVisible = it.isVisible,
              isSolid = it.isSolid,
              isTriggered = it.isTriggered,
              crumbleTimerFrames = it.crumbleTimerFrames,
              isCrumbling = it.isCrumbling
          )
        }

        val mpSnaps = movingPlatforms.map {
          MovingPlatformSnapshot(
              platformId = it.id,
              x = it.x,
              y = it.y,
              vx = it.vx,
              currentFrame = it.currentFrame
          )
        }

        val critterSnaps = critters.map {
          CritterSnapshot(
              critterId = it.id,
              x = it.x,
              vx = it.vx,
              stunFrames = it.stunFrames
          )
        }

        val collectedCoinsAtCp = coins.filter { it.isCollected }.map { it.id }.toSet()
        val litDiyasAtCp = diyas.filter { it.isLit }.map { it.id }.toSet()
        val switchSnaps = soundSwitches.map {
          SwitchSnapshot(
              switchId = it.id,
              isActivated = it.isActivated,
              remainingFrames = it.remainingFrames
          )
        }
        val pillarSnaps = collapsingPillars.map {
          PillarSnapshot(
              pillarId = it.id,
              currentDropY = it.currentDropY,
              phase = it.phase,
              timerFrames = it.timerFrames
          )
        }
        val collectedRelicsAtCp = relics.filter { it.isCollected }.map { it.id }.toSet()
        val trapSnaps = trapSystem.captureSnapshots(trollTraps)
        val orbitingSnaps = orbitingPlatforms.map {
          OrbitingPlatformSnapshot(
              platformId = it.id,
              x = it.x,
              y = it.y,
              vx = it.vx,
              vy = it.vy,
              currentFrame = it.currentFrame
          )
        }
        val pendulumSnaps = pendulums.map {
          PendulumSnapshot(
              pendulumId = it.id,
              currentFrame = it.currentFrame,
              bobX = it.bobX,
              bobY = it.bobY
          )
        }
        val lightningSnaps = lightningHazards.map {
          LightningSnapshot(
              hazardId = it.id,
              currentFrame = it.currentFrame,
              phase = it.phase
          )
        }

        checkpointSnapshot = LevelCheckpointSnapshot(
            playerX = checkpointTile.x + 8f,
            playerY = checkpointTile.y + checkpointTile.height - player.collisionHeight,
            playerFacingRight = true,
            playerHealth = player.health,
            levelTimerMs = levelTimerMs,
            scoreAtCheckpoint = score,
            coinsCollectedAtCheckpoint = collectedCoinsAtCp,
            litDiyasAtCheckpoint = litDiyasAtCp,
            tileSnapshots = tileSnaps,
            movingPlatformSnapshots = mpSnaps,
            critterSnapshots = critterSnaps,
            echoSealActivatedAtCheckpoint = echoPuzzleNode?.isActivated ?: false,
            resonanceStoneCollectedAtCheckpoint = resonanceStone?.isCollected ?: false,
            treeResolvedAtCheckpoint = awakeningTree.phase == TreePhase.RESOLVED,
            stalkerRepelledAtCheckpoint = forestStalker.phase == StalkerPhase.REPELLED,
            guardianDefeatedAtCheckpoint = stoneGuardian.phase == GuardianPhase.DEFEATED,
            triggeredFallingHazardIds = fallingHazards.filter { it.hasTriggered }.map { it.id }.toSet(),
            switchSnapshots = switchSnaps,
            pillarSnapshots = pillarSnaps,
            collectedRelicsAtCheckpoint = collectedRelicsAtCp,
            challengeTimerMs = challengeTimerMs,
            exitXAtCheckpoint = currentExitX,
            exitYAtCheckpoint = currentExitY,
            trollTrapSnapshots = trapSnaps,
            orbitingPlatformSnapshots = orbitingSnaps,
            pendulumSnapshots = pendulumSnaps,
            lightningSnapshots = lightningSnaps
        )

        // E3: Checkpoint Bell Glyph (permanent story clue #3) — only on 1-1 Whispering Woods
        if (level.enableForestSetPieces && !triggeredEvents.contains(GameplayEventId.E3_CHECKPOINT_BELL_GLYPH)) {
          triggeredEvents.add(GameplayEventId.E3_CHECKPOINT_BELL_GLYPH)
          discoverClue(
              StoryClueId.CHECKPOINT_BELL_GLYPH,
              GameplayEventId.E3_CHECKPOINT_BELL_GLYPH,
              playAudio = false
          )
        } else if (!level.enableForestSetPieces) {
          postNotification(
              eventKey = "INFO_CHECKPOINT_BOUND",
              title = "Shrine Bound",
              message = "Checkpoint anchored at shrine bell.",
              category = NotificationCategory.STORY_CLUE,
              priority = NotificationPriority.INFO,
              visualKind = NotificationVisualKind.INFO
          )
        }
      }
    }
  }

  // --- SET PIECE A: THE AWAKENING TREE ---

  private fun updateAwakeningTreeSetPiece() {
    when (awakeningTree.phase) {
      TreePhase.IDLE -> {
        if (player.x in 1860f..2090f) {
          awakeningTree.phase = TreePhase.WARNING
          awakeningTree.timerFrames = 42
          audioEngine.playTreeRumble()
          triggerCameraShake(14, 2.4f)
          postNotification(
              eventKey = "SP_TREE_WARN",
              title = "Banyan Awakening",
              message = "Roots rising ahead — climb clear!",
              category = NotificationCategory.EVENT_WARNING,
              priority = NotificationPriority.CRITICAL,
              visualKind = NotificationVisualKind.HAZARD_WARNING,
              worldFocusX = awakeningTree.trunkX,
              worldFocusY = awakeningTree.groundY - 52f
          )
        }
      }
      TreePhase.WARNING -> {
        awakeningTree.timerFrames--
        if (awakeningTree.timerFrames % 5 == 0) {
          for (root in awakeningTree.roots) {
            spawnDustPuff(root.x + root.width / 2f, root.baseY)
          }
        }
        if (awakeningTree.timerFrames <= 0) {
          awakeningTree.phase = TreePhase.ACTIVE
          awakeningTree.timerFrames = 0
          audioEngine.playCrumble()
          triggerCameraShake(10, 3.2f)
        }
      }
      TreePhase.ACTIVE -> {
        awakeningTree.timerFrames++
        for (root in awakeningTree.roots) {
          if (root.currentHeight < root.maxHeight) {
            root.currentHeight = (root.currentHeight + 6.5f).coerceAtMost(root.maxHeight)
            // Keep Aarav safely perched atop a growing root rather than clipping inside
            val pRight = player.x + player.collisionWidth
            val rTop = root.baseY - root.currentHeight
            if (pRight > root.x + 2f && player.x < root.x + root.width - 2f &&
                player.y + player.collisionHeight > rTop && player.y < root.baseY) {
              player.y = rTop - player.collisionHeight
              player.vy = 0f
              player.isGrounded = true
            }
          }
        }

        // Telegraphed canopy acorns (fair rhythmic spacing)
        if (awakeningTree.timerFrames % 48 == 12) {
          val slot = awakeningTree.acorns.firstOrNull { !it.active }
          if (slot != null) {
            val offset = if ((awakeningTree.timerFrames / 48) % 2 == 0) -32f else 28f
            slot.x = awakeningTree.trunkX + offset
            slot.y = awakeningTree.groundY - 245f
            slot.vy = 2.8f
            slot.active = true
          }
        }
        updateTreeAcorns()

        if (player.x >= 2125f) {
          awakeningTree.phase = TreePhase.RESOLVED
          for (a in awakeningTree.acorns) a.active = false
          addComboScore(250, player.x + 16f, player.y - 18f, "ROOTS CONQUERED!", 0xFF34D399.toInt())
          spawnSparkles(awakeningTree.trunkX + 20f, awakeningTree.groundY - 96f, 0xFF34D399.toInt(), 14)
          postNotification(
              eventKey = "SP_TREE_CLEAR",
              title = "Path Cleared",
              message = "You scaled the Awakening Banyan!",
              category = NotificationCategory.TRIUMPH,
              priority = NotificationPriority.DISCOVERY,
              visualKind = NotificationVisualKind.TRIUMPH,
              supersedesKeys = setOf("SP_TREE_WARN")
          )
        }
      }
      TreePhase.RESOLVED -> {
        for (root in awakeningTree.roots) {
          if (root.currentHeight < root.maxHeight) {
            root.currentHeight = (root.currentHeight + 6.5f).coerceAtMost(root.maxHeight)
          }
        }
        updateTreeAcorns()
      }
    }
  }

  private fun updateTreeAcorns() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight
    for (acorn in awakeningTree.acorns) {
      if (!acorn.active) continue
      acorn.vy = (acorn.vy + 0.22f).coerceAtMost(7.5f)
      acorn.y += acorn.vy
      if (acorn.y + acorn.radius >= awakeningTree.groundY) {
        spawnDustPuff(acorn.x, awakeningTree.groundY)
        acorn.active = false
        continue
      }
      if (acorn.x + acorn.radius > pLeft + 4f && acorn.x - acorn.radius < pRight - 4f &&
          acorn.y + acorn.radius > pTop + 4f && acorn.y - acorn.radius < pBottom - 4f) {
        acorn.active = false
        handlePlayerDamage()
      }
    }
  }

  // --- SET PIECE B: THE GREAT FOREST CHASE (VANAR SHADOW STALKER) ---

  private fun updateForestStalkerSetPiece() {
    when (forestStalker.phase) {
      StalkerPhase.IDLE -> {
        if (player.x in 3140f..3640f) {
          forestStalker.phase = StalkerPhase.WARNING
          forestStalker.phaseTimerFrames = 32
          forestStalker.x = (player.x - 210f).coerceAtLeast(3040f)
          forestStalker.y = 12 * 48f - forestStalker.height
          forestStalker.stunPauseFrames = 0
          audioEngine.playBeastRoar()
          triggerCameraShake(8, 2.5f)
          postNotification(
              eventKey = "SP_STALKER_WARN",
              title = "Shadow Stalker",
              message = "Sprint ahead to the Echo Seal!",
              category = NotificationCategory.EVENT_WARNING,
              priority = NotificationPriority.CRITICAL,
              visualKind = NotificationVisualKind.HAZARD_WARNING,
              worldFocusX = forestStalker.x,
              worldFocusY = forestStalker.y
          )
        }
      }
      StalkerPhase.WARNING -> {
        forestStalker.phaseTimerFrames--
        forestStalker.strideTick++
        if (forestStalker.phaseTimerFrames <= 0) {
          forestStalker.phase = StalkerPhase.CHASING
        }
      }
      StalkerPhase.CHASING -> {
        forestStalker.strideTick++
        if (forestStalker.stunPauseFrames > 0) {
          forestStalker.stunPauseFrames--
        } else {
          // Moves swiftly yet stays fairly outrunnable below MAX_RUN_SPEED (6.5f)
          val targetSpeed = if (player.x - forestStalker.x > 260f) 5.75f else 5.25f
          val dir = if (player.x >= forestStalker.x) 1f else -1f
          forestStalker.vx = dir * targetSpeed
          forestStalker.x = (forestStalker.x + forestStalker.vx).coerceAtMost(3850f)

          // Climb onto High Cliff Ledge (Tile 76, y = 10*48 = 480) smoothly across gap
          val desiredGroundY = if (forestStalker.x >= 3520f) 10 * 48f else 12 * 48f
          val targetY = desiredGroundY - forestStalker.height
          forestStalker.y += (targetY - forestStalker.y) * 0.22f
        }

        // If player cleared out past Tile 81 toward Moving Platform, Stalker retreats cleanly
        if (player.x >= 3905f) {
          forestStalker.phase = StalkerPhase.REPELLED
          spawnDustPuff(forestStalker.x + 24f, forestStalker.y + forestStalker.height)
          return
        }

        // Contact check with Aarav
        if (forestStalker.stunPauseFrames <= 0) {
          val pLeft = player.x
          val pRight = player.x + player.collisionWidth
          val pTop = player.y
          val pBottom = player.y + player.collisionHeight
          if (pRight > forestStalker.x + 6f && pLeft < forestStalker.x + forestStalker.width - 6f &&
              pBottom > forestStalker.y + 6f && pTop < forestStalker.y + forestStalker.height - 4f) {
            forestStalker.stunPauseFrames = 48
            handlePlayerDamage()
          }
        }
      }
      StalkerPhase.REPELLED -> Unit
    }
  }

  // --- SET PIECE D + C: STONE GUARDIAN (SHILA-RAKSHAK) & VANISHING CAUSEWAY ---

  private fun updateStoneGuardianSetPiece() {
    when (stoneGuardian.phase) {
      GuardianPhase.SLEEPING -> {
        if (player.x in stoneGuardian.triggerMinX..stoneGuardian.triggerMaxX) {
          stoneGuardian.phase = GuardianPhase.AWAKENING
          stoneGuardian.phaseTimerFrames = 40
          stoneGuardian.crystalInvulnFrames = 0
          audioEngine.playGuardianGong()
          triggerCameraShake(14, 3.2f)
          val msg = if (stoneGuardian.isTempleBoss) {
            "Shatter its Last Light Crown!"
          } else {
            "Strike its Sun Crystal!"
          }
          postNotification(
              eventKey = "SP_GUARDIAN_WARN",
              title = if (stoneGuardian.isTempleBoss) "Vajra-Surya Awakens" else "Guardian Awakens",
              message = msg,
              category = NotificationCategory.EVENT_WARNING,
              priority = NotificationPriority.CRITICAL,
              visualKind = NotificationVisualKind.BOSS_WARNING,
              worldFocusX = stoneGuardian.x,
              worldFocusY = stoneGuardian.groundY - 50f
          )
        }
      }
      GuardianPhase.AWAKENING -> {
        stoneGuardian.phaseTimerFrames--
        if (stoneGuardian.phaseTimerFrames % 6 == 0) {
          spawnDustPuff(stoneGuardian.x + stoneGuardian.width / 2f, stoneGuardian.groundY)
        }
        if (stoneGuardian.phaseTimerFrames <= 0) {
          stoneGuardian.phase = GuardianPhase.ACTIVE
          stoneGuardian.attackCooldownFrames = if (level.difficulty == DifficultyTier.NIGHTMARE) 38 else 55
          stoneGuardian.windupFrames = 0
        }
      }
      GuardianPhase.ACTIVE -> {
        stoneGuardian.phaseTimerFrames++
        if (stoneGuardian.crystalInvulnFrames > 0) {
          stoneGuardian.crystalInvulnFrames--
        }

        // Check weak-spot Sun Crown Crystal hit by Aarav
        val pCx = player.x + player.collisionWidth / 2f
        val pCy = player.y + player.collisionHeight / 2f
        val cdx = pCx - stoneGuardian.crystalX
        val cdy = pCy - stoneGuardian.crystalY
        val hitDist = stoneGuardian.crystalRadius + player.collisionWidth / 2f + 4f
        if (stoneGuardian.crystalInvulnFrames <= 0 && cdx * cdx + cdy * cdy <= hitDist * hitDist) {
          stoneGuardian.hitsRemaining--
          if (stoneGuardian.hitsRemaining > 0) {
            stoneGuardian.crystalInvulnFrames = 36
            stoneGuardian.attackCooldownFrames = 26
            stoneGuardian.windupFrames = 0
            player.vy = -12.4f
            player.isGrounded = false
            player.hasJumpCutApplied = false
            audioEngine.playGuardianGong()
            triggerCameraShake(7, 3.6f)
            addComboScore(250, stoneGuardian.crystalX, stoneGuardian.crystalY - 12f, "CRYSTAL CRACKED!", 0xFFFB923C.toInt())
            spawnSparkles(stoneGuardian.crystalX, stoneGuardian.crystalY, 0xFFFB923C.toInt(), 18)
            val phaseMsg = if (stoneGuardian.isTempleBoss && stoneGuardian.hitsRemaining == 1) {
              "Solar shockwaves accelerating!"
            } else {
              "Beware rebounding shockwaves!"
            }
            postNotification(
                eventKey = "SP_GUARDIAN_PHASE_${stoneGuardian.hitsRemaining}",
                title = "Guardian Enraged",
                message = phaseMsg,
                category = NotificationCategory.EVENT_WARNING,
                priority = NotificationPriority.CRITICAL,
                visualKind = NotificationVisualKind.BOSS_WARNING,
                worldFocusX = stoneGuardian.x,
                worldFocusY = stoneGuardian.groundY - 50f,
                supersedesKeys = setOf("SP_GUARDIAN_WARN")
            )
          } else {
            stoneGuardian.phase = GuardianPhase.DEFEATED
            for (sw in stoneGuardian.shockwaves) sw.active = false
            player.vy = -12.4f
            player.isGrounded = false
            player.hasJumpCutApplied = false
            audioEngine.playGuardianDefeat()
            triggerCameraShake(9, 4.2f)
            addComboScore(if (stoneGuardian.isTempleBoss) 1000 else 500, stoneGuardian.crystalX, stoneGuardian.crystalY - 12f, "GUARDIAN SHATTERED!", 0xFFFDE047.toInt())
            spawnSparkles(stoneGuardian.crystalX, stoneGuardian.crystalY, 0xFFFDE047.toInt(), 24)
            val clearMsg = if (stoneGuardian.isTempleBoss) {
              "Vajra-Surya down — enter Last Light!"
            } else {
              "Sun Gate threshold is now open!"
            }
            postNotification(
                eventKey = "SP_GUARDIAN_CLEAR",
                title = "Guardian Vanquished",
                message = clearMsg,
                category = NotificationCategory.TRIUMPH,
                priority = NotificationPriority.DISCOVERY,
                visualKind = NotificationVisualKind.TRIUMPH,
                supersedesKeys = setOf(
                    "SP_GUARDIAN_WARN",
                    "SP_GUARDIAN_PHASE_1",
                    "SP_GUARDIAN_PHASE_2",
                    "SP_GUARDIAN_PHASE_3",
                    "SP_GUARDIAN_PHASE_4",
                    "SP_GUARDIAN_PHASE_5",
                    "SP_SLAB_CRACK",
                    "BOSS_SEAL_GATE"
                )
            )
            return
          }
        }

        // Telegraphed Shockwave attack cycle
        if (stoneGuardian.windupFrames > 0) {
          stoneGuardian.windupFrames--
          if (stoneGuardian.windupFrames <= 0) {
            val slot = stoneGuardian.shockwaves.firstOrNull { !it.active }
            if (slot != null) {
              val isEnragedRebound = (level.difficulty != DifficultyTier.NORMAL || stoneGuardian.isTempleBoss) &&
                  (stoneGuardian.hitsRemaining < stoneGuardian.maxHits || level.difficulty == DifficultyTier.NIGHTMARE)
              slot.active = true
              slot.x = stoneGuardian.x - 10f
              slot.y = stoneGuardian.groundY - 14f
              val speedBoost = if (stoneGuardian.isTempleBoss && stoneGuardian.hitsRemaining <= 1) -0.9f else 0f
              slot.vx = when (level.difficulty) {
                DifficultyTier.NORMAL -> -5.4f + speedBoost
                DifficultyTier.HARD -> -6.1f + speedBoost
                DifficultyTier.NIGHTMARE -> -6.8f + speedBoost
              }
              slot.isRebound = isEnragedRebound
              slot.hasReversed = false
              slot.reboundX = stoneGuardian.reboundX
            }
            audioEngine.playBounce()
            triggerCameraShake(4, 2.2f)
            stoneGuardian.attackCooldownFrames = when {
              stoneGuardian.isTempleBoss && stoneGuardian.hitsRemaining <= 1 -> 54
              level.difficulty == DifficultyTier.NIGHTMARE -> 68
              stoneGuardian.hitsRemaining < stoneGuardian.maxHits -> 74
              else -> 95
            }
          }
        } else if (stoneGuardian.attackCooldownFrames > 0) {
          stoneGuardian.attackCooldownFrames--
          if (stoneGuardian.attackCooldownFrames <= 0) {
            stoneGuardian.windupFrames = if (level.difficulty == DifficultyTier.NIGHTMARE) 20 else 26
          }
        }

        updateGuardianShockwaves()
      }
      GuardianPhase.DEFEATED -> Unit
    }
  }

  private fun updateGuardianShockwaves() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight
    for (sw in stoneGuardian.shockwaves) {
      if (!sw.active) continue
      sw.x += sw.vx
      if (sw.isRebound && !sw.hasReversed && sw.x <= sw.reboundX) {
        sw.hasReversed = true
        sw.vx = kotlin.math.abs(sw.vx) * 0.9f
        spawnSparkles(sw.x, sw.y, 0xFFFB923C.toInt(), 8)
      }
      if (sw.x < stoneGuardian.despawnMinX || (sw.hasReversed && sw.x > stoneGuardian.x + 20f)) {
        sw.active = false
        continue
      }
      if (sw.x + sw.radius > pLeft + 4f && sw.x - sw.radius < pRight - 4f &&
          sw.y + sw.radius > pTop + 4f && sw.y - sw.radius < pBottom - 2f) {
        sw.active = false
        handlePlayerDamage()
      }
    }
  }

  private fun updateMysteryEvents() {
    val playerCenterX = player.x + player.collisionWidth / 2
    val playerBottom = player.y + player.collisionHeight
    val playerCenterY = player.y + player.collisionHeight / 2

    // E1 (Ash-Prints) & E6 (Ancient Carving) world clue markers
    for (marker in level.clueMarkers) {
      val eventId = when (marker.clueId) {
        StoryClueId.ASH_PRINTS -> GameplayEventId.E1_ASH_PRINTS
        StoryClueId.ANCIENT_CARVING -> GameplayEventId.E6_ANCIENT_CARVING
        else -> continue
      }
      if (triggeredEvents.contains(eventId)) continue

      val dx = playerCenterX - (marker.x + 24f)
      val dy = playerBottom - marker.y
      if (dx * dx + dy * dy <= marker.triggerRadius * marker.triggerRadius) {
        triggeredEvents.add(eventId)
        discoverClue(marker.clueId, eventId)
      }
    }

    // E2: Subtle Watcher silhouette in canopy on Beat 2 clearing (1-1 only)
    if (level.enableForestSetPieces &&
        !triggeredEvents.contains(GameplayEventId.E2_WATCHER_CANOPY) &&
        player.x >= 1464f && playerBottom <= 640f) {
      triggeredEvents.add(GameplayEventId.E2_WATCHER_CANOPY)
      watcherAppearance.isActive = true
      watcherAppearance.worldX = 1740f
      watcherAppearance.worldY = 505f
      watcherAppearance.totalFrames = 150
      watcherAppearance.remainingFrames = 150
      discoverClue(StoryClueId.WATCHER, GameplayEventId.E2_WATCHER_CANOPY)
    }

    // E4: Optional Bonus Environmental Discovery — Resonance Stone in Lower Basin
        resonanceStone?.let { stone ->
          stone.animTimer += 0.06f
          if (!stone.isCollected) {
            val dx = playerCenterX - stone.x
            val dy = playerCenterY - stone.y
            val pickupDist = stone.radius + player.collisionWidth / 2 + 4f
            if (dx * dx + dy * dy <= pickupDist * pickupDist) {
              stone.isCollected = true
              isResonanceStoneDiscovered = true
              onResonanceDiscovered()
              triggeredEvents.add(GameplayEventId.E4_RESONANCE_STONE_BASIN)
              audioEngine.playResonanceHum()
              spawnSparkles(stone.x, stone.y, 0xFF38BDF8.toInt(), 12)
              postNotification(
                  eventKey = GameplayEventId.E4_RESONANCE_STONE_BASIN.name,
                  title = "Secret Discovery",
                  message = "Resonance Stone attuned — warm chime echoes!",
                  category = NotificationCategory.SECRET_DISCOVERY,
                  priority = NotificationPriority.DISCOVERY,
                  visualKind = NotificationVisualKind.DISCOVERY
              )
              onWhisperEvent(
                  GameplayEventId.E4_RESONANCE_STONE_BASIN,
                  "Resonance Stone attuned — warm chime echoes!"
              )
            }
          }
        }

        // E5: Canopy Echo Seal at Beat 4 High Cliff Ledge (Tile 79) — also blasts Shadow Stalker back!
        echoPuzzleNode?.let { node ->
          if (!node.isActivated) {
            val dx = playerCenterX - (node.x + 24f)
            val dy = playerBottom - node.y
            if (dx * dx + dy * dy <= node.triggerRadius * node.triggerRadius) {
              node.isActivated = true
              triggeredEvents.add(GameplayEventId.E5_CANOPY_ECHO_SEAL)
              audioEngine.playResonanceHum()
              spawnSparkles(node.x + 24f, node.y - 8f, 0xFF14B8A6.toInt(), 12)
              watcherAppearance.isActive = true
              watcherAppearance.worldX = 4340f
              watcherAppearance.worldY = 260f
              watcherAppearance.totalFrames = 120
              watcherAppearance.remainingFrames = 120
              if (forestStalker.phase == StalkerPhase.WARNING || forestStalker.phase == StalkerPhase.CHASING) {
                forestStalker.phase = StalkerPhase.REPELLED
                spawnSparkles(forestStalker.x + 24f, forestStalker.y + 18f, 0xFF2DD4BF.toInt(), 16)
                addComboScore(300, node.x + 24f, node.y - 24f, "STALKER REPELLED!", 0xFF2DD4BF.toInt())
                val msg = "Echo Seal repelled the Shadow Stalker!"
                postNotification(
                    eventKey = GameplayEventId.E5_CANOPY_ECHO_SEAL.name,
                    title = "Stalker Repelled",
                    message = msg,
                    category = NotificationCategory.TRIUMPH,
                    priority = NotificationPriority.DISCOVERY,
                    visualKind = NotificationVisualKind.TRIUMPH,
                    supersedesKeys = setOf("SP_STALKER_WARN")
                )
                onWhisperEvent(GameplayEventId.E5_CANOPY_ECHO_SEAL, msg)
              } else {
                val msg = "Canopy Echo Seal lit — motes guide across the chasm."
                postNotification(
                    eventKey = GameplayEventId.E5_CANOPY_ECHO_SEAL.name,
                    title = "Echo Seal Lit",
                    message = msg,
                    category = NotificationCategory.SECRET_DISCOVERY,
                    priority = NotificationPriority.DISCOVERY,
                    visualKind = NotificationVisualKind.DISCOVERY
                )
                onWhisperEvent(GameplayEventId.E5_CANOPY_ECHO_SEAL, msg)
              }
            }
          }
        }
  }

  private fun discoverClue(
      clueId: StoryClueId,
      eventId: GameplayEventId,
      playAudio: Boolean = true
  ) {
    val isNewlyDiscovered = discoveredClues.add(clueId)
    if (isNewlyDiscovered) {
      onClueDiscovered(clueId)
    }
    if (playAudio) {
      audioEngine.playClueDiscovery()
    }
    val title = if (clueId == StoryClueId.WATCHER) {
      "Watcher Sighting"
    } else {
      "Clue ${clueId.number}/4 • ${clueId.title}"
    }
    val bannerMessage = when (clueId) {
      StoryClueId.ASH_PRINTS -> "Warm ash-prints lead into the woods."
      StoryClueId.WATCHER -> "Canopy silhouette watches, then fades."
      StoryClueId.CHECKPOINT_BELL_GLYPH -> "Teal glyph awakens on the shrine bell."
      StoryClueId.ANCIENT_CARVING -> "Carving: look above the main trail."
    }
    postNotification(
        eventKey = eventId.name,
        title = title,
        message = bannerMessage,
        category = NotificationCategory.STORY_CLUE,
        priority = NotificationPriority.STORY,
        visualKind = NotificationVisualKind.STORY
    )
    onWhisperEvent(eventId, clueId.whisperText)
  }

  fun showContextNotification(
      eventKey: String,
      message: String,
      category: NotificationCategory = NotificationCategory.STORY_CLUE
  ) {
    val priority = when (category) {
      NotificationCategory.EVENT_WARNING -> NotificationPriority.CRITICAL
      NotificationCategory.SECRET_DISCOVERY -> NotificationPriority.DISCOVERY
      NotificationCategory.TRIUMPH -> NotificationPriority.DISCOVERY
      NotificationCategory.STORY_CLUE -> NotificationPriority.STORY
    }
    val visualKind = when (category) {
      NotificationCategory.EVENT_WARNING -> NotificationVisualKind.HAZARD_WARNING
      NotificationCategory.SECRET_DISCOVERY -> NotificationVisualKind.DISCOVERY
      NotificationCategory.TRIUMPH -> NotificationVisualKind.TRIUMPH
      NotificationCategory.STORY_CLUE -> NotificationVisualKind.STORY
    }
    val title = when (category) {
      NotificationCategory.EVENT_WARNING -> "Hazard Warning"
      NotificationCategory.SECRET_DISCOVERY -> "Discovery"
      NotificationCategory.TRIUMPH -> "Milestone"
      NotificationCategory.STORY_CLUE -> "Forest Whisper"
    }
    postNotification(
        eventKey = eventKey,
        title = title,
        message = message,
        category = category,
        priority = priority,
        visualKind = visualKind
    )
  }

  fun postNotification(
      eventKey: String,
      title: String,
      message: String,
      category: NotificationCategory = NotificationCategory.STORY_CLUE,
      priority: NotificationPriority = NotificationPriority.STORY,
      visualKind: NotificationVisualKind = NotificationVisualKind.STORY,
      worldFocusX: Float? = null,
      worldFocusY: Float? = null,
      supersedesKeys: Set<String> = emptySet()
  ): Boolean {
    val active = notificationController.activeNotification
    if (active != null &&
        active.worldFocusX != null &&
        kotlin.math.abs(player.x - active.worldFocusX) > 400f &&
        !notificationController.hasShownKey(eventKey)) {
      notificationController.invalidateKeys(listOf(active.eventKey))
    }
    val posted = notificationController.post(
        GameplayNotification(
            eventKey = eventKey,
            title = title,
            message = message,
            category = category,
            priority = priority,
            visualKind = visualKind,
            durationFrames = priority.defaultDurationFrames,
            minVisibleFrames = priority.minVisibleFrames,
            worldFocusX = worldFocusX,
            worldFocusY = worldFocusY,
            supersedesKeys = supersedesKeys
        )
    )
    if (posted) {
      publishHudSnapshot()
    }
    return posted
  }

  private fun triggerWhisper(
      eventId: GameplayEventId,
      message: String,
      category: NotificationCategory = NotificationCategory.STORY_CLUE
  ) {
    showContextNotification(eventId.name, message, category)
    onWhisperEvent(eventId, message)
  }

  private fun updateOrbitingPlatforms() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight

    for (op in orbitingPlatforms) {
      val prevX = op.x
      val prevY = op.y
      op.currentFrame = (op.currentFrame + 1) % op.periodFrames
      val dir = if (op.clockwise) 1f else -1f
      val angle = op.phaseOffsetRad + dir * (2.0 * PI * op.currentFrame / op.periodFrames).toFloat()
      val cx = op.pivotX + op.orbitRadius * kotlin.math.cos(angle.toDouble()).toFloat()
      val cy = op.pivotY + op.orbitRadius * sin(angle.toDouble()).toFloat()
      op.x = cx - op.width / 2f
      op.y = cy - op.height / 2f
      op.vx = op.x - prevX
      op.vy = op.y - prevY

      if (op.isHazardGear) {
        val gearR = op.width * 0.45f
        val closestX = cx.coerceIn(pLeft, pRight)
        val closestY = cy.coerceIn(pTop, pBottom)
        val dx = cx - closestX
        val dy = cy - closestY
        if (dx * dx + dy * dy <= gearR * gearR) {
          handlePlayerDamage()
        }
      }
    }
  }

  private fun updateCollapsingPillars() {
    for (pillar in collapsingPillars) {
      when (pillar.phase) {
        TrapPhase.ARMED -> Unit
        TrapPhase.WARNING -> {
          pillar.timerFrames--
          if (pillar.timerFrames <= 0) {
            pillar.phase = TrapPhase.ACTIVE
            audioEngine.playCrumble()
            triggerCameraShake(4, 2.2f)
          }
        }
        TrapPhase.ACTIVE -> {
          val oldTop = pillar.topY + pillar.currentDropY
          val sinkStep = 2.4f
          pillar.currentDropY = (pillar.currentDropY + sinkStep).coerceAtMost(pillar.sinkDistance)
          val newTop = pillar.topY + pillar.currentDropY
          // Carry player smoothly if standing atop the sinking pillar
          if (player.isGrounded &&
              player.x + player.collisionWidth > pillar.x + 2f &&
              player.x < pillar.x + pillar.width - 2f &&
              kotlin.math.abs((player.y + player.collisionHeight) - oldTop) <= 8f) {
            player.y = newTop - player.collisionHeight
          }
          if (pillar.currentDropY >= pillar.sinkDistance) {
            pillar.phase = TrapPhase.COOLDOWN
          }
        }
        TrapPhase.COOLDOWN -> Unit
      }
    }
  }

  private fun updateWaterCurrents() {
    val cx = player.x + player.collisionWidth / 2f
    val cy = player.y + player.collisionHeight / 2f
    for (zone in waterCurrents) {
      if (cx in zone.x..(zone.x + zone.width) && cy in zone.y..(zone.y + zone.height)) {
        val maxWaterSpeed = MAX_RUN_SPEED * 1.32f
        val targetVx = (player.vx + zone.flowVx * 0.22f).coerceIn(-maxWaterSpeed, maxWaterSpeed)
        player.vx = targetVx
        if (zone.flowVy != 0f) {
          player.vy += zone.flowVy * 0.1f
        }
        if (player.stateTimerFrames % 6 == 0) {
          spawnSparkles(cx, player.y + player.collisionHeight - 4f, 0xFF38BDF8.toInt(), 2)
        }
      }
    }
  }

  private fun updateSoundSwitches() {
    val pCx = player.x + player.collisionWidth / 2f
    val pCy = player.y + player.collisionHeight / 2f

    // Track active duration timers first
    for (sw in soundSwitches) {
      if (sw.pulseAnimFrames > 0) sw.pulseAnimFrames--
      if (sw.isActivated && sw.remainingFrames > 0) {
        sw.remainingFrames--
        if (sw.remainingFrames <= 0) {
          sw.isActivated = false
          syncSoundGroupState(sw.groupId)
        }
      }
    }

    // Trigger when Aarav touches chime or jumps/stomps within resonance radius
    for (sw in soundSwitches) {
      val dx = pCx - sw.x
      val dy = pCy - sw.y
      val activeRadius = if (player.vy < -5f) sw.radius + 42f else sw.radius + 18f
      if (dx * dx + dy * dy <= activeRadius * activeRadius) {
        val shouldTrigger = !sw.isActivated || (sw.durationFrames > 0 && sw.remainingFrames < sw.durationFrames - 30)
        if (shouldTrigger) {
          val wasAlreadyActive = sw.isActivated
          sw.isActivated = true
          sw.remainingFrames = sw.durationFrames
          sw.pulseAnimFrames = 36
          syncSoundGroupState(sw.groupId)
          if (!wasAlreadyActive) {
            audioEngine.playEchoSwitch()
            spawnSparkles(sw.x, sw.y, 0xFF2DD4BF.toInt(), 16)
            addComboScore(150, sw.x, sw.y - 18f, "ECHO LINKED!", 0xFF2DD4BF.toInt())
            postNotification(
                eventKey = "ECHO_SW_${sw.id}",
                title = "Echo Chime",
                message = if (sw.durationFrames > 0) "Chime active — cross before it fades!" else "Resonant gate unsealed!",
                category = NotificationCategory.SECRET_DISCOVERY,
                priority = NotificationPriority.DISCOVERY,
                visualKind = NotificationVisualKind.DISCOVERY
            )
          }
        }
      }
    }
  }

  private fun syncSoundGroupState(groupId: Int) {
    val anyActive = soundSwitches.any { it.groupId == groupId && it.isActivated }
    for (tile in tiles) {
      if (tile.groupId != groupId) continue
      when (tile.type) {
        TileType.ECHO_BRIDGE -> {
          tile.isSolid = anyActive
          tile.isVisible = true
          tile.isTriggered = anyActive
        }
        TileType.SOUND_GATE -> {
          tile.isSolid = !anyActive
          tile.isVisible = !anyActive
          tile.isTriggered = anyActive
        }
        else -> Unit
      }
    }
  }

  private fun updateFalsePathWalls() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight
    for (tile in tiles) {
      if (tile.type != TileType.FALSE_PATH_WALL || tile.isTriggered) continue
      if (pRight > tile.x + 4f && pLeft < tile.x + tile.width - 4f &&
          pBottom > tile.y + 4f && pTop < tile.y + tile.height - 4f) {
        tile.isTriggered = true
        audioEngine.playResonanceHum()
        spawnSparkles(tile.x + tile.width / 2f, tile.y + tile.height / 2f, 0xFF67E8F9.toInt(), 12)
        spawnPopup(tile.x + 24f, tile.y - 8f, "FALSE WALL!", 0xFF67E8F9.toInt())
      }
    }
  }

  private fun updateLightningHazards() {
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight

    for (hz in lightningHazards) {
      hz.currentFrame = (hz.currentFrame + 1) % hz.cycleFrames
      val warningStart = (hz.cycleFrames - hz.warningFrames - hz.strikeFrames).coerceAtLeast(1)
      val strikeStart = (hz.cycleFrames - hz.strikeFrames).coerceAtLeast(warningStart + 1)
      val prevPhase = hz.phase
      hz.phase = when {
        hz.currentFrame < warningStart -> TrapPhase.ARMED
        hz.currentFrame < strikeStart -> TrapPhase.WARNING
        else -> TrapPhase.ACTIVE
      }
      if (hz.phase == TrapPhase.WARNING && prevPhase != TrapPhase.WARNING) {
        if (kotlin.math.abs(player.x - hz.x) < 420f) {
          postNotification(
              eventKey = "HAZARD_LIGHTNING_${hz.id}",
              title = "Storm Surge",
              message = "Lightning incoming — clear the arc!",
              category = NotificationCategory.EVENT_WARNING,
              priority = NotificationPriority.CRITICAL,
              visualKind = NotificationVisualKind.LIGHTNING_HAZARD,
              worldFocusX = hz.x,
              worldFocusY = hz.groundY - 50f
          )
        }
      }
      if (hz.phase == TrapPhase.ACTIVE && prevPhase != TrapPhase.ACTIVE) {
        if (kotlin.math.abs(player.x - hz.x) < 760f) {
          audioEngine.playLightningStrike()
          triggerCameraShake(5, 2.8f)
          lightningFlashFrames = 6
        }
        spawnSparkles(hz.x, hz.groundY - 6f, 0xFFBAE6FD.toInt(), 10)
      }
      if (hz.phase == TrapPhase.ACTIVE) {
        val left = hz.x - hz.width * 0.42f
        val right = hz.x + hz.width * 0.42f
        if (pRight > left && pLeft < right && pBottom > hz.topY && pTop < hz.groundY) {
          handlePlayerDamage()
        }
      }
    }
  }

  private fun updateRelics() {
    val cx = player.x + player.collisionWidth / 2f
    val cy = player.y + player.collisionHeight / 2f
    for (relic in relics) {
      relic.animTimer += 0.07f
      if (relic.isCollected) continue
      val dx = cx - relic.x
      val dy = cy - relic.y
      val pickupDist = relic.radius + player.collisionWidth / 2f + 4f
      if (dx * dx + dy * dy <= pickupDist * pickupDist) {
        relic.isCollected = true
        discoveredRelics.add(relic.id)
        onRelicDiscovered(relic)
        audioEngine.playResonanceHum()
        spawnSparkles(relic.x, relic.y, 0xFF38BDF8.toInt(), 18)
        addComboScore(350, relic.x, relic.y - 14f, "RELIC FOUND!", 0xFF38BDF8.toInt())
        postNotification(
            eventKey = "RELIC_${relic.id}",
            title = "Relic Uncovered",
            message = "${relic.title} uncovered!",
            category = NotificationCategory.SECRET_DISCOVERY,
            priority = NotificationPriority.DISCOVERY,
            visualKind = NotificationVisualKind.DISCOVERY
        )
      }
    }
  }

  private fun updateExitGate() {
    val gateTile = tiles.firstOrNull { it.type == TileType.SUN_GATE } ?: return
    val playerRight = player.x + player.collisionWidth
    val playerLeft = player.x
    val playerBottom = player.y + player.collisionHeight
    val playerTop = player.y

    if (playerRight > gateTile.x + 20f && playerLeft < gateTile.x + gateTile.width - 20f &&
        playerBottom > gateTile.y && playerTop < gateTile.y + gateTile.height) {
      if (guardianCfg?.isTempleBoss == true && stoneGuardian.phase != GuardianPhase.DEFEATED) {
        postNotification(
            eventKey = "BOSS_SEAL_GATE",
            title = "Sanctum Sealed",
            message = "Defeat Vajra-Surya to unseal gate!",
            category = NotificationCategory.EVENT_WARNING,
            priority = NotificationPriority.CRITICAL,
            visualKind = NotificationVisualKind.BOSS_WARNING,
            worldFocusX = gateTile.x,
            worldFocusY = gateTile.y
        )
        return
      }
      if (!isVictorious) {
        isVictorious = true
        completedChallengeIds.add(activeChallengeId)
        player.state = AaravState.VICTORY
        triggeredEvents.add(GameplayEventId.E7_SUN_GATE_AWAKENING)
        val victoryWhisper = if (level.isTrollLevel) {
          when (level.levelId) {
            1 -> "Wait... you actually made it past the runaway door!"
            2 -> "Every safe path lied, yet you read the real trail."
            3 -> "Even a lying exit couldn't outrun Aarav!"
            4 -> "Cracks, blinks, and ceilings conquered — the floor bows to you."
            5 -> "Outsmarted every troll in World 1 — you survived Almost There!"
            else -> "The Sun Gate awakens — trial cleared!"
          }
        } else {
          when (level.levelId) {
            5 -> "The Last Light ignites — World 1 is free!"
            else -> "The Sun Gate awakens — the path ahead opens."
          }
        }
        postNotification(
            eventKey = GameplayEventId.E7_SUN_GATE_AWAKENING.name,
            title = "Sun Gate Awakened",
            message = victoryWhisper,
            category = NotificationCategory.STORY_CLUE,
            priority = NotificationPriority.STORY,
            visualKind = NotificationVisualKind.TRIUMPH
        )
        onWhisperEvent(GameplayEventId.E7_SUN_GATE_AWAKENING, victoryWhisper)
        spawnSparkles(
            gateTile.x + gateTile.width / 2,
            gateTile.y + gateTile.height / 2,
            0xFFFDE047.toInt(),
            20
        )
        audioEngine.playVictory()
        val collectedCount = coins.count { it.isCollected }
        onLevelCompleted(levelTimerMs, collectedCount, coins.size)
      }
    }
  }

  private fun handlePlayerDamage(causeKey: String? = null) {
    if (level.isTrollLevel) {
      startDeathSequence("OUCH!", causeKey)
      return
    }
    if (player.invulnerableFrames > 0) return
    player.health--
    player.invulnerableFrames = 60 // 1 second invulnerability
    comboMultiplier = 1
    comboTimerFrames = 0
    audioEngine.playDamage()
    triggerCameraShake(8, 4.5f)

    if (player.health <= 0) {
      handlePlayerDeath(causeKey)
    } else {
      player.state = AaravState.DAMAGE
      player.vy = -7.0f
      player.vx = if (player.facingRight) -3.5f else 3.5f
    }
  }

  fun startDeathSequence(quip: String = "NOPE!", causeKey: String? = null) = synchronized(worldStateLock) {
    if (isDying || isVictorious || isGameOver) return@synchronized
    isDying = true
    deathAnimFrames = deathAnimDurationFrames.coerceAtLeast(1)
    pendingDeathCauseKey = causeKey
    player.health = 0
    player.state = AaravState.DEATH
    player.stateTimerFrames = 0
    player.vy = -7.5f
    player.vx = if (player.facingRight) -2.2f else 2.2f
    player.isGrounded = false
    comboMultiplier = 1
    comboTimerFrames = 0
    audioEngine.playFastTrollDeath()
    triggerCameraShake(8, 5.0f)
    spawnDeathStarburst(player.x + player.collisionWidth / 2f, player.y + player.collisionHeight / 2f)
    spawnPopup(player.x + player.collisionWidth / 2f, player.y - 14f, quip, 0xFFFB7185.toInt())
    publishHudSnapshot()
  }

  private fun completeDeathRespawn() {
    isDying = false
    deathAnimFrames = 0
    val cause = pendingDeathCauseKey
    pendingDeathCauseKey = null
    handlePlayerDeathInternal(playDeathSound = false, causeKey = cause)
  }

  fun handlePlayerDeath(causeKey: String? = null) = synchronized(worldStateLock) {
    if (isDying || isVictorious || isGameOver) return@synchronized
    isDying = false
    deathAnimFrames = 0
    pendingDeathCauseKey = null
    handlePlayerDeathInternal(playDeathSound = true, causeKey = causeKey)
  }

  private fun handlePlayerDeathInternal(playDeathSound: Boolean, causeKey: String? = null) {
    lastDeathCauseKey = causeKey
    deathCount++
    cumulativeDeathCount++
    onPlayerDied(deathCount)
    onChallengeDeathAttributed(activeChallengeId, deathCount, cumulativeDeathCount, causeKey)
    onChallengeDeathUpdated(activeChallengeId, deathCount, cumulativeDeathCount)

    // Retry boundary adaptive config transition (occurs strictly AFTER attempt death completes, never mid-attempt)
    if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
      val transition = onAttemptRetryConfigRequested?.invoke(activeChallengeId, deathCount, causeKey)
      if (transition != null) {
        lockedChallengeConfig = transition.lockedConfig
        challengeAttemptToken = transition.attemptToken
      }
    }

    if (playDeathSound) {
      audioEngine.playDeath()
      triggerCameraShake(10, 5.5f)
      spawnDeathStarburst(player.x + player.collisionWidth / 2, player.y + player.collisionHeight / 2)
    }

    val snap = checkpointSnapshot
    if (snap != null) {
      // Immediately clear active & queued notifications so pre-death messages never reappear after rollback
      notificationController.clearActiveAndQueue(clearShownKeys = false)

      // Deterministic rollback to Checkpoint (never clears held movement inputs,
      // but arms jump edge-guard so holding Jump through death cannot auto-trigger a frame-1 jump)
      player.x = snap.playerX
      player.y = snap.playerY
      player.prevX = snap.playerX
      player.prevY = snap.playerY
      player.vx = 0f
      player.vy = 0f
      player.isGrounded = true
      player.facingRight = snap.playerFacingRight
      player.health = snap.playerHealth
      player.invulnerableFrames = if (level.isTrollLevel) 0 else 40
      player.coyoteTimer = 0
      player.jumpBufferTimer = 0
      player.hasJumpCutApplied = false
      player.state = AaravState.IDLE
      player.stateTimerFrames = 0
      landingSquashFrames = 0
      isStandingOnSlippery = false
      lightningFlashFrames = 0
      cameraShakeFrames = 0
      cameraShakeMagnitude = 0f
      previousInputJump = inputJump
      levelTimerMs = snap.challengeTimerMs
      challengeTimerMs = snap.challengeTimerMs
      score = snap.scoreAtCheckpoint
      comboMultiplier = 1
      comboTimerFrames = 0

      // Restore tiles
      val snapMap = snap.tileSnapshots.associateBy { it.tileId }
      for (tile in tiles) {
        snapMap[tile.id]?.let {
          tile.isVisible = it.isVisible
          tile.isSolid = it.isSolid
          tile.isTriggered = it.isTriggered
          tile.crumbleTimerFrames = it.crumbleTimerFrames
          tile.isCrumbling = it.isCrumbling
        }
      }

      // Restore moving platforms, orbiting platforms, pendulums & lightning hazards
      val mpMap = snap.movingPlatformSnapshots.associateBy { it.platformId }
      for (mp in movingPlatforms) {
        mpMap[mp.id]?.let {
          mp.x = it.x
          mp.y = it.y
          mp.vx = it.vx
          mp.currentFrame = it.currentFrame
        }
      }
      val opMap = snap.orbitingPlatformSnapshots.associateBy { it.platformId }
      for (i in orbitingPlatforms.indices) {
        val op = orbitingPlatforms[i]
        val opSnap = opMap[op.id]
        if (opSnap != null) {
          op.x = opSnap.x
          op.y = opSnap.y
          op.vx = opSnap.vx
          op.vy = opSnap.vy
          op.currentFrame = opSnap.currentFrame
        } else {
          val orig = level.orbitingPlatforms[i]
          op.x = orig.x
          op.y = orig.y
          op.vx = 0f
          op.vy = 0f
          op.currentFrame = 0
        }
      }
      val pendMap = snap.pendulumSnapshots.associateBy { it.pendulumId }
      for (pend in pendulums) {
        val pendSnap = pendMap[pend.id]
        if (pendSnap != null) {
          pend.currentFrame = pendSnap.currentFrame
          pend.bobX = pendSnap.bobX
          pend.bobY = pendSnap.bobY
        } else {
          pend.currentFrame = 0
          pend.bobX = pend.anchorX
          pend.bobY = pend.anchorY + pend.length
        }
      }

      // Restore critters
      val critMap = snap.critterSnapshots.associateBy { it.critterId }
      for (c in critters) {
        critMap[c.id]?.let {
          c.x = it.x
          c.y = c.baseY
          c.vy = 0f
          c.vx = it.vx
          c.stunFrames = it.stunFrames
          c.alertFrames = 0
          c.lungeFrames = 0
        }
      }

      // Restore dynamic hazards
      val rollbackNotificationKeys = mutableListOf<String>()
      for (hz in fallingHazards) {
        if (!snap.triggeredFallingHazardIds.contains(hz.id)) {
          hz.y = hz.startY
          hz.vy = 0f
          hz.warningFrames = 0
          hz.isWarning = false
          hz.isFalling = false
          hz.hasTriggered = false
          rollbackNotificationKeys.add("HAZARD_FALL_${hz.id}")
        }
      }
      val lhMap = snap.lightningSnapshots.associateBy { it.hazardId }
      for (i in lightningHazards.indices) {
        val lh = lightningHazards[i]
        val lhSnap = lhMap[lh.id]
        if (lhSnap != null) {
          lh.currentFrame = lhSnap.currentFrame
          lh.phase = lhSnap.phase
        } else {
          val orig = level.lightningHazards[i]
          lh.currentFrame = orig.phaseOffsetFrames
          lh.phase = TrapPhase.ARMED
        }
        rollbackNotificationKeys.add("HAZARD_LIGHTNING_${lh.id}")
      }
      for (trap in ambushTraps) {
        trap.phase = TrapPhase.ARMED
        trap.phaseTimerFrames = 0
        trap.currentHeight = 0f
      }
      for (drum in bounceDrums) {
        drum.squashFrames = 0
      }

      // Restore coins & diyas: items collected post-checkpoint roll back
      for (coin in coins) {
        coin.isCollected = snap.coinsCollectedAtCheckpoint.contains(coin.id)
      }
      for (diya in diyas) {
        diya.isLit = snap.litDiyasAtCheckpoint.contains(diya.id)
      }
      if (!diyas.all { it.isLit }) {
        rollbackNotificationKeys.add("INFO_ALL_DIYAS_LIT")
      }

      // Restore sound switches, collapsing pillars & relics cleanly
      val swMap = snap.switchSnapshots.associateBy { it.switchId }
      val switchGroups = mutableSetOf<Int>()
      for (sw in soundSwitches) {
        swMap[sw.id]?.let {
          sw.isActivated = it.isActivated
          sw.remainingFrames = it.remainingFrames
          sw.pulseAnimFrames = 0
        }
        switchGroups.add(sw.groupId)
        if (!sw.isActivated) {
          rollbackNotificationKeys.add("ECHO_SW_${sw.id}")
        }
      }
      for (groupId in switchGroups) {
        syncSoundGroupState(groupId)
      }
      val pilMap = snap.pillarSnapshots.associateBy { it.pillarId }
      for (pil in collapsingPillars) {
        pilMap[pil.id]?.let {
          pil.currentDropY = it.currentDropY
          pil.phase = it.phase
          pil.timerFrames = it.timerFrames
        }
      }
      for (relic in relics) {
        relic.isCollected = snap.collectedRelicsAtCheckpoint.contains(relic.id)
      }

      // Clear transient particles & popups from death sequence
      for (p in particles) p.active = false
      nextParticleIndex = 0
      for (p in popups) p.active = false
      nextPopupIndex = 0

      // Restore temporary puzzle states deterministically
      echoPuzzleNode?.isActivated = snap.echoSealActivatedAtCheckpoint
      if (!snap.echoSealActivatedAtCheckpoint) {
        triggeredEvents.remove(GameplayEventId.E5_CANOPY_ECHO_SEAL)
        rollbackNotificationKeys.add(GameplayEventId.E5_CANOPY_ECHO_SEAL.name)
      }
      notificationController.invalidateKeys(rollbackNotificationKeys)
      resonanceStone?.isCollected = snap.resonanceStoneCollectedAtCheckpoint
      watcherAppearance.isActive = false
      watcherAppearance.remainingFrames = 0

      // Restore Set Pieces, Exit Gate & Troll Traps deterministically (pre-checkpoint traps stay solved)
      if (snap.trollTrapSnapshots.isNotEmpty()) {
        trapSystem.restoreSnapshots(trollTraps, snap.trollTrapSnapshots)
      } else {
        trapSystem.resetAll(trollTraps)
      }
      lockedChallengeConfig?.let {
        TrapFairnessPolicy.applyTierTuningInPlace(trollTraps, it.selectedAdaptiveTier)
      }
      val restoredExitX = snap.exitXAtCheckpoint ?: level.exitX
      val restoredExitY = snap.exitYAtCheckpoint ?: level.exitY
      currentExitX = restoredExitX
      currentExitY = restoredExitY
      for (tile in tiles) {
        if (tile.type == TileType.SUN_GATE) {
          tile.x = restoredExitX
          tile.y = restoredExitY
        }
      }
      resetSetPiecesForRollback(
          treeResolved = snap.treeResolvedAtCheckpoint,
          stalkerRepelled = snap.stalkerRepelledAtCheckpoint,
          guardianDefeated = snap.guardianDefeatedAtCheckpoint
      )
      pendingCameraSnap.set(true)
      // Note: Permanent clues (discoveredClues), discoveredRelics, completedChallengeIds, cumulative counters, and isResonanceStoneDiscovered NEVER roll back.
    } else {
      // Reset to challenge start (never clears held movement inputs!)
      resetCurrentChallengeInternal(clearInputs = false, invulnFrames = if (level.isTrollLevel) 0 else 40)
    }
    publishHudSnapshot()
  }

  private fun resetAllTrollTraps() {
    trapSystem.resetAll(trollTraps)
    lockedChallengeConfig?.let {
      TrapFairnessPolicy.applyTierTuningInPlace(trollTraps, it.selectedAdaptiveTier)
    }
    currentExitX = level.exitX
    currentExitY = level.exitY
    for (tile in tiles) {
      if (tile.type == TileType.SUN_GATE) {
        tile.x = level.exitX
        tile.y = level.exitY
      }
    }
  }

  private fun updateTrollTraps() {
    trapSystem.updateAll(trollTraps, player, trapCallbacks)
  }

  /**
   * Deterministic challenge retry/restart entry point.
   * Resets all current-challenge mutable state while preserving permanent discoveries,
   * previously completed challenges, and cumulative run counters.
   */
  fun resetCurrentChallenge(clearInputs: Boolean = true) = synchronized(worldStateLock) {
    resetCurrentChallengeInternal(clearInputs = clearInputs, invulnFrames = 0)
    publishHudSnapshot()
  }

  /**
   * Full deterministic level restart (from Victory, Pause, or explicit restart).
   * Preserves permanent story clues and Resonance Stone discovery while resetting all run state.
   */
  fun resetLevel() = resetCurrentChallenge(clearInputs = true)

  private fun resetCurrentChallengeInternal(clearInputs: Boolean, invulnFrames: Int) {
    player.x = level.spawnX
    player.y = level.spawnY
    player.prevX = level.spawnX
    player.prevY = level.spawnY
    player.vx = 0f
    player.vy = 0f
    player.isGrounded = false
    player.facingRight = true
    player.health = level.difficulty.startingHealth
    player.maxHealth = level.difficulty.startingHealth
    player.invulnerableFrames = invulnFrames
    player.coyoteTimer = 0
    player.jumpBufferTimer = 0
    player.hasJumpCutApplied = false
    player.state = AaravState.IDLE
    player.stateTimerFrames = 0
    landingSquashFrames = 0
    isStandingOnSlippery = false
    lightningFlashFrames = 0

    // Always reset attempt timer, camera shake, and request immediate camera snap
    levelTimerMs = 0L
    challengeTimerMs = 0L
    cameraShakeFrames = 0
    cameraShakeMagnitude = 0f
    pendingCameraSnap.set(true)

    if (clearInputs) {
      inputLeft = false
      inputRight = false
      inputJump = false
      previousInputJump = false
      checkpointSnapshot = null
      hasCheckpointActivated = false
      isPaused = false
      isGameOver = false
      isVictorious = false
    } else {
      // Guard against accidental frame-1 jump if Jump was held through death animation
      previousInputJump = inputJump
    }

    score = 0
    comboMultiplier = 1
    comboTimerFrames = 0

    for (i in tiles.indices) {
      val orig = level.tiles[i]
      val tile = tiles[i]
      tile.x = orig.x
      tile.y = orig.y
      tile.isVisible = true
      tile.isSolid = (tile.type != TileType.CHECKPOINT_BELL &&
          tile.type != TileType.SUN_GATE &&
          tile.type != TileType.ECHO_BRIDGE &&
          tile.type != TileType.FALSE_PATH_WALL)
      tile.isTriggered = false
      tile.isCrumbling = false
      tile.crumbleTimerFrames = 0
    }
    resetAllTrollTraps()
    isDying = false
    deathAnimFrames = 0
    for (i in movingPlatforms.indices) {
      val orig = level.movingPlatforms[i]
      val mp = movingPlatforms[i]
      mp.x = orig.x
      mp.y = orig.y
      mp.vx = 0f
      mp.vy = 0f
      mp.currentFrame = 0
    }
    for (i in orbitingPlatforms.indices) {
      val orig = level.orbitingPlatforms[i]
      val op = orbitingPlatforms[i]
      op.x = orig.x
      op.y = orig.y
      op.vx = 0f
      op.vy = 0f
      op.currentFrame = 0
    }
    for (pil in collapsingPillars) {
      pil.currentDropY = 0f
      pil.phase = TrapPhase.ARMED
      pil.timerFrames = 0
    }
    for (sw in soundSwitches) {
      sw.isActivated = false
      sw.remainingFrames = 0
      sw.pulseAnimFrames = 0
    }
    for (i in lightningHazards.indices) {
      val orig = level.lightningHazards[i]
      val hz = lightningHazards[i]
      hz.currentFrame = orig.phaseOffsetFrames
      hz.phase = TrapPhase.ARMED
    }
    for (drum in bounceDrums) {
      drum.squashFrames = 0
    }
    for (i in critters.indices) {
      val orig = level.critters[i]
      val c = critters[i]
      c.x = orig.x
      c.y = c.baseY
      c.vx = orig.vx
      c.vy = 0f
      c.stunFrames = 0
      c.alertFrames = 0
      c.lungeFrames = 0
      c.hopTimer = 0
      c.walkTick = 0
    }
    for (i in pendulums.indices) {
      pendulums[i].currentFrame = 0
      pendulums[i].bobX = pendulums[i].anchorX
      pendulums[i].bobY = pendulums[i].anchorY + pendulums[i].length
    }
    for (hz in fallingHazards) {
      hz.y = hz.startY
      hz.vy = 0f
      hz.warningFrames = 0
      hz.isWarning = false
      hz.isFalling = false
      hz.hasTriggered = false
    }
    for (trap in ambushTraps) {
      trap.phase = TrapPhase.ARMED
      trap.phaseTimerFrames = 0
      trap.currentHeight = 0f
    }
    for (coin in coins) {
      coin.isCollected = false
      coin.animTimer = 0f
    }
    for (diya in diyas) {
      diya.isLit = false
    }
    for (relic in relics) {
      relic.isCollected = false
      relic.animTimer = 0f
    }
    for (p in particles) p.active = false
    nextParticleIndex = 0
    for (p in popups) p.active = false
    nextPopupIndex = 0

    echoPuzzleNode?.isActivated = false
    resonanceStone?.isCollected = false
    resonanceStone?.animTimer = 0f
    triggeredEvents.clear()
    notificationController.clearActiveAndQueue(clearShownKeys = true)
    echoingBridgeCueCount = 0
    watcherAppearance.isActive = false
    watcherAppearance.remainingFrames = 0
    resetSetPiecesForRollback(
        treeResolved = false,
        stalkerRepelled = false,
        guardianDefeated = false
    )
    // Permanent story clues (discoveredClues), discoveredRelics, completedChallengeIds, cumulative counters, and isResonanceStoneDiscovered remain preserved.
  }

  private fun resetSetPiecesForRollback(
      treeResolved: Boolean,
      stalkerRepelled: Boolean,
      guardianDefeated: Boolean
  ) {
    awakeningTree.phase = if (treeResolved) TreePhase.RESOLVED else TreePhase.IDLE
    awakeningTree.timerFrames = 0
    for (root in awakeningTree.roots) {
      root.currentHeight = if (treeResolved) root.maxHeight else 0f
    }
    for (acorn in awakeningTree.acorns) {
      acorn.active = false
    }
    val rollbackKeys = mutableListOf<String>()
    if (!treeResolved) {
      rollbackKeys.add("SP_TREE_WARN")
      rollbackKeys.add("SP_TREE_CLEAR")
    }

    forestStalker.phase = if (stalkerRepelled) StalkerPhase.REPELLED else StalkerPhase.IDLE
    forestStalker.x = 3000f
    forestStalker.y = 12 * 48f - forestStalker.height
    forestStalker.vx = 0f
    forestStalker.phaseTimerFrames = 0
    forestStalker.stunPauseFrames = 0
    if (!stalkerRepelled) {
      rollbackKeys.add("SP_STALKER_WARN")
    }

    stoneGuardian.phase = if (guardianDefeated) GuardianPhase.DEFEATED else GuardianPhase.SLEEPING
    stoneGuardian.phaseTimerFrames = 0
    stoneGuardian.windupFrames = 0
    stoneGuardian.crystalInvulnFrames = 0
    stoneGuardian.hitsRemaining = if (guardianDefeated) 0 else stoneGuardian.maxHits
    stoneGuardian.attackCooldownFrames = if (level.difficulty == DifficultyTier.NIGHTMARE) 42 else 65
    for (sw in stoneGuardian.shockwaves) {
      sw.active = false
      sw.hasReversed = false
    }
    if (!guardianDefeated) {
      rollbackKeys.add("SP_GUARDIAN_WARN")
      rollbackKeys.add("SP_GUARDIAN_PHASE2")
      for (h in 1..5) rollbackKeys.add("SP_GUARDIAN_PHASE_$h")
      rollbackKeys.add("SP_GUARDIAN_CLEAR")
      rollbackKeys.add("SP_SLAB_CRACK")
      rollbackKeys.add("BOSS_SEAL_GATE")
    }
    notificationController.invalidateKeys(rollbackKeys)
  }

  private fun spawnPopup(x: Float, y: Float, text: String, color: Int) {
    val p = popups[nextPopupIndex % popups.size]
    nextPopupIndex++
    p.active = true
    p.x = x
    p.y = y
    p.vy = -1.1f
    p.text = text
    p.color = color
    p.life = 0f
    p.maxLife = 0.9f
  }

  private fun updatePopups() {
    for (p in popups) {
      if (!p.active) continue
      p.life += 0.016f
      if (p.life >= p.maxLife) {
        p.active = false
        continue
      }
      p.y += p.vy
      p.vy *= 0.96f
    }
  }

  private fun spawnDustTrickle(x: Float, y: Float) {
    for (i in 0 until 2) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x + (Math.random().toFloat() - 0.5f) * 28f
      p.y = y
      p.vx = (Math.random().toFloat() - 0.5f) * 0.6f
      p.vy = 0.7f + Math.random().toFloat() * 0.9f
      p.life = 0f
      p.maxLife = 0.42f
      p.color = 0xCCA88B68.toInt()
      p.size = 3.5f + Math.random().toFloat() * 2.5f
    }
  }

  private fun spawnDustPuff(x: Float, y: Float) {
    for (i in 0 until 6) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x + (Math.random().toFloat() - 0.5f) * 16f
      p.y = y - 4f
      p.vx = (Math.random().toFloat() - 0.5f) * 2.5f
      p.vy = -Math.random().toFloat() * 1.5f
      p.life = 0f
      p.maxLife = 0.35f
      p.color = 0xAA8B7355.toInt()
      p.size = 5f + Math.random().toFloat() * 4f
    }
  }

  private fun spawnSparkles(x: Float, y: Float, color: Int, count: Int) {
    for (i in 0 until count) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x
      p.y = y
      val angle = (Math.random() * 2.0 * PI).toFloat()
      val speed = 1.0f + Math.random().toFloat() * 3.5f
      p.vx = (Math.cos(angle.toDouble()) * speed).toFloat()
      p.vy = (Math.sin(angle.toDouble()) * speed).toFloat()
      p.life = 0f
      p.maxLife = 0.5f + Math.random().toFloat() * 0.3f
      p.color = color
      p.size = 4f + Math.random().toFloat() * 3f
    }
  }

  private fun spawnDeathStarburst(x: Float, y: Float) {
    for (i in 0 until 24) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x
      p.y = y
      val angle = (i.toDouble() / 24.0 * 2.0 * PI).toFloat()
      val speed = 2.5f + Math.random().toFloat() * 4.0f
      p.vx = (Math.cos(angle.toDouble()) * speed).toFloat()
      p.vy = (Math.sin(angle.toDouble()) * speed).toFloat()
      p.life = 0f
      p.maxLife = 0.65f
      p.color = if (i % 2 == 0) 0xFFF59E0B.toInt() else 0xFF0D7377.toInt()
      p.size = 6f + Math.random().toFloat() * 4f
    }
  }

  private fun updateParticles() {
    for (p in particles) {
      if (!p.active) continue
      p.life += 0.016f
      if (p.life >= p.maxLife) {
        p.active = false
        continue
      }
      p.x += p.vx
      p.y += p.vy
      p.vy += 0.08f // particle gravity
    }
  }
}
