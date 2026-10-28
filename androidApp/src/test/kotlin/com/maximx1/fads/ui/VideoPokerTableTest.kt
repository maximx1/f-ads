package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.dealVideoPoker
import com.maximx1.fads.core.den.drawVideoPoker
import com.maximx1.fads.core.den.videoPoker
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.holdTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The video poker machine: a bet, a deal, the cards held, and the draw paid by the pay table. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class VideoPokerTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun theCardsHeldStayAndTheDrawIsPaid() {
        val number = 21L
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(50)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.VIDEO_POKER))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.VIDEO_POKER)).performScrollTo().performClick()

        repeat(2) { compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick() }
        compose.onNodeWithText("Deal").performClick()
        compose.onNodeWithTag(holdTag(0)).performClick()
        compose.onNodeWithTag(holdTag(3)).performClick()
        compose.onNodeWithText("Draw").performClick()
        compose.waitForIdle()

        val expected = Den(purse = Purse(50)).dealVideoPoker(2, number).drawVideoPoker(setOf(0, 3))
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(
            expected.videoPoker().hand,
            tables.den.value
                ?.videoPoker()
                ?.hand,
        )
        compose.onNodeWithText("Deal").assertExists()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
