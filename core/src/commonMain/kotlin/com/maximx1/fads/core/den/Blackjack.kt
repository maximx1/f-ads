package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank
import kotlinx.serialization.Serializable

/** What the player can do with the hand being played. */
enum class BlackjackMove { HIT, STAND, DOUBLE, SPLIT }

/**
 * One of the player's hands: its [cards], the [bet] on it, whether it was [doubled] or came of a [split], and
 * whether it is [done], stood on, bust, at twenty-one or doubled.
 */
@Serializable
data class BlackjackHand(
    val cards: List<Card>,
    val bet: Long,
    val doubled: Boolean = false,
    val split: Boolean = false,
    val done: Boolean = false,
)

/**
 * A round of blackjack: the [dealer]'s cards, the player's [hands], the one being played, the cards left in the
 * [shoe], and once the round is over how each hand came out and what it [won] back in all.
 */
@Serializable
data class BlackjackRound(
    val dealer: List<Card>,
    val hands: List<BlackjackHand>,
    val shoe: List<Card>,
    val active: Int = 0,
    val results: List<HandResult>? = null,
    val won: Long = 0,
) {
    val over: Boolean get() = results != null
}

/** A blackjack table: how many rounds have been dealt at it, and the one on it. */
@Serializable
data class Blackjack(
    val rounds: Long = 0,
    val round: BlackjackRound? = null,
) {
    /** True while a round is being played. */
    val playing: Boolean get() = round != null && !round.over
}

/** The blackjack table in [room]. */
fun Den.blackjack(room: Room = Room.FLOOR): Blackjack = blackjackTables[room] ?: Blackjack()

/**
 * This den with round [number] dealt at [room]'s blackjack table for [bet] bronzes. Throws
 * [IllegalArgumentException] if a round is being played, the bet is under the room's least, or the purse does not
 * hold it.
 */
fun Den.dealBlackjack(
    bet: Long,
    number: Long,
    room: Room = Room.FLOOR,
): Den = dealBlackjackFrom(bet, Card.deck.shuffledFor(number), room)

/** As [dealBlackjack], from [shoe], top card first. */
internal fun Den.dealBlackjackFrom(
    bet: Long,
    shoe: List<Card>,
    room: Room = Room.FLOOR,
): Den {
    val table = blackjack(room)
    require(!table.playing) { "A round is being played" }
    require(bet >= room.leastBet) { "Bets start at ${room.leastBet}, not $bet" }
    // A card to the player, one to the dealer, and again.
    val round =
        BlackjackRound(
            dealer = listOf(shoe[1], shoe[DEALER_SECOND]),
            hands = listOf(BlackjackHand(listOf(shoe[0], shoe[2]), bet)),
            shoe = shoe.drop(FIRST_DEAL),
        )
    val dealt = copy(purse = purse - bet).withBlackjack(room, Blackjack(table.rounds + 1, round))
    // A blackjack on either side ends the round at once.
    return if (isBlackjack(round.dealer) ||
        isBlackjack(round.hands.first().cards)
    ) {
        dealt.settleBlackjack(room)
    } else {
        dealt
    }
}

/** True if the player can make [move] with the hand being played at [room]'s table. */
fun Den.canPlayBlackjack(
    move: BlackjackMove,
    room: Room = Room.FLOOR,
): Boolean {
    val round = blackjack(room).round?.takeIf { !it.over } ?: return false
    val hand = round.hands[round.active]
    val pair = hand.cards.size == 2 && hand.cards[0].rank == hand.cards[1].rank
    return when (move) {
        BlackjackMove.HIT, BlackjackMove.STAND -> true
        BlackjackMove.DOUBLE -> hand.cards.size == 2 && purse.holds(hand.bet)
        BlackjackMove.SPLIT -> pair && round.hands.size == 1 && purse.holds(hand.bet)
    }
}

