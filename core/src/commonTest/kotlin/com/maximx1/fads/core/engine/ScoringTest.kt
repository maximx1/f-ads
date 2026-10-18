package com.maximx1.fads.core.engine

import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ScoringTest {
    private fun dealOneAfter(moves: Int) = Game.resume(GameRecord(dealNumber = 1, moves = dealOneWin.take(moves)))

    private fun Game<GameState, Move>.cardsUp() = Suit.entries.sumOf { state.foundationCount(it) }

    @Test
    fun aFreshGameScoresNothing() {
        assertEquals(0, Game.start(1).score)
    }

    @Test
    fun everyCardOnAFoundationScoresTen() {
        for (moves in 0 until dealOneWin.size) {
            val game = dealOneAfter(moves)
            assertEquals(game.cardsUp() * Scoring.PER_CARD, game.score, "after $moves moves")
        }
    }

    @Test
    fun undosAndHintsCostPointsButTheScoreNeverGoesBelowZero() {
        // Far enough in for a few cards to be up, so the costs show.
        val game = (0 until dealOneWin.size).map(::dealOneAfter).first { it.cardsUp() >= 3 }
        val start = game.score

        val undone = game.play(game.hint()!!).undo()
        assertEquals(1, undone.undos)
        assertEquals(start - Scoring.UNDO_COST, undone.score)
        assertEquals(start - Scoring.HINT_COST, game.takeHint().score)

        assertEquals(1, Game.start(1).takeHint().hintsTaken)
        assertEquals(0, Game.start(1).takeHint().score)
    }

    @Test
    fun aWinAddsTheTimeBonusAndAQuickerWinAddsMore() {
        val won = dealOneAfter(dealOneWin.size)
        assertEquals(520 + 2800, won.withElapsed(250_000).score)
        assertEquals(520 + 5833, won.withElapsed(120_000).score)
        // No game is won in under half a minute, so none earns more than one that took that long.
        assertEquals(won.withElapsed(30_000).score, won.withElapsed(0).score)
    }

    @Test
    fun takingAHintLeavesThePositionAndTheMovesAlone() {
        val game = dealOneAfter(5)
        val hinted = game.takeHint()
        assertSame(game.state, hinted.state)
        assertEquals(game.record.moves, hinted.record.moves)
        assertEquals(game.movesMade, hinted.movesMade)
    }

    @Test
    fun hintsAreSavedAndAGameSavedWithoutACountHasTakenNone() {
        val game = dealOneAfter(5).takeHint().takeHint()
        assertEquals(2, Game.fromJson(game.toJson())?.hintsTaken)
        assertEquals(0, Game.fromJson("""{"dealNumber":1}""")?.hintsTaken)
        assertEquals(null, Game.fromJson("""{"dealNumber":1,"hintsTaken":-1}"""))
    }

    @Test
    fun restartingForgetsTheHintsAndTheUndos() {
        val played = dealOneAfter(5).takeHint().undo()
        assertEquals(0, played.restart().hintsTaken)
        assertEquals(0, played.restart().undos)
    }
}
