package com.maximx1.fads.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.SlotMachine
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.LINE_BET_TAG
import com.maximx1.fads.ui.den.SLOTS_RESULT_TAG
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The slot machine: a bet on every line built up with chips, a pull of the lever, and what the lines win. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SlotsTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun aPullTakesTheBetOnEveryLineAndPaysWhatTheLinesWin() {
        // A pull whose reels stop with a winning line, so there is something to be paid.
        val number = generateSequence(0L) { it + 1 }.first { SlotMachine.payout(SlotMachine.stops(it), 1) > 0 }
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.SLOTS))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.SLOTS)).performScrollTo().performClick()

        compose.onNodeWithText("Spin").assertIsNotEnabled()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(LINE_BET_TAG).assertTextEquals("On each of 5 lines: 2, 10 bronzes in all")
        compose.onNodeWithText("Spin").performClick()
        compose.waitForIdle()

        val won = SlotMachine.payout(SlotMachine.stops(number), 2)
        compose.onNodeWithTag(SLOTS_RESULT_TAG).assertTextEquals("Won $won bronzes!")
        assertEquals(Purse(100 - 10 + won), tables.den.value?.purse)
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
