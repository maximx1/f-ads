package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules
import com.maximx1.fads.core.engine.Scoring
import kotlin.math.max

/** Klondike, turning [draw] cards from the stock at a time. */
class KlondikeRules private constructor(
    val draw: Int,
) : Rules<KlondikeState, KlondikeMove> {
    override val type get() = GameType.Klondike(draw)
    override val moveSerializer = KlondikeMove.serializer()

    override fun deal(number: Long) = klondikeDeal(number, draw)

    override fun canMove(
        state: KlondikeState,
        move: KlondikeMove,
    ) = state.canMove(move)

    override fun play(
        state: KlondikeState,
        move: KlondikeMove,
    ) = state.play(move)

    override fun isWon(state: KlondikeState) = state.isWon

    override fun hint(
        state: KlondikeState,
        before: KlondikeState?,
    ) = state.hint(before)

    override fun canAutoComplete(state: KlondikeState) = state.canAutoComplete

    override fun nextAutoMove(state: KlondikeState) = state.nextAutoMove()

    /**
     * The Windows standard score, worked out again from the moves on the record, so an undone move takes its
     * points with it. See [KlondikeScoring].
     */
    override fun score(game: Game<KlondikeState, KlondikeMove>): Int {
        var points = 0
        var state = deal(game.dealNumber)
        for (move in game.record.moves) {
            val next = state.play(move)
            points += KlondikeScoring.pointsFor(state, move, next)
            state = next
        }
        points -= (game.elapsedMillis / KlondikeScoring.TIME_STEP_MILLIS).toInt() * KlondikeScoring.TIME_COST
        if (game.isWon) points += Scoring.timeBonus(game.elapsedMillis)
        return max(0, points)
    }

    override fun moveOrNull(move: Any) = move as? KlondikeMove

    companion object {
        private val drawOne = KlondikeRules(1)
        private val drawThree = KlondikeRules(KlondikeState.DRAW_THREE)

        fun forDraw(draw: Int): KlondikeRules = if (draw == 1) drawOne else drawThree
    }
}

/**
 * Windows Solitaire's standard scoring. A card from the waste to a pile earns [WASTE_TO_PILE]; any card to a
 * foundation [TO_FOUNDATION]; turning a pile's card over [TURN_OVER]. Taking a card back off a foundation costs
 * [FROM_FOUNDATION], and turning the waste back over costs [RECYCLE_DRAW_ONE] or [RECYCLE_DRAW_THREE]. Every
 * [TIME_STEP_MILLIS] of play costs [TIME_COST], and a win earns the same time bonus as FreeCell.
 */
object KlondikeScoring {
    const val WASTE_TO_PILE = 5
    const val TO_FOUNDATION = 10
    const val TURN_OVER = 5
    const val FROM_FOUNDATION = -15
    const val RECYCLE_DRAW_ONE = -100
    const val RECYCLE_DRAW_THREE = -20
    const val TIME_STEP_MILLIS = 10_000L
    const val TIME_COST = 2

    /** The points [move] earns, played from [before] to [after]. */
    fun pointsFor(
        before: KlondikeState,
        move: KlondikeMove,
        after: KlondikeState,
    ): Int {
        val turned = before.hidden.sum() - after.hidden.sum()
        val base =
            when (move) {
                KlondikeMove.Draw -> 0
                KlondikeMove.Recycle -> if (before.draw == 1) RECYCLE_DRAW_ONE else RECYCLE_DRAW_THREE
                KlondikeMove.WasteToFoundation, is KlondikeMove.PileToFoundation -> TO_FOUNDATION
                is KlondikeMove.WasteToPile -> WASTE_TO_PILE
                is KlondikeMove.PileToPile -> 0
                is KlondikeMove.FoundationToPile -> FROM_FOUNDATION
            }
        return base + turned * TURN_OVER
    }
}
