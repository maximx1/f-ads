package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.war.Battle
import com.maximx1.fads.core.war.Side
import com.maximx1.fads.core.war.WarMove
import com.maximx1.fads.core.war.WarState
import com.maximx1.fads.core.war.canMove
import kotlin.math.min

/**
 * War on the screen: the computer's pile at the top and yours at the bottom, each face down with how many cards
 * it holds beside it, and between them the last battle, the computer's cards over yours, with who took it. A tap
 * anywhere on the cards or the places turns the next ones over. Each card's id is its place in [Card.deck].
 */
object WarLayout : TableLayout<WarState, WarMove> {
    override fun arrange(
        state: WarState,
        width: Float,
        height: Float,
        density: Density,
    ): Arrangement<WarMove> =
        WarArrangement(
            state,
            WarGeometry(width, height, with(density) { Gap.toPx() }, with(density) { Band.toPx() }),
        )

    fun idOf(card: Card) = ids.getValue(card)

    private val ids = Card.deck.withIndex().associate { (index, card) -> card to index }

    const val YOUR_COUNT_TAG = "war-yours"
    const val THEIR_COUNT_TAG = "war-theirs"
    const val OUTCOME_TAG = "war-outcome"
}

/** Where everything on a War table goes, in pixels, at one size. */
private class WarGeometry(
    private val width: Float,
    height: Float,
    private val gap: Float,
    private val band: Float,
) {
    val cardWidth =
        min(min(width / WIDTH_IN_CARDS, (height - gap * ROW_GAPS - band) / ROWS / ASPECT), width * MAX_SHARE)
            .coerceAtLeast(1f)
    val cardHeight = cardWidth * ASPECT
    private val pileX = (width - cardWidth) / 2
    private val top = (height - (cardHeight * ROWS + gap * ROW_GAPS + band)) / 2
    private val theirBattleY = top + cardHeight + gap * 2
    private val yourBattleY = theirBattleY + cardHeight + band
    private val step = cardWidth * STACK_STEP

    val theirPile = Offset(pileX, top)
    val yourPile = Offset(pileX, yourBattleY + cardHeight + gap * 2)

    fun pileOf(side: Side) = if (side == Side.YOU) yourPile else theirPile

    /** Card [index] of a pile of [size], counted from the top, drawn a little up and left of the ones under it. */
    fun pileCardAt(
        side: Side,
        index: Int,
        size: Int,
    ): Offset {
        val depth = (size - 1 - index) / CARDS_PER_STEP
        return pileOf(side) - Offset(depth * step, depth * step)
    }

    /** Where the count of a pile is written: beside it, at its right. */
    fun countAt(side: Side) = pileOf(side) + Offset(cardWidth + gap * 2 + band / 2, cardHeight / 2)

    /** The middle of the band between the two rows of the battle, where who took it is written. */
    val outcomeAt = Offset(width / 2, theirBattleY + cardHeight + band / 2)

    /**
     * Card [index] of the [laid] cards [side] laid in a battle in which the longer side laid [longest]. Each row
     * is centred, and fanned more tightly the longer the war.
     */
    fun battleCardAt(
        side: Side,
        index: Int,
        longest: Int,
    ): Offset {
        val room = width - gap * 2 - cardWidth
        val fan = if (longest > 1) min(cardWidth * BATTLE_FAN, room / (longest - 1)) else 0f
        val left = (width - cardWidth - fan * (longest - 1)) / 2
        return Offset(left + index * fan, if (side == Side.YOU) yourBattleY else theirBattleY)
    }

    /** Where a battle of one card each puts [side]'s card. */
    fun battleAt(side: Side) = battleCardAt(side, 0, 1)
}

private class WarArrangement(
    private val state: WarState,
    private val at: WarGeometry,
) : Arrangement<WarMove> {
    private val flip = WarMove.Flip.takeIf { state.canMove(it) }

    override val cardSize = Size(at.cardWidth, at.cardHeight)

    override val cards: List<PlacedCard> =
        buildList {
            for (side in Side.entries) {
                val pile = state.pile(side)
                pile.forEachIndexed { index, card ->
                    add(placed(card, false, at.pileCardAt(side, index, pile.size), pile.size - index))
                }
            }
            val battle = state.battle ?: return@buildList
            val longest = maxOf(battle.yours.size, battle.theirs.size)
            for (side in Side.entries) {
                val laid = battle.laidBy(side)
                laid.forEachIndexed { index, card ->
                    add(placed(card, Battle.isFaceUp(index, laid), at.battleCardAt(side, index, longest), index))
                }
            }
        }

    private fun placed(
        card: Card,
        faceUp: Boolean,
        at: Offset,
        height: Int,
    ) = PlacedCard(WarLayout.idOf(card), card, faceUp, at, height.toFloat())

    override val slots: List<Slot<WarMove>> =
        listOf(at.theirPile, at.battleAt(Side.THEM), at.battleAt(Side.YOU), at.yourPile).map { Slot(it, tap = flip) }

    override val labels: List<TableLabel> =
        listOf(
            TableLabel("${state.theirs.size}", at.countAt(Side.THEM), WarLayout.THEIR_COUNT_TAG),
            TableLabel("${state.yours.size}", at.countAt(Side.YOU), WarLayout.YOUR_COUNT_TAG),
            TableLabel(outcome(), at.outcomeAt, WarLayout.OUTCOME_TAG),
        )

    private fun outcome(): String {
        val battle = state.battle
        return when {
            state.isLost -> Strings.WAR_LOST
            battle == null -> Strings.WAR_FIRST_FLIP
            else -> Strings.warTaken(battle.winner == Side.YOU, battle.cards.size, battle.wasWar)
        }
    }

    override fun tap(id: Int): WarMove? = flip

    // Nothing is ever picked up: the cards turn over by themselves.
    override fun pickUp(id: Int): List<Int> = emptyList()

    override fun drop(
        held: List<Int>,
        centre: Offset,
    ): WarMove? = null

    override fun movedBy(move: WarMove): List<Int> = listOfNotNull(state.yours.firstOrNull()?.let(WarLayout::idOf))

    override fun landing(move: WarMove): Offset = at.battleAt(Side.YOU)
}

private val Gap = 6.dp

// Room between the two rows of the battle for who took it.
private val Band = 36.dp

private const val ASPECT = 1.4f
private const val ROWS = 4
private const val ROW_GAPS = 6
private const val STACK_STEP = 0.012f
private const val CARDS_PER_STEP = 2
private const val BATTLE_FAN = 0.32f

// A war of five cards a side fits across, and no card takes more than this share of the width.
private const val WIDTH_IN_CARDS = 2.6f
private const val MAX_SHARE = 0.3f
