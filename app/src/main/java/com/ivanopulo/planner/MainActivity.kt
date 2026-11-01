package com.ivanopulo.planner

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ivanopulo.planner.data.ThemeMode
import com.ivanopulo.planner.sync.DropboxAuth
import com.ivanopulo.planner.ui.settings.SettingsScreen
import com.ivanopulo.planner.ui.settings.SyncViewModel
import com.ivanopulo.planner.ui.theme.PlannerTheme
import com.ivanopulo.planner.ui.week.WeekScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private var startCount by mutableIntStateOf(0)
    private val syncViewModel: SyncViewModel by viewModels { SyncViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity keeps its old intent, which was already handled.
        if (savedInstanceState == null) handleSignInRedirect(intent)
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
            PlannerTheme(darkTheme) {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                // Keeps the week pager position while the settings screen is shown.
                val stateHolder = rememberSaveableStateHolder()
                AnimatedContent(
                    targetState = showSettings,
                    transitionSpec = {
                        if (targetState) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                        } else {
                            slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "screen",
                ) { settingsVisible ->
                    if (settingsVisible) {
                        SettingsScreen(
                            themeMode = mode,
                            onThemeModeChange = { lifecycleScope.launch { settings.setThemeMode(it) } },
                            onBack = { showSettings = false },
                        )
                    } else {
                        stateHolder.SaveableStateProvider("week") {
                            WeekScreen(startCount = startCount, onOpenSettings = { showSettings = true })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSignInRedirect(intent)
    }

    private fun handleSignInRedirect(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != DropboxAuth.REDIRECT_SCHEME_PREFIX + BuildConfig.DROPBOX_APP_KEY) return
        syncViewModel.onDropboxRedirect(uri.getQueryParameter("code"), uri.getQueryParameter("state"))
    }

    override fun onStart() {
        super.onStart()
        startCount++
    }
}
