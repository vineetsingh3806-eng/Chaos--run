package com.example.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.GameplayNotification
import com.example.data.GuardianPhase
import com.example.data.NotificationPriority
import com.example.data.StalkerPhase
import com.example.data.TileType
import com.example.data.TrapPhase
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import kotlin.math.abs

enum class NotificationSlot {
  LOWER_LEFT_SAFE,
  UPPER_LEFT_HUD,
  UPPER_RIGHT_HUD,
  LOWER_RIGHT_SAFE
}

data class NotificationPlacementResult(
    val slot: NotificationSlot,
    val rect: Rect,
    val topLeftOffset: Offset
)

data class GameplayExclusionData(
    val playerRect: Rect,
    val centerFocusRect: Rect,
    val hazardAndEntityRects: List<Rect>
)

/**
 * Computes non-overlapping, gameplay-safe notification placement using actual measured
 * Compose layout bounds (safe drawing container, HUD top bar, and virtual control buttons)
 * combined with live world-to-screen bounding boxes for Aarav, enemies, hazards, and edges.
 */
object NotificationPlacementResolver {

  fun buildGameplayExclusionData(
      engine: ChaosGameEngine,
      renderer: ChaosGameRenderer,
      viewportWidthPx: Float,
      viewportHeightPx: Float,
      notification: GameplayNotification?,
      reusableExclusions: ArrayList<Rect>? = null,
      camXYBuffer: FloatArray? = null
  ): GameplayExclusionData {
    val camBuf = camXYBuffer ?: FloatArray(2)
    val exclusions = if (reusableExclusions != null) {
      reusableExclusions.clear()
      reusableExclusions
    } else {
      ArrayList(16)
    }

    val isPortrait = viewportHeightPx > viewportWidthPx
    val centerFocusRect = if (isPortrait) {
      Rect(
          left = viewportWidthPx * 0.25f,
          top = viewportHeightPx * 0.30f,
          right = viewportWidthPx * 0.75f,
          bottom = viewportHeightPx * 0.64f
      )
    } else {
      Rect(
          left = viewportWidthPx * 0.35f,
          top = viewportHeightPx * 0.28f,
          right = viewportWidthPx * 0.65f,
          bottom = viewportHeightPx * 0.68f
      )
    }

    val playerRect = engine.readWorldStateLocked { lockedEngine ->
      renderer.readCameraPosition(lockedEngine, viewportWidthPx, viewportHeightPx, camBuf)
      val camX = camBuf[0]
      val camY = camBuf[1]

      val pLeft = lockedEngine.player.x - camX - 12f
      val pTop = lockedEngine.player.y - camY - 10f
      val pRight = lockedEngine.player.x + lockedEngine.player.collisionWidth - camX + 12f
      val pBottom = lockedEngine.player.y + lockedEngine.player.collisionHeight - camY + 10f

      fun addWorldBox(wx: Float, wy: Float, ww: Float, wh: Float, pad: Float = 14f) {
        val sx = wx - camX - pad
        val sy = wy - camY - pad
        val ex = wx + ww - camX + pad
        val ey = wy + wh - camY + pad
        if (ex >= 0f && sx <= viewportWidthPx && ey >= 0f && sy <= viewportHeightPx) {
          exclusions.add(Rect(sx, sy, ex, ey))
        }
      }

      // Active critters
      for (i in lockedEngine.critters.indices) {
        val critter = lockedEngine.critters[i]
        if (critter.stunFrames <= 0) {
          addWorldBox(critter.x, critter.y, critter.width, critter.height, pad = 16f)
        }
      }

      // Forest Stalker
      if (lockedEngine.level.enableForestSetPieces &&
          (lockedEngine.forestStalker.phase == StalkerPhase.WARNING ||
              lockedEngine.forestStalker.phase == StalkerPhase.CHASING)) {
        addWorldBox(
            lockedEngine.forestStalker.x,
            lockedEngine.forestStalker.y,
            lockedEngine.forestStalker.width,
            lockedEngine.forestStalker.height,
            pad = 22f
        )
      }

      // Stone Guardian / Vajra-Surya + active shockwaves
      if (lockedEngine.level.guardianConfig != null &&
          (lockedEngine.stoneGuardian.phase == GuardianPhase.AWAKENING ||
              lockedEngine.stoneGuardian.phase == GuardianPhase.ACTIVE)) {
        addWorldBox(
            lockedEngine.stoneGuardian.x,
            lockedEngine.stoneGuardian.groundY - lockedEngine.stoneGuardian.height,
            lockedEngine.stoneGuardian.width,
            lockedEngine.stoneGuardian.height,
            pad = 24f
        )
        for (i in lockedEngine.stoneGuardian.shockwaves.indices) {
          val sw = lockedEngine.stoneGuardian.shockwaves[i]
          if (sw.active) {
            addWorldBox(
                sw.x - sw.radius,
                sw.y - sw.radius,
                sw.radius * 2f,
                sw.radius * 2f,
                pad = 18f
            )
          }
        }
      }

      // Active / telegraphed falling hazards (full drop corridor)
      for (i in lockedEngine.fallingHazards.indices) {
        val hz = lockedEngine.fallingHazards[i]
        if (hz.isWarning || hz.isFalling) {
          addWorldBox(
              hz.x - hz.radius,
              hz.startY,
              hz.radius * 2f,
              (hz.groundY - hz.startY).coerceAtLeast(48f),
              pad = 18f
          )
        }
      }

      // Active / telegraphed lightning columns
      for (i in lockedEngine.lightningHazards.indices) {
        val lh = lockedEngine.lightningHazards[i]
        if (lh.phase == TrapPhase.WARNING || lh.phase == TrapPhase.ACTIVE) {
          addWorldBox(
              lh.x - lh.width * 0.5f,
              lh.topY,
              lh.width,
              (lh.groundY - lh.topY).coerceAtLeast(48f),
              pad = 18f
          )
        }
      }

      // Active / telegraphed ambush traps
      for (i in lockedEngine.ambushTraps.indices) {
        val trap = lockedEngine.ambushTraps[i]
        if (trap.phase == TrapPhase.WARNING || trap.phase == TrapPhase.ACTIVE) {
          addWorldBox(
              trap.x,
              trap.groundY - trap.maxHeight,
              trap.width,
              trap.maxHeight,
              pad = 16f
          )
        }
      }

      // Pendulum bobs & hazard gears
      for (i in lockedEngine.pendulums.indices) {
        val pend = lockedEngine.pendulums[i]
        addWorldBox(
            pend.bobX - pend.bobRadius,
            pend.bobY - pend.bobRadius,
            pend.bobRadius * 2f,
            pend.bobRadius * 2f,
            pad = 16f
        )
      }
      for (i in lockedEngine.orbitingPlatforms.indices) {
        val op = lockedEngine.orbitingPlatforms[i]
        if (op.isHazardGear) {
          addWorldBox(op.x, op.y, op.width, op.height, pad = 16f)
        }
      }

      // Important interactive/edge tiles near Aarav
      for (i in lockedEngine.tiles.indices) {
        val tile = lockedEngine.tiles[i]
        if (!tile.isVisible) continue
        val isCriticalTile = tile.type == TileType.CRUMBLING_WOOD ||
            tile.type == TileType.VANISHING_SLAB ||
            tile.type == TileType.DISAPPEARING_FLOOR ||
            tile.type == TileType.ECHO_BRIDGE ||
            tile.type == TileType.SOUND_GATE ||
            tile.type == TileType.BRAMBLE_SPIKES
        if (isCriticalTile && abs(tile.x - lockedEngine.player.x) < 360f) {
          addWorldBox(tile.x, tile.y, tile.width, tile.height, pad = 10f)
        }
      }

      // Notification's own hazard world focus (guarantees a hazard warning never covers its hazard)
      if (notification?.worldFocusX != null && notification.worldFocusY != null) {
        addWorldBox(
            notification.worldFocusX - 28f,
            notification.worldFocusY - 28f,
            56f,
            56f,
            pad = 18f
        )
      }

      Rect(pLeft, pTop, pRight, pBottom)
    }

    return GameplayExclusionData(
        playerRect = playerRect,
        centerFocusRect = centerFocusRect,
        hazardAndEntityRects = exclusions
    )
  }

