package com.maximx1.fads.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.canMove
import com.maximx1.fads.core.state.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private typealias CardPosition = Animatable<Offset, AnimationVector2D>

/**
 * How the board is painted: the cards, the outlines of the empty places, and the marks that point out a hint.
 * [pane], if there is one, is painted under all of them, across the whole board, given the board's top row if it
 * has one.
 */
class BoardPaint(
    val cards: CardStyle,
    val slots: Color = SlotColor,
    val hint: Color = HintColor,
    val pane: (DrawScope.(Header?) -> Unit)? = null,
)

@Composable
fun rememberPlainBoardPaint(): BoardPaint {
    val cards = rememberPlaceholderCardStyle()
    return remember(cards) { BoardPaint(cards) }
}

/** What is picked out on a FreeCell board: [dimmed] cards are drawn darker, and [hint] is pointed out. */
class BoardMarks(
    val dimmed: Set<Card> = emptySet(),
    val hint: Move? = null,
) {
    companion object {
        val None = BoardMarks()
    }
}

/**
 * A FreeCell table: every card of [state] at its place. Cards glide to their new place whenever the state
 * changes. Tapping a card, or dragging a card or run and dropping it, calls [onMove] if the move is legal; an
 * illegal drop slides back to where it came from. A card on a foundation can be dragged back off it, but tapping
 * it does nothing.
 */
@Composable
fun Board(
    state: GameState,
    onMove: (Move) -> Unit,
    modifier: Modifier = Modifier,
    paint: BoardPaint = rememberPlainBoardPaint(),
    marks: BoardMarks = BoardMarks.None,
) {
    // A hint left over from another position points at nothing.
    val hint = remember(state, marks.hint) { marks.hint?.takeIf { state.canMove(it) } }
    val dimmed = remember(marks.dimmed) { marks.dimmed.mapTo(HashSet()) { FreeCellLayout.idOf(it) } }
    CardTable(state, FreeCellLayout, onMove, modifier, paint, TableMarks(dimmed, hint))
}

/**
 * Any game's table: every card of [state] where [layout] puts it, gliding to its new place whenever the state
 * changes. Taps and drags become moves as the layout says, and [onMove] is called with each; it is up to the
 * layout to offer only legal ones. A drop that plays nothing slides back to where it came from. [marks] picks out
 * cards for the player.
 */
@Composable
@Suppress("LongParameterList") // The position, how to lay it out, and the usual modifier and paint around them.
fun <S : Any, M : Any> CardTable(
    state: S,
    layout: TableLayout<S, M>,
    onMove: (M) -> Unit,
    modifier: Modifier = Modifier,
    paint: BoardPaint = rememberPlainBoardPaint(),
    marks: TableMarks<M> = TableMarks(),
) {
    val dimmed = marks.dimmed
    val hint = marks.hint
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val arrangement =
            remember(width, height, density, state, layout) { layout.arrange(state, width, height, density) }
        val hinted = remember(arrangement, hint) { hint?.let { arrangement.movedBy(it).toSet() }.orEmpty() }

        val scope = rememberCoroutineScope()
        val hand = remember(scope) { Hand<M>(scope) }
        SideEffect { hand.table = Laid(arrangement, onMove) }

        Canvas(Modifier.fillMaxSize()) { drawBareBoard(arrangement, paint) }
        val cardSize = with(density) { DpSize(arrangement.cardSize.width.toDp(), arrangement.cardSize.height.toDp()) }
        TappableSlots(arrangement, cardSize, onMove)

        for (placed in arrangement.cards) {
            key(placed.id) {
                val target = placed.topLeft
                val position = hand.positionOf(placed.id, target)
                val held = placed.id in hand.held
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
                        .zIndex(placed.height + lift)
                        .offset { position.value.round() }
                        .size(cardSize)
                        .then(if (placed.id in dimmed && !held) Dimmed else Modifier)
                        .testTag(placed.tag)
                        .gestures(placed.id, hand),
                ) {
                    with(paint.cards) { if (placed.faceUp) drawCard(placed.card) else drawBack() }
                    if (placed.id in hinted) drawHintMark(paint.hint, filled = false)
                }
            }
        }
        if (hint != null) {
            // Above every card, since the place a card lands is usually half over the card it lands on.
            Canvas(
                Modifier
                    .zIndex(HINT_LIFT)
                    .offset { arrangement.landing(hint).round() }
                    .size(cardSize)
                    .testTag(HINT_TAG),
            ) { drawHintMark(paint.hint, filled = true) }
        }
    }
}

/** Cards picked out for the player: [dimmed] ones, by id, are drawn darker, and [hint], legal here, is pointed out. */
class TableMarks<M : Any>(
    val dimmed: Set<Int> = emptySet(),
    val hint: M? = null,
)

/** A touch target over each empty place that plays a move when tapped, such as an empty stock. */
@Composable
private fun <M : Any> TappableSlots(
    arrangement: Arrangement<M>,
    cardSize: DpSize,
    onMove: (M) -> Unit,
) {
    arrangement.slots.forEachIndexed { index, slot ->
        val move = slot.tap ?: return@forEachIndexed
        Box(
            Modifier
                .offset { slot.topLeft.round() }
                .size(cardSize)
                .testTag("$SLOT_TAG$index")
                .pointerInput(move) { detectTapGestures { onMove(move) } },
        )
    }
}

