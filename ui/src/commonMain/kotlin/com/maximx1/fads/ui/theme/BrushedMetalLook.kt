package com.maximx1.fads.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import com.maximx1.fads.ui.drawPips
import kotlin.math.abs
import kotlin.random.Random

/**
 * Apple around 2003: a window of brushed aluminium, glossy grey buttons with etched labels, and the time on a
 * pale green display like the one in iTunes.
 */
internal object BrushedMetalLook : Look {
    override val slotColor = Color(0x47000000)
    override val accent = Color(0xFF9CC7FF)
    override val paleTable = true
    override val clock =
        ClockLook(
            text = TextStyle(color = Color(0xFF2A3120), fontWeight = FontWeight.Bold),
            plate = {
                val corner = CornerRadius(size.height / 2)
                drawRoundRect(Brush.verticalGradient(Display), cornerRadius = corner)
                drawRoundRect(DisplayRim, cornerRadius = corner, style = Stroke(1.dp.toPx()))
            },
        )
    override val buttons =
        ButtonLook(
            shape = RoundedCornerShape(percent = 50),
            ink = Color(0xFF2C2C2C),
            etch = Color.White,
            divider = Color(0xFF8E8E8E),
            border = BorderStroke(1.dp, Color(0xFF6A6A6A)),
            fill = { drawRect(Gloss.downward()) },
        )

    override fun DrawScope.drawTable() {
        drawRect(Aluminium)
        for (pass in Brushing) drawRect(pass.streaks.downward(pass.height.toPx(), TileMode.Repeated))
        drawRect(Brush.horizontalGradient(Sheen))
    }

    override fun cards(measurer: TextMeasurer): CardStyle = PaperCards(measurer)
}

private class PaperCards(
    private val measurer: TextMeasurer,
) : CardStyle {
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

private val Aluminium = Color(0xFFB9B9B9)

// Two passes of different heights, so the pattern they make together takes far longer than the screen to repeat.
private val Brushing = listOf(BrushPass(149.dp, seed = 2003), BrushPass(97.dp, seed = 1984))
private const val STREAKS = 140
private const val STRONGEST_STREAK = 0.11f

// Light falling across the metal: brightest down the middle.
private val Sheen = listOf(Color(0x33000000), Color(0x40FFFFFF), Color(0x33000000))

private val Gloss =
    arrayOf(
        0f to Color(0xFFFFFFFF),
        0.5f to Color(0xFFE6E6E6),
        0.5f to Color(0xFFD4D4D4),
        1f to Color(0xFFF2F2F2),
    )

private val Display = listOf(Color(0xFFC3CEA2), Color(0xFFE2EAC6))
private val DisplayRim = Color(0xFF5E6A4C)

private val CardFace = listOf(Color.White, Color(0xFFF0F0F0))
private val CardEdge = Color(0xFF8A8A8A)
private val CardShadow = Color(0x52000000)
private val RedInk = TextStyle(color = Color(0xFFC62828))
private val BlackInk = TextStyle(color = Color(0xFF1C1C1C))
private const val CARD_CORNER = 0.11f
private const val CARD_EDGE = 0.012f
private const val SHADOW_DROP = 0.03f
