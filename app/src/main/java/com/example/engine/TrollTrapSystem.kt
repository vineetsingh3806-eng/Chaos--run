package com.example.engine

import com.example.data.Player
import com.example.data.TrapSnapshot
import com.example.data.TrapTriggerType
import com.example.data.TrollTrap
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

class PendingTrapChainEvent(
    var targetTrapId: Int = -1,
    var remainingDelayFrames: Int = 0,
    var active: Boolean = false
)

/**
 * Side-effect callbacks emitted by [TrollTrapSystem] so trap mechanics remain
 * cleanly decoupled from player movement physics, audio synthesis, particles, and level gates.
 */
interface TrollTrapCallbacks {
  fun killPlayer(quip: String)
  fun killPlayer(quip: String, causeKey: String?) = killPlayer(quip)
  fun relocateExitGate(destX: Float, destY: Float)
  fun hideLinkedTile(tileId: Int)
  fun applyMirrorCurse(durationFrames: Int) {}
  fun applyWindForce(forceX: Float) {}
  fun launchPlayer(impulseVy: Float) {}
  fun playTrapSnap()
  fun playCrumble()
  fun playDisappear()
  fun playCeilingSlam()
  fun playDoorEscape()
  fun playFakeDoorPoof()
  fun playMechanismShift()
  fun playTrapWarning() {}
  fun triggerImpactHaptic() {}
  fun spawnDustTrickle(x: Float, y: Float)
  fun spawnDustPuff(x: Float, y: Float)
  fun spawnSparkles(x: Float, y: Float, color: Int, count: Int)
  fun spawnStarburst(x: Float, y: Float)
  fun spawnPopup(x: Float, y: Float, text: String, color: Int)
  fun triggerCameraShake(frames: Int, magnitude: Float)
}

/**
 * Reusable, deterministic Trap Architecture for CHAOS RUN 5.
 *
 * Manages activation conditions, telegraph/warning phases, smooth per-frame
 * visual animations, solid-surface & lethal hitbox resolution, and zero-leak resets
 * across all 29 [TrollTrapType] mechanics and 7 [TrapTriggerType] behaviours
 * with zero per-frame heap allocations.
 */
class TrollTrapSystem {

  companion object {
    private const val SPIKE_EXTEND_STEP = 8.5f
    private const val FAKE_PLATFORM_SPIKE_TARGET = 24f
    private const val CRUMBLE_DROP_DURATION_FRAMES = 10
    private const val MAX_CHAIN_CAPACITY = 64
    private const val GUST_TELEGRAPH_FRAMES = 18
  }

  // Pre-allocated zero-allocation chain event pool & deduplication tracking
  private val chainPool = Array(MAX_CHAIN_CAPACITY) { PendingTrapChainEvent() }
  private val readyTrapIdsBuffer = IntArray(MAX_CHAIN_CAPACITY)
  private val bfsQueue = IntArray(MAX_CHAIN_CAPACITY)
  private val triggeredCoinIds = HashSet<Int>(32)
  private val dispatchedChainTargetIds = HashSet<Int>(64)

  val pendingChainEventCount: Int
    get() {
      var count = 0
      for (i in chainPool.indices) {
        if (chainPool[i].active) count++
      }
      return count
    }

  fun enqueueChainEvent(targetTrapId: Int, delayFrames: Int = 0) {
    val safeDelay = delayFrames.coerceAtLeast(0)
    // Avoid duplicating an already-pending event for the exact same trap ID
    for (i in chainPool.indices) {
      val slot = chainPool[i]
      if (slot.active && slot.targetTrapId == targetTrapId) {
        if (safeDelay < slot.remainingDelayFrames) {
          slot.remainingDelayFrames = safeDelay
        }
        return
      }
    }
    for (i in chainPool.indices) {
      val slot = chainPool[i]
      if (!slot.active) {
        slot.targetTrapId = targetTrapId
        slot.remainingDelayFrames = safeDelay
        slot.active = true
        return
      }
    }
  }

  fun clearPendingChainEvents() {
    for (i in chainPool.indices) {
      chainPool[i].active = false
      chainPool[i].targetTrapId = -1
      chainPool[i].remainingDelayFrames = 0
    }
    dispatchedChainTargetIds.clear()
  }

  fun onCoinCollected(
      coinId: Int,
      traps: List<TrollTrap>,
      callbacks: TrollTrapCallbacks
  ) {
    if (!triggeredCoinIds.add(coinId)) return
    for (i in traps.indices) {
      val trap = traps[i]
      if (trap.triggerType == TrapTriggerType.ON_COIN_COLLECTED &&
          trap.state == TrollTrapState.ARMED &&
          (trap.triggerCoinId == null || trap.triggerCoinId == coinId)) {
        if (trap.chainDelayFrames <= 0) {
          triggerTrap(trap, traps, callbacks)
        } else {
          enqueueChainEvent(trap.id, trap.chainDelayFrames)
        }
      }
    }
  }

  fun syncTriggeredCoinsWithSet(collectedCoinIds: Set<Int>) {
    triggeredCoinIds.clear()
    triggeredCoinIds.addAll(collectedCoinIds)
  }

  private fun findTrapById(traps: List<TrollTrap>, id: Int): TrollTrap? {
    for (i in traps.indices) {
      if (traps[i].id == id) return traps[i]
    }
    return null
  }

  private fun tickChainEvents(traps: List<TrollTrap>, callbacks: TrollTrapCallbacks) {
    var readyCount = 0
    for (i in chainPool.indices) {
      val event = chainPool[i]
      if (!event.active) continue
      event.remainingDelayFrames--
      if (event.remainingDelayFrames <= 0) {
        if (readyCount < readyTrapIdsBuffer.size) {
          readyTrapIdsBuffer[readyCount++] = event.targetTrapId
        }
        event.active = false
        event.targetTrapId = -1
        event.remainingDelayFrames = 0
      }
    }
    for (i in 0 until readyCount) {
      val targetId = readyTrapIdsBuffer[i]
      val trap = findTrapById(traps, targetId) ?: continue
      if (trap.state == TrollTrapState.ARMED) {
        triggerTrap(trap, traps, callbacks)
      }
    }
  }

  private fun dispatchTrapActivatedChain(
      initialSource: TrollTrap,
      traps: List<TrollTrap>,
      callbacks: TrollTrapCallbacks
  ) {
    var head = 0
    var tail = 0
    bfsQueue[tail++] = initialSource.id
    dispatchedChainTargetIds.add(initialSource.id)

    while (head < tail) {
      val sourceId = bfsQueue[head++]
      val sourceTrap = findTrapById(traps, sourceId) ?: continue
      for (i in traps.indices) {
        val candidate = traps[i]
        if (candidate.id == sourceId) continue
        val matchesSource = candidate.triggerType == TrapTriggerType.ON_TRAP_ACTIVATED &&
            (candidate.triggerSourceTrapId == sourceId || sourceTrap.linkedTrapId == candidate.id)
        if (!matchesSource) continue
        if (candidate.state != TrollTrapState.ARMED) continue
        if (!dispatchedChainTargetIds.add(candidate.id)) continue

        if (candidate.chainDelayFrames <= 0) {
          val telegraph = candidate.lockedTelegraphDurationFrames
          if (telegraph > 0) {
            enterTelegraphPhase(candidate, callbacks)
          } else {
            activateSingleTrap(candidate, callbacks)
            if (tail < bfsQueue.size) {
              bfsQueue[tail++] = candidate.id
            }
          }
        } else {
          enqueueChainEvent(candidate.id, candidate.chainDelayFrames)
        }
      }
    }
  }

  /**
   * Advances continuous solid trap platforms (YO_YO_PLATFORM, active EVASIVE_PLATFORM)
   * cleanly BEFORE engine vertical/horizontal integration so player riding/carrying has zero jitter.
   */
  fun preStepMovingTraps(traps: List<TrollTrap>) {
    for (i in traps.indices) {
      val trap = traps[i]
      trap.preSteppedThisFrame = false
      when (trap.type) {
        TrollTrapType.YO_YO_PLATFORM -> {
          stepYoYoPlatform(trap)
          trap.preSteppedThisFrame = true
        }
        TrollTrapType.EVASIVE_PLATFORM -> {
          if (trap.state == TrollTrapState.ACTIVE) {
            stepEvasivePlatform(trap)
            trap.preSteppedThisFrame = true
          } else {
            trap.prevX = trap.x
            trap.prevY = trap.y
            trap.vx = 0f
            trap.vy = 0f
          }
        }
        TrollTrapType.SLIDING_FLOOR_SHIFT -> {
          if (trap.state == TrollTrapState.ACTIVE) {
            stepSlidingFloorShift(trap)
            trap.preSteppedThisFrame = true
          } else {
            trap.prevX = trap.x
            trap.prevY = trap.y
            trap.vx = 0f
            trap.vy = 0f
          }
        }
        TrollTrapType.REVERSING_PLATFORM -> {
          stepReversingPlatform(trap)
          trap.preSteppedThisFrame = true
        }
        TrollTrapType.SPLITTING_TWIN_FLOOR -> {
          if (trap.state == TrollTrapState.ACTIVE) {
            stepSplittingTwinFloor(trap)
            trap.preSteppedThisFrame = true
          } else {
            trap.prevX = trap.x
            trap.prevY = trap.y
            trap.vx = 0f
            trap.vy = 0f
          }
        }
        else -> {
          trap.prevX = trap.x
          trap.prevY = trap.y
        }
      }
    }
  }

