package com.maximx1.fads.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardTest {
    @Test
    fun ranksRunFromAceAtOneToKingAtThirteen() {
        assertEquals((1..13).toList(), Rank.entries.map { it.value })
        assertEquals("A 2 3 4 5 6 7 8 9 10 J Q K", Rank.entries.joinToString(" ") { it.label })
    }

    @Test
    fun deckHasAllFiftyTwoCardsInItsFixedOrder() {
        assertEquals(52, Card.deck.toSet().size)
        assertEquals("AS 2S 3S", Card.deck.take(3).joinToString(" ") { it.code })
        assertEquals("KS AH", Card.deck.slice(12..13).joinToString(" ") { it.code })
        assertEquals("KH AD", Card.deck.slice(25..26).joinToString(" ") { it.code })
        assertEquals("KD AC", Card.deck.slice(38..39).joinToString(" ") { it.code })
        assertEquals("KC", Card.deck.last().code)
    }

    @Test
    fun cardTakesItsColourFromItsSuit() {
        assertEquals(SuitColor.RED, Card(Rank.TEN, Suit.DIAMONDS).color)
        assertEquals(SuitColor.BLACK, Card(Rank.TEN, Suit.CLUBS).color)
    }

    @Test
    fun codeAndDisplayText() {
        val tenOfHearts = Card(Rank.TEN, Suit.HEARTS)
        assertEquals("10H", tenOfHearts.code)
        assertEquals("10♥", tenOfHearts.toString())
        assertEquals("AS", Card(Rank.ACE, Suit.SPADES).code)
    }

    @Test
    fun everyCardRoundTripsThroughItsCode() {
        for (card in Card.deck) {
            assertEquals(card, Card.fromCode(card.code))
        }
    }

    @Test
    fun badCodesAreRejected() {
        for (code in listOf("", "1S", "AX", "as", "11H", "A♠")) {
            assertFailsWith<IllegalArgumentException>(code) { Card.fromCode(code) }
        }
    }
}
