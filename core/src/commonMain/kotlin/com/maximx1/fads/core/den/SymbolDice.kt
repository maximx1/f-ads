package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/**
 * The games of three dice and six faces, played the same way and looking different: chuck-a-luck's dice show their
 * pips, and crown and anchor's a crown, an anchor and the four suits. A bet goes on a face, and pays evens for each
 * die that shows it, so two to one for two and three to one for three.
 */
enum class SymbolDiceGame { CHUCK_A_LUCK, CROWN_AND_ANCHOR }

/** A table of [SymbolDiceGame]: the [bets] down on each face, how many throws there have been, and the last. */
@Serializable
data class SymbolDice(
    val bets: Map<Int, Long> = emptyMap(),
    val throws: Long = 0,
    val dice: List<Int>? = null,
    val paid: Long = 0,
) {
    val staked: Long get() = bets.values.sum()

    companion object {
        const val FACES = 6
        const val DICE = 3

        /** What a bronze on [face] comes back as when the dice show [dice], the bronze included: 0 if it loses. */
        fun returns(
            face: Int,
            dice: List<Int>,
        ): Long = dice.count { it == face }.let { if (it > 0) it + 1L else 0 }
    }
}

/** [game]'s table in [room]. */
fun Den.symbolDice(
    game: SymbolDiceGame,
    room: Room = Room.FLOOR,
): SymbolDice = symbolTables(game)[room] ?: SymbolDice()

/**
 * This den with [amount] more bronzes on [face] at [game]'s table in [room]. Throws [IllegalArgumentException] if
 * there is no such face, or the amount is under the room's least bet or more than the purse holds.
 */
fun Den.betSymbolDice(
    game: SymbolDiceGame,
    face: Int,
    amount: Long,
    room: Room = Room.FLOOR,
): Den {
    require(face in 1..SymbolDice.FACES) { "There is no face $face" }
    require(amount >= room.leastBet) { "Bets start at ${room.leastBet}, not $amount" }
    val table = symbolDice(game, room)
    val bets = table.bets + (face to (table.bets[face] ?: 0) + amount)
    return copy(purse = purse - amount).withSymbolDice(game, room, table.copy(bets = bets))
}

/** This den with the bets at [game]'s table in [room] taken back into the purse. */
fun Den.clearSymbolDice(
    game: SymbolDiceGame,
    room: Room = Room.FLOOR,
): Den {
    val table = symbolDice(game, room)
    return copy(purse = purse + table.staked).withSymbolDice(game, room, table.copy(bets = emptyMap()))
}

/** This den after throw [number] at [game]'s table in [room]. Throws [IllegalArgumentException] with nothing bet. */
fun Den.rollSymbolDice(
    game: SymbolDiceGame,
    number: Long,
    room: Room = Room.FLOOR,
): Den {
    val random = SplitMix64(number)
    return rollSymbolDiceAs(game, List(SymbolDice.DICE) { random.nextBelow(SymbolDice.FACES) + 1 }, room)
}

/** As [rollSymbolDice], with the dice landing on [dice]. */
internal fun Den.rollSymbolDiceAs(
    game: SymbolDiceGame,
    dice: List<Int>,
    room: Room = Room.FLOOR,
): Den {
    val table = symbolDice(game, room)
    require(table.bets.isNotEmpty()) { "Nothing is bet" }
    val paid = table.bets.entries.sumOf { (face, stake) -> stake * SymbolDice.returns(face, dice) }
    return copy(
        purse = purse + paid,
    ).withSymbolDice(game, room, SymbolDice(throws = table.throws + 1, dice = dice, paid = paid))
}

private fun Den.symbolTables(game: SymbolDiceGame): Map<Room, SymbolDice> =
    if (game == SymbolDiceGame.CHUCK_A_LUCK) chuckALuckTables else crownAndAnchorTables

private fun Den.withSymbolDice(
    game: SymbolDiceGame,
    room: Room,
    table: SymbolDice,
): Den =
    if (game == SymbolDiceGame.CHUCK_A_LUCK) {
        copy(chuckALuckTables = chuckALuckTables + (room to table))
    } else {
        copy(crownAndAnchorTables = crownAndAnchorTables + (room to table))
    }
