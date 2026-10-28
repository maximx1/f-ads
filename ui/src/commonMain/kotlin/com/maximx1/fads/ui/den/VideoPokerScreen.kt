package com.maximx1.fads.ui.den

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.JacksOrBetter
import com.maximx1.fads.core.den.PayLine
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.VideoPokerHand
import com.maximx1.fads.core.den.videoPoker
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.Strings
import com.maximx1.fads.ui.rememberPlaceholderCardStyle

/**
 * The video poker machine in [room], Jacks or Better, dressed in the den's theme: its pay table, five cards to hold
 * or throw away, and a bet built up from chips.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun VideoPokerScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val machine = den.videoPoker(room)
    val hand = machine.hand
    val theme = denTheme(den.theme)
    val faces = rememberPlaceholderCardStyle()
    val style = remember(faces, theme) { theme.cards(faces) }
    var bet by rememberSaveable { mutableLongStateOf(0L) }
    // Nothing is held at the start of a hand, nor once it is drawn.
    var held by remember(machine.hands, hand?.drawn) { mutableStateOf(emptySet<Int>()) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.VIDEO_POKER, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        PayTable(theme, hand?.takeIf { it.drawn }?.let { JacksOrBetter.line(it.cards) }, Modifier.padding(top = 12.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Hand(hand, held, style, onHold = { if (machine.dealt) held = if (it in held) held - it else held + it })
        }
        val line = hand?.takeIf { it.drawn }?.let { JacksOrBetter.line(it.cards) }
        Text(
            when {
                hand == null || !hand.drawn -> ""
                line != null -> Strings.paidFor(line.label, hand.won)
                else -> Strings.slotResult(0)
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(VIDEO_POKER_RESULT_TAG),
        )
        Text(Strings.stakeOf(bet), modifier = Modifier.padding(top = 4.dp))
        Controls(den, room, bet, onBet = {
            bet = it
        }, onDeal = { actions.dealVideoPoker(bet, room) }, onDraw = { actions.drawVideoPoker(held, room) })
    }
}

/** The chips that make the bet, and the button to deal for it, or to draw once dealt. */
@Composable
@Suppress("LongParameterList") // The machine, the bet, and what each button does.
private fun Controls(
    den: Den,
    room: Room,
    bet: Long,
    onBet: (Long) -> Unit,
    onDeal: () -> Unit,
    onDraw: () -> Unit,
) {
    val dealt = den.videoPoker(room).dealt
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
                onClick = { if (!dealt && den.purse.holds(bet + chip.worth)) onBet(bet + chip.worth) },
                modifier = Modifier.size(ChipSize).testTag(chipTag(chip.coin)),
            )
        }
        Box(Modifier.weight(1f))
        if (dealt) {
            Button(onClick = onDraw) { Text(Strings.DRAW) }
        } else {
            OutlinedButton(onClick = { onBet(0) }, enabled = bet > 0) { Text(Strings.CLEAR) }
            Button(
                onClick = onDeal,
                enabled =
                    bet >= room.leastBet && den.purse.holds(bet),
            ) {
                Text(Strings.DEAL)
            }
        }
    }
}

/** What each hand pays, best first, on a panel in the theme's rim, the row of the hand just drawn [lit]. */
@Composable
private fun PayTable(
    theme: DenTheme,
    lit: PayLine?,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(theme.rimDeep, RoundedCornerShape(12.dp))
            .border(2.dp, theme.accent, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        for (line in PayLine.entries) {
            val on = line == lit
            Row(
                Modifier.fillMaxWidth().then(if (on) Modifier.background(theme.accent.copy(alpha = LIT)) else Modifier),
            ) {
                Text(line.label, fontWeight = if (on) FontWeight.Bold else null, modifier = Modifier.weight(1f))
                Text("${line.pays}×", fontWeight = if (on) FontWeight.Bold else null, color = theme.accent)
            }
        }
    }
}

/** The five cards, face down before the first deal; [onHold] holds or lets go of the card at its place. */
@Composable
private fun Hand(
    hand: VideoPokerHand?,
    held: Set<Int>,
    style: CardStyle,
    onHold: (Int) -> Unit,
) {
    BoxWithConstraints {
        val width = minOf(maxWidth / CARDS_ACROSS, CARD_MOST)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (index in 0 until CARDS) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (index in held) Strings.HELD else "", fontWeight = FontWeight.Bold)
                    PlayingCard(
                        hand?.cards?.get(index),
                        style,
                        width,
                        Modifier.clickable { onHold(index) }.testTag(holdTag(index)),
                    )
                }
            }
        }
    }
}

/** Tags the card at [index] of the video poker hand. */
fun holdTag(index: Int) = "hold-$index"

/** Tags the line that says what the last hand paid. */
const val VIDEO_POKER_RESULT_TAG = "videoPokerResult"

private val ChipSize = 40.dp
private val CARD_MOST = 80.dp

private const val CARDS = 5
private const val CARDS_ACROSS = 5.6f
private const val LIT = 0.35f
