package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.SymbolDice
import com.maximx1.fads.core.den.SymbolDiceGame
import com.maximx1.fads.core.den.symbolDice
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings
import com.maximx1.fads.ui.drawSuit

/**
 * Chuck-a-luck or crown and anchor, by [game], in [room], dressed in the den's theme: three dice, and a box for each
 * of the six faces, showing the face as the dice do. A chip is chosen, and a tap on a face puts it down there.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SymbolDiceScreen(
    game: SymbolDiceGame,
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val table = den.symbolDice(game, room)
    val theme = denTheme(den.theme)
    val measurer = rememberTextMeasurer()
    val chips = chipsFor(room)
    var chosen by rememberSaveable { mutableIntStateOf(chips.lastIndex) }
    var landed by rememberSaveable { mutableLongStateOf(table.throws) }
    val rolling = landed != table.throws
    val face: DrawScope.(Int?) -> Unit = { drawSymbolFace(game, it, theme) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.symbolDiceName(game), room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawEmblem(theme, measurer) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TumblingDice(
                    table.dice,
                    SymbolDice.DICE,
                    table.throws,
                    theme,
                    onLanded = { landed = it },
                    drawFace = face,
                )
                Text(
                    if (rolling || table.dice == null) "" else Strings.symbolResult(table.paid),
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp).testTag(SYMBOL_RESULT_TAG),
                )
            }
        }
        Faces(table, game, theme, face, onBet = { actions.betSymbolDice(game, it, chips[chosen].worth, room) })
        BetControls(
            chips,
            chosen,
            ready = table.bets.isNotEmpty() && !rolling,
            act = Strings.ROLL,
            onChoose = { chosen = it },
            onClear = { actions.clearSymbolDice(game, room) },
            onAct = { actions.rollSymbolDice(game, room) },
        )
    }
}

/** The six faces to bet on, two rows of three, each drawn as the dice show it. */
@Composable
@Suppress("LongParameterList") // The table, how its faces look, and what a tap does.
private fun Faces(
    table: SymbolDice,
    game: SymbolDiceGame,
    theme: DenTheme,
    face: DrawScope.(Int?) -> Unit,
    onBet: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in (1..SymbolDice.FACES).chunked(SymbolDice.FACES / 2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (each in row) {
                    LayoutBox(
                        Strings.faceName(game, each),
                        Strings.SYMBOL_ODDS,
                        table.bets[each] ?: 0,
                        open = true,
                        theme = theme,
                        onTap = { onBet(each) },
                        modifier = Modifier.weight(1f).testTag(faceTag(each)),
                        picture = { face(each) },
                    )
                }
            }
        }
    }
}

/**
 * A die of [game] showing [face], or blank: pips for chuck-a-luck, and a crown, an anchor or a suit for crown and
 * anchor.
 */
internal fun DrawScope.drawSymbolFace(
    game: SymbolDiceGame,
    face: Int?,
    theme: DenTheme,
) {
    if (game == SymbolDiceGame.CHUCK_A_LUCK || face == null) {
        drawDie(face, theme.back)
        return
    }
    drawRoundRect(Color.White, cornerRadius = CornerRadius(size.width * CORNER))
    val mark = size.width * MARK
    val topLeft = Offset((size.width - mark) / 2, (size.height - mark) / 2)
    when (face) {
        CROWN -> {
            drawCrown(topLeft, mark)
        }

        ANCHOR -> {
            drawAnchor(topLeft, mark)
        }

        else -> {
            val suit = Suits[face - HEARTS]
            drawSuit(suit, if (suit == Suit.HEARTS || suit == Suit.DIAMONDS) Red else Color.Black, topLeft, mark)
        }
    }
}

private fun DrawScope.drawCrown(
    at: Offset,
    side: Float,
) {
    val crown =
        Path().apply {
            CrownPoints.forEachIndexed { index, (x, y) ->
                if (index == 0) moveTo(at.x + x * side, at.y + y * side) else lineTo(at.x + x * side, at.y + y * side)
            }
            close()
        }
    drawPath(crown, Gold)
}

private fun DrawScope.drawAnchor(
    at: Offset,
    side: Float,
) {
    val line = Stroke(side * ANCHOR_LINE)
    val middle = at.x + side / 2
    drawCircle(Navy, side * RING, Offset(middle, at.y + side * RING), style = line)
    drawLine(Navy, Offset(middle, at.y + side * RING * 2), Offset(middle, at.y + side), line.width)
    drawLine(
        Navy,
        Offset(at.x + side * BAR_IN, at.y + side * BAR_DOWN),
        Offset(
            at.x + side * (1 - BAR_IN),
            at.y + side * BAR_DOWN,
        ),
        line.width,
    )
    drawArc(
        Navy,
        0f,
        HALF_TURN,
        false,
        Offset(at.x, at.y + side * ARC_TOP),
        Size(
            side,
            side * ARC_HEIGHT,
        ),
        style = line,
    )
}

/** Tags the box for bets on [face]. */
fun faceTag(face: Int) = "face-$face"

/** Tags what the last throw paid. */
const val SYMBOL_RESULT_TAG = "symbolResult"

private val Red = Color(0xFFC62828)
private val Gold = Color(0xFFE0A800)
private val Navy = Color(0xFF1A237E)
private val Suits = listOf(Suit.HEARTS, Suit.SPADES, Suit.DIAMONDS, Suit.CLUBS)

// The crown's outline, as shares of its square: a band along the bottom and three points above it.
private val CrownPoints =
    listOf(0.1f to 0.85f, 0.1f to 0.3f, 0.3f to 0.55f, 0.5f to 0.15f, 0.7f to 0.55f, 0.9f to 0.3f, 0.9f to 0.85f)

private const val CROWN = 1
private const val ANCHOR = 2
private const val HEARTS = 3
private const val CORNER = 0.18f
private const val MARK = 0.62f
private const val ANCHOR_LINE = 0.08f
private const val RING = 0.1f
private const val BAR_IN = 0.2f
private const val BAR_DOWN = 0.35f
private const val ARC_TOP = 0.4f
private const val ARC_HEIGHT = 0.6f
private const val HALF_TURN = 180f