  /**
   * Resolves horizontal side collisions between Aarav and solid/visible [TrollTrap]s.
   * Returns true if a lethal moving crusher, pincer, or falling block killed Aarav.
   */
  fun resolveHorizontalCollisions(
      traps: List<TrollTrap>,
      player: Player,
      callbacks: TrollTrapCallbacks
  ): Boolean {
    val curLeft = player.x
    val curRight = player.x + player.collisionWidth
    val curTop = player.y
    val curBottom = player.y + player.collisionHeight

    for (i in traps.indices) {
      val trap = traps[i]
      if (!trap.isSolid || !trap.isVisible) continue

      when (trap.type) {
        TrollTrapType.TILTING_PLANK -> {
          // Thin plank bridge pivots smoothly; never blocks horizontal approach with invisible side walls.
          continue
        }
        TrollTrapType.PINCER_WALLS -> {
          val jawWidth = computePincerJawWidth(trap)
          val leftJawRight = trap.x + jawWidth
          val rightJawLeft = trap.x + trap.width - jawWidth

          if (resolveWallBoxHorizontal(trap, trap.x, trap.y, jawWidth, trap.height, player, curLeft, curRight, curTop, curBottom, callbacks)) {
            return true
          }
          if (resolveWallBoxHorizontal(trap, rightJawLeft, trap.y, jawWidth, trap.height, player, curLeft, curRight, curTop, curBottom, callbacks)) {
            return true
          }
          // Check crush only if jaws actually closed inside player box past safe gap edge
          if (trap.killsPlayer && trap.state == TrollTrapState.ACTIVE &&
              jawWidth > 12f &&
              curTop < trap.y + trap.height - 6f && curBottom > trap.y + 6f &&
              ((curLeft < leftJawRight - 4f && curRight > leftJawRight) ||
                  (curRight > rightJawLeft + 4f && curLeft < rightJawLeft))) {
            // If standing cleanly inside central gap between leftJawRight..rightJawLeft, player is safe!
            val centerOverlapLeft = curLeft >= leftJawRight - 2f
            val centerOverlapRight = curRight <= rightJawLeft + 2f
            if (!(centerOverlapLeft && centerOverlapRight) &&
                curRight - curLeft > (rightJawLeft - leftJawRight)) {
              callbacks.killPlayer(trap.quipPopup ?: "PINCHED!", causeKeyFor(trap))
              return true
            }
          }
          continue
        }
        TrollTrapType.SPLIT_BRIDGE -> {
          if (trap.auxProgress >= 0.35f) {
            val wingW = trap.width * 0.28f
            resolveSolidBoxHorizontal(trap.x, trap.y, wingW, trap.height, player)
            resolveSolidBoxHorizontal(trap.x + trap.width - wingW, trap.y, wingW, trap.height, player)
          } else {
            resolveSolidBoxHorizontal(trap.x, trap.y, trap.width, trap.height, player)
          }
          continue
        }
        TrollTrapType.SPLITTING_TWIN_FLOOR -> {
          val wingW = trap.width * 0.48f
          val shift = computeTwinSplitShift(trap)
          resolveSolidBoxHorizontal(trap.x - shift, trap.y, wingW, trap.height, player)
          resolveSolidBoxHorizontal(trap.x + trap.width - wingW + shift, trap.y, wingW, trap.height, player)
          continue
        }
        TrollTrapType.EXIT_SLAM_CAGE -> {
          if (trap.state == TrollTrapState.ACTIVE) {
            val postW = 14f
            resolveSolidBoxHorizontal(trap.x, trap.y, postW, trap.height, player)
            resolveSolidBoxHorizontal(trap.x + trap.width - postW, trap.y, postW, trap.height, player)
          }
          continue
        }
        else -> Unit
      }

      if (curRight > trap.x && curLeft < trap.x + trap.width &&
          curBottom > trap.y + 4f && curTop < trap.y + trap.height - 4f) {
        if ((trap.type == TrollTrapType.MOVING_CRUSH_WALL ||
                trap.type == TrollTrapType.STALKER_CRUSH_WALL ||
                trap.type == TrollTrapType.FALLING_CEILING_BLOCK) &&
            trap.state == TrollTrapState.ACTIVE &&
            trap.killsPlayer) {
          callbacks.killPlayer(trap.quipPopup ?: "BONK!", causeKeyFor(trap))
          return true
        }
        if (player.vx > 0f) {
          player.x = trap.x - player.collisionWidth
          player.vx = 0f
        } else if (player.vx < 0f) {
          player.x = trap.x + trap.width
          player.vx = 0f
        }
      }
    }
    return false
  }

  private fun resolveSolidBoxHorizontal(
      bx: Float,
      by: Float,
      bw: Float,
      bh: Float,
      player: Player
  ) {
    val curLeft = player.x
    val curRight = player.x + player.collisionWidth
    val curTop = player.y
    val curBottom = player.y + player.collisionHeight
    if (curRight > bx && curLeft < bx + bw && curBottom > by + 4f && curTop < by + bh - 4f) {
      if (player.vx > 0f) {
        player.x = bx - player.collisionWidth
        player.vx = 0f
      } else if (player.vx < 0f) {
        player.x = bx + bw
        player.vx = 0f
      }
    }
  }

  private fun resolveWallBoxHorizontal(
      trap: TrollTrap,
      bx: Float,
      by: Float,
      bw: Float,
      bh: Float,
      player: Player,
      curLeft: Float,
      curRight: Float,
      curTop: Float,
      curBottom: Float,
      callbacks: TrollTrapCallbacks
  ): Boolean {
    if (curRight > bx && curLeft < bx + bw &&
        curBottom > by + 4f && curTop < by + bh - 4f) {
      // Push gently toward inner safe corridor unless trapped
      val boxCenter = bx + bw * 0.5f
      val playerCenter = (curLeft + curRight) * 0.5f
      if (playerCenter >= boxCenter) {
        player.x = bx + bw
        if (player.vx < 0f) player.vx = 0f
      } else {
        player.x = bx - player.collisionWidth
        if (player.vx > 0f) player.vx = 0f
      }
      if (trap.killsPlayer && trap.state == TrollTrapState.ACTIVE && trap.auxProgress >= 0.92f &&
          curRight - curLeft > trap.minSafeGapWidth) {
        callbacks.killPlayer(trap.quipPopup ?: "PINCHED!", causeKeyFor(trap))
        return true
      }
    }
    return false
  }

