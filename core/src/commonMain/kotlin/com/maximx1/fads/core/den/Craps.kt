package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/**
 * The bets on the craps layout. The pass and don't pass lines are bet before a point is set and settle when it is
 * made or the dice seven out. The field, any seven and any craps settle on the next roll. A place bet on a [number]
 * wins whenever it rolls while a point is on, paying [pays] to [per] and staying up, and loses to a seven.
 */
enum class CrapsBet(
    val number: Int = 0,
    val pays: Int = 1,
    val per: Int = 1,
) {
    PASS,
    DONT_PASS,
    FIELD,
    ANY_SEVEN(pays = ANY_SEVEN_PAYS),
    ANY_CRAPS(pays = ANY_CRAPS_PAYS),
    PLACE_4(FOUR, NINE, FIVE),
    PLACE_5(FIVE, SEVEN, FIVE),
    PLACE_6(SIX, SEVEN, SIX),
    PLACE_8(EIGHT, SEVEN, SIX),
    PLACE_9(NINE, SEVEN, FIVE),
    PLACE_10(TEN, NINE, FIVE),
    ;

    val isLine: Boolean get() = this == PASS || this == DONT_PASS

    val isPlace: Boolean get() = number > 0
}

/** One throw of the dice: the two faces, and what it paid back. */
@Serializable
data class Throw(
    val first: Int,
    val second: Int,
    val paid: Long,
) {
    val total: Int get() = first + second
}

/** A craps table: the [point], if one is on, the [bets] down, how many throws there have been, and the last. */
@Serializable
data class Craps(
    val point: Int? = null,
    val bets: Map<CrapsBet, Long> = emptyMap(),
    val throws: Long = 0,
    val last: Throw? = null,
) {
    val staked: Long get() = bets.values.sum()
}

/** The craps table in [room]. */
fun Den.craps(room: Room = Room.FLOOR): Craps = crapsTables[room] ?: Craps()

/** True if [bet] can go down now at [room]'s table: the lines only before a point is set. */
fun Den.canBetCraps(
    bet: CrapsBet,
    room: Room = Room.FLOOR,
): Boolean = !bet.isLine || craps(room).point == null

/**
 * This den with [amount] more bronzes on [bet] at [room]'s table. Throws [IllegalArgumentException] if it cannot go
 * down now, is under the room's least bet, or is more than the purse holds.
 */
fun Den.betCraps(
    bet: CrapsBet,
    amount: Long,
    room: Room = Room.FLOOR,
): Den {
    require(canBetCraps(bet, room)) { "The lines are bet before a point is set" }
    require(amount >= room.leastBet) { "Bets start at ${room.leastBet}, not $amount" }
    val table = craps(room)
    val bets = table.bets + (bet to (table.bets[bet] ?: 0) + amount)
    return copy(purse = purse - amount, crapsTables = crapsTables + (room to table.copy(bets = bets)))
}

/**
 * This den with every bet that may come down at [room]'s table back in the purse: all but the lines while a point is
 * on.
 */
fun Den.clearCraps(room: Room = Room.FLOOR): Den {
    val table = craps(room)
    val (stay, back) = table.bets.entries.partition { (bet, _) -> bet.isLine && table.point != null }
    val returned = back.sumOf { it.value }
    return copy(
        purse = purse + returned,
        crapsTables =
            crapsTables + (room to table.copy(bets = stay.associate { it.toPair() })),
    )
}

/** This den after throw [number] at [room]'s table. Throws [IllegalArgumentException] with nothing bet. */
fun Den.rollDice(
    number: Long,
    room: Room = Room.FLOOR,
): Den {
    val random = SplitMix64(number)
    return rollDiceAs(random.nextBelow(FACES) + 1, random.nextBelow(FACES) + 1, room)
}

