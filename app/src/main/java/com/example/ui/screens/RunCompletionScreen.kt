package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChallengeCatalog
import com.example.data.ChallengeRecord
import com.example.data.RunCompletionSummary

@Composable
fun RunCompletionScreen(
    summary: RunCompletionSummary,
    onReplayChallenge: (String) -> Unit,
    onStartNewRun: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
  Box(
      modifier = modifier
          .fillMaxSize()
          .background(
              Brush.verticalGradient(
                  colors = listOf(Color(0xFF071116), Color(0xFF0E2128), Color(0xFF091318))
              )
          )
          .safeDrawingPadding()
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .testTag("screen_run_completion")
  ) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Left Column: Run Totals, Best Run Records, and Primary Actions
      Column(
          modifier = Modifier
              .weight(0.46f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(18.dp))
              .background(Color(0xDD0D1B21))
              .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(18.dp))
              .padding(16.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Run Complete Trophy",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                  text = "RUN COMPLETE!",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFFD700),
                  maxLines = 1
              )
              Text(
                  text = "${summary.completedCount}/${summary.totalChallenges} Challenges Cleared",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF67E8F9),
                  modifier = Modifier.testTag("text_run_completion_count")
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Current Run Cumulative Totals
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            RunStatTile(
                label = "TOTAL TIME",
                value = formatDuration(summary.totalRunTimeMs),
                accent = Color(0xFF38BDF8),
                testTag = "text_run_total_time",
                modifier = Modifier.weight(1f)
            )
            RunStatTile(
                label = "TOTAL DEATHS",
                value = "${summary.totalDeaths}",
                accent = Color(0xFFFCA5A5),
                testTag = "text_run_total_deaths",
                modifier = Modifier.weight(1f)
            )
            RunStatTile(
                label = "TOTAL SCORE",
                value = "${summary.totalScore}",
                accent = Color(0xFFFDE047),
                testTag = "text_run_total_score",
                modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Best All-Time Run Records
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF13262E))
                  .border(1.dp, Color(0xFF14B8A6), RoundedCornerShape(12.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp)
                  .testTag("text_run_best_records")
          ) {
            Text(
                text = "BEST RUN RECORDS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = Color(0xFF5EEAD4)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val bestTimeLabel = if (summary.bestRunTimeMs > 0L) formatDuration(summary.bestRunTimeMs) else "--"
            val bestDeathsLabel = if (summary.bestRunLeastDeaths >= 0) "${summary.bestRunLeastDeaths}" else "--"
            Text(
                text = "Best Time: $bestTimeLabel  •  Fewest Deaths: $bestDeathsLabel  •  High Score: ${summary.bestRunScore}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Primary CTAs: Start New Run & Main Menu
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
              onClick = onStartNewRun,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("button_start_new_run")
          ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF091318),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "START NEW RUN",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF091318)
            )
          }

          OutlinedButton(
              onClick = onMainMenu,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
              modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("button_run_completion_main_menu")
          ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MAIN MENU",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFDE047)
            )
          }
        }
      }

      // Right Column: Completed Challenges & Non-Destructive Replay
      Column(
          modifier = Modifier
              .weight(0.54f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(18.dp))
              .background(Color(0xDD0D1B21))
              .border(1.5.dp, Color(0xFF1E3A44), RoundedCornerShape(18.dp))
              .padding(14.dp)
      ) {
        Text(
            text = "COMPLETED CHALLENGES • NON-DESTRUCTIVE REPLAY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = Color(0xFF67E8F9)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(summary.challengeRecords, key = { it.challengeId }) { record ->
            ChallengeRecordRow(
                record = record,
                onReplay = { onReplayChallenge(record.challengeId) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RunStatTile(
    label: String,
    value: String,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF13242B))
          .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
          .padding(horizontal = 8.dp, vertical = 8.dp)
  ) {
    Text(
        text = label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        maxLines = 1
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = value,
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        color = accent,
        maxLines = 1,
        modifier = Modifier.testTag(testTag)
    )
  }
}

@Composable
private fun ChallengeRecordRow(
    record: ChallengeRecord,
    onReplay: () -> Unit
) {
  val def = ChallengeCatalog.findById(record.challengeId)
  val titleText = if (def != null) "${def.orderIndex}. ${def.title}" else record.challengeId
  val bestTimeText = if (record.bestTimeMs > 0L) formatDuration(record.bestTimeMs) else "--"
  val leastDeathsText = if (record.leastDeaths >= 0) "${record.leastDeaths}" else "--"

  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF132229))
          .border(
              1.dp,
              if (record.completed) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF334155),
              RoundedCornerShape(12.dp)
          )
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("record_row_${record.challengeId}")
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = if (record.completed) "Completed" else "Incomplete",
            tint = if (record.completed) Color(0xFF10B981) else Color(0xFF475569),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = titleText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        repeat(3) { idx ->
          Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = if (idx < record.stars) Color(0xFFFFD700) else Color(0x4494A3B8),
              modifier = Modifier.size(13.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
          text = "Best: $bestTimeText  •  Score: ${record.bestScore}  •  Min Deaths: $leastDeathsText",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF94A3B8),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Button(
        onClick = onReplay,
        enabled = record.completed,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("button_replay_${record.challengeId}")
    ) {
      Icon(
          imageVector = Icons.Default.Replay,
          contentDescription = "Replay ${record.challengeId}",
          tint = Color(0xFF091318),
          modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
          text = "REPLAY",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF091318)
      )
    }
  }
}

private fun formatDuration(ms: Long): String {
  val safeMs = ms.coerceAtLeast(0L)
  val totalTenths = safeMs / 100L
  val tenths = totalTenths % 10L
  val totalSeconds = totalTenths / 10L
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return if (minutes > 0L) {
    "%d:%02d.%ds".format(minutes, seconds, tenths)
  } else {
    "%d.%ds".format(seconds, tenths)
  }
}
