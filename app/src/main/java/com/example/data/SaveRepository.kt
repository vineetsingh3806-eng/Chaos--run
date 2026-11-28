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

  // --- Vibration & Onboarding Preferences (Phase 5) ---

  fun isVibrationEnabled(): Boolean {
    return safeGetBoolean("pref_vibration_enabled", true)
  }

  fun setVibrationEnabled(enabled: Boolean, syncCommit: Boolean = false): Boolean {
    val editor = prefs.edit().putBoolean("pref_vibration_enabled", enabled)
    return commitOrApply(editor, syncCommit)
  }

  fun isOnboardingCompleted(): Boolean {
    return safeGetBoolean("pref_onboarding_completed", false)
  }

  fun setOnboardingCompleted(completed: Boolean, syncCommit: Boolean = false): Boolean {
    val editor = prefs.edit().putBoolean("pref_onboarding_completed", completed)
    return commitOrApply(editor, syncCommit)
  }

  // --- Character Selection Preferences (Phase 6) ---

  fun getSelectedCharacter(): PlayableCharacter {
    return PlayableCharacter.fromId(safeGetString(KEY_SELECTED_CHARACTER, PlayableCharacter.DEFAULT.id))
  }

  fun setSelectedCharacter(character: PlayableCharacter, syncCommit: Boolean = true): Boolean {
    val editor = prefs.edit().putString(KEY_SELECTED_CHARACTER, character.id)
    return commitOrApply(editor, syncCommit)
  }

  // --- Touch Control Customization Preferences (Phase 3) ---

  fun getControlLayoutMode(): ControlLayoutMode {
    return ControlLayoutMode.fromId(safeGetString(KEY_CONTROL_LAYOUT_MODE, ControlLayoutMode.DEFAULT.id))
  }

  fun setControlLayoutMode(mode: ControlLayoutMode, syncCommit: Boolean = true): Boolean {
    val editor = prefs.edit().putString(KEY_CONTROL_LAYOUT_MODE, mode.id)
    return commitOrApply(editor, syncCommit)
  }

  fun getControlButtonSize(): ControlButtonSize {
    return ControlButtonSize.fromId(safeGetString(KEY_CONTROL_BUTTON_SIZE, ControlButtonSize.DEFAULT.id))
  }

  fun setControlButtonSize(size: ControlButtonSize, syncCommit: Boolean = true): Boolean {
    val editor = prefs.edit().putString(KEY_CONTROL_BUTTON_SIZE, size.id)
    return commitOrApply(editor, syncCommit)
  }

  fun getControlOpacity(): Float {
    return safeGetFloat(KEY_CONTROL_OPACITY, 1.0f).coerceIn(MIN_CONTROL_OPACITY, MAX_CONTROL_OPACITY)
  }

  fun setControlOpacity(opacity: Float, syncCommit: Boolean = true): Boolean {
    val clamped = if (opacity.isFinite()) opacity.coerceIn(MIN_CONTROL_OPACITY, MAX_CONTROL_OPACITY) else 1.0f
    val editor = prefs.edit().putFloat(KEY_CONTROL_OPACITY, clamped)
    return commitOrApply(editor, syncCommit)
  }

  fun resetControlPreferences(syncCommit: Boolean = true): Boolean {
    val editor = prefs.edit()
        .putString(KEY_CONTROL_LAYOUT_MODE, ControlLayoutMode.DEFAULT.id)
        .putString(KEY_CONTROL_BUTTON_SIZE, ControlButtonSize.DEFAULT.id)
        .putFloat(KEY_CONTROL_OPACITY, 1.0f)
    return commitOrApply(editor, syncCommit)
  }

  // --- Phase 2 & Phase 4: Continuous Run + Retention Persistence, Migration (v1 -> v2 -> v3) ---

  companion object {
    const val SAVE_SCHEMA_VERSION = 3
    const val MIN_CONTROL_OPACITY = 0.45f
    const val MAX_CONTROL_OPACITY = 1.0f
    const val MAX_DAILY_HISTORY_DATES = 90

    private const val KEY_SELECTED_CHARACTER = "pref_selected_character_id"
    private const val KEY_CONTROL_LAYOUT_MODE = "pref_control_layout_mode"
    private const val KEY_CONTROL_BUTTON_SIZE = "pref_control_button_size"
    private const val KEY_CONTROL_OPACITY = "pref_control_opacity"
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

    // Phase 4 v3 additive retention keys (isolated from normal cr_rec_* & legacy best_* records)
    private const val KEY_P4_MASTERY_OBJECTIVES_JSON = "p4_mastery_objectives_json"
    private const val KEY_P4_MODIFIER_RECORDS_JSON = "p4_modifier_records_json"
    private const val KEY_P4_DAILY_COMPLETED_DATES = "p4_daily_completed_dates"
    private const val KEY_P4_DAILY_TOTAL_CLEARS = "p4_daily_total_clears"
    private const val KEY_P4_DAILY_LAST_SEEN_EPOCH = "p4_daily_last_seen_epoch"
    private const val KEY_P4_ZERO_DEATH_CHALLENGES = "p4_zero_death_challenges"
    private const val KEY_P4_COIN_GREEDY_EARNED = "p4_coin_greedy_earned"
    private const val KEY_P4_UNLOCKED_TRAILS = "p4_unlocked_trails"
    private const val KEY_P4_EQUIPPED_TRAIL = "p4_equipped_trail"
    private const val KEY_P4_UNLOCKED_EFFECTS = "p4_unlocked_effects"
    private const val KEY_P4_EQUIPPED_EFFECT = "p4_equipped_effect"
    private const val KEY_P4_UNLOCKED_TITLES = "p4_unlocked_titles"
    private const val KEY_P4_EQUIPPED_TITLE = "p4_equipped_title"
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

  private fun safeGetFloat(key: String, default: Float): Float {
    return try {
      val value = prefs.getFloat(key, default)
      if (value.isFinite()) value else default
    } catch (_: ClassCastException) {
      default
    }
  }

  fun getSchemaVersion(): Int = safeGetInt(KEY_CR_SCHEMA_VERSION, 1)

  /**
   * Idempotently migrates saves across versions:
   * - v1 -> v2: migrates legacy per-level keys (best_time_1_X, stars_1_X, best_score_1_X) into cr_rec_*
   * - v2 -> v3: initializes additive Phase 4 retention keys (mastery, modifiers, daily dates, cosmetics/titles)
   *   without deleting or altering any legacy or v2 continuous records.
   */
  @Synchronized
  fun migrateIfNeeded() {
    val currentVersion = safeGetInt(KEY_CR_SCHEMA_VERSION, 1)
    if (currentVersion >= SAVE_SCHEMA_VERSION) {
      return
    }

    val editor = prefs.edit()
    if (currentVersion < 2) {
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
    }

    // Additive v3 retention defaults (preserving any already-existing values)
    if (!prefs.contains(KEY_P4_UNLOCKED_TRAILS)) {
      editor.putString(KEY_P4_UNLOCKED_TRAILS, CosmeticTrail.NONE.id)
    }
    if (!prefs.contains(KEY_P4_EQUIPPED_TRAIL)) {
      editor.putString(KEY_P4_EQUIPPED_TRAIL, CosmeticTrail.NONE.id)
    }
    if (!prefs.contains(KEY_P4_UNLOCKED_EFFECTS)) {
      editor.putString(KEY_P4_UNLOCKED_EFFECTS, VictoryEffect.DEFAULT.id)
    }
    if (!prefs.contains(KEY_P4_EQUIPPED_EFFECT)) {
      editor.putString(KEY_P4_EQUIPPED_EFFECT, VictoryEffect.DEFAULT.id)
    }
    if (!prefs.contains(KEY_P4_UNLOCKED_TITLES)) {
      editor.putString(KEY_P4_UNLOCKED_TITLES, PlayerTitle.NONE.id)
    }
    if (!prefs.contains(KEY_P4_EQUIPPED_TITLE)) {
      editor.putString(KEY_P4_EQUIPPED_TITLE, PlayerTitle.NONE.id)
    }

    editor.putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
    editor.commit()
  }

  private fun commitOrApply(editor: SharedPreferences.Editor, syncCommit: Boolean): Boolean {
    return if (syncCommit) {
      try {
        editor.commit()
      } catch (_: Throwable) {
        false
      }
    } else {
      editor.apply()
      true
    }
  }

  fun isContinuousRunInForeground(): Boolean {
    return safeGetBoolean(KEY_CR_RUN_IN_FOREGROUND, false)
  }

  fun setContinuousRunInForeground(inForeground: Boolean, syncCommit: Boolean = true): Boolean {
    val editor = prefs.edit().putBoolean(KEY_CR_RUN_IN_FOREGROUND, inForeground)
    return commitOrApply(editor, syncCommit)
  }

  private fun populateContinuousRunEditor(
      editor: SharedPreferences.Editor,
      state: ContinuousRunPersistedState
  ) {
    editor
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
  }

  private fun populateLegacyRecordIfBetter(
      editor: SharedPreferences.Editor,
      challengeId: String,
      record: ChallengeRecord
  ) {
    val def = ChallengeCatalog.findById(challengeId) ?: return
    val legacyWorld = def.legacyWorldId ?: 1
    val legacyLevel = def.legacyLevelId ?: return
    if (record.bestTimeMs > 0L) {
      val prevTime = getBestTime(legacyWorld, legacyLevel)
      if (prevTime == 0L || record.bestTimeMs < prevTime) {
        editor.putLong("best_time_${legacyWorld}_$legacyLevel", record.bestTimeMs)
      }
    }
    if (record.stars > getStars(legacyWorld, legacyLevel)) {
      editor.putInt("stars_${legacyWorld}_$legacyLevel", record.stars)
    }
    if (record.bestScore > getBestScore(legacyWorld, legacyLevel)) {
      editor.putInt("best_score_${legacyWorld}_$legacyLevel", record.bestScore)
    }
  }

  @Synchronized
  fun saveContinuousRunState(
      state: ContinuousRunPersistedState,
      syncCommit: Boolean = true
  ): Boolean {
    val editor = prefs.edit()
    populateContinuousRunEditor(editor, state)
    return commitOrApply(editor, syncCommit)
  }

  @Synchronized
  fun saveChallengeCompletionBoundary(
      state: ContinuousRunPersistedState,
      completedChallengeId: String,
      record: ChallengeRecord,
      syncCommit: Boolean = true
  ): Boolean {
    val editor = prefs.edit()
    populateContinuousRunEditor(editor, state)
    populateLegacyRecordIfBetter(editor, completedChallengeId, record)
    return commitOrApply(editor, syncCommit)
  }

  /**
   * Non-destructive persistence helper for Replay mode:
   * only improves stored best records if strictly better; never modifies active run progress or cumulative stats.
   */
  @Synchronized
  fun saveReplayChallengeRecordIfImproved(
      challengeId: String,
      record: ChallengeRecord,
      syncCommit: Boolean = true
  ): Boolean {
    if (ChallengeCatalog.findById(challengeId) == null) return false
    val editor = prefs.edit().putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
    writeRecordIfBetterLocked(editor, challengeId, record)
    populateLegacyRecordIfBetter(editor, challengeId, record)
    return commitOrApply(editor, syncCommit)
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

  // --- Phase 4 Retention Persistence (Mastery, Modifiers, Daily Challenge, Cosmetics & Titles) ---

  @Synchronized
  fun loadPhase4RetentionSnapshot(): Phase4RetentionSnapshot {
    migrateIfNeeded()

    val mastery = loadMasteryProgressMapLocked()
    val modifiers = loadModifierRecordsMapLocked()
    val dailyDates = loadDailyCompletedDatesLocked()
    val dailyTotal = safeGetInt(KEY_P4_DAILY_TOTAL_CLEARS, dailyDates.size)
        .coerceAtLeast(dailyDates.size)
    val zeroDeathChallenges = parseCompletedIds(safeGetString(KEY_P4_ZERO_DEATH_CHALLENGES, ""))
        .filter { ChallengeCatalog.findById(it) != null }
        .toSet()
    val coinGreedyEarned = safeGetBoolean(KEY_P4_COIN_GREEDY_EARNED, false)

    val unlockedTrails = parseEnumSet(
        raw = safeGetString(KEY_P4_UNLOCKED_TRAILS, CosmeticTrail.NONE.id),
        defaultItem = CosmeticTrail.NONE,
        mapper = CosmeticTrail::fromId
    )
    val rawEquippedTrail = CosmeticTrail.fromId(safeGetString(KEY_P4_EQUIPPED_TRAIL, CosmeticTrail.NONE.id))
    val safeEquippedTrail = if (unlockedTrails.contains(rawEquippedTrail)) rawEquippedTrail else CosmeticTrail.NONE

    val unlockedEffects = parseEnumSet(
        raw = safeGetString(KEY_P4_UNLOCKED_EFFECTS, VictoryEffect.DEFAULT.id),
        defaultItem = VictoryEffect.DEFAULT,
        mapper = VictoryEffect::fromId
    )
    val rawEquippedEffect = VictoryEffect.fromId(safeGetString(KEY_P4_EQUIPPED_EFFECT, VictoryEffect.DEFAULT.id))
    val safeEquippedEffect = if (unlockedEffects.contains(rawEquippedEffect)) rawEquippedEffect else VictoryEffect.DEFAULT

    val unlockedTitles = parseEnumSet(
        raw = safeGetString(KEY_P4_UNLOCKED_TITLES, PlayerTitle.NONE.id),
        defaultItem = PlayerTitle.NONE,
        mapper = PlayerTitle::fromId
    )
    val rawEquippedTitle = PlayerTitle.fromId(safeGetString(KEY_P4_EQUIPPED_TITLE, PlayerTitle.NONE.id))
    val safeEquippedTitle = if (unlockedTitles.contains(rawEquippedTitle)) rawEquippedTitle else PlayerTitle.NONE

    return Phase4RetentionSnapshot(
        masteryByChallenge = mastery,
        modifierRecords = modifiers,
        dailyCompletedDates = dailyDates,
        dailyTotalClears = dailyTotal,
        zeroDeathChallenges = zeroDeathChallenges,
        coinGreedyEarned = coinGreedyEarned,
        rewards = RewardInventoryState(
            unlockedTrails = unlockedTrails,
            equippedTrail = safeEquippedTrail,
            unlockedEffects = unlockedEffects,
            equippedEffect = safeEquippedEffect,
            unlockedTitles = unlockedTitles,
            equippedTitle = safeEquippedTitle
        )
    )
  }

  /**
   * Records earned mastery objectives additively without downgrading any previously earned stars.
   */
  @Synchronized
  fun saveMasteryObjectivesEarned(
      challengeId: String,
      newlyEarnedObjectiveIds: Set<String>,
      syncCommit: Boolean = true
  ): Boolean {
    val spec = MasteryCatalog.specFor(challengeId) ?: return false
    val validIds = spec.objectives.map { it.id }.toSet()
    val filtered = newlyEarnedObjectiveIds.filter { validIds.contains(it) }.toSet()
    if (filtered.isEmpty()) return true

    val map = loadMasteryProgressMapLocked().toMutableMap()
    val existing = map[challengeId]?.completedObjectiveIds ?: emptySet()
    val merged = existing + filtered
    if (merged == existing) return true
    map[challengeId] = ChallengeMasteryProgress(challengeId, merged)

    val root = JSONObject()
    for ((chId, prog) in map) {
      if (prog.completedObjectiveIds.isEmpty()) continue
      val arr = JSONArray()
      for (objId in prog.completedObjectiveIds.sorted()) {
        arr.put(objId)
      }
      root.put(chId, arr)
    }
    val editor = prefs.edit()
        .putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
        .putString(KEY_P4_MASTERY_OBJECTIVES_JSON, root.toString())
    return commitOrApply(editor, syncCommit)
  }

  /**
   * Persists modifier run bests completely isolated from normal [ChallengeRecord] & legacy best_* keys.
   */
  @Synchronized
  fun saveModifierChallengeRecord(
      challengeId: String,
      modifiers: Set<ChaosModifier>,
      timeMs: Long,
      score: Int,
      deaths: Int,
      syncCommit: Boolean = true
  ): Boolean {
    if (ChallengeCatalog.findById(challengeId) == null || modifiers.isEmpty()) return false
    val modKey = ChaosModifier.canonicalKey(modifiers)
    val compositeKey = "${challengeId}::$modKey"
    val map = loadModifierRecordsMapLocked().toMutableMap()
    val prev = map[compositeKey]
    val safeTime = timeMs.coerceAtLeast(1L)
    val safeScore = score.coerceAtLeast(0)
    val safeDeaths = deaths.coerceAtLeast(0)
    val updated = if (prev == null) {
      ModifierChallengeRecord(
          challengeId = challengeId,
          modifierKey = modKey,
          bestTimeMs = safeTime,
          bestScore = safeScore,
          leastDeaths = safeDeaths,
          clearsCount = 1
      )
    } else {
      ModifierChallengeRecord(
          challengeId = challengeId,
          modifierKey = modKey,
          bestTimeMs = if (prev.bestTimeMs <= 0L) safeTime else minOf(prev.bestTimeMs, safeTime),
          bestScore = maxOf(prev.bestScore, safeScore),
          leastDeaths = if (prev.leastDeaths < 0) safeDeaths else minOf(prev.leastDeaths, safeDeaths),
          clearsCount = prev.clearsCount + 1
      )
    }
    map[compositeKey] = updated

    val root = JSONObject()
    for ((key, rec) in map) {
      val obj = JSONObject()
      obj.put("challengeId", rec.challengeId)
      obj.put("modifierKey", rec.modifierKey)
      obj.put("bestTimeMs", rec.bestTimeMs)
      obj.put("bestScore", rec.bestScore)
      obj.put("leastDeaths", rec.leastDeaths)
      obj.put("clearsCount", rec.clearsCount)
      root.put(key, obj)
    }
    val editor = prefs.edit()
        .putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
        .putString(KEY_P4_MODIFIER_RECORDS_JSON, root.toString())
    return commitOrApply(editor, syncCommit)
  }

  /**
   * Records a daily challenge completion idempotently.
   * Returns true if this [dateKey] was newly completed (false if already claimed or invalid/clock-tampered).
   */
  @Synchronized
  fun recordDailyCompletionIfNew(
      dateKey: String,
      syncCommit: Boolean = true
  ): Boolean {
    val parsedDate = OfflineCalendarDate.parseDateKey(dateKey) ?: return false
    val normalizedKey = parsedDate.toIsoDateKey()
    val existing = loadDailyCompletedDatesLocked().toMutableSet()
    if (existing.contains(normalizedKey)) {
      return false
    }
    existing.add(normalizedKey)
    val bounded = existing.sorted().takeLast(MAX_DAILY_HISTORY_DATES)
    val arr = JSONArray()
    for (d in bounded) arr.put(d)

    val prevClears = safeGetInt(KEY_P4_DAILY_TOTAL_CLEARS, existing.size - 1).coerceAtLeast(0)
    val lastEpoch = safeGetLong(KEY_P4_DAILY_LAST_SEEN_EPOCH, 0L)
    val nextEpoch = maxOf(lastEpoch, parsedDate.toEpochDayApprox())

    val editor = prefs.edit()
        .putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
        .putString(KEY_P4_DAILY_COMPLETED_DATES, arr.toString())
        .putInt(KEY_P4_DAILY_TOTAL_CLEARS, prevClears + 1)
        .putLong(KEY_P4_DAILY_LAST_SEEN_EPOCH, nextEpoch)
    commitOrApply(editor, syncCommit)
    return true
  }

  @Synchronized
  fun recordMilestoneFlags(
      zeroDeathChallengeId: String? = null,
      coinGreedyEarned: Boolean = false,
      syncCommit: Boolean = true
  ): Boolean {
    val editor = prefs.edit().putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
    var changed = false
    if (zeroDeathChallengeId != null && ChallengeCatalog.findById(zeroDeathChallengeId) != null) {
      val set = parseCompletedIds(safeGetString(KEY_P4_ZERO_DEATH_CHALLENGES, "")).toMutableSet()
      if (set.add(zeroDeathChallengeId)) {
        editor.putString(KEY_P4_ZERO_DEATH_CHALLENGES, formatCompletedIds(set))
        changed = true
      }
    }
    if (coinGreedyEarned && !safeGetBoolean(KEY_P4_COIN_GREEDY_EARNED, false)) {
      editor.putBoolean(KEY_P4_COIN_GREEDY_EARNED, true)
      changed = true
    }
    return if (changed) commitOrApply(editor, syncCommit) else true
  }

  @Synchronized
  fun saveUnlockedRewards(
      trails: Set<CosmeticTrail>,
      effects: Set<VictoryEffect>,
      titles: Set<PlayerTitle>,
      syncCommit: Boolean = true
  ): Boolean {
    val mergedTrails = (trails + CosmeticTrail.NONE).joinToString(",") { it.id }
    val mergedEffects = (effects + VictoryEffect.DEFAULT).joinToString(",") { it.id }
    val mergedTitles = (titles + PlayerTitle.NONE).joinToString(",") { it.id }
    val editor = prefs.edit()
        .putInt(KEY_CR_SCHEMA_VERSION, SAVE_SCHEMA_VERSION)
        .putString(KEY_P4_UNLOCKED_TRAILS, mergedTrails)
        .putString(KEY_P4_UNLOCKED_EFFECTS, mergedEffects)
        .putString(KEY_P4_UNLOCKED_TITLES, mergedTitles)
    return commitOrApply(editor, syncCommit)
  }

  @Synchronized
  fun setEquippedTrail(trail: CosmeticTrail, syncCommit: Boolean = true): Boolean {
    val unlocked = parseEnumSet(
        safeGetString(KEY_P4_UNLOCKED_TRAILS, CosmeticTrail.NONE.id),
        CosmeticTrail.NONE,
        CosmeticTrail::fromId
    )
    if (!unlocked.contains(trail)) return false
    return commitOrApply(prefs.edit().putString(KEY_P4_EQUIPPED_TRAIL, trail.id), syncCommit)
  }

  @Synchronized
  fun setEquippedVictoryEffect(effect: VictoryEffect, syncCommit: Boolean = true): Boolean {
    val unlocked = parseEnumSet(
        safeGetString(KEY_P4_UNLOCKED_EFFECTS, VictoryEffect.DEFAULT.id),
        VictoryEffect.DEFAULT,
        VictoryEffect::fromId
    )
    if (!unlocked.contains(effect)) return false
    return commitOrApply(prefs.edit().putString(KEY_P4_EQUIPPED_EFFECT, effect.id), syncCommit)
  }

  @Synchronized
  fun setEquippedTitle(title: PlayerTitle, syncCommit: Boolean = true): Boolean {
    val unlocked = parseEnumSet(
        safeGetString(KEY_P4_UNLOCKED_TITLES, PlayerTitle.NONE.id),
        PlayerTitle.NONE,
        PlayerTitle::fromId
    )
    if (!unlocked.contains(title)) return false
    return commitOrApply(prefs.edit().putString(KEY_P4_EQUIPPED_TITLE, title.id), syncCommit)
  }

  private fun loadMasteryProgressMapLocked(): Map<String, ChallengeMasteryProgress> {
    val raw = safeGetString(KEY_P4_MASTERY_OBJECTIVES_JSON, "")
    if (raw.isBlank()) return emptyMap()
    return try {
      val root = JSONObject(raw)
      val out = linkedMapOf<String, ChallengeMasteryProgress>()
      for (def in ChallengeCatalog.entries) {
        val arr = root.optJSONArray(def.challengeId) ?: continue
        val spec = MasteryCatalog.specFor(def.challengeId) ?: continue
        val validIds = spec.objectives.map { it.id }.toSet()
        val earned = linkedSetOf<String>()
        for (i in 0 until arr.length()) {
          val objId = arr.optString(i, "")
          if (validIds.contains(objId)) {
            earned.add(objId)
          }
        }
        if (earned.isNotEmpty()) {
          out[def.challengeId] = ChallengeMasteryProgress(def.challengeId, earned)
        }
      }
      out
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun loadModifierRecordsMapLocked(): Map<String, ModifierChallengeRecord> {
    val raw = safeGetString(KEY_P4_MODIFIER_RECORDS_JSON, "")
    if (raw.isBlank()) return emptyMap()
    return try {
      val root = JSONObject(raw)
      val out = linkedMapOf<String, ModifierChallengeRecord>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val obj = root.optJSONObject(key) ?: continue
        val chId = obj.optString("challengeId", "")
        if (ChallengeCatalog.findById(chId) == null) continue
        val rawModKey = obj.optString("modifierKey", "")
        val parsedMods = ChaosModifier.parseKey(rawModKey)
        if (parsedMods.isEmpty()) continue
        val canonicalModKey = ChaosModifier.canonicalKey(parsedMods)
        val bestTime = obj.optLong("bestTimeMs", 0L).coerceAtLeast(0L)
        val bestScore = obj.optInt("bestScore", 0).coerceAtLeast(0)
        val leastDeaths = obj.optInt("leastDeaths", -1).let { if (it < -1) -1 else it }
        val clears = obj.optInt("clearsCount", 1).coerceAtLeast(1)
        val canonicalComposite = "${chId}::$canonicalModKey"
        out[canonicalComposite] = ModifierChallengeRecord(
            challengeId = chId,
            modifierKey = canonicalModKey,
            bestTimeMs = bestTime,
            bestScore = bestScore,
            leastDeaths = leastDeaths,
            clearsCount = clears
        )
      }
      out
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun loadDailyCompletedDatesLocked(): Set<String> {
    val raw = safeGetString(KEY_P4_DAILY_COMPLETED_DATES, "")
    if (raw.isBlank()) return emptySet()
    return try {
      val arr = JSONArray(raw)
      val out = linkedSetOf<String>()
      for (i in 0 until arr.length()) {
        val parsed = OfflineCalendarDate.parseDateKey(arr.optString(i, ""))
        if (parsed != null) {
          out.add(parsed.toIsoDateKey())
        }
      }
      out
    } catch (_: Exception) {
      emptySet()
    }
  }

  private fun <T> parseEnumSet(
      raw: String,
      defaultItem: T,
      mapper: (String?) -> T
  ): Set<T> {
    val out = linkedSetOf(defaultItem)
    if (raw.isNotBlank()) {
      for (token in raw.split(',')) {
        if (token.isNotBlank()) {
          out.add(mapper(token))
        }
      }
    }
    return out
  }
}
