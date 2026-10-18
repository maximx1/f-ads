package com.maximx1.fads.core.engine

import kotlinx.serialization.KSerializer

/**
 * Everything that makes one kind of patience what it is: how it deals, which moves are legal, what they do,
 * when it is won, and how it scores. [Game] keeps the record and the clock and leaves the rest to these.
 *
 * [S] is a position, which must be immutable, and [M] a move, which must be serializable with [moveSerializer].
 */
interface Rules<S : Any, M : Any> {
    val type: GameType

    val moveSerializer: KSerializer<M>

    /** Deal number [number] of this game. The same number gives the same position, forever. */
    fun deal(number: Long): S

    fun canMove(
        state: S,
        move: M,
    ): Boolean

    /** The position after [move]. Throws [IllegalArgumentException] if the move is not legal. */
    fun play(
        state: S,
        move: M,
    ): S

    fun isWon(state: S): Boolean

    /**
     * A move worth trying next, or `null` if there is none to suggest. It never suggests going straight back to
     * [before], the position the last move was played from.
     */
    fun hint(
        state: S,
        before: S?,
    ): M?

    /** True once the game can play itself out to a win. */
    fun canAutoComplete(state: S): Boolean

    /** The next move of that automatic finish, or `null` if there is none to play. */
    fun nextAutoMove(state: S): M?

    fun score(game: Game<S, M>): Int

    /** [move] as one of this game's moves, or `null` if it belongs to another game. */
    fun moveOrNull(move: Any): M?
}

/** Deals [record]'s deal and plays its moves in order. Throws if any move is illegal. */
fun <S : Any, M : Any> Rules<S, M>.replay(record: GameRecord<M>): S =
    record.moves.fold(deal(record.dealNumber)) { state, move -> play(state, move) }