  /**
   * Resolves vertical floor/ceiling collisions against solid/visible [TrollTrap]s
   * and fires [TrapTriggerType.ON_STEP] traps deterministically when landed upon.
   * Returns true if a lethal downward-moving ceiling block crushed Aarav.
   */
  fun resolveVerticalCollisions(
      traps: List<TrollTrap>,
      player: Player,
      previousGrounded: Boolean,
      callbacks: TrollTrapCallbacks,
      onLanded: (surfaceY: Float) -> Unit
  ): Boolean {
    val tLeft = player.x
    val tRight = player.x + player.collisionWidth
    val tTop = player.y
    val tBottom = player.y + player.collisionHeight
    val prevBottom = player.prevY + player.collisionHeight
    val playerCenterX = (tLeft + tRight) * 0.5f

    for (i in traps.indices) {
      val trap = traps[i]
      if (!trap.isSolid || !trap.isVisible) continue

      when (trap.type) {
        TrollTrapType.TILTING_PLANK -> {
          if (tRight <= trap.x + 2f || tLeft >= trap.x + trap.width - 2f) continue
          val surfaceY = computeTiltingPlankSurfaceY(trap, playerCenterX)
          if (player.vy >= 0f &&
              prevBottom <= surfaceY + 12f &&
              tBottom >= surfaceY - 4f &&
              tTop < surfaceY + 6f) {
            player.y = surfaceY - player.collisionHeight
            player.vy = 0f
            player.isGrounded = true
            val offsetRatio = ((playerCenterX - (trap.x + trap.width * 0.5f)) / (trap.width * 0.5f))
                .coerceIn(-1f, 1f)
            val torqueScale = (trap.lockedMoveSpeed / 8f).coerceIn(0.75f, 1.25f)
            trap.angularVelocityDeg = (trap.angularVelocityDeg * 0.86f + offsetRatio * 0.48f * torqueScale)
                .coerceIn(-2.2f, 2.2f)
            // Gentle deterministic slope slide without jitter
            val rad = Math.toRadians(trap.tiltAngleDeg.toDouble())
            player.x += (kotlin.math.sin(rad) * 0.45).toFloat()
            if (trap.state == TrollTrapState.ARMED) {
              triggerTrap(trap, traps, callbacks)
            }
            if (!previousGrounded) {
              onLanded(surfaceY)
            }
          }
          continue
        }

        TrollTrapType.SPLIT_BRIDGE -> {
          if (trap.auxProgress >= 0.35f) {
            val wingW = trap.width * 0.28f
            val onLeftWing = tRight > trap.x + 4f && tLeft < trap.x + wingW - 2f
            val onRightWing = tRight > trap.x + trap.width - wingW + 2f && tLeft < trap.x + trap.width - 4f
            if (!onLeftWing && !onRightWing) continue
          } else if (tRight <= trap.x + 4f || tLeft >= trap.x + trap.width - 4f) {
            continue
          }
        }

        TrollTrapType.SPLITTING_TWIN_FLOOR -> {
          val wingW = trap.width * 0.48f
          val shift = computeTwinSplitShift(trap)
          val onLeftWing = tRight > (trap.x - shift) + 4f && tLeft < (trap.x - shift + wingW) - 2f
          val onRightWing = tRight > (trap.x + trap.width - wingW + shift) + 2f && tLeft < (trap.x + trap.width + shift) - 4f
          if (!onLeftWing && !onRightWing) continue
          val downSlopTwin = trap.vy.coerceAtLeast(0f) + 4f
          if (player.vy >= 0f &&
              prevBottom <= trap.y + 10f &&
              tBottom >= trap.y - downSlopTwin &&
              tTop < trap.y + 6f) {
            player.y = trap.y - player.collisionHeight
            player.vy = 0f
            player.isGrounded = true
            if (onLeftWing && !onRightWing) {
              player.x -= trap.vx
            } else if (onRightWing && !onLeftWing) {
              player.x += trap.vx
            }
            if (trap.triggerType == TrapTriggerType.ON_STEP && trap.state == TrollTrapState.ARMED) {
              triggerTrap(trap, traps, callbacks)
            }
            if (!previousGrounded) {
              onLanded(trap.y)
            }
          }
          continue
        }

        TrollTrapType.PINCER_WALLS -> {
          val jawW = computePincerJawWidth(trap)
          val onLeftJaw = tRight > trap.x + 2f && tLeft < trap.x + jawW - 2f
          val onRightJaw = tRight > trap.x + trap.width - jawW + 2f && tLeft < trap.x + trap.width - 2f
          if (!onLeftJaw && !onRightJaw) continue
        }

        TrollTrapType.EXIT_SLAM_CAGE -> {
          if (trap.state != TrollTrapState.ACTIVE) continue
          if (tRight <= trap.x + 4f || tLeft >= trap.x + trap.width - 4f) continue
          // Stand on top cage roof bar if player vaulted above
          if (player.vy >= 0f && prevBottom <= trap.y + 8f && tBottom >= trap.y - 1f && tTop < trap.y) {
            player.y = trap.y - player.collisionHeight
            player.vy = 0f
            player.isGrounded = true
            if (!previousGrounded) onLanded(trap.y)
          }
          continue
        }

        else -> {
          if (tRight <= trap.x + 4f || tLeft >= trap.x + trap.width - 4f) continue
        }
      }

      val downSlop = trap.vy.coerceAtLeast(0f) + 4f
      val prevTopBound = (trap.y - trap.vy) + 10f
      if (player.vy >= 0f &&
          prevBottom <= prevTopBound &&
          tBottom >= trap.y - downSlop &&
          tTop < trap.y + 6f) {
        player.y = trap.y - player.collisionHeight
        player.vy = 0f
        player.isGrounded = true
        if (trap.type == TrollTrapType.YO_YO_PLATFORM ||
            trap.type == TrollTrapType.EVASIVE_PLATFORM ||
            trap.type == TrollTrapType.SLIDING_FLOOR_SHIFT ||
            trap.type == TrollTrapType.REVERSING_PLATFORM) {
          player.x += trap.vx
        } else if (trap.type == TrollTrapType.REVERSE_CONVEYOR_BELT && trap.state != TrollTrapState.TELEGRAPH) {
          player.x += trap.gustForceX * trap.gustSign
        }
        if (trap.triggerType == TrapTriggerType.ON_STEP && trap.state == TrollTrapState.ARMED) {
          triggerTrap(trap, traps, callbacks)
        }
        if (!previousGrounded) {
          onLanded(trap.y)
        }
      } else if (player.vy < 0f &&
          player.prevY >= trap.y + trap.height - 6f &&
          tTop <= trap.y + trap.height) {
        if (trap.type == TrollTrapType.FALLING_CEILING_BLOCK &&
            trap.state == TrollTrapState.ACTIVE &&
            trap.killsPlayer) {
          callbacks.killPlayer(trap.quipPopup ?: "BONK!", causeKeyFor(trap))
          return true
        }
        player.y = trap.y + trap.height
        player.vy = 0f
      }
    }
    return false
  }

  /**
   * Advances every trap state machine by one deterministic 60 Hz frame.
   */
  fun updateAll(
      traps: List<TrollTrap>,
      player: Player,
      callbacks: TrollTrapCallbacks
  ) {
    tickChainEvents(traps, callbacks)

    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight
    val pCx = player.x + player.collisionWidth * 0.5f
    val pCy = player.y + player.collisionHeight * 0.5f

    for (i in traps.indices) {
      val trap = traps[i]
      if (trap.type == TrollTrapType.TIMED_BLINK_PLATFORM) {
        updateTimedBlinkPlatform(trap)
        continue
      }

      if (trap.type == TrollTrapType.YO_YO_PLATFORM) {
        if (!trap.preSteppedThisFrame) {
          stepYoYoPlatform(trap)
        }
        trap.preSteppedThisFrame = false
        continue
      }

      if (trap.type == TrollTrapType.GUST_REVERSAL_ZONE) {
        updateGustReversalZone(trap, pCx, pCy, callbacks)
        continue
      }

      if (trap.type == TrollTrapType.REVERSE_CONVEYOR_BELT) {
        updateReverseConveyorBelt(trap, callbacks)
        continue
      }

      if (trap.type == TrollTrapType.TILTING_PLANK) {
        updateTiltingPlank(trap, player, pLeft, pRight, pBottom)
      }

      if (trap.type == TrollTrapType.MOVING_SPIKE &&
          trap.triggerType == TrapTriggerType.ON_TIMER_CYCLE) {
        updateOscillatingSpike(trap)
      }

      when (trap.state) {
        TrollTrapState.ARMED -> {
          if (trap.triggerType == TrapTriggerType.ON_TIMER_CYCLE &&
              (trap.type == TrollTrapType.TELEGRAPHED_RUNE_BEAM || trap.type == TrollTrapType.RISING_EMBER_SURGE)) {
            val cycle = trap.lockedCycleFrames.coerceAtLeast(TrapFairnessPolicy.MIN_BEAM_CYCLE_FRAMES)
            val activeLen = if (trap.activeDurationFrames > 0) trap.activeDurationFrames else 36
            val telegraphLen = trap.lockedTelegraphDurationFrames.coerceAtLeast(12)
            val waitFrames = (cycle - activeLen - telegraphLen).coerceAtLeast(24)
            trap.stateTimerFrames++
            if (trap.stateTimerFrames >= waitFrames) {
              trap.stateTimerFrames = 0
              triggerTrap(trap, traps, callbacks)
            }
          } else if (evaluateTrigger(trap, player, pLeft, pRight, pTop, pBottom, pCy)) {
            triggerTrap(trap, traps, callbacks)
          }
        }
        TrollTrapState.TELEGRAPH -> {
          tickTelegraph(trap, traps, callbacks)
        }
        TrollTrapState.ACTIVE -> {
          if (tickActive(trap, traps, pLeft, pRight, pTop, pBottom, pCx, callbacks)) {
            return
          }
        }
        TrollTrapState.RETIRED -> Unit
      }
      trap.preSteppedThisFrame = false
    }
  }

  fun captureSnapshots(traps: List<TrollTrap>): List<TrapSnapshot> {
    val result = ArrayList<TrapSnapshot>(traps.size)
    for (i in traps.indices) {
      val trap = traps[i]
      result.add(
          TrapSnapshot(
              trapId = trap.id,
              x = trap.x,
              y = trap.y,
              vx = trap.vx,
              vy = trap.vy,
              state = trap.state,
              stateTimerFrames = trap.stateTimerFrames,
              isSolid = trap.isSolid,
              isVisible = trap.isVisible,
              morphSpikeHeight = trap.morphSpikeHeight,
              hasRelocated = trap.hasRelocated,
              dropOffsetY = trap.dropOffsetY,
              fadeAlpha = trap.fadeAlpha,
              tiltAngleDeg = trap.tiltAngleDeg,
              angularVelocityDeg = trap.angularVelocityDeg,
              auxProgress = trap.auxProgress,
              activationCount = trap.activationCount,
              gustSign = trap.gustSign,
              prevX = trap.prevX,
              prevY = trap.prevY
          )
      )
    }
    return result
  }

  fun restoreSnapshots(traps: List<TrollTrap>, snapshots: List<TrapSnapshot>) {
    clearPendingChainEvents()
    val snapById = HashMap<Int, TrapSnapshot>(snapshots.size)
    for (i in snapshots.indices) {
      val s = snapshots[i]
      snapById[s.trapId] = s
    }
    for (i in traps.indices) {
      val trap = traps[i]
      val snap = snapById[trap.id]
      if (snap != null) {
        trap.x = snap.x
        trap.y = snap.y
        trap.prevX = snap.prevX
        trap.prevY = snap.prevY
        trap.vx = snap.vx
        trap.vy = snap.vy
        trap.state = snap.state
        trap.stateTimerFrames = snap.stateTimerFrames
        trap.isSolid = snap.isSolid
        trap.isVisible = snap.isVisible
        trap.morphSpikeHeight = snap.morphSpikeHeight
        trap.hasRelocated = snap.hasRelocated
        trap.dropOffsetY = snap.dropOffsetY
        trap.fadeAlpha = snap.fadeAlpha
        trap.tiltAngleDeg = snap.tiltAngleDeg
        trap.angularVelocityDeg = snap.angularVelocityDeg
        trap.auxProgress = snap.auxProgress
        trap.activationCount = snap.activationCount
        trap.gustSign = snap.gustSign
        trap.preSteppedThisFrame = false
      } else {
        trap.reset()
      }
    }
  }

  fun resetAll(traps: List<TrollTrap>) {
    clearPendingChainEvents()
    triggeredCoinIds.clear()
    for (i in traps.indices) {
      traps[i].reset()
    }
  }