  private fun overlapArea(a: Rect, b: Rect): Float {
    val ox = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0f)
    val oy = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0f)
    return ox * oy
  }

  fun resolvePlacement(
      safeAreaBounds: Rect,
      hudBounds: Rect,
      controlBounds: List<Rect>,
      exclusionData: GameplayExclusionData?,
      bannerWidthPx: Float,
      bannerHeightPx: Float,
      gapPx: Float,
      priority: NotificationPriority = NotificationPriority.STORY
  ): NotificationPlacementResult {
    val safeLeft = safeAreaBounds.left
    val safeRight = safeAreaBounds.right
    val effectiveW = bannerWidthPx.coerceAtMost((safeRight - safeLeft).coerceAtLeast(120f))
    val effectiveH = bannerHeightPx.coerceAtLeast(1f)

    var minAllControlsTop = safeAreaBounds.bottom
    var hasAnyControl = false
    var leftControlsTop = safeAreaBounds.bottom
    var hasLeftControl = false
    var jumpControlTop = safeAreaBounds.bottom
    var hasJumpControl = false

    for (i in controlBounds.indices) {
      val top = controlBounds[i].top
      if (!hasAnyControl || top < minAllControlsTop) {
        minAllControlsTop = top
        hasAnyControl = true
      }
      if (i < 2) {
        if (!hasLeftControl || top < leftControlsTop) {
          leftControlsTop = top
          hasLeftControl = true
        }
      } else {
        if (!hasJumpControl || top < jumpControlTop) {
          jumpControlTop = top
          hasJumpControl = true
        }
      }
    }
    if (!hasLeftControl) leftControlsTop = minAllControlsTop
    if (!hasJumpControl) jumpControlTop = minAllControlsTop

    val hudBottomEdge = if (hudBounds.height > 0f) hudBounds.bottom else safeAreaBounds.top
    val topSafeBound = maxOf(safeAreaBounds.top, hudBottomEdge) + gapPx
    val bottomSafeBoundLeft = minOf(leftControlsTop, safeAreaBounds.bottom) - gapPx
    val bottomSafeBoundRight = minOf(jumpControlTop, safeAreaBounds.bottom) - gapPx

    val lowerLeftRect = Rect(
        left = safeLeft,
        top = (bottomSafeBoundLeft - effectiveH).coerceAtLeast(safeAreaBounds.top),
        right = (safeLeft + effectiveW).coerceAtMost(safeRight),
        bottom = bottomSafeBoundLeft.coerceAtLeast(safeAreaBounds.top + effectiveH)
    )
    val upperLeftRect = Rect(
        left = safeLeft,
        top = topSafeBound,
        right = (safeLeft + effectiveW).coerceAtMost(safeRight),
        bottom = (topSafeBound + effectiveH).coerceAtMost(safeAreaBounds.bottom)
    )
    val upperRightRect = Rect(
        left = (safeRight - effectiveW).coerceAtLeast(safeLeft),
        top = topSafeBound,
        right = safeRight,
        bottom = (topSafeBound + effectiveH).coerceAtMost(safeAreaBounds.bottom)
    )

    fun scoreCandidate(slot: NotificationSlot, rect: Rect): Float {
      var penalty = 0f
      for (i in controlBounds.indices) {
        val area = overlapArea(rect, controlBounds[i])
        if (area > 0f) penalty += 1_000_000f + area * 100f
      }
      if (hudBounds.height > 0f) {
        val hudOverlap = overlapArea(rect, hudBounds)
        if (hudOverlap > 0f) penalty += 1_000_000f + hudOverlap * 100f
      }
      if (rect.top < safeAreaBounds.top - 1f || rect.bottom > safeAreaBounds.bottom + 1f) {
        penalty += 500_000f
      }
      if (exclusionData != null) {
        val playerOverlap = overlapArea(rect, exclusionData.playerRect)
        if (playerOverlap > 0f) penalty += 50_000f + playerOverlap * 10f
        val centerOverlap = overlapArea(rect, exclusionData.centerFocusRect)
        if (centerOverlap > 0f) penalty += 40_000f + centerOverlap * 8f
        val hazardRects = exclusionData.hazardAndEntityRects
        for (i in hazardRects.indices) {
          val exOverlap = overlapArea(rect, hazardRects[i])
          if (exOverlap > 0f) penalty += 25_000f + exOverlap * 5f
        }
      }
      val orderBias = when (slot) {
        NotificationSlot.LOWER_LEFT_SAFE -> if (priority == NotificationPriority.INFO) 3f else 0f
        NotificationSlot.UPPER_LEFT_HUD -> if (priority == NotificationPriority.INFO) 0f else 1f
        NotificationSlot.UPPER_RIGHT_HUD -> 2f
        NotificationSlot.LOWER_RIGHT_SAFE -> 4f
      }
      return penalty + orderBias
    }

    var bestSlot: NotificationSlot
    var bestRect: Rect
    var bestScore: Float

    if (priority == NotificationPriority.INFO) {
      bestSlot = NotificationSlot.UPPER_LEFT_HUD
      bestRect = upperLeftRect
      bestScore = scoreCandidate(bestSlot, bestRect)

      val sUpperRight = scoreCandidate(NotificationSlot.UPPER_RIGHT_HUD, upperRightRect)
      if (sUpperRight < bestScore) {
        bestScore = sUpperRight
        bestSlot = NotificationSlot.UPPER_RIGHT_HUD
        bestRect = upperRightRect
      }
      val sLowerLeft = scoreCandidate(NotificationSlot.LOWER_LEFT_SAFE, lowerLeftRect)
      if (sLowerLeft < bestScore) {
        bestSlot = NotificationSlot.LOWER_LEFT_SAFE
        bestRect = lowerLeftRect
      }
    } else {
      val lowerRightRect = Rect(
          left = (safeRight - effectiveW).coerceAtLeast(safeLeft),
          top = (bottomSafeBoundRight - effectiveH).coerceAtLeast(safeAreaBounds.top),
          right = safeRight,
          bottom = bottomSafeBoundRight.coerceAtLeast(safeAreaBounds.top + effectiveH)
      )
      bestSlot = NotificationSlot.LOWER_LEFT_SAFE
      bestRect = lowerLeftRect
      bestScore = scoreCandidate(bestSlot, bestRect)

      val sUpperLeft = scoreCandidate(NotificationSlot.UPPER_LEFT_HUD, upperLeftRect)
      if (sUpperLeft < bestScore) {
        bestScore = sUpperLeft
        bestSlot = NotificationSlot.UPPER_LEFT_HUD
        bestRect = upperLeftRect
      }
      val sUpperRight = scoreCandidate(NotificationSlot.UPPER_RIGHT_HUD, upperRightRect)
      if (sUpperRight < bestScore) {
        bestScore = sUpperRight
        bestSlot = NotificationSlot.UPPER_RIGHT_HUD
        bestRect = upperRightRect
      }
      val sLowerRight = scoreCandidate(NotificationSlot.LOWER_RIGHT_SAFE, lowerRightRect)
      if (sLowerRight < bestScore) {
        bestSlot = NotificationSlot.LOWER_RIGHT_SAFE
        bestRect = lowerRightRect
      }
    }

    return NotificationPlacementResult(
        slot = bestSlot,
        rect = bestRect,
        topLeftOffset = Offset(
            x = bestRect.left - safeAreaBounds.left,
            y = bestRect.top - safeAreaBounds.top
        )
    )
  }
}
