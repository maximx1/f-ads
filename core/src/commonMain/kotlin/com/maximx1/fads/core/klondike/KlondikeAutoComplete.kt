package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.klondike.KlondikeState.Companion.PILE_COUNT

/** True once the stock and waste are gone and every card is face up, when the rest can only play out to a win. */
val KlondikeState.canAutoComplete: Boolean
    get() = !isWon && stock.isEmpty() && waste.isEmpty() && hidden.all { it == 0 }

/** The next move of an automatic finish: the lowest card that can go up, or `null` if none can. */
fun KlondikeState.nextAutoMove(): KlondikeMove? {
    val candidates =
        (0 until PILE_COUNT).mapNotNull { pile ->
            topOf(pile)?.takeIf { canPlayUp(it) }?.let { it to KlondikeMove.PileToFoundation(pile) }
        } + listOfNotNull(waste.lastOrNull()?.takeIf { canPlayUp(it) }?.let { it to KlondikeMove.WasteToFoundation })
    return candidates.minByOrNull { (card, _) -> card.rank }?.second
}
