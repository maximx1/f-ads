package com.maximx1.fads.ui.den

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.SlotMachine
import com.maximx1.fads.core.den.SlotSpin
import com.maximx1.fads.core.den.slots
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * The slot machine in [room]: three reels behind a window, a bet on each of its five lines built up from chips, and
 * the lever. The lines that won light up, and the winnings show, once the reels have stopped.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SlotsScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val machine = den.slots(room)
    var lineBet by rememberSaveable { mutableLongStateOf(0L) }
    var stopped by rememberSaveable { mutableLongStateOf(machine.pulls) }
    val spinning = stopped != machine.pulls
    val last = machine.last
    val lines = SlotMachine.lines.size
    // While the reels turn, the purse is as it was before their winnings were paid.
    val purse = if (spinning && last != null) Purse(den.purse.total - last.won) else den.purse
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.SLOTS, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(purse, coinSize = 20.dp)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Reels(
                last,
                machine.pulls,
                onStopped = { stopped = it },
                modifier = Modifier.fillMaxWidth().aspectRatio(MACHINE_ASPECT),
            )
        }
        Text(
            if (spinning || last == null) "" else Strings.slotResult(last.won),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(SLOTS_RESULT_TAG),
        )
        Text(Strings.lineBet(lineBet, lineBet * lines), modifier = Modifier.padding(top = 4.dp).testTag(LINE_BET_TAG))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (chip in chipsFor(room)) {
                ChipButton(
                    chip,
                    chosen = false,
                    description = Strings.betIn(chip.coin.label),
                    onClick = { if (purse.holds((lineBet + chip.worth) * lines)) lineBet += chip.worth },
                    modifier = Modifier.size(ChipSize).testTag(chipTag(chip.coin)),
                )
            }
            Box(Modifier.weight(1f))
            OutlinedButton(onClick = { lineBet = 0 }, enabled = lineBet > 0) { Text(Strings.CLEAR) }
            Button(
                onClick = { actions.pullLever(lineBet, room) },
                enabled = !spinning && lineBet >= room.leastBet && den.purse.holds(lineBet * lines),
            ) { Text(Strings.SPIN) }
        }
    }
}

/** The machine: its reels turning to the last pull's stops and, once they are still, the lines that won. */
@Composable
private fun Reels(
    spin: SlotSpin?,
    pulls: Long,
    onStopped: (Long) -> Unit,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val stopped by rememberUpdatedState(onStopped)
    val stripSize = SlotMachine.strip.size
    val reels = remember { List(SlotMachine.REELS) { Animatable(spin?.stops?.get(it)?.toFloat() ?: 0f) } }
    var shown by rememberSaveable { mutableLongStateOf(pulls) }
    LaunchedEffect(pulls) {
        if (pulls == shown || spin == null) return@LaunchedEffect
        shown = pulls
        val turning =
            reels.mapIndexed { reel, position ->
                // A few whole turns, more for each reel along, then on to its stop.
                val to =
                    floor(position.value) + (TURNS + reel) * stripSize +
                        (spin.stops[reel] - floor(position.value)).mod(stripSize.toFloat())
                launch {
                    position.animateTo(
                        to,
                        tween(FIRST_STOP_MILLIS + reel * NEXT_STOP_MILLIS, easing = FastOutSlowInEasing),
                    )
                }
            }
        turning.forEach { it.join() }
        stopped(pulls)
    }
    val still = reels.none { it.isRunning }
    Canvas(modifier.testTag(REELS_TAG)) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Cabinet, CabinetDeep)),
            cornerRadius =
                CornerRadius(size.width * CORNER),
        )
        val cell = size.width * (1 - MARGIN * 2) / SlotMachine.REELS
        val left = size.width * MARGIN
        val top = (size.height - cell * SlotMachine.ROWS) / 2
        reels.forEachIndexed {
            reel,
            position,
            ->
            drawReel(Offset(left + reel * cell, top), cell, position.value, measurer)
        }
        if (still && spin != null) drawWinningLines(spin, Offset(left, top), cell)
    }
}

// One reel's window at [at], [cell] wide, the strip [position] symbols round.
private fun DrawScope.drawReel(
    at: Offset,
    cell: Float,
    position: Float,
    measurer: TextMeasurer,
) {
    val strip = SlotMachine.strip
    drawRect(Window, at + Offset(GAP, 0f), Size(cell - GAP * 2, cell * SlotMachine.ROWS))
    clipRect(at.x, at.y, at.x + cell, at.y + cell * SlotMachine.ROWS) {
        val first = floor(position).toInt()
        val shift = position - first
        for (row in -1..SlotMachine.ROWS) {
            val symbol = strip[(first + row).mod(strip.size)]
            drawSymbol(symbol, at + Offset(0f, (row - shift) * cell), cell, measurer)
        }
    }
}

private fun DrawScope.drawWinningLines(
    spin: SlotSpin,
    at: Offset,
    cell: Float,
) {
    for (line in SlotMachine.winningLines(spin.stops)) {
        val rows = SlotMachine.lines[line]
        val points = rows.mapIndexed { reel, row -> at + Offset((reel + HALF) * cell, (row + HALF) * cell) }
        points.zipWithNext().forEach { (from, to) -> drawLine(Win, from, to, cell * LINE) }
    }
}

/** Tags the reels. */
const val REELS_TAG = "slotReels"

/** Tags the line that says what the last pull won. */
const val SLOTS_RESULT_TAG = "slotsResult"

/** Tags the bet on each line. */
const val LINE_BET_TAG = "lineBet"

private val Cabinet = Color(0xFF8E1C2E)
private val CabinetDeep = Color(0xFF3E0A14)
private val Window = Color(0xFFFAF7EE)
private val Win = Color(0xCCFFC107)
private val ChipSize = 40.dp

private const val MACHINE_ASPECT = 1.15f
private const val MARGIN = 0.08f
private const val CORNER = 0.06f
private const val GAP = 3f
private const val HALF = 0.5f
private const val LINE = 0.06f
private const val TURNS = 3
private const val FIRST_STOP_MILLIS = 1_100
private const val NEXT_STOP_MILLIS = 450
