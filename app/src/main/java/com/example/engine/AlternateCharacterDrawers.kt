package com.example.engine

import android.graphics.*
import com.example.data.AaravState
import com.example.data.Player
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hand-drawn cartoon vector drawers for Golu, Vega, and Roko.
 * Matches Aarav's bold black outline (2.5f), flat vibrant palette, minimal shading,
 * exaggerated expressions, and zero per-frame allocations.
 */
internal class AlternateCharacterDrawers {
  private val paintOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF1C1917.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.5f
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
  }
  private val paintOutlineThin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0xFF1C1917.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2.0f
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
  }
  private val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
  private val paintPupil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1C1917.toInt() }
  private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
  private val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG)
  private val rectTemp = RectF()
  private val pathScratch = Path()

  // ---------------- CHARACTER A: GOLU (The Snack Chef) ----------------

  fun drawGolu(canvas: Canvas, player: Player, tick: Int) {
    val state = player.state
    val breathe = if (state == AaravState.IDLE) (sin(tick * 0.09) * 0.9).toFloat() else 0f
    val bob = when (state) {
      AaravState.RUN -> kotlin.math.abs(sin(tick * 0.26) * 2.2).toFloat() * -1f
      else -> breathe
    }

    // Trailing apron sash ribbon behind waist
    drawGoluApronRibbon(canvas, player, tick, bob)

    // Stubby legs + chunky red sneakers
    drawGoluLegs(canvas, state, tick)

    // Plump coral-berry sweater torso + blue pocket apron + golden snack emblem
    val torsoY = -33f + bob
    paintFill.color = 0xFFEF4444.toInt()
    rectTemp.set(-11.5f, torsoY, 11.5f, torsoY + 19f)
    canvas.drawRoundRect(rectTemp, 8f, 8f, paintFill)
    canvas.drawRoundRect(rectTemp, 8f, 8f, paintOutline)

    // Apron bib
    paintFill.color = 0xFF38BDF8.toInt()
    rectTemp.set(-7.5f, torsoY + 4f, 7.5f, torsoY + 18.5f)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintFill)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)

    // Golden samosa/snack emblem on apron
    paintFill.color = 0xFFFDE047.toInt()
    pathScratch.reset()
    pathScratch.moveTo(0f, torsoY + 7f)
    pathScratch.lineTo(-4f, torsoY + 14f)
    pathScratch.lineTo(4f, torsoY + 14f)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    canvas.drawPath(pathScratch, paintOutlineThin)

    // Chubby head + rosy cheeks + oversized googly eyes
    val headY = -44f + bob
    paintFill.color = 0xFFF7C59F.toInt()
    rectTemp.set(-9f, headY, 9f, headY + 14f)
    canvas.drawRoundRect(rectTemp, 7f, 7f, paintFill)
    canvas.drawRoundRect(rectTemp, 7f, 7f, paintOutline)

    // Rosy cheek
    paintFill.color = 0xFFFB7185.toInt()
    canvas.drawCircle(1.5f, headY + 9.2f, 2.2f, paintFill)

    // Googly eyes & funny mouth expression
    val eyeJiggleY = when (state) {
      AaravState.JUMP_UP -> -0.9f
      AaravState.FALL_DOWN -> 1.1f
      else -> 0f
    }
    if (state == AaravState.DEATH) {
      canvas.drawCircle(0.2f, headY + 5.2f, 3.2f, paintWhite)
      canvas.drawCircle(0.2f, headY + 5.2f, 3.2f, paintOutlineThin)
      canvas.drawLine(-1.2f, headY + 3.8f, 1.6f, headY + 6.6f, paintOutlineThin)
      canvas.drawLine(1.6f, headY + 3.8f, -1.2f, headY + 6.6f, paintOutlineThin)

      canvas.drawCircle(5.6f, headY + 5.4f, 2.6f, paintWhite)
      canvas.drawCircle(5.6f, headY + 5.4f, 2.6f, paintOutlineThin)
      canvas.drawCircle(6.0f, headY + 5.0f, 1.1f, paintPupil)

      // Shocked wide mouth
      paintFill.color = 0xFF991B1B.toInt()
      canvas.drawCircle(3.2f, headY + 10.6f, 2.4f, paintFill)
      canvas.drawCircle(3.2f, headY + 10.6f, 2.4f, paintOutlineThin)
    } else if (state == AaravState.DAMAGE) {
      // Squeezed "> <" wince + wavy mouth
      canvas.drawLine(-1.5f, headY + 4f, 1.5f, headY + 5.5f, paintOutlineThin)
      canvas.drawLine(1.5f, headY + 5.5f, -1.5f, headY + 7f, paintOutlineThin)
      canvas.drawLine(7.0f, headY + 4f, 4.0f, headY + 5.5f, paintOutlineThin)
      canvas.drawLine(4.0f, headY + 5.5f, 7.0f, headY + 7f, paintOutlineThin)
      canvas.drawCircle(3.2f, headY + 10.4f, 1.7f, paintOutlineThin)
    } else {
      // Mismatched comical googly eyes
      canvas.drawCircle(0.2f, headY + 5.2f, 3.3f, paintWhite)
      canvas.drawCircle(0.2f, headY + 5.2f, 3.3f, paintOutlineThin)
      canvas.drawCircle(5.8f, headY + 5.4f, 2.6f, paintWhite)
      canvas.drawCircle(5.8f, headY + 5.4f, 2.6f, paintOutlineThin)

      if (state == AaravState.VICTORY) {
        rectTemp.set(-1.5f, headY + 4f, 2f, headY + 7f)
        canvas.drawArc(rectTemp, 190f, 160f, false, paintOutlineThin)
        rectTemp.set(4.2f, headY + 4.2f, 7.2f, headY + 6.8f)
        canvas.drawArc(rectTemp, 190f, 160f, false, paintOutlineThin)
        paintFill.color = 0xFF991B1B.toInt()
        rectTemp.set(0.5f, headY + 8.5f, 6.2f, headY + 12.5f)
        canvas.drawArc(rectTemp, 0f, 180f, true, paintFill)
        canvas.drawArc(rectTemp, 0f, 180f, true, paintOutlineThin)
      } else {
        canvas.drawCircle(1.0f, headY + 5.0f + eyeJiggleY, 1.3f, paintPupil)
        canvas.drawCircle(6.3f, headY + 5.6f - eyeJiggleY * 0.6f, 1.1f, paintPupil)
        if (state == AaravState.FALL_DOWN || state == AaravState.JUMP_UP) {
          canvas.drawCircle(3.5f, headY + 10.5f, 1.8f, paintOutlineThin)
        } else {
          rectTemp.set(1.2f, headY + 8.6f, 5.8f, headY + 11.4f)
          canvas.drawArc(rectTemp, 10f, 160f, false, paintOutlineThin)
        }
      }
    }

    // Oversized tilted chef pot-hat with saffron band & springy green sprout
    canvas.save()
    canvas.translate(-1f, headY + 1f)
    val hatTilt = when (state) {
      AaravState.RUN -> -14f + sin(tick * 0.35).toFloat() * 4f
      AaravState.JUMP_UP -> -18f
      AaravState.FALL_DOWN -> 8f
      else -> -10f
    }
    canvas.rotate(hatTilt)
    // Puffy hat crown
    paintFill.color = 0xFFFFFBEB.toInt()
    rectTemp.set(-10.5f, -15f, 9.5f, -3f)
    canvas.drawRoundRect(rectTemp, 7f, 7f, paintFill)
    canvas.drawRoundRect(rectTemp, 7f, 7f, paintOutline)
    // Saffron hat band
    paintFill.color = 0xFFF59E0B.toInt()
    rectTemp.set(-9f, -5.5f, 8.5f, 0f)
    canvas.drawRoundRect(rectTemp, 2.5f, 2.5f, paintFill)
    canvas.drawRoundRect(rectTemp, 2.5f, 2.5f, paintOutline)
    // Cute green sprout on top
    paintFill.color = 0xFF10B981.toInt()
    canvas.drawLine(0f, -15f, -1f, -19f, paintOutlineThin)
    rectTemp.set(-4.5f, -21.5f, 2.5f, -17.5f)
    canvas.drawOval(rectTemp, paintFill)
    canvas.drawOval(rectTemp, paintOutlineThin)
    canvas.restore()

    // Foreground chubby arm
    drawSimpleArm(canvas, state, tick, armX = 7.5f, armY = -29f + bob, sleeveColor = 0xFFEF4444.toInt(), handColor = 0xFFF7C59F.toInt())
  }

  private fun drawGoluApronRibbon(canvas: Canvas, player: Player, tick: Int, bob: Float) {
    val sway = (-player.vx * 1.4f + sin(tick * 0.2).toFloat() * 2f).coerceIn(-9f, 6f)
    paintFill.color = 0xFFFFFFFF.toInt()
    pathScratch.reset()
    pathScratch.moveTo(-9f, -21f + bob)
    pathScratch.quadTo(-15f + sway * 0.4f, -18f + bob, -17f + sway, -12f + bob)
    pathScratch.lineTo(-14f + sway, -9f + bob)
    pathScratch.lineTo(-8f, -17f + bob)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    canvas.drawPath(pathScratch, paintOutlineThin)
  }

  private fun drawGoluLegs(canvas: Canvas, state: AaravState, tick: Int) {
    val legY = -16f
    val sockColor = 0xFFFEF3C7.toInt()
    val shoeColor = 0xFFB91C1C.toInt()

    when (state) {
      AaravState.RUN -> {
        val phase = (tick % 20) / 20f * 2.0 * PI
        val leftAngle = (sin(phase) * 34.0).toFloat()
        val rightAngle = (sin(phase + PI) * 34.0).toFloat()
        drawSingleBootLeg(canvas, -4.5f, legY, leftAngle, 10f, sockColor, shoeColor)
        drawSingleBootLeg(canvas, 4.5f, legY, rightAngle, 10f, sockColor, shoeColor)
      }
      AaravState.JUMP_UP -> {
        drawSingleBootLeg(canvas, -5f, legY - 1f, 24f, 9f, sockColor, shoeColor)
        drawSingleBootLeg(canvas, 5f, legY - 2f, -26f, 10f, sockColor, shoeColor)
      }
      AaravState.FALL_DOWN -> {
        drawSingleBootLeg(canvas, -5f, legY, -18f, 10f, sockColor, shoeColor)
        drawSingleBootLeg(canvas, 5f, legY - 1f, 20f, 9f, sockColor, shoeColor)
      }
      else -> {
        drawSingleBootLeg(canvas, -4.5f, legY, 0f, 10f, sockColor, shoeColor)
        drawSingleBootLeg(canvas, 4.5f, legY, 0f, 10f, sockColor, shoeColor)
      }
    }
  }

  // ---------------- CHARACTER B: VEGA (The Volt Sprinter) ----------------

  fun drawVega(canvas: Canvas, player: Player, tick: Int) {
    val state = player.state
    val breathe = if (state == AaravState.IDLE) (sin(tick * 0.11) * 0.7).toFloat() else 0f
    val forwardLean = when (state) {
      AaravState.RUN -> 7f
      AaravState.JUMP_UP -> -4f
      else -> 0f
    }

    canvas.save()
    canvas.rotate(forwardLean, 0f, -18f)

    // Streaming lightning-headband tails behind neck
    drawVegaHeadbandTails(canvas, player, tick)

    // Winged hi-top sneakers & legs
    drawVegaLegs(canvas, state, tick)

    // Sleek emerald + lime athletic jacket with yellow bolt emblem
    val torsoY = -34f + breathe
    paintFill.color = 0xFF10B981.toInt()
    rectTemp.set(-8f, torsoY, 8f, torsoY + 17f)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintFill)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutline)

    // Lime speed stripe collar
    paintFill.color = 0xFFA3E635.toInt()
    rectTemp.set(-7.5f, torsoY + 1f, 7.5f, torsoY + 5.5f)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintFill)

    // Yellow lightning bolt emblem
    paintFill.color = 0xFFFACC15.toInt()
    pathScratch.reset()
    pathScratch.moveTo(2f, torsoY + 5.5f)
    pathScratch.lineTo(-2f, torsoY + 10.5f)
    pathScratch.lineTo(1f, torsoY + 10.5f)
    pathScratch.lineTo(-1.5f, torsoY + 15.5f)
    pathScratch.lineTo(3.5f, torsoY + 9.5f)
    pathScratch.lineTo(0.8f, torsoY + 9.5f)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    canvas.drawPath(pathScratch, paintOutlineThin)

    // Head base
    val headY = -44f + breathe
    paintFill.color = 0xFFD99B66.toInt()
    rectTemp.set(-7f, headY, 7f, headY + 12f)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintFill)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)

    // Wild spiky electric-cyan hair shooting upward & backward
    paintFill.color = 0xFF06B6D4.toInt()
    pathScratch.reset()
    pathScratch.moveTo(-7f, headY + 4f)
    pathScratch.lineTo(-14.5f, headY - 1f)
    pathScratch.lineTo(-9f, headY - 3f)
    pathScratch.lineTo(-13.5f, headY - 9f)
    pathScratch.lineTo(-5f, headY - 6f)
    pathScratch.lineTo(-6f, headY - 14f)
    pathScratch.lineTo(0.5f, headY - 7.5f)
    pathScratch.lineTo(6.5f, headY - 11.5f)
    pathScratch.lineTo(5.5f, headY - 3f)
    pathScratch.lineTo(8.5f, headY - 1f)
    pathScratch.lineTo(5f, headY + 2f)
    pathScratch.lineTo(-5f, headY + 2f)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    canvas.drawPath(pathScratch, paintOutline)

    // Pointed yellow-rimmed dark visor shades + expressive mouth
    val visorTilt = if (state == AaravState.DEATH || state == AaravState.DAMAGE) -14f else 0f
    canvas.save()
    canvas.rotate(visorTilt, 3f, headY + 5f)
    paintFill.color = 0xFF1E1B4B.toInt()
    pathScratch.reset()
    pathScratch.moveTo(-1.5f, headY + 3f)
    pathScratch.lineTo(8.5f, headY + 2.2f)
    pathScratch.lineTo(7.2f, headY + 6.6f)
    pathScratch.lineTo(0f, headY + 6.8f)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    paintAccent.color = 0xFFFACC15.toInt()
    paintAccent.style = Paint.Style.STROKE
    paintAccent.strokeWidth = 2.0f
    canvas.drawPath(pathScratch, paintAccent)
    paintAccent.style = Paint.Style.FILL
    // Glint streak on visor
    paintFill.color = 0xFF67E8F9.toInt()
    canvas.drawLine(1.5f, headY + 4.4f, 6f, headY + 3.8f, paintFill)
    canvas.restore()

    // Mouth expression
    if (state == AaravState.DEATH) {
      canvas.drawCircle(3f, headY + 9.4f, 1.9f, paintOutlineThin)
    } else if (state == AaravState.DAMAGE) {
      canvas.drawLine(0.5f, headY + 9f, 5f, headY + 8.2f, paintOutlineThin)
    } else if (state == AaravState.VICTORY) {
      rectTemp.set(0.5f, headY + 7f, 5.8f, headY + 10.6f)
      canvas.drawArc(rectTemp, 0f, 180f, false, paintOutlineThin)
    } else {
      rectTemp.set(1f, headY + 7.5f, 5.2f, headY + 9.8f)
      canvas.drawArc(rectTemp, 5f, 165f, false, paintOutlineThin)
    }

    // Athletic arm with cyan wristband
    drawSimpleArm(
        canvas = canvas,
        state = state,
        tick = tick,
        armX = 5.5f,
        armY = -30f + breathe,
        sleeveColor = 0xFF10B981.toInt(),
        handColor = 0xFFD99B66.toInt(),
        cuffColor = 0xFF67E8F9.toInt()
    )

    canvas.restore()
  }

  private fun drawVegaHeadbandTails(canvas: Canvas, player: Player, tick: Int) {
    val wave = (sin(tick * 0.30).toFloat() * 3f) - player.vx.coerceIn(-4f, 6f)
    paintFill.color = 0xFFFACC15.toInt()
    pathScratch.reset()
    pathScratch.moveTo(-6f, -37f)
    pathScratch.lineTo(-16f + wave * 0.4f, -39f + sin(tick * 0.25).toFloat() * 2f)
    pathScratch.lineTo(-14f + wave * 0.4f, -35f + sin(tick * 0.25).toFloat() * 2f)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    canvas.drawPath(pathScratch, paintOutlineThin)
  }

  private fun drawVegaLegs(canvas: Canvas, state: AaravState, tick: Int) {
    val legY = -18f
    val shortsColor = 0xFF1E293B.toInt()
    val kickColor = 0xFF0891B2.toInt()
    val wingFlap = when (state) {
      AaravState.JUMP_UP, AaravState.FALL_DOWN -> -22f
      AaravState.RUN -> sin(tick * 0.4).toFloat() * 14f
      else -> 0f
    }

    when (state) {
      AaravState.RUN -> {
        val phase = (tick % 18) / 18f * 2.0 * PI
        val leftAngle = (sin(phase) * 38.0).toFloat()
        val rightAngle = (sin(phase + PI) * 38.0).toFloat()
        drawWingedLeg(canvas, -3.8f, legY, leftAngle, shortsColor, kickColor, wingFlap)
        drawWingedLeg(canvas, 3.8f, legY, rightAngle, shortsColor, kickColor, -wingFlap)
      }
      AaravState.JUMP_UP -> {
        drawWingedLeg(canvas, -4.5f, legY, 32f, shortsColor, kickColor, wingFlap)
        drawWingedLeg(canvas, 4.5f, legY - 2f, -34f, shortsColor, kickColor, wingFlap)
      }
      AaravState.FALL_DOWN -> {
        drawWingedLeg(canvas, -4.5f, legY, -22f, shortsColor, kickColor, wingFlap)
        drawWingedLeg(canvas, 4.5f, legY - 1f, 24f, shortsColor, kickColor, wingFlap)
      }
      else -> {
        drawWingedLeg(canvas, -4f, legY, 0f, shortsColor, kickColor, wingFlap)
        drawWingedLeg(canvas, 4f, legY, 0f, shortsColor, kickColor, wingFlap)
      }
    }
  }

  private fun drawWingedLeg(
      canvas: Canvas,
      hipX: Float,
      hipY: Float,
      angleDeg: Float,
      legColor: Int,
      shoeColor: Int,
      wingAngle: Float
  ) {
    canvas.save()
    canvas.translate(hipX, hipY)
    canvas.rotate(angleDeg)
    paintFill.color = legColor
    rectTemp.set(-3f, 0f, 3f, 12f)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintFill)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)

    // Hi-top sneaker
    paintFill.color = shoeColor
    rectTemp.set(-3.5f, 10f, 5.5f, 17.5f)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintFill)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)

    // Signature white sneaker wing on heel
    canvas.save()
    canvas.translate(-3.5f, 12.5f)
    canvas.rotate(wingAngle)
    paintFill.color = 0xFFF8FAFC.toInt()
    pathScratch.reset()
    pathScratch.moveTo(0f, 0f)
    pathScratch.lineTo(-6.5f, -4.5f)
    pathScratch.lineTo(-4.5f, 1.5f)
    pathScratch.close()
    canvas.drawPath(pathScratch, paintFill)
    canvas.drawPath(pathScratch, paintOutlineThin)
    canvas.restore()

    canvas.restore()
  }

  // ---------------- CHARACTER C: ROKO (The Clockwork Tinker) ----------------

  fun drawRoko(canvas: Canvas, player: Player, tick: Int) {
    val state = player.state
    val breathe = if (state == AaravState.IDLE) (sin(tick * 0.08) * 0.7).toFloat() else 0f

    // Giant spinning brass wind-up key on back
    drawRokoWindUpKey(canvas, state, tick, breathe)

    // Coiled spring legs + iron boots
    drawRokoSpringLegs(canvas, state, tick)

    // Riveted bronze chestplate with glowing teal core dial
    val torsoY = -32f + breathe
    paintFill.color = 0xFF475569.toInt()
    rectTemp.set(-9.5f, torsoY, 9.5f, torsoY + 17f)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintFill)
    canvas.drawRoundRect(rectTemp, 5f, 5f, paintOutline)

    // Brass shoulder/chest trim
    paintFill.color = 0xFFD97706.toInt()
    rectTemp.set(-8f, torsoY + 1.5f, 8f, torsoY + 5f)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintFill)
    // Teal clockwork heart dial
    paintFill.color = 0xFF14B8A6.toInt()
    canvas.drawCircle(0.5f, torsoY + 10.5f, 4.0f, paintFill)
    canvas.drawCircle(0.5f, torsoY + 10.5f, 4.0f, paintOutlineThin)
    paintFill.color = 0xFFFDE047.toInt()
    canvas.drawCircle(0.5f, torsoY + 10.5f, 1.5f, paintFill)

    // Oversized domed violet kettle-helmet + dark visor + glowing expressive eyes
    val headY = -47f + breathe
    paintFill.color = 0xFF9333EA.toInt()
    rectTemp.set(-10.5f, headY, 10.5f, headY + 17f)
    canvas.drawRoundRect(rectTemp, 9f, 9f, paintFill)
    canvas.drawRoundRect(rectTemp, 9f, 9f, paintOutline)

    // Top brass antenna bolt & spring
    val antennaSway = (-player.vx * 0.7f + sin(tick * 0.22).toFloat() * 2f).coerceIn(-5f, 5f)
    canvas.drawLine(0f, headY, antennaSway, headY - 7f, paintOutlineThin)
    paintFill.color = 0xFFF59E0B.toInt()
    canvas.drawCircle(antennaSway, headY - 8f, 2.6f, paintFill)
    canvas.drawCircle(antennaSway, headY - 8f, 2.6f, paintOutlineThin)

    // Brass helmet collar rim
    paintFill.color = 0xFFF59E0B.toInt()
    rectTemp.set(-9.5f, headY + 13.5f, 9.5f, headY + 17f)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintFill)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)

    // Dark curved visor slit
    paintFill.color = 0xFF0F172A.toInt()
    rectTemp.set(-6f, headY + 5f, 8.5f, headY + 12f)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintFill)
    canvas.drawRoundRect(rectTemp, 4f, 4f, paintOutlineThin)

    // Expressive glowing amber eyes inside dark visor
    when (state) {
      AaravState.DEATH -> {
        paintAccent.color = 0xFFEF4444.toInt()
        paintAccent.style = Paint.Style.STROKE
        paintAccent.strokeWidth = 2.0f
        canvas.drawLine(-2.5f, headY + 7f, 0.5f, headY + 10f, paintAccent)
        canvas.drawLine(0.5f, headY + 7f, -2.5f, headY + 10f, paintAccent)
        canvas.drawLine(3.2f, headY + 7f, 6.2f, headY + 10f, paintAccent)
        canvas.drawLine(6.2f, headY + 7f, 3.2f, headY + 10f, paintAccent)
        paintAccent.style = Paint.Style.FILL
      }
      AaravState.DAMAGE -> {
        paintAccent.color = 0xFFF97316.toInt()
        paintAccent.style = Paint.Style.STROKE
        paintAccent.strokeWidth = 2.0f
        canvas.drawLine(-2.5f, headY + 7f, 0.2f, headY + 8.5f, paintAccent)
        canvas.drawLine(0.2f, headY + 8.5f, -2.5f, headY + 10f, paintAccent)
        canvas.drawLine(6.2f, headY + 7f, 3.5f, headY + 8.5f, paintAccent)
        canvas.drawLine(3.5f, headY + 8.5f, 6.2f, headY + 10f, paintAccent)
        paintAccent.style = Paint.Style.FILL
      }
      AaravState.VICTORY -> {
        paintAccent.color = 0xFFFDE047.toInt()
        paintAccent.style = Paint.Style.STROKE
        paintAccent.strokeWidth = 2.2f
        rectTemp.set(-3f, headY + 7f, 0.8f, headY + 10.5f)
        canvas.drawArc(rectTemp, 190f, 160f, false, paintAccent)
        rectTemp.set(2.5f, headY + 7f, 6.3f, headY + 10.5f)
        canvas.drawArc(rectTemp, 190f, 160f, false, paintAccent)
        paintAccent.style = Paint.Style.FILL
      }
      AaravState.JUMP_UP, AaravState.FALL_DOWN -> {
        paintFill.color = 0xFFFDE047.toInt()
        canvas.drawOval(-3f, headY + 6.4f, 0.5f, headY + 10.8f, paintFill)
        canvas.drawOval(2.8f, headY + 6.4f, 6.3f, headY + 10.8f, paintFill)
      }
      else -> {
        paintFill.color = 0xFFFDE047.toInt()
        canvas.drawCircle(-1.2f, headY + 8.5f, 1.8f, paintFill)
        canvas.drawCircle(4.6f, headY + 8.5f, 1.8f, paintFill)
      }
    }

    // Mechanical gauntlet arm
    drawSimpleArm(
        canvas = canvas,
        state = state,
        tick = tick,
        armX = 6.5f,
        armY = -29f + breathe,
        sleeveColor = 0xFF64748B.toInt(),
        handColor = 0xFFF59E0B.toInt()
    )
  }

  private fun drawRokoWindUpKey(canvas: Canvas, state: AaravState, tick: Int, breathe: Float) {
    val spinRate = when (state) {
      AaravState.RUN, AaravState.VICTORY -> 0.35
      AaravState.JUMP_UP, AaravState.FALL_DOWN -> 0.28
      AaravState.DEATH -> 0.0
      else -> 0.10
    }
    val loopScaleY = if (state == AaravState.DEATH) 0.5f else cos(tick * spinRate).toFloat()

    // Key shaft attached to back
    paintFill.color = 0xFFF59E0B.toInt()
    rectTemp.set(-15.5f, -26f + breathe, -9f, -22.5f + breathe)
    canvas.drawRoundRect(rectTemp, 1.5f, 1.5f, paintFill)
    canvas.drawRoundRect(rectTemp, 1.5f, 1.5f, paintOutlineThin)

    // Rotating butterfly loops on key handle
    val loopSpan = (5.5f * loopScaleY).coerceIn(-5.5f, 5.5f)
    rectTemp.set(-20.5f, -24.2f + breathe - kotlin.math.abs(loopSpan) - 1.8f, -14.5f, -24.2f + breathe - 0.3f)
    canvas.drawOval(rectTemp, paintFill)
    canvas.drawOval(rectTemp, paintOutlineThin)
    rectTemp.set(-20.5f, -24.2f + breathe + 0.3f, -14.5f, -24.2f + breathe + kotlin.math.abs(loopSpan) + 1.8f)
    canvas.drawOval(rectTemp, paintFill)
    canvas.drawOval(rectTemp, paintOutlineThin)
  }

  private fun drawRokoSpringLegs(canvas: Canvas, state: AaravState, tick: Int) {
    val hipY = -16f
    when (state) {
      AaravState.RUN -> {
        val phase = (tick % 20) / 20f * 2.0 * PI
        drawCoiledBootLeg(canvas, -4.5f, hipY, (sin(phase) * 28.0).toFloat(), 11f)
        drawCoiledBootLeg(canvas, 4.5f, hipY, (sin(phase + PI) * 28.0).toFloat(), 11f)
      }
      AaravState.JUMP_UP -> {
        drawCoiledBootLeg(canvas, -4.5f, hipY, 22f, 13f)
        drawCoiledBootLeg(canvas, 4.5f, hipY, -24f, 13f)
      }
      AaravState.FALL_DOWN -> {
        drawCoiledBootLeg(canvas, -4.5f, hipY, -16f, 12f)
        drawCoiledBootLeg(canvas, 4.5f, hipY, 18f, 12f)
      }
      else -> {
        drawCoiledBootLeg(canvas, -4.5f, hipY, 0f, 10.5f)
        drawCoiledBootLeg(canvas, 4.5f, hipY, 0f, 10.5f)
      }
    }
  }

  private fun drawCoiledBootLeg(
      canvas: Canvas,
      hipX: Float,
      hipY: Float,
      angleDeg: Float,
      coilHeight: Float
  ) {
    canvas.save()
    canvas.translate(hipX, hipY)
    canvas.rotate(angleDeg)

    // Metallic zig-zag spring coil
    paintAccent.color = 0xFFCBD5E1.toInt()
    paintAccent.style = Paint.Style.STROKE
    paintAccent.strokeWidth = 2.6f
    pathScratch.reset()
    pathScratch.moveTo(0f, 0f)
    pathScratch.lineTo(-3f, coilHeight * 0.25f)
    pathScratch.lineTo(3f, coilHeight * 0.50f)
    pathScratch.lineTo(-3f, coilHeight * 0.75f)
    pathScratch.lineTo(0f, coilHeight)
    canvas.drawPath(pathScratch, paintOutline)
    canvas.drawPath(pathScratch, paintAccent)
    paintAccent.style = Paint.Style.FILL

    // Chunky iron boot
    paintFill.color = 0xFF334155.toInt()
    rectTemp.set(-4.5f, coilHeight - 1f, 5.5f, coilHeight + 6.5f)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintFill)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
    canvas.restore()
  }

  // ---------------- SHARED LIMB HELPERS ----------------

  private fun drawSingleBootLeg(
      canvas: Canvas,
      hipX: Float,
      hipY: Float,
      angleDeg: Float,
      legLength: Float,
      legColor: Int,
      shoeColor: Int
  ) {
    canvas.save()
    canvas.translate(hipX, hipY)
    canvas.rotate(angleDeg)
    paintFill.color = legColor
    rectTemp.set(-3f, 0f, 3f, legLength)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintFill)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)

    paintFill.color = shoeColor
    rectTemp.set(-4f, legLength - 2f, 5.5f, legLength + 6f)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintFill)
    canvas.drawRoundRect(rectTemp, 3f, 3f, paintOutline)
    canvas.restore()
  }

  private fun drawSimpleArm(
      canvas: Canvas,
      state: AaravState,
      tick: Int,
      armX: Float,
      armY: Float,
      sleeveColor: Int,
      handColor: Int,
      cuffColor: Int? = null
  ) {
    val angle = when (state) {
      AaravState.RUN -> (sin((tick % 20) / 20f * 2.0 * PI) * 38.0).toFloat()
      AaravState.VICTORY -> -132f
      AaravState.DEATH -> -138f
      AaravState.FALL_DOWN -> -112f
      AaravState.DAMAGE -> 118f
      AaravState.JUMP_UP -> -62f
      else -> 4f
    }

    canvas.save()
    canvas.translate(armX, armY)
    canvas.rotate(angle)
    paintFill.color = sleeveColor
    rectTemp.set(-2.5f, 0f, 2.5f, 11.5f)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintFill)
    canvas.drawRoundRect(rectTemp, 2f, 2f, paintOutline)
    if (cuffColor != null) {
      paintFill.color = cuffColor
      rectTemp.set(-2.8f, 7.5f, 2.8f, 10.5f)
      canvas.drawRoundRect(rectTemp, 1.5f, 1.5f, paintFill)
      canvas.drawRoundRect(rectTemp, 1.5f, 1.5f, paintOutlineThin)
    }
    paintFill.color = handColor
    canvas.drawCircle(0f, 12f, 2.6f, paintFill)
    canvas.drawCircle(0f, 12f, 2.6f, paintOutlineThin)
    canvas.restore()
  }
}
