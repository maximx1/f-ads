package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** A European roulette wheel: a single zero and the numbers 1 to 36. */
object Wheel {
    const val HIGHEST = 36

    /** The pockets round the wheel, clockwise from the zero. */
    val pockets: List<Int> =
        listOf(
            0,
            32,
            15,
            19,
            4,
            21,
            2,
            25,
            17,
            34,
            6,
            27,
            13,
            36,
            11,
            30,
            8,
            23,
            10,
            5,
            24,
            16,
            33,
            1,
            20,
            14,
            31,
            9,
            22,
            18,
            29,
            7,
            28,
            12,
            35,
            3,
            26,
        )

    private val reds = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)

    fun colorOf(number: Int): PocketColor =
        when (number) {
            0 -> PocketColor.GREEN
            in reds -> PocketColor.RED
            else -> PocketColor.BLACK
        }

    /** The pocket spin number [number] lands in. The same number lands in the same pocket, forever. */
    fun landing(number: Long): Int = pockets[SplitMix64(number).nextBelow(pockets.size)]
}

enum class PocketColor { GREEN, RED, BLACK }

/**
 * The kinds of bet on the layout, each paying [pays] to one when it wins. The numbers run in rows of three across
 * the layout, 1 2 3, then 4 5 6, and so on to 34 35 36, so the columns are 1 4 7 ... 34, 2 5 8 ... 35 and
 * 3 6 9 ... 36. Each kind is placed at the number, row, column or dozen it counts from; the even-money bets at 0.
 */
enum class BetKind(
    val pays: Int,
) {
    /** One number, 0 to 36. */
    STRAIGHT(PAYS_STRAIGHT) {
        override fun numbersAt(at: Int) = if (at in 0..Wheel.HIGHEST) setOf(at) else emptySet()
    },

    /** A number and the one beside it in its row. */
    SPLIT_ACROSS(PAYS_SPLIT) {
        override fun numbersAt(at: Int) = if (at % ROW != 0) setOf(at, at + 1) else emptySet()
    },

    /** A number and the one below it in its column. */
    SPLIT_DOWN(PAYS_SPLIT) {
        override fun numbersAt(at: Int) = setOf(at, at + ROW)
    },

    /** A row of three, counted from 0 for 1 2 3. */
    STREET(PAYS_STREET) {
        override fun numbersAt(at: Int) = rows(at, 1)
    },

    /** A number, the one beside it, and the two below them. */
    CORNER(PAYS_CORNER) {
        override fun numbersAt(at: Int) = if (at % ROW != 0) setOf(at, at + 1, at + ROW, at + ROW + 1) else emptySet()
    },

    /** Two rows of three, counted from the upper one. */
    SIX_LINE(PAYS_SIX_LINE) {
        override fun numbersAt(at: Int) = rows(at, 2)
    },

    /** A column of twelve, counted from 0 for the one starting at 1. */
    COLUMN(2) {
        override fun numbersAt(at: Int) = numbersWhere { (it - 1) % ROW == at }
    },

    /** A dozen, counted from 0 for 1 to 12. */
    DOZEN(2) {
        override fun numbersAt(at: Int) = numbersWhere { (it - 1) / DOZEN_SIZE == at }
    },
    RED(1) {
        override fun numbersAt(at: Int) = evenMoney(at) { Wheel.colorOf(it) == PocketColor.RED }
    },
    BLACK(1) {
        override fun numbersAt(at: Int) = evenMoney(at) { Wheel.colorOf(it) == PocketColor.BLACK }
    },
    ODD(1) {
        override fun numbersAt(at: Int) = evenMoney(at) { it % 2 == 1 }
    },
    EVEN(1) {
        override fun numbersAt(at: Int) = evenMoney(at) { it % 2 == 0 }
    },

    /** 1 to 18. */
    LOW(1) {
        override fun numbersAt(at: Int) = evenMoney(at) { it <= HALF }
    },

    /** 19 to 36. */
    HIGH(1) {
        override fun numbersAt(at: Int) = evenMoney(at) { it > HALF }
    },
    ;

    /** The numbers this kind of bet placed [at] wins on. None, or some off the wheel, if there is no such place. */
    internal abstract fun numbersAt(at: Int): Set<Int>
}

/**
 * One bet on the layout: its [kind], placed [at]. Throws [IllegalArgumentException] if the layout has no such
 * place.
 */
