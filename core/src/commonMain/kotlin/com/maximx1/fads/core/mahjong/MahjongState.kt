package com.maximx1.fads.core.mahjong

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

/** One mahjong action. Tiles are named by their ids. */
@Serializable
sealed interface MahjongMove {
    /** Take tiles [first] and [second] off the table: both free, and a match. */
    @Serializable
    @SerialName("match")
    data class Match(
        val first: Int,
        val second: Int,
    ) : MahjongMove
}

/**
 * One immutable position of a mahjong game: the [tiles] still on the table, by id. It is won once none are left.
 */
data class MahjongState(
    val tiles: List<Tile>,
) {
    val isWon: Boolean get() = tiles.isEmpty()

    /** The ids of the tiles that are free: nothing on top of them, and nothing at their left or at their right. */
    val free: Set<Int> by lazy {
        val spots = tiles.map { it.spot }
        tiles.filter { spots.isFree(it.spot) }.mapTo(HashSet()) { it.id }
    }

    private val byId: Map<Int, Tile> by lazy { tiles.associateBy { it.id } }

    /** The tile with [id], if it is still on the table. */
    fun tile(id: Int): Tile? = byId[id]
}

/**
 * True if, among the tiles at these places, the one at [spot] is free: no tile above overlaps it, and it has no
 * neighbour on its own layer at its left or none at its right.
 */
internal fun Collection<Spot>.isFree(spot: Spot): Boolean {
    fun overlaps(other: Spot) = abs(other.row - spot.row) < TILE
    val covered = any { it.layer > spot.layer && abs(it.col - spot.col) < TILE && overlaps(it) }
    val left = any { it.layer == spot.layer && it.col == spot.col - TILE && overlaps(it) }
    val right = any { it.layer == spot.layer && it.col == spot.col + TILE && overlaps(it) }
    return !covered && !(left && right)
}

// A tile is two half steps wide and two high.
private const val TILE = 2
