package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.data.CosmeticTrail
import com.example.data.MasteryCatalog
import com.example.data.PlayerTitle
import com.example.data.VictoryEffect
import com.example.ui.GameStateViewModel

enum class RewardCategoryTab(
    val id: String,
    val label: String
) {
  ALL("all", "ALL"),
  TRAILS("trails", "TRAILS"),
  EFFECTS("effects", "EFFECTS"),
  TITLES("titles", "TITLES"),
  MASTERY("mastery", "MASTERY")
}

/**
 * Dedicated Rewards screen containing ONLY reward-related content:
 * Cosmetic Runner Trails, Victory Gate Effects, Player Titles, and Mastery Reward milestones.
 * Contains NO gameplay mode launchers or character selection/preview clutter.
 */
@Composable
fun ProgressionHubScreen(
    viewModel: GameStateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  val retention by viewModel.retentionSnapshot.collectAsState()
  var selectedTab by rememberSaveable { mutableStateOf(RewardCategoryTab.ALL) }

  val totalRewardItems = CosmeticTrail.entries.size + VictoryEffect.entries.size + PlayerTitle.entries.size
  val unlockedRewardItems =
      retention.rewards.unlockedTrails.size +
          retention.rewards.unlockedEffects.size +
          retention.rewards.unlockedTitles.size

  Column(
      modifier = modifier
          .fillMaxSize()
          .background(
              Brush.verticalGradient(
                  colors = listOf(Color(0xFF071116), Color(0xFF0E2128), Color(0xFF091318))
              )
          )
          .safeDrawingPadding()
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .testTag("screen_progression_hub")
  ) {
    // Top Header Bar
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF13262E))
                .border(1.5.dp, Color(0xFF14B8A6), RoundedCornerShape(12.dp))
                .testTag("button_hub_back")
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color(0xFFFFFBEB)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
              text = "REWARDS",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.5.sp,
              color = Color(0xFFFFD700),
              modifier = Modifier.testTag("text_rewards_title")
          )
          val equippedTitle = retention.rewards.equippedTitle
          Text(
              text = "Title: ${equippedTitle.displayName}",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(equippedTitle.accentColorHex),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("hub_equipped_title_badge")
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF13262E))
                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("hub_mastery_stars_badge")
        ) {
          Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Mastery Stars",
              tint = Color(0xFFFFD700),
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = "${retention.totalMasteryStars}/${MasteryCatalog.totalPossibleStars}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFFBEB)
          )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF13262E))
                .border(1.dp, Color(0xFF14B8A6), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("hub_unlocked_rewards_badge")
        ) {
          Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = "Unlocked Rewards",
              tint = Color(0xFF67E8F9),
              modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = "$unlockedRewardItems/$totalRewardItems",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE0F2FE)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Reward Filter Tabs (>= 48dp touch height)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      for (tab in RewardCategoryTab.entries) {
        val selected = selectedTab == tab
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selected) Color(0xFFF59E0B) else Color(0xFF101E24),
            border = BorderStroke(1.5.dp, if (selected) Color(0xFFFDE047) else Color(0xFF1E3A44)),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { selectedTab = tab }
                .testTag("tab_rewards_${tab.id}")
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = tab.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) Color(0xFF091318) else Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Rewards Body (No gameplay modes or character previews)
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      RewardsContentSection(
          selectedTab = selectedTab,
          retention = retention,
          onEquipTrail = { viewModel.equipTrail(it) },
          onEquipEffect = { viewModel.equipVictoryEffect(it) },
          onEquipTitle = { viewModel.equipTitle(it) }
      )
    }
  }
}
