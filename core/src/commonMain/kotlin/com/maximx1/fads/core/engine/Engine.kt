package com.maximx1.fads.core.engine

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.canMove
import com.maximx1.fads.core.state.GameState

/** Returns the position after [move]. Throws [IllegalArgumentException] if the move is not legal here. */
fun GameState.play(move: Move): GameState {
    require(canMove(move)) { "Illegal move: $move" }
    return when (move) {
        is Move.ColumnToColumn -> {
            val run = columns[move.from].takeLast(move.count)
            withColumn(move.from, columns[move.from].dropLast(move.count)).withColumn(move.to, columns[move.to] + run)
        }

        is Move.ColumnToFreeCell -> {
            withFreeCell(move.cell, columns[move.from].last()).withoutTopOf(move.from)
        }

        is Move.ColumnToFoundation -> {
            withOneMoreOn(columns[move.from].last().suit).withoutTopOf(move.from)
        }

        is Move.FreeCellToColumn -> {
            withColumn(move.to, columns[move.to] + freeCells.getValue(move.cell)).withFreeCell(move.cell, null)
        }

        is Move.FreeCellToFoundation -> {
            withOneMoreOn(freeCells.getValue(move.cell).suit).withFreeCell(move.cell, null)
        }
    }
}

private fun List<Card?>.getValue(index: Int): Card = checkNotNull(this[index])

private fun GameState.withColumn(
    index: Int,
    cards: List<Card>,
): GameState = copy(columns = columns.mapIndexed { i, column -> if (i == index) cards else column })

private fun GameState.withoutTopOf(index: Int): GameState = withColumn(index, columns[index].dropLast(1))

private fun GameState.withFreeCell(
    index: Int,
    card: Card?,
): GameState = copy(freeCells = freeCells.mapIndexed { i, held -> if (i == index) card else held })

private fun GameState.withOneMoreOn(suit: Suit): GameState {
    val counts = foundations + (suit to foundationCount(suit) + 1)
    return copy(foundations = counts)
}
