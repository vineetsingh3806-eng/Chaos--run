package com.example.engine

import android.graphics.*
import com.example.data.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity Tile & Interactive Objects Atlas Renderer for Green Forest.
 * Renders mossy timber, carved runic stone, crumbling planks, bramble spikes,
 * checkpoint bells (with bell glyph), animated coins, Ash-Prints, Resonance Stone,
 * Canopy Echo node, Ancient Carving stele, and the ancient Sun Gate portal
 * with zero per-frame object allocations.
 */
class TileAtlasRenderer(
    private val expansionRenderer: ExpansionAtlasRenderer = ExpansionAtlasRenderer()
) {
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
  private val paintGateOuterGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99B45309.toInt() }
  private val paintGateMidGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCF59E0B.toInt() }
  private val paintGateCoreGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFBEB.toInt() }
  private val paintGateSun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
  private val paintAshPrint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFB923C.toInt() }
  private val paintAshGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44F59E0B }
  private val paintResonanceAura = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x4438BDF8 }
  private val paintResonanceStone = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0EA5E9.toInt() }
  private val paintResonanceCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFBAE6FD.toInt() }
  private val paintEchoMote = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAA2DD4BF.toInt() }
  private val paintCrackPulse = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFFF59E0B.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
    strokeCap = Paint.Cap.ROUND
  }
  private val paintDrumBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB91C1C.toInt() }
  private val paintDrumHead = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFEF3C7.toInt() }
  private val paintDrumRope = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFFFBBF24.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }
  private val paintBeetleShell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F766E.toInt() }
  private val paintBeetleSpot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
  private val paintBeetleEye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFEF08A.toInt() }
  private val paintWindStream = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x552DD4BF
    style = Paint.Style.STROKE
    strokeWidth = 3f
    strokeCap = Paint.Cap.ROUND
  }
  private val paintWindFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1814B8A6 }
  private val paintPendulumCord = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF78350F.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 3.5f
  }
  private val paintPendulumArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x55F59E0B
    style = Paint.Style.STROKE
    strokeWidth = 1.8f
  }
  private val paintFlame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFB923C.toInt() }
  private val paintMarigold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
  private val paintBarkDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E2723.toInt() }
  private val paintCanopyDeep = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF14532D.toInt() }
  private val paintCanopyMid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF166534.toInt() }
  private val paintStalkerFur = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E1B4B.toInt() }
  private val paintStalkerMane = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF312E81.toInt() }
  private val paintStalkerEye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt() }
  private val paintDangerAura = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55EF4444 }
  private val paintGuardianStone = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334155.toInt() }
  private val paintGuardianEye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF97316.toInt() }
  private val paintCrimsonGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAEF4444.toInt() }
  private val paintHazardShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x997F1D1D.toInt() }
  private val paintTrapPlate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E293B.toInt() }
  private val paintTrapIron = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt() }
  private val paintOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF1C1917.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }
  private val paintTrollTeeth = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE2E8F0.toInt() }
  private val paintTrollCrimson = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEF4444.toInt() }
  private val paintTrollGhost = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF38BDF8.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }
  private val paintCursePurple = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA855F7.toInt() }
  private val paintCurseGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x99C084FC.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
  }
  private val paintWindCyan = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2838BDF8 }
  private val paintWindArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xCC7DD3FC.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.6f
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
  }
  private val paintCageIron = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF64748B.toInt() }
  private val paintPlankGold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD97706.toInt() }
  private val paintPhantomFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x4438BDF8 }

  // Pre-allocated geometric helpers
  private val rectTemp = RectF()
  private val subRectTemp = RectF()
  private val pathTemp = Path()

  fun drawTile(
      canvas: Canvas,
      tile: GameTile,
      cameraX: Float,
      cameraY: Float,
      animTick: Int,
      checkpointActive: Boolean = false
  ) {
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
      TileType.VANISHING_SLAB -> drawVanishingSlab(canvas, rectTemp, tile.isCrumbling, tile.crumbleTimerFrames, animTick)
      TileType.DISAPPEARING_FLOOR -> drawDisappearingFloor(canvas, rectTemp, animTick)
      TileType.BRAMBLE_SPIKES -> drawBrambleSpikes(canvas, rectTemp)
      TileType.CHECKPOINT_BELL -> drawCheckpointBell(canvas, rectTemp, animTick, checkpointActive)
      TileType.SUN_GATE -> drawSunGate(canvas, rectTemp, animTick)
      TileType.ECHO_BRIDGE,
      TileType.SOUND_GATE,
      TileType.FALSE_PATH_WALL,
      TileType.SLIPPERY_STONE -> expansionRenderer.drawExpansionTile(canvas, tile, rectTemp, animTick)
      else -> {
        canvas.drawRoundRect(rectTemp, 4f, 4f, paintStone)
        canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)
      }
    }
  }

  private fun drawVanishingSlab(
      canvas: Canvas,
      rect: RectF,
      isCrumbling: Boolean,
      crumbleTimerFrames: Int,
      tick: Int
  ) {
    val shakeOffset = if (isCrumbling) ((tick % 4) - 1.5f) * 2.0f else 0f
    subRectTemp.set(rect.left + shakeOffset, rect.top, rect.right + shakeOffset, rect.bottom)

    canvas.drawRoundRect(subRectTemp, 5f, 5f, paintStoneDark)
    canvas.drawLine(subRectTemp.left + 3f, subRectTemp.top + 3f, subRectTemp.right - 3f, subRectTemp.top + 3f, paintStoneLight)

    // Carved temple fracture lines
    pathTemp.reset()
    pathTemp.moveTo(subRectTemp.left + 12f, subRectTemp.top)
    pathTemp.lineTo(subRectTemp.centerX() - 4f, subRectTemp.centerY())
    pathTemp.lineTo(subRectTemp.left + 18f, subRectTemp.bottom)
    pathTemp.moveTo(subRectTemp.right - 14f, subRectTemp.top)
    pathTemp.lineTo(subRectTemp.centerX() + 6f, subRectTemp.centerY() + 3f)
    pathTemp.lineTo(subRectTemp.right - 16f, subRectTemp.bottom)
    canvas.drawPath(pathTemp, paintOutline)

    if (isCrumbling) {
      val urgency = (1f - (crumbleTimerFrames / 34f)).coerceIn(0f, 1f)
      val pulse = (0.5f + 0.5f * sin(tick * 0.55)).toFloat()
      paintCrackPulse.color = if (urgency > 0.55f) 0xFFEF4444.toInt() else 0xFFF59E0B.toInt()
      paintCrackPulse.alpha = ((135 + urgency * 120) * (0.6f + 0.4f * pulse)).toInt().coerceIn(90, 255)
      canvas.drawPath(pathTemp, paintCrackPulse)
      paintCrackPulse.color = 0xFFF59E0B.toInt()
      paintCrackPulse.alpha = 255
    } else {
      // Subtle cyan/amber warning glyphs on ancient unstable slabs
      paintRuneAmber.alpha = 180
      canvas.drawCircle(subRectTemp.centerX(), subRectTemp.centerY(), 6f, paintRuneAmber)
      paintRuneAmber.alpha = 255
    }

    canvas.drawRoundRect(subRectTemp, 5f, 5f, paintOutline)
  }

  private fun drawMossyTimber(canvas: Canvas, rect: RectF) {
    canvas.drawRoundRect(rect, 4f, 4f, paintWood)
    canvas.drawLine(rect.left + 4f, rect.top + 16f, rect.right - 4f, rect.top + 16f, paintWoodGrain)
    canvas.drawLine(rect.left + 8f, rect.top + 32f, rect.right - 8f, rect.top + 32f, paintWoodGrain)
    canvas.drawRoundRect(rect, 4f, 4f, paintOutline)

    subRectTemp.set(rect.left - 2f, rect.top - 3f, rect.right + 2f, rect.top + 10f)
    canvas.drawRoundRect(subRectTemp, 5f, 5f, paintMoss)
    canvas.drawCircle(rect.left + 12f, rect.top + 3f, 4f, paintMossLight)
    canvas.drawCircle(rect.left + 28f, rect.top + 2f, 5f, paintMossLight)
    canvas.drawCircle(rect.left + 40f, rect.top + 4f, 3.5f, paintMossLight)
  }

  private fun drawCarvedStone(canvas: Canvas, rect: RectF, tileId: Int) {
    canvas.drawRoundRect(rect, 4f, 4f, paintStone)
    canvas.drawLine(rect.left + 2f, rect.top + 2f, rect.right - 2f, rect.top + 2f, paintStoneLight)
    canvas.drawLine(rect.left + 2f, rect.bottom - 2f, rect.right - 2f, rect.bottom - 2f, paintStoneDark)
    canvas.drawRoundRect(rect, 4f, 4f, paintOutline)

    if (rect.height() >= 32f) {
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
  }

  private fun drawCrumblingWood(
      canvas: Canvas,
      rect: RectF,
      isCrumbling: Boolean,
      crumbleTimerFrames: Int,
      tick: Int
  ) {
    val shakeOffset = if (isCrumbling) ((tick % 4) - 2) * 1.5f else 0f
    subRectTemp.set(rect.left + shakeOffset, rect.top, rect.right + shakeOffset, rect.bottom)

    canvas.drawRoundRect(subRectTemp, 3f, 3f, paintWood)

    pathTemp.reset()
    pathTemp.moveTo(subRectTemp.left + 14f, subRectTemp.top)
    pathTemp.lineTo(subRectTemp.left + 20f, subRectTemp.centerY())
    pathTemp.lineTo(subRectTemp.left + 16f, subRectTemp.bottom)

    pathTemp.moveTo(subRectTemp.right - 18f, subRectTemp.top)
    pathTemp.lineTo(subRectTemp.right - 12f, subRectTemp.centerY() + 4f)
    pathTemp.lineTo(subRectTemp.right - 22f, subRectTemp.bottom)
    canvas.drawPath(pathTemp, paintWoodGrain)

    if (isCrumbling) {
      val urgency = (1f - (crumbleTimerFrames / 45f)).coerceIn(0f, 1f)
      val pulse = (0.5f + 0.5f * sin(tick * 0.45)).toFloat()
      paintCrackPulse.alpha = ((110 + urgency * 100) * (0.6f + 0.4f * pulse)).toInt().coerceIn(80, 255)
      canvas.drawPath(pathTemp, paintCrackPulse)
      paintCrackPulse.alpha = 255
    }

    canvas.drawRoundRect(subRectTemp, 3f, 3f, paintOutline)
  }

  private fun drawDisappearingFloor(canvas: Canvas, rect: RectF, tick: Int) {
    val alphaPulse = (180 + (sin(tick * 0.12) * 50).toInt()).coerceIn(100, 255)
    paintStoneLight.alpha = alphaPulse
    canvas.drawRoundRect(rect, 4f, 4f, paintStoneLight)
    canvas.drawRoundRect(rect, 4f, 4f, paintOutline)

    paintRuneGlow.alpha = alphaPulse
    canvas.drawCircle(rect.centerX(), rect.centerY(), 8f, paintRuneGlow)
    paintStoneLight.alpha = 255
    paintRuneGlow.alpha = 255
  }

  private fun drawBrambleSpikes(canvas: Canvas, rect: RectF) {
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
      canvas.drawCircle(sx + spikeWidth * 0.5f, rect.top + 3f, 2.5f, paintSpikeTip)
    }
  }

  fun drawMovingPlatform(canvas: Canvas, mp: MovingPlatform, cameraX: Float, cameraY: Float) {
    val drawX = mp.x - cameraX
    val drawY = mp.y - cameraY
    rectTemp.set(drawX, drawY, drawX + mp.width, drawY + mp.height)

    canvas.drawRoundRect(rectTemp, 6f, 6f, paintStone)
    canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)

    subRectTemp.set(drawX, drawY - 2f, drawX + mp.width, drawY + 8f)
    canvas.drawRoundRect(subRectTemp, 4f, 4f, paintMoss)

    canvas.drawCircle(drawX + 16f, drawY + 12f, 4f, paintCoinOuter)
    canvas.drawCircle(drawX + mp.width - 16f, drawY + 12f, 4f, paintCoinOuter)
  }

  private fun drawCheckpointBell(canvas: Canvas, rect: RectF, tick: Int, checkpointActive: Boolean) {
    val postWidth = 10f
    val postLeft = rect.centerX() - postWidth / 2
    canvas.drawRect(postLeft, rect.top, postLeft + postWidth, rect.bottom, paintBellPost)
    canvas.drawRect(postLeft, rect.top, postLeft + postWidth, rect.bottom, paintOutline)

    // Engraved Clue 3: Checkpoint Bell Glyph on post
    val glyphY = rect.centerY() + 10f
    paintRuneGlow.alpha = if (checkpointActive) 255 else 110
    canvas.drawCircle(rect.centerX(), glyphY, 8f, paintRuneGlow)
    canvas.drawCircle(rect.centerX(), glyphY, 3.5f, paintRuneGlow)
    paintRuneGlow.alpha = 255

    // Lantern Glow
    val lanternY = rect.top + 16f
    canvas.drawCircle(postLeft - 8f, lanternY, if (checkpointActive) 18f else 13f, paintLanternGlow)
    canvas.drawCircle(postLeft - 8f, lanternY, 5f, paintCoinInner)

    // Brass Bell (subtle idle sway)
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
    canvas.drawCircle(bellX, bellY + 16f, 3f, paintCoinOuter)

    // Small festive marigold toran blossoms along shrine crossbar
    for (m in 0..2) {
      canvas.drawCircle(rect.centerX() - 10f + m * 10f, rect.top + 4f + (m % 2) * 2f, 3.2f, paintMarigold)
    }
  }

  private fun drawSunGate(canvas: Canvas, rect: RectF, tick: Int) {
    canvas.drawRoundRect(rect, 24f, 24f, paintGateStone)
    canvas.drawRoundRect(rect, 24f, 24f, paintOutline)

    // Concentric zero-allocation swirling portal glow
    subRectTemp.set(rect.left + 14f, rect.top + 22f, rect.right - 14f, rect.bottom - 4f)
    canvas.drawRoundRect(subRectTemp, 18f, 18f, paintGateOuterGlow)
    subRectTemp.set(rect.left + 22f, rect.top + 32f, rect.right - 22f, rect.bottom - 12f)
    canvas.drawRoundRect(subRectTemp, 14f, 14f, paintGateMidGlow)
    val pulse = (sin(tick * 0.1) * 3f).toFloat()
    subRectTemp.set(rect.left + 30f - pulse, rect.top + 44f - pulse, rect.right - 30f + pulse, rect.bottom - 22f + pulse)
    canvas.drawRoundRect(subRectTemp, 10f, 10f, paintGateCoreGlow)

    val crestX = rect.centerX()
    val crestY = rect.top + 8f
    canvas.drawCircle(crestX, crestY, 12f, paintGateSun)
    canvas.drawCircle(crestX, crestY, 12f, paintOutline)

    for (i in 0..7) {
      val angle = (tick * 0.03 + i * (PI / 4)).toFloat()
      val rx1 = crestX + cos(angle.toDouble()).toFloat() * 14f
      val ry1 = crestY + sin(angle.toDouble()).toFloat() * 14f
      val rx2 = crestX + cos(angle.toDouble()).toFloat() * 20f
      val ry2 = crestY + sin(angle.toDouble()).toFloat() * 20f
      canvas.drawLine(rx1, ry1, rx2, ry2, paintGateSun)
    }

    // Marigold blossoms framing the ancient Sun Gate arch
    for (i in 0..4) {
      val gx = rect.left + 14f + i * ((rect.width() - 28f) / 4f)
      val sway = (sin(tick * 0.06 + i) * 1.5f).toFloat()
      canvas.drawCircle(gx, rect.top + 24f + sway, 3.6f, paintMarigold)
    }
  }

  fun drawClueMarker(
      canvas: Canvas,
      marker: ClueMarker,
      cameraX: Float,
      cameraY: Float,
      animTick: Int,
      isDiscovered: Boolean
  ) {
    val drawX = marker.x - cameraX
    val drawY = marker.y - cameraY
    if (drawX < -96f || drawX > canvas.width + 96f) return

    when (marker.clueId) {
      StoryClueId.ASH_PRINTS -> {
        val pulseAlpha = if (isDiscovered) 90 else (150 + (sin(animTick * 0.1) * 70).toInt()).coerceIn(80, 230)
        paintAshGlow.alpha = pulseAlpha / 3
        paintAshPrint.alpha = pulseAlpha
        for (i in 0..2) {
          val fx = drawX + i * 16f
          val fy = drawY - 3f + (if (i % 2 == 0) -1.5f else 1.5f)
          canvas.drawCircle(fx, fy, 7f, paintAshGlow)
          subRectTemp.set(fx - 5f, fy - 2.5f, fx + 5f, fy + 2.5f)
          canvas.drawOval(subRectTemp, paintAshPrint)
        }
        paintAshGlow.alpha = 68
        paintAshPrint.alpha = 255
      }
      StoryClueId.ANCIENT_CARVING -> {
        // Weathered stele behind Aarav on Overlook Ruins (Tile 98)
        subRectTemp.set(drawX + 6f, drawY - 58f, drawX + 42f, drawY)
        canvas.drawRoundRect(subRectTemp, 8f, 8f, paintStoneDark)
        canvas.drawRoundRect(subRectTemp, 8f, 8f, paintOutline)
        // Carved upward-sun relief
        val cx = subRectTemp.centerX()
        val cy = subRectTemp.centerY() - 6f
        paintRuneAmber.alpha = if (isDiscovered) 255 else (170 + (sin(animTick * 0.08) * 70).toInt()).coerceIn(110, 255)
        canvas.drawCircle(cx, cy, 8f, paintRuneAmber)
        pathTemp.reset()
        pathTemp.moveTo(cx, cy - 16f)
        pathTemp.lineTo(cx - 6f, cy - 10f)
        pathTemp.moveTo(cx, cy - 16f)
        pathTemp.lineTo(cx + 6f, cy - 10f)
        canvas.drawPath(pathTemp, paintRuneAmber)
        paintRuneAmber.alpha = 255
      }
      else -> Unit
    }
  }

  fun drawResonanceStone(
      canvas: Canvas,
      stone: ResonanceStone,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    val drawX = stone.x - cameraX
    val drawY = stone.y - cameraY + (sin(stone.animTimer.toDouble()) * 4.0).toFloat()
    if (drawX < -64f || drawX > canvas.width + 64f) return

    if (stone.isCollected) {
      // Attuned resting pedestal ring (subtle reminder it was gathered)
      paintResonanceAura.alpha = 40
      canvas.drawCircle(drawX, stone.y - cameraY + 12f, 10f, paintResonanceAura)
      paintResonanceAura.alpha = 68
      return
    }

    val auraRadius = 24f + (sin(animTick * 0.1) * 3f).toFloat()
    canvas.drawCircle(drawX, drawY, auraRadius, paintResonanceAura)

    // Diamond crystal shape
    pathTemp.reset()
    pathTemp.moveTo(drawX, drawY - 14f)
    pathTemp.lineTo(drawX + 11f, drawY)
    pathTemp.lineTo(drawX, drawY + 14f)
    pathTemp.lineTo(drawX - 11f, drawY)
    pathTemp.close()
    canvas.drawPath(pathTemp, paintResonanceStone)
    canvas.drawPath(pathTemp, paintOutline)
    canvas.drawCircle(drawX, drawY, 4.5f, paintResonanceCore)
  }

  fun drawEchoPuzzleNode(
      canvas: Canvas,
      node: EchoPuzzleNode,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    val drawX = node.x - cameraX
    val drawY = node.y - cameraY
    if (drawX < -64f || drawX > canvas.width + 420f) return

    // Ground rune plate on Tile 79
    subRectTemp.set(drawX + 6f, drawY - 6f, drawX + 42f, drawY + 2f)
    canvas.drawRoundRect(subRectTemp, 3f, 3f, if (node.isActivated) paintResonanceStone else paintStoneLight)
    canvas.drawRoundRect(subRectTemp, 3f, 3f, paintOutline)

    // When activated (temporary puzzle state), draw gentle guiding motes across the moving platform gap
    if (node.isActivated) {
      for (i in 0..4) {
        val mx = (node.x + 110f + i * 76f) - cameraX
        val my = (node.y - 54f + sin((animTick * 0.08 + i * 0.9)).toFloat() * 8f) - cameraY
        canvas.drawCircle(mx, my, 4.5f, paintEchoMote)
      }
    }
  }

  fun drawCoin(canvas: Canvas, coin: Coin, cameraX: Float, cameraY: Float) {
    if (coin.isCollected) return
    val drawX = coin.x - cameraX
    val drawY = coin.y - cameraY

    val spin = cos(coin.animTimer.toDouble()).toFloat()
    val scaleX = Math.abs(spin).coerceAtLeast(0.15f)

    canvas.save()
    canvas.translate(drawX, drawY)
    canvas.scale(scaleX, 1f)

    canvas.drawCircle(0f, 0f, coin.radius, paintCoinOuter)
    canvas.drawCircle(0f, 0f, coin.radius, paintOutline)
    canvas.drawCircle(0f, 0f, coin.radius * 0.72f, paintCoinInner)

    if (scaleX > 0.4f) {
      canvas.drawCircle(0f, 0f, 3f, paintCoinStar)
    }

    canvas.restore()
  }

  fun drawBounceDrum(canvas: Canvas, drum: BounceDrum, cameraX: Float, cameraY: Float, animTick: Int) {
    val drawX = drum.x - cameraX
    val drawY = drum.y - cameraY
    if (drawX + drum.width < -64f || drawX > canvas.width + 64f) return

    val compress = if (drum.squashFrames > 0) (drum.squashFrames / 14f) * 7f else 0f
    rectTemp.set(drawX - compress * 0.4f, drawY + compress, drawX + drum.width + compress * 0.4f, drawY + drum.height)
    canvas.drawRoundRect(rectTemp, 8f, 8f, paintDrumBody)
    canvas.drawRoundRect(rectTemp, 8f, 8f, paintOutline)

    // Stretched cream drumhead + glowing upward bounce ring
    subRectTemp.set(rectTemp.left + 4f, rectTemp.top - 3f, rectTemp.right - 4f, rectTemp.top + 6f)
    canvas.drawRoundRect(subRectTemp, 4f, 4f, paintDrumHead)
    canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)

    // Traditional Dholak rope zig-zag lacing
    pathTemp.reset()
    val midY = (rectTemp.top + rectTemp.bottom) / 2f + 2f
    pathTemp.moveTo(rectTemp.left + 8f, rectTemp.top + 6f)
    pathTemp.lineTo(rectTemp.left + 17f, rectTemp.bottom - 2f)
    pathTemp.lineTo(rectTemp.left + 26f, rectTemp.top + 6f)
    pathTemp.lineTo(rectTemp.left + 35f, rectTemp.bottom - 2f)
    pathTemp.lineTo(rectTemp.left + 44f, rectTemp.top + 6f)
    canvas.drawPath(pathTemp, paintDrumRope)

    val arrowPulse = (sin(animTick * 0.14) * 2.5f).toFloat()
    val cx = rectTemp.centerX()
    val ay = rectTemp.top - 10f + arrowPulse
    pathTemp.reset()
    pathTemp.moveTo(cx - 6f, ay + 4f)
    pathTemp.lineTo(cx, ay - 2f)
    pathTemp.lineTo(cx + 6f, ay + 4f)
    canvas.drawPath(pathTemp, paintRuneAmber)
    canvas.drawCircle(cx, midY - 12f, 2f, paintMarigold)
  }

  fun drawCritter(canvas: Canvas, critter: ForestCritter, cameraX: Float, cameraY: Float) {
    val drawX = critter.x - cameraX
    val drawY = critter.y - cameraY
    if (drawX + critter.width < -64f || drawX > canvas.width + 64f) return

    val isStunned = critter.stunFrames > 0
    val isLunging = critter.lungeFrames > 0 || critter.alertFrames > 0
    val bob = if (isStunned) 4f else (sin(critter.walkTick * 0.28) * 1.6f).toFloat()
    rectTemp.set(drawX, drawY + bob, drawX + critter.width, drawY + critter.height)

    // Lunging / alert warning halo
    if (!isStunned && isLunging) {
      paintDangerAura.alpha = 140
      canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY(), critter.width * 0.68f, paintDangerAura)
      paintDangerAura.alpha = 85
    }

    // Dome beetle carapace (visually differentiated by behavior)
    val shellColor = when (critter.behavior) {
      CritterBehavior.PATROL -> 0xFF0F766E.toInt()
      CritterBehavior.LUNGE_ON_PROXIMITY -> if (isLunging) 0xFFB91C1C.toInt() else 0xFF7C2D12.toInt()
      CritterBehavior.HOPPING -> 0xFF4D7C0F.toInt()
    }
    paintBeetleShell.color = shellColor
    canvas.drawRoundRect(rectTemp, 12f, 12f, paintBeetleShell)
    canvas.drawRoundRect(rectTemp, 12f, 12f, paintOutline)
    paintBeetleShell.color = 0xFF0F766E.toInt()

    // Golden horn / dorsal markings
    val spotColor = when (critter.behavior) {
      CritterBehavior.LUNGE_ON_PROXIMITY -> 0xFFFCA5A5.toInt()
      CritterBehavior.HOPPING -> 0xFFBEF264.toInt()
      else -> 0xFFF59E0B.toInt()
    }
    paintBeetleSpot.color = spotColor
    canvas.drawCircle(rectTemp.centerX() - 6f, rectTemp.centerY() - 2f, 3.2f, paintBeetleSpot)
    canvas.drawCircle(rectTemp.centerX() + 5f, rectTemp.centerY() - 3f, 2.8f, paintBeetleSpot)
    paintBeetleSpot.color = 0xFFF59E0B.toInt()

    val eyeX = if (critter.vx >= 0f) rectTemp.right - 8f else rectTemp.left + 8f
    canvas.drawCircle(eyeX, rectTemp.centerY(), 2.8f, if (isLunging) paintFlame else paintBeetleEye)

    // Telegraph alert glint above lunging beetle
    if (!isStunned && critter.alertFrames > 0) {
      canvas.drawCircle(rectTemp.centerX(), rectTemp.top - 10f, 3.5f, paintFlame)
    }

    // Pattering legs
    val stride = if (isStunned) 0f else (sin(critter.walkTick * 0.45) * 3f).toFloat()
    canvas.drawLine(rectTemp.left + 8f, rectTemp.bottom, rectTemp.left + 6f + stride, rectTemp.bottom + 4f, paintOutline)
    canvas.drawLine(rectTemp.right - 8f, rectTemp.bottom, rectTemp.right - 6f - stride, rectTemp.bottom + 4f, paintOutline)

    // Dizzy stars if stomped/stunned
    if (isStunned) {
      for (i in 0..2) {
        val ang = critter.walkTick * 0.14 + i * (2.0 * PI / 3.0)
        val sx = rectTemp.centerX() + (cos(ang) * 14.0).toFloat()
        val sy = rectTemp.top - 7f + (sin(ang) * 4.0).toFloat()
        canvas.drawCircle(sx, sy, 2.5f, paintCoinInner)
      }
    }
  }

  fun drawFallingHazard(
      canvas: Canvas,
      hazard: FallingHazard,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    if (!hazard.isWarning && !hazard.isFalling) return
    val hx = hazard.x - cameraX
    val gy = hazard.groundY - cameraY
    val hy = hazard.y - cameraY
    if (hx < -96f || hx > canvas.width + 96f) return

    // Telegraphed ground target shadow ring
    val progress = if (hazard.isWarning) {
      (1f - (hazard.warningFrames.toFloat() / hazard.warningDurationFrames.coerceAtLeast(1))).coerceIn(0f, 1f)
    } else {
      ((hazard.y - hazard.startY) / (hazard.groundY - hazard.startY).coerceAtLeast(1f)).coerceIn(0.2f, 1f)
    }
    val shadowR = hazard.radius * (0.75f + 0.55f * progress)
    paintHazardShadow.alpha = (110 + (progress * 115).toInt()).coerceIn(90, 225)
    subRectTemp.set(hx - shadowR, gy - 6f, hx + shadowR, gy + 5f)
    canvas.drawOval(subRectTemp, paintHazardShadow)

    val pulse = (0.5f + 0.5f * sin(animTick * 0.55)).toFloat()
    paintCrimsonGlow.alpha = ((120 + 110 * pulse)).toInt().coerceIn(80, 240)
    canvas.drawOval(subRectTemp, paintRuneAmber)

    // Subtle vertical fall guideline during warning & descent
    paintCrackPulse.color = if (hazard.isFalling) 0xFFEF4444.toInt() else 0xFFF59E0B.toInt()
    paintCrackPulse.alpha = if (hazard.isWarning) (110 + (80 * pulse).toInt()) else 85
    canvas.drawLine(hx, hy + hazard.radius, hx, gy - 4f, paintCrackPulse)
    paintCrackPulse.color = 0xFFF59E0B.toInt()
    paintCrackPulse.alpha = 255

    // Trembling canopy boulder / temple keystone
    val jitter = if (hazard.isWarning) (sin(animTick * 1.6) * 2.5f).toFloat() else 0f
    val drawX = hx + jitter
    val r = hazard.radius
    pathTemp.reset()
    pathTemp.moveTo(drawX, hy - r)
    pathTemp.lineTo(drawX + r * 0.92f, hy - r * 0.25f)
    pathTemp.lineTo(drawX + r * 0.72f, hy + r * 0.9f)
    pathTemp.lineTo(drawX - r * 0.72f, hy + r * 0.9f)
    pathTemp.lineTo(drawX - r * 0.92f, hy - r * 0.25f)
    pathTemp.close()
    canvas.drawPath(pathTemp, paintStoneDark)
    canvas.drawPath(pathTemp, paintOutline)
    canvas.drawCircle(drawX, hy, r * 0.34f, paintFlame)
  }

  fun drawAmbushTrap(
      canvas: Canvas,
      trap: AmbushSpikeTrap,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    val tx = trap.x - cameraX
    val gy = trap.groundY - cameraY
    if (tx + trap.width < -64f || tx > canvas.width + 64f) return

    // Carved floor housing slit
    subRectTemp.set(tx, gy - 5f, tx + trap.width, gy + 4f)
    canvas.drawRoundRect(subRectTemp, 3f, 3f, paintTrapPlate)
    canvas.drawRoundRect(subRectTemp, 3f, 3f, paintOutline)

    if (trap.phase == TrapPhase.WARNING) {
      val pulse = (0.5f + 0.5f * sin(animTick * 0.65)).toFloat()
      paintCrimsonGlow.alpha = (130 + (120 * pulse)).toInt().coerceIn(100, 250)
      subRectTemp.set(tx + 3f, gy - 7f, tx + trap.width - 3f, gy + 1f)
      canvas.drawRoundRect(subRectTemp, 2f, 2f, paintCrimsonGlow)

      // Peeking warning tips so the player gets a crystal-clear visual cue
      val sw = trap.width / 3f
      for (i in 0..2) {
        val sx = tx + i * sw
        pathTemp.reset()
        pathTemp.moveTo(sx + 4f, gy - 2f)
        pathTemp.lineTo(sx + sw * 0.5f, gy - 9f)
        pathTemp.lineTo(sx + sw - 4f, gy - 2f)
        pathTemp.close()
        canvas.drawPath(pathTemp, paintFlame)
      }
      return
    }

    if (trap.currentHeight > 1f) {
      val sw = trap.width / 3f
      for (i in 0..2) {
        val sx = tx + i * sw
        pathTemp.reset()
        pathTemp.moveTo(sx + 2f, gy)
        pathTemp.lineTo(sx + sw * 0.5f, gy - trap.currentHeight)
        pathTemp.lineTo(sx + sw - 2f, gy)
        pathTemp.close()
        canvas.drawPath(pathTemp, paintTrapIron)
        canvas.drawPath(pathTemp, paintOutline)
        canvas.drawCircle(sx + sw * 0.5f, gy - trap.currentHeight + 2.5f, 2.4f, paintSpikeTip)
      }
    }
  }

  fun drawWindDraft(canvas: Canvas, draft: WindDraft, cameraX: Float, cameraY: Float, animTick: Int) {
    val drawX = draft.x - cameraX
    val drawY = draft.y - cameraY
    if (drawX + draft.width < -64f || drawX > canvas.width + 64f) return

    rectTemp.set(drawX, drawY, drawX + draft.width, drawY + draft.height)
    canvas.drawRoundRect(rectTemp, 20f, 20f, paintWindFill)

    for (col in 0..2) {
      val baseX = drawX + draft.width * (0.22f + col * 0.28f)
      val scrollOffset = ((animTick * 3 + col * 40) % draft.height.toInt()).toFloat()
      pathTemp.reset()
      val segTop = drawY + draft.height - scrollOffset
      val segBottom = (segTop + 52f).coerceAtMost(drawY + draft.height)
      val segStart = segTop.coerceAtLeast(drawY)
      if (segBottom > segStart) {
        val wave = (sin(animTick * 0.1 + col) * 7f).toFloat()
        pathTemp.moveTo(baseX, segBottom)
        pathTemp.quadTo(baseX + wave, (segStart + segBottom) / 2f, baseX - wave * 0.5f, segStart)
        canvas.drawPath(pathTemp, paintWindStream)
      }
    }
  }

  fun drawPendulum(canvas: Canvas, pendulum: SwingingPendulum, cameraX: Float, cameraY: Float) {
    val ax = pendulum.anchorX - cameraX
    val ay = pendulum.anchorY - cameraY
    val bx = pendulum.bobX - cameraX
    val by = pendulum.bobY - cameraY
    if (ax < -240f || ax > canvas.width + 240f) return

    // Telegraphed swing arc guideline
    val degSpan = Math.toDegrees(pendulum.maxAngleRad.toDouble()).toFloat()
    rectTemp.set(ax - pendulum.length, ay - pendulum.length, ax + pendulum.length, ay + pendulum.length)
    canvas.drawArc(rectTemp, 90f - degSpan, degSpan * 2f, false, paintPendulumArc)

    // Anchor bracket & vine cord
    canvas.drawCircle(ax, ay, 6f, paintBellPost)
    canvas.drawLine(ax, ay, bx, by, paintPendulumCord)

    // Heavy Brass Temple Ghanta bob
    canvas.drawCircle(bx, by, pendulum.bobRadius, paintGateSun)
    canvas.drawCircle(bx, by, pendulum.bobRadius, paintOutline)
    canvas.drawCircle(bx, by, pendulum.bobRadius * 0.5f, paintCoinInner)
  }

  fun drawDiya(canvas: Canvas, diya: ForestDiya, cameraX: Float, cameraY: Float, animTick: Int) {
    val drawX = diya.x - cameraX
    val drawY = diya.y - cameraY
    if (drawX < -64f || drawX > canvas.width + 64f) return

    val floatY = drawY + (sin(animTick * 0.09 + diya.id) * 3f).toFloat()
    if (diya.isLit) {
      paintLanternGlow.alpha = 135
      canvas.drawCircle(drawX, floatY - 6f, 26f, paintLanternGlow)
      paintLanternGlow.alpha = 102
    }

    // Brass Diya saucer
    pathTemp.reset()
    pathTemp.moveTo(drawX - 13f, floatY)
    pathTemp.quadTo(drawX, floatY + 12f, drawX + 13f, floatY)
    pathTemp.close()
    canvas.drawPath(pathTemp, paintBell)
    canvas.drawPath(pathTemp, paintOutline)

    // Wick flame (warm amber/gold when lit, subtle teal bud when unlit)
    val flicker = (sin(animTick * 0.3 + diya.id) * 1.8f).toFloat()
    pathTemp.reset()
    pathTemp.moveTo(drawX - 4f, floatY)
    pathTemp.quadTo(drawX + flicker, floatY - 15f, drawX + 4f, floatY)
    pathTemp.close()
    canvas.drawPath(pathTemp, if (diya.isLit) paintFlame else paintResonanceStone)
    if (diya.isLit) {
      canvas.drawCircle(drawX + flicker * 0.4f, floatY - 5f, 2.5f, paintCoinInner)
    }
  }

  // --- SET PIECE A: AWAKENING BANYAN TREE ---

  fun drawAwakeningTree(
      canvas: Canvas,
      tree: AwakeningTreeState,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    val trunkX = tree.trunkX - cameraX
    val groundY = tree.groundY - cameraY
    if (trunkX < -320f || trunkX > canvas.width + 320f) return

    val jitterX = if (tree.phase == TreePhase.WARNING) (sin(animTick * 1.3) * 2.5f).toFloat() else 0f
    val tx = trunkX + jitterX

    // Gnarled ancient banyan trunk in the background
    rectTemp.set(tx - 46f, groundY - 205f, tx + 46f, groundY)
    canvas.drawRoundRect(rectTemp, 18f, 18f, paintBarkDark)
    canvas.drawRoundRect(rectTemp, 18f, 18f, paintOutline)

    // Twisted buttress hollows & hanging aerial roots
    canvas.drawLine(tx - 24f, groundY - 165f, tx - 32f, groundY - 30f, paintWood)
    canvas.drawLine(tx + 22f, groundY - 155f, tx + 28f, groundY - 20f, paintWood)

    // Lush layered banyan canopy crown
    val sway = (sin(animTick * 0.04) * 4f).toFloat()
    canvas.drawCircle(tx - 64f + sway, groundY - 186f, 52f, paintCanopyDeep)
    canvas.drawCircle(tx + 64f + sway, groundY - 182f, 54f, paintCanopyDeep)
    canvas.drawCircle(tx + sway * 0.6f, groundY - 216f, 66f, paintCanopyMid)
    canvas.drawCircle(tx - 26f + sway, groundY - 202f, 36f, paintMoss)
    canvas.drawCircle(tx + 32f + sway, groundY - 198f, 34f, paintMoss)

    // Awakening forest spirit eyes on trunk when stirring or active
    if (tree.phase == TreePhase.WARNING || tree.phase == TreePhase.ACTIVE) {
      val glowAlpha = (160 + (sin(animTick * 0.2) * 80).toInt()).coerceIn(100, 255)
      paintStalkerEye.alpha = glowAlpha
      canvas.drawCircle(tx - 14f, groundY - 134f, 4.5f, paintStalkerEye)
      canvas.drawCircle(tx + 14f, groundY - 134f, 4.5f, paintStalkerEye)
      paintStalkerEye.alpha = 255
    }

    // Ground-tremor telegraph rings before roots erupt
    if (tree.phase == TreePhase.WARNING) {
      val pulseR = 14f + (sin(animTick * 0.45) * 6f).toFloat()
      for (root in tree.roots) {
        val rx = root.x + root.width / 2f - cameraX
        val ry = root.baseY - cameraY - 4f
        paintCrackPulse.color = 0xFFF59E0B.toInt()
        paintCrackPulse.alpha = 220
        subRectTemp.set(rx - pulseR, ry - 6f, rx + pulseR, ry + 6f)
        canvas.drawOval(subRectTemp, paintCrackPulse)
      }
      paintCrackPulse.alpha = 255
    }

    // Emerging solid climbable root pillars
    for (root in tree.roots) {
      if (root.currentHeight <= 1f) continue
      val rx = root.x - cameraX
      val rBottom = root.baseY - cameraY
      val rTop = rBottom - root.currentHeight
      rectTemp.set(rx, rTop, rx + root.width, rBottom)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintWood)
      canvas.drawLine(rx + root.width * 0.35f, rTop + 6f, rx + root.width * 0.35f, rBottom - 4f, paintBarkDark)
      canvas.drawLine(rx + root.width * 0.68f, rTop + 10f, rx + root.width * 0.68f, rBottom - 4f, paintBarkDark)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)

      // Mossy footholds on root top so players clearly read steppable platform top
      subRectTemp.set(rx - 2f, rTop - 2f, rx + root.width + 2f, rTop + 8f)
      canvas.drawRoundRect(subRectTemp, 4f, 4f, paintMossLight)
      canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)
    }

    // Telegraphed falling canopy acorns
    for (acorn in tree.acorns) {
      if (!acorn.active) continue
      val ax = acorn.x - cameraX
      val ay = acorn.y - cameraY
      // Subtle vertical drop telegraph & ground target dot
      paintCrackPulse.alpha = 110
      canvas.drawLine(ax, ay + acorn.radius, ax, groundY, paintCrackPulse)
      paintCrackPulse.alpha = 255
      canvas.drawCircle(ax, ay, acorn.radius, paintBell)
      canvas.drawCircle(ax, ay, acorn.radius, paintOutline)
      subRectTemp.set(ax - acorn.radius - 1f, ay - acorn.radius - 3f, ax + acorn.radius + 1f, ay - 1f)
      canvas.drawRoundRect(subRectTemp, 4f, 4f, paintBarkDark)
    }
  }

  // --- SET PIECE B: VANAR SHADOW STALKER ---

  fun drawForestStalker(
      canvas: Canvas,
      stalker: ForestStalkerState,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    if (stalker.phase == StalkerPhase.IDLE || stalker.phase == StalkerPhase.REPELLED) return

    val sx = stalker.x - cameraX
    val sy = stalker.y - cameraY
    if (sx + stalker.width < -100f || sx > canvas.width + 100f) return

    val isWarning = stalker.phase == StalkerPhase.WARNING
    val crouch = if (isWarning) 6f else (sin(stalker.strideTick * 0.45) * 3.5f).toFloat()

    // Threat aura
    paintDangerAura.alpha = if (isWarning) 110 else 75
    canvas.drawCircle(sx + stalker.width / 2f, sy + stalker.height / 2f, 38f, paintDangerAura)
    paintDangerAura.alpha = 85

    // Swishing shadow tail
    val tailWave = (sin(stalker.strideTick * 0.3) * 10f).toFloat()
    pathTemp.reset()
    pathTemp.moveTo(sx + 6f, sy + stalker.height * 0.55f + crouch)
    pathTemp.quadTo(sx - 18f, sy + stalker.height * 0.25f + tailWave, sx - 24f, sy + 6f + tailWave)
    canvas.drawPath(pathTemp, paintStalkerMane)

    // Prowling hunched torso
    rectTemp.set(sx, sy + crouch, sx + stalker.width, sy + stalker.height)
    canvas.drawRoundRect(rectTemp, 16f, 16f, paintStalkerFur)
    canvas.drawRoundRect(rectTemp, 16f, 16f, paintOutline)

    // Spiked shoulder crest & head facing right toward Aarav
    val headX = sx + stalker.width - 10f
    val headY = sy + 11f + crouch
    canvas.drawCircle(headX, headY, 13f, paintStalkerMane)
    canvas.drawCircle(headX, headY, 13f, paintOutline)

    // Glowing amber predator eyes
    canvas.drawCircle(headX + 5f, headY - 2f, 3.2f, paintStalkerEye)
    canvas.drawCircle(headX - 1f, headY - 3f, 2.5f, paintStalkerEye)

    // Bounding legs
    val stride = if (isWarning) 0f else (sin(stalker.strideTick * 0.55) * 8f).toFloat()
    canvas.drawLine(sx + 12f, sy + stalker.height - 2f, sx + 8f - stride, sy + stalker.height + 5f, paintOutline)
    canvas.drawLine(sx + stalker.width - 12f, sy + stalker.height - 2f, sx + stalker.width - 6f + stride, sy + stalker.height + 5f, paintOutline)
  }

  // --- SET PIECE D: SHILA-RAKSHAK STONE GUARDIAN & SUN CROWN CRYSTAL ---

  fun drawStoneGuardian(
      canvas: Canvas,
      guardian: StoneGuardianState,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    val gx = guardian.x - cameraX
    val groundY = guardian.groundY - cameraY
    if (gx + guardian.width < -220f || gx > canvas.width + 220f) return

    if (guardian.phase == GuardianPhase.DEFEATED) {
      // Shattered peaceful base pedestal + cyan harmony wisps
      rectTemp.set(gx + 6f, groundY - 24f, gx + guardian.width - 6f, groundY)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintGuardianStone)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)
      paintRuneGlow.alpha = 180
      canvas.drawCircle(gx + guardian.width / 2f, groundY - 34f, 12f, paintRuneGlow)
      paintRuneGlow.alpha = 255
      return
    }

    val jitterX = if (guardian.phase == GuardianPhase.AWAKENING) (sin(animTick * 1.4) * 3f).toFloat() else 0f
    val bodyX = gx + jitterX
    val bodyTop = groundY - guardian.height

    // Temple Boss Vajra-Surya golden sanctum aura vs forest moss
    if (guardian.isTempleBoss) {
      paintGateOuterGlow.alpha = 95
      canvas.drawCircle(bodyX + guardian.width / 2f, bodyTop + guardian.height * 0.45f, 58f, paintGateOuterGlow)
      paintGateOuterGlow.alpha = 255
      rectTemp.set(bodyX, bodyTop, bodyX + guardian.width, groundY)
      canvas.drawRoundRect(rectTemp, 12f, 12f, paintGateStone)
      canvas.drawRoundRect(rectTemp, 12f, 12f, paintOutline)
      subRectTemp.set(bodyX + 6f, bodyTop + 10f, bodyX + guardian.width - 6f, bodyTop + 34f)
      canvas.drawRoundRect(subRectTemp, 6f, 6f, paintGateSun)
      subRectTemp.set(bodyX + 10f, bodyTop - 6f, bodyX + guardian.width - 10f, bodyTop + 6f)
      canvas.drawRoundRect(subRectTemp, 4f, 4f, paintCoinInner)
    } else {
      // Monolithic torso & carved shoulder plinths
      rectTemp.set(bodyX, bodyTop, bodyX + guardian.width, groundY)
      canvas.drawRoundRect(rectTemp, 12f, 12f, paintGuardianStone)
      canvas.drawRoundRect(rectTemp, 12f, 12f, paintOutline)

      subRectTemp.set(bodyX + 6f, bodyTop + 10f, bodyX + guardian.width - 6f, bodyTop + 34f)
      canvas.drawRoundRect(subRectTemp, 6f, 6f, paintStoneLight)

      // Moss patches on dormant/awakened colossus
      subRectTemp.set(bodyX - 2f, bodyTop - 3f, bodyX + guardian.width * 0.55f, bodyTop + 9f)
      canvas.drawRoundRect(subRectTemp, 4f, 4f, paintMoss)
    }

    // Guardian eyes
    if (guardian.phase == GuardianPhase.SLEEPING) {
      canvas.drawLine(bodyX + 14f, bodyTop + 22f, bodyX + 24f, bodyTop + 22f, paintOutline)
      canvas.drawLine(bodyX + 38f, bodyTop + 22f, bodyX + 48f, bodyTop + 22f, paintOutline)
    } else {
      val eyePulse = (190 + (sin(animTick * 0.25) * 65).toInt()).coerceIn(130, 255)
      paintGuardianEye.alpha = eyePulse
      canvas.drawCircle(bodyX + 19f, bodyTop + 22f, 5f, paintGuardianEye)
      canvas.drawCircle(bodyX + 43f, bodyTop + 22f, 5f, paintGuardianEye)
      paintGuardianEye.alpha = 255
    }

    // Telegraphed slam fist glow during windup
    if (guardian.phase == GuardianPhase.ACTIVE && guardian.windupFrames > 0) {
      paintDangerAura.alpha = 165
      canvas.drawCircle(bodyX - 8f, groundY - 22f, 22f, paintDangerAura)
      paintDangerAura.alpha = 85
      canvas.drawCircle(bodyX - 8f, groundY - 22f, 12f, paintFlame)
    }

    // Enraged phase fissures when Guardian has taken crystal hits (Hard/Nightmare multi-hit)
    if (guardian.hitsRemaining < guardian.maxHits && guardian.hitsRemaining > 0) {
      paintCrackPulse.color = 0xFFFB923C.toInt()
      paintCrackPulse.alpha = 230
      pathTemp.reset()
      pathTemp.moveTo(bodyX + 10f, bodyTop + 38f)
      pathTemp.lineTo(bodyX + 28f, bodyTop + 56f)
      pathTemp.lineTo(bodyX + 18f, groundY - 12f)
      pathTemp.moveTo(bodyX + guardian.width - 12f, bodyTop + 42f)
      pathTemp.lineTo(bodyX + guardian.width - 26f, bodyTop + 62f)
      canvas.drawPath(pathTemp, paintCrackPulse)
      paintCrackPulse.color = 0xFFF59E0B.toInt()
      paintCrackPulse.alpha = 255
    }

    // Overhead weak-spot Sun Crown Crystal target when Awakening or Active
    if (guardian.phase == GuardianPhase.AWAKENING || guardian.phase == GuardianPhase.ACTIVE) {
      val cx = guardian.crystalX - cameraX
      val bob = (sin(animTick * 0.12) * 5f).toFloat()
      val cy = guardian.crystalY - cameraY + bob

      // Protective invulnerability shimmer right after taking a hit
      if (guardian.crystalInvulnFrames > 0) {
        paintRuneGlow.alpha = 210
        canvas.drawCircle(cx, cy, guardian.crystalRadius + 10f, paintRuneGlow)
        paintRuneGlow.alpha = 255
      } else {
        val ringR = guardian.crystalRadius + 6f + (sin(animTick * 0.2) * 3f).toFloat()
        paintRuneAmber.alpha = 220
        canvas.drawCircle(cx, cy, ringR, paintRuneAmber)
        paintRuneAmber.alpha = 255
      }

      pathTemp.reset()
      pathTemp.moveTo(cx, cy - guardian.crystalRadius)
      pathTemp.lineTo(cx + guardian.crystalRadius * 0.72f, cy)
      pathTemp.lineTo(cx, cy + guardian.crystalRadius)
      pathTemp.lineTo(cx - guardian.crystalRadius * 0.72f, cy)
      pathTemp.close()
      canvas.drawPath(pathTemp, if (guardian.crystalInvulnFrames > 0) paintResonanceStone else paintGateSun)
      canvas.drawPath(pathTemp, paintOutline)
      canvas.drawCircle(cx, cy, 7f, paintGateCoreGlow)

      // Hit pips indicator above Sun Crown Crystal when maxHits > 1
      if (guardian.maxHits > 1) {
        val pipSpan = (guardian.maxHits - 1) * 12f
        for (h in 0 until guardian.maxHits) {
          val px = cx - pipSpan / 2f + h * 12f
          val py = cy - guardian.crystalRadius - 12f
          canvas.drawCircle(px, py, 4f, if (h < guardian.hitsRemaining) paintGateSun else paintStoneDark)
          canvas.drawCircle(px, py, 4f, paintOutline)
        }
      }
    }

    // Rolling ground shockwaves (with visual cue if capable of rebounding)
    for (sw in guardian.shockwaves) {
      if (!sw.active) continue
      val wx = sw.x - cameraX
      val wy = sw.y - cameraY
      canvas.drawCircle(wx, wy, sw.radius + 4f, paintDangerAura)
      if (sw.isRebound) {
        paintRuneGlow.alpha = 220
        canvas.drawCircle(wx, wy, sw.radius + 7f, paintRuneGlow)
        paintRuneGlow.alpha = 255
      }
      pathTemp.reset()
      pathTemp.moveTo(wx - sw.radius, wy + sw.radius)
      pathTemp.quadTo(wx, wy - sw.radius * 1.4f, wx + sw.radius, wy + sw.radius)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintFlame)
      canvas.drawPath(pathTemp, paintOutline)
      canvas.drawCircle(wx, wy + 2f, sw.radius * 0.45f, paintCoinInner)
    }
  }

  // --- TROLL PLATFORMER TRAP RENDERING (Zero Per-Frame Allocations) ---

  fun drawTrollTrap(
      canvas: Canvas,
      trap: TrollTrap,
      cameraX: Float,
      cameraY: Float,
      animTick: Int
  ) {
    if (!trap.isVisible) return

    val drawX = trap.x - cameraX
    val drawY = trap.y + trap.dropOffsetY - cameraY
    if (drawX + trap.width < -96f || drawX > canvas.width + 96f ||
        drawY + trap.height < -96f || drawY > canvas.height + 96f) {
      return
    }

    val shakeX = if (trap.state == TrollTrapState.TELEGRAPH) {
      ((animTick % 4) - 1.5f) * 2.4f
    } else {
      0f
    }
    rectTemp.set(drawX + shakeX, drawY, drawX + trap.width + shakeX, drawY + trap.height)

    when (trap.type) {
      TrollTrapType.DISAPPEARING_FLOOR -> {
        canvas.drawRoundRect(rectTemp, 4f, 4f, paintWood)
        subRectTemp.set(rectTemp.left - 1f, rectTemp.top - 2f, rectTemp.right + 1f, rectTemp.top + 8f)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintMoss)
        canvas.drawLine(rectTemp.left + 8f, rectTemp.top + 12f, rectTemp.centerX(), rectTemp.bottom - 6f, paintWoodGrain)
        if (trap.state == TrollTrapState.TELEGRAPH) {
          paintCrackPulse.color = 0xFFEF4444.toInt()
          canvas.drawRoundRect(rectTemp, 4f, 4f, paintCrackPulse)
          paintCrackPulse.color = 0xFFF59E0B.toInt()
        } else {
          canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)
        }
      }

      TrollTrapType.DELAYED_CRUMBLE -> {
        val alphaInt = (trap.fadeAlpha * 255f).toInt().coerceIn(0, 255)
        paintWood.alpha = alphaInt
        paintWoodGrain.alpha = alphaInt
        paintOutline.alpha = alphaInt
        paintMoss.alpha = alphaInt
        val isTelegraph = trap.state == TrollTrapState.TELEGRAPH
        val segmentCount = (trap.width / 48f).toInt().coerceAtLeast(1)
        val segW = rectTemp.width() / segmentCount
        for (s in 0 until segmentCount) {
          subRectTemp.set(
              rectTemp.left + s * segW + 1f,
              rectTemp.top,
              rectTemp.left + (s + 1) * segW - 1f,
              rectTemp.bottom
          )
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintWood)
          canvas.drawLine(subRectTemp.left + 12f, subRectTemp.top + 2f, subRectTemp.centerX() + 2f, subRectTemp.bottom - 4f, paintWoodGrain)
          canvas.drawLine(subRectTemp.right - 14f, subRectTemp.top + 4f, subRectTemp.centerX() - 4f, subRectTemp.bottom - 2f, paintWoodGrain)
          if (isTelegraph) {
            val urgency = (1f - (trap.stateTimerFrames.toFloat() / trap.telegraphDurationFrames.coerceAtLeast(1))).coerceIn(0f, 1f)
            paintCrackPulse.color = if (urgency > 0.5f) 0xFFEF4444.toInt() else 0xFFF59E0B.toInt()
            paintCrackPulse.alpha = alphaInt
            canvas.drawLine(subRectTemp.left + 12f, subRectTemp.top + 2f, subRectTemp.centerX() + 2f, subRectTemp.bottom - 4f, paintCrackPulse)
            canvas.drawRoundRect(subRectTemp, 4f, 4f, paintCrackPulse)
          }
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)
        }
        paintWood.alpha = 255
        paintWoodGrain.alpha = 255
        paintOutline.alpha = 255
        paintMoss.alpha = 255
        paintCrackPulse.color = 0xFFF59E0B.toInt()
        paintCrackPulse.alpha = 255
      }

      TrollTrapType.SURPRISE_SPIKE_POP -> {
        val floorY = rectTemp.bottom
        subRectTemp.set(rectTemp.left, floorY - 5f, rectTemp.right, floorY + 3f)
        canvas.drawRoundRect(subRectTemp, 2f, 2f, paintTrapPlate)
        canvas.drawRoundRect(subRectTemp, 2f, 2f, paintOutline)

        if (trap.state == TrollTrapState.TELEGRAPH) {
          paintCrimsonGlow.alpha = 210
          subRectTemp.set(rectTemp.left + 2f, floorY - 8f, rectTemp.right - 2f, floorY)
          canvas.drawRoundRect(subRectTemp, 2f, 2f, paintCrimsonGlow)
          paintCrimsonGlow.alpha = 170
        }

        val h = trap.morphSpikeHeight
        if (h > 1.5f) {
          val count = (trap.width / 16f).toInt().coerceIn(2, 6)
          val sw = rectTemp.width() / count
          for (i in 0 until count) {
            val sx = rectTemp.left + i * sw
            pathTemp.reset()
            pathTemp.moveTo(sx + 1.5f, floorY)
            pathTemp.lineTo(sx + sw * 0.5f, floorY - h)
            pathTemp.lineTo(sx + sw - 1.5f, floorY)
            pathTemp.close()
            canvas.drawPath(pathTemp, paintTrollTeeth)
            canvas.drawPath(pathTemp, paintOutline)
            canvas.drawCircle(sx + sw * 0.5f, floorY - h + 2.5f, 2.2f, paintTrollCrimson)
          }
        }
      }

      TrollTrapType.MOVING_SPIKE -> {
        subRectTemp.set(rectTemp.left + 4f, rectTemp.top + 8f, rectTemp.right - 4f, rectTemp.bottom - 2f)
        canvas.drawRoundRect(subRectTemp, 6f, 6f, paintGateStone)
        canvas.drawRoundRect(subRectTemp, 6f, 6f, paintOutline)
        val teethCount = (trap.width / 14f).toInt().coerceIn(2, 6)
        val tw = rectTemp.width() / teethCount
        for (i in 0 until teethCount) {
          val sx = rectTemp.left + i * tw
          pathTemp.reset()
          pathTemp.moveTo(sx + 2f, rectTemp.top + 10f)
          pathTemp.lineTo(sx + tw * 0.5f, rectTemp.top)
          pathTemp.lineTo(sx + tw - 2f, rectTemp.top + 10f)
          pathTemp.close()
          canvas.drawPath(pathTemp, paintTrollTeeth)
          canvas.drawPath(pathTemp, paintOutline)
        }
        canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY() + 4f, 4.5f, paintTrollCrimson)
      }

      TrollTrapType.FAKE_SAFE_PLATFORM -> {
        val alphaInt = (trap.fadeAlpha * 255f).toInt().coerceIn(0, 255)
        paintStone.alpha = alphaInt
        paintMossLight.alpha = alphaInt
        paintOutline.alpha = alphaInt
        canvas.drawRoundRect(rectTemp, 5f, 5f, paintStone)
        subRectTemp.set(rectTemp.left, rectTemp.top - 2f, rectTemp.right, rectTemp.top + 8f)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintMossLight)
        canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)
        paintStone.alpha = 255
        paintMossLight.alpha = 255
        paintOutline.alpha = 255
        if (trap.state == TrollTrapState.TELEGRAPH) {
          paintCrackPulse.color = 0xFFEF4444.toInt()
          canvas.drawRoundRect(rectTemp, 5f, 5f, paintCrackPulse)
          paintCrackPulse.color = 0xFFF59E0B.toInt()
        }
        if (trap.morphSpikeHeight > 1.5f) {
          val h = trap.morphSpikeHeight
          val count = (trap.width / 16f).toInt().coerceIn(2, 8)
          val sw = rectTemp.width() / count
          for (i in 0 until count) {
            val sx = rectTemp.left + i * sw
            pathTemp.reset()
            pathTemp.moveTo(sx + 2f, rectTemp.top)
            pathTemp.lineTo(sx + sw * 0.5f, rectTemp.top - h)
            pathTemp.lineTo(sx + sw - 2f, rectTemp.top)
            pathTemp.close()
            canvas.drawPath(pathTemp, paintTrollTeeth)
            canvas.drawPath(pathTemp, paintOutline)
            canvas.drawCircle(sx + sw * 0.5f, rectTemp.top - h + 2.2f, 2.0f, paintTrollCrimson)
          }
        }
      }

      TrollTrapType.FAKE_EXIT_DOOR -> {
        drawSunGate(canvas, rectTemp, animTick)
        if (trap.state == TrollTrapState.TELEGRAPH || trap.state == TrollTrapState.ACTIVE) {
          paintCrimsonGlow.alpha = 180
          canvas.drawRoundRect(rectTemp, 20f, 20f, paintCrimsonGlow)
          paintCrimsonGlow.alpha = 170
          val count = 4
          val sw = rectTemp.width() / count
          for (i in 0 until count) {
            val sx = rectTemp.left + i * sw
            pathTemp.reset()
            pathTemp.moveTo(sx + 2f, rectTemp.bottom)
            pathTemp.lineTo(sx + sw * 0.5f, rectTemp.centerY())
            pathTemp.lineTo(sx + sw - 2f, rectTemp.bottom)
            pathTemp.close()
            canvas.drawPath(pathTemp, paintTrollTeeth)
            canvas.drawPath(pathTemp, paintOutline)
          }
        }
      }

      TrollTrapType.RELOCATING_EXIT_DOOR -> {
        if (trap.hasRelocated) {
          val pulse = 18f + (sin(animTick * 0.25) * 4f).toFloat()
          paintRuneAmber.alpha = 150
          canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY(), pulse, paintRuneAmber)
          paintRuneAmber.alpha = 255
        }
      }

      TrollTrapType.MOVING_CRUSH_WALL -> {
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintGateStone)
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)
        subRectTemp.set(rectTemp.left + 6f, rectTemp.top + 8f, rectTemp.right - 6f, rectTemp.bottom - 8f)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintStoneDark)
        canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY() - 8f, 5f, paintTrollCrimson)
        canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY() + 12f, 5f, paintMarigold)
      }

      TrollTrapType.FALLING_CEILING_BLOCK -> {
        // Ground shadow telegraph underneath falling block
        val groundTargetY = trap.targetY + trap.height - cameraY
        if (trap.state == TrollTrapState.TELEGRAPH || trap.state == TrollTrapState.ACTIVE) {
          val alpha = if (trap.state == TrollTrapState.TELEGRAPH) 160 else 210
          paintHazardShadow.alpha = alpha
          subRectTemp.set(rectTemp.left + 4f, groundTargetY - 5f, rectTemp.right - 4f, groundTargetY + 4f)
          canvas.drawOval(subRectTemp, paintHazardShadow)
        }
        canvas.drawRoundRect(rectTemp, 7f, 7f, paintStoneDark)
        canvas.drawRoundRect(rectTemp, 7f, 7f, paintOutline)
        // Angry eyebrows + glowing crimson core
        val cx = rectTemp.centerX()
        val cy = rectTemp.centerY()
        canvas.drawLine(cx - 14f, cy - 10f, cx - 4f, cy - 4f, paintOutline)
        canvas.drawLine(cx + 14f, cy - 10f, cx + 4f, cy - 4f, paintOutline)
        canvas.drawCircle(cx - 8f, cy, 3.5f, paintTrollCrimson)
        canvas.drawCircle(cx + 8f, cy, 3.5f, paintTrollCrimson)
        // Jagged bottom edge
        val count = 3
        val sw = rectTemp.width() / count
        for (i in 0 until count) {
          val sx = rectTemp.left + i * sw
          pathTemp.reset()
          pathTemp.moveTo(sx + 2f, rectTemp.bottom)
          pathTemp.lineTo(sx + sw * 0.5f, rectTemp.bottom + 8f)
          pathTemp.lineTo(sx + sw - 2f, rectTemp.bottom)
          pathTemp.close()
          canvas.drawPath(pathTemp, paintTrollTeeth)
          canvas.drawPath(pathTemp, paintOutline)
        }
      }

      TrollTrapType.FAKE_CHECKPOINT -> {
        drawCheckpointBell(canvas, rectTemp, animTick, checkpointActive = false)
        if (trap.state == TrollTrapState.ACTIVE || trap.state == TrollTrapState.TELEGRAPH) {
          canvas.drawCircle(rectTemp.centerX(), rectTemp.top + 14f, 12f, paintTrollCrimson)
        }
      }

      TrollTrapType.TIMED_BLINK_PLATFORM -> {
        if (trap.isSolid) {
          val aboutToBlinkOut = trap.state == TrollTrapState.TELEGRAPH
          paintStoneLight.alpha = if (aboutToBlinkOut && (animTick % 6 < 3)) 110 else 255
          canvas.drawRoundRect(rectTemp, 5f, 5f, paintStoneLight)
          canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)
          paintRuneGlow.alpha = if (aboutToBlinkOut) 255 else 180
          canvas.drawLine(rectTemp.left + 8f, rectTemp.centerY(), rectTemp.right - 8f, rectTemp.centerY(), paintRuneGlow)
          paintStoneLight.alpha = 255
          paintRuneGlow.alpha = 255
        } else {
          paintTrollGhost.alpha = 85
          canvas.drawRoundRect(rectTemp, 5f, 5f, paintTrollGhost)
          paintTrollGhost.alpha = 255
        }
      }

      TrollTrapType.SPLIT_BRIDGE -> {
        val wingW = rectTemp.width() * 0.28f
        if (trap.auxProgress < 0.35f) {
          canvas.drawRoundRect(rectTemp, 4f, 4f, paintWood)
          subRectTemp.set(rectTemp.left, rectTemp.top, rectTemp.right, rectTemp.top + 7f)
          canvas.drawRoundRect(subRectTemp, 3f, 3f, paintMoss)
          // Visible crack seam telegraphing exact split line
          val cx = rectTemp.centerX()
          pathTemp.reset()
          pathTemp.moveTo(cx - 4f, rectTemp.top)
          pathTemp.lineTo(cx + 3f, rectTemp.centerY())
          pathTemp.lineTo(cx - 3f, rectTemp.bottom)
          canvas.drawPath(pathTemp, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintWoodGrain)
          canvas.drawRoundRect(rectTemp, 4f, 4f, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintOutline)
        } else {
          // Left and right safe outer wings
          subRectTemp.set(rectTemp.left, rectTemp.top, rectTemp.left + wingW, rectTemp.bottom)
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintWood)
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)
          subRectTemp.set(rectTemp.right - wingW, rectTemp.top, rectTemp.right, rectTemp.bottom)
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintWood)
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)
          // Center dropped slabs fading out
          val fallAlpha = ((1f - trap.auxProgress) * 255f).toInt().coerceIn(0, 200)
          if (fallAlpha > 12) {
            val dropY = (trap.auxProgress - 0.35f) * 72f
            paintWood.alpha = fallAlpha
            subRectTemp.set(rectTemp.left + wingW + 2f, rectTemp.top + dropY, rectTemp.right - wingW - 2f, rectTemp.bottom + dropY)
            canvas.drawRoundRect(subRectTemp, 3f, 3f, paintWood)
            paintWood.alpha = 255
          }
        }
      }

      TrollTrapType.PHANTOM_LEDGE -> {
        val alphaInt = (trap.fadeAlpha * 255f).toInt().coerceIn(45, 255)
        paintPhantomFill.alpha = (alphaInt * 0.45f).toInt().coerceIn(25, 160)
        paintTrollGhost.alpha = alphaInt
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintPhantomFill)
        if (trap.isSolid) {
          paintStoneLight.alpha = (alphaInt * 0.72f).toInt()
          canvas.drawRoundRect(rectTemp, 6f, 6f, paintStoneLight)
          paintStoneLight.alpha = 255
        }
        canvas.drawRoundRect(
            rectTemp,
            6f,
            6f,
            if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintTrollGhost
        )
        // Etched phantom diamond glyph clue
        val cx = rectTemp.centerX()
        val cy = rectTemp.centerY()
        pathTemp.reset()
        pathTemp.moveTo(cx - 10f, cy)
        pathTemp.lineTo(cx, cy - 5f)
        pathTemp.lineTo(cx + 10f, cy)
        pathTemp.lineTo(cx, cy + 5f)
        pathTemp.close()
        canvas.drawPath(pathTemp, paintTrollGhost)
        paintPhantomFill.alpha = 68
        paintTrollGhost.alpha = 255
      }

      TrollTrapType.TILTING_PLANK -> {
        val px = rectTemp.centerX()
        val py = rectTemp.centerY()
        canvas.save()
        canvas.rotate(trap.tiltAngleDeg, px, py)
        canvas.drawRoundRect(rectTemp, 5f, 5f, paintWood)
        subRectTemp.set(rectTemp.left + 2f, rectTemp.top + 2f, rectTemp.right - 2f, rectTemp.top + 6f)
        canvas.drawRoundRect(subRectTemp, 3f, 3f, paintPlankGold)
        canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)
        canvas.restore()
        // Central pivot rivet stays centered so player immediately reads seesaw behavior
        canvas.drawCircle(px, py, 5.5f, paintBell)
        canvas.drawCircle(px, py, 5.5f, paintOutline)
      }

      TrollTrapType.TRAILING_SPIKE -> {
        if (trap.state == TrollTrapState.TELEGRAPH) {
          paintCrimsonGlow.alpha = 190
          canvas.drawRoundRect(rectTemp, 6f, 6f, paintCrimsonGlow)
          paintCrimsonGlow.alpha = 170
        }
        subRectTemp.set(rectTemp.left + 3f, rectTemp.top + 8f, rectTemp.right - 3f, rectTemp.bottom - 3f)
        canvas.drawRoundRect(subRectTemp, 5f, 5f, paintStoneDark)
        canvas.drawRoundRect(subRectTemp, 5f, 5f, paintOutline)
        val teethCount = (trap.width / 14f).toInt().coerceIn(2, 6)
        val tw = rectTemp.width() / teethCount
        for (i in 0 until teethCount) {
          val sx = rectTemp.left + i * tw
          pathTemp.reset()
          pathTemp.moveTo(sx + 2f, rectTemp.top + 10f)
          pathTemp.lineTo(sx + tw * 0.5f, rectTemp.top)
          pathTemp.lineTo(sx + tw - 2f, rectTemp.top + 10f)
          pathTemp.close()
          canvas.drawPath(pathTemp, paintTrollTeeth)
          canvas.drawPath(pathTemp, paintOutline)
        }
        canvas.drawCircle(rectTemp.left + 9f, rectTemp.bottom - 3f, 4f, paintTrapIron)
        canvas.drawCircle(rectTemp.right - 9f, rectTemp.bottom - 3f, 4f, paintTrapIron)
        canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY() + 2f, 4.5f, paintTrollCrimson)
      }

      TrollTrapType.EVASIVE_PLATFORM -> {
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintStone)
        subRectTemp.set(rectTemp.left, rectTemp.top, rectTemp.right, rectTemp.top + 7f)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintMossLight)
        canvas.drawRoundRect(rectTemp, 6f, 6f, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintOutline)
        // Winged thruster clue underneath
        if (trap.state == TrollTrapState.TELEGRAPH || trap.state == TrollTrapState.ACTIVE) {
          canvas.drawCircle(rectTemp.left + 12f, rectTemp.bottom + 3f, 4f, paintFlame)
          canvas.drawCircle(rectTemp.right - 12f, rectTemp.bottom + 3f, 4f, paintFlame)
        } else {
          canvas.drawCircle(rectTemp.left + 12f, rectTemp.bottom, 3.5f, paintRuneAmber)
          canvas.drawCircle(rectTemp.right - 12f, rectTemp.bottom, 3.5f, paintRuneAmber)
        }
      }

      TrollTrapType.YO_YO_PLATFORM -> {
        // Draw subtle vertical guide rail connecting min..max Y so path is completely readable
        val minY = kotlin.math.min(trap.initialY, trap.targetY) - cameraY + rectTemp.height() * 0.5f
        val maxY = kotlin.math.max(trap.initialY, trap.targetY) - cameraY + rectTemp.height() * 0.5f
        canvas.drawLine(rectTemp.centerX(), minY, rectTemp.centerX(), maxY, paintPendulumArc)
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintStoneLight)
        subRectTemp.set(rectTemp.left + 4f, rectTemp.top + 4f, rectTemp.right - 4f, rectTemp.bottom - 4f)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintStoneDark)
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)
        canvas.drawCircle(rectTemp.left + 8f, rectTemp.centerY(), 3.5f, paintBell)
        canvas.drawCircle(rectTemp.right - 8f, rectTemp.centerY(), 3.5f, paintBell)
      }

      TrollTrapType.EXIT_SLAM_CAGE -> {
        if (trap.state == TrollTrapState.TELEGRAPH) {
          paintCrackPulse.color = 0xFFEF4444.toInt()
          canvas.drawRoundRect(rectTemp, 8f, 8f, paintCrackPulse)
          paintCrackPulse.color = 0xFFF59E0B.toInt()
        } else if (trap.state == TrollTrapState.ACTIVE) {
          val barDrop = rectTemp.height() * trap.auxProgress.coerceIn(0.15f, 1f)
          // Top vaultable cage cornice
          subRectTemp.set(rectTemp.left, rectTemp.top, rectTemp.right, rectTemp.top + 12f)
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintCageIron)
          canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)
          // Left and right solid posts (14f matching collision)
          subRectTemp.set(rectTemp.left, rectTemp.top, rectTemp.left + 14f, rectTemp.top + barDrop)
          canvas.drawRect(subRectTemp, paintCageIron)
          canvas.drawRect(subRectTemp, paintOutline)
          subRectTemp.set(rectTemp.right - 14f, rectTemp.top, rectTemp.right, rectTemp.top + barDrop)
          canvas.drawRect(subRectTemp, paintCageIron)
          canvas.drawRect(subRectTemp, paintOutline)
          // Inner decorative prison bars
          val bars = 3
          val step = (rectTemp.width() - 28f) / (bars + 1)
          for (b in 1..bars) {
            val bx = rectTemp.left + 14f + b * step
            canvas.drawLine(bx, rectTemp.top + 10f, bx, rectTemp.top + barDrop, paintCageIron)
          }
        } else if (trap.state == TrollTrapState.ARMED) {
          // Subtle hanging iron latch overhead clue
          subRectTemp.set(rectTemp.left + 4f, rectTemp.top - 8f, rectTemp.right - 4f, rectTemp.top + 2f)
          canvas.drawRoundRect(subRectTemp, 3f, 3f, paintCageIron)
          canvas.drawRoundRect(subRectTemp, 3f, 3f, paintOutline)
        }
      }

      TrollTrapType.FLEEING_SUN_GATE -> {
        if (trap.state != TrollTrapState.RETIRED) {
          val pulse = 16f + (sin(animTick * 0.25) * 4f).toFloat()
          paintRuneAmber.alpha = if (trap.state == TrollTrapState.TELEGRAPH || trap.state == TrollTrapState.ACTIVE) 235 else 120
          canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY(), pulse, paintRuneAmber)
          paintRuneAmber.alpha = 255
        }
      }

      TrollTrapType.PINCER_WALLS -> {
        val maxJaw = ((trap.width - trap.minSafeGapWidth) * 0.5f).coerceAtLeast(16f)
        val jawW = maxJaw * trap.auxProgress.coerceIn(0.05f, 1f)
        // Floor safe-center zone clue
        val safeLeft = rectTemp.left + maxJaw
        val safeRight = rectTemp.right - maxJaw
        subRectTemp.set(safeLeft, rectTemp.bottom - 6f, safeRight, rectTemp.bottom)
        canvas.drawRect(subRectTemp, paintRuneGlow)
        // Left & right jaws matching collision width
        subRectTemp.set(rectTemp.left, rectTemp.top, rectTemp.left + jawW, rectTemp.bottom)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintGateStone)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintOutline)
        canvas.drawCircle(subRectTemp.right - 6f, subRectTemp.centerY(), 4f, paintTrollCrimson)

        subRectTemp.set(rectTemp.right - jawW, rectTemp.top, rectTemp.right, rectTemp.bottom)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintGateStone)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintOutline)
        canvas.drawCircle(subRectTemp.left + 6f, subRectTemp.centerY(), 4f, paintTrollCrimson)
      }

      TrollTrapType.STALKER_CRUSH_WALL -> {
        canvas.drawRoundRect(rectTemp, 6f, 6f, paintGateStone)
        subRectTemp.set(rectTemp.left + 5f, rectTemp.top + 8f, rectTemp.right - 5f, rectTemp.bottom - 8f)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintStoneDark)
        canvas.drawRoundRect(rectTemp, 6f, 6f, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintOutline)
        val eyeY = rectTemp.top + rectTemp.height() * 0.35f
        canvas.drawCircle(rectTemp.centerX() - 7f, eyeY, 4.5f, paintStalkerEye)
        canvas.drawCircle(rectTemp.centerX() + 7f, eyeY, 4.5f, paintStalkerEye)
        canvas.drawCircle(rectTemp.centerX(), rectTemp.centerY() + 12f, 5f, paintTrollCrimson)
      }

      TrollTrapType.BETRAYAL_SWITCH -> {
        val depressed = trap.activationCount > 0
        subRectTemp.set(rectTemp.left + 4f, rectTemp.bottom - 12f, rectTemp.right - 4f, rectTemp.bottom)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintStoneDark)
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)
        val capTop = if (depressed) rectTemp.bottom - 15f else rectTemp.top + 6f
        subRectTemp.set(rectTemp.left + 8f, capTop, rectTemp.right - 8f, rectTemp.bottom - 10f)
        canvas.drawRoundRect(subRectTemp, 5f, 5f, if (depressed) paintStone else paintTrollCrimson)
        canvas.drawRoundRect(subRectTemp, 5f, 5f, paintOutline)
        if (!depressed) {
          canvas.drawCircle(rectTemp.centerX(), capTop + 6f, 3.5f, paintCoinInner)
        }
      }

      TrollTrapType.GUST_REVERSAL_ZONE -> {
        val isTelegraph = trap.state == TrollTrapState.TELEGRAPH
        if (isTelegraph) {
          paintCrimsonGlow.alpha = 55
          canvas.drawRoundRect(rectTemp, 8f, 8f, paintCrimsonGlow)
          paintCrimsonGlow.alpha = 170
        } else {
          canvas.drawRoundRect(rectTemp, 8f, 8f, paintWindCyan)
        }
        val pushRight = (trap.gustForceX * trap.gustSign) >= 0f
        val dir = if (pushRight) 1f else -1f
        val arrowPaint = if (isTelegraph) paintCrackPulse else paintWindArrow
        val rows = 3
        val cols = (rectTemp.width() / 72f).toInt().coerceIn(2, 5)
        for (r in 0 until rows) {
          val ay = rectTemp.top + (r + 0.5f) * (rectTemp.height() / rows)
          for (c in 0 until cols) {
            val offset = ((animTick * 2.4f * dir) % 36f)
            val ax = rectTemp.left + 20f + c * ((rectTemp.width() - 40f) / (cols - 1).coerceAtLeast(1)) + offset * 0.3f
            pathTemp.reset()
            pathTemp.moveTo(ax - 7f * dir, ay - 7f)
            pathTemp.lineTo(ax + 7f * dir, ay)
            pathTemp.lineTo(ax - 7f * dir, ay + 7f)
            canvas.drawPath(pathTemp, arrowPaint)
          }
        }
      }

      TrollTrapType.MIRROR_CURSE_TOTEM -> {
        canvas.drawRoundRect(rectTemp, 7f, 7f, paintStoneDark)
        canvas.drawRoundRect(rectTemp, 7f, 7f, if (trap.state == TrollTrapState.TELEGRAPH) paintCrackPulse else paintCurseGlow)
        val cx = rectTemp.centerX()
        val cy = rectTemp.centerY()
        val auraR = 14f + (sin(animTick * 0.22) * 3.5f).toFloat()
        canvas.drawCircle(cx, cy, auraR, paintCurseGlow)
        canvas.drawCircle(cx, cy - 4f, 7.5f, paintCursePurple)
        canvas.drawCircle(cx, cy - 4f, 3.2f, paintCoinInner)
      }
    }
  }
}
