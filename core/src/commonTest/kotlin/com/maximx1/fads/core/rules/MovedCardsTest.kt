package com.maximx1.fads.core.rules

import com.maximx1.fads.core.cards
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import kotlin.test.Test
import kotlin.test.assertEquals

class MovedCardsTest {
    @Test
    fun aMoveBetweenColumnsPicksUpTheRunAndEveryOtherMoveOneCard() {
        val state = position("KS 8S 7H 6C", "9D", free = "- 5H", foundations = mapOf(Suit.CLUBS to 2))
        assertEquals(cards("8S 7H 6C"), state.cardsMovedBy(Move.ColumnToColumn(0, 1, count = 3)))
        assertEquals(cards("6C"), state.cardsMovedBy(Move.ColumnToFreeCell(0, 0)))
        assertEquals(cards("9D"), state.cardsMovedBy(Move.ColumnToFoundation(1)))
        assertEquals(cards("5H"), state.cardsMovedBy(Move.FreeCellToColumn(1, 0)))
        assertEquals(cards("5H"), state.cardsMovedBy(Move.FreeCellToFoundation(1)))
        assertEquals(cards("2C"), state.cardsMovedBy(Move.FoundationToColumn(Suit.CLUBS, 2)))
        assertEquals(cards("2C"), state.cardsMovedBy(Move.FoundationToFreeCell(Suit.CLUBS, 0)))
    }

    @Test
    fun cardsUnderSomethingThatIsNotARunAreStuck() {
        val state = position("KS 8S 7H 6C", "9D", "QD", "QH", "QC", "QS", "JD", "JH")
        // KS has 8S on it, which does not follow a king. The run from 8S can go to 9D, and 7H goes with it.
        assertEquals(cards("KS").toSet(), state.stuckCards() intersect cards("KS 8S 7H 6C").toSet())
    }

    @Test
    fun cardsWithNowhereToGoAreStuckEvenOnTopOfAColumn() {
        val columns = arrayOf("KS 2S", "KH 2H", "KD 2D", "KC 2C", "QS", "QH", "QD", "QC")
        val state = position(*columns, free = "5S 5H 5D 5C")
        assertEquals(state.columns.flatten().toSet() + state.freeCells.filterNotNull(), state.stuckCards())
    }

    @Test
    fun aRunTooLongToMoveIsStuckFromTheCardThatMakesItTooLong() {
        // No free cells and no empty column: only one card can move at a time.
        val columns = arrayOf("KS 8S 7H", "9D", "QD", "QH", "QC", "QS", "JD", "JH 8C")
        val state = position(*columns, free = "2C 2D 2H 2S")
        assertEquals(cards("KS 8S").toSet(), state.stuckCards() intersect cards("KS 8S 7H").toSet())
    }

    @Test
    fun cardsOnTheFoundationsAreNeverStuck() {
        val state = position("KS", foundations = mapOf(Suit.HEARTS to 3))
        assertEquals(emptySet(), state.stuckCards())
    }
}
