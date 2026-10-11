package com.maximx1.fads.core.rules

import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import com.maximx1.fads.core.rules.Move.ColumnToColumn
import com.maximx1.fads.core.rules.Move.ColumnToFoundation
import com.maximx1.fads.core.rules.Move.ColumnToFreeCell
import com.maximx1.fads.core.rules.Move.FreeCellToColumn
import com.maximx1.fads.core.rules.Move.FreeCellToFoundation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TapMoveTest {
    private val blockers = arrayOf("KS", "KH", "KD", "KC", "QS")

    @Test
    fun foundationComesFirst() {
        val state = position("9C AS", "KH 2H", "", *blockers)
        assertEquals(ColumnToFoundation(0), state.tapMoveFromColumn(0))
    }

    @Test
    fun thenOntoAnotherCardLeftmostFirst() {
        val state = position("9C 6S", "KH 7H", "KD 7D", "", *blockers.take(4).toTypedArray())
        assertEquals(ColumnToColumn(0, 1), state.tapMoveFromColumn(0))
    }

    @Test
    fun thenAFreeCellBeforeAnEmptyColumn() {
        val state = position("9C 6S", "", "KH", "KD", "KC", "KS", "QS", "QC", free = "2D")
        assertEquals(ColumnToFreeCell(0, 1), state.tapMoveFromColumn(0))
    }

    @Test
    fun thenAnEmptyColumnWhenTheFreeCellsAreFull() {
        val state = position("9C 6S", "", "KH", "KD", "KC", "KS", "QS", "QC", free = "2D 3D 4D 5D")
        assertEquals(ColumnToColumn(0, 1), state.tapMoveFromColumn(0))
    }

    @Test
    fun nothingWhenThereIsNowhereToGo() {
        val state = position("9C 6S", "QH", "KH", "KD", "KC", "KS", "QS", "QC", free = "2D 3D 4D 5D")
        assertNull(state.tapMoveFromColumn(0))
        assertNull(state.tapMoveFromColumn(1, count = 3))
        assertNull(position().tapMoveFromColumn(0))
    }

    @Test
    fun runsGoOntoACardOrElseAnEmptyColumnButNeverToACellOrFoundation() {
        val onto = position("KS 8S 7H 6C", "9D", "", *blockers)
        assertEquals(ColumnToColumn(0, 1, count = 3), onto.tapMoveFromColumn(0, count = 3))

        val toEmpty = position("KS 8S 7H 6C", "", "KH", "KD", "KC", "QS", "QC", "QD")
        assertEquals(ColumnToColumn(0, 1, count = 3), toEmpty.tapMoveFromColumn(0, count = 3))

        val aceRun = position("3S 2H", "", "KH", "KD", "KC", "QS", "QC", "QD", foundations = mapOf(Suit.HEARTS to 1))
        assertNull(aceRun.tapMoveFromColumn(0, count = 2), "a whole column is not sent to an empty column")
    }

    @Test
    fun aLoneCardIsNotShuffledToAnotherEmptyColumn() {
        val state = position("9C", "", "KH", "KD", "KC", "KS", "QS", "QC", free = "2D 3D 4D 5D")
        assertNull(state.tapMoveFromColumn(0))
    }

    @Test
    fun fromAFreeCell() {
        val toFoundation = position("KS 2H", free = "AS")
        assertEquals(FreeCellToFoundation(0), toFoundation.tapMoveFromFreeCell(0))

        val ontoCard = position("", "KS 7H", *blockers, free = "- 6S")
        assertEquals(FreeCellToColumn(1, 1), ontoCard.tapMoveFromFreeCell(1))

        val toEmpty = position("KS", "", "KH", "KD", "KC", "QS", "QC", "QD", free = "6S")
        assertEquals(FreeCellToColumn(0, 1), toEmpty.tapMoveFromFreeCell(0))

        val stuck = position("KS", "QH", "KH", "KD", "KC", "QS", "QC", "QD", free = "6S")
        assertNull(stuck.tapMoveFromFreeCell(0))
        assertNull(stuck.tapMoveFromFreeCell(1), "empty cell")
    }
}
