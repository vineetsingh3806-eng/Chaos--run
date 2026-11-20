package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.audio.ProceduralAudioEngine
import com.example.data.ChallengeCatalog
import com.example.data.ContinuousRunPersistedState
import com.example.data.DifficultyTier
import com.example.data.LockedChallengeConfig
import com.example.data.RunCompletionSummary
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.StoryClueId
import com.example.data.World1Levels
import com.example.engine.AttemptRetryTransition
import com.example.engine.ChallengeCompletionOutcome
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.engine.ContinuousRunCoordinator
import com.example.ui.input.MultiTouchControlState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen {
  MAIN_MENU,
  GAMEPLAY,
  RUN_COMPLETION,
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
    val isTrollLevel: Boolean = false,
    val launchMode: RunLaunchMode = RunLaunchMode.SINGLE_STAGE
)

class GameStateViewModel(
    application: Application,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
  constructor(application: Application) : this(application, SavedStateHandle())

  val saveRepository = SaveRepository(application)
  val audioEngine = ProceduralAudioEngine(context = application)
  val touchControlState = MultiTouchControlState()
  val continuousCoordinator: ContinuousRunCoordinator

  private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _activeLevelId = MutableStateFlow(1)
  val activeLevelId: StateFlow<Int> = _activeLevelId.asStateFlow()

  private val _runLaunchMode = MutableStateFlow(RunLaunchMode.SINGLE_STAGE)
  val runLaunchMode: StateFlow<RunLaunchMode> = _runLaunchMode.asStateFlow()

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

  private val _continuousPersistedState: MutableStateFlow<ContinuousRunPersistedState>
  val continuousPersistedState: StateFlow<ContinuousRunPersistedState>

  private val _runCompletionSummary = MutableStateFlow<RunCompletionSummary?>(null)
  val runCompletionSummary: StateFlow<RunCompletionSummary?> = _runCompletionSummary.asStateFlow()

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
    saveRepository.migrateIfNeeded()
    val persisted = saveRepository.loadContinuousRunState()
    continuousCoordinator = ContinuousRunCoordinator(
        catalog = ChallengeCatalog.entries,
        initialRecords = persisted.challengeRecords,
        bestRunTimeMs = persisted.bestRunTimeMs,
        bestRunScore = persisted.bestRunScore,
        bestRunLeastDeaths = persisted.bestRunLeastDeaths
    )
    continuousCoordinator.restoreFromPersisted(persisted)
    _continuousPersistedState = MutableStateFlow(persisted)
    continuousPersistedState = _continuousPersistedState.asStateFlow()
    if (persisted.isRunCompleted) {
      _runCompletionSummary.value = continuousCoordinator.buildRunCompletionSummary()
    }

    audioEngine.isMusicEnabled = saveRepository.isMusicEnabled()
    audioEngine.isSfxEnabled = saveRepository.isSfxEnabled()
    audioEngine.musicVolume = saveRepository.getMusicVolume()
    audioEngine.sfxVolume = saveRepository.getSfxVolume()
    audioEngine.startMusic()

    // Reliable process-death recovery for an interrupted foreground continuous run
    if (persisted.isRunActive && persisted.isRunInForeground && !persisted.isRunCompleted) {
      val recoveredConfig = continuousCoordinator.restoreFromPersisted(persisted)
      launchContinuousSession(
          lockedConfig = recoveredConfig,
          attemptToken = continuousCoordinator.currentAttemptToken,
          launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
          initialDeathsInChallenge = persisted.currentChallengeDeaths
      )
    }
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

  /**
   * Starts a continuous run or resumes an active unfinished continuous run.
   */
  fun startOrContinueContinuousRun() {
    val persisted = saveRepository.loadContinuousRunState()
    if (persisted.isRunActive && !persisted.isRunCompleted) {
      val config = continuousCoordinator.restoreFromPersisted(persisted)
      saveRepository.saveContinuousRunState(
          continuousCoordinator.exportPersistedState(
              schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
              isRunInForeground = true
          ),
          syncCommit = true
      )
      _continuousPersistedState.value = saveRepository.loadContinuousRunState()
      launchContinuousSession(
          lockedConfig = config,
          attemptToken = continuousCoordinator.currentAttemptToken,
          launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
          initialDeathsInChallenge = persisted.currentChallengeDeaths
      )
    } else {
      startNewContinuousRun()
    }
  }

  fun startNewContinuousRun() {
    val firstConfig = continuousCoordinator.startNewRun(preserveBestRecords = true)
    val state = continuousCoordinator.exportPersistedState(
        schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
        isRunInForeground = true
    )
    saveRepository.saveContinuousRunState(state, syncCommit = true)
    _continuousPersistedState.value = state
    _runCompletionSummary.value = null
    launchContinuousSession(
        lockedConfig = firstConfig,
        attemptToken = continuousCoordinator.currentAttemptToken,
        launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
        initialDeathsInChallenge = 0
    )
  }

  fun replayCompletedChallenge(challengeId: String) {
    val config = ChallengeCatalog.createLockedConfig(challengeId) ?: return
    saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    launchContinuousSession(
        lockedConfig = config,
        attemptToken = System.nanoTime(),
        launchMode = RunLaunchMode.REPLAY,
        initialDeathsInChallenge = 0
    )
  }

  fun openRunCompletionScreen() {
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _victoryResult.value = null
    _runCompletionSummary.value = continuousCoordinator.buildRunCompletionSummary()
    _currentScreen.value = AppScreen.RUN_COMPLETION
  }

  fun returnToRunCompletion() {
    touchControlState.clearAll()
    _isUserPaused.value = true
    syncEffectivePause()
    _victoryResult.value = null
    _isFieldNotesOpen.value = false
    _runCompletionSummary.value = continuousCoordinator.buildRunCompletionSummary()
    _currentScreen.value = AppScreen.RUN_COMPLETION
  }

  fun retryCurrentChallenge() {
    val session = _gameSession.value
    if (session == null) {
      startOrContinueContinuousRun()
      return
    }
    touchControlState.clearAll()
    _isUserPaused.value = false
    _isHostPaused.value = false
    _isWindowFocusLost.value = false
    _victoryResult.value = null
    _isFieldNotesOpen.value = false

    val mode = session.engine.launchMode
    if (mode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
      val config = continuousCoordinator.prepareRetryCurrentChallenge()
      val state = continuousCoordinator.exportPersistedState(
          schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
          isRunInForeground = true
      )
      saveRepository.saveContinuousRunState(state, syncCommit = true)
      _continuousPersistedState.value = state
      session.engine.loadChallenge(
          lockedConfig = config,
          attemptToken = continuousCoordinator.currentAttemptToken,
          launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
          discoveredRelics = saveRepository.getDiscoveredRelicIds(config.levelDefinition.relics.map { it.id }),
          preserveCumulativeStats = true,
          initialDeathsInChallenge = continuousCoordinator.currentChallengeDeaths,
          cumulativeTimerMs = continuousCoordinator.cumulativeRunTimerMs,
          cumulativeDeaths = continuousCoordinator.cumulativeDeaths
      )
    } else {
      session.engine.resetCurrentChallenge(clearInputs = true)
    }
    syncEffectivePause()
    _currentScreen.value = AppScreen.GAMEPLAY
  }

  fun startLevel1() {
    startLevel(levelId = 1)
  }

  fun startLevel(
      levelId: Int,
      enforceUnlock: Boolean = true
  ) {
    if (enforceUnlock && !saveRepository.isLevelUnlocked(1, levelId)) {
      return
    }
    val config = ChallengeCatalog.fromLegacyLevel(1, levelId)
        ?: return
    saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    launchSingleStageSession(levelId = levelId, lockedConfig = config)
  }

  private fun launchContinuousSession(
      lockedConfig: LockedChallengeConfig,
      attemptToken: Long,
      launchMode: RunLaunchMode,
      initialDeathsInChallenge: Int
  ) {
    val levelDef = lockedConfig.levelDefinition
    touchControlState.clearAll()
    _isUserPaused.value = false
    _isHostPaused.value = false
    _isWindowFocusLost.value = false
    _victoryResult.value = null
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _activeLevelId.value = lockedConfig.orderIndex
    _runLaunchMode.value = launchMode

    val existingSession = _gameSession.value
    if (existingSession != null && _currentScreen.value == AppScreen.GAMEPLAY &&
        existingSession.engine.launchMode == launchMode) {
      existingSession.engine.loadChallenge(
          lockedConfig = lockedConfig,
          attemptToken = attemptToken,
          launchMode = launchMode,
          discoveredRelics = saveRepository.getDiscoveredRelicIds(levelDef.relics.map { it.id }),
          preserveCumulativeStats = (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION),
          initialDeathsInChallenge = initialDeathsInChallenge,
          cumulativeTimerMs = if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) continuousCoordinator.cumulativeRunTimerMs else 0L,
          cumulativeDeaths = if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) continuousCoordinator.cumulativeDeaths else 0
      )
      touchControlState.bindEngine(existingSession.engine)
      syncEffectivePause()
      _currentScreen.value = AppScreen.GAMEPLAY
      return
    }

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
        lockedChallengeConfig = lockedConfig,
        initialCompletedChallengeIds = if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
          continuousCoordinator.completedChallengeIds.toSet()
        } else {
          emptySet()
        },
        initialCumulativeRunTimerMs = if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
          continuousCoordinator.cumulativeRunTimerMs
        } else {
          0L
        },
        initialCumulativeDeathCount = if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
          continuousCoordinator.cumulativeDeaths
        } else {
          0
        },
        initialAttemptToken = attemptToken,
        initialLaunchMode = launchMode,
        initialChallengeDeaths = initialDeathsInChallenge,
        onChallengeDeathAttributed = { challengeId, challengeDeaths, cumulativeDeaths, causeKey ->
          if (_gameSession.value?.engine !== newEngine) return@ChaosGameEngine
          if (newEngine.launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
            continuousCoordinator.recordDeath(
                challengeId = challengeId,
                challengeDeathsOverride = challengeDeaths,
                cumulativeDeathsOverride = cumulativeDeaths,
                trapCauseKey = causeKey
            )
            val persistedState = continuousCoordinator.exportPersistedState(
                schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
                isRunInForeground = (_currentScreen.value == AppScreen.GAMEPLAY)
            )
            saveRepository.saveContinuousRunState(persistedState, syncCommit = true)
            _continuousPersistedState.value = persistedState
          }
        },
        onAttemptRetryConfigRequested = { _, _, _ ->
          if (_gameSession.value?.engine !== newEngine) return@ChaosGameEngine null
          if (newEngine.launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
            val nextLocked = continuousCoordinator.prepareRetryCurrentChallenge()
            val persistedState = continuousCoordinator.exportPersistedState(
                schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
                isRunInForeground = (_currentScreen.value == AppScreen.GAMEPLAY)
            )
            saveRepository.saveContinuousRunState(persistedState, syncCommit = true)
            _continuousPersistedState.value = persistedState
            AttemptRetryTransition(
                lockedConfig = nextLocked,
                attemptToken = continuousCoordinator.currentAttemptToken
            )
          } else {
            null
          }
        },
        onLevelCompleted = { timeMs, coinsCollected, totalCoins ->
          if (_gameSession.value?.engine !== newEngine) return@ChaosGameEngine
          handleEngineCompleted(
              engine = newEngine,
              timeMs = timeMs,
              coinsCollected = coinsCollected,
              totalCoins = totalCoins
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

  private fun launchSingleStageSession(
      levelId: Int,
      lockedConfig: LockedChallengeConfig
  ) {
    touchControlState.clearAll()
    _isUserPaused.value = false
    _isHostPaused.value = false
    _isWindowFocusLost.value = false
    _victoryResult.value = null
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _activeLevelId.value = levelId
    _runLaunchMode.value = RunLaunchMode.SINGLE_STAGE

    val levelDef = lockedConfig.levelDefinition
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
        lockedChallengeConfig = lockedConfig,
        initialLaunchMode = RunLaunchMode.SINGLE_STAGE,
        onLevelCompleted = { timeMs, coinsCollected, totalCoins ->
          if (_gameSession.value?.engine !== newEngine) return@ChaosGameEngine
          handleEngineCompleted(
              engine = newEngine,
              timeMs = timeMs,
              coinsCollected = coinsCollected,
              totalCoins = totalCoins
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

  private fun handleEngineCompleted(
      engine: ChaosGameEngine,
      timeMs: Long,
      coinsCollected: Int,
      totalCoins: Int
  ) {
    val activeLevelDef = engine.level
    val finalScore = engine.score
    val challengeId = engine.activeChallengeId
    val attemptToken = engine.challengeAttemptToken
    val challengeDeaths = engine.deathCount

    when (engine.launchMode) {
      RunLaunchMode.CONTINUOUS_PROGRESSION -> {
        val outcome = continuousCoordinator.onChallengeCompleted(
            challengeId = challengeId,
            attemptToken = attemptToken,
            timeMs = timeMs,
            score = finalScore,
            coinsCollected = coinsCollected,
            totalCoins = totalCoins,
            parTimeSeconds = activeLevelDef.parTimeSeconds,
            isTrollLevel = activeLevelDef.isTrollLevel,
            challengeDeaths = challengeDeaths,
            engineCumulativeRunTimerMs = engine.cumulativeRunTimerMs,
            engineCumulativeDeaths = engine.cumulativeDeathCount
        )
        when (outcome) {
          ChallengeCompletionOutcome.IgnoredDuplicate -> return
          is ChallengeCompletionOutcome.NextChallenge -> {
            touchControlState.clearAll()
            val persistedState = continuousCoordinator.exportPersistedState(
                schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
                isRunInForeground = true
            )
            saveRepository.saveChallengeCompletionBoundary(
                state = persistedState,
                completedChallengeId = outcome.completedChallengeId,
                record = outcome.completedRecord,
                syncCommit = true
            )
            _continuousPersistedState.value = persistedState
            _activeLevelId.value = outcome.nextConfig.orderIndex
            refreshSavedProgress()

            val nextRelics = saveRepository.getDiscoveredRelicIds(
                outcome.nextConfig.levelDefinition.relics.map { it.id }
            )
            engine.completedChallengeIds.add(outcome.completedChallengeId)
            engine.loadChallenge(
                lockedConfig = outcome.nextConfig,
                attemptToken = outcome.nextAttemptToken,
                launchMode = RunLaunchMode.CONTINUOUS_PROGRESSION,
                discoveredRelics = nextRelics,
                preserveCumulativeStats = true,
                initialDeathsInChallenge = 0,
                cumulativeTimerMs = outcome.cumulativeRunTimerMs,
                cumulativeDeaths = outcome.cumulativeDeaths
            )
          }
          is ChallengeCompletionOutcome.RunCompleted -> {
            touchControlState.clearAll()
            val persistedState = continuousCoordinator.exportPersistedState(
                schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
                isRunInForeground = false
            )
            saveRepository.saveChallengeCompletionBoundary(
                state = persistedState,
                completedChallengeId = outcome.completedChallengeId,
                record = outcome.completedRecord,
                syncCommit = true
            )
            _continuousPersistedState.value = persistedState
            _runCompletionSummary.value = outcome.summary
            refreshSavedProgress()
            _victoryResult.value = null
            _isUserPaused.value = false
            syncEffectivePause()
            _currentScreen.value = AppScreen.RUN_COMPLETION
          }
        }
      }

      RunLaunchMode.REPLAY -> {
        touchControlState.clearAll()
        val updatedRecord = continuousCoordinator.recordReplayCompletionIfImproved(
            challengeId = challengeId,
            timeMs = timeMs,
            score = finalScore,
            coinsCollected = coinsCollected,
            totalCoins = totalCoins,
            parTimeSeconds = activeLevelDef.parTimeSeconds,
            isTrollLevel = activeLevelDef.isTrollLevel,
            challengeDeaths = challengeDeaths
        )
        saveRepository.saveReplayChallengeRecordIfImproved(
            challengeId = challengeId,
            record = updatedRecord,
            syncCommit = true
        )
        _runCompletionSummary.value = continuousCoordinator.buildRunCompletionSummary()
        _continuousPersistedState.value = saveRepository.loadContinuousRunState()
        refreshSavedProgress()

        val litDiyas = engine.diyas.count { it.isLit }
        val totalDiyas = engine.diyas.size
        val stars = ContinuousRunCoordinator.computeStars(
            levelDef = activeLevelDef,
            timeMs = timeMs,
            coinsCollected = coinsCollected,
            totalCoins = totalCoins,
            deathsCount = challengeDeaths
        )
        val relicIds = activeLevelDef.relics.map { it.id }
        _victoryResult.value = VictoryResult(
            timeMs = timeMs,
            coinsCollected = coinsCollected,
            totalCoins = totalCoins,
            stars = stars,
            cluesDiscoveredCount = saveRepository.getDiscoveredClueCount(),
            totalClues = StoryClueId.entries.size,
            resonanceStoneFound = saveRepository.isResonanceStoneDiscovered(),
            score = finalScore,
            diyasLit = litDiyas,
            totalDiyas = totalDiyas,
            difficulty = DifficultyTier.NORMAL,
            levelId = activeLevelDef.levelId,
            levelTitle = activeLevelDef.title,
            nextLevelUnlocked = false,
            relicsFoundCount = saveRepository.getDiscoveredRelicIds(relicIds).size,
            totalRelics = activeLevelDef.relics.size,
            deathsCount = challengeDeaths,
            isTrollLevel = activeLevelDef.isTrollLevel,
            launchMode = RunLaunchMode.REPLAY
        )
      }

      RunLaunchMode.SINGLE_STAGE -> {
        touchControlState.clearAll()
        val levelId = activeLevelDef.levelId
        val litDiyas = engine.diyas.count { it.isLit }
        val totalDiyas = engine.diyas.size
        val stars = ContinuousRunCoordinator.computeStars(
            levelDef = activeLevelDef,
            timeMs = timeMs,
            coinsCollected = coinsCollected,
            totalCoins = totalCoins,
            deathsCount = challengeDeaths
        )
        saveRepository.saveBestTime(1, levelId, timeMs)
        saveRepository.saveStars(1, levelId, stars)
        saveRepository.saveBestScore(1, levelId, finalScore)
        refreshSavedProgress()
        val clueCount = saveRepository.getDiscoveredClueCount()
        val stoneFound = saveRepository.isResonanceStoneDiscovered()
        val relicIds = activeLevelDef.relics.map { it.id }
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
            difficulty = DifficultyTier.NORMAL,
            levelId = levelId,
            levelTitle = activeLevelDef.title,
            nextLevelUnlocked = nextUnlocked,
            relicsFoundCount = relicsFoundCount,
            totalRelics = activeLevelDef.relics.size,
            deathsCount = challengeDeaths,
            isTrollLevel = activeLevelDef.isTrollLevel,
            launchMode = RunLaunchMode.SINGLE_STAGE
        )
      }
    }
  }

  fun startNextLevel() {
    val nextId = (_activeLevelId.value + 1).coerceAtMost(5)
    startLevel(levelId = nextId)
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
    saveRepository.setContinuousRunInForeground(false, syncCommit = true)
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
    val mode = _gameSession.value?.engine?.launchMode ?: _runLaunchMode.value
    if (mode == RunLaunchMode.CONTINUOUS_PROGRESSION || mode == RunLaunchMode.REPLAY) {
      retryCurrentChallenge()
    } else {
      startLevel(_activeLevelId.value, enforceUnlock = false)
    }
  }

  fun refreshSavedProgress() {
    _discoveredClues.value = saveRepository.getDiscoveredClues()
    _isResonanceDiscovered.value = saveRepository.isResonanceStoneDiscovered()
    _bestTimeMs.value = saveRepository.getBestTime(1, 1)
    _bestStars.value = saveRepository.getStars(1, 1)
    _bestScore.value = saveRepository.getBestScore(1, 1)
    _levelsProgress.value = buildLevelsProgress()
    _continuousPersistedState.value = saveRepository.loadContinuousRunState()
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
