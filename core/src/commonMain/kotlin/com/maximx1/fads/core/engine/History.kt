package com.maximx1.fads.core.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One game that came to an end, won or given up on: which game and deal, when it ended, how it went, and its
 * saved [game] so it can be watched again move by move.
 *
 * @property endedAt when it ended, in milliseconds since 1970 began.
 * @property game the game's saved text, which [type] reads back with [GameType.load].
 */
@Serializable
data class Played(
    val type: GameType,
    val dealNumber: Long,
    val endedAt: Long,
    val millis: Long,
    val moves: Int,
    val score: Int,
    val won: Boolean,
    val game: String,
) {
    /** The game as it ended, or `null` if its saved text no longer reads back. */
    fun load(): Game<*, *>? = type.load(game)

    companion object {
        fun of(
            game: Game<*, *>,
            endedAt: Long,
        ) = Played(
            type = game.type,
            dealNumber = game.dealNumber,
            endedAt = endedAt,
            millis = game.elapsedMillis,
            moves = game.movesMade,
            score = game.score,
            won = game.isWon,
            game = game.toJson(),
        )
    }
}

/** Every game the player has finished or given up on, newest first. */
@Serializable
data class History(
    val games: List<Played> = emptyList(),
) {
    /** This history with [played] added as the newest. */
    fun with(played: Played): History = History(listOf(played) + games)

    /**
     * The games whose deal number has [number] in it (any, if it is blank), whose type [type] accepts, and that
     * were won if [wonOnly].
     */
    fun search(
        number: String = "",
        type: (GameType) -> Boolean = { true },
        wonOnly: Boolean = false,
    ): List<Played> =
        games.filter { played ->
            (number.isBlank() || number.trim() in played.dealNumber.toString()) &&
                type(played.type) &&
                (!wonOnly || played.won)
        }

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Reads a saved history. Returns `null` if the text is not one. */
        fun fromJson(text: String): History? =
            try {
                json.decodeFromString<History>(text)
            } catch (ignored: IllegalArgumentException) {
                null
            }
    }
}

/** Every position [game] passed through, from the deal to where it stands now. */
fun <S : Any, M : Any> Game<S, M>.positions(): List<S> =
    record.moves.runningFold(rules.deal(dealNumber)) { state, move -> rules.play(state, move) }
