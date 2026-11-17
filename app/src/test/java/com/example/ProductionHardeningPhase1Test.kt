package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioFocusController
import com.example.audio.AudioLifecycleState
import com.example.audio.AudioStreamHandle
import com.example.audio.AudioTrackBackend
import com.example.audio.ProceduralAudioEngine
import com.example.data.TileType
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProductionHardeningPhase1Test {

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

  private class FakeAudioStreamHandle : AudioStreamHandle {
    val playCalls = AtomicInteger(0)
    val pauseCalls = AtomicInteger(0)
    val stopCalls = AtomicInteger(0)
    val releaseCalls = AtomicInteger(0)
    val writes = AtomicInteger(0)
    val isPlaying = AtomicBoolean(false)
    val isStopped = AtomicBoolean(false)
    val isReleased = AtomicBoolean(false)

    override fun play() {
      isStopped.set(false)
      isPlaying.set(true)
      playCalls.incrementAndGet()
    }

    override fun pause() {
      isPlaying.set(false)
      pauseCalls.incrementAndGet()
    }

    override fun stop() {
      isPlaying.set(false)
      isStopped.set(true)
      stopCalls.incrementAndGet()
    }

    override fun release() {
      isPlaying.set(false)
      isStopped.set(true)
      isReleased.set(true)
      releaseCalls.incrementAndGet()
    }

    override fun writeBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
      if (isStopped.get() || isReleased.get()) return -1
      writes.incrementAndGet()
      try {
        Thread.sleep(6L)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        return -1
      }
      return if (isStopped.get() || isReleased.get()) -1 else sizeInShorts
    }

    override fun writeNonBlocking(buffer: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
      if (isStopped.get() || isReleased.get() || !isPlaying.get()) return -1
      writes.incrementAndGet()
      return sizeInShorts
    }
  }

  private class FakeAudioTrackBackend : AudioTrackBackend {
    val musicTracks = mutableListOf<FakeAudioStreamHandle>()
    val sfxTracks = mutableListOf<FakeAudioStreamHandle>()

    @Synchronized
    override fun createMusicTrack(sampleRate: Int): AudioStreamHandle {
      return FakeAudioStreamHandle().also { musicTracks.add(it) }
    }

    @Synchronized
    override fun createSfxTrack(sampleRate: Int): AudioStreamHandle {
      return FakeAudioStreamHandle().also { sfxTracks.add(it) }
    }
  }

  private class FakeAudioFocusController : AudioFocusController {
    val requests = AtomicInteger(0)
    val abandons = AtomicInteger(0)
    var grantNext = true

    override fun requestFocus(): Boolean {
      requests.incrementAndGet()
      return grantNext
    }

    override fun abandonFocus() {
      abandons.incrementAndGet()
    }
  }

  @Test
  fun test01_audioEnginePreventsConcurrentThreadsLeaksAndDoubleRelease() {
    val backend = FakeAudioTrackBackend()
    val focus = FakeAudioFocusController()
    val engine = ProceduralAudioEngine(
        trackBackend = backend,
        context = null,
        focusControllerOverride = focus
    )

    assertEquals(AudioLifecycleState.IDLE, engine.currentLifecycleState)
    assertEquals(1, backend.sfxTracks.size)
    val sfx = backend.sfxTracks.single()
    assertTrue(sfx.isPlaying.get())

    // Repeated startMusic calls must reuse existing active thread & track without leaking
    engine.startMusic()
    engine.startMusic()
    engine.startMusic()
    assertEquals(AudioLifecycleState.PLAYING, engine.currentLifecycleState)
    assertEquals(1, backend.musicTracks.size)
    assertTrue(engine.isMusicThreadAlive())

    // Wait briefly so music thread produces at least one frame
    Thread.sleep(30L)
    assertTrue(backend.musicTracks[0].writes.get() > 0)

    // Repeated Home -> Return (pause / resume) cycles pause & resume BOTH music and SFX without recreating tracks
    repeat(3) {
      engine.pause()
      assertEquals(AudioLifecycleState.PAUSED, engine.currentLifecycleState)
      assertFalse(backend.musicTracks[0].isPlaying.get())
      assertFalse(sfx.isPlaying.get())

      val sfxWritesBeforeBg = sfx.writes.get()
      engine.playJump()
      assertEquals("SFX must not play while backgrounded/paused", sfxWritesBeforeBg, sfx.writes.get())

      engine.resume()
      assertEquals(AudioLifecycleState.PLAYING, engine.currentLifecycleState)
      assertTrue(backend.musicTracks[0].isPlaying.get())
      assertTrue(sfx.isPlaying.get())
    }
    assertEquals(1, backend.musicTracks.size)

    // SFX works again when resumed
    val sfxWritesBeforeResumeJump = sfx.writes.get()
    engine.playJump()
    assertTrue(sfx.writes.get() > sfxWritesBeforeResumeJump)

    // Stop -> Start cycle cleanly joins & releases old music track before creating new one
    engine.stop()
    assertEquals(AudioLifecycleState.STOPPED, engine.currentLifecycleState)
    assertFalse(engine.isMusicThreadAlive())
    assertFalse(engine.hasActiveMusicTrack())
    assertEquals(1, backend.musicTracks[0].releaseCalls.get())

    engine.startMusic()
    assertEquals(AudioLifecycleState.PLAYING, engine.currentLifecycleState)
    assertEquals(2, backend.musicTracks.size)
    assertTrue(engine.isMusicThreadAlive())

    // AudioFocus interruptions pause & resume cleanly
    engine.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
    assertFalse(backend.musicTracks[1].isPlaying.get())
    assertFalse(sfx.isPlaying.get())
    engine.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
    assertTrue(backend.musicTracks[1].isPlaying.get())
    assertTrue(sfx.isPlaying.get())

    // Release is strictly idempotent: releases each track exactly once and blocks subsequent starts
    engine.release()
    engine.release()
    engine.stop()
    engine.startMusic()
    engine.resume()

    assertEquals(AudioLifecycleState.RELEASED, engine.currentLifecycleState)
    assertFalse(engine.isMusicThreadAlive())
    assertFalse(engine.hasActiveMusicTrack())
    assertFalse(engine.hasActiveSfxTrack())
    assertEquals(1, backend.musicTracks[0].releaseCalls.get())
    assertEquals(1, backend.musicTracks[1].releaseCalls.get())
    assertEquals(1, sfx.releaseCalls.get())
  }

  @Test
  fun test02_activityRecreationPreservesViewModelAudioUntilFinalViewModelClear() {
    lateinit var vmBefore: GameStateViewModel
    lateinit var audioRef: ProceduralAudioEngine

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      scenario.onActivity { activity ->
        vmBefore = ViewModelProvider(activity)[GameStateViewModel::class.java]
        audioRef = vmBefore.audioEngine
        assertNotEqualsOrReleased(audioRef)
        assertTrue(audioRef.hasActiveSfxTrack())
      }

      // Config change recreation: MainActivity.onDestroy() runs, ViewModel survives
      scenario.recreate()

      scenario.onActivity { activity ->
        val vmAfter = ViewModelProvider(activity)[GameStateViewModel::class.java]
        assertTrue("ViewModel must survive Activity recreation", vmBefore === vmAfter)
        assertTrue(
            "AudioEngine must remain alive across Activity recreation",
            vmAfter.audioEngine.currentLifecycleState != AudioLifecycleState.RELEASED
        )
        assertTrue(vmAfter.audioEngine.hasActiveSfxTrack())
      }
    } finally {
      // Closing scenario destroys Activity permanently and triggers ViewModelStore.clear() -> onCleared()
      scenario.close()
    }

    assertEquals(AudioLifecycleState.RELEASED, audioRef.currentLifecycleState)
    assertFalse(audioRef.hasActiveMusicTrack())
    assertFalse(audioRef.hasActiveSfxTrack())
  }

  private fun assertNotEqualsOrReleased(engine: ProceduralAudioEngine) {
    assertTrue(engine.currentLifecycleState != AudioLifecycleState.RELEASED)
  }

  @Test
  fun test03_gameplayBackHandlerPauseResumeVictoryAndModals() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startLevel(levelId = 1, enforceUnlock = false)
    }
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertFalse(vm.isUserPaused.value)

    // 1. Back during active gameplay opens pause dialog
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    assertTrue("Back during gameplay must pause", vm.isUserPaused.value)
    composeTestRule.onNodeWithTag("dialog_pause").assertIsDisplayed()

    // 2. Back while paused closes pause dialog & resumes (Activity NOT finishing)
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
      assertFalse("Back while paused must not finish MainActivity", activity.isFinishing)
    }
    composeTestRule.waitForIdle()
    assertFalse("Back while paused must resume gameplay", vm.isUserPaused.value)
    assertEquals(0, composeTestRule.onAllNodesWithTag("dialog_pause").fetchSemanticsNodes().size)

    // 3. Back during Field Notes from gameplay closes Field Notes cleanly
    composeTestRule.activityRule.scenario.onActivity {
      vm.openFieldNotes()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("modal_field_notes").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    assertFalse(vm.isFieldNotesOpen.value)
    assertEquals(AppScreen.GAMEPLAY, vm.currentScreen.value)
    assertFalse(vm.isUserPaused.value)

    // 4. Back during Victory NEVER leaves invisible pause dialog; returns to Main Menu
    val engine = vm.gameEngine!!
    val gate = engine.tiles.first { it.type == TileType.SUN_GATE }
    engine.player.x = gate.x + 8f
    engine.player.y = gate.y + 8f
    engine.update() // Triggers RELOCATING_EXIT_DOOR hop on 1-1
    engine.player.x = gate.x + 8f
    engine.player.y = gate.y + 8f
    engine.update()
    composeTestRule.waitForIdle()
    assertNotNull("VictoryResult should be active", vm.victoryResult.value)
    composeTestRule.onNodeWithTag("dialog_victory").assertIsDisplayed()

    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
      assertFalse(activity.isFinishing)
    }
    composeTestRule.waitForIdle()
    assertEquals(AppScreen.MAIN_MENU, vm.currentScreen.value)
    assertNull("Victory result must clear when Back exits to Main Menu", vm.victoryResult.value)
    assertEquals(0, composeTestRule.onAllNodesWithTag("dialog_pause").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("dialog_victory").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithTag("screen_main_menu").assertIsDisplayed()

    // 5. Back closes Chapter Trail modal on Main Menu
    composeTestRule.onNodeWithTag("button_chapter_trail").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("modal_chapter_trail").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    assertFalse(vm.isChapterTrailOpen.value)
  }

  @Test
  fun test04_customLauncherIconConfiguredAndRendersWithoutTemplateWebps() {
    // 1. Verify no template webp icons remain across all mipmap density buckets
    val resDir = File("src/main/res").takeIf { it.exists() } ?: File("app/src/main/res")
    listOf("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi").forEach { dirName ->
      val dir = File(resDir, dirName)
      assertTrue("$dirName must exist", dir.isDirectory)
      assertTrue("$dirName/ic_launcher.png must exist", File(dir, "ic_launcher.png").length() > 100L)
      assertTrue("$dirName/ic_launcher_round.png must exist", File(dir, "ic_launcher_round.png").length() > 100L)
      assertFalse("$dirName/ic_launcher.webp must be removed", File(dir, "ic_launcher.webp").exists())
      assertFalse("$dirName/ic_launcher_round.webp must be removed", File(dir, "ic_launcher_round.webp").exists())
    }

    // 2. Render both launcher & round launcher drawables into bitmaps via Android Resources
    for (resId in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
      val drawable = ContextCompat.getDrawable(app, resId)
      assertNotNull("Launcher icon drawable must resolve", drawable)
      val bmp = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      drawable!!.setBounds(0, 0, 108, 108)
      drawable.draw(canvas)
      var nonZeroPixels = 0
      for (y in 0 until 108) {
        for (x in 0 until 108) {
          if (bmp.getPixel(x, y) != 0) nonZeroPixels++
        }
      }
      assertTrue("Launcher icon bitmap must render non-empty pixels", nonZeroPixels > 5000)
    }
  }

  @Test
  fun test05_dedicatedRendererPerSessionAndCachedLightmapResizeRendersCleanly() {
    lateinit var vm: GameStateViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      vm = ViewModelProvider(activity)[GameStateViewModel::class.java]
      vm.startLevel(levelId = 1, enforceUnlock = false)
    }
    composeTestRule.waitForIdle()
    val firstRenderer = vm.gameRenderer!!

    composeTestRule.activityRule.scenario.onActivity {
      vm.startLevel(levelId = 2, enforceUnlock = false)
    }
    composeTestRule.waitForIdle()
    val secondRenderer = vm.gameRenderer!!
    assertNotSame("Each GameSession must own an independent ChaosGameRenderer", firstRenderer, secondRenderer)

    val level2Engine = ChaosGameEngine(
        level = World1Levels.createLevel(2),
        audioEngine = ProceduralAudioEngine()
    ) { _, _, _ -> }

    // Render Level 1-2 darkness lantern overlay across two viewport sizes (exercises cached 1/8 lightmap)
    val bmpSmall = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
    secondRenderer.render(Canvas(bmpSmall), level2Engine, 800f, 480f)

    val bmpLarge = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    secondRenderer.render(Canvas(bmpLarge), level2Engine, 1280f, 720f)
    secondRenderer.expansionRenderer.release()

    var nonBlackPixels = 0
    for (y in 0 until 720 step 8) {
      for (x in 0 until 1280 step 8) {
        if ((bmpLarge.getPixel(x, y) ushr 24) > 0) nonBlackPixels++
      }
    }
    assertTrue("Level 1-2 lightmap render must produce populated frame", nonBlackPixels > 100)
  }
}