private fun Modifier.gestures(
    id: Int,
    hand: Hand<*>,
): Modifier =
    pointerInput(id) { detectTapGestures(onTap = { hand.tap(id) }) }
        .pointerInput(id) {
            detectDragGestures(
                onDragStart = { hand.pickUp(id) },
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

/** Everything a gesture needs to know about the table at the moment it happens. */
private class Laid<M : Any>(
    val arrangement: Arrangement<M>,
    val onMove: (M) -> Unit,
)

/** The player's hand: turns taps and drags on cards into moves, and tracks the cards being dragged. */
private class Hand<M : Any>(
    private val scope: CoroutineScope,
) {
    var table: Laid<M>? = null

    /** The cards being dragged, by id, lowest first. Empty when nothing is held. */
    var held: List<Int> by mutableStateOf(emptyList())
        private set

    private val positions = HashMap<Int, CardPosition>()

    fun positionOf(
        id: Int,
        start: Offset,
    ): CardPosition = positions.getOrPut(id) { Animatable(start, Offset.VectorConverter) }

    fun tap(id: Int) {
        val table = table ?: return
        table.arrangement.tap(id)?.let(table.onMove)
    }

    fun pickUp(id: Int) {
        held = table?.arrangement?.pickUp(id).orEmpty()
    }

    fun carry(amount: Offset) {
        for (id in held) {
            val position = positions[id] ?: continue
            // Undispatched so the card has moved by the time the next touch event arrives.
            scope.launch(start = CoroutineStart.UNDISPATCHED) { position.snapTo(position.value + amount) }
        }
    }

    /** Lets go. Plays the move the drop makes, if any; otherwise the cards slide back. */
    fun drop() {
        val table = table
        val ids = held
        held = emptyList()
        val lead = ids.firstOrNull()?.let { positions[it] } ?: return
        val arrangement = table?.arrangement ?: return
        val size = arrangement.cardSize
        arrangement.drop(ids, lead.value + Offset(size.width / 2, size.height / 2))?.let(table.onMove)
        // Head home. If a move was played, the table retargets the cards to their new place a moment later.
        val homes = arrangement.cards.associate { it.id to it.topLeft }
        for (id in ids) {
            val home = homes[id] ?: continue
            scope.launch { positions[id]?.animateTo(home, MoveSpring) }
        }
    }
}

/** Everything under the cards: the pane, if there is one, and the outlines of the places they can go. */
private fun DrawScope.drawBareBoard(
    arrangement: Arrangement<*>,
    paint: BoardPaint,
) {
    paint.pane?.invoke(this, arrangement.header)
    val size = arrangement.cardSize
    val corner = CornerRadius(size.width * SLOT_CORNER)
    val line = Stroke(size.width * SLOT_LINE)
    for (slot in arrangement.slots) {
        drawRoundRect(paint.slots, slot.topLeft, size, corner, line)
        val mark = size.width * SLOT_MARK
        val inset = Offset((size.width - mark) / 2, (size.height - mark) / 2)
        when (val what = slot.mark) {
            is SlotMark.OfSuit -> drawSuit(what.suit, paint.slots, slot.topLeft + inset, mark)
            SlotMark.Recycle -> drawRecycle(paint.slots, slot.topLeft + inset, mark)
            null -> Unit
        }
    }
}

/** A ring with an arrowhead, in a square of side [side] whose top-left corner is at [topLeft]. */
private fun DrawScope.drawRecycle(
    color: Color,
    topLeft: Offset,
    side: Float,
) {
    val stroke = side * RECYCLE_LINE
    val inset = stroke / 2
    drawArc(
        color,
        startAngle = RECYCLE_START,
        sweepAngle = RECYCLE_SWEEP,
        useCenter = false,
        topLeft = topLeft + Offset(inset, inset),
        size = Size(side - stroke, side - stroke),
        style = Stroke(stroke),
    )
}

private val SlotColor = Color(0x40FFFFFF)
private val HintColor = Color(0xFFFFE082)

// Darkens whatever a card style paints without touching its shape.
private val Dimmed = Modifier.graphicsLayer { colorFilter = ColorFilter.lighting(Color(0xFFB4B4B4), Color.Black) }
private val MoveSpring = spring<Offset>(stiffness = Spring.StiffnessMediumLow)

private const val SLOT_CORNER = 0.09f
private const val SLOT_LINE = 0.02f
private const val SLOT_MARK = 0.5f
private const val RECYCLE_LINE = 0.12f
private const val RECYCLE_START = -60f
private const val RECYCLE_SWEEP = 300f
private const val HELD_LIFT = 200f
private const val MOVING_LIFT = 100f
private const val HINT_LIFT = 300f
private const val HINT_LINE = 0.05f
private const val HINT_FILL = 0.3f

const val HINT_TAG = "hint"

/** Tags the tappable empty places, followed by the slot's index. */
const val SLOT_TAG = "slot"
