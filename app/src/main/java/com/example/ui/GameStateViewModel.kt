package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.audio.ProceduralAudioEngine
import com.example.data.AdaptiveTier
import com.example.data.ChallengeAttemptMetrics
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeMasteryProgress
import com.example.data.ChaosModifier
import com.example.data.ContinuousRunPersistedState
import com.example.data.ControlButtonSize
import com.example.data.ControlLayoutMode
import com.example.data.CosmeticTrail
import com.example.data.DailyChallengeSpec
import com.example.data.DifficultyTier
import com.example.data.LevelTheme
import com.example.data.LockedChallengeConfig
import com.example.data.MasteryCatalog
import com.example.data.OfflineCalendarDate
import com.example.data.OfflineDailyChallengeGenerator
import com.example.data.Phase4RetentionSnapshot
import com.example.data.PlayableCharacter
import com.example.data.PlayerTitle
import com.example.data.RunCompletionSummary
import com.example.data.RunLaunchMode
import com.example.data.SaveRepository
import com.example.data.StoryClueId
import com.example.data.UnlockedRewardNotification
import com.example.data.VictoryEffect
import com.example.data.World1Levels
import com.example.engine.AttemptRetryTransition
import com.example.engine.ChallengeCompletionOutcome
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import com.example.engine.ContinuousRunCoordinator
import com.example.engine.HapticCue
import com.example.ui.input.MultiTouchControlState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class AppScreen {
  MAIN_MENU,
  GAMEPLAY,
  RUN_COMPLETION,
  PROGRESSION_HUB,
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
    val launchMode: RunLaunchMode = RunLaunchMode.SINGLE_STAGE,
    val challengeId: String = "ch_01",
    val activeModifiers: Set<ChaosModifier> = emptySet(),
    val dailyDateKey: String? = null,
    val isDailyNewCompletion: Boolean = false,
    val newlyEarnedMasteryIds: Set<String> = emptySet(),
    val totalChallengeMasteryStars: Int = 0,
    val unlockedRewardsNotification: UnlockedRewardNotification = UnlockedRewardNotification()
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

  private val _isMusicEnabled = MutableStateFlow(saveRepository.isMusicEnabled())
  val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

  private val _isSfxEnabled = MutableStateFlow(saveRepository.isSfxEnabled())
  val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

  private val _musicVolume = MutableStateFlow(saveRepository.getMusicVolume())
  val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

  private val _sfxVolume = MutableStateFlow(saveRepository.getSfxVolume())
  val sfxVolume: StateFlow<Float> = _sfxVolume.asStateFlow()

  private val _isVibrationEnabled = MutableStateFlow(saveRepository.isVibrationEnabled())
  val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

  private val _isOnboardingCompleted = MutableStateFlow(saveRepository.isOnboardingCompleted())
  val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

  private val _isOnboardingModalOpen = MutableStateFlow(false)
  val isOnboardingModalOpen: StateFlow<Boolean> = _isOnboardingModalOpen.asStateFlow()

  private val _selectedCharacter = MutableStateFlow(saveRepository.getSelectedCharacter())
  val selectedCharacter: StateFlow<PlayableCharacter> = _selectedCharacter.asStateFlow()

  private val _controlLayoutMode = MutableStateFlow(saveRepository.getControlLayoutMode())
  val controlLayoutMode: StateFlow<ControlLayoutMode> = _controlLayoutMode.asStateFlow()

  private val _controlButtonSize = MutableStateFlow(saveRepository.getControlButtonSize())
  val controlButtonSize: StateFlow<ControlButtonSize> = _controlButtonSize.asStateFlow()

  private val _controlOpacity = MutableStateFlow(saveRepository.getControlOpacity())
  val controlOpacity: StateFlow<Float> = _controlOpacity.asStateFlow()

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

  private val _lastSaveError = MutableStateFlow(false)
  val lastSaveError: StateFlow<Boolean> = _lastSaveError.asStateFlow()

  private val _hasDismissedFirstRunHint = MutableStateFlow(saveRepository.isOnboardingCompleted())
  val hasDismissedFirstRunHint: StateFlow<Boolean> = _hasDismissedFirstRunHint.asStateFlow()

  var dailyDateProvider: () -> OfflineCalendarDate = { OfflineCalendarDate.todayLocal() }

  private val _retentionSnapshot = MutableStateFlow(saveRepository.loadPhase4RetentionSnapshot())
  val retentionSnapshot: StateFlow<Phase4RetentionSnapshot> = _retentionSnapshot.asStateFlow()

  private val _selectedModifiers = MutableStateFlow<Set<ChaosModifier>>(emptySet())
  val selectedModifiers: StateFlow<Set<ChaosModifier>> = _selectedModifiers.asStateFlow()

  private val _dailyChallengeSpec = MutableStateFlow(
      OfflineDailyChallengeGenerator.forDate(OfflineCalendarDate.todayLocal())
  )
  val dailyChallengeSpec: StateFlow<DailyChallengeSpec> = _dailyChallengeSpec.asStateFlow()

  private var hubReturnScreen: AppScreen = AppScreen.MAIN_MENU

  private val persistenceExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "ChaosRunPersistence").apply { isDaemon = true }
  }

  private var nextSessionId = 0L

  val gameEngine: ChaosGameEngine?
    get() = _gameSession.value?.engine

  val gameRenderer: ChaosGameRenderer?
    get() = _gameSession.value?.renderer

  private fun isOnRenderThread(): Boolean =
      Thread.currentThread().name == "ChaosRunRenderThread"

  private fun recordSaveOutcome(succeeded: Boolean): Boolean {
    _lastSaveError.value = !succeeded
    return succeeded
  }

  private fun persistCriticalBoundary(operation: () -> Boolean) {
    if (isOnRenderThread()) {
      try {
        persistenceExecutor.execute {
          val ok = try {
            operation()
          } catch (_: Throwable) {
            false
          }
          recordSaveOutcome(ok)
        }
      } catch (_: Throwable) {
        recordSaveOutcome(false)
      }
      return
    }

    val ok = try {
      if (persistenceExecutor.isShutdown) {
        operation()
      } else {
        persistenceExecutor.submit(Callable {
          try {
            operation()
          } catch (_: Throwable) {
            false
          }
        }).get()
      }
    } catch (_: Throwable) {
      false
    }
    recordSaveOutcome(ok)
  }

  fun flushPendingPersistence() {
    if (isOnRenderThread() || persistenceExecutor.isShutdown) return
    try {
      persistenceExecutor.submit {}.get()
    } catch (_: Throwable) {
      // Best-effort drain of queued persistence operations
    }
  }

  fun clearSaveError() {
    _lastSaveError.value = false
  }

  init {
    saveRepository.migrateIfNeeded()
    _retentionSnapshot.value = saveRepository.loadPhase4RetentionSnapshot()
    _dailyChallengeSpec.value = OfflineDailyChallengeGenerator.forDate(dailyDateProvider())
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
    flushPendingPersistence()
    val persisted = saveRepository.loadContinuousRunState()
    if (persisted.isRunActive && !persisted.isRunCompleted) {
      val config = continuousCoordinator.restoreFromPersisted(persisted)
      val foregroundState = continuousCoordinator.exportPersistedState(
          schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
          isRunInForeground = true
      )
      persistCriticalBoundary {
        saveRepository.saveContinuousRunState(foregroundState, syncCommit = true)
      }
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
    persistCriticalBoundary {
      saveRepository.saveContinuousRunState(state, syncCommit = true)
    }
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
    persistCriticalBoundary {
      saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    }
    launchContinuousSession(
        lockedConfig = config,
        attemptToken = System.nanoTime(),
        launchMode = RunLaunchMode.REPLAY,
        initialDeathsInChallenge = 0
    )
  }

  fun refreshDailyChallenge(date: OfflineCalendarDate = dailyDateProvider()) {
    _dailyChallengeSpec.value = OfflineDailyChallengeGenerator.forDate(date)
  }

  fun openProgressionHub(fromCompletionScreen: Boolean = false) {
    hubReturnScreen = if (fromCompletionScreen) AppScreen.RUN_COMPLETION else AppScreen.MAIN_MENU
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _victoryResult.value = null
    refreshSavedProgress()
    refreshDailyChallenge()
    _currentScreen.value = AppScreen.PROGRESSION_HUB
  }

  fun closeProgressionHub() {
    if (hubReturnScreen == AppScreen.RUN_COMPLETION) {
      openRunCompletionScreen()
    } else {
      returnToMainMenu()
    }
  }

  fun returnToProgressionHub() {
    touchControlState.clearAll()
    touchControlState.bindEngine(null)
    _isUserPaused.value = true
    syncEffectivePause()
    _victoryResult.value = null
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    persistCriticalBoundary {
      saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    }
    audioEngine.setThemeAndIntensity(LevelTheme.WHISPERING_WOODS, AdaptiveTier.STANDARD)
    refreshSavedProgress()
    refreshDailyChallenge()
    _currentScreen.value = AppScreen.PROGRESSION_HUB
  }

  fun toggleModifierSelection(modifier: ChaosModifier) {
    val next = _selectedModifiers.value.toMutableSet()
    if (!next.add(modifier)) {
      next.remove(modifier)
    }
    _selectedModifiers.value = next
    audioEngine.playUiClick()
  }

  fun setSelectedModifiers(modifiers: Set<ChaosModifier>) {
    _selectedModifiers.value = modifiers.toSet()
  }

  fun clearSelectedModifiers() {
    _selectedModifiers.value = emptySet()
    audioEngine.playUiClick()
  }

  fun launchMasteryOrModifierChallenge(
      challengeId: String,
      modifiers: Set<ChaosModifier> = _selectedModifiers.value
  ) {
    val tier = if (modifiers.contains(ChaosModifier.TRAP_RUSH)) {
      AdaptiveTier.RELENTLESS
    } else {
      AdaptiveTier.STANDARD
    }
    val config = ChallengeCatalog.createLockedConfig(challengeId, tier) ?: return
    persistCriticalBoundary {
      saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    }
    launchContinuousSession(
        lockedConfig = config,
        attemptToken = System.nanoTime(),
        launchMode = RunLaunchMode.MASTERY_CHALLENGE,
        initialDeathsInChallenge = 0,
        modifiers = modifiers.toSet(),
        dailyDateKey = null
    )
  }

  fun launchDailyChallenge(date: OfflineCalendarDate = dailyDateProvider()) {
    val spec = OfflineDailyChallengeGenerator.forDate(date)
    _dailyChallengeSpec.value = spec
    val config = ChallengeCatalog.createLockedConfig(spec.challengeId, spec.adaptiveTier) ?: return
    persistCriticalBoundary {
      saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    }
    launchContinuousSession(
        lockedConfig = config,
        attemptToken = System.nanoTime(),
        launchMode = RunLaunchMode.DAILY_CHALLENGE,
        initialDeathsInChallenge = 0,
        modifiers = spec.modifiers,
        dailyDateKey = spec.dateKey
    )
  }

  fun equipTrail(trail: CosmeticTrail): Boolean {
    val ok = saveRepository.setEquippedTrail(trail, syncCommit = true)
    recordSaveOutcome(ok)
    if (ok) {
      _retentionSnapshot.value = saveRepository.loadPhase4RetentionSnapshot()
      _gameSession.value?.renderer?.equippedTrail = trail
      audioEngine.playUiClick()
    }
    return ok
  }

  fun equipVictoryEffect(effect: VictoryEffect): Boolean {
    val ok = saveRepository.setEquippedVictoryEffect(effect, syncCommit = true)
    recordSaveOutcome(ok)
    if (ok) {
      _retentionSnapshot.value = saveRepository.loadPhase4RetentionSnapshot()
      _gameSession.value?.engine?.activeVictoryEffect = effect
      audioEngine.playUiClick()
    }
    return ok
  }

  fun equipTitle(title: PlayerTitle): Boolean {
    val ok = saveRepository.setEquippedTitle(title, syncCommit = true)
    recordSaveOutcome(ok)
    if (ok) {
      _retentionSnapshot.value = saveRepository.loadPhase4RetentionSnapshot()
      audioEngine.playUiClick()
    }
    return ok
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
      persistCriticalBoundary {
        saveRepository.saveContinuousRunState(state, syncCommit = true)
      }
      _continuousPersistedState.value = state
      audioEngine.onTransitionOrRetry(suppressAmbientMs = 150L, playRetryCue = true)
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
    persistCriticalBoundary {
      saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    }
    launchSingleStageSession(levelId = levelId, lockedConfig = config)
  }

  private fun launchContinuousSession(
      lockedConfig: LockedChallengeConfig,
      attemptToken: Long,
      launchMode: RunLaunchMode,
      initialDeathsInChallenge: Int,
      modifiers: Set<ChaosModifier> = emptySet(),
      dailyDateKey: String? = null
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

    val equippedRewards = _retentionSnapshot.value.rewards
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
          cumulativeDeaths = if (launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) continuousCoordinator.cumulativeDeaths else 0,
          modifiers = modifiers,
          dailyDateKey = dailyDateKey
      )
      existingSession.engine.selectedCharacter = _selectedCharacter.value
      existingSession.engine.activeVictoryEffect = equippedRewards.equippedEffect
      existingSession.renderer.selectedCharacter = _selectedCharacter.value
      existingSession.renderer.equippedTrail = equippedRewards.equippedTrail
      touchControlState.bindEngine(existingSession.engine)
      syncEffectivePause()
      _currentScreen.value = AppScreen.GAMEPLAY
      return
    }

    val initialRelics = saveRepository.getDiscoveredRelicIds(levelDef.relics.map { it.id })
    val renderer = ChaosGameRenderer(getApplication()).apply {
      selectedCharacter = _selectedCharacter.value
      equippedTrail = equippedRewards.equippedTrail
    }

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
            _continuousPersistedState.value = persistedState
            persistCriticalBoundary {
              saveRepository.saveContinuousRunState(persistedState, syncCommit = true)
            }
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
            _continuousPersistedState.value = persistedState
            persistCriticalBoundary {
              saveRepository.saveContinuousRunState(persistedState, syncCommit = true)
            }
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
        },
        initialSelectedCharacter = _selectedCharacter.value,
        initialModifiers = modifiers,
        initialDailyDateKey = dailyDateKey,
        initialVictoryEffect = equippedRewards.equippedEffect,
        onHapticCue = { cue -> triggerHaptic(cue) }
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

    val equippedRewards = _retentionSnapshot.value.rewards
    val levelDef = lockedConfig.levelDefinition
    val initialRelics = saveRepository.getDiscoveredRelicIds(levelDef.relics.map { it.id })
    val renderer = ChaosGameRenderer(getApplication()).apply {
      selectedCharacter = _selectedCharacter.value
      equippedTrail = equippedRewards.equippedTrail
    }

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
        },
        initialSelectedCharacter = _selectedCharacter.value,
        initialVictoryEffect = equippedRewards.equippedEffect,
        onHapticCue = { cue -> triggerHaptic(cue) }
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
    val activeModifiers = engine.activeModifiers.toSet()
    val dailyDateKey = engine.dailyDateKey

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
            _continuousPersistedState.value = persistedState
            _activeLevelId.value = outcome.nextConfig.orderIndex
            var syncUnlockMsg: String? = null
            persistCriticalBoundary {
              val savedBoundary = saveRepository.saveChallengeCompletionBoundary(
                  state = persistedState,
                  completedChallengeId = outcome.completedChallengeId,
                  record = outcome.completedRecord,
                  syncCommit = true
              )
              val masteryEval = evaluateMasteryAndRewardsSync(
                  challengeId = challengeId,
                  timeMs = timeMs,
                  challengeDeaths = challengeDeaths,
                  coinsCollected = coinsCollected,
                  totalCoins = totalCoins,
                  usedCheckpoint = engine.usedCheckpointInAttempt,
                  score = finalScore,
                  completedModifiers = emptySet()
              )
              val unlockNames = buildList {
                masteryEval.unlockedRewards.newTitles.forEach { add(it.displayName) }
                masteryEval.unlockedRewards.newTrails.forEach { add(it.displayName) }
                masteryEval.unlockedRewards.newEffects.forEach { add(it.displayName) }
              }
              val msg = when {
                unlockNames.isNotEmpty() -> "Unlocked: ${unlockNames.joinToString(", ")}"
                masteryEval.newlyEarnedMasteryIds.isNotEmpty() ->
                  "Mastery +${masteryEval.newlyEarnedMasteryIds.size} ★ on ${activeLevelDef.title}!"
                else -> null
              }
              if (msg != null) {
                if (engine.activeChallengeId == outcome.nextConfig.challengeId) {
                  engine.postNotification(
                      eventKey = "UNLOCK_${challengeId}_${unlockNames.size}_${masteryEval.newlyEarnedMasteryIds.size}",
                      title = "Reward Unlocked",
                      message = msg,
                      category = com.example.data.NotificationCategory.STORY_CLUE,
                      priority = com.example.data.NotificationPriority.DISCOVERY,
                      visualKind = com.example.data.NotificationVisualKind.TRIUMPH
                  )
                } else {
                  syncUnlockMsg = msg
                }
              }
              savedBoundary
            }

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
            syncUnlockMsg?.let { msg ->
              engine.postNotification(
                  eventKey = "UNLOCK_${challengeId}_SYNC",
                  title = "Reward Unlocked",
                  message = msg,
                  category = com.example.data.NotificationCategory.STORY_CLUE,
                  priority = com.example.data.NotificationPriority.DISCOVERY,
                  visualKind = com.example.data.NotificationVisualKind.TRIUMPH
              )
            }
          }
          is ChallengeCompletionOutcome.RunCompleted -> {
            touchControlState.clearAll()
            val persistedState = continuousCoordinator.exportPersistedState(
                schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
                isRunInForeground = false
            )
            _continuousPersistedState.value = persistedState
            _runCompletionSummary.value = outcome.summary
            persistCriticalBoundary {
              val savedBoundary = saveRepository.saveChallengeCompletionBoundary(
                  state = persistedState,
                  completedChallengeId = outcome.completedChallengeId,
                  record = outcome.completedRecord,
                  syncCommit = true
              )
              evaluateMasteryAndRewardsSync(
                  challengeId = challengeId,
                  timeMs = timeMs,
                  challengeDeaths = challengeDeaths,
                  coinsCollected = coinsCollected,
                  totalCoins = totalCoins,
                  usedCheckpoint = engine.usedCheckpointInAttempt,
                  score = finalScore,
                  completedModifiers = emptySet()
              )
              savedBoundary
            }
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
        var masteryEval = MasteryEvalResult()
        persistCriticalBoundary {
          val ok = saveRepository.saveReplayChallengeRecordIfImproved(
              challengeId = challengeId,
              record = updatedRecord,
              syncCommit = true
          )
          masteryEval = evaluateMasteryAndRewardsSync(
              challengeId = challengeId,
              timeMs = timeMs,
              challengeDeaths = challengeDeaths,
              coinsCollected = coinsCollected,
              totalCoins = totalCoins,
              usedCheckpoint = engine.usedCheckpointInAttempt,
              score = finalScore,
              completedModifiers = emptySet()
          )
          ok
        }
        _runCompletionSummary.value = continuousCoordinator.buildRunCompletionSummary()
        _continuousPersistedState.value = continuousCoordinator.exportPersistedState(
            schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
            isRunInForeground = false
        )
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
            launchMode = RunLaunchMode.REPLAY,
            challengeId = challengeId,
            newlyEarnedMasteryIds = masteryEval.newlyEarnedMasteryIds,
            totalChallengeMasteryStars = masteryEval.challengeMasteryStars,
            unlockedRewardsNotification = masteryEval.unlockedRewards
        )
      }

      RunLaunchMode.MASTERY_CHALLENGE -> {
        touchControlState.clearAll()
        var masteryEval = MasteryEvalResult()
        persistCriticalBoundary {
          if (activeModifiers.isNotEmpty()) {
            // Modifier runs are stored strictly in isolated modifier records; never overwrite normal records
            saveRepository.saveModifierChallengeRecord(
                challengeId = challengeId,
                modifiers = activeModifiers,
                timeMs = timeMs,
                score = finalScore,
                deaths = challengeDeaths,
                syncCommit = true
            )
          } else {
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
          }
          masteryEval = evaluateMasteryAndRewardsSync(
              challengeId = challengeId,
              timeMs = timeMs,
              challengeDeaths = challengeDeaths,
              coinsCollected = coinsCollected,
              totalCoins = totalCoins,
              usedCheckpoint = engine.usedCheckpointInAttempt,
              score = finalScore,
              completedModifiers = activeModifiers
          )
          true
        }
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
            launchMode = RunLaunchMode.MASTERY_CHALLENGE,
            challengeId = challengeId,
            activeModifiers = activeModifiers,
            newlyEarnedMasteryIds = masteryEval.newlyEarnedMasteryIds,
            totalChallengeMasteryStars = masteryEval.challengeMasteryStars,
            unlockedRewardsNotification = masteryEval.unlockedRewards
        )
      }

      RunLaunchMode.DAILY_CHALLENGE -> {
        touchControlState.clearAll()
        val resolvedDateKey = dailyDateKey ?: _dailyChallengeSpec.value.dateKey
        var isNewDaily = false
        var masteryEval = MasteryEvalResult()
        persistCriticalBoundary {
          isNewDaily = saveRepository.recordDailyCompletionIfNew(
              dateKey = resolvedDateKey,
              syncCommit = true
          )
          if (activeModifiers.isNotEmpty()) {
            saveRepository.saveModifierChallengeRecord(
                challengeId = challengeId,
                modifiers = activeModifiers,
                timeMs = timeMs,
                score = finalScore,
                deaths = challengeDeaths,
                syncCommit = true
            )
          }
          masteryEval = evaluateMasteryAndRewardsSync(
              challengeId = challengeId,
              timeMs = timeMs,
              challengeDeaths = challengeDeaths,
              coinsCollected = coinsCollected,
              totalCoins = totalCoins,
              usedCheckpoint = engine.usedCheckpointInAttempt,
              score = finalScore,
              completedModifiers = activeModifiers
          )
          true
        }
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
            launchMode = RunLaunchMode.DAILY_CHALLENGE,
            challengeId = challengeId,
            activeModifiers = activeModifiers,
            dailyDateKey = resolvedDateKey,
            isDailyNewCompletion = isNewDaily,
            newlyEarnedMasteryIds = masteryEval.newlyEarnedMasteryIds,
            totalChallengeMasteryStars = masteryEval.challengeMasteryStars,
            unlockedRewardsNotification = masteryEval.unlockedRewards
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
        val masteryEval = evaluateMasteryAndRewardsSync(
            challengeId = challengeId,
            timeMs = timeMs,
            challengeDeaths = challengeDeaths,
            coinsCollected = coinsCollected,
            totalCoins = totalCoins,
            usedCheckpoint = engine.usedCheckpointInAttempt,
            score = finalScore,
            completedModifiers = emptySet()
        )
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
            launchMode = RunLaunchMode.SINGLE_STAGE,
            challengeId = challengeId,
            newlyEarnedMasteryIds = masteryEval.newlyEarnedMasteryIds,
            totalChallengeMasteryStars = masteryEval.challengeMasteryStars,
            unlockedRewardsNotification = masteryEval.unlockedRewards
        )
      }
    }
  }

  private data class MasteryEvalResult(
      val newlyEarnedMasteryIds: Set<String> = emptySet(),
      val challengeMasteryStars: Int = 0,
      val unlockedRewards: UnlockedRewardNotification = UnlockedRewardNotification()
  )

  private fun evaluateMasteryAndRewardsSync(
      challengeId: String,
      timeMs: Long,
      challengeDeaths: Int,
      coinsCollected: Int,
      totalCoins: Int,
      usedCheckpoint: Boolean,
      score: Int,
      completedModifiers: Set<ChaosModifier>
  ): MasteryEvalResult {
    val prevSnapshot = saveRepository.loadPhase4RetentionSnapshot()
    val prevChallengeEarned = prevSnapshot.masteryByChallenge[challengeId]?.completedObjectiveIds ?: emptySet()

    val metrics = ChallengeAttemptMetrics(
        challengeId = challengeId,
        completed = true,
        timeMs = timeMs,
        deaths = challengeDeaths,
        coinsCollected = coinsCollected,
        totalCoins = totalCoins,
        usedCheckpoint = usedCheckpoint,
        score = score
    )
    val earnedThisAttempt = MasteryCatalog.evaluateAttempt(metrics)
    val newlyEarned = earnedThisAttempt - prevChallengeEarned
    if (newlyEarned.isNotEmpty()) {
      saveRepository.saveMasteryObjectivesEarned(challengeId, earnedThisAttempt, syncCommit = true)
    }
    val zeroDeathAdded = (challengeDeaths == 0 && !prevSnapshot.zeroDeathChallenges.contains(challengeId))
    val coinGreedyAdded = (totalCoins > 0 && coinsCollected >= totalCoins && !prevSnapshot.coinGreedyEarned)
    if (zeroDeathAdded || coinGreedyAdded) {
      saveRepository.recordMilestoneFlags(
          zeroDeathChallengeId = if (zeroDeathAdded) challengeId else null,
          coinGreedyEarned = coinGreedyAdded,
          syncCommit = true
      )
    }

    val mergedMastery = if (newlyEarned.isNotEmpty()) {
      prevSnapshot.masteryByChallenge.toMutableMap().apply {
        put(challengeId, ChallengeMasteryProgress(challengeId, prevChallengeEarned + newlyEarned))
      }
    } else {
      prevSnapshot.masteryByChallenge
    }
    val updatedSnapshot = prevSnapshot.copy(
        masteryByChallenge = mergedMastery,
        zeroDeathChallenges = if (zeroDeathAdded) prevSnapshot.zeroDeathChallenges + challengeId else prevSnapshot.zeroDeathChallenges,
        coinGreedyEarned = prevSnapshot.coinGreedyEarned || coinGreedyAdded
    )
    val allCleared = continuousCoordinator.completedChallengeIds.size >= ChallengeCatalog.entries.size
    val unlockedNotifications = evaluateAndSaveRewardUnlocksLocked(
        snapshot = updatedSnapshot,
        all12Cleared = allCleared,
        completedModifiers = completedModifiers
    )
    val finalSnapshot = if (!unlockedNotifications.isEmpty) {
      updatedSnapshot.copy(
          rewards = updatedSnapshot.rewards.copy(
              unlockedTrails = updatedSnapshot.rewards.unlockedTrails + unlockedNotifications.newTrails,
              unlockedEffects = updatedSnapshot.rewards.unlockedEffects + unlockedNotifications.newEffects,
              unlockedTitles = updatedSnapshot.rewards.unlockedTitles + unlockedNotifications.newTitles
          )
      )
    } else {
      updatedSnapshot
    }
    _retentionSnapshot.value = finalSnapshot
    val totalChallengeStars = finalSnapshot.masteryByChallenge[challengeId]?.masteryStars ?: 0
    return MasteryEvalResult(
        newlyEarnedMasteryIds = newlyEarned,
        challengeMasteryStars = totalChallengeStars,
        unlockedRewards = unlockedNotifications
    )
  }

  private fun evaluateAndSaveRewardUnlocksLocked(
      snapshot: Phase4RetentionSnapshot,
      all12Cleared: Boolean,
      completedModifiers: Set<ChaosModifier>
  ): UnlockedRewardNotification {
    val prevTrails = snapshot.rewards.unlockedTrails
    val prevEffects = snapshot.rewards.unlockedEffects
    val prevTitles = snapshot.rewards.unlockedTitles

    val nextTrails = prevTrails.toMutableSet()
    if (snapshot.totalMasteryStars >= 3) nextTrails.add(CosmeticTrail.EMBER_SPARK)
    if (snapshot.dailyTotalClears >= 1) nextTrails.add(CosmeticTrail.TEAL_ECHO)
    if (snapshot.totalMasteryStars >= 12) nextTrails.add(CosmeticTrail.SOLAR_GOLD)
    if (snapshot.totalModifierClears >= 3) nextTrails.add(CosmeticTrail.VOID_VIOLET)

    val nextEffects = prevEffects.toMutableSet()
    if (snapshot.totalMasteryStars >= 6 || all12Cleared) nextEffects.add(VictoryEffect.SAFFRON_FIREWORKS)
    if (snapshot.dailyTotalClears >= 2) nextEffects.add(VictoryEffect.MONSOON_BURST)
    if (snapshot.totalMasteryStars >= 24) nextEffects.add(VictoryEffect.STARLIGHT_CROWN)

    val speedMasteryCount = snapshot.masteryByChallenge.values.sumOf { prog ->
      prog.completedObjectiveIds.count { it.endsWith("_speed_par") }
    }
    val hasNoCheckpointOrRushRecord =
        completedModifiers.contains(ChaosModifier.NO_CHECKPOINT) ||
            completedModifiers.contains(ChaosModifier.TRAP_RUSH) ||
            snapshot.modifierRecords.values.any {
              it.modifierKey.contains(ChaosModifier.NO_CHECKPOINT.id) ||
                  it.modifierKey.contains(ChaosModifier.TRAP_RUSH.id)
            }
    val hasSpeedRunClear =
        completedModifiers.contains(ChaosModifier.SPEED_RUN) ||
            snapshot.modifierRecords.values.any { it.modifierKey.contains(ChaosModifier.SPEED_RUN.id) }

    val nextTitles = prevTitles.toMutableSet()
    if (snapshot.totalMasteryStars >= 3 || all12Cleared) nextTitles.add(PlayerTitle.TRAP_SURVIVOR)
    if (snapshot.coinGreedyEarned) nextTitles.add(PlayerTitle.COIN_GREEDY)
    if (hasNoCheckpointOrRushRecord) nextTitles.add(PlayerTitle.NO_MERCY)
    if (hasSpeedRunClear || speedMasteryCount >= 3) nextTitles.add(PlayerTitle.SPEED_DEMON)
    if (snapshot.dailyTotalClears >= 1) nextTitles.add(PlayerTitle.DAILY_CHALLENGER)
    if (snapshot.totalMasteryStars >= 18 && snapshot.dailyTotalClears >= 1) {
      nextTitles.add(PlayerTitle.MASTER_OF_CHAOS)
    }

    if (nextTrails != prevTrails || nextEffects != prevEffects || nextTitles != prevTitles) {
      saveRepository.saveUnlockedRewards(
          trails = nextTrails,
          effects = nextEffects,
          titles = nextTitles,
          syncCommit = true
      )
    }

    return UnlockedRewardNotification(
        newTrails = CosmeticTrail.entries.filter { it in nextTrails && it !in prevTrails },
        newEffects = VictoryEffect.entries.filter { it in nextEffects && it !in prevEffects },
        newTitles = PlayerTitle.entries.filter { it in nextTitles && it !in prevTitles }
    )
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
    val sessionEngine = _gameSession.value?.engine
    if (sessionEngine != null &&
        sessionEngine.launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION &&
        _currentScreen.value == AppScreen.GAMEPLAY &&
        !continuousCoordinator.isRunCompleted) {
      val persisted = continuousCoordinator.exportPersistedState(
          schemaVersion = SaveRepository.SAVE_SCHEMA_VERSION,
          isRunInForeground = true
      )
      _continuousPersistedState.value = persisted
      persistCriticalBoundary {
        saveRepository.saveContinuousRunState(persisted, syncCommit = true)
      }
    } else {
      flushPendingPersistence()
    }
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
    val effective = _isUserPaused.value ||
        _isHostPaused.value ||
        _isWindowFocusLost.value ||
        _isFieldNotesOpen.value ||
        _isOnboardingModalOpen.value
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
    persistCriticalBoundary {
      saveRepository.setContinuousRunInForeground(false, syncCommit = true)
    }
    audioEngine.setThemeAndIntensity(LevelTheme.WHISPERING_WOODS, AdaptiveTier.STANDARD)
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
    if (mode == RunLaunchMode.CONTINUOUS_PROGRESSION ||
        mode == RunLaunchMode.REPLAY ||
        mode == RunLaunchMode.MASTERY_CHALLENGE ||
        mode == RunLaunchMode.DAILY_CHALLENGE) {
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
    val retention = saveRepository.loadPhase4RetentionSnapshot()
    _retentionSnapshot.value = retention
    _isOnboardingCompleted.value = saveRepository.isOnboardingCompleted()
    _isVibrationEnabled.value = saveRepository.isVibrationEnabled()
    _selectedCharacter.value = saveRepository.getSelectedCharacter()
    _controlLayoutMode.value = saveRepository.getControlLayoutMode()
    _controlButtonSize.value = saveRepository.getControlButtonSize()
    _controlOpacity.value = saveRepository.getControlOpacity()
    _gameSession.value?.renderer?.selectedCharacter = _selectedCharacter.value
    _gameSession.value?.renderer?.equippedTrail = retention.rewards.equippedTrail
    _gameSession.value?.engine?.selectedCharacter = _selectedCharacter.value
    _gameSession.value?.engine?.activeVictoryEffect = retention.rewards.equippedEffect
    val musicEnabled = saveRepository.isMusicEnabled()
    val sfxEnabled = saveRepository.isSfxEnabled()
    val musicVol = saveRepository.getMusicVolume()
    val sfxVol = saveRepository.getSfxVolume()
    _isMusicEnabled.value = musicEnabled
    _isMusicMuted.value = !musicEnabled
    _isSfxEnabled.value = sfxEnabled
    _musicVolume.value = musicVol
    _sfxVolume.value = sfxVol
    audioEngine.isMusicEnabled = musicEnabled
    audioEngine.isSfxEnabled = sfxEnabled
    audioEngine.musicVolume = musicVol
    audioEngine.sfxVolume = sfxVol
  }

  fun openOnboardingModal() {
    _isFieldNotesOpen.value = false
    _isChapterTrailOpen.value = false
    _isOnboardingModalOpen.value = true
    audioEngine.playUiClick()
    syncEffectivePause()
  }

  fun closeOnboardingModal(markCompleted: Boolean = true) {
    _isOnboardingModalOpen.value = false
    if (markCompleted) {
      completeOnboarding(syncCommit = true)
    }
    audioEngine.playUiClick()
    syncEffectivePause()
  }

  fun completeOnboarding(syncCommit: Boolean = true) {
    val ok = saveRepository.setOnboardingCompleted(true, syncCommit = syncCommit)
    recordSaveOutcome(ok)
    _isOnboardingCompleted.value = true
    _hasDismissedFirstRunHint.value = true
    _isOnboardingModalOpen.value = false
    syncEffectivePause()
  }

  fun dismissFirstRunControlHint(markOnboardingCompleted: Boolean = true) {
    _hasDismissedFirstRunHint.value = true
    if (markOnboardingCompleted && !_isOnboardingCompleted.value) {
      val ok = saveRepository.setOnboardingCompleted(true, syncCommit = true)
      recordSaveOutcome(ok)
      _isOnboardingCompleted.value = true
    }
  }

  fun toggleMusic() {
    val newState = !audioEngine.isMusicEnabled
    audioEngine.isMusicEnabled = newState
    saveRepository.setMusicEnabled(newState)
    _isMusicEnabled.value = newState
    _isMusicMuted.value = !newState
  }

  fun toggleSfx() {
    val newState = !audioEngine.isSfxEnabled
    audioEngine.isSfxEnabled = newState
    saveRepository.setSfxEnabled(newState)
    _isSfxEnabled.value = newState
    if (newState) {
      audioEngine.playUiClick()
    }
  }

  fun setMusicVolume(vol: Float) {
    val clamped = vol.coerceIn(0f, 1f)
    audioEngine.musicVolume = clamped
    saveRepository.setMusicVolume(clamped)
    _musicVolume.value = clamped
  }

  fun setSfxVolume(vol: Float) {
    val clamped = vol.coerceIn(0f, 1f)
    audioEngine.sfxVolume = clamped
    saveRepository.setSfxVolume(clamped)
    _sfxVolume.value = clamped
  }

  fun toggleVibration() {
    setVibrationEnabled(!_isVibrationEnabled.value)
  }

  fun setVibrationEnabled(enabled: Boolean) {
    _isVibrationEnabled.value = enabled
    saveRepository.setVibrationEnabled(enabled)
    if (enabled) {
      triggerHaptic(HapticCue.JUMP)
    }
  }

  fun selectCharacter(character: PlayableCharacter, syncCommit: Boolean = true) {
    val ok = saveRepository.setSelectedCharacter(character, syncCommit = syncCommit)
    recordSaveOutcome(ok)
    _selectedCharacter.value = character
    _gameSession.value?.renderer?.selectedCharacter = character
    _gameSession.value?.engine?.selectedCharacter = character
    audioEngine.playUiClick()
  }

  fun setControlLayoutMode(mode: ControlLayoutMode, syncCommit: Boolean = true) {
    val ok = saveRepository.setControlLayoutMode(mode, syncCommit = syncCommit)
    recordSaveOutcome(ok)
    _controlLayoutMode.value = mode
    audioEngine.playUiClick()
  }

  fun setControlButtonSize(size: ControlButtonSize, syncCommit: Boolean = true) {
    val ok = saveRepository.setControlButtonSize(size, syncCommit = syncCommit)
    recordSaveOutcome(ok)
    _controlButtonSize.value = size
    audioEngine.playUiClick()
  }

  fun setControlOpacity(opacity: Float, syncCommit: Boolean = true) {
    val clamped = if (opacity.isFinite()) {
      opacity.coerceIn(SaveRepository.MIN_CONTROL_OPACITY, SaveRepository.MAX_CONTROL_OPACITY)
    } else {
      1.0f
    }
    val ok = saveRepository.setControlOpacity(clamped, syncCommit = syncCommit)
    recordSaveOutcome(ok)
    _controlOpacity.value = clamped
  }

  fun resetControlSettings(syncCommit: Boolean = true) {
    val ok = saveRepository.resetControlPreferences(syncCommit = syncCommit)
    recordSaveOutcome(ok)
    _controlLayoutMode.value = ControlLayoutMode.DEFAULT
    _controlButtonSize.value = ControlButtonSize.DEFAULT
    _controlOpacity.value = 1.0f
    audioEngine.playUiClick()
  }

  fun triggerHaptic(cue: HapticCue) {
    if (!_isVibrationEnabled.value) return
    val durationMs = when (cue) {
      HapticCue.JUMP -> 12L
      HapticCue.TRAP_IMPACT -> 24L
      HapticCue.DEATH -> 34L
      HapticCue.VICTORY -> 42L
    }
    try {
      val app = getApplication<Application>()
      val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val amp = when (cue) {
            HapticCue.JUMP -> 70
            HapticCue.TRAP_IMPACT -> 150
            HapticCue.DEATH -> 190
            HapticCue.VICTORY -> 160
          }
          vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amp))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(durationMs)
        }
      }
    } catch (_: Throwable) {
      // Safe fallback in headless test environments or devices without haptic motors
    }
  }

  override fun onCleared() {
    super.onCleared()
    touchControlState.clearAll()
    flushPendingPersistence()
    persistenceExecutor.shutdown()
    try {
      persistenceExecutor.awaitTermination(500L, TimeUnit.MILLISECONDS)
    } catch (_: InterruptedException) {
      // Ignore interruption on teardown
    }
    audioEngine.release()
  }
}
