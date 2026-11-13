package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.engine.GameSurfaceView
import com.example.ui.GameStateViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameplayView(
    viewModel: GameStateViewModel,
    modifier: Modifier = Modifier
) {
  val engine = viewModel.gameEngine ?: return
  val renderer = viewModel.gameRenderer ?: return
  val victoryResult by viewModel.victoryResult.collectAsState()

  var isPaused by remember { mutableStateOf(false) }

  // Coin counter & timer state polled from engine
  var displayedCoins by remember { mutableIntStateOf(0) }
  var displayedHealth by remember { mutableIntStateOf(3) }
  var displayedTimeSec by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(isPaused) {
    while (true) {
      if (!isPaused) {
        displayedCoins = engine.coins.count { it.isCollected }
        displayedHealth = engine.player.health
        displayedTimeSec = engine.levelTimerMs / 1000f
      }
      kotlinx.coroutines.delay(100L)
    }
  }

  Box(modifier = modifier.fillMaxSize().testTag("screen_gameplay")) {
    // 1. Dedicated SurfaceView Game Canvas
    AndroidView(
        factory = { ctx ->
          GameSurfaceView(ctx, engine, renderer)
        },
        modifier = Modifier.fillMaxSize()
    )

    // 2. Illustrated Adventure HUD (Top Bar)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      // Health Hearts Container
      Row(
          modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xCC1E293B))
              .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
              .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        repeat(3) { index ->
          Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = "Heart",
              tint = if (index < displayedHealth) Color(0xFFEF4444) else Color(0x6694A3B8),
              modifier = Modifier
                  .size(22.dp)
                  .padding(end = 4.dp)
          )
        }
      }

      // Golden Medallion Coin Counter
      Row(
          modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xCC1E293B))
              .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
              .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Coins",
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$displayedCoins / 10",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFFBEB)
        )
      }

      // Level Title Badge, Timer & Pause
      Row(
          verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC1E293B))
                .border(2.dp, Color(0xFF0D7377), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
              text = "1-1: Whispering Woods",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF67E8F9)
          )
          Text(
              text = "%.1fs".format(displayedTimeSec),
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE2E8F0)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Pause Button
        IconButton(
            onClick = {
              isPaused = true
              engine.isPaused = true
            },
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC334155))
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                .testTag("button_pause")
        ) {
          Icon(
              imageVector = Icons.Default.Pause,
              contentDescription = "Pause",
              tint = Color(0xFFFFFBEB),
              modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    // 3. Virtual Controls (Bottom Layer)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp, start = 28.dp, end = 28.dp)
    ) {
      // D-Pad: Left & Right Buttons
      Row(
          modifier = Modifier.align(Alignment.BottomStart)
      ) {
        // Left Button
        TouchControlButton(
            icon = Icons.Default.ArrowBack,
            description = "Left",
            testTag = "control_left",
            onTouchChange = { pressed -> engine.inputLeft = pressed }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Right Button
        TouchControlButton(
            icon = Icons.Default.ArrowForward,
            description = "Right",
            testTag = "control_right",
            onTouchChange = { pressed -> engine.inputRight = pressed }
        )
      }

      // Jump Button (Bottom-Right)
      TouchControlButton(
          icon = Icons.Default.ArrowUpward,
          description = "Jump",
          size = 76,
          accentColor = Color(0xFFF59E0B),
          testTag = "control_jump",
          onTouchChange = { pressed -> engine.inputJump = pressed },
          modifier = Modifier.align(Alignment.BottomEnd)
      )
    }

    // 4. Pause Dialog
    if (isPaused) {
      Dialog(onDismissRequest = {
        isPaused = false
        engine.isPaused = false
      }) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFF59E0B)),
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
                .testTag("dialog_pause")
        ) {
          Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
                text = "GAME PAUSED",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFDE047)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                  isPaused = false
                  engine.isPaused = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_resume")
            ) {
              Text("RESUME", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                  isPaused = false
                  viewModel.restartLevel()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_restart")
            ) {
              Text("RESTART LEVEL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                  isPaused = false
                  viewModel.returnToMainMenu()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_return_menu")
            ) {
              Text("RETURN TO MENU", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE2E8F0))
            }
          }
        }
      }
    }

    // 5. Victory Dialog Modal
    victoryResult?.let { result ->
      Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFD700)),
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp)
                .testTag("dialog_victory")
        ) {
          Column(
              modifier = Modifier.padding(26.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
                text = "SUN GATE UNLOCKED!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700)
            )
            Text(
                text = "1-1: Whispering Woods Complete",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Star Rating
            Row {
              repeat(3) { index ->
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (index < result.stars) Color(0xFFFFD700) else Color(0x6694A3B8),
                    modifier = Modifier
                        .size(42.dp)
                        .padding(4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("COINS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Text("${result.coinsCollected} / ${result.totalCoins}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFFBEB))
              }
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Text("%.1fs".format(result.timeMs / 1000f), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFFBEB))
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.returnToMainMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_victory_continue")
            ) {
              Text("CONTINUE TO MENU", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
          }
        }
      }
    }
  }
}

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    testTag: String,
    onTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 68,
    accentColor: Color = Color(0xFF0D7377)
) {
  var isPressed by remember { mutableStateOf(false) }

  Box(
      modifier = modifier
          .size(size.dp)
          .shadow(8.dp, CircleShape)
          .clip(CircleShape)
          .background(
              Brush.radialGradient(
                  colors = if (isPressed)
                    listOf(Color(0xEE475569), Color(0xDD1E293B))
                  else
                    listOf(Color(0xAA334155), Color(0x880F172A))
              )
          )
          .border(2.5.dp, if (isPressed) Color(0xFFFFD700) else accentColor, CircleShape)
          .pointerInteropFilter { event ->
            when (event.actionMasked) {
              MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                isPressed = true
                onTouchChange(true)
                true
              }
              MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                onTouchChange(false)
                true
              }
              else -> false
            }
          }
          .testTag(testTag),
      contentAlignment = Alignment.Center
  ) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = if (isPressed) Color(0xFFFFD700) else Color(0xFFF8FAFC),
        modifier = Modifier.size((size * 0.48).dp)
    )
  }
}
