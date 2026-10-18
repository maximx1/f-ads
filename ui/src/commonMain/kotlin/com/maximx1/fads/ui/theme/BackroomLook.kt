package com.maximx1.fads.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

/**
 * A card game in somebody's back room. One bare bulb hangs over the table a third of the way down the screen:
 * the baize glows under it and everything falls into shadow towards the edges, cards and buttons included.
 * Smoke from the other players drifts through now and again, and the deck has seen a lot of hands.
 */
internal object BackroomLook : Look {
    override val slotColor = Color(0x40E2BF7A)
    override val accent = Brass
    override val clock =
        ClockLook(TextStyle(color = Color(0xFFF6DFA8), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold))
    override val buttons =
        ButtonLook(
            shape = RoundedCornerShape(3.dp),
            ink = Color(0xFFFFE2A0),
            etch = Color(0xCC140A04),
            divider = Color(0xCC140A04),
            border = BorderStroke(1.dp, Brass),
            fill = {
                drawRect(Brush.verticalGradient(Wood))
                drawRect(Grain.downward(GrainHeight.toPx(), TileMode.Repeated))
            },
        )

    override fun DrawScope.drawTable() {
        drawRect(Baize)
        val bulb = bulb()
        drawRect(Lamplight.around(bulb, reach(bulb)))
    }

    override fun cards(measurer: TextMeasurer): CardStyle = WornCards(measurer)

    @Composable
    override fun Overlay(modifier: Modifier) {
        Smoke(modifier)
        // Over the smoke, so that it too is only lit where the lamp reaches. A layer of its own because it never
        // changes, while the smoke under it is painted afresh every frame.
        Spacer(
            modifier.graphicsLayer().drawBehind {
                val bulb = bulb()
                drawRect(Shade.around(bulb, reach(bulb)))
            },
        )
    }
}

/** Where the bulb hangs: in the middle, a third of the way down. */
private fun DrawScope.bulb() = Offset(size.width / 2, size.height * BULB_DROP)

/** How far it is from the bulb to the farthest corner of the screen. */
private fun DrawScope.reach(bulb: Offset) = hypot(max(bulb.x, size.width - bulb.x), max(bulb.y, size.height - bulb.y))

/** Cards that have been through many hands: uneven corners, nicked and grimy edges, the odd bent corner. */
private class WornCards(
    private val measurer: TextMeasurer,
) : CardStyle {
    // Old red, the lattice faded to the colour of the paper.
    override fun DrawScope.drawBack() = drawPlainBack(CardBack, CardBackLines)

    private val wear = HashMap<Card, Wear>()
    private var grime = emptyList<Brush>()
    private var wornAt = Size.Unspecified

    override fun DrawScope.drawCard(card: Card) {
        // The damage is worked out once for each card and kept: it is slow to cut, and must not change.
        if (size != wornAt) {
            wear.clear()
            grime = grime(size)
            wornAt = size
        }
        val damage = wear.getOrPut(card) { Wear.of(card, size) }
        val width = size.width
        clipPath(damage.outline) {
            drawRect(Paper)
            drawGlow(damage.stain, width * STAIN_SIZE, Stain)
            grime.forEach(::drawRect)
            drawPips(measurer, card, if (card.color == SuitColor.RED) RedInk else BlackInk)
            damage.crease?.let { (from, to) ->
                drawLine(CreaseShadow, from, to, strokeWidth = width * CREASE_WIDTH)
                val lit = Offset(width * CREASE_WIDTH, width * CREASE_WIDTH)
                drawLine(CreaseLight, from + lit, to + lit, strokeWidth = width * CREASE_WIDTH)
            }
        }
        drawPath(damage.outline, PaperEdge, style = Stroke(width * EDGE_WIDTH))
    }
}

/** Dirt worked in from each of the four edges, thickest at the edge. It doubles up in the corners, as it would. */
private fun grime(size: Size): List<Brush> {
    val depth = size.width * GRIME_DEPTH
    val fade = listOf(Grime, Color.Transparent)
    return listOf(
        Brush.horizontalGradient(fade, startX = 0f, endX = depth),
        Brush.horizontalGradient(fade, startX = size.width, endX = size.width - depth),
        Brush.verticalGradient(fade, startY = 0f, endY = depth),
        Brush.verticalGradient(fade, startY = size.height, endY = size.height - depth),
    )
}

