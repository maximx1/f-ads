package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.canMove
import com.maximx1.fads.core.rules.cardsMovedBy
import com.maximx1.fads.core.rules.isRun
import com.maximx1.fads.core.rules.stuckCards
import com.maximx1.fads.core.rules.tapMoveFromColumn
import com.maximx1.fads.core.rules.tapMoveFromFreeCell
import com.maximx1.fads.core.state.GameState

/** FreeCell on the screen, by [BoardLayout]. Each card's id is its place in [Card.deck]. */
object FreeCellLayout : TableLayout<GameState, Move> {
    fun idOf(card: Card): Int = card.suit.ordinal * Rank.COUNT + card.rank.ordinal

    override fun arrange(
        state: GameState,
        width: Float,
        height: Float,
        density: Density,
    ): Arrangement<Move> =
        with(density) {
            FreeCellArrangement(
                state,
                BoardLayout(width, height, SlotGap.toPx(), GroupSplit.toPx(), state.columns.map { it.size }),
            )
        }

    override fun stuck(state: GameState): Set<Int> = state.stuckCards().mapTo(HashSet(), ::idOf)

    private object Rank {
        const val COUNT = 13
    }
}

private class FreeCellArrangement(
    private val state: GameState,
    private val layout: BoardLayout,
) : Arrangement<Move> {
    private val spots = state.spots()
    private val byId = spots.keys.associateBy(FreeCellLayout::idOf)

    override val cardSize = Size(layout.cardWidth, layout.cardHeight)

    override val cards =
        spots.map { (card, spot) ->
            PlacedCard(FreeCellLayout.idOf(card), card, faceUp = true, layout.position(spot), spot.height)
        }

    override val slots: List<Slot<Move>> =
        List(GameState.FREE_CELL_COUNT) { Slot<Move>(layout.freeCell(it)) } +
            List(GameState.COLUMN_COUNT) { Slot(layout.columnBase(it)) } +
            Suit.entries.map { Slot(layout.foundation(it), SlotMark.OfSuit(it)) }

    override val header: Header
        get() {
            val lastCellEnd = layout.freeCell(GameState.FREE_CELL_COUNT - 1).x + layout.cardWidth
            val split = (lastCellEnd + layout.foundation(FoundationOrder.first()).x) / 2
            // Half way down the double gap under the top row.
            return Header(bottom = layout.columnsTop - layout.gap, dividers = listOf(split))
        }

    override fun tap(id: Int): Move? {
        val card = byId[id] ?: return null
        return when (val spot = spots[card]) {
            is Spot.InColumn -> state.tapMoveFromColumn(spot.column, state.columns[spot.column].size - spot.index)
            is Spot.InFreeCell -> state.tapMoveFromFreeCell(spot.cell)
            else -> null
        }
    }

    /** [id]'s card and, in a column, the cards on top of it too if together they form a run. */
    override fun pickUp(id: Int): List<Int> {
        val card = byId[id] ?: return emptyList()
        val cards =
            when (val spot = spots[card]) {
                is Spot.InColumn -> {
                    state.columns[spot.column]
                        .drop(spot.index)
                        .takeIf { it.isRun() }
                        .orEmpty()
                }

                is Spot.InFreeCell -> {
                    listOf(card)
                }

                // Only the card on top of a foundation can be taken back.
                is Spot.OnFoundation -> {
                    listOf(card).filter { state.foundationTop(spot.suit) == it }
                }

                null -> {
                    emptyList()
                }
            }
        return cards.map(FreeCellLayout::idOf)
    }

    override fun drop(
        held: List<Int>,
        centre: Offset,
    ): Move? {
        val from = held.firstOrNull()?.let(byId::get)?.let(spots::get)
        val over = layout.targetAt(centre)
        if (from == null || over == null) return null
        return moveFor(from, held.size, over)?.takeIf { state.canMove(it) }
    }

    override fun movedBy(move: Move): List<Int> = state.cardsMovedBy(move).map(FreeCellLayout::idOf)

    override fun landing(move: Move): Offset = layout.landing(state, move)
}

private val Spot.height: Float
    get() =
        when (this) {
            is Spot.InColumn -> index.toFloat()
            is Spot.InFreeCell -> 0f
            is Spot.OnFoundation -> height.toFloat()
        }

private val SlotGap = 4.dp
private val GroupSplit = 2.dp
