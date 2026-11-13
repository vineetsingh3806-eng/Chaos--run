package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.audio.ProceduralAudioEngine
import com.example.data.SaveRepository
import com.example.data.World1Levels
import com.example.engine.ChaosGameEngine
import com.example.engine.ChaosGameRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen {
  MAIN_MENU,
  GAMEPLAY,
  HOW_TO_PLAY,
  SETTINGS
}

data class VictoryResult(
    val timeMs: Long,
    val coinsCollected: Int,
    val totalCoins: Int,
    val stars: Int
)

class GameStateViewModel(application: Application) : AndroidViewModel(application) {
  val saveRepository = SaveRepository(application)
  val audioEngine = ProceduralAudioEngine()

  private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _victoryResult = MutableStateFlow<VictoryResult?>(null)
  val victoryResult: StateFlow<VictoryResult?> = _victoryResult.asStateFlow()

  private val _isMusicMuted = MutableStateFlow(!saveRepository.isMusicEnabled())
  val isMusicMuted: StateFlow<Boolean> = _isMusicMuted.asStateFlow()

  var gameEngine: ChaosGameEngine? = null
  var gameRenderer: ChaosGameRenderer? = null

  init {
    audioEngine.isMusicEnabled = saveRepository.isMusicEnabled()
    audioEngine.isSfxEnabled = saveRepository.isSfxEnabled()
    audioEngine.musicVolume = saveRepository.getMusicVolume()
    audioEngine.sfxVolume = saveRepository.getSfxVolume()
    audioEngine.startMusic()
  }

  fun startLevel1() {
    val levelDef = World1Levels.createLevel1()
    gameRenderer = ChaosGameRenderer(getApplication())
    gameEngine = ChaosGameEngine(
        level = levelDef,
        audioEngine = audioEngine,
        onLevelCompleted = { timeMs, coinsCollected, totalCoins ->
          val stars = if (coinsCollected >= 9 && timeMs < 45000L) 3 else if (coinsCollected >= 5) 2 else 1
          saveRepository.saveBestTime(1, 1, timeMs)
          saveRepository.saveStars(1, 1, stars)
          _victoryResult.value = VictoryResult(timeMs, coinsCollected, totalCoins, stars)
        }
    )
    _victoryResult.value = null
    _currentScreen.value = AppScreen.GAMEPLAY
  }

  fun returnToMainMenu() {
    gameEngine?.isPaused = true
    _currentScreen.value = AppScreen.MAIN_MENU
  }

  fun showHowToPlay() {
    _currentScreen.value = AppScreen.HOW_TO_PLAY
  }

  fun showSettings() {
    _currentScreen.value = AppScreen.SETTINGS
  }

  fun restartLevel() {
    startLevel1()
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
    audioEngine.release()
  }
}
