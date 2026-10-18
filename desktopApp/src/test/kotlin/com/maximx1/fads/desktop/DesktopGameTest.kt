package com.maximx1.fads.desktop

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.game.Storage
import com.maximx1.fads.ui.ELAPSED_TAG
import com.maximx1.fads.ui.FadsTheme
import com.maximx1.fads.ui.GameScreen
import com.maximx1.fads.ui.MOVES_TAG
import com.maximx1.fads.ui.Settings
import com.maximx1.fads.ui.theme.TableTheme
import org.junit.Rule
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals

/** The shared game screen, running on the desktop's own Compose with nothing saved to disk. */
class DesktopGameTest {
    @get:Rule
    val compose = createComposeRule()

    private val saved = ConcurrentHashMap<String, String>()
    private val storage =
        object : Storage {
            override fun read(name: String): String? = saved[name]

            override fun write(
                name: String,
                text: String,
            ) {
                saved[name] = text
            }
        }

    private fun savedGame(): Game<*, *>? = saved[GameStore.GAME]?.let(GameType.FreeCell::load)

    @Test
    fun theGameComesUpPlaysAMoveSavesItAndChangesTheme() {
        saved[GameStore.GAME] = """{"dealNumber":1}"""
        val game = GameViewModel(GameStore(storage)) { System.nanoTime() / 1_000_000 }
        compose.setContent { FadsTheme { GameScreen(game, Settings(storage)) } }
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasTestTag(ELAPSED_TAG)).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("3D").performClick()
        compose.waitUntil(TIMEOUT) { savedGame()?.moveCount == 1 }
        compose.onNodeWithTag(MOVES_TAG).assertTextEquals("Moves: 1")

        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNodeWithText("Game #1").assertExists()
        compose.onNodeWithText("Backroom").performClick()
        compose.onNodeWithText("Resume").performClick()
        assertEquals(TableTheme.BACKROOM, Settings(storage).theme)
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
