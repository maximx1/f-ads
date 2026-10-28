package com.maximx1.fads.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * How far the table is zoomed in, and which part of it shows. The table is never smaller than its place on the
 * screen, and never moved so far that bare background shows at an edge.
 */
@Stable
internal class TableView {
    /** How many times its own size the table is drawn, from 1 up to [MAX_ZOOM]. */
    var scale by mutableFloatStateOf(1f)
        private set

    /** Where the table's top left corner is drawn, from the top left of its place. */
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** The size of the table's place on the screen. */
    var size = Size.Zero

    val zoomed: Boolean get() = scale > 1f

    /** The table easing out or gliding on by itself, if it is. */
    private var motion: Job? = null

    /** Zooms by [factor] about [anchor], a point in the table's place, then moves the table by [pan]. */
    fun zoomBy(
        factor: Float,
        anchor: Offset,
        pan: Offset = Offset.Zero,
    ) {
        val under = (anchor - offset) / scale
        scale = (scale * factor).coerceIn(1f, MAX_ZOOM)
        offset = clamped(anchor + pan - under * scale)
    }

    /** Moves the table by [pan], as far as it goes, and returns how far it went. */
    fun panBy(pan: Offset): Offset {
        val from = offset
        offset = clamped(offset + pan)
        return offset - from
    }

    /**
     * Lets the table go, moving at [velocity] in pixels a second, after the fingers moved it [swept] pixels. Left
     * only a little zoomed in, it eases back out to the whole table; otherwise it glides on and slows to a stop,
     * further the harder and longer it was thrown, or until it reaches an edge.
     */
    fun letGo(
        velocity: Offset,
        swept: Float,
        scope: CoroutineScope,
    ) {
        motion =
            if (zoomed && scale < SNAP_BELOW) {
                val centre = Offset(size.width / 2, size.height / 2)
                scope.launch { animate(scale, 1f) { value, _ -> zoomBy(value / scale, centre) } }
            } else if (zoomed && velocity != Offset.Zero) {
                scope.launch { glide(velocity, swept * GLIDE_REACH) }
            } else {
                null
            }
    }

    /** Stops the table easing out or gliding, as a finger comes down to take hold of it. */
    fun hold() {
        motion?.cancel()
    }

    /**
     * Glides from [velocity] to a stop, no further than [reach]. A flick can be fast but very short, and a short
     * flick should only nudge the table on, so it is slowed to what would stop within its reach.
     */
    private suspend fun glide(
        velocity: Offset,
        reach: Float,
    ) {
        val distance = Glide.calculateTargetValue(Offset.VectorConverter, Offset.Zero, velocity).getDistance()
        val start = if (distance > reach) velocity * (reach / distance) else velocity
        var travelled = Offset.Zero
        AnimationState(Offset.VectorConverter, Offset.Zero, start).animateDecay(Glide) {
            val moved = panBy(value - travelled)
            // Stuck against the edges it was heading for.
            if (value != travelled && moved == Offset.Zero) cancelAnimation()
            travelled = value
        }
    }

    private fun clamped(offset: Offset) =
        Offset(
            offset.x.coerceIn(size.width * (1 - scale), 0f),
            offset.y.coerceIn(size.height * (1 - scale), 0f),
        )
}

/**
 * A table that zooms. Two fingers pinch to zoom and move together to pan, and take over from whatever the first
 * finger was doing, so a card being dragged goes back where it came from. One finger is left to [content]: it
 * picks up, drags and taps cards as it always does, and only pans, while zoomed in, if what it drags does not move
 * by itself, such as the bare table or a mahjong tile. Fingers that let go while panning throw the table, which
 * glides on and slows to a stop. A mouse wheel zooms about the pointer.
 */
