package com.maximx1.fads.core.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A game that was won: which deal, how long it took, the move count at the end, and the score. */
@Serializable
data class Win(
    val dealNumber: Long,
    val millis: Long,
    val moves: Int,
    val score: Int = 0,
)

/**
 * The player's records: the quickest win of all and the highest scoring one, and the quickest time and highest
 * score on each deal that has been won.
 *
 * @property deals deal number to the best time on it, in milliseconds.
 * @property dealScores deal number to the highest score on it. Wins from before there were scores are not in it.
 */
@Serializable
data class Records(
    val fastest: Win? = null,
    val deals: Map<Long, Long> = emptyMap(),
    val highest: Win? = null,
    val dealScores: Map<Long, Int> = emptyMap(),
) {
    /** The best time on [dealNumber] in milliseconds, or `null` if it has never been won. */
    fun bestOn(dealNumber: Long): Long? = deals[dealNumber]

    /** The highest score on [dealNumber], or `null` if it has never been won since there were scores. */
    fun highestOn(dealNumber: Long): Int? = dealScores[dealNumber]

    /** These records with [win] counted. The very same object comes back if the win beats nothing. */
    fun with(win: Win): Records {
        val deal = win.dealNumber
        val counted =
            Records(
                fastest = fastest?.takeIf { it.millis <= win.millis } ?: win,
                deals =
                    if (bestOn(
                            deal,
                        ).let { it != null && it <= win.millis }
                    ) {
                        deals
                    } else {
                        deals + (deal to win.millis)
                    },
                highest = highest?.takeIf { it.score >= win.score } ?: win,
                dealScores =
                    if (highestOn(deal).let { it != null && it >= win.score }) {
                        dealScores
                    } else {
                        dealScores +
                            (deal to win.score)
                    },
            )
        return if (counted == this) this else counted
    }

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Reads saved records. Returns `null` if the text is not a set of records. */
        fun fromJson(text: String): Records? =
            try {
                json.decodeFromString<Records>(text)
            } catch (ignored: IllegalArgumentException) {
                null
            }
    }
}
