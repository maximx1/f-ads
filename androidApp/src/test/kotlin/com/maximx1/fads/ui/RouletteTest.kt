package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.Spin
import com.maximx1.fads.core.den.Wheel
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.LAYOUT_TAG
import com.maximx1.fads.ui.den.RESULT_TAG
import com.maximx1.fads.ui.den.RouletteLayout
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.coinTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The roulette table: putting coins down on the layout, spinning, and being paid. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RouletteTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private val storage = FileStorage(files)

    /** Sits down at the table with [den], where every spin lands in [pocket]. */
    private fun sitDown(
        den: Den,
        pocket: Int = 0,
    ) {
        File(files, GameStore.DEN).writeText(den.toJson())
        val spin = generateSequence(0L) { it + 1 }.first { Wheel.landing(it) == pocket }
        val game = GameViewModel(GameStore(storage)) { 0L }
        tables = DenViewModel(GameStore(storage), game.history) { spin }
        compose.setContent { FadsTheme { GameScreen(game, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.ROULETTE))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.ROULETTE)).performClick()
    }

    private fun tap(bet: Bet) {
        val (across, down) = RouletteLayout.centreOf(bet)
        compose.onNodeWithTag(LAYOUT_TAG).performTouchInput {
            click(
                Offset(
                    (across / RouletteLayout.WIDTH * width).toFloat(),
                    (down / RouletteLayout.HEIGHT * height).toFloat(),
                ),
            )
        }
    }

    private lateinit var tables: DenViewModel

    // The den as the table has it; it is written out to its file a moment later.
    private fun savedDen(): Den? = tables.den.value

    @Test
    fun coinsGoDownWhereTheLayoutIsTappedAndTheSpinPaysWhatWon() {
        sitDown(Den(purse = Purse(100)), pocket = 17)
        tap(Bet(BetKind.STRAIGHT, 17))
        compose.onNodeWithTag(chipTag(Coin.GOLD)).performClick()
        tap(Bet(BetKind.RED))
        compose.onNodeWithText("On the table: 26 bronzes").assertExists()
        compose.onNodeWithTag(coinTag(Coin.GOLD)).assertTextEquals("× 2")
        compose.onNodeWithTag(coinTag(Coin.SILVER)).assertTextEquals("× 2")
        compose.onNodeWithTag(coinTag(Coin.BRONZE)).assertTextEquals("× 4")

        compose.onNodeWithText("Spin").performClick()
        compose.waitForIdle()
        // 17 is black: the number pays 35 to 1 and red loses.
        compose.onNodeWithTag(RESULT_TAG).assertTextEquals("17 Black: won 36 bronzes")
        compose.onNodeWithText("On the table: 0 bronzes").assertExists()
        assertEquals(Purse(110), savedDen()?.purse)
        assertEquals(Spin(17, staked = 26, won = 36), savedDen()?.roulette?.last)
    }

    @Test
    fun clearTakesTheBetsBackAndRebetPutsTheLastOnesDownAgain() {
        sitDown(Den(purse = Purse(30)), pocket = 0)
        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        tap(Bet(BetKind.DOZEN, 1))
        compose.onNodeWithText("Clear").performClick()
        compose.onNodeWithText("On the table: 0 bronzes").assertExists()
        compose.onNodeWithText("Spin").assertIsNotEnabled()

        tap(Bet(BetKind.COLUMN, 2))
        tap(Bet(BetKind.COLUMN, 2))
        compose.onNodeWithText("Spin").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(RESULT_TAG).assertTextEquals("0 Green: no win")
        assertEquals(Purse(10), savedDen()?.purse)

        // Twenty again is more than is left.
        compose.onNodeWithText("Rebet").assertIsNotEnabled()
    }

    @Test
    fun rebetPutsTheSameBetsDownAgain() {
        sitDown(Den(purse = Purse(50)), pocket = 0)
        tap(Bet(BetKind.ODD))
        tap(Bet(BetKind.CORNER, 1))
        compose.onNodeWithText("Spin").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Rebet").performClick()
        compose.onNodeWithText("On the table: 2 bronzes").assertExists()
        assertEquals(
            mapOf(Bet(BetKind.ODD) to 1L, Bet(BetKind.CORNER, 1) to 1L),
            savedDen()?.roulette?.bets,
        )
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
