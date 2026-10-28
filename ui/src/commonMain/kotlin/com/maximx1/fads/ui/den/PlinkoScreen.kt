package com.maximx1.fads.ui.den

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Drop
import com.maximx1.fads.core.den.PlinkoBoard
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.plinko
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings
import kotlin.math.floor

/**
 * The plinko board in [room]: pegs in a triangle over a row of buckets, and a ball to drop for the stake built up
 * from chips. What it wins shows once it lands.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PlinkoScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val board = den.plinko(room)
    var stake by rememberSaveable { mutableLongStateOf(0L) }
    var landed by rememberSaveable { mutableLongStateOf(board.drops) }
    val dropping = landed != board.drops
    val last = board.last
    // While the ball falls, the purse is as it was before its winnings were paid.
    val purse = if (dropping && last != null) Purse(den.purse.total - last.won) else den.purse
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.PLINKO, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(purse, coinSize = 20.dp)
        Text(
            if (dropping || last == null) "" else Strings.landedIn(multiplier(last.bucket), last.won),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp).testTag(PLINKO_RESULT_TAG),
        )
        Board(last, board.drops, onLanded = {
            landed = it
        }, modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp))
        Text(Strings.stakeOf(stake), modifier = Modifier.testTag(STAKE_TAG))
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
                    onClick = { if (purse.holds(stake + chip.worth)) stake += chip.worth },
                    modifier = Modifier.size(ChipSize).testTag(chipTag(chip.coin)),
                )
            }
            Box(Modifier.weight(1f))
            OutlinedButton(onClick = { stake = 0 }, enabled = stake > 0) { Text(Strings.CLEAR) }
            Button(
                onClick = { actions.dropBall(stake, room) },
                enabled = !dropping && stake >= room.leastBet && den.purse.holds(stake),
            ) { Text(Strings.DROP) }
        }
    }
}

/** The pegs and buckets, and the last ball falling down its path, or lying where it landed. */
@Composable
private fun Board(
    drop: Drop?,
    drops: Long,
    onLanded: (Long) -> Unit,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val landed by rememberUpdatedState(onLanded)
    // Rows fallen through: a ball that landed before this was on the screen lies in its bucket already.
    val fallen = remember { Animatable(PlinkoBoard.ROWS + 1f) }
    var shown by rememberSaveable { mutableLongStateOf(drops) }
    LaunchedEffect(drops) {
        if (drops == shown) return@LaunchedEffect
        shown = drops
        fallen.snapTo(0f)
        fallen.animateTo(PlinkoBoard.ROWS + 1f, tween(ROW_MILLIS * (PlinkoBoard.ROWS + 1), easing = LinearEasing))
        landed(drops)
    }
    Canvas(modifier.testTag(BOARD_TAG)) {
        val spacing = size.width / (PlinkoBoard.ROWS + 2)
        val rowHeight = (size.height - spacing * BUCKET_HEIGHT) / (PlinkoBoard.ROWS + 1)
        drawPegs(spacing, rowHeight)
        drawBuckets(
            spacing,
            rowHeight,
            if (fallen.value >= PlinkoBoard.ROWS + 1 &&
                drop != null
            ) {
                drop.bucket
            } else {
                null
            },
            measurer,
        )
        if (drop != null) drawBall(drop.path, fallen.value, spacing, rowHeight)
    }
}

private fun DrawScope.drawPegs(
    spacing: Float,
    rowHeight: Float,
) {
    for (row in 0 until PlinkoBoard.ROWS) {
        for (peg in 0..row + 1) {
            drawCircle(Peg, spacing * PEG, Offset(across(peg - (row + 1) / 2f, spacing), rowHeight * (row + 1)))
        }
    }
}

// The buckets along the bottom, coloured from hot at the edges to cool in the middle, [lit] one ringed.
private fun DrawScope.drawBuckets(
    spacing: Float,
    rowHeight: Float,
    lit: Int?,
    measurer: TextMeasurer,
) {
    val top = rowHeight * (PlinkoBoard.ROWS + 1)
    val middle = PlinkoBoard.ROWS / 2
    for (bucket in 0..PlinkoBoard.ROWS) {
        val heat = kotlin.math.abs(bucket - middle) / middle.toFloat()
        val left = Offset(across(bucket - middle - HALF, spacing) + GAP, top)
        val box = Size(spacing - GAP * 2, spacing * BUCKET_HEIGHT)
        drawRoundRect(lerp(Cool, Hot, heat), left, box, CornerRadius(spacing * CORNER))
        if (bucket ==
            lit
        ) {
            drawRoundRect(Color.White, left, box, CornerRadius(spacing * CORNER), style = Stroke(spacing * RING))
        }
        val text = measurer.measure(multiplier(bucket), Label.copy(fontSize = (spacing * LABEL).toSp()))
        drawText(text, topLeft = left + Offset((box.width - text.size.width) / 2, (box.height - text.size.height) / 2))
    }
}

// The ball [fallen] rows down [path], bouncing a little between pegs.
private fun DrawScope.drawBall(
    path: List<Boolean>,
    fallen: Float,
    spacing: Float,
    rowHeight: Float,
) {
    val row = floor(fallen).toInt().coerceAtMost(PlinkoBoard.ROWS)
    val step = fallen - row
    val from = place(path, row)
    val to = place(path, minOf(row + 1, PlinkoBoard.ROWS + 1))
    val x = across(from + (to - from) * step, spacing)
    // Up off each peg and down to the next, highest halfway between.
    val bounce = BOUNCE * rowHeight * PARABOLA * step * (1 - step)
    val y = rowHeight * (row + step) - bounce
    drawCircle(Ball, spacing * BALL, Offset(x, y.coerceAtLeast(0f)))
}

// Where the ball is across the board after [rows] rows of [path], in spacings from the middle.
private fun place(
    path: List<Boolean>,
    rows: Int,
): Float {
    val taken = path.take(minOf(rows, path.size))
    return taken.count { it } - taken.size / 2f
}

// The x of a point [fromMiddle] spacings either side of the board's middle.
private fun DrawScope.across(
    fromMiddle: Float,
    spacing: Float,
) = size.width / 2 + fromMiddle * spacing

private fun multiplier(bucket: Int): String {
    val tenths = PlinkoBoard.tenths[bucket]
    val whole = tenths / PlinkoBoard.TENTHS
    val part = tenths % PlinkoBoard.TENTHS
    return if (part == 0) "$whole×" else "$whole.$part×"
}

/** Tags the plinko board. */
const val BOARD_TAG = "plinkoBoard"

/** Tags the line that says where the last ball landed and what it won. */
const val PLINKO_RESULT_TAG = "plinkoResult"

/** Tags the stake riding on the next ball. */
const val STAKE_TAG = "plinkoStake"

private val Peg = Color(0xCCFFFFFF)
private val Ball = Color(0xFFFFD54F)
private val Hot = Color(0xFFE53935)
private val Cool = Color(0xFF3949AB)
private val Label = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
private val ChipSize = 40.dp

private const val ROW_MILLIS = 150
private const val BUCKET_HEIGHT = 1.4f
private const val PEG = 0.09f
private const val BALL = 0.2f
private const val GAP = 2f
private const val CORNER = 0.15f
private const val RING = 0.08f
private const val LABEL = 0.28f
private const val HALF = 0.5f
private const val BOUNCE = 0.35f
private const val PARABOLA = 4
