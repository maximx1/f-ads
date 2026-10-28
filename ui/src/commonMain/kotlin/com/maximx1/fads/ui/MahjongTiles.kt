package com.maximx1.fads.ui

import com.maximx1.fads.core.mahjong.MahjongState
import com.maximx1.fads.core.mahjong.Tile
import com.maximx1.fads.core.mahjong.legalMoves

/** What the mahjong table needs that is not drawing: test tags, and which tiles to dim. */
object MahjongTiles {
    /** A tile's test tag: its face and its id, since there are copies of every face. */
    fun tagOf(tile: Tile) = "${tile.face.code}#${tile.id}"

    /** The tiles that are in no pair that could be taken off right now. */
    fun stuck(state: MahjongState): Set<Int> {
        val loose = state.legalMoves().flatMapTo(HashSet()) { listOf(it.first, it.second) }
        return state.tiles.map { it.id }.filterNotTo(HashSet()) { it in loose }
    }
}
