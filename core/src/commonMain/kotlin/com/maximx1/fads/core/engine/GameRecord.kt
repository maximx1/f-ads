package com.maximx1.fads.core.engine

import kotlinx.serialization.Serializable

/**
 * A whole game: which deal, the moves that lead to the position on the table, how many moves the player has made
 * in all, how many hints they have taken, and how long they have spent. Enough to save, resume, undo and replay.
 * The [Rules] of the game it belongs to turn it back into a position.
 *
 * @property movesMade the player's move count. It is more than the length of [moves] once anything has been
 * undone, because an undo takes a move off [moves] but counts as a move made. A game saved before there was a
 * count has made as many moves as it holds.
 */
@Serializable
data class GameRecord<M>(
    val dealNumber: Long,
    val moves: List<M> = emptyList(),
    val elapsedMillis: Long = 0,
    val movesMade: Int = moves.size,
    val hintsTaken: Int = 0,
) {
    init {
        require(movesMade >= moves.size) { "Fewer moves made ($movesMade) than are on the record (${moves.size})" }
        require(hintsTaken >= 0) { "Hints taken cannot be negative: $hintsTaken" }
        require(elapsedMillis >= 0) { "Elapsed time cannot be negative: $elapsedMillis" }
    }
}
