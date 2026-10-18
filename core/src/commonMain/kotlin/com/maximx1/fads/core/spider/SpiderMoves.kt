package com.maximx1.fads.core.spider

import com.maximx1.fads.core.spider.SpiderState.Companion.PILE_COUNT

/** Every legal move in this position. */
fun SpiderState.legalMoves(): List<SpiderMove> =
    buildList {
        add(SpiderMove.DealRow)
        for (from in 0 until PILE_COUNT) {
            for (to in 0 until PILE_COUNT) {
                for (count in 1..showing(from).size) add(SpiderMove.PileToPile(from, to, count))
            }
        }
    }.filter { canMove(it) }

/** The cards [move] picks up, lowest first. Empty for dealing a row. The move must be legal here. */
fun SpiderState.cardsMovedBy(move: SpiderMove): List<Piece> =
    when (move) {
        SpiderMove.DealRow -> emptyList()
        is SpiderMove.PileToPile -> piles[move.from].takeLast(move.count)
    }
