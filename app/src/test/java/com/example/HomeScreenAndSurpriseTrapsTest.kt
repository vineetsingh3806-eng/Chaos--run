package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeRecord
import com.example.data.ContinuousRunPersistedState
import com.example.data.PlayableCharacter
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.TrapBehaviorCategory
import com.example.data.TrollTrapState
import com.example.data.TrollTrapType
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.TrapFairnessClass
import com.example.engine.TrapFairnessPolicy
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenAndSurpriseTrapsTest {

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

  private fun finishDeathAnimation(engine: ChaosGameEngine) {
    repeat(ChaosGameEngine.DEATH_ANIM_FRAMES + 2) {
      if (engine.isDying) engine.update()
    }
    assertFalse("Death animation must settle cleanly", engine.isDying)
  }

  private fun buildPersistedState(
      isRunActive: Boolean,
      isRunInForeground: Boolean,
      isRunCompleted: Boolean,
      activeChallengeId: String,
      completedChallengeIds: Set<String>,
      cumulativeRunTimerMs: Long = 0L,
      cumulativeDeaths: Int = 0,
      bestRunTimeMs: Long = 0L,
      bestRunScore: Int = 0,
      bestRunLeastDeaths: Int = -1,
      challengeRecords: Map<String, ChallengeRecord> = emptyMap()
  ): ContinuousRunPersistedState {
    val def = ChallengeCatalog.findById(activeChallengeId) ?: ChallengeCatalog.entries.first()
    return ContinuousRunPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunActive = isRunActive,
        isRunInForeground = isRunInForeground,
        isRunCompleted = isRunCompleted,
        activeChallengeId = def.challengeId,
        activeChallengeSeed = def.challengeSeed,
        activeConfigVersion = def.configVersion,
        activeAdaptiveTier = AdaptiveTier.STANDARD,
        currentChallengeDeaths = 0,
        completedChallengeIds = completedChallengeIds,
        challengeRecords = challengeRecords,
        cumulativeRunTimerMs = cumulativeRunTimerMs,
        cumulativeDeaths = cumulativeDeaths,
        cumulativeScore = 0,
        bestRunTimeMs = bestRunTimeMs,
        bestRunScore = bestRunScore,
        bestRunLeastDeaths = bestRunLeastDeaths
    )
  }

  @Test
  fun test01_minimalHomeScreenShowsOnlyRequiredElementsAndHidesProgression() {
    // Seed saved stars, scores, clues, and an unfinished continuous run at ch_04 to ensure NOTHING leaks to Home UI
    val repo = SaveRepository(app)
    repo.saveBestTime(1, 1, 12340L)
    repo.saveStars(1, 1, 3)
    repo.saveBestScore(1, 1, 1800)
    repo.saveContinuousRunState(
        buildPersistedState(
            isRunActive = true,
            isRunInForeground = false,
            isRunCompleted = false,
            activeChallengeId = "ch_04",
            completedChallengeIds = setOf("ch_01", "ch_02", "ch_03"),
            cumulativeRunTimerMs = 45_000L,
            cumulativeDeaths = 7
        ),
        syncCommit = true
    )

    composeTestRule.activityRule.scenario.onActivity { activity ->
      val vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.refreshSavedProgress()
    }
    composeTestRule.waitForIdle()
    run {
      // Phase 13 simplified Main Menu: Title, TAP TO PLAY, REWARDS, SETTINGS only
      composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()
      composeTestRule.onNodeWithTag("text_home_title")
          .assertIsDisplayed()
          .assertTextEquals("CHAOS RUN 5")
      composeTestRule.onNodeWithTag("button_settings").assertIsDisplayed()
      composeTestRule.onNodeWithTag("button_rewards").assertIsDisplayed()
      composeTestRule.onNodeWithTag("button_tap_to_start").assertIsDisplayed()
      composeTestRule.onNodeWithText("TAP TO PLAY").assertIsDisplayed()

      val density = app.resources.displayMetrics.density
      val minTouchPx = 48f * density

      val settingsBounds = composeTestRule.onNodeWithTag("button_settings").fetchSemanticsNode().boundsInRoot
      assertTrue("Settings touch width >= 48dp", settingsBounds.width >= minTouchPx - 1f)
      assertTrue("Settings touch height >= 48dp", settingsBounds.height >= minTouchPx - 1f)

      val rewardsBounds = composeTestRule.onNodeWithTag("button_rewards").fetchSemanticsNode().boundsInRoot
      assertTrue("Rewards touch width >= 48dp", rewardsBounds.width >= minTouchPx - 1f)
      assertTrue("Rewards touch height >= 48dp", rewardsBounds.height >= minTouchPx - 1f)

      val startBounds = composeTestRule.onNodeWithTag("button_tap_to_start").fetchSemanticsNode().boundsInRoot
      assertTrue("TAP TO PLAY height >= 48dp", startBounds.height >= minTouchPx - 1f)

      // Removed cluttered UI tags must all be absent
      val forbiddenTags = listOf(
          "button_play_level_1",
          "button_continue_latest",
          "button_continuous_run",
          "button_chapter_trail",
          "button_field_notes",
          "button_how_to_play",
          "button_view_run_summary",
          "card_first_launch_onboarding",
          "button_mute_toggle",
          "button_modes_hub",
          "home_character_row",
          "section_character_selection"
      )
      for (tag in forbiddenTags) {
        assertEquals(
            "Forbidden cluttered tag '$tag' must not exist on Home screen",
            0,
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size
        )
      }

      // No progression / level numbers / counters exposed on Home screen
      val forbiddenTexts = listOf(
          "PLAY 1-1",
          "CONTINUE",
          "CHAPTER TRAIL",
          "FIELD NOTES",
          "HOW TO PLAY",
          "RUN COMPLETION SUMMARY",
          "THE FOREST IS WATCHING",
          "WORLD 1",
          "1-1",
          "1-5",
          "ch_04",
          "CH ",
          "Clues",
          "pts"
      )
      for (text in forbiddenTexts) {
        assertEquals(
            "Forbidden text '$text' must not appear on Home screen",
            0,
            composeTestRule.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().size
        )
      }
    }
  }

  @Test
  fun test02_homeCharacterSelectionSyncsWithSettingsSaveRepositoryAndGameplay() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.waitForIdle()

    // Default character Aarav & programmatic character preservation work without UI clutter
    assertEquals(PlayableCharacter.AARAV, vm.selectedCharacter.value)
    assertEquals(0, composeTestRule.onAllNodesWithTag("home_character_row").fetchSemanticsNodes().size)

    composeTestRule.activityRule.scenario.onActivity {
      vm.selectCharacter(PlayableCharacter.ROKO, syncCommit = true)
    }
    composeTestRule.waitForIdle()
    assertEquals(PlayableCharacter.ROKO, SaveRepository(app).getSelectedCharacter())

    // Settings has NO character selection UI
    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("section_character_selection").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithTag("button_settings_save").performClick()
    composeTestRule.waitForIdle()

    // Tap To Play directly launches Gameplay preserving saved character
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()
    assertEquals(PlayableCharacter.ROKO, vm.gameRenderer?.selectedCharacter)
    assertEquals(PlayableCharacter.ROKO, SaveRepository(app).getSelectedCharacter())
  }

  @Test
  fun test03_tapToStartStartsFreshResumesUnfinishedAndStartsNewAfterCompletion() {
    // 1. Fresh run starts at ch_01
    composeTestRule.waitForIdle()
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
    }
    composeTestRule.onNodeWithTag("button_tap_to_start").performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertEquals(RunLaunchMode.CONTINUOUS_PROGRESSION, vm.runLaunchMode.value)
    assertEquals("ch_01", vm.gameEngine?.activeChallengeId)

    // 2. Active unfinished run at ch_03 -> TAP TO START resumes at ch_03
    val repo = SaveRepository(app)
    repo.saveContinuousRunState(
        buildPersistedState(
            isRunActive = true,
            isRunInForeground = false,
            isRunCompleted = false,
            activeChallengeId = "ch_03",
            completedChallengeIds = setOf("ch_01", "ch_02"),
            cumulativeRunTimerMs = 24_000L,
            cumulativeDeaths = 3
        ),
        syncCommit = true
    )
    val resumeScenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      composeTestRule.waitForIdle()
      lateinit var resumeVm: GameStateViewModel
      resumeScenario.onActivity { resumeVm = ViewModelProvider(it)[GameStateViewModel::class.java] }
      assertEquals(AppScreen.MAIN_MENU, resumeVm.currentScreen.value)
      resumeVm.startOrContinueContinuousRun()
      assertEquals(AppScreen.GAMEPLAY, resumeVm.currentScreen.value)
      assertEquals("ch_03", resumeVm.gameEngine?.activeChallengeId)
    } finally {
      resumeScenario.close()
    }

    // 3. Previously completed run -> TAP TO START starts a fresh run at ch_01 while preserving bestRunScore
    val completedRecords = ChallengeCatalog.entries.associate {
      it.challengeId to ChallengeRecord(
          challengeId = it.challengeId,
          completed = true,
          bestTimeMs = 15_000L,
          bestScore = 1000,
          stars = 3,
          leastDeaths = 0
      )
    }
    repo.saveContinuousRunState(
        buildPersistedState(
            isRunActive = false,
            isRunInForeground = false,
            isRunCompleted = true,
            activeChallengeId = "ch_12",
            completedChallengeIds = ChallengeCatalog.entries.map { it.challengeId }.toSet(),
            cumulativeRunTimerMs = 180_000L,
            cumulativeDeaths = 2,
            bestRunTimeMs = 180_000L,
            bestRunScore = 12_400,
            bestRunLeastDeaths = 2,
            challengeRecords = completedRecords
        ),
        syncCommit = true
    )
    val freshAfterCompleteScenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      lateinit var nextVm: GameStateViewModel
      freshAfterCompleteScenario.onActivity { nextVm = ViewModelProvider(it)[GameStateViewModel::class.java] }
      assertEquals(AppScreen.MAIN_MENU, nextVm.currentScreen.value)
      nextVm.startOrContinueContinuousRun()
      assertEquals(AppScreen.GAMEPLAY, nextVm.currentScreen.value)
      assertEquals("ch_01", nextVm.gameEngine?.activeChallengeId)
      assertEquals(12_400, repo.loadContinuousRunState().bestRunScore)
    } finally {
      freshAfterCompleteScenario.close()
    }
  }

  @Test
  fun test04_surpriseTrapsActivateImmediatelyWithoutTelegraphAndResetWithoutSoftlocks() {
    val t = World1Levels.TILE_SIZE

    // 1. ch_01: SURPRISE_SPIKE_POP, DISAPPEARING_FLOOR, FAKE_SAFE_PLATFORM all have 0f & skip TELEGRAPH
    val ch1Engine = ChaosGameEngine(
        level = ChallengeCatalog.findById("ch_01")!!.buildLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }

    val spike = ch1Engine.trollTraps.first { it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    assertEquals(TrapBehaviorCategory.SURPRISE, spike.behaviorCategory)
    assertEquals(0, spike.lockedTelegraphDurationFrames)

    ch1Engine.player.x = 17.6f * t
    ch1Engine.player.y = 13f * t - ch1Engine.player.collisionHeight
    ch1Engine.player.isGrounded = true
    ch1Engine.inputRight = true

    var enteredTelegraph = false
    for (f in 0..8) {
      ch1Engine.update()
      if (spike.state == TrollTrapState.TELEGRAPH) enteredTelegraph = true
      if (ch1Engine.isDying) break
    }
    assertFalse("Surprise spike must NOT enter TELEGRAPH pre-warning", enteredTelegraph)
    assertTrue("Surprise spike must kill player immediately on ambush", ch1Engine.isDying)
    finishDeathAnimation(ch1Engine)
    assertEquals(TrollTrapState.ARMED, spike.state)
    assertEquals(0f, spike.morphSpikeHeight, 0.01f)

    // Disappearing floor drops immediately on frame of step
    val disp = ch1Engine.trollTraps.first { it.id == 1 && it.type == TrollTrapType.DISAPPEARING_FLOOR }
    ch1Engine.player.x = disp.x + 8f
    ch1Engine.player.y = disp.y - ch1Engine.player.collisionHeight - 2f
    ch1Engine.player.prevY = ch1Engine.player.y - 3f
    ch1Engine.player.vy = 3f
    ch1Engine.inputRight = false
    ch1Engine.update()
    assertFalse("Surprise disappearing floor must vanish on contact frame", disp.isSolid)
    assertFalse(disp.isVisible)
    ch1Engine.resetLevel()
    assertTrue(disp.isSolid && disp.isVisible)
    assertEquals(TrollTrapState.ARMED, disp.state)

    // 2. ch_03: FAKE_EXIT_DOOR triggers immediately without telegraph
    val ch3Engine = ChaosGameEngine(
        level = ChallengeCatalog.findById("ch_03")!!.buildLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val fakeDoor = ch3Engine.trollTraps.first { it.type == TrollTrapType.FAKE_EXIT_DOOR }
    assertEquals(TrapBehaviorCategory.SURPRISE, fakeDoor.behaviorCategory)
    assertEquals(0, fakeDoor.lockedTelegraphDurationFrames)
    ch3Engine.player.x = fakeDoor.initialX + 12f
    ch3Engine.player.y = fakeDoor.initialY + fakeDoor.height - ch3Engine.player.collisionHeight
    ch3Engine.player.isGrounded = true
    ch3Engine.update()
    assertNotEquals(TrollTrapState.TELEGRAPH, fakeDoor.state)
    ch3Engine.update()
    assertTrue("Surprise FAKE_EXIT_DOOR must kill immediately without telegraph", ch3Engine.isDying)
    finishDeathAnimation(ch3Engine)
    assertEquals(TrollTrapState.ARMED, fakeDoor.state)

    // 3. ch_05: FAKE_CHECKPOINT triggers immediately without telegraph
    val ch5Engine = ChaosGameEngine(
        level = ChallengeCatalog.findById("ch_05")!!.buildLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val fakeBell = ch5Engine.trollTraps.first { it.type == TrollTrapType.FAKE_CHECKPOINT }
    assertEquals(TrapBehaviorCategory.SURPRISE, fakeBell.behaviorCategory)
    assertEquals(0, fakeBell.lockedTelegraphDurationFrames)
    ch5Engine.player.x = fakeBell.initialX + 8f
    ch5Engine.player.y = fakeBell.initialY + fakeBell.height - ch5Engine.player.collisionHeight
    ch5Engine.player.isGrounded = true
    ch5Engine.update()
    assertNotEquals(TrollTrapState.TELEGRAPH, fakeBell.state)
    ch5Engine.update()
    assertTrue("Surprise FAKE_CHECKPOINT must kill immediately without telegraph", ch5Engine.isDying)

    // 4. ch_09: EXIT_SLAM_CAGE locks immediately on approach, then reopens cleanly (zero softlock)
    val ch9Engine = ChaosGameEngine(
        level = ChallengeCatalog.findById("ch_09")!!.buildLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val slamCage = ch9Engine.trollTraps.first { it.type == TrollTrapType.EXIT_SLAM_CAGE }
    assertEquals(TrapBehaviorCategory.SURPRISE, slamCage.behaviorCategory)
    assertEquals(0, slamCage.lockedTelegraphDurationFrames)
    ch9Engine.player.x = (slamCage.triggerMinX + slamCage.triggerMaxX) / 2f - ch9Engine.player.collisionWidth / 2f
    ch9Engine.player.y = (slamCage.triggerMinY + slamCage.triggerMaxY) / 2f
    ch9Engine.update()
    assertEquals("Surprise EXIT_SLAM_CAGE slams immediately", TrollTrapState.ACTIVE, slamCage.state)
    repeat(slamCage.effectDurationFrames.coerceAtLeast(45) + 5) { ch9Engine.update() }
    assertEquals("EXIT_SLAM_CAGE must retire cleanly to avoid softlocks", TrollTrapState.RETIRED, slamCage.state)
    assertFalse(slamCage.isSolid)

    // 5. ch_11: BETRAYAL_SWITCH immediately fires linked SURPRISE_SPIKE_POP without telegraph & resets cleanly
    val ch11Engine = ChaosGameEngine(
        level = ChallengeCatalog.findById("ch_11")!!.buildLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val switchTrap = ch11Engine.trollTraps.first { it.id == 1 && it.type == TrollTrapType.BETRAYAL_SWITCH }
    val chainedSpike = ch11Engine.trollTraps.first { it.id == 2 && it.type == TrollTrapType.SURPRISE_SPIKE_POP }
    assertEquals(TrapBehaviorCategory.SURPRISE, switchTrap.behaviorCategory)
    assertEquals(TrapBehaviorCategory.SURPRISE, chainedSpike.behaviorCategory)
    assertEquals(0, switchTrap.lockedTelegraphDurationFrames)
    assertEquals(0, chainedSpike.lockedTelegraphDurationFrames)
    ch11Engine.player.x = switchTrap.x + 8f
    ch11Engine.player.y = switchTrap.y + switchTrap.height - ch11Engine.player.collisionHeight
    ch11Engine.player.isGrounded = true
    repeat(10) { ch11Engine.update() }
    assertEquals(TrollTrapState.ACTIVE, chainedSpike.state)
    ch11Engine.resetLevel()
    assertEquals(TrollTrapState.ARMED, switchTrap.state)
    assertEquals(TrollTrapState.ARMED, chainedSpike.state)
  }

  @Test
  fun test05_telegraphedTrapsPreserveWarningsAndAllCatalogTrapsPassPolicy() {
    for (def in ChallengeCatalog.entries) {
      val level = def.buildLevel()
      val surpriseCount = level.trollTraps.count { it.behaviorCategory == TrapBehaviorCategory.SURPRISE }
      val telegraphedCount = level.trollTraps.count { it.behaviorCategory == TrapBehaviorCategory.TELEGRAPHED }
      assertTrue("${def.challengeId} must contain intentional traps", surpriseCount + telegraphedCount > 0)

      for (tier in AdaptiveTier.entries) {
        val validations = TrapFairnessPolicy.validateChallengeTraps(def, tier)
        for (v in validations) {
          assertTrue(
              "Policy failed on ${def.challengeId} trap #${v.trapId} (${v.trapType}) @ $tier: ${v.reason}",
              v.isFair
          )
          val trap = level.trollTraps.first { it.id == v.trapId }
          if (trap.behaviorCategory == TrapBehaviorCategory.SURPRISE) {
            assertEquals(TrapFairnessClass.SURPRISE_TRAP, v.fairnessClass)
          } else if (v.fairnessClass == TrapFairnessClass.STANDARD_LETHAL_FLOOR ||
              v.fairnessClass == TrapFairnessClass.QUALIFIED_STATIC_CLUE_LETHAL) {
            assertTrue(
                "Telegraphed lethal trap #${v.trapId} in ${def.challengeId} must preserve warning > 0",
                v.effectiveTelegraphFrames > 0
            )
          }
        }
      }
    }

    // Telegraphed trap in ch_01 (FALLING_CEILING_BLOCK) still enters TELEGRAPH and extends under ASSISTED
    val ch1Engine = ChaosGameEngine(
        level = ChallengeCatalog.findById("ch_01")!!.buildLevel(),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }
    val ceiling = ch1Engine.trollTraps.first { it.type == TrollTrapType.FALLING_CEILING_BLOCK }
    assertEquals(TrapBehaviorCategory.TELEGRAPHED, ceiling.behaviorCategory)
    val stdTel = TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ceiling, AdaptiveTier.STANDARD)
    val astTel = TrapFairnessPolicy.resolveEffectiveTelegraphFrames(ceiling, AdaptiveTier.ASSISTED)
    assertTrue("Telegraphed ceiling trap must extend under ASSISTED ($astTel > $stdTel)", astTel > stdTel)

    ch1Engine.player.x = (ceiling.triggerMinX + ceiling.triggerMaxX) / 2f
    ch1Engine.player.y = 13f * World1Levels.TILE_SIZE - ch1Engine.player.collisionHeight
    ch1Engine.player.isGrounded = true
    ch1Engine.update()
    assertEquals("Telegraphed trap must enter TELEGRAPH before slamming", TrollTrapState.TELEGRAPH, ceiling.state)
  }

  @Test
  fun test06_processDeathRecoveryAndSettingsReplayGuideRemainFunctional() {
    val repo = SaveRepository(app)
    repo.saveContinuousRunState(
        buildPersistedState(
            isRunActive = true,
            isRunInForeground = true,
            isRunCompleted = false,
            activeChallengeId = "ch_04",
            completedChallengeIds = setOf("ch_01", "ch_02", "ch_03"),
            cumulativeRunTimerMs = 31_000L,
            cumulativeDeaths = 2
        ),
        syncCommit = true
    )

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      lateinit var recoveredVm: GameStateViewModel
      scenario.onActivity { recoveredVm = ViewModelProvider(it)[GameStateViewModel::class.java] }
      assertEquals(AppScreen.GAMEPLAY, recoveredVm.currentScreen.value)
      assertEquals("ch_04", recoveredVm.gameEngine?.activeChallengeId)
    } finally {
      scenario.close()
    }

    // Verify Settings opens cleanly and contains only audio/vibration controls (no onboarding tour clutter)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_music").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_sfx").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_vibration").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("button_replay_onboarding").fetchSemanticsNodes().size)
  }
}

private fun assertNotEquals(unexpected: Any?, actual: Any?) {
  org.junit.Assert.assertNotEquals(unexpected, actual)
}
