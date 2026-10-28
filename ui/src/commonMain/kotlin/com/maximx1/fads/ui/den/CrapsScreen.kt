package com.maximx1.fads.ui.den

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Craps
import com.maximx1.fads.core.den.CrapsBet
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.canBetCraps
import com.maximx1.fads.core.den.craps
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings

/**
 * The craps table in [room], dressed in the den's theme: the dice and the point, and a layout of place numbers, the
 * field, the lines and the one-roll bets, each box on the deep felt edged in the accent. A chip is chosen, and a tap
 * on a box puts it down there.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CrapsScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val table = den.craps(room)
    val theme = denTheme(den.theme)
    val measurer = rememberTextMeasurer()
    val chips = chipsFor(room)
    var chosen by rememberSaveable { mutableIntStateOf(chips.lastIndex) }
    var landed by rememberSaveable { mutableLongStateOf(table.throws) }
    val rolling = landed != table.throws
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.CRAPS, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawEmblem(theme, measurer) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TumblingDice(table.last?.let { listOf(it.first, it.second) }, 2, table.throws, theme, onLanded = {
                    landed =
                        it
                })
                Text(
                    if (rolling) "" else table.last?.let { Strings.rolled(it.first, it.second, it.paid) }.orEmpty(),
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp).testTag(CRAPS_RESULT_TAG),
                )
                Text(
                    table.point?.let { Strings.pointIs(it) } ?: Strings.POINT_OFF,
                    modifier = Modifier.testTag(POINT_TAG),
                )
            }
        }
        Layout(den, room, table, theme, onBet = { actions.betCraps(it, chips[chosen].worth, room) })
        BetControls(
            chips,
            chosen,
            ready = table.bets.isNotEmpty() && !rolling,
            act = Strings.ROLL,
            onChoose = { chosen = it },
            onClear = { actions.clearCraps(room) },
            onAct = { actions.rollDice(room) },
        )
    }
}

/** The layout: the place numbers with the point marked, the field, the lines, and the one-roll bets. */
@Composable
@Suppress("LongParameterList") // The table, how it is dressed, and what a tap on a box does.
private fun Layout(
    den: Den,
    room: Room,
    table: Craps,
    theme: DenTheme,
    onBet: (CrapsBet) -> Unit,
) {
    val box = @Composable { scope: RowScope, bet: CrapsBet, weight: Float ->
        with(scope) {
            BetBox(
                bet,
                table.bets[bet] ?: 0,
                den.canBetCraps(bet, room),
                table.point == bet.number,
                theme,
                onBet,
                Modifier.weight(weight),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (bet in CrapsBet.entries.filter { it.isPlace }) box(this, bet, 1f)
        }
        Row { box(this, CrapsBet.FIELD, 1f) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            box(this, CrapsBet.DONT_PASS, 1f)
            box(this, CrapsBet.PASS, 2f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            box(this, CrapsBet.ANY_SEVEN, 1f)
            box(this, CrapsBet.ANY_CRAPS, 1f)
        }
    }
}

/** One box of the layout: its name and odds, what is on it, and the puck if it is the point. */
@Composable
@Suppress("LongParameterList") // The bet, how it stands, how it is dressed, and what a tap does.
private fun BetBox(
    bet: CrapsBet,
    stake: Long,
    open: Boolean,
    point: Boolean,
    theme: DenTheme,
    onBet: (CrapsBet) -> Unit,
    modifier: Modifier,
) = LayoutBox(
    Strings.crapsBet(bet),
    Strings.crapsOdds(bet),
    stake,
    open,
    theme,
    onTap = { onBet(bet) },
    modifier = modifier.testTag(crapsTag(bet)),
    lit = point,
    puck = Strings.ON.takeIf { point },
)

/** Tags the box of the layout for [bet]. */
fun crapsTag(bet: CrapsBet) = "craps-${bet.name.lowercase()}"

/** Tags what the last throw was and paid. */
const val CRAPS_RESULT_TAG = "crapsResult"

/** Tags the point, or that there is none. */
const val POINT_TAG = "point"
