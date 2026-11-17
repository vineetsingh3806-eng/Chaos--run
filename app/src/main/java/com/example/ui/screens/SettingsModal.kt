package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GameStateViewModel

@Composable
fun SettingsModal(
    viewModel: GameStateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  var musicEnabled by remember { mutableStateOf(viewModel.saveRepository.isMusicEnabled()) }
  var sfxEnabled by remember { mutableStateOf(viewModel.saveRepository.isSfxEnabled()) }
  var musicVolume by remember { mutableFloatStateOf(viewModel.saveRepository.getMusicVolume()) }
  var sfxVolume by remember { mutableFloatStateOf(viewModel.saveRepository.getSfxVolume()) }

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
            .widthIn(max = 520.dp)
            .fillMaxWidth(0.9f)
            .heightIn(max = 380.dp)
            .padding(12.dp)
    ) {
      Column(
          modifier = Modifier
              .padding(24.dp)
              .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = "AUDIO & SETTINGS",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFFBEB)
          )
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Music (Bilawal Raga)", color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Switch(
              checked = musicEnabled,
              onCheckedChange = {
                musicEnabled = it
                viewModel.toggleMusic()
              },
              modifier = Modifier.testTag("switch_music")
          )
        }
        Slider(
            value = musicVolume,
            onValueChange = {
              musicVolume = it
              viewModel.setMusicVolume(it)
            },
            valueRange = 0f..1f,
            enabled = musicEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Sound Effects (SFX)", color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Switch(
              checked = sfxEnabled,
              onCheckedChange = {
                sfxEnabled = it
                viewModel.toggleSfx()
              },
              modifier = Modifier.testTag("switch_sfx")
          )
        }
        Slider(
            value = sfxVolume,
            onValueChange = {
              sfxVolume = it
              viewModel.setSfxVolume(it)
            },
            valueRange = 0f..1f,
            enabled = sfxEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_settings_save")
        ) {
          Text("SAVE & RETURN", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF091318))
        }
      }
    }
  }
}
