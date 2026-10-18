package com.maximx1.fads.core.spider

import com.maximx1.fads.core.model.Card

/**
 * One card of Spider's two decks' worth. [id] tells it apart from the other copies of the same card: it is the
 * card's place in [spiderDeck].
 */
data class Piece(
    val id: Int,
    val card: Card,
)

/**
 * One immutable position of a Spider game.
 *
 * @property piles the ten piles, bottom card first. In each, the first [hidden] cards lie face down.
 * @property hidden how many cards at the bottom of each pile lie face down. A pile with cards always shows at
 * least its top one.
 * @property stock the cards still to be dealt, ten at a time, face down. The last is the top.
 * @property finished the runs from king down to ace of one suit that have left the table, in the order they did,
 * each king first.
 * @property suits how many suits the game is played with: 1, 2 or 4.
 */
data class SpiderState(
    val piles: List<List<Piece>>,
    val hidden: List<Int>,
    val stock: List<Piece>,
    val finished: List<List<Piece>> = emptyList(),
    val suits: Int = 1,
) {
    init {
        require(piles.size == PILE_COUNT) { "Expected $PILE_COUNT piles, got ${piles.size}" }
        require(hidden.size == PILE_COUNT) { "Expected $PILE_COUNT hidden counts, got ${hidden.size}" }
        require(piles.indices.all { hidden[it] in 0..maxOf(0, piles[it].size - 1) }) { "Bad hidden counts $hidden" }
        require(suits in SUIT_CHOICES) { "Spider is played with 1, 2 or 4 suits, not $suits" }
    }

    val isWon: Boolean get() = finished.size == RUNS

    /** The face-up cards of pile [index], bottom first. */
    fun showing(index: Int): List<Piece> = piles[index].drop(hidden[index])

    companion object {
        const val PILE_COUNT = 10

        /** Runs from king to ace in the whole game, which is how many must leave the table to win. */
        const val RUNS = 8
        val SUIT_CHOICES = listOf(1, 2, 4)
    }
}
