package com.maximx1.fads.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.maximx1.fads.core.mahjong.TileFace
import com.maximx1.fads.core.mahjong.TileSuit

/** How a tile is marked out for the player. */
internal enum class TileMark { NONE, PICKED, HINTED }

@Composable
internal fun rememberTileStyle(): TileStyle {
    val measurer = rememberTextMeasurer(cacheSize = TEXT_CACHE_SIZE)
    return remember(measurer) { TileStyle(measurer) }
}

/**
 * Draws mahjong tiles in code: an ivory face on a block that shows its thickness at the right and the bottom. Dots
 * and bamboo are drawn as pictures; characters, winds, dragons and seasons are written, with a number or a letter
 * in the corner for anyone who does not read them.
 */
internal class TileStyle(
    private val measurer: TextMeasurer,
) {
    /**
     * One tile whose face is [face] in size, raised by [depth]: the draw area must be that much bigger than the face
     * at the right and the bottom. [mark] picks it out in [markColor].
     */
    fun DrawScope.drawTile(
        tile: TileFace,
        face: Size,
        depth: Float,
        mark: TileMark,
        markColor: Color,
    ) {
        val corner = CornerRadius(face.width * CORNER)
        drawRoundRect(Base, Offset(depth, depth), face, corner)
        drawRoundRect(Side, Offset(depth / 2, depth / 2), face, corner)
        drawRoundRect(Ivory, Offset.Zero, face, corner)
        drawRoundRect(Edge, Offset.Zero, face, corner, Stroke(face.width * EDGE))
        val pad = face.width * PADDING
        val box = Picture(Offset(pad, pad), Size(face.width - pad * 2, face.height - pad * 2))
        drawFace(tile, box)
        if (mark == TileMark.PICKED) drawRoundRect(markColor.copy(alpha = PICKED_FILL), Offset.Zero, face, corner)
        if (mark != TileMark.NONE) drawRoundRect(markColor, Offset.Zero, face, corner, Stroke(face.width * MARK_LINE))
    }

    private fun DrawScope.drawFace(
        tile: TileFace,
        box: Picture,
    ) {
        val rank = tile.rank
        when (tile.suit) {
            TileSuit.DOTS -> {
                drawDots(rank, box)
            }

            TileSuit.BAMBOO -> {
                drawBamboo(rank, box)
            }

            TileSuit.CHARACTERS -> {
                write(NUMERALS[rank - 1], Navy, box.at(HALF, UPPER), box.size.width * CHARACTER_SIZE)
                write(TEN_THOUSAND, Red, box.at(HALF, LOWER), box.size.width * CHARACTER_SIZE)
                corner(rank.toString(), box)
            }

            TileSuit.WINDS -> {
                write(WINDS[rank - 1], Navy, box.centre, box.size.width * HONOUR_SIZE)
                corner(WIND_LETTERS[rank - 1], box)
            }

            TileSuit.DRAGONS -> {
                drawDragon(rank, box)
            }

            TileSuit.FLOWERS -> {
                drawFlower(FlowerColours[rank - 1], box)
                corner(rank.toString(), box)
            }

            TileSuit.SEASONS -> {
                write(SEASONS[rank - 1], SeasonColours[rank - 1], box.centre, box.size.width * HONOUR_SIZE)
                corner(rank.toString(), box)
            }
        }
    }

    private fun DrawScope.drawDots(
        count: Int,
        box: Picture,
    ) {
        val radius = box.size.width * DOT_RADII[count - 1]
        DOTS[count - 1].forEachIndexed { index, (x, y) ->
            val centre = box.at(x, y)
            drawCircle(DOT_COLOURS[count - 1][index], radius, centre)
            drawCircle(Ivory, radius * DOT_RING, centre)
            drawCircle(DOT_COLOURS[count - 1][index], radius * DOT_HEART, centre)
        }
    }

    private fun DrawScope.drawBamboo(
        count: Int,
        box: Picture,
    ) {
        val sticks = BAMBOO[count - 1]
        val rows = sticks.map { it.second }.distinct().size
        val length = box.size.height * (STICK_ROOM / rows)
        val width = box.size.width * STICK_WIDTH
        sticks.forEachIndexed { index, (x, y) ->
            val colour = if (index in RED_STICKS[count - 1]) Red else Green
            val topLeft = box.at(x, y) - Offset(width / 2, length / 2)
            val round = CornerRadius(width / 2)
            drawRoundRect(colour, topLeft, Size(width, length), round)
            // Knots where the stick is jointed.
            for (knot in 1 until KNOTS) {
                val y = topLeft.y + length * knot / KNOTS
                drawLine(Ivory, Offset(topLeft.x, y), Offset(topLeft.x + width, y), width * KNOT_LINE)
            }
        }
    }

    private fun DrawScope.drawDragon(
        rank: Int,
        box: Picture,
    ) {
        when (rank) {
            RED_DRAGON -> {
                write(RED_DRAGON_TEXT, Red, box.centre, box.size.width * HONOUR_SIZE)
            }

            GREEN_DRAGON -> {
                write(GREEN_DRAGON_TEXT, Green, box.centre, box.size.width * HONOUR_SIZE)
            }

            else -> {
                // The white dragon: a blue frame round a blank face.
                val inset = box.size.width * FRAME_INSET
                val frame = Size(box.size.width - inset * 2, box.size.height - inset * 2)
                val line = Stroke(box.size.width * FRAME_LINE)
                drawRect(Blue, box.topLeft + Offset(inset, inset), frame, style = line)
                val inner = inset * 2
                drawRect(
                    Blue,
                    box.topLeft + Offset(inner, inner),
                    Size(box.size.width - inner * 2, box.size.height - inner * 2),
                    style = Stroke(box.size.width * FRAME_LINE / 2),
                )
            }
        }
    }

    /** A five-petalled flower on a stem. */
    private fun DrawScope.drawFlower(
        colour: Color,
        box: Picture,
    ) {
        val centre = box.at(HALF, FLOWER_HEIGHT)
        val petal = box.size.width * PETAL
        drawLine(Green, centre, box.at(HALF, 1f), box.size.width * STEM)
        for (index in 0 until PETALS) {
            val angle = (index * FULL_TURN / PETALS - QUARTER_TURN) * DEGREES
            val at = centre + Offset(kotlin.math.cos(angle), kotlin.math.sin(angle)) * petal
            drawCircle(colour, petal * PETAL_SIZE, at)
        }
        drawCircle(Gold, petal * HEART_SIZE, centre)
    }

    /** [text] centred on [centre], [size] pixels high. */
    private fun DrawScope.write(
        text: String,
        colour: Color,
        centre: Offset,
        size: Float,
    ) {
        val laid =
            measurer.measure(text, TextStyle(color = colour, fontSize = size.toSp(), fontWeight = FontWeight.Bold))
        drawText(laid, topLeft = centre - Offset(laid.size.width / 2f, laid.size.height / 2f))
    }

    /** A small index in the top-left corner of the face. */
    private fun DrawScope.corner(
        text: String,
        box: Picture,
    ) {
        val laid =
            measurer.measure(
                text,
                TextStyle(color = Navy, fontSize = (box.size.width * INDEX_SIZE).toSp(), fontWeight = FontWeight.Bold),
            )
        drawText(laid, topLeft = box.topLeft - Offset(box.size.width * INDEX_OUT, box.size.width * INDEX_OUT))
    }
}

