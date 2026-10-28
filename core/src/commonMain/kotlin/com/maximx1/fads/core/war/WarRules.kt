package com.maximx1.fads.core.war

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules

/**
 * War against the computer: the deck split in two, both turn over their top card, and the higher takes both. It
 * is won by taking every card, and lost by losing them all. Nothing is chosen, so it can play itself out at any
 * time.
 */
object WarRules : Rules<WarState, WarMove> {
    override val type get() = GameType.War
    override val moveSerializer = WarMove.serializer()

    override fun deal(number: Long) = warDeal(number)

    override fun canMove(
        state: WarState,
        move: WarMove,
    ) = state.canMove(move)

    override fun play(
        state: WarState,
        move: WarMove,
    ) = state.play(move)

    override fun isWon(state: WarState) = state.isWon

    override fun hint(
        state: WarState,
        before: WarState?,
    ) = nextAutoMove(state)

    override fun canAutoComplete(state: WarState) = !state.isOver

    override fun nextAutoMove(state: WarState): WarMove? = WarMove.Flip.takeIf { state.canMove(it) }

    /**
     * The cards you hold: 26 at the deal and 52 on a win. Luck is all there is to it, so undos and hints cost
     * nothing.
     */
    override fun score(game: Game<WarState, WarMove>): Int = game.state.held(Side.YOU)

    override fun moveOrNull(move: Any) = move as? WarMove
}
