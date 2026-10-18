package com.maximx1.fads.core.deal

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.state.GameState

/**
 * Numbered deals: the same number gives the same layout, forever, on every platform.
 *
 * The algorithm, which the fixture tests lock in place:
 * 1. Start from [Card.deck].
 * 2. Shuffle it for the deal number (see [shuffledFor]).
 * 3. Deal left to right, one card per column per round, so the first four columns get seven cards and the
 *    last four get six.
 */
object Deal {
    fun fromNumber(number: Long): GameState {
        val cards = Card.deck.shuffledFor(number)
        val columnCount = GameState.COLUMN_COUNT
        return GameState(columns = List(columnCount) { cards.slice(it..cards.lastIndex step columnCount) })
    }
}
