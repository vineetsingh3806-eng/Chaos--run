package com.example.engine

import android.content.Context
import android.graphics.*
import com.example.R

/**
 * 60 FPS Native Canvas Game Renderer.
 * Implements 4-layer parallax scrolling using approved forest artwork,
 * smooth camera tracking with deadzone & lookahead, and zero-allocation draw calls.
 */
class ChaosGameRenderer(val context: Context) {
  val aaravRenderer = AaravCharacterRenderer()
  val tileRenderer = TileAtlasRenderer()

  // Camera coordinates (top-left of viewport in game units)
  var cameraX = 0f
  var cameraY = 0f

  // Cached parallax backdrop bitmap
  private var bgLayersBitmap: Bitmap? = null
  private var bgSourceWidth = 1280
  private var bgSourceHeight = 720

  // Pre-allocated draw rects & paints
  private val srcRect = Rect()
  private val dstRect = RectF()
  private val paintParticle = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintParallax = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

  private var animTick = 0

  init {
    loadBackgroundLayers()
  }

  private fun loadBackgroundLayers() {
    try {
      val options = BitmapFactory.Options().apply { inScaled = false }
      bgLayersBitmap = BitmapFactory.decodeResource(
          context.resources,
          R.drawable.img_forest_background_layers_1794493662667,
          options
      )
      bgLayersBitmap?.let {
        bgSourceWidth = it.width
        bgSourceHeight = it.height
      }
    } catch (_: Exception) {
      // Fallback if resource unavailable
    }
  }

  fun updateCamera(playerX: Float, playerY: Float, playerFacingRight: Boolean, viewportWidth: Float, viewportHeight: Float) {
    // Lookahead: shifts camera forward in movement direction
    val lookahead = if (playerFacingRight) 60f else -60f
    val targetCamX = (playerX + lookahead - viewportWidth * 0.38f).coerceAtLeast(0f)
    val targetCamY = (playerY - viewportHeight * 0.55f).coerceIn(-48f, 240f)

    // Smooth exponential damping
    cameraX += (targetCamX - cameraX) * 0.10f
    cameraY += (targetCamY - cameraY) * 0.08f
  }

  fun render(canvas: Canvas, engine: ChaosGameEngine, viewportWidth: Float, viewportHeight: Float) {
    animTick++
    updateCamera(engine.player.x, engine.player.y, engine.player.facingRight, viewportWidth, viewportHeight)

    // 1. Draw Parallax Background Layers
    drawParallaxBackdrop(canvas, viewportWidth, viewportHeight)

    // 2. Draw Tiles
    for (tile in engine.tiles) {
      tileRenderer.drawTile(canvas, tile, cameraX, cameraY, animTick)
    }

    // 3. Draw Moving Platforms
    for (mp in engine.movingPlatforms) {
      tileRenderer.drawMovingPlatform(canvas, mp, cameraX, cameraY)
    }

    // 4. Draw Collectible Coins
    for (coin in engine.coins) {
      tileRenderer.drawCoin(canvas, coin, cameraX, cameraY)
    }

    // 5. Draw Aarav
    if (!engine.isGameOver) {
      aaravRenderer.drawPlayer(canvas, engine.player, cameraX, cameraY)
    }

    // 6. Draw Particles
    drawParticles(canvas)

    // 7. Draw Foreground Parallax Layer (Hanging jungle vines)
    drawForegroundVines(canvas, viewportWidth, viewportHeight)
  }

  private fun drawParallaxBackdrop(canvas: Canvas, viewW: Float, viewH: Float) {
    val bmp = bgLayersBitmap
    if (bmp != null) {
      // The background image contains 4 horizontal visual slices (top to bottom):
      // Slice 0 (Distant Mountains): Y 0 to 25% of height
      // Slice 1 (Far Canopy): Y 25% to 50%
      // Slice 2 (Mid Canopy): Y 50% to 75%
      // Slice 3 (Foreground Foliage): Y 75% to 100%
      val sliceH = bgSourceHeight / 4

      // Layer 1: Distant Mountains (0.05x parallax scroll)
      val scrollL1 = (cameraX * 0.05f) % viewW
      srcRect.set(0, 0, bgSourceWidth, sliceH)
      dstRect.set(-scrollL1, 0f, viewW - scrollL1, viewH * 0.45f)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      if (scrollL1 > 0) {
        dstRect.set(viewW - scrollL1, 0f, viewW * 2 - scrollL1, viewH * 0.45f)
        canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      }

      // Layer 2: Canopy & Sunbeams (0.20x parallax scroll)
      val scrollL2 = (cameraX * 0.20f) % viewW
      srcRect.set(0, sliceH, bgSourceWidth, sliceH * 2)
      dstRect.set(-scrollL2, viewH * 0.15f, viewW - scrollL2, viewH * 0.75f)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      if (scrollL2 > 0) {
        dstRect.set(viewW - scrollL2, viewH * 0.15f, viewW * 2 - scrollL2, viewH * 0.75f)
        canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      }

      // Layer 3: Mid Canopy & Ancient Trunks (0.50x parallax scroll)
      val scrollL3 = (cameraX * 0.50f) % viewW
      srcRect.set(0, sliceH * 2, bgSourceWidth, sliceH * 3)
      dstRect.set(-scrollL3, viewH * 0.35f, viewW - scrollL3, viewH)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      if (scrollL3 > 0) {
        dstRect.set(viewW - scrollL3, viewH * 0.35f, viewW * 2 - scrollL3, viewH)
        canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
      }
    } else {
      // Solid atmospheric gradient fallback
      canvas.drawColor(0xFF1B3B2B.toInt())
    }
  }

  private fun drawForegroundVines(canvas: Canvas, viewW: Float, viewH: Float) {
    val bmp = bgLayersBitmap ?: return
    val sliceH = bgSourceHeight / 4
    val scrollL4 = (cameraX * 1.10f) % viewW

    // Slice 3: Hanging vines in foreground
    srcRect.set(0, sliceH * 3, bgSourceWidth, bgSourceHeight)
    dstRect.set(-scrollL4, -20f, viewW - scrollL4, viewH * 0.35f)
    canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
    if (scrollL4 > 0) {
      dstRect.set(viewW - scrollL4, -20f, viewW * 2 - scrollL4, viewH * 0.35f)
      canvas.drawBitmap(bmp, srcRect, dstRect, paintParallax)
    }
  }

  private fun drawParticles(canvas: Canvas) {
    // Note: Engine maintains pre-allocated particle array
  }
}
