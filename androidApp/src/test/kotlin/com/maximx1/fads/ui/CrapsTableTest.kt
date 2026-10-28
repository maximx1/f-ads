package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.CrapsBet
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.betCraps
import com.maximx1.fads.core.den.craps
import com.maximx1.fads.core.den.rollDice
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.crapsTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The craps table: chips on the layout, a throw of the dice, and the bets settled. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CrapsTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun theLineAndTheFieldGoDownAndTheThrowSettlesThem() {
        val number = 9L
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.CRAPS))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.CRAPS)).performScrollTo().performClick()

        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithTag(crapsTag(CrapsBet.PASS)).performClick()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(crapsTag(CrapsBet.FIELD)).performClick()
        assertEquals(
            mapOf(CrapsBet.PASS to 10L, CrapsBet.FIELD to 1L),
            tables.den.value
                ?.craps()
                ?.bets,
        )
        compose.onNodeWithText("Roll").performClick()
        compose.waitForIdle()

        val expected = Den(purse = Purse(100)).betCraps(CrapsBet.PASS, 10).betCraps(CrapsBet.FIELD, 1).rollDice(number)
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(expected.craps(), tables.den.value?.craps())
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
