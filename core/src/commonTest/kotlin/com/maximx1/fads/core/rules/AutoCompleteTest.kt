package com.maximx1.fads.core.rules

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.engine.dealOneWin
import com.maximx1.fads.core.engine.play
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoCompleteTest {
    private val upToNine = Suit.entries.associateWith { 9 }

    @Test
    fun aFreshDealCannotFinishItself() {
        assertFalse(Deal.fromNumber(1).canAutoComplete)
    }

    @Test
    fun columnsThatOnlyEverStepDownCanFinishThemselves() {
        // Colours and suits do not matter, and equal ranks may sit on each other.
        var state =
            position("KS QH QS 10C", "KH JH", "KD JD JS 10S", "KC JC", free = "QD QC 10H 10D", foundations = upToNine)
        assertTrue(state.canAutoComplete)
        while (true) state = state.play(state.nextAutoMove() ?: break)
        assertTrue(state.isWon)
        assertFalse(state.canAutoComplete)
    }

    @Test
    fun aLowerCardUnderAHigherOneStopsIt() {
        assertFalse(position("9H KS", foundations = upToNine - Suit.HEARTS + (Suit.HEARTS to 8)).canAutoComplete)
    }

    @Test
    fun aWonGameHasNothingLeftToFinish() {
        val won = dealOneWin.fold(Deal.fromNumber(1)) { state, move -> state.play(move) }
        assertFalse(won.canAutoComplete)
        assertNull(won.nextAutoMove())
    }

    @Test
    fun theLowestCardGoesFirstWhetherInAColumnOrAFreeCell() {
        val foundations = mapOf(Suit.SPADES to 9, Suit.HEARTS to 10, Suit.DIAMONDS to 12, Suit.CLUBS to 11)
        val state = position("KS QS JS", "KH QH", "KD", "KC QC", free = "- 10S JH", foundations = foundations)
        assertEquals(Move.FreeCellToFoundation(1), state.nextAutoMove())
        assertEquals(
            Move.ColumnToFoundation(3),
            position("KS QS JS", "KH QH", "KD", "KC QC JH", foundations = foundations).nextAutoMove(),
        )
    }

    @Test
    fun playingTheAutoMovesOneAfterAnotherWinsTheGame() {
        // Deal 1 played to the first point where it can finish, then left to finish by itself.
        var state =
            dealOneWin.indices
                .map { moves -> dealOneWin.take(moves).fold(Deal.fromNumber(1)) { state, move -> state.play(move) } }
                .first { it.canAutoComplete }
        while (true) state = state.play(state.nextAutoMove() ?: break)
        assertTrue(state.isWon)
    }
}
