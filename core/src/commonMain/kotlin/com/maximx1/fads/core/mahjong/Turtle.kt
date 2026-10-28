package com.maximx1.fads.core.mahjong

/**
 * The turtle, the classic layout of 144 tiles. The bottom layer is twelve tiles across at its top, bottom and middle
 * and narrower between, with one tile out at the left (the head) and two at the right (the tail), set half a tile
 * down between the middle rows. On it sit squares of six, four and two tiles a side, and one tile on top of all.
 */
object Turtle {
    /** The bottom layer's rows but for the head and tail: each row, and the columns it runs across. */
    private val bottomRows =
        listOf(
            0 to 2..24,
            2 to 6..20,
            4 to 4..22,
            6 to 2..24,
            8 to 2..24,
            10 to 4..22,
            12 to 6..20,
            14 to 2..24,
        )

    /** Every place a tile goes, bottom layer first. A tile's id is the index of its place here. */
    val spots: List<Spot> =
        buildList {
            for ((row, cols) in bottomRows) {
                for (col in cols step 2) add(Spot(col, row, 0))
            }
            add(Spot(0, 7, 0))
            add(Spot(26, 7, 0))
            add(Spot(28, 7, 0))
            square(layer = 1, cols = 8..18, rows = 2..12)
            square(layer = 2, cols = 10..16, rows = 4..10)
            square(layer = 3, cols = 12..14, rows = 6..8)
            add(Spot(13, 7, 4))
        }

    private fun MutableList<Spot>.square(
        layer: Int,
        cols: IntRange,
        rows: IntRange,
    ) {
        for (row in rows step 2) {
            for (col in cols step 2) add(Spot(col, row, layer))
        }
    }
}
