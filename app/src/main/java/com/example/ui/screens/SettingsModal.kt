package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
          .background(Color(0xFF0F172A))
          .testTag("screen_settings"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFF59E0B)),
        modifier = Modifier
            .width(520.dp)
            .padding(16.dp)
    ) {
      Column(
          modifier = Modifier.padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = "AUDIO & SETTINGS",
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFFBEB)
          )
          IconButton(
              onClick = onBack,
              modifier = Modifier.testTag("button_settings_back")
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFFF59E0B)
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Music Toggle & Slider
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

        Spacer(modifier = Modifier.height(16.dp))

        // SFX Toggle & Slider
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D7377)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_settings_save")
        ) {
          Text("SAVE & RETURN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}
