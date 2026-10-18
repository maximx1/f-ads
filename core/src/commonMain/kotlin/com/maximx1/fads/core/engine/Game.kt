package com.maximx1.fads.core.engine

import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.hint
import com.maximx1.fads.core.state.GameState
import kotlinx.serialization.json.Json

/** A game in progress: its [record] and the position the record leads to. Immutable, like [GameState]. */
class Game private constructor(
    val record: GameRecord,
    val state: GameState,
) {
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

    /** Time spent playing so far. The game does not count it: whoever runs the clock sets it with [withElapsed]. */
    val elapsedMillis: Long get() = record.elapsedMillis

    /** True from the first move until the game is won, which is when the clock should be running. */
    val inPlay: Boolean get() = (canUndo || elapsedMillis > 0) && !state.isWon

    /** Throws [IllegalArgumentException] if the move is not legal. */
    fun play(move: Move): Game =
        Game(record.copy(moves = record.moves + move, movesMade = movesMade + 1), state.play(move))

    /** Takes back the last move, which counts as a move made. Does nothing at the start of the game. */
    fun undo(): Game =
        if (canUndo) resume(record.copy(moves = record.moves.dropLast(1), movesMade = movesMade + 1)) else this

    /** The same deal again from the first card, with the clock and the move count back at zero. */
    fun restart(): Game = start(dealNumber)

    /** A move worth trying next, or `null` if there is none to suggest. It never just undoes the last move. */
    fun hint(): Move? =
        state.hint(before = if (canUndo) record.copy(moves = record.moves.dropLast(1)).replay() else null)

    /** The same game with one more hint counted against it. */
    fun takeHint(): Game = Game(record.copy(hintsTaken = hintsTaken + 1), state)

    /** The same game with [millis] on its clock. */
    fun withElapsed(millis: Long): Game = Game(record.copy(elapsedMillis = millis), state)

    /** The saved-game text: the deal number, the moves and the time spent, as JSON. */
    fun toJson(): String = json.encodeToString(record)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun start(dealNumber: Long): Game = resume(GameRecord(dealNumber))

        /** Throws [IllegalArgumentException] if the record holds an illegal move. */
        fun resume(record: GameRecord): Game = Game(record, record.replay())

        /** Reads a saved game. Returns `null` if the text is not a saved game or its moves do not replay. */
        fun fromJson(text: String): Game? =
            try {
                resume(json.decodeFromString<GameRecord>(text))
            } catch (ignored: IllegalArgumentException) {
                null
            }
    }
}
