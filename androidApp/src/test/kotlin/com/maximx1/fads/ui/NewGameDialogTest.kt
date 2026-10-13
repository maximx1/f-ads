package com.maximx1.fads.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import com.maximx1.fads.MainActivity
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.game.GameViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * The new-game dialog, in the whole app. Kept apart from [GameScreenTest] because these run on Robolectric's
 * default small screen: on a phone-sized one, a text field inside a dialog never lets the test settle. That
 * happens with a bare Material dialog and no code of ours, so it is the test tools, not the app.
 */
@RunWith(RobolectricTestRunner::class)
class NewGameDialogTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val saveFile = File(RuntimeEnvironment.getApplication().filesDir, "game.json")
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun openTheDialogOnASavedGame() {
        saveFile.writeText("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}""")
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText("New")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("New").performClick()
    }

    @After
    fun close() {
        scenario.close()
    }

    private fun inDialog(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    private fun savedGame(): Game? = Game.fromJson(saveFile.readText())

    /** The deal number is only shown in the pause dialog, so this opens it to look. */
    private fun assertGameNumber(number: Long) {
        compose.onNodeWithText("Pause").performClick()
        inDialog("Game #$number").assertExists()
        inDialog("Resume").performClick()
    }

    @Test
    fun itDealsARandomGameWithoutANumberBeingTyped() {
        inDialog("Deal").assertIsNotEnabled()
        inDialog("Random deal").performClick()

        compose.waitUntil(TIMEOUT) { savedGame()?.moveCount == 0 }
        val number = savedGame()!!.dealNumber
        assertTrue(number in 1..GameViewModel.MAX_RANDOM_DEAL)
        assertGameNumber(number)
    }

    @Test
    fun itDealsTheNumberThatWasTyped() {
        compose.onNode(hasSetTextAction()).performTextReplacement("617")
        inDialog("Deal").performClick()

        assertGameNumber(617)
        compose.waitUntil(TIMEOUT) { savedGame()?.let { it.dealNumber == 617L && it.moveCount == 0 } == true }
    }

    @Test
    fun onlyDigitsAreKept() {
        compose.onNode(hasSetTextAction()).performTextReplacement("6a1-7 ")
        inDialog("Deal").performClick()
        assertGameNumber(617)
    }

    @Test
    fun anEmptyNumberCannotBeDealtAndCancelKeepsTheGame() {
        compose.onNode(hasSetTextAction()).performTextReplacement("")
        inDialog("Deal").assertIsNotEnabled()
        inDialog("Cancel").performClick()

        assertGameNumber(1)
        assertEquals(1, savedGame()?.moveCount)
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
