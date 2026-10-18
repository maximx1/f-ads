package com.maximx1.fads.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.SuitColor
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.drawPips
import com.maximx1.fads.ui.drawPlainBack

/** Windows Vista's Aero: a blue-green sky with streaks of light, and glass with a hard shine across its top half. */
internal object VistaLook : Look {
    override val slotColor = Color(0x66FFFFFF)
    override val accent = Color(0xFF8AD8FF)
    override val clock =
        ClockLook(TextStyle(color = Color.White, shadow = Shadow(Color(0xCC00131F), Offset(0f, 2f), blurRadius = 6f)))
    override val buttons =
        ButtonLook(
            shape = RoundedCornerShape(6.dp),
            ink = Color.White,
            etch = Color(0x99000000),
            divider = Color(0x8000111C),
            border = BorderStroke(1.dp, Color(0xA6FFFFFF)),
            fill = {
                drawRect(GlassTint)
                drawRect(GlassShine.downward())
            },
        )

    override fun DrawScope.drawTable() {
        drawRect(Sky.between(Offset.Zero, Offset(size.width, size.height)))
        for (streak in Streaks) {
            drawGlow(
                centre = Offset(size.width * streak.across, size.height * streak.down),
                radius = size.width * streak.radius,
                color = streak.color,
                stretch = STREAK_STRETCH,
                degrees = STREAK_TILT,
            )
        }
    }

    override fun cards(measurer: TextMeasurer): CardStyle = GlassCards(measurer)
}

private class GlassCards(
    private val measurer: TextMeasurer,
) : CardStyle {
    // Deep sky blue under a lattice of light.
    override fun DrawScope.drawBack() = drawPlainBack(CardBack, CardBackLines)

    override fun DrawScope.drawCard(card: Card) {
        val corner = CornerRadius(size.width * CARD_CORNER)
        drawRoundRect(CardShine.downward(), cornerRadius = corner)
        drawPips(measurer, card, if (card.color == SuitColor.RED) RedInk else BlackInk)
        drawRoundRect(CardEdge, cornerRadius = corner, style = Stroke(size.width * CARD_EDGE))
    }
}

/** A streak of light on the table. Its place and size are fractions of the screen. */
private class Streak(
    val across: Float,
    val down: Float,
    val radius: Float,
    val color: Color,
)

private val Sky =
    arrayOf(
        0f to Color(0xFF07243F),
        0.4f to Color(0xFF0D5E92),
        0.72f to Color(0xFF1C9C9A),
        1f to Color(0xFF0A4234),
    )
private val Streaks =
    listOf(
        Streak(across = 0.15f, down = 0.3f, radius = 0.85f, color = Color(0x40FFFFFF)),
        Streak(across = 0.9f, down = 0.62f, radius = 0.7f, color = Color(0x38B4FFD8)),
        Streak(across = 0.4f, down = 0.95f, radius = 0.6f, color = Color(0x2E7FD4FF)),
    )
private const val STREAK_STRETCH = 2.4f
private const val STREAK_TILT = -28f

// Glass: a dark tint, then light that stops dead half way down and a glow that rises from the bottom edge.
private val GlassTint = Color(0x8C0B2238)
private val GlassShine =
    arrayOf(
        0f to Color(0x8CFFFFFF),
        0.5f to Color(0x38FFFFFF),
        0.5f to Color(0x14000000),
        1f to Color(0x6657C8FF),
    )

private val CardShine =
    arrayOf(
        0f to Color(0xFFFFFFFF),
        0.5f to Color(0xFFE6F0FA),
        0.5f to Color(0xFFD5E4F4),
        1f to Color(0xFFEDF5FC),
    )
private val CardEdge = Color(0xFF4F7CAC)
private val RedInk = TextStyle(color = Color(0xFFC4161C))
private val BlackInk = TextStyle(color = Color(0xFF14202E))
private const val CARD_CORNER = 0.1f
private const val CARD_EDGE = 0.02f
private val CardBack = Color(0xFF0D5E92)
private val CardBackLines = Color(0x5C8AD8FF)
