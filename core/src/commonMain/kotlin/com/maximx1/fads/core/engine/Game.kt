package com.maximx1.fads.core.engine

import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import kotlinx.serialization.json.Json

/** A game in progress: its [record] and the position the record leads to. Immutable, like [GameState]. */
class Game private constructor(
    val record: GameRecord,
    val state: GameState,
) {
    val dealNumber: Long get() = record.dealNumber

    val moveCount: Int get() = record.moves.size

    val canUndo: Boolean get() = record.moves.isNotEmpty()

    /** Throws [IllegalArgumentException] if the move is not legal. */
    fun play(move: Move): Game = Game(record.copy(moves = record.moves + move), state.play(move))

    /** Takes back the last move. Does nothing at the start of the game. */
    fun undo(): Game = if (canUndo) resume(record.copy(moves = record.moves.dropLast(1))) else this

    /** The same deal again from the first card. */
    fun restart(): Game = start(dealNumber)

    /** The saved-game text: the deal number and the moves, as JSON. */
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
