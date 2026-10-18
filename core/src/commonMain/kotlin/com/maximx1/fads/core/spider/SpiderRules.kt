package com.maximx1.fads.core.spider

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules
import kotlin.math.max

/** Spider, played with [suits] suits: 1, 2 or 4. */
class SpiderRules private constructor(
    val suits: Int,
) : Rules<SpiderState, SpiderMove> {
    override val type get() = GameType.Spider(suits)
    override val moveSerializer = SpiderMove.serializer()

    override fun deal(number: Long) = spiderDeal(number, suits)

    override fun canMove(
        state: SpiderState,
        move: SpiderMove,
    ) = state.canMove(move)

    override fun play(
        state: SpiderState,
        move: SpiderMove,
    ) = state.play(move)

    override fun isWon(state: SpiderState) = state.isWon

    override fun hint(
        state: SpiderState,
        before: SpiderState?,
    ) = state.hint(before)

    override fun canAutoComplete(state: SpiderState) = state.canAutoComplete

    override fun nextAutoMove(state: SpiderState) = state.nextAutoMove()

    /**
     * Windows Spider's score: [START], less [PER_MOVE] for every move made (an undo counts as one), and
     * [PER_RUN] for every king-to-ace run off the table. Never below zero.
     */
    override fun score(game: Game<SpiderState, SpiderMove>): Int =
        max(0, START - game.movesMade * PER_MOVE + game.state.finished.size * PER_RUN)

    override fun moveOrNull(move: Any) = move as? SpiderMove

    companion object {
        const val START = 500
        const val PER_MOVE = 1
        const val PER_RUN = 100

        private val bySuits = SpiderState.SUIT_CHOICES.associateWith { SpiderRules(it) }

        fun forSuits(suits: Int): SpiderRules = bySuits.getValue(suits)
    }
}

/**
 * A move worth trying, or `null` if there is none to suggest. A rule of thumb, not a solver: whatever finishes a
 * run, then whatever turns a face-down card over, then building on a card of the same suit, and dealing a row only
 * when nothing on the table is worth doing. It never suggests a move that puts the cards back as they were in
 * [before].
 */
fun SpiderState.hint(before: SpiderState? = null): SpiderMove? =
    legalMoves()
        .mapNotNull { move -> worth(move).takeIf { it > 0 }?.let { move to it } }
        .filter { (move, _) -> play(move) != before }
        .maxByOrNull { (_, worth) -> worth }
        ?.first

private fun SpiderState.worth(move: SpiderMove): Int =
    when (move) {
        SpiderMove.DealRow -> DEAL
        is SpiderMove.PileToPile -> worthOf(move)
    }

private fun SpiderState.worthOf(move: SpiderMove.PileToPile): Int {
    val left = piles[move.from].size - move.count
    val top = piles[move.to].lastOrNull()?.card
    val lead = piles[move.from][left].card
    return when {
        play(move).finished.size > finished.size -> FINISHES_RUN
        left > 0 && left == hidden[move.from] -> TURNS_OVER + move.count
        breaksRun(move.from, left) -> 0
        top == null -> 0
        top.suit == lead.suit -> SAME_SUIT + move.count
        left == 0 -> EMPTIES_PILE
        else -> OTHER_SUIT + move.count
    }
}

/**
 * True if taking the cards above the first [left] of pile [pile] lifts them off the face-up card of their own
 * suit one rank higher, which only shuffles them about.
 */
private fun SpiderState.breaksRun(
    pile: Int,
    left: Int,
): Boolean {
    val below = piles[pile].getOrNull(left - 1)?.card
    val lead = piles[pile][left].card
    return below != null && left > hidden[pile] && below.suit == lead.suit && below.rank.value == lead.rank.value + 1
}

private const val FINISHES_RUN = 1000
private const val TURNS_OVER = 800
private const val SAME_SUIT = 600
private const val EMPTIES_PILE = 400
private const val OTHER_SUIT = 200
private const val DEAL = 100
