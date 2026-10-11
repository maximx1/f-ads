package com.maximx1.fads.core.engine

import com.maximx1.fads.core.columnCodes
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import com.maximx1.fads.core.rules.Move.ColumnToColumn
import com.maximx1.fads.core.rules.Move.ColumnToFoundation
import com.maximx1.fads.core.rules.Move.ColumnToFreeCell
import com.maximx1.fads.core.rules.Move.FreeCellToColumn
import com.maximx1.fads.core.rules.Move.FreeCellToFoundation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTest {
    @Test
    fun columnToColumnMovesTheRunInOrder() {
        val before = position("KS 8S 7H 6C", "9D")
        val after = before.play(ColumnToColumn(0, 1, count = 3))
        assertEquals(listOf("KS", "9D 8S 7H 6C"), after.columnCodes().take(2))
        assertEquals(before.freeCells, after.freeCells)
        assertEquals(before.foundations, after.foundations)
    }

    @Test
    fun columnToFreeCellAndBack() {
        val parked = position("KS 7H", "8C").play(ColumnToFreeCell(0, 2))
        assertEquals(listOf("KS", "8C"), parked.columnCodes().take(2))
        assertEquals(listOf(null, null, Card.fromCode("7H"), null), parked.freeCells)

        val back = parked.play(FreeCellToColumn(2, 1))
        assertEquals(listOf("KS", "8C 7H"), back.columnCodes().take(2))
        assertTrue(back.freeCells.all { it == null })
    }

    @Test
    fun columnToFoundation() {
        val after = position("KS 2H", foundations = mapOf(Suit.HEARTS to 1)).play(ColumnToFoundation(0))
        assertEquals("KS", after.columnCodes().first())
        assertEquals(2, after.foundationCount(Suit.HEARTS))
        assertEquals(Card.fromCode("2H"), after.foundationTop(Suit.HEARTS))
    }

    @Test
    fun freeCellToFoundation() {
        val after = position(free = "- AS").play(FreeCellToFoundation(1))
        assertNull(after.freeCells[1])
        assertEquals(1, after.foundationCount(Suit.SPADES))
    }

    @Test
    fun playingDoesNotChangeTheOriginalPosition() {
        val before = position("KS 7H", "8C")
        val copy = before.copy()
        before.play(ColumnToColumn(0, 1))
        assertEquals(copy, before)
    }

    @Test
    fun illegalMovesThrow() {
        val state = position("KS 7H", "8D")
        assertFailsWith<IllegalArgumentException> { state.play(ColumnToColumn(0, 1)) }
        assertFailsWith<IllegalArgumentException> { state.play(ColumnToFoundation(0)) }
        assertFailsWith<IllegalArgumentException> { state.play(FreeCellToFoundation(0)) }
        assertFailsWith<IllegalArgumentException> { state.play(FreeCellToColumn(0, 1)) }
        assertFailsWith<IllegalArgumentException> { state.play(ColumnToFreeCell(2, 0)) }
    }

    @Test
    fun playingTheLastCardWinsTheGame() {
        val almost = position("KS", foundations = Suit.entries.associateWith { if (it == Suit.SPADES) 12 else 13 })
        assertTrue(almost.play(ColumnToFoundation(0)).isWon)
    }
}
