package com.maximx1.fads.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.canMove
import com.maximx1.fads.core.rules.cardsMovedBy
import com.maximx1.fads.core.rules.isRun
import com.maximx1.fads.core.rules.tapMoveFromColumn
import com.maximx1.fads.core.rules.tapMoveFromFreeCell
import com.maximx1.fads.core.state.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private typealias CardPosition = Animatable<Offset, AnimationVector2D>

/** How the board is painted: the cards, the outlines of the empty places, and the marks that point out a hint. */
class BoardPaint(
    val cards: CardStyle,
    val slots: Color = SlotColor,
    val hint: Color = HintColor,
)

@Composable
fun rememberPlainBoardPaint(): BoardPaint {
    val cards = rememberPlaceholderCardStyle()
    return remember(cards) { BoardPaint(cards) }
}

/** What is picked out on the board for the player: [dimmed] cards are drawn darker, and [hint] is pointed out. */
class BoardMarks(
    val dimmed: Set<Card> = emptySet(),
    val hint: Move? = null,
) {
    companion object {
        val None = BoardMarks()
    }
}

/**
 * The table: every card of [state] at its place. Cards glide to their new place whenever the state changes.
 * Tapping a card, or dragging a card or run and dropping it, calls [onMove] if the move is legal; an illegal
 * drop slides back to where it came from. A card on a foundation can be dragged back off it, but tapping it
 * does nothing.
 */
@Composable
fun Board(
    state: GameState,
    onMove: (Move) -> Unit,
    modifier: Modifier = Modifier,
    paint: BoardPaint = rememberPlainBoardPaint(),
    marks: BoardMarks = BoardMarks.None,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val layout =
            remember(width, height, density, state) {
                val gap = with(density) { slotGap(width, height).toPx() }
                BoardLayout(width, height, gap, state.columns.map { it.size })
            }
        val spots = remember(state) { state.spots() }
        // A hint left over from another position points at nothing.
        val hint = remember(state, marks.hint) { marks.hint?.takeIf { state.canMove(it) } }
        val hinted = remember(state, hint) { hint?.let { state.cardsMovedBy(it).toSet() }.orEmpty() }

        val scope = rememberCoroutineScope()
        val hand = remember(scope) { Hand(scope) }
        SideEffect { hand.table = Table(state, spots, layout, onMove) }

        Canvas(Modifier.fillMaxSize()) { drawSlots(layout, paint.slots) }

        val cardSize = with(density) { DpSize(layout.cardWidth.toDp(), layout.cardHeight.toDp()) }
        spots.forEach { (card, spot) ->
            key(card) {
                val target = layout.position(spot)
                val position = hand.positionOf(card, target)
                val held = card in hand.held
                val lift =
                    if (held) {
                        HELD_LIFT
                    } else if (position.isRunning) {
                        MOVING_LIFT
                    } else {
                        0f
                    }
                LaunchedEffect(target) { position.animateTo(target, MoveSpring) }
                Canvas(
                    Modifier
                        .zIndex(spot.height + lift)
                        .offset { position.value.round() }
                        .size(cardSize)
                        .then(if (card in marks.dimmed && !held) Dimmed else Modifier)
                        .testTag(card.code)
                        .gestures(card, hand),
                ) {
                    with(paint.cards) { drawCard(card) }
                    if (card in hinted) drawHintMark(paint.hint, filled = false)
                }
            }
        }
        if (hint != null) {
            // Above every card, since the place a card lands is usually half over the card it lands on.
            Canvas(
                Modifier
                    .zIndex(HINT_LIFT)
                    .offset { layout.landing(state, hint).round() }
                    .size(cardSize)
                    .testTag(HINT_TAG),
            ) { drawHintMark(paint.hint, filled = true) }
        }
    }
}

private fun Modifier.gestures(
    card: Card,
    hand: Hand,
): Modifier =
    pointerInput(card) { detectTapGestures(onTap = { hand.tap(card) }) }
        .pointerInput(card) {
            detectDragGestures(
                onDragStart = { hand.pickUp(card) },
                onDragEnd = hand::drop,
                onDragCancel = hand::drop,
                onDrag = { change, amount ->
                    change.consume()
                    hand.carry(amount)
                },
            )
        }

/** An outline the size of a card: round the cards a hint would move, and [filled] in where they would land. */
private fun DrawScope.drawHintMark(
    color: Color,
    filled: Boolean,
) {
    val corner = CornerRadius(size.width * SLOT_CORNER)
    if (filled) drawRoundRect(color.copy(alpha = HINT_FILL), cornerRadius = corner)
    drawRoundRect(color, cornerRadius = corner, style = Stroke(size.width * HINT_LINE))
}

/** Everything a gesture needs to know about the board at the moment it happens. */
private class Table(
    val state: GameState,
    val spots: Map<Card, Spot>,
    val layout: BoardLayout,
    val onMove: (Move) -> Unit,
)

