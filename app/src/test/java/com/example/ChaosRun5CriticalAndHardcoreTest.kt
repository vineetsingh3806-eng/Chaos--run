package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.engine.GameSurfaceView
import com.example.ui.GameStateViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChaosRun5CriticalAndHardcoreTest {

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

  private fun launchGameplayViaUi(difficulty: DifficultyTier = DifficultyTier.NORMAL): Pair<GameStateViewModel, ChaosGameEngine> {
    if (difficulty != DifficultyTier.NORMAL) {
      composeTestRule.onNodeWithTag("difficulty_${difficulty.name.lowercase()}").performClick()
      composeTestRule.waitForIdle()
    }
    composeTestRule.onNodeWithTag("button_play_level_1").performClick()
    composeTestRule.waitForIdle()
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    return vm to vm.gameEngine!!
  }

  private fun findGameSurfaceViews(root: View): List<GameSurfaceView> {
    val result = mutableListOf<GameSurfaceView>()
    if (root is GameSurfaceView) result.add(root)
    if (root is ViewGroup) {
      for (i in 0 until root.childCount) {
        result.addAll(findGameSurfaceViews(root.getChildAt(i)))
      }
    }
    return result
  }

  @Test
  fun test01_homeAndFocusLifecycleRecoveryClearsPointersAndRestoresAllControls() {
    val (vm, engine) = launchGameplayViaUi()

    // Persist a clue first to verify pause/resume preserves saved progress
    engine.onClueDiscovered(StoryClueId.ASH_PRINTS)
    assertTrue(vm.saveRepository.getDiscoveredClues().contains(StoryClueId.ASH_PRINTS))

    val leftCenter = composeTestRule.onNodeWithTag("control_left").fetchSemanticsNode().boundsInRoot.center
    val rightCenter = composeTestRule.onNodeWithTag("control_right").fetchSemanticsNode().boundsInRoot.center
    val jumpCenter = composeTestRule.onNodeWithTag("control_jump").fetchSemanticsNode().boundsInRoot.center

    // Hold Right + Jump as user presses Home
    composeTestRule.onRoot().performTouchInput {
      down(0, rightCenter)
      down(1, jumpCenter)
    }
    composeTestRule.waitForIdle()
    assertTrue(vm.touchControlState.isRightPressed)
    assertTrue(vm.touchControlState.isJumpPressed)
    assertTrue(engine.inputRight && engine.inputJump)

    val pausedX = engine.player.x
    val pausedY = engine.player.y
    val pausedHealth = engine.player.health

    // Simulate Home button -> focus loss -> onPause -> onStop (CREATED)
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onWindowFocusChanged(false)
    }
    composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
    composeTestRule.waitForIdle()

    // No stale pointers or stuck controls; engine is safely paused
    assertFalse("Left pointer must be cleared on background", vm.touchControlState.isLeftPressed)
    assertFalse("Right pointer must be cleared on background", vm.touchControlState.isRightPressed)
    assertFalse("Jump pointer must be cleared on background", vm.touchControlState.isJumpPressed)
    assertFalse(engine.inputLeft || engine.inputRight || engine.inputJump)
    assertTrue("Engine must pause while app is backgrounded", engine.isPaused)

    // Stepping updates while paused must not move or damage Aarav
    repeat(30) { engine.update() }
    assertEquals(pausedX, engine.player.x, 0.001f)
    assertEquals(pausedY, engine.player.y, 0.001f)
    assertEquals(pausedHealth, engine.player.health)

    // Return from Home -> onStart -> onResume -> onWindowFocusChanged(true)
    composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onWindowFocusChanged(true)
    }
    composeTestRule.waitForIdle()

    assertFalse("Engine must resume cleanly after returning from Home", engine.isPaused)
    assertFalse("No button may remain visually or logically pressed on resume",
        vm.touchControlState.isLeftPressed || vm.touchControlState.isRightPressed || vm.touchControlState.isJumpPressed)
    assertTrue("Saved progress must be intact across Home pause/resume",
        vm.saveRepository.getDiscoveredClues().contains(StoryClueId.ASH_PRINTS))

    // Immediately verify Left, Right, Jump and combined Forward+Jump work cleanly after resume
    composeTestRule.onRoot().performTouchInput { down(2, leftCenter) }
    composeTestRule.waitForIdle()
    assertTrue("Left control must respond immediately after Home return",
        vm.touchControlState.isLeftPressed && engine.inputLeft)
    composeTestRule.onRoot().performTouchInput { up(2) }
    composeTestRule.waitForIdle()

    composeTestRule.onRoot().performTouchInput {
      down(3, rightCenter)
      down(4, jumpCenter)
    }
    composeTestRule.waitForIdle()
    assertTrue("Right control must respond immediately after Home return",
        vm.touchControlState.isRightPressed && engine.inputRight)
    assertTrue("Jump control must respond immediately after Home return",
        vm.touchControlState.isJumpPressed && engine.inputJump)

    // Step physics and verify Aarav runs & jumps cleanly
    val xBefore = engine.player.x
    val yBefore = engine.player.y
    engine.player.isGrounded = true
    repeat(5) { engine.update() }
    assertTrue("Aarav must move right after resume", engine.player.x > xBefore)
    assertTrue("Aarav must jump upward after resume", engine.player.y < yBefore)

    composeTestRule.onRoot().performTouchInput {
      up(4)
      up(3)
    }
  }

  @Test
  fun test02_pauseDialogAndOverlayFocusLossRecoverControlsCleanly() {
    val (vm, engine) = launchGameplayViaUi()
    val leftCenter = composeTestRule.onNodeWithTag("control_left").fetchSemanticsNode().boundsInRoot.center
    val rightCenter = composeTestRule.onNodeWithTag("control_right").fetchSemanticsNode().boundsInRoot.center

    // System overlay / incoming call drops window focus while player holds Left
    composeTestRule.onRoot().performTouchInput { down(0, leftCenter) }
    composeTestRule.waitForIdle()
    assertTrue(engine.inputLeft)

    composeTestRule.activityRule.scenario.onActivity { it.onWindowFocusChanged(false) }
    composeTestRule.waitForIdle()
    assertTrue(engine.isPaused)
    assertFalse("Focus loss must clear held Left pointer", vm.touchControlState.isLeftPressed)
    // Cancel synthetic Compose touch stream just as Android dispatches ACTION_CANCEL on focus loss
    composeTestRule.onRoot().performTouchInput { cancel() }
    composeTestRule.waitForIdle()

    composeTestRule.activityRule.scenario.onActivity { it.onWindowFocusChanged(true) }
    composeTestRule.waitForIdle()
    assertFalse("Focus regain must unpause gameplay", engine.isPaused)

    // Explicit Pause button -> Pause Dialog -> Resume -> Controls remain responsive
    composeTestRule.onNodeWithTag("button_pause").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_pause").assertIsDisplayed()
    assertTrue(engine.isPaused)

    composeTestRule.onNodeWithTag("button_resume").performClick()
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("dialog_pause").fetchSemanticsNodes().size)
    assertFalse(engine.isPaused)

    composeTestRule.onRoot().performTouchInput { down(1, rightCenter) }
    composeTestRule.waitForIdle()
    assertTrue("Right control works cleanly after Pause->Resume", vm.touchControlState.isRightPressed && engine.inputRight)
    composeTestRule.onRoot().performTouchInput { up(1) }
  }

  @Test
  fun test03_surfaceDestructionAndRecreationPreservesPauseAndRestartsRenderThread() {
    val engine = ChaosGameEngine(World1Levels.createLevel1(), ProceduralAudioEngine()) { _, _, _ -> }
    val renderer = ChaosGameRenderer(app)
    val surfaceView = GameSurfaceView(app, engine, renderer)

    val dummyHolder = object : SurfaceHolder {
      override fun addCallback(callback: SurfaceHolder.Callback) {}
      override fun removeCallback(callback: SurfaceHolder.Callback) {}
      override fun isCreating(): Boolean = false
      override fun setType(type: Int) {}
      override fun setFixedSize(width: Int, height: Int) {}
      override fun setSizeFromLayout() {}
      override fun setFormat(format: Int) {}
      override fun setKeepScreenOn(screenOn: Boolean) {}
      override fun lockCanvas(): Canvas? = null
      override fun lockCanvas(dirty: android.graphics.Rect?): Canvas? = null
      override fun unlockCanvasAndPost(canvas: Canvas?) {}
      override fun getSurfaceFrame(): android.graphics.Rect = android.graphics.Rect(0, 0, 800, 480)
      override fun getSurface(): Surface? = null
    }

    assertFalse("GameSurfaceView must not steal Compose focus", surfaceView.isFocusable)
    surfaceView.setPaused(true)
    surfaceView.surfaceCreated(dummyHolder)
    assertTrue("Render thread must start on surfaceCreated", surfaceView.isRenderThreadAlive())

    surfaceView.surfaceDestroyed(dummyHolder)
    assertFalse("Render thread must stop cleanly on surfaceDestroyed", surfaceView.isRenderThreadAlive())

    surfaceView.surfaceCreated(dummyHolder)
    assertTrue("Render thread must restart cleanly after surface recreation", surfaceView.isRenderThreadAlive())
    surfaceView.setPaused(false)
    surfaceView.release()
    assertFalse(surfaceView.isRenderThreadAlive())
  }

  @Test
  fun test04_victoryRestartLevelResetsAllStateRepeatedlyAndRebindsSurfaceView() {
    val (vm, _) = launchGameplayViaUi()

    repeat(3) { cycle ->
      val activeSession = vm.gameSession.value!!
      val activeEngine = activeSession.engine

      // Verify actual SurfaceView in View hierarchy points to the current session's engine
      composeTestRule.activityRule.scenario.onActivity { activity ->
        val surfaceViews = findGameSurfaceViews(activity.window.decorView)
        assertTrue("Expected live GameSurfaceView in hierarchy on cycle $cycle", surfaceViews.isNotEmpty())
        assertSame(
            "GameSurfaceView must bind to latest session engine on cycle $cycle",
            activeEngine,
            surfaceViews.last().engine
        )
      }

      // Mutate trap + coin state on Troll Level 1-1
      activeEngine.coins[0].isCollected = true
      activeEngine.player.x = 18.2f * 48f
      activeEngine.player.y = 13f * 48f - activeEngine.player.collisionHeight
      activeEngine.update() // Triggers Surprise Spike telegraph/activation

      // Approach initial Sun Gate (relocates -> walk into relocated Sun Gate)
      val gate = activeEngine.tiles.first { it.type == TileType.SUN_GATE }
      activeEngine.player.x = gate.x + 24f
      activeEngine.player.y = gate.y + 48f
      activeEngine.update()
      activeEngine.player.x = gate.x + 24f
      activeEngine.player.y = gate.y + 48f
      repeat(3) { activeEngine.update() }
      assertTrue("Level must reach victorious state on cycle $cycle", activeEngine.isVictorious)

      ShadowLooper.idleMainLooper(100L, TimeUnit.MILLISECONDS)
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("dialog_victory").assertIsDisplayed()

      // Tap Replay Level from Victory Modal
      composeTestRule.onNodeWithTag("button_victory_replay").performClick()
      composeTestRule.waitForIdle()

      // Verify complete reset
      assertEquals(0, composeTestRule.onAllNodesWithTag("dialog_victory").fetchSemanticsNodes().size)
      assertNull(vm.victoryResult.value)

      val nextSession = vm.gameSession.value!!
      assertTrue("Replay must increment sessionId", nextSession.sessionId > activeSession.sessionId)
      val nextEngine = nextSession.engine
      assertNotSame(activeEngine, nextEngine)

      assertEquals(nextEngine.level.spawnX, nextEngine.player.x, 0.01f)
      assertEquals(nextEngine.level.spawnY, nextEngine.player.y, 0.01f)
      assertEquals(0f, nextEngine.player.vx, 0.001f)
      assertEquals(0f, nextEngine.player.vy, 0.001f)
      assertEquals(nextEngine.level.difficulty.startingHealth, nextEngine.player.health)
      assertEquals(0L, nextEngine.levelTimerMs)
      assertEquals(0, nextEngine.score)
      assertEquals(1, nextEngine.comboMultiplier)
      assertFalse(nextEngine.isVictorious)
      assertFalse(nextEngine.isGameOver)
      assertFalse(nextEngine.isPaused)
      assertTrue(nextEngine.coins.none { it.isCollected })
      assertTrue(nextEngine.trollTraps.all { it.state == TrollTrapState.ARMED && !it.hasRelocated && it.isVisible })
      assertEquals(nextEngine.level.exitX, nextEngine.tiles.first { it.type == TileType.SUN_GATE }.x, 0.01f)
      assertEquals(0f, nextSession.renderer.cameraX, 0.001f)
      assertEquals(0f, nextSession.renderer.cameraY, 0.001f)

      // Stepping updates does NOT immediately re-mark complete, and Right touch immediately moves Aarav
      repeat(10) { nextEngine.update() }
      assertFalse("Restarted level must not auto-complete", nextEngine.isVictorious)

      val rightCenter = composeTestRule.onNodeWithTag("control_right").fetchSemanticsNode().boundsInRoot.center
      composeTestRule.onRoot().performTouchInput { down(0, rightCenter) }
      composeTestRule.waitForIdle()
      val x0 = nextEngine.player.x
      repeat(5) { nextEngine.update() }
      assertTrue("Player must immediately move after Victory->Replay on cycle $cycle", nextEngine.player.x > x0)
      composeTestRule.onRoot().performTouchInput { up(0) }
    }
  }

  @Test
  fun test05_resetLevelAfterTrapsSetPiecesAndPauseCycles() {
    val engine = ChaosGameEngine(
        level = World1Levels.createLevel(3, DifficultyTier.HARD),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }

    // 1. Trigger multiple traps on Level 1-3 + pause cycles
    engine.isPaused = true
    repeat(3) { engine.update() }
    engine.isPaused = false

    val crumble = engine.trollTraps.first { it.type == TrollTrapType.DELAYED_CRUMBLE }
    crumble.state = TrollTrapState.RETIRED
    crumble.isSolid = false
    crumble.isVisible = false

    val hop1 = engine.trollTraps.first { it.type == TrollTrapType.RELOCATING_EXIT_DOOR }
    engine.player.x = hop1.triggerMinX + 10f
    engine.player.y = 12f * 48f - engine.player.collisionHeight
    engine.update()
    assertTrue("Exit must relocate on approach", hop1.hasRelocated)
    assertNotEquals(engine.level.exitX, engine.tiles.first { it.type == TileType.SUN_GATE }.x, 1f)

    engine.inputRight = true
    engine.isVictorious = true
    engine.resetLevel()

    assertEquals(engine.level.spawnX, engine.player.x, 0.01f)
    assertEquals(engine.level.spawnY, engine.player.y, 0.01f)
    assertEquals(DifficultyTier.HARD.startingHealth, engine.player.health)
    assertEquals(0L, engine.levelTimerMs)
    assertEquals(0, engine.score)
    assertFalse(engine.isVictorious || engine.isPaused || engine.isGameOver)
    assertFalse(engine.inputLeft || engine.inputRight || engine.inputJump)
    assertTrue(engine.trollTraps.all { it.state == TrollTrapState.ARMED && !it.hasRelocated && it.isVisible })
    assertEquals(engine.level.exitX, engine.tiles.first { it.type == TileType.SUN_GATE }.x, 0.01f)
  }

  @Test
  fun test06_difficultyTiersScaleHealthTrapTimingsAndSyntheticHazards() {
    // 1. Verify difficulty tier selector in UI switches to NIGHTMARE & launches 2-heart / tighter trap timings
    val (vm, nightmareEngine) = launchGameplayViaUi(DifficultyTier.NIGHTMARE)
    assertEquals(DifficultyTier.NIGHTMARE, vm.selectedDifficulty.value)
    assertEquals(2, nightmareEngine.player.maxHealth)
    assertEquals(2, nightmareEngine.player.health)

    val normalL4 = World1Levels.createLevel(4, DifficultyTier.NORMAL)
    val nightmareL4 = World1Levels.createLevel(4, DifficultyTier.NIGHTMARE)
    val normalBlink = normalL4.trollTraps.first { it.type == TrollTrapType.TIMED_BLINK_PLATFORM }
    val nightmareBlink = nightmareL4.trollTraps.first { it.type == TrollTrapType.TIMED_BLINK_PLATFORM }
    assertTrue("Nightmare tightens blink timing", nightmareBlink.cycleFrames < normalBlink.cycleFrames)

    // 2. Verify Hard mode 2-hit Guardian + rebounding shockwaves + hazards on a synthetic boss arena
    val t = 48f
    val bossArena = LevelDefinition(
        worldId = 1,
        levelId = 5,
        title = "Synthetic Boss",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 7f * t, width = 120f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 115f * t, y = 4f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = emptyList(),
        movingPlatforms = emptyList(),
        spawnX = 100f * t,
        spawnY = 5f * t,
        exitX = 115f * t,
        exitY = 4f * t,
        difficulty = DifficultyTier.HARD,
        critters = listOf(
            ForestCritter(id = 1, x = 10f * t, y = 7f * t - 26f, minX = 8f * t, maxX = 15f * t, behavior = CritterBehavior.LUNGE_ON_PROXIMITY),
            ForestCritter(id = 2, x = 20f * t, y = 7f * t - 26f, minX = 18f * t, maxX = 24f * t, behavior = CritterBehavior.HOPPING)
        ),
        fallingHazards = listOf(
            FallingHazard(id = 3, x = 114f * t, startY = 1f * t, groundY = 7f * t, triggerMinX = 113f * t, triggerMaxX = 115f * t, warningDurationFrames = 24)
        ),
        ambushTraps = listOf(
            AmbushSpikeTrap(id = 1, x = 30f * t, groundY = 7f * t, warningDurationFrames = 24, activeDurationFrames = 30)
        ),
        guardianConfig = GuardianEncounterConfig(
            x = 5392f, groundY = 7f * t, crystalX = 5425f, crystalY = 7f * t - 122f,
            triggerMinX = 5060f, triggerMaxX = 5380f, reboundX = 5150f, despawnMinX = 4880f,
            baseHitsNormal = 1, baseHitsHard = 2, baseHitsNightmare = 3
        )
    )
    val hardEngine = ChaosGameEngine(bossArena, ProceduralAudioEngine()) { _, _, _ -> }
    assertEquals(3, hardEngine.player.maxHealth)
    assertEquals(2, hardEngine.stoneGuardian.maxHits)

    hardEngine.player.x = 5142f
    hardEngine.player.y = 7f * t - hardEngine.player.collisionHeight
    hardEngine.player.isGrounded = true
    hardEngine.update()
    repeat(42) { hardEngine.update() }
    assertEquals(GuardianPhase.ACTIVE, hardEngine.stoneGuardian.phase)

    hardEngine.player.x = hardEngine.stoneGuardian.crystalX - hardEngine.player.collisionWidth / 2f
    hardEngine.player.y = hardEngine.stoneGuardian.crystalY - hardEngine.player.collisionHeight / 2f
    hardEngine.update()
    assertEquals(1, hardEngine.stoneGuardian.hitsRemaining)
    assertEquals(GuardianPhase.ACTIVE, hardEngine.stoneGuardian.phase)

    hardEngine.player.x = 5165f
    hardEngine.player.y = hardEngine.stoneGuardian.groundY - hardEngine.player.collisionHeight
    hardEngine.player.vx = 0f
    hardEngine.player.vy = 0f
    hardEngine.player.isGrounded = true

    var ticks = 0
    while (ticks < 160 && hardEngine.stoneGuardian.shockwaves.none { it.active }) {
      hardEngine.update()
      ticks++
    }
    val sw = hardEngine.stoneGuardian.shockwaves.firstOrNull { it.active }
    assertNotNull("Enraged Guardian must fire shockwave", sw)
    assertTrue("Hard Phase-2 shockwave must rebound", sw!!.isRebound)
    sw.x = sw.reboundX - 1f
    hardEngine.update()
    assertTrue("Shockwave reverses direction upon reaching reboundX", sw.hasReversed && sw.vx > 0f)

    repeat(40) { hardEngine.update() }
    hardEngine.player.x = hardEngine.stoneGuardian.crystalX - hardEngine.player.collisionWidth / 2f
    hardEngine.player.y = hardEngine.stoneGuardian.crystalY - hardEngine.player.collisionHeight / 2f
    hardEngine.update()
    assertEquals(GuardianPhase.DEFEATED, hardEngine.stoneGuardian.phase)

    // 3. Verify FallingHazard + AmbushSpikeTrap + Critter behaviors
    val keystone = hardEngine.fallingHazards.first()
    hardEngine.player.x = keystone.triggerMinX + 8f
    hardEngine.player.y = keystone.groundY - hardEngine.player.collisionHeight
    hardEngine.player.isGrounded = true
    hardEngine.update()
    assertTrue(keystone.isWarning && !keystone.isFalling)
    repeat(keystone.warningDurationFrames + 1) { hardEngine.update() }
    assertTrue(keystone.isFalling || keystone.hasTriggered)

    val trap = hardEngine.ambushTraps.first()
    hardEngine.player.x = trap.x + 8f
    hardEngine.player.y = trap.groundY - hardEngine.player.collisionHeight
    hardEngine.player.isGrounded = true
    hardEngine.update()
    assertEquals(TrapPhase.WARNING, trap.phase)
    hardEngine.player.x = trap.x - 160f
    repeat(trap.warningDurationFrames + 1) { hardEngine.update() }
    assertEquals(TrapPhase.ACTIVE, trap.phase)
    repeat(trap.activeDurationFrames + 1) { hardEngine.update() }
    assertEquals(TrapPhase.COOLDOWN, trap.phase)

    val lunger = hardEngine.critters.first { it.behavior == CritterBehavior.LUNGE_ON_PROXIMITY }
    hardEngine.player.x = lunger.x - 90f
    hardEngine.player.y = lunger.baseY
    repeat(4) { hardEngine.update() }
    assertTrue(lunger.alertFrames > 0 || lunger.lungeFrames > 0)

    val hopper = hardEngine.critters.first { it.behavior == CritterBehavior.HOPPING }
    var hopperLeaped = false
    repeat(105) {
      hardEngine.update()
      if (hopper.y < hopper.baseY) hopperLeaped = true
    }
    assertTrue(hopperLeaped)
  }

  @Test
  fun test07_generateStage5VisualEvidence() {
    val outDir = File("build/reports/stage5_evidence").apply { mkdirs() }

    // 1. Main Menu with Difficulty Tier selector
    val menuBmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    composeTestRule.onNodeWithTag("difficulty_nightmare").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(1280)
      val h = decor.height.coerceAtLeast(720)
      val snap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      decor.draw(Canvas(snap))
      Canvas(menuBmp).drawBitmap(Bitmap.createScaledBitmap(snap, 1280, 720, true), 0f, 0f, null)
    }
    FileOutputStream(File(outDir, "stage5_main_menu_difficulty.png")).use {
      menuBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    assertTrue(File(outDir, "stage5_main_menu_difficulty.png").length() > 2000L)

    // 2. Gameplay screen with lower safe-area whisper panel positioned above movement controls
    val (vm, uiEngine) = launchGameplayViaUi(DifficultyTier.HARD)
    val uiRenderer = vm.gameRenderer!!
    uiEngine.showContextNotification(
        "STAGE5_WHISPER",
        StoryClueId.WATCHER.whisperText,
        NotificationCategory.STORY_CLUE
    )
    uiEngine.player.x = 1520f
    uiEngine.player.y = 540f - uiEngine.player.collisionHeight
    uiEngine.update()
    ShadowLooper.idleMainLooper(220L, TimeUnit.MILLISECONDS)
    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()

    lateinit var whisperBmp: Bitmap
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(1280)
      val h = decor.height.coerceAtLeast(720)
      whisperBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(whisperBmp)
      uiRenderer.cameraX = (uiEngine.player.x - 420f).coerceAtLeast(0f)
      uiRenderer.cameraY = (uiEngine.player.y - 360f).coerceIn(-48f, 240f)
      uiRenderer.render(canvas, uiEngine, w.toFloat(), h.toFloat())

      val svs = findGameSurfaceViews(decor)
      svs.forEach { it.visibility = View.INVISIBLE }
      val savedBg = decor.background
      decor.background = null
      decor.draw(canvas)
      decor.background = savedBg
      svs.forEach { it.visibility = View.VISIBLE }
    }
    FileOutputStream(File(outDir, "stage5_lower_whisper_panel.png")).use {
      whisperBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    assertTrue(File(outDir, "stage5_lower_whisper_panel.png").length() > 2000L)

    // 3. Troll Traps 2x2 storyboard (1280 x 720) across World 1 Levels 1-2..1-5
    val strip = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    val sc = Canvas(strip)
    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(225, 9, 19, 24) }
    val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(253, 224, 71)
      textSize = 28f
      isFakeBoldText = true
    }
    val hRenderer = ChaosGameRenderer(app)

    val scenes = listOf(
        "1-2: Fake Safe Path & Fake Checkpoint" to (2 to 22f * 48f),
        "1-3: Lying Exit & Moving Crush Wall" to (3 to 24f * 48f),
        "1-4: Blinking Platforms & Falling Ceiling" to (4 to 28f * 48f),
        "1-5: Almost There Finale Gauntlet" to (5 to 52f * 48f)
    )
    scenes.forEachIndexed { idx, (caption, spec) ->
      val (lvlId, focusX) = spec
      val eng = ChaosGameEngine(World1Levels.createLevel(lvlId, DifficultyTier.NIGHTMARE), ProceduralAudioEngine()) { _, _, _ -> }
      eng.player.x = focusX
      eng.player.y = 11f * 48f
      repeat(10) { eng.update() }
      hRenderer.cameraX = (focusX - 420f).coerceAtLeast(0f)
      hRenderer.cameraY = 120f
      val raw = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
      val rc = Canvas(raw)
      hRenderer.render(rc, eng, 1280f, 720f)
      rc.drawRect(0f, 0f, 1280f, 52f, barPaint)
      rc.drawText(caption, 20f, 35f, capPaint)
      val scaled = Bitmap.createScaledBitmap(raw, 640, 360, true)
      sc.drawBitmap(scaled, (idx % 2) * 640f, (idx / 2) * 360f, null)
    }

    FileOutputStream(File(outDir, "stage5_hardcore_hazards_and_guardian.png")).use {
      strip.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    assertTrue(File(outDir, "stage5_hardcore_hazards_and_guardian.png").length() > 2000L)
  }
}
