package com.maximx1.fads.core.mahjong

import com.maximx1.fads.core.deal.SplitMix64
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Scoring
import com.maximx1.fads.core.engine.replay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MahjongTest {
    private val dot = TileFace(TileSuit.DOTS, 5)
    private val wind = TileFace(TileSuit.WINDS, 1)

    /** A position of just these tiles, numbered from 0 in the order given. */
    private fun position(vararg tiles: Pair<TileFace, Spot>) =
        MahjongState(tiles.mapIndexed { id, (face, spot) -> Tile(id, face, spot) })

    @Test
    fun aSetHas144TilesAndOnlyFlowersAndSeasonsMatchOtherFaces() {
        assertEquals(144, TileFace.set.size)
        assertEquals(42, TileFace.set.toSet().size)
        assertTrue(dot.matches(TileFace(TileSuit.DOTS, 5)))
        assertFalse(dot.matches(TileFace(TileSuit.DOTS, 6)))
        assertFalse(dot.matches(TileFace(TileSuit.BAMBOO, 5)))
        assertTrue(TileFace(TileSuit.FLOWERS, 1).matches(TileFace(TileSuit.FLOWERS, 4)))
        assertTrue(TileFace(TileSuit.SEASONS, 2).matches(TileFace(TileSuit.SEASONS, 3)))
        assertFalse(TileFace(TileSuit.FLOWERS, 1).matches(TileFace(TileSuit.SEASONS, 1)))
        assertEquals("dots5", dot.code)
        assertFailsWith<IllegalArgumentException> { TileFace(TileSuit.DRAGONS, 4) }
        assertEquals(72, facePairs().size)
        assertTrue(facePairs().all { (first, second) -> first.matches(second) })
    }

    @Test
    fun theTurtleHasFiveLayersOf144TilesInAll() {
        val spots = Turtle.spots
        assertEquals(144, spots.toSet().size)
        assertEquals(listOf(87, 36, 16, 4, 1), (0..4).map { layer -> spots.count { it.layer == layer } })
    }

    @Test
    fun aDealLaysEveryTileOfTheSetOnTheTurtleTheSameWayEveryTime() {
        val state = mahjongDeal(617)
        assertEquals(TileFace.set.sortedBy { it.code }, state.tiles.map { it.face }.sortedBy { it.code })
        assertEquals(Turtle.spots, state.tiles.map { it.spot })
        assertEquals(Turtle.spots.indices.toList(), state.tiles.map { it.id })
        assertEquals(state, MahjongRules.deal(617))
        assertNotEquals(state, mahjongDeal(618))
        // Deal 1 locked in place, so a change to the shuffle or the way places are emptied shows up here.
        assertEquals(
            DEAL_ONE_START,
            mahjongDeal(1)
                .tiles
                .take(DEAL_ONE_START.size)
                .map { it.face.code },
        )
    }

    @Test
    fun everyDealCanBeWon() {
        for (number in listOf(0L, 1L, 2L, 99L, 123_456_789L, Long.MAX_VALUE)) {
            val (state, solution) = solvedDeal(number)
            assertEquals(mahjongDeal(number), state)
            val end = solution.fold(state) { position, move -> position.play(move) }
            assertTrue(end.isWon, "deal $number")
        }
    }

    @Test
    fun theTurtleStartsWith35FreeTiles() {
        val state = mahjongDeal(1)
        assertEquals(35, state.free.size)
        val top = state.tiles.last()
        assertEquals(4, top.spot.layer)
        assertTrue(top.id in state.free)
        // The head, out on its own at the left, and the far tile of the tail are free; the four under the top are not.
        assertTrue(state.tiles.first { it.spot == Spot(0, 7, 0) }.id in state.free)
        assertTrue(state.tiles.first { it.spot == Spot(28, 7, 0) }.id in state.free)
        assertTrue(state.tiles.filter { it.spot.layer == 3 }.none { it.id in state.free })
    }

    @Test
    fun aTileIsFreeWithNothingOnItAndOneSideOpen() {
        // The middle one is hemmed in at both sides, even by a neighbour set half a row lower.
        val row = position(dot to Spot(0, 0, 0), dot to Spot(2, 0, 0), wind to Spot(4, 1, 0))
        assertEquals(setOf(0, 2), row.free)
        assertFalse(row.canMove(MahjongMove.Match(0, 1)))
        assertEquals(emptyList(), row.legalMoves())
        assertNull(row.hint())

        // A tile half on top of another covers it.
        val covered = position(dot to Spot(0, 0, 0), dot to Spot(4, 0, 0), wind to Spot(1, 0, 1))
        assertEquals(setOf(1, 2), covered.free)
        assertFalse(covered.canMove(MahjongMove.Match(0, 1)))

        // A whole row lower is not beside it.
        val apart = position(dot to Spot(0, 0, 0), dot to Spot(2, 0, 0), wind to Spot(4, 2, 0))
        assertEquals(setOf(0, 1, 2), apart.free)
    }

    @Test
    fun onlyTwoFreeTilesThatMatchCanBeTakenOff() {
        val state =
            position(
                dot to Spot(0, 0, 0),
                dot to Spot(2, 0, 0),
                dot to Spot(4, 0, 0),
                wind to Spot(10, 0, 0),
            )
        assertTrue(state.canMove(MahjongMove.Match(0, 2)))
        assertTrue(state.canMove(MahjongMove.Match(2, 0)))
        assertFalse(state.canMove(MahjongMove.Match(0, 1)), "the middle one is not free")
        assertFalse(state.canMove(MahjongMove.Match(0, 0)), "a tile does not match itself")
        assertFalse(state.canMove(MahjongMove.Match(0, 3)), "no match")
        assertFalse(state.canMove(MahjongMove.Match(0, 9)), "no such tile")
        assertEquals(listOf(MahjongMove.Match(0, 2)), state.legalMoves())
        assertFailsWith<IllegalArgumentException> { state.play(MahjongMove.Match(0, 3)) }

        val after = state.play(MahjongMove.Match(0, 2))
        assertEquals(listOf(1, 3), after.tiles.map { it.id })
        assertNull(after.tile(0))
    }

    @Test
    fun aHintFreesTheMostTilesItCan() {
        val state =
            position(
                // Taking this pair off frees the two under it.
                wind to Spot(1, 0, 1),
                wind to Spot(5, 0, 1),
                dot to Spot(0, 0, 0),
                dot to Spot(2, 0, 0),
                dot to Spot(4, 0, 0),
                dot to Spot(6, 0, 0),
                // A pair on its own that frees nothing.
                TileFace(TileSuit.DRAGONS, 1) to Spot(20, 0, 0),
                TileFace(TileSuit.DRAGONS, 1) to Spot(24, 0, 0),
            )
        assertEquals(MahjongMove.Match(0, 1), state.hint())
        assertEquals(MahjongMove.Match(0, 1), MahjongRules.hint(state, null))
        assertNotNull(mahjongDeal(1).hint())
    }

    @Test
    fun itFinishesItselfOnceEveryTileLeftIsFree() {
        val start = mahjongDeal(5)
        assertFalse(start.canAutoComplete)
        assertNull(start.nextAutoMove())

        val (_, solution) = solvedDeal(5)
        var state = solution.dropLast(10).fold(start) { position, move -> position.play(move) }
        while (!state.canAutoComplete) state = state.play(solution[(144 - state.tiles.size) / 2])
        while (!state.isWon) state = state.play(assertNotNull(MahjongRules.nextAutoMove(state)))
        assertFalse(state.canAutoComplete)
    }

    @Test
    fun randomPlayNeverLosesOrDuplicatesATile() {
        for (number in 0L until 40L) {
            val random = SplitMix64(number)
            var state = mahjongDeal(number)
            while (true) {
                val moves = state.legalMoves()
                if (moves.isEmpty()) break
                val before = state.tiles.size
                state = state.play(moves[random.nextBelow(moves.size)])
                assertEquals(before - 2, state.tiles.size)
                assertEquals(
                    state.tiles.size,
                    state.tiles
                        .map { it.id }
                        .toSet()
                        .size,
                )
            }
            // However the game went, the tiles left come in matching pairs.
            assertTrue(
                state.tiles
                    .groupingBy { it.face.kind }
                    .eachCount()
                    .values
                    .all { it % 2 == 0 },
            )
        }
    }

    /** Faces that match one another share a kind: the face itself, or just the suit for flowers and seasons. */
    private val TileFace.kind: Any get() = if (suit.isBonus) suit else this

    @Test
    fun itScoresTenAPairLessUndosAndHintsAndATimeBonusOnAWin() {
        val (_, solution) = solvedDeal(3)
        var game = GameType.Mahjong.start(3)
        assertEquals(0, game.score)
        game = game.playIfLegal(solution[0]).playIfLegal(solution[1])
        assertEquals(2 * MahjongRules.PER_PAIR, game.score)
        game = game.takeHint()
        assertEquals(2 * MahjongRules.PER_PAIR - Scoring.HINT_COST, game.score)
        game = game.undo()
        assertEquals(0, game.score, "never below zero")

        var won = Game.start(MahjongRules, 3)
        for (move in solution) won = won.play(move)
        won = won.withElapsed(600_000)
        assertTrue(won.isWon)
        assertEquals(72 * MahjongRules.PER_PAIR + Scoring.timeBonus(600_000), won.score)
    }

    @Test
    fun aGameIsSavedAndLoadedAsMahjong() {
        val (_, solution) = solvedDeal(9)
        val game = GameType.Mahjong.start(9).playIfLegal(solution.first())
        val loaded = assertNotNull(GameType.Mahjong.load(game.toJson()))
        assertEquals(game.record, loaded.record)
        assertEquals(game.state, MahjongRules.replay(loaded.record))
        assertTrue(game.toJson().contains("\"match\""))
        assertEquals(GameType.Mahjong, GameType.fromKey("mahjong"))
        assertNull(MahjongRules.moveOrNull("not a move"))
    }

    private companion object {
        val DEAL_ONE_START =
            listOf(
                "winds1",
                "bamboo3",
                "bamboo4",
                "dots8",
                "dragons1",
                "bamboo2",
                "characters8",
                "characters3",
                "characters4",
                "characters6",
                "bamboo6",
                "dots3",
            )
    }
}
