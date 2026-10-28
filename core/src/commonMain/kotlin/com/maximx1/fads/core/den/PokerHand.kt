package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.model.Card
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The poker games played in the den. */
enum class PokerGame {
    /** Five-card draw: antes, five cards each, a round of betting, a draw, and a second round. */
    DRAW,

    /** Texas Hold'em: blinds, two cards each, and five shared ones dealt three, one and one, with betting between. */
    HOLDEM,
}

/** Where a hand has got to. Draw goes [BEFORE_DRAW], [DRAW], [AFTER_DRAW]; Hold'em [PREFLOP] to [RIVER]. */
enum class Street { BEFORE_DRAW, DRAW, AFTER_DRAW, PREFLOP, FLOP, TURN, RIVER, SHOWDOWN }

/** A player at a poker table: the [name] shown, the bronzes in front of them, and whether the den plays them. */
@Serializable
data class Seat(
    val name: String,
    val stack: Long,
    val bot: Boolean = true,
)

/** What a player can do when it is their turn. */
@Serializable
sealed interface PokerMove {
    @Serializable
    @SerialName("fold")
    data object Fold : PokerMove

    /** Bets nothing more, when nothing more is owed. */
    @Serializable
    @SerialName("check")
    data object Check : PokerMove

    /** Puts in what is owed, or everything left if that is less. */
    @Serializable
    @SerialName("call")
    data object Call : PokerMove

    /** Bets or raises, so as to have [to] in front of them on this street. */
    @Serializable
    @SerialName("raise")
    data class Raise(
        val to: Long,
    ) : PokerMove

    /** In five-card draw, throws away the cards at [discards], places in the hand, for as many new ones. */
    @Serializable
    @SerialName("draw")
    data class Draw(
        val discards: Set<Int>,
    ) : PokerMove
}

/** What a player last did, as the table shows it, with the [amount] it took or the cards it drew. */
@Serializable
data class Said(
    val word: Word,
    val amount: Long = 0,
) {
    enum class Word { ANTE, BLIND, FOLD, CHECK, CALL, BET, RAISE, ALL_IN, DRAW }
}

/** How a hand ended: what each seat [won], whose cards were [shown], and what each shown hand was. */
@Serializable
data class Outcome(
    val won: List<Long>,
    val shown: Set<Int>,
    val hands: Map<Int, HandCategory> = emptyMap(),
) {
    val winners: Set<Int> get() = won.indices.filter { won[it] > 0 }.toSet()
}

/**
 * One hand of poker, from the deal to the end. The cards come off deck number [number] in turn, [dealt] of them
 * so far. Each seat's [stack] has had their bets taken out: [bets] is what each has in front of them on this
 * street, and [paid] what each has put in over the whole hand, which together make the pot. [toAct] is whose turn
 * it is, `null` once the hand is over. [raiseBy] is the least a raise must add, and [acted] the seats that have
 * acted since the last bet or raise. Seats in [folded] are out of the hand, as are seats that had nothing to play.
 */
@Serializable
data class PokerHand(
    val game: PokerGame,
    val number: Long,
    val stake: Long,
    val seats: List<Seat>,
    val button: Int,
    val hole: List<List<Card>>,
    val board: List<Card> = emptyList(),
    val dealt: Int,
    val street: Street,
    val bets: List<Long>,
    val paid: List<Long>,
    val toAct: Int?,
    val raiseBy: Long,
    val folded: Set<Int> = emptySet(),
    val acted: Set<Int> = emptySet(),
    val said: List<Said?> = seats.map { null },
    val moves: Int = 0,
    val outcome: Outcome? = null,
) {
    /** The most anyone has in front of them on this street. */
    val topBet: Long get() = bets.max()

    val pot: Long get() = paid.sum()

    /** The seats still in the hand. */
    val live: List<Int> get() = seats.indices.filter { it !in folded }

    /** The seats still in the hand with bronzes left to bet. */
    val actors: List<Int> get() = live.filter { seats[it].stack > 0 }

    val isOver: Boolean get() = outcome != null

    /** What [seat] must put in to stay in. */
    fun owed(seat: Int): Long = topBet - bets[seat]

    /** The least a bet or raise can bring a seat's bet up to, unless it goes all in for less. */
    val leastRaise: Long get() = topBet + raiseBy

    /** The deck the cards come off. */
    internal val deck: List<Card> get() = Card.deck.shuffledFor(number)

    /** The seat after [seat] round the table, of those [among]. */
    internal fun after(
        seat: Int,
        among: List<Int>,
    ): Int? = (1..seats.size).map { (seat + it) % seats.size }.firstOrNull { it in among }

    companion object {
        /** Cards each player gets. */
        fun cardsEach(game: PokerGame): Int = if (game == PokerGame.DRAW) HandRank.HAND_SIZE else 2
    }
}
