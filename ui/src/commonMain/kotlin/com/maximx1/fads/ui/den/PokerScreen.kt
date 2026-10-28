package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.PokerHand
import com.maximx1.fads.core.den.PokerMove
import com.maximx1.fads.core.den.PokerTable
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.Street
import com.maximx1.fads.core.den.canMove
import com.maximx1.fads.core.den.playersTurn
import com.maximx1.fads.core.den.table
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings
import com.maximx1.fads.ui.rememberPlaceholderCardStyle

/**
 * A poker table, five-card draw or Hold'em by [game]: the player sits down with the den's players as it opens, and
 * gets up, folding any hand they are in, as it closes. Between hands the purse and the Deal button show; in a hand,
 * whatever the player can do on their turn.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PokerScreen(
    game: PokerGame,
    room: Room,
    den: Den,
    actions: DenActions,
    onClose: () -> Unit,
) {
    LaunchedEffect(game, room) { actions.sitDown(game, room) }
    val leave = {
        actions.leaveTable(game, room)
        onClose()
    }
    BackHandler(onBack = leave)
    val table = den.table(game, room)
    val hand = table?.hand
    var discards by remember(hand?.number, hand?.street) { mutableStateOf(emptySet<Int>()) }
    val theme = denTheme(den.theme)
    val faces = rememberPlaceholderCardStyle()
    val style = remember(faces, theme) { theme.cards(faces) }
    val drawing = hand?.street == Street.DRAW && den.playersTurn(game, room)

    Box(Modifier.fillMaxSize().drawBehind { drawRect(theme.room) }) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Strings.tableName(Strings.pokerName(game), room),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = leave) { Text(Strings.BACK) }
                }
                if (table != null) {
                    PokerFelt(
                        theme,
                        table,
                        discards,
                        style,
                        onCard = if (drawing) { index -> discards = toggled(discards, index) } else null,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    ActionBar(game, room, den, hand, discards, actions)
                }
            }
        }
    }
}

// [discards] with the card at [index] marked to throw away, or no longer marked.
private fun toggled(
    discards: Set<Int>,
    index: Int,
) = if (index in discards) discards - index else discards + index

/** What the player can do now: deal between hands, draw during the draw, or bet when it is their turn. */
@Composable
@Suppress("LongParameterList") // The table, the hand on it, and what the player can do.
private fun ActionBar(
    game: PokerGame,
    room: Room,
    den: Den,
    hand: PokerHand?,
    discards: Set<Int>,
    actions: DenActions,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            hand == null || hand.isOver -> {
                PurseCoins(den.purse, coinSize = 20.dp)
                val enough = den.purse.total > 0 && den.purse.holds(room.leastBet)
                if (!enough) Text(Strings.EMPTY_PURSE, modifier = Modifier.padding(top = 4.dp))
                Button(
                    onClick = { actions.deal(game, room) },
                    enabled = enough,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(Strings.DEAL)
                }
            }

            !den.playersTurn(game, room) -> {
                Text(Strings.waitingFor(hand.toAct?.let { hand.seats[it].name }.orEmpty()))
            }

            hand.street == Street.DRAW -> {
                val move = PokerMove.Draw(discards)
                Button(onClick = { actions.playPoker(game, move, room) }, enabled = hand.canMove(move)) {
                    Text(if (discards.isEmpty()) Strings.STAND_PAT else Strings.drawCards(discards.size))
                }
            }

            else -> {
                Betting(game, room, hand, actions)
            }
        }
    }
}

/** Fold, check or call, and a bet or raise built up from coins. */
@Composable
private fun Betting(
    game: PokerGame,
    room: Room,
    hand: PokerHand,
    actions: DenActions,
) {
    val seat = PokerTable.PLAYER
    val most = hand.bets[seat] + hand.seats[seat].stack
    // What the player has added, with the coins, to the least they could raise to.
    var extra by remember(hand.moves) { mutableLongStateOf(0L) }
    val to = minOf(hand.leastRaise + extra, most)
    val owed = hand.owed(seat)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { actions.playPoker(game, PokerMove.Fold, room) }) { Text(Strings.FOLD) }
        if (owed == 0L) {
            OutlinedButton(onClick = { actions.playPoker(game, PokerMove.Check, room) }) { Text(Strings.CHECK) }
        } else {
            OutlinedButton(onClick = {
                actions.playPoker(game, PokerMove.Call, room)
            }) { Text(Strings.call(minOf(owed, hand.seats[seat].stack))) }
        }
    }
    val raise = PokerMove.Raise(to)
    if (!hand.canMove(raise)) return
    Row(
        Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (chip in chipsFor(room)) {
            ChipButton(
                chip,
                chosen = false,
                description = Strings.raiseWith(chip.coin.label),
                onClick = { extra += chip.worth },
                modifier = Modifier.size(RaiseChip).testTag(raiseCoinTag(chip.coin)),
            )
        }
        Button(onClick = { actions.playPoker(game, raise, room) }) {
            Text(if (to == most) Strings.ALL_IN else Strings.raiseTo(to, opening = hand.topBet == 0L))
        }
        if (to < most) TextButton(onClick = { extra = most }) { Text(Strings.ALL_IN) }
    }
}

private val RaiseChip = 32.dp

/** Tags the coin of each kind that adds to a raise. */
fun raiseCoinTag(coin: Coin) = "raise-${coin.name.lowercase()}"
