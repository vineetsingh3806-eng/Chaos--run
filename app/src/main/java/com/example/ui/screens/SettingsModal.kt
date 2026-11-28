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

/**
 * Minimal Settings screen containing ONLY:
 * - Background Music ON/OFF + Music Volume slider
 * - Sound Effects ON/OFF + Sound Effects Volume slider
 * - Vibration ON/OFF
 */
@Composable
fun SettingsModal(
    viewModel: GameStateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  val musicEnabled by viewModel.isMusicEnabled.collectAsState()
  val sfxEnabled by viewModel.isSfxEnabled.collectAsState()
  val musicVolume by viewModel.musicVolume.collectAsState()
  val sfxVolume by viewModel.sfxVolume.collectAsState()
  val vibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

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
        border = BorderStroke(2.dp, Color(0xFFF59E0B)),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(0.94f)
            .padding(6.dp)
    ) {
      Column(
          modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
                text = "SETTINGS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFFFFFBEB),
                modifier = Modifier.testTag("text_settings_title")
            )
            Text(
                text = "Music, sound effects & vibration",
                fontSize = 11.5.sp,
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

        // Scrollable controls body
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_settings_save")
        ) {
          Text(
              text = "SAVE & RETURN",
              fontWeight = FontWeight.Black,
              fontSize = 14.sp,
              color = Color(0xFF091318)
          )
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
  Column(
      modifier = modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF091318))
          .border(1.dp, Color(0xFF283D47), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
              text = "Background Music",
              color = Color(0xFFE2E8F0),
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp
          )
          Text(
              text = if (musicEnabled) "${(musicVolume * 100f).roundToInt()}%" else "OFF",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              color = if (musicEnabled) Color(0xFFFDE047) else Color(0xFF64748B)
          )
        }
      }
      Switch(
          checked = musicEnabled,
          onCheckedChange = { onToggleMusic() },
          modifier = Modifier.testTag("switch_music")
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_music_volume")
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
  Column(
      modifier = modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF091318))
          .border(1.dp, Color(0xFF283D47), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = Color(0xFF14B8A6),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
              text = "Sound Effects",
              color = Color(0xFFE2E8F0),
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp
          )
          Text(
              text = if (sfxEnabled) "${(sfxVolume * 100f).roundToInt()}%" else "OFF",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              color = if (sfxEnabled) Color(0xFF67E8F9) else Color(0xFF64748B)
          )
        }
      }
      Switch(
          checked = sfxEnabled,
          onCheckedChange = { onToggleSfx() },
          modifier = Modifier.testTag("switch_sfx")
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slider_sfx_volume")
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
          .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(
          imageVector = Icons.Default.Vibration,
          contentDescription = null,
          tint = Color(0xFF10B981),
          modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
            text = "Vibration",
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )
        Text(
            text = if (vibrationEnabled) "ON" else "OFF",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = if (vibrationEnabled) Color(0xFF34D399) else Color(0xFF64748B)
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
