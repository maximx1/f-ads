package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlinkoTest {
    @Test
    fun theBucketsPayMostAtTheEdgesAndTheBoardKeepsAboutOneInAHundred() {
        val tenths = PlinkoBoard.tenths
        assertEquals(PlinkoBoard.ROWS + 1, tenths.size)
        assertEquals(tenths, tenths.reversed())
        assertTrue(tenths.first() > tenths[tenths.size / 2])
        // Each bucket is as likely as the ways of reaching it: twelve choose how many bounces go right.
        var ways = 1L
        var paid = 0.0
        for (bucket in tenths.indices) {
            paid += ways * tenths[bucket] / PlinkoBoard.TENTHS.toDouble()
            ways = ways * (PlinkoBoard.ROWS - bucket) / (bucket + 1)
        }
        val back = paid / (1L shl PlinkoBoard.ROWS)
        assertTrue(back in 0.97..1.0, "pays back $back")
    }

    @Test
    fun aDropFollowsItsNumberAndLandsWhereItsBouncesTakeIt() {
        assertEquals(PlinkoBoard.path(42), PlinkoBoard.path(42))
        assertEquals(PlinkoBoard.ROWS, PlinkoBoard.path(42).size)
        // Every bucket is reached by some number.
        assertEquals(
            (0..PlinkoBoard.ROWS).toSet(),
            (0L until 20_000L)
                .map { n ->
                    PlinkoBoard.path(n).count { it }
                }.toSet(),
        )
        assertEquals(330, PlinkoBoard.payout(10, 0))
        assertEquals(3, PlinkoBoard.payout(10, 6))
        assertEquals(0, PlinkoBoard.payout(1, 6), "a bronze in the middle bucket earns nothing back")
    }

    @Test
    fun theStakeComesOutOfThePurseAndTheBucketPaysBackIn() {
        val number = 7L
        val bucket = PlinkoBoard.path(number).count { it }
        val den = Den(purse = Purse(100)).dropBall(20, number)
        val won = PlinkoBoard.payout(20, bucket)
        assertEquals(Purse(100 - 20 + won), den.purse)
        assertEquals(Drop(PlinkoBoard.path(number), bucket, 20, won), den.plinko().last)
        assertEquals(1, den.plinko().drops)
        assertEquals(0, den.plinko(Room.HIGH_ROLLERS).drops)
        assertEquals(den, Den.fromJson(den.toJson()))
        assertFailsWith<IllegalArgumentException>("too much") { den.dropBall(den.purse.total + 1, 1) }
        assertFailsWith<IllegalArgumentException>("too little") {
            Den(purse = Purse(5_000)).dropBall(999, 1, Room.HIGH_ROLLERS)
        }
    }
}
