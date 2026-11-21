package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AdaptiveTier
import com.example.data.ChallengeCatalog
import com.example.data.GameplayNotification
import com.example.data.LevelTheme
import com.example.data.NotificationCategory
import com.example.data.NotificationPriority
import com.example.data.NotificationVisualKind
import com.example.data.RunLaunchMode
import com.example.data.StoryClueId
import com.example.engine.GameSurfaceView
import com.example.ui.GameStateViewModel
import com.example.ui.VictoryResult
import com.example.ui.input.GameControlButton
import com.example.ui.input.MultiTouchControlState
import kotlin.math.roundToInt

@Composable
fun GameplayView(
    viewModel: GameStateViewModel,
    modifier: Modifier = Modifier
) {
  val session by viewModel.gameSession.collectAsState()
  val activeSession = session ?: return
  val engine = activeSession.engine
  val renderer = activeSession.renderer
  val touchControlState = viewModel.touchControlState
  val victoryResult by viewModel.victoryResult.collectAsState()
  val isFieldNotesOpen by viewModel.isFieldNotesOpen.collectAsState()
  val isUserPaused by viewModel.isUserPaused.collectAsState()
  val isEffectivePaused by viewModel.isEffectivePaused.collectAsState()

  // Bind touch controller to the active session engine; clear stale touches on leave
  DisposableEffect(activeSession.sessionId, engine) {
    touchControlState.bindEngine(engine)
    onDispose {
      touchControlState.clearAll()
    }
  }

  // HUD state keyed to sessionId: snapshot updates when discrete HUD counters, notification, or challengeEpoch change.
  var hudSnapshot by remember(activeSession.sessionId) { mutableStateOf(engine.hudSnapshot) }
  var timerTenths by remember(activeSession.sessionId) { mutableIntStateOf(engine.hudSnapshot.timerTenths) }
  val totalDiyas = remember(activeSession.sessionId, hudSnapshot.challengeEpoch) { engine.diyas.size }
  val totalRelics = remember(activeSession.sessionId, hudSnapshot.challengeEpoch) { engine.relics.size }
  val totalCoins = remember(activeSession.sessionId, hudSnapshot.challengeEpoch) { engine.coins.size }

  // Measured bounds in root coordinates (no hardcoded control positions)
  var rootBounds by remember { mutableStateOf(Rect.Zero) }
  var safeAreaBounds by remember { mutableStateOf(Rect.Zero) }
  var hudBounds by remember { mutableStateOf(Rect.Zero) }
  var leftControlBounds by remember { mutableStateOf(Rect.Zero) }
  var rightControlBounds by remember { mutableStateOf(Rect.Zero) }
  var jumpControlBounds by remember { mutableStateOf(Rect.Zero) }

  LaunchedEffect(activeSession.sessionId, isEffectivePaused) {
    while (true) {
      val snap = engine.hudSnapshot
      val hudChanged =
          snap.coinsCollected != hudSnapshot.coinsCollected ||
              snap.health != hudSnapshot.health ||
              snap.cluesCount != hudSnapshot.cluesCount ||
              snap.resonanceDiscovered != hudSnapshot.resonanceDiscovered ||
              snap.score != hudSnapshot.score ||
              snap.comboMultiplier != hudSnapshot.comboMultiplier ||
              snap.diyasLit != hudSnapshot.diyasLit ||
              snap.relicsCollected != hudSnapshot.relicsCollected ||
              snap.deathCount != hudSnapshot.deathCount ||
              snap.isDying != hudSnapshot.isDying ||
              snap.challengeEpoch != hudSnapshot.challengeEpoch ||
              snap.challengeId != hudSnapshot.challengeId ||
              snap.challengeOrderIndex != hudSnapshot.challengeOrderIndex ||
              snap.launchMode != hudSnapshot.launchMode ||
              snap.cumulativeDeathCount != hudSnapshot.cumulativeDeathCount ||
              snap.activeAdaptiveTier != hudSnapshot.activeAdaptiveTier ||
              (snap.mirrorCurseFrames > 0) != (hudSnapshot.mirrorCurseFrames > 0) ||
              snap.activeNotification != hudSnapshot.activeNotification
      if (hudChanged) {
        hudSnapshot = snap
      }
      if (timerTenths != snap.timerTenths) {
        timerTenths = snap.timerTenths
      }
      kotlinx.coroutines.delay(80L)
    }
  }

  Box(
      modifier = modifier
          .fillMaxSize()
          .onGloballyPositioned { rootBounds = it.boundsInRoot() }
          .testTag("screen_gameplay")
  ) {
    // 1. Dedicated 60 FPS SurfaceView Game Canvas keyed by sessionId so Restart/Replay always binds fresh engine
    key(activeSession.sessionId) {
      AndroidView(
          factory = { ctx ->
            GameSurfaceView(ctx, engine, renderer).apply {
              setPaused(isEffectivePaused)
            }
          },
          update = { surfaceView ->
            surfaceView.setPaused(isEffectivePaused)
          },
          onRelease = { surfaceView ->
            surfaceView.release()
          },
          modifier = Modifier.fillMaxSize()
      )
    }

    // Smooth non-blocking theme-matched level/challenge transition veil (~180ms)
    val transitionAlpha = remember(activeSession.sessionId, hudSnapshot.challengeEpoch) { Animatable(0.75f) }
    LaunchedEffect(activeSession.sessionId, hudSnapshot.challengeEpoch) {
      transitionAlpha.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 180)
      )
    }
    if (transitionAlpha.value > 0.01f) {
      val transitionTint = when (engine.level.theme) {
        LevelTheme.WHISPERING_WOODS -> Color(0xFF7DD3FC)
        LevelTheme.HOLLOW_OF_ECHOES -> Color(0xFF285C7E)
        LevelTheme.RUINS_OF_LOST_KING -> Color(0xFFF4AA80)
        LevelTheme.MONSOON_RUN -> Color(0xFF4B7699)
        LevelTheme.TEMPLE_OF_LAST_LIGHT -> Color(0xFF564AA0)
      }
      Box(
          modifier = Modifier
              .fillMaxSize()
              .background(transitionTint.copy(alpha = transitionAlpha.value))
              .testTag("level_transition_veil")
      )
    }

    // 2. Polished Illustrated Adventure HUD (Responsive Top Bar with safe insets)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(top = 8.dp, start = 14.dp, end = 14.dp)
            .onGloballyPositioned { hudBounds = it.boundsInRoot() }
            .testTag("hud_top_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      // Left Zone: Player Vitals / Troll Deaths + Mirror Curse Active State
      Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
        if (engine.level.isTrollLevel) {
          Row(
              modifier = Modifier
                  .shadow(4.dp, RoundedCornerShape(12.dp))
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xD8101E24))
                  .border(1.8.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("hud_deaths_pill"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Troll Deaths",
                tint = Color(0xFFFCA5A5),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Deaths: ${hudSnapshot.deathCount}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFEE2E2),
                maxLines = 1
            )
          }
        } else {
          val isLowHealth = hudSnapshot.health <= 1
          Row(
              modifier = Modifier
                  .shadow(4.dp, RoundedCornerShape(12.dp))
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xD8101E24))
                  .border(
                      width = 1.8.dp,
                      color = if (isLowHealth) Color(0xFFEF4444) else Color(0xFFF59E0B),
                      shape = RoundedCornerShape(12.dp)
                  )
                  .padding(horizontal = 9.dp, vertical = 6.dp)
                  .testTag("hud_health_pill"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            repeat(engine.player.maxHealth) { index ->
              Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = "Heart",
                  tint = if (index < hudSnapshot.health) Color(0xFFEF4444) else Color(0x5594A3B8),
                  modifier = Modifier
                      .size(17.dp)
                      .padding(end = 2.dp)
              )
            }
          }
        }

        if (hudSnapshot.mirrorCurseFrames > 0) {
          Row(
              modifier = Modifier
                  .shadow(4.dp, RoundedCornerShape(12.dp))
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xE0241436))
                  .border(1.8.dp, Color(0xFFC084FC), RoundedCornerShape(12.dp))
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("hud_mirror_badge"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Inverted Controls Active",
                tint = Color(0xFFE9D5FF),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "MIRROR",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF3E8FF),
                maxLines = 1
            )
          }
        }
      }

      // Center Zone: Coins, Score/Combo, Diyas & Clues/Relics (scroll-safe on narrow viewports)
      Row(
          modifier = Modifier
              .weight(1f)
              .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically
      ) {
        val allCoinsCollected = totalCoins > 0 && hudSnapshot.coinsCollected >= totalCoins
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xD8101E24))
                .border(
                    width = 1.8.dp,
                    color = if (allCoinsCollected) Color(0xFFFDE047) else Color(0xFFD97706),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .testTag("hud_coins_pill"),
            verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Coins",
              tint = Color(0xFFFFD700),
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
              text = "${hudSnapshot.coinsCollected}/$totalCoins",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFFBEB),
              maxLines = 1
          )
        }

        // Score & Combo Multiplier Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xD8101E24))
                .border(
                    width = 1.8.dp,
                    color = if (hudSnapshot.comboMultiplier > 1) Color(0xFFFB923C) else Color(0xFF10B981),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 9.dp, vertical = 6.dp)
                .testTag("hud_score_pill"),
            verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Score",
              tint = Color(0xFFFDE047),
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
              text = "${hudSnapshot.score}",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFFBEB),
              maxLines = 1
          )
          if (hudSnapshot.comboMultiplier > 1) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "x${hudSnapshot.comboMultiplier}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF091318),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFB923C))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
                    .testTag("hud_combo_badge")
            )
          }
        }

        // Brass Diyas Secret Flames Pill
        if (totalDiyas > 0) {
          Row(
              modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xD8101E24))
                  .border(1.8.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                  .padding(horizontal = 9.dp, vertical = 6.dp)
                  .testTag("hud_diyas_pill"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Brass Diyas Lit",
                tint = if (hudSnapshot.diyasLit > 0) Color(0xFFFB923C) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "${hudSnapshot.diyasLit}/$totalDiyas",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFEF3C7),
                maxLines = 1
            )
          }
        }

        // Clues Pill (non-troll 1-1) or Relics Pill (1-2..1-5)
        if (engine.level.levelId == 1 && !engine.level.isTrollLevel) {
          Row(
              modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xD8101E24))
                  .border(1.8.dp, Color(0xFF14B8A6), RoundedCornerShape(12.dp))
                  .padding(horizontal = 9.dp, vertical = 6.dp)
                  .testTag("hud_clues_pill"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Story Clues",
                tint = Color(0xFF67E8F9),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Clues ${hudSnapshot.cluesCount}/${StoryClueId.entries.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE0F2FE),
                maxLines = 1
            )
            if (hudSnapshot.resonanceDiscovered) {
              Spacer(modifier = Modifier.width(5.dp))
              Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = "Resonance Stone Found",
                  tint = Color(0xFF38BDF8),
                  modifier = Modifier
                      .size(15.dp)
                      .testTag("hud_resonance_badge")
              )
            }
          }
        } else if (totalRelics > 0) {
          Row(
              modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xD8101E24))
                  .border(1.8.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                  .padding(horizontal = 9.dp, vertical = 6.dp)
                  .testTag("hud_relics_pill"),
              verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Relics",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Relics ${hudSnapshot.relicsCollected}/$totalRelics",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE0F2FE),
                maxLines = 1
            )
          }
        }
      }

      // Right Zone: Challenge Progress Indicator, Timer & Pause Button
      Row(verticalAlignment = Alignment.CenterVertically) {
        val totalChallenges = ChallengeCatalog.entries.size.coerceAtLeast(1)
        val tierColor = if (hudSnapshot.launchMode == RunLaunchMode.REPLAY) Color(0xFFFDE047) else Color(0xFF67E8F9)
        val levelLabel = when (hudSnapshot.launchMode) {
          RunLaunchMode.CONTINUOUS_PROGRESSION ->
            "CH ${hudSnapshot.challengeOrderIndex}/$totalChallenges • ${engine.level.title}"
          RunLaunchMode.REPLAY ->
            "REPLAY • CH ${hudSnapshot.challengeOrderIndex}"
          RunLaunchMode.SINGLE_STAGE ->
            "1-${engine.level.levelId} • ${engine.level.title}"
        }
        val progressFraction = when (hudSnapshot.launchMode) {
          RunLaunchMode.CONTINUOUS_PROGRESSION ->
            (hudSnapshot.challengeOrderIndex.toFloat() / totalChallenges.toFloat()).coerceIn(0.05f, 1f)
          RunLaunchMode.REPLAY -> 1f
          RunLaunchMode.SINGLE_STAGE ->
            if (totalCoins > 0) (hudSnapshot.coinsCollected.toFloat() / totalCoins.toFloat()).coerceIn(0.08f, 1f)
            else 0.25f
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .widthIn(min = 120.dp, max = 225.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xD8101E24))
                .border(1.8.dp, Color(0xFF0D7377), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag(
                    if (hudSnapshot.launchMode == RunLaunchMode.REPLAY) "hud_replay_badge"
                    else "hud_challenge_badge"
                )
        ) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            if (hudSnapshot.activeAdaptiveTier != AdaptiveTier.STANDARD) {
              val (tierLabel, tierBadgeColor) = when (hudSnapshot.activeAdaptiveTier) {
                AdaptiveTier.ASSISTED -> "ASSIST" to Color(0xFF2DD4BF)
                AdaptiveTier.ESCALATED -> "BOOST" to Color(0xFFFBBF24)
                AdaptiveTier.RELENTLESS -> "FIERCE" to Color(0xFFFB7185)
                AdaptiveTier.STANDARD -> "" to Color.Transparent
              }
              Text(
                  text = tierLabel,
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF091318),
                  modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(tierBadgeColor)
                      .padding(horizontal = 4.dp, vertical = 1.dp)
                      .testTag("hud_adaptive_tier_badge")
              )
            }
            Text(
                text = levelLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = tierColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(modifier = Modifier.height(3.dp))

          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("hud_progress_bar")
            ) {
              Box(
                  modifier = Modifier
                      .fillMaxHeight()
                      .fillMaxWidth(progressFraction)
                      .clip(CircleShape)
                      .background(tierColor)
              )
            }
            HudTimerText(timerTenthsProvider = { timerTenths })
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = { viewModel.setUserPaused(true) },
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xDD1D313B))
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

    // 3. Multi-Touch Ergonomic Virtual Controls + Measured-Bounds Adaptive Notification Layer
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(bottom = 16.dp, start = 22.dp, end = 22.dp)
            .onGloballyPositioned { safeAreaBounds = it.boundsInRoot() }
    ) {
      GameplayNotificationOverlay(
          sessionId = activeSession.sessionId,
          engine = engine,
          renderer = renderer,
          activeNotification = hudSnapshot.activeNotification,
          isVisible = hudSnapshot.activeNotification != null && victoryResult == null && !isEffectivePaused,
          isEffectivePaused = isEffectivePaused,
          hasVictory = victoryResult != null,
          maxWidth = maxWidth,
          maxHeight = maxHeight,
          rootBounds = rootBounds,
          safeAreaBounds = safeAreaBounds,
          hudBounds = hudBounds,
          leftControlBounds = leftControlBounds,
          rightControlBounds = rightControlBounds,
          jumpControlBounds = jumpControlBounds
      )

      Row(
          modifier = Modifier.align(Alignment.BottomStart),
          verticalAlignment = Alignment.CenterVertically
      ) {
        TouchControlButton(
            controlButton = GameControlButton.LEFT,
            controlState = touchControlState,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = "Move Left",
            buttonSizeDp = 74,
            accentColor = Color(0xFF0D7377),
            testTag = "control_left",
            modifier = Modifier.onGloballyPositioned { leftControlBounds = it.boundsInRoot() }
        )

        Spacer(modifier = Modifier.width(18.dp))

        TouchControlButton(
            controlButton = GameControlButton.RIGHT,
            controlState = touchControlState,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            description = "Move Forward Right",
            buttonSizeDp = 82,
            accentColor = Color(0xFF10B981),
            testTag = "control_right",
            modifier = Modifier.onGloballyPositioned { rightControlBounds = it.boundsInRoot() }
        )
      }

      TouchControlButton(
          controlButton = GameControlButton.JUMP,
          controlState = touchControlState,
          icon = Icons.Default.ArrowUpward,
          description = "Jump",
          buttonSizeDp = 86,
          accentColor = Color(0xFFF59E0B),
          testTag = "control_jump",
          modifier = Modifier
              .align(Alignment.BottomEnd)
              .onGloballyPositioned { jumpControlBounds = it.boundsInRoot() }
      )
    }

    // 4. Polished Pause Dialog with Quick Audio & Haptic Controls
    if (isUserPaused && victoryResult == null) {
      val musicOn by viewModel.isMusicEnabled.collectAsState()
      val sfxOn by viewModel.isSfxEnabled.collectAsState()
      val vibrationOn by viewModel.isVibrationEnabled.collectAsState()

      Dialog(onDismissRequest = { viewModel.setUserPaused(false) }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101E24)),
            border = BorderStroke(2.5.dp, Color(0xFFF59E0B)),
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .padding(10.dp)
                .testTag("dialog_pause")
        ) {
          Column(
              modifier = Modifier
                  .verticalScroll(rememberScrollState())
                  .padding(horizontal = 20.dp, vertical = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
                text = "GAME PAUSED",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFDE047)
            )
            Text(
                text = "${engine.level.title} • ${hudSnapshot.coinsCollected}/$totalCoins Coins • ${hudSnapshot.deathCount} Deaths",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Audio & Haptics Toggle Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF091318))
                    .border(1.dp, Color(0xFF283D47), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                  onClick = { viewModel.toggleMusic() },
                  modifier = Modifier
                      .size(48.dp)
                      .testTag("button_pause_music")
              ) {
                Icon(
                    imageVector = if (musicOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle Music",
                    tint = if (musicOn) Color(0xFFF59E0B) else Color(0xFF64748B)
                )
              }
              IconButton(
                  onClick = { viewModel.toggleSfx() },
                  modifier = Modifier
                      .size(48.dp)
                      .testTag("button_pause_sfx")
              ) {
                Icon(
                    imageVector = if (sfxOn) Icons.Default.GraphicEq else Icons.Default.DoNotDisturbOn,
                    contentDescription = "Toggle SFX",
                    tint = if (sfxOn) Color(0xFF14B8A6) else Color(0xFF64748B)
                )
              }
              IconButton(
                  onClick = { viewModel.toggleVibration() },
                  modifier = Modifier
                      .size(48.dp)
                      .testTag("button_pause_vibration")
              ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = "Toggle Vibration",
                    tint = if (vibrationOn) Color(0xFF10B981) else Color(0xFF64748B)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.setUserPaused(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_resume")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF091318), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("RESUME", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF091318))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.restartLevel() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_restart")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFFFBEB), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                  text = if (hudSnapshot.launchMode == RunLaunchMode.CONTINUOUS_PROGRESSION) {
                    "RESTART CHALLENGE"
                  } else {
                    "RESTART LEVEL"
                  },
                  fontWeight = FontWeight.Black,
                  fontSize = 14.sp,
                  color = Color(0xFFFFFBEB)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (hudSnapshot.launchMode == RunLaunchMode.REPLAY) {
              OutlinedButton(
                  onClick = { viewModel.returnToRunCompletion() },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("button_return_summary")
              ) {
                Text("RUN SUMMARY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF67E8F9))
              }
              Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { viewModel.returnToMainMenu() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("button_return_menu")
            ) {
              Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFFE2E8F0), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("RETURN TO MENU", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE2E8F0))
            }
          }
        }
      }
    }

    // 5. Victory Dialog Modal (hidden cleanly while inspecting Field Notes)
    if (!isFieldNotesOpen) {
      victoryResult?.let { result ->
        VictoryDialog(
            result = result,
            onNextLevel = if (result.launchMode == RunLaunchMode.SINGLE_STAGE && result.levelId < 5 && result.nextLevelUnlocked) {
              { viewModel.startNextLevel() }
            } else null,
            onReplay = { viewModel.restartLevel() },
            onOpenFieldNotes = { viewModel.openFieldNotes() },
            onMainMenu = if (result.launchMode == RunLaunchMode.REPLAY) {
              { viewModel.returnToRunCompletion() }
            } else {
              { viewModel.returnToMainMenu() }
            }
        )
      }
    }
  }
}

