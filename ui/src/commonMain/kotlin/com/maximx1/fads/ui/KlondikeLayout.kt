package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.klondike.KlondikeMove
import com.maximx1.fads.core.klondike.KlondikeState
import com.maximx1.fads.core.klondike.KlondikeState.Companion.PILE_COUNT
import com.maximx1.fads.core.klondike.canMove
import com.maximx1.fads.core.klondike.cardsMovedBy
import com.maximx1.fads.core.klondike.legalMoves
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.isRun
import kotlin.math.min

/**
 * Klondike on the screen. Along the top: the stock, the waste (fanned three wide when drawing three), and the four
 * foundations. Below: the seven piles, face-down cards packed tighter than face-up ones. Each card's id is its
 * place in [Card.deck].
 */
object KlondikeLayout : TableLayout<KlondikeState, KlondikeMove> {
    override fun arrange(
        state: KlondikeState,
        width: Float,
        height: Float,
        density: Density,
    ): Arrangement<KlondikeMove> =
        KlondikeArrangement(state, KlondikeGeometry(state, width, height, with(density) { Gap.toPx() }))

    /** Face-up cards on the table that no legal move would move. The stock and waste are never dimmed. */
    override fun stuck(state: KlondikeState): Set<Int> {
        val movable = state.legalMoves().flatMapTo(HashSet()) { state.cardsMovedBy(it) }
        return state.piles.indices
            .flatMap { state.showing(it) }
            .filterNot { it in movable }
            .mapTo(HashSet(), ::idOf)
    }

    fun idOf(card: Card): Int = card.suit.ordinal * RANKS + card.rank.ordinal
}

/** Where everything on a Klondike table goes, in pixels, for one position at one size. */
private class KlondikeGeometry(
    private val state: KlondikeState,
    width: Float,
    height: Float,
    val gap: Float,
) {
    val cardWidth =
        min((width - gap * (PILE_COUNT + 1)) / PILE_COUNT, (height - gap * ROW_GAPS) / HEIGHT_IN_CARDS / ASPECT)
            .coerceAtLeast(1f)
    val cardHeight = cardWidth * ASPECT
    private val left = (width - cardWidth * PILE_COUNT - gap * (PILE_COUNT - 1)) / 2
    val pilesTop = gap + cardHeight + gap * 2
    private val pileRoom = (height - gap - pilesTop - cardHeight).coerceAtLeast(0f)

    fun slotLeft(index: Int) = left + index * (cardWidth + gap)

    val stockAt = Offset(slotLeft(0), gap)
    val wasteSlot = Offset(slotLeft(1), gap)

    fun foundationAt(suit: Suit) = Offset(slotLeft(FOUNDATION_START + FoundationOrder.indexOf(suit)), gap)

    /** Where the waste card [index] from the bottom sits: the top three fan out to the right when drawing three. */
    fun wasteAt(index: Int): Offset {
        val fanned = if (state.draw == 1) 0 else (index - (state.waste.size - FANNED)).coerceAtLeast(0)
        return wasteSlot + Offset(fanned * cardWidth * WASTE_FAN, 0f)
    }

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
            pilesTop + min(index, down) * cardHeight * HIDDEN_FAN * squeeze +
                (index - down).coerceAtLeast(0) * cardHeight * SHOWING_FAN * squeeze
        return Offset(slotLeft(pile), y)
    }

    /** The column under [point], or `null` if it is off the sides. */
    fun columnAt(point: Offset): Int? {
        val column = ((point.x - left + gap / 2) / (cardWidth + gap)).toInt()
        return column.takeIf { point.x >= left - gap / 2 && it in 0 until PILE_COUNT }
    }

    fun isOnTopRow(point: Offset) = point.y < pilesTop - gap
}