@Composable
internal fun ZoomableTable(
    view: TableView,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .onSizeChanged { view.size = it.toSize() }
            // Only once zoomed, so a card dragged past the edge of the table is not cut off.
            .then(if (view.zoomed) Modifier.clipToBounds() else Modifier)
            .pointerInput(view) { awaitEachGesture { followGesture(view, viewConfiguration, scope) } }
            .pointerInput(view) { awaitPointerEventScope { while (true) wheel(view, awaitPointerEvent()) } },
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = view.scale
                scaleY = view.scale
                translationX = view.offset.x
                translationY = view.offset.y
            },
            content = content,
        )
    }
}

/** One gesture, from the first finger down to the last up. */
private suspend fun AwaitPointerEventScope.followGesture(
    view: TableView,
    configuration: ViewConfiguration,
    scope: CoroutineScope,
) {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    view.hold()
    var pinching = false
    val finger = OneFinger(view, configuration.touchSlop)
    val sweep = Sweep()
    do {
        // Before the cards see it, so a second finger can take the gesture from them.
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val time = event.changes.first().uptimeMillis
        pinching = pinching || event.changes.count { it.pressed } > 1
        if (pinching) {
            // Unspecified when every finger has only just come down, with nowhere they were before.
            val anchor = event.calculateCentroid(useCurrent = false)
            val pan = event.calculatePan()
            if (anchor.isSpecified) view.zoomBy(event.calculateZoom(), anchor, pan)
            sweep.add(time, pan)
            event.changes.forEach { it.consume() }
        } else {
            // After the cards, to pan only with a finger they left alone.
            val change = awaitPointerEvent(PointerEventPass.Final).changes.singleOrNull()
            change?.let(finger::move)?.let { sweep.add(time, it) }
        }
    } while (event.changes.any { it.pressed })
    view.letGo(sweep.velocity(configuration.maximumFlingVelocity), sweep.distance, scope)
}

/** How fast the fingers were panning the table as they let go, and how far they moved it. */
private class Sweep {
    private val tracker = VelocityTracker()
    private var position = Offset.Zero

    /** How far the fingers moved the table, along the way they went. */
    var distance = 0f
        private set

    fun add(
        time: Long,
        pan: Offset,
    ) {
        position += pan
        distance += pan.getDistance()
        tracker.addPosition(time, position)
    }

    /** In pixels a second, no faster than [maximum] either way. Nothing if the fingers stopped before letting go. */
    fun velocity(maximum: Float): Offset {
        val velocity = tracker.calculateVelocity()
        return Offset(velocity.x.coerceIn(-maximum, maximum), velocity.y.coerceIn(-maximum, maximum))
    }
}

/** A single finger, which pans the zoomed table once it goes further than [touchSlop] with nothing else using it. */
private class OneFinger(
    private val view: TableView,
    private val touchSlop: Float,
) {
    private var travel = Offset.Zero
    private var panning = false

    /** Pans with [change] if this finger is panning, and returns how far it moved, or null if it is not panning. */
    fun move(change: PointerInputChange): Offset? {
        if (!view.zoomed || change.isConsumed) return null
        val moved = change.positionChange()
        travel += moved
        panning = panning || travel.getDistance() > touchSlop
        return moved.takeIf { panning }?.also {
            view.panBy(it)
            change.consume()
        }
    }
}

private fun wheel(
    view: TableView,
    event: PointerEvent,
) {
    if (event.type != PointerEventType.Scroll) return
    val change = event.changes.firstOrNull() ?: return
    view.zoomBy(WHEEL_STEP.pow(-change.scrollDelta.y), change.position)
    change.consume()
}

private const val MAX_ZOOM = 3f

// Let go below this and it eases back out, so a small pinch by mistake undoes itself.
private const val SNAP_BELOW = 1.15f

// How much one notch of a mouse wheel zooms.
private const val WHEEL_STEP = 1.15f

// How quickly a thrown table slows down. At this it glides on about a third of a second's worth of the speed it was
// thrown at.
private const val GLIDE_FRICTION = 0.7f

// A thrown table glides on no more than this many times as far as the fingers moved it.
private const val GLIDE_REACH = 3f

private val Glide = exponentialDecay<Offset>(GLIDE_FRICTION)
