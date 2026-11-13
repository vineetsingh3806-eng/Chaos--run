package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import com.example.ui.screens.GameplayView
import com.example.ui.screens.HowToPlayModal
import com.example.ui.screens.MainMenuView
import com.example.ui.screens.SettingsModal
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: GameStateViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
          val currentScreen by viewModel.currentScreen.collectAsState()

          when (currentScreen) {
            AppScreen.MAIN_MENU -> {
              MainMenuView(viewModel = viewModel)
            }
            AppScreen.GAMEPLAY -> {
              BackHandler {
                viewModel.returnToMainMenu()
              }
              GameplayView(viewModel = viewModel)
            }
            AppScreen.HOW_TO_PLAY -> {
              BackHandler {
                viewModel.returnToMainMenu()
              }
              HowToPlayModal(onBack = { viewModel.returnToMainMenu() })
            }
            AppScreen.SETTINGS -> {
              BackHandler {
                viewModel.returnToMainMenu()
              }
              SettingsModal(viewModel = viewModel, onBack = { viewModel.returnToMainMenu() })
            }
          }
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    viewModel.audioEngine.pause()
    viewModel.gameEngine?.isPaused = true
  }

  override fun onResume() {
    super.onResume()
    viewModel.audioEngine.resume()
  }

  override fun onDestroy() {
    super.onDestroy()
    viewModel.audioEngine.release()
  }
}
