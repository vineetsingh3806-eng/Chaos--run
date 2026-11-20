package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.DifficultyTier
import com.example.data.SaveRepository
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
class RemoveDifficultySelectionTest {

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
  fun test01_mainMenuAndSettingsHaveNoDifficultySelectionUi() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()

    // Difficulty selector row & buttons must not exist
    assertEquals(0, composeTestRule.onAllNodesWithTag("difficulty_selector_row").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("difficulty_normal").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("difficulty_hard").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("difficulty_nightmare").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("NORMAL", substring = true, ignoreCase = false).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("HARD", substring = true, ignoreCase = false).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("NIGHTMARE", substring = true, ignoreCase = false).fetchSemanticsNodes().size)

    // Settings screen must also contain no difficulty selection
    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("difficulty_selector_row").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Difficulty", substring = true, ignoreCase = true).fetchSemanticsNodes().size)

    composeTestRule.onNodeWithTag("button_settings_back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()
  }

  @Test
  fun test02_playFromMenuLaunchesDirectlyInNormalDifficulty() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_play_level_1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()

    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    val engine = vm.gameEngine!!
    assertEquals(1, engine.level.levelId)
    assertEquals(DifficultyTier.NORMAL, engine.level.difficulty)
    assertEquals(DifficultyTier.NORMAL.startingHealth, engine.player.maxHealth)
    assertEquals(DifficultyTier.NORMAL.startingHealth, engine.player.health)
  }

  @Test
  fun test03_legacySavedDifficultyIsIgnoredAndExistingSaveDataPreserved() {
    // Simulate legacy saved progress including an old non-Normal difficulty preference
    app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
        .edit()
        .putString("pref_difficulty_tier", "NIGHTMARE")
        .putInt("stars_1_1", 3)
        .putLong("best_time_1_1", 21450L)
        .commit()

    val saveRepo = SaveRepository(app)
    assertTrue("Existing save progress for level 1-1 must be preserved", saveRepo.isLevelCompleted(1, 1))
    assertEquals(3, saveRepo.getStars(1, 1))
    assertEquals(21450L, saveRepo.getBestTime(1, 1))
    assertTrue("Level 1-2 unlock from saved progress must be preserved", saveRepo.isLevelUnlocked(1, 2))

    val vm = GameStateViewModel(app)
    for (levelId in 1..5) {
      vm.startLevel(levelId = levelId, enforceUnlock = false)
      val engine = vm.gameEngine!!
      assertEquals("Level 1-$levelId must start in NORMAL difficulty", DifficultyTier.NORMAL, engine.level.difficulty)
      assertEquals(DifficultyTier.NORMAL.startingHealth, engine.player.maxHealth)
    }

    vm.restartLevel()
    assertEquals(DifficultyTier.NORMAL, vm.gameEngine!!.level.difficulty)
  }
}