@Composable
fun VictoryDialog(
    result: VictoryResult,
    onNextLevel: (() -> Unit)?,
    onReplay: () -> Unit,
    onOpenFieldNotes: () -> Unit,
    onMainMenu: () -> Unit
) {
  Dialog(
      onDismissRequest = onMainMenu,
      properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
  ) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B161A)),
        border = BorderStroke(3.dp, Color(0xFFFFD700)),
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(10.dp)
            .testTag("dialog_victory")
    ) {
      Column(
          modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
            text = when {
              result.levelId == 5 -> "WORLD 1 COMPLETE!"
              result.isTrollLevel -> "TROLLS OUTSMARTED!"
              else -> "SUN GATE AWAKENED!"
            },
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFD700)
        )
        Text(
            text = "1-${result.levelId}: ${result.levelTitle} Complete",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Star Rating
        Row {
          repeat(3) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < result.stars) Color(0xFFFFD700) else Color(0x5594A3B8),
                modifier = Modifier
                    .size(36.dp)
                    .padding(3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Text("${result.score}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE047))
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("COINS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Text("${result.coinsCollected} / ${result.totalCoins}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFFBEB))
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Text("%.1fs".format(result.timeMs / 1000f), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFFBEB))
          }
          if (result.isTrollLevel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("DEATHS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
              Text(
                  text = "${result.deathsCount}",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFCA5A5),
                  modifier = Modifier.testTag("text_victory_deaths")
              )
            }
          } else if (result.levelId == 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("CLUES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
              Text("${result.cluesDiscoveredCount} / ${result.totalClues}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF67E8F9))
            }
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("RELICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
              Text("${result.relicsFoundCount} / ${result.totalRelics}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF67E8F9))
            }
          }
        }

        if (result.totalDiyas > 0) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
              text = "Brass Diyas Lit: ${result.diyasLit} / ${result.totalDiyas}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFDBA74),
              modifier = Modifier.testTag("text_victory_diyas")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (result.levelId == 1 && !result.isTrollLevel) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF132229))
                  .border(1.dp, if (result.resonanceStoneFound) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(8.dp))
                  .padding(horizontal = 12.dp, vertical = 5.dp)
          ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (result.resonanceStoneFound) Color(0xFF38BDF8) else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (result.resonanceStoneFound)
                  "Bonus Discovery: Resonance Stone Attuned"
                else
                  "Bonus Discovery: Resonance Stone Missed",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (result.resonanceStoneFound) Color(0xFFBAE6FD) else Color(0xFF94A3B8)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        } else if (result.levelId == 5) {
          Text(
              text = if (result.isTrollLevel) {
                "You outsmarted every trap and beat all 5 World 1 Troll Trials!"
              } else {
                "The Last Light burns bright — Vajra-Surya bows and the Sanctum is free!"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFBAE6FD),
              textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        if (onNextLevel != null) {
          Button(
              onClick = onNextLevel,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("button_victory_next_level")
          ) {
            Text(
                text = "NEXT LEVEL (1-${result.levelId + 1})",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color(0xFF091318)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Functional REPLAY LEVEL and MAIN MENU actions + Field Notes access
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
              onClick = onReplay,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("button_victory_replay")
          ) {
            Text("REPLAY LEVEL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF091318))
          }

          OutlinedButton(
              onClick = onMainMenu,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
              modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("button_victory_main_menu")
          ) {
            Text(
                text = if (result.launchMode == RunLaunchMode.REPLAY) "RUN SUMMARY" else "MAIN MENU",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color(0xFFFDE047)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onOpenFieldNotes,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("button_victory_field_notes")
        ) {
          Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFF67E8F9), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("VIEW FIELD NOTES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF67E8F9))
        }
      }
    }
  }
}

/**
 * Multi-touch-isolated virtual control button.
 *
 * Key guarantees:
 * - Keyed on `(controlButton, controlState, resetEpoch)` so visual press recomposition or
 *   player death/respawn never drops an active gesture, while any lifecycle/pause reset
 *   (`clearAll()` incrementing `resetEpoch`) immediately flushes stale pointers & visual press state.
 * - Tracks only the pointer IDs pressed on this button and forwards them to [MultiTouchControlState].
 * - Releasing Jump (or sliding off Jump) NEVER releases a held Forward/Left pointer.
 */
@Composable
fun TouchControlButton(
    controlButton: GameControlButton,
    controlState: MultiTouchControlState,
    icon: ImageVector,
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
    buttonSizeDp: Int = 74,
    accentColor: Color = Color(0xFF0D7377)
) {
  val resetEpoch by controlState.resetEpoch.collectAsState()
  var isPressed by remember(controlButton, controlState, resetEpoch) { mutableStateOf(false) }
  val pressScale by animateFloatAsState(
      targetValue = if (isPressed) 0.92f else 1.0f,
      label = "touchButtonScale"
  )

  DisposableEffect(controlButton, controlState, resetEpoch) {
    onDispose {
      controlState.releaseButtonPointers(controlButton)
    }
  }

  Box(
      modifier = modifier
          .size(buttonSizeDp.dp)
          .scale(pressScale)
          .shadow(if (isPressed) 14.dp else 8.dp, CircleShape)
          .clip(CircleShape)
          .background(
              Brush.radialGradient(
                  colors = if (isPressed)
                    listOf(accentColor.copy(alpha = 0.85f), Color(0xEE1E293B))
                  else
                    listOf(Color(0xBB253844), Color(0x990D1A21))
              )
          )
          .border(
              width = if (isPressed) 3.dp else 2.dp,
              color = if (isPressed) Color(0xFFFFD700) else accentColor,
              shape = CircleShape
          )
          .pointerInput(controlButton, controlState, resetEpoch) {
            val localPointers = LinkedHashSet<Long>(4)
            val edgeSlopPx = 28f
            try {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Initial)
                  val boundsW = this.size.width.toFloat()
                  val boundsH = this.size.height.toFloat()
                  for (change in event.changes) {
                    val pid = change.id.value
                    val x = change.position.x
                    val y = change.position.y
                    val inBounds = x >= -edgeSlopPx &&
                        x <= boundsW + edgeSlopPx &&
                        y >= -edgeSlopPx &&
                        y <= boundsH + edgeSlopPx

                    when {
                      change.changedToDownIgnoreConsumed() -> {
                        localPointers.add(pid)
                        controlState.onPointerDown(pid, controlButton)
                        change.consume()
                      }
                      change.changedToUpIgnoreConsumed() -> {
                        if (localPointers.remove(pid)) {
                          controlState.onPointerUp(pid, controlButton)
                          change.consume()
                        }
                      }
                      !change.pressed && localPointers.contains(pid) -> {
                        localPointers.remove(pid)
                        controlState.onPointerUp(pid, controlButton)
                        change.consume()
                      }
                      change.pressed && localPointers.contains(pid) -> {
                        if (!inBounds) {
                          localPointers.remove(pid)
                          controlState.onPointerUp(pid, controlButton)
                        }
                        change.consume()
                      }
                    }
                  }

                  isPressed = localPointers.isNotEmpty()
                }
              }
            } finally {
              localPointers.clear()
              controlState.releaseButtonPointers(controlButton)
              isPressed = false
            }
          }
          .testTag(testTag),
      contentAlignment = Alignment.Center
  ) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = if (isPressed) Color(0xFFFFD700) else Color(0xFFF8FAFC),
        modifier = Modifier.size((buttonSizeDp * 0.46f).dp)
    )
  }
}

