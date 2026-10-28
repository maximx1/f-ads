package com.maximx1.fads.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.engine.play
import com.maximx1.fads.core.rules.FreeCellRules
import com.maximx1.fads.core.rules.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pinching the table to zoom, and what one finger still does with it zoomed. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ZoomTest {
    @get:Rule
    val compose = createComposeRule()

    private var state by mutableStateOf(Deal.fromNumber(1))
    private val moves = mutableListOf<Move>()

    private fun show(playable: Boolean = true) {
        compose.setContent {
            GameTable(
                rules = FreeCellRules,
                state = state,
                onMove = {
                    moves += it
                    state = state.play(it)
                },
                modifier = Modifier.fillMaxSize(),
                playable = playable,
            )
        }
        compose.waitForIdle()
    }

    private fun bounds(card: String): Rect = compose.onNodeWithTag(card).fetchSemanticsNode().boundsInRoot

    /** The bare table at the foot of the screen, below [card]. */
    private fun bareBelow(card: Rect): Offset {
        val height =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .size.height
        return Offset(card.center.x, height - 40f)
    }

    /**
     * Spreads two fingers apart, one above the other, from [from] to [to] apart, about [at], or the middle of the
     * table at [x]. The table zooms a little less than the fingers spread, since what they move as they lift is lost.
     */
    private fun pinch(
        from: Float,
        to: Float,
        x: Float = 40f,
        at: Offset? = null,
    ) {
        compose.onRoot().performTouchInput {
            val middle = at ?: Offset(x, centerY)
            pinch(
                start0 = middle - Offset(0f, from / 2),
                end0 = middle - Offset(0f, to / 2),
                start1 = middle + Offset(0f, from / 2),
                end1 = middle + Offset(0f, to / 2),
            )
        }
        compose.waitForIdle()
    }

    /** A quick drag, a step every frame, that lifts at once, or holds still for [holdMillis] first. */
    private fun drag(
        from: Offset,
        to: Offset,
        holdMillis: Long = 0,
    ) {
        compose.onRoot().performTouchInput {
            down(from)
            repeat(DRAG_STEPS) { moveBy((to - from) / DRAG_STEPS.toFloat()) }
            advanceEventTime(holdMillis)
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun pinchingOutZoomsInAndPinchingBackShowsTheWholeTable() {
        show()
        val before = bounds("3D")
        pinch(from = 200f, to = 600f, at = before.center)
        // By its height: it is at the left edge, so part of it goes off the screen.
        val zoom = bounds("3D").height / before.height
        assertTrue("zoomed $zoom", zoom > 2.5f && zoom <= 3f)
        assertEquals(emptyList<Move>(), moves)

        pinch(from = 600f, to = 200f, at = bounds("3D").center)
        assertEquals(before, bounds("3D"))
    }

    @Test
    fun aSmallPinchByMistakeEasesBackOut() {
        show()
        val before = bounds("3D")
        pinch(from = 400f, to = 440f)
        assertEquals(before, bounds("3D"))
    }

    @Test
    fun zoomedInOneFingerStillDragsACard() {
        show()
        // Half again as big, about the left edge, which keeps the first five columns in sight.
        pinch(from = 400f, to = 600f, x = 0f)
        val target = state.columns[4].last().code
        drag(bounds("3D").topLeft + grip, bounds(target).center)
        assertEquals(listOf<Move>(Move.ColumnToColumn(0, 4)), moves)
    }

    /** How far the table moves across when a finger on the bare table drags it [by] across, zoomed in. */
    private fun panAcross(
        by: Float,
        holdMillis: Long = 0,
    ): Float {
        show()
        pinch(from = 400f, to = 600f, x = 0f)
        // A card well in from the left, so it is still in sight once the table moves left under it.
        val card = state.columns[3].last().code
        val before = bounds(card)
        val bare = bareBelow(before)
        drag(bare, bare + Offset(by, 0f), holdMillis)
        assertEquals(emptyList<Move>(), moves)
        return bounds(card).left - before.left
    }

    @Test
    fun zoomedInOneFingerOnTheBareTablePansAndStaysWhereItStops() {
        val shift = panAcross(-300f, holdMillis = 200)
        assertTrue("moved $shift", shift < -200f && shift >= -300f)
    }

    @Test
    fun aTableThrownGlidesOnAfterTheFingerLifts() {
        val shift = panAcross(-150f)
        assertTrue("moved $shift", shift < -250f)
    }

    @Test
    fun withoutZoomOneFingerOnTheBareTableDoesNothing() {
        show()
        val before = bounds("3D")
        val bare = bareBelow(before)
        drag(bare, bare - Offset(300f, 0f))
        assertEquals(before, bounds("3D"))
    }

    @Test
    fun aSecondFingerTakesADraggedCardBackWithoutPlayingIt() {
        show()
        val before = bounds("9D")
        val target = bounds("10S").center
        val start = before.topLeft + grip
        compose.onRoot().performTouchInput {
            down(0, start)
            repeat(DRAG_STEPS) { moveBy(0, (target - start) / DRAG_STEPS.toFloat()) }
            down(1, Offset(centerX, bottom - 100f))
            moveBy(1, Offset(0f, -50f))
            up(0)
            up(1)
        }
        compose.waitForIdle()
        assertEquals(emptyList<Move>(), moves)
        assertEquals(before.size, bounds("9D").size)
        assertEquals(before.left, bounds("9D").left, 1f)
    }

    @Test
    fun aTableToWatchZoomsButNothingOnItCanBePlayed() {
        show(playable = false)
        val before = bounds("3D")
        compose.onNodeWithTag("3D").performTouchInput {
            down(grip)
            up()
        }
        compose.waitForIdle()
        assertEquals(emptyList<Move>(), moves)

        pinch(from = 200f, to = 400f, at = before.center)
        // By its height: it is at the left edge, so part of it goes off the screen.
        val zoom = bounds("3D").height / before.height
        assertTrue("zoomed $zoom", zoom > 1.8f && zoom <= 2f)
    }

    private companion object {
        const val DRAG_STEPS = 8

        /** Where the finger lands on a card: a little in from its top-left corner. */
        val grip = Offset(30f, 20f)
    }
}
