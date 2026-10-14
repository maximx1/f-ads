package com.maximx1.fads.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.FadsTheme
import com.maximx1.fads.ui.GameScreen
import com.maximx1.fads.ui.Settings
import kotlinx.coroutines.launch

private const val APP_NAME = "Fuck Ads"
private const val NANOS_IN_MILLI = 1_000_000

// A phone held upright, near enough: the board is laid out for that first.
private val FirstWindowSize = DpSize(460.dp, 900.dp)

fun main() =
    application {
        val storage = remember { DesktopStorage(DesktopStorage.directoryFor(APP_NAME)) }
        val settings = remember { Settings(storage) }
        val game = remember { GameViewModel(GameStore(storage)) { System.nanoTime() / NANOS_IN_MILLI } }
        val scope = rememberCoroutineScope()

        Window(
            // The game is written out before the window goes, or the last few seconds on the clock would be lost.
            onCloseRequest = {
                scope.launch {
                    game.close()
                    exitApplication()
                }
            },
            title = APP_NAME,
            state = rememberWindowState(size = FirstWindowSize),
        ) {
            FadsTheme { GameScreen(game, settings) }
        }
    }
