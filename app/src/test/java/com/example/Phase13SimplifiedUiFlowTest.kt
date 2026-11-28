package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeRecord
import com.example.data.ContinuousRunPersistedState
import com.example.data.CosmeticTrail
import com.example.data.PlayableCharacter
import com.example.data.PlayerTitle
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.VictoryEffect
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 13 Compose UI & Navigation verification suite (Robolectric):
 * - Simplified Main Menu contains ONLY CHAOS RUN 5 title, TAP TO PLAY, REWARDS, SETTINGS
 * - TAP TO PLAY starts or resumes continuous challenge progression directly with zero intermediate screens
 * - Rewards displays only reward items/milestones (no modes or character previews) & preserves earned unlocks
 * - Settings displays ONLY Music ON/OFF + Volume, SFX ON/OFF + Volume, Vibration ON/OFF & persists all 5
 * - Safe back navigation across Main Menu <-> Rewards / Settings / Gameplay
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase13SimplifiedUiFlowTest {

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
  fun test01_mainMenuShowsOnlyTitleTapToPlayRewardsAndSettingsWithoutCharacterOrModeClutter() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()
    composeTestRule.onNodeWithTag("text_home_title")
        .assertIsDisplayed()
        .assertTextEquals("CHAOS RUN 5")
    composeTestRule.onNodeWithTag("button_tap_to_start").assertIsDisplayed()
    composeTestRule.onNodeWithText("TAP TO PLAY").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithText("REWARDS").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_settings").assertIsDisplayed()
    composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()

    // No character selection/previews or mode-selection UI on Main Menu
    val forbiddenHomeTags = listOf(
        "button_modes_hub",
        "home_character_row",
        "home_character_card_aarav",
        "home_character_card_golu",
        "home_character_card_vega",
        "home_character_card_roko",
        "section_character_selection",
        "button_play_level_1",
        "button_continue_latest"
    )
    for (tag in forbiddenHomeTags) {
      assertEquals(0, composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size)
    }
    assertEquals(0, composeTestRule.onAllNodesWithText("MODES", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
  }

  @Test
  fun test02_tapToPlayLaunchesAndResumesContinuousProgressionDirectly() {
    // 1. Fresh TAP TO PLAY launches ch_01 directly
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()

    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertEquals(RunLaunchMode.CONTINUOUS_PROGRESSION, vm.runLaunchMode.value)
    assertEquals("ch_01", vm.gameEngine?.activeChallengeId)
    assertEquals(PlayableCharacter.AARAV, vm.gameEngine?.selectedCharacter)
    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()

    // Gameplay back pauses safely
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    assertTrue(vm.isUserPaused.value)

    // 2. Saved active run at ch_05 resumes ch_05 directly without character/mode prompts
    val ch5 = ChallengeCatalog.findById("ch_05")!!
    SaveRepository(app).saveContinuousRunState(
        ContinuousRunPersistedState(
            schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
            isRunActive = true,
            isRunInForeground = false,
            isRunCompleted = false,
            activeChallengeId = ch5.challengeId,
            activeChallengeSeed = ch5.challengeSeed,
            activeConfigVersion = ch5.configVersion,
            activeAdaptiveTier = AdaptiveTier.STANDARD,
            currentChallengeDeaths = 0,
            completedChallengeIds = setOf("ch_01", "ch_02", "ch_03", "ch_04"),
            challengeRecords = mapOf("ch_01" to ChallengeRecord("ch_01", completed = true, bestTimeMs = 12_000L, bestScore = 500, stars = 3, leastDeaths = 0)),
            cumulativeRunTimerMs = 45_000L,
            cumulativeDeaths = 2,
            cumulativeScore = 1800,
            bestRunTimeMs = 0L,
            bestRunScore = 0,
            bestRunLeastDeaths = -1
        ),
        syncCommit = true
    )

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      composeTestRule.waitForIdle()
      lateinit var resumeVm: GameStateViewModel
      scenario.onActivity { resumeVm = ViewModelProvider(it)[GameStateViewModel::class.java] }
      assertEquals(AppScreen.MAIN_MENU, resumeVm.currentScreen.value)
      composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
      composeTestRule.waitForIdle()
      assertEquals(AppScreen.GAMEPLAY, resumeVm.currentScreen.value)
      assertEquals("ch_05", resumeVm.gameEngine?.activeChallengeId)
    } finally {
      scenario.close()
    }
  }

  @Test
  fun test03_rewardsScreenShowsOnlyRewardsPreservesUnlocksAndReturnsToMainMenu() {
    val repo = SaveRepository(app)
    repo.saveMasteryObjectivesEarned("ch_01", setOf("ch_01_zero_deaths", "ch_01_speed_par", "ch_01_no_coins"), syncCommit = true)
    repo.saveUnlockedRewards(
        trails = setOf(CosmeticTrail.EMBER_SPARK),
        effects = setOf(VictoryEffect.SAFFRON_FIREWORKS),
        titles = setOf(PlayerTitle.TRAP_SURVIVOR),
        syncCommit = true
    )

    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.refreshSavedProgress()
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("button_rewards").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.PROGRESSION_HUB, vm.currentScreen.value)
    composeTestRule.onNodeWithTag("text_rewards_title").assertIsDisplayed()

    // Earned rewards remain unlocked and equippable; no gameplay mode launchers or character selection
    composeTestRule.onNodeWithTag("reward_trail_ember_spark").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(CosmeticTrail.EMBER_SPARK, SaveRepository(app).loadPhase4RetentionSnapshot().rewards.equippedTrail)

    assertEquals(0, composeTestRule.onAllNodesWithTag("button_play_daily").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("modifier_card_speed_run").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("section_character_selection").fetchSemanticsNodes().size)

    // Back returns cleanly to Main Menu
    composeTestRule.onNodeWithTag("button_hub_back").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.MAIN_MENU, vm.currentScreen.value)
    composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()
  }

  @Test
  fun test04_settingsContainsOnlyAudioAndVibrationControlsPersistsChangesAndReturnsCleanly() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.SETTINGS, vm.currentScreen.value)
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()

    // Exact 5 settings controls present
    composeTestRule.onNodeWithTag("switch_music").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("slider_music_volume").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_sfx").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("slider_sfx_volume").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_vibration").performScrollTo().assertIsDisplayed().performClick()

    composeTestRule.activityRule.scenario.onActivity {
      vm.setMusicVolume(0.35f)
      vm.setSfxVolume(0.55f)
    }
    composeTestRule.waitForIdle()

    // Excluded UI sections must NOT appear in Settings
    val forbiddenSettingsTags = listOf(
        "section_character_selection",
        "character_card_aarav",
        "character_card_golu",
        "section_touch_controls",
        "button_replay_onboarding",
        "card_privacy_notice"
    )
    for (tag in forbiddenSettingsTags) {
      assertEquals(0, composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size)
    }

    // Back button returns cleanly to Main Menu and persistence survives reload
    composeTestRule.onNodeWithTag("button_settings_back").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.MAIN_MENU, vm.currentScreen.value)

    val repo = SaveRepository(app)
    assertFalse(repo.isMusicEnabled())
    assertFalse(repo.isSfxEnabled())
    assertFalse(repo.isVibrationEnabled())
    assertEquals(0.35f, repo.getMusicVolume(), 0.01f)
    assertEquals(0.55f, repo.getSfxVolume(), 0.01f)
  }
}
