package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.BaccaratSide
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.baccarat
import com.maximx1.fads.core.den.betBaccarat
import com.maximx1.fads.core.den.playBaccarat
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.sideTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The baccarat table: chips put down in the boxes, a coup dealt, and the bets paid. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BaccaratTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun chipsGoInTheBoxesAndTheCoupPaysThem() {
        val number = 5L
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.BACCARAT))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.BACCARAT)).performScrollTo().performClick()

        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithTag(sideTag(BaccaratSide.BANKER)).performClick()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(sideTag(BaccaratSide.TIE)).performClick()
        assertEquals(
            mapOf(BaccaratSide.BANKER to 10L, BaccaratSide.TIE to 1L),
            tables.den.value
                ?.baccarat()
                ?.bets,
        )
        compose.onNodeWithText("Deal").performClick()
        compose.waitForIdle()

        val expected =
            Den(
                purse = Purse(100),
            ).betBaccarat(BaccaratSide.BANKER, 10).betBaccarat(BaccaratSide.TIE, 1).playBaccarat(number)
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(
            expected.baccarat().last,
            tables.den.value
                ?.baccarat()
                ?.last,
        )
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
