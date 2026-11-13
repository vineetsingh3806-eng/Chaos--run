package com.example.engine

import android.graphics.*
import com.example.data.AaravState
import com.example.data.Player
import kotlin.math.PI
import kotlin.math.sin

/**
 * Character renderer for Aarav the Seeker.
 * Implements clean vector character art faithful to the approved turnaround model sheet,
 * 8 animation states, and dynamic 4-node Verlet cloth physics for the traveler's scarf.
 */
class AaravCharacterRenderer {
  // Pre-allocated Paints to prevent GC allocation in 60 FPS loop
  private val paintTurban = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF59E0B.toInt() }
  private val paintTurbanDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD97706.toInt() }
  private val paintGogglesRim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB45309.toInt() }
  private val paintGogglesLens = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF67E8F9.toInt() }
  private val paintSkin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD49B6A.toInt() }
  private val paintSkinShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB57A4A.toInt() }
  private val paintVest = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D7377.toInt() }
  private val paintVestDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF094E51.toInt() }
  private val paintTunic = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFDFBF7.toInt() }
  private val paintPants = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC2B280.toInt() }
  private val paintBoots = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3E2723.toInt() }
  private val paintScarf = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD97706.toInt() }
  private val paintScarfDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB45309.toInt() }
  private val paintBelt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4A3525.toInt() }
  private val paintBuckle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt() }
  private val paintOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF1C1917.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
  }

  // Pre-allocated Paths
  private val pathScarf = Path()
  private val pathLimb = Path()
  private val rectTemp = RectF()

  // 4-Node Cloth Scarf Simulation Nodes
  private val scarfNodesX = FloatArray(4)
  private val scarfNodesY = FloatArray(4)
  private val scarfOldX = FloatArray(4)
  private val scarfOldY = FloatArray(4)

  init {
    for (i in 0..3) {
      scarfNodesX[i] = 0f
      scarfNodesY[i] = i * 6f
      scarfOldX[i] = 0f
      scarfOldY[i] = i * 6f
    }
  }

  fun drawPlayer(canvas: Canvas, player: Player, cameraX: Float, cameraY: Float) {
    if (player.invulnerableFrames % 4 in 1..2) {
      // Flash when damaged
      return
    }

    val screenX = player.x - cameraX
    val screenY = player.y - cameraY

    canvas.save()
    // Anchor to player center
    canvas.translate(screenX + player.collisionWidth / 2, screenY + player.collisionHeight)

    // Flip horizontally based on facing direction
    if (!player.facingRight) {
      canvas.scale(-1f, 1f)
    }

    // Apply Landing Squash & Stretch
    if (player.state == AaravState.LAND) {
      val squashProgress = (player.stateTimerFrames.coerceAtMost(6)) / 6f
      val scaleY = 0.80f + 0.20f * squashProgress
      val scaleX = 1.15f - 0.15f * squashProgress
      canvas.scale(scaleX, scaleY)
    } else if (player.state == AaravState.JUMP_UP) {
      canvas.scale(0.92f, 1.08f) // Vertical stretch
    }

    // Draw Character Components from back to front
    val animTick = player.stateTimerFrames

    // 1. Draw Trailing Scarf (Behind Body)
    drawDynamicScarf(canvas, player)

    // 2. Legs & Boots (Animated run stride / jump stance)
    drawLegs(canvas, player.state, animTick)

    // 3. Torso (Tunic, Utility Vest, Belt, Buckle)
    drawTorso(canvas, player.state, animTick)

    // 4. Head & Face (Travel Turban, Goggles, Expression)
    drawHead(canvas, player.state, animTick)

    // 5. Arms & Hands
    drawArms(canvas, player.state, animTick)

    canvas.restore()
  }

  private fun drawDynamicScarf(canvas: Canvas, player: Player) {
    val neckX = -4f
    val neckY = -38f

    // Verlet node physics update
    val wind = (sin((player.stateTimerFrames * 0.15).toDouble()) * 1.5).toFloat()
    val dragX = -player.vx * 1.8f
    val dragY = -player.vy * 0.5f

    scarfNodesX[0] = neckX
    scarfNodesY[0] = neckY

    for (i in 1..3) {
      val vx = (scarfNodesX[i] - scarfOldX[i]) * 0.85f
      val vy = (scarfNodesY[i] - scarfOldY[i]) * 0.85f

      scarfOldX[i] = scarfNodesX[i]
      scarfOldY[i] = scarfNodesY[i]

      scarfNodesX[i] += vx + dragX * 0.3f + wind * (i * 0.4f)
      scarfNodesY[i] += vy + 0.5f + dragY * 0.2f

      // Distance constraint
      val dx = scarfNodesX[i] - scarfNodesX[i - 1]
      val dy = scarfNodesY[i] - scarfNodesY[i - 1]
      val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.01f)
      val targetDist = 8f
      val diff = (dist - targetDist) / dist
      scarfNodesX[i] -= dx * 0.5f * diff
      scarfNodesY[i] -= dy * 0.5f * diff
    }

    // Render smooth Bézier cloth ribbon
    pathScarf.reset()
    pathScarf.moveTo(scarfNodesX[0], scarfNodesY[0])
    for (i in 1..3) {
      val prevX = scarfNodesX[i - 1]
      val prevY = scarfNodesY[i - 1]
      val currX = scarfNodesX[i]
      val currY = scarfNodesY[i]
      val midX = (prevX + currX) / 2
      val midY = (prevY + currY) / 2
      pathScarf.quadTo(prevX, prevY, midX, midY)
    }
    pathScarf.lineTo(scarfNodesX[3] - 4f, scarfNodesY[3] + 2f)
    for (i in 3 downTo 1) {
      pathScarf.lineTo(scarfNodesX[i] - 3f, scarfNodesY[i] + 3f)
    }
    pathScarf.close()

    canvas.drawPath(pathScarf, paintScarfDark)
    canvas.drawPath(pathScarf, paintScarf)
    canvas.drawPath(pathScarf, paintOutline)
  }

  private fun drawLegs(canvas: Canvas, state: AaravState, tick: Int) {
    val legY = -18f

    when (state) {
      AaravState.RUN -> {
        // 6-Frame running cycle animation
        val phase = (tick % 24) / 24f * 2.0 * PI
        val leftAngle = (sin(phase) * 32.0).toFloat()
        val rightAngle = (sin(phase + PI) * 32.0).toFloat()

        // Left Leg
        canvas.save()
        canvas.translate(-4f, legY)
        canvas.rotate(leftAngle)
        rectTemp.set(-3.5f, 0f, 3.5f, 14f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintPants)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        // Left Boot
        rectTemp.set(-4f, 11f, 5f, 18f)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintBoots)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
        canvas.restore()

        // Right Leg
        canvas.save()
        canvas.translate(4f, legY)
        canvas.rotate(rightAngle)
        rectTemp.set(-3.5f, 0f, 3.5f, 14f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintPants)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        // Right Boot
        rectTemp.set(-4f, 11f, 5f, 18f)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintBoots)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
        canvas.restore()
      }
      AaravState.JUMP_UP -> {
        // Leaping forward legs
        rectTemp.set(-8f, legY, -2f, legY + 12f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintPants)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        rectTemp.set(-9f, legY + 10f, 0f, legY + 16f)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintBoots)

        rectTemp.set(1f, legY, 7f, legY + 15f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintPants)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        rectTemp.set(0f, legY + 13f, 9f, legY + 19f)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintBoots)
      }
      else -> {
        // Standing / Idle stance
        // Left Leg
        rectTemp.set(-7f, legY, -1f, legY + 14f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintPants)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        rectTemp.set(-8f, legY + 11f, 0f, legY + 18f)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintBoots)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)

        // Right Leg
        rectTemp.set(1f, legY, 7f, legY + 14f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintPants)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        rectTemp.set(0f, legY + 11f, 8f, legY + 18f)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintBoots)
        canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
      }
    }
  }

  private fun drawTorso(canvas: Canvas, state: AaravState, tick: Int) {
    val breathe = if (state == AaravState.IDLE) (sin(tick * 0.08) * 0.8).toFloat() else 0f
    val torsoY = -34f + breathe

    // 1. Linen Cream Tunic
    rectTemp.set(-9f, torsoY, 9f, torsoY + 18f)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintTunic)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)

    // 2. Teal Explorer Vest
    rectTemp.set(-9.5f, torsoY, -2f, torsoY + 16f)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintVest)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)

    rectTemp.set(2f, torsoY, 9.5f, torsoY + 16f)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintVest)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)

    // Pockets on Vest
    rectTemp.set(-8f, torsoY + 8f, -4f, torsoY + 13f)
    canvas.drawRoundRect(rectTemp, 1f, 1f, paintVestDark)
    rectTemp.set(4f, torsoY + 8f, 8f, torsoY + 13f)
    canvas.drawRoundRect(rectTemp, 1f, 1f, paintVestDark)

    // 3. Leather Belt & Brass Buckle
    rectTemp.set(-9f, torsoY + 15f, 9f, torsoY + 18.5f)
    canvas.drawRect(rectTemp, paintBelt)
    canvas.drawRect(rectTemp, paintOutline)

    rectTemp.set(-2.5f, torsoY + 14.5f, 2.5f, torsoY + 19f)
    canvas.drawRoundRect(rectTemp, 1f, 1f, paintBuckle)
    canvas.drawRoundRect(rectTemp, 1f, 1f, paintOutline)
  }

  private fun drawHead(canvas: Canvas, state: AaravState, tick: Int) {
    val breathe = if (state == AaravState.IDLE) (sin(tick * 0.08) * 0.8).toFloat() else 0f
    val headY = -43f + breathe

    // Face / Head base
    rectTemp.set(-7f, headY, 7f, headY + 12f)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintSkin)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)

    // Eyes
    paintOutline.strokeWidth = 2f
    canvas.drawCircle(3f, headY + 5.5f, 1.2f, paintOutline)

    // Smile / expression
    if (state == AaravState.VICTORY) {
      canvas.drawArc(RectF(-1f, headY + 6f, 5f, headY + 9f), 0f, 180f, false, paintOutline)
    } else {
      canvas.drawLine(1f, headY + 8f, 4f, headY + 8f, paintOutline)
    }
    paintOutline.strokeWidth = 2.5f

    // Saffron Travel Turban (Gurban dome with folds)
    rectTemp.set(-8.5f, headY - 11f, 8.5f, headY + 2f)
    canvas.drawRoundRect(rectTemp, 8f, 8f, paintTurban)
    canvas.drawRoundRect(rectTemp, 8f, 8f, paintOutline)

    // Turban folds
    canvas.drawLine(-7f, headY - 4f, 7f, headY - 2f, paintTurbanDark)
    canvas.drawLine(-6f, headY - 7f, 6f, headY - 6f, paintTurbanDark)

    // Brass Explorer Goggles on Turban Brim
    rectTemp.set(-1f, headY - 4f, 5f, headY)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintGogglesRim)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
    rectTemp.set(0.5f, headY - 3f, 4f, headY - 1f)
    canvas.drawRoundRect(rectTemp, 1.5f, 1.5f, paintGogglesLens)

    // Scarf Drape around Neck
    rectTemp.set(-7.5f, headY + 9f, 7.5f, headY + 13.5f)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintScarf)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
  }

  private fun drawArms(canvas: Canvas, state: AaravState, tick: Int) {
    val armY = -31f

    when (state) {
      AaravState.RUN -> {
        val phase = (tick % 24) / 24f * 2.0 * PI
        val armSwing = (sin(phase) * 35.0).toFloat()

        // Right Arm (Forward/Back)
        canvas.save()
        canvas.translate(6f, armY)
        canvas.rotate(armSwing)
        rectTemp.set(-2.5f, 0f, 2.5f, 13f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintSkin)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        // Hand
        canvas.drawCircle(0f, 13f, 2.5f, paintSkin)
        canvas.restore()
      }
      AaravState.VICTORY -> {
        // Triumphant Fist Pump
        canvas.save()
        canvas.translate(6f, armY)
        canvas.rotate(-130f)
        rectTemp.set(-2.5f, 0f, 2.5f, 14f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintSkin)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        canvas.drawCircle(0f, 14f, 3f, paintSkin)
        canvas.restore()
      }
      AaravState.JUMP_UP -> {
        // Reaching upward
        canvas.save()
        canvas.translate(6f, armY)
        canvas.rotate(-60f)
        rectTemp.set(-2.5f, 0f, 2.5f, 12f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintSkin)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        canvas.restore()
      }
      else -> {
        // Resting arm by side
        canvas.save()
        canvas.translate(6f, armY)
        rectTemp.set(-2.5f, 0f, 2.5f, 12f)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintSkin)
        canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
        canvas.drawCircle(0f, 12f, 2.2f, paintSkin)
        canvas.restore()
      }
    }
  }
}
