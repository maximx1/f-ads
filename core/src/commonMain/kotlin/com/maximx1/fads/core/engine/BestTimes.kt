package com.maximx1.fads.core.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A game that was won: which deal, how long it took, and the move count at the end. */
@Serializable
data class Win(
    val dealNumber: Long,
    val millis: Long,
    val moves: Int,
)

/**
 * The player's records: the quickest win of all, and the quickest time on each deal that has been won.
 *
 * @property deals deal number to the best time on it, in milliseconds.
 */
@Serializable
data class BestTimes(
    val fastest: Win? = null,
    val deals: Map<Long, Long> = emptyMap(),
) {
    /** The best time on [dealNumber] in milliseconds, or `null` if it has never been won. */
    fun bestOn(dealNumber: Long): Long? = deals[dealNumber]

    /** These records with [win] counted. The very same object comes back if the win beats nothing. */
    fun with(win: Win): BestTimes {
        val beatsAll = fastest == null || win.millis < fastest.millis
        val beatsDeal = bestOn(win.dealNumber).let { it == null || win.millis < it }
        if (!beatsAll && !beatsDeal) return this
        return BestTimes(
            fastest = if (beatsAll) win else fastest,
            deals = if (beatsDeal) deals + (win.dealNumber to win.millis) else deals,
        )
    }

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Reads saved records. Returns `null` if the text is not a set of records. */
        fun fromJson(text: String): BestTimes? =
            try {
                json.decodeFromString<BestTimes>(text)
            } catch (ignored: IllegalArgumentException) {
                null
            }
    }
}
