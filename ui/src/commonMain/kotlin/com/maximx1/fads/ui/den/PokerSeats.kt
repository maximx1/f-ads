package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.PokerHand
import com.maximx1.fads.core.den.PokerTable
import com.maximx1.fads.core.den.Seat
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.Strings

/**
 * The poker table: the felt, the player at the foot of it and the den's players round the rest, and the pot and the
 * shared cards in the middle. During the draw, tapping one of the player's own cards marks it to throw away.
 */
@Composable
internal fun PokerFelt(
    table: PokerTable,
    discards: Set<Int>,
    style: CardStyle,
    onCard: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val hand = table.hand
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 48.dp)) {
            val rim = size.minDimension * RIM
            drawOval(Brush.verticalGradient(listOf(RimLight, RimDark)))
            drawOval(
                Brush.radialGradient(listOf(FeltLight, FeltDark)),
                Offset(rim, rim),
                Size(
                    size.width - rim * 2,
                    size.height - rim * 2,
                ),
            )
            drawOval(
                FeltLine,
                Offset(rim * 2, rim * 2),
                Size(size.width - rim * LINE_INSET, size.height - rim * LINE_INSET),
                style = Stroke(LINE_WIDTH),
            )
        }
        val small = minOf(maxWidth / SMALL_CARDS_ACROSS, SMALL_CARD_MOST)
        Middle(hand, style, small, Modifier.align(Alignment.Center))
        // Round the table clockwise from the player at the foot of it.
        OtherSeat(table, LEFT, style, small, Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
        OtherSeat(table, ACROSS, style, small, Modifier.align(Alignment.TopCenter))
        OtherSeat(table, RIGHT, style, small, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
        PlayerSeat(
            table,
            discards,
            style,
            minOf(maxWidth / BIG_CARDS_ACROSS, BIG_CARD_MOST),
            onCard,
            Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The pot, and in Hold'em the cards on the board. */
@Composable
private fun Middle(
    hand: PokerHand?,
    style: CardStyle,
    width: Dp,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (hand != null && hand.board.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) { for (card in hand.board) PlayingCard(card, style, width) }
        }
        if (hand != null && !hand.isOver) {
            Text(
                Strings.pot(hand.pot),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp).testTag(POT_TAG),
            )
        }
    }
}

/** One of the den's players: name, stack and what they last did, with their cards face down until they show. */
@Composable
private fun OtherSeat(
    table: PokerTable,
    seat: Int,
    style: CardStyle,
    width: Dp,
    modifier: Modifier,
) {
    val hand = table.hand
    val player = hand?.seats?.getOrNull(seat) ?: table.seats[seat]
    val cards = hand?.hole?.getOrNull(seat).orEmpty()
    val shown = hand?.outcome?.shown?.contains(seat) == true
    val out = hand != null && seat in hand.folded
    Column(
        modifier.alpha(if (out) FOLDED_ALPHA else 1f).testTag(seatTag(seat)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SeatLabel(player, seat, table, hand)
        if (cards.isNotEmpty() && !out) {
            // Fanned close together, as a player holds them.
            Box(Modifier.size(width + width * FAN * (cards.size - 1), width * CARD_ASPECT)) {
                cards.forEachIndexed { index, card ->
                    PlayingCard(card.takeIf { shown }, style, width, Modifier.offset(x = width * FAN * index))
                }
            }
        }
    }
}

/** The player: their own cards face up, their stack, and what they last did. */
@Composable
@Suppress("LongParameterList") // The seat, how its cards are drawn, and what a tap on one does.
private fun PlayerSeat(
    table: PokerTable,
    discards: Set<Int>,
    style: CardStyle,
    width: Dp,
    onCard: ((Int) -> Unit)?,
    modifier: Modifier,
) {
    val hand = table.hand
    val cards = hand?.hole?.getOrNull(PokerTable.PLAYER).orEmpty()
    val out = hand != null && PokerTable.PLAYER in hand.folded
    Column(modifier.alpha(if (out) FOLDED_ALPHA else 1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            cards.forEachIndexed { index, card ->
                val thrown = index in discards
                PlayingCard(
                    card,
                    style,
                    width,
                    Modifier
                        .offset(y = if (thrown) -width * LIFT else 0.dp)
                        .alpha(if (thrown) THROWN_ALPHA else 1f)
                        .then(if (onCard != null) Modifier.clickable { onCard(index) } else Modifier)
                        .testTag(playerCardTag(index)),
                )
            }
        }
        SeatLabel(hand?.seats?.get(PokerTable.PLAYER) ?: table.seats[PokerTable.PLAYER], PokerTable.PLAYER, table, hand)
    }
}

/** A player's name, with the button beside it if it is theirs, then their stack and what they last did. */
@Composable
private fun SeatLabel(
    player: Seat,
    seat: Int,
    table: PokerTable,
    hand: PokerHand?,
) {
    val turn = hand?.toAct == seat
    Column(
        Modifier
            .then(if (turn) Modifier.border(2.dp, Turn, RoundedCornerShape(8.dp)) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (player.bot) player.name else Strings.YOU,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if ((hand?.button ?: table.button) == seat) {
                Box(
                    Modifier.padding(start = 4.dp).size(16.dp).border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("D", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text(Strings.stack(player.stack), style = MaterialTheme.typography.labelMedium)
        val said = hand?.said?.getOrNull(seat)
        val result = hand?.outcome?.let { Strings.outcomeOf(it, seat) }
        Text(
            result ?: said?.let(Strings::said) ?: "",
            style = MaterialTheme.typography.labelMedium,
            color = Said,
            modifier = Modifier.testTag(saidTag(seat)),
        )
    }
}

/** A playing card [width] wide, face up, or face down if [card] is `null`. */
@Composable
internal fun PlayingCard(
    card: Card?,
    style: CardStyle,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(width, width * CARD_ASPECT)) {
        with(style) { if (card == null) drawBack() else drawCard(card) }
    }
}

/** Tags a seat at the poker table. */
fun seatTag(seat: Int) = "seat-$seat"

/** Tags the line under a seat that says what its player last did, or how the hand went for them. */
fun saidTag(seat: Int) = "said-$seat"

/** Tags each of the player's own cards. */
fun playerCardTag(index: Int) = "card-$index"

/** Tags the size of the pot. */
const val POT_TAG = "pot"

private val RimLight = Color(0xFF6B4423)
private val RimDark = Color(0xFF2E1A0B)
private val FeltLight = Color(0xFF1F7A45)
private val FeltDark = Color(0xFF0E3F22)
private val FeltLine = Color(0x40FFFFFF)
private val Turn = Color(0xFFFFE082)
private val Said = Color(0xFFFFE082)
private val SMALL_CARD_MOST = 44.dp
private val BIG_CARD_MOST = 72.dp

private const val RIM = 0.05f
private const val LINE_INSET = 4
private const val LINE_WIDTH = 2f
private const val LEFT = 1
private const val ACROSS = 2
private const val RIGHT = 3
private const val CARD_ASPECT = 1.4f
private const val SMALL_CARDS_ACROSS = 9
private const val BIG_CARDS_ACROSS = 6
private const val FAN = 0.45f
private const val LIFT = 0.25f
private const val FOLDED_ALPHA = 0.45f
private const val THROWN_ALPHA = 0.6f
