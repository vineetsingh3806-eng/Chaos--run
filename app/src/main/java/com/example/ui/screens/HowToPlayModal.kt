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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HowToPlayModal(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  Box(
      modifier = modifier
          .fillMaxSize()
          .background(Color(0xFF091318))
          .safeDrawingPadding()
          .testTag("screen_how_to_play"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E24)),
        border = BorderStroke(2.5.dp, Color(0xFF14B8A6)),
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(0.92f)
            .heightIn(max = 380.dp)
            .padding(12.dp)
    ) {
      Column(
          modifier = Modifier
              .padding(22.dp)
              .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = "HOW TO PLAY & HAZARD RULES",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF67E8F9)
          )
          IconButton(
              onClick = onBack,
              modifier = Modifier
                  .size(48.dp)
                  .testTag("button_how_to_play_back")
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF67E8F9)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        GuideRuleRow(
            title = "CONTROLS & MULTI-TOUCH JUMPING",
            desc = "Hold Left or Right with one thumb while tapping or holding Jump with the other—running stays uninterrupted through takeoff and landing! Coyote time (80ms) and variable jump cut give crisp control."
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideRuleRow(
            title = "WORLD 1 TROLL TRIALS (1-1 TO 1-5)",
            desc = "Expect the unexpected across all 5 levels: disappearing floors, delayed crumbling planks, pop-up & moving spikes, fake safe bridges, fake checkpoints, falling ceilings, crush walls, blink platforms, and lying exits!"
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideRuleRow(
            title = "FAST DEATH & INSTANT AUTO-RETRY",
            desc = "Traps eliminate Aarav in one quick hit and respawn you cleanly with every trap reset—memorize the gags, dodge the bait, and outsmart each trial!"
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuideRuleRow(
            title = "READING TRAPS & FINAL SURPRISES",
            desc = "Spot subtle red telegraphs, don't blindly trust suspiciously flat 'safe' bridges or too-early exit doors, and keep jumping until you cross the true Sun Gate!"
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
          Text("GOT IT, LET'S PLAY!", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF091318))
        }
      }
    }
  }
}

@Composable
fun GuideRuleRow(title: String, desc: String) {
  Column(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF091318))
          .padding(12.dp)
  ) {
    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = desc, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 17.sp)
  }
}
