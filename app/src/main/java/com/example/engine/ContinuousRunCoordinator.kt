package com.example.engine

import com.example.data.AdaptiveMetricsState
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeDefinition
import com.example.data.ChallengeRecord
import com.example.data.ContinuousRunPersistedState
import com.example.data.LevelDefinition
import com.example.data.LockedChallengeConfig
import com.example.data.RunCompletionSummary

sealed class ChallengeCompletionOutcome {
  data class NextChallenge(
      val completedChallengeId: String,
      val completedRecord: ChallengeRecord,
      val nextConfig: LockedChallengeConfig,
      val nextAttemptToken: Long,
      val completedChallengeIds: Set<String>,
      val challengeRecords: Map<String, ChallengeRecord>,
      val cumulativeRunTimerMs: Long,
      val cumulativeDeaths: Int,
      val cumulativeScore: Int
  ) : ChallengeCompletionOutcome()

  data class RunCompleted(
      val completedChallengeId: String,
      val completedRecord: ChallengeRecord,
      val summary: RunCompletionSummary,
      val completedChallengeIds: Set<String>,
      val challengeRecords: Map<String, ChallengeRecord>
  ) : ChallengeCompletionOutcome()

  data object IgnoredDuplicate : ChallengeCompletionOutcome()
}

/**
 * Coordinates continuous progression across ChallengeCatalog entries (ch_01..ch_36),
 * duplicate completion protection, cumulative run statistics, and non-destructive records.
 */
