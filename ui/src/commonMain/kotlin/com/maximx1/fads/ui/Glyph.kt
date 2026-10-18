package com.maximx1.fads.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

/** The pictures on the split button. */
internal enum class Glyph {
    /** An arrow that goes out to the right, turns, and comes back pointing left. */
    UNDO,

    /** Two chevrons pointing up, one above the other: everything goes up to the foundations. */
    AUTO_COMPLETE,

    /** An arrow bent anticlockwise into a circle, with a gap between its tail and its point. */
    RESTART,

    /** Three cards fanned out. */
    NEW_GAME,

    /** A light bulb. */
    HINT,

    /** Two upright bars. */
    PAUSE,
}

/** Draws [glyph] in [color], filling the draw area, which should be square. */
internal fun DrawScope.drawGlyph(
    glyph: Glyph,
    color: Color,
) {
    scale(size.minDimension / BOX, pivot = Offset.Zero) {
        when (glyph) {
            Glyph.UNDO -> {
                drawPath(UndoArrow, color, style = Line)
            }

            Glyph.AUTO_COMPLETE -> {
                drawPath(Chevrons, color, style = Line)
            }

            Glyph.RESTART -> {
                drawPath(RestartCircle, color, style = Line)
                drawPath(RestartPoint, color)
            }

            Glyph.NEW_GAME -> {
                // Back card first. Each is drawn only where the cards in front of it leave it showing.
                Fan.forEachIndexed { index, card ->
                    clipPath(FanInFront[index], ClipOp.Difference) { drawPath(card, color, style = ThinLine) }
                }
            }

            Glyph.HINT -> {
                drawPath(Bulb, color, style = Line)
            }

            Glyph.PAUSE -> {
                for (left in PauseBars) drawRoundRect(color, Offset(left, PAUSE_TOP), PauseBar, PauseCorner)
            }
        }
    }
}

// Everything below is drawn in a box this many units square.
private const val BOX = 24f

private val Line = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val ThinLine = Stroke(width = 1.7f, join = StrokeJoin.Round)

private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

private val UndoArrow = path("M8.5,5.5 L4,10 L8.5,14.5 M4,10 H14.5 A4.75,4.75 0 0 1 14.5,19.5 H9")

private val Chevrons = path("M6,12 L12,6 L18,12 M6,19 L12,13 L18,19")

// The circle starts at its tail, low on the left, runs anticlockwise round the bottom and ends at the top, where
// the point takes over. The stretch between the point and the tail is left open.
private val RestartCircle = path("M4.95,9.93 A7.5,7.5 0 1 0 12,5")
private val RestartPoint = path("M7,5 L12.6,0.9 L12.6,9.1 Z")

// One upright card, turned about a point below it to make each card of the fan.
private val Fan: List<Path> =
    listOf(-FAN_TURN, 0f, FAN_TURN).map { degrees ->
        Path().apply {
            addRoundRect(RoundRect(Rect(8f, 4f, 16f, 16f), CornerRadius(1.5f)))
            transform(
                Matrix().apply {
                    translate(FAN_PIVOT_X, FAN_PIVOT_Y)
                    rotateZ(degrees)
                    translate(-FAN_PIVOT_X, -FAN_PIVOT_Y)
                },
            )
        }
    }
private val FanInFront: List<Path> =
    Fan.indices.map { index ->
        Fan.drop(index + 1).fold(Path()) { covered, card -> Path.combine(PathOperation.Union, covered, card) }
    }
private const val FAN_TURN = 22f
private const val FAN_PIVOT_X = 12f
private const val FAN_PIVOT_Y = 23f

// The glass, and under it one line for the cap.
private val Bulb =
    path(
        "M9.2,17.2 V15.6 C7.3,14.5 6,12.5 6,10.2 A6,6 0 0 1 18,10.2 C18,12.5 16.7,14.5 14.8,15.6 V17.2 Z " +
            "M9.8,20.6 H14.2",
    )

private val PauseBars = listOf(6.5f, 14f)
private val PauseBar = Size(3.5f, 14f)
private val PauseCorner = CornerRadius(1f)
private const val PAUSE_TOP = 5f