private class KlondikeArrangement(
    private val state: KlondikeState,
    private val at: KlondikeGeometry,
) : Arrangement<KlondikeMove> {
    override val cardSize = Size(at.cardWidth, at.cardHeight)

    override val cards: List<PlacedCard> =
        buildList {
            state.stock.forEachIndexed { index, card -> add(placed(card, false, at.stockAt, index)) }
            state.waste.forEachIndexed { index, card -> add(placed(card, true, at.wasteAt(index), index)) }
            for (suit in Suit.entries) {
                repeat(
                    state.foundationCount(suit),
                ) { add(placed(Card.deck[suit.ordinal * RANKS + it], true, at.foundationAt(suit), it)) }
            }
            state.piles.forEachIndexed { pile, cards ->
                cards.forEachIndexed { index, card ->
                    add(placed(card, index >= state.hidden[pile], at.pileCardAt(pile, index), index))
                }
            }
        }

    private fun placed(
        card: Card,
        faceUp: Boolean,
        at: Offset,
        height: Int,
    ) = PlacedCard(KlondikeLayout.idOf(card), card, faceUp, at, height.toFloat())

    override val slots: List<Slot<KlondikeMove>> =
        listOf(
            Slot<KlondikeMove>(at.stockAt, SlotMark.Recycle, KlondikeMove.Recycle.takeIf { state.canMove(it) }),
            Slot<KlondikeMove>(at.wasteSlot),
        ) +
            Suit.entries.map { Slot<KlondikeMove>(at.foundationAt(it), SlotMark.OfSuit(it)) } +
            List(PILE_COUNT) { Slot<KlondikeMove>(Offset(at.slotLeft(it), at.pilesTop)) }

    override val header: Header
        get() {
            val split = (at.slotLeft(FOUNDATION_START - 1) + at.cardWidth + at.slotLeft(FOUNDATION_START)) / 2
            return Header(bottom = at.pilesTop - at.gap, dividers = listOf(split))
        }

    private val ids: Map<Int, Card> = cards.associate { it.id to it.card }

    private fun pileOf(card: Card): Int? = state.piles.indexOfFirst { card in it }.takeIf { it >= 0 }

    override fun tap(id: Int): KlondikeMove? {
        val card = ids[id] ?: return null
        val pile = pileOf(card)
        return when {
            card in state.stock -> {
                KlondikeMove.Draw
            }

            card == state.waste.lastOrNull() -> {
                (listOf(KlondikeMove.WasteToFoundation) + pileTargets().map { KlondikeMove.WasteToPile(it) })
                    .firstOrNull { state.canMove(it) }
            }

            pile != null -> {
                val count = state.piles[pile].size - state.piles[pile].indexOf(card)
                val toFoundation = KlondikeMove.PileToFoundation(pile).takeIf { count == 1 }
                (listOfNotNull(toFoundation) + pileTargets().map { KlondikeMove.PileToPile(pile, it, count) })
                    .firstOrNull { state.canMove(it) }
            }

            else -> {
                null
            }
        }
    }

    /** Piles to try a card on: those with cards first, so a king only goes to an empty pile if nothing else fits. */
    private fun pileTargets(): List<Int> = (0 until PILE_COUNT).sortedBy { state.piles[it].isEmpty() }

    override fun pickUp(id: Int): List<Int> {
        val card = ids[id] ?: return emptyList()
        val pile = pileOf(card)
        val cards =
            when {
                card == state.waste.lastOrNull() -> {
                    listOf(card)
                }

                state.foundationTop(card.suit) == card -> {
                    listOf(card)
                }

                pile != null -> {
                    val index = state.piles[pile].indexOf(card)
                    if (index <
                        state.hidden[pile]
                    ) {
                        emptyList()
                    } else {
                        state.piles[pile]
                            .drop(index)
                            .takeIf { it.isRun() }
                            .orEmpty()
                    }
                }

                else -> {
                    emptyList()
                }
            }
        return cards.map(KlondikeLayout::idOf)
    }

    override fun drop(
        held: List<Int>,
        centre: Offset,
    ): KlondikeMove? {
        val card = held.firstOrNull()?.let(ids::get)
        val column = at.columnAt(centre)
        val move =
            when {
                card == null || column == null -> null
                at.isOnTopRow(centre) -> toFoundation(card).takeIf { column >= FOUNDATION_START && held.size == 1 }
                else -> toPile(card, column, held.size)
            }
        return move?.takeIf { state.canMove(it) }
    }

    /** The move that puts [card] up on its foundation, wherever it is. */
    private fun toFoundation(card: Card): KlondikeMove? {
        val pile = pileOf(card)
        return when {
            card == state.waste.lastOrNull() -> KlondikeMove.WasteToFoundation
            pile != null -> KlondikeMove.PileToFoundation(pile)
            else -> null
        }
    }

    /** The move that puts [card], and the [count] - 1 cards on top of it, onto pile [to], wherever it is. */
    private fun toPile(
        card: Card,
        to: Int,
        count: Int,
    ): KlondikeMove? {
        val pile = pileOf(card)
        return when {
            card == state.waste.lastOrNull() -> KlondikeMove.WasteToPile(to)
            pile != null -> KlondikeMove.PileToPile(pile, to, count)
            state.foundationTop(card.suit) == card -> KlondikeMove.FoundationToPile(card.suit, to)
            else -> null
        }
    }

    override fun movedBy(move: KlondikeMove): List<Int> =
        when (move) {
            // A hint to draw points at the stock itself.
            KlondikeMove.Draw -> listOfNotNull(state.stock.lastOrNull()).map(KlondikeLayout::idOf)

            else -> state.cardsMovedBy(move).map(KlondikeLayout::idOf)
        }

    override fun landing(move: KlondikeMove): Offset =
        when (move) {
            KlondikeMove.Draw -> at.wasteSlot
            KlondikeMove.Recycle -> at.stockAt
            KlondikeMove.WasteToFoundation -> at.foundationAt(state.waste.last().suit)
            is KlondikeMove.PileToFoundation -> at.foundationAt(state.piles[move.from].last().suit)
            is KlondikeMove.WasteToPile -> at.pileCardAt(move.to, state.piles[move.to].size)
            is KlondikeMove.PileToPile -> at.pileCardAt(move.to, state.piles[move.to].size)
            is KlondikeMove.FoundationToPile -> at.pileCardAt(move.to, state.piles[move.to].size)
        }
}

private val Gap = 4.dp

private const val RANKS = 13
private const val ASPECT = 1.4f
private const val HIDDEN_FAN = 0.12f
private const val SHOWING_FAN = 0.3f
private const val WASTE_FAN = 0.3f
private const val FANNED = 3
private const val FOUNDATION_START = 3

// Height the table asks for: the top row and a full pile of six face down under a run of seven, in card heights.
private const val HEIGHT_IN_CARDS = 4.6f
private const val ROW_GAPS = 4
