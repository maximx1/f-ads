package com.maximx1.fads.ui.den

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.core.den.PocketColor
import com.maximx1.fads.core.den.Wheel
import com.maximx1.fads.ui.Strings

// How the roulette layout and the chips on it are drawn, in cells of [RouletteLayout].

internal fun DrawScope.drawNumbers(
    cell: Size,
    measurer: TextMeasurer,
) {
    val left = RouletteLayout.NUMBERS_LEFT * cell.width
    box(Offset(left, 0f), Size(cell.width * RouletteLayout.COLUMNS, cell.height), PocketGreen, "0", measurer)
    for (row in 0 until RouletteLayout.ROWS) {
        for (column in 0 until RouletteLayout.COLUMNS) {
            val number = RouletteLayout.number(row, column)
            val at = Offset(left + column * cell.width, (RouletteLayout.NUMBERS_TOP + row) * cell.height)
            val fill = if (Wheel.colorOf(number) == PocketColor.RED) PocketRed else PocketBlack
            box(at, cell, fill, number.toString(), measurer)
        }
    }
    val columnsTop = (RouletteLayout.NUMBERS_TOP + RouletteLayout.ROWS) * cell.height
    for (column in 0 until RouletteLayout.COLUMNS) {
        box(Offset(left + column * cell.width, columnsTop), cell, null, Strings.COLUMN_BET, measurer)
    }
}

internal fun DrawScope.drawOutsideBets(
    cell: Size,
    measurer: TextMeasurer,
) {
    val top = RouletteLayout.NUMBERS_TOP * cell.height
    val dozen = Size(cell.width, cell.height * RouletteLayout.DOZEN_ROWS)
    for (index in 0 until RouletteLayout.ROWS / RouletteLayout.DOZEN_ROWS) {
        box(Offset(0f, top + index * dozen.height), dozen, null, Strings.dozen(index), measurer, upright = false)
    }
    val pair = Size(cell.width, cell.height * RouletteLayout.EVEN_MONEY_ROWS)
    RouletteLayout.evenMoney.forEachIndexed { index, kind ->
        val at = Offset(cell.width, top + index * pair.height)
        when (val label = Strings.evenMoney(kind)) {
            null -> {
                box(at, pair, null, "", measurer)
                drawDiamond(at, pair, if (kind == BetKind.RED) PocketRed else PocketBlack)
            }

            else -> {
                box(at, pair, null, label, measurer, upright = false)
            }
        }
    }
}

// A cell of the layout, [fill]ed if it has a colour, with its [label] across it or, unless [upright], up it.
@Suppress("LongParameterList") // A box and what goes in it.
private fun DrawScope.box(
    at: Offset,
    size: Size,
    fill: Color?,
    label: String,
    measurer: TextMeasurer,
    upright: Boolean = true,
) {
    if (fill != null) drawRect(fill, at + Offset(LINE, LINE), Size(size.width - LINE * 2, size.height - LINE * 2))
    drawRect(Lines, at, size, style = Stroke(LINE))
    if (label.isEmpty()) return
    // As big as the box's narrow side allows, and smaller if it would run out of the box's long one.
    val room = (if (upright) size.width else size.height) * LABEL_ROOM
    val wanted = minOf(size.width, size.height) * LABEL_SIZE
    val trial = measurer.measure(label, Label.copy(fontSize = wanted.toSp()))
    val fontSize = (wanted * minOf(1f, room / trial.size.width)).toSp()
    val text = measurer.measure(label, Label.copy(fontSize = fontSize))
    val centre = at + Offset(size.width / 2, size.height / 2)
    val topLeft = centre - Offset(text.size.width / 2f, text.size.height / 2f)
    if (upright) {
        drawText(text, topLeft = topLeft)
    } else {
        rotate(-QUARTER_TURN, centre) { drawText(text, topLeft = topLeft) }
    }
}

private fun DrawScope.drawDiamond(
    at: Offset,
    size: Size,
    color: Color,
) {
    val centre = at + Offset(size.width / 2, size.height / 2)
    val half = minOf(size.width, size.height) * DIAMOND
    val diamond =
        Path().apply {
            moveTo(centre.x, centre.y - half)
            lineTo(centre.x + half * DIAMOND_WIDTH, centre.y)
            lineTo(centre.x, centre.y + half)
            lineTo(centre.x - half * DIAMOND_WIDTH, centre.y)
            close()
        }
    drawPath(diamond, color)
    drawPath(diamond, Lines, style = Stroke(LINE))
}

// The chips on [bet]: a coin of the largest kind the stake holds, with the stake written on it.
internal fun DrawScope.drawChip(
    bet: Bet,
    stake: Long,
    cell: Size,
    measurer: TextMeasurer,
) {
    val (across, down) = RouletteLayout.centreOf(bet)
    val centre = Offset(across.toFloat() * cell.width, down.toFloat() * cell.height)
    val radius = minOf(cell.width, cell.height) * CHIP
    val coin = Coin.entries.first { stake >= it.worth }
    drawCircle(
        Color.Black.copy(alpha = SHADOW),
        radius,
        centre + Offset(radius * SHADOW_OFFSET, radius * SHADOW_OFFSET),
    )
    drawCoin(coin, centre, radius)
    // Thousands as K, so a high roller's chip still reads at a glance.
    val written = if (stake >= THOUSAND && stake % THOUSAND == 0L) "${stake / THOUSAND}K" else stake.toString()
    val text =
        measurer.measure(
            written,
            Stake.copy(fontSize = (radius * STAKE_SIZE / maxOf(1, written.length - 1)).toSp()),
        )
    drawText(text, topLeft = centre - Offset(text.size.width / 2f, text.size.height / 2f))
}

private val PocketGreen = Color(0xFF1B7A3A)
private val PocketRed = Color(0xFFB3202A)
private val PocketBlack = Color(0xFF1A1A1A)
private val Lines = Color(0xCCFFFFFF)
private val Label = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
private val Stake = TextStyle(color = Color.Black, fontWeight = FontWeight.Bold)
private const val LINE = 2f
private const val LABEL_SIZE = 0.38f
private const val LABEL_ROOM = 0.85f
private const val QUARTER_TURN = 90f
private const val DIAMOND = 0.38f
private const val DIAMOND_WIDTH = 0.6f
private const val CHIP = 0.34f
private const val STAKE_SIZE = 0.8f
private const val THOUSAND = 1_000L
private const val SHADOW = 0.35f
private const val SHADOW_OFFSET = 0.12f
