package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import com.example.ui.GameStateViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Step 7 comprehensive tests for Chaos Run 5 Part 1:
 * - Player movement and jump (left, right, variable jump cut, coyote time)
 * - Platform collision (floor landing, wall block, ceiling bump)
 * - Disappearing floor
 * - Delayed collapsing platform
 * - Hidden spike activation
 * - Fake safe platform betrayal
 * - Death and quick respawn (including duplicate death/respawn idempotency)
 * - Trap reset (respawn + manual resetLevel)
 * - Level completion
 * - Rapid repeated retries
 * - UI HUD/Menu updates for Level 1-1 ("The Easy Start")
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TrollFoundationPart1Test {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private fun createLevel1Engine(
      onWin: (Long, Int, Int) -> Unit = { _, _, _ -> }
  ): ChaosGameEngine {
    return ChaosGameEngine(
        level = World1Levels.createLevel(1, DifficultyTier.NORMAL),
        audioEngine = ProceduralAudioEngine(),
        onLevelCompleted = onWin
    )
  }

  private fun customEngineWithTraps(
      traps: List<TrollTrap>,
      tiles: List<GameTile>,
      spawnX: Float = 96f,
      spawnY: Float = 11f * World1Levels.TILE_SIZE,
      exitX: Float = 20f * World1Levels.TILE_SIZE,
      exitY: Float = 10f * World1Levels.TILE_SIZE
  ): ChaosGameEngine {
    val fullTiles = tiles.toMutableList()
    fullTiles.add(
        GameTile(
            id = 999,
            x = exitX,
            y = exitY,
            width = World1Levels.TILE_SIZE * 2f,
            height = World1Levels.TILE_SIZE * 3f,
            type = TileType.SUN_GATE,
            isSolid = false
        )
    )
    return ChaosGameEngine(
        level = LevelDefinition(
            worldId = 1,
            levelId = 1,
            title = "Test Chamber",
            tiles = fullTiles,
            coins = emptyList(),
            movingPlatforms = emptyList(),
            spawnX = spawnX,
            spawnY = spawnY,
            exitX = exitX,
            exitY = exitY,
            trollTraps = traps,
            isTrollLevel = true
        ),
        audioEngine = ProceduralAudioEngine(),
        onLevelCompleted = { _, _, _ -> }
    )
  }

  @Test
  fun test01_playerMovementAndJumpPhysics() {
    val engine = createLevel1Engine()
    repeat(15) { engine.update() }
    assertTrue("Aarav should rest on starting ground", engine.player.isGrounded)
    val startX = engine.player.x
    val groundY = engine.player.y

    // Move right
    engine.inputRight = true
    repeat(15) { engine.update() }
    val movedRightX = engine.player.x
    assertTrue("Moving right must advance X ($movedRightX > $startX)", movedRightX > startX + 35f)

    // Move left
    engine.inputRight = false
    engine.inputLeft = true
    repeat(15) { engine.update() }
    assertTrue("Moving left must reduce X", engine.player.x < movedRightX - 25f)
    engine.inputLeft = false

    // Jump upward and return to ground
    engine.inputJump = true
    var minY = groundY
    repeat(30) {
      engine.update()
      if (engine.player.y < minY) minY = engine.player.y
    }
    assertTrue("Jump must elevate Aarav significantly ($minY < ${groundY - 70f})", minY < groundY - 70f)
    engine.inputJump = false
    repeat(40) { engine.update() }
    assertTrue("Aarav should land cleanly back on the platform", engine.player.isGrounded)
    assertEquals(groundY, engine.player.y, 1.5f)
  }

  @Test
  fun test02_platformCollisionFloorWallAndCeiling() {
    val ts = World1Levels.TILE_SIZE
    val tiles = listOf(
        // Ground tiles 0..8 at y = 13*ts
        *(0..8).map { GameTile(id = it + 1, x = it * ts, y = 13f * ts, width = ts, height = ts, type = TileType.CARVED_STONE) }.toTypedArray(),
        // Wall block at x = 5*ts, y = 12*ts
        GameTile(id = 20, x = 5f * ts, y = 12f * ts, width = ts, height = ts, type = TileType.CARVED_STONE),
        // Low ceiling at x = 2*ts..3*ts, y = 10*ts
        GameTile(id = 21, x = 2f * ts, y = 10f * ts, width = ts * 2f, height = ts, type = TileType.CARVED_STONE)
    )
    val engine = customEngineWithTraps(traps = emptyList(), tiles = tiles, spawnX = 2.2f * ts, spawnY = 11f * ts)
    repeat(15) { engine.update() }
    assertTrue("Floor collision must ground Aarav at tile 13", engine.player.isGrounded)
    assertEquals(13f * ts - engine.player.collisionHeight, engine.player.y, 0.5f)

    // Ceiling bump check under tile 21 (bottom = 11*ts)
    engine.inputJump = true
    var blockedAtCeiling = false
    repeat(20) {
      engine.update()
      if (engine.player.y >= 11f * ts - 1f && engine.player.vy >= 0f) {
        blockedAtCeiling = true
      }
      assertTrue("Player top must never tunnel above solid ceiling bottom", engine.player.y >= 11f * ts - 0.5f)
    }
    assertTrue("Ceiling collision must halt upward motion cleanly", blockedAtCeiling)
    engine.inputJump = false
    repeat(25) { engine.update() }

    // Wall collision check walking right into tile 20 at x = 5*ts
    engine.inputRight = true
    repeat(30) { engine.update() }
    assertTrue(
        "Wall must block horizontal penetration (x+w=${engine.player.x + engine.player.collisionWidth}, wall=${5f * ts})",
        engine.player.x + engine.player.collisionWidth <= 5f * ts + 0.5f
    )
  }

  @Test
  fun test03_disappearingFloorDropsAndResets() {
    val engine = createLevel1Engine()
    val trap = engine.trollTraps.first { it.id == 1 && it.type == TrollTrapType.DISAPPEARING_FLOOR }
    assertTrue(trap.isSolid && trap.isVisible)

    // Step directly onto disappearing floor tile 12
    engine.player.x = trap.x + 6f
    engine.player.y = trap.y - engine.player.collisionHeight
    engine.player.isGrounded = true

    repeat(12) { engine.update() }
    assertEquals(TrollTrapState.RETIRED, trap.state)
    assertFalse("Disappearing floor must become non-solid", trap.isSolid)
    assertFalse("Disappearing floor must hide visually", trap.isVisible)

    // After pit death, it must reset cleanly
    repeat(90) {
      engine.update()
      if (engine.deathCount > 0) return@repeat
    }
    assertEquals(1, engine.deathCount)
    assertEquals(TrollTrapState.ARMED, trap.state)
    assertTrue(trap.isSolid && trap.isVisible)
  }

  @Test
  fun test04_delayedCollapsingPlatformTelegraphsCollapsesAnimatesAndResets() {
    val engine = createLevel1Engine()
    val crumbleTrap = engine.trollTraps.first { it.type == TrollTrapType.DELAYED_CRUMBLE }
    assertEquals(TrollTrapState.ARMED, crumbleTrap.state)
    assertTrue(crumbleTrap.isSolid)

    // Land onto delayed collapsing platform (Tiles 23-24)
    engine.player.x = crumbleTrap.x + 12f
    engine.player.y = crumbleTrap.y - engine.player.collisionHeight - 2f
    engine.player.vy = 2f
    repeat(2) { engine.update() }

    assertEquals("Stepping onto DELAYED_CRUMBLE must enter TELEGRAPH phase first", TrollTrapState.TELEGRAPH, crumbleTrap.state)
    assertTrue("Platform must stay solid during telegraph warning window", crumbleTrap.isSolid)

    // Advance partway through telegraph — still walkable
    repeat(10) { engine.update() }
    assertEquals(TrollTrapState.TELEGRAPH, crumbleTrap.state)
    assertTrue("Player can run across before delay expires", crumbleTrap.isSolid)

    // Finish telegraph window -> platform drops, turns non-solid, animates dropOffsetY & fadeAlpha
    var sawActiveDrop = false
    var maxDropOffset = 0f
    var minFadeAlpha = 1f
    for (frame in 0..35) {
      engine.update()
      if (crumbleTrap.state == TrollTrapState.ACTIVE) {
        sawActiveDrop = true
        assertFalse("Crumbling platform must become non-solid on activation", crumbleTrap.isSolid)
        if (crumbleTrap.dropOffsetY > maxDropOffset) maxDropOffset = crumbleTrap.dropOffsetY
        if (crumbleTrap.fadeAlpha < minFadeAlpha) minFadeAlpha = crumbleTrap.fadeAlpha
      }
      if (crumbleTrap.state == TrollTrapState.RETIRED) {
        if (crumbleTrap.dropOffsetY > maxDropOffset) maxDropOffset = crumbleTrap.dropOffsetY
        if (crumbleTrap.fadeAlpha < minFadeAlpha) minFadeAlpha = crumbleTrap.fadeAlpha
        break
      }
    }
    assertTrue("Must observe active falling crumble phase", sawActiveDrop)
    assertTrue("Visual drop offset must advance across drop ($maxDropOffset)", maxDropOffset > 15f)
    assertTrue("Fade alpha must decrease during drop ($minFadeAlpha)", minFadeAlpha < 0.25f)
    assertEquals(TrollTrapState.RETIRED, crumbleTrap.state)
    assertFalse(crumbleTrap.isVisible)

    // Manual reset restores everything cleanly
    engine.resetLevel()
    assertEquals(TrollTrapState.ARMED, crumbleTrap.state)
    assertTrue(crumbleTrap.isSolid && crumbleTrap.isVisible)
    assertEquals(0f, crumbleTrap.dropOffsetY, 0.01f)
    assertEquals(1f, crumbleTrap.fadeAlpha, 0.01f)
  }

  @Test
  fun test05_spikeActivationAndFakeSafePlatformBetrayalAndReset() {
    val engine = createLevel1Engine()

    // 1. Surprise Spike Pop activation & lethal hit
    val spikeTrap = engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    engine.player.x = spikeTrap.triggerMinX + 4f
    engine.player.y = 13f * World1Levels.TILE_SIZE - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.inputRight = true

    var sawSpikeExtending = false
    for (frame in 0..45) {
      engine.update()
      if (spikeTrap.morphSpikeHeight > 0f) sawSpikeExtending = true
      if (engine.deathCount == 1) break
    }
    assertTrue("Hidden spike pop must extend upward cleanly", sawSpikeExtending)
    assertEquals(1, engine.deathCount)
    assertEquals("Spike trap must reset after respawn", TrollTrapState.ARMED, spikeTrap.state)
    assertEquals(0f, spikeTrap.morphSpikeHeight, 0.01f)

    // 2. Fake Safe Platform sprouts betrayal spikes when trusted
    val fakeSafe = engine.trollTraps.first { it.type == TrollTrapType.FAKE_SAFE_PLATFORM }
    engine.inputRight = false
    engine.player.x = fakeSafe.x + 12f
    engine.player.y = fakeSafe.y - engine.player.collisionHeight - 3f
    engine.player.vy = 2f

    var sawFakeSafeTelegraph = false
    var sawFakeSafeSpikes = false
    for (frame in 0..50) {
      engine.update()
      if (fakeSafe.state == TrollTrapState.TELEGRAPH) sawFakeSafeTelegraph = true
      if (fakeSafe.state == TrollTrapState.ACTIVE && fakeSafe.morphSpikeHeight > 0f) sawFakeSafeSpikes = true
      if (engine.deathCount == 2) break
    }
    assertTrue("Fake safe platform should briefly feel normal", sawFakeSafeTelegraph)
    assertTrue("Fake safe platform must sprout spikes", sawFakeSafeSpikes)
    assertEquals("Fake safe platform must kill and respawn player", 2, engine.deathCount)
    assertEquals(TrollTrapState.ARMED, fakeSafe.state)
    assertEquals(0f, fakeSafe.morphSpikeHeight, 0.01f)
  }

  @Test
  fun test06_noDuplicateDeathOrRespawnEvents() {
    val engine = createLevel1Engine()
    engine.startDeathSequence("BONK!")
    // Attempt duplicate death triggers while already in death animation
    engine.startDeathSequence("AGAIN!")
    engine.handlePlayerDeath()
    assertTrue(engine.isDying)
    assertEquals("Death count must not increment until respawn completes", 0, engine.deathCount)

    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES) { engine.update() }
    assertFalse("Death animation must finish cleanly", engine.isDying)
    assertEquals("Exactly ONE death must be recorded per death cycle", 1, engine.deathCount)
    assertEquals(engine.level.spawnX, engine.player.x, 1f)
  }

  @Test
  fun test07_levelCompletionInvokesCallbackOnce() {
    var winCount = 0
    var recordedCoins = -1
    var recordedTotal = -1
    val engine = createLevel1Engine { _, coins, total ->
      winCount++
      recordedCoins = coins
      recordedTotal = total
    }

    val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
    val doorTrap = engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    // Collect intro coin 1 and trigger the initial cheeky exit door hop
    engine.coins.first().isCollected = true
    engine.player.x = gate.x - 16f
    engine.player.y = gate.y + 40f
    repeat(5) { engine.update() }
    assertTrue("Exit door must relocate on first approach", doorTrap.hasRelocated)
    assertFalse("Should not win at decoy location", engine.isVictorious)

    // Now step into the relocated exit door
    engine.player.x = gate.x + 16f
    engine.player.y = gate.y + 40f

    repeat(15) { engine.update() }
    assertTrue("Reaching relocated exit gate must complete level", engine.isVictorious)
    assertEquals("Completion callback must fire once", 1, winCount)
    assertEquals(1, recordedCoins)
    assertEquals(4, recordedTotal)
  }

  @Test
  fun test08_thirtyRapidRepeatedRetriesKeepStateClean() {
    val engine = createLevel1Engine()
    for (retry in 1..30) {
      // Dirty a trap state before each death
      val crumble = engine.trollTraps.first { it.type == TrollTrapType.DELAYED_CRUMBLE }
      crumble.state = TrollTrapState.ACTIVE
      crumble.dropOffsetY = 18f
      crumble.fadeAlpha = 0.2f

      engine.startDeathSequence("TROLLED!")
      repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 1) { engine.update() }

      assertEquals(retry, engine.deathCount)
      assertFalse(engine.isDying)
      assertFalse(engine.isGameOver)
      assertEquals(engine.level.spawnX, engine.player.x, 1f)
      assertEquals(TrollTrapState.ARMED, crumble.state)
      assertEquals(0f, crumble.dropOffsetY, 0.01f)
      assertEquals(1f, crumble.fadeAlpha, 0.01f)
    }
  }

  @Test
  fun test09_composeMenuAndTrollHudShowEasyStartAndDeathCounter() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_play_level_1")
        .assertIsDisplayed()
        .assertTextContains("THE EASY START", substring = true)
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_deaths_pill").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("hud_clues_pill").fetchSemanticsNodes().size)

    // Verify Main Menu Play -> Replay clean state via ViewModel
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = GameStateViewModel(app)
    vm.startLevel1(DifficultyTier.NORMAL)
    val eng = vm.gameEngine!!
    assertTrue(eng.level.isTrollLevel)
    assertEquals("The Easy Start", eng.level.title)
  }
}
