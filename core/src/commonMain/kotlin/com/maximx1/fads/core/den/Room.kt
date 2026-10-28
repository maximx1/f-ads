package com.maximx1.fads.core.den

/**
 * The rooms of the den, each with tables of its own. A poker hand there is played for antes or a small blind of
 * [stake] bronzes, every bet is at least [leastBet], and the player needs [entry] bronzes in their purse to walk in.
 */
enum class Room(
    val stake: Long,
    val leastBet: Long,
    val entry: Long,
) {
    /** The den's floor, open to everyone, where a bronze will do. */
    FLOOR(stake = 1, leastBet = 1, entry = 0),

    /** The high rollers' room, for big purses and big bets. */
    HIGH_ROLLERS(stake = HIGH_STAKE, leastBet = HIGH_STAKE * 2, entry = HIGH_ENTRY),
}

/** True if the player's purse is big enough to walk into [room]. */
fun Den.canEnter(room: Room): Boolean = purse.holds(room.entry)

/** The roulette table in [room]. */
fun Den.roulette(room: Room): Roulette = if (room == Room.FLOOR) roulette else highRoulette

internal fun Den.withRoulette(
    room: Room,
    table: Roulette,
): Den = if (room == Room.FLOOR) copy(roulette = table) else copy(highRoulette = table)

private const val HIGH_STAKE = 500L
private const val HIGH_ENTRY = 25_000L
