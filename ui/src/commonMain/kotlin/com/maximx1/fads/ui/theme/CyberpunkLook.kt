package com.maximx1.fads.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.SuitColor
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.drawPips
import com.maximx1.fads.ui.drawPlainBack

/** Neon on black: a night-city grid, cut-corner panels, and dark cards lit pink and cyan. */
internal object CyberpunkLook : Look {
    override val slotColor = Color(0x6605D9E8)
    override val accent = Yellow
    override val clock =
        ClockLook(
            TextStyle(
                color = Cyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                shadow = Shadow(Cyan, blurRadius = 14f),
            ),
        )
    override val buttons =
        ButtonLook(
            shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
            ink = Yellow,
            divider = Color(0x8005D9E8),
            border = BorderStroke(1.5.dp, Cyan),
            fill = { drawRect(Panel) },
        )

    override fun DrawScope.drawTable() {
        drawRect(Brush.verticalGradient(Night))
        val step = GridStep.toPx()
        var x = size.width / 2 % step
        while (x < size.width) {
            drawLine(GridLine, Offset(x, 0f), Offset(x, size.height))
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(GridLine, Offset(0f, y), Offset(size.width, y))
            y += step
        }
        // The city beyond the table: pink rising from the bottom, cyan leaking in at the top.
        drawGlow(Offset(size.width / 2, size.height), size.width, CityGlow, stretch = 2f)
        drawGlow(Offset.Zero, size.width, SkyGlow)
    }

    override fun cards(measurer: TextMeasurer): CardStyle = NeonCards(measurer)
}

private class NeonCards(
    private val measurer: TextMeasurer,
) : CardStyle {
    // Near black under a magenta neon lattice.
    override fun DrawScope.drawBack() = drawPlainBack(CardBack, CardBackLines)

    override fun DrawScope.drawCard(card: Card) {
        val width = size.width
        val corner = CornerRadius(width * CARD_CORNER)
        val neon = if (card.color == SuitColor.RED) Pink else Cyan
        drawRoundRect(CardFace, cornerRadius = corner)
        // A tube of light round the edge: wide faint strokes for the glow, a thin bright one for the tube.
        for (halo in Halos) {
            drawRoundRect(neon.copy(alpha = HALO_ALPHA), cornerRadius = corner, style = Stroke(width * halo))
        }
        drawRoundRect(neon, cornerRadius = corner, style = Stroke(width * TUBE))
        val ink =
            TextStyle(
                color = neon,
                fontFamily = FontFamily.Monospace,
                shadow =
                    Shadow(
                        neon,
                        blurRadius =
                            width * GLOW,
                    ),
            )
        drawPips(measurer, card, ink)
    }
}

private val Cyan = Color(0xFF05D9E8)
private val Pink = Color(0xFFFF2A6D)
private val Yellow = Color(0xFFFCEE0A)

private val Night = listOf(Color(0xFF05000C), Color(0xFF120024), Color(0xFF210034))
private val GridLine = Color(0x1FFF2A6D)
private val GridStep = 28.dp
private val CityGlow = Color(0x73FF2A6D)
private val SkyGlow = Color(0x3305D9E8)
private val Panel = Color(0xD908080F)

private val CardFace = Color(0xFF0C0E1C)
private val Halos = listOf(0.12f, 0.07f)
private const val HALO_ALPHA = 0.16f
private const val TUBE = 0.025f
private const val GLOW = 0.12f
private const val CARD_CORNER = 0.06f
private val CardBack = Color(0xFF12001F)
private val CardBackLines = Color(0xB3FF2BD6)
