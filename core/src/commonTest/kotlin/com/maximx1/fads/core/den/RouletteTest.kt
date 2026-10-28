package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouletteTest {
    /** The first spin number that lands in [pocket]. */
    private fun spinLandingOn(pocket: Int): Long = generateSequence(0L) { it + 1 }.first { Wheel.landing(it) == pocket }

    @Test
    fun theWheelHasEveryNumberOnceAndEighteenOfEachColour() {
        assertEquals((0..36).toSet(), Wheel.pockets.toSet())
        assertEquals(37, Wheel.pockets.size)
        assertEquals(PocketColor.GREEN, Wheel.colorOf(0))
        assertEquals(PocketColor.RED, Wheel.colorOf(32))
        assertEquals(PocketColor.BLACK, Wheel.colorOf(15))
        assertEquals(18, (1..36).count { Wheel.colorOf(it) == PocketColor.RED })
        // Round the wheel the colours take turns.
        assertTrue(
            Wheel.pockets
                .drop(1)
                .zipWithNext()
                .all { (a, b) -> Wheel.colorOf(a) != Wheel.colorOf(b) },
        )
        // Every pocket comes up, and the same spin number always lands in the same one.
        assertEquals((0..36).toSet(), (0L until 2_000L).map(Wheel::landing).toSet())
        assertEquals(Wheel.landing(1234), Wheel.landing(1234))
    }

    @Test
    fun eachBetCoversItsNumbers() {
        assertEquals(setOf(17), Bet(BetKind.STRAIGHT, 17).numbers)
        assertEquals(setOf(0), Bet(BetKind.STRAIGHT, 0).numbers)
        assertEquals(setOf(17, 18), Bet(BetKind.SPLIT_ACROSS, 17).numbers)
        assertEquals(setOf(17, 20), Bet(BetKind.SPLIT_DOWN, 17).numbers)
        assertEquals(setOf(16, 17, 18), Bet(BetKind.STREET, 5).numbers)
        assertEquals(setOf(17, 18, 20, 21), Bet(BetKind.CORNER, 17).numbers)
        assertEquals(setOf(31, 32, 33, 34, 35, 36), Bet(BetKind.SIX_LINE, 10).numbers)
        assertEquals((1..34 step 3).toSet(), Bet(BetKind.COLUMN, 0).numbers)
        assertEquals((3..36 step 3).toSet(), Bet(BetKind.COLUMN, 2).numbers)
        assertEquals((13..24).toSet(), Bet(BetKind.DOZEN, 1).numbers)
        assertEquals((1..18).toSet(), Bet(BetKind.LOW).numbers)
        assertEquals((19..36).toSet(), Bet(BetKind.HIGH).numbers)
        assertEquals(18, Bet(BetKind.RED).numbers.size)
        assertEquals(18, Bet(BetKind.BLACK).numbers.size)
        assertTrue(Bet(BetKind.ODD).wins(35))
        assertFalse(Bet(BetKind.EVEN).wins(0), "zero is neither")
        assertFalse(Bet(BetKind.RED).wins(0))
        assertEquals(listOf(35, 17, 17, 11, 8, 5, 2, 2, 1, 1, 1, 1, 1, 1), BetKind.entries.map { it.pays })
    }

    @Test
    fun aPlaceTheLayoutDoesNotHaveIsRefused() {
        val nowhere =
            listOf(
                BetKind.STRAIGHT to 37,
                BetKind.STRAIGHT to -1,
                // 3 is at the end of its row, and 34 at the foot of its column.
                BetKind.SPLIT_ACROSS to 3,
                BetKind.SPLIT_DOWN to 34,
                BetKind.SPLIT_DOWN to 0,
                BetKind.CORNER to 6,
                BetKind.CORNER to 34,
                BetKind.STREET to 12,
                BetKind.SIX_LINE to 11,
                BetKind.COLUMN to 3,
                BetKind.DOZEN to -1,
                BetKind.RED to 1,
            )
        for ((kind, at) in nowhere) assertFailsWith<IllegalArgumentException>("$kind at $at") { Bet(kind, at) }
    }

    @Test
    fun betsComeOutOfThePurseAndGoBackWhenCleared() {
        val red = Bet(BetKind.RED)
        var den = Den(purse = Purse(100)).placeBet(red, 10).placeBet(red, 5).placeBet(Bet(BetKind.STRAIGHT, 7), 1)
        assertEquals(Purse(84), den.purse)
        assertEquals(15, den.stakeOn(red))
        assertEquals(16, den.roulette.staked)
        assertFailsWith<IllegalArgumentException> { den.placeBet(red, 85) }
        assertFailsWith<IllegalArgumentException> { den.placeBet(red, 0) }
        assertFailsWith<IllegalArgumentException>("under the table's minimum") { den.placeBet(red, 5, minimum = 10) }

        den = den.clearBets()
        assertEquals(Purse(100), den.purse)
        assertEquals(0, den.stakeOn(red))
        assertFalse(den.canRebet, "no spin yet")
    }

    @Test
    fun aSpinPaysEachWinningBetItsOddsAndHandsBackItsStake() {
        val pocket = 17
        val number = spinLandingOn(pocket)
        val den =
            Den(purse = Purse(200))
                .placeBet(Bet(BetKind.STRAIGHT, 17), 2)
                .placeBet(Bet(BetKind.CORNER, 17), 3)
                .placeBet(Bet(BetKind.BLACK), 10)
                .placeBet(Bet(BetKind.EVEN), 20)
                .spin(number)
        // 2 x 36 back for the number, 3 x 9 for the corner and 10 x 2 for black; even loses its 20.
        assertEquals(Spin(pocket, staked = 35, won = 72 + 27 + 20), den.roulette.last)
        assertEquals(Purse(200 - 35 + 119), den.purse)
        assertTrue(den.roulette.bets.isEmpty())
        assertEquals(1, den.roulette.spins)
    }

    @Test
    fun zeroTakesTheEvenMoneyBets() {
        val den =
            Den(
                purse = Purse(50),
            ).placeBet(Bet(BetKind.RED), 10).placeBet(Bet(BetKind.ODD), 10).spin(spinLandingOn(0))
        assertEquals(Spin(0, staked = 20, won = 0), den.roulette.last)
        assertEquals(Purse(30), den.purse)
    }

    @Test
    fun theLastBetsGoDownAgainIfThePurseHoldsThem() {
        val bets = Den(purse = Purse(30)).placeBet(Bet(BetKind.DOZEN, 2), 10).placeBet(Bet(BetKind.COLUMN, 1), 10)
        val spun = bets.spin(spinLandingOn(1))
        assertEquals(Purse(10), spun.purse, "1 is in neither")
        assertFalse(spun.canRebet, "20 down again needs more than 10")
        assertFailsWith<IllegalArgumentException> { spun.rebet() }

        val richer = spun.copy(purse = Purse(25))
        assertTrue(richer.canRebet)
        val again = richer.rebet()
        assertEquals(bets.roulette.bets, again.roulette.bets)
        assertEquals(Purse(5), again.purse)
        assertFalse(again.canRebet, "the bets are already down")
        assertFailsWith<IllegalArgumentException> { Den().spin(1) }
    }

    @Test
    fun betsOnTheLayoutAreSavedAndReadBack() {
        val den = Den(purse = Purse(40)).placeBet(Bet(BetKind.SPLIT_DOWN, 8), 4).placeBet(Bet(BetKind.HIGH), 6)
        assertEquals(den, Den.fromJson(den.toJson()))
        val spun = den.spin(3)
        assertEquals(spun, Den.fromJson(spun.toJson()))
        assertNull(Den.fromJson("""{"roulette":{"bets":[{"kind":"CORNER","at":3},1]}}"""), "no corner at 3")
    }
}
