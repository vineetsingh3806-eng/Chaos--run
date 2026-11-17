package com.example.engine

import com.example.data.Player
import com.example.data.TrapTriggerType
import com.example.data.TrollTrap
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Side-effect callbacks emitted by [TrollTrapSystem] so trap mechanics remain
 * cleanly decoupled from player movement physics, audio synthesis, particles, and level gates.
 */
interface TrollTrapCallbacks {
  fun killPlayer(quip: String)
  fun relocateExitGate(destX: Float, destY: Float)
  fun hideLinkedTile(tileId: Int)
  fun playTrapSnap()
  fun playCrumble()
  fun playDisappear()
  fun playCeilingSlam()
  fun playDoorEscape()
  fun playFakeDoorPoof()
  fun playMechanismShift()
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
 * across all [TrollTrapType] implementations.
 */
class TrollTrapSystem {

  companion object {
    private const val SPIKE_EXTEND_STEP = 8.5f
    private const val FAKE_PLATFORM_SPIKE_TARGET = 24f
    private const val CRUMBLE_DROP_DURATION_FRAMES = 10
  }

  /**
   * Resolves horizontal side collisions between Aarav and solid/visible [TrollTrap]s.
   * Returns true if a lethal moving crusher or falling block killed Aarav.
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

    for (trap in traps) {
      if (!trap.isSolid || !trap.isVisible) continue
      if (curRight > trap.x && curLeft < trap.x + trap.width &&
          curBottom > trap.y + 4f && curTop < trap.y + trap.height - 4f) {
        if ((trap.type == TrollTrapType.MOVING_CRUSH_WALL ||
                trap.type == TrollTrapType.FALLING_CEILING_BLOCK) &&
            trap.state == TrollTrapState.ACTIVE &&
            trap.killsPlayer) {
          callbacks.killPlayer(trap.quipPopup ?: "BONK!")
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

    for (trap in traps) {
      if (!trap.isSolid || !trap.isVisible) continue
      if (tRight <= trap.x + 4f || tLeft >= trap.x + trap.width - 4f) continue

      if (player.vy >= 0f &&
          player.prevY + player.collisionHeight <= trap.y + 8f &&
          tBottom >= trap.y - 1f &&
          tTop < trap.y) {
        player.y = trap.y - player.collisionHeight
        player.vy = 0f
        player.isGrounded = true
        if (trap.triggerType == TrapTriggerType.ON_STEP && trap.state == TrollTrapState.ARMED) {
          triggerTrap(trap, callbacks)
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
          callbacks.killPlayer(trap.quipPopup ?: "BONK!")
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
    val pLeft = player.x
    val pRight = player.x + player.collisionWidth
    val pTop = player.y
    val pBottom = player.y + player.collisionHeight
    val pCx = player.x + player.collisionWidth / 2f
    val pCy = player.y + player.collisionHeight / 2f

    for (trap in traps) {
      if (trap.type == TrollTrapType.TIMED_BLINK_PLATFORM) {
        updateTimedBlinkPlatform(trap)
        continue
      }

      if (trap.type == TrollTrapType.MOVING_SPIKE &&
          trap.triggerType == TrapTriggerType.ON_TIMER_CYCLE) {
        updateOscillatingSpike(trap)
      }

      when (trap.state) {
        TrollTrapState.ARMED -> {
          if (evaluateTrigger(trap, player, pLeft, pRight, pTop, pBottom, pCy)) {
            triggerTrap(trap, callbacks)
          }
        }
        TrollTrapState.TELEGRAPH -> {
          tickTelegraph(trap, callbacks)
        }
        TrollTrapState.ACTIVE -> {
          if (tickActive(trap, pLeft, pRight, pTop, pBottom, callbacks)) {
            return
          }
        }
        TrollTrapState.RETIRED -> Unit
      }
    }
  }

  fun resetAll(traps: List<TrollTrap>) {
    for (trap in traps) {
      trap.reset()
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
        // Non-solid ON_STEP traps trigger on floor contact; solid ON_STEP traps trigger on landing
        // or if standing horizontally atop their surface.
        if (!trap.isSolid) {
          pRight > trap.x + 2f && pLeft < trap.x + trap.width - 2f &&
              pBottom >= trap.y + trap.height - 14f && pTop <= trap.y + trap.height
        } else {
          player.isGrounded &&
              pRight > trap.x + 4f && pLeft < trap.x + trap.width - 4f &&
              kotlin.math.abs(pBottom - trap.y) <= 2.5f
        }
      }
      TrapTriggerType.ON_TIMER_CYCLE -> false
    }
  }

  private fun triggerTrap(trap: TrollTrap, callbacks: TrollTrapCallbacks) {
    if (trap.telegraphDurationFrames > 0) {
      trap.state = TrollTrapState.TELEGRAPH
      trap.stateTimerFrames = trap.telegraphDurationFrames
      when (trap.type) {
        TrollTrapType.DELAYED_CRUMBLE,
        TrollTrapType.DISAPPEARING_FLOOR -> {
          callbacks.playCrumble()
          callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
        }
        TrollTrapType.FAKE_SAFE_PLATFORM -> {
          callbacks.playMechanismShift()
          callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + 4f)
        }
        TrollTrapType.SURPRISE_SPIKE_POP -> {
          callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
        }
        else -> Unit
      }
    } else {
      activateTrap(trap, callbacks)
    }
  }

  private fun tickTelegraph(trap: TrollTrap, callbacks: TrollTrapCallbacks) {
    trap.stateTimerFrames--
    if (trap.type == TrollTrapType.DELAYED_CRUMBLE &&
        trap.stateTimerFrames > 0 &&
        trap.stateTimerFrames % 5 == 0) {
      callbacks.spawnDustTrickle(trap.x + trap.width / 2f, trap.y + trap.height)
    }
    if (trap.stateTimerFrames <= 0) {
      activateTrap(trap, callbacks)
    }
  }

  private fun activateTrap(trap: TrollTrap, callbacks: TrollTrapCallbacks) {
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
        // Platform gives way: immediately non-solid while visual plank tumbles & fades out cleanly
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

      TrollTrapType.SURPRISE_SPIKE_POP -> {
        // Begin smooth upward spike extension (first pop is clearly visible & rises cleanly to full height)
        trap.morphSpikeHeight = (trap.height * 0.45f).coerceAtLeast(8f)
        callbacks.playTrapSnap()
        callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height)
      }

      TrollTrapType.FAKE_SAFE_PLATFORM -> {
        if (trap.killsPlayer) {
          trap.morphSpikeHeight = 10f
          callbacks.playTrapSnap()
          callbacks.spawnSparkles(trap.x + trap.width / 2f, trap.y, 0xFFEF4444.toInt(), 12)
        } else {
          trap.isSolid = false
          trap.vy = 3.2f
          trap.stateTimerFrames = CRUMBLE_DROP_DURATION_FRAMES
          callbacks.playDisappear()
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

      TrollTrapType.TIMED_BLINK_PLATFORM -> Unit
    }
  }

  private fun tickActive(
      trap: TrollTrap,
      pLeft: Float,
      pRight: Float,
      pTop: Float,
      pBottom: Float,
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

      TrollTrapType.FALLING_CEILING_BLOCK -> {
        if (trap.y < trap.targetY) {
          trap.vy = (trap.vy + 1.8f).coerceAtMost(trap.moveSpeed.coerceAtLeast(8f))
          trap.y = (trap.y + trap.vy).coerceAtMost(trap.targetY)
          if (trap.killsPlayer &&
              pRight > trap.x + 3f && pLeft < trap.x + trap.width - 3f &&
              pBottom > trap.y + 2f && pTop < trap.y + trap.height + 4f) {
            callbacks.killPlayer(trap.quipPopup ?: "BONK!")
            return true
          }
          if (trap.y >= trap.targetY) {
            callbacks.playCeilingSlam()
            callbacks.triggerCameraShake(6, 3.6f)
            callbacks.spawnDustPuff(trap.x + trap.width / 2f, trap.y + trap.height)
            trap.state = TrollTrapState.RETIRED
          }
        }
      }

      TrollTrapType.MOVING_CRUSH_WALL -> {
        val dx = trap.targetX - trap.x
        val dy = trap.targetY - trap.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist <= trap.moveSpeed) {
          trap.x = trap.targetX
          trap.y = trap.targetY
          callbacks.playCeilingSlam()
          callbacks.triggerCameraShake(5, 3.0f)
          trap.state = TrollTrapState.RETIRED
        } else {
          trap.x += (dx / dist) * trap.moveSpeed
          trap.y += (dy / dist) * trap.moveSpeed
        }
        if (trap.killsPlayer &&
            pRight > trap.x + 3f && pLeft < trap.x + trap.width - 3f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 4f) {
          callbacks.killPlayer(trap.quipPopup ?: "SQUISHED!")
          return true
        }
      }

      TrollTrapType.MOVING_SPIKE -> {
        if (trap.triggerType != TrapTriggerType.ON_TIMER_CYCLE) {
          val dx = trap.targetX - trap.x
          val dy = trap.targetY - trap.y
          val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
          if (dist > trap.moveSpeed) {
            trap.x += (dx / dist) * trap.moveSpeed
            trap.y += (dy / dist) * trap.moveSpeed
          } else {
            trap.x = trap.targetX
            trap.y = trap.targetY
          }
        }
        if (trap.killsPlayer &&
            pRight > trap.x + 5f && pLeft < trap.x + trap.width - 5f &&
            pBottom > trap.y + 4f && pTop < trap.y + trap.height - 2f) {
          callbacks.killPlayer(trap.quipPopup ?: "GOTCHA!")
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
          callbacks.killPlayer(trap.quipPopup ?: "SURPRISE!")
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
            callbacks.killPlayer(trap.quipPopup ?: "TRICKED!")
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
          callbacks.killPlayer(trap.quipPopup ?: "FAKE DOOR!")
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
          callbacks.killPlayer(trap.quipPopup ?: "NOPE!")
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

      else -> Unit
    }
    return false
  }

  private fun updateTimedBlinkPlatform(trap: TrollTrap) {
    val cycle = trap.cycleFrames.coerceAtLeast(120)
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
    val cycle = trap.cycleFrames.coerceAtLeast(90)
    trap.stateTimerFrames = (trap.stateTimerFrames + 1) % cycle
    val wave = (0.5 - 0.5 * cos(2.0 * PI * trap.stateTimerFrames / cycle)).toFloat()
    trap.x = trap.initialX + (trap.targetX - trap.initialX) * wave
    trap.y = trap.initialY + (trap.targetY - trap.initialY) * wave
    trap.state = TrollTrapState.ACTIVE
  }
}
