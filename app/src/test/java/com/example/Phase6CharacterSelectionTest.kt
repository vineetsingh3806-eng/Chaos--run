package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ProceduralAudioEngine
import com.example.data.AaravState
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ContinuousRunPersistedState
import com.example.data.PlayableCharacter
import com.example.data.Player
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.StoryClueId
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.engine.PlayerCharacterRenderer
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
class Phase6CharacterSelectionTest {

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
  fun test01_fourSelectableCharactersAndAaravIsDefault() {
    assertEquals(4, PlayableCharacter.entries.size)
    assertEquals(PlayableCharacter.AARAV, PlayableCharacter.DEFAULT)
    assertEquals(
        listOf("aarav", "golu", "vega", "roko"),
        PlayableCharacter.entries.map { it.id }
    )
    assertEquals(4, PlayableCharacter.entries.map { it.displayName }.toSet().size)
    assertEquals(4, PlayableCharacter.entries.map { it.description }.toSet().size)

    val repo = SaveRepository(app)
    assertEquals(PlayableCharacter.AARAV, repo.getSelectedCharacter())

    val vm = GameStateViewModel(app)
    assertEquals(PlayableCharacter.AARAV, vm.selectedCharacter.value)
  }

  @Test
  fun test02_characterSelectionPersistsAcrossRestartAndCorruptIdsFallbackToAarav() {
    val vm1 = GameStateViewModel(app)
    vm1.selectCharacter(PlayableCharacter.VEGA, syncCommit = true)
    assertEquals(PlayableCharacter.VEGA, vm1.selectedCharacter.value)
    assertEquals(PlayableCharacter.VEGA, SaveRepository(app).getSelectedCharacter())

    // Simulated cold restart
    val vm2 = GameStateViewModel(app)
    assertEquals(PlayableCharacter.VEGA, vm2.selectedCharacter.value)

    // Unknown or corrupted character ID falls back safely to Aarav
    assertEquals(PlayableCharacter.AARAV, PlayableCharacter.fromId(null))
    assertEquals(PlayableCharacter.AARAV, PlayableCharacter.fromId(""))
    assertEquals(PlayableCharacter.AARAV, PlayableCharacter.fromId("unknown_hero_99"))

    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit().putString("pref_selected_character_id", "corrupt_character_!@#").commit()
    assertEquals(PlayableCharacter.AARAV, SaveRepository(app).getSelectedCharacter())
    assertEquals(PlayableCharacter.AARAV, GameStateViewModel(app).selectedCharacter.value)

    // Wrong SharedPreferences type (Int instead of String) falls back safely without crashing
    prefs.edit().putInt("pref_selected_character_id", 12345).commit()
    assertEquals(PlayableCharacter.AARAV, SaveRepository(app).getSelectedCharacter())
  }

  @Test
  fun test03_existingSaveDataCompatibilityAndProcessDeathRecoveryPreserveSelectedCharacter() {
    // Seed legacy v1 save data + continuous run interrupted in foreground + ROKO selected
    val prefs = app.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("stars_1_1", 3)
        .putLong("best_time_1_1", 19800L)
        .putInt("best_score_1_1", 1450)
        .commit()

    val repo = SaveRepository(app)
    repo.saveClueDiscovered(StoryClueId.ASH_PRINTS)
    repo.setSelectedCharacter(PlayableCharacter.ROKO, syncCommit = true)

    val ch03 = ChallengeCatalog.createLockedConfig("ch_03", AdaptiveTier.STANDARD)!!
    repo.saveContinuousRunState(
        ContinuousRunPersistedState(
            schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
            isRunActive = true,
            isRunInForeground = true,
            isRunCompleted = false,
            activeChallengeId = "ch_03",
            activeChallengeSeed = ch03.challengeSeed,
            activeConfigVersion = ch03.configVersion,
            activeAdaptiveTier = AdaptiveTier.STANDARD,
            currentChallengeDeaths = 2,
            completedChallengeIds = setOf("ch_01", "ch_02"),
            challengeRecords = emptyMap(),
            cumulativeRunTimerMs = 41_200L,
            cumulativeDeaths = 3,
            cumulativeScore = 900,
            bestRunTimeMs = 0L,
            bestRunScore = 0,
            bestRunLeastDeaths = -1
        ),
        syncCommit = true
    )

    // Process-death recovery ViewModel boot restores ch_03 AND preserves ROKO + legacy data
    val recoveredVm = GameStateViewModel(app)
    assertEquals(AppScreen.GAMEPLAY, recoveredVm.currentScreen.value)
    assertEquals(PlayableCharacter.ROKO, recoveredVm.selectedCharacter.value)
    assertEquals(PlayableCharacter.ROKO, recoveredVm.gameRenderer?.selectedCharacter)
    assertEquals("ch_03", recoveredVm.gameEngine?.activeChallengeId)
    assertEquals(3, repo.getStars(1, 1))
    assertEquals(19800L, repo.getBestTime(1, 1))
    assertTrue(repo.isClueDiscovered(StoryClueId.ASH_PRINTS))
  }

