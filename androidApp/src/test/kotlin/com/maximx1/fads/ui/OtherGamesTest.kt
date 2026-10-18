package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.maximx1.fads.MainActivity
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.klondike.KlondikeMove
import com.maximx1.fads.core.klondike.KlondikeState
import com.maximx1.fads.core.spider.SpiderMove
import com.maximx1.fads.core.spider.SpiderState
import com.maximx1.fads.core.spider.legalMoves
import com.maximx1.fads.core.spider.play
import com.maximx1.fads.game.FileStorage
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

/** The games other than FreeCell, in the whole app: choosing them, keeping them, and playing their tables. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class OtherGamesTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun touchScreen() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @After
    fun close() {
        scenario?.close()
    }

    private fun settings() = Settings(FileStorage(files))

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(ELAPSED_TAG) or hasText("Play")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun saved(type: GameType): Game<*, *>? =
        File(files, GameStore.gameFile(type)).takeIf { it.exists() }?.readText()?.let(type::load)

    private fun waitForSave(
        type: GameType,
        matches: (Game<*, *>) -> Boolean,
    ) = compose.waitUntil(TIMEOUT) { saved(type)?.let(matches) == true }

    @Test
    fun tappingTheStockDrawsAndTappingItEmptyTurnsTheWasteBack() {
        val drawOne = GameType.Klondike(1)
        settings().gameType = drawOne
        File(files, GameStore.gameFile(drawOne)).writeText("""{"dealNumber":1}""")
        launch()

        val stock = (drawOne.start(1).state as KlondikeState).stock
        compose.onNodeWithTag(stock.last().code).performTouchInput {
            down(center)
            up()
        }
        waitForSave(drawOne) { it.record.moves == listOf(KlondikeMove.Draw) }

        // Draw the rest, then the empty stock is a place to tap. The app saves as it closes, so close it first.
        scenario!!.close()
        File(files, GameStore.gameFile(drawOne)).writeText(
            """{"dealNumber":1,"moves":[""" + List(stock.size) { """{"type":"draw"}""" }.joinToString(",") + "]}",
        )
        launch()
        compose.onNodeWithTag("${SLOT_TAG}0").performClick()
        waitForSave(drawOne) { it.record.moves.lastOrNull() == KlondikeMove.Recycle }
    }

    @Test
    fun theFirstOpenOffersSolitaireToo() {
        launch()
        compose.onNodeWithText("Solitaire").performClick()
        compose.onNodeWithText("Draw 1").assertIsSelected()
        compose.onNodeWithText("Play").performClick()
        compose.waitUntil(TIMEOUT) { settings().gameType == GameType.Klondike(1) }
        waitForSave(GameType.Klondike(1)) { it.moveCount == 0 }
        assertEquals(GameType.Klondike(1), settings().gameType)
    }

    @Test
    fun tappingSpidersStockDealsARowAndARunMovesByDraggingItsFirstCard() {
        val oneSuit = GameType.Spider(1)
        settings().gameType = oneSuit
        File(files, GameStore.gameFile(oneSuit)).writeText("""{"dealNumber":1}""")
        launch()

        val state = oneSuit.start(1).state as SpiderState
        compose.onNodeWithTag(SpiderLayout.tagOf(state.stock.last())).performClick()
        waitForSave(oneSuit) { it.record.moves == listOf(SpiderMove.DealRow) }

        // Any legal move between piles, played by dragging its lowest card onto the pile it goes to.
        val dealt = state.play(SpiderMove.DealRow)
        val move = dealt.legalMoves().filterIsInstance<SpiderMove.PileToPile>().first()
        val lead = dealt.piles[move.from][dealt.piles[move.from].size - move.count]
        val target = dealt.piles[move.to].last()
        val to =
            compose
                .onNodeWithTag(SpiderLayout.tagOf(target))
                .fetchSemanticsNode()
                .boundsInRoot.center
        val from =
            compose
                .onNodeWithTag(SpiderLayout.tagOf(lead))
                .fetchSemanticsNode()
                .boundsInRoot.topLeft
        compose.onNodeWithTag(SpiderLayout.tagOf(lead)).performTouchInput {
            down(grip)
            val travel = to - (from + grip)
            repeat(DRAG_STEPS) { moveBy(travel / DRAG_STEPS.toFloat()) }
            up()
        }
        waitForSave(oneSuit) { it.record.moves.lastOrNull() == move }
    }

    private companion object {
        const val DRAG_STEPS = 10
        val grip = Offset(20f, 12f)
        const val TIMEOUT = 10_000L
    }
}
