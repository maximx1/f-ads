package com.maximx1.fads.core.rules

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.state.GameState

/** Tableau rule: one rank lower and the opposite colour. */
fun Card.canStackOn(below: Card): Boolean = color != below.color && rank.value + 1 == below.rank.value

/** True if the cards, listed from the bottom up, each stack on the one before. */
fun List<Card>.isRun(): Boolean = zipWithNext().all { (below, above) -> above.canStackOn(below) }

/** Foundation rule: same suit, ascending, ace first. */
fun GameState.canPlayToFoundation(card: Card): Boolean = foundationCount(card.suit) == card.rank.value - 1

/**
 * The longest run that can be moved onto column [toColumn] in one go:
 * (empty free cells + 1) x 2^(empty columns), not counting the destination itself when it is empty.
 */
fun GameState.maxMovableRun(toColumn: Int): Int {
    val emptyFreeCells = freeCells.count { it == null }
    val emptyColumns = columns.indices.count { it != toColumn && columns[it].isEmpty() }
    return (emptyFreeCells + 1) shl emptyColumns
}

fun GameState.canMove(move: Move): Boolean =
    when (move) {
        is Move.ColumnToColumn -> canMoveRun(move)
        is Move.ColumnToFreeCell -> topOfColumn(move.from) != null && isEmptyFreeCell(move.cell)
        is Move.ColumnToFoundation -> topOfColumn(move.from)?.let { canPlayToFoundation(it) } ?: false
        is Move.FreeCellToColumn -> freeCells.getOrNull(move.cell)?.let { canLandOn(move.to, it) } ?: false
        is Move.FreeCellToFoundation -> freeCells.getOrNull(move.cell)?.let { canPlayToFoundation(it) } ?: false
    }

/** Every legal move in this position. Empty when the game is won or stuck. */
fun GameState.legalMoves(): List<Move> =
    buildList {
        for (from in columns.indices) {
            add(Move.ColumnToFoundation(from))
            freeCells.indices.mapTo(this) { Move.ColumnToFreeCell(from, it) }
            for (to in columns.indices) {
                (1..columns[from].size).mapTo(this) { Move.ColumnToColumn(from, to, it) }
            }
        }
        for (cell in freeCells.indices) {
            add(Move.FreeCellToFoundation(cell))
            columns.indices.mapTo(this) { Move.FreeCellToColumn(cell, it) }
        }
    }.filter { canMove(it) }

private fun GameState.topOfColumn(index: Int): Card? = columns.getOrNull(index)?.lastOrNull()

private fun GameState.isEmptyFreeCell(index: Int): Boolean = index in freeCells.indices && freeCells[index] == null

private fun GameState.canLandOn(
    column: Int,
    card: Card,
): Boolean = columns.getOrNull(column)?.let { it.isEmpty() || card.canStackOn(it.last()) } ?: false

private fun GameState.canMoveRun(move: Move.ColumnToColumn): Boolean {
    val source = columns.getOrNull(move.from).orEmpty()
    val fits = move.from != move.to && move.count in 1..source.size && move.count <= maxMovableRun(move.to)
    return fits && source.takeLast(move.count).let { run -> run.isRun() && canLandOn(move.to, run.first()) }
}
