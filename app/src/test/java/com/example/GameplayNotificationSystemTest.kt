package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.DifficultyTier
import com.example.data.GuardianPhase
import com.example.data.NotificationCategory
import com.example.data.NotificationPriority
import com.example.data.NotificationVisualKind
import com.example.data.StoryClueId
import com.example.data.TrapPhase
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.ui.GameStateViewModel
import com.example.ui.screens.GameplayExclusionData
import com.example.ui.screens.NotificationPlacementResolver
import com.example.ui.screens.NotificationSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameplayNotificationSystemTest {

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

  private fun engineFor(levelId: Int, difficulty: DifficultyTier = DifficultyTier.NORMAL): ChaosGameEngine {
    return ChaosGameEngine(
        level = World1Levels.createLevel(levelId, difficulty),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
  }

  private fun launchLevelViaUi(levelId: Int): Pair<GameStateViewModel, ChaosGameEngine> {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startLevel(levelId = levelId, enforceUnlock = false)
    }
    composeTestRule.waitForIdle()
    return vm to vm.gameEngine!!
  }

  private fun syncNotificationUi() {
    ShadowLooper.idleMainLooper(180L, TimeUnit.MILLISECONDS)
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()
  }

  private fun overlaps(a: Rect, b: Rect): Boolean {
    return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
  }

  @Test
  fun test01_allNotificationCategoriesPriorityOrderingAndQueueBehavior() {
    val engine = engineFor(1)

    // 1. CRITICAL active -> lower-priority STORY, DISCOVERY, INFO queue behind it without stacking
    assertTrue(
        engine.postNotification(
            eventKey = "CRIT_1",
            title = "Falling Hazard",
            message = "Rockslide overhead — dodge clear!",
            category = NotificationCategory.EVENT_WARNING,
            priority = NotificationPriority.CRITICAL,
            visualKind = NotificationVisualKind.FALLING_HAZARD
        )
    )
    assertTrue(
        engine.postNotification(
            eventKey = "INFO_1",
            title = "Status",
            message = "Checkpoint anchored.",
            category = NotificationCategory.STORY_CLUE,
            priority = NotificationPriority.INFO,
            visualKind = NotificationVisualKind.INFO
        )
    )
    assertTrue(
        engine.postNotification(
            eventKey = "STORY_1",
            title = "Watcher Sighting",
            message = StoryClueId.WATCHER.whisperText,
            category = NotificationCategory.STORY_CLUE,
            priority = NotificationPriority.STORY,
            visualKind = NotificationVisualKind.STORY
        )
    )
    assertTrue(
        engine.postNotification(
            eventKey = "DISC_1",
            title = "Relic Uncovered",
            message = "Whispering Stalactite uncovered!",
            category = NotificationCategory.SECRET_DISCOVERY,
            priority = NotificationPriority.DISCOVERY,
            visualKind = NotificationVisualKind.DISCOVERY
        )
    )

    // Active banner stays CRITICAL (no stacking, no premature overwrite by lower priority)
    assertEquals("CRIT_1", engine.activeWhisperEventKey)
    assertEquals(3, engine.queuedNotificationCount)
    assertEquals(
        listOf("DISC_1", "STORY_1", "INFO_1"),
        engine.peekQueuedNotifications().map { it.eventKey }
    )

    // CRITICAL remains visible for reaction window (~150 frames)
    repeat(100) { engine.update() }
    assertEquals("CRIT_1", engine.activeWhisperEventKey)

    // After CRITICAL expires, DISCOVERY promotes first -> then STORY -> then INFO -> then null
    repeat(52) { engine.update() }
    assertEquals("DISC_1", engine.activeWhisperEventKey)
    repeat(122) { engine.update() }
    assertEquals("STORY_1", engine.activeWhisperEventKey)
    repeat(122) { engine.update() }
    assertEquals("INFO_1", engine.activeWhisperEventKey)
    repeat(92) { engine.update() }
    assertNull(engine.activeNotification)

    // 2. Higher-priority CRITICAL preempts lower-priority STORY, then resumes STORY afterward
    assertTrue(
        engine.postNotification(
            eventKey = "STORY_2",
            title = "Forest Whisper",
            message = "Warm ash-prints lead deeper.",
            category = NotificationCategory.STORY_CLUE,
            priority = NotificationPriority.STORY,
            visualKind = NotificationVisualKind.STORY
        )
    )
    repeat(10) { engine.update() }
    assertEquals("STORY_2", engine.activeWhisperEventKey)

    assertTrue(
        engine.postNotification(
            eventKey = "CRIT_2",
            title = "Storm Surge",
            message = "Lightning strike incoming!",
            category = NotificationCategory.EVENT_WARNING,
            priority = NotificationPriority.CRITICAL,
            visualKind = NotificationVisualKind.LIGHTNING_HAZARD
        )
    )
    assertEquals("CRIT_2", engine.activeWhisperEventKey)
    assertEquals(listOf("STORY_2"), engine.peekQueuedNotifications().map { it.eventKey })

    repeat(152) { engine.update() }
    assertEquals("STORY_2", engine.activeWhisperEventKey)
  }

  @Test
  fun test02_duplicateSuppressionAcrossKeysAndMessages() {
    val engine = engineFor(2)

    assertTrue(
        engine.postNotification(
            eventKey = "RELIC_A",
            title = "Relic Uncovered",
            message = "Echo Shard uncovered!",
            category = NotificationCategory.SECRET_DISCOVERY,
            priority = NotificationPriority.DISCOVERY,
            visualKind = NotificationVisualKind.DISCOVERY
        )
    )
    // Duplicate key rejected
    assertFalse(
        engine.postNotification(
            eventKey = "RELIC_A",
            title = "Relic Uncovered",
            message = "Different text same key",
            category = NotificationCategory.SECRET_DISCOVERY,
            priority = NotificationPriority.DISCOVERY,
            visualKind = NotificationVisualKind.DISCOVERY
        )
    )
    // Different key but identical active message rejected
    assertFalse(
        engine.postNotification(
            eventKey = "RELIC_B",
            title = "Relic Uncovered",
            message = "Echo Shard uncovered!",
            category = NotificationCategory.SECRET_DISCOVERY,
            priority = NotificationPriority.DISCOVERY,
            visualKind = NotificationVisualKind.DISCOVERY
        )
    )
    assertEquals(0, engine.queuedNotificationCount)

    // After expiration, same eventKey remains suppressed within run
    repeat(125) { engine.update() }
    assertNull(engine.activeNotification)
    assertFalse(
        engine.postNotification(
            eventKey = "RELIC_A",
            title = "Relic Uncovered",
            message = "Echo Shard uncovered!",
            category = NotificationCategory.SECRET_DISCOVERY,
            priority = NotificationPriority.DISCOVERY,
            visualKind = NotificationVisualKind.DISCOVERY
        )
    )
  }

  private fun syntheticCheckpointLevel(difficulty: DifficultyTier = DifficultyTier.NORMAL): com.example.data.LevelDefinition {
    val t = World1Levels.TILE_SIZE
    val tiles = mutableListOf<com.example.data.GameTile>()
    var id = 1
    for (tx in 0..30) {
      tiles.add(com.example.data.GameTile(id++, tx * t, 13f * t, t, t, com.example.data.TileType.CARVED_STONE))
    }
    tiles.add(com.example.data.GameTile(id++, 8f * t, 12f * t, t, t, com.example.data.TileType.CHECKPOINT_BELL, isSolid = false))
    tiles.add(com.example.data.GameTile(id++, 20f * t, 11f * t, t, t * 2f, com.example.data.TileType.SOUND_GATE, isSolid = true, groupId = 1))
    tiles.add(com.example.data.GameTile(id++, 26f * t, 10f * t, t * 2f, t * 3f, com.example.data.TileType.SUN_GATE, isSolid = false))
    return com.example.data.LevelDefinition(
        worldId = 1,
        levelId = 2,
        title = "Synthetic Checkpoint Fixture",
        tiles = tiles,
        coins = listOf(com.example.data.Coin(1, 5f * t, 12f * t)),
        movingPlatforms = emptyList(),
        spawnX = 2f * t,
        spawnY = 11f * t,
        exitX = 26f * t,
        exitY = 10f * t,
        difficulty = difficulty,
        soundSwitches = listOf(
            com.example.data.SoundSwitchNode(id = 2, groupId = 1, x = 15f * t, y = 12f * t)
        )
    )
  }

  @Test
  fun test03_checkpointRollbackAndRestartClearActiveAndQueuedMessages() {
    val engine = ChaosGameEngine(
        level = syntheticCheckpointLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }

    // Activate Checkpoint first
    val bell = engine.tiles.first { it.type == com.example.data.TileType.CHECKPOINT_BELL }
    engine.player.x = bell.x + 12f
    engine.player.y = bell.y + 12f
    engine.update()
    assertTrue(engine.hasCheckpointActivated)

    // Let INFO checkpoint notice expire, then trigger post-checkpoint Echo Switch + queued notice
    repeat(100) { engine.update() }
    val sw2 = engine.soundSwitches.first { it.id == 2 }
    engine.player.x = sw2.x - 8f
    engine.player.y = sw2.y - 8f
    engine.update()
    assertEquals("ECHO_SW_2", engine.activeWhisperEventKey)

    engine.postNotification(
        eventKey = "QUEUED_POST_CP",
        title = "Forest Whisper",
        message = "Should vanish on checkpoint rollback",
        category = NotificationCategory.STORY_CLUE,
        priority = NotificationPriority.STORY,
        visualKind = NotificationVisualKind.STORY
    )
    assertEquals(1, engine.queuedNotificationCount)

    // Die -> rollback to checkpoint MUST clear active and queued notifications immediately
    engine.handlePlayerDeath()
    assertNull("Active notification must clear on checkpoint rollback", engine.activeNotification)
    assertEquals("Queued notifications must clear on checkpoint rollback", 0, engine.queuedNotificationCount)

    // Advancing ticks must NOT resurrect old message
    repeat(20) { engine.update() }
    assertNull("Old messages must never reappear after checkpoint rollback", engine.activeNotification)

    // Re-triggering rolled-back Echo Switch #2 works cleanly
    engine.player.x = sw2.x - 8f
    engine.player.y = sw2.y - 8f
    engine.update()
    assertEquals("ECHO_SW_2", engine.activeWhisperEventKey)

    // Full level restart clears active and queued notifications
    engine.postNotification(
        eventKey = "PRE_RESTART",
        title = "Hazard Warning",
        message = "Should vanish on restart",
        category = NotificationCategory.EVENT_WARNING,
        priority = NotificationPriority.CRITICAL,
        visualKind = NotificationVisualKind.HAZARD_WARNING
    )
    engine.resetLevel()
    assertNull("Active notification must clear on restart", engine.activeNotification)
    assertEquals("Queued notifications must clear on restart", 0, engine.queuedNotificationCount)
  }

  @Test
  fun test04_world1Levels1To5NotificationsAndDifficultiesIntegrationWithoutPhysicsImpact() {
    // Verify all 5 World 1 Troll levels across Normal, Hard, Nightmare emit notifications & exit triumphs without altering physics/scoring
    for (tier in DifficultyTier.entries) {
      for (levelId in 1..5) {
        val engine = engineFor(levelId, tier)
        val scoreStart = engine.score
        assertTrue(
            engine.postNotification(
                eventKey = "TROLL_WARN_${levelId}_${tier.name}",
                title = engine.level.title,
                message = "Stay sharp — traps are live!",
                category = NotificationCategory.EVENT_WARNING,
                priority = NotificationPriority.CRITICAL,
                visualKind = NotificationVisualKind.HAZARD_WARNING,
                worldFocusX = engine.player.x + 120f,
                worldFocusY = engine.player.y
            )
        )
        engine.update()
        assertNotNull(engine.activeNotification)
        assertEquals("Notifications must not alter score on 1-$levelId", scoreStart, engine.score)
        assertFalse("Notifications must never pause engine on 1-$levelId", engine.isPaused)

        // Advance any RELOCATING_EXIT_DOOR hops, then verify SUN_GATE triumph notification
        var hops = 0
        while (hops < 3) {
          val reloc = engine.trollTraps.firstOrNull {
            it.type == com.example.data.TrollTrapType.RELOCATING_EXIT_DOOR && !it.hasRelocated &&
                kotlin.math.abs(it.initialX - engine.currentExitX) < 12f
          } ?: break
          engine.player.x = (reloc.triggerMinX + reloc.triggerMaxX) / 2f - engine.player.collisionWidth / 2f
          engine.player.y = (reloc.triggerMinY + reloc.triggerMaxY) / 2f - engine.player.collisionHeight / 2f
          engine.player.vx = 0f
          engine.player.vy = 0f
          engine.update()
          hops++
        }
        engine.player.x = engine.currentExitX + 24f
        engine.player.y = engine.currentExitY + 60f
        engine.update()
        assertTrue("1-$levelId ($tier) must reach victory cleanly", engine.isVictorious)
        assertEquals("E7_SUN_GATE_AWAKENING", engine.activeWhisperEventKey)
        assertEquals(NotificationVisualKind.TRIUMPH, engine.activeNotification!!.visualKind)
      }
    }
  }

  @Test
  fun test05_placementAcrossScreenDimensionsAndFallbackWhenLowerLeftOccupied() {
    data class ScreenSpec(val name: String, val w: Float, val h: Float, val insetLeft: Float = 0f, val insetBottom: Float = 0f)

    val specs = listOf(
        ScreenSpec("compact_landscape", 1280f, 720f),
        ScreenSpec("wide_landscape_cutout", 1680f, 824f, insetLeft = 64f, insetBottom = 48f),
        ScreenSpec("tablet_landscape", 2560f, 1600f),
        ScreenSpec("portrait_phone", 1030f, 2280f, insetBottom = 64f)
    )

    for (spec in specs) {
      val safeBounds = Rect(
          left = spec.insetLeft + 44f,
          top = 24f,
          right = spec.w - 44f,
          bottom = spec.h - spec.insetBottom - 32f
      )
      val hudBounds = Rect(safeBounds.left, safeBounds.top, safeBounds.right, safeBounds.top + 96f)
      val leftBtn = Rect(safeBounds.left, safeBounds.bottom - 148f, safeBounds.left + 148f, safeBounds.bottom)
      val rightBtn = Rect(leftBtn.right + 36f, safeBounds.bottom - 164f, leftBtn.right + 200f, safeBounds.bottom)
      val jumpBtn = Rect(safeBounds.right - 172f, safeBounds.bottom - 172f, safeBounds.right, safeBounds.bottom)
      val controls = listOf(leftBtn, rightBtn, jumpBtn)

      val bannerW = minOf(520f, safeBounds.width * 0.30f)
      val bannerH = 84f
      val gap = 20f

      // Case A: Clear lower-left -> chooses LOWER_LEFT_SAFE, zero overlap with controls/HUD/center
      val clearExclusions = GameplayExclusionData(
          playerRect = Rect(spec.w * 0.38f, spec.h * 0.45f, spec.w * 0.38f + 64f, spec.h * 0.45f + 108f),
          centerFocusRect = Rect(spec.w * 0.35f, spec.h * 0.28f, spec.w * 0.65f, spec.h * 0.68f),
          hazardAndEntityRects = emptyList()
      )
      val primary = NotificationPlacementResolver.resolvePlacement(
          safeAreaBounds = safeBounds,
          hudBounds = hudBounds,
          controlBounds = controls,
          exclusionData = clearExclusions,
          bannerWidthPx = bannerW,
          bannerHeightPx = bannerH,
          gapPx = gap,
          priority = NotificationPriority.STORY
      )
      assertEquals("${spec.name}: must default to LOWER_LEFT_SAFE", NotificationSlot.LOWER_LEFT_SAFE, primary.slot)
      assertFalse("${spec.name}: must not overlap HUD", overlaps(primary.rect, hudBounds))
      controls.forEach { ctrl ->
        assertFalse("${spec.name}: must not overlap control $ctrl", overlaps(primary.rect, ctrl))
      }
      assertFalse("${spec.name}: must not overlap center focus", overlaps(primary.rect, clearExclusions.centerFocusRect))
      assertTrue("${spec.name}: must stay inside safe bottom inset", primary.rect.bottom <= safeBounds.bottom)

      // Case B: Hazard occupying lower-left -> moves automatically into UPPER_LEFT_HUD!
      val occupiedExclusions = clearExclusions.copy(
          hazardAndEntityRects = listOf(
              Rect(safeBounds.left, leftBtn.top - 140f, safeBounds.left + bannerW, leftBtn.top)
          )
      )
      val fallback = NotificationPlacementResolver.resolvePlacement(
          safeAreaBounds = safeBounds,
          hudBounds = hudBounds,
          controlBounds = controls,
          exclusionData = occupiedExclusions,
          bannerWidthPx = bannerW,
          bannerHeightPx = bannerH,
          gapPx = gap,
          priority = NotificationPriority.CRITICAL
      )
      assertEquals("${spec.name}: must relocate to UPPER_LEFT_HUD when lower-left is occupied", NotificationSlot.UPPER_LEFT_HUD, fallback.slot)
      assertFalse("${spec.name}: fallback must not overlap HUD", overlaps(fallback.rect, hudBounds))
      controls.forEach { ctrl ->
        assertFalse("${spec.name}: fallback must not overlap controls", overlaps(fallback.rect, ctrl))
      }
      assertFalse("${spec.name}: fallback must not overlap hazard", overlaps(fallback.rect, occupiedExclusions.hazardAndEntityRects.first()))
    }
  }

  @Test
  fun test06_noTouchInterceptionAndMultiTouchForwardJumpIntactDuringNotification() {
    val (vm, engine) = launchLevelViaUi(1)
    engine.postNotification(
        eventKey = "TOUCH_TEST_WATCHER",
        title = "Watcher Sighting",
        message = StoryClueId.WATCHER.whisperText,
        category = NotificationCategory.STORY_CLUE,
        priority = NotificationPriority.STORY,
        visualKind = NotificationVisualKind.STORY
    )
    syncNotificationUi()

    composeTestRule.onNodeWithTag("whisper_banner").assertIsDisplayed()
    val bannerBounds = composeTestRule.onNodeWithTag("whisper_banner").fetchSemanticsNode().boundsInRoot
    val rightBounds = composeTestRule.onNodeWithTag("control_right").fetchSemanticsNode().boundsInRoot
    val jumpBounds = composeTestRule.onNodeWithTag("control_jump").fetchSemanticsNode().boundsInRoot

    // Verify notification sits outside all virtual control hitboxes
    assertFalse("Notification must not overlap control_right", overlaps(bannerBounds, rightBounds))
    assertFalse("Notification must not overlap control_jump", overlaps(bannerBounds, jumpBounds))

    // Tap directly through the notification banner center — must not pause or freeze or affect state
    composeTestRule.onRoot().performTouchInput {
      down(0, bannerBounds.center)
      up(0)
    }
    composeTestRule.waitForIdle()
    assertFalse("Tapping notification must not pause gameplay", engine.isPaused)

    // Hold Forward + Jump simultaneously while notification is active
    engine.player.x = 120f
    engine.player.y = 624f - engine.player.collisionHeight
    engine.player.isGrounded = true
    composeTestRule.onRoot().performTouchInput {
      down(0, rightBounds.center)
      down(1, jumpBounds.center)
    }
    composeTestRule.waitForIdle()
    engine.update()
    assertTrue("Forward remains active during live notification", engine.inputRight && vm.touchControlState.isRightPressed)
    assertTrue("Jump remains active during live notification", engine.inputJump && vm.touchControlState.isJumpPressed)
    assertTrue("Aarav leaps forward cleanly", engine.player.vy < 0f && engine.player.vx > 0f)

    // Release Jump -> Forward stays pressed
    composeTestRule.onRoot().performTouchInput { up(1) }
    composeTestRule.waitForIdle()
    assertTrue("Forward stays held after Jump release during notification", engine.inputRight && vm.touchControlState.isRightPressed)
    assertFalse(engine.inputJump)
    composeTestRule.onRoot().performTouchInput { up(0) }
  }

  @Test
  fun test07_generateUpdatedScreenshotsForAllFiveWorld1Levels() {
    val outDir = File("build/reports/notification_evidence").apply { mkdirs() }

    fun captureLevelNotificationScreenshot(levelId: Int, filename: String, setup: (ChaosGameEngine) -> Unit) {
      val (vm, engine) = launchLevelViaUi(levelId)
      setup(engine)
      syncNotificationUi()
      composeTestRule.onNodeWithTag("whisper_banner").assertIsDisplayed()

      composeTestRule.activityRule.scenario.onActivity { activity ->
        val decor = activity.window.decorView
        val w = decor.width.coerceAtLeast(1280)
        val h = decor.height.coerceAtLeast(720)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val renderer = vm.gameRenderer!!
        val (camX, camY) = renderer.effectiveCameraPosition(engine, w.toFloat(), h.toFloat())
        renderer.cameraX = camX
        renderer.cameraY = camY
        renderer.render(canvas, engine, w.toFloat(), h.toFloat())

        fun findSurfaceViews(v: android.view.View, list: MutableList<android.view.SurfaceView>) {
          if (v is android.view.SurfaceView) list.add(v)
          if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findSurfaceViews(v.getChildAt(i), list)
        }
        val sv = mutableListOf<android.view.SurfaceView>()
        findSurfaceViews(decor, sv)
        sv.forEach { it.visibility = android.view.View.INVISIBLE }
        val savedBg = decor.background
        decor.background = null
        decor.draw(canvas)
        decor.background = savedBg
        sv.forEach { it.visibility = android.view.View.VISIBLE }

        FileOutputStream(File(outDir, filename)).use { fos ->
          bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
      }
      assertTrue("Screenshot $filename must exist", File(outDir, filename).length() > 2000L)
    }

    // Level 1-1: The Easy Start
    captureLevelNotificationScreenshot(1, "level_1_1_troll_notification.png") { e ->
      e.postNotification(
          eventKey = "TROLL_1_1",
          title = "The Easy Start",
          message = "Watch every plank — the forest plays tricks now.",
          category = NotificationCategory.STORY_CLUE,
          priority = NotificationPriority.STORY,
          visualKind = NotificationVisualKind.STORY
      )
      e.update()
    }

    // Level 1-2: The Fake Safe Path
    captureLevelNotificationScreenshot(2, "level_1_2_fake_safe_notification.png") { e ->
      e.postNotification(
          eventKey = "TROLL_1_2",
          title = "The Fake Safe Path",
          message = "Smooth stone lies — trust the sketchy planks above!",
          category = NotificationCategory.SECRET_DISCOVERY,
          priority = NotificationPriority.DISCOVERY,
          visualKind = NotificationVisualKind.DISCOVERY
      )
      e.update()
    }

    // Level 1-3: The Exit Is Lying
    captureLevelNotificationScreenshot(3, "level_1_3_lying_exit_notification.png") { e ->
      e.postNotification(
          eventKey = "TROLL_1_3",
          title = "The Exit Is Lying",
          message = "Decoy doors bite — watch for the gate that hops!",
          category = NotificationCategory.EVENT_WARNING,
          priority = NotificationPriority.CRITICAL,
          visualKind = NotificationVisualKind.HAZARD_WARNING
      )
      e.update()
    }

    // Level 1-4: The Floor Has Plans
    captureLevelNotificationScreenshot(4, "level_1_4_floor_plans_notification.png") { e ->
      e.postNotification(
          eventKey = "TROLL_1_4",
          title = "The Floor Has Plans",
          message = "Time the blinking stones and watch the ceiling!",
          category = NotificationCategory.EVENT_WARNING,
          priority = NotificationPriority.CRITICAL,
          visualKind = NotificationVisualKind.LIGHTNING_HAZARD
      )
      e.update()
    }

    // Level 1-5: Almost There
    captureLevelNotificationScreenshot(5, "level_1_5_almost_there_notification.png") { e ->
      e.postNotification(
          eventKey = "TROLL_1_5",
          title = "Almost There",
          message = "Final gauntlet — expect one last exit surprise!",
          category = NotificationCategory.EVENT_WARNING,
          priority = NotificationPriority.CRITICAL,
          visualKind = NotificationVisualKind.BOSS_WARNING
      )
      e.update()
    }
  }
}
