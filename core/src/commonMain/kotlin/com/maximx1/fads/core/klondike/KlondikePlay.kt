package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.klondike.KlondikeState.Companion.PILE_COUNT
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.canStackOn
import com.maximx1.fads.core.rules.isRun

/**
 * Klondike deal [number]: the deck shuffled for the number (see [shuffledFor]), dealt in rows from the left, each
 * row starting one pile further right, so pile `i` gets `i + 1` cards with only its top one face up. The other
 * 24 cards are the stock, the last of them on top.
 */
fun klondikeDeal(
    number: Long,
    draw: Int,
): KlondikeState {
    val cards = Card.deck.shuffledFor(number)
    val piles = List(PILE_COUNT) { mutableListOf<Card>() }
    var next = 0
    for (row in 0 until PILE_COUNT) {
        for (pile in row until PILE_COUNT) piles[pile] += cards[next++]
    }
    return KlondikeState(piles, hidden = List(PILE_COUNT) { it }, stock = cards.drop(next), draw = draw)
}

/** Foundation rule: same suit, ascending, ace first. */
fun KlondikeState.canPlayUp(card: Card): Boolean = foundationCount(card.suit) == card.rank.value - 1

/** A card can go on the opposite colour one rank higher, or a king alone onto an empty pile. */
fun KlondikeState.canLand(
    pile: Int,
    card: Card,
): Boolean {
    val cards = piles.getOrNull(pile) ?: return false
    return if (cards.isEmpty()) card.rank == Rank.KING else card.canStackOn(cards.last())
}

fun KlondikeState.canMove(move: KlondikeMove): Boolean =
    when (move) {
        KlondikeMove.Draw -> stock.isNotEmpty()
        KlondikeMove.Recycle -> stock.isEmpty() && waste.isNotEmpty()
        KlondikeMove.WasteToFoundation -> waste.lastOrNull()?.let { canPlayUp(it) } == true
        is KlondikeMove.WasteToPile -> waste.lastOrNull()?.let { canLand(move.to, it) } == true
        is KlondikeMove.PileToFoundation -> topOf(move.from)?.let { canPlayUp(it) } == true
        is KlondikeMove.PileToPile -> canMoveRun(move)
        is KlondikeMove.FoundationToPile -> foundationTop(move.suit)?.let { canLand(move.to, it) } == true
    }

/** Returns the position after [move]. Throws [IllegalArgumentException] if the move is not legal here. */
fun KlondikeState.play(move: KlondikeMove): KlondikeState {
    require(canMove(move)) { "Illegal move: $move" }
    return when (move) {
        KlondikeMove.Draw -> {
            // The cards turn over as one packet, so the top of the stock ends up lowest of them on the waste.
            val turned = stock.takeLast(minOf(draw, stock.size))
            copy(stock = stock.dropLast(turned.size), waste = waste + turned.reversed())
        }

        KlondikeMove.Recycle -> {
            copy(stock = waste.reversed(), waste = emptyList())
        }

        KlondikeMove.WasteToFoundation -> {
            withOneMoreOn(waste.last().suit).copy(waste = waste.dropLast(1))
        }

        is KlondikeMove.WasteToPile -> {
            withPile(move.to, piles[move.to] + waste.last()).copy(waste = waste.dropLast(1))
        }

        is KlondikeMove.PileToFoundation -> {
            withOneMoreOn(piles[move.from].last().suit).withPile(move.from, piles[move.from].dropLast(1))
        }

        is KlondikeMove.PileToPile -> {
            val run = piles[move.from].takeLast(move.count)
            withPile(move.from, piles[move.from].dropLast(move.count)).withPile(move.to, piles[move.to] + run)
        }

        is KlondikeMove.FoundationToPile -> {
            val card = checkNotNull(foundationTop(move.suit))
            withPile(
                move.to,
                piles[move.to] + card,
            ).copy(foundations = foundations + (move.suit to card.rank.value - 1))
        }
    }
}

private fun KlondikeState.canMoveRun(move: KlondikeMove.PileToPile): Boolean {
    val showing = if (move.from in piles.indices) showing(move.from) else return false
    val fits = move.from != move.to && move.count in 1..showing.size
    return fits && showing.takeLast(move.count).let { run -> run.isRun() && canLand(move.to, run.first()) }
}

/** This position with pile [index] holding [cards], its top card turned face up if it was face down. */
private fun KlondikeState.withPile(
    index: Int,
    cards: List<Card>,
): KlondikeState =
    copy(
        piles = piles.mapIndexed { i, pile -> if (i == index) cards else pile },
        hidden = hidden.mapIndexed { i, count -> if (i == index) minOf(count, maxOf(0, cards.size - 1)) else count },
    )

private fun KlondikeState.withOneMoreOn(suit: Suit): KlondikeState =
    copy(foundations = foundations + (suit to foundationCount(suit) + 1))
