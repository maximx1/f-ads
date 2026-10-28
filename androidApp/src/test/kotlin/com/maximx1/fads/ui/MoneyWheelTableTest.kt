package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.WheelSymbol
import com.maximx1.fads.core.den.betMoneyWheel
import com.maximx1.fads.core.den.moneyWheel
import com.maximx1.fads.core.den.spinMoneyWheel
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.tableTag
import com.maximx1.fads.ui.den.wheelTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The money wheel: chips on the symbols, a spin, and the symbol it stops on paid its odds. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MoneyWheelTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun chipsGoOnTheSymbolsAndTheSpinPaysWhereItStops() {
        val number = 6L
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.MONEY_WHEEL))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.MONEY_WHEEL)).performScrollTo().performClick()

        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithTag(wheelTag(WheelSymbol.TWO)).performClick()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(wheelTag(WheelSymbol.ONE)).performClick()
        compose.onNodeWithText("Spin").performClick()
        compose.waitForIdle()

        val expected =
            Den(
                purse = Purse(100),
            ).betMoneyWheel(WheelSymbol.TWO, 10).betMoneyWheel(WheelSymbol.ONE, 1).spinMoneyWheel(number)
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(expected.moneyWheel(), tables.den.value?.moneyWheel())
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
