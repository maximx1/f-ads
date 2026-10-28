package com.maximx1.fads.core.den

import com.maximx1.fads.core.engine.History
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max

/**
 * Everything the player has in the gambler's den: the [purse], how many of the points won at patience have been
 * [cashedIn] for it already, and where things stand at each table: the bets down on the [roulette] layout, and the
 * five-card [draw] and [holdem] tables, once the player has sat down at them. The points still to cash in are
 * worked out from the history, so a win counts once however often they are.
 */
@Serializable
data class Den(
    val cashedIn: Long = 0,
    val purse: Purse = Purse(),
    val roulette: Roulette = Roulette(),
    val draw: PokerTable? = null,
    val holdem: PokerTable? = null,
) {
    /** The points won and not yet cashed in. */
    fun pointsLeft(history: History): Long = max(0, history.pointsWon() - cashedIn)

    /** The bronzes cashing in now would bring: one for every whole [POINTS_PER_BRONZE] points left. */
    fun bronzesToCashIn(history: History): Long = pointsLeft(history) / POINTS_PER_BRONZE

    /** Cashes in every whole [POINTS_PER_BRONZE] points left. The few left over wait for the next time. */
    fun cashIn(history: History): Den {
        val bronzes = bronzesToCashIn(history)
        return copy(cashedIn = cashedIn + bronzes * POINTS_PER_BRONZE, purse = purse + bronzes)
    }

    fun toJson(): String = json.encodeToString(this)

    companion object {
        const val POINTS_PER_BRONZE = 100L

        // The roulette bets are kept by the bet they are on, which is more than a string.
        private val json =
            Json {
                ignoreUnknownKeys = true
                allowStructuredMapKeys = true
            }

        /** Reads a saved den. Returns `null` if the text is not one. */
        fun fromJson(text: String): Den? =
            try {
                json.decodeFromString<Den>(text)
            } catch (ignored: IllegalArgumentException) {
                null
            }
    }
}

/** The points earned at patience: the score of every game won. Games given up on earn nothing. */
fun History.pointsWon(): Long = games.filter { it.won }.sumOf { it.score.toLong() }
