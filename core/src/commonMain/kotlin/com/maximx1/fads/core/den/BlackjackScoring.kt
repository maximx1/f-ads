package com.maximx1.fads.core.den

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank

/** A card's value in blackjack: two to ten as marked, the court cards ten, and the ace one, or eleven if it helps. */
val Card.blackjackValue: Int get() = minOf(rank.value, TEN)

/** What [points] cards come to, and whether it is [soft]: an ace counting eleven, which could still count one. */
data class Total(
    val points: Int,
    val soft: Boolean,
)

/** What [cards] come to, an ace counting eleven if that does not take them over twenty-one. */
fun totalOf(cards: List<Card>): Total {
    val hard = cards.sumOf { it.blackjackValue }
    val soft = cards.any { it.rank == Rank.ACE } && hard + ACE_EXTRA <= TWENTY_ONE
    return Total(if (soft) hard + ACE_EXTRA else hard, soft)
}

/** True for twenty-one in the first two cards. */
fun isBlackjack(cards: List<Card>): Boolean = cards.size == 2 && totalOf(cards).points == TWENTY_ONE

/** How a hand of blackjack came out. */
enum class HandResult { BLACKJACK, WIN, PUSH, LOSE }

internal fun resultOf(
    hand: BlackjackHand,
    dealer: List<Card>,
): HandResult {
    val mine = totalOf(hand.cards).points
    val theirs = totalOf(dealer).points
    val natural = isBlackjack(hand.cards) && !hand.split
    return when {
        mine > TWENTY_ONE -> HandResult.LOSE
        natural && !isBlackjack(dealer) -> HandResult.BLACKJACK
        isBlackjack(dealer) && !natural -> HandResult.LOSE
        theirs > TWENTY_ONE || mine > theirs -> HandResult.WIN
        mine == theirs -> HandResult.PUSH
        else -> HandResult.LOSE
    }
}

// What [bet] comes back as for [result], the bet itself included: three to two for a blackjack.
internal fun paid(
    bet: Long,
    result: HandResult,
): Long =
    when (result) {
        HandResult.BLACKJACK -> bet + bet * THREE / 2
        HandResult.WIN -> bet * 2
        HandResult.PUSH -> bet
        HandResult.LOSE -> 0
    }

private const val TEN = 10
private const val ACE_EXTRA = 10
private const val THREE = 3
