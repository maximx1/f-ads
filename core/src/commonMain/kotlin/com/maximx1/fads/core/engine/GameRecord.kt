package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import kotlinx.serialization.Serializable

/**
 * A whole game: which deal, the moves that lead to the position on the table, how many moves the player has made
 * in all, and how long they have spent. Enough to save, resume, undo and replay.
 *
 * @property movesMade the player's move count. It is more than the length of [moves] once anything has been
 * undone, because an undo takes a move off [moves] but counts as a move made. A game saved before there was a
 * count has made as many moves as it holds.
 */
@Serializable
data class GameRecord(
    val dealNumber: Long,
    val moves: List<Move> = emptyList(),
    val elapsedMillis: Long = 0,
    val movesMade: Int = moves.size,
) {
    init {
        require(movesMade >= moves.size) { "Fewer moves made ($movesMade) than are on the record (${moves.size})" }
        require(elapsedMillis >= 0) { "Elapsed time cannot be negative: $elapsedMillis" }
    }

    /** Deals [dealNumber] and plays [moves] in order. Throws if any move is illegal. */
    fun replay(): GameState = moves.fold(Deal.fromNumber(dealNumber)) { state, move -> state.play(move) }
}
