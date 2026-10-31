package com.ivanopulo.planner

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanopulo.planner.data.ThemeMode
import com.ivanopulo.planner.ui.theme.PlannerTheme
import com.ivanopulo.planner.ui.week.WeekScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as PlannerApp).container.settingsRepository
        // Read once synchronously so a forced theme does not flash the system one on launch.
        val initialMode = runBlocking { settings.themeMode.first() }
        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle(initialMode)
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            PlannerTheme(darkTheme) { WeekScreen() }
        }
    }
}
