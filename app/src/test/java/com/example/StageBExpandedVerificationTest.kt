package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Restored Stage B mechanical, persistence, and UI verification tests
 * ensuring full regression coverage alongside ChaosRunBenchmarkTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StageBExpandedVerificationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
  }

  private fun newEngine(): ChaosGameEngine {
    return ChaosGameEngine(
        level = World1Levels.createLevel1(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
  }

  @Test
  fun testJumpBufferExecutesJumpUponLanding() {
    val engine = newEngine()
    val player = engine.player
    // Hover 4 px above Beat 1 Intro Plains (y = 624)
    player.x = 200f
    player.y = 624f - player.collisionHeight - 4f
    player.prevY = player.y
    player.isGrounded = false
    player.coyoteTimer = 0
    player.vy = 5.0f

    // Press jump slightly before landing
    engine.inputJump = true
    engine.update() // lands + buffers jump; next frame or same frame triggers buffered jump
    engine.update()

    assertTrue("Jump buffer must trigger upward velocity after landing", player.vy < 0f)
  }

  private fun syntheticLegacyMechanicsLevel(): LevelDefinition {
    val t = World1Levels.TILE_SIZE
    val tiles = mutableListOf(
        GameTile(1, 0f, 13f * t, t * 4f, t, TileType.CARVED_STONE),
        GameTile(2, 5f * t, 13f * t, t, t, TileType.CRUMBLING_WOOD),
        GameTile(3, 8f * t, 13f * t, t, t, TileType.BRAMBLE_SPIKES),
        GameTile(4, 8f * t, 14f * t, t * 4f, t, TileType.CARVED_STONE),
        GameTile(5, 20f * t, 10f * t, t * 2f, t * 3f, TileType.SUN_GATE, isSolid = false)
    )
    return LevelDefinition(
        worldId = 1,
        levelId = 1,
        title = "Synthetic Legacy Fixture",
        tiles = tiles,
        coins = listOf(Coin(1, 3f * t, 12f * t)),
        movingPlatforms = listOf(
            MovingPlatform(id = 1, x = 12f * t, y = 12f * t, minX = 11f * t, maxX = 14f * t, centerX = 12.5f * t, amplitude = 1.5f * t)
        ),
        spawnX = 2f * t,
        spawnY = 11f * t,
        exitX = 20f * t,
        exitY = 10f * t
    )
  }

  @Test
  fun testCrumblingBridge45FrameCountdownAndCollapse() {
    val engine = ChaosGameEngine(
        level = syntheticLegacyMechanicsLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val bridgeTile = engine.tiles.first { it.type == TileType.CRUMBLING_WOOD }
    engine.player.x = bridgeTile.x + 8f
    engine.player.y = bridgeTile.y - engine.player.collisionHeight - 2f
    engine.player.prevY = engine.player.y - 4f
    engine.player.vy = 4.0f
    engine.player.isGrounded = false

    engine.update()
    assertTrue("Bridge must start crumbling", bridgeTile.isCrumbling)
    assertTrue("Bridge must remain solid immediately", bridgeTile.isSolid)

    repeat(ChaosGameEngine.CRUMBLE_DELAY_FRAMES - 1) {
      engine.update()
    }
    assertTrue("Bridge must collapse after 45 frames", bridgeTile.isTriggered)
    assertFalse("Collapsed bridge must become non-solid", bridgeTile.isSolid)
    assertFalse("Collapsed bridge must become invisible", bridgeTile.isVisible)
  }

  @Test
  fun testMovingPlatformCarriesGroundedPlayer() {
    val engine = ChaosGameEngine(
        level = syntheticLegacyMechanicsLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val mp = engine.movingPlatforms.single()
    val player = engine.player
    player.x = mp.x + 32f
    player.y = mp.y - player.collisionHeight - 2f
    player.prevY = player.y - 3f
    player.vy = 3.0f
    player.isGrounded = false

    engine.update()
    assertTrue("Player must land on moving platform", player.isGrounded)
    val xAfterLand = player.x
    engine.update()
    assertNotEquals("Moving platform vx must carry stationary grounded player", xAfterLand, player.x, 0.01f)
  }

  @Test
  fun testBrambleSpikesDamageInvulnerabilityAndThreeHitDeath() {
    val engine = ChaosGameEngine(
        level = syntheticLegacyMechanicsLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val spikeTile = engine.tiles.first { it.type == TileType.BRAMBLE_SPIKES }
    val player = engine.player
    assertEquals(3, player.health)

    // Hit 1
    player.x = spikeTile.x + 12f
    player.y = spikeTile.y + 16f - player.collisionHeight
    player.prevY = player.y - 2f
    player.vy = 2f
    engine.update()
    assertEquals("First spike hit reduces health to 2", 2, player.health)
    assertTrue("Player must gain invulnerability frames", player.invulnerableFrames > 0)

    // Immediate re-overlap while invulnerable must not deduct health
    player.x = spikeTile.x + 12f
    player.y = spikeTile.y + 16f - player.collisionHeight
    engine.update()
    assertEquals("Invulnerability prevents double damage", 2, player.health)

    // Clear invulnerability and knockback velocity for hits 2 and 3
    player.invulnerableFrames = 0
    player.vx = 0f
    player.vy = 2f
    player.x = spikeTile.x + 12f
    player.y = spikeTile.y + 16f - player.collisionHeight
    player.prevY = player.y - 2f
    engine.update()
    assertEquals(1, player.health)

    player.invulnerableFrames = 0
    player.vx = 0f
    player.vy = 2f
    player.x = spikeTile.x + 12f
    player.y = spikeTile.y + 16f - player.collisionHeight
    player.prevY = player.y - 2f
    engine.update()
    assertEquals("Third hit respawns player with full health at level start", 3, player.health)
    assertEquals(engine.level.spawnX, player.x, 1.0f)
  }

  @Test
  fun testPitKillRespawnsPlayerAtSpawnWithoutCheckpoint() {
    val engine = newEngine()
    val coin1 = engine.coins.first { it.id == 1 }
    coin1.isCollected = true

    engine.player.x = 600f
    engine.player.y = ChaosGameEngine.PIT_KILL_Y + 20f
    engine.update()

    assertEquals("Pit fall without checkpoint resets X to spawn", engine.level.spawnX, engine.player.x, 1.0f)
    assertFalse("Coin collected without checkpoint resets", coin1.isCollected)
  }

  @Test
  fun testWorld1LevelsHaveValidTrollCoordinates() {
    for (id in 1..5) {
      val level = World1Levels.createLevel(id)
      assertTrue("1-$id must be a troll level", level.isTrollLevel)
      assertTrue("1-$id must have >= 4 coins", level.coins.size >= 4)
      assertEquals((1..level.coins.size).toList(), level.coins.map { it.id })
      assertTrue("1-$id spawn must be near start", level.spawnX in 0f..(5 * 48f))
      val sunGate = level.tiles.firstOrNull { it.type == TileType.SUN_GATE }
      assertNotNull("1-$id must have SUN_GATE tile", sunGate)
      assertEquals(level.exitX, sunGate!!.x, 0.1f)
      assertEquals(level.exitY, sunGate.y, 0.1f)
    }
  }

  @Test
  fun testCoinPickupIncrementsCollectedCount() {
    val engine = newEngine()
    val coin1 = engine.coins.first { it.id == 1 }
    engine.player.x = coin1.x - engine.player.collisionWidth / 2
    engine.player.y = coin1.y - engine.player.collisionHeight / 2
    engine.update()
    assertTrue("Coin 1 must be collected on overlap", coin1.isCollected)
    assertEquals(1, engine.coins.count { it.isCollected })
  }

  @Test
  fun testSaveRepositoryBestTimeAndStarsOverwriteGuards() {
    val repo = SaveRepository(app)
    repo.saveBestTime(1, 1, 42000L)
    assertEquals(42000L, repo.getBestTime(1, 1))
    // Slower time must NOT overwrite faster record
    repo.saveBestTime(1, 1, 55000L)
    assertEquals(42000L, repo.getBestTime(1, 1))
    // Faster time MUST overwrite
    repo.saveBestTime(1, 1, 31500L)
    assertEquals(31500L, repo.getBestTime(1, 1))

    repo.saveStars(1, 1, 2)
    assertEquals(2, repo.getStars(1, 1))
    // Lower stars must NOT downgrade
    repo.saveStars(1, 1, 1)
    assertEquals(2, repo.getStars(1, 1))
    repo.saveStars(1, 1, 3)
    assertEquals(3, repo.getStars(1, 1))
  }

  @Test
  fun testAudioSettingsPersistenceAcrossRestart() {
    val vm = GameStateViewModel(app)
    vm.toggleMusic()
    vm.toggleSfx()
    vm.setMusicVolume(0.35f)
    vm.setSfxVolume(0.45f)

    val freshRepo = SaveRepository(app)
    assertFalse(freshRepo.isMusicEnabled())
    assertFalse(freshRepo.isSfxEnabled())
    assertEquals(0.35f, freshRepo.getMusicVolume(), 0.01f)
    assertEquals(0.45f, freshRepo.getSfxVolume(), 0.01f)
  }

  private fun finishLevel1TrollGate(e: ChaosGameEngine) {
    // Trigger Level 1-1 RELOCATING_EXIT_DOOR hop from Tile 36 to Tile 42.5, then enter gate
    e.player.x = 36f * 48f
    e.player.y = 11f * 48f
    e.update()
    e.player.x = e.currentExitX + 24f
    e.player.y = 11f * 48f
    e.update()
  }

  @Test
  fun testStarRatingTiersOneTwoAndThreeStars() {
    val vm = GameStateViewModel(app)
    // Tier 1: <45% coins AND >5 deaths -> 1 star
    vm.startLevel1()
    var e = vm.gameEngine!!
    repeat(6) { e.handlePlayerDeath() }
    finishLevel1TrollGate(e)
    assertEquals(1, vm.victoryResult.value!!.stars)

    // Tier 2: <=5 deaths with <50% coins -> 2 stars
    vm.restartLevel()
    e = vm.gameEngine!!
    finishLevel1TrollGate(e)
    assertEquals(2, vm.victoryResult.value!!.stars)

    // Tier 3: >=50% coins, <parTimeSeconds, <=3 deaths -> 3 stars
    vm.restartLevel()
    e = vm.gameEngine!!
    e.coins.take(3).forEach { it.isCollected = true }
    finishLevel1TrollGate(e)
    assertEquals(3, vm.victoryResult.value!!.stars)
  }

  @Test
  fun testCaptureActualComposeUiScreenshotsForStageCEvidence() {
    val outDir = File("build/reports/stage_c_evidence").apply { mkdirs() }
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.saveRepository.saveBestTime(1, 1, 14200L)
      vm.saveRepository.saveStars(1, 1, 2)
      vm.saveRepository.saveBestScore(1, 1, 450)
      vm.refreshSavedProgress()
    }
    composeTestRule.waitForIdle()

    fun saveWindowShot(filename: String) {
      composeTestRule.waitForIdle()
      composeTestRule.activityRule.scenario.onActivity { activity ->
        val decor = activity.window.decorView
        val w = decor.width.coerceAtLeast(1280)
        val h = decor.height.coerceAtLeast(720)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        decor.draw(canvas)
        FileOutputStream(File(outDir, filename)).use { fos ->
          bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
      }
      assertTrue("UI screenshot $filename must exist", File(outDir, filename).length() > 1000L)
    }

    // 1. Main Menu (minimal suspenseful home screen)
    composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_home_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_tap_to_start").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_settings").assertIsDisplayed()
    saveWindowShot("ui_main_menu.png")

    // 2. Settings Modal
    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    saveWindowShot("ui_settings.png")
    composeTestRule.onNodeWithTag("button_settings_back").performClick()

    // 3. Victory Screen (capture Sun Gate backdrop + HUD + Dialog window root into ui_victory_dialog.png)
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startLevel(1, enforceUnlock = false)
      val e = vm.gameEngine!!
      e.coins.take(3).forEach { it.isCollected = true }
      finishLevel1TrollGate(e)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_victory").assertIsDisplayed()
    val victoryNode = composeTestRule.onNodeWithTag("dialog_victory").fetchSemanticsNode()
    val bounds = victoryNode.boundsInWindow
    assertTrue("Victory dialog bounds must have non-zero size", bounds.width > 100f && bounds.height > 100f)

    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(1280)
      val h = decor.height.coerceAtLeast(720)
      val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = android.graphics.Canvas(bmp)
      val e = vm.gameEngine
      val r = vm.gameRenderer
      if (e != null && r != null) {
        r.cameraX = (e.player.x - 420f).coerceAtLeast(0f)
        r.cameraY = (e.player.y - 360f).coerceIn(-48f, 240f)
        r.render(canvas, e, w.toFloat(), h.toFloat())
      }

      fun findSurfaceViews(v: android.view.View, list: MutableList<android.view.SurfaceView>) {
        if (v is android.view.SurfaceView) list.add(v)
        if (v is android.view.ViewGroup) {
          for (i in 0 until v.childCount) findSurfaceViews(v.getChildAt(i), list)
        }
      }
      val surfaceViews = mutableListOf<android.view.SurfaceView>()
      findSurfaceViews(decor, surfaceViews)
      surfaceViews.forEach { it.visibility = android.view.View.INVISIBLE }
      decor.draw(canvas)
      surfaceViews.forEach { it.visibility = android.view.View.VISIBLE }

      val dialogViews = android.view.inspector.WindowInspector.getGlobalWindowViews().filter { it !== decor }
      assertTrue("Victory dialog root window must be present in WindowInspector", dialogViews.isNotEmpty())
      canvas.drawColor(0x99000000.toInt())
      for (dv in dialogViews) {
        val left = ((w - dv.width) / 2f).coerceAtLeast(0f)
        val top = ((h - dv.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(left, top)
        dv.draw(canvas)
        canvas.restore()
      }
      FileOutputStream(File(outDir, "ui_victory_dialog.png")).use { fos ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
      }
    }
    assertTrue("UI screenshot ui_victory_dialog.png must exist", File(outDir, "ui_victory_dialog.png").length() > 1000L)
  }
}
