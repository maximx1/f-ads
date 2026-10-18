package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.rules.FreeCellRules
import com.maximx1.fads.core.rules.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class GameTypeTest {
    @Test
    fun everyTypeIsFoundByItsOwnKeyAndNothingElse() {
        for (type in GameType.all) assertEquals(type, GameType.fromKey(type.key))
        assertNull(GameType.fromKey("canfield"))
    }

    @Test
    fun aTypeStartsAndReloadsItsOwnGames() {
        val game = GameType.FreeCell.start(617)
        assertSame(GameType.FreeCell, game.type)
        assertEquals(Deal.fromNumber(617), game.state)
        val played = Game.start(617).play(Move.ColumnToFreeCell(0, 0))
        assertEquals(played.record, GameType.FreeCell.load(played.toJson())?.record)
        assertNull(GameType.FreeCell.load("not a game"))
    }

    @Test
    fun aMoveFromAnotherGameIsIgnored() {
        val game = Game.start(FreeCellRules, 1)
        assertSame(game, game.playIfLegal("not a move"))
        assertSame(game, game.playIfLegal(Move.ColumnToFoundation(0)))
        assertEquals(1, game.playIfLegal(Move.ColumnToFreeCell(0, 0)).moveCount)
    }
}
