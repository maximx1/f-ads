package com.maximx1.fads.ui.den

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import com.maximx1.fads.core.den.SlotSymbol

/**
 * [symbol] drawn in the square of side [side] whose top left is at [at]. Each is laid out on a grid [UNITS] a side,
 * with 0, 0 in the middle, x to the right and y down.
 */
internal fun DrawScope.drawSymbol(
    symbol: SlotSymbol,
    at: Offset,
    side: Float,
    measurer: TextMeasurer,
) {
    val middle = at + Offset(side / 2, side / 2)
    val unit = side / UNITS

    fun point(grid: Offset) = middle + grid * unit

    fun size(grid: Size) = Size(grid.width * unit, grid.height * unit)
    when (symbol) {
        SlotSymbol.CHERRY -> {
            for (cherry in Cherries) drawLine(Leaf, point(cherry), point(Stalk), unit * STEM)
            for (cherry in Cherries) drawCircle(CherryRed, unit * CHERRY_RADIUS, point(cherry + CherryDrop))
        }

        SlotSymbol.LEMON -> {
            drawOval(LemonYellow, point(LemonCorner), size(LemonSize))
        }

        SlotSymbol.BELL -> {
            val bell =
                Path().apply {
                    moveTo(point(BellLeft).x, point(BellLeft).y)
                    cubicTo(
                        point(BellPullLeft).x,
                        point(BellPullLeft).y,
                        point(BellPullRight).x,
                        point(BellPullRight).y,
                        point(BellRight).x,
                        point(BellRight).y,
                    )
                    close()
                }
            drawPath(bell, BellGold)
            drawCircle(BellGold, unit, point(Clapper))
        }

        SlotSymbol.BAR -> {
            drawRect(Color.Black, point(BarCorner), size(BarSize))
            word("BAR", Color.White, middle, unit * BAR_TEXT, measurer)
        }

        SlotSymbol.SEVEN -> {
            word("7", SevenRed, middle, unit * SEVEN_TEXT, measurer)
        }

        SlotSymbol.DIAMOND -> {
            val gem =
                Path().apply {
                    GemCorners.forEachIndexed { index, corner ->
                        val p = point(corner)
                        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    close()
                }
            drawPath(gem, Gem)
            drawPath(gem, Color.White, style = Stroke(unit * EDGE))
        }
    }
}

private fun DrawScope.word(
    text: String,
    color: Color,
    middle: Offset,
    height: Float,
    measurer: TextMeasurer,
) {
    val laid = measurer.measure(text, TextStyle(color = color, fontWeight = FontWeight.Black, fontSize = height.toSp()))
    drawText(laid, topLeft = middle - Offset(laid.size.width / 2f, laid.size.height / 2f))
}

// Where each shape's points sit on the grid.
private val Cherries = listOf(Offset(-2f, 1f), Offset(2f, 1f))
private val CherryDrop = Offset(0f, 1f)
private val Stalk = Offset(1f, -3f)
private val LemonCorner = Offset(-3f, -2f)
private val LemonSize = Size(6f, 4f)
private val BellLeft = Offset(-3f, 2f)
private val BellRight = Offset(3f, 2f)
private val BellPullLeft = Offset(-3f, -4f)
private val BellPullRight = Offset(3f, -4f)
private val Clapper = Offset(0f, 3f)
private val BarCorner = Offset(-4f, -2f)
private val BarSize = Size(8f, 4f)
private val GemCorners = listOf(Offset(0f, 3f), Offset(-3f, -1f), Offset(-2f, -2f), Offset(2f, -2f), Offset(3f, -1f))

private val Leaf = Color(0xFF2E7D32)
private val CherryRed = Color(0xFFD32F2F)
private val LemonYellow = Color(0xFFFDD835)
private val BellGold = Color(0xFFFFB300)
private val SevenRed = Color(0xFFC62828)
private val Gem = Color(0xFF26C6DA)

private const val UNITS = 10f
private const val STEM = 0.5f
private const val CHERRY_RADIUS = 2f
private const val BAR_TEXT = 2.5f
private const val SEVEN_TEXT = 6f
private const val EDGE = 0.3f