/** The player's hand: turns taps and drags on cards into moves, and tracks the cards being dragged. */
private class Hand(
    private val scope: CoroutineScope,
) {
    var table: Table? = null

    /** The cards being dragged, lowest first. Empty when nothing is held. */
    var held: List<Card> by mutableStateOf(emptyList())
        private set

    private val positions = HashMap<Card, CardPosition>()

    fun positionOf(
        card: Card,
        start: Offset,
    ): CardPosition = positions.getOrPut(card) { Animatable(start, Offset.VectorConverter) }

    fun tap(card: Card) {
        val table = table ?: return
        val move =
            when (val spot = table.spots[card]) {
                is Spot.InColumn -> {
                    table.state.tapMoveFromColumn(spot.column, table.state.columns[spot.column].size - spot.index)
                }

                is Spot.InFreeCell -> {
                    table.state.tapMoveFromFreeCell(spot.cell)
                }

                else -> {
                    null
                }
            }
        move?.let(table.onMove)
    }

    /** Picks up [card] and, in a column, the cards on top of it too if together they form a run. */
    fun pickUp(card: Card) {
        val table = table ?: return
        held =
            when (val spot = table.spots[card]) {
                is Spot.InColumn -> {
                    table.state.columns[spot.column]
                        .drop(spot.index)
                        .takeIf { it.isRun() }
                        .orEmpty()
                }

                is Spot.InFreeCell -> {
                    listOf(card)
                }

                // Only the card on top of a foundation can be taken back.
                is Spot.OnFoundation -> {
                    listOf(card).filter { table.state.foundationTop(spot.suit) == it }
                }

                null -> {
                    emptyList()
                }
            }
    }

    fun carry(amount: Offset) {
        for (card in held) {
            val position = positions[card] ?: continue
            // Undispatched so the card has moved by the time the next touch event arrives.
            scope.launch(start = CoroutineStart.UNDISPATCHED) { position.snapTo(position.value + amount) }
        }
    }

    /** Lets go. Plays the move if the lowest held card is over a legal place; otherwise the cards slide back. */
    fun drop() {
        val table = table
        val cards = held
        val lead = cards.firstOrNull()
        val from = table?.spots?.get(lead)
        val over = positions[lead]?.let { table?.layout?.targetAt(it.value + table.layout.cardCentre) }
        held = emptyList()
        if (table != null && from != null && over != null) {
            moveFor(from, cards.size, over)?.takeIf { table.state.canMove(it) }?.let(table.onMove)
        }
        // Head home. If a move was played, the board retargets the cards to their new place a moment later.
        for (card in cards) {
            val home = table?.spots?.get(card)?.let { table.layout.position(it) } ?: continue
            scope.launch { positions[card]?.animateTo(home, MoveSpring) }
        }
    }
}

private val Spot.height: Float
    get() =
        when (this) {
            is Spot.InColumn -> index.toFloat()
            is Spot.InFreeCell -> 0f
            is Spot.OnFoundation -> height.toFloat()
        }

private fun DrawScope.drawSlots(
    layout: BoardLayout,
    color: Color,
) {
    val size = Size(layout.cardWidth, layout.cardHeight)
    val corner = CornerRadius(layout.cardWidth * SLOT_CORNER)
    val line = Stroke(layout.cardWidth * SLOT_LINE)

    fun slot(topLeft: Offset) = drawRoundRect(color, topLeft, size, corner, line)

    repeat(GameState.FREE_CELL_COUNT) { slot(layout.freeCell(it)) }
    repeat(GameState.COLUMN_COUNT) { slot(layout.columnBase(it)) }
    for (suit in Suit.entries) {
        val topLeft = layout.foundation(suit)
        slot(topLeft)
        val mark = layout.cardWidth * SLOT_MARK
        val inset = Offset((layout.cardWidth - mark) / 2, (layout.cardHeight - mark) / 2)
        drawSuit(suit, color, topLeft + inset, mark)
    }
}

private val SlotColor = Color(0x40FFFFFF)
private val HintColor = Color(0xFFFFE082)

// Darkens whatever a card style paints without touching its shape.
private val Dimmed = Modifier.graphicsLayer { colorFilter = ColorFilter.lighting(Color(0xFFB4B4B4), Color.Black) }
private val MoveSpring = spring<Offset>(stiffness = Spring.StiffnessMediumLow)

private const val SLOT_CORNER = 0.09f
private const val SLOT_LINE = 0.02f
private const val SLOT_MARK = 0.5f
private const val HELD_LIFT = 200f
private const val MOVING_LIFT = 100f
private const val HINT_LIFT = 300f
private const val HINT_LINE = 0.05f
private const val HINT_FILL = 0.3f

const val HINT_TAG = "hint"