  @Test
  fun test04_continuousProgressionAndReplayPreserveSelectedCharacter() {
    val vm = GameStateViewModel(app)
    vm.selectCharacter(PlayableCharacter.GOLU, syncCommit = true)

    // Start continuous progression
    vm.startNewContinuousRun()
    assertEquals(PlayableCharacter.GOLU, vm.gameRenderer?.selectedCharacter)

    // Advance / retry current challenge preserves GOLU
    vm.retryCurrentChallenge()
    assertEquals(PlayableCharacter.GOLU, vm.gameRenderer?.selectedCharacter)

    // Simulate completing challenge 1 to unlock/advance to challenge 2 while keeping GOLU
    val engine = vm.gameEngine!!
    val ch01 = ChallengeCatalog.createLockedConfig("ch_01")!!
    vm.continuousCoordinator.onChallengeCompleted(
        challengeId = ch01.challengeId,
        attemptToken = engine.challengeAttemptToken,
        timeMs = 15_000L,
        score = 600,
        coinsCollected = 3,
        totalCoins = 5,
        parTimeSeconds = 40,
        isTrollLevel = true,
        challengeDeaths = 0,
        engineCumulativeRunTimerMs = 15_000L,
        engineCumulativeDeaths = 0
    )

    // Launch Replay on ch_01 -> GOLU remains active
    vm.replayCompletedChallenge("ch_01")
    assertEquals(RunLaunchMode.REPLAY, vm.gameEngine?.launchMode)
    assertEquals(PlayableCharacter.GOLU, vm.gameRenderer?.selectedCharacter)
  }

  @Test
  fun test05_allFourCharactersRenderAllStatesWithDistinctSilhouettes() {
    val renderer = PlayerCharacterRenderer()
    val gameRenderer = ChaosGameRenderer(app)

    val signatureHashes = mutableSetOf<Int>()
    for (character in PlayableCharacter.entries) {
      gameRenderer.selectedCharacter = character
      val bmp = Bitmap.createBitmap(120, 140, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)

      // Render every gameplay animation state cleanly without exception
      for (state in AaravState.entries) {
        bmp.eraseColor(0)
        renderer.drawPreview(
            canvas = canvas,
            character = character,
            width = 120f,
            height = 140f,
            animTick = 12,
            state = state,
            facingRight = true
        )
        assertTrue(
            "Character ${character.displayName} in state $state must draw visible pixels",
            countOpaquePixels(bmp) > 80
        )

        // Also exercise full gameplay draw path (facing right and left)
        val p = Player(x = 100f, y = 200f, facingRight = (state.ordinal % 2 == 0), state = state, stateTimerFrames = 10)
        renderer.drawCharacter(canvas, character, p, cameraX = 40f, cameraY = 120f)
      }

      // Compute silhouette/color signature on IDLE preview to prove genuinely distinct character art
      bmp.eraseColor(0)
      renderer.drawPreview(canvas, character, 120f, 140f, animTick = 6, state = AaravState.IDLE)
      signatureHashes.add(computePixelSignature(bmp))
    }

    assertEquals(
        "All 4 characters must produce genuinely distinct rendered silhouettes/art",
        4,
        signatureHashes.size
    )
    gameRenderer.expansionRenderer.release()
  }

