package com.maximx1.fads.ui.den

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** [pattern] across the whole draw area in [ink], in a weave about an eighteenth of its smaller side. */
internal fun DrawScope.drawPattern(
    pattern: FeltPattern,
    ink: Color,
) {
    val step = size.minDimension / WEAVES
    val line = Stroke(step * THREAD)
    when (pattern) {
        FeltPattern.PLAIN -> {
            Unit
        }

        FeltPattern.PINSTRIPE -> {
            grid(step, 1f) { at -> drawLine(ink, at, at + Offset(0f, step), line.width) }
        }

        FeltPattern.DOTS -> {
            grid(step, 1f) { at -> drawCircle(ink, step * DOT, at) }
        }

        FeltPattern.WAVES -> {
            waves(step, ink, line)
        }

        FeltPattern.HEXES -> {
            grid(
                step,
                HEX_ROWS,
            ) { at -> drawPath(polygon(at, step / 2, HEX_SIDES, 0f), ink, style = line) }
        }

        FeltPattern.STARS -> {
            grid(step * 2, 1f) { at -> drawPath(star(at, step / 2), ink) }
        }

        FeltPattern.SCALES -> {
            scales(step, ink, line)
        }

        FeltPattern.LATTICE -> {
            lattice(step, ink, line)
        }

        FeltPattern.SUNBURST -> {
            sunburst(ink, line)
        }

        FeltPattern.DIAMONDS -> {
            grid(
                step,
                1f,
            ) { at -> drawPath(polygon(at, step / 2, DIAMOND_SIDES, 0f), ink, style = line) }
        }
    }
}

// Calls [mark] at every point of a grid [step] apart across and [rows] steps apart down, every other row offset.
private inline fun DrawScope.grid(
    step: Float,
    rows: Float,
    mark: (Offset) -> Unit,
) {
    var row = 0
    var y = 0f
    while (y < size.height + step) {
        var x = if (row % 2 == 0) 0f else step / 2
        while (x < size.width + step) {
            mark(Offset(x, y))
            x += step
        }
        y += step * rows
        row++
    }
}

private fun DrawScope.waves(
    step: Float,
    ink: Color,
    line: Stroke,
) {
    var y = 0f
    while (y < size.height + step) {
        val wave = Path().apply { moveTo(0f, y) }
        var x = 0f
        while (x < size.width) {
            wave.relativeQuadraticTo(step / 2, -step / 2, step, 0f)
            wave.relativeQuadraticTo(step / 2, step / 2, step, 0f)
            x += step * 2
        }
        drawPath(wave, ink, style = line)
        y += step
    }
}

private fun DrawScope.scales(
    step: Float,
    ink: Color,
    line: Stroke,
) = grid(step, HALF) { at ->
    drawArc(ink, 0f, HALF_TURN, false, at - Offset(step / 2, step / 2), Size(step, step), style = line)
}

private fun DrawScope.lattice(
    step: Float,
    ink: Color,
    line: Stroke,
) {
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(ink, Offset(x, 0f), Offset(x + size.height, size.height), line.width)
        drawLine(ink, Offset(x + size.height, 0f), Offset(x, size.height), line.width)
        x += step * 2
    }
}

private fun DrawScope.sunburst(
    ink: Color,
    line: Stroke,
) {
    for (ray in 0 until RAYS) {
        val angle = ray * 2 * PI / RAYS
        drawLine(
            ink,
            center,
            center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * size.maxDimension,
            line.width,
        )
    }
}

// A shape with [sides] corners round [centre], each [radius] from it, the first [turn] radians round from the top.
private fun polygon(
    centre: Offset,
    radius: Float,
    sides: Int,
    turn: Float,
): Path =
    Path().apply {
        for (corner in 0..sides) {
            val angle = turn + corner * 2 * PI / sides - PI / 2
            val point = centre + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
            if (corner == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

// A four-pointed star round [centre], its points [radius] out.
private fun star(
    centre: Offset,
    radius: Float,
): Path =
    Path().apply {
        for (corner in 0..STAR_CORNERS) {
            val angle = corner * PI / (STAR_CORNERS / 2) - PI / 2
            val reach = if (corner % 2 == 0) radius else radius * STAR_WAIST
            val point = centre + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * reach
            if (corner == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

private const val WEAVES = 18f
private const val THREAD = 0.08f
private const val DOT = 0.12f
private const val HALF = 0.5f
private const val HALF_TURN = 180f
private const val HEX_ROWS = 0.87f
private const val HEX_SIDES = 6
private const val DIAMOND_SIDES = 4
private const val RAYS = 36
private const val STAR_CORNERS = 8
private const val STAR_WAIST = 0.35f
