package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.GameStateViewModel

@Composable
fun MainMenuView(
    viewModel: GameStateViewModel,
    modifier: Modifier = Modifier
) {
  val isMuted = viewModel.audioEngine.isMusicEnabled.not()
  val bestTime = viewModel.saveRepository.getBestTime(1, 1)
  val stars = viewModel.saveRepository.getStars(1, 1)

  // Subtle floating motion for title plate
  val infiniteTransition = rememberInfiniteTransition(label = "titleFloat")
  val titleOffset by infiniteTransition.animateFloat(
      initialValue = -3f,
      targetValue = 3f,
      animationSpec = infiniteRepeatable(
          animation = tween(2000, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse
      ),
      label = "titleY"
  )

  Box(
      modifier = modifier
          .fillMaxSize()
          .testTag("screen_main_menu")
  ) {
    // 1. Illustrated Widescreen Cover
    Image(
        painter = painterResource(id = R.drawable.img_chaos_run_concept_1794492614247),
        contentDescription = "Chaos Run Concept Cover",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    // Dark vignette gradient for menu readability
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xEE0F172A),
                        Color(0x880F172A),
                        Color(0x220F172A)
                    ),
                    startX = 0f,
                    endX = 1400f
                )
            )
    )

    // Top-Right Quick Sound Toggle
    IconButton(
        onClick = { viewModel.toggleMusic() },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(24.dp)
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC1E293B))
            .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
            .testTag("button_mute_toggle")
    ) {
      Icon(
          imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
          contentDescription = "Toggle Audio",
          tint = Color(0xFFFDE047),
          modifier = Modifier.size(28.dp)
      )
    }

    // Left Control Panel (Adventure Buttons & Game Title)
    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 48.dp)
            .width(380.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
      // Embossed Title Header
      Column(
          modifier = Modifier
              .offset(y = titleOffset.dp)
              .shadow(12.dp, RoundedCornerShape(16.dp))
              .clip(RoundedCornerShape(16.dp))
              .background(
                  Brush.verticalGradient(
                      colors = listOf(Color(0xFF5C3D2E), Color(0xFF3E2723))
                  )
              )
              .border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
              .padding(horizontal = 24.dp, vertical = 14.dp)
      ) {
        Text(
            text = "CHAOS RUN",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color(0xFFFFFBEB)
        )
        Text(
            text = "WORLD 1 • GREEN FOREST BENCHMARK",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color(0xFFFDE047)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Level 1 Progress Badge
      if (stars > 0 || bestTime > 0) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xAA1E293B))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          repeat(3) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < stars) Color(0xFFFFD700) else Color(0x6694A3B8),
                modifier = Modifier.size(18.dp)
            )
          }
          if (bestTime > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            val seconds = bestTime / 1000f
            Text(
                text = "Best: %.1fs".format(seconds),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0)
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // 1. PLAY BUTTON
      AdventureButton(
          text = "PLAY LEVEL 1-1",
          subtitle = "Whispering Woods",
          accentColor = Color(0xFF10B981),
          onClick = { viewModel.startLevel1() },
          testTag = "button_play_level_1"
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 2. HOW TO PLAY BUTTON
      AdventureButton(
          text = "HOW TO PLAY",
          subtitle = "Controls & Trap Rules",
          accentColor = Color(0xFF0D7377),
          onClick = { viewModel.showHowToPlay() },
          testTag = "button_how_to_play"
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 3. SETTINGS BUTTON
      AdventureButton(
          text = "AUDIO & SETTINGS",
          subtitle = "Volume & Preferences",
          accentColor = Color(0xFF475569),
          onClick = { viewModel.showSettings() },
          testTag = "button_settings"
      )
    }
  }
}

@Composable
fun AdventureButton(
    text: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
  Box(
      modifier = modifier
          .fillMaxWidth()
          .height(64.dp)
          .shadow(6.dp, RoundedCornerShape(12.dp))
          .clip(RoundedCornerShape(12.dp))
          .background(
              Brush.horizontalGradient(
                  colors = listOf(Color(0xFF334155), Color(0xFF1E293B))
              )
          )
          .border(2.dp, accentColor, RoundedCornerShape(12.dp))
          .clickable { onClick() }
          .testTag(testTag)
          .padding(horizontal = 18.dp, vertical = 8.dp),
      contentAlignment = Alignment.CenterStart
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFF8FAFC)
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )
      }
      Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(28.dp)
      )
    }
  }
}
