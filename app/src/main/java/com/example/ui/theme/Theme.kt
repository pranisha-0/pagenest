package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
  LIGHT,
  DARK,
  WARM_PAPER
}

private val LightColorScheme = lightColorScheme(
  primary = LavenderPrimary,
  onPrimary = Color.White,
  primaryContainer = LavenderLight,
  onPrimaryContainer = LavenderPrimaryDark,
  secondary = LavenderAccent,
  onSecondary = Color.White,
  background = CreamBackground,
  onBackground = CharcoalText,
  surface = CreamSurface,
  onSurface = CharcoalText,
  surfaceVariant = CreamSurfaceVariant,
  onSurfaceVariant = CharcoalTextSecondary,
  outline = Color(0xFFE2DCD5)
)

private val DarkColorScheme = darkColorScheme(
  primary = DarkLavenderPrimary,
  onPrimary = DarkBackground,
  primaryContainer = DarkSurfaceVariant,
  onPrimaryContainer = DarkTextPrimary,
  secondary = LavenderAccent,
  onSecondary = Color.White,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = Color(0xFF3B3549)
)

private val PaperColorScheme = lightColorScheme(
  primary = Color(0xFF6B5842),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFEDE0D0),
  onPrimaryContainer = Color(0xFF382D1D),
  secondary = Color(0xFF8A7760),
  onSecondary = Color.White,
  background = PaperBackground,
  onBackground = PaperText,
  surface = PaperSurface,
  onSurface = PaperText,
  surfaceVariant = Color(0xFFEADBCE),
  onSurfaceVariant = Color(0xFF5E5446),
  outline = Color(0xFFD8C7B5)
)

@Composable
fun PageNestTheme(
  themeMode: AppThemeMode = AppThemeMode.LIGHT,
  content: @Composable () -> Unit
) {
  val colorScheme: ColorScheme = when (themeMode) {
    AppThemeMode.LIGHT -> LightColorScheme
    AppThemeMode.DARK -> DarkColorScheme
    AppThemeMode.WARM_PAPER -> PaperColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
