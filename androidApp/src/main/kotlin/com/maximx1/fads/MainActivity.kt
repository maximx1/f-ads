package com.maximx1.fads

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.FadsTheme
import com.maximx1.fads.ui.GameScreen
import com.maximx1.fads.ui.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(FileStorage(filesDir))
        systemBarIcons(dark = settings.theme.isPale)
        setContent {
            // The system bars are drawn over the table, so their icons have to show against the theme's.
            val pale = settings.theme.isPale
            LaunchedEffect(pale) { systemBarIcons(dark = pale) }
            FadsTheme {
                GameScreen(viewModel(factory = gameFactory), settings)
            }
        }
    }

    // Time while the phone sleeps counts on this clock, but the game's own is never running then.
    private val gameFactory
        get() =
            viewModelFactory {
                initializer { GameViewModel(GameStore(FileStorage(filesDir)), now = SystemClock::elapsedRealtime) }
            }

    private fun systemBarIcons(dark: Boolean) {
        val style =
            if (dark) {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            } else {
                SystemBarStyle.dark(Color.TRANSPARENT)
            }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
