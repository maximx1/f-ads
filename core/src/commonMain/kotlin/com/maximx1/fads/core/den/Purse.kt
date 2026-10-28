package com.maximx1.fads.core.den

import kotlinx.serialization.Serializable

/**
 * The player's money in the den, worth [total] bronzes. It is counted out in the fewest coins: golds of [GOLD]
 * bronzes, silvers of [SILVER], and bronzes for the rest.
 */
@Serializable
data class Purse(
    val total: Long = 0,
) {
    init {
        require(total >= 0) { "A purse cannot hold less than nothing, got $total" }
    }

    val golds: Long get() = total / GOLD

    val silvers: Long get() = total % GOLD / SILVER

    /** The bronze coins left once the golds and silvers are counted out. */
    val bronzes: Long get() = total % GOLD % SILVER

    operator fun plus(amount: Long): Purse = Purse(total + amount)

    /** This purse with [amount] taken out. Throws [IllegalArgumentException] if it does not hold that much. */
    operator fun minus(amount: Long): Purse {
        require(amount <= total) { "Cannot take $amount from a purse of $total" }
        return Purse(total - amount)
    }

    fun holds(amount: Long): Boolean = amount <= total

    companion object {
        const val SILVER = 10L
        const val GOLD = 25L
    }
}
