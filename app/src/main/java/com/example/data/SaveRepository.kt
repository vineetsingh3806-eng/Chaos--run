package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local offline persistence for progress, stars, best speedrun records,
 * permanent story clues (1-4), bonus Resonance Stone discovery, and audio settings.
 */
class SaveRepository(context: Context) {
  private val prefs: SharedPreferences =
      context.getSharedPreferences("chaos_run_save_data", Context.MODE_PRIVATE)

  fun getBestTime(worldId: Int, levelId: Int): Long {
    return prefs.getLong("best_time_${worldId}_$levelId", 0L)
  }

  fun saveBestTime(worldId: Int, levelId: Int, timeMs: Long) {
    val current = getBestTime(worldId, levelId)
    if (current == 0L || timeMs < current) {
      prefs.edit().putLong("best_time_${worldId}_$levelId", timeMs).apply()
    }
  }

  fun getStars(worldId: Int, levelId: Int): Int {
    return prefs.getInt("stars_${worldId}_$levelId", 0)
  }

  fun saveStars(worldId: Int, levelId: Int, stars: Int) {
    val current = getStars(worldId, levelId)
    if (stars > current) {
      prefs.edit().putInt("stars_${worldId}_$levelId", stars).apply()
    }
  }

  fun getBestScore(worldId: Int, levelId: Int): Int {
    return prefs.getInt("best_score_${worldId}_$levelId", 0)
  }

  fun saveBestScore(worldId: Int, levelId: Int, score: Int) {
    val current = getBestScore(worldId, levelId)
    if (score > current) {
      prefs.edit().putInt("best_score_${worldId}_$levelId", score).apply()
    }
  }

  fun isLevelCompleted(worldId: Int, levelId: Int): Boolean {
    return getStars(worldId, levelId) > 0 || getBestTime(worldId, levelId) > 0L
  }

  fun isLevelUnlocked(worldId: Int, levelId: Int): Boolean {
    if (worldId != 1) return false
    if (levelId <= 1) return true
    if (levelId > 5) return false
    return isLevelCompleted(worldId, levelId - 1)
  }

  fun getHighestUnlockedLevel(worldId: Int = 1): Int {
    if (worldId != 1) return 1
    var highest = 1
    for (id in 2..5) {
      if (isLevelUnlocked(worldId, id)) highest = id else break
    }
    return highest
  }

  fun getCompletedLevelsCount(worldId: Int = 1): Int {
    if (worldId != 1) return 0
    return (1..5).count { isLevelCompleted(worldId, it) }
  }

  // --- Permanent Relics for Expansion Levels (1-2..1-5) ---

  fun isRelicDiscovered(relicId: String): Boolean {
    return prefs.getBoolean("relic_discovered_$relicId", false)
  }

  fun saveRelicDiscovered(relicId: String) {
    if (!isRelicDiscovered(relicId)) {
      prefs.edit().putBoolean("relic_discovered_$relicId", true).apply()
    }
  }

  fun getDiscoveredRelicIds(relicIds: List<String>): Set<String> {
    return relicIds.filter { isRelicDiscovered(it) }.toSet()
  }

  // --- Permanent Story Clues (1..4) ---

  fun isClueDiscovered(clueId: StoryClueId): Boolean {
    return prefs.getBoolean("clue_discovered_${clueId.number}", false)
  }

  fun saveClueDiscovered(clueId: StoryClueId) {
    if (!isClueDiscovered(clueId)) {
      prefs.edit().putBoolean("clue_discovered_${clueId.number}", true).apply()
    }
  }

  fun getDiscoveredClues(): Set<StoryClueId> {
    return StoryClueId.entries.filter { isClueDiscovered(it) }.toSet()
  }

  fun getDiscoveredClueCount(): Int {
    return StoryClueId.entries.count { isClueDiscovered(it) }
  }

  // --- Bonus Environmental Discovery: Resonance Stone ---

  fun isResonanceStoneDiscovered(): Boolean {
    return prefs.getBoolean("bonus_resonance_stone_discovered", false)
  }

  fun saveResonanceStoneDiscovered() {
    if (!isResonanceStoneDiscovered()) {
      prefs.edit().putBoolean("bonus_resonance_stone_discovered", true).apply()
    }
  }