class ContinuousRunCoordinator(
    private val catalog: List<ChallengeDefinition> = ChallengeCatalog.entries,
    initialRecords: Map<String, ChallengeRecord> = emptyMap(),
    bestRunTimeMs: Long = 0L,
    bestRunScore: Int = 0,
    bestRunLeastDeaths: Int = -1
) {
  var isRunActive: Boolean = false
    private set

  var isRunCompleted: Boolean = false
    private set

  var activeChallengeId: String = catalog.firstOrNull()?.challengeId ?: "ch_01"
    private set

  var activeLockedConfig: LockedChallengeConfig =
      catalog.firstOrNull()?.lockConfig(AdaptiveTier.STANDARD)
          ?: ChallengeCatalog.entries.first().lockConfig(AdaptiveTier.STANDARD)
    private set

  var currentAttemptToken: Long = 0L
    private set

  var currentChallengeDeaths: Int = 0
    private set

  val completedChallengeIds: MutableSet<String> = linkedSetOf()
  val challengeRecords: MutableMap<String, ChallengeRecord> = initialRecords.toMutableMap()

  var cumulativeRunTimerMs: Long = 0L
    private set

  var cumulativeDeaths: Int = 0
    private set

  var cumulativeScore: Int = 0
    private set

  var bestRunTimeMs: Long = bestRunTimeMs
    private set

  var bestRunScore: Int = bestRunScore
    private set

  var bestRunLeastDeaths: Int = bestRunLeastDeaths
    private set

  private val processedCompletionTokens = HashSet<Long>()

  val adaptiveController = AdaptiveDifficultyController()

  val currentAdaptiveTier: AdaptiveTier
    get() = adaptiveController.currentTier

  fun getAdaptiveMetricsState(): AdaptiveMetricsState = adaptiveController.exportState()

  fun startNewRun(
      preserveBestRecords: Boolean = true,
      existingRecords: Map<String, ChallengeRecord> = challengeRecords
  ): LockedChallengeConfig {
    val firstDef = catalog.firstOrNull() ?: ChallengeCatalog.entries.first()
    isRunActive = true
    isRunCompleted = false
    activeChallengeId = firstDef.challengeId
    currentAttemptToken++
    processedCompletionTokens.clear()
    currentChallengeDeaths = 0
    completedChallengeIds.clear()
    cumulativeRunTimerMs = 0L
    cumulativeDeaths = 0
    cumulativeScore = 0
    adaptiveController.resetForNewRun(startingChallengeId = firstDef.challengeId)
    if (preserveBestRecords) {
      for ((id, rec) in existingRecords) {
        mergeRecordLocked(id, rec)
      }
    } else {
      challengeRecords.clear()
    }
    activeLockedConfig = firstDef.lockConfig(AdaptiveTier.STANDARD)
    return activeLockedConfig
  }

  fun restoreFromPersisted(state: ContinuousRunPersistedState): LockedChallengeConfig {
    bestRunTimeMs = state.bestRunTimeMs.coerceAtLeast(0L)
    bestRunScore = state.bestRunScore.coerceAtLeast(0)
    bestRunLeastDeaths = state.bestRunLeastDeaths

    challengeRecords.clear()
    for ((id, rec) in state.challengeRecords) {
      if (ChallengeCatalog.findById(id) != null) {
        challengeRecords[id] = rec
      }
    }

    completedChallengeIds.clear()
    for (id in state.completedChallengeIds) {
      if (ChallengeCatalog.findById(id) != null) {
        completedChallengeIds.add(id)
      }
    }

    cumulativeRunTimerMs = state.cumulativeRunTimerMs.coerceAtLeast(0L)
    cumulativeDeaths = state.cumulativeDeaths.coerceAtLeast(0)
    cumulativeScore = state.cumulativeScore.coerceAtLeast(0)
    currentChallengeDeaths = state.currentChallengeDeaths.coerceAtLeast(0)

    isRunCompleted = state.isRunCompleted && completedChallengeIds.containsAll(catalog.map { it.challengeId })
    isRunActive = state.isRunActive && !isRunCompleted

    val resolvedDef = resolveSafeChallengeDefinition(state.activeChallengeId)
    activeChallengeId = resolvedDef.challengeId
    currentAttemptToken++
    processedCompletionTokens.clear()

    val isAlreadyCompleted = completedChallengeIds.contains(resolvedDef.challengeId)
    val restoredTier = if (isAlreadyCompleted && state.activeAdaptiveTier == AdaptiveTier.ASSISTED) {
      AdaptiveTier.STANDARD
    } else {
      state.activeAdaptiveTier
    }
    adaptiveController.restoreState(
        state = state.adaptiveMetrics,
        resolvedChallengeId = resolvedDef.challengeId,
        resolvedActiveTier = restoredTier,
        resolvedChallengeDeaths = currentChallengeDeaths
    )
    activeLockedConfig = resolvedDef.lockConfig(restoredTier)
    return activeLockedConfig
  }

  fun prepareRetryCurrentChallenge(): LockedChallengeConfig {
    val def = resolveSafeChallengeDefinition(activeChallengeId)
    activeChallengeId = def.challengeId
    currentAttemptToken++
    val isAlreadyCompleted = completedChallengeIds.contains(def.challengeId)
    val nextTier = adaptiveController.evaluateRetryBoundary(
        challengeId = def.challengeId,
        isAlreadyCompletedChallenge = isAlreadyCompleted
    )
    activeLockedConfig = def.lockConfig(nextTier)
    return activeLockedConfig
  }

  fun recordDeath(
      challengeId: String = activeChallengeId,
      challengeDeathsOverride: Int? = null,
      cumulativeDeathsOverride: Int? = null,
      trapCauseKey: String? = null
  ) {
    if (!isRunActive || isRunCompleted || challengeId != activeChallengeId) return
    currentChallengeDeaths = (challengeDeathsOverride ?: (currentChallengeDeaths + 1)).coerceAtLeast(0)
    cumulativeDeaths = (cumulativeDeathsOverride ?: (cumulativeDeaths + 1)).coerceAtLeast(currentChallengeDeaths)
    adaptiveController.recordDeath(
        challengeId = challengeId,
        causeKey = trapCauseKey,
        challengeDeathsOverride = currentChallengeDeaths,
        totalRunDeathsOverride = cumulativeDeaths
    )
  }

  fun onChallengeCompleted(
      challengeId: String,
      attemptToken: Long,
      timeMs: Long,
      score: Int,
      coinsCollected: Int,
      totalCoins: Int,
      parTimeSeconds: Int,
      isTrollLevel: Boolean,
      challengeDeaths: Int,
      engineCumulativeRunTimerMs: Long? = null,
      engineCumulativeDeaths: Int? = null
  ): ChallengeCompletionOutcome {
    if (!isRunActive || isRunCompleted) return ChallengeCompletionOutcome.IgnoredDuplicate
    if (challengeId != activeChallengeId) return ChallengeCompletionOutcome.IgnoredDuplicate
    if (attemptToken != currentAttemptToken || !processedCompletionTokens.add(attemptToken)) {
      return ChallengeCompletionOutcome.IgnoredDuplicate
    }

    val safeTimeMs = timeMs.coerceAtLeast(1L)
    val safeScore = score.coerceAtLeast(0)
    val safeChallengeDeaths = challengeDeaths.coerceAtLeast(0)

    val stars = computeStars(
        challengeId = challengeId,
        parTimeSeconds = parTimeSeconds,
        isTrollLevel = isTrollLevel,
        timeMs = safeTimeMs,
        coinsCollected = coinsCollected,
        totalCoins = totalCoins,
        deathsCount = safeChallengeDeaths
    )

    completedChallengeIds.add(challengeId)

    val incomingRecord = ChallengeRecord(
        challengeId = challengeId,
        completed = true,
        bestTimeMs = safeTimeMs,
        bestScore = safeScore,
        stars = stars,
        leastDeaths = safeChallengeDeaths
    )
    val mergedRecord = mergeRecordLocked(challengeId, incomingRecord)

    cumulativeRunTimerMs = if (engineCumulativeRunTimerMs != null) {
      maxOf(cumulativeRunTimerMs, engineCumulativeRunTimerMs.coerceAtLeast(safeTimeMs))
    } else {
      cumulativeRunTimerMs + safeTimeMs
    }
    cumulativeDeaths = if (engineCumulativeDeaths != null) {
      maxOf(cumulativeDeaths, engineCumulativeDeaths.coerceAtLeast(safeChallengeDeaths))
    } else {
      maxOf(cumulativeDeaths, safeChallengeDeaths)
    }
    cumulativeScore += safeScore

    val currentIndex = catalog.indexOfFirst { it.challengeId == challengeId }
    val nextDef = if (currentIndex >= 0 && currentIndex + 1 < catalog.size) {
      catalog[currentIndex + 1]
    } else {
      null
    }

    val tierUsedForAttempt = activeLockedConfig.selectedAdaptiveTier
    if (nextDef != null) {
      val nextTier = adaptiveController.evaluateTransitionBoundary(
          completedChallengeId = challengeId,
          nextChallengeId = nextDef.challengeId,
          deathsInChallenge = safeChallengeDeaths,
          timeMs = safeTimeMs,
          parTimeSeconds = parTimeSeconds,
          lockedTierUsedForAttempt = tierUsedForAttempt
      )
      activeChallengeId = nextDef.challengeId
      currentChallengeDeaths = 0
      currentAttemptToken++
      val nextLocked = nextDef.lockConfig(nextTier)
      activeLockedConfig = nextLocked
      return ChallengeCompletionOutcome.NextChallenge(
          completedChallengeId = challengeId,
          completedRecord = mergedRecord,
          nextConfig = nextLocked,
          nextAttemptToken = currentAttemptToken,
          completedChallengeIds = completedChallengeIds.toSet(),
          challengeRecords = challengeRecords.toMap(),
          cumulativeRunTimerMs = cumulativeRunTimerMs,
          cumulativeDeaths = cumulativeDeaths,
          cumulativeScore = cumulativeScore
      )
    }

    // Final challenge (ch_36) completed -> update rolling telemetry & complete run
    adaptiveController.evaluateTransitionBoundary(
        completedChallengeId = challengeId,
        nextChallengeId = null,
        deathsInChallenge = safeChallengeDeaths,
        timeMs = safeTimeMs,
        parTimeSeconds = parTimeSeconds,
        lockedTierUsedForAttempt = tierUsedForAttempt
    )
    isRunActive = false
    isRunCompleted = true
    currentChallengeDeaths = 0
    if (cumulativeRunTimerMs > 0L && (bestRunTimeMs <= 0L || cumulativeRunTimerMs < bestRunTimeMs)) {
      bestRunTimeMs = cumulativeRunTimerMs
    }
    if (cumulativeScore > bestRunScore) {
      bestRunScore = cumulativeScore
    }
    if (bestRunLeastDeaths < 0 || cumulativeDeaths < bestRunLeastDeaths) {
      bestRunLeastDeaths = cumulativeDeaths
    }

    return ChallengeCompletionOutcome.RunCompleted(
        completedChallengeId = challengeId,
        completedRecord = mergedRecord,
        summary = buildRunCompletionSummary(),
        completedChallengeIds = completedChallengeIds.toSet(),
        challengeRecords = challengeRecords.toMap()
    )
  }

  /**
   * Non-destructive update from REPLAY mode: only improves best record metrics;
   * never modifies cumulative run stats, active challenge progression, or completed flags.
   */
  fun recordReplayCompletionIfImproved(
      challengeId: String,
      timeMs: Long,
      score: Int,
      coinsCollected: Int,
      totalCoins: Int,
      parTimeSeconds: Int,
      isTrollLevel: Boolean,
      challengeDeaths: Int
  ): ChallengeRecord {
    val stars = computeStars(
        challengeId = challengeId,
        parTimeSeconds = parTimeSeconds,
        isTrollLevel = isTrollLevel,
        timeMs = timeMs.coerceAtLeast(1L),
        coinsCollected = coinsCollected,
        totalCoins = totalCoins,
        deathsCount = challengeDeaths.coerceAtLeast(0)
    )
    return mergeRecordLocked(
        challengeId = challengeId,
        incoming = ChallengeRecord(
            challengeId = challengeId,
            completed = true,
            bestTimeMs = timeMs.coerceAtLeast(1L),
            bestScore = score.coerceAtLeast(0),
            stars = stars,
            leastDeaths = challengeDeaths.coerceAtLeast(0)
        )
    )
  }

  fun buildRunCompletionSummary(): RunCompletionSummary {
    val orderedRecords = catalog.map { def ->
      challengeRecords[def.challengeId] ?: ChallengeRecord(
          challengeId = def.challengeId,
          completed = completedChallengeIds.contains(def.challengeId)
      )
    }
    val completedCount = completedChallengeIds.size.coerceAtMost(catalog.size)
    return RunCompletionSummary(
        completedCount = maxOf(completedCount, orderedRecords.count { it.completed }),
        totalChallenges = catalog.size,
        totalRunTimeMs = cumulativeRunTimerMs,
        totalDeaths = cumulativeDeaths,
        totalScore = cumulativeScore,
        bestRunTimeMs = bestRunTimeMs,
        bestRunScore = bestRunScore,
        bestRunLeastDeaths = bestRunLeastDeaths,
        challengeRecords = orderedRecords
    )
  }

  fun exportPersistedState(
      schemaVersion: Int,
      isRunInForeground: Boolean
  ): ContinuousRunPersistedState {
    return ContinuousRunPersistedState(
        schemaVersion = schemaVersion,
        isRunActive = isRunActive,
        isRunInForeground = isRunInForeground,
        isRunCompleted = isRunCompleted,
        activeChallengeId = activeChallengeId,
        activeChallengeSeed = activeLockedConfig.challengeSeed,
        activeConfigVersion = activeLockedConfig.configVersion,
        activeAdaptiveTier = activeLockedConfig.selectedAdaptiveTier,
        currentChallengeDeaths = currentChallengeDeaths,
        completedChallengeIds = completedChallengeIds.toSet(),
        challengeRecords = challengeRecords.toMap(),
        cumulativeRunTimerMs = cumulativeRunTimerMs,
        cumulativeDeaths = cumulativeDeaths,
        cumulativeScore = cumulativeScore,
        bestRunTimeMs = bestRunTimeMs,
        bestRunScore = bestRunScore,
        bestRunLeastDeaths = bestRunLeastDeaths,
        adaptiveMetrics = adaptiveController.exportState()
    )
  }

  private fun resolveSafeChallengeDefinition(candidateId: String): ChallengeDefinition {
    val byId = catalog.firstOrNull { it.challengeId == candidateId }
    if (byId != null && !completedChallengeIds.contains(byId.challengeId)) {
      return byId
    }
    val firstIncomplete = catalog.firstOrNull { !completedChallengeIds.contains(it.challengeId) }
    return firstIncomplete ?: byId ?: catalog.first()
  }

  private fun mergeRecordLocked(challengeId: String, incoming: ChallengeRecord): ChallengeRecord {
    val prev = challengeRecords[challengeId]
    val merged = if (prev == null) {
      incoming
    } else {
      val bestTime = when {
        prev.bestTimeMs <= 0L -> incoming.bestTimeMs.coerceAtLeast(0L)
        incoming.bestTimeMs <= 0L -> prev.bestTimeMs
        else -> minOf(prev.bestTimeMs, incoming.bestTimeMs)
      }
      val leastDeaths = when {
        prev.leastDeaths < 0 -> incoming.leastDeaths
        incoming.leastDeaths < 0 -> prev.leastDeaths
        else -> minOf(prev.leastDeaths, incoming.leastDeaths)
      }
      ChallengeRecord(
          challengeId = challengeId,
          completed = prev.completed || incoming.completed,
          bestTimeMs = bestTime,
          bestScore = maxOf(prev.bestScore, incoming.bestScore),
          stars = maxOf(prev.stars, incoming.stars),
          leastDeaths = leastDeaths
      )
    }
    challengeRecords[challengeId] = merged
    return merged
  }

  companion object {
    fun computeStars(
        challengeId: String,
        parTimeSeconds: Int,
        isTrollLevel: Boolean,
        timeMs: Long,
        coinsCollected: Int,
        totalCoins: Int,
        deathsCount: Int
    ): Int {
      return if (isTrollLevel) {
        val ratio = coinsCollected.toFloat() / totalCoins.coerceAtLeast(1)
        if (ratio >= 0.5f && timeMs < parTimeSeconds * 1000L && deathsCount <= 3) 3
        else if (ratio >= 0.45f || deathsCount <= 5) 2
        else 1
      } else if (challengeId == "ch_01") {
        if (coinsCollected >= 9 && timeMs < 45000L) 3 else if (coinsCollected >= 5) 2 else 1
      } else {
        val ratio = coinsCollected.toFloat() / totalCoins.coerceAtLeast(1)
        if (ratio >= 0.8f && timeMs < parTimeSeconds * 1000L) 3
        else if (ratio >= 0.45f) 2
        else 1
      }
    }

    fun computeStars(
        levelDef: LevelDefinition,
        timeMs: Long,
        coinsCollected: Int,
        totalCoins: Int,
        deathsCount: Int
    ): Int {
      val chId = "ch_${levelDef.levelId.toString().padStart(2, '0')}"
      return computeStars(
          challengeId = chId,
          parTimeSeconds = levelDef.parTimeSeconds,
          isTrollLevel = levelDef.isTrollLevel,
          timeMs = timeMs,
          coinsCollected = coinsCollected,
          totalCoins = totalCoins,
          deathsCount = deathsCount
      )
    }
  }
}
