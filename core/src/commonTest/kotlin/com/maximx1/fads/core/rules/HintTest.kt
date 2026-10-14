package com.maximx1.fads.core.rules

import com.maximx1.fads.core.engine.play
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HintTest {
    // Columns to fill out a position. Black on black never stacks, so these stay out of the way as long as the
    // columns under test show no red king, queen or jack for them to go to.
    private val rest = arrayOf("KS", "KC", "QS", "QC", "JS", "JC")

    @Test
    fun aCardThatCanGoToAFoundationIsSuggestedFirst() {
        val state = position("9S 7H", "8C", "5D AS", *rest.copyOfRange(0, 5), free = "AH")
        assertEquals(Move.ColumnToFoundation(2), state.hint())
        assertEquals(Move.FreeCellToFoundation(0), position("9S 7H", "8C", *rest, free = "AH").hint())
    }

    @Test
    fun buildingOnACardBeatsUsingAFreeCellOrAnEmptyColumn() {
        assertEquals(Move.ColumnToColumn(0, 1), position("9S 7H", "5D 8C", *rest).hint())
        assertEquals(Move.FreeCellToColumn(0, 1), position("9S", "5D 8C", *rest, free = "7H").hint())
    }

    @Test
    fun theLongestRunThatFitsIsSuggested() {
        val state = position("9S 8S 7H 6C", "5D 9D", *rest)
        assertEquals(Move.ColumnToColumn(0, 1, count = 3), state.hint())
    }

    @Test
    fun uncoveringACardForAFoundationIsWorthAFreeCell() {
        val state = position("9S AH 9C", "5D", "5H", *rest.copyOfRange(0, 5))
        assertEquals(Move.ColumnToFreeCell(0, 0), state.hint())
    }

    @Test
    fun aRunIsNotSplitWhenThereIsSomethingBetterToDo() {
        // 7S could leave 8D for 8H, but 6D going onto 7S builds something new.
        val state = position("9S 8D 7S", "5D 8H", "5H 6D", "4S", *rest.copyOfRange(0, 4))
        assertTrue(state.canMove(Move.ColumnToColumn(0, 1)))
        assertEquals(Move.ColumnToColumn(2, 0), state.hint())
    }

    @Test
    fun itNeverSuggestsMovingAWholeColumnToAnEmptyOneOrTakingACardBack() {
        // Every column card and the 5H could go to the empty column, but only a free cell's card is worth it.
        val state = position(*rest, "10S", "", free = "9S 9C 8S 8C", foundations = mapOf(Suit.HEARTS to 5))
        assertTrue(state.canMove(Move.ColumnToColumn(6, 7)))
        assertTrue(state.canMove(Move.FoundationToColumn(Suit.HEARTS, 7)))
        assertEquals(Move.FreeCellToColumn(0, 7), state.hint())
    }

    @Test
    fun itNeverSuggestsPuttingTheLastMoveBack() {
        val before = position("9S 8D 7S", "5D 8H", *rest)
        val after = before.play(Move.ColumnToColumn(0, 1))
        assertEquals(Move.ColumnToColumn(1, 0), after.hint())

        val other = assertNotNull(after.hint(before))
        assertNotEquals<Move>(Move.ColumnToColumn(1, 0), other)
        assertTrue(after.canMove(other))
    }

    @Test
    fun thereIsNoHintWhenNothingCanMoveOrTheGameIsWon() {
        val columns = arrayOf("KS 2S", "KH 2H", "KD 2D", "KC 2C", "QS", "QH", "QD", "QC")
        assertNull(position(*columns, free = "5S 5H 5D 5C").hint())
        assertNull(position(foundations = Suit.entries.associateWith { 13 }).hint())
    }
}