/** As [rollDice], with the dice landing on [first] and [second]. */
internal fun Den.rollDiceAs(
    first: Int,
    second: Int,
    room: Room = Room.FLOOR,
): Den {
    val table = craps(room)
    require(table.bets.isNotEmpty()) { "Nothing is bet" }
    val total = first + second
    var paid = 0L
    val staying = mutableMapOf<CrapsBet, Long>()
    for ((bet, stake) in table.bets) {
        val (back, stays) = settle(bet, stake, total, table.point)
        paid += back
        if (stays) staying[bet] = stake
    }
    val point =
        when {
            table.point == null && total in POINTS -> total
            table.point != null && (total == table.point || total == SEVEN) -> null
            else -> table.point
        }
    val rolled = Craps(point, staying, table.throws + 1, Throw(first, second, paid))
    return copy(purse = purse + paid, crapsTables = crapsTables + (room to rolled))
}

// What [stake] on [bet] pays back on a throw of [total] with [point] on, and whether the bet stays down.
private fun settle(
    bet: CrapsBet,
    stake: Long,
    total: Int,
    point: Int?,
): Pair<Long, Boolean> =
    when {
        bet == CrapsBet.PASS -> {
            line(stake, total, point, mine = true)
        }

        bet == CrapsBet.DONT_PASS -> {
            line(stake, total, point, mine = false)
        }

        bet == CrapsBet.FIELD -> {
            (
                if (total in FIELD_DOUBLES) {
                    stake * FIELD_DOUBLE_BACK
                } else if (total in FIELD) {
                    stake * 2
                } else {
                    0L
                }
            ) to
                false
        }

        bet == CrapsBet.ANY_SEVEN -> {
            (if (total == SEVEN) stake * (bet.pays + 1) else 0L) to false
        }

        bet == CrapsBet.ANY_CRAPS -> {
            (if (total in CRAPS) stake * (bet.pays + 1) else 0L) to false
        }

        point == null -> {
            0L to true
        }

        total == bet.number -> {
            stake * bet.pays / bet.per to true
        }

        total == SEVEN -> {
            0L to false
        }

        else -> {
            0L to true
        }
    }

// The pass line, if [mine], or the don't pass, on a throw of [total] with [point] on: what it pays back, and whether
// it stays down. On the come-out roll 7 and 11 win for the pass and 2, 3 and 12 lose; the don't pass is the other way
// round, but a 12 only stands it off. Once a point is on, making it wins for the pass and a seven for the don't.
private fun line(
    stake: Long,
    total: Int,
    point: Int?,
    mine: Boolean,
): Pair<Long, Boolean> {
    val passWins = passWins(total, point)
    val barred = !mine && point == null && total == TWELVE
    return when {
        barred || passWins == null -> 0L to true
        passWins == mine -> stake * 2 to false
        else -> 0L to false
    }
}

// Whether a throw of [total] with [point] on wins for the pass line, loses for it, or, `null`, settles nothing.
private fun passWins(
    total: Int,
    point: Int?,
): Boolean? =
    when {
        point == null && total in NATURALS -> true
        point == null && total in CRAPS -> false
        point == null -> null
        total == point -> true
        total == SEVEN -> false
        else -> null
    }

private const val FACES = 6
private const val FIELD_DOUBLE_BACK = 3
private const val FOUR = 4
private const val FIVE = 5
private const val SIX = 6
private const val SEVEN = 7
private const val EIGHT = 8
private const val NINE = 9
private const val TEN = 10
private const val TWELVE = 12
private const val ANY_SEVEN_PAYS = 4
private const val ANY_CRAPS_PAYS = 7
private val POINTS = setOf(FOUR, FIVE, SIX, EIGHT, NINE, TEN)
private val NATURALS = setOf(SEVEN, 11)
private val CRAPS = setOf(2, 3, TWELVE)
private val FIELD = setOf(3, FOUR, NINE, TEN, 11)
private val FIELD_DOUBLES = setOf(2, TWELVE)
