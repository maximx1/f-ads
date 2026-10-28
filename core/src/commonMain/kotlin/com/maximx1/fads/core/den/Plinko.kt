package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import kotlinx.serialization.Serializable

/**
 * A plinko board: a ball dropped in at the top bounces left or right off a peg in each of [ROWS] rows, and lands in
 * one of the buckets along the bottom, one more than there are rows. Each bucket pays the stake times its
 * multiplier, in tenths: the ones out at the edges, hardest to reach, the most, and the middle less than the stake.
 */
object PlinkoBoard {
    const val ROWS = 12

    /** What each bucket pays, left to right, in tenths of the stake. */
    val tenths: List<Int> = listOf(330, 110, 40, 20, 11, 6, 3, 6, 11, 20, 40, 110, 330)

    const val TENTHS = 10

    /** The way drop number [number] goes, row by row from the top: `true` for right. */
    fun path(number: Long): List<Boolean> {
        val random = SplitMix64(number)
        return List(ROWS) { random.nextBelow(2) == 1 }
    }

    /** What [stake] wins in [bucket]. */
    fun payout(
        stake: Long,
        bucket: Int,
    ): Long = stake * tenths[bucket] / TENTHS
}

/** How a drop went: the [path] the ball took, the [bucket] it landed in, the [staked] bronzes and the [won]. */
@Serializable
data class Drop(
    val path: List<Boolean>,
    val bucket: Int,
    val staked: Long,
    val won: Long,
)

/** A plinko table: how many balls have been dropped on it, and how the last went. */
@Serializable
data class Plinko(
    val drops: Long = 0,
    val last: Drop? = null,
)

/** The plinko table in [room]. */
fun Den.plinko(room: Room = Room.FLOOR): Plinko = plinkoTables[room] ?: Plinko()

/**
 * This den after drop number [number] at [room]'s plinko table, [stake] bronzes riding on it. Throws
 * [IllegalArgumentException] if the stake is under the room's least bet or more than the purse holds.
 */
fun Den.dropBall(
    stake: Long,
    number: Long,
    room: Room = Room.FLOOR,
): Den {
    require(stake >= room.leastBet) { "Bets start at ${room.leastBet}, not $stake" }
    val path = PlinkoBoard.path(number)
    val bucket = path.count { it }
    val won = PlinkoBoard.payout(stake, bucket)
    val dropped = Plinko(plinko(room).drops + 1, Drop(path, bucket, stake, won))
    return copy(purse = purse - stake + won, plinkoTables = plinkoTables + (room to dropped))
}
