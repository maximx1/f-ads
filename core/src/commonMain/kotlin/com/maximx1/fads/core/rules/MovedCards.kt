package com.maximx1.fads.core.rules

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.state.GameState

/**
 * The cards [move] picks up, lowest first: a whole run for a move between columns, otherwise one card. The move
 * must be legal here.
 */
fun GameState.cardsMovedBy(move: Move): List<Card> =
    when (move) {
        is Move.ColumnToColumn -> columns[move.from].takeLast(move.count)
        is Move.ColumnToFreeCell -> columns[move.from].takeLast(1)
        is Move.ColumnToFoundation -> columns[move.from].takeLast(1)
        is Move.FreeCellToColumn -> listOfNotNull(freeCells[move.cell])
        is Move.FreeCellToFoundation -> listOfNotNull(freeCells[move.cell])
        is Move.FoundationToColumn -> listOfNotNull(foundationTop(move.suit))
        is Move.FoundationToFreeCell -> listOfNotNull(foundationTop(move.suit))
    }

/**
 * The cards in the columns and free cells that no legal move would move right now. A card in a column is stuck
 * when the cards on top of it are not a run, or when neither it nor any card under it can lead a run to a legal
 * place. A card in the middle of a run that can move as a whole is not stuck: it goes with the run. Cards on the
 * foundations are never counted.
 */
fun GameState.stuckCards(): Set<Card> {
    val movable = legalMoves().flatMapTo(HashSet()) { cardsMovedBy(it) }
    return (columns.flatten() + freeCells.filterNotNull()).filterNotTo(HashSet()) { it in movable }
}
