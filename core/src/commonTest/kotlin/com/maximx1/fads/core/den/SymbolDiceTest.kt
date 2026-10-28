package com.maximx1.fads.core.den

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SymbolDiceTest {
    @Test
    fun aFacePaysEvensForEachDieShowingItAndTheHouseKeepsAboutEightInAHundred() {
        assertEquals(0, SymbolDice.returns(4, listOf(1, 2, 3)))
        assertEquals(2, SymbolDice.returns(4, listOf(4, 2, 3)))
        assertEquals(3, SymbolDice.returns(4, listOf(4, 4, 3)))
        assertEquals(4, SymbolDice.returns(4, listOf(4, 4, 4)))
        val everyThrow = (1..6).flatMap { a -> (1..6).flatMap { b -> (1..6).map { c -> listOf(a, b, c) } } }
        val back = everyThrow.sumOf { SymbolDice.returns(1, it) }.toDouble() / everyThrow.size
        assertTrue(back in 0.9..0.93, "pays back $back")
    }

    @Test
    fun eachGameKeepsItsOwnTableAndAThrowPaysEveryFaceBet() {
        val game = SymbolDiceGame.CROWN_AND_ANCHOR
        val den =
            Den(purse = Purse(100))
                .betSymbolDice(game, 1, 10)
                .betSymbolDice(game, 3, 5)
                .betSymbolDice(SymbolDiceGame.CHUCK_A_LUCK, 2, 1)
        assertEquals(Purse(84), den.purse)
        val rolled = den.rollSymbolDiceAs(game, listOf(1, 1, 5))
        assertEquals(30, rolled.symbolDice(game).paid)
        assertEquals(Purse(84 + 30), rolled.purse)
        assertTrue(rolled.symbolDice(game).bets.isEmpty())
        assertEquals(mapOf(2 to 1L), rolled.symbolDice(SymbolDiceGame.CHUCK_A_LUCK).bets, "the other game's bets stay")
        assertEquals(rolled, Den.fromJson(rolled.toJson()))
        assertEquals(Purse(100), Den(purse = Purse(100)).betSymbolDice(game, 6, 9).clearSymbolDice(game).purse)
        assertEquals(
            1,
            den.rollSymbolDice(SymbolDiceGame.CHUCK_A_LUCK, 3).symbolDice(SymbolDiceGame.CHUCK_A_LUCK).throws,
        )
        assertFailsWith<IllegalArgumentException> { Den(purse = Purse(9)).betSymbolDice(game, 7, 1) }
        assertFailsWith<IllegalArgumentException> { Den().rollSymbolDice(game, 1) }
        assertFailsWith<IllegalArgumentException> {
            Den(
                purse = Purse(5_000),
            ).betSymbolDice(game, 1, 999, Room.HIGH_ROLLERS)
        }
    }
}
