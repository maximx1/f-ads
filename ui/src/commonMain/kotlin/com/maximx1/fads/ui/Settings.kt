package com.maximx1.fads.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.maximx1.fads.game.Storage
import com.maximx1.fads.ui.theme.TableTheme

/**
 * The player's choices, kept between runs. A composable that reads one is redrawn when it changes, and setting
 * one saves it straight away.
 */
@Stable
class Settings(
    private val storage: Storage,
) {
    private val saved: Map<String, String> =
        storage
            .read(FILE)
            .orEmpty()
            .lines()
            .filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }

    private var chosenTheme by mutableStateOf(TableTheme.entries.find { it.name == saved[THEME] } ?: TableTheme.FLAT)
    private var dimming by mutableStateOf(saved[DIM] != false.toString())

    /** The flat theme until another is chosen, or if the saved one no longer exists. */
    var theme: TableTheme
        get() = chosenTheme
        set(value) {
            chosenTheme = value
            save()
        }

    /** Whether cards that cannot be moved right now are drawn darker. They are unless the player says not. */
    var dimsStuckCards: Boolean
        get() = dimming
        set(value) {
            dimming = value
            save()
        }

    private fun save() = storage.write(FILE, "$THEME=${chosenTheme.name}\n$DIM=$dimming\n")

    private companion object {
        const val FILE = "settings.txt"
        const val THEME = "theme"
        const val DIM = "dimStuckCards"
    }
}
