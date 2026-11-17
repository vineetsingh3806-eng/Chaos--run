package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.StoryClueId
import com.example.ui.AppScreen
import com.example.ui.GameStateViewModel
import com.example.ui.screens.ChapterTrailModal
import com.example.ui.screens.FieldNotesModal
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
      ChaosRunApp(viewModel = viewModel)
    }
  }

  override fun onPause() {
    super.onPause()
    viewModel.onHostPaused()
  }

  override fun onStop() {
    super.onStop()
    viewModel.onHostPaused()
  }

  override fun onResume() {
    super.onResume()
    viewModel.onHostResumed()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    viewModel.onWindowFocusChanged(hasFocus)
  }
}

@Composable
fun ChaosRunApp(viewModel: GameStateViewModel) {
  MyApplicationTheme {
    val currentScreen by viewModel.currentScreen.collectAsState()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (currentScreen == AppScreen.GAMEPLAY) Color.Transparent else Color(0xFF091318)
    ) {
      val isFieldNotesOpen by viewModel.isFieldNotesOpen.collectAsState()
      val isChapterTrailOpen by viewModel.isChapterTrailOpen.collectAsState()
      val isUserPaused by viewModel.isUserPaused.collectAsState()
      val victoryResult by viewModel.victoryResult.collectAsState()
      val discoveredClues by viewModel.discoveredClues.collectAsState()
      val isResonanceFound by viewModel.isResonanceDiscovered.collectAsState()
      val levelsProgress by viewModel.levelsProgress.collectAsState()

      Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
          AppScreen.MAIN_MENU -> {
            MainMenuView(viewModel = viewModel)
          }
          AppScreen.GAMEPLAY -> {
            BackHandler(
                enabled = !isFieldNotesOpen && !isChapterTrailOpen &&
                    victoryResult == null && !isUserPaused
            ) {
              viewModel.setUserPaused(true)
            }
            BackHandler(
                enabled = !isFieldNotesOpen && !isChapterTrailOpen &&
                    victoryResult == null && isUserPaused
            ) {
              viewModel.setUserPaused(false)
            }
            BackHandler(
                enabled = !isFieldNotesOpen && !isChapterTrailOpen &&
                    victoryResult != null
            ) {
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

        if (isChapterTrailOpen) {
          BackHandler {
            viewModel.closeChapterTrail()
          }
          ChapterTrailModal(
              levelsProgress = levelsProgress,
              cluesDiscovered = discoveredClues.size,
              totalClues = StoryClueId.entries.size,
              onPlayLevel = { levelId -> viewModel.startLevel(levelId) },
              onClose = { viewModel.closeChapterTrail() }
          )
        }

        if (isFieldNotesOpen) {
          BackHandler {
            viewModel.closeFieldNotes()
          }
          FieldNotesModal(
              discoveredClues = discoveredClues,
              isResonanceStoneDiscovered = isResonanceFound,
              onClose = { viewModel.closeFieldNotes() }
          )
        }
      }
    }
  }
}
