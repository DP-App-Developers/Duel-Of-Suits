package com.dehong.duelofSuits

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dehong.duelofSuits.model.Difficulty
import com.dehong.duelofSuits.ui.screens.GameScreen
import com.dehong.duelofSuits.ui.screens.HomeScreen
import com.dehong.duelofSuits.ui.theme.DuelOfSuitsTheme
import com.dehong.duelofSuits.viewmodel.GameViewModelFactory

sealed class AppScreen {
    object Home : AppScreen()
    data class Game(val playerCount: Int, val difficulty: Difficulty, val sessionId: Int) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        enableEdgeToEdge()
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        hideSystemBars()

        setContent {
            var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
            var sessionId by remember { mutableStateOf(0) }

            DuelOfSuitsTheme {
                when (val screen = currentScreen) {
                    AppScreen.Home -> HomeScreen(onStartGame = { count, diff ->
                        sessionId++
                        currentScreen = AppScreen.Game(count, diff, sessionId)
                    })
                    is AppScreen.Game -> GameScreen(
                        playerCount = screen.playerCount,
                        onNavigateHome = { currentScreen = AppScreen.Home },
                        viewModel = viewModel(
                            factory = GameViewModelFactory(application, screen.playerCount, screen.difficulty),
                            key = "game_${screen.playerCount}_${screen.difficulty}_${screen.sessionId}"
                        )
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
