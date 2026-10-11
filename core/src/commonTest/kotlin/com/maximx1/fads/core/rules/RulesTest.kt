package com.maximx1.fads.core.rules

import com.maximx1.fads.core.cards
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {
    private fun stacks(
        moving: String,
        below: String,
    ) = Card.fromCode(moving).canStackOn(Card.fromCode(below))

    @Test
    fun oppositeColourOneRankLowerStacks() {
        assertTrue(stacks("6S", "7H"))
        assertTrue(stacks("6S", "7D"))
        assertTrue(stacks("6C", "7H"))
        assertTrue(stacks("6C", "7D"))
        assertTrue(stacks("6H", "7S"))
        assertTrue(stacks("6H", "7C"))
        assertTrue(stacks("6D", "7S"))
        assertTrue(stacks("6D", "7C"))
    }

    @Test
    fun sameColourNeverStacks() {
        assertFalse(stacks("6S", "7S"))
        assertFalse(stacks("6S", "7C"))
        assertFalse(stacks("6C", "7S"))
        assertFalse(stacks("6H", "7H"))
        assertFalse(stacks("6H", "7D"))
        assertFalse(stacks("6D", "7H"))
    }

    @Test
    fun rankMustBeExactlyOneLower() {
        assertFalse(stacks("7S", "7H"))
        assertFalse(stacks("8S", "7H"))
        assertFalse(stacks("5S", "7H"))
        assertTrue(stacks("AS", "2H"))
        assertTrue(stacks("QS", "KH"))
    }

    @Test
    fun kingsDoNotWrapOntoAces() {
        assertFalse(stacks("KS", "AH"))
        assertFalse(stacks("AS", "KH"))
    }

    @Test
    fun stackingAgreesWithTheRuleForEveryPairOfCards() {
        val legalPairs = Card.deck.sumOf { moving -> Card.deck.count { below -> moving.canStackOn(below) } }
        // Each of the 48 non-king cards fits on the two next-higher cards of the other colour.
        assertEquals(96, legalPairs)
    }

    @Test
    fun runsAreCheckedBottomUp() {
        assertTrue(cards("").isRun())
        assertTrue(cards("9C").isRun())
        assertTrue(cards("9C 8H 7S 6D").isRun())
        assertFalse(cards("9C 8H 7H 6S").isRun())
        assertFalse(cards("6D 7S 8H").isRun())
    }

    @Test
    fun foundationTakesTheAceThenTheNextOfTheSameSuit() {
        val empty = position()
        assertTrue(empty.canPlayToFoundation(Card.fromCode("AS")))
        assertFalse(empty.canPlayToFoundation(Card.fromCode("2S")))

        val twoSpades = position(foundations = mapOf(Suit.SPADES to 2))
        assertTrue(twoSpades.canPlayToFoundation(Card.fromCode("3S")))
        assertFalse(twoSpades.canPlayToFoundation(Card.fromCode("4S")))
        assertFalse(twoSpades.canPlayToFoundation(Card.fromCode("2S")))
        assertFalse(twoSpades.canPlayToFoundation(Card.fromCode("3H")))
        assertTrue(twoSpades.canPlayToFoundation(Card.fromCode("AH")))
    }

    @Test
    fun maxRunIsFreeCellsPlusOneDoubledPerEmptyColumn() {
        val full = Array(8) { "KS" }
        assertEquals(5, position(*full).maxMovableRun(1))
        assertEquals(4, position(*full, free = "2C").maxMovableRun(1))
        assertEquals(1, position(*full, free = "2C 3C 4C 5C").maxMovableRun(1))

        val oneEmpty = arrayOf("KS", "KS", "KS", "KS", "KS", "KS", "KS", "")
        assertEquals(10, position(*oneEmpty).maxMovableRun(1))
        assertEquals(2, position(*oneEmpty, free = "2C 3C 4C 5C").maxMovableRun(1))

        val twoEmpty = arrayOf("KS", "KS", "KS", "KS", "KS", "KS", "", "")
        assertEquals(20, position(*twoEmpty).maxMovableRun(1))
        assertEquals(12, position(*twoEmpty, free = "2C - 4C").maxMovableRun(1))
    }

    @Test
    fun anEmptyDestinationDoesNotCountTowardsTheMaxRun() {
        val twoEmpty = arrayOf("KS", "KS", "KS", "KS", "KS", "KS", "", "")
        assertEquals(10, position(*twoEmpty).maxMovableRun(7))
        assertEquals(10, position(*twoEmpty).maxMovableRun(6))
        assertEquals(20, position(*twoEmpty).maxMovableRun(0))
    }
}
