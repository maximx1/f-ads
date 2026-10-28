package com.maximx1.fads.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.BlackjackMove
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.blackjack
import com.maximx1.fads.core.den.dealBlackjack
import com.maximx1.fads.core.den.playBlackjack
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
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

/** The blackjack table: a bet, a deal, standing, and the dealer playing out the round. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BlackjackTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun standingLetsTheDealerPlayAndPaysTheHand() {
        // A round with no blackjack in it, so there is a hand to play.
        val number =
            generateSequence(0L) {
                it + 1
            }.first {
                !Den(purse = Purse(100))
                    .dealBlackjack(10, it)
                    .blackjack()
                    .round!!
                    .over
            }
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.BLACKJACK))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.BLACKJACK)).performScrollTo().performClick()

        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithText("Deal").performClick()
        compose.onNodeWithText("Stand").performClick()
        compose.waitForIdle()

        val expected = Den(purse = Purse(100)).dealBlackjack(10, number).playBlackjack(BlackjackMove.STAND)
        assertEquals(expected.purse, tables.den.value?.purse)
        assertEquals(
            expected.blackjack().round,
            tables.den.value
                ?.blackjack()
                ?.round,
        )
        compose.onNodeWithText("Deal").assertExists()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
