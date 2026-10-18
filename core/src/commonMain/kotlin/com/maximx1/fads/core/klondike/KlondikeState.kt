package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank
import com.maximx1.fads.core.model.Suit

/**
 * One immutable position of a Klondike game.
 *
 * @property piles the seven tableau piles, bottom card first. In each, the first [hidden] cards lie face down.
 * @property hidden how many cards at the bottom of each pile lie face down. A pile with cards always shows at
 * least its top one: uncovering a face-down card turns it over.
 * @property stock the cards still to be drawn, face down. The last is the top.
 * @property waste the cards drawn, face up. The last is the top, and the only one that can be played.
 * @property foundations how many cards of each suit have been played up, 0 to 13.
 * @property draw how many cards each draw turns over: 1 or 3.
 */
data class KlondikeState(
    val piles: List<List<Card>>,
    val hidden: List<Int>,
    val stock: List<Card>,
    val waste: List<Card> = emptyList(),
    val foundations: Map<Suit, Int> = emptyMap(),
    val draw: Int = 1,
) {
    init {
        require(piles.size == PILE_COUNT) { "Expected $PILE_COUNT piles, got ${piles.size}" }
        require(hidden.size == PILE_COUNT) { "Expected $PILE_COUNT hidden counts, got ${hidden.size}" }
        require(piles.indices.all { hidden[it] in 0..maxOf(0, piles[it].size - 1) }) { "Bad hidden counts $hidden" }
        require(draw == 1 || draw == DRAW_THREE) { "Draw 1 or 3, not $draw" }
    }

    val isWon: Boolean get() = Suit.entries.all { foundationCount(it) == Rank.entries.size }

    fun foundationCount(suit: Suit): Int = foundations[suit] ?: 0

    /** The top card of a suit's foundation, or `null` if nothing has been played to it yet. */
    fun foundationTop(suit: Suit): Card? = Rank.entries.getOrNull(foundationCount(suit) - 1)?.let { Card(it, suit) }

    /** The face-up cards of pile [index], bottom first. */
    fun showing(index: Int): List<Card> = piles[index].drop(hidden[index])

    companion object {
        const val PILE_COUNT = 7
        const val DRAW_THREE = 3
    }
}
