package com.maximx1.fads.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.maximx1.fads.MainActivity
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.History
import com.maximx1.fads.game.GameStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The history of games played, in the whole app: what goes in it, searching it, playing again and watching. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class HistoryTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private val oneMoveLeft = checkNotNull(javaClass.getResource("/deal-1-one-move-left.json")).readText()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun winDealOne() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        File(files, GameStore.GAME).writeText(oneMoveLeft)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasTestTag(ELAPSED_TAG)).fetchSemanticsNodes().isNotEmpty() }
        val lastCard =
            Game
                .fromJson(oneMoveLeft)!!
                .state.columns[0]
                .last()
        compose.onNodeWithTag(lastCard.code).performTouchInput {
            down(topLeft + center / 4f)
            up()
        }
        compose.waitUntil(TIMEOUT) { savedHistory().games.isNotEmpty() }
        compose.onNode(hasText("Close") and hasAnyAncestor(isDialog())).performClick()
    }

    @After
    fun close() {
        scenario?.close()
    }

    private fun savedHistory(): History =
        File(files, GameStore.HISTORY).takeIf { it.exists() }?.readText()?.let(History::fromJson) ?: History()

    private fun openHistory() {
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNode(hasText("History") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
    }

    @Test
    fun aWinGoesInTheHistoryAndCanBeFoundByItsDealNumber() {
        val played = savedHistory().games.single()
        assertEquals(1L, played.dealNumber)
        assertEquals(true, played.won)

        openHistory()
        compose.onNodeWithText("FreeCell #1").assertExists()
        compose.onNodeWithText("Won").assertExists()
        compose.onNodeWithTag(HISTORY_SEARCH_TAG).performTextReplacement("99")
        compose.onNodeWithText("FreeCell #1").assertDoesNotExist()
        compose.onNodeWithText("No games match.").assertExists()
        compose.onNodeWithTag(HISTORY_SEARCH_TAG).performTextReplacement("1")
        compose.onNodeWithText("FreeCell #1").assertExists()
    }

    @Test
    fun watchingStepsThroughTheMovesAndPlayAgainDealsTheSameNumberFresh() {
        openHistory()
        compose.onNodeWithText("Watch").performClick()
        val moves = Game.fromJson(oneMoveLeft)!!.moveCount + 1
        compose.onNodeWithTag(WATCH_STEP_TAG).assertTextEquals("Move 0 of $moves")
        compose.onNodeWithContentDescription("Step forward").performClick()
        compose.onNodeWithTag(WATCH_STEP_TAG).assertTextEquals("Move 1 of $moves")
        compose.onNodeWithContentDescription("Back to the start").performClick()
        compose.onNodeWithTag(WATCH_STEP_TAG).assertTextEquals("Move 0 of $moves")

        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Play again").performClick()
        compose.waitUntil(TIMEOUT) {
            File(
                files,
                GameStore.GAME,
            ).readText().let { Game.fromJson(it) }?.let { it.dealNumber == 1L && it.moveCount == 0 } ==
                true
        }
        compose.onNodeWithTag(MOVES_TAG).assertTextEquals("Moves: 0")
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
