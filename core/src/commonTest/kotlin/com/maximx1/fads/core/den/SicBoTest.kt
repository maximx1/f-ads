package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SicBoTest {
    private val everyThrow = (1..6).flatMap { a -> (1..6).flatMap { b -> (1..6).map { c -> listOf(a, b, c) } } }

    /** What a bronze on [bet] comes back as, on average over every throw. */
    private fun back(bet: SicBoBet) = everyThrow.sumOf { bet.returns(it) }.toDouble() / everyThrow.size

    @Test
    fun everyBetGivesTheHouseItsEdge() {
        val bets =
            listOf(
                SicBoKind.SMALL,
                SicBoKind.BIG,
                SicBoKind.ODD,
                SicBoKind.EVEN,
                SicBoKind.ANY_TRIPLE,
            ).map { SicBoBet(it) } +
                SicBoBet.TOTAL_PAYS.keys.map { SicBoBet(SicBoKind.TOTAL, it) } +
                listOf(SicBoKind.SINGLE, SicBoKind.DOUBLE, SicBoKind.TRIPLE).flatMap { kind ->
                    (1..6).map { SicBoBet(kind, it) }
                }
        for (bet in bets) assertTrue(back(bet) in 0.8..1.0, "$bet pays back ${back(bet)}")
    }

    @Test
    fun eachBetPaysOnTheThrowsItCovers() {
        assertEquals(2, SicBoBet(SicBoKind.SMALL).returns(listOf(1, 2, 3)))
        assertEquals(0, SicBoBet(SicBoKind.SMALL).returns(listOf(2, 2, 2)), "a triple takes small and big")
        assertEquals(2, SicBoBet(SicBoKind.BIG).returns(listOf(6, 5, 1)))
        assertEquals(2, SicBoBet(SicBoKind.ODD).returns(listOf(6, 5, 2)))
        assertEquals(61, SicBoBet(SicBoKind.TOTAL, 4).returns(listOf(1, 1, 2)))
        assertEquals(3, SicBoBet(SicBoKind.SINGLE, 5).returns(listOf(5, 5, 2)), "two to one for two of them")
        assertEquals(4, SicBoBet(SicBoKind.SINGLE, 5).returns(listOf(5, 5, 5)))
        assertEquals(11, SicBoBet(SicBoKind.DOUBLE, 5).returns(listOf(5, 5, 2)))
        assertEquals(181, SicBoBet(SicBoKind.TRIPLE, 3).returns(listOf(3, 3, 3)))
        assertEquals(31, SicBoBet(SicBoKind.ANY_TRIPLE).returns(listOf(6, 6, 6)))
        assertEquals(0, SicBoBet(SicBoKind.TRIPLE, 3).returns(listOf(6, 6, 6)))
        assertFailsWith<IllegalArgumentException> { SicBoBet(SicBoKind.TOTAL, 3) }
        assertFailsWith<IllegalArgumentException> { SicBoBet(SicBoKind.SINGLE, 7) }
        assertFailsWith<IllegalArgumentException> { SicBoBet(SicBoKind.BIG, 1) }
    }

    @Test
    fun aThrowPaysEveryBetAndClearsTheLayout() {
        val den =
            Den(purse = Purse(100))
                .betSicBo(SicBoBet(SicBoKind.BIG), 10)
                .betSicBo(SicBoBet(SicBoKind.SINGLE, 6), 5)
                .rollSicBoAs(listOf(6, 6, 2))
        assertEquals(20 + 15, den.sicBo().paid)
        assertEquals(Purse(100 - 15 + 35), den.purse)
        assertEquals(listOf(6, 6, 2), den.sicBo().dice)
        assertTrue(den.sicBo().bets.isEmpty())
        assertEquals(den, Den.fromJson(den.toJson()))
        assertEquals(Purse(100), Den(purse = Purse(100)).betSicBo(SicBoBet(SicBoKind.ODD), 7).clearSicBo().purse)
        assertFailsWith<IllegalArgumentException> { Den().rollSicBo(1) }
        assertEquals(
            1,
            Den(purse = Purse(9))
                .betSicBo(SicBoBet(SicBoKind.EVEN), 1)
                .rollSicBo(5)
                .sicBo()
                .throws,
        )
    }
}
