package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.rules.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryTest {
    private val won = Game.resume(GameRecord(dealNumber = 1, moves = dealOneWin)).withElapsed(250_000)
    private val givenUp = GameType.Klondike(3).start(617).withElapsed(9_000)
    private val history = History().with(Played.of(won, endedAt = 100)).with(Played.of(givenUp, endedAt = 200))

    @Test
    fun aPlayedGameKeepsWhatItWasAndHowItWent() {
        val played = Played.of(won, endedAt = 100)
        assertEquals(GameType.FreeCell, played.type)
        assertEquals(1, played.dealNumber)
        assertEquals(250_000, played.millis)
        assertEquals(won.movesMade, played.moves)
        assertEquals(won.score, played.score)
        assertTrue(played.won)
        assertEquals(won.record, played.load()?.record)
    }

    @Test
    fun theNewestGameComesFirst() {
        assertEquals(listOf(617L, 1L), history.games.map { it.dealNumber })
    }

    @Test
    fun searchMatchesDealNumbersTypesAndWins() {
        assertEquals(listOf(617L), history.search("61").map { it.dealNumber })
        assertEquals(listOf(617L, 1L), history.search("1").map { it.dealNumber })
        assertEquals(listOf(617L, 1L), history.search(" ").map { it.dealNumber })
        assertEquals(listOf(1L), history.search(type = { it == GameType.FreeCell }).map { it.dealNumber })
        assertEquals(listOf(1L), history.search(wonOnly = true).map { it.dealNumber })
        assertTrue(history.search("99").isEmpty())
    }

    @Test
    fun theHistoryIsSavedAndReadBack() {
        assertEquals(history, History.fromJson(history.toJson()))
        assertEquals(History(), History.fromJson("{}"))
        assertNull(History.fromJson("not a history"))
    }

    @Test
    fun aGamesPositionsRunFromTheDealToWhereItStands() {
        val game = Game.start(1).play(Move.ColumnToFreeCell(0, 0)).play(Move.ColumnToFreeCell(1, 1))
        val positions = game.positions()
        assertEquals(3, positions.size)
        assertEquals(Deal.fromNumber(1), positions.first())
        assertEquals(game.state, positions.last())
        assertFalse(positions[1] == positions[2])
    }
}