  private fun evaluateTrigger(
      trap: TrollTrap,
      player: Player,
      pLeft: Float,
      pRight: Float,
      pTop: Float,
      pBottom: Float,
      pCy: Float
  ): Boolean {
    if (trap.type == TrollTrapType.FLEEING_SUN_GATE && trap.activationCount > 0) {
      // Subsequent flee hops check vicinity around current gate position!
      val minX = trap.x - 56f
      val maxX = trap.x + trap.width + 56f
      return pRight >= minX && pLeft <= maxX &&
          pCy >= trap.y - 60f && pCy <= trap.y + trap.height + 32f
    }

    return when (trap.triggerType) {
      TrapTriggerType.ON_PROXIMITY_X,
      TrapTriggerType.ON_APPROACH_EXIT -> {
        pRight >= trap.triggerMinX && pLeft <= trap.triggerMaxX &&
            pCy >= trap.triggerMinY && pCy <= trap.triggerMaxY
      }
      TrapTriggerType.ON_JUMP_ABOVE -> {
        pRight >= trap.triggerMinX && pLeft <= trap.triggerMaxX &&
            !player.isGrounded && player.vy < 0f &&
            pCy >= trap.triggerMinY && pCy <= trap.triggerMaxY
      }
      TrapTriggerType.ON_STEP -> {
        if (trap.type == TrollTrapType.LAUNCH_BETRAYAL_PAD) {
          player.vy >= 0f &&
              pRight > trap.x + 2f && pLeft < trap.x + trap.width - 2f &&
              pBottom >= trap.y && player.prevY + player.collisionHeight <= trap.y + trap.height + 6f
        } else if (!trap.isSolid) {
          pRight > trap.x + 2f && pLeft < trap.x + trap.width - 2f &&
              pBottom >= trap.y + trap.height - 14f && pTop <= trap.y + trap.height
        } else {
          player.isGrounded &&
              pRight > trap.x + 4f && pLeft < trap.x + trap.width - 4f &&
              abs(pBottom - trap.y) <= 2.5f
        }
      }
      TrapTriggerType.ON_TIMER_CYCLE,
      TrapTriggerType.ON_COIN_COLLECTED,
      TrapTriggerType.ON_TRAP_ACTIVATED -> false
    }
  }

  private fun causeKeyFor(trap: TrollTrap): String = "trap_${trap.id}_${trap.type.name}"

  private fun triggerTrap(
      trap: TrollTrap,
      traps: List<TrollTrap>,
      callbacks: TrollTrapCallbacks
  ) {
    val telegraphFrames = trap.lockedTelegraphDurationFrames
    if (telegraphFrames > 0) {
      enterTelegraphPhase(trap, callbacks)
    } else {
      activateSingleTrap(trap, callbacks)
      dispatchTrapActivatedChain(trap, traps, callbacks)
    }
  }

