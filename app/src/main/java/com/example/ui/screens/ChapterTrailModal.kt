package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LevelProgressUi

@Composable
fun ChapterTrailModal(
    levelsProgress: List<LevelProgressUi>,
    cluesDiscovered: Int,
    totalClues: Int,
    onPlayLevel: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
  val unlockedCount = levelsProgress.count { it.isUnlocked }
  val totalStars = levelsProgress.sumOf { it.stars }
  val maxStars = levelsProgress.size * 3

  Box(
      modifier = modifier
          .fillMaxSize()
          .background(Color(0xDD060D10))
          .safeDrawingPadding()
          .testTag("modal_chapter_trail"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E24)),
        border = BorderStroke(2.5.dp, Color(0xFF14B8A6)),
        modifier = Modifier
            .widthIn(max = 660.dp)
            .fillMaxWidth(0.92f)
            .heightIn(max = 385.dp)
    ) {
      Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
      ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = "Chapter Trail",
                tint = Color(0xFF67E8F9),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                  text = "WORLD 1 • CHAPTER TRAIL",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFFFBEB)
              )
              Text(
                  text = "$unlockedCount/${levelsProgress.size} Levels Unlocked • $totalStars/$maxStars Stars",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFF94A3B8)
              )
            }
          }

          IconButton(
              onClick = onClose,
              modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1D313B))
                  .testTag("button_chapter_trail_back")
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close Chapter Trail",
                tint = Color(0xFF67E8F9)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Trail List for Levels 1-1 through 1-5
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
          levelsProgress.forEachIndexed { idx, level ->
            if (level.isUnlocked) {
              UnlockedLevelCard(
                  level = level,
                  cluesDiscovered = cluesDiscovered,
                  totalClues = totalClues,
                  onPlay = { onPlayLevel(level.levelId) }
              )
            } else {
              LockedLevelCard(level = level)
            }

            if (idx < levelsProgress.lastIndex) {
              Box(
                  modifier = Modifier
                      .padding(start = 24.dp)
                      .width(3.dp)
                      .height(10.dp)
                      .background(if (levelsProgress[idx + 1].isUnlocked) Color(0xFF10B981) else Color(0xFF334155))
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UnlockedLevelCard(
    level: LevelProgressUi,
    cluesDiscovered: Int,
    totalClues: Int,
    onPlay: () -> Unit
) {
  val accentColor = when (level.levelId) {
    1 -> Color(0xFF10B981)
    2 -> Color(0xFF2DD4BF)
    3 -> Color(0xFFF59E0B)
    4 -> Color(0xFF38BDF8)
    else -> Color(0xFFFACC15)
  }

  Row(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF162C33))
          .border(2.dp, accentColor, RoundedCornerShape(14.dp))
          .padding(horizontal = 14.dp, vertical = 11.dp)
          .testTag("card_level_1_${level.levelId}_unlocked"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
          text = "1-${level.levelId} • ${level.title.uppercase()}",
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFFBEB)
      )
      Text(
          text = level.mechanicTag,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF94A3B8)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { idx ->
          Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = if (idx < level.stars) Color(0xFFFFD700) else Color(0x5594A3B8),
              modifier = Modifier.size(15.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        val bestStr = if (level.bestTimeMs > 0L) "Best %.1fs".format(level.bestTimeMs / 1000f) else "Uncompleted"
        val extraStr = when {
          level.bestScore > 0 -> "${level.bestScore}pts"
          level.totalRelics > 0 -> "Relics ${level.relicsFound}/${level.totalRelics}"
          else -> level.subtitle
        }
        Text(
            text = "$bestStr • $extraStr",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Button(
        onClick = onPlay,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .height(48.dp)
            .testTag("button_trail_play_1_${level.levelId}")
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF091318))
      Spacer(modifier = Modifier.width(4.dp))
      Text(
          text = if (level.isCompleted) "REPLAY" else "PLAY",
          fontWeight = FontWeight.Black,
          color = Color(0xFF091318)
      )
    }
  }
}

@Composable
private fun LockedLevelCard(level: LevelProgressUi) {
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .alpha(0.80f)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF0C1519))
          .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
          .padding(horizontal = 14.dp, vertical = 11.dp)
          .testTag("card_level_1_${level.levelId}_locked"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
          modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF1E293B)),
          contentAlignment = Alignment.Center
      ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
            text = "1-${level.levelId} • ${level.title.uppercase()}",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = "Complete 1-${level.levelId - 1} to unlock • ${level.mechanicTag}",
            fontSize = 10.5.sp,
            color = Color(0xFF64748B)
        )
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF475569))
    ) {
      Text(
          text = "LOCKED",
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF94A3B8),
          modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("badge_level_1_${level.levelId}_locked")
      )
    }
  }
}
