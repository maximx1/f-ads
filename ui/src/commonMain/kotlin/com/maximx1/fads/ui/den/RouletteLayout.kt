package com.maximx1.fads.ui.den

import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.BetKind
import kotlin.math.floor

/**
 * Where everything is on the roulette layout, in cells: it is [WIDTH] cells across and [HEIGHT] down. The
 * numbers fill the three columns on the right in their twelve rows, with the zero above them and the column bets
 * below. On the left, a strip of dozens, each four rows tall, and beside it the even-money bets, two rows each.
 *
 * A tap near the line between two numbers bets on both, near the corner of four on all four, and along the outer
 * edge of the numbers on their row, or on two rows where rows meet.
 */
object RouletteLayout {
    const val WIDTH = 5
    const val HEIGHT = 14

    /** The cell the numbers start at, across and down. */
    const val NUMBERS_LEFT = 2
    const val NUMBERS_TOP = 1
    const val ROWS = 12
    const val COLUMNS = 3

    /** The even-money bets, top to bottom. */
    val evenMoney = listOf(BetKind.LOW, BetKind.EVEN, BetKind.RED, BetKind.BLACK, BetKind.ODD, BetKind.HIGH)

    const val DOZEN_ROWS = 4
    const val EVEN_MONEY_ROWS = 2

    // How near a line, as a share of a cell, a tap has to be to bet on both sides of it.
    private const val EDGE = 0.25
    private const val HALF = 0.5

    /** The bet a tap at [x], [y] cells from the top left makes, or `null` if it is off every bet. */
    fun betAt(
        x: Double,
        y: Double,
    ): Bet? {
        val numbersBottom = NUMBERS_TOP + ROWS
        return when {
            x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT -> null
            x >= NUMBERS_LEFT && y < NUMBERS_TOP -> Bet(BetKind.STRAIGHT, 0)
            x >= NUMBERS_LEFT && y >= numbersBottom -> Bet(BetKind.COLUMN, floor(x - NUMBERS_LEFT).toInt())
            y < NUMBERS_TOP || y >= numbersBottom -> null
            x < 1 -> Bet(BetKind.DOZEN, floor((y - NUMBERS_TOP) / DOZEN_ROWS).toInt())
            x < NUMBERS_LEFT -> Bet(evenMoney[floor((y - NUMBERS_TOP) / EVEN_MONEY_ROWS).toInt()])
            else -> numberBetAt(x - NUMBERS_LEFT, y - NUMBERS_TOP)
        }
    }

    // A tap among the numbers, at [x] and [y] cells from the top left of 1.
    private fun numberBetAt(
        x: Double,
        y: Double,
    ): Bet {
        val column = floor(x).toInt()
        val row = floor(y).toInt()
        val across = x - column
        val down = y - row
        // The column on the left of a line the tap is near, if it is, and the row above one.
        val leftColumn = beforeLine(column, across, COLUMNS)
        val upperRow = beforeLine(row, down, ROWS)
        return when {
            column == 0 && across < EDGE && upperRow != null -> Bet(BetKind.SIX_LINE, upperRow)
            column == 0 && across < EDGE -> Bet(BetKind.STREET, row)
            leftColumn != null && upperRow != null -> Bet(BetKind.CORNER, number(upperRow, leftColumn))
            leftColumn != null -> Bet(BetKind.SPLIT_ACROSS, number(row, leftColumn))
            upperRow != null -> Bet(BetKind.SPLIT_DOWN, number(upperRow, column))
            else -> Bet(BetKind.STRAIGHT, number(row, column))
        }
    }

    // Of [count] cells in a line, the one before the line between two of them that a tap [into] cell [cell] is
    // near, or `null` if it is near none: near the outer edge of the first or last is not between two.
    private fun beforeLine(
        cell: Int,
        into: Double,
        count: Int,
    ): Int? =
        when {
            into > 1 - EDGE && cell < count - 1 -> cell
            into < EDGE && cell > 0 -> cell - 1
            else -> null
        }

    /** Where the chips on [bet] sit, in cells from the top left, as across and down. */
    fun centreOf(bet: Bet): Pair<Double, Double> {
        val at = bet.at
        val left = NUMBERS_LEFT.toDouble()
        val top = NUMBERS_TOP.toDouble()
        return when (bet.kind) {
            BetKind.STRAIGHT -> if (at == 0) left + COLUMNS * HALF to top * HALF else cellOf(at, HALF, HALF)
            BetKind.SPLIT_ACROSS -> cellOf(at, 1.0, HALF)
            BetKind.SPLIT_DOWN -> cellOf(at, HALF, 1.0)
            BetKind.CORNER -> cellOf(at, 1.0, 1.0)
            BetKind.STREET -> left to top + at + HALF
            BetKind.SIX_LINE -> left to top + at + 1
            BetKind.COLUMN -> left + at + HALF to top + ROWS + HALF
            BetKind.DOZEN -> HALF to top + at * DOZEN_ROWS + DOZEN_ROWS * HALF
            else -> 1 + HALF to top + evenMoney.indexOf(bet.kind) * EVEN_MONEY_ROWS + EVEN_MONEY_ROWS * HALF
        }
    }

    /** The number in [row] and [column] of the numbers, both counted from 0. */
    fun number(
        row: Int,
        column: Int,
    ) = row * COLUMNS + column + 1

    // A point [across] and [down] cells into the cell of [number].
    private fun cellOf(
        number: Int,
        across: Double,
        down: Double,
    ): Pair<Double, Double> =
        NUMBERS_LEFT + (number - 1) % COLUMNS + across to NUMBERS_TOP + (number - 1) / COLUMNS + down
}
