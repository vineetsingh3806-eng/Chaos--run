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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoryClueId

@Composable
fun FieldNotesModal(
    discoveredClues: Set<StoryClueId>,
    isResonanceStoneDiscovered: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
  val discoveredCount = discoveredClues.size
  val totalClues = StoryClueId.entries.size

  Box(
      modifier = modifier
          .fillMaxSize()
          .background(Color(0xDD060D10))
          .safeDrawingPadding()
          .testTag("modal_field_notes"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E24)),
        border = BorderStroke(2.5.dp, Color(0xFFF59E0B)),
        modifier = Modifier
            .widthIn(max = 660.dp)
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.88f)
    ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 22.dp, vertical = 18.dp)
      ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Field Notes",
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                  text = "FIELD NOTES • WHISPERING WOODS",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFFFBEB)
              )
              Text(
                  text = "Story Clues Discovered: $discoveredCount / $totalClues",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF67E8F9),
                  modifier = Modifier.testTag("text_field_notes_count")
              )
            }
          }

          IconButton(
              onClick = onClose,
              modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF1D313B))
                  .testTag("button_field_notes_back")
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close Field Notes",
                tint = Color(0xFFFDE047)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable clues + bonus discovery
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StoryClueId.entries.forEach { clue ->
            val found = discoveredClues.contains(clue)
            StoryClueCard(clue = clue, isDiscovered = found)
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Separate Bonus Environmental Discovery section (explicitly NOT a story clue)
          Text(
              text = "BONUS ENVIRONMENTAL DISCOVERY (OPTIONAL)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp,
              color = Color(0xFF38BDF8)
          )

          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(
                      Brush.horizontalGradient(
                          colors = if (isResonanceStoneDiscovered)
                            listOf(Color(0xFF102A36), Color(0xFF163846))
                          else
                            listOf(Color(0xFF0D181E), Color(0xFF132229))
                      )
                  )
                  .border(
                      1.5.dp,
                      if (isResonanceStoneDiscovered) Color(0xFF38BDF8) else Color(0xFF334155),
                      RoundedCornerShape(12.dp)
                  )
                  .padding(14.dp)
                  .testTag("bonus_resonance_stone_card"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                imageVector = if (isResonanceStoneDiscovered) Icons.Default.AutoAwesome else Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Resonance Stone",
                tint = if (isResonanceStoneDiscovered) Color(0xFF38BDF8) else Color(0xFF64748B),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                    text = "Resonance Stone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isResonanceStoneDiscovered) Color(0xFFE0F2FE) else Color(0xFF94A3B8)
                )
                Text(
                    text = if (isResonanceStoneDiscovered) "DISCOVERED" else "UNDISCOVERED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isResonanceStoneDiscovered) Color(0xFF38BDF8) else Color(0xFF64748B)
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                  text = if (isResonanceStoneDiscovered) {
                    "A glowing harmonic crystal resting in the lower basin beneath the Deceptive Step. Its gentle hum echoes the forest bells."
                  } else {
                    "Optional relic hidden in the mossy lower basin of Beat 3. Not counted among the 4 story clues."
                  },
                  fontSize = 12.sp,
                  lineHeight = 16.sp,
                  color = Color(0xFFCBD5E1)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StoryClueCard(
    clue: StoryClueId,
    isDiscovered: Boolean
) {
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(if (isDiscovered) Color(0xFF162A30) else Color(0xFF0C161B))
          .border(
              1.5.dp,
              if (isDiscovered) Color(0xFFF59E0B) else Color(0xFF283842),
              RoundedCornerShape(12.dp)
          )
          .padding(14.dp)
          .testTag("clue_card_${clue.number}"),
      verticalAlignment = Alignment.Top
  ) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isDiscovered) Color(0xFFF59E0B) else Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
      if (isDiscovered) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Discovered",
            tint = Color(0xFF091318),
            modifier = Modifier.size(20.dp)
        )
      } else {
        Text(
            text = "${clue.number}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF94A3B8)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
            text = if (isDiscovered) "Clue ${clue.number}: ${clue.title}" else "Clue ${clue.number}: Unrecorded",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (isDiscovered) Color(0xFFFDE047) else Color(0xFF94A3B8)
        )
        Text(
            text = clue.locationHint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDiscovered) Color(0xFF14B8A6) else Color(0xFF64748B)
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = if (isDiscovered) clue.fieldNote else "Explore Whispering Woods carefully to uncover this permanent story clue.",
          fontSize = 12.sp,
          lineHeight = 17.sp,
          color = if (isDiscovered) Color(0xFFE2E8F0) else Color(0xFF64748B)
      )
    }
  }
}
