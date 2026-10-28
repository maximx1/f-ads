package com.maximx1.fads.ui

import androidx.compose.animation.core.animate
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
import androidx.compose.ui.layout.onSizeChanged
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

    private var settling: Job? = null

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

    fun panBy(pan: Offset) {
        offset = clamped(offset + pan)
    }

    /** Eases back out to the whole table if it was left only a little zoomed in. */
    fun settle(scope: CoroutineScope) {
        if (scale == 1f || scale >= SNAP_BELOW) return
        val centre = Offset(size.width / 2, size.height / 2)
        settling = scope.launch { animate(scale, 1f) { value, _ -> zoomBy(value / scale, centre) } }
    }

    fun stopSettling() {
        settling?.cancel()
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
 * by itself, such as the bare table or a mahjong tile. A mouse wheel zooms about the pointer.
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
            .pointerInput(view) { awaitEachGesture { followGesture(view, viewConfiguration.touchSlop, scope) } }
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
    touchSlop: Float,
    scope: CoroutineScope,
) {
    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    view.stopSettling()
    var pinching = false
    val finger = OneFinger(view, touchSlop)
    do {
        // Before the cards see it, so a second finger can take the gesture from them.
        val event = awaitPointerEvent(PointerEventPass.Initial)
        pinching = pinching || event.changes.count { it.pressed } > 1
        if (pinching) {
            // Unspecified when every finger has only just come down, with nowhere they were before.
            val anchor = event.calculateCentroid(useCurrent = false)
            if (anchor.isSpecified) view.zoomBy(event.calculateZoom(), anchor, event.calculatePan())
            event.changes.forEach { it.consume() }
        } else {
            // After the cards, to pan only with a finger they left alone.
            awaitPointerEvent(PointerEventPass.Final).changes.singleOrNull()?.let(finger::move)
        }
    } while (event.changes.any { it.pressed })
    view.settle(scope)
}

/** A single finger, which pans the zoomed table once it goes further than [touchSlop] with nothing else using it. */
private class OneFinger(
    private val view: TableView,
    private val touchSlop: Float,
) {
    private var travel = Offset.Zero
    private var panning = false

    fun move(change: PointerInputChange) {
        if (!view.zoomed || change.isConsumed) return
        travel += change.positionChange()
        panning = panning || travel.getDistance() > touchSlop
        if (panning) {
            view.panBy(change.positionChange())
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
