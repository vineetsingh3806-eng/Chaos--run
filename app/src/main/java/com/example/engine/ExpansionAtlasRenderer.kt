package com.example.engine

import android.graphics.*
import com.example.data.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Zero-allocation Canvas Renderer for World 1 Expansion Levels (1-2 to 1-5):
 * - Distinct atmospheric backdrops & parallax silhouettes per LevelTheme
 * - SoundSwitchNode, ECHO_BRIDGE, SOUND_GATE, FALSE_PATH_WALL
 * - OrbitingPlatform (astrolabe stone slabs & spiked clockwork gears) & CollapsingPillar
 * - WaterCurrentZone, SLIPPERY_STONE, LightningHazard, Heavy Rain & Sky Flashes
 * - LevelRelic prism artifacts & Darkness Lantern radial cutout overlay
 */
class ExpansionAtlasRenderer {
  private val rectTemp = RectF()
  private val subRectTemp = RectF()
  private val pathTemp = Path()

  private val paintBgTop = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintSilhouette = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
    strokeCap = Paint.Cap.ROUND
  }
  private val paintDashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 2.2f
    pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
  }
  private val paintOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF0F172A.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
  }
  private val paintRain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x88BAE6FD.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.1f
    strokeCap = Paint.Cap.ROUND
  }

  private val lanternCutoutShader = RadialGradient(
      0f,
      0f,
      250f,
      intArrayOf(0xFFFFFFFF.toInt(), 0xE6FFFFFF.toInt(), 0x55FFFFFF, 0x00FFFFFF),
      floatArrayOf(0f, 0.45f, 0.78f, 1f),
      Shader.TileMode.CLAMP
  )
  private val paintLanternCutout = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    shader = lanternCutoutShader
    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
  }
  private val paintWarmLanternGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x26FDE68A
  }
  private val paintLightmap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isFilterBitmap = true
  }
  private var lightmapBitmap: Bitmap? = null
  private var lightmapCanvas: Canvas? = null
  private var lightmapSrcRect = Rect()
  private var lightmapDstRect = RectF()

  private var cachedTheme: LevelTheme? = null
  private var cachedHeight: Float = -1f
  private var cachedBgShader: LinearGradient? = null

  fun drawThemeBackdrop(
      canvas: Canvas,
      theme: LevelTheme,
      viewW: Float,
      viewH: Float,
      cameraX: Float,
      animTick: Int
  ) {
    if (cachedTheme != theme || cachedHeight != viewH || cachedBgShader == null) {
      val colors = when (theme) {
        LevelTheme.HOLLOW_OF_ECHOES -> intArrayOf(0xFF070C18.toInt(), 0xFF132238.toInt(), 0xFF1A3142.toInt())
        LevelTheme.RUINS_OF_LOST_KING -> intArrayOf(0xFF2B1B12.toInt(), 0xFF5C3A21.toInt(), 0xFF8C5A2B.toInt())
        LevelTheme.MONSOON_RUN -> intArrayOf(0xFF091520.toInt(), 0xFF132E3C.toInt(), 0xFF1B4552.toInt())
        LevelTheme.TEMPLE_OF_LAST_LIGHT -> intArrayOf(0xFF1A102B.toInt(), 0xFF2D2252.toInt(), 0xFF4A3B5E.toInt())
        LevelTheme.WHISPERING_WOODS -> intArrayOf(0xFF0F2922.toInt(), 0xFF1B3B2B.toInt(), 0xFF274E3B.toInt())
      }
      cachedBgShader = LinearGradient(0f, 0f, 0f, viewH, colors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
      cachedTheme = theme
      cachedHeight = viewH
      paintBgTop.shader = cachedBgShader
    }
    canvas.drawRect(0f, 0f, viewW, viewH, paintBgTop)

    when (theme) {
      LevelTheme.HOLLOW_OF_ECHOES -> drawCavernParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.RUINS_OF_LOST_KING -> drawRuinsParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.MONSOON_RUN -> drawMonsoonParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.TEMPLE_OF_LAST_LIGHT -> drawTempleParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.WHISPERING_WOODS -> Unit
    }
  }

  private fun drawCavernParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Distant stalactite vault & crystal spires
    paintSilhouette.color = 0xFF0B1626.toInt()
    val span = 180f
    val offset1 = ((camX * 0.18f) % span)
    var x = -offset1 - span
    var idx = 0
    while (x < viewW + span) {
      val toothH = 65f + (idx % 3) * 38f
      pathTemp.reset()
      pathTemp.moveTo(x, 0f)
      pathTemp.lineTo(x + span * 0.45f, toothH)
      pathTemp.lineTo(x + span * 0.9f, 0f)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintSilhouette)

      // Distant echo crystal glint
      if (idx % 2 == 0) {
        paintGlow.color = 0x442DD4BF
        val pulse = 4f + (sin(tick * 0.05 + idx) * 2f).toFloat()
        canvas.drawCircle(x + span * 0.45f, toothH + 6f, pulse, paintGlow)
      }
      x += span
      idx++
    }

    // Mid-ground subterranean arches
    paintSilhouette.color = 0xFF102033.toInt()
    val archSpan = 280f
    val offset2 = ((camX * 0.36f) % archSpan)
    x = -offset2 - archSpan
    while (x < viewW + archSpan) {
      rectTemp.set(x, viewH * 0.25f, x + 46f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      x += archSpan
    }
  }

  private fun drawRuinsParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Golden desert/citadel sun halo
    paintGlow.color = 0x33F59E0B
    canvas.drawCircle(viewW * 0.78f, viewH * 0.24f, 92f, paintGlow)
    paintGlow.color = 0x55FDE047
    canvas.drawCircle(viewW * 0.78f, viewH * 0.24f, 44f, paintGlow)

    // Distant sandstone pyramids & broken obelisks
    paintSilhouette.color = 0xFF3E2716.toInt()
    val span = 240f
    val offset1 = ((camX * 0.15f) % span)
    var x = -offset1 - span
    var idx = 0
    while (x < viewW + span) {
      val topY = viewH * (0.34f + (idx % 3) * 0.08f)
      rectTemp.set(x + 30f, topY, x + 84f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      rectTemp.set(x + 20f, topY - 10f, x + 94f, topY + 6f)
      canvas.drawRect(rectTemp, paintSilhouette)
      x += span
      idx++
    }

    // Mid-ground carved colonnade arches
    paintSilhouette.color = 0xFF4F311A.toInt()
    val archSpan = 320f
    val offset2 = ((camX * 0.35f) % archSpan)
    x = -offset2 - archSpan
    while (x < viewW + archSpan) {
      rectTemp.set(x + 24f, viewH * 0.42f, x + 62f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      rectTemp.set(x + 180f, viewH * 0.42f, x + 218f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      rectTemp.set(x + 18f, viewH * 0.40f, x + 224f, viewH * 0.46f)
      canvas.drawRect(rectTemp, paintSilhouette)
      x += archSpan
    }

    // Floating warm dust motes
    paintGlow.color = 0x55FDE68A
    for (i in 0..8) {
      val mx = ((i * 157f - camX * 0.5f + tick * 0.35f) % viewW + viewW) % viewW
      val my = viewH * 0.25f + (i % 4) * (viewH * 0.15f) + (sin(tick * 0.04 + i) * 10f).toFloat()
      canvas.drawCircle(mx, my, 2.2f, paintGlow)
    }
  }

  private fun drawMonsoonParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Rolling storm clouds
    paintSilhouette.color = 0xFF0C202B.toInt()
    val cloudSpan = 260f
    val offset1 = ((camX * 0.12f + tick * 0.45f) % cloudSpan)
    var x = -offset1 - cloudSpan
    var idx = 0
    while (x < viewW + cloudSpan) {
      val cy = viewH * (0.14f + (idx % 2) * 0.06f)
      canvas.drawCircle(x + 70f, cy, 52f, paintSilhouette)
      canvas.drawCircle(x + 135f, cy - 8f, 64f, paintSilhouette)
      canvas.drawCircle(x + 205f, cy + 4f, 48f, paintSilhouette)
      x += cloudSpan
      idx++
    }

    // Distant rain-veiled cliffs & aqueduct arches
    paintSilhouette.color = 0xFF10303E.toInt()
    val span = 300f
    val offset2 = ((camX * 0.28f) % span)
    x = -offset2 - span
    while (x < viewW + span) {
      rectTemp.set(x + 16f, viewH * 0.45f, x + 56f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      rectTemp.set(x + 140f, viewH * 0.50f, x + 188f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      rectTemp.set(x, viewH * 0.45f, x + 220f, viewH * 0.51f)
      canvas.drawRect(rectTemp, paintSilhouette)
      x += span
    }
  }

  private fun drawTempleParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Diagonal sacred sunbeams streaming down from the sanctuary skylight
    paintGlow.color = 0x1CFDE047
    for (i in 0..3) {
      val bx = ((i * 290f - camX * 0.12f) % (viewW + 300f) + (viewW + 300f)) % (viewW + 300f) - 100f
      pathTemp.reset()
      pathTemp.moveTo(bx, 0f)
      pathTemp.lineTo(bx + 78f, 0f)
      pathTemp.lineTo(bx + 210f, viewH)
      pathTemp.lineTo(bx + 90f, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintGlow)
    }

    // Sanctum pillars & mandala frieze
    paintSilhouette.color = 0xFF241B40.toInt()
    val span = 260f
    val offset = ((camX * 0.30f) % span)
    var x = -offset - span
    while (x < viewW + span) {
      rectTemp.set(x + 30f, viewH * 0.14f, x + 76f, viewH)
      canvas.drawRect(rectTemp, paintSilhouette)
      canvas.drawCircle(x + 165f, viewH * 0.38f, 34f, paintSilhouette)
      x += span
    }

    // Radiant golden temple embers
    paintGlow.color = 0x88FDE047.toInt()
    for (i in 0..10) {
      val fx = ((i * 149f - camX * 0.48f) % viewW + viewW) % viewW
      val fy = ((viewH - ((tick * 0.9f + i * 71f) % viewH)) + viewH) % viewH
      canvas.drawCircle(fx, fy, 2.3f, paintGlow)
    }
  }

  fun drawExpansionTile(
      canvas: Canvas,
      tile: GameTile,
      rect: RectF,
      animTick: Int
  ) {
    when (tile.type) {
      TileType.ECHO_BRIDGE -> {
        if (tile.isSolid) {
          paintFill.color = 0xFF0E7490.toInt()
          canvas.drawRoundRect(rect, 6f, 6f, paintFill)
          subRectTemp.set(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.top + 10f)
          paintFill.color = 0xFF67E8F9.toInt()
          canvas.drawRoundRect(subRectTemp, 3f, 3f, paintFill)
          paintStroke.color = 0xFF2DD4BF.toInt()
          canvas.drawRoundRect(rect, 6f, 6f, paintStroke)
        } else {
          // Dormant ghost outline showing where the echo slab will materialize
          paintFill.color = 0x220891B2
          canvas.drawRoundRect(rect, 6f, 6f, paintFill)
          paintDashed.color = 0x9938BDF8.toInt()
          canvas.drawRoundRect(rect, 6f, 6f, paintDashed)
        }
      }

      TileType.SOUND_GATE -> {
        // Carved monolithic sonic seal barrier (disappears when matching chime rings)
        paintFill.color = 0xFF1E293B.toInt()
        canvas.drawRoundRect(rect, 5f, 5f, paintFill)
        paintStroke.color = 0xFF2DD4BF.toInt()
        canvas.drawRoundRect(rect, 5f, 5f, paintStroke)
        val cx = rect.centerX()
        val cy = rect.centerY()
        val pulse = 7f + (sin(animTick * 0.14 + tile.id) * 2.5f).toFloat()
        canvas.drawCircle(cx, cy, pulse, paintStroke)
      }

      TileType.FALSE_PATH_WALL -> {
        if (!tile.isTriggered) {
          // Solid-looking stone block hiding a secret passage
          paintFill.color = 0xFF3F4E5E.toInt()
          canvas.drawRoundRect(rect, 4f, 4f, paintFill)
          canvas.drawRoundRect(rect, 4f, 4f, paintOutline)
          paintStroke.color = 0x5567E8F9
          canvas.drawLine(rect.left + 8f, rect.top + 12f, rect.centerX(), rect.centerY(), paintStroke)
          canvas.drawLine(rect.centerX(), rect.centerY(), rect.right - 10f, rect.bottom - 10f, paintStroke)
        } else {
          // Revealed illusion wall (translucent dashed shimmer)
          paintFill.color = 0x2838BDF8
          canvas.drawRoundRect(rect, 4f, 4f, paintFill)
          paintDashed.color = 0x8867E8F9.toInt()
          canvas.drawRoundRect(rect, 4f, 4f, paintDashed)
        }
      }

      TileType.SLIPPERY_STONE -> {
        // Wet rain-slicked slate with glossy cyan-white specular gleam
        paintFill.color = 0xFF2C4A5E.toInt()
        canvas.drawRoundRect(rect, 5f, 5f, paintFill)
        subRectTemp.set(rect.left, rect.top, rect.right, rect.top + 10f)
        paintFill.color = 0xFF7DD3FC.toInt()
        canvas.drawRoundRect(subRectTemp, 4f, 4f, paintFill)
        paintStroke.color = 0xFFE0F2FE.toInt()
        val shimmerShift = ((animTick * 1.4f + tile.id * 9f) % rect.width())
        val sx = (rect.left + shimmerShift).coerceIn(rect.left + 4f, rect.right - 12f)
        canvas.drawLine(sx, rect.top + 4f, (sx + 10f).coerceAtMost(rect.right - 3f), rect.top + 4f, paintStroke)
        canvas.drawRoundRect(rect, 5f, 5f, paintOutline)
      }

      else -> Unit
    }
  }

  fun drawSoundSwitch(
      canvas: Canvas,
      sw: SoundSwitchNode,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    val sx = sw.x - camX
    val sy = sw.y - camY
    if (sx < -90f || sx > canvas.width + 90f) return

    // Pedestal base
    rectTemp.set(sx - 12f, sy + 10f, sx + 12f, sy + 28f)
    paintFill.color = 0xFF334155.toInt()
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintFill)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)

    // Expanding sonic rings when activated
    if (sw.isActivated) {
      val ringR = sw.radius + 8f + ((animTick % 24) * 1.4f)
      paintStroke.color = 0xAA2DD4BF.toInt()
      canvas.drawCircle(sx, sy, ringR, paintStroke)
      if (sw.durationFrames > 0) {
        val ratio = (sw.remainingFrames.toFloat() / sw.durationFrames.toFloat()).coerceIn(0f, 1f)
        rectTemp.set(sx - 20f, sy - 34f, sx + 20f, sy - 28f)
        paintFill.color = 0xAA0F172A.toInt()
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintFill)
        subRectTemp.set(sx - 19f, sy - 33f, sx - 19f + 38f * ratio, sy - 29f)
        paintFill.color = if (ratio < 0.25f) 0xFFF59E0B.toInt() else 0xFF2DD4BF.toInt()
        canvas.drawRoundRect(subRectTemp, 2f, 2f, paintFill)
      }
    } else {
      val idlePulse = sw.radius * 0.72f + (sin(animTick * 0.1) * 3f).toFloat()
      paintStroke.color = 0x6667E8F9
      canvas.drawCircle(sx, sy, idlePulse, paintStroke)
    }

    // Resonant crystal gong diamond
    val r = 15f
    pathTemp.reset()
    pathTemp.moveTo(sx, sy - r)
    pathTemp.lineTo(sx + r * 0.8f, sy)
    pathTemp.lineTo(sx, sy + r)
    pathTemp.lineTo(sx - r * 0.8f, sy)
    pathTemp.close()
    paintFill.color = if (sw.isActivated) 0xFF2DD4BF.toInt() else 0xFF0E7490.toInt()
    canvas.drawPath(pathTemp, paintFill)
    canvas.drawPath(pathTemp, paintOutline)
    paintGlow.color = if (sw.isActivated) 0xFFCCFBF1.toInt() else 0xFF67E8F9.toInt()
    canvas.drawCircle(sx, sy, 5f, paintGlow)
  }

  fun drawOrbitingPlatform(
      canvas: Canvas,
      op: OrbitingPlatform,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    val px = op.pivotX - camX
    val py = op.pivotY - camY
    val ox = op.x - camX
    val oy = op.y - camY
    val cx = ox + op.width / 2f
    val cy = oy + op.height / 2f
    if (cx < -240f || cx > canvas.width + 240f) return

    // Astrolabe track ring & mechanical arm
    paintDashed.color = if (op.isHazardGear) 0x55EF4444 else 0x55F59E0B
    canvas.drawCircle(px, py, op.orbitRadius, paintDashed)
    paintStroke.color = 0xFF78350F.toInt()
    canvas.drawLine(px, py, cx, cy, paintStroke)

    // Pivot hub
    paintFill.color = 0xFF92400E.toInt()
    canvas.drawCircle(px, py, 9f, paintFill)
    canvas.drawCircle(px, py, 9f, paintOutline)

    if (op.isHazardGear) {
      val gearR = op.width * 0.45f
      paintGlow.color = 0x55EF4444
      canvas.drawCircle(cx, cy, gearR + 5f, paintGlow)
      paintFill.color = 0xFF991B1B.toInt()
      canvas.drawCircle(cx, cy, gearR, paintFill)
      // Spinning gear teeth
      val spin = animTick * 0.09f
      paintStroke.color = 0xFFFCA5A5.toInt()
      for (t in 0..7) {
        val a = spin + t * (PI / 4.0).toFloat()
        val tx = cx + cos(a.toDouble()).toFloat() * (gearR + 5f)
        val ty = cy + sin(a.toDouble()).toFloat() * (gearR + 5f)
        canvas.drawLine(cx, cy, tx, ty, paintStroke)
      }
      paintFill.color = 0xFFFDE047.toInt()
      canvas.drawCircle(cx, cy, 6f, paintFill)
    } else {
      rectTemp.set(ox, oy, ox + op.width, oy + op.height)
      paintFill.color = 0xFF57534E.toInt()
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintFill)
      subRectTemp.set(ox + 6f, oy + 4f, ox + op.width - 6f, oy + 10f)
      paintFill.color = 0xFFD97706.toInt()
      canvas.drawRoundRect(subRectTemp, 3f, 3f, paintFill)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintOutline)
    }
  }

  fun drawCollapsingPillar(
      canvas: Canvas,
      pillar: CollapsingPillar,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    val jitterX = if (pillar.phase == TrapPhase.WARNING) ((animTick % 4) - 1.5f) * 2.2f else 0f
    val px = pillar.x - camX + jitterX
    val py = pillar.topY + pillar.currentDropY - camY
    if (px + pillar.width < -64f || px > canvas.width + 64f) return

    rectTemp.set(px, py, px + pillar.width, py + pillar.height)
    paintFill.color = if (pillar.phase == TrapPhase.WARNING) 0xFF78350F.toInt() else 0xFF57534E.toInt()
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintFill)

    // Carved capital & vertical flutes
    subRectTemp.set(px - 4f, py, px + pillar.width + 4f, py + 14f)
    paintFill.color = if (pillar.phase == TrapPhase.WARNING) 0xFFF59E0B.toInt() else 0xFFA8A29E.toInt()
    canvas.drawRoundRect(subRectTemp, 4f, 4f, paintFill)
    canvas.drawRoundRect(subRectTemp, 4f, 4f, paintOutline)

    paintStroke.color = 0xFF292524.toInt()
    canvas.drawLine(px + pillar.width * 0.33f, py + 18f, px + pillar.width * 0.33f, py + pillar.height - 8f, paintStroke)
    canvas.drawLine(px + pillar.width * 0.67f, py + 18f, px + pillar.width * 0.67f, py + pillar.height - 8f, paintStroke)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)
  }

  fun drawWaterCurrent(
      canvas: Canvas,
      zone: WaterCurrentZone,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    val zx = zone.x - camX
    val zy = zone.y - camY
    if (zx + zone.width < -64f || zx > canvas.width + 64f) return

    rectTemp.set(zx, zy, zx + zone.width, zy + zone.height)
    paintFill.color = 0x550284C7
    canvas.drawRoundRect(rectTemp, 6f, 6f, paintFill)

    val dir = if (zone.flowVx >= 0f) 1f else -1f
    paintStroke.color = 0xAA7DD3FC.toInt()
    val spacing = 48f
    val shift = ((animTick * dir * 3.4f) % spacing + spacing) % spacing
    var wx = zx + shift
    while (wx < zx + zone.width - 14f) {
      val wy1 = zy + zone.height * 0.32f
      val wy2 = zy + zone.height * 0.68f
      canvas.drawLine(wx, wy1, wx + 18f * dir, wy1, paintStroke)
      canvas.drawLine(wx + 8f, wy2, wx + 24f * dir, wy2, paintStroke)
      // Directional chevron arrow
      canvas.drawLine(wx + 18f * dir, wy1, wx + 12f * dir, wy1 - 5f, paintStroke)
      canvas.drawLine(wx + 18f * dir, wy1, wx + 12f * dir, wy1 + 5f, paintStroke)
      wx += spacing
    }
  }

  fun drawLightningHazard(
      canvas: Canvas,
      hz: LightningHazard,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    val hx = hz.x - camX
    val topY = hz.topY - camY
    val botY = hz.groundY - camY
    if (hx < -80f || hx > canvas.width + 80f) return

    when (hz.phase) {
      TrapPhase.ARMED -> {
        // Subtle storm static node overhead
        paintGlow.color = 0x4438BDF8
        canvas.drawCircle(hx, topY + 12f, 8f, paintGlow)
      }
      TrapPhase.WARNING -> {
        val alpha = 110 + ((sin(animTick * 0.55) * 90).toInt()).coerceIn(-80, 120)
        rectTemp.set(hx - hz.width * 0.38f, topY, hx + hz.width * 0.38f, botY)
        paintFill.color = 0xFF38BDF8.toInt()
        paintFill.alpha = alpha
        canvas.drawRect(rectTemp, paintFill)
        paintFill.alpha = 255
        paintStroke.color = 0xFFE0F2FE.toInt()
        canvas.drawCircle(hx, botY - 4f, hz.width * 0.45f, paintStroke)
      }
      TrapPhase.ACTIVE -> {
        rectTemp.set(hx - hz.width * 0.5f, topY, hx + hz.width * 0.5f, botY)
        paintFill.color = 0xAABAE6FD.toInt()
        canvas.drawRect(rectTemp, paintFill)
        paintStroke.color = 0xFFFFFFFF.toInt()
        pathTemp.reset()
        pathTemp.moveTo(hx, topY)
        val steps = 6
        val stepH = (botY - topY) / steps
        for (i in 1..steps) {
          val offset = if (i % 2 == 0) -11f else 11f
          pathTemp.lineTo(hx + offset, topY + i * stepH)
        }
        canvas.drawPath(pathTemp, paintStroke)
        paintGlow.color = 0xCCFFFFFF.toInt()
        canvas.drawCircle(hx, botY - 4f, 18f, paintGlow)
      }
      TrapPhase.COOLDOWN -> Unit
    }
  }

  fun drawRelic(
      canvas: Canvas,
      relic: LevelRelic,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    if (relic.isCollected) return
    val rx = relic.x - camX
    val bob = (sin(animTick * 0.09 + relic.levelId) * 5f).toFloat()
    val ry = relic.y - camY + bob
    if (rx < -80f || rx > canvas.width + 80f) return

    paintGlow.color = 0x5538BDF8
    canvas.drawCircle(rx, ry, relic.radius + 10f, paintGlow)
    paintStroke.color = 0xFFBAE6FD.toInt()
    canvas.drawCircle(rx, ry, relic.radius + 2f, paintStroke)

    val r = relic.radius * 0.85f
    pathTemp.reset()
    pathTemp.moveTo(rx, ry - r)
    pathTemp.lineTo(rx + r * 0.72f, ry)
    pathTemp.lineTo(rx, ry + r)
    pathTemp.lineTo(rx - r * 0.72f, ry)
    pathTemp.close()
    paintFill.color = 0xFF0EA5E9.toInt()
    canvas.drawPath(pathTemp, paintFill)
    canvas.drawPath(pathTemp, paintOutline)
    paintGlow.color = 0xFFFDE047.toInt()
    canvas.drawCircle(rx, ry, 5f, paintGlow)
  }

  fun drawThemeAtmosphereOverlay(
      canvas: Canvas,
      engine: ChaosGameEngine,
      viewW: Float,
      viewH: Float,
      camX: Float,
      camY: Float,
      animTick: Int
  ) {
    // 1. Heavy Monsoon Rain streaks
    if (engine.level.hasHeavyRain) {
      for (i in 0..38) {
        val seed = i * 97
        val rx = ((seed + animTick * 7f) % (viewW + 120f)) - 60f
        val ry = ((i * 59f + animTick * 18f) % (viewH + 60f)) - 30f
        canvas.drawLine(rx, ry, rx - 10f, ry + 26f, paintRain)
      }
    }

    // 2. Lightning storm sky flash
    if (engine.lightningFlashFrames > 0) {
      val alpha = (engine.lightningFlashFrames * 24).coerceIn(0, 140)
      canvas.drawColor(Color.argb(alpha, 224, 242, 254))
    }

    // 3. Darkness Cavern Lantern radial lighting cutout around Aarav & active chimes/relics
    // Uses a cached 1/8-resolution offscreen lightmap bitmap instead of full-viewport saveLayer.
    if (engine.level.hasDarknessLantern && viewW > 0f && viewH > 0f) {
      val lmW = (viewW / 8f).toInt().coerceAtLeast(1)
      val lmH = (viewH / 8f).toInt().coerceAtLeast(1)
      var bmp = lightmapBitmap
      var lmCanvas = lightmapCanvas
      if (bmp == null || bmp.isRecycled || bmp.width != lmW || bmp.height != lmH || lmCanvas == null) {
        bmp?.recycle()
        bmp = Bitmap.createBitmap(lmW, lmH, Bitmap.Config.ARGB_8888)
        lmCanvas = Canvas(bmp)
        lightmapBitmap = bmp
        lightmapCanvas = lmCanvas
        lightmapSrcRect.set(0, 0, lmW, lmH)
      }
      lightmapDstRect.set(0f, 0f, viewW, viewH)

      val scaleX = lmW.toFloat() / viewW
      val scaleY = lmH.toFloat() / viewH
      lmCanvas.drawColor(0xA4040914.toInt(), PorterDuff.Mode.SRC)
      lmCanvas.save()
      lmCanvas.scale(scaleX, scaleY)

      val px = engine.player.x + engine.player.collisionWidth / 2f - camX
      val py = engine.player.y + engine.player.collisionHeight / 2f - camY
      lmCanvas.save()
      lmCanvas.translate(px, py)
      lmCanvas.drawCircle(0f, 0f, 250f, paintLanternCutout)
      lmCanvas.restore()

      for (sw in engine.soundSwitches) {
        val sx = sw.x - camX
        val sy = sw.y - camY
        if (sx in -160f..(viewW + 160f)) {
          lmCanvas.save()
          lmCanvas.translate(sx, sy)
          val s = if (sw.isActivated) 0.72f else 0.45f
          lmCanvas.scale(s, s)
          lmCanvas.drawCircle(0f, 0f, 250f, paintLanternCutout)
          lmCanvas.restore()
        }
      }

      for (relic in engine.relics) {
        if (relic.isCollected) continue
        val rx = relic.x - camX
        val ry = relic.y - camY
        if (rx in -140f..(viewW + 140f)) {
          lmCanvas.save()
          lmCanvas.translate(rx, ry)
          lmCanvas.scale(0.55f, 0.55f)
          lmCanvas.drawCircle(0f, 0f, 250f, paintLanternCutout)
          lmCanvas.restore()
        }
      }

      lmCanvas.restore()
      canvas.drawBitmap(bmp, lightmapSrcRect, lightmapDstRect, paintLightmap)

      // Subtle warm lantern rim around Aarav
      canvas.drawCircle(px, py, 96f, paintWarmLanternGlow)
    }
  }

  fun release() {
    lightmapBitmap?.recycle()
    lightmapBitmap = null
    lightmapCanvas = null
  }
}
