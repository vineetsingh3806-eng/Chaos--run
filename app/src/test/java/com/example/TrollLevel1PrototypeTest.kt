package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase C Prototype Verification for Level 1-1 ("The Easy Start").
 * Verifies every trap gag triggers deterministically, causes fast comedic death &
 * auto-respawn without stalling, resets all traps + relocated exit door cleanly,
 * handles 20+ rapid retries, and proves Level 1-1 is 100% winnable from spawn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TrollLevel1PrototypeTest {

  private fun createPrototypeEngine(): ChaosGameEngine {
    return ChaosGameEngine(
        level = World1Levels.createLevel(1, DifficultyTier.NORMAL),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
  }

  @Test
  fun test01_safeWarmUpThenDisappearingFloorTriggersPitDeathAndAutoRespawn() {
    val engine = createPrototypeEngine()
    // Settle onto ground at spawn
    repeat(15) { engine.update() }
    assertTrue("Player should land on safe starting runway", engine.player.isGrounded)
    assertEquals(0, engine.deathCount)

    // Walk right across safe zone onto Gag 1 disappearing tile (Tile 12, x = 576)
    engine.inputRight = true
    var floorDropped = false
    for (frame in 0..150) {
      engine.update()
      val bridge1 = engine.trollTraps.first { it.id == 1 }
      if (!bridge1.isSolid && !bridge1.isVisible) {
        floorDropped = true
      }
      if (engine.deathCount > 0) break
    }

    assertTrue("Disappearing floor must drop when stepped on", floorDropped)
    assertTrue("Stepping onto trap bridge must drop Aarav into pit & respawn", engine.deathCount >= 1)
    assertFalse("Should not be stuck in Game Over modal", engine.isGameOver)
    assertEquals("Player must respawn back at start X", engine.level.spawnX, engine.player.x, 48f)
    assertTrue("Input right stays responsive across automatic respawn", engine.inputRight)

    // Verify Gag 1 disappearing floor reset cleanly after respawn
    val bridge1After = engine.trollTraps.first { it.id == 1 }
    assertEquals(TrollTrapState.ARMED, bridge1After.state)
    assertTrue(bridge1After.isSolid)
    assertTrue(bridge1After.isVisible)
  }

  @Test
  fun test02_surpriseSpikePopTriggers1HitComedicDeathAndAutoRespawnIn24Frames() {
    val engine = createPrototypeEngine()
    val spikeTrap = engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    assertEquals(TrollTrapState.ARMED, spikeTrap.state)

    // Walk Aarav right into spike trigger zone on Tile 17..18
    engine.player.x = 17.6f * World1Levels.TILE_SIZE
    engine.player.y = 13f * World1Levels.TILE_SIZE - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.inputRight = true

    var sawDeathSpin = false
    for (frame in 0..45) {
      engine.update()
      if (engine.isDying) {
        sawDeathSpin = true
        assertEquals(AaravState.DEATH, engine.player.state)
      }
      if (engine.deathCount == 1) break
    }

    assertTrue("Spike hit must enter fast comedic death spin", sawDeathSpin)
    assertEquals("Fast respawn must increment deathCount cleanly", 1, engine.deathCount)
    assertFalse("isDying must clear automatically after respawn", engine.isDying)
    assertEquals(TrollTrapState.ARMED, spikeTrap.state)
    assertEquals(0f, spikeTrap.morphSpikeHeight, 0.01f)
  }

  @Test
  fun test03_coinBaitFallingCeilingBlockSlamsAndKillsOnDirectHit() {
    val engine = createPrototypeEngine()
    val thwomp = engine.trollTraps.first { it.type == TrollTrapType.FALLING_CEILING_BLOCK }

    // Stand directly underneath Coin #3 bait
    engine.player.x = 27.6f * World1Levels.TILE_SIZE
    engine.player.y = 13f * World1Levels.TILE_SIZE - engine.player.collisionHeight
    engine.player.isGrounded = true

    for (frame in 0..55) {
      engine.update()
      if (engine.deathCount == 1) break
    }

    assertEquals("Falling ceiling block must punish stationary coin baiting", 1, engine.deathCount)
    assertEquals("Falling ceiling block must reset to ceiling on respawn", thwomp.initialY, thwomp.y, 0.1f)
    assertEquals(TrollTrapState.ARMED, thwomp.state)
  }

  @Test
  fun test04_relocatingExitDoorHopsAwayAndResetsOnManualRestart() {
    val engine = createPrototypeEngine()
    val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
    val initialGateX = gate.x

    // Approach original door at Tile 36
    engine.player.x = 35.1f * World1Levels.TILE_SIZE
    engine.player.y = 13f * World1Levels.TILE_SIZE - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.inputRight = true

    repeat(5) { engine.update() }

    val doorTrap = engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    assertTrue("Exit door gag must trigger before level win", doorTrap.hasRelocated)
    assertFalse("Player must NOT immediately win at decoy initial door position", engine.isVictorious)
    assertTrue("SUN_GATE must have relocated further right", gate.x > initialGateX + 200f)

    // Manual reset must move SUN_GATE back to initial position cleanly
    engine.resetLevel()
    assertFalse(doorTrap.hasRelocated)
    assertEquals(initialGateX, gate.x, 0.1f)
    assertEquals(engine.level.exitX, engine.currentExitX, 0.1f)
  }

  @Test
  fun test05_fullWinnablePlaythroughFromSpawnToRelocatedExitAndRenderCheck() {
    val engine = createPrototypeEngine()
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    val renderer = ChaosGameRenderer(ctx)
    val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Run real simulated inputs from Spawn -> Jump Gap 1 -> Jump Spikes -> Bait Thwomp ->
    // Jump Gap 2 -> Jump Gap 3 -> Trigger Door Hop -> Jump Surprise Pit (38-39) -> Victory!
    var maxFrames = 750
    var baitedThwomp = false
    var jumpCooldown = 0
    while (!engine.isVictorious && maxFrames-- > 0) {
      val px = engine.player.x
      val onGround = engine.player.isGrounded
      if (jumpCooldown > 0) jumpCooldown--

      // Hold right except brief pause just inside triggerMinX (pRight >= 1272 -> px >= 1240)
      // stopping safely clear of thwomp body (x = 1315.2)
      val thwomp = engine.trollTraps.first { it.type == TrollTrapType.FALLING_CEILING_BLOCK }
      if (!baitedThwomp && px in 1242f..1275f && thwomp.state != TrollTrapState.RETIRED) {
        engine.inputRight = false
        if (thwomp.y >= thwomp.targetY - 2f) {
          baitedThwomp = true
        }
      } else {
        engine.inputRight = true
      }

      // Deterministic learned jumps at each hazard/gap edge (clearing 0-telegraph surprise traps before trigger)
      val shouldJump = onGround && jumpCooldown == 0 && (
          px in 520f..555f ||      // Gap 1 + disappearing bridge (576..720)
          px in 735f..782f ||      // Gag 2 surprise spike pop (triggerMinX = 816, pRight < 814)
          px in 1055f..1106f ||    // Gap 2 / Delayed Crumble bridge (1104..1200)
          (thwomp.state == TrollTrapState.RETIRED && px in 1240f..1285f) || // Jump over fallen thwomp
          px in 1465f..1503f ||    // Gap 3 / Fake Safe Platform (1536..1632)
          px in 1775f..1822f       // Surprise pit behind hopping exit door (1824..1920)
      )
      val jumpHoldCutoff = if (px in 520f..740f || px in 1240f..1450f || px >= 1770f) -6.0f else -1.5f
      engine.inputJump = shouldJump || (!onGround && engine.player.vy < jumpHoldCutoff)
      if (shouldJump) jumpCooldown = 10

      engine.update()
      if (maxFrames % 10 == 0) {
        renderer.render(canvas, engine, 1280f, 720f)
      }
      assertEquals(
          "Winnable learned route must have 0 deaths (failed around px=$px, py=${engine.player.y}, remaining=$maxFrames)",
          0,
          engine.deathCount
      )
    }

    assertTrue("Level 1-1 must be cleanly winnable via simulated player inputs (ended px=${engine.player.x})", engine.isVictorious)
  }

  @Test
  fun test06_twentyRapidRetriesRemainDeterministicWithZeroStateLeaks() {
    val engine = createPrototypeEngine()
    for (attempt in 1..20) {
      // Trigger death via pit or trap alternation
      if (attempt % 2 == 0) {
        engine.startDeathSequence("BONK!")
        repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 1) { engine.update() }
      } else {
        engine.player.y = ChaosGameEngine.PIT_KILL_Y + 40f
        engine.update()
      }
      assertEquals("Death count must match retry $attempt", attempt, engine.deathCount)
      assertFalse(engine.isDying)
      assertFalse(engine.isGameOver)
      assertEquals(engine.level.spawnX, engine.player.x, 1f)
      assertTrue("All traps reset on retry $attempt", engine.trollTraps.all { it.state == TrollTrapState.ARMED })
    }
  }
}
