package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CosmeticTrail
import com.example.data.Phase4RetentionSnapshot
import com.example.data.PlayerTitle
import com.example.data.VictoryEffect

@Composable
internal fun RewardsContentSection(
    selectedTab: RewardCategoryTab,
    retention: Phase4RetentionSnapshot,
    onEquipTrail: (CosmeticTrail) -> Unit,
    onEquipEffect: (VictoryEffect) -> Unit,
    onEquipTitle: (PlayerTitle) -> Unit
) {
  val rewards = retention.rewards
  Column(
      modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .testTag("rewards_scroll_container"),
      verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (selectedTab == RewardCategoryTab.ALL || selectedTab == RewardCategoryTab.TRAILS) {
      RewardSectionHeader(
          title = "COSMETIC RUNNER TRAILS",
          color = Color(0xFF67E8F9)
      )
      for (trail in CosmeticTrail.entries) {
        val unlocked = rewards.unlockedTrails.contains(trail)
        val equipped = rewards.equippedTrail == trail
        RewardItemCard(
            title = trail.displayName,
            subtitle = trail.description,
            unlockHint = trail.unlockHint,
            accent = Color(trail.primaryColorHex),
            unlocked = unlocked,
            equipped = equipped,
            testTag = "reward_trail_${trail.id}",
            onClick = { if (unlocked) onEquipTrail(trail) }
        )
      }
    }

    if (selectedTab == RewardCategoryTab.ALL || selectedTab == RewardCategoryTab.EFFECTS) {
      RewardSectionHeader(
          title = "VICTORY GATE EFFECTS",
          color = Color(0xFFC084FC)
      )
      for (effect in VictoryEffect.entries) {
        val unlocked = rewards.unlockedEffects.contains(effect)
        val equipped = rewards.equippedEffect == effect
        RewardItemCard(
            title = effect.displayName,
            subtitle = effect.description,
            unlockHint = effect.unlockHint,
            accent = Color(effect.primaryColorHex),
            unlocked = unlocked,
            equipped = equipped,
            testTag = "reward_effect_${effect.id}",
            onClick = { if (unlocked) onEquipEffect(effect) }
        )
      }
    }

    if (selectedTab == RewardCategoryTab.ALL || selectedTab == RewardCategoryTab.TITLES) {
      RewardSectionHeader(
          title = "PLAYER TITLES",
          color = Color(0xFFFDE047)
      )
      for (title in PlayerTitle.entries) {
        val unlocked = rewards.unlockedTitles.contains(title)
        val equipped = rewards.equippedTitle == title
        RewardItemCard(
            title = title.displayName,
            subtitle = title.description,
            unlockHint = title.unlockHint,
            accent = Color(title.accentColorHex),
            unlocked = unlocked,
            equipped = equipped,
            testTag = "reward_title_${title.id}",
            onClick = { if (unlocked) onEquipTitle(title) }
        )
      }
    }

    if (selectedTab == RewardCategoryTab.ALL || selectedTab == RewardCategoryTab.MASTERY) {
      MasteryRewardsMilestoneSection(retention = retention)
    }
  }
}

@Composable
private fun RewardSectionHeader(
    title: String,
    color: Color
) {
  Text(
      text = title,
      fontSize = 11.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 0.8.sp,
      color = color
  )
}

@Composable
private fun MasteryRewardsMilestoneSection(
    retention: Phase4RetentionSnapshot
) {
  val totalStars = retention.totalMasteryStars
  val speedStars = retention.masteryByChallenge.values.sumOf { prog ->
    prog.completedObjectiveIds.count { it.endsWith("_speed_par") }
  }
  val rewards = retention.rewards

  RewardSectionHeader(
      title = "MASTERY & MILESTONE REWARDS",
      color = Color(0xFF34D399)
  )

  val milestones = listOf(
      MasteryMilestoneRowData(
          id = "stars_3",
          title = "Ember Spark Trail & Trap Survivor Title",
          requirement = "Earn 3 Mastery Stars",
          progressLabel = "${totalStars.coerceAtMost(3)}/3 ★",
          progressFraction = (totalStars / 3f).coerceIn(0f, 1f),
          unlocked = rewards.unlockedTrails.contains(CosmeticTrail.EMBER_SPARK) || totalStars >= 3,
          accent = Color(CosmeticTrail.EMBER_SPARK.primaryColorHex)
      ),
      MasteryMilestoneRowData(
          id = "stars_6",
          title = "Saffron Fireworks Victory Effect",
          requirement = "Earn 6 Mastery Stars or finish all 200 challenges",
          progressLabel = "${totalStars.coerceAtMost(6)}/6 ★",
          progressFraction = (totalStars / 6f).coerceIn(0f, 1f),
          unlocked = rewards.unlockedEffects.contains(VictoryEffect.SAFFRON_FIREWORKS),
          accent = Color(VictoryEffect.SAFFRON_FIREWORKS.primaryColorHex)
      ),
      MasteryMilestoneRowData(
          id = "stars_12",
          title = "Solar Gold Trail",
          requirement = "Earn 12 Mastery Stars",
          progressLabel = "${totalStars.coerceAtMost(12)}/12 ★",
          progressFraction = (totalStars / 12f).coerceIn(0f, 1f),
          unlocked = rewards.unlockedTrails.contains(CosmeticTrail.SOLAR_GOLD),
          accent = Color(CosmeticTrail.SOLAR_GOLD.primaryColorHex)
      ),
      MasteryMilestoneRowData(
          id = "stars_18",
          title = "Master of Chaos Title",
          requirement = "Earn 18 Mastery Stars",
          progressLabel = "${totalStars.coerceAtMost(18)}/18 ★",
          progressFraction = (totalStars / 18f).coerceIn(0f, 1f),
          unlocked = rewards.unlockedTitles.contains(PlayerTitle.MASTER_OF_CHAOS),
          accent = Color(PlayerTitle.MASTER_OF_CHAOS.accentColorHex)
      ),
      MasteryMilestoneRowData(
          id = "stars_24",
          title = "Starlight Crown Victory Effect",
          requirement = "Earn 24 Mastery Stars",
          progressLabel = "${totalStars.coerceAtMost(24)}/24 ★",
          progressFraction = (totalStars / 24f).coerceIn(0f, 1f),
          unlocked = rewards.unlockedEffects.contains(VictoryEffect.STARLIGHT_CROWN),
          accent = Color(VictoryEffect.STARLIGHT_CROWN.primaryColorHex)
      ),
      MasteryMilestoneRowData(
          id = "speed_3",
          title = "Speed Demon Title",
          requirement = "Earn 3 Swift Runner speed stars",
          progressLabel = "${speedStars.coerceAtMost(3)}/3 Swift",
          progressFraction = (speedStars / 3f).coerceIn(0f, 1f),
          unlocked = rewards.unlockedTitles.contains(PlayerTitle.SPEED_DEMON),
          accent = Color(PlayerTitle.SPEED_DEMON.accentColorHex)
      ),
      MasteryMilestoneRowData(
          id = "coin_greedy",
          title = "Coin Greedy Title",
          requirement = "Collect 100% of coins in any challenge",
          progressLabel = if (retention.coinGreedyEarned || rewards.unlockedTitles.contains(PlayerTitle.COIN_GREEDY)) "1/1" else "0/1",
          progressFraction = if (retention.coinGreedyEarned || rewards.unlockedTitles.contains(PlayerTitle.COIN_GREEDY)) 1f else 0f,
          unlocked = rewards.unlockedTitles.contains(PlayerTitle.COIN_GREEDY) || retention.coinGreedyEarned,
          accent = Color(PlayerTitle.COIN_GREEDY.accentColorHex)
      )
  )

  for (milestone in milestones) {
    MasteryMilestoneCard(milestone = milestone)
  }
}

