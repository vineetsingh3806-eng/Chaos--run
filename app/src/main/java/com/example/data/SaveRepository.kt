package com.example.data

import android.content.Context
import android.content.SharedPreferences

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

  // --- Difficulty Tier Preference ---

  fun getSelectedDifficulty(): DifficultyTier {
    val name = prefs.getString("pref_difficulty_tier", DifficultyTier.NORMAL.name)
    return DifficultyTier.entries.firstOrNull { it.name == name } ?: DifficultyTier.NORMAL
  }

  fun setSelectedDifficulty(tier: DifficultyTier) {
    prefs.edit().putString("pref_difficulty_tier", tier.name).apply()
  }
}
