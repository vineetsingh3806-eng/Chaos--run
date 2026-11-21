package com.example.engine

import android.content.Context
import android.graphics.*
import com.example.data.PlayableCharacter
import com.example.data.WatcherAppearance
import kotlin.math.sin

/**
 * 60 FPS Native Canvas Game Renderer.
 * Implements 4-layer parallax scrolling using approved forest artwork,
 * subtle Watcher canopy silhouette, smooth camera tracking with deadzone & lookahead,
 * and zero-allocation draw calls.
 */
class ChaosGameRenderer(val context: Context) {
  val aaravRenderer = AaravCharacterRenderer()
  val characterRenderer = PlayerCharacterRenderer(aaravRenderer)
  val expansionRenderer = ExpansionAtlasRenderer()
  val tileRenderer = TileAtlasRenderer(expansionRenderer)

  @Volatile
  var selectedCharacter: PlayableCharacter = PlayableCharacter.DEFAULT

  private val cameraLock = Any()

  // Camera coordinates (top-left of viewport in game units)
  var cameraX = 0f
  var cameraY = 0f
  var hasRenderedFrame = false
    private set

  // Pre-allocated draw rects, paths & paints
  private val watcherRect = RectF()
  private val watcherPath = Path()
  private val paintParticle = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintWatcherCloak = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F292E.toInt() }
  private val paintWatcherEyes = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFDE047.toInt() }
  private val paintPopupStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF091318.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 4f
    textSize = 18f
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
  }
  private val paintPopupFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFFFDE047.toInt()
    textSize = 18f
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT_BOLD
  }
  private val paintFirefly = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FEF9C3.toInt() }
  private val paintMirrorAura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xAAC084FC.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 3f
    strokeCap = Paint.Cap.ROUND
  }
  private val paintMirrorCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55A855F7 }

  private var animTick = 0

  fun resetCamera() {
    synchronized(cameraLock) {
      cameraX = 0f
      cameraY = 0f
      hasRenderedFrame = false
    }
    animTick = 0
  }

  fun readCameraPosition(
      engine: ChaosGameEngine,
      viewportWidth: Float,
      viewportHeight: Float,
      outXY: FloatArray
  ) {
    synchronized(cameraLock) {
      if (hasRenderedFrame) {
        outXY[0] = cameraX
        outXY[1] = cameraY
        return
      }
    }
    val lookahead = if (engine.player.facingRight) 60f else -60f
    outXY[0] = (engine.player.x + lookahead - viewportWidth * 0.38f).coerceAtLeast(0f)
    outXY[1] = (engine.player.y - viewportHeight * 0.55f)
        .coerceIn(engine.level.minCamY, engine.level.maxCamY)
  }

  fun effectiveCameraPosition(
      engine: ChaosGameEngine,
      viewportWidth: Float,
      viewportHeight: Float
  ): Pair<Float, Float> {
    synchronized(cameraLock) {
      if (hasRenderedFrame) {
        return cameraX to cameraY
      }
    }
    val lookahead = if (engine.player.facingRight) 60f else -60f
    val targetCamX = (engine.player.x + lookahead - viewportWidth * 0.38f).coerceAtLeast(0f)
    val targetCamY = (engine.player.y - viewportHeight * 0.55f)
        .coerceIn(engine.level.minCamY, engine.level.maxCamY)
    return targetCamX to targetCamY
  }

  fun snapCamera(
      playerX: Float,
      playerY: Float,
      playerFacingRight: Boolean,
      viewportWidth: Float,
      viewportHeight: Float,
      minCamY: Float = -48f,
      maxCamY: Float = 240f
  ) {
    val lookahead = if (playerFacingRight) 60f else -60f
    val targetCamX = (playerX + lookahead - viewportWidth * 0.38f).coerceAtLeast(0f)
    val targetCamY = (playerY - viewportHeight * 0.55f).coerceIn(minCamY, maxCamY)

    synchronized(cameraLock) {
      cameraX = targetCamX
      cameraY = targetCamY
    }
  }

  fun updateCamera(
      playerX: Float,
      playerY: Float,
      playerFacingRight: Boolean,
      viewportWidth: Float,
      viewportHeight: Float,
      minCamY: Float = -48f,
      maxCamY: Float = 240f
  ) {
    val lookahead = if (playerFacingRight) 60f else -60f
    val targetCamX = (playerX + lookahead - viewportWidth * 0.38f).coerceAtLeast(0f)
    val targetCamY = (playerY - viewportHeight * 0.55f).coerceIn(minCamY, maxCamY)

    synchronized(cameraLock) {
      cameraX += (targetCamX - cameraX) * 0.10f
      cameraY += (targetCamY - cameraY) * 0.08f
    }
  }

  fun render(canvas: Canvas, engine: ChaosGameEngine, viewportWidth: Float, viewportHeight: Float) {
    animTick++

    // Step 1: Read player camera target atomically from worldStateLock (released before cameraLock)
    var playerX = 0f
    var playerY = 0f
    var playerFacingRight = true
    var minCamY = -48f
    var maxCamY = 240f
    var shouldSnapCamera = false
    engine.readWorldStateLocked { locked ->
      playerX = locked.player.x
      playerY = locked.player.y
      playerFacingRight = locked.player.facingRight
      minCamY = locked.level.minCamY
      maxCamY = locked.level.maxCamY
      shouldSnapCamera = locked.consumePendingCameraSnap()
    }

    // Step 2: Update camera under cameraLock (leaf lock; released before Step 3 worldStateLock)
    if (shouldSnapCamera) {
      snapCamera(
          playerX,
          playerY,
          playerFacingRight,
          viewportWidth,
          viewportHeight,
          minCamY,
          maxCamY
      )
    } else {
      updateCamera(
          playerX,
          playerY,
          playerFacingRight,
          viewportWidth,
          viewportHeight,
          minCamY,
          maxCamY
      )
    }
    val baseCamX: Float
    val baseCamY: Float
    synchronized(cameraLock) {
      hasRenderedFrame = true
      baseCamX = cameraX
      baseCamY = cameraY
    }

    // Step 3: Draw entire world coherently under worldStateLock (no nested cameraLock calls)
    engine.readWorldStateLocked { locked ->
      // Subtle damped micro camera shake on heavy landings, drum bounces, and impacts
      val shakeX = if (locked.cameraShakeFrames > 0) {
        sin(animTick * 1.9).toFloat() * locked.cameraShakeMagnitude
      } else 0f
      val shakeY = if (locked.cameraShakeFrames > 0) {
        kotlin.math.cos(animTick * 2.3).toFloat() * locked.cameraShakeMagnitude
      } else 0f
      val renderCamX = (baseCamX + shakeX).coerceAtLeast(0f)
      val renderCamY = baseCamY + shakeY

      // 1. Draw Clean Cartoon Parallax Background Layers across all World 1 levels
      expansionRenderer.drawThemeBackdrop(
          canvas,
          locked.level.theme,
          viewportWidth,
          viewportHeight,
          renderCamX,
          animTick
      )

      // 2. Subtle Watcher Canopy Silhouette (rare, atmospheric, zero jump scares)
      drawWatcherSilhouette(canvas, locked.watcherAppearance, renderCamX, renderCamY)

      // 3. Draw Monsoon Wind Drafts & Water Currents behind foreground tiles
      for (draft in locked.windDrafts) {
        tileRenderer.drawWindDraft(canvas, draft, renderCamX, renderCamY, animTick)
      }
      for (zone in locked.waterCurrents) {
        expansionRenderer.drawWaterCurrent(canvas, zone, renderCamX, renderCamY, animTick)
      }

      // 4. Draw Collapsing Pillars, Tiles & Ambush Floor Traps
      for (pillar in locked.collapsingPillars) {
        expansionRenderer.drawCollapsingPillar(canvas, pillar, renderCamX, renderCamY, animTick)
      }
      for (tile in locked.tiles) {
        tileRenderer.drawTile(canvas, tile, renderCamX, renderCamY, animTick, locked.hasCheckpointActivated)
      }
      for (trap in locked.trollTraps) {
        tileRenderer.drawTrollTrap(canvas, trap, renderCamX, renderCamY, animTick)
      }
      for (trap in locked.ambushTraps) {
        tileRenderer.drawAmbushTrap(canvas, trap, renderCamX, renderCamY, animTick)
      }

      // 5. Draw World Clue Markers, Resonance Stone, Echo Puzzle Node, Sound Switches & Relics
      for (marker in locked.level.clueMarkers) {
        tileRenderer.drawClueMarker(
            canvas,
            marker,
            renderCamX,
            renderCamY,
            animTick,
            locked.discoveredClues.contains(marker.clueId)
        )
      }
      locked.resonanceStone?.let {
        tileRenderer.drawResonanceStone(canvas, it, renderCamX, renderCamY, animTick)
      }
      locked.echoPuzzleNode?.let {
        tileRenderer.drawEchoPuzzleNode(canvas, it, renderCamX, renderCamY, animTick)
      }
      for (sw in locked.soundSwitches) {
        expansionRenderer.drawSoundSwitch(canvas, sw, renderCamX, renderCamY, animTick)
      }
      for (relic in locked.relics) {
        expansionRenderer.drawRelic(canvas, relic, renderCamX, renderCamY, animTick)
      }

      // 5b. Draw Set Pieces (Awakening Banyan Tree & Shila-Rakshak / Vajra-Surya Stone Guardian)
      if (locked.level.enableForestSetPieces) {
        tileRenderer.drawAwakeningTree(canvas, locked.awakeningTree, renderCamX, renderCamY, animTick)
      }
      if (locked.level.guardianConfig != null) {
        tileRenderer.drawStoneGuardian(canvas, locked.stoneGuardian, renderCamX, renderCamY, animTick)
      }

      // 6. Draw Moving Platforms, Orbiting Mechanisms, Bounce Drums, Swinging Pendulums, Hazards, Diyas & Critters
      for (mp in locked.movingPlatforms) {
        tileRenderer.drawMovingPlatform(canvas, mp, renderCamX, renderCamY)
      }
      for (op in locked.orbitingPlatforms) {
        expansionRenderer.drawOrbitingPlatform(canvas, op, renderCamX, renderCamY, animTick)
      }
      for (drum in locked.bounceDrums) {
        tileRenderer.drawBounceDrum(canvas, drum, renderCamX, renderCamY, animTick)
      }
      for (pend in locked.pendulums) {
        tileRenderer.drawPendulum(canvas, pend, renderCamX, renderCamY)
      }
      for (hazard in locked.fallingHazards) {
        tileRenderer.drawFallingHazard(canvas, hazard, renderCamX, renderCamY, animTick)
      }
      for (lh in locked.lightningHazards) {
        expansionRenderer.drawLightningHazard(canvas, lh, renderCamX, renderCamY, animTick)
      }
      for (diya in locked.diyas) {
        tileRenderer.drawDiya(canvas, diya, renderCamX, renderCamY, animTick)
      }
      for (critter in locked.critters) {
        tileRenderer.drawCritter(canvas, critter, renderCamX, renderCamY)
      }

      // 7. Draw Collectible Coins
      for (coin in locked.coins) {
        tileRenderer.drawCoin(canvas, coin, renderCamX, renderCamY)
      }

      // 8. Draw Vanar Shadow Stalker & Aarav
      if (locked.level.enableForestSetPieces) {
        tileRenderer.drawForestStalker(canvas, locked.forestStalker, renderCamX, renderCamY, animTick)
      }
      if (!locked.isGameOver) {
        characterRenderer.drawCharacter(canvas, selectedCharacter, locked.player, renderCamX, renderCamY)
        if (locked.mirrorCurseFrames > 0) {
          val cx = locked.player.x + locked.player.collisionWidth * 0.5f - renderCamX
          val cy = locked.player.y - 14f - renderCamY
          val pulse = 10f + sin(animTick * 0.35).toFloat() * 2.5f
          canvas.drawCircle(cx, cy, pulse, paintMirrorCore)
          canvas.drawCircle(cx, cy, pulse, paintMirrorAura)
          // Left/right flip chevron arrows above Aarav
          canvas.drawLine(cx - 7f, cy, cx + 7f, cy, paintMirrorAura)
          canvas.drawLine(cx - 4f, cy - 3f, cx - 7f, cy, paintMirrorAura)
          canvas.drawLine(cx - 4f, cy + 3f, cx - 7f, cy, paintMirrorAura)
          canvas.drawLine(cx + 4f, cy - 3f, cx + 7f, cy, paintMirrorAura)
          canvas.drawLine(cx + 4f, cy + 3f, cx + 7f, cy, paintMirrorAura)
        }
      }

      // 9. Draw Ambient Fireflies (1-1), Particles, Atmospheric Overlays & Floating Score Popups
      if (locked.level.theme == com.example.data.LevelTheme.WHISPERING_WOODS) {
        drawAmbientFireflies(canvas, viewportWidth, viewportHeight, renderCamX)
      }
      drawParticles(canvas, locked, renderCamX, renderCamY)
      expansionRenderer.drawThemeAtmosphereOverlay(
          canvas,
          locked,
          viewportWidth,
          viewportHeight,
          renderCamX,
          renderCamY,
          animTick
      )
      drawPopups(canvas, locked, renderCamX, renderCamY)
    }
  }

  private fun drawWatcherSilhouette(
      canvas: Canvas,
      watcher: WatcherAppearance,
      camX: Float,
      camY: Float
  ) {
    if (!watcher.isActive || watcher.remainingFrames <= 0) return

    val elapsed = watcher.totalFrames - watcher.remainingFrames
    val fadeAlpha = when {
      elapsed < 24 -> elapsed / 24f
      watcher.remainingFrames < 32 -> watcher.remainingFrames / 32f
      else -> 1f
    }.coerceIn(0f, 1f)

    val drawX = watcher.worldX - camX
    val drawY = watcher.worldY - camY
    if (drawX < -80f || drawX > canvas.width + 80f) return

    paintWatcherCloak.color = 0xFF0A1D22.toInt()
    paintWatcherCloak.alpha = (fadeAlpha * 225).toInt()
    paintWatcherEyes.alpha = (fadeAlpha * 240).toInt()

    // Subtle misty teal halo behind canopy boughs (zero jump-scare, atmospheric)
    paintParticle.color = 0xFF14B8A6.toInt()
    paintParticle.alpha = (fadeAlpha * 70).toInt()
    canvas.drawCircle(drawX, drawY - 4f, 36f, paintParticle)

    // Subtle cloaked figure perched among the boughs
    watcherPath.reset()
    watcherPath.moveTo(drawX, drawY - 26f)
    watcherPath.quadTo(drawX - 18f, drawY + 6f, drawX - 22f, drawY + 38f)
    watcherPath.lineTo(drawX + 22f, drawY + 38f)
    watcherPath.quadTo(drawX + 18f, drawY + 6f, drawX, drawY - 26f)
    watcherPath.close()
    canvas.drawPath(watcherPath, paintWatcherCloak)

    watcherRect.set(drawX - 12f, drawY - 36f, drawX + 12f, drawY - 14f)
    canvas.drawOval(watcherRect, paintWatcherCloak)

    // Two soft amber eyes watching quietly
    canvas.drawCircle(drawX - 4.5f, drawY - 24f, 2.8f, paintWatcherEyes)
    canvas.drawCircle(drawX + 4.5f, drawY - 24f, 2.8f, paintWatcherEyes)
  }

  private fun drawAmbientFireflies(canvas: Canvas, viewW: Float, viewH: Float, camX: Float) {
    if (viewW <= 0f || viewH <= 0f) return
    for (i in 0..5) {
      val fx = ((i * 223f - camX * 0.55f + animTick * 0.35f) % viewW + viewW) % viewW
      val fy = viewH * 0.34f + (i % 3) * (viewH * 0.14f) + (sin(animTick * 0.04 + i) * 12f).toFloat()
      paintFirefly.alpha = (65 + (sin(animTick * 0.07 + i * 1.3) * 45).toInt()).coerceIn(25, 120)
      canvas.drawCircle(fx, fy, 2.3f, paintFirefly)
    }
  }

  private fun drawParticles(canvas: Canvas, engine: ChaosGameEngine, camX: Float, camY: Float) {
    for (p in engine.particles) {
      if (!p.active) continue
      val alpha = ((1f - (p.life / p.maxLife)) * 255f).toInt().coerceIn(0, 255)
      paintParticle.color = p.color
      paintParticle.alpha = alpha
      canvas.drawCircle(p.x - camX, p.y - camY, p.size, paintParticle)
    }
  }

  private fun drawPopups(canvas: Canvas, engine: ChaosGameEngine, camX: Float, camY: Float) {
    for (p in engine.popups) {
      if (!p.active) continue
      val alpha = ((1f - (p.life / p.maxLife)) * 255f).toInt().coerceIn(0, 255)
      val sx = p.x - camX
      val sy = p.y - camY
      paintPopupStroke.alpha = alpha
      paintPopupFill.color = p.color
      paintPopupFill.alpha = alpha
      canvas.drawText(p.text, sx, sy, paintPopupStroke)
      canvas.drawText(p.text, sx, sy, paintPopupFill)
    }
  }
}
