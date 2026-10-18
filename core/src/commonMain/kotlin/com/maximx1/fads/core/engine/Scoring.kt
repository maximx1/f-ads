package com.maximx1.fads.core.engine

import com.maximx1.fads.core.model.Suit
import kotlin.math.max

/**
 * How a game is scored. It climbs by [PER_CARD] for every card on a foundation and drops by [UNDO_COST] for
 * every undo and [HINT_COST] for every hint, but never below zero. A won game adds a time bonus as well:
 * [TIME_BONUS] divided by the seconds it took, as Windows Solitaire did, so a win in half the time earns twice
 * the bonus.
 */
object Scoring {
    const val PER_CARD = 10
    const val UNDO_COST = 5
    const val HINT_COST = 15
    const val TIME_BONUS = 700_000L

    // Wins quicker than this earn no more than one this long, so a win with next to nothing on the clock (a game
    // saved before there was one) does not earn a bonus beyond anything a real game could.
    private const val QUICKEST_SECONDS = 30L
    private const val MILLIS_IN_SECOND = 1000L

    /** The bonus for a win in [millis]. */
    fun timeBonus(millis: Long): Int = (TIME_BONUS / max(millis / MILLIS_IN_SECOND, QUICKEST_SECONDS)).toInt()
}

/** The game's score as it stands: see [Scoring]. */
val Game.score: Int
    get() {
        val cards = Suit.entries.sumOf { state.foundationCount(it) }
        val played = cards * Scoring.PER_CARD - undos * Scoring.UNDO_COST - hintsTaken * Scoring.HINT_COST
        val bonus = if (state.isWon) Scoring.timeBonus(elapsedMillis) else 0
        return max(0, played + bonus)
    }
