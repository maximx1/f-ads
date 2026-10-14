package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.deal.SplitMix64
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.canMove
import com.maximx1.fads.core.rules.legalMoves
import com.maximx1.fads.core.state.GameState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Plays random legal moves on many deals and checks nothing ever goes wrong with the cards. */
class RandomPlayTest {
    private val deck = Card.deck.sortedBy { it.code }

    private fun assertSound(
        state: GameState,
        context: String,
    ) {
        assertEquals(deck, state.allCards().sortedBy { it.code }, "cards lost or duplicated: $context")
        for (suit in state.foundations.keys) {
            assertTrue(state.foundationCount(suit) in 0..13, context)
        }
    }

    @Test
    fun randomLegalPlayNeverLosesOrDuplicatesACard() {
        for (deal in 0L until 200L) {
            val random = SplitMix64(deal xor 0x5EED)
            var state = Deal.fromNumber(deal)
            var played = 0
            while (played < 250) {
                val moves = state.legalMoves()
                if (moves.isEmpty()) break
                val move = moves[random.nextBelow(moves.size)]
                state = state.play(move)
                played++
                assertSound(state, "deal $deal after $played moves, last $move")
            }
        }
    }

    @Test
    fun everyMoveOutsideTheLegalListIsRefused() {
        val indexes = -1..8
        val candidates =
            buildList {
                for (a in indexes) {
                    add(Move.ColumnToFoundation(a))
                    add(Move.FreeCellToFoundation(a))
                    for (suit in Suit.entries) {
                        add(Move.FoundationToColumn(suit, a))
                        add(Move.FoundationToFreeCell(suit, a))
                    }
                    for (b in indexes) {
                        add(Move.ColumnToFreeCell(a, b))
                        add(Move.FreeCellToColumn(a, b))
                        (0..8).mapTo(this) { Move.ColumnToColumn(a, b, it) }
                    }
                }
            }
        for (deal in 0L until 20L) {
            val random = SplitMix64(deal)
            var state = Deal.fromNumber(deal)
            repeat(30) {
                val legal = state.legalMoves().toSet()
                for (move in candidates) {
                    assertEquals(move in legal, state.canMove(move), "deal $deal: $move")
                    if (move !in legal) assertFailsWith<IllegalArgumentException> { state.play(move) }
                }
                if (legal.isNotEmpty()) state = state.play(legal.elementAt(random.nextBelow(legal.size)))
            }
        }
    }
}
