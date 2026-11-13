package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
          .background(Color(0xFF0F172A))
          .testTag("screen_how_to_play"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF0D7377)),
        modifier = Modifier
            .width(580.dp)
            .heightIn(max = 380.dp)
            .padding(16.dp)
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
              text = "HOW TO PLAY & HAZARD RULES",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF67E8F9)
          )
          IconButton(
              onClick = onBack,
              modifier = Modifier.testTag("button_how_to_play_back")
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF67E8F9)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guide entries
        GuideRuleRow(
            title = "CONTROLS & RUNNING",
            desc = "Use the Left and Right arrows to move. Hold Jump for high leaps, or tap quickly for short hops. Coyote time (80ms) lets you jump right after walking off an edge!"
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideRuleRow(
            title = "TREMBLING BRIDGES",
            desc = "Wooden plank bridges start trembling when you step on them and collapse after 0.75 seconds. Keep running to safely cross!"
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideRuleRow(
            title = "DECEPTIVE STEPS (DISAPPEARING FLOORS)",
            desc = "Ancient stone glyphs vanish the instant you land on them from above. Side brushes won't trigger them. Look for the safe landing basin below!"
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideRuleRow(
            title = "CHECKPOINT BELLS",
            desc = "Touch the brass bell to capture a snapshot of the world. If you fall, you will respawn deterministically at the bell!"
        )

        Spacer(modifier = Modifier.height(12.dp))

        GuideRuleRow(
            title = "MOVING PLATFORMS & SECRETS",
            desc = "Rhythmically board floating stone platforms. Look out for the secret high ruin ledge near the finish for bonus crest coins!"
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
          Text("GOT IT, LET'S PLAY!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
          .background(Color(0xFF0F172A))
          .padding(12.dp)
  ) {
    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = desc, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 17.sp)
  }
}