/** The part of a face the picture goes in. Points in it run from 0 to 1 across and down. */
private class Picture(
    val topLeft: Offset,
    val size: Size,
) {
    val centre get() = at(HALF, HALF)

    fun at(
        x: Float,
        y: Float,
    ) = topLeft + Offset(size.width * x, size.height * y)
}

private val Ivory = Color(0xFFFBF7EC)
private val Edge = Color(0xFFB9AE94)
private val Side = Color(0xFFCFC09A)
private val Base = Color(0xFF2F6F55)
private val Navy = Color(0xFF1A2A5C)
private val Red = Color(0xFFC62828)
private val Green = Color(0xFF2E7D32)
private val Blue = Color(0xFF1F5FA8)
private val Gold = Color(0xFFF2B705)

private const val TEN_THOUSAND = "萬"
private val NUMERALS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
private val WINDS = listOf("東", "南", "西", "北")
private val WIND_LETTERS = listOf("E", "S", "W", "N")
private val SEASONS = listOf("春", "夏", "秋", "冬")
private const val RED_DRAGON = 1
private const val GREEN_DRAGON = 2
private const val RED_DRAGON_TEXT = "中"
private const val GREEN_DRAGON_TEXT = "發"
private val FlowerColours = listOf(Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFFF57C00), Color(0xFF2E7D32))
private val SeasonColours = listOf(Color(0xFF2E7D32), Color(0xFFC62828), Color(0xFFE65100), Color(0xFF1F5FA8))

