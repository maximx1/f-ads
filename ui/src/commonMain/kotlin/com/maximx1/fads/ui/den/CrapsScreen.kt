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
                Dice(table, onLanded = { landed = it })
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

/** The two dice: tumbling when a throw is made, then showing it; [onLanded] says which throw they came to rest on. */
@Composable
private fun Dice(
    table: Craps,
    onLanded: (Long) -> Unit,
) {
    val landed by rememberUpdatedState(onLanded)
    val tumble = remember { Animatable(1f) }
    var shown by rememberSaveable { mutableLongStateOf(table.throws) }
    LaunchedEffect(table.throws) {
        if (table.throws == shown) return@LaunchedEffect
        shown = table.throws
        tumble.snapTo(0f)
        tumble.animateTo(1f, tween(TUMBLE_MILLIS, easing = FastOutSlowInEasing))
        landed(table.throws)
    }
    val last = table.last
    val settled = tumble.value >= 1f
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(last?.first, last?.second).forEachIndexed { index, face ->
            // While tumbling, the faces flick round as the die turns over.
            val showing =
                if (settled ||
                    face == null
                ) {
                    face
                } else {
                    ((tumble.value * FLICKS).toInt() + index * 2) % FACES + 1
                }
            Canvas(
                Modifier.size(DieSize).rotate((1 - tumble.value) * TURNS * (if (index == 0) 1 else -1)),
            ) { drawDie(showing) }
        }
    }
}

// A die showing [face], or blank before the first throw.
private fun DrawScope.drawDie(face: Int?) {
    drawRoundRect(Color.White, cornerRadius = CornerRadius(size.width * DIE_CORNER))
    val pips = face?.let { Pips.getOrNull(it - 1) }.orEmpty()
    for (at in pips) drawCircle(Color.Black, size.width * PIP, Offset(at.x * size.width, at.y * size.width))
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
) {
    Column(
        modifier
            .alpha(if (open) 1f else CLOSED)
            .background(theme.feltDeep, RoundedCornerShape(8.dp))
            .border(if (point) 3.dp else 1.dp, theme.accent, RoundedCornerShape(8.dp))
            .then(if (open) Modifier.clickable { onBet(bet) } else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(crapsTag(bet)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Strings.crapsBet(bet), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(Strings.crapsOdds(bet), color = theme.accent, style = MaterialTheme.typography.labelSmall)
        if (point) {
            Text(
                Strings.ON,
                modifier = Modifier.background(theme.accent, CircleShape).padding(horizontal = 6.dp),
                color = Color.Black,
            )
        }
        if (stake > 0) Text(Strings.stack(stake), style = MaterialTheme.typography.labelLarge)
    }
}

/** Tags the box of the layout for [bet]. */
fun crapsTag(bet: CrapsBet) = "craps-${bet.name.lowercase()}"

/** Tags what the last throw was and paid. */
const val CRAPS_RESULT_TAG = "crapsResult"

/** Tags the point, or that there is none. */
const val POINT_TAG = "point"

private val DieSize = 56.dp

// Where the pips of each face sit, one to six, as shares of the die's side.
private const val NEAR = 0.27f
private const val MID = 0.5f
private const val FAR = 0.73f
private val Pips: List<List<Offset>> =
    listOf(
        listOf(Offset(MID, MID)),
        listOf(Offset(NEAR, NEAR), Offset(FAR, FAR)),
        listOf(Offset(NEAR, NEAR), Offset(MID, MID), Offset(FAR, FAR)),
        listOf(Offset(NEAR, NEAR), Offset(FAR, NEAR), Offset(NEAR, FAR), Offset(FAR, FAR)),
        listOf(Offset(NEAR, NEAR), Offset(FAR, NEAR), Offset(MID, MID), Offset(NEAR, FAR), Offset(FAR, FAR)),
        listOf(NEAR, MID, FAR).flatMap { y -> listOf(Offset(NEAR, y), Offset(FAR, y)) },
    )

private const val TUMBLE_MILLIS = 800
private const val FLICKS = 12
private const val FACES = 6
private const val TURNS = 540f
private const val DIE_CORNER = 0.18f
private const val PIP = 0.09f
private const val CLOSED = 0.4f
