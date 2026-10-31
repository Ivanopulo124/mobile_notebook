package com.ivanopulo.planner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ivanopulo.planner.domain.TaskColor

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
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF2F2F2),
    surfaceContainerHigh = Color(0xFFEDEDED),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    inverseSurface = Color(0xFF222222),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFF9497FF),
    error = Color(0xFFD93025),
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
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF5B5FE8),
    error = Color(0xFFFF6B5E),
)

@Composable
fun PlannerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

val HighlightText = Color(0xFF111111)

fun highlightColor(color: TaskColor): Color = when (color) {
    TaskColor.YELLOW -> Color(0xFFFFE45C)
    TaskColor.GREEN -> Color(0xFF3DEBA0)
    TaskColor.BLUE -> Color(0xFF8CCBFF)
    TaskColor.PINK -> Color(0xFFFFA6D5)
    TaskColor.ORANGE -> Color(0xFFFFB25C)
}
