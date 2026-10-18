package com.maximx1.fads.core.spider

import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.spider.SpiderState.Companion.PILE_COUNT

/**
 * The 104 cards a game with [suits] suits is played with, in a fixed order that numbered deals shuffle: as many
 * copies as it takes of spades ace to king, then (with two suits) hearts, then (with four) diamonds and clubs.
 */
fun spiderDeck(suits: Int): List<Piece> {
    val used = Suit.entries.take(suits)
    val copies = SpiderState.RUNS / suits
    return List(copies) { used }
        .flatten()
        .flatMap { suit -> Rank.entries.map { Card(it, suit) } }
        .mapIndexed { id, card -> Piece(id, card) }
}

/**
 * Spider deal [number]: the deck shuffled for the number (see [shuffledFor]) and dealt a card to a pile at a time,
 * left to right, 54 cards in all, so the first four piles get six and the rest five, only the top one face up. The
 * other 50 are the stock, five rows of ten.
 */
fun spiderDeal(
    number: Long,
    suits: Int,
): SpiderState {
    val cards = spiderDeck(suits).shuffledFor(number)
    val piles = List(PILE_COUNT) { mutableListOf<Piece>() }
    repeat(DEALT) { piles[it % PILE_COUNT] += cards[it] }
    return SpiderState(piles, hidden = piles.map { it.size - 1 }, stock = cards.drop(DEALT), suits = suits)
}

/** True if the cards, listed from the bottom up, are all one suit and each one rank lower than the one before. */
fun List<Piece>.isSuitRun(): Boolean =
    zipWithNext().all { (below, above) ->
        above.card.suit == below.card.suit && above.card.rank.value + 1 == below.card.rank.value
    }

fun SpiderState.canMove(move: SpiderMove): Boolean =
    when (move) {
        SpiderMove.DealRow -> stock.size >= PILE_COUNT && piles.none { it.isEmpty() }
        is SpiderMove.PileToPile -> canMoveRun(move)
    }

/**
 * Returns the position after [move], with any king-to-ace run of one suit it leaves on top of a pile taken off
 * the table. Throws [IllegalArgumentException] if the move is not legal here.
 */
fun SpiderState.play(move: SpiderMove): SpiderState {
    require(canMove(move)) { "Illegal move: $move" }
    return when (move) {
        SpiderMove.DealRow -> {
            val row = stock.takeLast(PILE_COUNT).reversed()
            val dealt = copy(piles = piles.mapIndexed { i, pile -> pile + row[i] }, stock = stock.dropLast(PILE_COUNT))
            piles.indices.fold(dealt) { state, pile -> state.withoutFinishedRun(pile) }
        }

        is SpiderMove.PileToPile -> {
            val run = piles[move.from].takeLast(move.count)
            withPile(move.from, piles[move.from].dropLast(move.count))
                .withPile(move.to, piles[move.to] + run)
                .withoutFinishedRun(move.to)
        }
    }
}

private fun SpiderState.canMoveRun(move: SpiderMove.PileToPile): Boolean {
    if (move.from !in piles.indices || move.to !in piles.indices || move.from == move.to) return false
    val showing = showing(move.from)
    val run = showing.takeLast(move.count.coerceIn(0, showing.size))
    val top = piles[move.to].lastOrNull()?.card
    return move.count in 1..showing.size &&
        run.isSuitRun() &&
        (
            top == null || top.rank.value == run
                .first()
                .card.rank.value + 1
        )
}

/** This position with pile [index] holding [cards], its top card turned face up if it was face down. */
private fun SpiderState.withPile(
    index: Int,
    cards: List<Piece>,
): SpiderState =
    copy(
        piles = piles.mapIndexed { i, pile -> if (i == index) cards else pile },
        hidden = hidden.mapIndexed { i, count -> if (i == index) minOf(count, maxOf(0, cards.size - 1)) else count },
    )

/** This position with a king-to-ace run of one suit on top of pile [index], if there is one, taken off the table. */
private fun SpiderState.withoutFinishedRun(index: Int): SpiderState {
    val top = showing(index).takeLast(Rank.entries.size)
    val whole = top.size == Rank.entries.size && top.first().card.rank == Rank.KING && top.isSuitRun()
    return if (whole) withPile(index, piles[index].dropLast(top.size)).copy(finished = finished + listOf(top)) else this
}

private const val DEALT = 54
