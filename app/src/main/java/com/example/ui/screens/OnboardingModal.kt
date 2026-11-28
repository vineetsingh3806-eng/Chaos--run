package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingStepData(
    val stepNumber: Int,
    val badge: String,
    val title: String,
    val summary: String,
    val tip: String,
    val accentColor: Color,
    val icon: ImageVector
)

val ONBOARDING_STEPS = listOf(
    OnboardingStepData(
        stepNumber = 1,
        badge = "CONTROLS",
        title = "RUN & JUMP TOGETHER",
        summary = "Hold Left or Right with your left thumb while tapping Jump with your right thumb.",
        tip = "Tap Jump again in mid-air for a Double Jump—two crisp leaps per airborne sequence!",
        accentColor = Color(0xFF10B981),
        icon = Icons.Default.TouchApp
    ),
    OnboardingStepData(
        stepNumber = 2,
        badge = "HAZARDS",
        title = "OUTSMART TRICK TILES",
        summary = "Watch for cracks, glints, shifting stones, and pop-up spikes before you land.",
        tip = "Don't trust suspiciously flat safe paths—bait traps first or leap past them cleanly!",
        accentColor = Color(0xFFEF4444),
        icon = Icons.Default.WarningAmber
    ),
    OnboardingStepData(
        stepNumber = 3,
        badge = "GOAL",
        title = "REACH THE TRUE SUN GATE",
        summary = "Cross the glowing golden Sun Gate at the end of each trial to advance forward.",
        tip = "Careful near the finish: cheeky gates may dodge or disguise fakes before the real exit settles!",
        accentColor = Color(0xFFF59E0B),
        icon = Icons.Default.Flag
    ),
    OnboardingStepData(
        stepNumber = 4,
        badge = "RETRY LOOP",
        title = "FAST DEATH, INSTANT RETRY",
        summary = "Traps reset Aarav straight back in a snap with zero waiting screens.",
        tip = "Remember every gag, keep momentum flowing, and clear all 200 trials in one continuous run!",
        accentColor = Color(0xFF38BDF8),
        icon = Icons.Default.Refresh
    )
)

/**
 * Compact, non-blocking first-time welcome guide rendered cleanly in open menu space
 * only when onboarding has not yet been completed or skipped.
 */
@Composable
fun FirstLaunchOnboardingCard(
    onOpenFullGuide: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
  var stepIndex by remember { mutableIntStateOf(0) }
  val step = ONBOARDING_STEPS[stepIndex]
  val isLastStep = stepIndex == ONBOARDING_STEPS.lastIndex

  Column(
      modifier = modifier
          .shadow(10.dp, RoundedCornerShape(14.dp))
          .clip(RoundedCornerShape(14.dp))
          .background(
              Brush.verticalGradient(
                  colors = listOf(Color(0xEE142730), Color(0xEE0B171D))
              )
          )
          .border(1.8.dp, step.accentColor, RoundedCornerShape(14.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("card_first_launch_onboarding"),
      verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
            imageVector = step.icon,
            contentDescription = null,
            tint = step.accentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "QUICK GUIDE • ${step.stepNumber}/${ONBOARDING_STEPS.size}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = step.accentColor
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "TOUR",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF67E8F9),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1D313B))
                .clickable { onOpenFullGuide() }
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .testTag("button_first_launch_tour")
        )
        Text(
            text = "SKIP",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSkip() }
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .testTag("button_first_launch_skip")
        )
      }
    }

    Text(
        text = step.title,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFFFBEB),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Text(
        text = step.summary,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        color = Color(0xFFCBD5E1),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )

    // Step indicator dots + Next / Got It action
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        ONBOARDING_STEPS.forEachIndexed { idx, s ->
          Box(
              modifier = Modifier
                  .height(6.dp)
                  .width(if (idx == stepIndex) 18.dp else 6.dp)
                  .clip(CircleShape)
                  .background(if (idx == stepIndex) s.accentColor else Color(0xFF334155))
          )
        }
      }

      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(step.accentColor)
              .clickable {
                if (isLastStep) {
                  onComplete()
                } else {
                  stepIndex++
                }
              }
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .testTag("button_first_launch_next")
      ) {
        Text(
            text = if (isLastStep) "GOT IT!" else "NEXT",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF091318)
        )
        if (!isLastStep) {
          Spacer(modifier = Modifier.width(3.dp))
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              tint = Color(0xFF091318),
              modifier = Modifier.size(12.dp)
          )
        }
      }
    }
  }
}

