package com.maximx1.fads.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.SuitColor
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.Header
import com.maximx1.fads.ui.drawPips
import com.maximx1.fads.ui.drawPlainBack
import kotlin.math.abs
import kotlin.random.Random

/**
 * Apple around 2004, iTunes in particular: brushed aluminium only round the edges of the window, and the board
 * set into it as a white list under a glossy grey header. The time is on a pale green
 * display, and the buttons are glossy grey with etched labels.
 */
internal object BrushedMetalLook : Look {
    override val slotColor = Color(0x47000000)
    override val accent = Color(0xFF3D80DF)
    override val paleTable = true
    override val clock =
        ClockLook(
            text = TextStyle(color = Color(0xFF2A2E1C), fontWeight = FontWeight.Medium),
            plate = {
                val corner = CornerRadius(size.height / 2)
                val edge = 1.dp.toPx()
                // Set into the metal: a highlight under the rim, and a shadow just inside its top edge.
                drawRoundRect(Engraving, Offset(0f, edge), cornerRadius = corner)
                drawRoundRect(Brush.verticalGradient(Display), cornerRadius = corner)
                drawRoundRect(Brush.verticalGradient(DisplayShade, endY = size.height / 2), cornerRadius = corner)
                drawRoundRect(DisplayRim, cornerRadius = corner, style = Stroke(edge))
            },
        )
    override val buttons =
        ButtonLook(
            shape = RoundedCornerShape(percent = 50),
            ink = Color(0xFF2C2C2C),
            etch = Color.White,
            divider = Color(0xFF8E8E8E),
            border = BorderStroke(1.dp, Color(0xFF5E5E5E)),
            fill = { drawRect(Gloss.downward()) },
        )
    override val boardPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp)
    override val pane: DrawScope.(Header?) -> Unit = { drawList(it) }

    override fun DrawScope.drawTable() {
        drawRect(Aluminium.downward())
        for (pass in Brushing) drawRect(pass.streaks.downward(pass.height.toPx(), TileMode.Repeated))
        drawRect(Brush.horizontalGradient(Sheen))
    }

    override fun cards(measurer: TextMeasurer): CardStyle = PaperCards(measurer)
}

/** The board as a white list, with a glossy grey header behind its top row, split where its groups meet. */
private fun DrawScope.drawList(header: Header?) {
    val line = 1.dp.toPx()
    drawRect(ListBackground)
    if (header != null) {
        drawRect(HeaderGloss.downward(header.bottom), size = Size(size.width, header.bottom))
        val edge = header.bottom - line / 2
        drawLine(HeaderEdge, Offset(0f, edge), Offset(size.width, edge), line)
        val inset = HEADER_LINE_INSET.dp.toPx()
        for (x in header.dividers) drawLine(HeaderLine, Offset(x, inset), Offset(x, header.bottom - inset), line)
    }
    drawRect(PaneEdge, style = Stroke(line))
}

private class PaperCards(
    private val measurer: TextMeasurer,
) : CardStyle {
    // Aqua blue, the colour of a selected row.
    override fun DrawScope.drawBack() = drawPlainBack(CardBack, CardBackLines)

    override fun DrawScope.drawCard(card: Card) {
        val corner = CornerRadius(size.width * CARD_CORNER)
        drawRoundRect(CardShadow, Offset(0f, size.width * SHADOW_DROP), cornerRadius = corner)
        drawRoundRect(Brush.verticalGradient(CardFace), cornerRadius = corner)
        drawRoundRect(CardEdge, cornerRadius = corner, style = Stroke(size.width * CARD_EDGE))
        drawPips(measurer, card, if (card.color == SuitColor.RED) RedInk else BlackInk)
    }
}

/** One pass of the brush: thin streaks, lighter and darker than the metal, in a strip that repeats down the screen. */
private class BrushPass(
    val height: Dp,
    seed: Int,
) {
    // The last streak matches the first, so the repeats join without a seam.
    val streaks: Stops =
        Random(seed).let { random ->
            val shades = List(STREAKS - 1) { random.nextFloat() * 2 - 1 }
            Array(STREAKS) { index ->
                val shade = shades[index % shades.size]
                val tone = if (shade < 0) Color.Black else Color.White
                index / (STREAKS - 1f) to tone.copy(alpha = abs(shade) * STRONGEST_STREAK)
            }
        }
}

// Lighter at the top of the window than at the bottom.
private val Aluminium = arrayOf(0f to Color(0xFFCFCFCF), 1f to Color(0xFFA9A9A9))

// Two passes of different heights, so the pattern they make together takes far longer than the screen to repeat.
private val Brushing = listOf(BrushPass(149.dp, seed = 2003), BrushPass(97.dp, seed = 1984))
private const val STREAKS = 140
private const val STRONGEST_STREAK = 0.07f

// Light falling across the metal: brightest down the middle.
private val Sheen = listOf(Color(0x1F000000), Color(0x33FFFFFF), Color(0x1F000000))

private val Gloss =
    arrayOf(
        0f to Color(0xFFFFFFFF),
        0.5f to Color(0xFFE6E6E6),
        0.5f to Color(0xFFD4D4D4),
        1f to Color(0xFFF2F2F2),
    )

private val Display = listOf(Color(0xFFD3D7AC), Color(0xFFE4E7C4))
private val DisplayShade = listOf(Color(0x40000000), Color.Transparent)
private val DisplayRim = Color(0xFF6E7256)
private val Engraving = Color(0x99FFFFFF)

private val ListBackground = Color.White
private val PaneEdge = Color(0xFF6B6B6B)
private val HeaderGloss =
    arrayOf(
        0f to Color(0xFFFFFFFF),
        0.5f to Color(0xFFF1F1F1),
        0.5f to Color(0xFFE4E4E4),
        1f to Color(0xFFF4F4F4),
    )
private val HeaderEdge = Color(0xFF9C9C9C)
private val HeaderLine = Color(0xFFB4B4B4)
private const val HEADER_LINE_INSET = 3

private val CardFace = listOf(Color.White, Color(0xFFF0F0F0))
private val CardEdge = Color(0xFF8A8A8A)
private val CardShadow = Color(0x52000000)
private val RedInk = TextStyle(color = Color(0xFFC62828))
private val BlackInk = TextStyle(color = Color(0xFF1C1C1C))
private const val CARD_CORNER = 0.11f
private const val CARD_EDGE = 0.012f
private const val SHADOW_DROP = 0.03f
private val CardBack = Color(0xFF3D80DF)
private val CardBackLines = Color(0x40FFFFFF)
