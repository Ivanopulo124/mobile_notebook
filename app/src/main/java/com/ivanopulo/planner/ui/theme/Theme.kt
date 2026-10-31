package com.ivanopulo.planner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5FE8),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFFA6A6A6),
    outlineVariant = Color(0xFFE8E8E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9497FF),
    onPrimary = Color(0xFF111111),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFF707070),
    outlineVariant = Color(0xFF2A2A2A),
)

@Composable
fun PlannerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