// Where the dots go for one to nine, as fractions of the face's picture area.
private val DOTS: List<List<Pair<Float, Float>>> =
    listOf(
        listOf(0.5f to 0.5f),
        listOf(0.5f to 0.25f, 0.5f to 0.75f),
        listOf(0.22f to 0.18f, 0.5f to 0.5f, 0.78f to 0.82f),
        grid(listOf(0.27f, 0.73f), listOf(0.27f, 0.73f)),
        listOf(0.25f to 0.2f, 0.75f to 0.2f, 0.5f to 0.5f, 0.25f to 0.8f, 0.75f to 0.8f),
        grid(listOf(0.28f, 0.72f), listOf(0.18f, 0.5f, 0.82f)),
        listOf(0.2f to 0.12f, 0.5f to 0.25f, 0.8f to 0.38f) + grid(listOf(0.28f, 0.72f), listOf(0.64f, 0.88f)),
        grid(listOf(0.28f, 0.72f), listOf(0.12f, 0.37f, 0.63f, 0.88f)),
        grid(listOf(0.2f, 0.5f, 0.8f), listOf(0.18f, 0.5f, 0.82f)),
    )
private val DOT_RADII = listOf(0.36f, 0.2f, 0.16f, 0.17f, 0.15f, 0.14f, 0.12f, 0.11f, 0.12f)
private val DOT_COLOURS: List<List<Color>> =
    listOf(
        listOf(Red),
        listOf(Green, Blue),
        listOf(Blue, Red, Green),
        listOf(Blue, Green, Green, Blue),
        listOf(Blue, Green, Red, Green, Blue),
        List(2) { Green } + List(4) { Red },
        List(3) { Green } + List(4) { Red },
        List(8) { Blue },
        List(3) { Blue } + List(3) { Red } + List(3) { Green },
    )

// Where the sticks go for one to nine bamboo, and which of them are red.
private val BAMBOO: List<List<Pair<Float, Float>>> =
    listOf(
        listOf(0.5f to 0.5f),
        listOf(0.5f to 0.25f, 0.5f to 0.75f),
        listOf(0.5f to 0.25f, 0.3f to 0.75f, 0.7f to 0.75f),
        grid(listOf(0.3f, 0.7f), listOf(0.25f, 0.75f)),
        grid(listOf(0.22f, 0.78f), listOf(0.25f, 0.75f)) + (0.5f to 0.5f),
        grid(listOf(0.2f, 0.5f, 0.8f), listOf(0.25f, 0.75f)),
        listOf(0.5f to 0.17f) + grid(listOf(0.2f, 0.5f, 0.8f), listOf(0.5f, 0.83f)),
        grid(listOf(0.14f, 0.38f, 0.62f, 0.86f), listOf(0.25f, 0.75f)),
        grid(listOf(0.2f, 0.5f, 0.8f), listOf(0.17f, 0.5f, 0.83f)),
    )
private val RED_STICKS: List<Set<Int>> =
    listOf(setOf(0), emptySet(), setOf(0), emptySet(), setOf(4), emptySet(), setOf(0), emptySet(), setOf(1, 4, 7))

/** Every point at one of [xs] across and one of [ys] down, row by row. */
private fun grid(
    xs: List<Float>,
    ys: List<Float>,
): List<Pair<Float, Float>> = ys.flatMap { y -> xs.map { x -> x to y } }

private const val TEXT_CACHE_SIZE = 64

// Fractions of the face's width, or of the picture area's for what goes in it.
private const val CORNER = 0.12f
private const val EDGE = 0.025f
private const val PADDING = 0.12f
private const val MARK_LINE = 0.08f
private const val PICKED_FILL = 0.35f
private const val CHARACTER_SIZE = 0.62f
private const val HONOUR_SIZE = 0.82f
private const val INDEX_SIZE = 0.3f
private const val INDEX_OUT = 0.08f
private const val DOT_RING = 0.62f
private const val DOT_HEART = 0.3f
private const val STICK_ROOM = 0.86f
private const val STICK_WIDTH = 0.15f
private const val KNOTS = 3
private const val KNOT_LINE = 0.18f
private const val FRAME_INSET = 0.08f
private const val FRAME_LINE = 0.08f
private const val PETALS = 5
private const val PETAL = 0.22f
private const val PETAL_SIZE = 0.75f
private const val HEART_SIZE = 0.55f
private const val STEM = 0.06f
private const val FLOWER_HEIGHT = 0.4f
private const val HALF = 0.5f
private const val UPPER = 0.27f
private const val LOWER = 0.73f
private const val FULL_TURN = 360f
private const val QUARTER_TURN = 90f
private const val DEGREES = (kotlin.math.PI / 180).toFloat()
