package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChallengeCatalog
import com.example.data.StoryClueId
import com.example.ui.GameStateViewModel

@Composable
fun MainMenuView(
    viewModel: GameStateViewModel,
    modifier: Modifier = Modifier
) {
  val isMuted by viewModel.isMusicMuted.collectAsState()
  val bestTime by viewModel.bestTimeMs.collectAsState()
  val stars by viewModel.bestStars.collectAsState()
  val bestScore by viewModel.bestScore.collectAsState()
  val discoveredClues by viewModel.discoveredClues.collectAsState()
  val isResonanceFound by viewModel.isResonanceDiscovered.collectAsState()
  val levelsProgress by viewModel.levelsProgress.collectAsState()
  val continuousState by viewModel.continuousPersistedState.collectAsState()
  val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
  val unlockedCount = levelsProgress.count { it.isUnlocked }
  val highestUnlocked = levelsProgress.lastOrNull { it.isUnlocked }?.levelId ?: 1
  val highestMeta = levelsProgress.firstOrNull { it.levelId == highestUnlocked }

  val totalClues = StoryClueId.entries.size

  val infiniteTransition = rememberInfiniteTransition(label = "titleFloat")
  val titleOffset by infiniteTransition.animateFloat(
      initialValue = -2.5f,
      targetValue = 2.5f,
      animationSpec = infiniteRepeatable(
          animation = tween(2400, easing = EaseInOutSine),
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

    // Atmospheric twilight emerald vignette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xF0081115),
                        Color(0xBB0B181E),
                        Color(0x33091318)
                    ),
                    startX = 0f,
                    endX = 1450f
                )
            )
    )

    // Subtle top edge vignette to keep status strip crisp above background art
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.22f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xEE071014),
                        Color(0x99071014),
                        Color.Transparent
                    )
                )
            )
    )

    // Safe area content container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
      // Top-Right Quick Audio Toggle + Mystery Status Chip
      Row(
          modifier = Modifier.align(Alignment.TopEnd),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC0F1E24))
                .border(1.5.dp, Color(0xFF14B8A6), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = null,
              tint = Color(0xFF67E8F9),
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
              text = "THE FOREST IS WATCHING",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp,
              color = Color(0xFF67E8F9)
          )
        }

        IconButton(
            onClick = { viewModel.toggleMusic() },
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC132229))
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                .testTag("button_mute_toggle")
        ) {
          Icon(
              imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Toggle Audio",
              tint = Color(0xFFFDE047),
              modifier = Modifier.size(24.dp)
          )
        }
      }

      // Left Adventure Panel (Scrollable so landscape height never clips)
      Column(
          modifier = Modifier
              .align(Alignment.CenterStart)
              .widthIn(max = 480.dp)
              .fillMaxWidth(0.58f)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.Start
      ) {
        // Embossed Title Header
        Column(
            modifier = Modifier
                .offset(y = titleOffset.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A3138), Color(0xFF0E1B20))
                    )
                )
                .border(2.5.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
              text = "CHAOS RUN",
              fontSize = 26.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp,
              color = Color(0xFFFFFBEB),
              maxLines = 1
          )
          Text(
              text = "WORLD 1 • THE FOREST IS WATCHING",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              color = Color(0xFFFDE047),
              maxLines = 1
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress & Clues Status Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xBB101E24))
                .border(1.dp, Color(0xFF283D47), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          repeat(3) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < stars) Color(0xFFFFD700) else Color(0x5594A3B8),
                modifier = Modifier.size(16.dp)
            )
          }
          if (bestTime > 0) {
            Text(
                text = "%.1fs".format(bestTime / 1000f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0)
            )
          }
          if (bestScore > 0) {
            Text(
                text = "•",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = "${bestScore}pts",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE047)
            )
          }
          Text(
              text = "•",
              fontSize = 11.sp,
              color = Color(0xFF64748B)
          )
          Text(
              text = "Clues ${discoveredClues.size}/$totalClues",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF67E8F9)
          )
          if (isResonanceFound) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Resonance Stone Discovered",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Primary Play / Continue Buttons
        if (highestUnlocked > 1 && highestMeta != null) {
          AdventureButton(
              text = "CONTINUE 1-${highestMeta.levelId}: ${highestMeta.title.uppercase()}",
              subtitle = highestMeta.mechanicTag,
              accentColor = Color(0xFF38BDF8),
              icon = Icons.Default.FastForward,
              onClick = { viewModel.startLevel(highestMeta.levelId) },
              testTag = "button_continue_latest"
          )
          Spacer(modifier = Modifier.height(6.dp))
        }

        AdventureButton(
            text = "PLAY 1-1: THE EASY START",
            subtitle = "Disappearing Floors, Crumble & Pop Spikes",
            accentColor = Color(0xFF10B981),
            icon = Icons.Default.PlayArrow,
            onClick = { viewModel.startLevel1() },
            testTag = "button_play_level_1"
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Row 1: Chapter Trail & Field Notes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CompactMenuCard(
              title = "CHAPTER TRAIL",
              subtitle = "$unlockedCount/5 Unlocked (1-1..1-5)",
              accentColor = Color(0xFF14B8A6),
              icon = Icons.Default.Explore,
              onClick = { viewModel.openChapterTrail() },
              testTag = "button_chapter_trail",
              modifier = Modifier.weight(1f)
          )
          CompactMenuCard(
              title = "FIELD NOTES",
              subtitle = "${discoveredClues.size}/$totalClues Clues",
              accentColor = Color(0xFFF59E0B),
              icon = Icons.AutoMirrored.Filled.MenuBook,
              onClick = { viewModel.openFieldNotes() },
              testTag = "button_field_notes",
              modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Row 2: How to Play & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CompactMenuCard(
              title = "HOW TO PLAY",
              subtitle = "Controls & Rules",
              accentColor = Color(0xFF0EA5E9),
              icon = Icons.AutoMirrored.Filled.HelpOutline,
              onClick = { viewModel.showHowToPlay() },
              testTag = "button_how_to_play",
              modifier = Modifier.weight(1f)
          )
          CompactMenuCard(
              title = "SETTINGS",
              subtitle = "Audio & Volume",
              accentColor = Color(0xFF64748B),
              icon = Icons.Default.Settings,
              onClick = { viewModel.showSettings() },
              testTag = "button_settings",
              modifier = Modifier.weight(1f)
          )
        }
      }

      // Right Panel: First-Time Quick Onboarding (first launch only) + Continuous Run Entry
      Column(
          modifier = Modifier
              .align(Alignment.BottomEnd)
              .widthIn(max = 290.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (!isOnboardingCompleted) {
          FirstLaunchOnboardingCard(
              onOpenFullGuide = { viewModel.openOnboardingModal() },
              onSkip = { viewModel.completeOnboarding(syncCommit = false) },
              onComplete = { viewModel.completeOnboarding(syncCommit = false) },
              modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 205.dp)
          )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xDD0E1D24))
                .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val activeChallengeDef = ChallengeCatalog.findById(continuousState.activeChallengeId)
          val continuousTitle = when {
            continuousState.isRunActive && !continuousState.isRunCompleted && activeChallengeDef != null ->
              "CONTINUE RUN • CH ${activeChallengeDef.orderIndex}/${ChallengeCatalog.entries.size}"
            else -> "START CONTINUOUS RUN"
          }
          val totalChallenges = ChallengeCatalog.entries.size
          val continuousSubtitle = when {
            continuousState.isRunActive && !continuousState.isRunCompleted ->
              "${continuousState.completedChallengeIds.size}/$totalChallenges cleared • ${continuousState.cumulativeDeaths} deaths"
            continuousState.isRunCompleted ->
              "Last run $totalChallenges/$totalChallenges cleared • Best ${continuousState.bestRunScore}pts"
            else -> "Ch 1–$totalChallenges continuous progression"
          }

          AdventureButton(
              text = continuousTitle,
              subtitle = continuousSubtitle,
              accentColor = Color(0xFFF59E0B),
              icon = Icons.Default.FastForward,
              onClick = { viewModel.startOrContinueContinuousRun() },
              testTag = "button_continuous_run"
          )

          if (continuousState.isRunCompleted) {
            OutlinedButton(
                onClick = { viewModel.openRunCompletionScreen() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("button_view_run_summary")
            ) {
              Text(
                  text = "RUN COMPLETION SUMMARY",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF67E8F9)
              )
            }
          }
        }
      }
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
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.PlayArrow
) {
  Box(
      modifier = modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .shadow(6.dp, RoundedCornerShape(12.dp))
          .clip(RoundedCornerShape(12.dp))
          .background(
              Brush.horizontalGradient(
                  colors = listOf(Color(0xFF1B333A), Color(0xFF112127))
              )
          )
          .border(2.dp, accentColor, RoundedCornerShape(12.dp))
          .clickable { onClick() }
          .testTag(testTag)
          .padding(horizontal = 14.dp, vertical = 8.dp),
      contentAlignment = Alignment.CenterStart
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFF8FAFC),
            maxLines = 1
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8),
            maxLines = 1
        )
      }
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
private fun CompactMenuCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xDD132229))
          .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
          .clickable { onClick() }
          .testTag(testTag)
          .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column {
      Text(
          text = title,
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFF8FAFC),
          maxLines = 1
      )
      Text(
          text = subtitle,
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF94A3B8),
          maxLines = 1
      )
    }
  }
}
