package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyWheelTest {
    @Test
    fun theWheelHasEachSymbolAsOftenAsItShouldAndTheHouseItsEdge() {
        val wheel = MoneyWheel.segments
        assertEquals(54, wheel.size)
        for (symbol in WheelSymbol.entries) assertEquals(symbol.segments, wheel.count { it == symbol }, "$symbol")
        assertEquals(wheel, MoneyWheel.segments, "always the same wheel")
        // No two of the rare ones side by side.
        val rare = wheel.indices.filter { wheel[it].pays >= 10 }
        assertTrue(rare.zipWithNext().all { (a, b) -> b - a > 1 })
        for (symbol in WheelSymbol.entries) {
            val back = symbol.segments * (symbol.pays + 1.0) / wheel.size
            assertTrue(back in 0.7..0.9, "$symbol pays back $back")
        }
        assertEquals((0 until 54).toSet(), (0L until 3_000L).map(MoneyWheel::landing).toSet())
    }

    @Test
    fun aSpinPaysTheSymbolItStopsOnItsOdds() {
        val tenAt = MoneyWheel.segments.indexOf(WheelSymbol.TEN)
        val bets = Den(purse = Purse(100)).betMoneyWheel(WheelSymbol.TEN, 5).betMoneyWheel(WheelSymbol.ONE, 10)
        assertEquals(Purse(85), bets.purse)
        val spun = bets.spinMoneyWheelTo(tenAt)
        assertEquals(55, spun.moneyWheel().paid)
        assertEquals(Purse(85 + 55), spun.purse)
        assertEquals(tenAt, spun.moneyWheel().landed)
        assertTrue(spun.moneyWheel().bets.isEmpty())
        assertEquals(spun, Den.fromJson(spun.toJson()))
        assertEquals(Purse(100), bets.clearMoneyWheel().purse)
        assertEquals(1, bets.spinMoneyWheel(9).moneyWheel().spins)
        assertFailsWith<IllegalArgumentException> { Den().spinMoneyWheel(1) }
        assertFailsWith<IllegalArgumentException> {
            Den(purse = Purse(5_000)).betMoneyWheel(WheelSymbol.LOGO, 999, Room.HIGH_ROLLERS)
        }
    }
}