/** This den after the player makes [move] at [room]'s table. Throws [IllegalArgumentException] if they cannot. */
fun Den.playBlackjack(
    move: BlackjackMove,
    room: Room = Room.FLOOR,
): Den {
    require(canPlayBlackjack(move, room)) { "$move cannot be played now" }
    val round = blackjack(room).round ?: error("A round is being played")
    val hand = round.hands[round.active]
    val (played, cost) =
        when (move) {
            BlackjackMove.HIT -> {
                round.replace(hand.copy(cards = hand.cards + round.shoe.first()), 1) to 0L
            }

            BlackjackMove.STAND -> {
                round.replace(hand.copy(done = true), 0) to 0L
            }

            BlackjackMove.DOUBLE -> {
                round.replace(
                    hand.copy(cards = hand.cards + round.shoe.first(), bet = hand.bet * 2, doubled = true, done = true),
                    1,
                ) to
                    hand.bet
            }

            BlackjackMove.SPLIT -> {
                round.split() to hand.bet
            }
        }
    val next = played.onward()
    val den = copy(purse = purse - cost).withBlackjack(room, blackjack(room).copy(round = next))
    return if (next.hands.all { it.done }) den.settleBlackjack(room) else den
}

// The round with [hand] in place of the one being played and [used] cards gone from the shoe. A hand that has
// reached twenty-one or more is done.
private fun BlackjackRound.replace(
    hand: BlackjackHand,
    used: Int,
): BlackjackRound {
    val finished = hand.done || totalOf(hand.cards).points >= TWENTY_ONE
    return copy(hands = hands.toMutableList().also { it[active] = hand.copy(done = finished) }, shoe = shoe.drop(used))
}

// Two hands of one card each from the pair, with a card dealt to each. Split aces get one card each and no more.
private fun BlackjackRound.split(): BlackjackRound {
    val hand = hands[active]
    val aces = hand.cards[0].rank == Rank.ACE
    val first = hand.cards.take(1) + shoe[0]
    val second = hand.cards.drop(1) + shoe[1]
    val split =
        listOf(first, second).map { cards ->
            BlackjackHand(cards, hand.bet, split = true, done = aces || totalOf(cards).points >= TWENTY_ONE)
        }
    return copy(hands = split, shoe = shoe.drop(2))
}

// The round with the first hand not yet done in play, if any.
private fun BlackjackRound.onward(): BlackjackRound =
    copy(
        active =
            hands.indexOfFirst { !it.done }.takeIf { it >= 0 } ?: active,
    )

// The dealer turns over and draws to seventeen, unless every hand is bust, and the hands are paid.
private fun Den.settleBlackjack(room: Room): Den {
    val table = blackjack(room)
    val round = table.round ?: return this
    val allBust = round.hands.all { totalOf(it.cards).points > TWENTY_ONE }
    val naturals = isBlackjack(round.dealer) || (round.hands.size == 1 && isBlackjack(round.hands.first().cards))
    var dealer = round.dealer
    var shoe = round.shoe
    while (!allBust && !naturals && totalOf(dealer).points < DEALER_STANDS) {
        dealer = dealer + shoe.first()
        shoe = shoe.drop(1)
    }
    val results = round.hands.map { resultOf(it, dealer) }
    val won = round.hands.zip(results).sumOf { (hand, result) -> paid(hand.bet, result) }
    val settled =
        round.copy(
            dealer = dealer,
            shoe = shoe,
            results = results,
            won = won,
            hands =
                round.hands.map {
                    it.copy(done = true)
                },
        )
    return copy(purse = purse + won).withBlackjack(room, table.copy(round = settled))
}

private fun Den.withBlackjack(
    room: Room,
    table: Blackjack,
): Den = copy(blackjackTables = blackjackTables + (room to table))

internal const val TWENTY_ONE = 21
private const val DEALER_STANDS = 17
private const val DEALER_SECOND = 3
private const val FIRST_DEAL = 4
