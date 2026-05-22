package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ElegantDarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = DarkPurpleAccent,
    primaryContainer = CardBgPrimary,
    onPrimaryContainer = LightText,
    secondary = AccentPurple,
    onSecondary = DarkPurpleAccent,
    secondaryContainer = CardBgSecondary,
    onSecondaryContainer = LightText,
    background = DarkBg,
    onBackground = LightText,
    surface = CardBgPrimary,
    onSurface = LightText,
    surfaceVariant = CardBgSecondary,
    onSurfaceVariant = GrayMuted,
    outline = BorderColor,
    outlineVariant = BorderColor.copy(alpha = 0.5f)
)

private val LightColorScheme = ElegantDarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for Purity Lock Elegant Dark
  dynamicColor: Boolean = false, // Disable dynamic colors to ensure the Elegant Dark theme is applied
  content: @Composable () -> Unit,
) {
  val colorScheme = ElegantDarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
