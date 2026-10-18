package com.maximx1.fads.core.engine

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.replay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deal 1 from the first card to a win, found once by a throwaway search program. One move per word: where from,
 * then where to. `1`-`8` are the columns, `a`-`d` the free cells and `h` the foundations. `x3` on the end means
 * a run of three cards.
 */
private const val DEAL_ONE_SOLUTION =
    "15 1a 1h 2h 1b 3c 3h 3h 6h 5h 4d 4h a8 3a 3h 53 23 25 78 27 2h a2 7a 74 71 67 6h a4 24 2a 24 b2 " +
        "82x2 c8 31x3 8b 8c 86 a6 58x2 4a 38 c3 b3 5b 5c 5h d5 a4 85x3 4a 8d 86 8h a4 58x2 5h 5a 54 a5 d4 " +
        "85x3 68x3 48x3 4a 46x2 4d 43 4h a6 b4 58x3 4a 1b 1h a4 5a c5 15 12x2 1c 13 61 c1 4c 23 2h 53 c4 " +
        "5c d5 65x2 6d 6h bh 6h ch ah 8h 3h 8h 3h 8h 3h 3h 1h 8h 2h 8h 2h 1h 2h 5h 7h 8h 41x2 4h 3h 3h 8h " +
        "8h 1h dh 72 7h 5h 2h 5h 7h 8h 1h"

/** The moves that win deal 1. */
val dealOneWin: List<Move> = DEAL_ONE_SOLUTION.split(' ').map(::parse)

private fun parse(word: String): Move {
    val from = word[0]
    val to = word[1]
    val count = word.substringAfter('x', "1").toInt()
    return when {
        from in 'a'..'d' && to == 'h' -> Move.FreeCellToFoundation(from - 'a')
        from in 'a'..'d' -> Move.FreeCellToColumn(from - 'a', to - '1')
        to == 'h' -> Move.ColumnToFoundation(from - '1')
        to in 'a'..'d' -> Move.ColumnToFreeCell(from - '1', to - 'a')
        else -> Move.ColumnToColumn(from - '1', to - '1', count)
    }
}

/** Plays deal 1 from the first card to a win. */
class ScriptedWinTest {
    @Test
    fun dealOneCanBePlayedToAWin() {
        val moves = dealOneWin
        val record = GameRecord(dealNumber = 1, moves = moves)

        assertFalse(record.copy(moves = moves.dropLast(1)).replay().isWon, "not won until the last card")

        val end = record.replay()
        assertTrue(end.isWon)
        assertTrue(end.columns.all { it.isEmpty() })
        assertTrue(end.freeCells.all { it == null })
        assertEquals(Card.deck.toSet(), end.allCards().toSet())

        assertTrue(Game.resume(record.copy(moves = moves.dropLast(1))).inPlay)
        assertFalse(Game.resume(record).inPlay, "the clock stops at the win")
    }
}
