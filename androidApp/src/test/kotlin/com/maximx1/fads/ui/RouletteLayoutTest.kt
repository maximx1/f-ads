package com.maximx1.fads.ui

import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.ui.den.RouletteLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where each bet sits on the roulette layout, and which bet a tap makes. */
class RouletteLayoutTest {
    private val everyBet: List<Bet> =
        BetKind.entries.flatMap { kind -> (-1..37).mapNotNull { at -> runCatching { Bet(kind, at) }.getOrNull() } }

    @Test
    fun aTapWhereABetsChipsSitMakesThatBet() {
        // 37 numbers, 33 + 24 splits, 12 streets, 22 corners, 11 six lines, 3 columns, 3 dozens and 6 even chances.
        assertEquals(151, everyBet.size)
        for (bet in everyBet) {
            val (across, down) = RouletteLayout.centreOf(bet)
            assertEquals("$bet at $across, $down", bet, RouletteLayout.betAt(across, down))
        }
    }

    @Test
    fun theMiddleOfANumberIsThatNumberAndItsEdgesAreSplitsCornersAndStreets() {
        // 17 is in the middle column, row 5.
        val left = 3.0
        val top = 6.0
        assertEquals(Bet(BetKind.STRAIGHT, 17), RouletteLayout.betAt(left + 0.5, top + 0.5))
        assertEquals(Bet(BetKind.SPLIT_ACROSS, 16), RouletteLayout.betAt(left + 0.1, top + 0.5))
        assertEquals(Bet(BetKind.SPLIT_ACROSS, 17), RouletteLayout.betAt(left + 0.9, top + 0.5))
        assertEquals(Bet(BetKind.SPLIT_DOWN, 14), RouletteLayout.betAt(left + 0.5, top + 0.1))
        assertEquals(Bet(BetKind.CORNER, 17), RouletteLayout.betAt(left + 0.9, top + 0.9))
        assertEquals(Bet(BetKind.STREET, 5), RouletteLayout.betAt(2.1, top + 0.5))
        assertEquals(Bet(BetKind.SIX_LINE, 4), RouletteLayout.betAt(2.1, top + 0.1))
        // The outer edges of the numbers are not lines between two of them.
        assertEquals(Bet(BetKind.STRAIGHT, 18), RouletteLayout.betAt(4.95, top + 0.5))
        assertEquals(Bet(BetKind.STRAIGHT, 2), RouletteLayout.betAt(3.5, 1.05))
    }

    @Test
    fun theOutsideBetsAndTheCornersOfTheLayout() {
        assertEquals(Bet(BetKind.STRAIGHT, 0), RouletteLayout.betAt(4.5, 0.5))
        assertEquals(Bet(BetKind.DOZEN, 2), RouletteLayout.betAt(0.5, 12.9))
        assertEquals(Bet(BetKind.RED), RouletteLayout.betAt(1.5, 5.5))
        assertEquals(Bet(BetKind.HIGH), RouletteLayout.betAt(1.9, 12.9))
        assertEquals(Bet(BetKind.COLUMN, 1), RouletteLayout.betAt(3.5, 13.5))
        // Beside the zero and below the outside bets there is nothing to bet on, and nothing off the layout.
        assertNull(RouletteLayout.betAt(1.0, 0.5))
        assertNull(RouletteLayout.betAt(1.0, 13.5))
        assertNull(RouletteLayout.betAt(-0.1, 5.0))
        assertNull(RouletteLayout.betAt(5.0, 5.0))
        assertNull(RouletteLayout.betAt(2.5, 14.0))
    }
}
