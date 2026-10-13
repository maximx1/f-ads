package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.cardsMovedBy
import com.maximx1.fads.core.state.GameState
import kotlin.math.floor
import kotlin.math.min

/** Where a card currently sits. */
sealed interface Spot {
    data class InColumn(
        val column: Int,
        val index: Int,
    ) : Spot

    data class InFreeCell(
        val cell: Int,
    ) : Spot

    data class OnFoundation(
        val suit: Suit,
        val height: Int,
    ) : Spot
}

/** Somewhere a dragged card can be dropped. Any foundation slot stands for all of them: the suit decides. */
sealed interface DropTarget {
    data class Column(
        val index: Int,
    ) : DropTarget

    data class FreeCell(
        val index: Int,
    ) : DropTarget

    data object Foundation : DropTarget
}

fun GameState.spots(): Map<Card, Spot> =
    buildMap {
        columns.forEachIndexed { column, cards ->
            cards.forEachIndexed { index, card -> put(card, Spot.InColumn(column, index)) }
        }
        freeCells.forEachIndexed { cell, card -> if (card != null) put(card, Spot.InFreeCell(cell)) }
        for (suit in Suit.entries) {
            repeat(foundationCount(suit)) { put(Card.deck[suit.ordinal * RANKS + it], Spot.OnFoundation(suit, it)) }
        }
    }

/** The move that takes [count] cards from [from] to [to], or `null` if the game has no such move. */
fun moveFor(
    from: Spot,
    count: Int,
    to: DropTarget,
): Move? =
    when (from) {
        is Spot.InColumn -> {
            when (to) {
                is DropTarget.Column -> Move.ColumnToColumn(from.column, to.index, count)
                is DropTarget.FreeCell -> Move.ColumnToFreeCell(from.column, to.index).takeIf { count == 1 }
                DropTarget.Foundation -> Move.ColumnToFoundation(from.column).takeIf { count == 1 }
            }
        }

        is Spot.InFreeCell -> {
            when (to) {
                is DropTarget.Column -> Move.FreeCellToColumn(from.cell, to.index)
                is DropTarget.FreeCell -> null
                DropTarget.Foundation -> Move.FreeCellToFoundation(from.cell)
            }
        }

        is Spot.OnFoundation -> {
            when (to) {
                is DropTarget.Column -> Move.FoundationToColumn(from.suit, to.index)
                is DropTarget.FreeCell -> Move.FoundationToFreeCell(from.suit, to.index)
                DropTarget.Foundation -> null
            }
        }
    }

/** Where the first card of [move] comes to rest. The move must be legal in [state]. */
fun BoardLayout.landing(
    state: GameState,
    move: Move,
): Offset =
    when (move) {
        is Move.ColumnToColumn -> position(Spot.InColumn(move.to, state.columns[move.to].size))
        is Move.FreeCellToColumn -> position(Spot.InColumn(move.to, state.columns[move.to].size))
        is Move.FoundationToColumn -> position(Spot.InColumn(move.to, state.columns[move.to].size))
        is Move.ColumnToFreeCell -> freeCell(move.cell)
        is Move.FoundationToFreeCell -> freeCell(move.cell)
        is Move.ColumnToFoundation -> foundation(state.cardsMovedBy(move).first().suit)
        is Move.FreeCellToFoundation -> foundation(state.cardsMovedBy(move).first().suit)
    }

/**
 * The space between neighbouring cards for a board of this size. An upright board gets a little more, so the
 * free cells and the foundations do not sit quite so close together.
 */
fun slotGap(
    width: Float,
    height: Float,
): Dp = if (height > width) UprightGap else SidewaysGap

private val UprightGap = 6.dp
private val SidewaysGap = 4.dp

/**
 * The geometry of the board, in pixels, for a given area. Free cells and foundations share the top row; the
 * eight columns hang below, each lined up under one slot of the top row.
 *
 * The cards are as wide as the area allows, unless that would leave too little height for the columns (a phone
 * on its side), in which case they shrink and the board is centred. Long columns squeeze their cards together
 * rather than run off the bottom.
 */
class BoardLayout(
    width: Float,
    height: Float,
    private val gap: Float,
    private val columnSizes: List<Int>,
) {
    val cardWidth: Float =
        min(
            (width - gap * (SLOTS + 1)) / SLOTS,
            (height - gap * ROW_GAPS) / HEIGHT_IN_CARDS / CARD_ASPECT,
        ).coerceAtLeast(1f)
    val cardHeight: Float = cardWidth * CARD_ASPECT

    private val left = (width - cardWidth * SLOTS - gap * (SLOTS - 1)) / 2
    private val columnsTop = gap + cardHeight + gap * 2
    private val columnRoom = (height - gap - columnsTop - cardHeight).coerceAtLeast(0f)

    val cardCentre: Offset get() = Offset(cardWidth / 2, cardHeight / 2)

    fun freeCell(index: Int): Offset = Offset(slotLeft(index), gap)

    fun foundation(suit: Suit): Offset =
        Offset(slotLeft(GameState.FREE_CELL_COUNT + FoundationOrder.indexOf(suit)), gap)

    fun columnBase(column: Int): Offset = Offset(slotLeft(column), columnsTop)

    fun position(spot: Spot): Offset =
        when (spot) {
            is Spot.InColumn -> columnBase(spot.column).let { it.copy(y = it.y + spot.index * fan(spot.column)) }
            is Spot.InFreeCell -> freeCell(spot.cell)
            is Spot.OnFoundation -> foundation(spot.suit)
        }

    /** What lies under [point], or `null` if it is off the sides of the board. */
    fun targetAt(point: Offset): DropTarget? {
        val slot = floor((point.x - left + gap / 2) / (cardWidth + gap)).toInt()
        return when {
            slot !in 0 until SLOTS -> null
            point.y >= columnsTop - gap -> DropTarget.Column(slot)
            slot < GameState.FREE_CELL_COUNT -> DropTarget.FreeCell(slot)
            else -> DropTarget.Foundation
        }
    }

    private fun slotLeft(index: Int): Float = left + index * (cardWidth + gap)

    /** How far each card in a column sits below the one before. */
    private fun fan(column: Int): Float {
        val steps = columnSizes[column] - 1
        return if (steps < 1) 0f else min(cardHeight * FAN, columnRoom / steps)
    }

    private companion object {
        const val SLOTS = GameState.COLUMN_COUNT
        const val CARD_ASPECT = 1.4f
        const val FAN = 0.34f

        // Height the board asks for: the top row, plus a freshly dealt seven-card column, in card heights.
        const val HEIGHT_IN_CARDS = 4.1f
        const val ROW_GAPS = 4
    }
}

/**
 * The foundations, left to right. This is only where each suit's pile is shown: it is separate from the order of
 * [Suit] itself, which numbered deals depend on.
 */
val FoundationOrder = listOf(Suit.SPADES, Suit.CLUBS, Suit.HEARTS, Suit.DIAMONDS)

private const val RANKS = 13
