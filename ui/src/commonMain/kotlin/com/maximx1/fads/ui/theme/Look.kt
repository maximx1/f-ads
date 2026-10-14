package com.maximx1.fads.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.maximx1.fads.ui.CardStyle

/** Everything about the screen that changes with the [TableTheme]. */
internal interface Look {
    /** The outlines of the empty places on the board. */
    val slotColor: Color

    /** The colour of the buttons in dialogs. */
    val accent: Color

    val clock: ClockLook

    val buttons: ButtonLook

    /** True if the table is pale, so the status bar needs dark icons to show against it. */
    val paleTable: Boolean get() = false

    /** Paints the table. It fills the whole screen, under the system bars too. */
    fun DrawScope.drawTable()

    fun cards(measurer: TextMeasurer): CardStyle

    /** Whatever hangs over the table and the cards. It must not take touches. */
    @Composable
    fun Overlay(modifier: Modifier) = Unit
}

/** How the clock is written, and the [plate] behind it if it has one. */
internal class ClockLook(
    val text: TextStyle,
    val plate: (DrawScope.() -> Unit)? = null,
)

/**
 * How the split button is drawn. [fill] paints its whole face, already clipped to [shape]. Its pictures are
 * drawn in [ink], over a copy in [etch] set just below if there is one: dark for a shadow, light to make them
 * look pressed in.
 */
internal class ButtonLook(
    val shape: Shape,
    val ink: Color,
    val divider: Color,
    val etch: Color? = null,
    val border: BorderStroke? = null,
    val fill: DrawScope.() -> Unit,
)

/**
 * A soft patch of [color] that fades to nothing at [radius]. [stretch] widens it into an ellipse, which
 * [degrees] then turns.
 */
internal fun DrawScope.drawGlow(
    centre: Offset,
    radius: Float,
    color: Color,
    stretch: Float = 1f,
    degrees: Float = 0f,
) {
    withTransform({
        rotate(degrees, centre)
        scale(stretch, 1f, centre)
    }) {
        drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), centre, radius), radius, centre)
    }
}
