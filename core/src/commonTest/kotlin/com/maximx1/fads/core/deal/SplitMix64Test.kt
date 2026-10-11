package com.maximx1.fads.core.deal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplitMix64Test {
    /** Published SplitMix64 reference values, written as unsigned numbers. */
    @Test
    fun matchesTheReferenceValues() {
        assertEquals(
            listOf("e220a8397b1dcdaf", "6e789e6aa1b965f4", "6c45d188009454f"),
            SplitMix64(0).let { random -> List(3) { random.nextLong().toULong().toString(16) } },
        )
        assertEquals(
            listOf(
                "6457827717110365317",
                "3203168211198807973",
                "9817491932198370423",
                "4593380528125082431",
                "16408922859458223821",
            ),
            SplitMix64(1234567).let { random -> List(5) { random.nextLong().toULong().toString() } },
        )
    }

    @Test
    fun nextBelowIsTheUnsignedRemainder() {
        // First value for seed 0 is 0xE220A8397B1DCDAF, which is negative as a signed Long.
        assertEquals((0xE220A8397B1DCDAFuL % 52uL).toInt(), SplitMix64(0).nextBelow(52))
        val random = SplitMix64(42)
        assertTrue(List(1000) { random.nextBelow(7) }.all { it in 0..6 })
    }
}
