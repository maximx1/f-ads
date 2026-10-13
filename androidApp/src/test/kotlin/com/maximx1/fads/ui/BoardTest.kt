package com.maximx1.fads.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.engine.play
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives the real board with touches. Runs on this machine through Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BoardTest {
    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(Deal.fromNumber(1))
    private val moves = mutableListOf<Move>()

    private fun show(start: GameState = Deal.fromNumber(1)) {
        state = start
        compose.setContent {
            Board(
                state = state,
                onMove = {
                    moves += it
                    state = state.play(it)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        compose.waitForIdle()
    }

    private fun position(
        vararg columns: String,
        free: String = "",
        foundations: Map<Suit, Int> = emptyMap(),
    ): GameState {
        fun cards(codes: String) =
            codes.split(' ').filter { it.isNotEmpty() }.map {
                if (it ==
                    "-"
                ) {
                    null
                } else {
                    Card.fromCode(it)
                }
            }
        return GameState(
            columns = List(8) { cards(columns.getOrElse(it) { "" }).filterNotNull() },
            freeCells = List(4) { cards(free).getOrNull(it) },
            foundations = foundations,
        )
    }

    private fun bounds(card: String): Rect = compose.onNodeWithTag(card).fetchSemanticsNode().boundsInRoot

    private fun tap(card: String) {
        compose.onNodeWithTag(card).performTouchInput {
            down(grip)
            up()
        }
        compose.waitForIdle()
    }

    /** Presses near the top of [card], where it shows even under other cards, and drags to [point] on the board. */
    private fun drag(
        card: String,
        point: Offset,
    ) {
        val start = bounds(card).topLeft
        compose.onNodeWithTag(card).performTouchInput {
            down(grip)
            val travel = point - (start + grip)
            repeat(DRAG_STEPS) { moveBy(travel / DRAG_STEPS.toFloat()) }
            up()
        }
        compose.waitForIdle()
    }

    private fun dragOnto(
        card: String,
        other: String,
    ) = drag(card, bounds(other).center)

    private fun layout(): BoardLayout {
        val root = compose.onRoot().fetchSemanticsNode().size
        val gap = with(compose.density) { 4.dp.toPx() }
        val split = with(compose.density) { 2.dp.toPx() }
        return BoardLayout(root.width.toFloat(), root.height.toFloat(), gap, split, state.columns.map { it.size })
    }

    @Test
    fun tappingACardSendsItWhereItFits() {
        show()
        tap("3D")
        assertEquals(listOf<Move>(Move.ColumnToColumn(0, 4)), moves)
    }

    @Test
    fun tappingACardThatCannotMoveDoesNothing() {
        show(position("9C 6S", "QH", "KH", "KD", "KC", "KS", "QS", "QC", free = "2D 3D 4D 5D"))
        tap("6S")
        assertEquals(emptyList<Move>(), moves)
    }

    @Test
    fun tappingPartWayUpAColumnMovesTheRunFromThere() {
        show(position("KS 8S 7H 6C", "9D"))
        tap("8S")
        assertEquals(listOf<Move>(Move.ColumnToColumn(0, 1, count = 3)), moves)
    }

    @Test
    fun draggingACardOntoALegalCardPlaysTheMove() {
        show()
        dragOnto("9D", "10S")
        assertEquals(listOf<Move>(Move.ColumnToColumn(2, 7)), moves)
        assertEquals(bounds("10S").left, bounds("9D").left)
    }

    @Test
    fun anIllegalDropSlidesBackAndPlaysNothing() {
        show()
        val before = bounds("2C")
        dragOnto("2C", "7C")
        assertEquals(emptyList<Move>(), moves)
        assertEquals(before, bounds("2C"))
    }

    @Test
    fun draggingMovesTheWholeRunAboveTheCard() {
        show(position("KS 8S 7H 6C", "9D"))
        dragOnto("8S", "9D")
        assertEquals(listOf<Move>(Move.ColumnToColumn(0, 1, count = 3)), moves)
        assertEquals(
            listOf("KS", "9D 8S 7H 6C"),
            state.columns.take(2).map { column ->
                column.joinToString(" ") { it.code }
            },
        )
    }

    @Test
    fun cardsThatAreNotARunCannotBePickedUp() {
        show(position("KS 8S 7H 6C", "9D"))
        val before = bounds("KS")
        drag("KS", layout().columnBase(3) + layout().cardCentre)
        assertEquals(emptyList<Move>(), moves)
        assertEquals(before, bounds("KS"))
    }

    @Test
    fun draggingToAFreeCellAndBackToAColumn() {
        show(position("KS 7H", "8C"))
        drag("7H", layout().freeCell(2) + layout().cardCentre)
        assertEquals(listOf<Move>(Move.ColumnToFreeCell(0, 2)), moves)

        dragOnto("7H", "8C")
        assertEquals(listOf(Move.ColumnToFreeCell(0, 2), Move.FreeCellToColumn(2, 1)), moves)
    }

    @Test
    fun draggingToAnyFoundationSlotPlaysToTheCardsOwnFoundation() {
        show(position("KS AH", free = "AS"))
        drag("AH", layout().foundation(Suit.CLUBS) + layout().cardCentre)
        drag("AS", layout().foundation(Suit.DIAMONDS) + layout().cardCentre)
        assertEquals(listOf(Move.ColumnToFoundation(0), Move.FreeCellToFoundation(0)), moves)
        assertEquals(layout().foundation(Suit.HEARTS).round(), bounds("AH").topLeft.round())
        assertEquals(layout().foundation(Suit.SPADES).round(), bounds("AS").topLeft.round())
    }

    @Test
    fun draggingToAnEmptyColumn() {
        show(position("KS 7H", "8D"))
        drag("7H", layout().columnBase(5) + layout().cardCentre)
        assertEquals(listOf<Move>(Move.ColumnToColumn(0, 5)), moves)
    }

    @Test
    fun cardsOnAFoundationStayPut() {
        show(position("KS", foundations = mapOf(Suit.HEARTS to 1)))
        tap("AH")
        drag("AH", layout().columnBase(3) + layout().cardCentre)
        assertEquals(emptyList<Move>(), moves)
    }

    private companion object {
        const val DRAG_STEPS = 8

        /** Where the finger lands on a card: a little in from its top-left corner. */
        val grip = Offset(30f, 20f)
    }
}
