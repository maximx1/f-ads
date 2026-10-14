package com.maximx1.fads.core.rules

import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import com.maximx1.fads.core.rules.Move.ColumnToColumn
import com.maximx1.fads.core.rules.Move.ColumnToFoundation
import com.maximx1.fads.core.rules.Move.ColumnToFreeCell
import com.maximx1.fads.core.rules.Move.FoundationToColumn
import com.maximx1.fads.core.rules.Move.FoundationToFreeCell
import com.maximx1.fads.core.rules.Move.FreeCellToColumn
import com.maximx1.fads.core.rules.Move.FreeCellToFoundation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveLegalityTest {
    @Test
    fun singleCardBetweenColumns() {
        val state = position("KS 6S", "9C 7H", "9D 7C", "", "QD", "QH", "QC", "QS")
        assertTrue(state.canMove(ColumnToColumn(0, 1)))
        assertFalse(state.canMove(ColumnToColumn(0, 2)), "same colour")
        assertTrue(state.canMove(ColumnToColumn(0, 3)), "empty column takes anything")
        assertFalse(state.canMove(ColumnToColumn(0, 4)), "wrong rank")
    }

    @Test
    fun columnMovesRejectNonsense() {
        val state = position("KS 6S", "9C 7H", "", "QD", "QH", "QC", "QS", "JD")
        assertFalse(state.canMove(ColumnToColumn(0, 0)), "onto itself")
        assertFalse(state.canMove(ColumnToColumn(2, 1)), "from an empty column")
        assertFalse(state.canMove(ColumnToColumn(0, 1, count = 0)))
        assertFalse(state.canMove(ColumnToColumn(0, 1, count = -1)))
        assertFalse(state.canMove(ColumnToColumn(0, 1, count = 3)), "more cards than the column has")
        assertFalse(state.canMove(ColumnToColumn(-1, 1)))
        assertFalse(state.canMove(ColumnToColumn(0, 8)))
        assertFalse(state.canMove(ColumnToColumn(8, 0)))
    }

    @Test
    fun runsMoveOnlyWhenTheyAreRunsAndTheBottomCardFits() {
        val state = position("KS 8S 7H 6C", "9D", "9C", "2S 4D 3C", "QD", "QH", "QC", "QS")
        assertTrue(state.canMove(ColumnToColumn(0, 1, count = 3)))
        assertFalse(state.canMove(ColumnToColumn(0, 2, count = 3)), "8S on 9C is the same colour")
        assertFalse(state.canMove(ColumnToColumn(0, 1, count = 2)), "7H does not fit on 9D")
        assertFalse(state.canMove(ColumnToColumn(0, 1, count = 4)), "KS 8S is not a run")
        assertFalse(state.canMove(ColumnToColumn(3, 0, count = 3)), "2S 4D 3C is not a run")
    }

    @Test
    fun runLengthIsLimitedByFreeCellsAndEmptyColumns() {
        val columns = arrayOf("KS 8S 7H 6C 5H 4C 3H", "9D", "QD", "QH", "QC", "QS", "JD", "JH")
        assertFalse(position(*columns).canMove(ColumnToColumn(0, 1, count = 6)), "5 is the limit with 4 free cells")
        assertFalse(position(*columns, free = "2C 2D").canMove(ColumnToColumn(0, 1, count = 6)))

        val withEmptyColumn = arrayOf("KS 8S 7H 6C 5H 4C 3H", "9D", "QD", "QH", "QC", "QS", "JD", "")
        assertTrue(position(*withEmptyColumn).canMove(ColumnToColumn(0, 1, count = 6)))
        assertTrue(position(*withEmptyColumn, free = "2C 2D").canMove(ColumnToColumn(0, 1, count = 6)))
        assertFalse(position(*withEmptyColumn, free = "2C 2D 2H").canMove(ColumnToColumn(0, 1, count = 6)))
    }

    @Test
    fun movingARunIntoAnEmptyColumnCannotUseThatColumnAsSpace() {
        val state = position("KS 8S 7H 6C 5H 4C 3H", "9C", "QD", "QH", "QC", "QS", "JD", "")
        assertTrue(state.canMove(ColumnToColumn(0, 7, count = 5)))
        assertFalse(state.canMove(ColumnToColumn(0, 7, count = 6)))
    }

    @Test
    fun toFreeCell() {
        val state = position("KS", "", free = "- 2C")
        assertTrue(state.canMove(ColumnToFreeCell(0, 0)))
        assertFalse(state.canMove(ColumnToFreeCell(0, 1)), "cell is taken")
        assertFalse(state.canMove(ColumnToFreeCell(1, 0)), "column is empty")
        assertFalse(state.canMove(ColumnToFreeCell(0, 4)))
        assertFalse(state.canMove(ColumnToFreeCell(0, -1)))
        assertFalse(state.canMove(ColumnToFreeCell(8, 0)))
    }

    @Test
    fun toFoundationFromAColumn() {
        val state = position("KS AS", "KH 2H", "KD 3H", "", foundations = mapOf(Suit.HEARTS to 1))
        assertTrue(state.canMove(ColumnToFoundation(0)))
        assertTrue(state.canMove(ColumnToFoundation(1)))
        assertFalse(state.canMove(ColumnToFoundation(2)), "2H has to go first")
        assertFalse(state.canMove(ColumnToFoundation(3)), "column is empty")
        assertFalse(state.canMove(ColumnToFoundation(8)))
    }

    @Test
    fun fromFreeCell() {
        val state = position("KS 7H", "KD 7S", "", free = "6S - AS 2H")
        assertTrue(state.canMove(FreeCellToColumn(0, 0)))
        assertFalse(state.canMove(FreeCellToColumn(0, 1)), "same colour")
        assertTrue(state.canMove(FreeCellToColumn(0, 2)), "empty column")
        assertFalse(state.canMove(FreeCellToColumn(1, 2)), "cell is empty")
        assertFalse(state.canMove(FreeCellToColumn(0, 8)))
        assertFalse(state.canMove(FreeCellToColumn(4, 0)))

        assertTrue(state.canMove(FreeCellToFoundation(2)))
        assertFalse(state.canMove(FreeCellToFoundation(3)), "AH has to go first")
        assertFalse(state.canMove(FreeCellToFoundation(1)), "cell is empty")
        assertFalse(state.canMove(FreeCellToFoundation(4)))
    }

    @Test
    fun backFromAFoundation() {
        val state =
            position("KS 3S", "KD 3D", "", free = "- 9C", foundations = mapOf(Suit.HEARTS to 2, Suit.CLUBS to 0))
        assertTrue(state.canMove(FoundationToColumn(Suit.HEARTS, 0)), "2H goes on 3S")
        assertFalse(state.canMove(FoundationToColumn(Suit.HEARTS, 1)), "same colour")
        assertTrue(state.canMove(FoundationToColumn(Suit.HEARTS, 2)), "empty column")
        assertFalse(state.canMove(FoundationToColumn(Suit.CLUBS, 2)), "nothing on that foundation")
        assertFalse(state.canMove(FoundationToColumn(Suit.SPADES, 2)), "nothing on that foundation")
        assertFalse(state.canMove(FoundationToColumn(Suit.HEARTS, 8)))
        assertFalse(state.canMove(FoundationToColumn(Suit.HEARTS, -1)))

        assertTrue(state.canMove(FoundationToFreeCell(Suit.HEARTS, 0)))
        assertFalse(state.canMove(FoundationToFreeCell(Suit.HEARTS, 1)), "cell is taken")
        assertFalse(state.canMove(FoundationToFreeCell(Suit.SPADES, 0)), "nothing on that foundation")
        assertFalse(state.canMove(FoundationToFreeCell(Suit.HEARTS, 4)))
    }

    @Test
    fun legalMovesListsExactlyTheLegalOnes() {
        val state = position("8S 7H", "8D", "AS", "KC", "KD", "KH", "KS", "QD", free = "2H 3H 4H 5H")
        assertEquals(
            setOf(ColumnToFoundation(2), ColumnToColumn(7, 3), ColumnToColumn(7, 6)),
            state.legalMoves().toSet(),
        )
    }

    @Test
    fun legalMovesIncludeTakingCardsBackOffTheFoundations() {
        val columns = arrayOf("8S", "8D", "3S", "KC", "KD", "KH", "KS", "QD")
        val state = position(*columns, free = "2C 3C 4C -", foundations = mapOf(Suit.HEARTS to 2))
        assertEquals(
            setOf(FoundationToColumn(Suit.HEARTS, 2), FoundationToFreeCell(Suit.HEARTS, 3)),
            state.legalMoves().filter { it is FoundationToColumn || it is FoundationToFreeCell }.toSet(),
        )
    }

    @Test
    fun aWonGameCanOnlyTakeItsKingsBack() {
        val moves = position(foundations = Suit.entries.associateWith { 13 }).legalMoves()
        assertEquals(Suit.entries.size * (8 + 4), moves.size)
        assertTrue(moves.all { it is FoundationToColumn || it is FoundationToFreeCell })
    }

    @Test
    fun aGameWithNothingToMoveAndNothingPlayedHasNoMoves() {
        val columns = arrayOf("KS 2S", "KH 2H", "KD 2D", "KC 2C", "QS", "QH", "QD", "QC")
        assertEquals(emptyList(), position(*columns, free = "5S 5H 5D 5C").legalMoves())
    }
}
