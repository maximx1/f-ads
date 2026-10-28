package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.SicBoBet
import com.maximx1.fads.core.den.SicBoKind
import com.maximx1.fads.core.den.betSicBo
import com.maximx1.fads.core.den.rollSicBo
import com.maximx1.fads.core.den.sicBo
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.sicBoTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The sic bo table: chips on the layout, a throw of three dice, and the bets paid. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SicBoTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun chipsGoOnTheLayoutAndTheThrowPaysThem() {
        val number = 12L
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.SIC_BO))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.SIC_BO)).performScrollTo().performClick()

        val big = SicBoBet(SicBoKind.BIG)
        val six = SicBoBet(SicBoKind.SINGLE, 6)
        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithTag(sicBoTag(big)).performClick()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(sicBoTag(six)).performScrollTo().performClick()
        assertEquals(
            mapOf(big to 10L, six to 1L),
            tables.den.value
                ?.sicBo()
                ?.bets,
        )
        compose.onNodeWithText("Roll").performClick()
        compose.waitForIdle()

        val expected = Den(purse = Purse(100)).betSicBo(big, 10).betSicBo(six, 1).rollSicBo(number)
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(expected.sicBo(), tables.den.value?.sicBo())
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
