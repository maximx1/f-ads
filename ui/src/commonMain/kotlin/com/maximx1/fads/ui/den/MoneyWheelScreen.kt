package com.maximx1.fads.ui.den

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.MoneyWheel
import com.maximx1.fads.core.den.MoneyWheelTable
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.WheelSymbol
import com.maximx1.fads.core.den.moneyWheel
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings

/**
 * The money wheel in [room], dressed in the den's theme: the wheel in the theme's rim with its emblem on the logo
 * segment and the pointer in its accent, and a box for each symbol to bet on.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MoneyWheelScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val table = den.moneyWheel(room)
    val theme = denTheme(den.theme)
    val chips = chipsFor(room)
    var chosen by rememberSaveable { mutableIntStateOf(chips.lastIndex) }
    var settled by rememberSaveable { mutableLongStateOf(table.spins) }
    val spinning = settled != table.spins
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.MONEY_WHEEL, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        // As big as fits both ways, so none of the wheel is cut off on a tall screen or a wide one.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Wheel(table, theme, onSettled = { settled = it }, modifier = Modifier.size(minOf(maxWidth, maxHeight)))
        }
        Text(
            if (spinning ||
                table.landed == null
            ) {
                ""
            } else {
                Strings.wheelResult(symbolLabel(MoneyWheel.segments[table.landed ?: 0], theme), table.paid)
            },
            color = theme.accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(WHEEL_RESULT_TAG),
        )
        Symbols(table, theme, onBet = { actions.betMoneyWheel(it, chips[chosen].worth, room) })
        BetControls(
            chips,
            chosen,
            ready = table.bets.isNotEmpty() && !spinning,
            act = Strings.SPIN,
            onChoose = { chosen = it },
            onClear = { actions.clearMoneyWheel(room) },
            onAct = { actions.spinMoneyWheel(room) },
        )
    }
}

/** A box for each symbol to bet on, in rows. */
@Composable
private fun Symbols(
    table: MoneyWheelTable,
    theme: DenTheme,
    onBet: (WheelSymbol) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in WheelSymbol.entries.chunked(BOXES_IN_ROW)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (symbol in row) {
                    LayoutBox(
                        Strings.wheelSymbol(symbol),
                        Strings.wheelOdds(symbol),
                        table.bets[symbol] ?: 0,
                        open = true,
                        theme = theme,
                        onTap = { onBet(symbol) },
                        modifier = Modifier.weight(1f).testTag(wheelTag(symbol)),
                    )
                }
            }
        }
    }
}

/** The wheel, turning to the segment the last spin stopped on when [MoneyWheelTable.spins] goes up. */
@Composable
private fun Wheel(
    table: MoneyWheelTable,
    theme: DenTheme,
    onSettled: (Long) -> Unit,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val settled by rememberUpdatedState(onSettled)
    val turn = remember { Animatable(restingTurn(table.landed)) }
    var shown by rememberSaveable { mutableLongStateOf(table.spins) }
    LaunchedEffect(table.spins) {
        val landed = table.landed
        if (table.spins == shown || landed == null) return@LaunchedEffect
        shown = table.spins
        val from = turn.value
        val to = from - TURNS * FULL_TURN - (from - restingTurn(landed)).mod(FULL_TURN)
        turn.animateTo(to, tween(SPIN_MILLIS, easing = FastOutSlowInEasing))
        settled(table.spins)
    }
    Canvas(modifier.testTag(WHEEL_TAG)) { drawMoneyWheel(turn.value, theme, measurer) }
}

// The turn of the wheel, in degrees clockwise, that brings [segment] to the pointer at the top.
private fun restingTurn(segment: Int?): Float = if (segment == null) 0f else -segment * STEP

private fun DrawScope.drawMoneyWheel(
    turn: Float,
    theme: DenTheme,
    measurer: TextMeasurer,
) {
    val radius = size.minDimension / 2
    val face = radius * FACE
    drawCircle(Brush.radialGradient(listOf(theme.rim, theme.rimDeep), center, radius), radius)
    rotate(turn, center) {
        MoneyWheel.segments.forEachIndexed { index, symbol ->
            drawArc(
                fillOf(symbol, theme),
                startAngle = index * STEP - QUARTER - STEP / 2,
                sweepAngle = STEP,
                useCenter = true,
                topLeft = center - Offset(face, face),
                size = Size(face * 2, face * 2),
            )
            rotate(index * STEP, center) {
                val label =
                    measurer.measure(
                        symbolLabel(symbol, theme),
                        Label.copy(
                            color = inkOf(symbol),
                            fontSize =
                                (
                                    radius *
                                        LABEL
                                ).toSp(),
                        ),
                    )
                drawText(
                    label,
                    topLeft = Offset(center.x - label.size.width / 2, center.y - face + radius * LABEL_INSET),
                )
            }
        }
    }
    drawCircle(theme.feltDeep, radius * HUB)
    drawCircle(theme.accent, radius * HUB, style = Stroke(radius * HUB_RING))
    // The pointer at the top that the winning segment stops under.
    val pointer =
        Path().apply {
            moveTo(center.x - radius * POINTER, center.y - radius)
            lineTo(center.x + radius * POINTER, center.y - radius)
            lineTo(center.x, center.y - radius + radius * POINTER * 2)
            close()
        }
    drawPath(pointer, theme.accent)
}

private fun fillOf(
    symbol: WheelSymbol,
    theme: DenTheme,
): Color =
    when (symbol) {
        WheelSymbol.ONE -> Cream
        WheelSymbol.TWO -> Blue
        WheelSymbol.FIVE -> Green
        WheelSymbol.TEN -> Purple
        WheelSymbol.TWENTY -> Orange
        WheelSymbol.JOKER -> Color.Black
        WheelSymbol.LOGO -> theme.accent
    }

private fun inkOf(symbol: WheelSymbol): Color =
    if (symbol == WheelSymbol.ONE ||
        symbol == WheelSymbol.LOGO
    ) {
        Color.Black
    } else {
        Color.White
    }

// What a segment shows: its number, a star for the joker, and the theme's emblem for the logo.
private fun symbolLabel(
    symbol: WheelSymbol,
    theme: DenTheme,
): String =
    when (symbol) {
        WheelSymbol.JOKER -> "★"
        WheelSymbol.LOGO -> theme.emblem
        else -> symbol.pays.toString()
    }

/** Tags the box for bets on [symbol]. */
fun wheelTag(symbol: WheelSymbol) = "wheel-${symbol.name.lowercase()}"

/** Tags the money wheel. */
const val WHEEL_TAG = "moneyWheel"

/** Tags where the last spin stopped and what it paid. */
const val WHEEL_RESULT_TAG = "wheelResult"

private val Cream = Color(0xFFF3E5C3)
private val Blue = Color(0xFF1E88E5)
private val Green = Color(0xFF43A047)
private val Purple = Color(0xFF8E24AA)
private val Orange = Color(0xFFF4511E)
private val Label = TextStyle(fontWeight = FontWeight.Bold)

private const val FULL_TURN = 360f
private const val QUARTER = 90f
private const val STEP = FULL_TURN / MoneyWheel.SEGMENTS
private const val TURNS = 3
private const val SPIN_MILLIS = 4_000
private const val BOXES_IN_ROW = 4

// Sizes as shares of the wheel's radius.
private const val FACE = 0.92f
private const val LABEL = 0.055f
private const val LABEL_INSET = 0.03f
private const val HUB = 0.22f
private const val HUB_RING = 0.03f
private const val POINTER = 0.05f
