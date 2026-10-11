package com.maximx1.fads.core.state

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank
import com.maximx1.fads.core.model.Suit
import kotlinx.serialization.Serializable

/**
 * One immutable position of a FreeCell game.
 *
 * @property columns the eight tableau columns. In each column the last card is the exposed one.
 * @property freeCells the four free cells, `null` where a cell is empty.
 * @property foundations how many cards of each suit have been played to its foundation, 0 to 13.
 */
@Serializable
data class GameState(
    val columns: List<List<Card>>,
    val freeCells: List<Card?> = List(FREE_CELL_COUNT) { null },
    val foundations: Map<Suit, Int> = emptyMap(),
) {
    init {
        require(columns.size == COLUMN_COUNT) { "Expected $COLUMN_COUNT columns, got ${columns.size}" }
        require(freeCells.size == FREE_CELL_COUNT) { "Expected $FREE_CELL_COUNT free cells, got ${freeCells.size}" }
        require(foundations.values.all { it in 0..Rank.entries.size }) { "Bad foundation count in $foundations" }
    }

    val isWon: Boolean get() = Suit.entries.all { foundationCount(it) == Rank.entries.size }

    fun foundationCount(suit: Suit): Int = foundations[suit] ?: 0

    /** The top card of a suit's foundation, or `null` if nothing has been played to it yet. */
    fun foundationTop(suit: Suit): Card? = Rank.entries.getOrNull(foundationCount(suit) - 1)?.let { Card(it, suit) }

    /** Every card in the position, foundations included. A sound position has all 52, once each. */
    fun allCards(): List<Card> =
        columns.flatten() +
            freeCells.filterNotNull() +
            Suit.entries.flatMap { suit -> Rank.entries.take(foundationCount(suit)).map { Card(it, suit) } }

    companion object {
        const val COLUMN_COUNT = 8
        const val FREE_CELL_COUNT = 4
    }
}
