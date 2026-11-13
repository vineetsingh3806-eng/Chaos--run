package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Local offline persistence for progress, stars, best speedrun records, and audio settings.
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
}
