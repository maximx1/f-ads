package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/** What can come up on a reel, each paying [pays] times the line bet for three in a line. */
enum class SlotSymbol(
    val pays: Int,
) {
    CHERRY(PAYS_CHERRY),
    LEMON(PAYS_LEMON),
    BELL(PAYS_BELL),
    BAR(PAYS_BAR),
    SEVEN(PAYS_SEVEN),
    DIAMOND(PAYS_DIAMOND),
}

/**
 * A slot machine of [REELS] reels, each the same [strip] of symbols, with [ROWS] of each showing in its window. A
 * bet covers each of the [lines] across the window: the three rows and the two diagonals. A line pays when it has
 * three of a symbol, or starts with two cherries.
 */
object SlotMachine {
    const val REELS = 3
    const val ROWS = 3

    /** Two cherries at the start of a line pay this many times the line bet. */
    const val TWO_CHERRIES = 2

    private val C = SlotSymbol.CHERRY
    private val L = SlotSymbol.LEMON
    private val B = SlotSymbol.BELL
    private val R = SlotSymbol.BAR

    /** The symbols round each reel, in order: plenty of cherries and lemons, a single seven and diamond. */
    val strip: List<SlotSymbol> =
        listOf(C, L, B, C, R, L, C, B, L, SlotSymbol.SEVEN, C, L, B, R, C, SlotSymbol.DIAMOND, L, B, C, R)

    /** The rows each line runs through, reel by reel, from 0 at the top of the window. */
    val lines: List<List<Int>> =
        listOf(listOf(1, 1, 1), listOf(0, 0, 0), listOf(2, 2, 2), listOf(0, 1, 2), listOf(2, 1, 0))

    /** Where each reel stops for spin number [number]: the place on the strip at the top of its window. */
    fun stops(number: Long): List<Int> {
        val random = SplitMix64(number)
        return List(REELS) { random.nextBelow(strip.size) }
    }

    /** The symbols showing, reel by reel and top to bottom, with the reels stopped at [stops]. */
    fun window(stops: List<Int>): List<List<SlotSymbol>> =
        stops.map { stop ->
            List(ROWS) {
                strip[
                    (stop + it) %
                        strip.size,
                ]
            }
        }

    /** What a line of these [symbols] pays, in line bets. */
    fun pays(symbols: List<SlotSymbol>): Int =
        when {
            symbols.all { it == symbols.first() } -> symbols.first().pays
            symbols.take(2).all { it == SlotSymbol.CHERRY } -> TWO_CHERRIES
            else -> 0
        }

    /** The lines that win with the reels stopped at [stops], by index into [lines]. */
    fun winningLines(stops: List<Int>): List<Int> {
        val window = window(stops)
        return lines.indices.filter { line -> pays(lines[line].mapIndexed { reel, row -> window[reel][row] }) > 0 }
    }

    /** What [lineBet] on each line wins with the reels stopped at [stops]. */
    fun payout(
        stops: List<Int>,
        lineBet: Long,
    ): Long {
        val window = window(stops)
        return lines.sumOf { line -> lineBet * pays(line.mapIndexed { reel, row -> window[reel][row] }) }
    }
}

/** How a pull went: where the reels [stops], the [lineBet] on each line, and what it [won]. */
@Serializable
data class SlotSpin(
    val stops: List<Int>,
    val lineBet: Long,
    val won: Long,
)

/** A slot machine in the den: how many times its lever has been pulled, and how the last went. */
@Serializable
data class Slots(
    val pulls: Long = 0,
    val last: SlotSpin? = null,
)

/** The slot machine in [room]. */
fun Den.slots(room: Room = Room.FLOOR): Slots = slotMachines[room] ?: Slots()

/**
 * This den after pull number [number] of [room]'s slot machine, [lineBet] bronzes on each line. Throws
 * [IllegalArgumentException] if the line bet is under the room's least bet or the purse does not hold a bet on every
 * line.
 */
fun Den.pullLever(
    lineBet: Long,
    number: Long,
    room: Room = Room.FLOOR,
): Den {
    require(lineBet >= room.leastBet) { "Bets start at ${room.leastBet}, not $lineBet" }
    val stops = SlotMachine.stops(number)
    val won = SlotMachine.payout(stops, lineBet)
    val pulled = Slots(slots(room).pulls + 1, SlotSpin(stops, lineBet, won))
    return copy(purse = purse - lineBet * SlotMachine.lines.size + won, slotMachines = slotMachines + (room to pulled))
}

private const val PAYS_CHERRY = 10
private const val PAYS_LEMON = 12
private const val PAYS_BELL = 20
private const val PAYS_BAR = 40
private const val PAYS_SEVEN = 200
private const val PAYS_DIAMOND = 500
