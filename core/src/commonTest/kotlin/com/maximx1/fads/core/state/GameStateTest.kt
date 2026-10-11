package com.maximx1.fads.core.state

import com.maximx1.fads.core.cards
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameStateTest {
    @Test
    fun needsEightColumnsAndFourFreeCells() {
        assertFailsWith<IllegalArgumentException> { GameState(columns = List(7) { emptyList() }) }
        assertFailsWith<IllegalArgumentException> {
            GameState(columns = List(8) { emptyList() }, freeCells = List(5) { null })
        }
    }

    @Test
    fun foundationCountsMustBeBetweenZeroAndThirteen() {
        assertFailsWith<IllegalArgumentException> { position(foundations = mapOf(Suit.SPADES to 14)) }
        assertFailsWith<IllegalArgumentException> { position(foundations = mapOf(Suit.SPADES to -1)) }
    }

    @Test
    fun foundationTopFollowsTheCount() {
        val state = position(foundations = mapOf(Suit.HEARTS to 3, Suit.CLUBS to 13))
        assertNull(state.foundationTop(Suit.SPADES))
        assertEquals(Card.fromCode("3H"), state.foundationTop(Suit.HEARTS))
        assertEquals(Card.fromCode("KC"), state.foundationTop(Suit.CLUBS))
        assertEquals(0, state.foundationCount(Suit.DIAMONDS))
    }

    @Test
    fun wonOnlyWhenEveryFoundationIsFull() {
        assertTrue(position(foundations = Suit.entries.associateWith { 13 }).isWon)
        val oneCardLeft = Suit.entries.associateWith { if (it == Suit.SPADES) 12 else 13 }
        assertFalse(position("KS", foundations = oneCardLeft).isWon)
        assertFalse(position().isWon)
    }

    @Test
    fun allCardsCountsColumnsFreeCellsAndFoundations() {
        val state = position("KS QH", "5D", free = "- 9C", foundations = mapOf(Suit.HEARTS to 2))
        assertEquals(cards("KS QH 5D 9C AH 2H").toSet(), state.allCards().toSet())
        assertEquals(6, state.allCards().size)
    }
}