/** One card's damage. The same card is always damaged the same way. */
private class Wear(
    val outline: Path,
    val stain: Offset,
    val crease: Pair<Offset, Offset>?,
) {
    companion object {
        fun of(
            card: Card,
            size: Size,
        ): Wear {
            val random = Random(card.code.hashCode())
            val width = size.width

            fun corner() = CornerRadius(width * (SHARPEST_CORNER + random.nextFloat() * CORNER_RANGE))

            val whole = Path().apply { addRoundRect(RoundRect(size.toRect(), corner(), corner(), corner(), corner())) }
            val chips = Path()
            repeat(FEWEST_CHIPS + random.nextInt(EXTRA_CHIPS)) { chips.addOval(random.chip(size)) }
            return Wear(
                outline = Path.combine(PathOperation.Difference, whole, chips),
                stain = Offset(size.width * random.nextFloat(), size.height * random.nextFloat()),
                crease = if (random.nextInt(CARDS_PER_CREASE) == 0) random.creaseAcrossATopCorner(width) else null,
            )
        }

        /** A sliver worn off one edge: long and shallow, centred on the edge so that half of it bites in. */
        private fun Random.chip(size: Size): Rect {
            val length = size.width * (SHORTEST_CHIP + nextFloat() * CHIP_RANGE)
            val depth = length * CHIP_DEPTH
            val along = nextFloat()
            return when (nextInt(SIDES)) {
                0 -> oval(Offset(size.width * along, 0f), length, depth)
                1 -> oval(Offset(size.width, size.height * along), depth, length)
                2 -> oval(Offset(size.width * along, size.height), length, depth)
                else -> oval(Offset(0f, size.height * along), depth, length)
            }
        }

        private fun oval(
            centre: Offset,
            halfWidth: Float,
            halfHeight: Float,
        ) = Rect(centre.x - halfWidth, centre.y - halfHeight, centre.x + halfWidth, centre.y + halfHeight)

        // The top corners, because those still show when a card is half covered in a column.
        private fun Random.creaseAcrossATopCorner(width: Float): Pair<Offset, Offset> {
            val reach = width * (SHORTEST_CREASE + nextFloat() * CREASE_RANGE)
            return if (nextBoolean()) {
                Offset(reach, 0f) to Offset(0f, reach)
            } else {
                Offset(width - reach, 0f) to Offset(width, reach)
            }
        }
    }
}

private val Brass = Color(0xFFC9A25A)
private val Baize = Color(0xFF143A27)
private const val BULB_DROP = 1f / 3

// Both are measured from the bulb out to the farthest corner. The lamplight goes under the cards and the shade
// over them, so the table is brightest, and the cards cleanest, right under the bulb.
private val Lamplight =
    arrayOf(
        0f to Color(0xB3FFE9B0),
        0.12f to Color(0x80FFDF99),
        0.35f to Color(0x38FFCB73),
        0.7f to Color(0x00FFCB73),
    )
private val Shade =
    arrayOf(
        0f to Color(0x00000000),
        0.22f to Color(0x00000000),
        0.6f to Color(0x52050302),
        1f to Color(0x85050302),
    )

private val Wood = listOf(Color(0xFF6E4423), Color(0xFF3B2211))
private val Grain =
    arrayOf(
        0f to Color(0x00000000),
        0.3f to Color(0x33000000),
        0.45f to Color(0x0DFFFFFF),
        0.8f to Color(0x40000000),
        1f to Color(0x00000000),
    )
private val GrainHeight = 9.dp

private val Paper = Color(0xFFEADDBC)
private val PaperEdge = Color(0xB35C4A2C)
private val Stain = Color(0x33794F1C)
private val Grime = Color(0x595A3A14)
private val CreaseShadow = Color(0x40382510)
private val CreaseLight = Color(0x40FFF8E4)
private val RedInk = TextStyle(color = Color(0xFF982B1F), fontFamily = FontFamily.Serif)
private val BlackInk = TextStyle(color = Color(0xFF2A241D), fontFamily = FontFamily.Serif)

// Fractions of the card's width.
private const val GRIME_DEPTH = 0.2f
private const val STAIN_SIZE = 0.45f
private const val EDGE_WIDTH = 0.012f
private const val SHARPEST_CORNER = 0.07f
private const val CORNER_RANGE = 0.09f
private const val SHORTEST_CHIP = 0.025f
private const val CHIP_RANGE = 0.05f
private const val SHORTEST_CREASE = 0.2f
private const val CREASE_RANGE = 0.14f
private const val CREASE_WIDTH = 0.012f
private const val CHIP_DEPTH = 0.3f
private const val FEWEST_CHIPS = 2
private const val EXTRA_CHIPS = 4
private const val CARDS_PER_CREASE = 3
private const val SIDES = 4
private val CardBack = Color(0xFF7A1F1F)
private val CardBackLines = Color(0x33F0D9A8)