  @Test
  fun test06_identicalPhysicsConstantsHitboxAndDeterministicTrajectoryAcrossCharacters() {
    assertEquals(0.58f, ChaosGameEngine.GRAVITY, 0.0001f)
    assertEquals(-13.2f, ChaosGameEngine.JUMP_IMPULSE, 0.0001f)
    assertEquals(6.5f, ChaosGameEngine.MAX_RUN_SPEED, 0.0001f)
    assertEquals(5, ChaosGameEngine.COYOTE_FRAMES)
    assertEquals(6, ChaosGameEngine.JUMP_BUFFER_FRAMES)

    val trajectories = PlayableCharacter.entries.map { character ->
      val vm = GameStateViewModel(app)
      vm.selectCharacter(character, syncCommit = true)
      vm.startLevel(1, enforceUnlock = false)
      val engine = vm.gameEngine!!
      val renderer = vm.gameRenderer!!
      assertEquals(character, renderer.selectedCharacter)
      assertEquals(32f, engine.player.collisionWidth, 0.001f)
      assertEquals(54f, engine.player.collisionHeight, 0.001f)

      // Run identical deterministic inputs (run right + jump)
      engine.inputRight = true
      repeat(10) { engine.update() }
      engine.inputJump = true
      repeat(20) { engine.update() }
      engine.inputJump = false
      repeat(10) { engine.update() }

      Triple(engine.player.x, engine.player.y, engine.player.vy)
    }

    val baseline = trajectories.first()
    for (other in trajectories.drop(1)) {
      assertEquals(baseline.first, other.first, 0.0001f)
      assertEquals(baseline.second, other.second, 0.0001f)
      assertEquals(baseline.third, other.third, 0.0001f)
    }
  }

  @Test
  fun test07_settingsOnlyCharacterSelectionUiDisplaysAllFourAndNotInGameplayOrPause() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.completeOnboarding(syncCommit = true)
    }
    composeTestRule.waitForIdle()

    // Must NOT appear on Main Menu
    assertEquals(0, composeTestRule.onAllNodesWithTag("section_character_selection").fetchSemanticsNodes().size)

    // Open Settings -> all 4 characters, previews, names, descriptions, and Select/Selected are visible
    composeTestRule.onNodeWithTag("button_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("section_character_selection").assertIsDisplayed()

    for (character in PlayableCharacter.entries) {
      composeTestRule.onNodeWithTag("character_card_${character.id}").assertIsDisplayed()
      val previewNode = composeTestRule.onNodeWithTag("character_preview_${character.id}")
      previewNode.assertIsDisplayed()
      val previewBounds = previewNode.fetchSemanticsNode().boundsInRoot
      assertTrue(
          "Preview for ${character.id} must have non-zero size, was $previewBounds",
          previewBounds.width > 0f && previewBounds.height > 0f
      )
      composeTestRule.onNodeWithText(character.displayName).assertIsDisplayed()
      composeTestRule.onNodeWithText(character.description).assertIsDisplayed()
    }
    composeTestRule.onNodeWithTag("character_card_aarav").assertIsSelected()

    // Select Roko via UI click -> updates selected state & persists across Save & Return
    composeTestRule.onNodeWithTag("character_card_roko").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("character_card_roko").assertIsSelected()
    assertEquals(PlayableCharacter.ROKO, vm.selectedCharacter.value)

    // Audio & Vibration controls + pinned footer stay visible without clipping alongside Character Selection
    composeTestRule.onNodeWithTag("switch_music").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_sfx").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_vibration").assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_settings_save").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(PlayableCharacter.ROKO, SaveRepository(app).getSelectedCharacter())

    // Enter Gameplay & Pause -> Character Selection must NOT appear on Gameplay HUD or Pause dialog
    composeTestRule.onNodeWithTag("button_continuous_run").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("screen_gameplay").assertIsDisplayed()
    assertEquals(PlayableCharacter.ROKO, vm.gameRenderer?.selectedCharacter)
    assertEquals(0, composeTestRule.onAllNodesWithTag("section_character_selection").fetchSemanticsNodes().size)

    composeTestRule.onNodeWithTag("button_pause").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_pause").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("section_character_selection").fetchSemanticsNodes().size)
  }

  private fun countOpaquePixels(bmp: Bitmap): Int {
    var count = 0
    for (y in 0 until bmp.height) {
      for (x in 0 until bmp.width) {
        if ((bmp.getPixel(x, y) ushr 24) > 20) {
          count++
        }
      }
    }
    return count
  }

  private fun computePixelSignature(bmp: Bitmap): Int {
    var hash = 17
    for (y in 0 until bmp.height step 3) {
      for (x in 0 until bmp.width step 3) {
        hash = 31 * hash + bmp.getPixel(x, y)
      }
    }
    return hash
  }
}
