package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PocketColor
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.Roulette
import com.maximx1.fads.core.den.Wheel
import com.maximx1.fads.core.den.canRebet
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings

/**
 * The roulette table: the wheel, the layout to bet on, and the coin each tap puts down. The winnings of a spin,
 * and where it landed, show once the ball has settled.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun RouletteScreen(
    den: Den,
    actions: DenActions,
    onClose: () -> Unit,
) {
    var coin by rememberSaveable { mutableStateOf(Coin.BRONZE) }
    val roulette = den.roulette
    var settled by rememberSaveable { mutableLongStateOf(roulette.spins) }
    val spinning = settled != roulette.spins
    val last = roulette.last
    // While the ball is rolling, the purse is as it was before the winnings were paid.
    val purse = if (spinning && last != null) Purse(den.purse.total - last.won) else den.purse

    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().drawBehind { drawRect(TableFelt) }) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Table(den, actions, coin, spinning, purse, onCoin = { coin = it }, onSettled = { settled = it }, onClose)
        }
    }
}

/** Everything on the table: the wheel and how the last spin went, the layout, and the coins and buttons. */
@Composable
@Suppress("LongParameterList") // The table and the few things about it that only the screen keeps.
private fun Table(
    den: Den,
    actions: DenActions,
    coin: Coin,
    spinning: Boolean,
    purse: Purse,
    onCoin: (Coin) -> Unit,
    onSettled: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val roulette = den.roulette
    val last = roulette.last
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.ROULETTE, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouletteWheel(
                pocket = last?.pocket,
                spins = roulette.spins,
                onSettled = { onSettled(roulette.spins) },
                modifier = Modifier.size(WheelSize),
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    if (spinning ||
                        last == null
                    ) {
                        ""
                    } else {
                        Strings.landed(last.pocket, colourName(last.pocket), last.won)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(RESULT_TAG),
                )
                PurseCoins(purse, coinSize = 20.dp, modifier = Modifier.padding(top = 8.dp))
                Text(Strings.staked(roulette.staked), modifier = Modifier.padding(top = 8.dp))
            }
        }
        BettingLayout(
            roulette,
            enabled = !spinning,
            onTap = { actions.placeBet(it, coin.worth) },
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
        )
        Controls(den, coin, spinning, onCoin = onCoin, actions = actions)
    }
}

/** The coins to bet with, and the buttons to clear the layout, bet the same again, and spin. */
@Composable
private fun Controls(
    den: Den,
    coin: Coin,
    spinning: Boolean,
    onCoin: (Coin) -> Unit,
    actions: DenActions,
) {
    val bets = den.roulette.bets.isNotEmpty()
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (each in Coin.entries) {
            Canvas(
                Modifier
                    .size(ChipSize)
                    .then(if (each == coin) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .clickable { onCoin(each) }
                    .semantics { contentDescription = Strings.betIn(each.label) }
                    .testTag(chipTag(each)),
            ) { drawCoin(each) }
        }
        Box(Modifier.weight(1f))
        // Clearing does something only with bets down, and betting the same again only with none.
        if (bets) {
            OutlinedButton(onClick = actions::clearBets, enabled = !spinning) { Text(Strings.CLEAR) }
        } else {
            OutlinedButton(onClick = actions::rebet, enabled = den.canRebet && !spinning) { Text(Strings.REBET) }
        }
        Button(onClick = actions::spin, enabled = bets && !spinning) { Text(Strings.SPIN) }
    }
}

/** The layout of numbers and outside bets, with the chips down on it. A tap puts a coin down where it lands. */
@Composable
private fun BettingLayout(
    roulette: Roulette,
    enabled: Boolean,
    onTap: (Bet) -> Unit,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val tap by rememberUpdatedState(onTap)
    Canvas(
        modifier
            .testTag(LAYOUT_TAG)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { at ->
                        val across = at.x / size.width * RouletteLayout.WIDTH
                        val down = at.y / size.height * RouletteLayout.HEIGHT
                        RouletteLayout.betAt(across.toDouble(), down.toDouble())?.let(tap)
                    }
                }
            },
    ) {
        val cell = Size(size.width / RouletteLayout.WIDTH, size.height / RouletteLayout.HEIGHT)
        drawNumbers(cell, measurer)
        drawOutsideBets(cell, measurer)
        for ((bet, stake) in roulette.bets) drawChip(bet, stake, cell, measurer)
    }
}

private fun colourName(pocket: Int): String =
    when (Wheel.colorOf(pocket)) {
        PocketColor.GREEN -> Strings.GREEN
        PocketColor.RED -> Strings.RED
        PocketColor.BLACK -> Strings.BLACK
    }

/** Tags the roulette layout. */
const val LAYOUT_TAG = "rouletteLayout"

/** Tags the line that says where the last spin landed and what it won. */
const val RESULT_TAG = "rouletteResult"

/** Tags the coin to bet with of each kind. */
fun chipTag(coin: Coin) = "chip-${coin.name.lowercase()}"

private val TableFelt = Color(0xFF14532D)
private val WheelSize = 180.dp
private val ChipSize = 40.dp
