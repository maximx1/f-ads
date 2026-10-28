package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.model.Card
import kotlinx.serialization.Serializable

/** Where a bet goes at baccarat: on the player's hand, the banker's, or a tie between them. */
enum class BaccaratSide { PLAYER, BANKER, TIE }

/** A card's value at baccarat: aces one, two to nine as marked, tens and court cards nothing. */
val Card.baccaratValue: Int get() = rank.value.takeIf { it < TEN } ?: 0

/** What [cards] come to at baccarat: their values added, only the last digit counting. */
fun baccaratTotal(cards: List<Card>): Int = cards.sumOf { it.baccaratValue } % TEN

/** One coup: the [player]'s and [banker]'s cards, which side [won], the [staked] bronzes and what was [paid]. */
@Serializable
data class Coup(
    val player: List<Card>,
    val banker: List<Card>,
    val won: BaccaratSide,
    val staked: Long,
    val paid: Long,
)

/** A baccarat table: the [bets] down, how many coups there have been, and the last. */
@Serializable
data class Baccarat(
    val bets: Map<BaccaratSide, Long> = emptyMap(),
    val coups: Long = 0,
    val last: Coup? = null,
) {
    val staked: Long get() = bets.values.sum()
}

/** The baccarat table in [room]. */
fun Den.baccarat(room: Room = Room.FLOOR): Baccarat = baccaratTables[room] ?: Baccarat()

/**
 * This den with [amount] more bronzes from the purse on [side] at [room]'s table. Throws
 * [IllegalArgumentException] if the amount is under the room's least bet or more than the purse holds.
 */
fun Den.betBaccarat(
    side: BaccaratSide,
    amount: Long,
    room: Room = Room.FLOOR,
): Den {
    require(amount >= room.leastBet) { "Bets start at ${room.leastBet}, not $amount" }
    val table = baccarat(room)
    val bets = table.bets + (side to (table.bets[side] ?: 0) + amount)
    return copy(purse = purse - amount, baccaratTables = baccaratTables + (room to table.copy(bets = bets)))
}

/** This den with every bet at [room]'s table taken back into the purse. */
fun Den.clearBaccarat(room: Room = Room.FLOOR): Den {
    val table = baccarat(room)
    return copy(purse = purse + table.staked, baccaratTables = baccaratTables + (room to table.copy(bets = emptyMap())))
}

/**
 * This den after coup [number] at [room]'s table: the cards dealt by the rules of the game, and the bets paid. Throws
 * [IllegalArgumentException] with nothing bet.
 */
fun Den.playBaccarat(
    number: Long,
    room: Room = Room.FLOOR,
): Den = playBaccaratFrom(Card.deck.shuffledFor(number), room)

/** As [playBaccarat], from [shoe], top card first. */
internal fun Den.playBaccaratFrom(
    shoe: List<Card>,
    room: Room = Room.FLOOR,
): Den {
    val table = baccarat(room)
    require(table.bets.isNotEmpty()) { "Nothing is bet" }
    val (player, banker) = coupOf(shoe)
    val playerTotal = baccaratTotal(player)
    val bankerTotal = baccaratTotal(banker)
    val won =
        when {
            playerTotal > bankerTotal -> BaccaratSide.PLAYER
            bankerTotal > playerTotal -> BaccaratSide.BANKER
            else -> BaccaratSide.TIE
        }
    val paid = table.bets.entries.sumOf { (side, stake) -> payout(side, stake, won) }
    val coup = Coup(player, banker, won, table.staked, paid)
    return copy(
        purse = purse + paid,
        baccaratTables =
            baccaratTables + (room to Baccarat(coups = table.coups + 1, last = coup)),
    )
}

/**
 * The player's and banker's cards for a coup off [shoe]: two each, a card at a time from the player, then a third
 * for either by the rules. Nobody draws to an eight or nine. The player draws to five or less; the banker then draws
 * to five or less if the player stood, and otherwise as its total and the player's third card say.
 */
internal fun coupOf(shoe: List<Card>): Pair<List<Card>, List<Card>> {
    var player = listOf(shoe[0], shoe[2])
    var banker = listOf(shoe[1], shoe[THIRD_DEALT])
    var next = FIRST_DEAL
    if (baccaratTotal(player) >= NATURAL || baccaratTotal(banker) >= NATURAL) return player to banker
    var third: Int? = null
    if (baccaratTotal(player) <= DRAWS_TO) {
        player = player + shoe[next++]
        third = player.last().baccaratValue
    }
    if (bankerDraws(baccaratTotal(banker), third)) banker = banker + shoe[next]
    return player to banker
}

// Whether the banker draws on [total], given the player's [third] card, or `null` if the player stood.
private fun bankerDraws(
    total: Int,
    third: Int?,
): Boolean =
    when {
        third == null -> total <= DRAWS_TO
        total <= 2 -> true
        total == BANKER_THREE -> third != EIGHT
        total == BANKER_FOUR -> third in 2..SEVEN
        total == BANKER_FIVE -> third in BANKER_FOUR..SEVEN
        total == BANKER_SIX -> third in BANKER_SIX..SEVEN
        else -> false
    }

// What [stake] on [side] comes back as when [won] wins: evens for the player, evens less five in a hundred for the
// banker, eight to one for a tie, and the player and banker bets handed back on a tie.
private fun payout(
    side: BaccaratSide,
    stake: Long,
    won: BaccaratSide,
): Long =
    when {
        side == won && side == BaccaratSide.TIE -> stake * (TIE_PAYS + 1)
        side == won && side == BaccaratSide.BANKER -> stake + stake * BANKER_PAYS / PERCENT
        side == won -> stake * 2
        won == BaccaratSide.TIE -> stake
        else -> 0
    }

private const val TEN = 10
private const val NATURAL = 8
private const val DRAWS_TO = 5
private const val FIRST_DEAL = 4
private const val THIRD_DEALT = 3
private const val BANKER_THREE = 3
private const val BANKER_FOUR = 4
private const val BANKER_FIVE = 5
private const val BANKER_SIX = 6
private const val SEVEN = 7
private const val EIGHT = 8
private const val TIE_PAYS = 8
private const val BANKER_PAYS = 95
private const val PERCENT = 100
