package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ForestDarkColorScheme = darkColorScheme(
    primary = AmberRune,
    onPrimary = ForestNight,
    secondary = MysticTeal,
    onSecondary = ForestNight,
    tertiary = EmeraldTrail,
    onTertiary = ForestNight,
    background = ForestNight,
    onBackground = MistCream,
    surface = CanopySurface,
    onSurface = MistCream,
    surfaceVariant = CanopyElevated,
    onSurfaceVariant = SlateMist,
    outline = AmberRune
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
  MaterialTheme(
      colorScheme = ForestDarkColorScheme,
      typography = Typography,
      content = content
  )
}
