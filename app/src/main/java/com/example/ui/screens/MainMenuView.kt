package com.example.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.GameStateViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simplified, immediate Home Screen for CHAOS RUN 5.
 * Contains ONLY:
 * 1. CHAOS RUN 5 title/logo
 * 2. Prominent TAP TO PLAY button (launches/resumes continuous progression directly)
 * 3. Rewards button
 * 4. Settings button
 */
@Composable
fun MainMenuView(
    viewModel: GameStateViewModel,
    modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "homeAtmosphere")
  val titleFloat by infiniteTransition.animateFloat(
      initialValue = -2.2f,
      targetValue = 2.2f,
      animationSpec = infiniteRepeatable(
          animation = tween(2800, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse
      ),
      label = "titleFloat"
  )
  val fogPhase by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
          animation = tween(14000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart
      ),
      label = "fogPhase"
  )
  val eyeGlow by infiniteTransition.animateFloat(
      initialValue = 0.28f,
      targetValue = 0.72f,
      animationSpec = infiniteRepeatable(
          animation = tween(3600, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse
      ),
      label = "eyeGlow"
  )
  val ctaPulse by infiniteTransition.animateFloat(
      initialValue = 0.78f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
          animation = tween(2200, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse
      ),
      label = "ctaPulse"
  )

  Box(
      modifier = modifier
          .fillMaxSize()
          .testTag("screen_main_menu")
  ) {
    // 1. Illustrated forest concept artwork backdrop
    Image(
        painter = painterResource(id = R.drawable.img_chaos_run_concept_1794492614247),
        contentDescription = "Atmospheric twilight forest background",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    // 2. Twilight scrim for high text contrast
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD8050D12),
                        Color(0xA608161C),
                        Color(0xE8040A0E)
                    )
                )
            )
    )

    // 3. Atmospheric forest silhouettes, drifting mist, glow & subtle watching eyes
    SuspenseForestAtmosphereLayer(
        fogPhase = fogPhase,
        eyeGlow = eyeGlow,
        modifier = Modifier.fillMaxSize()
    )

    // 4. Clean Centered Menu Layout
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      val compactHeight = maxHeight < 370.dp
      val titleFontSize = if (compactHeight) 32.sp else 40.sp

      Column(
          modifier = Modifier
              .align(Alignment.Center)
              .widthIn(max = 420.dp)
              .fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(if (compactHeight) 14.dp else 22.dp)
      ) {
        // 1. CHAOS RUN 5 Title / Logo
        Column(
            modifier = Modifier
                .offset(y = titleFloat.dp)
                .testTag("home_logo_container"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
              text = "CHAOS RUN 5",
              fontSize = titleFontSize,
              fontWeight = FontWeight.Black,
              letterSpacing = 4.sp,
              color = Color(0xFFFFFBEB),
              textAlign = TextAlign.Center,
              maxLines = 1,
              modifier = Modifier.testTag("text_home_title")
          )
          Box(
              modifier = Modifier
                  .width(140.dp)
                  .height(3.5.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(
                      Brush.horizontalGradient(
                          colors = listOf(
                              Color.Transparent,
                              Color(0xFFF59E0B),
                              Color(0xFFFDE047),
                              Color(0xFFF59E0B),
                              Color.Transparent
                          )
                      )
                  )
          )
        }

        // 2. Prominent TAP TO PLAY Button (direct gameplay entry)
        val ctaBorderColor = Color(0xFFFBBF24).copy(alpha = 0.72f + 0.28f * ctaPulse)
        Button(
            onClick = { viewModel.startOrContinueContinuousRun() },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF59E0B),
                contentColor = Color(0xFF081014)
            ),
            border = BorderStroke(2.dp, ctaBorderColor),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 4.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color(0xFFF59E0B),
                    spotColor = Color(0xFFF59E0B)
                )
                .testTag("button_tap_to_start")
        ) {
          Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = "TAP TO PLAY",
              fontSize = 19.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp,
              maxLines = 1
          )
        }

        // 3. Secondary Menu Actions: Rewards & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
              onClick = { viewModel.openProgressionHub(fromCompletionScreen = false) },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.5.dp, Color(0xFF14B8A6).copy(alpha = 0.85f)),
              colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = Color(0xCC0B181E),
                  contentColor = Color(0xFF67E8F9)
              ),
              modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 50.dp)
                  .testTag("button_rewards")
          ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REWARDS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1
            )
          }

          OutlinedButton(
              onClick = { viewModel.showSettings() },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.72f)),
              colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = Color(0xCC0B181E),
                  contentColor = Color(0xFFFDE68A)
              ),
              modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 50.dp)
                  .testTag("button_settings")
          ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFFFDE68A),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SETTINGS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SuspenseForestAtmosphereLayer(
    fogPhase: Float,
    eyeGlow: Float,
    modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x36F59E0B),
                Color(0x1814B8A6),
                Color.Transparent
            ),
            center = Offset(w * 0.5f, h * 0.54f),
            radius = maxOf(w, h) * 0.44f
        ),
        radius = maxOf(w, h) * 0.44f,
        center = Offset(w * 0.5f, h * 0.54f)
    )

    val leftSilhouette = Path().apply {
      moveTo(0f, 0f)
      lineTo(w * 0.14f, 0f)
      lineTo(w * 0.09f, h * 0.24f)
      lineTo(w * 0.13f, h * 0.48f)
      lineTo(w * 0.07f, h * 0.78f)
      lineTo(w * 0.17f, h)
      lineTo(0f, h)
      close()
    }
    drawPath(leftSilhouette, color = Color(0xBB03080B))

    val rightSilhouette = Path().apply {
      moveTo(w, 0f)
      lineTo(w * 0.85f, 0f)
      lineTo(w * 0.90f, h * 0.28f)
      lineTo(w * 0.86f, h * 0.55f)
      lineTo(w * 0.92f, h * 0.82f)
      lineTo(w * 0.82f, h)
      lineTo(w, h)
      close()
    }
    drawPath(rightSilhouette, color = Color(0xBB03080B))

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xD903070A), Color.Transparent),
            startY = 0f,
            endY = h * 0.24f
        ),
        size = Size(w, h * 0.24f)
    )

    drawWatchingEyePair(
        center = Offset(w * 0.07f, h * 0.34f),
        eyeSpacing = 12f,
        eyeRadius = 3.2f,
        color = Color(0xFF67E8F9).copy(alpha = eyeGlow * 0.72f)
    )
    drawWatchingEyePair(
        center = Offset(w * 0.93f, h * 0.44f),
        eyeSpacing = 13f,
        eyeRadius = 3.4f,
        color = Color(0xFFFBBF24).copy(alpha = (0.95f - eyeGlow * 0.55f) * 0.68f)
    )
    drawWatchingEyePair(
        center = Offset(w * 0.10f, h * 0.74f),
        eyeSpacing = 10f,
        eyeRadius = 2.6f,
        color = Color(0xFFFDE047).copy(alpha = eyeGlow * 0.45f)
    )

    val angle = fogPhase * 2f * PI.toFloat()
    val mist1X = w * 0.25f + sin(angle) * w * 0.09f
    val mist2X = w * 0.72f + cos(angle) * w * 0.08f
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0x1F94A3B8), Color.Transparent)
        ),
        topLeft = Offset(mist1X - w * 0.25f, h * 0.74f),
        size = Size(w * 0.60f, h * 0.16f),
        cornerRadius = CornerRadius(80f, 80f)
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0x18CBD5E1), Color.Transparent)
        ),
        topLeft = Offset(mist2X - w * 0.28f, h * 0.82f),
        size = Size(w * 0.62f, h * 0.18f),
        cornerRadius = CornerRadius(90f, 90f)
    )

    val fireflies = listOf(
        Triple(0.18f, 0.24f, 0f),
        Triple(0.28f, 0.68f, 1.4f),
        Triple(0.76f, 0.28f, 2.7f),
        Triple(0.82f, 0.70f, 4.1f),
        Triple(0.45f, 0.18f, 5.2f)
    )
    for ((fx, fy, phaseOffset) in fireflies) {
      val driftX = w * fx + sin(angle + phaseOffset) * 14f
      val driftY = h * fy + cos(angle * 0.8f + phaseOffset) * 10f
      val alpha = 0.25f + 0.25f * sin(angle + phaseOffset)
      drawCircle(
          color = Color(0xFFFDE047).copy(alpha = alpha.coerceIn(0.12f, 0.55f)),
          radius = 3.2f,
          center = Offset(driftX, driftY)
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWatchingEyePair(
    center: Offset,
    eyeSpacing: Float,
    eyeRadius: Float,
    color: Color
) {
  val half = eyeSpacing * 0.5f
  drawCircle(
      color = color.copy(alpha = color.alpha * 0.28f),
      radius = eyeRadius * 2.4f,
      center = Offset(center.x - half, center.y)
  )
  drawCircle(
      color = color.copy(alpha = color.alpha * 0.28f),
      radius = eyeRadius * 2.4f,
      center = Offset(center.x + half, center.y)
  )
  drawCircle(
      color = color,
      radius = eyeRadius,
      center = Offset(center.x - half, center.y)
  )
  drawCircle(
      color = color,
      radius = eyeRadius,
      center = Offset(center.x + half, center.y)
  )
}
