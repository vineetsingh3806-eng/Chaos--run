package com.example.engine

import android.graphics.*
import com.example.data.Coin
import com.example.data.GameTile
import com.example.data.MovingPlatform
import com.example.data.TileType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity Tile & Interactive Objects Atlas Renderer for Green Forest.
 * Renders mossy timber, carved runic stone, crumbling planks, bramble spikes,
 * checkpoint bells, animated coins, and the ancient Sun Gate portal.
 */
class TileAtlasRenderer {
  // Pre-allocated Paints
  private val paintWood = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5C3D2E.toInt() }
  private val paintWoodGrain = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF42281D.toInt() }
  private val paintMoss = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4A7A3A.toInt() }
  private val paintMossLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6CA84E.toInt() }
  private val paintStone = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF475569.toInt() }
  private val paintStoneDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334155.toInt() }
  private val paintStoneLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF64748B.toInt() }
  private val paintRuneGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF38BDF8.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
  }
  private val paintRuneAmber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFFF59E0B.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
  }
  private val paintSpike = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF14532D.toInt() }
  private val paintSpikeTip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDC2626.toInt() }
  private val paintCoinOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
  private val paintCoinInner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFDE047.toInt() }
  private val paintCoinStar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD97706.toInt() }
  private val paintBell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEAB308.toInt() }
  private val paintBellPost = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E2723.toInt() }
  private val paintLanternGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FDE047 }
  private val paintGateStone = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E293B.toInt() }
  private val paintGatePortal = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintGateSun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
  private val paintOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF1C1917.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }

  // Pre-allocated geometric helpers
  private val rectTemp = RectF()
  private val pathTemp = Path()

  fun drawTile(canvas: Canvas, tile: GameTile, cameraX: Float, cameraY: Float, animTick: Int) {
    if (!tile.isVisible) return

    val drawX = tile.x - cameraX
    val drawY = tile.y - cameraY

    // Off-screen culling (safety margin: 64 px)
    if (drawX + tile.width < -64f || drawX > canvas.width + 64f ||
        drawY + tile.height < -64f || drawY > canvas.height + 64f) {
      return
    }

    rectTemp.set(drawX, drawY, drawX + tile.width, drawY + tile.height)

    when (tile.type) {
      TileType.MOSSY_TIMBER -> drawMossyTimber(canvas, rectTemp)
      TileType.CARVED_STONE -> drawCarvedStone(canvas, rectTemp, tile.id)
      TileType.CRUMBLING_WOOD -> drawCrumblingWood(canvas, rectTemp, tile.isCrumbling, tile.crumbleTimerFrames, animTick)
      TileType.DISAPPEARING_FLOOR -> drawDisappearingFloor(canvas, rectTemp, animTick)
      TileType.BRAMBLE_SPIKES -> drawBrambleSpikes(canvas, rectTemp)
      TileType.CHECKPOINT_BELL -> drawCheckpointBell(canvas, rectTemp, animTick)
      TileType.SUN_GATE -> drawSunGate(canvas, rectTemp, animTick)
      else -> {
        canvas.drawRoundRect(rectTemp, 4f, 4f, paintStone)
        canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)
      }
    }
  }

  private fun drawMossyTimber(canvas: Canvas, rect: RectF) {
    // 1. Timber wood base
    canvas.drawRoundRect(rect, 4f, 4f, paintWood)
    // Wood grain lines
    canvas.drawLine(rect.left + 4f, rect.top + 16f, rect.right - 4f, rect.top + 16f, paintWoodGrain)
    canvas.drawLine(rect.left + 8f, rect.top + 32f, rect.right - 8f, rect.top + 32f, paintWoodGrain)
    canvas.drawRoundRect(rect, 4f, 4f, paintOutline)

    // 2. Mossy canopy cap fringe on top
    val mossRect = RectF(rect.left - 2f, rect.top - 3f, rect.right + 2f, rect.top + 10f)
    canvas.drawRoundRect(mossRect, 5f, 5f, paintMoss)
    // Moss light highlights
    canvas.drawCircle(rect.left + 12f, rect.top + 3f, 4f, paintMossLight)
    canvas.drawCircle(rect.left + 28f, rect.top + 2f, 5f, paintMossLight)
    canvas.drawCircle(rect.left + 40f, rect.top + 4f, 3.5f, paintMossLight)
  }

  private fun drawCarvedStone(canvas: Canvas, rect: RectF, tileId: Int) {
    // Weathered ancient stone block
    canvas.drawRoundRect(rect, 4f, 4f, paintStone)
    // Stone bevel highlight
    canvas.drawLine(rect.left + 2f, rect.top + 2f, rect.right - 2f, rect.top + 2f, paintStoneLight)
    canvas.drawLine(rect.left + 2f, rect.bottom - 2f, rect.right - 2f, rect.bottom - 2f, paintStoneDark)
    canvas.drawRoundRect(rect, 4f, 4f, paintOutline)

    // Ancient runic glyphs (cyan or amber based on tileId)
    val runePaint = if (tileId % 2 == 0) paintRuneGlow else paintRuneAmber
    val cx = rect.centerX()
    val cy = rect.centerY()
    pathTemp.reset()
    pathTemp.moveTo(cx - 8f, cy - 8f)
    pathTemp.lineTo(cx, cy + 6f)
    pathTemp.lineTo(cx + 8f, cy - 8f)
    pathTemp.moveTo(cx, cy - 10f)
    pathTemp.lineTo(cx, cy + 10f)
    canvas.drawPath(pathTemp, runePaint)
  }

  private fun drawCrumblingWood(canvas: Canvas, rect: RectF, isCrumbling: Boolean, timer: Int, tick: Int) {
    val shakeOffset = if (isCrumbling) ((tick % 4) - 2) * 1.5f else 0f
    rectTemp.set(rect.left + shakeOffset, rect.top, rect.right + shakeOffset, rect.bottom)

    canvas.drawRoundRect(rectTemp, 3f, 3f, paintWood)

    // Stress fracture lines
    pathTemp.reset()
    pathTemp.moveTo(rectTemp.left + 14f, rectTemp.top)
    pathTemp.lineTo(rectTemp.left + 20f, rectTemp.centerY())
    pathTemp.lineTo(rectTemp.left + 16f, rectTemp.bottom)

    pathTemp.moveTo(rectTemp.right - 18f, rectTemp.top)
    pathTemp.lineTo(rectTemp.right - 12f, rectTemp.centerY() + 4f)
    pathTemp.lineTo(rectTemp.right - 22f, rectTemp.bottom)
    canvas.drawPath(pathTemp, paintWoodGrain)

    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
  }

  private fun drawDisappearingFloor(canvas: Canvas, rect: RectF, tick: Int) {
    // Shimmering mystical rune stone
    val alphaPulse = (180 + (sin(tick * 0.12) * 50).toInt()).coerceIn(100, 255)
    paintStoneLight.alpha = alphaPulse
    canvas.drawRoundRect(rect, 4f, 4f, paintStoneLight)
    canvas.drawRoundRect(rect, 4f, 4f, paintOutline)

    // Glyphs glowing
    paintRuneGlow.alpha = alphaPulse
    canvas.drawCircle(rect.centerX(), rect.centerY(), 8f, paintRuneGlow)
    paintStoneLight.alpha = 255
    paintRuneGlow.alpha = 255
  }

  private fun drawBrambleSpikes(canvas: Canvas, rect: RectF) {
    // 3 Curved bramble spikes
    val spikeWidth = rect.width() / 3f
    for (i in 0..2) {
      val sx = rect.left + i * spikeWidth
      pathTemp.reset()
      pathTemp.moveTo(sx, rect.bottom)
      pathTemp.quadTo(sx + spikeWidth * 0.4f, rect.top + 8f, sx + spikeWidth * 0.5f, rect.top)
      pathTemp.quadTo(sx + spikeWidth * 0.6f, rect.top + 8f, sx + spikeWidth, rect.bottom)
      pathTemp.close()

      canvas.drawPath(pathTemp, paintSpike)
      canvas.drawPath(pathTemp, paintOutline)

      // Crimson poison tip
      canvas.drawCircle(sx + spikeWidth * 0.5f, rect.top + 3f, 2.5f, paintSpikeTip)
    }
  }

  fun drawMovingPlatform(canvas: Canvas, mp: MovingPlatform, cameraX: Float, cameraY: Float) {
    val drawX = mp.x - cameraX
    val drawY = mp.y - cameraY
    rectTemp.set(drawX, drawY, drawX + mp.width, drawY + mp.height)

    // Platform slab
    canvas.drawRoundRect(rectTemp, 6f, 6f, paintStone)
    canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)

    // Moss top fringe
    val mossRect = RectF(drawX, drawY - 2f, drawX + mp.width, drawY + 8f)
    canvas.drawRoundRect(mossRect, 4f, 4f, paintMoss)

    // Bronze suspension brackets
    canvas.drawCircle(drawX + 16f, drawY + 12f, 4f, paintCoinOuter)
    canvas.drawCircle(drawX + mp.width - 16f, drawY + 12f, 4f, paintCoinOuter)
  }

  private fun drawCheckpointBell(canvas: Canvas, rect: RectF, tick: Int) {
    // Wooden post
    val postWidth = 10f
    val postLeft = rect.centerX() - postWidth / 2
    canvas.drawRect(postLeft, rect.top, postLeft + postWidth, rect.bottom, paintBellPost)
    canvas.drawRect(postLeft, rect.top, postLeft + postWidth, rect.bottom, paintOutline)

    // Lantern Glow
    val lanternY = rect.top + 16f
    canvas.drawCircle(postLeft - 8f, lanternY, 14f, paintLanternGlow)
    canvas.drawCircle(postLeft - 8f, lanternY, 5f, paintCoinInner)

    // Brass Bell (with subtle idle sway)
    val bellSway = (sin(tick * 0.08) * 3f).toFloat()
    val bellX = rect.centerX() + 10f + bellSway
    val bellY = rect.top + 22f

    pathTemp.reset()
    pathTemp.moveTo(bellX - 10f, bellY + 14f)
    pathTemp.quadTo(bellX - 8f, bellY, bellX, bellY)
    pathTemp.quadTo(bellX + 8f, bellY, bellX + 10f, bellY + 14f)
    pathTemp.close()
    canvas.drawPath(pathTemp, paintBell)
    canvas.drawPath(pathTemp, paintOutline)
    // Clapper
    canvas.drawCircle(bellX, bellY + 16f, 3f, paintCoinOuter)
  }

  private fun drawSunGate(canvas: Canvas, rect: RectF, tick: Int) {
    // Ancient Stone Archway
    canvas.drawRoundRect(rect, 24f, 24f, paintGateStone)
    canvas.drawRoundRect(rect, 24f, 24f, paintOutline)

    // Swirling Sun Vortex Portal
    val portalRect = RectF(rect.left + 16f, rect.top + 24f, rect.right - 16f, rect.bottom - 4f)
    val portalShader = RadialGradient(
        portalRect.centerX(), portalRect.centerY(), portalRect.width() * 0.6f,
        intArrayOf(0xFFFFFBEB.toInt(), 0xFFF59E0B.toInt(), 0xFFB45309.toInt(), 0x00000000),
        floatArrayOf(0f, 0.4f, 0.8f, 1.0f),
        Shader.TileMode.CLAMP
    )
    paintGatePortal.shader = portalShader
    canvas.drawRoundRect(portalRect, 16f, 16f, paintGatePortal)

    // Rotating Sun Crest atop arch
    val crestX = rect.centerX()
    val crestY = rect.top + 8f
    canvas.drawCircle(crestX, crestY, 12f, paintGateSun)
    canvas.drawCircle(crestX, crestY, 12f, paintOutline)

    // Sun rays
    for (i in 0..7) {
      val angle = (tick * 0.03 + i * (PI / 4)).toFloat()
      val rx1 = crestX + cos(angle.toDouble()).toFloat() * 14f
      val ry1 = crestY + sin(angle.toDouble()).toFloat() * 14f
      val rx2 = crestX + cos(angle.toDouble()).toFloat() * 20f
      val ry2 = crestY + sin(angle.toDouble()).toFloat() * 20f
      canvas.drawLine(rx1, ry1, rx2, ry2, paintGateSun)
    }
  }

  fun drawCoin(canvas: Canvas, coin: Coin, cameraX: Float, cameraY: Float) {
    if (coin.isCollected) return
    val drawX = coin.x - cameraX
    val drawY = coin.y - cameraY

    // 3D spinning coin effect using horizontal scale cosine
    val spin = cos(coin.animTimer.toDouble()).toFloat()
    val scaleX = Math.abs(spin).coerceAtLeast(0.15f)

    canvas.save()
    canvas.translate(drawX, drawY)
    canvas.scale(scaleX, 1f)

    // Outer rim
    canvas.drawCircle(0f, 0f, coin.radius, paintCoinOuter)
    canvas.drawCircle(0f, 0f, coin.radius, paintOutline)

    // Inner disc
    canvas.drawCircle(0f, 0f, coin.radius * 0.72f, paintCoinInner)

    // Embossed Star Emblem
    if (scaleX > 0.4f) {
      canvas.drawCircle(0f, 0f, 3f, paintCoinStar)
    }

    canvas.restore()
  }
}
