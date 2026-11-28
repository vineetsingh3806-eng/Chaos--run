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

  private val paintSky = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintMist = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintSunHalo = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintSunCore = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintCloud = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintHillFar = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintHillMid = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintTreeTrunk = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B5344.toInt() }
  private val paintTreeCanopy = Paint(Paint.ANTI_ALIAS_FLAG)
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
    color = 0x66E0F2FE
    style = Paint.Style.STROKE
    strokeWidth = 1.9f
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
  private var cachedMistShader: LinearGradient? = null

  fun drawThemeBackdrop(
      canvas: Canvas,
      theme: LevelTheme,
      viewW: Float,
      viewH: Float,
      cameraX: Float,
      animTick: Int
  ) {
    if (viewW <= 0f || viewH <= 0f) return

    if (cachedTheme != theme || cachedHeight != viewH || cachedBgShader == null || cachedMistShader == null) {
      val skyColors = when (theme) {
        LevelTheme.WHISPERING_WOODS -> intArrayOf(0xFF38BDF8.toInt(), 0xFF8BE0FF.toInt(), 0xFFE6FCF2.toInt())
        LevelTheme.HOLLOW_OF_ECHOES -> intArrayOf(0xFF38A9E0.toInt(), 0xFF8BE3FA.toInt(), 0xFFE3FBFE.toInt())
        LevelTheme.RUINS_OF_LOST_KING -> intArrayOf(0xFF56B4E9.toInt(), 0xFF9ED5FA.toInt(), 0xFFFEF3C7.toInt())
        LevelTheme.MONSOON_RUN -> intArrayOf(0xFF42A5E5.toInt(), 0xFF8DD7FA.toInt(), 0xFFE4F8FD.toInt())
        LevelTheme.TEMPLE_OF_LAST_LIGHT -> intArrayOf(0xFF5A8FF2.toInt(), 0xFFA2CAFC.toInt(), 0xFFFDE68A.toInt())
      }
      val mistColor = when (theme) {
        LevelTheme.WHISPERING_WOODS -> 0x4CEEFDF7
        LevelTheme.HOLLOW_OF_ECHOES -> 0x4AE6FCFF
        LevelTheme.RUINS_OF_LOST_KING -> 0x48FEF6D8
        LevelTheme.MONSOON_RUN -> 0x4AE8FAFF
        LevelTheme.TEMPLE_OF_LAST_LIGHT -> 0x45FEF3C7
      }
      cachedBgShader = LinearGradient(0f, 0f, 0f, viewH, skyColors, floatArrayOf(0f, 0.56f, 1f), Shader.TileMode.CLAMP)
      cachedMistShader = LinearGradient(0f, viewH * 0.54f, 0f, viewH, intArrayOf(0x00FFFFFF, mistColor), null, Shader.TileMode.CLAMP)
      cachedTheme = theme
      cachedHeight = viewH
      paintSky.shader = cachedBgShader
      paintMist.shader = cachedMistShader
    }
    canvas.drawRect(0f, 0f, viewW, viewH, paintSky)

    when (theme) {
      LevelTheme.WHISPERING_WOODS -> drawWoodsParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.HOLLOW_OF_ECHOES -> drawCavernParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.RUINS_OF_LOST_KING -> drawRuinsParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.MONSOON_RUN -> drawMonsoonParallax(canvas, viewW, viewH, cameraX, animTick)
      LevelTheme.TEMPLE_OF_LAST_LIGHT -> drawTempleParallax(canvas, viewW, viewH, cameraX, animTick)
    }

    // Soft daylight horizon haze so foreground platforms, traps, spikes, and character stay razor-sharp
    canvas.drawRect(0f, viewH * 0.54f, viewW, viewH, paintMist)
  }

  private fun drawCartoonCloud(canvas: Canvas, cx: Float, cy: Float, scale: Float, color: Int) {
    paintCloud.color = color
    canvas.drawCircle(cx - 24f * scale, cy + 5f * scale, 18f * scale, paintCloud)
    canvas.drawCircle(cx - 2f * scale, cy - 6f * scale, 25f * scale, paintCloud)
    canvas.drawCircle(cx + 22f * scale, cy + 4f * scale, 17f * scale, paintCloud)
    rectTemp.set(cx - 26f * scale, cy + 1f * scale, cx + 26f * scale, cy + 21f * scale)
    canvas.drawRoundRect(rectTemp, 10f * scale, 10f * scale, paintCloud)
  }

  private fun drawDistantMountainRange(
      canvas: Canvas,
      viewW: Float,
      viewH: Float,
      camX: Float,
      parallax: Float,
      span: Float,
      baseRatio: Float,
      peakHeight: Float,
      color: Int
  ) {
    paintHillFar.color = color
    val offset = (camX * parallax) % span
    var mx = -offset - span
    var idx = 0
    while (mx < viewW + span) {
      val baseY = viewH * baseRatio
      val h = peakHeight * (0.82f + (idx % 3) * 0.16f)
      pathTemp.reset()
      pathTemp.moveTo(mx, viewH)
      pathTemp.lineTo(mx, baseY)
      pathTemp.quadTo(mx + span * 0.45f, baseY - h, mx + span * 0.55f, baseY - h * 0.94f)
      pathTemp.quadTo(mx + span * 0.85f, baseY - h * 0.35f, mx + span, baseY)
      pathTemp.lineTo(mx + span, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillFar)
      mx += span
      idx++
    }
  }

  private fun drawWoodsParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Cheerful cartoon sun with soft golden halo
    val sunX = viewW * 0.82f - ((camX * 0.02f) % 80f)
    val sunY = viewH * 0.18f
    paintSunHalo.color = 0x5CEEF08A
    canvas.drawCircle(sunX, sunY, 58f, paintSunHalo)
    paintSunCore.color = 0xFFFDE047.toInt()
    canvas.drawCircle(sunX, sunY, 30f, paintSunCore)

    // Fluffy drifting cartoon clouds
    val cloudSpan = 380f
    val cloudOffset = (camX * 0.05f + tick * 0.14f) % cloudSpan
    var cx = -cloudOffset - cloudSpan * 0.5f
    var cIdx = 0
    while (cx < viewW + cloudSpan) {
      val cy = viewH * (0.13f + (cIdx % 3) * 0.06f)
      val scale = 0.85f + (cIdx % 2) * 0.25f
      drawCartoonCloud(canvas, cx + 110f, cy, scale, 0xF2FFFFFF.toInt())
      cx += cloudSpan
      cIdx++
    }

    // Far atmospheric blue-mint mountains (0.08x parallax)
    drawDistantMountainRange(
        canvas, viewW, viewH, camX,
        parallax = 0.08f, span = 420f, baseRatio = 0.68f, peakHeight = 128f,
        color = 0xFF9EDAE0.toInt()
    )

    // Mid-far rolling mint hills (0.14x parallax)
    paintHillFar.color = 0xFF7CD4A6.toInt()
    val farSpan = 340f
    val farOffset = (camX * 0.14f) % farSpan
    var hx = -farOffset - farSpan
    while (hx < viewW + farSpan) {
      val baseY = viewH * 0.74f
      pathTemp.reset()
      pathTemp.moveTo(hx, viewH)
      pathTemp.lineTo(hx, baseY)
      pathTemp.quadTo(hx + farSpan * 0.5f, baseY - 76f, hx + farSpan, baseY)
      pathTemp.lineTo(hx + farSpan, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillFar)
      hx += farSpan
    }

    // Foreground meadow ridge + bright cartoon trees (0.24x parallax)
    paintHillMid.color = 0xFF56BD87.toInt()
    val midSpan = 260f
    val midOffset = (camX * 0.24f) % midSpan
    hx = -midOffset - midSpan
    var idx = 0
    while (hx < viewW + midSpan) {
      val ridgeY = viewH * 0.80f
      pathTemp.reset()
      pathTemp.moveTo(hx, viewH)
      pathTemp.lineTo(hx, ridgeY)
      pathTemp.quadTo(hx + midSpan * 0.5f, ridgeY - 46f, hx + midSpan, ridgeY)
      pathTemp.lineTo(hx + midSpan, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillMid)

      val tx = hx + midSpan * 0.48f
      val treeBaseY = ridgeY - 20f
      val treeH = if (idx % 2 == 0) 56f else 44f
      rectTemp.set(tx - 5f, treeBaseY - treeH, tx + 5f, treeBaseY + 12f)
      canvas.drawRoundRect(rectTemp, 3f, 3f, paintTreeTrunk)

      paintTreeCanopy.color = if (idx % 2 == 0) 0xFF399E6F.toInt() else 0xFF45AF7D.toInt()
      canvas.drawCircle(tx, treeBaseY - treeH - 12f, 23f, paintTreeCanopy)
      canvas.drawCircle(tx - 15f, treeBaseY - treeH + 2f, 16f, paintTreeCanopy)
      canvas.drawCircle(tx + 15f, treeBaseY - treeH + 2f, 16f, paintTreeCanopy)

      hx += midSpan
      idx++
    }
  }

  private fun drawCavernParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Bright sunlit crystal valley sky opening
    val sunX = viewW * 0.78f - ((camX * 0.02f) % 80f)
    val sunY = viewH * 0.19f
    paintSunHalo.color = 0x55BAE6FD
    canvas.drawCircle(sunX, sunY, 56f, paintSunHalo)
    paintSunCore.color = 0xFFF0F9FF.toInt()
    canvas.drawCircle(sunX, sunY, 27f, paintSunCore)

    // Light crisp clouds drifting across sunlit canyon
    val cloudSpan = 400f
    val cloudOffset = (camX * 0.05f + tick * 0.12f) % cloudSpan
    var cx = -cloudOffset - cloudSpan * 0.5f
    var cIdx = 0
    while (cx < viewW + cloudSpan) {
      val cy = viewH * (0.14f + (cIdx % 2) * 0.07f)
      drawCartoonCloud(canvas, cx + 95f, cy, 0.9f, 0xE6FFFFFF.toInt())
      cx += cloudSpan
      cIdx++
    }

    // Far sunlit crystal canyon peaks (0.10x parallax)
    drawDistantMountainRange(
        canvas, viewW, viewH, camX,
        parallax = 0.10f, span = 380f, baseRatio = 0.69f, peakHeight = 118f,
        color = 0xFF8CD5EB.toInt()
    )

    // Mid-far turquoise ridges & subtle crystal spires (0.18x parallax)
    paintHillFar.color = 0xFF63BED8.toInt()
    val hillSpan = 320f
    val hillOffset = (camX * 0.18f) % hillSpan
    var x = -hillOffset - hillSpan
    var idx = 0
    while (x < viewW + hillSpan) {
      val baseY = viewH * 0.76f
      pathTemp.reset()
      pathTemp.moveTo(x, viewH)
      pathTemp.lineTo(x, baseY)
      pathTemp.quadTo(x + hillSpan * 0.5f, baseY - 68f, x + hillSpan, baseY)
      pathTemp.lineTo(x + hillSpan, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillFar)
      if (idx % 2 == 0) {
        paintGlow.color = 0x66CCFBF1
        val pulse = 4.5f + (sin(tick * 0.05 + idx) * 1.5f).toFloat()
        canvas.drawCircle(x + hillSpan * 0.5f, baseY - 56f, pulse, paintGlow)
      }
      x += hillSpan
      idx++
    }

    // Clean sunlit stone arches (0.28x parallax)
    paintSilhouette.color = 0xFF53A3BC.toInt()
    val archSpan = 320f
    val offset2 = ((camX * 0.28f) % archSpan)
    x = -offset2 - archSpan
    while (x < viewW + archSpan) {
      rectTemp.set(x + 24f, viewH * 0.50f, x + 60f, viewH)
      canvas.drawRoundRect(rectTemp, 10f, 10f, paintSilhouette)
      x += archSpan
    }
  }

  private fun drawRuinsParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Bright cheerful morning sun & fluffy ivory clouds
    val sunX = viewW * 0.78f - ((camX * 0.02f) % 80f)
    val sunY = viewH * 0.20f
    paintSunHalo.color = 0x55FDE047
    canvas.drawCircle(sunX, sunY, 64f, paintSunHalo)
    paintSunCore.color = 0xFFFEF08A.toInt()
    canvas.drawCircle(sunX, sunY, 33f, paintSunCore)

    val cloudSpan = 420f
    val cloudOffset = (camX * 0.06f + tick * 0.12f) % cloudSpan
    var cx = -cloudOffset - cloudSpan * 0.5f
    var cIdx = 0
    while (cx < viewW + cloudSpan) {
      val cy = viewH * (0.14f + (cIdx % 2) * 0.08f)
      drawCartoonCloud(canvas, cx + 100f, cy, 0.95f, 0xECFFFBEB.toInt())
      cx += cloudSpan
      cIdx++
    }

    // Distant warm sandstone peaks (0.09x parallax)
    drawDistantMountainRange(
        canvas, viewW, viewH, camX,
        parallax = 0.09f, span = 420f, baseRatio = 0.69f, peakHeight = 112f,
        color = 0xFFEBC8A6.toInt()
    )

    // Mid-far rolling golden dunes (0.16x parallax)
    paintHillFar.color = 0xFFE0AB82.toInt()
    val duneSpan = 360f
    val duneOffset = (camX * 0.16f) % duneSpan
    var x = -duneOffset - duneSpan
    while (x < viewW + duneSpan) {
      val baseY = viewH * 0.75f
      pathTemp.reset()
      pathTemp.moveTo(x, viewH)
      pathTemp.lineTo(x, baseY)
      pathTemp.quadTo(x + duneSpan * 0.5f, baseY - 68f, x + duneSpan, baseY)
      pathTemp.lineTo(x + duneSpan, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillFar)
      x += duneSpan
    }

    // Mid-ground sunlit sandstone colonnade arches (0.28x parallax)
    paintSilhouette.color = 0xFFC88B6E.toInt()
    val archSpan = 320f
    val offset2 = ((camX * 0.28f) % archSpan)
    x = -offset2 - archSpan
    var idx = 0
    while (x < viewW + archSpan) {
      val topY = viewH * (0.52f + (idx % 2) * 0.05f)
      rectTemp.set(x + 24f, topY, x + 58f, viewH)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintSilhouette)
      rectTemp.set(x + 174f, topY, x + 208f, viewH)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintSilhouette)
      rectTemp.set(x + 16f, topY - 8f, x + 216f, topY + 14f)
      canvas.drawRoundRect(rectTemp, 8f, 8f, paintSilhouette)
      x += archSpan
      idx++
    }
  }

  private fun drawMonsoonParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Bright sunlit monsoon sky break + crisp bright clouds
    val sunX = viewW * 0.80f - ((camX * 0.02f) % 80f)
    val sunY = viewH * 0.18f
    paintSunHalo.color = 0x4DFEF9C3
    canvas.drawCircle(sunX, sunY, 50f, paintSunHalo)
    paintSunCore.color = 0xFFFEF08A.toInt()
    canvas.drawCircle(sunX, sunY, 24f, paintSunCore)

    val cloudSpan = 320f
    val offset1 = ((camX * 0.08f + tick * 0.24f) % cloudSpan)
    var x = -offset1 - cloudSpan
    var idx = 0
    while (x < viewW + cloudSpan) {
      val cy = viewH * (0.14f + (idx % 2) * 0.07f)
      drawCartoonCloud(canvas, x + 130f, cy, 1.15f, 0xE8F0F9FF.toInt())
      x += cloudSpan
      idx++
    }

    // Distant lush teal-blue monsoon mountains (0.10x parallax)
    drawDistantMountainRange(
        canvas, viewW, viewH, camX,
        parallax = 0.10f, span = 400f, baseRatio = 0.68f, peakHeight = 122f,
        color = 0xFF83C5DB.toInt()
    )

    // Mid-far tropical jade ridge (0.18x parallax)
    paintHillFar.color = 0xFF5CB3B8.toInt()
    val ridgeSpan = 340f
    val ridgeOffset = (camX * 0.18f) % ridgeSpan
    x = -ridgeOffset - ridgeSpan
    while (x < viewW + ridgeSpan) {
      val baseY = viewH * 0.74f
      pathTemp.reset()
      pathTemp.moveTo(x, viewH)
      pathTemp.lineTo(x, baseY)
      pathTemp.quadTo(x + ridgeSpan * 0.5f, baseY - 76f, x + ridgeSpan, baseY)
      pathTemp.lineTo(x + ridgeSpan, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillFar)
      x += ridgeSpan
    }

    // Mid-ground sunlit stone aqueduct arches (0.28x parallax)
    paintSilhouette.color = 0xFF5A9AB5.toInt()
    val span = 300f
    val offset2 = ((camX * 0.28f) % span)
    x = -offset2 - span
    while (x < viewW + span) {
      rectTemp.set(x + 18f, viewH * 0.56f, x + 54f, viewH)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintSilhouette)
      rectTemp.set(x + 144f, viewH * 0.56f, x + 180f, viewH)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintSilhouette)
      rectTemp.set(x + 6f, viewH * 0.53f, x + 196f, viewH * 0.58f)
      canvas.drawRoundRect(rectTemp, 8f, 8f, paintSilhouette)
      x += span
    }
  }

  private fun drawTempleParallax(canvas: Canvas, viewW: Float, viewH: Float, camX: Float, tick: Int) {
    // Bright radiant temple sun & crisp warm clouds
    val sunX = viewW * 0.79f - ((camX * 0.02f) % 80f)
    val sunY = viewH * 0.19f
    paintSunHalo.color = 0x55FEF08A
    canvas.drawCircle(sunX, sunY, 62f, paintSunHalo)
    paintSunCore.color = 0xFFFEF08A.toInt()
    canvas.drawCircle(sunX, sunY, 32f, paintSunCore)

    val cloudSpan = 400f
    val cloudOffset = (camX * 0.05f + tick * 0.11f) % cloudSpan
    var cx = -cloudOffset - cloudSpan * 0.5f
    var cIdx = 0
    while (cx < viewW + cloudSpan) {
      val cy = viewH * (0.13f + (cIdx % 2) * 0.07f)
      drawCartoonCloud(canvas, cx + 100f, cy, 0.92f, 0xEAFFFBEB.toInt())
      cx += cloudSpan
      cIdx++
    }

    // Soft daylight sunbeams
    paintGlow.color = 0x22FEF08A
    for (i in 0..2) {
      val bx = ((i * 360f - camX * 0.10f) % (viewW + 340f) + (viewW + 340f)) % (viewW + 340f) - 100f
      pathTemp.reset()
      pathTemp.moveTo(bx, 0f)
      pathTemp.lineTo(bx + 72f, 0f)
      pathTemp.lineTo(bx + 190f, viewH)
      pathTemp.lineTo(bx + 84f, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintGlow)
    }

    // Far sunlit amethyst-indigo ridges (0.10x parallax)
    drawDistantMountainRange(
        canvas, viewW, viewH, camX,
        parallax = 0.10f, span = 400f, baseRatio = 0.69f, peakHeight = 114f,
        color = 0xFF9E9FE8.toInt()
    )

    // Mid-far warm sunlit hills (0.18x parallax)
    paintHillFar.color = 0xFF817FD4.toInt()
    val hillSpan = 340f
    val hillOffset = (camX * 0.18f) % hillSpan
    var x = -hillOffset - hillSpan
    while (x < viewW + hillSpan) {
      val baseY = viewH * 0.75f
      pathTemp.reset()
      pathTemp.moveTo(x, viewH)
      pathTemp.lineTo(x, baseY)
      pathTemp.quadTo(x + hillSpan * 0.5f, baseY - 64f, x + hillSpan, baseY)
      pathTemp.lineTo(x + hillSpan, viewH)
      pathTemp.close()
      canvas.drawPath(pathTemp, paintHillFar)
      x += hillSpan
    }

    // Mid-ground carved temple spires & sunlit domes (0.28x parallax)
    paintSilhouette.color = 0xFF7B6DC2.toInt()
    val span = 280f
    val offset = ((camX * 0.28f) % span)
    x = -offset - span
    while (x < viewW + span) {
      rectTemp.set(x + 30f, viewH * 0.42f, x + 70f, viewH)
      canvas.drawRoundRect(rectTemp, 8f, 8f, paintSilhouette)
      rectTemp.set(x + 22f, viewH * 0.40f, x + 78f, viewH * 0.45f)
      canvas.drawRoundRect(rectTemp, 6f, 6f, paintSilhouette)
      canvas.drawCircle(x + 170f, viewH * 0.56f, 26f, paintSilhouette)
      x += span
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
    // 1. Clean Monsoon Rain streaks (subtle and uncluttered)
    if (engine.level.hasHeavyRain) {
      for (i in 0..21) {
        val seed = i * 131
        val rx = ((seed + animTick * 6f) % (viewW + 120f)) - 60f
        val ry = ((i * 73f + animTick * 15f) % (viewH + 60f)) - 30f
        canvas.drawLine(rx, ry, rx - 9f, ry + 22f, paintRain)
      }
    }

    // 2. Lightning storm sky flash
    if (engine.lightningFlashFrames > 0) {
      val alpha = (engine.lightningFlashFrames * 24).coerceIn(0, 140)
      canvas.drawColor(Color.argb(alpha, 224, 242, 254))
    }

    // 3. Darkness Cavern / Lantern Mist radial lighting cutout around player, chimes, relics & exit gate
    // Uses a cached 1/8-resolution offscreen lightmap bitmap instead of full-viewport saveLayer.
    val useLantern = engine.level.hasDarknessLantern || engine.activeModifiers.contains(ChaosModifier.LANTERN_MIST)
    if (useLantern && viewW > 0f && viewH > 0f) {
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
      val fogColor = if (engine.activeModifiers.contains(ChaosModifier.LANTERN_MIST) && !engine.level.hasDarknessLantern) {
        0x94070B18.toInt()
      } else {
        0xA4040914.toInt()
      }
      lmCanvas.drawColor(fogColor, PorterDuff.Mode.SRC)
      lmCanvas.save()
      lmCanvas.scale(scaleX, scaleY)

      val px = engine.player.x + engine.player.collisionWidth / 2f - camX
      val py = engine.player.y + engine.player.collisionHeight / 2f - camY
      lmCanvas.save()
      lmCanvas.translate(px, py)
      lmCanvas.drawCircle(0f, 0f, 250f, paintLanternCutout)
      lmCanvas.restore()

      val gx = engine.currentExitX + 24f - camX
      val gy = engine.currentExitY + 24f - camY
      if (gx in -180f..(viewW + 180f)) {
        lmCanvas.save()
        lmCanvas.translate(gx, gy)
        lmCanvas.scale(0.65f, 0.65f)
        lmCanvas.drawCircle(0f, 0f, 250f, paintLanternCutout)
        lmCanvas.restore()
      }

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

      // Subtle warm lantern rim around player
      canvas.drawCircle(px, py, 96f, paintWarmLanternGlow)
    }
  }

  fun release() {
    lightmapBitmap?.recycle()
    lightmapBitmap = null
    lightmapCanvas = null
  }
}
