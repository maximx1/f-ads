package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/**
 * A poker table in the den: the player in the first of the [seats], the den's players in the rest, the seat the
 * button is at, and the [hand] being played, or the last one played while its result shows. [joined] counts the
 * den's players who have sat down here, which picks the next one's name.
 */
@Serializable
data class PokerTable(
    val seats: List<Seat>,
    val button: Int = 0,
    val hand: PokerHand? = null,
    val joined: Int = 0,
) {
    /** True while a hand is being played. */
    val playing: Boolean get() = hand != null && hand.outcome == null

    /**
     * This table with a new player of the den's in every seat but the player's that has nothing left, each bringing
     * between half and one and a half times [purse], and never less than [LEAST_BOT_STACK], as [seed] picks.
     */
    fun refilled(
        purse: Purse,
        seed: Long,
    ): PokerTable {
        val random = SplitMix64(seed)
        var newcomers = joined
        val filled =
            seats.mapIndexed { seat, player ->
                if (seat == PLAYER || player.stack > 0) {
                    player
                } else {
                    val share = HALF + random.nextBelow(PERCENT + 1)
                    Seat(names[newcomers++ % names.size], maxOf(LEAST_BOT_STACK, purse.total) * share / PERCENT)
                }
            }
        return copy(seats = filled, joined = newcomers)
    }

    /** This table with [stack] in front of the player. */
    fun withPlayerStack(stack: Long): PokerTable =
        copy(seats = seats.mapIndexed { seat, player -> if (seat == PLAYER) player.copy(stack = stack) else player })

    companion object {
        const val PLAYER = 0
        const val BOTS = 3

        /** The bronzes each player antes, or the small blind; bets start at twice it. */
        const val STAKE = 1L

        /** The fewest bronzes one of the den's players sits down with. */
        const val LEAST_BOT_STACK = 100L

        private const val HALF = 50
        private const val PERCENT = 100

        /** The den's players, in the order they come to sit down. */
        val names =
            listOf(
                "Lucky Lou",
                "Duchess",
                "Big Sal",
                "Ivan",
                "Mae",
                "Doc",
                "Rosa",
                "Slim",
                "Kit",
                "The Count",
                "Honey",
                "Ace",
                "Moll",
                "Sarge",
                "Pearl",
                "Dutch",
            )
    }
}

/** [game]'s table, or `null` if the player has never sat down at it. */
fun Den.table(game: PokerGame): PokerTable? = if (game == PokerGame.DRAW) draw else holdem

private fun Den.withTable(
    game: PokerGame,
    table: PokerTable,
): Den = if (game == PokerGame.DRAW) copy(draw = table) else copy(holdem = table)

/**
 * This den with the player sitting at [game]'s table: as it was if they have sat there before, or with the den's
 * players taking their seats, [seed] picking how much each brings.
 */
fun Den.sitDown(
    game: PokerGame,
    seed: Long,
): Den {
    if (table(game) != null) return this
    val empty = PokerTable(List(PokerTable.BOTS + 1) { Seat("", 0, bot = it != PokerTable.PLAYER) })
    return withTable(game, empty.refilled(purse, seed))
}

/**
 * This den with hand [number] dealt at [game]'s table, the player playing with the whole of their purse, and the
 * button moved on a seat. The den's players who have nothing left get up, and new ones sit down in their places.
 * Throws [IllegalArgumentException] if a hand is still being played or the purse is empty.
 */
fun Den.dealHand(
    game: PokerGame,
    number: Long,
): Den {
    val table = requireNotNull(table(game)) { "Sit down first" }
    require(!table.playing) { "A hand is being played" }
    require(purse.total > 0) { "Nothing to play with" }
    val ready = table.refilled(purse, number).withPlayerStack(purse.total)
    val button = (ready.button + 1) % ready.seats.size
    val hand = dealPoker(game, number, ready.seats, button, PokerTable.STAKE)
    // Through [withHand], in case the hand is over as soon as it is dealt.
    return copy(purse = Purse()).withTable(game, ready.copy(button = button)).withHand(game, hand)
}

/** True if it is the player's turn at [game]'s table. */
fun Den.playersTurn(game: PokerGame): Boolean = table(game)?.hand?.toAct == PokerTable.PLAYER

/** This den after the player makes [move] at [game]'s table. Throws [IllegalArgumentException] if they cannot. */
fun Den.playPoker(
    game: PokerGame,
    move: PokerMove,
): Den {
    require(playersTurn(game)) { "It is not the player's turn" }
    val hand = table(game)?.hand ?: error("A hand is being played")
    return withHand(game, hand.play(move))
}

/** True if one of the den's players is to act at [game]'s table. */
fun Den.botsTurn(game: PokerGame): Boolean =
    table(game)?.hand?.let { hand -> hand.toAct?.let { hand.seats[it].bot } } == true

/** This den after the den's player whose turn it is at [game]'s table acts. */
fun Den.playBot(game: PokerGame): Den {
    require(botsTurn(game)) { "It is not a bot's turn" }
    val hand = table(game)?.hand ?: error("A hand is being played")
    return withHand(game, hand.play(hand.botMove()))
}

/**
 * This den with the player away from [game]'s table. A hand they are still in is folded, and the den's players play
 * it out. The table keeps their places for when the player comes back.
 */
fun Den.leaveTable(game: PokerGame): Den {
    val table = table(game)
    var hand = table?.hand?.takeIf { table.playing } ?: return this
    if (PokerTable.PLAYER !in hand.folded) hand = hand.foldOut(PokerTable.PLAYER)
    while (!hand.isOver) hand = hand.play(hand.botMove())
    return withHand(game, hand)
}

// The table with [hand] on it; once the hand is over, what the player has in front of them goes into the purse.
private fun Den.withHand(
    game: PokerGame,
    hand: PokerHand,
): Den {
    val table = (table(game) ?: error("Sit down first")).copy(hand = hand, seats = hand.seats)
    val won = if (hand.isOver) table.seats[PokerTable.PLAYER].stack else 0
    return copy(purse = purse + won).withTable(game, if (won > 0) table.withPlayerStack(0) else table)
}
