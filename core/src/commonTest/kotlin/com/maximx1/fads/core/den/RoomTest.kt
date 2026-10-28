package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomTest {
    @Test
    fun theHighRollersRoomTakesABigPurseToWalkInto() {
        assertTrue(Den().canEnter(Room.FLOOR))
        assertFalse(Den(purse = Purse(24_999)).canEnter(Room.HIGH_ROLLERS))
        assertTrue(Den(purse = Purse(25_000)).canEnter(Room.HIGH_ROLLERS))
        assertEquals(1_000, Room.HIGH_ROLLERS.leastBet)
    }

    @Test
    fun theHighRollersRouletteTableTakesThousandsAndKeepsItsOwnBets() {
        val red = Bet(BetKind.RED)
        assertFailsWith<IllegalArgumentException> { Den(purse = Purse(30_000)).placeBet(red, 999, Room.HIGH_ROLLERS) }
        var den = Den(purse = Purse(30_000)).placeBet(red, 1_000, Room.HIGH_ROLLERS).placeBet(red, 5)
        assertEquals(1_000, den.stakeOn(red, Room.HIGH_ROLLERS))
        assertEquals(5, den.stakeOn(red))
        assertEquals(Purse(28_995), den.purse)

        den = den.clearBets(Room.HIGH_ROLLERS)
        assertEquals(Purse(29_995), den.purse)
        assertEquals(5, den.stakeOn(red), "the floor's bets stay down")

        den = den.placeBet(red, 2_000, Room.HIGH_ROLLERS).spin(7, Room.HIGH_ROLLERS)
        assertEquals(1, den.roulette(Room.HIGH_ROLLERS).spins)
        assertEquals(0, den.roulette.spins)
        assertTrue(den.canRebet(Room.HIGH_ROLLERS) || den.purse.total < 2_000)
        assertEquals(den, Den.fromJson(den.toJson()))
    }

    @Test
    fun theHighRollersPokerTablesPlayForBiggerBlindsAgainstBiggerStacks() {
        var den = Den(purse = Purse(30_000)).sitDown(PokerGame.HOLDEM, 3, Room.HIGH_ROLLERS)
        assertNull(den.table(PokerGame.HOLDEM))
        val table = assertNotNull(den.table(PokerGame.HOLDEM, Room.HIGH_ROLLERS))
        assertTrue(table.seats.drop(1).all { it.stack >= 25_000 })

        den = den.dealHand(PokerGame.HOLDEM, 5, Room.HIGH_ROLLERS)
        val hand = assertNotNull(den.table(PokerGame.HOLDEM, Room.HIGH_ROLLERS)?.hand)
        assertEquals(listOf(500L, 1_000L), hand.bets.filter { it > 0 }.sorted())
        assertEquals(2_000, hand.leastRaise)
        while (den.botsTurn(PokerGame.HOLDEM, Room.HIGH_ROLLERS)) den = den.playBot(PokerGame.HOLDEM, Room.HIGH_ROLLERS)
        if (den.playersTurn(PokerGame.HOLDEM, Room.HIGH_ROLLERS)) {
            den =
                den.playPoker(PokerGame.HOLDEM, PokerMove.Fold, Room.HIGH_ROLLERS)
        }
        den = den.leaveTable(PokerGame.HOLDEM, Room.HIGH_ROLLERS)
        assertFalse(assertNotNull(den.table(PokerGame.HOLDEM, Room.HIGH_ROLLERS)).playing)
        assertTrue(den.purse.total >= 29_000)

        assertFailsWith<IllegalArgumentException>("less than a least bet") {
            Den(
                purse = Purse(999),
            ).sitDown(PokerGame.DRAW, 1, Room.HIGH_ROLLERS).dealHand(PokerGame.DRAW, 2, Room.HIGH_ROLLERS)
        }
    }
}
