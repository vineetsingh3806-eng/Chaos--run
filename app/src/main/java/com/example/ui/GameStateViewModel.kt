package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.audio.ProceduralAudioEngine
import com.example.data.DifficultyTier
import com.example.data.SaveRepository
import com.example.data.StoryClueId
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.ui.input.MultiTouchControlState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen {
  MAIN_MENU,
  GAMEPLAY,
  HOW_TO_PLAY,
  SETTINGS
}

data class GameSession(
    val sessionId: Long,
    val engine: ChaosGameEngine,
    val renderer: ChaosGameRenderer
)

data class LevelProgressUi(
    val levelId: Int,
    val title: String,
    val subtitle: String,
    val mechanicTag: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val stars: Int,
    val bestTimeMs: Long,
    val bestScore: Int,
    val relicsFound: Int,
    val totalRelics: Int
)

data class VictoryResult(
    val timeMs: Long,
    val coinsCollected: Int,
    val totalCoins: Int,
    val stars: Int,
    val cluesDiscoveredCount: Int,
    val totalClues: Int = StoryClueId.entries.size,
    val resonanceStoneFound: Boolean = false,
    val score: Int = 0,
    val diyasLit: Int = 0,
    val totalDiyas: Int = 0,
    val difficulty: DifficultyTier = DifficultyTier.NORMAL,
    val levelId: Int = 1,
    val levelTitle: String = "Whispering Woods",
    val nextLevelUnlocked: Boolean = false,
    val relicsFoundCount: Int = 0,
    val totalRelics: Int = 0,
    val deathsCount: Int = 0,
    val isTrollLevel: Boolean = false
)

