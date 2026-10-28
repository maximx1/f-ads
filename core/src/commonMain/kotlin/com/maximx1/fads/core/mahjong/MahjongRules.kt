package com.maximx1.fads.core.mahjong

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules
import com.maximx1.fads.core.engine.Scoring
import kotlin.math.max

/** Mahjong solitaire on the [Turtle]: take free tiles off in matching pairs until the table is clear. */
object MahjongRules : Rules<MahjongState, MahjongMove> {
    /** Points for every pair off the table. */
    const val PER_PAIR = 10

    override val type get() = GameType.Mahjong
    override val moveSerializer = MahjongMove.serializer()

    override fun deal(number: Long) = mahjongDeal(number)

    override fun canMove(
        state: MahjongState,
        move: MahjongMove,
    ) = state.canMove(move)

    override fun play(
        state: MahjongState,
        move: MahjongMove,
    ) = state.play(move)

    override fun isWon(state: MahjongState) = state.isWon

    // No move puts tiles back, so there is no going straight back to the position before.
    override fun hint(
        state: MahjongState,
        before: MahjongState?,
    ) = state.hint()

    override fun canAutoComplete(state: MahjongState) = state.canAutoComplete

    override fun nextAutoMove(state: MahjongState) = state.nextAutoMove()

    /**
     * Scored as FreeCell is: [PER_PAIR] for every pair off the table, less the cost of each undo and hint, and the
     * time bonus on a win. Never below zero. See [Scoring].
     */
    override fun score(game: Game<MahjongState, MahjongMove>): Int {
        val pairs = (Turtle.spots.size - game.state.tiles.size) / 2
        val played = pairs * PER_PAIR - game.undos * Scoring.UNDO_COST - game.hintsTaken * Scoring.HINT_COST
        val bonus = if (game.isWon) Scoring.timeBonus(game.elapsedMillis) else 0
        return max(0, played + bonus)
    }

    override fun moveOrNull(move: Any) = move as? MahjongMove
}

/**
 * A pair worth taking off, or `null` if no pair can be. A rule of thumb, not a solver: the pair that frees the most
 * tiles, then one that takes the last of its kind off the table, then the one highest up.
 */
fun MahjongState.hint(): MahjongMove? = legalMoves().maxByOrNull { worth(it) }

private fun MahjongState.worth(move: MahjongMove.Match): Int {
    val first = tile(move.first)
    val second = tile(move.second)
    if (first == null || second == null) return 0
    val after = play(move)
    val freed = after.free.size - (free.size - 2)
    val lastOfKind = after.tiles.none { it.face.matches(first.face) }
    return freed * FREES + (if (lastOfKind) LAST_OF_KIND else 0) + first.spot.layer + second.spot.layer
}

private const val FREES = 100
private const val LAST_OF_KIND = 50
