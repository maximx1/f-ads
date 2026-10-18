package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.spider.Piece
import com.maximx1.fads.core.spider.SpiderMove
import com.maximx1.fads.core.spider.SpiderState
import com.maximx1.fads.core.spider.SpiderState.Companion.PILE_COUNT
import com.maximx1.fads.core.spider.canMove
import com.maximx1.fads.core.spider.cardsMovedBy
import com.maximx1.fads.core.spider.isSuitRun
import com.maximx1.fads.core.spider.legalMoves
import kotlin.math.min

/**
 * Spider on the screen: the ten piles across the top, face-down cards packed tighter than face-up ones, and along
 * the bottom the finished runs on the left and the stock on the right, one face-down stack for each row left to
 * deal. Each card's id is its [Piece.id], and its test tag its code and id, since there are copies of every card.
 */
object SpiderLayout : TableLayout<SpiderState, SpiderMove> {
    override fun arrange(
        state: SpiderState,
        width: Float,
        height: Float,
        density: Density,
    ): Arrangement<SpiderMove> =
        SpiderArrangement(state, SpiderGeometry(state, width, height, with(density) { Gap.toPx() }))

    /** Face-up cards on the piles that no legal move would move. */
    override fun stuck(state: SpiderState): Set<Int> {
        val movable = state.legalMoves().flatMapTo(HashSet()) { move -> state.cardsMovedBy(move).map { it.id } }
        return state.piles.indices
            .flatMap { state.showing(it) }
            .map { it.id }
            .filterNotTo(HashSet()) { it in movable }
    }

    fun tagOf(piece: Piece) = "${piece.card.code}#${piece.id}"
}

/** Where everything on a Spider table goes, in pixels, for one position at one size. */
private class SpiderGeometry(
    private val state: SpiderState,
    width: Float,
    height: Float,
    private val gap: Float,
) {
    val cardWidth =
        min((width - gap * (PILE_COUNT + 1)) / PILE_COUNT, (height - gap * ROW_GAPS) / HEIGHT_IN_CARDS / ASPECT)
            .coerceAtLeast(1f)
    val cardHeight = cardWidth * ASPECT
    private val left = (width - cardWidth * PILE_COUNT - gap * (PILE_COUNT - 1)) / 2
    private val bottomRow = height - gap - cardHeight
    private val pileRoom = (bottomRow - gap * 2 - gap - cardHeight).coerceAtLeast(0f)
    private val stackStep = cardWidth * STACK_STEP

    fun pileAt(index: Int) = Offset(left + index * (cardWidth + gap), gap)

    fun pileCardAt(
        pile: Int,
        index: Int,
    ): Offset {
        // Face-down cards sit closer together than face-up ones, and both squeeze up if the pile is long.
        val down = state.hidden[pile]
        val up = (state.piles[pile].size - down - 1).coerceAtLeast(0)
        val wanted = down * cardHeight * HIDDEN_FAN + up * cardHeight * SHOWING_FAN
        val squeeze = if (wanted > pileRoom && wanted > 0) pileRoom / wanted else 1f
        val y =
            min(index, down) * cardHeight * HIDDEN_FAN * squeeze +
                (index - down).coerceAtLeast(0) * cardHeight * SHOWING_FAN * squeeze
        return pileAt(pile) + Offset(0f, y)
    }

    /** The stack for row [row] of the stock, counted from the one dealt last, so the next to deal is rightmost. */
    fun stockAt(row: Int) = Offset(pileAt(PILE_COUNT - 1).x - row * stackStep, bottomRow)

    fun finishedAt(run: Int) = Offset(pileAt(0).x + run * stackStep, bottomRow)

    /** The pile under [point], or `null` if it is off the sides or down among the stock. */
    fun pileUnder(point: Offset): Int? {
        val column = ((point.x - left + gap / 2) / (cardWidth + gap)).toInt()
        return column.takeIf { point.x >= left - gap / 2 && it in 0 until PILE_COUNT && point.y < bottomRow }
    }
}

