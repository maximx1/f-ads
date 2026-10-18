package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.klondike.KlondikeState.Companion.PILE_COUNT
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit

/** Every legal move in this position. */
fun KlondikeState.legalMoves(): List<KlondikeMove> =
    buildList {
        add(KlondikeMove.Draw)
        add(KlondikeMove.Recycle)
        add(KlondikeMove.WasteToFoundation)
        for (to in 0 until PILE_COUNT) add(KlondikeMove.WasteToPile(to))
        for (from in 0 until PILE_COUNT) {
            add(KlondikeMove.PileToFoundation(from))
            for (to in 0 until PILE_COUNT) {
                for (count in 1..showing(from).size) add(KlondikeMove.PileToPile(from, to, count))
            }
        }
        for (suit in Suit.entries) {
            for (to in 0 until PILE_COUNT) add(KlondikeMove.FoundationToPile(suit, to))
        }
    }.filter { canMove(it) }

/** The cards [move] picks up, lowest first. Empty for a draw or a recycle. The move must be legal here. */
fun KlondikeState.cardsMovedBy(move: KlondikeMove): List<Card> =
    when (move) {
        KlondikeMove.Draw, KlondikeMove.Recycle -> emptyList()
        KlondikeMove.WasteToFoundation, is KlondikeMove.WasteToPile -> waste.takeLast(1)
        is KlondikeMove.PileToFoundation -> piles[move.from].takeLast(1)
        is KlondikeMove.PileToPile -> piles[move.from].takeLast(move.count)
        is KlondikeMove.FoundationToPile -> listOfNotNull(foundationTop(move.suit))
    }

/** The top card of pile [pile], or `null` if it is empty or there is no such pile. */
internal fun KlondikeState.topOf(pile: Int): Card? = piles.getOrNull(pile)?.lastOrNull()
