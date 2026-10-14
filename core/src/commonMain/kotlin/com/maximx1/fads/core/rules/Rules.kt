package com.maximx1.fads.core.rules

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
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
        is Move.ColumnToFoundation -> topOfColumn(move.from).let { it != null && canPlayToFoundation(it) }
        is Move.FreeCellToColumn -> canLandOn(move.to, freeCells.getOrNull(move.cell))
        is Move.FreeCellToFoundation -> freeCells.getOrNull(move.cell).let { it != null && canPlayToFoundation(it) }
        is Move.FoundationToColumn -> canLandOn(move.to, foundationTop(move.suit))
        is Move.FoundationToFreeCell -> foundationTop(move.suit) != null && isEmptyFreeCell(move.cell)
    }

/**
 * Every legal move in this position. A card can always come back off a foundation into an empty free cell or
 * column, so the list is only empty when no card can move and nothing has been played to a foundation yet.
 */
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
        for (suit in Suit.entries) {
            columns.indices.mapTo(this) { Move.FoundationToColumn(suit, it) }
            freeCells.indices.mapTo(this) { Move.FoundationToFreeCell(suit, it) }
        }
    }.filter { canMove(it) }

private fun GameState.topOfColumn(index: Int): Card? = columns.getOrNull(index)?.lastOrNull()

private fun GameState.isEmptyFreeCell(index: Int): Boolean = index in freeCells.indices && freeCells[index] == null

/** False for no card at all, so callers can pass whatever they found at the place the move starts from. */
private fun GameState.canLandOn(
    column: Int,
    card: Card?,
): Boolean {
    val cards = columns.getOrNull(column)
    return card != null && cards != null && (cards.isEmpty() || card.canStackOn(cards.last()))
}

private fun GameState.canMoveRun(move: Move.ColumnToColumn): Boolean {
    val source = columns.getOrNull(move.from).orEmpty()
    val fits = move.from != move.to && move.count in 1..source.size && move.count <= maxMovableRun(move.to)
    return fits && source.takeLast(move.count).let { run -> run.isRun() && canLandOn(move.to, run.first()) }
}
