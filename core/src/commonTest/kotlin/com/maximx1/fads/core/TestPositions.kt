package com.maximx1.fads.core

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.state.GameState

/** Cards from their codes, separated by spaces: `cards("KS QH JS")`. */
fun cards(codes: String): List<Card> = codes.split(' ').filter { it.isNotEmpty() }.map(Card::fromCode)

/**
 * Builds a position for a test. Columns are listed left to right, bottom card first; missing columns are empty.
 * [free] lists the free cells left to right with `-` for an empty one; missing cells are empty.
 */
fun position(
    vararg columns: String,
    free: String = "",
    foundations: Map<Suit, Int> = emptyMap(),
): GameState {
    val cells = free.split(' ').filter { it.isNotEmpty() }.map { if (it == "-") null else Card.fromCode(it) }
    return GameState(
        columns = List(GameState.COLUMN_COUNT) { cards(columns.getOrElse(it) { "" }) },
        freeCells = List(GameState.FREE_CELL_COUNT) { cells.getOrNull(it) },
        foundations = foundations,
    )
}

fun GameState.columnCodes(): List<String> = columns.map { column -> column.joinToString(" ") { it.code } }
