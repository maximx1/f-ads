package com.maximx1.fads.ui

import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.maximx1.fads.MainActivity
import com.maximx1.fads.core.engine.BestTimes
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.theme.TableTheme
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
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasTestTag(ELAPSED_TAG)).fetchSemanticsNodes().isNotEmpty() }
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

    private fun moves() = compose.onNodeWithTag(MOVES_TAG)

    /** The settings as they are saved right now. */
    private fun settings() = Settings(FileStorage(RuntimeEnvironment.getApplication().filesDir))

    /** The deal number is only shown in the pause dialog, so this opens it to look. */
    private fun assertGameNumber(number: Long) {
        compose.onNodeWithContentDescription("Pause").performClick()
        inDialog("Game #$number").assertExists()
        inDialog("Resume").performClick()
    }

    @Test
    fun aFirstStartDealsARandomGameAndSavesIt() {
        launch()
        waitForSave { it.moveCount == 0 }
        val number = savedGame()!!.dealNumber
        assertTrue(number in 1..GameViewModel.MAX_RANDOM_DEAL)
        assertGameNumber(number)
        compose.onNodeWithContentDescription("Undo").assertIsNotEnabled()
    }

    @Test
    fun aSavedGameIsPickedUpWhereItWasLeft() {
        launch("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}""")
        assertGameNumber(1)
        compose.onNodeWithContentDescription("Undo").assertIsEnabled()
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

        compose.onNodeWithContentDescription("Undo").performClick()
        waitForSave { it.moveCount == 1 }
        compose.onNodeWithContentDescription("Undo").performClick()
        waitForSave { it.moveCount == 0 }
        compose.onNodeWithContentDescription("Undo").assertIsNotEnabled()
    }

    @Test
    fun theGameSurvivesTheScreenBeingRebuilt() {
        launch("""{"dealNumber":1}""")
        tap("3D")
        scenario!!.recreate()
        assertGameNumber(1)
        compose.onNodeWithContentDescription("Undo").assertIsEnabled()
    }

    @Test
    fun theClockIsCentredAboveTheBoardAndTheSplitButtonRunsUnderIt() {
        launch("""{"dealNumber":1}""")
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val time = clock().fetchSemanticsNode().boundsInRoot
        assertEquals(screen.center.x, time.center.x, 1f)
        assertTrue(
            time.bottom <=
                compose
                    .onNodeWithTag("JD")
                    .fetchSemanticsNode()
                    .boundsInRoot.top,
        )

        val lowestCard = compose.onNodeWithTag("3D").fetchSemanticsNode().boundsInRoot
        val parts =
            listOf("Undo", "Restart", "New", "Hint", "Pause").map {
                compose.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot
            }
        for (part in parts) {
            assertTrue(part.top > lowestCard.bottom)
            assertEquals(parts[0].top, part.top)
            assertEquals(parts[0].width, part.width, 1f)
        }
        // One button, not four: each part starts where the last one stops, but for the line between them.
        for ((left, right) in parts.zipWithNext()) assertTrue(right.left - left.right in 0f..DIVIDER)
        assertEquals(screen.center.x, (parts.first().left + parts.last().right) / 2, 1f)
    }

    @Test
    fun theMoveCountSitsTopRightAndCountsMovesAndUndosAlike() {
        launch("""{"dealNumber":1}""")
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val time = clock().fetchSemanticsNode().boundsInRoot
        val count = moves().fetchSemanticsNode().boundsInRoot
        assertTrue(count.left > time.right)
        assertTrue(screen.right - count.right in 0f..EDGE)
        assertTrue(
            count.bottom <=
                compose
                    .onNodeWithTag("JD")
                    .fetchSemanticsNode()
                    .boundsInRoot.top,
        )

        moves().assertTextEquals("Moves: 0")
        tap("3D")
        tap("2C")
        moves().assertTextEquals("Moves: 2")
        compose.onNodeWithContentDescription("Undo").performClick()
        moves().assertTextEquals("Moves: 3")
        waitForSave { it.moveCount == 1 && it.movesMade == 3 }

        scenario!!.recreate()
        moves().assertTextEquals("Moves: 3")
        compose.onNodeWithContentDescription("Restart").performClick()
        inDialog("Restart").performClick()
        moves().assertTextEquals("Moves: 0")
    }

    @Test
    fun hintPointsOutAMoveUntilAMoveIsMadeAndCountsForNothing() {
        launch("""{"dealNumber":1}""")
        compose.onNodeWithTag(HINT_TAG).assertDoesNotExist()
        compose.onNodeWithContentDescription("Hint").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(HINT_TAG).assertExists()
        moves().assertTextEquals("Moves: 0")

        tap("3D")
        compose.onNodeWithTag(HINT_TAG).assertDoesNotExist()
        moves().assertTextEquals("Moves: 1")
    }

    @Test
    fun aHintGoesAwayByItselfAfterAFewSeconds() {
        launch("""{"dealNumber":1}""")
        compose.onNodeWithContentDescription("Hint").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(HINT_TAG).assertExists()
        compose.mainClock.advanceTimeBy(HINT_LASTS)
        compose.onNodeWithTag(HINT_TAG).assertDoesNotExist()
    }

    @Test
    fun dimmingStuckCardsIsOnUntilItIsSwitchedOffInThePauseDialog() {
        launch("""{"dealNumber":1}""")
        compose.onNodeWithContentDescription("Pause").performClick()
        inDialog("Dim cards that can't move").assertIsOn()
        inDialog("Dim cards that can't move").performClick()
        inDialog("Dim cards that can't move").assertIsOff()
        assertEquals(false, settings().dimsStuckCards)
    }

    @Test
    fun aWinWithTimeOnTheClockBecomesTheBestTimeAndIsKept() {
        val oneMoveLeft = checkNotNull(javaClass.getResource("/deal-1-one-move-left.json")).readText()
        launch(oneMoveLeft)
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNode(hasText("Best time", substring = true)).assertDoesNotExist()
        inDialog("Resume").performClick()
        compose.waitForIdle()

        letPass(seconds = 75)
        tap(
            Game
                .fromJson(oneMoveLeft)!!
                .state.columns[0]
                .last()
                .code,
        )
        inDialog("A new best time!").assertExists()
        inDialog("Close").performClick()
        compose.onNodeWithContentDescription("Pause").performClick()
        inDialog("Best time: 01:15").assertExists()
        inDialog("Best on this deal: 01:15").assertExists()

        val bestTimes = File(RuntimeEnvironment.getApplication().filesDir, "best-times.json")
        compose.waitUntil(TIMEOUT) { bestTimes.exists() }
        assertEquals(1L, BestTimes.fromJson(bestTimes.readText())?.fastest?.dealNumber)
    }

    @Test
    fun aThemePickedInThePauseDialogGoesOnTheTableAndIsKept() {
        launch("""{"dealNumber":1}""")
        compose.onNodeWithContentDescription("Pause").performClick()
        inDialog("Flat").assertIsSelected()
        inDialog("Cyberpunk").performClick()
        inDialog("Cyberpunk").assertIsSelected()
        inDialog("Resume").performClick()
        assertEquals(TableTheme.CYBERPUNK, settings().theme)

        scenario!!.close()
        launch()
        compose.onNodeWithContentDescription("Pause").performClick()
        inDialog("Cyberpunk").assertIsSelected()
    }

    @Test
    fun restartAsksFirstThenClearsTheMoves() {
        launch("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}""")
        compose.onNodeWithContentDescription("Restart").performClick()
        inDialog("Restart this deal?").assertExists()
        inDialog("Cancel").performClick()
        assertEquals(1, savedGame()?.moveCount)

        compose.onNodeWithContentDescription("Restart").performClick()
        inDialog("Restart").performClick()
        waitForSave { it.dealNumber == 1L && it.moveCount == 0 }
        compose.onNodeWithContentDescription("Undo").assertIsNotEnabled()
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
    fun pauseStopsTheClockSavesItsTimeAndShowsTheGameNumber() {
        launch("""{"dealNumber":617,"elapsedMillis":60000}""")
        letPass(seconds = 3)

        compose.onNodeWithContentDescription("Pause").performClick()
        inDialog("Paused").assertExists()
        inDialog("Game #617").assertExists()
        inDialog("Time: 01:03").assertExists()
        waitForSave { it.elapsedMillis in 63_000..63_999 }
        letPass(seconds = 300)
        inDialog("Time: 01:03").assertExists()

        inDialog("Resume").performClick()
        compose.onNodeWithText("Paused").assertDoesNotExist()
        clock().assertTextEquals("01:03")
        letPass(seconds = 1)
        clock().assertTextEquals("01:04")
    }

    @Test
    fun aPausedGameStaysPausedWhenTheAppComesBack() {
        launch("""{"dealNumber":1,"elapsedMillis":60000}""")
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitForIdle()

        scenario!!.moveToState(Lifecycle.State.CREATED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        letPass(seconds = 30)
        inDialog("Time: 01:00").assertExists()
    }

    @Test
    fun theClockKeepsGoingThroughUndoAndGoesBackToZeroOnRestart() {
        launch("""{"dealNumber":1}""")
        tap("3D")
        letPass(seconds = 4)
        compose.onNodeWithContentDescription("Undo").performClick()
        letPass(seconds = 4)
        clock().assertTextEquals("00:08")
        waitForSave { it.moveCount == 0 && it.elapsedMillis >= 4_000 }

        tap("3D")
        compose.onNodeWithContentDescription("Restart").performClick()
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
        const val DIVIDER = 4f
        const val HINT_LASTS = 5_000L
        const val EDGE = 60f
        val grip = Offset(30f, 20f)
    }
}
