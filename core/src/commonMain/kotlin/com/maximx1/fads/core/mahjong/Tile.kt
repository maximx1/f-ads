package com.maximx1.fads.core.mahjong

/**
 * The kinds of mahjong tile, each numbered from 1 to [ranks], with [copies] of every one in a set of 144. Winds
 * are east, south, west and north; dragons red, green and white; flowers plum, orchid, chrysanthemum and bamboo;
 * seasons spring, summer, autumn and winter.
 */
enum class TileSuit(
    val ranks: Int,
    val copies: Int,
) {
    DOTS(NUMBERED, COPIES),
    BAMBOO(NUMBERED, COPIES),
    CHARACTERS(NUMBERED, COPIES),
    WINDS(FOUR_KINDS, COPIES),
    DRAGONS(DRAGON_KINDS, COPIES),
    FLOWERS(FOUR_KINDS, 1),
    SEASONS(FOUR_KINDS, 1),
    ;

    /** True for flowers and seasons: there is one of each, and any of them matches any other of its suit. */
    val isBonus: Boolean get() = copies == 1
}

/** What is on a tile's face. */
data class TileFace(
    val suit: TileSuit,
    val rank: Int,
) {
    init {
        require(rank in 1..suit.ranks) { "$suit runs from 1 to ${suit.ranks}, not $rank" }
    }

    /** True if this tile and [other] can be taken off as a pair: the same face, or two flowers or two seasons. */
    fun matches(other: TileFace): Boolean = suit == other.suit && (rank == other.rank || suit.isBonus)

    /** A short name for the face, such as `dots5` or `winds1`. */
    val code: String get() = "${suit.name.lowercase()}$rank"

    companion object {
        /** The 144 tiles of a set, in a fixed order: every copy of each face together, suit by suit. */
        val set: List<TileFace> =
            TileSuit.entries.flatMap { suit ->
                (1..suit.ranks).flatMap { rank -> List(suit.copies) { TileFace(suit, rank) } }
            }
    }
}

/**
 * A place for a tile: [col] counts half a tile's width across, [row] half a tile's height down, and [layer] how
 * many tiles it sits on, 0 at the bottom. Half steps let a tile sit across the join of the two below it.
 */
data class Spot(
    val col: Int,
    val row: Int,
    val layer: Int,
)

/** A tile on the table. [id] is the place of its [spot] in the layout, so it tells it apart from its copies. */
data class Tile(
    val id: Int,
    val face: TileFace,
    val spot: Spot,
)

private const val NUMBERED = 9
private const val FOUR_KINDS = 4
private const val DRAGON_KINDS = 3
private const val COPIES = 4
