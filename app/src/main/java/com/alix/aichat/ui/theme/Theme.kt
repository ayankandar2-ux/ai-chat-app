package com.alix.aichat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A restrained, warm-neutral palette in the spirit of Claude's minimal look —
// original values, not copied assets/brand colors.
private val Cream = Color(0xFFF7F5F2)
private val InkText = Color(0xFF2B2A28)
private val Accent = Color(0xFFB0623C)
private val BubbleUser = Color(0xFFEDE7DF)
private val Surface = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = Accent,
    background = Cream,
    surface = Surface,
    onBackground = InkText,
    onSurface = InkText,
    secondaryContainer = BubbleUser
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    background = Color(0xFF1E1D1B),
    surface = Color(0xFF262523),
    onBackground = Color(0xFFEDEAE4),
    onSurface = Color(0xFFEDEAE4),
    secondaryContainer = Color(0xFF33322F)
)

@Composable
fun AiChatTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
