package com.maximx1.fads.ui.den

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.drawPlainBack

/** The patterns woven into a den theme's felt. */
enum class FeltPattern { PLAIN, PINSTRIPE, DOTS, WAVES, HEXES, STARS, SCALES, LATTICE, SUNBURST, DIAMONDS }

/**
 * How the den looks: the [felt] of its tables with a [pattern] in it, the [rim] round them, the [back] of the cards
 * with its [backLines], the [accent] of its buttons, and an [emblem] at the heart of each table. [key] is what is
 * saved, so it never changes; [name] is what the player sees.
 */
@Suppress("LongParameterList") // One for each part of the look.
class DenTheme(
    val key: String,
    val name: String,
    val felt: Color,
    val pattern: FeltPattern,
    val rim: Color,
    val back: Color,
    val accent: Color,
    val emblem: String,
) {
    /** The felt at its edges, where the light falls off. */
    val feltDeep: Color get() = lerp(felt, Color.Black, DEEP)

    val rimDeep: Color get() = lerp(rim, Color.Black, DEEP)

    val backLines: Color get() = lerp(back, Color.White, LINES)

    /** The room round the tables, darker than the felt. */
    val room: Color get() = lerp(felt, Color.Black, ROOM)

    /** [base]'s card faces, with this theme's backs. */
    fun cards(base: CardStyle): CardStyle =
        object : CardStyle {
            override fun DrawScope.drawCard(card: Card) = with(base) { drawCard(card) }

            override fun DrawScope.drawBack() = drawPlainBack(back, backLines)
        }
}

/** The theme's felt across the draw area, lit from the middle, with its pattern faint over it. */
internal fun DrawScope.drawFelt(theme: DenTheme) {
    drawRect(Brush.radialGradient(listOf(theme.felt, theme.feltDeep), center, size.maxDimension * REACH))
    clipRect { drawPattern(theme.pattern, Weave) }
}

/** The theme's emblem, large and faint, at the middle of the draw area. */
internal fun DrawScope.drawEmblem(
    theme: DenTheme,
    measurer: TextMeasurer,
) {
    val height = size.minDimension * EMBLEM
    val text =
        measurer.measure(
            theme.emblem,
            TextStyle(color = Color.White.copy(alpha = EMBLEM_ALPHA), fontSize = height.toSp()),
        )
    drawText(text, topLeft = center - Offset(text.size.width / 2f, text.size.height / 2f))
}

/** A swatch of the theme, for the shop: its felt in its rim, the emblem, and one of its cards face down. */
internal fun DrawScope.drawSwatch(
    theme: DenTheme,
    measurer: TextMeasurer,
) {
    val rim = size.minDimension * SWATCH_RIM
    drawRect(Brush.verticalGradient(listOf(theme.rim, theme.rimDeep)))
    val inner = Size(size.width - rim * 2, size.height - rim * 2)
    translateTo(Offset(rim, rim), inner) {
        drawFelt(theme)
        drawEmblem(theme, measurer)
        val card = Size(size.width * SWATCH_CARD, size.width * SWATCH_CARD * CARD_ASPECT)
        translateTo(
            Offset(size.width - card.width * SWATCH_CARD_INSET, size.height - card.height * SWATCH_CARD_INSET),
            card,
        ) {
            drawPlainBack(theme.back, theme.backLines)
        }
    }
}

// Draws [block] as if the draw area were [area] at [at].
private inline fun DrawScope.translateTo(
    at: Offset,
    area: Size,
    crossinline block: DrawScope.() -> Unit,
) {
    val outer = drawContext.size
    drawContext.transform.translate(at.x, at.y)
    drawContext.size = area
    try {
        block()
    } finally {
        drawContext.size = outer
        drawContext.transform.translate(-at.x, -at.y)
    }
}

private val Weave = Color.White.copy(alpha = 0.07f)

private const val DEEP = 0.45f
private const val LINES = 0.35f
private const val ROOM = 0.75f
private const val REACH = 0.7f
private const val EMBLEM = 0.28f
private const val EMBLEM_ALPHA = 0.12f
private const val SWATCH_RIM = 0.06f
private const val SWATCH_CARD = 0.22f
private const val SWATCH_CARD_INSET = 1.15f
private const val CARD_ASPECT = 1.4f