private class SpiderArrangement(
    private val state: SpiderState,
    private val at: SpiderGeometry,
) : Arrangement<SpiderMove> {
    override val cardSize = Size(at.cardWidth, at.cardHeight)

    override val cards: List<PlacedCard> =
        buildList {
            state.stock.forEachIndexed {
                index,
                piece,
                ->
                add(placed(piece, false, at.stockAt(index / PILE_COUNT), index))
            }
            state.finished.forEachIndexed { run, pieces ->
                // The king on top.
                pieces.forEachIndexed { index, piece ->
                    add(
                        placed(
                            piece,
                            true,
                            at.finishedAt(run),
                            run * RANKS + RANKS - index,
                        ),
                    )
                }
            }
            state.piles.forEachIndexed { pile, pieces ->
                pieces.forEachIndexed { index, piece ->
                    add(placed(piece, index >= state.hidden[pile], at.pileCardAt(pile, index), index))
                }
            }
        }

    private fun placed(
        piece: Piece,
        faceUp: Boolean,
        at: Offset,
        height: Int,
    ) = PlacedCard(piece.id, piece.card, faceUp, at, height.toFloat(), SpiderLayout.tagOf(piece))

    override val slots: List<Slot<SpiderMove>> = List(PILE_COUNT) { Slot<SpiderMove>(at.pileAt(it)) }

    private val pieces: Map<Int, Piece> = (state.piles.flatten() + state.stock).associateBy { it.id }

    private fun pileOf(id: Int): Int? = state.piles.indexOfFirst { pile -> pile.any { it.id == id } }.takeIf { it >= 0 }

    /** How many cards from [id]'s card to the top of its pile, if they are a run that can be picked up. */
    private fun runFrom(id: Int): Int? {
        val pile = pileOf(id) ?: return null
        val index = state.piles[pile].indexOfFirst { it.id == id }
        val run = state.piles[pile].drop(index)
        return run.size.takeIf { index >= state.hidden[pile] && run.isSuitRun() }
    }

    override fun tap(id: Int): SpiderMove? {
        val from = pileOf(id)
        val count = runFrom(id)
        return when {
            state.stock.any { it.id == id } -> {
                SpiderMove.DealRow.takeIf { state.canMove(it) }
            }

            from == null || count == null -> {
                null
            }

            else -> {
                targetsFor(
                    pieces.getValue(id),
                ).map { SpiderMove.PileToPile(from, it, count) }.firstOrNull { state.canMove(it) }
            }
        }
    }

    /** Piles to try [lead] on: its own suit first, then any card one higher, then an empty pile. */
    private fun targetsFor(lead: Piece): List<Int> =
        (0 until PILE_COUNT).sortedBy { to ->
            val top = state.piles[to].lastOrNull()?.card
            when {
                top == null -> 2
                top.suit == lead.card.suit -> 0
                else -> 1
            }
        }

    override fun pickUp(id: Int): List<Int> {
        val pile = pileOf(id)
        val count = runFrom(id)
        return if (pile == null || count == null) emptyList() else state.piles[pile].takeLast(count).map { it.id }
    }

    override fun drop(
        held: List<Int>,
        centre: Offset,
    ): SpiderMove? {
        val from = held.firstOrNull()?.let(::pileOf)
        val to = at.pileUnder(centre)
        if (from == null || to == null) return null
        return SpiderMove.PileToPile(from, to, held.size).takeIf { state.canMove(it) }
    }

    override fun movedBy(move: SpiderMove): List<Int> =
        when (move) {
            // A hint to deal points at the stock itself.
            SpiderMove.DealRow -> listOfNotNull(state.stock.lastOrNull()?.id)

            else -> state.cardsMovedBy(move).map { it.id }
        }

    override fun landing(move: SpiderMove): Offset =
        when (move) {
            SpiderMove.DealRow -> at.pileCardAt(0, state.piles[0].size)
            is SpiderMove.PileToPile -> at.pileCardAt(move.to, state.piles[move.to].size)
        }
}

private val Gap = 3.dp

private const val RANKS = 13
private const val ASPECT = 1.4f
private const val HIDDEN_FAN = 0.1f
private const val SHOWING_FAN = 0.28f
private const val STACK_STEP = 0.18f

// Height the table asks for: a pile of five face down under a run of eight, and the stock along the bottom.
private const val HEIGHT_IN_CARDS = 5.5f
private const val ROW_GAPS = 4
