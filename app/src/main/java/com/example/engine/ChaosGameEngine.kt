package com.example.engine

import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * 60 FPS Core Game Physics & Simulation Engine.
 * Implements deterministic kinematics, coyote time, jump buffering,
 * top-contact-only disappearing floor state machine, and snapshot rollback.
 */
class ChaosGameEngine(
    val level: LevelDefinition,
    val audioEngine: ProceduralAudioEngine,
    val onLevelCompleted: (timeMs: Long, coinsCollected: Int, totalCoins: Int) -> Unit
) {
  companion object {
    const val GRAVITY = 0.58f
    const val JUMP_IMPULSE = -13.2f
    const val TERMINAL_VELOCITY = 14.0f
    const val MAX_RUN_SPEED = 6.5f
    const val RUN_ACCEL = 0.85f
    const val GROUND_FRICTION = 1.1f
    const val AIR_ACCEL = 0.45f
    const val COYOTE_FRAMES = 5 // ~80 ms
    const val JUMP_BUFFER_FRAMES = 6 // ~100 ms
    const val CRUMBLE_DELAY_FRAMES = 45 // 0.75 s
    const val PIT_KILL_Y = 760f
  }

  val player = Player(x = level.spawnX, y = level.spawnY)
  val tiles: List<GameTile> = level.tiles.map { it.copy() }
  val coins: List<Coin> = level.coins.map { it.copy() }
  val movingPlatforms: List<MovingPlatform> = level.movingPlatforms.map { it.copy() }

  // Particle pool (64 pre-allocated particles)
  val particles = Array(64) { Particle() }
  private var nextParticleIndex = 0

  var levelTimerMs = 0L
  var isPaused = false
  var isGameOver = false
  var isVictorious = false

  var checkpointSnapshot: LevelCheckpointSnapshot? = null
  private var hasCheckpointActivated = false

  // Controller inputs
  var inputLeft = false
  var inputRight = false
  var inputJump = false
  private var previousInputJump = false

  fun update() {
    if (isPaused || isGameOver || isVictorious) {
      if (isVictorious) {
        player.state = AaravState.VICTORY
        player.stateTimerFrames++
        updateParticles()
      }
      return
    }

    levelTimerMs += 16L

    // 1. Process Jump Buffer
    if (inputJump && !previousInputJump) {
      player.jumpBufferTimer = JUMP_BUFFER_FRAMES
    } else if (player.jumpBufferTimer > 0) {
      player.jumpBufferTimer--
    }

    // 2. Variable Jump Cut on Release
    if (!inputJump && previousInputJump) {
      if (player.vy < -2.0f && !player.hasJumpCutApplied) {
        player.vy *= 0.42f
        player.hasJumpCutApplied = true
      }
    }
    previousInputJump = inputJump

    // 3. Horizontal Kinematics
    val accel = if (player.isGrounded) RUN_ACCEL else AIR_ACCEL
    if (inputLeft && !inputRight) {
      player.vx = (player.vx - accel).coerceAtLeast(-MAX_RUN_SPEED)
      player.facingRight = false
    } else if (inputRight && !inputLeft) {
      player.vx = (player.vx + accel).coerceAtMost(MAX_RUN_SPEED)
      player.facingRight = true
    } else {
      // Ground or air friction
      if (player.vx > 0f) {
        player.vx = (player.vx - GROUND_FRICTION).coerceAtLeast(0f)
      } else if (player.vx < 0f) {
        player.vx = (player.vx + GROUND_FRICTION).coerceAtMost(0f)
      }
    }

    // 4. Coyote Time and Jump Trigger
    if (player.isGrounded) {
      player.coyoteTimer = COYOTE_FRAMES
      player.hasJumpCutApplied = false
    } else if (player.coyoteTimer > 0) {
      player.coyoteTimer--
    }

    if (player.jumpBufferTimer > 0 && player.coyoteTimer > 0) {
      // Execute ground/coyote jump
      player.vy = JUMP_IMPULSE
      player.isGrounded = false
      player.coyoteTimer = 0
      player.jumpBufferTimer = 0
      player.hasJumpCutApplied = false
      player.state = AaravState.JUMP_UP
      audioEngine.playJump()
      spawnDustPuff(player.x + player.collisionWidth / 2, player.y + player.collisionHeight)
    }

    // 5. Apply Gravity
    player.prevX = player.x
    player.prevY = player.y

    if (!player.isGrounded) {
      player.vy = (player.vy + GRAVITY).coerceAtMost(TERMINAL_VELOCITY)
      player.state = if (player.vy < 0f) AaravState.JUMP_UP else AaravState.FALL_DOWN
    } else {
      player.state = if (Math.abs(player.vx) > 0.4f) AaravState.RUN else AaravState.IDLE
    }

    // 6. Update Moving Platforms
    for (mp in movingPlatforms) {
      val prevMpX = mp.x
      mp.currentFrame = (mp.currentFrame + 1) % mp.periodFrames
      val angle = (2.0 * PI * mp.currentFrame / mp.periodFrames).toFloat()
      mp.x = mp.centerX + mp.amplitude * sin(angle)
      mp.vx = mp.x - prevMpX
    }

    // 7. Integrate Physics & Collision
    integrateHorizontalMovement()
    integrateVerticalMovement()

    // 8. Update Crumbling Platforms
    updateCrumblingTiles()

    // 9. Update Interactive Entities (Coins, Checkpoint, Sun Gate)
    updateCoins()
    updateCheckpoint()
    updateExitGate()

    // 10. Pit Death Detection
    if (player.y > PIT_KILL_Y) {
      handlePlayerDeath()
    }

    // 11. Invulnerability timer
    if (player.invulnerableFrames > 0) {
      player.invulnerableFrames--
    }

    // 12. Update Particles
    updateParticles()
    player.stateTimerFrames++
  }

  private fun integrateHorizontalMovement() {
    player.x += player.vx

    // Tile side collisions (Solid blocks only)
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val playerTop = player.y
    val playerBottom = player.y + player.collisionHeight

    for (tile in tiles) {
      if (!tile.isSolid || !tile.isVisible) continue
      if (tile.type == TileType.CHECKPOINT_BELL || tile.type == TileType.SUN_GATE || tile.type == TileType.BRAMBLE_SPIKES) continue

      val tileLeft = tile.x
      val tileRight = tile.x + tile.width
      val tileTop = tile.y
      val tileBottom = tile.y + tile.height

      // AABB overlap check
      if (playerRight > tileLeft && playerLeft < tileRight &&
          playerBottom > tileTop + 4f && playerTop < tileBottom - 4f) {
        if (player.vx > 0f) {
          player.x = tileLeft - player.collisionWidth
          player.vx = 0f
        } else if (player.vx < 0f) {
          player.x = tileRight
          player.vx = 0f
        }
      }
    }
  }

  private fun integrateVerticalMovement() {
    val previousGrounded = player.isGrounded
    player.isGrounded = false
    player.y += player.vy

    // 1. Moving Platform Contact
    for (mp in movingPlatforms) {
      val mpLeft = mp.x
      val mpRight = mp.x + mp.width
      val mpTop = mp.y
      val mpBottom = mp.y + mp.height

      val playerLeft = player.x
      val playerRight = player.x + player.collisionWidth
      val prevBottom = player.prevY + player.collisionHeight
      val currBottom = player.y + player.collisionHeight

      if (player.vy >= 0f && prevBottom <= mpTop + 4f && currBottom >= mpTop - 2f &&
          playerRight > mpLeft + 8f && playerLeft < mpRight - 8f) {
        player.y = mpTop - player.collisionHeight
        player.vy = 0f
        player.isGrounded = true
        player.x += mp.vx // Carry player with platform
        if (!previousGrounded) {
          audioEngine.playLand()
          spawnDustPuff(player.x + player.collisionWidth / 2, mpTop)
        }
        return
      }
    }

    // 2. Tile Floor & Ceiling Collisions
    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val playerTop = player.y
    val playerBottom = player.y + player.collisionHeight

    for (tile in tiles) {
      if (!tile.isVisible) continue

      // A. Disappearing Floor Check (Strict Top-Contact Only)
      if (tile.type == TileType.DISAPPEARING_FLOOR) {
        if (evaluateDisappearingFloor(tile)) {
          audioEngine.playDisappear()
          spawnSparkles(tile.x + tile.width / 2, tile.y + tile.height / 2, 0xFF64DFDF.toInt(), 12)
          continue
        }
      }

      if (!tile.isSolid) continue

      // B. Bramble Spikes Check
      if (tile.type == TileType.BRAMBLE_SPIKES) {
        val tileLeft = tile.x
        val tileRight = tile.x + tile.width
        val tileTop = tile.y + 12f // Spikes hit area
        val tileBottom = tile.y + tile.height

        if (playerRight > tileLeft + 6f && playerLeft < tileRight - 6f &&
            playerBottom > tileTop && playerTop < tileBottom) {
          handlePlayerDamage()
        }
        continue
      }

      // C. Standard Solid Floor / Ceiling Collision
      val tileLeft = tile.x
      val tileRight = tile.x + tile.width
      val tileTop = tile.y
      val tileBottom = tile.y + tile.height

      if (playerRight > tileLeft + 4f && playerLeft < tileRight - 4f) {
        // Landing on floor
        if (player.vy >= 0f && player.prevY + player.collisionHeight <= tileTop + 8f &&
            playerBottom >= tileTop - 1f && playerTop < tileTop) {
          player.y = tileTop - player.collisionHeight
          player.vy = 0f
          player.isGrounded = true

          // Trigger crumbling wood if applicable
          if (tile.type == TileType.CRUMBLING_WOOD && !tile.isCrumbling && !tile.isTriggered) {
            tile.isCrumbling = true
            tile.crumbleTimerFrames = CRUMBLE_DELAY_FRAMES
            audioEngine.playCrumble()
          }

          if (!previousGrounded) {
            audioEngine.playLand()
            spawnDustPuff(player.x + player.collisionWidth / 2, tileTop)
          }
        }
        // Ceiling bump
        else if (player.vy < 0f && player.prevY >= tileBottom - 6f && playerTop <= tileBottom) {
          player.y = tileBottom
          player.vy = 0f
        }
      }
    }
  }

  /**
   * Swept top-contact detection for instant disappearing floors.
   * Strictly requires vy > 0.1f and downward crossing of the top edge.
   */
  fun evaluateDisappearingFloor(tile: GameTile): Boolean {
    if (tile.isTriggered || !tile.isSolid) return false
    if (player.vy <= 0.1f) return false // Rejects vy <= 0 (stationary, ceiling bumps)

    val prevBottom = player.prevY + player.collisionHeight
    val currBottom = player.y + player.collisionHeight
    val tileTop = tile.y

    val wasAboveSurface = prevBottom <= tileTop + 2f
    val crossedSurface = currBottom >= tileTop - 1f

    val playerLeft = player.x
    val playerRight = player.x + player.collisionWidth
    val tileLeft = tile.x
    val tileRight = tile.x + tile.width
    val horizontalOverlap = (playerRight > tileLeft + 8f) && (playerLeft < tileRight - 8f)

    if (wasAboveSurface && crossedSurface && horizontalOverlap) {
      tile.isTriggered = true
      tile.isVisible = false
      tile.isSolid = false
      player.isGrounded = false
      return true
    }
    return false
  }

  private fun updateCrumblingTiles() {
    for (tile in tiles) {
      if (tile.isCrumbling && !tile.isTriggered) {
        tile.crumbleTimerFrames--
        if (tile.crumbleTimerFrames <= 0) {
          tile.isTriggered = true
          tile.isVisible = false
          tile.isSolid = false
          spawnDustPuff(tile.x + tile.width / 2, tile.y + tile.height / 2)
        }
      }
    }
  }

  private fun updateCoins() {
    val playerCenterX = player.x + player.collisionWidth / 2
    val playerCenterY = player.y + player.collisionHeight / 2

    for (coin in coins) {
      if (coin.isCollected) continue
      coin.animTimer += 0.08f

      val dx = playerCenterX - coin.x
      val dy = playerCenterY - coin.y
      val distSq = dx * dx + dy * dy
      val pickupDist = coin.radius + player.collisionWidth / 2

      if (distSq < pickupDist * pickupDist) {
        coin.isCollected = true
        audioEngine.playCoin()
        spawnSparkles(coin.x, coin.y, 0xFFFFD700.toInt(), 8)
      }
    }
  }

  private fun updateCheckpoint() {
    val checkpointTile = tiles.firstOrNull { it.type == TileType.CHECKPOINT_BELL } ?: return
    val playerRight = player.x + player.collisionWidth
    val playerLeft = player.x
    val playerBottom = player.y + player.collisionHeight
    val playerTop = player.y

    if (playerRight > checkpointTile.x && playerLeft < checkpointTile.x + checkpointTile.width &&
        playerBottom > checkpointTile.y && playerTop < checkpointTile.y + checkpointTile.height) {
      if (!hasCheckpointActivated) {
        hasCheckpointActivated = true
        audioEngine.playCheckpoint()
        spawnSparkles(checkpointTile.x + checkpointTile.width / 2, checkpointTile.y + checkpointTile.height / 2, 0xFFFFCC00.toInt(), 16)

        // Capture immutable snapshot
        val tileSnaps = tiles.map {
          TileSnapshot(
              tileId = it.id,
              isVisible = it.isVisible,
              isSolid = it.isSolid,
              isTriggered = it.isTriggered,
              crumbleTimerFrames = it.crumbleTimerFrames,
              isCrumbling = it.isCrumbling
          )
        }

        val mpSnaps = movingPlatforms.map {
          MovingPlatformSnapshot(
              platformId = it.id,
              x = it.x,
              y = it.y,
              vx = it.vx,
              currentFrame = it.currentFrame
          )
        }

        val collectedCoinsAtCp = coins.filter { it.isCollected }.map { it.id }.toSet()

        checkpointSnapshot = LevelCheckpointSnapshot(
            playerX = checkpointTile.x + 8f,
            playerY = checkpointTile.y + checkpointTile.height - player.collisionHeight,
            playerFacingRight = true,
            playerHealth = player.health,
            levelTimerMs = levelTimerMs,
            coinsCollectedAtCheckpoint = collectedCoinsAtCp,
            tileSnapshots = tileSnaps,
            movingPlatformSnapshots = mpSnaps
        )
      }
    }
  }

  private fun updateExitGate() {
    val gateTile = tiles.firstOrNull { it.type == TileType.SUN_GATE } ?: return
    val playerRight = player.x + player.collisionWidth
    val playerLeft = player.x
    val playerBottom = player.y + player.collisionHeight
    val playerTop = player.y

    if (playerRight > gateTile.x + 20f && playerLeft < gateTile.x + gateTile.width - 20f &&
        playerBottom > gateTile.y && playerTop < gateTile.y + gateTile.height) {
      if (!isVictorious) {
        isVictorious = true
        player.state = AaravState.VICTORY
        audioEngine.playVictory()
        val collectedCount = coins.count { it.isCollected }
        onLevelCompleted(levelTimerMs, collectedCount, coins.size)
      }
    }
  }

  private fun handlePlayerDamage() {
    if (player.invulnerableFrames > 0) return
    player.health--
    player.invulnerableFrames = 60 // 1 second invulnerability
    audioEngine.playDamage()

    if (player.health <= 0) {
      handlePlayerDeath()
    } else {
      player.state = AaravState.DAMAGE
      player.vy = -7.0f
      player.vx = if (player.facingRight) -3.5f else 3.5f
    }
  }

  fun handlePlayerDeath() {
    audioEngine.playDeath()
    spawnDeathStarburst(player.x + player.collisionWidth / 2, player.y + player.collisionHeight / 2)

    val snap = checkpointSnapshot
    if (snap != null) {
      // Deterministic rollback to Checkpoint
      player.x = snap.playerX
      player.y = snap.playerY
      player.vx = 0f
      player.vy = 0f
      player.isGrounded = true
      player.facingRight = snap.playerFacingRight
      player.health = snap.playerHealth
      player.invulnerableFrames = 40
      player.state = AaravState.IDLE

      // Restore tiles
      val snapMap = snap.tileSnapshots.associateBy { it.tileId }
      for (tile in tiles) {
        snapMap[tile.id]?.let {
          tile.isVisible = it.isVisible
          tile.isSolid = it.isSolid
          tile.isTriggered = it.isTriggered
          tile.crumbleTimerFrames = it.crumbleTimerFrames
          tile.isCrumbling = it.isCrumbling
        }
      }

      // Restore moving platforms
      val mpMap = snap.movingPlatformSnapshots.associateBy { it.platformId }
      for (mp in movingPlatforms) {
        mpMap[mp.id]?.let {
          mp.x = it.x
          mp.y = it.y
          mp.vx = it.vx
          mp.currentFrame = it.currentFrame
        }
      }

      // Restore coins: coins collected post-checkpoint are un-collected
      for (coin in coins) {
        coin.isCollected = snap.coinsCollectedAtCheckpoint.contains(coin.id)
      }
    } else {
      // Reset to level start
      player.x = level.spawnX
      player.y = level.spawnY
      player.vx = 0f
      player.vy = 0f
      player.isGrounded = false
      player.health = player.maxHealth
      player.invulnerableFrames = 40
      player.state = AaravState.IDLE

      for (tile in tiles) {
        tile.isVisible = true
        tile.isSolid = (tile.type != TileType.CHECKPOINT_BELL && tile.type != TileType.SUN_GATE)
        tile.isTriggered = false
        tile.isCrumbling = false
        tile.crumbleTimerFrames = 0
      }
      for (coin in coins) {
        coin.isCollected = false
      }
    }
  }

  private fun spawnDustPuff(x: Float, y: Float) {
    for (i in 0 until 6) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x + (Math.random().toFloat() - 0.5f) * 16f
      p.y = y - 4f
      p.vx = (Math.random().toFloat() - 0.5f) * 2.5f
      p.vy = -Math.random().toFloat() * 1.5f
      p.life = 0f
      p.maxLife = 0.35f
      p.color = 0xAA8B7355.toInt()
      p.size = 5f + Math.random().toFloat() * 4f
    }
  }

  private fun spawnSparkles(x: Float, y: Float, color: Int, count: Int) {
    for (i in 0 until count) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x
      p.y = y
      val angle = (Math.random() * 2.0 * PI).toFloat()
      val speed = 1.0f + Math.random().toFloat() * 3.5f
      p.vx = (Math.cos(angle.toDouble()) * speed).toFloat()
      p.vy = (Math.sin(angle.toDouble()) * speed).toFloat()
      p.life = 0f
      p.maxLife = 0.5f + Math.random().toFloat() * 0.3f
      p.color = color
      p.size = 4f + Math.random().toFloat() * 3f
    }
  }

  private fun spawnDeathStarburst(x: Float, y: Float) {
    for (i in 0 until 24) {
      val p = particles[nextParticleIndex % particles.size]
      nextParticleIndex++
      p.active = true
      p.x = x
      p.y = y
      val angle = (i.toDouble() / 24.0 * 2.0 * PI).toFloat()
      val speed = 2.5f + Math.random().toFloat() * 4.0f
      p.vx = (Math.cos(angle.toDouble()) * speed).toFloat()
      p.vy = (Math.sin(angle.toDouble()) * speed).toFloat()
      p.life = 0f
      p.maxLife = 0.65f
      p.color = if (i % 2 == 0) 0xFFF59E0B.toInt() else 0xFF0D7377.toInt()
      p.size = 6f + Math.random().toFloat() * 4f
    }
  }

  private fun updateParticles() {
    for (p in particles) {
      if (!p.active) continue
      p.life += 0.016f
      if (p.life >= p.maxLife) {
        p.active = false
        continue
      }
      p.x += p.vx
      p.y += p.vy
      p.vy += 0.08f // particle gravity
    }
  }
}