/**
 * Full-screen interactive 4-step cartoon Onboarding Tour.
 * Re-accessible anytime from Main Menu ("TOUR"), How To Play, or Settings.
 */
@Composable
fun OnboardingModal(
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
  var currentStepIndex by remember { mutableIntStateOf(0) }
  val step = ONBOARDING_STEPS[currentStepIndex]
  val isFirst = currentStepIndex == 0
  val isLast = currentStepIndex == ONBOARDING_STEPS.lastIndex

  Box(
      modifier = modifier
          .fillMaxSize()
          .background(Color(0xE6060D11))
          .safeDrawingPadding()
          .testTag("modal_onboarding"),
      contentAlignment = Alignment.Center
  ) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101E24)),
        border = BorderStroke(2.5.dp, step.accentColor),
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth(0.92f)
            .heightIn(max = 390.dp)
            .padding(10.dp)
    ) {
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 20.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Header: Badge + Step Progress + Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(step.accentColor.copy(alpha = 0.2f))
                    .border(1.dp, step.accentColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                  text = "STEP ${step.stepNumber} OF ${ONBOARDING_STEPS.size} • ${step.badge}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp,
                  color = step.accentColor
              )
            }
          }

          TextButton(
              onClick = onSkip,
              modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("button_onboarding_skip")
          ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Skip Guide",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SKIP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1)
            )
          }
        }

        // Middle Content: Cartoon Visual Schematic + Crisp Rule Copy
        AnimatedContent(
            targetState = currentStepIndex,
            transitionSpec = {
              fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
            },
            modifier = Modifier.weight(1f),
            label = "onboardingStepContent"
        ) { idx ->
          val activeStep = ONBOARDING_STEPS[idx]
          Row(
              modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState())
                  .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            // Illustrated Cartoon Preview Tile
            OnboardingStepIllustration(
                stepNumber = activeStep.stepNumber,
                accentColor = activeStep.accentColor,
                modifier = Modifier
                    .width(190.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF091318))
                    .border(1.5.dp, Color(0xFF283D47), RoundedCornerShape(16.dp))
            )

            // Copy & Actionable Tip
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                  text = activeStep.title,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFFFBEB),
                  modifier = Modifier.testTag("text_onboarding_title")
              )
              Text(
                  text = activeStep.summary,
                  fontSize = 13.5.sp,
                  lineHeight = 19.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFFE2E8F0)
              )
              Row(
                  verticalAlignment = Alignment.Top,
                  modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .background(Color(0xFF091318))
                      .border(1.dp, activeStep.accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                      .padding(10.dp)
              ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = activeStep.accentColor,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeStep.tip,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFCBD5E1)
                )
              }
            }
          }
        }

        // Bottom Controls: Step Pills + Back / Next / Start Playing
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ONBOARDING_STEPS.forEachIndexed { idx, s ->
              val selected = idx == currentStepIndex
              Box(
                  modifier = Modifier
                      .height(10.dp)
                      .width(if (selected) 28.dp else 10.dp)
                      .clip(CircleShape)
                      .background(if (selected) s.accentColor else Color(0xFF334155))
                      .clickable { currentStepIndex = idx }
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isFirst) {
              OutlinedButton(
                  onClick = { currentStepIndex-- },
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.5.dp, Color(0xFF475569)),
                  modifier = Modifier
                      .height(48.dp)
                      .testTag("button_onboarding_prev")
              ) {
                Text(
                    text = "BACK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0)
                )
              }
            }

            if (isLast) {
              Button(
                  onClick = onFinish,
                  colors = ButtonDefaults.buttonColors(containerColor = step.accentColor),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                      .height(48.dp)
                      .testTag("button_onboarding_done")
              ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = null,
                    tint = Color(0xFF091318),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "START RUNNING!",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFF091318)
                )
              }
            } else {
              Button(
                  onClick = { currentStepIndex++ },
                  colors = ButtonDefaults.buttonColors(containerColor = step.accentColor),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                      .height(48.dp)
                      .testTag("button_onboarding_next")
              ) {
                Text(
                    text = "NEXT",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFF091318)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF091318),
                    modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingStepIllustration(
    stepNumber: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Subtle twilight sky backdrop + grass ledge matching Chaos Run aesthetic
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D2129), Color(0xFF15323D))
        )
    )
    drawRoundRect(
        color = Color(0xFF3E2723),
        topLeft = Offset(0f, h * 0.76f),
        size = Size(w, h * 0.24f)
    )
    drawRect(
        color = Color(0xFF10B981),
        topLeft = Offset(0f, h * 0.74f),
        size = Size(w, h * 0.05f)
    )

    when (stepNumber) {
      1 -> {
        // Controls schematic: Left/Right thumb pads + Jump pad + leaping Aarav silhouette
        drawCircle(
            color = Color(0xFF0D7377),
            radius = w * 0.095f,
            center = Offset(w * 0.18f, h * 0.82f),
            style = Stroke(width = 5f)
        )
        drawCircle(
            color = Color(0xFF10B981),
            radius = w * 0.105f,
            center = Offset(w * 0.40f, h * 0.82f),
            style = Stroke(width = 5f)
        )
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = w * 0.115f,
            center = Offset(w * 0.82f, h * 0.80f),
            style = Stroke(width = 6f)
        )
        // Jump arc
        val arc = Path().apply {
          moveTo(w * 0.24f, h * 0.62f)
          quadraticTo(w * 0.50f, h * 0.18f, w * 0.74f, h * 0.54f)
        }
        drawPath(
            path = arc,
            color = Color(0xFFFDE047),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        // Aarav cartoon runner badge mid-leap
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(w * 0.45f, h * 0.26f),
            size = Size(w * 0.12f, h * 0.20f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawCircle(
            color = Color(0xFFFFE0B2),
            radius = w * 0.055f,
            center = Offset(w * 0.51f, h * 0.22f)
        )
      }
      2 -> {
        // Cracked trap bridge + pop-up warning spikes + exclamation clue
        drawRoundRect(
            color = Color(0xFF78350F),
            topLeft = Offset(w * 0.16f, h * 0.56f),
            size = Size(w * 0.68f, h * 0.10f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Crack lines
        drawLine(
            color = Color(0xFFFDE047),
            start = Offset(w * 0.48f, h * 0.56f),
            end = Offset(w * 0.54f, h * 0.66f),
            strokeWidth = 4f
        )
        // Two coral spikes poking up
        for (sx in listOf(0.32f, 0.64f)) {
          val spike = Path().apply {
            moveTo(w * (sx - 0.05f), h * 0.74f)
            lineTo(w * sx, h * 0.56f)
            lineTo(w * (sx + 0.05f), h * 0.74f)
            close()
          }
          drawPath(spike, Color(0xFFEF4444))
        }
        // Telegraph alert circle
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.25f),
            radius = w * 0.13f,
            center = Offset(w * 0.50f, h * 0.30f)
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = w * 0.13f,
            center = Offset(w * 0.50f, h * 0.30f),
            style = Stroke(width = 4f)
        )
        drawLine(
            color = Color(0xFFFFFBEB),
            start = Offset(w * 0.50f, h * 0.22f),
            end = Offset(w * 0.50f, h * 0.31f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFFFFBEB),
            radius = 3.5f,
            center = Offset(w * 0.50f, h * 0.365f)
        )
      }
      3 -> {
        // Glowing golden Sun Gate arch
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = 0.22f),
            radius = w * 0.26f,
            center = Offset(w * 0.52f, h * 0.48f)
        )
        drawRoundRect(
            color = Color(0xFFFFD700),
            topLeft = Offset(w * 0.37f, h * 0.26f),
            size = Size(w * 0.30f, h * 0.48f),
            cornerRadius = CornerRadius(32f, 32f),
            style = Stroke(width = 8f)
        )
        drawRoundRect(
            color = Color(0xFF67E8F9).copy(alpha = 0.45f),
            topLeft = Offset(w * 0.41f, h * 0.31f),
            size = Size(w * 0.22f, h * 0.42f),
            cornerRadius = CornerRadius(24f, 24f)
        )
      }
      else -> {
        // Instant retry loop + sparkles
        drawArc(
            color = accentColor,
            startAngle = -45f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.22f),
            size = Size(w * 0.40f, h * 0.40f),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color(0xFFFDE047),
            radius = 6f,
            center = Offset(w * 0.26f, h * 0.30f)
        )
        drawCircle(
            color = Color(0xFF67E8F9),
            radius = 5f,
            center = Offset(w * 0.75f, h * 0.28f)
        )
        drawCircle(
            color = Color(0xFF10B981),
            radius = 5f,
            center = Offset(w * 0.68f, h * 0.60f)
        )
      }
    }
  }
}
