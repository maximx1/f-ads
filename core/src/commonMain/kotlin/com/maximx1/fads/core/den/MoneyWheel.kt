package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/** What a segment of the money wheel shows, how many of the wheel's segments show it, and what it pays, to one. */
enum class WheelSymbol(
    val pays: Int,
    val segments: Int,
) {
    ONE(1, ONES),
    TWO(2, TWOS),
    FIVE(FIVE_PAYS, FIVES),
    TEN(TEN_PAYS, TENS),
    TWENTY(TWENTY_PAYS, 2),
    JOKER(TOP_PAYS, 1),
    LOGO(TOP_PAYS, 1),
}

/**
 * The money wheel, or big six: [SEGMENTS] segments round it, the rarer symbols spaced evenly round and the ones
 * filling the rest. The joker sits at the top of the wheel's own turn and the logo opposite it.
 */
object MoneyWheel {
    const val SEGMENTS = 54

    /** The symbol on each segment, clockwise. */
    val segments: List<WheelSymbol> =
        Array<WheelSymbol?>(SEGMENTS) { null }
            .also { wheel ->
                for (symbol in WheelSymbol.entries.reversed().filter { it != WheelSymbol.ONE }) {
                    repeat(symbol.segments) { copy ->
                        // Evenly round, starting a little along for each symbol, onto the next free segment.
                        var at = (copy * SEGMENTS / symbol.segments + symbol.ordinal * SPREAD) % SEGMENTS
                        while (wheel[at] != null) at = (at + 1) % SEGMENTS
                        wheel[at] = symbol
                    }
                }
            }.map { it ?: WheelSymbol.ONE }

    /** The segment spin number [number] stops on. */
    fun landing(number: Long): Int = SplitMix64(number).nextBelow(SEGMENTS)
}

/**
 * A money wheel table: the [bets] on each symbol, how many spins there have been, and the last one's segment and
 * payout.
 */
@Serializable
data class MoneyWheelTable(
    val bets: Map<WheelSymbol, Long> = emptyMap(),
    val spins: Long = 0,
    val landed: Int? = null,
    val paid: Long = 0,
) {
    val staked: Long get() = bets.values.sum()
}

/** The money wheel in [room]. */
fun Den.moneyWheel(room: Room = Room.FLOOR): MoneyWheelTable = moneyWheels[room] ?: MoneyWheelTable()

/**
 * This den with [amount] more bronzes on [symbol] at [room]'s wheel. Throws [IllegalArgumentException] if the amount
 * is under the room's least bet or more than the purse holds.
 */
fun Den.betMoneyWheel(
    symbol: WheelSymbol,
    amount: Long,
    room: Room = Room.FLOOR,
): Den {
    require(amount >= room.leastBet) { "Bets start at ${room.leastBet}, not $amount" }
    val table = moneyWheel(room)
    val bets = table.bets + (symbol to (table.bets[symbol] ?: 0) + amount)
    return copy(purse = purse - amount, moneyWheels = moneyWheels + (room to table.copy(bets = bets)))
}

/** This den with the bets at [room]'s wheel taken back into the purse. */
fun Den.clearMoneyWheel(room: Room = Room.FLOOR): Den {
    val table = moneyWheel(room)
    return copy(purse = purse + table.staked, moneyWheels = moneyWheels + (room to table.copy(bets = emptyMap())))
}

/** This den after spin number [number] of [room]'s wheel. Throws [IllegalArgumentException] with nothing bet. */
fun Den.spinMoneyWheel(
    number: Long,
    room: Room = Room.FLOOR,
): Den = spinMoneyWheelTo(MoneyWheel.landing(number), room)

/** As [spinMoneyWheel], stopping on [segment]. */
internal fun Den.spinMoneyWheelTo(
    segment: Int,
    room: Room = Room.FLOOR,
): Den {
    val table = moneyWheel(room)
    require(table.bets.isNotEmpty()) { "Nothing is bet" }
    val symbol = MoneyWheel.segments[segment]
    val paid = (table.bets[symbol] ?: 0) * (symbol.pays + 1)
    val spun = MoneyWheelTable(spins = table.spins + 1, landed = segment, paid = paid)
    return copy(purse = purse + paid, moneyWheels = moneyWheels + (room to spun))
}

private const val ONES = 24
private const val TWOS = 15
private const val FIVES = 7
private const val TENS = 4
private const val FIVE_PAYS = 5
private const val TEN_PAYS = 10
private const val TWENTY_PAYS = 20
private const val TOP_PAYS = 40
private const val SPREAD = 3
