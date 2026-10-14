package com.maximx1.fads.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.model.SuitColor

/**
 * Draws one card face, filling the draw area. The board only knows this interface, so real card designs can
 * replace [PlaceholderCardStyle] without touching the layout or the rules.
 */
fun interface CardStyle {
    fun DrawScope.drawCard(card: Card)
}

@Composable
fun rememberPlaceholderCardStyle(): CardStyle = rememberCardStyle(::PlaceholderCardStyle)

/** Builds a card style around a text measurer, which every style needs to draw the ranks. */
@Composable
fun rememberCardStyle(create: (TextMeasurer) -> CardStyle): CardStyle {
    val measurer = rememberTextMeasurer(cacheSize = TEXT_CACHE_SIZE)
    return remember(measurer, create) { create(measurer) }
}

/** Plain cards drawn in code: the rank as text, the suit as a simple shape. */
class PlaceholderCardStyle(
    private val measurer: TextMeasurer,
) : CardStyle {
    override fun DrawScope.drawCard(card: Card) {
        val width = size.width
        val corner = CornerRadius(width * CORNER)
        drawRoundRect(Face, cornerRadius = corner)
        drawRoundRect(Edge, cornerRadius = corner, style = Stroke(width * EDGE_WIDTH))

        drawPips(measurer, card, TextStyle(color = if (card.color == SuitColor.RED) Red else Black))
    }

    private companion object {
        val Face = Color(0xFFFAFAFA)
        val Edge = Color(0xFF9E9E9E)
        val Red = Color(0xFFC62828)
        val Black = Color(0xFF212121)

        // All sizes are fractions of the card's width, so cards look the same at any size.
        const val CORNER = 0.09f
        const val EDGE_WIDTH = 0.015f
    }
}

/**
 * Draws what every card face has, whatever its style: the rank top left, a small suit top right and a large one
 * below. [ink] gives the colour, and may add a font or a shadow.
 */
fun DrawScope.drawPips(
    measurer: TextMeasurer,
    card: Card,
    ink: TextStyle,
) {
    val width = size.width
    val pad = width * PADDING
    val rank =
        measurer.measure(
            text = card.rank.label,
            style = ink.merge(fontSize = (width * RANK_SIZE).toSp(), fontWeight = FontWeight.Bold),
        )
    drawText(rank, topLeft = Offset(pad, 0f))

    val small = width * CORNER_SUIT
    drawSuit(card.suit, ink.color, Offset(width - pad - small, pad), small)
    val centre = width * CENTRE_SUIT
    drawSuit(card.suit, ink.color, Offset((width - centre) / 2, size.height - centre - pad * 2), centre)
}

/** Draws a suit shape in a square of side [size] whose top-left corner is at [topLeft]. */
fun DrawScope.drawSuit(
    suit: Suit,
    color: Color,
    topLeft: Offset,
    size: Float,
) {
    val scale = size / SuitShapes.BOX
    withTransform({
        translate(topLeft.x, topLeft.y)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(SuitShapes.of(suit), color)
    }
}

/** Suit outlines in a 100 x 100 box, each built from one or more overlapping pieces of SVG path data. */
private object SuitShapes {
    const val BOX = 100f

    private const val STEM = "M50,58 C52,80 58,92 68,97 L32,97 C42,92 48,80 50,58 Z"

    private val shapes: Map<Suit, Path> =
        mapOf(
            Suit.SPADES to
                shape(
                    "M50,3 C85,28 100,45 100,60 C100,73 89,80 76,80 C65,80 55,74 50,64 " +
                        "C45,74 35,80 24,80 C11,80 0,73 0,60 C0,45 15,28 50,3 Z",
                    STEM,
                ),
            Suit.HEARTS to
                shape(
                    "M50,95 C15,70 0,50 0,30 C0,13 12,5 26,5 C37,5 46,12 50,22 " +
                        "C54,12 63,5 74,5 C88,5 100,13 100,30 C100,50 85,70 50,95 Z",
                ),
            Suit.DIAMONDS to shape("M50,2 L90,50 L50,98 L10,50 Z"),
            Suit.CLUBS to
                shape(
                    "M28,25 a22,22 0 1,1 44,0 a22,22 0 1,1 -44,0 Z",
                    "M3,57 a22,22 0 1,1 44,0 a22,22 0 1,1 -44,0 Z",
                    "M53,57 a22,22 0 1,1 44,0 a22,22 0 1,1 -44,0 Z",
                    "M38,48 a12,12 0 1,1 24,0 a12,12 0 1,1 -24,0 Z",
                    STEM,
                ),
        )

    fun of(suit: Suit): Path = shapes.getValue(suit)

    private fun shape(vararg pieces: String): Path =
        pieces
            .map { PathParser().parsePathString(it).toPath() }
            .reduce { whole, piece -> Path.combine(PathOperation.Union, whole, piece) }
}

private const val TEXT_CACHE_SIZE = 32

// Fractions of the card's width.
private const val PADDING = 0.08f
private const val RANK_SIZE = 0.36f
private const val CORNER_SUIT = 0.28f
private const val CENTRE_SUIT = 0.56f
