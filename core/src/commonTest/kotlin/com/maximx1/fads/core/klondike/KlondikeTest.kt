package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.cards
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameRecord
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KlondikeTest {
    private val drawOne = KlondikeRules.forDraw(1)
    private val drawThree = KlondikeRules.forDraw(3)

    private fun KlondikeState.allCards(): List<Card> =
        piles.flatten() + stock + waste +
            Suit.entries.flatMap { suit -> Card.deck.filter { it.suit == suit }.take(foundationCount(suit)) }

    /** A position for a test: piles bottom card first, with [hidden] cards face down at the bottom of each. */
    private fun position(
        vararg piles: String,
        hidden: List<Int> = emptyList(),
        stock: String = "",
        waste: String = "",
        foundations: Map<Suit, Int> = emptyMap(),
    ) = KlondikeState(
        piles = List(KlondikeState.PILE_COUNT) { cards(piles.getOrElse(it) { "" }) },
        hidden = List(KlondikeState.PILE_COUNT) { hidden.getOrElse(it) { 0 } },
        stock = cards(stock),
        waste = cards(waste),
        foundations = foundations,
    )

    @Test
    fun aDealHasOneToSevenCardsAPileOnlyTheTopFaceUpAndTheRestInTheStock() {
        val state = drawOne.deal(617)
        assertEquals((1..7).toList(), state.piles.map { it.size })
        assertEquals((0..6).toList(), state.hidden)
        assertEquals(24, state.stock.size)
        assertTrue(state.waste.isEmpty())
        assertEquals(Card.deck.toSet(), state.allCards().toSet())
        assertEquals(52, state.allCards().size)
    }

    @Test
    fun theSameNumberAlwaysDealsTheSameGame() {
        assertEquals(drawOne.deal(617), drawOne.deal(617))
        assertNotEquals(drawOne.deal(617), drawOne.deal(618))
        // Deal 1 locked in place, so a change to the shuffle or the dealing shows up here.
        val one = drawOne.deal(1)
        assertEquals(dealOneTops, one.piles.map { it.last().code })
        assertEquals(DEAL_ONE_STOCK_TOP, one.stock.last().code)
    }

    @Test
    fun drawingOneTurnsTheTopOfTheStockAndDrawingThreeTurnsThreeAsAPacket() {
        val state = position(stock = "2C 3C 4C 5C")
        assertEquals(cards("5C"), state.play(KlondikeMove.Draw).waste)
        val three = state.copy(draw = 3).play(KlondikeMove.Draw)
        assertEquals(cards("5C 4C 3C"), three.waste)
        assertEquals(cards("2C"), three.stock)
        // Fewer than three left: all of them.
        assertEquals(cards("5C 4C 3C 2C"), three.play(KlondikeMove.Draw).waste)
    }

    @Test
    fun recyclingPutsTheStockBackAsItWasAndOnlyWhenTheStockIsEmpty() {
        val start = position(stock = "2C 3C 4C 5C")
        var state = start
        repeat(4) { state = state.play(KlondikeMove.Draw) }
        assertFalse(start.canMove(KlondikeMove.Recycle))
        assertEquals(start, state.play(KlondikeMove.Recycle))
        assertFalse(position().canMove(KlondikeMove.Recycle))
    }

    @Test
    fun cardsBuildDownInAlternateColoursAndOnlyAKingGoesToAnEmptyPile() {
        val state = position("8S", "", waste = "7H KD", foundations = emptyMap())
        assertTrue(state.canMove(KlondikeMove.WasteToPile(1)))
        assertFalse(state.canMove(KlondikeMove.WasteToPile(0)))
        val played = state.play(KlondikeMove.WasteToPile(1))
        assertTrue(played.canMove(KlondikeMove.WasteToPile(0)))
        assertFalse(position("8S", waste = "7C").canMove(KlondikeMove.WasteToPile(0)))
        assertFalse(position("8S", "", "QH").canMove(KlondikeMove.PileToPile(2, 1)))
    }

    @Test
    fun uncoveringAFaceDownCardTurnsItOver() {
        val state = position("2C 9D 8S", "", "10C", hidden = listOf(1))
        val moved = state.play(KlondikeMove.PileToPile(0, 2, count = 2))
        assertEquals(cards("2C"), moved.piles[0])
        assertEquals(0, moved.hidden[0])
        assertFailsWith<IllegalArgumentException> { state.play(KlondikeMove.PileToPile(0, 2, count = 3)) }
    }

    @Test
    fun aCardComesBackOffAFoundationOntoAPile() {
        val state = position("3S", foundations = mapOf(Suit.HEARTS to 2))
        val back = state.play(KlondikeMove.FoundationToPile(Suit.HEARTS, 0))
        assertEquals(cards("3S 2H"), back.piles[0])
        assertEquals(1, back.foundationCount(Suit.HEARTS))
    }

    @Test
    fun randomPlayNeverLosesOrDuplicatesACard() {
        val random = Random(2026)
        for (deal in 1L..20L) {
            for (rules in listOf(drawOne, drawThree)) {
                var state = rules.deal(deal)
                repeat(300) {
                    val moves = state.legalMoves()
                    if (moves.isEmpty()) return@repeat
                    state = state.play(moves[random.nextInt(moves.size)])
                    assertEquals(52, state.allCards().size)
                    assertEquals(Card.deck.toSet(), state.allCards().toSet())
                    state.piles.indices.forEach { pile ->
                        if (state.piles[pile].isNotEmpty()) assertTrue(state.hidden[pile] < state.piles[pile].size)
                    }
                }
            }
        }
    }

    @Test
    fun onceEverythingIsFaceUpOnThePilesItFinishesItself() {
        val upToTen = Suit.entries.associateWith { 10 }
        var state = position("KS QH JS", "KH QS JH", "KD QC JD", "KC QD JC", foundations = upToTen)
        assertTrue(state.canAutoComplete)
        while (true) state = state.play(state.nextAutoMove() ?: break)
        assertTrue(state.isWon)
        assertFalse(position("KS QH", stock = "JS", foundations = upToTen).canAutoComplete)
        assertFalse(position("KS QH JS", hidden = listOf(1), foundations = upToTen).canAutoComplete)
    }

    @Test
    fun theHintPrefersFoundationsThenTurningCardsOverThenTheWasteThenTheStock() {
        assertEquals(KlondikeMove.PileToFoundation(0), position("AS", waste = "7H", stock = "2C").hint())
        assertEquals(
            KlondikeMove.PileToPile(0, 1),
            position("2C 7H", "8S", waste = "4D", stock = "2D", hidden = listOf(1)).hint(),
        )
        assertEquals(KlondikeMove.WasteToPile(1), position("", "8S", waste = "7H", stock = "2C").hint())
        assertEquals(KlondikeMove.Draw, position("9S", stock = "2C").hint())
        assertEquals(KlondikeMove.Recycle, position("9S", waste = "2C").hint())
        assertNull(position("9S").hint())
    }

    @Test
    fun scoringFollowsWindowsStandard() {
        fun scoreAfter(
            rules: KlondikeRules,
            moves: List<KlondikeMove>,
            millis: Long = 0,
        ) = Game.resume(rules, GameRecord(1, moves)).withElapsed(millis).score

        val game = Game.start(drawOne, 1)
        assertEquals(0, game.score)
        // Points never go below zero, and time costs two every ten seconds.
        val drawn = List(24) { KlondikeMove.Draw }
        assertEquals(0, scoreAfter(drawOne, drawn + KlondikeMove.Recycle))
        val state = drawOne.deal(1)
        val firstUseful = state.legalMoves().firstOrNull { it is KlondikeMove.PileToPile && state.hidden[it.from] > 0 }
        if (firstUseful != null) {
            // Any move off a pile that uncovers a face-down card turns it, for 5.
            assertEquals(5, scoreAfter(drawOne, listOf(firstUseful)))
            assertEquals(1, scoreAfter(drawOne, listOf(firstUseful), millis = 25_000))
        }
        assertEquals(
            -100,
            KlondikeScoring.pointsFor(position(waste = "2C"), KlondikeMove.Recycle, position(stock = "2C")),
        )
        assertEquals(
            -20,
            KlondikeScoring.pointsFor(
                position(waste = "2C").copy(draw = 3),
                KlondikeMove.Recycle,
                position(stock = "2C").copy(draw = 3),
            ),
        )
        val back = position("3S", foundations = mapOf(Suit.HEARTS to 2))
        assertEquals(
            -15,
            KlondikeScoring.pointsFor(
                back,
                KlondikeMove.FoundationToPile(Suit.HEARTS, 0),
                back.play(KlondikeMove.FoundationToPile(Suit.HEARTS, 0)),
            ),
        )
        val fromWaste = position("8S", waste = "7H")
        assertEquals(
            5,
            KlondikeScoring.pointsFor(
                fromWaste,
                KlondikeMove.WasteToPile(0),
                fromWaste.play(KlondikeMove.WasteToPile(0)),
            ),
        )
    }

    @Test
    fun gamesSaveAndLoadForEachDrawAndTheMovesKeepTheirNames() {
        val type = GameType.Klondike(3)
        val game = type.start(42)
        assertEquals(drawThree.deal(42), game.state)
        val played = Game.start(drawThree, 42).play(KlondikeMove.Draw)
        assertEquals(played.record, type.load(played.toJson())?.record)
        assertEquals("""{"type":"draw"}""", Json.encodeToString(KlondikeMove.serializer(), KlondikeMove.Draw))
        assertEquals(
            """{"type":"pp","from":1,"to":2,"count":3}""",
            Json.encodeToString(KlondikeMove.serializer(), KlondikeMove.PileToPile(1, 2, 3)),
        )
    }

    private companion object {
        val dealOneTops = listOf("7H", "6H", "2H", "2S", "KD", "JC", "8C")
        const val DEAL_ONE_STOCK_TOP = "7C"
    }
}
