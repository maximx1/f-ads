package com.maximx1.fads.core.rules

import com.maximx1.fads.core.state.GameState

/**
 * True once the game can finish itself: it is not won yet, and no card in any column sits on one of lower rank.
 * From then on the lowest card left is always uncovered and is always the next card for its foundation, so
 * playing it, then the next lowest, and so on, puts every card up.
 */
val GameState.canAutoComplete: Boolean
    get() = !isWon && columns.all { column -> column.zipWithNext().all { (below, above) -> above.rank <= below.rank } }

/**
 * The next move of an automatic finish: the lowest uncovered card that can go to its foundation, from a free
 * cell or a column. `null` when no card can, which is always the case once the game is won.
 */
fun GameState.nextAutoMove(): Move? {
    val fromCells =
        freeCells.withIndex().mapNotNull { (cell, card) ->
            card?.takeIf { canPlayToFoundation(it) }?.let { it to Move.FreeCellToFoundation(cell) }
        }
    val fromColumns =
        columns.withIndex().mapNotNull { (column, cards) ->
            cards.lastOrNull()?.takeIf { canPlayToFoundation(it) }?.let { it to Move.ColumnToFoundation(column) }
        }
    return (fromCells + fromColumns).minByOrNull { (card, _) -> card.rank }?.second
}
