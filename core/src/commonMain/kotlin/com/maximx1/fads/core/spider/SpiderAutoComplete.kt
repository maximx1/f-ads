package com.maximx1.fads.core.spider

import com.maximx1.fads.core.spider.SpiderState.Companion.PILE_COUNT

/**
 * True once the game can only play out to a win: the stock is dealt, every card is face up, and each pile is one
 * run of one suit. Runs of a suit then always fit together, one onto another, into whole king-to-ace runs.
 */
val SpiderState.canAutoComplete: Boolean
    get() = !isWon && stock.isEmpty() && hidden.all { it == 0 } && piles.all { it.isSuitRun() }

/** The next move of an automatic finish: a whole pile onto the top card of its suit one rank higher. */
fun SpiderState.nextAutoMove(): SpiderMove? =
    if (!canAutoComplete) {
        null
    } else {
        (0 until PILE_COUNT).firstNotNullOfOrNull { from ->
            ontoItsSuit(from)?.let { SpiderMove.PileToPile(from, it, piles[from].size) }
        }
    }

/** The pile whose top card is of the same suit as pile [from]'s bottom card and one rank higher, if there is one. */
private fun SpiderState.ontoItsSuit(from: Int): Int? {
    val first = piles[from].firstOrNull()?.card ?: return null
    return (0 until PILE_COUNT).firstOrNull { to ->
        val top = piles[to].lastOrNull()?.card
        to != from && top != null && top.suit == first.suit && top.rank.value == first.rank.value + 1
    }
}