  private fun enterTelegraphPhase(trap: TrollTrap, callbacks: TrollTrapCallbacks) {
    trap.state = TrollTrapState.TELEGRAPH
    trap.stateTimerFrames = trap.lockedTelegraphDurationFrames
    when (trap.type) {
      TrollTrapType.DELAYED_CRUMBLE,
      TrollTrapType.DISAPPEARING_FLOOR,
      TrollTrapType.SPLIT_BRIDGE -> {
        callbacks.playCrumble()
        callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
      }
      TrollTrapType.FAKE_SAFE_PLATFORM,
      TrollTrapType.PHANTOM_LEDGE,
      TrollTrapType.EVASIVE_PLATFORM -> {
        callbacks.playMechanismShift()
        callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + 4f)
      }
      TrollTrapType.SURPRISE_SPIKE_POP,
      TrollTrapType.TRAILING_SPIKE -> {
        callbacks.playTrapWarning()
        callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
      }
      TrollTrapType.EXIT_SLAM_CAGE,
      TrollTrapType.PINCER_WALLS,
      TrollTrapType.STALKER_CRUSH_WALL -> {
        callbacks.playMechanismShift()
        callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
      }
      TrollTrapType.MIRROR_CURSE_TOTEM -> {
        callbacks.playMechanismShift()
        callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y + trap.height / 2f, 0xFFC084FC.toInt(), 10)
      }
      TrollTrapType.FLEEING_SUN_GATE -> {
        callbacks.playTrapWarning()
        callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y + trap.height / 2f, 0xFFFDE047.toInt(), 8)
      }
      TrollTrapType.LAUNCH_BETRAYAL_PAD -> {
        callbacks.launchPlayer(trap.launchImpulseVy)
        callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y, 0xFFF59E0B.toInt(), 12)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width / 2f, trap.y - 14f, it, 0xFFFBBF24.toInt())
        }
      }
      TrollTrapType.RISING_EMBER_SURGE -> {
        callbacks.playTrapWarning()
        callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.initialY)
      }
      TrollTrapType.TELEGRAPHED_RUNE_BEAM -> {
        callbacks.playTrapWarning()
        callbacks.spawnSparkles(trap.x + 8f, trap.y + trap.height / 2f, 0xFFF59E0B.toInt(), 6)
      }
      else -> {
        callbacks.playTrapWarning()
      }
    }
  }

  private fun tickTelegraph(
      trap: TrollTrap,
      traps: List<TrollTrap>,
      callbacks: TrollTrapCallbacks
  ) {
    trap.stateTimerFrames--
    if ((trap.type == TrollTrapType.DELAYED_CRUMBLE || trap.type == TrollTrapType.SPLIT_BRIDGE) &&
        trap.stateTimerFrames > 0 &&
        trap.stateTimerFrames % 5 == 0) {
      callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
    }
    if (trap.stateTimerFrames <= 0) {
      activateSingleTrap(trap, callbacks)
      dispatchTrapActivatedChain(trap, traps, callbacks)
    }
  }

  private fun activateSingleTrap(trap: TrollTrap, callbacks: TrollTrapCallbacks) {
    trap.state = TrollTrapState.ACTIVE
    trap.stateTimerFrames = trap.activeDurationFrames

    when (trap.type) {
      TrollTrapType.DISAPPEARING_FLOOR -> {
        trap.isSolid = false
        trap.isVisible = false
        trap.state = TrollTrapState.RETIRED
        callbacks.playDisappear()
        callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height / 2f)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width / 2f, trap.y - 10f, it, 0xFFFDE047.toInt())
        }
        trap.linkedTileId?.let { callbacks.hideLinkedTile(it) }
      }

      TrollTrapType.DELAYED_CRUMBLE -> {
        trap.isSolid = false
        trap.vy = 2.6f
        trap.stateTimerFrames = CRUMBLE_DROP_DURATION_FRAMES
        callbacks.playDisappear()
        callbacks.triggerCameraShake(3, 2.0f)
        callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height / 2f)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width / 2f, trap.y - 10f, it, 0xFFFDBA74.toInt())
        }
        trap.linkedTileId?.let { callbacks.hideLinkedTile(it) }
      }

      TrollTrapType.SPLIT_BRIDGE -> {
        trap.auxProgress = 0.05f
        trap.stateTimerFrames = 24
        callbacks.playCrumble()
        callbacks.triggerCameraShake(4, 2.5f)
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFFFDBA74.toInt())
        }
      }

      TrollTrapType.PHANTOM_LEDGE -> {
        trap.isSolid = false
        trap.fadeAlpha = 0.28f
        val cycle = trap.lockedCycleFrames
        if (cycle > 0) {
          trap.stateTimerFrames = cycle
        } else {
          trap.state = TrollTrapState.RETIRED
        }
        callbacks.playDisappear()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFF38BDF8.toInt(), 10)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 12f, it, 0xFF67E8F9.toInt())
        }
      }

      TrollTrapType.TILTING_PLANK -> {
        trap.state = TrollTrapState.ACTIVE
      }

      TrollTrapType.TRAILING_SPIKE,
      TrollTrapType.STALKER_CRUSH_WALL -> {
        trap.stateTimerFrames = if (trap.activeDurationFrames > 0) trap.activeDurationFrames else 300
        callbacks.playMechanismShift()
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFFFB7185.toInt())
        }
      }

      TrollTrapType.EVASIVE_PLATFORM -> {
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 12f, it, 0xFF38BDF8.toInt())
        }
        callbacks.playMechanismShift()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFFDE047.toInt(), 8)
      }

      TrollTrapType.EXIT_SLAM_CAGE -> {
        trap.isSolid = true
        trap.auxProgress = 0.15f
        val lockFrames = trap.effectDurationFrames.coerceAtLeast(45)
        trap.stateTimerFrames = lockFrames
        callbacks.playCeilingSlam()
        callbacks.triggerCameraShake(5, 3.2f)
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height)
        callbacks.spawnPopup(
            trap.x + trap.width * 0.5f,
            trap.y - 16f,
            trap.quipPopup ?: "SLAMMED!",
            0xFFFCA5A5.toInt()
        )
      }

      TrollTrapType.FLEEING_SUN_GATE -> {
        trap.hasRelocated = true
        trap.stateTimerFrames = 14 // Smooth rapid hop
        callbacks.playDoorEscape()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFFDE047.toInt(), 14)
        callbacks.spawnPopup(
            trap.x + trap.width * 0.5f,
            trap.y - 16f,
            trap.quipPopup ?: "CATCH ME!",
            0xFFFBBF24.toInt()
        )
      }

      TrollTrapType.PINCER_WALLS -> {
        trap.auxProgress = 0.12f
        trap.stateTimerFrames = 0 // phase sub-counter: closing first
        callbacks.playMechanismShift()
        callbacks.spawnDustTrickle(trap.x + 12f, trap.y + trap.height * 0.5f)
        callbacks.spawnDustTrickle(trap.x + trap.width - 12f, trap.y + trap.height * 0.5f)
      }

      TrollTrapType.BETRAYAL_SWITCH -> {
        trap.activationCount = 1
        trap.auxProgress = 1f
        trap.state = TrollTrapState.RETIRED
        callbacks.playTrapSnap()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFEF4444.toInt(), 12)
        callbacks.spawnPopup(
            trap.x + trap.width * 0.5f,
            trap.y - 14f,
            trap.quipPopup ?: "WRONG LEVER!",
            0xFFFB7185.toInt()
        )
      }

      TrollTrapType.MIRROR_CURSE_TOTEM -> {
        trap.activationCount = 1
        trap.auxProgress = 1f
        val duration = trap.effectDurationFrames.coerceIn(60, 240)
        trap.stateTimerFrames = duration
        callbacks.applyMirrorCurse(duration)
        callbacks.playFakeDoorPoof()
        callbacks.triggerCameraShake(4, 2.5f)
        callbacks.triggerImpactHaptic()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFC084FC.toInt(), 16)
        callbacks.spawnPopup(
            trap.x + trap.width * 0.5f,
            trap.y - 16f,
            trap.quipPopup ?: "CONTROLS MIRRORED!",
            0xFFD8B4FE.toInt()
        )
      }

      TrollTrapType.SURPRISE_SPIKE_POP -> {
        trap.morphSpikeHeight = (trap.height * 0.45f).coerceAtLeast(8f)
        callbacks.playTrapSnap()
        callbacks.triggerCameraShake(3, 2.2f)
        callbacks.triggerImpactHaptic()
        callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height)
      }

      TrollTrapType.FAKE_SAFE_PLATFORM -> {
        if (trap.killsPlayer) {
          trap.morphSpikeHeight = 10f
          callbacks.playTrapSnap()
          callbacks.triggerCameraShake(3, 2.4f)
          callbacks.triggerImpactHaptic()
          callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y, 0xFFEF4444.toInt(), 12)
        } else {
          trap.isSolid = false
          trap.vy = 3.2f
          trap.stateTimerFrames = CRUMBLE_DROP_DURATION_FRAMES
          callbacks.playDisappear()
          callbacks.triggerCameraShake(3, 2.2f)
          callbacks.triggerImpactHaptic()
          callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height / 2f)
        }
      }

      TrollTrapType.FAKE_EXIT_DOOR -> {
        callbacks.playFakeDoorPoof()
        callbacks.spawnStarburst(trap.x + trap.width / 2f, trap.y + trap.height / 2f)
        callbacks.spawnPopup(trap.x + trap.width / 2f, trap.y - 12f, trap.quipPopup ?: "NICE TRY!", 0xFFFB7185.toInt())
      }

      TrollTrapType.RELOCATING_EXIT_DOOR -> {
        if (!trap.hasRelocated) {
          trap.hasRelocated = true
          val destX = trap.linkedDoorTargetX ?: trap.targetX
          val destY = trap.linkedDoorTargetY ?: trap.targetY
          callbacks.relocateExitGate(destX, destY)
          callbacks.playDoorEscape()
          callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y + trap.height / 2f, 0xFFFDE047.toInt(), 16)
          callbacks.spawnPopup(trap.x + trap.width / 2f, trap.y - 18f, trap.quipPopup ?: "SEE YA!", 0xFFFBBF24.toInt())
        }
        trap.state = TrollTrapState.RETIRED
      }

      TrollTrapType.FALLING_CEILING_BLOCK,
      TrollTrapType.MOVING_CRUSH_WALL,
      TrollTrapType.MOVING_SPIKE -> {
        callbacks.playMechanismShift()
      }

      TrollTrapType.FAKE_CHECKPOINT -> {
        callbacks.playFakeDoorPoof()
        callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y + trap.height / 2f, 0xFFEF4444.toInt(), 14)
        callbacks.spawnPopup(trap.x + trap.width / 2f, trap.y - 14f, trap.quipPopup ?: "SIKE!", 0xFFFB7185.toInt())
      }

      TrollTrapType.LAUNCH_BETRAYAL_PAD -> {
        if (trap.lockedTelegraphDurationFrames <= 0) {
          callbacks.launchPlayer(trap.launchImpulseVy)
          callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y, 0xFFF59E0B.toInt(), 12)
        }
        if (trap.killsPlayer) {
          trap.morphSpikeHeight = 10f
          trap.stateTimerFrames = if (trap.activeDurationFrames > 0) trap.activeDurationFrames else 75
          callbacks.playTrapSnap()
        } else {
          trap.activationCount++
          trap.state = if (trap.activationCount < trap.maxActivations || trap.cycleFrames > 0) {
            TrollTrapState.ARMED
          } else {
            TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.RISING_EMBER_SURGE -> {
        trap.auxProgress = 0f
        trap.stateTimerFrames = 0
        callbacks.playMechanismShift()
        callbacks.triggerCameraShake(4, 2.5f)
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.initialY, 0xFFF97316.toInt(), 12)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.initialY - 14f, it, 0xFFFB923C.toInt())
        }
      }

      TrollTrapType.TELEGRAPHED_RUNE_BEAM -> {
        trap.stateTimerFrames = if (trap.activeDurationFrames > 0) trap.activeDurationFrames else 36
        callbacks.playTrapSnap()
        callbacks.triggerCameraShake(3, 2.2f)
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFEF4444.toInt(), 10)
      }

      TrollTrapType.LOCKED_RUNE_GATE -> {
        callbacks.playMechanismShift()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFF2DD4BF.toInt(), 14)
        callbacks.spawnPopup(
            trap.x + trap.width * 0.5f,
            trap.y - 14f,
            trap.quipPopup ?: "GATE OPENED!",
            0xFF34D399.toInt()
        )
        val travel = hypot((trap.targetX - trap.initialX).toDouble(), (trap.targetY - trap.initialY).toDouble()).toFloat()
        if (travel < 4f) {
          trap.isSolid = false
          trap.isVisible = false
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.SLIDING_FLOOR_SHIFT -> {
        trap.gustSign = 1f
        trap.stateTimerFrames = if (trap.lockedCycleFrames > 0) trap.lockedCycleFrames else 0
        callbacks.playMechanismShift()
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFFFDBA74.toInt())
        }
      }

      TrollTrapType.SPLITTING_TWIN_FLOOR -> {
        trap.auxProgress = 0.02f
        trap.gustSign = 1f
        trap.stateTimerFrames = if (trap.lockedCycleFrames > 0) trap.lockedCycleFrames else 0
        callbacks.playCrumble()
        callbacks.triggerCameraShake(3, 2.2f)
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFFFDBA74.toInt())
        }
      }

      TrollTrapType.REVERSING_PLATFORM -> {
        trap.gustSign = -trap.gustSign
        trap.activationCount++
        trap.stateTimerFrames = 40
        callbacks.playMechanismShift()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFF38BDF8.toInt(), 8)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFF67E8F9.toInt())
        }
      }

      TrollTrapType.OPENING_AMBUSH_WALL -> {
        callbacks.playMechanismShift()
        callbacks.triggerCameraShake(4, 2.6f)
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFFFDE047.toInt())
        }
      }

      TrollTrapType.REVERSING_HAZARD -> {
        trap.auxProgress = 0f
        trap.gustSign = 1f
        trap.activationCount = 0
        trap.stateTimerFrames = if (trap.activeDurationFrames > 0) trap.activeDurationFrames else 320
        callbacks.playTrapSnap()
        callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFEF4444.toInt(), 10)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 14f, it, 0xFFFB7185.toInt())
        }
      }

      TrollTrapType.TEMP_VANISH_REFORM_FLOOR -> {
        trap.isSolid = false
        trap.fadeAlpha = 0.22f
        trap.stateTimerFrames = trap.lockedCycleFrames.coerceAtLeast(45)
        callbacks.playDisappear()
        callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f)
        trap.quipPopup?.let {
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 12f, it, 0xFF67E8F9.toInt())
        }
      }

      TrollTrapType.TIMED_BLINK_PLATFORM,
      TrollTrapType.YO_YO_PLATFORM,
      TrollTrapType.GUST_REVERSAL_ZONE,
      TrollTrapType.REVERSE_CONVEYOR_BELT -> Unit
    }
  }

  private fun tickActive(
      trap: TrollTrap,
      traps: List<TrollTrap>,
      pLeft: Float,
      pRight: Float,
      pTop: Float,
      pBottom: Float,
      pCx: Float,
      callbacks: TrollTrapCallbacks
  ): Boolean {
    when (trap.type) {
      TrollTrapType.DELAYED_CRUMBLE -> {
        trap.vy = (trap.vy + 0.75f).coerceAtMost(9.5f)
        trap.dropOffsetY += trap.vy
        trap.stateTimerFrames--
        trap.fadeAlpha = (trap.stateTimerFrames.toFloat() / CRUMBLE_DROP_DURATION_FRAMES).coerceIn(0f, 1f)
        if (trap.stateTimerFrames <= 0) {
          trap.isVisible = false
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.SPLIT_BRIDGE -> {
        if (trap.auxProgress < 1f) {
          trap.auxProgress = (trap.auxProgress + 0.10f).coerceAtMost(1f)
        }
        if (trap.stateTimerFrames > 0) {
          trap.stateTimerFrames--
        } else {
          trap.auxProgress = 1f
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.PHANTOM_LEDGE -> {
        if (trap.lockedCycleFrames > 0) {
          trap.stateTimerFrames--
          if (trap.stateTimerFrames <= 0) {
            trap.isSolid = true
            trap.fadeAlpha = 1f
            trap.state = TrollTrapState.ARMED
            callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + 8f, 0xFF38BDF8.toInt(), 6)
          }
        }
      }

      TrollTrapType.TRAILING_SPIKE -> {
        val minBound = min(trap.initialX, trap.targetX)
        val maxBound = max(trap.initialX, trap.targetX)
        val center = trap.x + trap.width * 0.5f
        val diff = pCx - center
        val step = trap.lockedMoveSpeed.coerceAtMost(TrapFairnessPolicy.MAX_PURSUIT_TRAP_SPEED)
        if (abs(diff) > 2f) {
          val dir = if (diff > 0f) 1f else -1f
          val nextX = (trap.x + dir * step).coerceIn(minBound, maxBound)
          trap.vx = nextX - trap.x
          trap.x = nextX
        } else {
          trap.vx = 0f
        }
        if (trap.killsPlayer &&
            pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 2f) {
          callbacks.killPlayer(trap.quipPopup ?: "CAUGHT UP!", causeKeyFor(trap))
          return true
        }
        trap.stateTimerFrames--
        val reachedTargetEnd = abs(trap.x - trap.targetX) <= 1.5f
        if (trap.stateTimerFrames <= 0 || reachedTargetEnd) {
          trap.vx = 0f
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.STALKER_CRUSH_WALL -> {
        val dx = trap.targetX - trap.x
        val dy = trap.targetY - trap.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val speed = trap.lockedMoveSpeed.coerceAtMost(TrapFairnessPolicy.MAX_PURSUIT_TRAP_SPEED)
        if (dist <= speed) {
          trap.x = trap.targetX
          trap.y = trap.targetY
          trap.vx = 0f
          trap.vy = 0f
          callbacks.playCeilingSlam()
          callbacks.triggerCameraShake(5, 3.0f)
          trap.state = TrollTrapState.RETIRED
        } else {
          trap.vx = (dx / dist) * speed
          trap.vy = (dy / dist) * speed
          trap.x += trap.vx
          trap.y += trap.vy
        }
        if (trap.killsPlayer &&
            pRight > trap.x + 4f && pLeft < trap.x + trap.width - 4f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 4f) {
          callbacks.killPlayer(trap.quipPopup ?: "STALKED!", causeKeyFor(trap))
          return true
        }
      }

      TrollTrapType.EVASIVE_PLATFORM -> {
        if (!trap.preSteppedThisFrame) {
          stepEvasivePlatform(trap)
        }
        if (abs(trap.x - trap.targetX) <= 0.5f && abs(trap.y - trap.targetY) <= 0.5f) {
          trap.vx = 0f
          trap.vy = 0f
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.EXIT_SLAM_CAGE -> {
        if (trap.auxProgress < 1f) {
          trap.auxProgress = (trap.auxProgress + 0.16f).coerceAtMost(1f)
        }
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          trap.isSolid = false
          trap.auxProgress = 0f
          trap.state = TrollTrapState.RETIRED
          callbacks.playMechanismShift()
          callbacks.spawnPopup(trap.x + trap.width * 0.5f, trap.y - 12f, "CAGE OPENED!", 0xFF34D399.toInt())
        }
      }

      TrollTrapType.FLEEING_SUN_GATE -> {
        val hopIndex = trap.activationCount
        val destX = if (hopIndex == 0) trap.targetX else trap.secondaryX
        val destY = if (hopIndex == 0) trap.targetY else trap.secondaryY
        val remaining = trap.stateTimerFrames.coerceAtLeast(1)
        trap.x += (destX - trap.x) / remaining
        trap.y += (destY - trap.y) / remaining
        callbacks.relocateExitGate(trap.x, trap.y)
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          trap.x = destX
          trap.y = destY
          callbacks.relocateExitGate(destX, destY)
          trap.activationCount++
          if (trap.activationCount >= trap.maxActivations) {
            trap.state = TrollTrapState.RETIRED
            callbacks.spawnPopup(destX + trap.width * 0.5f, destY - 14f, "OKAY FINE!", 0xFF34D399.toInt())
          } else {
            trap.state = TrollTrapState.ARMED
          }
        }
      }

      TrollTrapType.PINCER_WALLS -> {
        val maxJaw = ((trap.width - trap.minSafeGapWidth) * 0.5f).coerceAtLeast(16f)
        val stepNorm = (trap.lockedMoveSpeed / maxJaw).coerceIn(0.02f, 0.25f)
        if (trap.stateTimerFrames == 0) {
          // Closing inward
          trap.auxProgress = (trap.auxProgress + stepNorm).coerceAtMost(1f)
          if (trap.auxProgress >= 1f) {
            trap.stateTimerFrames = 1 // Hold closed briefly
            callbacks.playCeilingSlam()
            callbacks.triggerCameraShake(4, 2.6f)
          }
        } else if (trap.stateTimerFrames in 1..24) {
          trap.stateTimerFrames++
        } else {
          // Opening back out safely
          trap.auxProgress = (trap.auxProgress - stepNorm * 0.7f).coerceAtLeast(0.08f)
          if (trap.auxProgress <= 0.08f) {
            trap.auxProgress = 0.08f
            trap.stateTimerFrames = 0
            trap.state = if (trap.cycleFrames > 0) TrollTrapState.ARMED else TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.MIRROR_CURSE_TOTEM -> {
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.FALLING_CEILING_BLOCK -> {
        if (trap.y < trap.targetY) {
          trap.vy = (trap.vy + 1.8f).coerceAtMost(trap.lockedMoveSpeed.coerceAtLeast(8f))
          trap.y = (trap.y + trap.vy).coerceAtMost(trap.targetY)
          if (trap.killsPlayer &&
              pRight > trap.x + 3f && pLeft < trap.x + trap.width - 3f &&
              pBottom > trap.y + 2f && pTop < trap.y + trap.height + 4f) {
            callbacks.killPlayer(trap.quipPopup ?: "BONK!", causeKeyFor(trap))
            return true
          }
          if (trap.y >= trap.targetY) {
            callbacks.playCeilingSlam()
            callbacks.triggerCameraShake(6, 3.6f)
            callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height)
            trap.state = TrollTrapState.RETIRED
          }
        } else {
          trap.state = TrollTrapState.RETIRED
        }
      }

      TrollTrapType.MOVING_CRUSH_WALL -> {
        val dx = trap.targetX - trap.x
        val dy = trap.targetY - trap.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val speed = trap.lockedMoveSpeed
        if (dist <= speed) {
          trap.x = trap.targetX
          trap.y = trap.targetY
          callbacks.playCeilingSlam()
          callbacks.triggerCameraShake(5, 3.0f)
          trap.state = TrollTrapState.RETIRED
        } else {
          trap.x += (dx / dist) * speed
          trap.y += (dy / dist) * speed
        }
        if (trap.killsPlayer &&
            pRight > trap.x + 3f && pLeft < trap.x + trap.width - 3f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 4f) {
          callbacks.killPlayer(trap.quipPopup ?: "SQUISHED!", causeKeyFor(trap))
          return true
        }
      }

      TrollTrapType.MOVING_SPIKE -> {
        if (trap.triggerType != TrapTriggerType.ON_TIMER_CYCLE) {
          val dx = trap.targetX - trap.x
          val dy = trap.targetY - trap.y
          val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
          val speed = trap.lockedMoveSpeed
          if (dist > speed) {
            trap.x += (dx / dist) * speed
            trap.y += (dy / dist) * speed
          } else {
            trap.x = trap.targetX
            trap.y = trap.targetY
          }
        }
        if (trap.killsPlayer &&
            pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 2f) {
          callbacks.killPlayer(trap.quipPopup ?: "GOTCHA!", causeKeyFor(trap))
          return true
        }
      }

      TrollTrapType.SURPRISE_SPIKE_POP -> {
        if (trap.morphSpikeHeight < trap.height) {
          trap.morphSpikeHeight = (trap.morphSpikeHeight + SPIKE_EXTEND_STEP).coerceAtMost(trap.height)
        }
        val spikeTop = trap.y + trap.height - trap.morphSpikeHeight + 4f
        if (trap.killsPlayer && trap.morphSpikeHeight > 4f &&
            pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
            pBottom > spikeTop && pTop < trap.y + trap.height) {
          callbacks.killPlayer(trap.quipPopup ?: "SURPRISE!", causeKeyFor(trap))
          return true
        }
        if (trap.activeDurationFrames > 0) {
          trap.stateTimerFrames--
          if (trap.stateTimerFrames <= 0) {
            trap.morphSpikeHeight = 0f
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.FAKE_SAFE_PLATFORM -> {
        if (trap.killsPlayer) {
          if (trap.morphSpikeHeight < FAKE_PLATFORM_SPIKE_TARGET) {
            trap.morphSpikeHeight = (trap.morphSpikeHeight + 6.0f).coerceAtMost(FAKE_PLATFORM_SPIKE_TARGET)
          }
          if (trap.morphSpikeHeight > 4f &&
              pRight > trap.x + 4f && pLeft < trap.x + trap.width - 4f &&
              pBottom > trap.y - trap.morphSpikeHeight + 3f && pTop < trap.y + 8f) {
            callbacks.killPlayer(trap.quipPopup ?: "TRICKED!", causeKeyFor(trap))
            return true
          }
        } else {
          trap.vy = (trap.vy + 0.75f).coerceAtMost(9.5f)
          trap.dropOffsetY += trap.vy
          trap.stateTimerFrames--
          trap.fadeAlpha = (trap.stateTimerFrames.toFloat() / CRUMBLE_DROP_DURATION_FRAMES).coerceIn(0f, 1f)
          if (trap.stateTimerFrames <= 0) {
            trap.isVisible = false
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.FAKE_EXIT_DOOR -> {
        if (trap.killsPlayer &&
            pRight > trap.x + 12f && pLeft < trap.x + trap.width - 12f &&
            pBottom > trap.y + 8f && pTop < trap.y + trap.height) {
          callbacks.killPlayer(trap.quipPopup ?: "FAKE DOOR!", causeKeyFor(trap))
          return true
        }
        if (!trap.killsPlayer && trap.activeDurationFrames > 0) {
          trap.stateTimerFrames--
          if (trap.stateTimerFrames <= 0) {
            trap.isVisible = false
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.FAKE_CHECKPOINT -> {
        if (trap.killsPlayer &&
            pRight > trap.x + 6f && pLeft < trap.x + trap.width - 6f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height) {
          callbacks.killPlayer(trap.quipPopup ?: "NOPE!", causeKeyFor(trap))
          return true
        }
        if (!trap.killsPlayer && trap.activeDurationFrames > 0) {
          trap.stateTimerFrames--
          if (trap.stateTimerFrames <= 0) {
            trap.isVisible = false
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.LAUNCH_BETRAYAL_PAD -> {
        if (trap.morphSpikeHeight < 22f) {
          trap.morphSpikeHeight = (trap.morphSpikeHeight + SPIKE_EXTEND_STEP).coerceAtMost(22f)
        }
        val spikeTop = trap.y + trap.height - trap.morphSpikeHeight + 2f
        if (trap.killsPlayer && trap.morphSpikeHeight > 4f &&
            pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
            pBottom > spikeTop && pTop < trap.y + trap.height) {
          callbacks.killPlayer(trap.quipPopup ?: "SPRING SPIKED!", causeKeyFor(trap))
          return true
        }
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          trap.morphSpikeHeight = 0f
          trap.activationCount++
          trap.state = if (trap.cycleFrames > 0 || trap.activationCount < trap.maxActivations) {
            TrollTrapState.ARMED
          } else {
            TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.RISING_EMBER_SURGE -> {
        if (trap.auxProgress < 0.25f) {
          // Rising toward targetY
          val speed = trap.lockedMoveSpeed.coerceIn(
              TrapFairnessPolicy.MIN_PURSUIT_TRAP_SPEED,
              TrapFairnessPolicy.MAX_PURSUIT_TRAP_SPEED
          )
          trap.vy = -speed
          trap.y = (trap.y - speed).coerceAtLeast(trap.targetY)
          if (trap.y <= trap.targetY) {
            trap.y = trap.targetY
            trap.vy = 0f
            trap.auxProgress = 0.5f
            trap.stateTimerFrames = trap.effectDurationFrames.coerceAtLeast(36)
          }
        } else if (trap.auxProgress < 0.75f) {
          // Holding at peak
          trap.vy = 0f
          trap.stateTimerFrames--
          if (trap.stateTimerFrames <= 0) {
            trap.auxProgress = 1f
          }
        } else {
          // Receding safely back to initialY
          val speed = (trap.lockedMoveSpeed * 0.75f).coerceAtLeast(2.2f)
          trap.vy = speed
          trap.y = (trap.y + speed).coerceAtMost(trap.initialY)
          if (trap.y >= trap.initialY) {
            trap.y = trap.initialY
            trap.vy = 0f
            trap.auxProgress = 0f
            trap.activationCount++
            trap.stateTimerFrames = 0
            trap.state = if (trap.cycleFrames > 0 || trap.activationCount < trap.maxActivations) {
              TrollTrapState.ARMED
            } else {
              TrollTrapState.RETIRED
            }
          }
        }
        val basinBottom = trap.initialY + trap.height
        if (trap.killsPlayer && trap.y < trap.initialY - 4f &&
            pRight > trap.x + 4f && pLeft < trap.x + trap.width - 4f &&
            pBottom > trap.y + 4f && pTop < basinBottom) {
          callbacks.killPlayer(trap.quipPopup ?: "SCORCHED!", causeKeyFor(trap))
          return true
        }
      }

      TrollTrapType.TELEGRAPHED_RUNE_BEAM -> {
        if (trap.killsPlayer &&
            pRight > trap.x + 6f && pLeft < trap.x + trap.width - 6f &&
            pBottom > trap.y + 6f && pTop < trap.y + trap.height - 6f) {
          callbacks.killPlayer(trap.quipPopup ?: "RUNE BEAM!", causeKeyFor(trap))
          return true
        }
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          trap.activationCount++
          trap.stateTimerFrames = 0
          trap.state = if (trap.cycleFrames > 0 || trap.activationCount < trap.maxActivations) {
            TrollTrapState.ARMED
          } else {
            TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.LOCKED_RUNE_GATE -> {
        val dx = trap.targetX - trap.x
        val dy = trap.targetY - trap.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val speed = trap.lockedMoveSpeed.coerceAtLeast(TrapFairnessPolicy.MIN_MOVING_TRAP_SPEED)
        if (dist <= speed) {
          trap.x = trap.targetX
          trap.y = trap.targetY
          trap.vx = 0f
          trap.vy = 0f
          trap.isSolid = false
          trap.state = TrollTrapState.RETIRED
        } else {
          trap.vx = (dx / dist) * speed
          trap.vy = (dy / dist) * speed
          trap.x += trap.vx
          trap.y += trap.vy
        }
      }

      TrollTrapType.SLIDING_FLOOR_SHIFT -> {
        if (!trap.preSteppedThisFrame) {
          stepSlidingFloorShift(trap)
        }
        val destX = if (trap.gustSign >= 0f) trap.targetX else trap.initialX
        val destY = if (trap.gustSign >= 0f) trap.targetY else trap.initialY
        val arrived = abs(trap.x - destX) <= 0.5f && abs(trap.y - destY) <= 0.5f
        if (arrived) {
          trap.vx = 0f
          trap.vy = 0f
          if (trap.gustSign >= 0f && trap.lockedCycleFrames > 0) {
            trap.stateTimerFrames--
            if (trap.stateTimerFrames <= 0) {
              trap.gustSign = -1f
            }
          } else if (trap.gustSign < 0f) {
            trap.gustSign = 1f
            trap.activationCount++
            trap.state = if (trap.activationCount < trap.maxActivations || trap.cycleFrames > 0) {
              TrollTrapState.ARMED
            } else {
              TrollTrapState.RETIRED
            }
          } else {
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.SPLITTING_TWIN_FLOOR -> {
        if (!trap.preSteppedThisFrame) {
          stepSplittingTwinFloor(trap)
        }
        if (trap.gustSign >= 0f && trap.auxProgress >= 1f) {
          if (trap.lockedCycleFrames > 0) {
            trap.stateTimerFrames--
            if (trap.stateTimerFrames <= 0) {
              trap.gustSign = -1f
            }
          } else {
            trap.vx = 0f
            trap.state = TrollTrapState.RETIRED
          }
        } else if (trap.gustSign < 0f && trap.auxProgress <= 0f) {
          trap.auxProgress = 0f
          trap.vx = 0f
          trap.gustSign = 1f
          trap.activationCount++
          trap.state = if (trap.activationCount < trap.maxActivations || trap.cycleFrames > 0) {
            TrollTrapState.ARMED
          } else {
            TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.REVERSING_PLATFORM -> {
        if (!trap.preSteppedThisFrame) {
          stepReversingPlatform(trap)
        }
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          if (trap.activationCount < trap.maxActivations) {
            trap.state = TrollTrapState.ARMED
          } else {
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.OPENING_AMBUSH_WALL -> {
        val dx = trap.targetX - trap.x
        val dy = trap.targetY - trap.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val speed = trap.lockedMoveSpeed.coerceAtLeast(TrapFairnessPolicy.MIN_MOVING_TRAP_SPEED)
        if (dist <= speed) {
          trap.x = trap.targetX
          trap.y = trap.targetY
          trap.vx = 0f
          trap.vy = 0f
          trap.state = TrollTrapState.RETIRED
        } else {
          trap.vx = (dx / dist) * speed
          trap.vy = (dy / dist) * speed
          trap.x += trap.vx
          trap.y += trap.vy
        }
      }

      TrollTrapType.REVERSING_HAZARD -> {
        val dx = trap.targetX - trap.initialX
        val dy = trap.targetY - trap.initialY
        val totalDist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(16f)
        val deltaProgress = (trap.lockedMoveSpeed / totalDist) * trap.gustSign
        trap.auxProgress = (trap.auxProgress + deltaProgress).coerceIn(0f, 1f)
        if (trap.gustSign > 0f && trap.activationCount == 0 && trap.auxProgress >= 0.65f) {
          trap.gustSign = -1f
          trap.activationCount = 1
          callbacks.playMechanismShift()
          callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f, 0xFFFDE047.toInt(), 8)
        } else if (trap.gustSign < 0f && trap.auxProgress <= 0.15f) {
          trap.gustSign = 1f
          trap.activationCount = 2
          callbacks.playMechanismShift()
        } else if (trap.auxProgress >= 1f) {
          if (trap.cycleFrames > 0) {
            trap.gustSign = -1f
          } else {
            trap.state = TrollTrapState.RETIRED
          }
        } else if (trap.auxProgress <= 0f && trap.gustSign < 0f) {
          trap.gustSign = 1f
        }
        val prevX = trap.x
        val prevY = trap.y
        trap.x = trap.initialX + dx * trap.auxProgress
        trap.y = trap.initialY + dy * trap.auxProgress
        trap.vx = trap.x - prevX
        trap.vy = trap.y - prevY

        if (trap.killsPlayer &&
            pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 4f) {
          callbacks.killPlayer(trap.quipPopup ?: "REVERSE SAW!", causeKeyFor(trap))
          return true
        }
        if (trap.stateTimerFrames > 0) {
          trap.stateTimerFrames--
          if (trap.stateTimerFrames <= 0 && trap.cycleFrames <= 0) {
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.TEMP_VANISH_REFORM_FLOOR -> {
        trap.stateTimerFrames--
        if (trap.stateTimerFrames <= 0) {
          trap.isSolid = true
          trap.fadeAlpha = 1f
          trap.state = TrollTrapState.ARMED
          callbacks.spawnSparkles(trap.x + trap.width * 0.5f, trap.y + 8f, 0xFF38BDF8.toInt(), 8)
        }
      }

      else -> Unit
    }
    return false
  }

  private fun updateTimedBlinkPlatform(trap: TrollTrap) {
    val cycle = trap.lockedCycleFrames.coerceAtLeast(120)
    val half = cycle / 2
    trap.stateTimerFrames = (trap.stateTimerFrames + 1) % cycle
    val solidWindow = trap.stateTimerFrames < half
    trap.isSolid = solidWindow
    trap.state = when {
      solidWindow && trap.stateTimerFrames >= half - 18 -> TrollTrapState.TELEGRAPH
      solidWindow -> TrollTrapState.ACTIVE
      else -> TrollTrapState.RETIRED
    }
  }

  private fun updateOscillatingSpike(trap: TrollTrap) {
    val cycle = trap.lockedCycleFrames.coerceAtLeast(90)
    trap.stateTimerFrames = (trap.stateTimerFrames + 1) % cycle
    val wave = (0.5 - 0.5 * cos(2.0 * PI * trap.stateTimerFrames / cycle)).toFloat()
    trap.x = trap.initialX + (trap.targetX - trap.initialX) * wave
    trap.y = trap.initialY + (trap.targetY - trap.initialY) * wave
    trap.state = TrollTrapState.ACTIVE
  }

  private fun stepYoYoPlatform(trap: TrollTrap) {
    val cycle = trap.lockedCycleFrames.coerceAtLeast(100)
    trap.prevX = trap.x
    trap.prevY = trap.y
    trap.stateTimerFrames = (trap.stateTimerFrames + 1) % cycle
    val wave = (0.5 - 0.5 * cos(2.0 * PI * trap.stateTimerFrames / cycle)).toFloat()
    trap.x = trap.initialX + (trap.targetX - trap.initialX) * wave
    trap.y = trap.initialY + (trap.targetY - trap.initialY) * wave
    trap.vx = trap.x - trap.prevX
    trap.vy = trap.y - trap.prevY
    trap.state = TrollTrapState.ACTIVE
  }

  private fun stepEvasivePlatform(trap: TrollTrap) {
    trap.prevX = trap.x
    trap.prevY = trap.y
    val dx = trap.targetX - trap.x
    val dy = trap.targetY - trap.y
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    val speed = trap.lockedMoveSpeed
    if (dist <= speed) {
      trap.x = trap.targetX
      trap.y = trap.targetY
    } else {
      trap.x += (dx / dist) * speed
      trap.y += (dy / dist) * speed
    }
    trap.vx = trap.x - trap.prevX
    trap.vy = trap.y - trap.prevY
  }

  private fun updateTiltingPlank(
      trap: TrollTrap,
      player: Player,
      pLeft: Float,
      pRight: Float,
      pBottom: Float
  ) {
    val pCx = (pLeft + pRight) * 0.5f
    val surfaceY = computeTiltingPlankSurfaceY(trap, pCx)
    val playerStandingOnPlank = player.isGrounded &&
        pRight > trap.x + 2f && pLeft < trap.x + trap.width - 2f &&
        abs(pBottom - surfaceY) <= 5f
    if (!playerStandingOnPlank) {
      // Smooth spring-return toward level
      trap.angularVelocityDeg = trap.angularVelocityDeg * 0.82f - trap.tiltAngleDeg * 0.045f
    }
    trap.tiltAngleDeg = (trap.tiltAngleDeg + trap.angularVelocityDeg)
        .coerceIn(-trap.maxTiltDegrees, trap.maxTiltDegrees)
    if (abs(trap.tiltAngleDeg) >= trap.maxTiltDegrees) {
      trap.angularVelocityDeg = 0f
    }
  }

  private fun updateGustReversalZone(
      trap: TrollTrap,
      pCx: Float,
      pCy: Float,
      callbacks: TrollTrapCallbacks
  ) {
    val cycle = trap.lockedCycleFrames.coerceAtLeast(TrapFairnessPolicy.MIN_GUST_CYCLE_FRAMES)
    trap.stateTimerFrames = (trap.stateTimerFrames + 1) % cycle
    val nearFlip = trap.stateTimerFrames >= cycle - GUST_TELEGRAPH_FRAMES
    if (trap.stateTimerFrames == 0) {
      trap.gustSign = -trap.gustSign
      trap.activationCount++
      callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f)
    }
    trap.state = if (nearFlip) TrollTrapState.TELEGRAPH else TrollTrapState.ACTIVE
    if (!nearFlip &&
        pCx in trap.x..(trap.x + trap.width) &&
        pCy in trap.y..(trap.y + trap.height)) {
      callbacks.applyWindForce(trap.gustForceX * trap.gustSign)
    }
  }

  private fun updateReverseConveyorBelt(
      trap: TrollTrap,
      callbacks: TrollTrapCallbacks
  ) {
    val cycle = trap.lockedCycleFrames.coerceAtLeast(TrapFairnessPolicy.MIN_GUST_CYCLE_FRAMES)
    trap.stateTimerFrames = (trap.stateTimerFrames + 1) % cycle
    val nearFlip = trap.stateTimerFrames >= cycle - GUST_TELEGRAPH_FRAMES
    if (trap.stateTimerFrames == 0) {
      trap.gustSign = -trap.gustSign
      trap.activationCount++
      callbacks.playMechanismShift()
      callbacks.spawnDustPuff(trap.x + trap.width * 0.5f, trap.y + trap.height * 0.5f)
    }
    trap.state = if (nearFlip) TrollTrapState.TELEGRAPH else TrollTrapState.ACTIVE
  }

  private fun stepSlidingFloorShift(trap: TrollTrap) {
    trap.prevX = trap.x
    trap.prevY = trap.y
    val destX = if (trap.gustSign >= 0f) trap.targetX else trap.initialX
    val destY = if (trap.gustSign >= 0f) trap.targetY else trap.initialY
    val dx = destX - trap.x
    val dy = destY - trap.y
    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    val speed = trap.lockedMoveSpeed.coerceAtLeast(TrapFairnessPolicy.MIN_MOVING_TRAP_SPEED)
    if (dist <= speed) {
      trap.x = destX
      trap.y = destY
    } else {
      trap.x += (dx / dist) * speed
      trap.y += (dy / dist) * speed
    }
    trap.vx = trap.x - trap.prevX
    trap.vy = trap.y - trap.prevY
  }

  private fun stepSplittingTwinFloor(trap: TrollTrap) {
    trap.prevX = trap.x
    trap.prevY = trap.y
    val step = 0.08f
    val maxShift = computeTwinMaxShift(trap)
    val prevProgress = trap.auxProgress
    if (trap.gustSign >= 0f) {
      trap.auxProgress = (trap.auxProgress + step).coerceAtMost(1f)
    } else {
      trap.auxProgress = (trap.auxProgress - step).coerceAtLeast(0f)
    }
    trap.vx = (trap.auxProgress - prevProgress) * maxShift
    trap.vy = 0f
  }

  private fun stepReversingPlatform(trap: TrollTrap) {
    trap.prevX = trap.x
    trap.prevY = trap.y
    val dx = trap.targetX - trap.initialX
    val dy = trap.targetY - trap.initialY
    val totalDist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(16f)
    val deltaProgress = (trap.lockedMoveSpeed.coerceAtLeast(2.4f) / totalDist) * trap.gustSign
    var nextP = trap.auxProgress + deltaProgress
    if (nextP >= 1f) {
      nextP = 1f
      trap.gustSign = -1f
    } else if (nextP <= 0f) {
      nextP = 0f
      trap.gustSign = 1f
    }
    trap.auxProgress = nextP
    trap.x = trap.initialX + dx * nextP
    trap.y = trap.initialY + dy * nextP
    trap.vx = trap.x - trap.prevX
    trap.vy = trap.y - trap.prevY
  }

  fun computeTwinMaxShift(trap: TrollTrap): Float {
    return if (trap.secondaryX > 4f) trap.secondaryX else 44f
  }

  fun computeTwinSplitShift(trap: TrollTrap): Float {
    return computeTwinMaxShift(trap) * trap.auxProgress.coerceIn(0f, 1f)
  }

  fun computePincerJawWidth(trap: TrollTrap): Float {
    val maxJaw = ((trap.width - trap.minSafeGapWidth) * 0.5f).coerceAtLeast(16f)
    return maxJaw * trap.auxProgress.coerceIn(0.05f, 1f)
  }

  fun computeTiltingPlankSurfaceY(trap: TrollTrap, worldX: Float): Float {
    val cx = trap.x + trap.width * 0.5f
    val halfW = trap.width * 0.5f
    val clampedOffset = (worldX - cx).coerceIn(-halfW, halfW)
    val rad = Math.toRadians(trap.tiltAngleDeg.toDouble())
    val deltaY = (clampedOffset * tan(rad)).toFloat().coerceIn(-trap.height * 0.8f, trap.height * 0.8f)
    return trap.y + deltaY
  }
}
