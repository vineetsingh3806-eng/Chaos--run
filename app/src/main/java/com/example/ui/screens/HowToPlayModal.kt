package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
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
    onOpenOnboarding: (() -> Unit)? = null,
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
            .heightIn(max = 390.dp)
            .padding(10.dp)
    ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Fixed Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
              text = "HOW TO PLAY & HAZARD RULES",
              fontSize = 18.sp,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Middle Rules List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          GuideRuleRow(
              title = "CONTROLS, DOUBLE JUMP & MULTI-TOUCH",
              desc = "Hold Left or Right with one thumb while tapping Jump with the other. Tap Jump a second time in mid-air to execute a crisp Double Jump—two jumps total per jump, resetting when you land!"
          )

          GuideRuleRow(
              title = "200 HANDCRAFTED TROLL TRIALS (CH 1–200)",
              desc = "Survive all 200 continuous trials: sliding & splitting floors, opening ambush walls, reversing saws, vanishing bridges, spring traps, rising ember basins, rune beams, locked gates, mirror totems, gale gusts, and fleeing Sun Gates!"
          )

          GuideRuleRow(
              title = "FAST DEATH & INSTANT AUTO-RETRY",
              desc = "Traps eliminate Aarav in one quick hit and respawn you cleanly with every trap reset—memorize the gags, dodge the bait, and outsmart each trial!"
          )

          GuideRuleRow(
              title = "READING TELEGRAPHS & ADAPTIVE FAIRNESS",
              desc = "Spot subtle red glints and warning chimes before traps spring. Struggling on a tricky sequence unlocks helpful timing fairness automatically at your next retry!"
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pinned Bottom Action Row (always visible & clickable without scrolling)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (onOpenOnboarding != null) {
            OutlinedButton(
                onClick = onOpenOnboarding,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("button_how_to_play_onboarding")
            ) {
              Icon(
                  imageVector = Icons.Default.Explore,
                  contentDescription = null,
                  tint = Color(0xFFFDE047),
                  modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("VISUAL TOUR", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFFDE047))
            }
          }

          Button(
              onClick = onBack,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
          ) {
            Text("GOT IT, LET'S PLAY!", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF091318))
          }
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
