package com.maximx1.fads.core.engine

import com.maximx1.fads.core.rules.FreeCellRules
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import kotlinx.serialization.json.Json

/**
 * A game in progress: its [record] and the position the record leads to under [rules]. Immutable, like the
 * positions themselves.
 */
class Game<S : Any, M : Any> private constructor(
    val rules: Rules<S, M>,
    val record: GameRecord<M>,
    val state: S,
) {
    val type: GameType get() = rules.type

    val dealNumber: Long get() = record.dealNumber

    /** How many moves lead from the deal to this position. Undo takes one off. */
    val moveCount: Int get() = record.moves.size

    /**
     * The player's move count: one for every move played and one for every undo. A run of cards moved together
     * is one move, however many free cells it would take to move it card by card.
     */
    val movesMade: Int get() = record.movesMade

    /** How many moves have been taken back. Each one put a move on [movesMade] and took one off the record. */
    val undos: Int get() = (movesMade - moveCount) / 2

    val hintsTaken: Int get() = record.hintsTaken

    val canUndo: Boolean get() = record.moves.isNotEmpty()

    val isWon: Boolean get() = rules.isWon(state)

    val canAutoComplete: Boolean get() = rules.canAutoComplete(state)

    val score: Int get() = rules.score(this)

    /** Time spent playing so far. The game does not count it: whoever runs the clock sets it with [withElapsed]. */
    val elapsedMillis: Long get() = record.elapsedMillis

    /** True from the first move until the game is won, which is when the clock should be running. */
    val inPlay: Boolean get() = (canUndo || elapsedMillis > 0) && !isWon

    fun canMove(move: M): Boolean = rules.canMove(state, move)

    /** Throws [IllegalArgumentException] if the move is not legal. */
    fun play(move: M): Game<S, M> =
        Game(rules, record.copy(moves = record.moves + move, movesMade = movesMade + 1), rules.play(state, move))

    /** Plays [move] if it is one of this game's moves and legal here, and otherwise leaves the game as it is. */
    fun playIfLegal(move: Any): Game<S, M> = rules.moveOrNull(move)?.takeIf { canMove(it) }?.let(::play) ?: this

    /** Takes back the last move, which counts as a move made. Does nothing at the start of the game. */
    fun undo(): Game<S, M> =
        if (canUndo) resume(rules, record.copy(moves = record.moves.dropLast(1), movesMade = movesMade + 1)) else this

    /** The same deal again from the first card, with the clock and the move count back at zero. */
    fun restart(): Game<S, M> = start(rules, dealNumber)

    /** A move worth trying next, or `null` if there is none to suggest. It never just undoes the last move. */
    fun hint(): M? =
        rules.hint(state, before = if (canUndo) rules.replay(record.copy(moves = record.moves.dropLast(1))) else null)

    /** The next move of an automatic finish, or `null` if there is none. */
    fun nextAutoMove(): M? = rules.nextAutoMove(state)

    /** The same game with one more hint counted against it. */
    fun takeHint(): Game<S, M> = Game(rules, record.copy(hintsTaken = hintsTaken + 1), state)

    /** The same game with [millis] on its clock. */
    fun withElapsed(millis: Long): Game<S, M> = Game(rules, record.copy(elapsedMillis = millis), state)

    /** The saved-game text: the deal number, the moves and the time spent, as JSON. */
    fun toJson(): String = json.encodeToString(GameRecord.serializer(rules.moveSerializer), record)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun <S : Any, M : Any> start(
            rules: Rules<S, M>,
            dealNumber: Long,
        ): Game<S, M> = resume(rules, GameRecord(dealNumber))

        /** Throws [IllegalArgumentException] if the record holds an illegal move. */
        fun <S : Any, M : Any> resume(
            rules: Rules<S, M>,
            record: GameRecord<M>,
        ): Game<S, M> = Game(rules, record, rules.replay(record))

        /** Reads a saved game. Returns `null` if the text is not a saved game or its moves do not replay. */
        fun <S : Any, M : Any> fromJson(
            rules: Rules<S, M>,
            text: String,
        ): Game<S, M>? =
            try {
                resume(rules, json.decodeFromString(GameRecord.serializer(rules.moveSerializer), text))
            } catch (ignored: IllegalArgumentException) {
                // Bad JSON is a SerializationException, which is one of these too.
                null
            }

        /** A FreeCell game on deal [dealNumber]. */
        fun start(dealNumber: Long): Game<GameState, Move> = start(FreeCellRules, dealNumber)

        fun resume(record: GameRecord<Move>): Game<GameState, Move> = resume(FreeCellRules, record)

        /** Reads a saved FreeCell game. */
        fun fromJson(text: String): Game<GameState, Move>? = fromJson(FreeCellRules, text)
    }
}
