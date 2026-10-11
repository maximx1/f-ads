package com.maximx1.fads.core.deal

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.state.GameState

/**
 * Numbered deals: the same number gives the same layout, forever, on every platform.
 *
 * The algorithm, which the fixture tests lock in place:
 * 1. Start from [Card.deck].
 * 2. Seed [SplitMix64] with the deal number.
 * 3. Fisher-Yates shuffle: for `i` from 51 down to 1, swap card `i` with card `nextBelow(i + 1)`.
 * 4. Deal left to right, one card per column per round, so the first four columns get seven cards and the
 *    last four get six.
 */
object Deal {
    fun fromNumber(number: Long): GameState {
        require(number >= 0) { "Deal numbers start at 0, got $number" }
        val cards = Card.deck.toMutableList()
        val random = SplitMix64(number)
        for (i in cards.lastIndex downTo 1) {
            val j = random.nextBelow(i + 1)
            cards[i] = cards[j].also { cards[j] = cards[i] }
        }
        val columnCount = GameState.COLUMN_COUNT
        return GameState(columns = List(columnCount) { cards.slice(it..cards.lastIndex step columnCount) })
    }
}
