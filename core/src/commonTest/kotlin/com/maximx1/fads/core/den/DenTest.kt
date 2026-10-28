package com.maximx1.fads.core.den

import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.History
import com.maximx1.fads.core.engine.Played
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DenTest {
    private fun played(
        score: Int,
        won: Boolean,
    ) = Played(GameType.FreeCell, 1, endedAt = 0, millis = 0, moves = 1, score = score, won = won, game = "")

    @Test
    fun aPurseIsCountedOutInTheFewestCoins() {
        val purse = Purse(Purse.GOLD * 3 + Purse.SILVER * 2 + 4)
        assertEquals(3, purse.golds)
        assertEquals(2, purse.silvers)
        assertEquals(4, purse.bronzes)
        // Two silvers and a few bronzes make no gold, but a third silver would.
        assertEquals(0, Purse(24).golds)
        assertEquals(2, Purse(24).silvers)
        assertEquals(1, Purse(35).golds)
        assertEquals(1, Purse(35).silvers)
        assertEquals(0, Purse(35).bronzes)
    }

    @Test
    fun aPurseTakesInAndPaysOutButNeverMoreThanItHolds() {
        val purse = Purse(30) + 12
        assertEquals(42, purse.total)
        assertEquals(Purse(2), purse - 40)
        assertTrue(purse.holds(42))
        assertFalse(purse.holds(43))
        assertFailsWith<IllegalArgumentException> { purse - 43 }
        assertFailsWith<IllegalArgumentException> { Purse(-1) }
    }

    @Test
    fun onlyWinsEarnPoints() {
        val history = History(listOf(played(2_500, won = true), played(500, won = false), played(1_234, won = true)))
        assertEquals(3_734, history.pointsWon())
        assertEquals(3_734, Den().pointsLeft(history))
        assertEquals(37, Den().bronzesToCashIn(history))
    }

    @Test
    fun cashingInTakesWholeHundredsAndLeavesTheRestForNextTime() {
        var history = History(listOf(played(1_234, won = true)))
        val den = Den(purse = Purse(5)).cashIn(history)
        assertEquals(Purse(17), den.purse)
        assertEquals(1_200, den.cashedIn)
        assertEquals(34, den.pointsLeft(history))
        assertEquals(den, den.cashIn(history), "nothing more to cash in")

        // The next win adds to what was left over.
        history = history.with(played(70, won = true))
        assertEquals(104, den.pointsLeft(history))
        assertEquals(Purse(18), den.cashIn(history).purse)
    }

    @Test
    fun pointsLeftNeverGoBelowNothing() {
        assertEquals(0, Den(cashedIn = 500).pointsLeft(History()))
        assertEquals(0, Den(cashedIn = 500).bronzesToCashIn(History()))
    }

    @Test
    fun aDenIsSavedAndReadBack() {
        val den = Den(cashedIn = 300, purse = Purse(61))
        assertEquals(den, Den.fromJson(den.toJson()))
        assertEquals(Den(), Den.fromJson("{}"))
        assertNull(Den.fromJson("not a den"))
    }
}
