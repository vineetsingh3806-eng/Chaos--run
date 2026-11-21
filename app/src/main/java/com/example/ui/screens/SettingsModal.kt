package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GameStateViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsModal(
    viewModel: GameStateViewModel,
    onBack: () -> Unit,
    onOpenOnboarding: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
  val musicEnabled by viewModel.isMusicEnabled.collectAsState()
  val sfxEnabled by viewModel.isSfxEnabled.collectAsState()
  val musicVolume by viewModel.musicVolume.collectAsState()
  val sfxVolume by viewModel.sfxVolume.collectAsState()
  val vibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
  val selectedCharacter by viewModel.selectedCharacter.collectAsState()

  Box(
      modifier = modifier
          .fillMaxSize()
          .background(Color(0xFF091318))
          .safeDrawingPadding()
          .testTag("screen_settings"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E24)),
        border = BorderStroke(2.5.dp, Color(0xFFF59E0B)),
        modifier = Modifier
            .widthIn(max = 800.dp)
            .fillMaxWidth(0.96f)
            .heightIn(max = 404.dp)
            .padding(4.dp)
    ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Fixed Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
                text = "SETTINGS & RUNNERS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFFBEB)
            )
            Text(
                text = "Pick your runner, tune audio mix & haptics",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
          }
          IconButton(
              onClick = onBack,
              modifier = Modifier
                  .size(48.dp)
                  .testTag("button_settings_back")
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFFF59E0B)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Adaptive Body: Side-by-side Runner Selection + Audio/Haptics on landscape; stacked on portrait
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
          val twoPane = maxWidth >= 560.dp
          if (twoPane) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Column(
                  modifier = Modifier
                      .weight(1.24f)
                      .fillMaxHeight()
                      .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                CharacterSelectionSection(
                    selectedCharacter = selectedCharacter,
                    onSelectCharacter = { viewModel.selectCharacter(it) },
                    modifier = Modifier.fillMaxWidth()
                )
              }

              Column(
                  modifier = Modifier
                      .weight(0.76f)
                      .fillMaxHeight()
                      .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                    text = "AUDIO & HAPTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFF67E8F9)
                )
                MusicMixCard(
                    musicEnabled = musicEnabled,
                    musicVolume = musicVolume,
                    onToggleMusic = { viewModel.toggleMusic() },
                    onVolumeChange = { viewModel.setMusicVolume(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                SfxMixCard(
                    sfxEnabled = sfxEnabled,
                    sfxVolume = sfxVolume,
                    onToggleSfx = { viewModel.toggleSfx() },
                    onVolumeChange = { viewModel.setSfxVolume(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                VibrationControlRow(
                    vibrationEnabled = vibrationEnabled,
                    onToggle = { viewModel.setVibrationEnabled(it) },
                    modifier = Modifier.fillMaxWidth()
                )
              }
            }
          } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CharacterSelectionSection(
                  selectedCharacter = selectedCharacter,
                  onSelectCharacter = { viewModel.selectCharacter(it) },
                  modifier = Modifier.fillMaxWidth()
              )
              MusicMixCard(
                  musicEnabled = musicEnabled,
                  musicVolume = musicVolume,
                  onToggleMusic = { viewModel.toggleMusic() },
                  onVolumeChange = { viewModel.setMusicVolume(it) },
                  modifier = Modifier.fillMaxWidth()
              )
              SfxMixCard(
                  sfxEnabled = sfxEnabled,
                  sfxVolume = sfxVolume,
                  onToggleSfx = { viewModel.toggleSfx() },
                  onVolumeChange = { viewModel.setSfxVolume(it) },
                  modifier = Modifier.fillMaxWidth()
              )
              VibrationControlRow(
                  vibrationEnabled = vibrationEnabled,
                  onToggle = { viewModel.setVibrationEnabled(it) },
                  modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pinned Bottom Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (onOpenOnboarding != null) {
            OutlinedButton(
                onClick = onOpenOnboarding,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFF0EA5E9)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("button_replay_onboarding")
            ) {
              Icon(
                  imageVector = Icons.Default.Explore,
                  contentDescription = null,
                  tint = Color(0xFF67E8F9),
                  modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("REPLAY GUIDE", fontWeight = FontWeight.Black, fontSize = 12.5.sp, color = Color(0xFF67E8F9))
            }
          }

          Button(
              onClick = onBack,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("button_settings_save")
          ) {
            Text("SAVE & RETURN", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF091318))
          }
        }
      }
    }
  }
}

@Composable
private fun MusicMixCard(
    musicEnabled: Boolean,
    musicVolume: Float,
    onToggleMusic: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF091318))
          .border(1.dp, Color(0xFF283D47), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
              text = "Music (Raga)",
              color = Color(0xFFE2E8F0),
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              maxLines = 1
          )
        }
        Text(
            text = if (musicEnabled) "${(musicVolume * 100f).roundToInt()}%" else "MUTED",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (musicEnabled) Color(0xFFFDE047) else Color(0xFF64748B)
        )
      }
      Slider(
          value = musicVolume,
          onValueChange = onVolumeChange,
          valueRange = 0f..1f,
          enabled = musicEnabled,
          colors = SliderDefaults.colors(
              thumbColor = Color(0xFFF59E0B),
              activeTrackColor = Color(0xFFF59E0B)
          ),
          modifier = Modifier.fillMaxWidth()
      )
    }
    Switch(
        checked = musicEnabled,
        onCheckedChange = { onToggleMusic() },
        modifier = Modifier.testTag("switch_music")
    )
  }
}

@Composable
private fun SfxMixCard(
    sfxEnabled: Boolean,
    sfxVolume: Float,
    onToggleSfx: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF091318))
          .border(1.dp, Color(0xFF283D47), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = null,
              tint = Color(0xFF14B8A6),
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
              text = "Sound Effects",
              color = Color(0xFFE2E8F0),
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              maxLines = 1
          )
        }
        Text(
            text = if (sfxEnabled) "${(sfxVolume * 100f).roundToInt()}%" else "MUTED",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (sfxEnabled) Color(0xFF67E8F9) else Color(0xFF64748B)
        )
      }
      Slider(
          value = sfxVolume,
          onValueChange = onVolumeChange,
          valueRange = 0f..1f,
          enabled = sfxEnabled,
          colors = SliderDefaults.colors(
              thumbColor = Color(0xFF14B8A6),
              activeTrackColor = Color(0xFF14B8A6)
          ),
          modifier = Modifier.fillMaxWidth()
      )
    }
    Switch(
        checked = sfxEnabled,
        onCheckedChange = { onToggleSfx() },
        modifier = Modifier.testTag("switch_sfx")
    )
  }
}

@Composable
private fun VibrationControlRow(
    vibrationEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF091318))
          .border(1.dp, Color(0xFF283D47), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(
          imageVector = Icons.Default.Vibration,
          contentDescription = null,
          tint = Color(0xFF10B981),
          modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Column {
        Text(
            text = "Vibration & Haptics",
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp
        )
        Text(
            text = "Jumps, trap hits & clears",
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )
      }
    }
    Switch(
        checked = vibrationEnabled,
        onCheckedChange = onToggle,
        modifier = Modifier.testTag("switch_vibration")
    )
  }
}