@Composable
private fun GameplayNotificationPanel(notification: GameplayNotification) {
  val visualStyle = when (notification.visualKind) {
    NotificationVisualKind.FALLING_HAZARD -> NotificationVisualStyle(
        accent = Color(0xFFF97316),
        bg = Color(0xCC18100A),
        icon = Icons.Default.ArrowDownward,
        tint = Color(0xFFFDBA74)
    )
    NotificationVisualKind.LIGHTNING_HAZARD -> NotificationVisualStyle(
        accent = Color(0xFF38BDF8),
        bg = Color(0xCC0A1622),
        icon = Icons.Default.Bolt,
        tint = Color(0xFFFDE047)
    )
    NotificationVisualKind.BOSS_WARNING -> NotificationVisualStyle(
        accent = Color(0xFFEF4444),
        bg = Color(0xCC1A0B0E),
        icon = Icons.Default.Shield,
        tint = Color(0xFFFCA5A5)
    )
    NotificationVisualKind.HAZARD_WARNING -> NotificationVisualStyle(
        accent = Color(0xFFF59E0B),
        bg = Color(0xCC161009),
        icon = Icons.Default.WarningAmber,
        tint = Color(0xFFFCD34D)
    )
    NotificationVisualKind.DISCOVERY -> NotificationVisualStyle(
        accent = Color(0xFF38BDF8),
        bg = Color(0xCC09151E),
        icon = Icons.Default.AutoAwesome,
        tint = Color(0xFF7DD3FC)
    )
    NotificationVisualKind.TRIUMPH -> NotificationVisualStyle(
        accent = Color(0xFFFACC15),
        bg = Color(0xCC10180D),
        icon = Icons.Default.EmojiEvents,
        tint = Color(0xFFFDE047)
    )
    NotificationVisualKind.INFO -> NotificationVisualStyle(
        accent = Color(0xFF2DD4BF),
        bg = Color(0xB80A161B),
        icon = Icons.Default.Info,
        tint = Color(0xFF99F6E4)
    )
    NotificationVisualKind.STORY -> NotificationVisualStyle(
        accent = Color(0xFF14B8A6),
        bg = Color(0xCC091419),
        icon = if (notification.eventKey.contains("WATCHER")) Icons.Default.Visibility else Icons.AutoMirrored.Filled.MenuBook,
        tint = Color(0xFF5EEAD4)
    )
  }

  if (notification.priority == NotificationPriority.INFO) {
    // Smallest compact presentation for routine INFO messages
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 26.dp, max = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(visualStyle.bg)
            .border(1.dp, visualStyle.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("whisper_banner")
    ) {
      Icon(
          imageVector = visualStyle.icon,
          contentDescription = null,
          tint = visualStyle.tint,
          modifier = Modifier
              .size(13.dp)
              .testTag("toast_category_icon")
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
          text = notification.message,
          fontSize = 10.sp,
          lineHeight = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFE2E8F0),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
      )
    }
    return
  }

  // Compact translucent dark card: icon + short title + 1-line message (max 2 lines total)
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
          .height(IntrinsicSize.Min)
          .heightIn(min = 38.dp, max = 44.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(visualStyle.bg)
          .border(1.dp, visualStyle.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
          .testTag("whisper_banner")
  ) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight()
            .background(visualStyle.accent)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Icon(
        imageVector = visualStyle.icon,
        contentDescription = null,
        tint = visualStyle.tint,
        modifier = Modifier
            .size(16.dp)
            .testTag("toast_category_icon")
    )
    Spacer(modifier = Modifier.width(7.dp))
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(end = 10.dp, top = 3.dp, bottom = 3.dp)
    ) {
      Text(
          text = notification.title,
          fontSize = 10.sp,
          lineHeight = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = visualStyle.tint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("notification_title")
      )
      Text(
          text = notification.message,
          fontSize = 11.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFF1F5F9),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private data class NotificationVisualStyle(
    val accent: Color,
    val bg: Color,
    val icon: ImageVector,
    val tint: Color
)

@Composable
private fun HudTimerText(timerTenthsProvider: () -> Int) {
  val tenths = timerTenthsProvider()
  Text(
      text = "%.1fs".format(tenths / 10f),
      fontSize = 12.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      color = Color(0xFFE2E8F0),
      maxLines = 1,
      modifier = Modifier.testTag("hud_timer_text")
  )
}

@Composable
private fun GameplayNotificationOverlay(
    sessionId: Long,
    engine: com.example.engine.ChaosGameEngine,
    renderer: com.example.engine.ChaosGameRenderer,
    activeNotification: GameplayNotification?,
    isVisible: Boolean,
    isEffectivePaused: Boolean,
    hasVictory: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp,
    rootBounds: Rect,
    safeAreaBounds: Rect,
    hudBounds: Rect,
    leftControlBounds: Rect,
    rightControlBounds: Rect,
    jumpControlBounds: Rect
) {
  val density = LocalDensity.current
  val reusableExclusions = remember(sessionId) { ArrayList<Rect>(16) }
  val reusableControls = remember(sessionId) { ArrayList<Rect>(3) }
  val camXYBuffer = remember(sessionId) { FloatArray(2) }
  var placementPulse by remember(sessionId) { mutableIntStateOf(0) }
  var lastNotif by remember(sessionId) { mutableStateOf<GameplayNotification?>(null) }
  if (activeNotification != null) {
    lastNotif = activeNotification
  }
  val renderNotif = activeNotification ?: lastNotif

  val shouldTrackPlacement = activeNotification != null && !isEffectivePaused && !hasVictory
  LaunchedEffect(sessionId, activeNotification?.eventKey, shouldTrackPlacement) {
    if (!shouldTrackPlacement) return@LaunchedEffect
    while (true) {
      kotlinx.coroutines.delay(80L)
      placementPulse++
    }
  }

  val isPortrait = maxHeight > maxWidth
  val isInfoOnly = renderNotif?.priority == NotificationPriority.INFO
  val maxBannerWidthDp = when {
    isInfoOnly -> (maxWidth * 0.36f).coerceIn(160.dp, 230.dp)
    isPortrait -> (maxWidth - 8.dp).coerceIn(200.dp, 280.dp)
    else -> (maxWidth * 0.32f).coerceIn(195.dp, 268.dp)
  }

  var lastPlacement by remember(sessionId) {
    mutableStateOf(
        NotificationPlacementResult(
            slot = NotificationSlot.LOWER_LEFT_SAFE,
            rect = Rect.Zero,
            topLeftOffset = androidx.compose.ui.geometry.Offset.Zero
        )
    )
  }

  if (activeNotification != null) {
    // Depend explicitly on placementPulse so periodic refreshes occur only while notification is active
    @Suppress("UNUSED_VARIABLE")
    val pulseTick = placementPulse
    val targetBannerHeightDp = if (isInfoOnly) 28.dp else 42.dp
    val bannerWidthPx = with(density) { maxBannerWidthDp.toPx() }
    val bannerHeightPx = with(density) { targetBannerHeightDp.toPx() }
    val gapPx = with(density) { 10.dp.toPx() }

    reusableControls.clear()
    if (leftControlBounds.width > 0f) reusableControls.add(leftControlBounds)
    if (rightControlBounds.width > 0f) reusableControls.add(rightControlBounds)
    if (jumpControlBounds.width > 0f) reusableControls.add(jumpControlBounds)

    val effectiveSafeBounds = if (safeAreaBounds.width > 0f) {
      safeAreaBounds
    } else {
      Rect(
          left = with(density) { 22.dp.toPx() },
          top = with(density) { 8.dp.toPx() },
          right = with(density) { (maxWidth + 22.dp).toPx() },
          bottom = with(density) { (maxHeight + 8.dp).toPx() }
      )
    }
    val viewportW = if (rootBounds.width > 0f) rootBounds.width else with(density) { maxWidth.toPx() }
    val viewportH = if (rootBounds.height > 0f) rootBounds.height else with(density) { maxHeight.toPx() }

    val exclusionData = NotificationPlacementResolver.buildGameplayExclusionData(
        engine = engine,
        renderer = renderer,
        viewportWidthPx = viewportW,
        viewportHeightPx = viewportH,
        notification = activeNotification,
        reusableExclusions = reusableExclusions,
        camXYBuffer = camXYBuffer
    )
    lastPlacement = NotificationPlacementResolver.resolvePlacement(
        safeAreaBounds = effectiveSafeBounds,
        hudBounds = hudBounds,
        controlBounds = reusableControls,
        exclusionData = exclusionData,
        bannerWidthPx = bannerWidthPx,
        bannerHeightPx = bannerHeightPx,
        gapPx = gapPx,
        priority = activeNotification.priority
    )
  }

  val placement = lastPlacement
  if (renderNotif == null) return

  AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(180)) {
        if (placement.slot == NotificationSlot.UPPER_LEFT_HUD || placement.slot == NotificationSlot.UPPER_RIGHT_HUD) {
          -it / 3
        } else {
          it / 3
        }
      },
      exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) {
        if (placement.slot == NotificationSlot.UPPER_LEFT_HUD || placement.slot == NotificationSlot.UPPER_RIGHT_HUD) {
          -it / 4
        } else {
          it / 4
        }
      },
      modifier = Modifier
          .offset {
            IntOffset(
                x = placement.topLeftOffset.x.roundToInt(),
                y = placement.topLeftOffset.y.roundToInt()
            )
          }
          .widthIn(max = maxBannerWidthDp)
          .testTag("notification_slot_${placement.slot.name.lowercase()}")
  ) {
    GameplayNotificationPanel(notification = renderNotif)
  }
}
