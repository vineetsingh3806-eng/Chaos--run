package com.example.engine

import android.graphics.Canvas
import com.example.data.AaravState
import com.example.data.PlayableCharacter
import com.example.data.Player
import kotlin.math.PI
import kotlin.math.sin

/**
 * Unified 60 FPS character renderer supporting all four playable runners
 * (Aarav, Golu, Vega, Roko) across all 8 animation states and Settings previews.
 * Reuses pre-allocated paints/paths for zero per-frame allocations.
 */
class PlayerCharacterRenderer(
    private val aaravRenderer: AaravCharacterRenderer = AaravCharacterRenderer()
) {
  private val alternateDrawers = AlternateCharacterDrawers()
  private val previewPlayer = Player(x = 0f, y = 0f)

  fun drawCharacter(
      canvas: Canvas,
      character: PlayableCharacter,
      player: Player,
      cameraX: Float,
      cameraY: Float
  ) {
    if (character == PlayableCharacter.AARAV) {
      aaravRenderer.drawPlayer(canvas, player, cameraX, cameraY)
      return
    }

    if (player.invulnerableFrames % 4 in 1..2) {
      return
    }

    val screenX = player.x - cameraX
    val screenY = player.y - cameraY

    canvas.save()
    canvas.translate(screenX + player.collisionWidth / 2f, screenY + player.collisionHeight)

    if (!player.facingRight) {
      canvas.scale(-1f, 1f)
    }

    applyStateBodyTransform(canvas, player.state, player.stateTimerFrames)

    val tick = player.stateTimerFrames
    when (character) {
      PlayableCharacter.GOLU -> alternateDrawers.drawGolu(canvas, player, tick)
      PlayableCharacter.VEGA -> alternateDrawers.drawVega(canvas, player, tick)
      PlayableCharacter.ROKO -> alternateDrawers.drawRoko(canvas, player, tick)
      PlayableCharacter.AARAV -> Unit
    }

    canvas.restore()
  }

  /**
   * Renders a clean, centered vector character preview for Settings selection cards
   * and automated multi-state rendering tests.
   */
  fun drawPreview(
      canvas: Canvas,
      character: PlayableCharacter,
      width: Float,
      height: Float,
      animTick: Int = 0,
      state: AaravState = AaravState.IDLE,
      facingRight: Boolean = true
  ) {
    if (width <= 0f || height <= 0f) return
    val scale = (minOf(width / 46f, height / 64f)).coerceAtLeast(0.4f)

    previewPlayer.x = 0f
    previewPlayer.y = 0f
    previewPlayer.vx = if (state == AaravState.RUN) 3.8f else 0f
    previewPlayer.vy = when (state) {
      AaravState.JUMP_UP -> -8f
      AaravState.FALL_DOWN -> 6f
      else -> 0f
    }
    previewPlayer.facingRight = facingRight
    previewPlayer.state = state
    previewPlayer.stateTimerFrames = animTick
    previewPlayer.invulnerableFrames = 0

    canvas.save()
    canvas.translate(width * 0.5f, height * 0.90f)
    canvas.scale(scale, scale)
    // Offset camera so character bottom-center lands cleanly at (0, 0)
    drawCharacter(
        canvas = canvas,
        character = character,
        player = previewPlayer,
        cameraX = previewPlayer.collisionWidth * 0.5f,
        cameraY = previewPlayer.collisionHeight
    )
    canvas.restore()
  }

  private fun applyStateBodyTransform(canvas: Canvas, state: AaravState, timerFrames: Int) {
    when (state) {
      AaravState.DEATH -> {
        val progress = (timerFrames / 24f).coerceIn(0f, 1f)
        canvas.translate(0f, -24f)
        canvas.rotate(timerFrames * 19f)
        val scale = 1.0f + 0.14f * sin(progress * PI).toFloat()
        canvas.scale(scale, scale)
        canvas.translate(0f, 24f)
      }
      AaravState.LAND -> {
        val squashProgress = (timerFrames.coerceAtMost(6)) / 6f
        val scaleY = 0.80f + 0.20f * squashProgress
        val scaleX = 1.15f - 0.15f * squashProgress
        canvas.scale(scaleX, scaleY)
      }
      AaravState.JUMP_UP -> canvas.scale(0.92f, 1.08f)
      AaravState.FALL_DOWN -> canvas.scale(0.96f, 1.04f)
      AaravState.DAMAGE -> {
        canvas.rotate(-8f)
        canvas.scale(1.06f, 0.94f)
      }
      else -> Unit
    }
  }
}
