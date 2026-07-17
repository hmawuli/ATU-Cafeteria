package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HighContrastColorScheme =
  darkColorScheme(
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFFFFD600),
    tertiary = Color(0xFFFFD600),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = AtuDarkNavy,
    secondary = AtuDarkAmber,
    tertiary = AtuCrimson,
    background = AtuDarkBg,
    surface = AtuDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AtuNavy,
    secondary = AtuAmber,
    tertiary = AtuCrimson,
    background = AtuGreyBg,
    surface = AtuSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  highContrast: Boolean = false,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      highContrast -> HighContrastColorScheme
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
