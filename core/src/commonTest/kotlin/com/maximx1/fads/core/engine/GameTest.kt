package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.rules.Move
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GameTest {
    // Deal 1 opens with 3D on column 1 and 4S on column 5.
    private val first = Move.ColumnToColumn(0, 4)
    private val second = Move.ColumnToFreeCell(0, 0)

    @Test
    fun startsAtTheDealWithNoMoves() {
        val game = Game.start(1)
        assertEquals(1, game.dealNumber)
        assertEquals(0, game.moveCount)
        assertFalse(game.canUndo)
        assertEquals(Deal.fromNumber(1), game.state)
    }

    @Test
    fun playAddsToTheRecordAndLeavesTheOldGameAlone() {
        val start = Game.start(1)
        val game = start.play(first).play(second)
        assertEquals(listOf(first, second), game.record.moves)
        assertEquals(2, game.moveCount)
        assertEquals(game.record.replay(), game.state)
        assertEquals(0, start.moveCount)
    }

    @Test
    fun illegalMovesThrowAndRecordNothing() {
        val game = Game.start(1)
        assertFailsWith<IllegalArgumentException> { game.play(Move.ColumnToFoundation(0)) }
        assertEquals(0, game.moveCount)
    }

    @Test
    fun undoStepsBackOneMoveAtATime() {
        val one = Game.start(1).play(first)
        val two = one.play(second)
        assertTrue(two.canUndo)
        assertEquals(one.state, two.undo().state)
        assertEquals(one.record.moves, two.undo().record.moves)
        assertEquals(Deal.fromNumber(1), two.undo().undo().state)
    }

    @Test
    fun everyMovePlayedAndEveryUndoCountsAsAMoveMade() {
        val start = Game.start(1)
        assertEquals(0, start.movesMade)
        val played = start.play(first).play(second)
        assertEquals(2, played.movesMade)

        val undone = played.undo()
        assertEquals(3, undone.movesMade)
        assertEquals(1, undone.moveCount)
        assertEquals(4, undone.play(second).movesMade)
        assertEquals(0, start.undo().movesMade)
    }

    @Test
    fun aRunMovedTogetherIsOneMove() {
        // Four moves on deal 1 leave 9H 8S on top of column 8 and a bare 10C on column 1.
        val game =
            Game
                .start(1)
                .play(first)
                .play(Move.ColumnToColumn(0, 7))
                .play(Move.ColumnToFoundation(0))
                .play(Move.ColumnToColumn(6, 7))
        assertEquals(4, game.movesMade)
        assertEquals(5, game.play(Move.ColumnToColumn(7, 0, count = 2)).movesMade)
    }

    @Test
    fun restartPutsTheMoveCountBackAtZero() {
        assertEquals(
            0,
            Game
                .start(1)
                .play(first)
                .undo()
                .restart()
                .movesMade,
        )
    }

    @Test
    fun theMoveCountIsSavedAndAGameSavedWithoutOneCountsItsMoves() {
        val game =
            Game
                .start(1)
                .play(first)
                .undo()
                .play(first)
        assertEquals(3, Game.fromJson(game.toJson())?.movesMade)
        assertEquals(1, Game.fromJson("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}""")?.movesMade)
        assertNull(Game.fromJson("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}],"movesMade":0}"""))
    }

    @Test
    fun theHintIsALegalMoveThatDoesNotJustUndoTheLastOne() {
        val start = Game.start(1)
        val hint = assertNotNull(start.hint())
        val played = start.play(hint)
        val next = assertNotNull(played.hint())
        assertTrue(played.state != start.state)
        assertTrue(played.play(next).state != start.state)
    }

    @Test
    fun undoAtTheStartDoesNothing() {
        val game = Game.start(1)
        assertSame(game, game.undo())
    }

    @Test
    fun restartKeepsTheDealAndDropsTheMoves() {
        val game = Game.start(1).play(first).restart()
        assertEquals(1, game.dealNumber)
        assertEquals(0, game.moveCount)
        assertEquals(Deal.fromNumber(1), game.state)
    }

    @Test
    fun theClockStartsAtZeroAndKeepsItsTimeThroughMovesAndUndo() {
        val start = Game.start(1)
        assertEquals(0, start.elapsedMillis)
        val timed = start.play(first).withElapsed(4_200)
        assertEquals(4_200, timed.elapsedMillis)
        assertEquals(timed.state, timed.withElapsed(9_000).state)
        assertEquals(4_200, timed.play(second).elapsedMillis)
        assertEquals(4_200, timed.undo().elapsedMillis)
        assertEquals(0, start.play(first).elapsedMillis)
    }

    @Test
    fun restartPutsTheClockBackAtZero() {
        assertEquals(
            0,
            Game
                .start(1)
                .play(first)
                .withElapsed(4_200)
                .restart()
                .elapsedMillis,
        )
    }

    @Test
    fun theClockCannotGoBelowZero() {
        assertFailsWith<IllegalArgumentException> { Game.start(1).withElapsed(-1) }
    }

    @Test
    fun aGameIsInPlayFromTheFirstMoveUntilItIsWon() {
        val start = Game.start(1)
        assertFalse(start.inPlay)
        assertTrue(start.play(first).inPlay)
        assertTrue(
            start
                .play(first)
                .withElapsed(4_200)
                .undo()
                .inPlay,
            "taking back the only move",
        )
        assertFalse(
            start
                .play(first)
                .withElapsed(4_200)
                .restart()
                .inPlay,
        )
    }

    @Test
    fun savedGamesComeBackTheSame() {
        val game =
            Game
                .start(1)
                .play(first)
                .play(second)
                .withElapsed(61_500)
        val restored = assertNotNull(Game.fromJson(game.toJson()))
        assertEquals(game.record, restored.record)
        assertEquals(game.state, restored.state)
        assertEquals(61_500, restored.elapsedMillis)
    }

    @Test
    fun aGameSavedBeforeThereWasAClockLoadsWithNoTimeOnIt() {
        val restored = assertNotNull(Game.fromJson("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}]}"""))
        assertEquals(0, restored.elapsedMillis)
        assertTrue(restored.inPlay)
    }

    @Test
    fun unknownFieldsInASavedGameAreIgnored() {
        val restored = assertNotNull(Game.fromJson("""{"dealNumber":5,"moves":[],"addedLater":true}"""))
        assertEquals(5, restored.dealNumber)
    }

    @Test
    fun unreadableOrImpossibleSavedGamesGiveNull() {
        assertNull(Game.fromJson(""))
        assertNull(Game.fromJson("not json"))
        assertNull(Game.fromJson("""{"moves":[]}"""))
        assertNull(Game.fromJson("""{"dealNumber":-1}"""))
        assertNull(Game.fromJson("""{"dealNumber":1,"elapsedMillis":-5}"""))
        assertNull(Game.fromJson("""{"dealNumber":1,"moves":[{"type":"ch","from":0}]}"""), "illegal first move")
        assertNull(Game.fromJson("""{"dealNumber":1,"moves":[{"type":"nope"}]}"""))
    }
}
