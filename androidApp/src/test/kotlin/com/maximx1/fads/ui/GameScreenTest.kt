package com.maximx1.fads.ui

import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/** Runs the whole app, save file included, through Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class GameScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val saveFile = File(RuntimeEnvironment.getApplication().filesDir, "game.json")
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun touchScreen() {
        // As on a phone: without this the number field grabs focus the moment its dialog opens.
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @After
    fun close() {
        scenario?.close()
    }

    private fun launch(saved: String? = null) {
        saved?.let { saveFile.writeText(it) }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasText("Game #", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun savedGame(): Game? = if (saveFile.exists()) Game.fromJson(saveFile.readText()) else null

    private fun waitForSave(matches: (Game) -> Boolean) =
        compose.waitUntil(TIMEOUT) { savedGame()?.let(matches) == true }

    private fun tap(card: String) {
        compose.onNodeWithTag(card).performTouchInput {
            down(topLeft + grip)
            up()
        }
        compose.waitForIdle()
    }

    private fun inDialog(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    /** Lets time go by on the phone's clock, which otherwise stands still in these tests. */
    private fun letPass(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
        compose.waitForIdle()
    }

    private fun clock() = compose.onNodeWithTag(ELAPSED_TAG)

    @Test
    fun aFirstStartDealsARandomGameAndSavesIt() {
        launch()
        waitForSave { it.moveCount == 0 }
        val number = savedGame()!!.dealNumber
        assertTrue(number in 1..GameViewModel.MAX_RANDOM_DEAL)
        compose.onNodeWithText("Game #$number").assertExists()
        compose.onNodeWithText("Undo").assertIsNotEnabled()
    }

    @Test
    fun aSavedGameIsPickedUpWhereItWasLeft() {
        launch("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}""")
        compose.onNodeWithText("Game #1").assertExists()
        compose.onNodeWithText("Undo").assertIsEnabled()
        val threeOfDiamonds = compose.onNodeWithTag("3D").fetchSemanticsNode().boundsInRoot
        val fourOfSpades = compose.onNodeWithTag("4S").fetchSemanticsNode().boundsInRoot
        assertEquals(fourOfSpades.left, threeOfDiamonds.left)
        assertTrue(threeOfDiamonds.top > fourOfSpades.top)
    }

    @Test
    fun aDamagedSaveFileJustStartsANewGame() {
        launch("this is not a saved game")
        waitForSave { it.moveCount == 0 }
    }

    @Test
    fun movesAreSavedAndUndoTakesThemBack() {
        launch("""{"dealNumber":1}""")
        tap("3D")
        waitForSave { it.moveCount == 1 }
        tap("2C")
        waitForSave { it.moveCount == 2 }

        compose.onNodeWithText("Undo").performClick()
        waitForSave { it.moveCount == 1 }
        compose.onNodeWithText("Undo").performClick()
        waitForSave { it.moveCount == 0 }
        compose.onNodeWithText("Undo").assertIsNotEnabled()
    }

    @Test
    fun theGameSurvivesTheScreenBeingRebuilt() {
        launch("""{"dealNumber":1}""")
        tap("3D")
        scenario!!.recreate()
        compose.onNodeWithText("Game #1").assertExists()
        compose.onNodeWithText("Undo").assertIsEnabled()
    }

    @Test
    fun restartAsksFirstThenClearsTheMoves() {
        launch("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}""")
        compose.onNodeWithText("Restart").performClick()
        inDialog("Restart this deal?").assertExists()
        inDialog("Cancel").performClick()
        assertEquals(1, savedGame()?.moveCount)

        compose.onNodeWithText("Restart").performClick()
        inDialog("Restart").performClick()
        waitForSave { it.dealNumber == 1L && it.moveCount == 0 }
        compose.onNodeWithText("Undo").assertIsNotEnabled()
    }

    @Test
    fun playingTheLastCardShowsTheWinAndOffersANewGame() {
        val oneMoveLeft = checkNotNull(javaClass.getResource("/deal-1-one-move-left.json")).readText()
        launch(oneMoveLeft)
        val lastCard =
            Game
                .fromJson(oneMoveLeft)!!
                .state.columns[0]
                .last()
        tap(lastCard.code)

        inDialog("You won!").assertExists()
        inDialog("Deal #1 solved. Moves: 131. Time: 00:00.").assertExists()
        inDialog("New game").performClick()
        compose.onNodeWithText("You won!").assertDoesNotExist()
        waitForSave { it.moveCount == 0 }
    }

    @Test
    fun theClockWaitsForTheFirstMoveThenCountsTheSeconds() {
        launch("""{"dealNumber":1}""")
        clock().assertTextEquals("00:00")
        letPass(seconds = 30)
        clock().assertTextEquals("00:00")

        tap("3D")
        letPass(seconds = 5)
        clock().assertTextEquals("00:05")
        letPass(seconds = 60)
        clock().assertTextEquals("01:05")
    }

    @Test
    fun theClockPicksUpFromTheSavedTime() {
        launch("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}],"elapsedMillis":3600000}""")
        clock().assertTextEquals("1:00:00")
        letPass(seconds = 2)
        clock().assertTextEquals("1:00:02")
    }

    @Test
    fun theClockStopsWhileTheGameIsOutOfSightAndItsTimeIsSaved() {
        launch("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}],"elapsedMillis":60000}""")
        letPass(seconds = 3)

        scenario!!.moveToState(Lifecycle.State.CREATED)
        waitForSave { it.elapsedMillis in 63_000..63_999 }
        letPass(seconds = 300)

        scenario!!.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        clock().assertTextEquals("01:03")
        letPass(seconds = 1)
        clock().assertTextEquals("01:04")
    }

    @Test
    fun theClockKeepsGoingThroughUndoAndGoesBackToZeroOnRestart() {
        launch("""{"dealNumber":1}""")
        tap("3D")
        letPass(seconds = 4)
        compose.onNodeWithText("Undo").performClick()
        letPass(seconds = 4)
        clock().assertTextEquals("00:08")
        waitForSave { it.moveCount == 0 && it.elapsedMillis >= 4_000 }

        tap("3D")
        compose.onNodeWithText("Restart").performClick()
        inDialog("Restart").performClick()
        letPass(seconds = 4)
        clock().assertTextEquals("00:00")
        waitForSave { it.moveCount == 0 && it.elapsedMillis == 0L }
    }

    @Test
    fun winningStopsTheClockAndShowsTheTime() {
        val oneMoveLeft = checkNotNull(javaClass.getResource("/deal-1-one-move-left.json")).readText()
        launch(oneMoveLeft)
        letPass(seconds = 75)
        tap(
            Game
                .fromJson(oneMoveLeft)!!
                .state.columns[0]
                .last()
                .code,
        )

        inDialog("Deal #1 solved. Moves: 131. Time: 01:15.").assertExists()
        letPass(seconds = 20)
        clock().assertTextEquals("01:15")
        waitForSave { it.state.isWon && it.elapsedMillis in 75_000..75_999 }
    }

    private companion object {
        const val TIMEOUT = 10_000L
        val grip = Offset(30f, 20f)
    }
}
