package com.maximx1.fads.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SuitTest {
    @Test
    fun heartsAndDiamondsAreRed() {
        assertEquals(SuitColor.RED, Suit.HEARTS.color)
        assertEquals(SuitColor.RED, Suit.DIAMONDS.color)
    }

    @Test
    fun spadesAndClubsAreBlack() {
        assertEquals(SuitColor.BLACK, Suit.SPADES.color)
        assertEquals(SuitColor.BLACK, Suit.CLUBS.color)
    }

    @Test
    fun everySuitHasItsOwnSymbol() {
        assertEquals("♠♥♦♣", Suit.entries.map { it.symbol }.joinToString(""))
    }
}
