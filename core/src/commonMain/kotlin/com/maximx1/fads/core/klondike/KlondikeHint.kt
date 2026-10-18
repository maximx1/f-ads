package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.model.Rank

/**
 * A move worth trying, or `null` if there is none to suggest. A rule of thumb, not a solver: cards to the
 * foundations first, then whatever turns a face-down card over, then cards off the waste, and the stock only when
 * nothing on the table is worth doing. It never suggests taking a card off a foundation, a move that only shuffles
 * cards between piles, or one that puts the cards back as they were in [before].
 */
fun KlondikeState.hint(before: KlondikeState? = null): KlondikeMove? =
    legalMoves()
        .mapNotNull { move -> worth(move).takeIf { it > 0 }?.let { move to it } }
        .filter { (move, _) -> play(move) != before }
        .maxByOrNull { (_, worth) -> worth }
        ?.first

private fun KlondikeState.worth(move: KlondikeMove): Int =
    when (move) {
        KlondikeMove.WasteToFoundation -> TO_FOUNDATION
        is KlondikeMove.PileToFoundation -> TO_FOUNDATION + if (turnsOver(move.from, 1)) TURNS_OVER else 0
        is KlondikeMove.PileToPile -> worthOf(move)
        is KlondikeMove.WasteToPile -> FROM_WASTE
        KlondikeMove.Draw -> DRAW
        KlondikeMove.Recycle -> RECYCLE
        is KlondikeMove.FoundationToPile -> 0
    }

private fun KlondikeState.worthOf(move: KlondikeMove.PileToPile): Int {
    val left = piles[move.from].dropLast(move.count)
    return when {
        turnsOver(move.from, move.count) -> TURNS_OVER

        // Moving a whole pile off is worth it only to free the space for a king that is waiting.
        left.isEmpty() && piles[move.to].isNotEmpty() && kingWaiting() -> EMPTIES_PILE

        // Splitting a run is worth it only for the card it uncovers, if that can go up.
        left.isNotEmpty() && canPlayUp(left.last()) -> UNCOVERS_FOUNDATION_CARD

        else -> 0
    }
}

/** True if taking [count] cards off pile [pile] leaves a face-down card on top, to be turned over. */
private fun KlondikeState.turnsOver(
    pile: Int,
    count: Int,
): Boolean = hidden[pile] > 0 && count == showing(pile).size

/** True if a king somewhere other than the bottom of a pile could use an empty pile. */
private fun KlondikeState.kingWaiting(): Boolean =
    waste.lastOrNull()?.rank == Rank.KING ||
        piles.indices.any { pile ->
            val first = showing(pile).firstOrNull()
            first?.rank == Rank.KING && hidden[pile] > 0
        }

private const val TO_FOUNDATION = 1000
private const val TURNS_OVER = 900
private const val FROM_WASTE = 600
private const val UNCOVERS_FOUNDATION_CARD = 400
private const val EMPTIES_PILE = 300
private const val DRAW = 100
private const val RECYCLE = 50
