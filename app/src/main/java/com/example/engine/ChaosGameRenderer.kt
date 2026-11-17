package com.example.engine

import android.content.Context
import android.graphics.*
import com.example.R
import com.example.data.WatcherAppearance
import kotlin.math.sin

/**
 * 60 FPS Native Canvas Game Renderer.
 * Implements 4-layer parallax scrolling using approved forest artwork,
 * subtle Watcher canopy silhouette, smooth camera tracking with deadzone & lookahead,
 * and zero-allocation draw calls.
 */
class ChaosGameRenderer(val context: Context) {
  companion object {
    private val backdropCacheLock = Any()
    private var cachedBackdropBitmap: Bitmap? = null
    private var cachedBgSourceWidth = 1280
    private var cachedBgSourceHeight = 720

    private fun getOrLoadBackdropBitmap(context: Context): Bitmap? = synchronized(backdropCacheLock) {
      val existing = cachedBackdropBitmap
      if (existing != null && !existing.isRecycled) {
        return existing
      }
      return try {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val decoded = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.img_forest_background_layers_1794493662667,
            options
        )
        if (decoded != null) {
          cachedBackdropBitmap = decoded
          cachedBgSourceWidth = decoded.width
          cachedBgSourceHeight = decoded.height
        }
        decoded
      } catch (_: Exception) {
        null
      }
    }
  }

  val aaravRenderer = AaravCharacterRenderer()
  val expansionRenderer = ExpansionAtlasRenderer()
  val tileRenderer = TileAtlasRenderer(expansionRenderer)

  private val cameraLock = Any()

  // Camera coordinates (top-left of viewport in game units)
  var cameraX = 0f
  var cameraY = 0f
  var hasRenderedFrame = false
    private set

  // Cached parallax backdrop bitmap
  private var bgLayersBitmap: Bitmap? = null
  private var bgSourceWidth = 1280
  private var bgSourceHeight = 720

  // Pre-allocated draw rects, paths & paints
  private val srcRect = Rect()
  private val dstRect = RectF()
  private val watcherRect = RectF()
  private val watcherPath = Path()
  private val paintParticle = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintParallax = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
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
  private val paintFirefly = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99FDE047.toInt() }

  private var animTick = 0

  init {
    loadBackgroundLayers()
  }

  private fun loadBackgroundLayers() {
    bgLayersBitmap = getOrLoadBackdropBitmap(context)
    if (bgLayersBitmap != null) {
      bgSourceWidth = cachedBgSourceWidth
      bgSourceHeight = cachedBgSourceHeight
    }
  }

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
    updateCamera(
        engine.player.x,
        engine.player.y,
        engine.player.facingRight,
        viewportWidth,
        viewportHeight,
        engine.level.minCamY,
        engine.level.maxCamY
    )
    val baseCamX: Float
    val baseCamY: Float
    synchronized(cameraLock) {
      hasRenderedFrame = true
      baseCamX = cameraX
      baseCamY = cameraY
    }

    // Subtle damped micro camera shake on heavy landings, drum bounces, and impacts
    val shakeX = if (engine.cameraShakeFrames > 0) {
      sin(animTick * 1.9).toFloat() * engine.cameraShakeMagnitude
    } else 0f
    val shakeY = if (engine.cameraShakeFrames > 0) {
      kotlin.math.cos(animTick * 2.3).toFloat() * engine.cameraShakeMagnitude
    } else 0f
    val renderCamX = (baseCamX + shakeX).coerceAtLeast(0f)
    val renderCamY = baseCamY + shakeY

    // 1. Draw Parallax Background Layers (Forest art for 1-1; distinct themed parallax for 1-2..1-5)
    if (engine.level.theme == com.example.data.LevelTheme.WHISPERING_WOODS) {
      drawParallaxBackdrop(canvas, viewportWidth, viewportHeight)
    } else {
      expansionRenderer.drawThemeBackdrop(
          canvas,
          engine.level.theme,
          viewportWidth,
          viewportHeight,
          renderCamX,
          animTick
      )
    }

    // 2. Subtle Watcher Canopy Silhouette (rare, atmospheric, zero jump scares)
    drawWatcherSilhouette(canvas, engine.watcherAppearance, renderCamX, renderCamY)

    // 3. Draw Monsoon Wind Drafts & Water Currents behind foreground tiles
    for (draft in engine.windDrafts) {
      tileRenderer.drawWindDraft(canvas, draft, renderCamX, renderCamY, animTick)
    }
    for (zone in engine.waterCurrents) {
      expansionRenderer.drawWaterCurrent(canvas, zone, renderCamX, renderCamY, animTick)
    }

    // 4. Draw Collapsing Pillars, Tiles & Ambush Floor Traps
    for (pillar in engine.collapsingPillars) {
      expansionRenderer.drawCollapsingPillar(canvas, pillar, renderCamX, renderCamY, animTick)
    }
    for (tile in engine.tiles) {
      tileRenderer.drawTile(canvas, tile, renderCamX, renderCamY, animTick, engine.hasCheckpointActivated)
    }
    for (trap in engine.trollTraps) {
      tileRenderer.drawTrollTrap(canvas, trap, renderCamX, renderCamY, animTick)
    }
    for (trap in engine.ambushTraps) {
      tileRenderer.drawAmbushTrap(canvas, trap, renderCamX, renderCamY, animTick)
    }

    // 5. Draw World Clue Markers, Resonance Stone, Echo Puzzle Node, Sound Switches & Relics
    for (marker in engine.level.clueMarkers) {
      tileRenderer.drawClueMarker(
          canvas,
          marker,
          renderCamX,
          renderCamY,
          animTick,
          engine.discoveredClues.contains(marker.clueId)
      )
    }
    engine.resonanceStone?.let {
      tileRenderer.drawResonanceStone(canvas, it, renderCamX, renderCamY, animTick)
    }
    engine.echoPuzzleNode?.let {
      tileRenderer.drawEchoPuzzleNode(canvas, it, renderCamX, renderCamY, animTick)
    }
    for (sw in engine.soundSwitches) {
      expansionRenderer.drawSoundSwitch(canvas, sw, renderCamX, renderCamY, animTick)
    }
    for (relic in engine.relics) {
      expansionRenderer.drawRelic(canvas, relic, renderCamX, renderCamY, animTick)
    }

    // 5b. Draw Set Pieces (Awakening Banyan Tree & Shila-Rakshak / Vajra-Surya Stone Guardian)
    if (engine.level.enableForestSetPieces) {
      tileRenderer.drawAwakeningTree(canvas, engine.awakeningTree, renderCamX, renderCamY, animTick)
    }
    if (engine.level.guardianConfig != null) {
      tileRenderer.drawStoneGuardian(canvas, engine.stoneGuardian, renderCamX, renderCamY, animTick)
    }

    // 6. Draw Moving Platforms, Orbiting Mechanisms, Bounce Drums, Swinging Pendulums, Hazards, Diyas & Critters
    for (mp in engine.movingPlatforms) {
      tileRenderer.drawMovingPlatform(canvas, mp, renderCamX, renderCamY)
    }
    for (op in engine.orbitingPlatforms) {
      expansionRenderer.drawOrbitingPlatform(canvas, op, renderCamX, renderCamY, animTick)
    }
    for (drum in engine.bounceDrums) {
      tileRenderer.drawBounceDrum(canvas, drum, renderCamX, renderCamY, animTick)
    }
    for (pend in engine.pendulums) {
      tileRenderer.drawPendulum(canvas, pend, renderCamX, renderCamY)
    }
    for (hazard in engine.fallingHazards) {
      tileRenderer.drawFallingHazard(canvas, hazard, renderCamX, renderCamY, animTick)
    }
    for (lh in engine.lightningHazards) {
      expansionRenderer.drawLightningHazard(canvas, lh, renderCamX, renderCamY, animTick)
    }
    for (diya in engine.diyas) {
      tileRenderer.drawDiya(canvas, diya, renderCamX, renderCamY, animTick)
    }
    for (critter in engine.critters) {
      tileRenderer.drawCritter(canvas, critter, renderCamX, renderCamY)
    }

    // 7. Draw Collectible Coins
    for (coin in engine.coins) {
      tileRenderer.drawCoin(canvas, coin, renderCamX, renderCamY)
    }

    // 8. Draw Vanar Shadow Stalker & Aarav
    if (engine.level.enableForestSetPieces) {
      tileRenderer.drawForestStalker(canvas, engine.forestStalker, renderCamX, renderCamY, animTick)
    }
    if (!engine.isGameOver) {
      aaravRenderer.drawPlayer(canvas, engine.player, renderCamX, renderCamY)
    }

    // 9. Draw Ambient Fireflies (1-1), Particles, Atmospheric Overlays & Floating Score Popups
    if (engine.level.theme == com.example.data.LevelTheme.WHISPERING_WOODS) {
      drawAmbientFireflies(canvas, viewportWidth, viewportHeight, renderCamX)
    }
    drawParticles(canvas, engine, renderCamX, renderCamY)
    expansionRenderer.drawThemeAtmosphereOverlay(
        canvas,
        engine,
        viewportWidth,
        viewportHeight,
        renderCamX,
        renderCamY,
        animTick
    )
    drawPopups(canvas, engine, renderCamX, renderCamY)

    // 10. Draw Foreground Parallax Layer (Hanging jungle vines on 1-1)
    if (engine.level.theme == com.example.data.LevelTheme.WHISPERING_WOODS) {
      drawForegroundVines(canvas, viewportWidth, viewportHeight)
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

  private fun drawParallaxBackdrop(canvas: Canvas, viewW: Float, viewH: Float) {
    val bmp = bgLayersBitmap
    if (bmp != null) {
      val sliceH = bgSourceHeight / 4

      val scrollL1 = (cameraX * 0.05f) % viewW
      srcRect.set(0, 0, bgSourceWidth, sliceH)
      dstRect.set(-scrollL1, 0f, viewW - scrollL1, viewH * 0.45f)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      if (scrollL1 > 0) {
        dstRect.set(viewW - scrollL1, 0f, viewW * 2 - scrollL1, viewH * 0.45f)
        canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      }

      val scrollL2 = (cameraX * 0.20f) % viewW
      srcRect.set(0, sliceH, bgSourceWidth, sliceH * 2)
      dstRect.set(-scrollL2, viewH * 0.15f, viewW - scrollL2, viewH * 0.75f)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      if (scrollL2 > 0) {
        dstRect.set(viewW - scrollL2, viewH * 0.15f, viewW * 2 - scrollL2, viewH * 0.75f)
        canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      }

      val scrollL3 = (cameraX * 0.50f) % viewW
      srcRect.set(0, sliceH * 2, bgSourceWidth, sliceH * 3)
      dstRect.set(-scrollL3, viewH * 0.35f, viewW - scrollL3, viewH)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      if (scrollL3 > 0) {
        dstRect.set(viewW - scrollL3, viewH * 0.35f, viewW * 2 - scrollL3, viewH)
        canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      }
    } else {
      canvas.drawColor(0xFF1B3B2B.toInt())
    }
  }

  private fun drawForegroundVines(canvas: Canvas, viewW: Float, viewH: Float) {
    val bmp = bgLayersBitmap ?: return
    val sliceH = bgSourceHeight / 4
    val scrollL4 = (cameraX * 1.10f) % viewW

    srcRect.set(0, sliceH * 3, bgSourceWidth, bgSourceHeight)
    dstRect.set(-scrollL4, -20f, viewW - scrollL4, viewH * 0.35f)
    canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
    if (scrollL4 > 0) {
      dstRect.set(viewW - scrollL4, -20f, viewW * 2 - scrollL4, viewH * 0.35f)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
    }
  }

  private fun drawAmbientFireflies(canvas: Canvas, viewW: Float, viewH: Float, camX: Float) {
    for (i in 0..9) {
      val fx = ((i * 173f - camX * 0.65f + animTick * 0.4f) % viewW + viewW) % viewW
      val fy = viewH * 0.32f + (i % 5) * (viewH * 0.11f) + (sin(animTick * 0.04 + i) * 14f).toFloat()
      paintFirefly.alpha = (80 + (sin(animTick * 0.07 + i * 1.3) * 65).toInt()).coerceIn(25, 160)
      canvas.drawCircle(fx, fy, 2.4f, paintFirefly)
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
