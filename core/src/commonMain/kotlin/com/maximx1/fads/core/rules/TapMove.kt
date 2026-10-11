package com.maximx1.fads.core.rules

import com.maximx1.fads.core.state.GameState

/**
 * The move a tap makes for the top [count] cards of [column], or `null` if they cannot go anywhere.
 * First choice wins: a foundation, then onto another card, then a free cell, then an empty column.
 * A whole column is never sent to an empty column, since that changes nothing.
 */
fun GameState.tapMoveFromColumn(
    column: Int,
    count: Int = 1,
): Move? {
    val (occupied, empty) = columns.indices.partition { columns[it].isNotEmpty() }
    val wholeColumn = columns.getOrNull(column)?.size == count
    val candidates =
        buildList {
            if (count == 1) add(Move.ColumnToFoundation(column))
            occupied.mapTo(this) { Move.ColumnToColumn(column, it, count) }
            if (count == 1) freeCells.indices.mapTo(this) { Move.ColumnToFreeCell(column, it) }
            if (!wholeColumn) empty.mapTo(this) { Move.ColumnToColumn(column, it, count) }
        }
    return candidates.firstOrNull { canMove(it) }
}

/**
 * The move a tap makes for the card in free cell [cell], or `null` if it cannot go anywhere.
 * First choice wins: a foundation, then onto another card, then an empty column.
 */
fun GameState.tapMoveFromFreeCell(cell: Int): Move? {
    val (occupied, empty) = columns.indices.partition { columns[it].isNotEmpty() }
    val candidates =
        listOf(Move.FreeCellToFoundation(cell)) + (occupied + empty).map { Move.FreeCellToColumn(cell, it) }
    return candidates.firstOrNull { canMove(it) }
}
