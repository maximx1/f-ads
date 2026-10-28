package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.BlackjackMove
import com.maximx1.fads.core.den.BlackjackRound
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.blackjack
import com.maximx1.fads.core.den.blackjackValue
import com.maximx1.fads.core.den.canPlayBlackjack
import com.maximx1.fads.core.den.totalOf
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.Strings
import com.maximx1.fads.ui.rememberPlaceholderCardStyle

/**
 * The blackjack table in [room], dressed in the den's theme: the dealer's cards at the top, one face down until the
 * round is over, the player's hands below, and the rules printed on the felt between them in the accent.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BlackjackScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val table = den.blackjack(room)
    val round = table.round
    val theme = denTheme(den.theme)
    val faces = rememberPlaceholderCardStyle()
    val style = remember(faces, theme) { theme.cards(faces) }
    val measurer = rememberTextMeasurer()
    var bet by rememberSaveable { mutableLongStateOf(0L) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.BLACKJACK, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(Modifier.fillMaxSize()) { drawEmblem(theme, measurer) }
            val width = minOf(maxWidth / CARDS_ACROSS, CARD_MOST)
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Dealer(round, style, width)
                Text(Strings.BLACKJACK_RULES, color = theme.accent, fontWeight = FontWeight.Bold)
                Hands(round, theme, style, width)
            }
        }
        Text(
            if (round?.over == true) Strings.blackjackWon(round.won) else "",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(BLACKJACK_RESULT_TAG),
        )
        if (table.playing) {
            Moves(den, room, actions)
        } else {
            Betting(den, room, bet, onBet = { bet = it }, onDeal = { actions.dealBlackjack(bet, room) })
        }
    }
}

/** The dealer's cards, the second face down while the round is played, and what they show or come to. */
@Composable
private fun Dealer(
    round: BlackjackRound?,
    style: CardStyle,
    width: Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val hidden = round != null && !round.over
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            round?.dealer?.forEachIndexed {
                index,
                card,
                ->
                PlayingCard(card.takeUnless { hidden && index == 1 }, style, width)
            }
        }
        Text(
            when {
                round == null -> Strings.DEALER
                hidden -> Strings.dealerShows(round.dealer.first().blackjackValue)
                else -> Strings.dealerHas(totalOf(round.dealer).points)
            },
            modifier = Modifier.padding(top = 4.dp).testTag(DEALER_TAG),
        )
    }
}

/** The player's hands, the one being played edged in the theme's accent once there are two. */
@Composable
private fun Hands(
    round: BlackjackRound?,
    theme: DenTheme,
    style: CardStyle,
    width: Dp,
) {
    val hands = round?.hands.orEmpty()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        hands.forEachIndexed { index, hand ->
            val playing = round != null && !round.over && hands.size > 1 && index == round.active
            Column(
                Modifier
                    .then(if (playing) Modifier.border(2.dp, theme.accent, RoundedCornerShape(8.dp)) else Modifier)
                    .padding(6.dp)
                    .testTag(handTag(index)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) { for (card in hand.cards) PlayingCard(card, style, width) }
                val result = round?.results?.get(index)
                Text(
                    Strings.blackjackHand(totalOf(hand.cards).points, hand.bet, result),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Hit, stand, double and split, each only when the hand being played allows it. */
@Composable
private fun Moves(
    den: Den,
    room: Room,
    actions: DenActions,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (move in BlackjackMove.entries) {
            val button = @Composable { Text(Strings.blackjackMove(move)) }
            val enabled = den.canPlayBlackjack(move, room)
            if (move == BlackjackMove.HIT || move == BlackjackMove.STAND) {
                Button(onClick = { actions.playBlackjack(move, room) }, enabled = enabled) { button() }
            } else {
                OutlinedButton(onClick = { actions.playBlackjack(move, room) }, enabled = enabled) { button() }
            }
        }
    }
}

/** The chips that make the bet, and the button that deals a round for it. */
@Composable
private fun Betting(
    den: Den,
    room: Room,
    bet: Long,
    onBet: (Long) -> Unit,
    onDeal: () -> Unit,
) {
    Text(Strings.stakeOf(bet), modifier = Modifier.padding(top = 4.dp))
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
                onClick = { if (den.purse.holds(bet + chip.worth)) onBet(bet + chip.worth) },
                modifier = Modifier.size(ChipSize).testTag(chipTag(chip.coin)),
            )
        }
        Box(Modifier.weight(1f))
        OutlinedButton(onClick = { onBet(0) }, enabled = bet > 0) { Text(Strings.CLEAR) }
        Button(onClick = onDeal, enabled = bet >= room.leastBet && den.purse.holds(bet)) { Text(Strings.DEAL) }
    }
}

/** Tags the player's hand at [index]. */
fun handTag(index: Int) = "blackjackHand-$index"

/** Tags what the dealer shows, or comes to. */
const val DEALER_TAG = "dealer"

/** Tags what the last round won back. */
const val BLACKJACK_RESULT_TAG = "blackjackResult"

private val ChipSize = 40.dp
private val CARD_MOST = 72.dp

private const val CARDS_ACROSS = 6.5f
