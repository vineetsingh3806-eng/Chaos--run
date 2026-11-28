package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.*
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.ui.GameStateViewModel
import com.example.ui.input.GameControlButton
import com.example.ui.input.MultiTouchControlState
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
 * Stage D Verification Suite:
 * 1. All 10 mandatory multi-touch Forward + Jump reliability scenarios (Pointer-ID + Compose Touch Input + Engine).
 * 2. Air control, apex hang-time, landing squash, and directional transitions.
 * 3. Interactive gameplay mechanics (Dholak BounceDrums, Kanta Beetles, Monsoon WindDrafts,
 *    Temple Ghanta Pendulums, Brass Diyas, Combo Multiplier, Checkpoint Rollback, BestScore persistence).
 * 4. Fresh Stage D UI/HUD screenshots and 4-frame Forward+Jump+Land sequential evidence strip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MultiTouchAndGameplayTest {

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

  private fun launchGameplayViaUi(): Pair<GameStateViewModel, ChaosGameEngine> {
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    return vm to vm.gameEngine!!
  }

  private fun buttonCenterInRoot(tag: String): Offset {
    return composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center
  }

  // =========================================================================
  // 10 MANDATORY MULTI-TOUCH SCENARIOS
  // =========================================================================

  @Test
  fun test01_holdRightAndTapJumpViaComposeMultiTouchKeepsRightHeld() {
    val (vm, engine) = launchGameplayViaUi()
    engine.player.x = 120f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true
    val rightCenter = buttonCenterInRoot("control_right")
    val jumpCenter = buttonCenterInRoot("control_jump")

    composeTestRule.onRoot().performTouchInput {
      down(0, rightCenter)
    }
    composeTestRule.waitForIdle()
    assertTrue("Pointer 0 on control_right must assert RIGHT", vm.touchControlState.isRightPressed)
    assertTrue("Engine inputRight must be true", engine.inputRight)

    composeTestRule.onRoot().performTouchInput {
      down(1, jumpCenter)
    }
    composeTestRule.waitForIdle()
    assertTrue("While both held, RIGHT must remain true", engine.inputRight)
    assertTrue("While both held, JUMP must be true", engine.inputJump)

    // Step 1 frame -> takeoff!
    engine.update()
    assertTrue("Player must jump upward on simultaneous Right + Jump", engine.player.vy < 0f)
    assertTrue("Player must move forward horizontally", engine.player.vx > 0f)

    // Release ONLY Jump pointer 1
    composeTestRule.onRoot().performTouchInput {
      up(1)
    }
    composeTestRule.waitForIdle()
    assertFalse("JUMP pointer 1 released -> inputJump false", engine.inputJump)
    assertTrue("CRITICAL: RIGHT pointer 0 must STAY true after Jump release", vm.touchControlState.isRightPressed)
    assertTrue("CRITICAL: engine.inputRight must STAY true after Jump release", engine.inputRight)

    composeTestRule.onRoot().performTouchInput {
      up(0)
    }
    composeTestRule.waitForIdle()
    assertFalse("After releasing pointer 0, RIGHT clears cleanly", engine.inputRight)
  }

  @Test
  fun test02_holdRightAndHoldJumpReachesFullApexHeight() {
    val controlState = MultiTouchControlState()
    val engine = newEngine()
    controlState.bindEngine(engine)

    // Measure short tap height (2 frames held)
    engine.player.x = 180f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true
    controlState.onPointerDown(0L, GameControlButton.RIGHT)
    controlState.onPointerDown(1L, GameControlButton.JUMP)
    repeat(2) { engine.update() }
    controlState.onPointerUp(1L, GameControlButton.JUMP)
    var shortTapMinY = engine.player.y
    repeat(30) {
      engine.update()
      if (engine.player.y < shortTapMinY) shortTapMinY = engine.player.y
    }

    // Measure full hold jump height while RIGHT stays continuously held
    val fullHoldEngine = newEngine()
    val fullControl = MultiTouchControlState().apply { bindEngine(fullHoldEngine) }
    fullHoldEngine.player.x = 180f
    fullHoldEngine.player.y = 624f - fullHoldEngine.player.collisionHeight
    fullHoldEngine.player.isGrounded = true
    fullControl.onPointerDown(0L, GameControlButton.RIGHT)
    fullControl.onPointerDown(1L, GameControlButton.JUMP)
    var fullHoldMinY = fullHoldEngine.player.y
    repeat(32) {
      fullHoldEngine.update()
      if (fullHoldEngine.player.y < fullHoldMinY) fullHoldMinY = fullHoldEngine.player.y
      assertTrue("RIGHT must remain true throughout full jump arc", fullHoldEngine.inputRight)
    }

    assertTrue("Full hold jump ($fullHoldMinY) must reach higher apex than short tap ($shortTapMinY)", fullHoldMinY < shortTapMinY - 25f)
    assertTrue("Player must gain substantial forward distance while airborne", fullHoldEngine.player.x > 340f)
  }

  @Test
  fun test03_releaseJumpMidAirKeepsRightHeldAndContinuesRunningAfterLanding() {
    val controlState = MultiTouchControlState()
    val engine = newEngine()
    controlState.bindEngine(engine)
    engine.player.x = 140f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true

    // Hold RIGHT (pointer 0) + JUMP (pointer 1)
    controlState.onPointerDown(0L, GameControlButton.RIGHT)
    controlState.onPointerDown(1L, GameControlButton.JUMP)
    repeat(6) { engine.update() }
    assertFalse("Must be airborne", engine.player.isGrounded)

    // Release JUMP mid-air while RIGHT stays held
    controlState.onPointerUp(1L, GameControlButton.JUMP)
    assertTrue("RIGHT must stay pressed after mid-air Jump release", controlState.isRightPressed)
    assertTrue("engine.inputRight must stay true", engine.inputRight)
    assertFalse("inputJump must clear", engine.inputJump)

    // Simulate until landing + 20 extra frames without re-pressing RIGHT
    var landedFrame = -1
    for (frame in 1..60) {
      engine.update()
      assertTrue("inputRight must NEVER drop on frame $frame", engine.inputRight)
      if (engine.player.isGrounded && landedFrame == -1) {
        landedFrame = frame
      }
    }
    assertTrue("Player must land within 60 frames", landedFrame > 0)
    val xAtCheck = engine.player.x
    repeat(20) {
      engine.update()
      assertTrue("Post-landing RIGHT must remain active without re-pressing", engine.inputRight)
      assertEquals(ChaosGameEngine.MAX_RUN_SPEED, engine.player.vx, 0.01f)
    }
    assertTrue(
        "Aarav must continue running smoothly forward after landing without re-press",
        engine.player.x > xAtCheck + 120f
    )
  }

  @Test
  fun test04_holdRightAndRapidlyTapJumpRepeatedlyNeverDropsRight() {
    val (vm, engine) = launchGameplayViaUi()
    val rightCenter = buttonCenterInRoot("control_right")
    val jumpCenter = buttonCenterInRoot("control_jump")
    engine.player.x = 96f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true

    // Start holding RIGHT with pointer 0
    composeTestRule.onRoot().performTouchInput {
      down(0, rightCenter)
    }
    composeTestRule.waitForIdle()

    var successfulJumps = 0
    repeat(3) { cycle ->
      // Ensure grounded before next jump
      while (!engine.player.isGrounded) {
        engine.update()
        assertTrue("During rapid cycle $cycle descent, RIGHT must never drop", engine.inputRight)
      }

      // Tap JUMP with pointer 1 (different pointer ID per press like Android fingers)
      val jumpPid = cycle + 1
      composeTestRule.onRoot().performTouchInput {
        down(jumpPid, jumpCenter)
      }
      composeTestRule.waitForIdle()
      engine.update()
      if (engine.player.vy < 0f && !engine.player.isGrounded) {
        successfulJumps++
      }

      composeTestRule.onRoot().performTouchInput {
        up(jumpPid)
      }
      composeTestRule.waitForIdle()
      assertTrue("After releasing Jump #$cycle, RIGHT must stay pressed", vm.touchControlState.isRightPressed)
      assertTrue("After releasing Jump #$cycle, engine.inputRight must stay true", engine.inputRight)

      // Advance until landing on flat intro plain (y = 624)
      repeat(45) {
        if (!engine.player.isGrounded) engine.update()
      }
    }

    assertEquals("All 3 consecutive jumps while holding Forward must execute", 3, successfulJumps)
    composeTestRule.onRoot().performTouchInput { up(0) }
  }

  @Test
  fun test05_symmetricalHoldLeftAndJumpWorksReliably() {
    val (vm, engine) = launchGameplayViaUi()
    val leftCenter = buttonCenterInRoot("control_left")
    val jumpCenter = buttonCenterInRoot("control_jump")

    engine.player.x = 500f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true

    composeTestRule.onRoot().performTouchInput {
      down(0, leftCenter)
      down(1, jumpCenter)
    }
    composeTestRule.waitForIdle()
    engine.update()

    assertTrue("LEFT and JUMP both active", engine.inputLeft && engine.inputJump)
    assertTrue("Player jumps up while moving left", engine.player.vy < 0f && engine.player.vx < 0f)
    assertFalse("Facing becomes left", engine.player.facingRight)

    composeTestRule.onRoot().performTouchInput { up(1) }
    composeTestRule.waitForIdle()

    assertFalse(engine.inputJump)
    assertTrue("LEFT stays active after Jump release", vm.touchControlState.isLeftPressed && engine.inputLeft)

    composeTestRule.onRoot().performTouchInput { up(0) }
  }

  @Test
  fun test06_landingTriggersLandStateSquashAndPreservesContinuousForwardRun() {
    val controlState = MultiTouchControlState()
    val engine = newEngine()
    controlState.bindEngine(engine)

    engine.player.x = 120f
    engine.player.y = 624f - engine.player.collisionHeight - 6f
    engine.player.prevY = engine.player.y - 6f
    engine.player.vy = 6.0f
    engine.player.isGrounded = false

    controlState.onPointerDown(0L, GameControlButton.RIGHT)
    engine.update() // lands on tile 624

    assertTrue("Player should ground on tile", engine.player.isGrounded)
    assertEquals("Landing frame triggers polished LAND squash state", AaravState.LAND, engine.player.state)
    assertTrue("inputRight stays true across landing impact", engine.inputRight)

    repeat(10) { engine.update() }
    assertEquals("Transitions seamlessly from LAND into RUN", AaravState.RUN, engine.player.state)
    assertEquals("Full run velocity achieved after landing", ChaosGameEngine.MAX_RUN_SPEED, engine.player.vx, 0.01f)
  }

  @Test
  fun test07_pointerCancelAndSlideOffOnJumpNeverCancelsHeldRight() {
    val controlState = MultiTouchControlState()
    val engine = newEngine()
    controlState.bindEngine(engine)

    controlState.onPointerDown(0L, GameControlButton.RIGHT)
    controlState.onPointerDown(1L, GameControlButton.JUMP)
    assertTrue(engine.inputRight && engine.inputJump)

    // Simulate Android ACTION_CANCEL / POINTER_CANCEL on Jump pointer
    controlState.onPointerCancel(1L, GameControlButton.JUMP)
    assertFalse("Cancelled Jump pointer must clear", engine.inputJump)
    assertTrue("RIGHT pointer must stay completely unaffected by Jump cancel", engine.inputRight)

    // Also test real Compose UI slide-off Jump button while RIGHT stays down
    val (vm, uiEngine) = launchGameplayViaUi()
    val rightCenter = buttonCenterInRoot("control_right")
    val jumpCenter = buttonCenterInRoot("control_jump")
    composeTestRule.onRoot().performTouchInput {
      down(0, rightCenter)
      down(1, jumpCenter)
      moveTo(1, Offset(jumpCenter.x - 240f, jumpCenter.y - 240f)) // Slide far off Jump
    }
    composeTestRule.waitForIdle()
    assertFalse("Sliding far off Jump releases Jump pointer", uiEngine.inputJump)
    assertTrue("Sliding off Jump MUST NOT release held RIGHT pointer", uiEngine.inputRight && vm.touchControlState.isRightPressed)

    composeTestRule.onRoot().performTouchInput {
      up(1)
      up(0)
    }
  }

  @Test
  fun test08_midAirDirectionSwitchWhileHoldingJump() {
    val controlState = MultiTouchControlState()
    val engine = newEngine()
    controlState.bindEngine(engine)
    engine.player.x = 300f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true

    // Hold RIGHT + JUMP
    controlState.onPointerDown(0L, GameControlButton.RIGHT)
    controlState.onPointerDown(1L, GameControlButton.JUMP)
    repeat(6) { engine.update() }
    assertTrue(engine.player.vx > 0f && engine.inputJump)

    // Switch mid-air: lift RIGHT (0), press LEFT (2) while Jump (1) remains held
    controlState.onPointerUp(0L, GameControlButton.RIGHT)
    controlState.onPointerDown(2L, GameControlButton.LEFT)
    assertTrue("Jump remains held during air direction flip", engine.inputJump)
    assertTrue("Left activates cleanly", engine.inputLeft)
    assertFalse("Right deactivates cleanly", engine.inputRight)

    repeat(12) { engine.update() }
    assertTrue("Responsive air control reverses velocity left mid-air", engine.player.vx < 0f)

    // Release LEFT, re-press RIGHT while still holding Jump
    controlState.onPointerUp(2L, GameControlButton.LEFT)
    controlState.onPointerDown(3L, GameControlButton.RIGHT)
    repeat(12) { engine.update() }
    assertTrue("Reverses back right mid-air smoothly", engine.player.vx > 0f)
    assertTrue("Jump stayed true throughout direction switches", controlState.isJumpPressed)
  }

  @Test
  fun test09_deathRespawnPreservesHeldTouchWhilePauseOrExitClearsCleanly() {
    val (vm, engine) = launchGameplayViaUi()
    val controlState = vm.touchControlState

    // Hold RIGHT pointer 0 physically
    controlState.onPointerDown(0L, GameControlButton.RIGHT)
    assertTrue(engine.inputRight)

    // Fall into pit -> triggers fast troll death -> auto-respawn while Right stays held!
    engine.player.y = ChaosGameEngine.PIT_KILL_Y + 20f
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) { engine.update() }

    assertTrue("Holding Forward across pit/spike respawn MUST keep RIGHT pressed!", controlState.isRightPressed)
    assertTrue("Engine inputRight MUST stay true after respawn!", engine.inputRight)

    // Next update frame moves immediately forward without lifting thumb
    val xAfterRespawn = engine.player.x
    repeat(3) { engine.update() }
    assertTrue("Aarav accelerates right immediately on post-respawn frame", engine.player.x > xAfterRespawn)

    // Exiting to Main Menu MUST clear all active touches to prevent stuck keys
    vm.returnToMainMenu()
    assertFalse("Exiting gameplay clears RIGHT touch", controlState.isRightPressed)
    assertFalse("Exiting gameplay clears engine inputRight", engine.inputRight)
    assertEquals(0, controlState.activePointerCount())
  }

  @Test
  fun test10_dholakBounceDrumsCrittersWindDraftPendulumDiyasComboAndSavePersistence() {
    val t = 48f
    val synthLevel = LevelDefinition(
        worldId = 1,
        levelId = 1,
        title = "Synthetic Interactive Props",
        tiles = listOf(
            GameTile(id = 1, x = 0f, y = 13f * t, width = 60f * t, height = t, type = TileType.CARVED_STONE),
            GameTile(id = 2, x = 16f * t, y = 12f * t, width = t, height = t, type = TileType.CHECKPOINT_BELL, isSolid = false),
            GameTile(id = 3, x = 50f * t, y = 10f * t, width = 2f * t, height = 3f * t, type = TileType.SUN_GATE, isSolid = false)
        ),
        coins = List(10) { idx -> Coin(id = idx + 1, x = (3 + idx) * t, y = 11.5f * t) },
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 11f * t,
        exitX = 50f * t,
        exitY = 10f * t,
        bounceDrums = listOf(BounceDrum(id = 1, x = 8f * t, y = 13f * t - 20f)),
        critters = listOf(ForestCritter(id = 1, x = 11f * t, y = 13f * t - 26f, minX = 10f * t, maxX = 14f * t)),
        windDrafts = listOf(WindDraft(id = 1, x = 14f * t, y = 8f * t, width = 64f, height = 180f)),
        pendulums = listOf(SwingingPendulum(id = 1, anchorX = 32f * t, anchorY = 6f * t, length = 140f)),
        diyas = listOf(
            ForestDiya(id = 1, x = 20f * t, y = 11.5f * t),
            ForestDiya(id = 2, x = 22f * t, y = 11.5f * t),
            ForestDiya(id = 3, x = 24f * t, y = 11.5f * t)
        )
    )
    var completedScore = 0
    lateinit var engine: ChaosGameEngine
    engine = ChaosGameEngine(
        level = synthLevel,
        audioEngine = ProceduralAudioEngine(),
        onLevelCompleted = { _, _, _ -> completedScore = engine.score }
    )

    // 1. Dholak BounceDrum launches Aarav & starts combo
    val drum = engine.bounceDrums.first()
    engine.player.x = drum.x + 4f
    engine.player.y = drum.y - engine.player.collisionHeight - 2f
    engine.player.prevY = engine.player.y - 4f
    engine.player.vy = 4.0f
    engine.update()
    assertEquals("Dholak must impart high upward bounce", drum.launchVy, engine.player.vy, 0.8f)
    assertTrue("Drum squash feedback active", drum.squashFrames > 0)
    assertEquals(100, engine.score)
    assertEquals(2, engine.comboMultiplier)
    assertTrue("Camera shake subtly triggered", engine.cameraShakeFrames > 0)

    // 2. Stomp Kanta Beetle from above -> stuns + bounces + x2 combo (+300 -> 400),
    // then same-frame updateCoins() collects Coin #9 overhead at (11T, 11.5T) at x3 combo (+300 -> 700, combo -> 4)
    val critter = engine.critters.first()
    engine.player.x = critter.x + 2f
    engine.player.y = critter.y - engine.player.collisionHeight - 1f
    engine.player.prevY = engine.player.y - 4f
    engine.player.vy = 3.5f
    engine.update()
    assertTrue("Stomped beetle must be stunned", critter.stunFrames > 150)
    assertTrue("Aarav must bounce up off beetle", engine.player.vy < -10f)
    assertEquals("100 + (150 * x2) + (100 * x3 overhead coin) = 700 score", 700, engine.score)
    assertEquals(4, engine.comboMultiplier)

    // 3. Monsoon WindDraft lifts falling player
    val draft = engine.windDrafts.first()
    engine.player.x = draft.x + draft.width / 2f - engine.player.collisionWidth / 2f
    engine.player.y = draft.y + draft.height / 2f
    engine.player.prevY = engine.player.y
    engine.player.vy = 2.0f
    engine.update()
    assertTrue("WindDraft must lift player velocity upward", engine.player.vy < 2.0f)

    // 4. Brass Diyas: activate Checkpoint first, light 1 Diya post-checkpoint, die -> Diya rolls back!
    engine.player.x = 16f * t + 8f
    engine.player.y = 12f * t - engine.player.collisionHeight / 2f
    engine.update()
    assertTrue(engine.hasCheckpointActivated)
    val scoreAtCp = engine.score
    val postCpDiya = engine.diyas.last()
    engine.player.x = postCpDiya.x - engine.player.collisionWidth / 2f
    engine.player.y = postCpDiya.y - engine.player.collisionHeight / 2f
    engine.update()
    assertTrue("Diya must light on contact", postCpDiya.isLit)
    assertTrue("Score increased on Diya", engine.score > scoreAtCp)

    engine.handlePlayerDeath()
    assertFalse("Post-checkpoint Diya rolls back deterministically on death", postCpDiya.isLit)
    assertEquals("Score rolls back deterministically to checkpoint snapshot", scoreAtCp, engine.score)

    // 5. Light all 3 Diyas -> completes +500 Festival Bonus and persists BestScore on victory
    for (d in engine.diyas) {
      engine.player.x = d.x - engine.player.collisionWidth / 2f
      engine.player.y = d.y - engine.player.collisionHeight / 2f
      engine.update()
    }
    assertTrue("All 3 diyas lit", engine.diyas.all { it.isLit })

    engine.player.x = 50f * t + 24f
    engine.player.y = 10f * t
    engine.update()
    assertTrue(engine.isVictorious)
    assertTrue(completedScore > 0)

    // Also verify BestScore saves cleanly through ViewModel on troll Level 1-1
    val vm = GameStateViewModel(app)
    vm.startLevel1()
    val vmEngine = vm.gameEngine!!
    val c1 = vmEngine.coins.first()
    vmEngine.player.x = c1.x - vmEngine.player.collisionWidth / 2f
    vmEngine.player.y = c1.y - vmEngine.player.collisionHeight / 2f
    vmEngine.update()
    assertTrue("Collecting coin must raise score", vmEngine.score > 0)
    val gate = vmEngine.tiles.first { it.type == TileType.SUN_GATE }
    vmEngine.player.x = gate.x + 24f
    vmEngine.player.y = gate.y + 48f
    vmEngine.update() // relocates exit
    vmEngine.player.x = gate.x + 24f
    vmEngine.player.y = gate.y + 48f
    vmEngine.update() // wins
    assertTrue(vmEngine.isVictorious)
    assertEquals(vmEngine.score, SaveRepository(app).getBestScore(1, 1))

    // 6. Verify Temple Ghanta Pendulum swings continuously and is dangerous on contact
    val pendEngine = ChaosGameEngine(synthLevel, ProceduralAudioEngine()) { _, _, _ -> }
    val pend = pendEngine.pendulums.single()
    val startBobX = pend.bobX
    repeat(20) { pendEngine.update() }
    assertNotEquals("Pendulum must swing dynamically", startBobX, pend.bobX, 2.0f)
    pendEngine.player.x = pend.bobX - pendEngine.player.collisionWidth / 2f
    pendEngine.player.y = pend.bobY - pendEngine.player.collisionHeight / 2f
    pendEngine.player.invulnerableFrames = 0
    val hpBefore = pendEngine.player.health
    pendEngine.update()
    assertTrue("Touching Temple Ghanta bob reduces health", pendEngine.player.health < hpBefore)
  }

  // =========================================================================
  // FRESH VISUAL EVIDENCE & 4-FRAME MULTI-TOUCH ACTION SEQUENCE CAPTURE
  // =========================================================================

  @Test
  fun testCaptureStageDEvidenceScreenshotsAndMultiFrameSequence() {
    val outDir = File("build/reports/stage_d_evidence").apply { mkdirs() }

    // 1. Seed progress for crisp Main Menu Best Score + Stars screenshot
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.saveRepository.saveBestTime(1, 1, 29400L)
      vm.saveRepository.saveStars(1, 1, 3)
      vm.saveRepository.saveBestScore(1, 1, 2450)
      vm.saveRepository.saveClueDiscovered(StoryClueId.ASH_PRINTS)
      vm.saveRepository.saveClueDiscovered(StoryClueId.WATCHER)
      vm.saveRepository.saveClueDiscovered(StoryClueId.CHECKPOINT_BELL_GLYPH)
      vm.saveRepository.saveClueDiscovered(StoryClueId.ANCIENT_CARVING)
      vm.saveRepository.saveResonanceStoneDiscovered()
      vm.refreshSavedProgress()
    }
    composeTestRule.waitForIdle()

    // Capture Main Menu
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(1280)
      val h = decor.height.coerceAtLeast(720)
      val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      decor.draw(Canvas(bmp))
      FileOutputStream(File(outDir, "d_ui_main_menu.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    assertTrue(File(outDir, "d_ui_main_menu.png").length() > 1000L)

    // 2. Enter Gameplay -> Press BOTH Right + Jump simultaneously during live HUD notification!
    val (vm, engine) = launchGameplayViaUi()
    val renderer = vm.gameRenderer!!
    val demoCoin = engine.coins.first()
    engine.player.x = demoCoin.x - engine.player.collisionWidth / 2f
    engine.player.y = demoCoin.y - engine.player.collisionHeight / 2f
    engine.update()
    engine.player.x = 120f
    engine.player.y = 13f * 48f - engine.player.collisionHeight
    engine.player.isGrounded = true
    engine.showContextNotification(
        "UI_DEMO_WATCHER",
        "Stay sharp — fake floors and hidden spikes ahead!",
        NotificationCategory.STORY_CLUE
    )

    val rightCenter = buttonCenterInRoot("control_right")
    val jumpCenter = buttonCenterInRoot("control_jump")
    composeTestRule.onRoot().performTouchInput {
      down(0, rightCenter)
      down(1, jumpCenter)
    }
    composeTestRule.waitForIdle()
    repeat(6) { engine.update() }
    Thread.sleep(160L) // allow HUD polling coroutine & enter animation to sync
    composeTestRule.waitForIdle()

    // Render Composite Gameplay Canvas + Transparent Compose HUD & Active Pressed Multi-Touch Controls
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(1280)
      val h = decor.height.coerceAtLeast(720)
      val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      renderer.cameraX = (engine.player.x - 420f).coerceAtLeast(0f)
      renderer.cameraY = (engine.player.y - 360f).coerceIn(-48f, 240f)
      renderer.render(canvas, engine, w.toFloat(), h.toFloat())

      fun findSurfaceViews(v: android.view.View, list: MutableList<android.view.SurfaceView>) {
        if (v is android.view.SurfaceView) list.add(v)
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findSurfaceViews(v.getChildAt(i), list)
      }
      val sv = mutableListOf<android.view.SurfaceView>()
      findSurfaceViews(decor, sv)
      sv.forEach { it.visibility = android.view.View.INVISIBLE }
      val savedDecorBg = decor.background
      decor.background = null
      decor.draw(canvas)
      decor.background = savedDecorBg
      sv.forEach { it.visibility = android.view.View.VISIBLE }

      FileOutputStream(File(outDir, "d_ui_gameplay_multitouch_hud.png")).use {
        bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
      }
    }
    composeTestRule.onRoot().performTouchInput {
      up(1)
      up(0)
    }
    assertTrue(File(outDir, "d_ui_gameplay_multitouch_hud.png").length() > 1000L)

    // 3. Render Wide Interactive Action Scene (Troll traps on 1-2)
    val sceneEngine = ChaosGameEngine(World1Levels.createLevel(2), ProceduralAudioEngine()) { _, _, _ -> }
    val sceneRenderer = ChaosGameRenderer(app)
    sceneEngine.player.x = 14.5f * 48f
    sceneEngine.player.y = 10.5f * 48f
    sceneEngine.player.vx = 5.8f
    sceneEngine.player.vy = -5.2f
    sceneEngine.player.facingRight = true
    sceneEngine.update()
    val actionBmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    sceneRenderer.cameraX = (sceneEngine.player.x - 420f).coerceAtLeast(0f)
    sceneRenderer.cameraY = 0f
    sceneRenderer.render(Canvas(actionBmp), sceneEngine, 1280f, 720f)
    FileOutputStream(File(outDir, "d_gameplay_interactive_objects.png")).use {
      actionBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    assertTrue(File(outDir, "d_gameplay_interactive_objects.png").length() > 1000L)

    // 4. Render 4-Frame Sequential Forward + Jump + Release + Post-Landing Continuous Run Strip
    val seqEngine = newEngine()
    val seqControls = MultiTouchControlState().apply { bindEngine(seqEngine) }
    val seqRenderer = ChaosGameRenderer(app)
    seqEngine.player.x = 180f
    seqEngine.player.y = 624f - seqEngine.player.collisionHeight
    seqEngine.player.isGrounded = true

    val fullW = 1280
    val fullH = 720
    val panelW = 640
    val panelH = 360
    val stripBmp = Bitmap.createBitmap(panelW * 2, panelH * 2, Bitmap.Config.ARGB_8888)
    val stripCanvas = Canvas(stripBmp)
    val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 9, 19, 24) }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(253, 224, 71)
      textSize = 32f
      isFakeBoldText = true
    }

    fun stampPanel(col: Int, row: Int, caption: String) {
      val fullBmp = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
      val fullCanvas = Canvas(fullBmp)
      seqRenderer.cameraX = (seqEngine.player.x - 420f).coerceAtLeast(0f)
      seqRenderer.cameraY = (seqEngine.player.y - fullH * 0.55f).coerceIn(-48f, 240f)
      seqRenderer.render(fullCanvas, seqEngine, fullW.toFloat(), fullH.toFloat())
      fullCanvas.drawRect(0f, 0f, fullW.toFloat(), 58f, bannerPaint)
      fullCanvas.drawText(caption, 20f, 40f, labelPaint)
      val scaled = Bitmap.createScaledBitmap(fullBmp, panelW, panelH, true)
      stripCanvas.drawBitmap(scaled, (col * panelW).toFloat(), (row * panelH).toFloat(), null)
    }

    // Panel 1: Holding RIGHT -> running forward
    seqControls.onPointerDown(0L, GameControlButton.RIGHT)
    repeat(10) { seqEngine.update() }
    stampPanel(0, 0, "1. Hold RIGHT (ptr 0): x=${seqEngine.player.x.toInt()}, vx=%.1f".format(seqEngine.player.vx))

    // Panel 2: Press JUMP (ptr 1) while RIGHT stays held -> ascending cleanly
    seqControls.onPointerDown(1L, GameControlButton.JUMP)
    repeat(8) { seqEngine.update() }
    assertTrue(seqEngine.inputRight && seqEngine.inputJump && seqEngine.player.vy < 0f)
    stampPanel(1, 0, "2. RIGHT+JUMP (ptrs 0+1): x=${seqEngine.player.x.toInt()}, vy=%.1f".format(seqEngine.player.vy))

    // Panel 3: Release JUMP (ptr 1 UP) mid-air -> RIGHT stays TRUE!
    seqControls.onPointerUp(1L, GameControlButton.JUMP)
    repeat(10) { seqEngine.update() }
    assertTrue("Pointer 0 RIGHT stayed active after Jump UP", seqEngine.inputRight && !seqEngine.inputJump)
    stampPanel(0, 1, "3. JUMP Released (ptr 0 held): RIGHT=${seqEngine.inputRight}, x=${seqEngine.player.x.toInt()}")

    // Panel 4: Landed + running forward seamlessly with zero re-press!
    repeat(30) { seqEngine.update() }
    assertTrue("Landed & running continuously forward", seqEngine.player.isGrounded && seqEngine.inputRight && seqEngine.player.vx > 5f)
    stampPanel(1, 1, "4. Landed & Running (no re-press): x=${seqEngine.player.x.toInt()}, vx=%.1f".format(seqEngine.player.vx))

    FileOutputStream(File(outDir, "d_sequence_forward_jump_landing.png")).use {
      stripBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    assertTrue(File(outDir, "d_sequence_forward_jump_landing.png").length() > 2000L)

    // 5. Updated Victory Dialog screenshot (triggers Relocating Exit then finishes on true gate)
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val currentVm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      currentVm.startLevel(1, enforceUnlock = false)
    }
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val currentVm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      val e = currentVm.gameEngine!!
      e.coins.forEach { it.isCollected = true }
      val gate = e.tiles.first { it.type == TileType.SUN_GATE }
      e.player.x = gate.x + 24f
      e.player.y = gate.y + 48f
      e.update() // relocates
      e.player.x = gate.x + 24f
      e.player.y = gate.y + 48f
      e.update() // wins
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_victory").assertIsDisplayed()

    composeTestRule.activityRule.scenario.onActivity { activity ->
      val currentVm = androidx.lifecycle.ViewModelProvider(activity)[GameStateViewModel::class.java]
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(1280)
      val h = decor.height.coerceAtLeast(720)
      val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val e = currentVm.gameEngine!!
      val r = currentVm.gameRenderer!!
      r.cameraX = (e.player.x - 420f).coerceAtLeast(0f)
      r.cameraY = (e.player.y - 360f).coerceIn(-48f, 240f)
      r.render(canvas, e, w.toFloat(), h.toFloat())
      val dialogViews = android.view.inspector.WindowInspector.getGlobalWindowViews().filter { it !== decor }
      canvas.drawColor(0x99000000.toInt())
      for (dv in dialogViews) {
        val left = ((w - dv.width) / 2f).coerceAtLeast(0f)
        val top = ((h - dv.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(left, top)
        dv.draw(canvas)
        canvas.restore()
      }
      FileOutputStream(File(outDir, "d_ui_victory_dialog.png")).use {
        bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
      }
    }
    assertTrue(File(outDir, "d_ui_victory_dialog.png").length() > 1000L)
  }
}
