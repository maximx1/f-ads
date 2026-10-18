package com.maximx1.fads.core.spider

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameRecord
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpiderTest {
    private var nextId = 1000

    /** Pieces from card codes, each with an id of its own. */
    private fun pieces(codes: String): List<Piece> =
        codes.split(' ').filter { it.isNotEmpty() }.map { Piece(nextId++, Card.fromCode(it)) }

    private fun position(
        vararg piles: String,
        hidden: List<Int> = emptyList(),
        stock: String = "",
        finished: Int = 0,
        suits: Int = 4,
    ) = SpiderState(
        piles = List(SpiderState.PILE_COUNT) { pieces(piles.getOrElse(it) { "" }) },
        hidden = List(SpiderState.PILE_COUNT) { hidden.getOrElse(it) { 0 } },
        stock = pieces(stock),
        finished = List(finished) { pieces("KS QS JS 10S 9S 8S 7S 6S 5S 4S 3S 2S AS") },
        suits = suits,
    )

    private val run = "KH QH JH 10H 9H 8H 7H 6H 5H 4H 3H 2H AH"

    private fun SpiderState.all(): List<Piece> = piles.flatten() + stock + finished.flatten()

    @Test
    fun eachSuitChoicePlaysWithItsOwn104Cards() {
        assertEquals(setOf(Suit.SPADES), spiderDeck(1).map { it.card.suit }.toSet())
        assertEquals(setOf(Suit.SPADES, Suit.HEARTS), spiderDeck(2).map { it.card.suit }.toSet())
        for (suits in SpiderState.SUIT_CHOICES) {
            val deck = spiderDeck(suits)
            assertEquals(104, deck.size)
            assertEquals((0 until 104).toList(), deck.map { it.id })
            // Every card appears the same number of times.
            assertEquals(
                1,
                deck
                    .groupingBy { it.card }
                    .eachCount()
                    .values
                    .toSet()
                    .size,
            )
        }
    }

    @Test
    fun aDealHasSixCardsOnTheFirstFourPilesFiveOnTheRestAndFiftyInTheStock() {
        val state = SpiderRules.forSuits(2).deal(617)
        assertEquals(listOf(6, 6, 6, 6, 5, 5, 5, 5, 5, 5), state.piles.map { it.size })
        assertEquals(listOf(5, 5, 5, 5, 4, 4, 4, 4, 4, 4), state.hidden)
        assertEquals(50, state.stock.size)
        assertEquals(spiderDeck(2).toSet(), state.all().toSet())
        assertEquals(state, SpiderRules.forSuits(2).deal(617))
        assertNotEquals(state, SpiderRules.forSuits(2).deal(618))
        // Deal 1 locked in place, so a change to the shuffle or the dealing shows up here.
        assertEquals(
            DEAL_ONE_TOPS,
            SpiderRules
                .forSuits(4)
                .deal(1)
                .piles
                .map { it.last().card.code },
        )
    }

    @Test
    fun aRunOfOneSuitGoesOntoAnyCardOneHigherOrAnEmptyPile() {
        val state = position("9S 8S 7H", "8C", "", "9D 8D")
        assertTrue(state.canMove(SpiderMove.PileToPile(0, 1)))
        assertFalse(state.canMove(SpiderMove.PileToPile(0, 1, count = 2)), "8S and 7H are not one suit")
        assertTrue(state.canMove(SpiderMove.PileToPile(3, 2, count = 2)))
        assertFalse(state.canMove(SpiderMove.PileToPile(3, 0, count = 1)))
        assertFalse(position("9S", "7S").canMove(SpiderMove.PileToPile(1, 0)))
    }

    @Test
    fun uncoveringAFaceDownCardTurnsItOver() {
        val state = position("2C 9D 8D", "10S", hidden = listOf(1))
        val moved = state.play(SpiderMove.PileToPile(0, 1, count = 2))
        assertEquals(0, moved.hidden[0])
    }

    @Test
    fun aRowIsDealtOnlyWithNoPileEmptyAndAWholeRunLeavesTheTable() {
        val full =
            position(
                "5S",
                "5S",
                "5S",
                "5S",
                "5S",
                "5S",
                "5S",
                "5S",
                "5S",
                "5S",
                stock = "AS AS AS AS AS AS AS AS AS 2C",
            )
        val dealt = full.play(SpiderMove.DealRow)
        assertTrue(dealt.stock.isEmpty())
        assertEquals(
            "2C",
            dealt.piles[0]
                .last()
                .card.code,
        )
        assertFalse(position("5S", stock = "AS AS AS AS AS AS AS AS AS AS").canMove(SpiderMove.DealRow))

        val almost = position("KH QH JH 10H 9H 8H 7H 6H 5H 4H 3H 2H", "AH", "4C")
        val finished = almost.play(SpiderMove.PileToPile(1, 0))
        assertEquals(1, finished.finished.size)
        assertTrue(finished.piles[0].isEmpty())
    }

    @Test
    fun randomPlayNeverLosesOrDuplicatesACard() {
        val random = Random(2026)
        for (suits in SpiderState.SUIT_CHOICES) {
            for (deal in 1L..5L) {
                var state = SpiderRules.forSuits(suits).deal(deal)
                val cards = state.all().toSet()
                repeat(300) {
                    val moves = state.legalMoves()
                    if (moves.isEmpty()) return@repeat
                    state = state.play(moves[random.nextInt(moves.size)])
                    assertEquals(104, state.all().size)
                    assertEquals(cards, state.all().toSet())
                }
            }
        }
    }

    @Test
    fun onceEveryPileIsOneSuitRunWithNothingHiddenItFinishesItself() {
        var state =
            position(
                "KH QH JH 10H 9H 8H 7H",
                "6H 5H 4H 3H 2H AH",
                "KS QS JS 10S",
                "9S 8S 7S 6S 5S 4S 3S 2S AS",
                finished = 6,
            )
        assertTrue(state.canAutoComplete)
        while (true) state = state.play(state.nextAutoMove() ?: break)
        assertTrue(state.isWon)
        assertFalse(position("KH QH", hidden = listOf(1), finished = 7).canAutoComplete)
        assertFalse(position("KH QH", stock = "JH", finished = 7).canAutoComplete)
        assertNull(position("KH QH", hidden = listOf(1), finished = 7).nextAutoMove())
    }

    @Test
    fun theHintFinishesARunFirstThenTurnsCardsThenBuildsOnItsOwnSuit() {
        val almost = position("KH QH JH 10H 9H 8H 7H 6H 5H 4H 3H 2H", "AH", "2C 5S", hidden = listOf(0, 0, 1))
        assertEquals(SpiderMove.PileToPile(1, 0), almost.hint())
        assertEquals(SpiderMove.PileToPile(0, 1), position("2C 5S", "6D", hidden = listOf(1)).hint())
        assertEquals(SpiderMove.PileToPile(0, 2), position("5S", "6D", "6S", "KC").hint())
        assertEquals(
            SpiderMove.DealRow,
            position(
                "KS",
                "KS",
                "KS",
                "KS",
                "KS",
                "KS",
                "KS",
                "KS",
                "KS",
                "KS",
                stock = run.split(' ').take(10).joinToString(" "),
            ).hint(),
        )
    }

    @Test
    fun theScoreStartsAt500LosesOneAMoveAndGainsAHundredARun() {
        val rules = SpiderRules.forSuits(1)
        val game = Game.start(rules, 1)
        assertEquals(500, game.score)
        val move = game.state.legalMoves().first { it is SpiderMove.PileToPile }
        assertEquals(499, game.play(move).score)
        assertEquals(498, game.play(move).undo().score)
        assertEquals(497, Game.resume(rules, GameRecord<SpiderMove>(1, movesMade = 3)).score)
    }

    @Test
    fun gamesSaveAndLoadForEachSuitCountAndTheMovesKeepTheirNames() {
        val type = GameType.Spider(2)
        val played = Game.start(SpiderRules.forSuits(2), 42).play(SpiderMove.DealRow)
        assertEquals(played.record, type.load(played.toJson())?.record)
        assertEquals("""{"type":"deal"}""", Json.encodeToString(SpiderMove.serializer(), SpiderMove.DealRow))
    }

    private companion object {
        val DEAL_ONE_TOPS = listOf("2S", "8D", "7S", "4C", "JC", "10H", "5C", "5H", "2H", "3C")
    }
}