private data class MasteryMilestoneRowData(
    val id: String,
    val title: String,
    val requirement: String,
    val progressLabel: String,
    val progressFraction: Float,
    val unlocked: Boolean,
    val accent: Color
)

@Composable
private fun MasteryMilestoneCard(milestone: MasteryMilestoneRowData) {
  Column(
      modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(if (milestone.unlocked) Color(0xFF13262E) else Color(0xDD0D1B21))
          .border(
              width = if (milestone.unlocked) 1.5.dp else 1.dp,
              color = if (milestone.unlocked) milestone.accent else Color(0xFF1E3A44),
              shape = RoundedCornerShape(12.dp)
          )
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("reward_mastery_milestone_${milestone.id}"),
      verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
            imageVector = if (milestone.unlocked) Icons.Default.CheckCircle else Icons.Default.Star,
            contentDescription = if (milestone.unlocked) "Unlocked" else "Mastery Milestone",
            tint = if (milestone.unlocked) Color(0xFF10B981) else milestone.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
              text = milestone.title,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = if (milestone.unlocked) Color(0xFFFFFBEB) else Color(0xFFE2E8F0)
          )
          Text(
              text = milestone.requirement,
              fontSize = 10.5.sp,
              color = Color(0xFF94A3B8)
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
          text = if (milestone.unlocked) "UNLOCKED" else milestone.progressLabel,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Black,
          color = if (milestone.unlocked) Color(0xFF091318) else milestone.accent,
          modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (milestone.unlocked) Color(0xFF10B981) else milestone.accent.copy(alpha = 0.16f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
    ) {
      Box(
          modifier = Modifier
              .fillMaxWidth(milestone.progressFraction)
              .height(5.dp)
              .clip(CircleShape)
              .background(if (milestone.unlocked) Color(0xFF10B981) else milestone.accent)
      )
    }
  }
}

@Composable
private fun RewardItemCard(
    title: String,
    subtitle: String,
    unlockHint: String,
    accent: Color,
    unlocked: Boolean,
    equipped: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (equipped) Color(0xFF13262E) else Color(0xDD0D1B21))
          .border(
              width = if (equipped) 2.dp else 1.dp,
              color = when {
                equipped -> accent
                unlocked -> Color(0xFF283D47)
                else -> Color(0xFF1A2A32)
              },
              shape = RoundedCornerShape(12.dp)
          )
          .clickable(enabled = unlocked, onClick = onClick)
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag(testTag)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(if (unlocked) accent.copy(alpha = 0.25f) else Color(0xFF1E293B))
      ) {
        Icon(
            imageVector = if (unlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
            contentDescription = if (unlocked) "Unlocked" else "Locked",
            tint = if (unlocked) accent else Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = if (unlocked) Color(0xFFFFFBEB) else Color(0xFF64748B)
        )
        Text(
            text = if (unlocked) subtitle else "Unlock: $unlockHint",
            fontSize = 10.5.sp,
            color = if (unlocked) Color(0xFF94A3B8) else Color(0xFF64748B),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Text(
        text = when {
          equipped -> "EQUIPPED"
          unlocked -> "EQUIP"
          else -> "LOCKED"
        },
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Black,
        color = when {
          equipped -> Color(0xFF091318)
          unlocked -> accent
          else -> Color(0xFF64748B)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                  equipped -> accent
                  unlocked -> accent.copy(alpha = 0.16f)
                  else -> Color(0xFF132229)
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
  }
}