@Serializable
data class Bet(
    val kind: BetKind,
    val at: Int = 0,
) {
    /** The numbers it wins on. */
    @Transient
    val numbers: Set<Int> = kind.numbersAt(at)

    init {
        val onTheWheel = numbers.isNotEmpty() && numbers.all { it in 0..Wheel.HIGHEST }
        // Only a bet on the zero alone covers it.
        require(onTheWheel && (kind == BetKind.STRAIGHT || 0 !in numbers)) { "There is no $kind bet at $at" }
    }

    val pays: Int get() = kind.pays

    fun wins(pocket: Int): Boolean = pocket in numbers
}

private const val PAYS_STRAIGHT = 35
private const val PAYS_SPLIT = 17
private const val PAYS_STREET = 11
private const val PAYS_CORNER = 8
private const val PAYS_SIX_LINE = 5
private const val ROW = 3
private const val ROWS = 12
private const val DOZEN_SIZE = 12
private const val HALF = 18

// The numbers 1 to 36 that [where] picks.
private fun numbersWhere(where: (Int) -> Boolean): Set<Int> = (1..Wheel.HIGHEST).filter(where).toSet()

// An even-money bet, which is placed at nothing, so only at 0.
private fun evenMoney(
    at: Int,
    where: (Int) -> Boolean,
): Set<Int> = if (at == 0) numbersWhere(where) else emptySet()

// [count] rows of three from row [first], or nothing if they run off the layout.
private fun rows(
    first: Int,
    count: Int,
): Set<Int> = if (first in 0..ROWS - count) (first * ROW + 1..(first + count) * ROW).toSet() else emptySet()

/** How the last spin went: the [pocket] it landed in, the [staked] bronzes riding on it, and the [won] paid back. */
@Serializable
data class Spin(
    val pocket: Int,
    val staked: Long,
    val won: Long,
)

/**
 * The roulette table: the [bets] down on the layout, in bronzes, the bets of the last spin to put down again, and
 * how that spin went. [spins] counts every spin there has been.
 */
@Serializable
data class Roulette(
    val bets: Map<Bet, Long> = emptyMap(),
    val lastBets: Map<Bet, Long> = emptyMap(),
    val last: Spin? = null,
    val spins: Long = 0,
) {
    val staked: Long get() = bets.values.sum()
}

/**
 * This den with [amount] bronzes from the purse put on [bet], on top of anything already on it. Throws
 * [IllegalArgumentException] if the purse does not hold that much, or the amount is below [minimum].
 */
fun Den.placeBet(
    bet: Bet,
    amount: Long,
    minimum: Long = 1,
): Den {
    require(amount >= minimum) { "Bets start at $minimum, not $amount" }
    return copy(purse = purse - amount, roulette = roulette.copy(bets = roulette.bets + (bet to stakeOn(bet) + amount)))
}

/** This den with every bet taken back off the layout into the purse. */
fun Den.clearBets(): Den = copy(purse = purse + roulette.staked, roulette = roulette.copy(bets = emptyMap()))

/** True if the last spin's bets can go down again: the layout is bare and the purse holds enough. */
val Den.canRebet: Boolean get() =
    roulette.bets.isEmpty() && roulette.lastBets.isNotEmpty() &&
        purse.holds(roulette.lastBets.values.sum())

/** This den with the last spin's bets put down again. Throws [IllegalArgumentException] unless it [canRebet]. */
fun Den.rebet(): Den {
    require(canRebet) { "The last bets cannot go down again" }
    val stake = roulette.lastBets.values.sum()
    return copy(purse = purse - stake, roulette = roulette.copy(bets = roulette.lastBets))
}

/**
 * This den after spin number [number]: the ball lands where [Wheel.landing] says, every winning bet is paid its
 * odds and handed back with them, and the layout is cleared. Throws [IllegalArgumentException] with no bets down.
 */
fun Den.spin(number: Long): Den {
    require(roulette.bets.isNotEmpty()) { "Nothing is bet" }
    val pocket = Wheel.landing(number)
    val won =
        roulette.bets.entries.filter { (bet, _) -> bet.wins(pocket) }.sumOf { (bet, stake) ->
            stake *
                (bet.pays + 1)
        }
    return copy(
        purse = purse + won,
        roulette =
            Roulette(
                lastBets = roulette.bets,
                last = Spin(pocket, roulette.staked, won),
                spins = roulette.spins + 1,
            ),
    )
}

/** The bronzes riding on [bet] now. */
fun Den.stakeOn(bet: Bet): Long = roulette.bets[bet] ?: 0
