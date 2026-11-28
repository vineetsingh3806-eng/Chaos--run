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
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.NotificationCategory
import com.example.data.NotificationPriority
import com.example.data.NotificationVisualKind
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertFalse
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
@Config(sdk = [36], qualifiers = "w412dp-h915dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameplayNotificationPortraitTest {

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

  private fun overlaps(a: Rect, b: Rect): Boolean {
    return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
  }

  @Test
  fun test01_portraitLayoutPlacesCompactNotificationInSafeZoneWithZeroTouchBlocking() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startLevel(levelId = 1, enforceUnlock = false)
    }
    composeTestRule.waitForIdle()
    val engine = vm.gameEngine!!

    engine.postNotification(
        eventKey = "PORTRAIT_WATCHER",
        title = "Watcher Sighting",
        message = "Canopy silhouette watches, then fades.",
        category = NotificationCategory.STORY_CLUE,
        priority = NotificationPriority.STORY,
        visualKind = NotificationVisualKind.STORY
    )
    ShadowLooper.idleMainLooper(180L, TimeUnit.MILLISECONDS)
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("whisper_banner").assertIsDisplayed()
    val bannerBounds = composeTestRule.onNodeWithTag("whisper_banner").fetchSemanticsNode().boundsInRoot
    val hudBounds = composeTestRule.onNodeWithTag("hud_top_bar").fetchSemanticsNode().boundsInRoot
    val leftBounds = composeTestRule.onNodeWithTag("control_left").fetchSemanticsNode().boundsInRoot
    val rightBounds = composeTestRule.onNodeWithTag("control_right").fetchSemanticsNode().boundsInRoot
    val jumpBounds = composeTestRule.onNodeWithTag("control_jump").fetchSemanticsNode().boundsInRoot
    val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot

    assertFalse("Portrait notification must not overlap HUD", overlaps(bannerBounds, hudBounds))
    assertFalse("Portrait notification must not overlap left control", overlaps(bannerBounds, leftBounds))
    assertFalse("Portrait notification must not overlap right control", overlaps(bannerBounds, rightBounds))
    assertFalse("Portrait notification must not overlap jump control", overlaps(bannerBounds, jumpBounds))
    assertTrue("Portrait notification stays inside bottom control clearance", bannerBounds.bottom <= minOf(leftBounds.top, rightBounds.top, jumpBounds.top) + 1f)
    assertTrue("Portrait notification stays outside vertical center playfield", bannerBounds.top >= rootBounds.height * 0.60f || bannerBounds.bottom <= rootBounds.height * 0.30f)

    // Verify zero touch interception & intact multi-touch Forward + Jump
    composeTestRule.onRoot().performTouchInput {
      down(0, bannerBounds.center)
      up(0)
    }
    composeTestRule.waitForIdle()
    assertFalse(engine.isPaused)

    composeTestRule.onRoot().performTouchInput {
      down(0, rightBounds.center)
      down(1, jumpBounds.center)
    }
    composeTestRule.waitForIdle()
    assertTrue(engine.inputRight && engine.inputJump)
    composeTestRule.onRoot().performTouchInput {
      up(1)
      up(0)
    }

    // Save portrait verification screenshot
    val outDir = File("build/reports/notification_evidence").apply { mkdirs() }
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val decor = activity.window.decorView
      val w = decor.width.coerceAtLeast(824)
      val h = decor.height.coerceAtLeast(1830)
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
      FileOutputStream(File(outDir, "portrait_notification_safe_area.png")).use {
        bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
      }
    }
    assertTrue(File(outDir, "portrait_notification_safe_area.png").length() > 2000L)
  }
}
