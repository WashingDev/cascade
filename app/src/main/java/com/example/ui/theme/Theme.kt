package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = CobaltPrimaryLight,
  onPrimary = Color.White,
  primaryContainer = CobaltContainerLight,
  onPrimaryContainer = CobaltOnContainerLight,
  secondary = TerracottaSecondaryLight,
  onSecondary = Color.White,
  secondaryContainer = TerracottaContainerLight,
  onSecondaryContainer = TerracottaOnContainerLight,
  tertiary = EmeraldTertiaryLight,
  onTertiary = Color.White,
  tertiaryContainer = EmeraldContainerLight,
  onTertiaryContainer = EmeraldOnContainerLight,
  background = PaperBackgroundLight,
  onBackground = InkDark,
  surface = SurfaceLight,
  onSurface = InkDark,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = InkMutedLight,
  surfaceContainerLow = SurfaceContainerLowLight,
  surfaceContainerHigh = SurfaceContainerHighLight,
  outline = OutlineLight,
  error = ErrorLight,
  onError = Color.White,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = CobaltPrimaryDark,
  onPrimary = Color(0xFF06184F),
  primaryContainer = CobaltContainerDark,
  onPrimaryContainer = CobaltOnContainerDark,
  secondary = TerracottaSecondaryDark,
  onSecondary = Color(0xFF3E1506),
  secondaryContainer = TerracottaContainerDark,
  onSecondaryContainer = TerracottaOnContainerDark,
  tertiary = EmeraldTertiaryDark,
  onTertiary = Color(0xFF042912),
  tertiaryContainer = EmeraldContainerDark,
  onTertiaryContainer = EmeraldOnContainerDark,
  background = SlateBackgroundDark,
  onBackground = InkLight,
  surface = SurfaceDark,
  onSurface = InkLight,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = InkMutedDark,
  surfaceContainerLow = SurfaceContainerLowDark,
  surfaceContainerHigh = SurfaceContainerHighDark,
  outline = OutlineDark,
  error = ErrorDark,
  onError = Color(0xFF3F0C0C),
  errorContainer = ErrorContainerDark,
  onErrorContainer = OnErrorContainerDark,
)

private val CadenceShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CadenceTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = CadenceTypography,
    shapes = CadenceShapes,
    content = content,
  )
}