  // --- Audio Preferences ---

  fun isMusicEnabled(): Boolean {
    return prefs.getBoolean("pref_music_enabled", true)
  }

  fun setMusicEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("pref_music_enabled", enabled).apply()
  }

  fun isSfxEnabled(): Boolean {
    return prefs.getBoolean("pref_sfx_enabled", true)
  }

  fun setSfxEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("pref_sfx_enabled", enabled).apply()
  }

  fun getMusicVolume(): Float {
    return prefs.getFloat("pref_music_volume", 0.8f)
  }

  fun setMusicVolume(volume: Float) {
    prefs.edit().putFloat("pref_music_volume", volume).apply()
  }

  fun getSfxVolume(): Float {
    return prefs.getFloat("pref_sfx_volume", 0.9f)
  }

  fun setSfxVolume(volume: Float) {
    prefs.edit().putFloat("pref_sfx_volume", volume).apply()
  }

  // --- Phase 2: Continuous Run Persistence, Migration (v1 -> v2) & Recovery ---

  companion object {
    const val SAVE_SCHEMA_VERSION = 2

    private const val KEY_CR_SCHEMA_VERSION = "cr_schema_version"
    private const val KEY_CR_RUN_ACTIVE = "cr_run_active"
    private const val KEY_CR_RUN_IN_FOREGROUND = "cr_run_in_foreground"
    private const val KEY_CR_RUN_COMPLETED = "cr_run_completed"
    private const val KEY_CR_ACTIVE_CHALLENGE_ID = "cr_active_challenge_id"
    private const val KEY_CR_ACTIVE_SEED = "cr_active_challenge_seed"
    private const val KEY_CR_ACTIVE_CONFIG_VERSION = "cr_active_config_version"
    private const val KEY_CR_ACTIVE_TIER = "cr_active_adaptive_tier"
    private const val KEY_CR_CHALLENGE_DEATHS = "cr_current_challenge_deaths"
    private const val KEY_CR_COMPLETED_IDS = "cr_completed_challenge_ids"
    private const val KEY_CR_CUMULATIVE_TIMER_MS = "cr_cumulative_timer_ms"
    private const val KEY_CR_CUMULATIVE_DEATHS = "cr_cumulative_deaths"
    private const val KEY_CR_CUMULATIVE_SCORE = "cr_cumulative_score"
    private const val KEY_CR_BEST_RUN_TIME_MS = "cr_best_run_time_ms"
    private const val KEY_CR_BEST_RUN_SCORE = "cr_best_run_score"
    private const val KEY_CR_BEST_RUN_LEAST_DEATHS = "cr_best_run_least_deaths"
    private const val KEY_CR_ADAPTIVE_CHALLENGE_ID = "cr_adaptive_challenge_id"
    private const val KEY_CR_ADAPTIVE_TOTAL_DEATHS = "cr_adaptive_total_deaths"
    private const val KEY_CR_ADAPTIVE_TOTAL_COMPLETIONS = "cr_adaptive_total_completions"
    private const val KEY_CR_ADAPTIVE_CONSEC_STRONG = "cr_adaptive_consec_strong"
    private const val KEY_CR_ADAPTIVE_ESCALATION_COOLDOWN = "cr_adaptive_escalation_cooldown"
    private const val KEY_CR_ADAPTIVE_DEMOTION_COOLDOWN = "cr_adaptive_demotion_cooldown"
    private const val KEY_CR_ADAPTIVE_TRAP_DEATHS = "cr_adaptive_trap_deaths"
    private const val KEY_CR_ADAPTIVE_RECENT_OUTCOMES = "cr_adaptive_recent_outcomes"
  }

  private fun recCompletedKey(challengeId: String) = "cr_rec_${challengeId}_completed"
  private fun recBestTimeKey(challengeId: String) = "cr_rec_${challengeId}_best_time"
  private fun recBestScoreKey(challengeId: String) = "cr_rec_${challengeId}_best_score"
  private fun recStarsKey(challengeId: String) = "cr_rec_${challengeId}_stars"
  private fun recLeastDeathsKey(challengeId: String) = "cr_rec_${challengeId}_least_deaths"

  private fun safeGetInt(key: String, default: Int): Int {
    return try {
      prefs.getInt(key, default)
    } catch (_: ClassCastException) {
      default
    }
  }

  private fun safeGetLong(key: String, default: Long): Long {
    return try {
      prefs.getLong(key, default)
    } catch (_: ClassCastException) {
      default
    }
  }

  private fun safeGetBoolean(key: String, default: Boolean): Boolean {
    return try {
      prefs.getBoolean(key, default)
    } catch (_: ClassCastException) {
      default
    }
  }

  private fun safeGetString(key: String, default: String): String {
    return try {
      prefs.getString(key, default) ?: default
    } catch (_: ClassCastException) {
      default
    }
  }

  fun getSchemaVersion(): Int = safeGetInt(KEY_CR_SCHEMA_VERSION, 1)

  /**
   * Idempotently migrates legacy v1 per-level keys (best_time_1_X, stars_1_X, best_score_1_X)
   * to schema version 2 without deleting legacy data or downgrading existing continuous records.
   */
  @Synchronized
  fun migrateIfNeeded() {
    val currentVersion = safeGetInt(KEY_CR_SCHEMA_VERSION, 1)
    if (currentVersion >= SAVE_SCHEMA_VERSION) {
      return
    }

    val editor = prefs.edit()
    val migratedCompleted = parseCompletedIds(safeGetString(KEY_CR_COMPLETED_IDS, ""))
        .toMutableSet()

    for (def in ChallengeCatalog.entries) {
      val legacyLevelId = def.legacyLevelId ?: continue
      val legacyWorldId = def.legacyWorldId ?: 1
      val legacyBestTime = safeGetLong("best_time_${legacyWorldId}_$legacyLevelId", 0L).coerceAtLeast(0L)
      val legacyStars = safeGetInt("stars_${legacyWorldId}_$legacyLevelId", 0).coerceIn(0, 3)
      val legacyBestScore = safeGetInt("best_score_${legacyWorldId}_$legacyLevelId", 0).coerceAtLeast(0)
      val legacyDone = legacyStars > 0 || legacyBestTime > 0L

      val existingCompleted = safeGetBoolean(recCompletedKey(def.challengeId), false)
      val existingTime = safeGetLong(recBestTimeKey(def.challengeId), 0L).coerceAtLeast(0L)
      val existingScore = safeGetInt(recBestScoreKey(def.challengeId), 0).coerceAtLeast(0)
      val existingStars = safeGetInt(recStarsKey(def.challengeId), 0).coerceIn(0, 3)
      val existingLeastDeaths = safeGetInt(recLeastDeathsKey(def.challengeId), -1)

      val mergedCompleted = existingCompleted || legacyDone
      val mergedTime = when {
        existingTime <= 0L -> legacyBestTime
        legacyBestTime <= 0L -> existingTime
        else -> minOf(existingTime, legacyBestTime)
      }
      val mergedScore = maxOf(existingScore, legacyBestScore)
      val mergedStars = maxOf(existingStars, legacyStars)

      if (mergedCompleted || mergedTime > 0L || mergedScore > 0 || mergedStars > 0) {
        editor.putBoolean(recCompletedKey(def.challengeId), mergedCompleted)
        editor.putLong(recBestTimeKey(def.challengeId), mergedTime)
        editor.putInt(recBestScoreKey(def.challengeId), mergedScore)
        editor.putInt(recStarsKey(def.challengeId), mergedStars)
        if (!prefs.contains(recLeastDeathsKey(def.challengeId))) {
          editor.putInt(recLeastDeathsKey(def.challengeId), existingLeastDeaths)
        }
      }
      if (legacyDone) {
        migratedCompleted.add(def.challengeId)
      }
    }

    if (!prefs.contains(KEY_CR_ACTIVE_CHALLENGE_ID)) {
      val firstUnfinished = ChallengeCatalog.entries
          .firstOrNull { !migratedCompleted.contains(it.challengeId) }
          ?.challengeId ?: ChallengeCatalog.entries.first().challengeId
      val resolvedDef = ChallengeCatalog.findById(firstUnfinished) ?: ChallengeCatalog.entries.first()
      editor.putString(KEY_CR_ACTIVE_CHALLENGE_ID, resolvedDef.challengeId)
      editor.putLong(KEY_CR_ACTIVE_SEED, resolvedDef.challengeSeed)
      editor.putInt(KEY_CR_ACTIVE_CONFIG_VERSION, resolvedDef.configVersion)
      editor.putString(KEY_CR_ACTIVE_TIER, AdaptiveTier.STANDARD.name)
    }

    if (!prefs.contains(KEY_CR_COMPLETED_IDS) || migratedCompleted.isNotEmpty()) {
      editor.putString(KEY_CR_COMPLETED_IDS, formatCompletedIds(migratedCompleted))
    }

    if (!prefs.contains(KEY_CR_RUN_ACTIVE)) {
      editor.putBoolean(KEY_CR_RUN_ACTIVE, false)
    }
    // Migrating from v1 must never fabricate a foreground active run recovery launch
    if (!prefs.contains(KEY_CR_RUN_IN_FOREGROUND)) {
      editor.putBoolean(KEY_CR_RUN_IN_FOREGROUND, false)
    }

    editor.putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
    editor.commit()
  }

  fun isContinuousRunInForeground(): Boolean {
    return safeGetBoolean(KEY_CR_RUN_IN_FOREGROUND, false)
  }

  fun setContinuousRunInForeground(inForeground: Boolean, syncCommit: Boolean = true) {
    val editor = prefs.edit().putBoolean(KEY_CR_RUN_IN_FOREGROUND, inForeground)
    if (syncCommit) editor.commit() else editor.apply()
  }

  fun saveContinuousRunState(
      state: ContinuousRunPersistedState,
      syncCommit: Boolean = true
  ) {
    val editor = prefs.edit()
        .putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
        .putBoolean(KEY_CR_RUN_ACTIVE, state.isRunActive)
        .putBoolean(KEY_CR_RUN_IN_FOREGROUND, state.isRunInForeground)
        .putBoolean(KEY_CR_RUN_COMPLETED, state.isRunCompleted)
        .putString(KEY_CR_ACTIVE_CHALLENGE_ID, state.activeChallengeId)
        .putLong(KEY_CR_ACTIVE_SEED, state.activeChallengeSeed)
        .putInt(KEY_CR_ACTIVE_CONFIG_VERSION, state.activeConfigVersion)
        .putString(KEY_CR_ACTIVE_TIER, state.activeAdaptiveTier.name)
        .putInt(KEY_CR_CHALLENGE_DEATHS, state.currentChallengeDeaths.coerceAtLeast(0))
        .putString(KEY_CR_COMPLETED_IDS, formatCompletedIds(state.completedChallengeIds))
        .putLong(KEY_CR_CUMULATIVE_TIMER_MS, state.cumulativeRunTimerMs.coerceAtLeast(0L))
        .putInt(KEY_CR_CUMULATIVE_DEATHS, state.cumulativeDeaths.coerceAtLeast(0))
        .putInt(KEY_CR_CUMULATIVE_SCORE, state.cumulativeScore.coerceAtLeast(0))
        .putLong(KEY_CR_BEST_RUN_TIME_MS, state.bestRunTimeMs.coerceAtLeast(0L))
        .putInt(KEY_CR_BEST_RUN_SCORE, state.bestRunScore.coerceAtLeast(0))
        .putInt(KEY_CR_BEST_RUN_LEAST_DEATHS, state.bestRunLeastDeaths)

    val metrics = state.adaptiveMetrics
    editor
        .putString(KEY_CR_ADAPTIVE_CHALLENGE_ID, metrics.activeChallengeId)
        .putInt(KEY_CR_ADAPTIVE_TOTAL_DEATHS, metrics.totalRunDeaths.coerceAtLeast(0))
        .putInt(KEY_CR_ADAPTIVE_TOTAL_COMPLETIONS, metrics.totalRunCompletions.coerceAtLeast(0))
        .putInt(KEY_CR_ADAPTIVE_CONSEC_STRONG, metrics.consecutiveStrongCompletions.coerceAtLeast(0))
        .putInt(KEY_CR_ADAPTIVE_ESCALATION_COOLDOWN, metrics.escalationCooldownChallenges.coerceAtLeast(0))
        .putInt(KEY_CR_ADAPTIVE_DEMOTION_COOLDOWN, metrics.demotionCooldownRetries.coerceAtLeast(0))
        .putString(KEY_CR_ADAPTIVE_TRAP_DEATHS, serializeTrapDeaths(metrics.currentChallengeTrapDeaths))
        .putString(KEY_CR_ADAPTIVE_RECENT_OUTCOMES, serializeRecentCompletions(metrics.recentCompletions))

    for ((id, rec) in state.challengeRecords) {
      if (ChallengeCatalog.findById(id) == null) continue
      writeRecordIfBetterLocked(editor, id, rec)
    }

    if (syncCommit) editor.commit() else editor.apply()
  }

  fun saveChallengeCompletionBoundary(
      state: ContinuousRunPersistedState,
      completedChallengeId: String,
      record: ChallengeRecord,
      syncCommit: Boolean = true
  ) {
    saveContinuousRunState(state, syncCommit = syncCommit)
    val def = ChallengeCatalog.findById(completedChallengeId) ?: return
    val legacyWorld = def.legacyWorldId ?: 1
    val legacyLevel = def.legacyLevelId ?: return
    if (record.bestTimeMs > 0L) {
      val prevTime = getBestTime(legacyWorld, legacyLevel)
      if (prevTime == 0L || record.bestTimeMs < prevTime) {
        val e = prefs.edit().putLong("best_time_${legacyWorld}_$legacyLevel", record.bestTimeMs)
        if (syncCommit) e.commit() else e.apply()
      }
    }
    if (record.stars > getStars(legacyWorld, legacyLevel)) {
      val e = prefs.edit().putInt("stars_${legacyWorld}_$legacyLevel", record.stars)
      if (syncCommit) e.commit() else e.apply()
    }
    if (record.bestScore > getBestScore(legacyWorld, legacyLevel)) {
      val e = prefs.edit().putInt("best_score_${legacyWorld}_$legacyLevel", record.bestScore)
      if (syncCommit) e.commit() else e.apply()
    }
  }

  /**
   * Non-destructive persistence helper for Replay mode:
   * only improves stored best records if strictly better; never modifies active run progress or cumulative stats.
   */
  fun saveReplayChallengeRecordIfImproved(
      challengeId: String,
      record: ChallengeRecord,
      syncCommit: Boolean = true
  ) {
    if (ChallengeCatalog.findById(challengeId) == null) return
    val editor = prefs.edit().putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
    writeRecordIfBetterLocked(editor, challengeId, record)
    if (syncCommit) editor.commit() else editor.apply()

    val def = ChallengeCatalog.findById(challengeId) ?: return
    val legacyWorld = def.legacyWorldId ?: 1
    val legacyLevel = def.legacyLevelId ?: return
    if (record.bestTimeMs > 0L) {
      val prevTime = getBestTime(legacyWorld, legacyLevel)
      if (prevTime == 0L || record.bestTimeMs < prevTime) {
        val e = prefs.edit().putLong("best_time_${legacyWorld}_$legacyLevel", record.bestTimeMs)
        if (syncCommit) e.commit() else e.apply()
      }
    }
    if (record.stars > getStars(legacyWorld, legacyLevel)) {
      val e = prefs.edit().putInt("stars_${legacyWorld}_$legacyLevel", record.stars)
      if (syncCommit) e.commit() else e.apply()
    }
    if (record.bestScore > getBestScore(legacyWorld, legacyLevel)) {
      val e = prefs.edit().putInt("best_score_${legacyWorld}_$legacyLevel", record.bestScore)
      if (syncCommit) e.commit() else e.apply()
    }
  }

  fun loadContinuousRunState(): ContinuousRunPersistedState {
    migrateIfNeeded()

    val completedIds = parseCompletedIds(safeGetString(KEY_CR_COMPLETED_IDS, ""))
        .filter { ChallengeCatalog.findById(it) != null }
        .toSet()

    val records = linkedMapOf<String, ChallengeRecord>()
    for (def in ChallengeCatalog.entries) {
      val id = def.challengeId
      val legacyLevelId = def.legacyLevelId ?: 0
      val legacyWorldId = def.legacyWorldId ?: 1
      val legacyBestTime = if (legacyLevelId > 0) safeGetLong("best_time_${legacyWorldId}_$legacyLevelId", 0L).coerceAtLeast(0L) else 0L
      val legacyStars = if (legacyLevelId > 0) safeGetInt("stars_${legacyWorldId}_$legacyLevelId", 0).coerceIn(0, 3) else 0
      val legacyScore = if (legacyLevelId > 0) safeGetInt("best_score_${legacyWorldId}_$legacyLevelId", 0).coerceAtLeast(0) else 0

      val recTime = safeGetLong(recBestTimeKey(id), 0L).coerceAtLeast(0L)
      val recStars = safeGetInt(recStarsKey(id), 0).coerceIn(0, 3)
      val recScore = safeGetInt(recBestScoreKey(id), 0).coerceAtLeast(0)
      val recCompleted = safeGetBoolean(recCompletedKey(id), false)
      val recLeastDeaths = safeGetInt(recLeastDeathsKey(id), -1).let { if (it < -1) -1 else it }

      val mergedCompleted = recCompleted || legacyStars > 0 || legacyBestTime > 0L
      val mergedTime = when {
        recTime <= 0L -> legacyBestTime
        legacyBestTime <= 0L -> recTime
        else -> minOf(recTime, legacyBestTime)
      }
      if (mergedCompleted || mergedTime > 0L || recScore > 0 || legacyScore > 0 || recStars > 0 || legacyStars > 0) {
        records[id] = ChallengeRecord(
            challengeId = id,
            completed = mergedCompleted,
            bestTimeMs = mergedTime,
            bestScore = maxOf(recScore, legacyScore),
            stars = maxOf(recStars, legacyStars),
            leastDeaths = recLeastDeaths
        )
      }
    }

    val allCatalogIds = ChallengeCatalog.entries.map { it.challengeId }
    val allCompleted = allCatalogIds.isNotEmpty() && completedIds.containsAll(allCatalogIds)
    val rawCompletedFlag = safeGetBoolean(KEY_CR_RUN_COMPLETED, false)
    val isRunCompleted = rawCompletedFlag && allCompleted

    val rawActiveId = safeGetString(KEY_CR_ACTIVE_CHALLENGE_ID, ChallengeCatalog.entries.first().challengeId)
    val candidateDef = ChallengeCatalog.findById(rawActiveId)
    val resolvedDef = when {
      candidateDef != null && !completedIds.contains(candidateDef.challengeId) -> candidateDef
      else -> ChallengeCatalog.entries.firstOrNull { !completedIds.contains(it.challengeId) }
          ?: candidateDef
          ?: ChallengeCatalog.entries.first()
    }

    val rawRunActive = safeGetBoolean(KEY_CR_RUN_ACTIVE, false)
    val isRunActive = rawRunActive && !isRunCompleted
    val isRunInForeground = isRunActive && safeGetBoolean(KEY_CR_RUN_IN_FOREGROUND, false)

    val persistedTierName = safeGetString(KEY_CR_ACTIVE_TIER, AdaptiveTier.STANDARD.name)
    val parsedTier = try {
      AdaptiveTier.valueOf(persistedTierName)
    } catch (_: IllegalArgumentException) {
      AdaptiveTier.STANDARD
    }
    val safeTier = if (completedIds.contains(resolvedDef.challengeId) && parsedTier == AdaptiveTier.ASSISTED) {
      AdaptiveTier.STANDARD
    } else {
      parsedTier
    }

    val rawConfigVersion = safeGetInt(KEY_CR_ACTIVE_CONFIG_VERSION, resolvedDef.configVersion)
    val rawSeed = safeGetLong(KEY_CR_ACTIVE_SEED, resolvedDef.challengeSeed)
    val safeSeed = if (rawConfigVersion == resolvedDef.configVersion && rawSeed == resolvedDef.challengeSeed) {
      rawSeed
    } else {
      resolvedDef.challengeSeed
    }

    val challengeDeaths = safeGetInt(KEY_CR_CHALLENGE_DEATHS, 0).coerceAtLeast(0)
    val cumTimerMs = safeGetLong(KEY_CR_CUMULATIVE_TIMER_MS, 0L).coerceAtLeast(0L)
    val cumDeaths = safeGetInt(KEY_CR_CUMULATIVE_DEATHS, 0).coerceAtLeast(challengeDeaths)
    val cumScore = safeGetInt(KEY_CR_CUMULATIVE_SCORE, 0).coerceAtLeast(0)
    val bestRunTime = safeGetLong(KEY_CR_BEST_RUN_TIME_MS, 0L).coerceAtLeast(0L)
    val bestRunScore = safeGetInt(KEY_CR_BEST_RUN_SCORE, 0).coerceAtLeast(0)
    val bestRunLeastDeaths = safeGetInt(KEY_CR_BEST_RUN_LEAST_DEATHS, -1).let { if (it < -1) -1 else it }

    val adaptiveMetrics = loadAdaptiveMetricsState(
        resolvedChallengeId = resolvedDef.challengeId,
        resolvedTier = safeTier,
        challengeDeaths = challengeDeaths,
        cumulativeDeaths = cumDeaths
    )

    return ContinuousRunPersistedState(
        schemaVersion = SAVE_SCHEMA_VERSION,
        isRunActive = isRunActive,
        isRunInForeground = isRunInForeground,
        isRunCompleted = isRunCompleted,
        activeChallengeId = resolvedDef.challengeId,
        activeChallengeSeed = safeSeed,
        activeConfigVersion = resolvedDef.configVersion,
        activeAdaptiveTier = safeTier,
        currentChallengeDeaths = challengeDeaths,
        completedChallengeIds = completedIds,
        challengeRecords = records,
        cumulativeRunTimerMs = cumTimerMs,
        cumulativeDeaths = cumDeaths,
        cumulativeScore = cumScore,
        bestRunTimeMs = bestRunTime,
        bestRunScore = bestRunScore,
        bestRunLeastDeaths = bestRunLeastDeaths,
        adaptiveMetrics = adaptiveMetrics
    )
  }

  private fun loadAdaptiveMetricsState(
      resolvedChallengeId: String,
      resolvedTier: AdaptiveTier,
      challengeDeaths: Int,
      cumulativeDeaths: Int
  ): AdaptiveMetricsState {
    val metricsChallengeId = safeGetString(KEY_CR_ADAPTIVE_CHALLENGE_ID, resolvedChallengeId)
    val sameChallenge = metricsChallengeId == resolvedChallengeId
    val trapDeaths = if (sameChallenge) {
      parseTrapDeaths(safeGetString(KEY_CR_ADAPTIVE_TRAP_DEATHS, ""))
    } else {
      emptyMap()
    }
    val recentOutcomes = parseRecentCompletions(safeGetString(KEY_CR_ADAPTIVE_RECENT_OUTCOMES, ""))
    return AdaptiveMetricsState(
        currentTier = resolvedTier,
        activeChallengeId = resolvedChallengeId,
        currentChallengeDeaths = challengeDeaths,
        currentChallengeTrapDeaths = trapDeaths,
        totalRunDeaths = safeGetInt(KEY_CR_ADAPTIVE_TOTAL_DEATHS, cumulativeDeaths).coerceAtLeast(cumulativeDeaths),
        totalRunCompletions = safeGetInt(KEY_CR_ADAPTIVE_TOTAL_COMPLETIONS, recentOutcomes.size).coerceAtLeast(0),
        consecutiveStrongCompletions = safeGetInt(KEY_CR_ADAPTIVE_CONSEC_STRONG, 0).coerceAtLeast(0),
        escalationCooldownChallenges = safeGetInt(KEY_CR_ADAPTIVE_ESCALATION_COOLDOWN, 0).coerceAtLeast(0),
        demotionCooldownRetries = safeGetInt(KEY_CR_ADAPTIVE_DEMOTION_COOLDOWN, 0).coerceAtLeast(0),
        recentCompletions = recentOutcomes
    )
  }

  private fun serializeTrapDeaths(map: Map<String, Int>): String {
    if (map.isEmpty()) return ""
    val obj = JSONObject()
    for ((key, count) in map) {
      if (key.isNotBlank() && count > 0) {
        obj.put(key, count)
      }
    }
    return obj.toString()
  }

  private fun parseTrapDeaths(raw: String): Map<String, Int> {
    if (raw.isBlank()) return emptyMap()
    return try {
      val obj = JSONObject(raw)
      val result = linkedMapOf<String, Int>()
      val iter = obj.keys()
      while (iter.hasNext()) {
        val key = iter.next()
        val count = obj.optInt(key, 0)
        if (key.isNotBlank() && count > 0) {
          result[key] = count
        }
      }
      result
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun serializeRecentCompletions(records: List<ChallengePerformanceRecord>): String {
    if (records.isEmpty()) return ""
    val arr = JSONArray()
    for (rec in records.takeLast(5)) {
      val obj = JSONObject()
      obj.put("challengeId", rec.challengeId)
      obj.put("deaths", rec.deaths.coerceAtLeast(0))
      obj.put("timeMs", rec.timeMs.coerceAtLeast(1L))
      obj.put("parTimeSeconds", rec.parTimeSeconds.coerceAtLeast(10))
      obj.put("tierUsed", rec.tierUsed.name)
      obj.put("usedAssisted", rec.usedAssisted)
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun parseRecentCompletions(raw: String): List<ChallengePerformanceRecord> {
    if (raw.isBlank()) return emptyList()
    return try {
      val arr = JSONArray(raw)
      val list = mutableListOf<ChallengePerformanceRecord>()
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val chId = obj.optString("challengeId", "")
        if (ChallengeCatalog.findById(chId) == null) continue
        val deaths = obj.optInt("deaths", -1)
        val timeMs = obj.optLong("timeMs", -1L)
        if (deaths < 0 || timeMs <= 0L) continue
        val parSec = obj.optInt("parTimeSeconds", 30).coerceAtLeast(10)
        val tierName = obj.optString("tierUsed", AdaptiveTier.STANDARD.name)
        val parsedTier = try {
          AdaptiveTier.valueOf(tierName)
        } catch (_: IllegalArgumentException) {
          AdaptiveTier.STANDARD
        }
        val usedAssisted = obj.optBoolean("usedAssisted", parsedTier == AdaptiveTier.ASSISTED)
        list.add(
            ChallengePerformanceRecord(
                challengeId = chId,
                deaths = deaths,
                timeMs = timeMs,
                parTimeSeconds = parSec,
                tierUsed = parsedTier,
                usedAssisted = usedAssisted
            )
        )
      }
      list.takeLast(5)
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun writeRecordIfBetterLocked(
      editor: SharedPreferences.Editor,
      challengeId: String,
      incoming: ChallengeRecord
  ) {
    val prevCompleted = safeGetBoolean(recCompletedKey(challengeId), false)
    val prevTime = safeGetLong(recBestTimeKey(challengeId), 0L).coerceAtLeast(0L)
    val prevScore = safeGetInt(recBestScoreKey(challengeId), 0).coerceAtLeast(0)
    val prevStars = safeGetInt(recStarsKey(challengeId), 0).coerceIn(0, 3)
    val prevLeastDeaths = safeGetInt(recLeastDeathsKey(challengeId), -1)

    val mergedCompleted = prevCompleted || incoming.completed
    val mergedTime = when {
      prevTime <= 0L -> incoming.bestTimeMs.coerceAtLeast(0L)
      incoming.bestTimeMs <= 0L -> prevTime
      else -> minOf(prevTime, incoming.bestTimeMs)
    }
    val mergedScore = maxOf(prevScore, incoming.bestScore.coerceAtLeast(0))
    val mergedStars = maxOf(prevStars, incoming.stars.coerceIn(0, 3))
    val mergedLeastDeaths = when {
      prevLeastDeaths < 0 -> incoming.leastDeaths
      incoming.leastDeaths < 0 -> prevLeastDeaths
      else -> minOf(prevLeastDeaths, incoming.leastDeaths)
    }

    editor.putBoolean(recCompletedKey(challengeId), mergedCompleted)
    editor.putLong(recBestTimeKey(challengeId), mergedTime)
    editor.putInt(recBestScoreKey(challengeId), mergedScore)
    editor.putInt(recStarsKey(challengeId), mergedStars)
    editor.putInt(recLeastDeathsKey(challengeId), mergedLeastDeaths)
  }

  private fun parseCompletedIds(raw: String): Set<String> {
    if (raw.isBlank()) return emptySet()
    return raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
  }

  private fun formatCompletedIds(ids: Set<String>): String {
    if (ids.isEmpty()) return ""
    val ordered = ChallengeCatalog.entries
        .map { it.challengeId }
        .filter { ids.contains(it) }
    return ordered.joinToString(",")
  }
}
