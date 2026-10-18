package com.maximx1.fads.core.rules

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameRecord
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules
import com.maximx1.fads.core.engine.Scoring
import com.maximx1.fads.core.engine.play
import com.maximx1.fads.core.engine.replay
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.state.GameState
import kotlin.math.max

/** FreeCell, as the functions in this package and [play] define it. */
object FreeCellRules : Rules<GameState, Move> {
    override val type get() = GameType.FreeCell
    override val moveSerializer = Move.serializer()

    override fun deal(number: Long) = Deal.fromNumber(number)

    override fun canMove(
        state: GameState,
        move: Move,
    ) = state.canMove(move)

    override fun play(
        state: GameState,
        move: Move,
    ) = state.play(move)

    override fun isWon(state: GameState) = state.isWon

    override fun hint(
        state: GameState,
        before: GameState?,
    ) = state.hint(before)

    override fun canAutoComplete(state: GameState) = state.canAutoComplete

    override fun nextAutoMove(state: GameState) = state.nextAutoMove()

    /** See [Scoring]. */
    override fun score(game: Game<GameState, Move>): Int {
        val cards = Suit.entries.sumOf { game.state.foundationCount(it) }
        val played = cards * Scoring.PER_CARD - game.undos * Scoring.UNDO_COST - game.hintsTaken * Scoring.HINT_COST
        val bonus = if (game.isWon) Scoring.timeBonus(game.elapsedMillis) else 0
        return max(0, played + bonus)
    }

    override fun moveOrNull(move: Any) = move as? Move
}

/** Deals this FreeCell record's deal and plays its moves in order. Throws if any move is illegal. */
fun GameRecord<Move>.replay(): GameState = FreeCellRules.replay(this)
