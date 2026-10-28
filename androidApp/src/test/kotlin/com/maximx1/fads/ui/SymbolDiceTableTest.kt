package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.SymbolDiceGame
import com.maximx1.fads.core.den.betSymbolDice
import com.maximx1.fads.core.den.rollSymbolDice
import com.maximx1.fads.core.den.symbolDice
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.faceTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Crown and anchor: chips on the faces, a throw of three dice, and a bet paid for each die showing its face. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SymbolDiceTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun chipsGoOnTheFacesAndTheThrowPaysThem() {
        val number = 4L
        val game = SymbolDiceGame.CROWN_AND_ANCHOR
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.CROWN_AND_ANCHOR))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.CROWN_AND_ANCHOR)).performScrollTo().performClick()
        compose.onNodeWithText("Crown").assertExists()

        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithTag(faceTag(1)).performClick()
        compose.onNodeWithTag(faceTag(4)).performClick()
        compose.onNodeWithText("Roll").performClick()
        compose.waitForIdle()

        val expected =
            Den(
                purse = Purse(100),
            ).betSymbolDice(game, 1, 10).betSymbolDice(game, 4, 10).rollSymbolDice(game, number)
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(expected.symbolDice(game), tables.den.value?.symbolDice(game))
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
