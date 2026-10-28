package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/** The kinds of bet at sic bo, on three dice. */
enum class SicBoKind {
    /** A total of 4 to 10, but not a triple. Evens. */
    SMALL,

    /** A total of 11 to 17, but not a triple. Evens. */
    BIG,

    /** An odd total, but not a triple. Evens. */
    ODD,

    /** An even total, but not a triple. Evens. */
    EVEN,

    /** The dice add up to the bet's value, 4 to 17, paying the more the rarer the total. */
    TOTAL,

    /** The value shows on one die or more: evens for one, two to one for two, three to one for three. */
    SINGLE,

    /** The value shows on two dice or more. Ten to one. */
    DOUBLE,

    /** The value shows on all three dice. 180 to one. */
    TRIPLE,

    /** Any triple at all. Thirty to one. */
    ANY_TRIPLE,
}

/** A bet at sic bo: its [kind], on [value] for the kinds that name a number or a total. */
@Serializable
data class SicBoBet(
    val kind: SicBoKind,
    val value: Int = 0,
) {
    init {
        val onTheLayout =
            when (kind) {
                SicBoKind.TOTAL -> value in TOTAL_PAYS.keys
                SicBoKind.SINGLE, SicBoKind.DOUBLE, SicBoKind.TRIPLE -> value in 1..FACES
                else -> value == 0
            }
        require(onTheLayout) { "There is no $kind bet on $value" }
    }

    /** What a bronze on this bet comes back as when the dice show [dice], the bronze itself included: 0 if it loses. */
    fun returns(dice: List<Int>): Long {
        val total = dice.sum()
        val triple = dice.all { it == dice.first() }
        val showing = dice.count { it == value }
        return when (kind) {
            SicBoKind.SMALL, SicBoKind.BIG, SicBoKind.ODD, SicBoKind.EVEN -> if (!triple && covers(total)) 2 else 0
            SicBoKind.TOTAL -> if (total == value) TOTAL_PAYS.getValue(value) + 1L else 0
            SicBoKind.SINGLE -> if (showing > 0) showing + 1L else 0
            SicBoKind.DOUBLE -> if (showing >= 2) DOUBLE_PAYS + 1L else 0
            SicBoKind.TRIPLE -> if (showing == DICE) TRIPLE_PAYS + 1L else 0
            SicBoKind.ANY_TRIPLE -> if (triple) ANY_TRIPLE_PAYS + 1L else 0
        }
    }

    // Whether an even-money bet covers [total]; a triple takes them all whatever it comes to.
    private fun covers(total: Int): Boolean =
        when (kind) {
            SicBoKind.SMALL -> total in SMALL
            SicBoKind.BIG -> total in BIG
            SicBoKind.ODD -> total % 2 == 1
            else -> total % 2 == 0
        }

    companion object {
        /** What each total pays, to one. */
        val TOTAL_PAYS: Map<Int, Int> =
            mapOf(
                4 to 60,
                5 to 30,
                6 to 17,
                7 to 12,
                8 to 8,
                9 to 6,
                10 to 6,
                11 to 6,
                12 to 6,
                13 to 8,
                14 to 12,
                15 to 17,
                16 to 30,
                17 to 60,
            )
    }
}

/** A sic bo table: the [bets] down, how many throws there have been, and the last three dice and what they paid. */
@Serializable
data class SicBo(
    val bets: Map<SicBoBet, Long> = emptyMap(),
    val throws: Long = 0,
    val dice: List<Int>? = null,
    val paid: Long = 0,
) {
    val staked: Long get() = bets.values.sum()
}

/** The sic bo table in [room]. */
fun Den.sicBo(room: Room = Room.FLOOR): SicBo = sicBoTables[room] ?: SicBo()

/**
 * This den with [amount] more bronzes on [bet] at [room]'s table. Throws [IllegalArgumentException] if the amount is
 * under the room's least bet or more than the purse holds.
 */
fun Den.betSicBo(
    bet: SicBoBet,
    amount: Long,
    room: Room = Room.FLOOR,
): Den {
    require(amount >= room.leastBet) { "Bets start at ${room.leastBet}, not $amount" }
    val table = sicBo(room)
    val bets = table.bets + (bet to (table.bets[bet] ?: 0) + amount)
    return copy(purse = purse - amount, sicBoTables = sicBoTables + (room to table.copy(bets = bets)))
}

/** This den with the bets at [room]'s table taken back into the purse. */
fun Den.clearSicBo(room: Room = Room.FLOOR): Den {
    val table = sicBo(room)
    return copy(purse = purse + table.staked, sicBoTables = sicBoTables + (room to table.copy(bets = emptyMap())))
}

/** This den after throw [number] at [room]'s table. Throws [IllegalArgumentException] with nothing bet. */
fun Den.rollSicBo(
    number: Long,
    room: Room = Room.FLOOR,
): Den {
    val random = SplitMix64(number)
    return rollSicBoAs(List(DICE) { random.nextBelow(FACES) + 1 }, room)
}

/** As [rollSicBo], with the dice landing on [dice]. */
internal fun Den.rollSicBoAs(
    dice: List<Int>,
    room: Room = Room.FLOOR,
): Den {
    val table = sicBo(room)
    require(table.bets.isNotEmpty()) { "Nothing is bet" }
    val paid = table.bets.entries.sumOf { (bet, stake) -> stake * bet.returns(dice) }
    return copy(
        purse = purse + paid,
        sicBoTables =
            sicBoTables + (room to SicBo(throws = table.throws + 1, dice = dice, paid = paid)),
    )
}

private const val DICE = 3
private const val FACES = 6
private const val DOUBLE_PAYS = 10
private const val TRIPLE_PAYS = 180
private const val ANY_TRIPLE_PAYS = 30
private val SMALL = 4..10
private val BIG = 11..17
