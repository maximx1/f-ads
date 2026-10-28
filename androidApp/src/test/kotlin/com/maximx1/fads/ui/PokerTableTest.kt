package com.maximx1.fads.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.table
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The poker tables: sitting down, dealing, playing a hand through, and getting up. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PokerTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private lateinit var tables: DenViewModel

    /** Sits down at [game]'s table with a purse of [bronzes], the den's players taking no time over their moves. */
    private fun sitDown(
        game: DenGame,
        bronzes: Long = 300,
    ) {
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(bronzes)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        var luck = 1L
        tables = DenViewModel(GameStore(storage), patience.history, botMillis = 0) { luck++ }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        waitFor(hasTestTag(tableTag(game)))
        compose.onNodeWithTag(tableTag(game)).performClick()
        waitFor(hasText("Deal") and hasClickAction())
    }

    private fun waitFor(matcher: androidx.compose.ui.test.SemanticsMatcher) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun button(text: String) = hasText(text) and hasClickAction()

    private fun showing(text: String) = compose.onAllNodes(button(text)).fetchSemanticsNodes().isNotEmpty()

    private fun den(): Den = checkNotNull(tables.den.value)

    // Waits for the den to come to [done]. Nothing on the screen changes for the test rule to wait on, so this
    // lets the main thread run between looks, which is where the den's players' moves land.
    private fun untilTheDen(done: () -> Boolean) {
        repeat((TIMEOUT / LOOK_MILLIS).toInt()) {
            if (done()) return
            Thread.sleep(LOOK_MILLIS)
            compose.waitForIdle()
        }
        assertTrue("the den never got there", done())
    }

    @Test
    fun foldingAtFiveCardDrawCostsOnlyTheAnte() {
        sitDown(DenGame.FIVE_CARD_DRAW)
        compose.onNode(button("Deal")).performClick()
        waitFor(button("Fold"))
        compose.onNode(button("Fold")).performClick()
        waitFor(hasText("Deal"))
        assertEquals(Purse(299), den().purse)
        assertFalse(checkNotNull(den().table(PokerGame.DRAW)).playing)
    }

    @Test
    fun checkingAndCallingThroughAHandOfHoldemLosesAndMakesNothingAtTheTable() {
        sitDown(DenGame.TEXAS_HOLDEM)
        val before = den().purse.total + checkNotNull(den().table(PokerGame.HOLDEM)).seats.sumOf { it.stack }
        compose.onNode(button("Deal")).performClick()
        var turns = 0
        while (!showing("Deal")) {
            compose.waitUntil(TIMEOUT) {
                showing("Deal") || showing("Check") ||
                    compose
                        .onAllNodes(
                            hasText("Call ", substring = true) and hasClickAction(),
                        ).fetchSemanticsNodes()
                        .isNotEmpty()
            }
            when {
                showing("Deal") -> break
                showing("Check") -> compose.onNode(button("Check")).performClick()
                else -> compose.onNode(hasText("Call ", substring = true) and hasClickAction()).performClick()
            }
            compose.waitForIdle()
            assertTrue("the hand never ends", ++turns < 30)
        }
        val table = checkNotNull(den().table(PokerGame.HOLDEM))
        assertFalse(table.playing)
        assertEquals(before, den().purse.total + table.seats.sumOf { it.stack })
    }

    @Test
    fun gettingUpInTheMiddleOfAHandFoldsIt() {
        sitDown(DenGame.TEXAS_HOLDEM, bronzes = 50)
        compose.onNode(button("Deal")).performClick()
        waitFor(button("Fold"))
        compose.onNode(button("Back")).performClick()
        waitFor(hasText("Points to cash in", substring = true))
        untilTheDen { den().table(PokerGame.HOLDEM)?.playing == false }
        assertTrue(den().purse.total in 48L..50L)
    }

    private companion object {
        const val TIMEOUT = 20_000L
        const val LOOK_MILLIS = 100L
    }
}
