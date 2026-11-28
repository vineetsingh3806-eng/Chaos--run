package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.LevelTheme
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.engine.ExpansionAtlasRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CartoonBackgroundPolishTest {

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

  @Test
  fun test01_allFiveThemesRenderDistinctCleanCartoonBackdropsWithParallax() {
    val expansionRenderer = ExpansionAtlasRenderer()
    val topSkyColors = mutableSetOf<Int>()

    for (theme in LevelTheme.entries) {
      val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      expansionRenderer.drawThemeBackdrop(
          canvas = canvas,
          theme = theme,
          viewW = 1280f,
          viewH = 720f,
          cameraX = 240f,
          animTick = 30
      )
      // Top-left sky sample and lower-horizon sample should differ and never be pure black
      val topPixel = bmp.getPixel(40, 20)
      val horizonPixel = bmp.getPixel(40, 620)
      assertNotEquals("Sky and horizon should not be pure black for $theme", Color.BLACK, topPixel)
      assertNotEquals("Sky and horizon should have tonal separation for $theme", topPixel, horizonPixel)
      topSkyColors.add(topPixel)
    }

    assertEquals("Each World 1 theme should have a distinct sky color", LevelTheme.entries.size, topSkyColors.size)
    expansionRenderer.release()
  }

  @Test
  fun test02_zeroViewportDimensionsAndFullGameplayRenderStaySafeAndFast() {
    val renderer = ChaosGameRenderer(app)

    // Safe no-op on zero-sized viewport
    val tinyBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    renderer.expansionRenderer.drawThemeBackdrop(Canvas(tinyBmp), LevelTheme.WHISPERING_WOODS, 0f, 0f, 0f, 0)

    val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    for (levelId in 1..5) {
      val engine = ChaosGameEngine(World1Levels.createLevel(levelId), ProceduralAudioEngine()) { _, _, _ -> }
      repeat(12) { frame ->
        engine.player.x += 6f
        engine.update()
        renderer.render(canvas, engine, 1280f, 720f)
      }
      assertTrue("Renderer should mark hasRenderedFrame for level 1-$levelId", renderer.hasRenderedFrame)
    }
    renderer.expansionRenderer.release()
  }

  @Test
  fun test03_levelTransitionVeilSettlesCleanlyAndControlsRemainInteractive() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("control_left").assertIsDisplayed()
    composeTestRule.onNodeWithTag("control_right").assertIsDisplayed()
    composeTestRule.onNodeWithTag("control_jump").assertIsDisplayed()
    // After composeTestRule.waitForIdle(), the 260ms transition veil fades out and removes itself
    assertEquals(0, composeTestRule.onAllNodesWithTag("level_transition_veil").fetchSemanticsNodes().size)
  }
}
