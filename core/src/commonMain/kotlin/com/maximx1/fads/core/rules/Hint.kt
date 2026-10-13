package com.maximx1.fads.core.rules

import com.maximx1.fads.core.engine.play
import com.maximx1.fads.core.state.GameState

/**
 * A move worth trying, or `null` if there is none to suggest. This is a rule of thumb, not a solver: it likes
 * moves to the foundations best, then moves that build on another card, and keeps free cells and empty columns
 * for when there is nothing better. It does not promise that the move leads to a win.
 *
 * It never suggests taking a card back off a foundation, moving a whole column to an empty one, or a move that
 * only puts the cards back as they were in [before], the position the last move was played from.
 */
fun GameState.hint(before: GameState? = null): Move? =
    legalMoves()
        .filter { worthSuggesting(it) && play(it) != before }
        .maxByOrNull { score(it) }

private fun GameState.worthSuggesting(move: Move): Boolean =
    when (move) {
        is Move.FoundationToColumn, is Move.FoundationToFreeCell -> false
        is Move.ColumnToColumn -> columns[move.to].isNotEmpty() || move.count < columns[move.from].size
        else -> true
    }

private fun GameState.score(move: Move): Int =
    when (move) {
        is Move.ColumnToFoundation, is Move.FreeCellToFoundation -> TO_FOUNDATION
        is Move.FreeCellToColumn -> if (columns[move.to].isEmpty()) CELL_TO_EMPTY_COLUMN else CELL_TO_CARD
        is Move.ColumnToColumn -> scoreRun(move) + move.count + whatIsUncovered(move.from, move.count)
        is Move.ColumnToFreeCell -> TO_FREE_CELL + whatIsUncovered(move.from, 1)
        is Move.FoundationToColumn, is Move.FoundationToFreeCell -> 0
    }

private fun GameState.scoreRun(move: Move.ColumnToColumn): Int {
    val source = columns[move.from]
    val left = source.dropLast(move.count)
    // Taking part of a run off the card it already sits on only shuffles cards about.
    val splitsARun = left.isNotEmpty() && source[left.size].canStackOn(left.last())
    return when {
        columns[move.to].isEmpty() -> RUN_TO_EMPTY_COLUMN
        splitsARun -> SPLIT_RUN
        else -> RUN_TO_CARD
    }
}

/** Extra for what taking [count] cards off [column] leaves behind: an empty column, or a card for a foundation. */
private fun GameState.whatIsUncovered(
    column: Int,
    count: Int,
): Int {
    val left = columns[column].dropLast(count)
    return when {
        left.isEmpty() -> EMPTIES_COLUMN
        canPlayToFoundation(left.last()) -> FREES_FOUNDATION_CARD
        else -> 0
    }
}

private const val TO_FOUNDATION = 1000
private const val CELL_TO_CARD = 800
private const val RUN_TO_CARD = 600
private const val FREES_FOUNDATION_CARD = 350
private const val CELL_TO_EMPTY_COLUMN = 300
private const val RUN_TO_EMPTY_COLUMN = 200
private const val SPLIT_RUN = 150
private const val TO_FREE_CELL = 100
private const val EMPTIES_COLUMN = 50