class GameStateViewModel(
    application: Application,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
  constructor(application: Application) : this(application, SavedStateHandle())

  val saveRepository = SaveRepository(application)
  val audioEngine = ProceduralAudioEngine(context = application)
  val touchControlState = MultiTouchControlState()

  private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _selectedDifficulty = MutableStateFlow(saveRepository.getSelectedDifficulty())
  val selectedDifficulty: StateFlow<DifficultyTier> = _selectedDifficulty.asStateFlow()

  private val _activeLevelId = MutableStateFlow(1)
  val activeLevelId: StateFlow<Int> = _activeLevelId.asStateFlow()

  private val _gameSession = MutableStateFlow<GameSession?>(null)
  val gameSession: StateFlow<GameSession?> = _gameSession.asStateFlow()

  private val _victoryResult = MutableStateFlow<VictoryResult?>(null)
  val victoryResult: StateFlow<VictoryResult?> = _victoryResult.asStateFlow()

  private val _isUserPaused = MutableStateFlow(false)
  val isUserPaused: StateFlow<Boolean> = _isUserPaused.asStateFlow()

  private val _isHostPaused = MutableStateFlow(false)
  val isHostPaused: StateFlow<Boolean> = _isHostPaused.asStateFlow()

  private val _isWindowFocusLost = MutableStateFlow(false)
  val isWindowFocusLost: StateFlow<Boolean> = _isWindowFocusLost.asStateFlow()

  private val _isEffectivePaused = MutableStateFlow(false)
  val isEffectivePaused: StateFlow<Boolean> = _isEffectivePaused.asStateFlow()

  private val _isMusicMuted = MutableStateFlow(!saveRepository.isMusicEnabled())
  val isMusicMuted: StateFlow<Boolean> = _isMusicMuted.asStateFlow()

  private val _discoveredClues = MutableStateFlow(saveRepository.getDiscoveredClues())
  val discoveredClues: StateFlow<Set<StoryClueId>> = _discoveredClues.asStateFlow()

  private val _isResonanceDiscovered = MutableStateFlow(saveRepository.isResonanceStoneDiscovered())
  val isResonanceDiscovered: StateFlow<Boolean> = _isResonanceDiscovered.asStateFlow()

  private val _bestTimeMs = MutableStateFlow(saveRepository.getBestTime(1, 1))
  val bestTimeMs: StateFlow<Long> = _bestTimeMs.asStateFlow()

  private val _bestStars = MutableStateFlow(saveRepository.getStars(1, 1))
  val bestStars: StateFlow<Int> = _bestStars.asStateFlow()

  private val _bestScore = MutableStateFlow(saveRepository.getBestScore(1, 1))
  val bestScore: StateFlow<Int> = _bestScore.asStateFlow()

  private val _levelsProgress = MutableStateFlow(buildLevelsProgress())
  val levelsProgress: StateFlow<List<LevelProgressUi>> = _levelsProgress.asStateFlow()

  private val _isFieldNotesOpen = MutableStateFlow(false)
  val isFieldNotesOpen: StateFlow<Boolean> = _isFieldNotesOpen.asStateFlow()

  private val _isChapterTrailOpen = MutableStateFlow(false)
  val isChapterTrailOpen: StateFlow<Boolean> = _isChapterTrailOpen.asStateFlow()

  private var nextSessionId = 0L

  val gameEngine: ChaosGameEngine?
    get() = _gameSession.value?.engine

  val gameRenderer: ChaosGameRenderer?
    get() = _gameSession.value?.renderer

  init {
    audioEngine.isMusicEnabled = saveRepository.isMusicEnabled()
    audioEngine.isSfxEnabled = saveRepository.isSfxEnabled()
    audioEngine.musicVolume = saveRepository.getMusicVolume()
    audioEngine.sfxVolume = saveRepository.getSfxVolume()
    audioEngine.startMusic()
  }

  private fun buildLevelsProgress(): List<LevelProgressUi> {
    return World1Levels.catalog.map { meta ->
      val relicIds = World1Levels.relicIdsForLevel(meta.levelId)
      val foundCount = saveRepository.getDiscoveredRelicIds(relicIds).size
      LevelProgressUi(
          levelId = meta.levelId,
          title = meta.title,
          subtitle = meta.subtitle,
          mechanicTag = meta.mechanicTag,
          isUnlocked = saveRepository.isLevelUnlocked(1, meta.levelId),
          isCompleted = saveRepository.isLevelCompleted(1, meta.levelId),
          stars = saveRepository.getStars(1, meta.levelId),
          bestTimeMs = saveRepository.getBestTime(1, meta.levelId),
          bestScore = saveRepository.getBestScore(1, meta.levelId),
          relicsFound = foundCount,
          totalRelics = relicIds.size
      )
    }
  }

  fun setDifficulty(tier: DifficultyTier) {
    _selectedDifficulty.value = tier
    saveRepository.setSelectedDifficulty(tier)
  }

  fun startLevel1(difficulty: DifficultyTier = _selectedDifficulty.value) {
    startLevel(levelId = 1, difficulty = difficulty)
  }

  fun startLevel(
      levelId: Int,
      difficulty: DifficultyTier = _selectedDifficulty.value,
      enforceUnlock: Boolean = true
  ) {
    if (enforceUnlock && !saveRepository.isLevelUnlocked(1, levelId)) {
      return
    }

    touchControlState.clearAll()
    _isUserPaused.value = false
    _isHostPaused.value = false
    _isWindowFocusLost.value = false
    _victoryResult.value = null
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _activeLevelId.value = levelId

    val levelDef = World1Levels.createLevel(levelId, difficulty)
    val initialRelics = saveRepository.getDiscoveredRelicIds(levelDef.relics.map { it.id })
    val renderer = ChaosGameRenderer(getApplication())

    lateinit var newEngine: ChaosGameEngine
    newEngine = ChaosGameEngine(
        level = levelDef,
        audioEngine = audioEngine,
        initialDiscoveredClues = saveRepository.getDiscoveredClues(),
        initialResonanceDiscovered = saveRepository.isResonanceStoneDiscovered(),
        onClueDiscovered = { clueId ->
          saveRepository.saveClueDiscovered(clueId)
          _discoveredClues.value = saveRepository.getDiscoveredClues()
        },
        onResonanceDiscovered = {
          saveRepository.saveResonanceStoneDiscovered()
          _isResonanceDiscovered.value = true
        },
        initialDiscoveredRelics = initialRelics,
        onRelicDiscovered = { relic ->
          saveRepository.saveRelicDiscovered(relic.id)
          refreshSavedProgress()
        },
        onLevelCompleted = { timeMs, coinsCollected, totalCoins ->
          if (_gameSession.value?.engine !== newEngine) return@ChaosGameEngine
          touchControlState.clearAll()
          val finalScore = newEngine.score
          val litDiyas = newEngine.diyas.count { it.isLit }
          val totalDiyas = newEngine.diyas.size
          val stars = if (levelDef.isTrollLevel) {
            val ratio = coinsCollected.toFloat() / totalCoins.coerceAtLeast(1)
            val deaths = newEngine.deathCount
            if (ratio >= 0.5f && timeMs < levelDef.parTimeSeconds * 1000L && deaths <= 3) 3
            else if (ratio >= 0.45f || deaths <= 5) 2
            else 1
          } else if (levelId == 1) {
            if (coinsCollected >= 9 && timeMs < 45000L) 3 else if (coinsCollected >= 5) 2 else 1
          } else {
            val ratio = coinsCollected.toFloat() / totalCoins.coerceAtLeast(1)
            if (ratio >= 0.8f && timeMs < levelDef.parTimeSeconds * 1000L) 3
            else if (ratio >= 0.45f) 2
            else 1
          }
          saveRepository.saveBestTime(1, levelId, timeMs)
          saveRepository.saveStars(1, levelId, stars)
          saveRepository.saveBestScore(1, levelId, finalScore)
          refreshSavedProgress()
          val clueCount = saveRepository.getDiscoveredClueCount()
          val stoneFound = saveRepository.isResonanceStoneDiscovered()
          val relicIds = levelDef.relics.map { it.id }
          val relicsFoundCount = saveRepository.getDiscoveredRelicIds(relicIds).size
          val nextUnlocked = levelId < 5 && saveRepository.isLevelUnlocked(1, levelId + 1)
          _victoryResult.value = VictoryResult(
              timeMs = timeMs,
              coinsCollected = coinsCollected,
              totalCoins = totalCoins,
              stars = stars,
              cluesDiscoveredCount = clueCount,
              totalClues = StoryClueId.entries.size,
              resonanceStoneFound = stoneFound,
              score = finalScore,
              diyasLit = litDiyas,
              totalDiyas = totalDiyas,
              difficulty = difficulty,
              levelId = levelId,
              levelTitle = levelDef.title,
              nextLevelUnlocked = nextUnlocked,
              relicsFoundCount = relicsFoundCount,
              totalRelics = levelDef.relics.size,
              deathsCount = newEngine.deathCount,
              isTrollLevel = levelDef.isTrollLevel
          )
        }
    )

    val session = GameSession(
        sessionId = ++nextSessionId,
        engine = newEngine,
        renderer = renderer
    )
    _gameSession.value = session
    touchControlState.bindEngine(newEngine)
    syncEffectivePause()
    _currentScreen.value = AppScreen.GAMEPLAY
  }

  fun startNextLevel() {
    val nextId = (_activeLevelId.value + 1).coerceAtMost(5)
    startLevel(levelId = nextId, difficulty = _selectedDifficulty.value)
  }

  fun setUserPaused(paused: Boolean) {
    if (paused) {
      touchControlState.clearAll()
    }
    _isUserPaused.value = paused
    syncEffectivePause()
  }

  fun onHostPaused() {
    touchControlState.clearAll()
    audioEngine.pause()
    _isHostPaused.value = true
    syncEffectivePause()
  }

  fun onHostResumed() {
    touchControlState.clearAll()
    audioEngine.resume()
    _isHostPaused.value = false
    _isWindowFocusLost.value = false
    syncEffectivePause()
  }

  fun onWindowFocusChanged(hasFocus: Boolean) {
    if (!hasFocus) {
      touchControlState.clearAll()
      _isWindowFocusLost.value = true
    } else {
      _isWindowFocusLost.value = false
    }
    syncEffectivePause()
  }

  private fun syncEffectivePause() {
    val effective = _isUserPaused.value || _isHostPaused.value || _isWindowFocusLost.value || _isFieldNotesOpen.value
    _isEffectivePaused.value = effective
    _gameSession.value?.engine?.isPaused = effective
  }

  fun returnToMainMenu() {
    touchControlState.clearAll()
    touchControlState.bindEngine(null)
    _isUserPaused.value = true
    syncEffectivePause()
    _victoryResult.value = null
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    refreshSavedProgress()
    _currentScreen.value = AppScreen.MAIN_MENU
  }

  fun showHowToPlay() {
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _currentScreen.value = AppScreen.HOW_TO_PLAY
  }

  fun showSettings() {
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _currentScreen.value = AppScreen.SETTINGS
  }

  fun openFieldNotes() {
    refreshSavedProgress()
    _isChapterTrailOpen.value = false
    _isFieldNotesOpen.value = true
    syncEffectivePause()
  }

  fun closeFieldNotes() {
    _isFieldNotesOpen.value = false
    syncEffectivePause()
  }

  fun openChapterTrail() {
    refreshSavedProgress()
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = true
  }

  fun closeChapterTrail() {
    _isChapterTrailOpen.value = false
  }

  fun restartLevel() {
    startLevel(_activeLevelId.value, _selectedDifficulty.value, enforceUnlock = false)
  }

  fun refreshSavedProgress() {
    _discoveredClues.value = saveRepository.getDiscoveredClues()
    _isResonanceDiscovered.value = saveRepository.isResonanceStoneDiscovered()
    _bestTimeMs.value = saveRepository.getBestTime(1, 1)
    _bestStars.value = saveRepository.getStars(1, 1)
    _bestScore.value = saveRepository.getBestScore(1, 1)
    _levelsProgress.value = buildLevelsProgress()
  }

  fun toggleMusic() {
    val newState = !audioEngine.isMusicEnabled
    audioEngine.isMusicEnabled = newState
    saveRepository.setMusicEnabled(newState)
    _isMusicMuted.value = !newState
  }

  fun toggleSfx() {
    val newState = !audioEngine.isSfxEnabled
    audioEngine.isSfxEnabled = newState
    saveRepository.setSfxEnabled(newState)
  }

  fun setMusicVolume(vol: Float) {
    audioEngine.musicVolume = vol
    saveRepository.setMusicVolume(vol)
  }

  fun setSfxVolume(vol: Float) {
    audioEngine.sfxVolume = vol
    saveRepository.setSfxVolume(vol)
  }

  override fun onCleared() {
    super.onCleared()
    touchControlState.clearAll()
    audioEngine.release()
  }
}
