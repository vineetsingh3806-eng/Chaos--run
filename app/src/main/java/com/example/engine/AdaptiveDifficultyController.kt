package com.example.engine

import com.example.data.AdaptiveMetricsState
import com.example.data.AdaptiveTier
import com.example.data.ChallengePerformanceRecord

/**
 * Local, deterministic adaptive difficulty controller for CHAOS RUN 5.
 *
 * Design guarantees:
 * - Fresh runs start in [AdaptiveTier.STANDARD].
 * - Active locked attempts are NEVER mutated mid-attempt or on death frame; telemetry is recorded
 *   immediately, while tier transitions occur exclusively at retry or challenge-completion boundaries.
 * - [AdaptiveTier.ASSISTED] activates at a retry boundary on >= 4 challenge deaths or >= 3 deaths
 *   to the same trap (never forced on already-completed challenges or replay runs).
 * - [AdaptiveTier.ESCALATED] and [AdaptiveTier.RELENTLESS] require conservative multi-challenge
 *   completion evidence in the rolling history window with hysteresis/cooldowns to prevent thrashing.
 */
class AdaptiveDifficultyController(
    initialState: AdaptiveMetricsState = AdaptiveMetricsState()
) {
  companion object {
    const val ASSISTED_CHALLENGE_DEATH_THRESHOLD = 4
    const val ASSISTED_SAME_TRAP_DEATH_THRESHOLD = 3
    const val HIGH_TIER_STEPDOWN_DEATHS = 2
    const val HIGH_TIER_STEPDOWN_SAME_TRAP = 2
    const val ESCALATED_CONSECUTIVE_STRONG_REQUIRED = 2
    const val ESCALATED_MAX_COMBINED_DEATHS_LAST_2 = 2
    const val RELENTLESS_WINDOW_SIZE = 4
    const val RELENTLESS_CONSECUTIVE_STRONG_REQUIRED = 4
    const val RELENTLESS_MAX_DEATHS_IN_WINDOW = 1
    const val MAX_ROLLING_HISTORY = 5
    const val STRONG_CLEAR_PAR_MULTIPLIER = 1.50f
    const val DOMINANT_CLEAR_PAR_MULTIPLIER = 1.25f
  }

  var currentTier: AdaptiveTier = initialState.currentTier
    private set

  var activeChallengeId: String = initialState.activeChallengeId
    private set

  var currentChallengeDeaths: Int = initialState.currentChallengeDeaths.coerceAtLeast(0)
    private set

  private val currentChallengeTrapDeaths = LinkedHashMap<String, Int>().apply {
    for ((k, v) in initialState.currentChallengeTrapDeaths) {
      if (k.isNotBlank() && v > 0) {
        put(k, v)
      }
    }
  }

  var totalRunDeaths: Int = initialState.totalRunDeaths.coerceAtLeast(0)
    private set

  var totalRunCompletions: Int = initialState.totalRunCompletions.coerceAtLeast(0)
    private set

  var consecutiveStrongCompletions: Int = initialState.consecutiveStrongCompletions.coerceAtLeast(0)
    private set

  var escalationCooldownChallenges: Int = initialState.escalationCooldownChallenges.coerceAtLeast(0)
    private set

  var demotionCooldownRetries: Int = initialState.demotionCooldownRetries.coerceAtLeast(0)
    private set

  private val recentCompletions = ArrayDeque<ChallengePerformanceRecord>().apply {
    initialState.recentCompletions.takeLast(MAX_ROLLING_HISTORY).forEach { addLast(it) }
  }

  fun getTrapDeathsMap(): Map<String, Int> = currentChallengeTrapDeaths.toMap()

  fun getRecentCompletions(): List<ChallengePerformanceRecord> = recentCompletions.toList()

  fun exportState(): AdaptiveMetricsState {
    return AdaptiveMetricsState(
        currentTier = currentTier,
        activeChallengeId = activeChallengeId,
        currentChallengeDeaths = currentChallengeDeaths,
        currentChallengeTrapDeaths = currentChallengeTrapDeaths.toMap(),
        totalRunDeaths = totalRunDeaths,
        totalRunCompletions = totalRunCompletions,
        consecutiveStrongCompletions = consecutiveStrongCompletions,
        escalationCooldownChallenges = escalationCooldownChallenges,
        demotionCooldownRetries = demotionCooldownRetries,
        recentCompletions = recentCompletions.toList()
    )
  }

  fun resetForNewRun(startingChallengeId: String = "ch_01") {
    currentTier = AdaptiveTier.STANDARD
    activeChallengeId = startingChallengeId
    currentChallengeDeaths = 0
    currentChallengeTrapDeaths.clear()
    totalRunDeaths = 0
    totalRunCompletions = 0
    consecutiveStrongCompletions = 0
    escalationCooldownChallenges = 0
    demotionCooldownRetries = 0
    recentCompletions.clear()
  }

  fun restoreState(
      state: AdaptiveMetricsState,
      resolvedChallengeId: String,
      resolvedActiveTier: AdaptiveTier,
      resolvedChallengeDeaths: Int
  ) {
    currentTier = resolvedActiveTier
    activeChallengeId = resolvedChallengeId
    val resetTrapMap = state.activeChallengeId == resolvedChallengeId
    currentChallengeDeaths = resolvedChallengeDeaths.coerceAtLeast(0)
    currentChallengeTrapDeaths.clear()
    if (resetTrapMap && currentChallengeDeaths > 0) {
      for ((k, v) in state.currentChallengeTrapDeaths) {
        if (k.isNotBlank() && v > 0) {
          currentChallengeTrapDeaths[k] = v.coerceAtMost(currentChallengeDeaths)
        }
      }
    }
    totalRunDeaths = state.totalRunDeaths.coerceAtLeast(currentChallengeDeaths)
    totalRunCompletions = state.totalRunCompletions.coerceAtLeast(0)
    consecutiveStrongCompletions = state.consecutiveStrongCompletions.coerceAtLeast(0)
    escalationCooldownChallenges = state.escalationCooldownChallenges.coerceAtLeast(0)
    demotionCooldownRetries = state.demotionCooldownRetries.coerceAtLeast(0)
    recentCompletions.clear()
    state.recentCompletions.takeLast(MAX_ROLLING_HISTORY).forEach { recentCompletions.addLast(it) }
  }

  /**
   * Records death telemetry for attribution & retry evaluation.
   * Deliberately does NOT modify [currentTier] so any active locked attempt stays immutable.
   */
  fun recordDeath(
      challengeId: String,
      causeKey: String? = null,
      challengeDeathsOverride: Int? = null,
      totalRunDeathsOverride: Int? = null
  ) {
    if (challengeId != activeChallengeId) return
    currentChallengeDeaths = if (challengeDeathsOverride != null) {
      maxOf(currentChallengeDeaths, challengeDeathsOverride.coerceAtLeast(0))
    } else {
      currentChallengeDeaths + 1
    }
    totalRunDeaths = if (totalRunDeathsOverride != null) {
      maxOf(totalRunDeaths, totalRunDeathsOverride.coerceAtLeast(currentChallengeDeaths))
    } else {
      maxOf(totalRunDeaths + 1, currentChallengeDeaths)
    }
    val normalizedCause = causeKey?.trim()?.takeIf { it.isNotEmpty() }
    if (normalizedCause != null && normalizedCause.startsWith("trap_")) {
      val prev = currentChallengeTrapDeaths[normalizedCause] ?: 0
      currentChallengeTrapDeaths[normalizedCause] = prev + 1
    }
  }

  /**
   * Selects the adaptive tier for the upcoming retry of [challengeId].
   * Invoked exclusively at retry boundaries (never mid-attempt).
   */
  fun evaluateRetryBoundary(
      challengeId: String,
      isAlreadyCompletedChallenge: Boolean
  ): AdaptiveTier {
    if (challengeId != activeChallengeId) {
      activeChallengeId = challengeId
      currentChallengeDeaths = 0
      currentChallengeTrapDeaths.clear()
      demotionCooldownRetries = 0
    }

    if (isAlreadyCompletedChallenge) {
      if (currentTier == AdaptiveTier.ASSISTED) {
        currentTier = AdaptiveTier.STANDARD
      }
      return currentTier
    }

    // Hysteresis: remain in ASSISTED for subsequent retries within the same struggling challenge
    if (currentTier == AdaptiveTier.ASSISTED) {
      return AdaptiveTier.ASSISTED
    }

    val maxSameTrapDeaths = currentChallengeTrapDeaths.values.maxOrNull() ?: 0
    if (currentChallengeDeaths >= ASSISTED_CHALLENGE_DEATH_THRESHOLD ||
        maxSameTrapDeaths >= ASSISTED_SAME_TRAP_DEATH_THRESHOLD) {
      currentTier = AdaptiveTier.ASSISTED
      consecutiveStrongCompletions = 0
      escalationCooldownChallenges = 1
      demotionCooldownRetries = 2
      return currentTier
    }

    if (demotionCooldownRetries > 0) {
      demotionCooldownRetries--
      return currentTier
    }

    if (currentChallengeDeaths >= HIGH_TIER_STEPDOWN_DEATHS ||
        maxSameTrapDeaths >= HIGH_TIER_STEPDOWN_SAME_TRAP) {
      when (currentTier) {
        AdaptiveTier.RELENTLESS -> {
          currentTier = AdaptiveTier.ESCALATED
          consecutiveStrongCompletions = 0
          escalationCooldownChallenges = 1
          demotionCooldownRetries = 1
        }
        AdaptiveTier.ESCALATED -> {
          currentTier = AdaptiveTier.STANDARD
          consecutiveStrongCompletions = 0
          escalationCooldownChallenges = 1
          demotionCooldownRetries = 1
        }
        AdaptiveTier.STANDARD, AdaptiveTier.ASSISTED -> Unit
      }
    }

    return currentTier
  }

  /**
   * Records a completed challenge attempt and resolves the tier for the next challenge.
   * Never mutates the just-completed attempt's configuration.
   */
  fun evaluateTransitionBoundary(
      completedChallengeId: String,
      nextChallengeId: String?,
      deathsInChallenge: Int,
      timeMs: Long,
      parTimeSeconds: Int,
      lockedTierUsedForAttempt: AdaptiveTier
  ): AdaptiveTier {
    val safeDeaths = deathsInChallenge.coerceAtLeast(0)
    val safeTimeMs = timeMs.coerceAtLeast(1L)
    val safeParSec = parTimeSeconds.coerceAtLeast(10)
    val usedAssisted = lockedTierUsedForAttempt == AdaptiveTier.ASSISTED

    val record = ChallengePerformanceRecord(
        challengeId = completedChallengeId,
        deaths = safeDeaths,
        timeMs = safeTimeMs,
        parTimeSeconds = safeParSec,
        tierUsed = lockedTierUsedForAttempt,
        usedAssisted = usedAssisted
    )
    if (recentCompletions.size >= MAX_ROLLING_HISTORY) {
      recentCompletions.removeFirst()
    }
    recentCompletions.addLast(record)
    totalRunCompletions++

    // Reset per-challenge death tracking before starting next challenge
    if (nextChallengeId != null) {
      activeChallengeId = nextChallengeId
    }
    currentChallengeDeaths = 0
    currentChallengeTrapDeaths.clear()
    demotionCooldownRetries = 0

    val parMs = safeParSec * 1000L
    val isStrongClear = !usedAssisted &&
        safeDeaths <= 1 &&
        safeTimeMs <= (parMs * STRONG_CLEAR_PAR_MULTIPLIER).toLong()

    if (usedAssisted) {
      // Conservative recovery back to STANDARD, requiring at least 1 clean clear before escalation
      currentTier = AdaptiveTier.STANDARD
      consecutiveStrongCompletions = 0
      escalationCooldownChallenges = 1
      return currentTier
    }

    if (safeDeaths >= 3) {
      consecutiveStrongCompletions = 0
      escalationCooldownChallenges = 1
      currentTier = when (currentTier) {
        AdaptiveTier.RELENTLESS -> AdaptiveTier.ESCALATED
        AdaptiveTier.ESCALATED -> AdaptiveTier.STANDARD
        AdaptiveTier.STANDARD, AdaptiveTier.ASSISTED -> AdaptiveTier.STANDARD
      }
      return currentTier
    }

    if (escalationCooldownChallenges > 0) {
      escalationCooldownChallenges--
      consecutiveStrongCompletions = if (isStrongClear) consecutiveStrongCompletions + 1 else 0
      return currentTier
    }

    consecutiveStrongCompletions = if (isStrongClear) {
      consecutiveStrongCompletions + 1
    } else {
      0
    }

    when (currentTier) {
      AdaptiveTier.ASSISTED -> {
        currentTier = AdaptiveTier.STANDARD
        escalationCooldownChallenges = 1
      }
      AdaptiveTier.STANDARD -> {
        if (recentCompletions.size >= 2 &&
            consecutiveStrongCompletions >= ESCALATED_CONSECUTIVE_STRONG_REQUIRED) {
          val lastTwo = recentCompletions.takeLast(2)
          val lastTwoStrong = lastTwo.all { rec ->
            !rec.usedAssisted &&
                rec.deaths <= 1 &&
                rec.timeMs <= (rec.parTimeSeconds.coerceAtLeast(10) * 1000L * STRONG_CLEAR_PAR_MULTIPLIER).toLong()
          }
          val combinedDeaths = lastTwo.sumOf { it.deaths }
          if (lastTwoStrong && combinedDeaths <= ESCALATED_MAX_COMBINED_DEATHS_LAST_2) {
            currentTier = AdaptiveTier.ESCALATED
            escalationCooldownChallenges = 1
          }
        }
      }
      AdaptiveTier.ESCALATED -> {
        if (recentCompletions.size >= RELENTLESS_WINDOW_SIZE &&
            consecutiveStrongCompletions >= RELENTLESS_CONSECUTIVE_STRONG_REQUIRED) {
          val lastFour = recentCompletions.takeLast(RELENTLESS_WINDOW_SIZE)
          val totalWindowDeaths = lastFour.sumOf { it.deaths }
          val noneAssisted = lastFour.none { it.usedAssisted }
          val lastTwoDominant = lastFour.takeLast(2).all { rec ->
            rec.deaths == 0 &&
                rec.timeMs <= (rec.parTimeSeconds.coerceAtLeast(10) * 1000L * DOMINANT_CLEAR_PAR_MULTIPLIER).toLong()
          }
          if (noneAssisted && totalWindowDeaths <= RELENTLESS_MAX_DEATHS_IN_WINDOW && lastTwoDominant) {
            currentTier = AdaptiveTier.RELENTLESS
            escalationCooldownChallenges = 1
          }
        }
      }
      AdaptiveTier.RELENTLESS -> Unit
    }

    return currentTier
  }
}
