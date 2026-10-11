package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import kotlinx.serialization.Serializable

/** A whole game: which deal, and every move played so far. Enough to save, resume, undo and replay. */
@Serializable
data class GameRecord(
    val dealNumber: Long,
    val moves: List<Move> = emptyList(),
) {
    /** Deals [dealNumber] and plays [moves] in order. Throws if any move is illegal. */
    fun replay(): GameState = moves.fold(Deal.fromNumber(dealNumber)) { state, move -> state.play(move) }
}
