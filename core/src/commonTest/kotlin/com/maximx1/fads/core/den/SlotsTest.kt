package com.maximx1.fads.core.den

import com.maximx1.fads.core.den.SlotSymbol.BAR
import com.maximx1.fads.core.den.SlotSymbol.BELL
import com.maximx1.fads.core.den.SlotSymbol.CHERRY
import com.maximx1.fads.core.den.SlotSymbol.LEMON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SlotsTest {
    @Test
    fun theReelsHoldMostlyLowSymbolsAndPayBackAboutNinetySixInAHundred() {
        val counts = SlotMachine.strip.groupingBy { it }.eachCount()
        assertEquals(
            mapOf(CHERRY to 6, LEMON to 5, BELL to 4, BAR to 3, SlotSymbol.SEVEN to 1, SlotSymbol.DIAMOND to 1),
            counts,
        )
        // Every way the three reels can stop, each as likely as the next.
        val size = SlotMachine.strip.size
        var paid = 0L
        for (a in 0 until size) {
            for (b in 0 until size) {
                for (c in 0 until size) {
                    paid +=
                        SlotMachine.payout(listOf(a, b, c), 1)
                }
            }
        }
        val back = paid.toDouble() / (size * size * size * SlotMachine.lines.size)
        assertTrue(back in 0.9..1.0, "pays back $back")
    }

    @Test
    fun aLinePaysForThreeOfASymbolOrTwoCherriesToStart() {
        assertEquals(500, SlotMachine.pays(listOf(SlotSymbol.DIAMOND, SlotSymbol.DIAMOND, SlotSymbol.DIAMOND)))
        assertEquals(10, SlotMachine.pays(listOf(CHERRY, CHERRY, CHERRY)))
        assertEquals(2, SlotMachine.pays(listOf(CHERRY, CHERRY, BAR)))
        assertEquals(0, SlotMachine.pays(listOf(BAR, CHERRY, CHERRY)))
        assertEquals(0, SlotMachine.pays(listOf(LEMON, LEMON, BELL)))
        assertEquals(5, SlotMachine.lines.size)
    }

    @Test
    fun theWindowShowsThreeOfEachReelAndTheLinesReadAcrossIt() {
        val window = SlotMachine.window(listOf(0, 19, 3))
        assertEquals(listOf(CHERRY, LEMON, BELL), window[0])
        assertEquals(listOf(BAR, CHERRY, LEMON), window[1], "the strip wraps round")
        assertEquals(SlotMachine.stops(9), SlotMachine.stops(9))
        // Stopped so the middle row reads cherry, cherry, cherry: the middle line pays, and every line that starts
        // with two cherries.
        val stops = listOf(19, 19, 19)
        assertEquals(listOf(CHERRY, CHERRY, CHERRY), SlotMachine.window(stops).map { it[1] })
        assertTrue(0 in SlotMachine.winningLines(stops))
        assertTrue(SlotMachine.payout(stops, 1) >= SlotSymbol.CHERRY.pays)
    }

    @Test
    fun aPullTakesABetForEveryLineAndPaysWhatTheLinesWin() {
        val number = 4L
        val stops = SlotMachine.stops(number)
        val den = Den(purse = Purse(100)).pullLever(2, number)
        val won = SlotMachine.payout(stops, 2)
        assertEquals(Purse(100 - 10 + won), den.purse)
        assertEquals(SlotSpin(stops, 2, won), den.slots().last)
        assertEquals(1, den.slots().pulls)
        assertEquals(den, Den.fromJson(den.toJson()))
        assertFailsWith<IllegalArgumentException>("five lines of 30 is more than the purse") {
            Den(purse = Purse(100)).pullLever(30, 1)
        }
        assertFailsWith<IllegalArgumentException>("under a thousand") {
            Den(purse = Purse(50_000)).pullLever(999, 1, Room.HIGH_ROLLERS)
        }
    }
}
