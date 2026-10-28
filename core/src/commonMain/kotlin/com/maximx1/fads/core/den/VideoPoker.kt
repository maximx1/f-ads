package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.model.Card
import kotlinx.serialization.Serializable

/** The hands Jacks or Better pays for, best first, each paying [pays] for every bronze bet. */
enum class PayLine(
    val label: String,
    val pays: Int,
) {
    ROYAL_FLUSH("Royal flush", ROYAL),
    STRAIGHT_FLUSH("Straight flush", STRAIGHT_FLUSH_PAYS),
    FOUR_OF_A_KIND("Four of a kind", FOUR),
    FULL_HOUSE("Full house", FULL_HOUSE_PAYS),
    FLUSH("Flush", FLUSH_PAYS),
    STRAIGHT("Straight", STRAIGHT_PAYS),
    THREE_OF_A_KIND("Three of a kind", THREE),
    TWO_PAIR("Two pair", TWO_PAIR_PAYS),
    JACKS_OR_BETTER("Jacks or better", 1),
}

/**
 * Jacks or Better: five cards dealt, any of them held, the rest drawn again once, and the hand paid by its
 * [PayLine], from a pair of jacks or better, which gives the bet back, to a royal flush.
 */
object JacksOrBetter {
    /** The line of the pay table [cards] make, or `null` if they pay nothing. */
    fun line(cards: List<Card>): PayLine? {
        val rank = HandRank.of(cards)
        val top = rank.kickers.first()
        return when (rank.category) {
            HandCategory.STRAIGHT_FLUSH -> if (top == ACE) PayLine.ROYAL_FLUSH else PayLine.STRAIGHT_FLUSH
            HandCategory.FOUR_OF_A_KIND -> PayLine.FOUR_OF_A_KIND
            HandCategory.FULL_HOUSE -> PayLine.FULL_HOUSE
            HandCategory.FLUSH -> PayLine.FLUSH
            HandCategory.STRAIGHT -> PayLine.STRAIGHT
            HandCategory.THREE_OF_A_KIND -> PayLine.THREE_OF_A_KIND
            HandCategory.TWO_PAIR -> PayLine.TWO_PAIR
            HandCategory.ONE_PAIR -> PayLine.JACKS_OR_BETTER.takeIf { top >= JACK }
            HandCategory.HIGH_CARD -> null
        }
    }

    /** What [cards] pay for every bronze bet. */
    fun pays(cards: List<Card>): Int = line(cards)?.pays ?: 0
}

/**
 * One hand of video poker: deck number [number], the [bet] on it, the five [cards] showing, and whether it has been
 * [drawn] yet, after which it has [won].
 */
@Serializable
data class VideoPokerHand(
    val number: Long,
    val bet: Long,
    val cards: List<Card>,
    val drawn: Boolean = false,
    val won: Long = 0,
)

/** A video poker machine: how many hands it has dealt, and the one showing. */
@Serializable
data class VideoPoker(
    val hands: Long = 0,
    val hand: VideoPokerHand? = null,
) {
    /** True while a hand waits for its draw. */
    val dealt: Boolean get() = hand != null && !hand.drawn
}

/** The video poker machine in [room]. */
fun Den.videoPoker(room: Room = Room.FLOOR): VideoPoker = videoPokers[room] ?: VideoPoker()

/**
 * This den with hand [number] dealt at [room]'s machine for [bet] bronzes. Throws [IllegalArgumentException] if a
 * hand waits for its draw, the bet is under the room's least, or the purse does not hold it.
 */
fun Den.dealVideoPoker(
    bet: Long,
    number: Long,
    room: Room = Room.FLOOR,
): Den {
    val machine = videoPoker(room)
    require(!machine.dealt) { "Draw first" }
    require(bet >= room.leastBet) { "Bets start at ${room.leastBet}, not $bet" }
    val hand = VideoPokerHand(number, bet, Card.deck.shuffledFor(number).take(HandRank.HAND_SIZE))
    return copy(purse = purse - bet, videoPokers = videoPokers + (room to VideoPoker(machine.hands + 1, hand)))
}

/**
 * This den after the draw at [room]'s machine: the cards not [held], by place, are thrown away for the next ones
 * off the deck, and the hand is paid. Throws [IllegalArgumentException] if no hand waits for its draw.
 */
fun Den.drawVideoPoker(
    held: Set<Int>,
    room: Room = Room.FLOOR,
): Den {
    val machine = videoPoker(room)
    val hand = machine.hand?.takeIf { machine.dealt } ?: throw IllegalArgumentException("Deal first")
    val fresh =
        Card.deck
            .shuffledFor(hand.number)
            .drop(HandRank.HAND_SIZE)
            .iterator()
    val cards = hand.cards.mapIndexed { index, card -> if (index in held) card else fresh.next() }
    val won = hand.bet * JacksOrBetter.pays(cards)
    val drawn = hand.copy(cards = cards, drawn = true, won = won)
    return copy(purse = purse + won, videoPokers = videoPokers + (room to machine.copy(hand = drawn)))
}

private const val ROYAL = 250
private const val STRAIGHT_FLUSH_PAYS = 50
private const val FOUR = 25
private const val FULL_HOUSE_PAYS = 9
private const val FLUSH_PAYS = 6
private const val STRAIGHT_PAYS = 4
private const val THREE = 3
private const val TWO_PAIR_PAYS = 2
private const val ACE = 14
private const val JACK = 11
