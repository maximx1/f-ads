package com.maximx1.fads.core.war

import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.replay
import com.maximx1.fads.core.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WarTest {
    private fun cards(vararg codes: String) = codes.map(Card::fromCode)

    private fun position(
        yours: List<Card>,
        theirs: List<Card>,
    ) = WarState(dealNumber = 1, yours = yours, theirs = theirs)

    @Test
    fun theDeckIsDealtOneAtATimeYouFirst() {
        val deal = warDeal(1)
        assertEquals(26, deal.yours.size)
        assertEquals(26, deal.theirs.size)
        assertEquals(Card.deck.toSet(), (deal.yours + deal.theirs).toSet())
        assertEquals(deal, warDeal(1))
        assertNotEquals(deal, warDeal(2))
        assertEquals(DEAL_ONE_YOURS, deal.yours.take(DEAL_ONE_YOURS.size).map { it.code })
        assertNull(deal.battle)
        assertEquals(26, deal.held(Side.YOU))
    }

    @Test
    fun theHigherCardTakesBothAndAcesAreHigh() {
        val after = position(cards("AS", "2H"), cards("KD", "3C")).play(WarMove.Flip)
        val battle = assertNotNull(after.battle)
        assertEquals(cards("AS"), battle.yours)
        assertEquals(cards("KD"), battle.theirs)
        assertEquals(Side.YOU, battle.winner)
        assertFalse(battle.wasWar)
        assertEquals(3, after.held(Side.YOU))
        assertEquals(1, after.held(Side.THEM))

        val next = after.play(WarMove.Flip)
        assertEquals(Side.THEM, next.battle?.winner, "3 beats 2")
        assertEquals(2, next.battles)
        // The ace and the king went under your pile before the twos and threes were fought.
        assertEquals(cards("AS", "KD").toSet(), next.yours.toSet())
    }

    @Test
    fun aTieGoesToWarThreeDownAndOneUp() {
        val start =
            position(
                cards("7S", "2S", "3S", "4S", "KS", "9S"),
                cards("7H", "2H", "3H", "4H", "QH", "9H"),
            )
        val after = start.play(WarMove.Flip)
        val battle = assertNotNull(after.battle)
        assertTrue(battle.wasWar)
        assertEquals(Side.YOU, battle.winner)
        assertEquals(5, battle.yours.size)
        assertEquals(
            listOf(true, false, false, false, true),
            battle.yours.indices.map { Battle.isFaceUp(it, battle.yours) },
        )
        assertEquals(cards("9S"), after.yours)
        assertEquals(11, after.held(Side.YOU))
    }

    @Test
    fun aSideShortOfCardsInAWarLaysWhatItHasItsLastFaceUp() {
        val after = position(cards("7S", "2S", "KS"), cards("7H", "2H", "3H", "4H", "QH", "9H")).play(WarMove.Flip)
        val battle = assertNotNull(after.battle)
        assertEquals(cards("7S", "2S", "KS"), battle.yours)
        assertEquals(listOf(true, false, true), battle.yours.indices.map { Battle.isFaceUp(it, battle.yours) })
        assertEquals(Side.YOU, battle.winner)
        assertEquals(cards("9H"), after.theirs)
    }

    @Test
    fun aSideWithNoCardLeftForAWarLosesItAndTheGame() {
        val after = position(cards("7S"), cards("7H", "2H")).play(WarMove.Flip)
        assertEquals(Side.THEM, after.battle?.winner)
        assertTrue(after.isLost)
        assertTrue(after.isOver)
        assertFalse(after.canMove(WarMove.Flip))
        assertFailsWith<IllegalArgumentException> { after.play(WarMove.Flip) }
    }

    @Test
    fun whenBothRunOutInAWarATossSettlesIt() {
        val after = position(cards("7S", "2S"), cards("7H", "3H")).play(WarMove.Flip)
        assertEquals(Side.THEM, after.battle?.winner, "one card each left after the tie: 3 beats 2")

        val tosses = (0L until 20L).map { WarState(it, cards("7S"), cards("7H")).play(WarMove.Flip) }
        assertTrue(tosses.all { it.isOver })
        assertEquals(setOf(Side.YOU, Side.THEM), tosses.mapNotNull { it.battle?.winner }.toSet())
        assertEquals(tosses, (0L until 20L).map { WarState(it, cards("7S"), cards("7H")).play(WarMove.Flip) })
    }

    @Test
    fun takingTheLastCardWins() {
        val after = position(cards("KS"), cards("2H")).play(WarMove.Flip)
        assertTrue(after.isWon)
        assertEquals(2, after.held(Side.YOU))
    }

    @Test
    fun everyDealEndsWithOneSideHoldingAllFiftyTwo() {
        var wins = 0
        for (number in 0L until DEALS) {
            var state = warDeal(number)
            var flips = 0
            while (!state.isOver) {
                state = state.play(WarMove.Flip)
                flips++
                assertEquals(52, state.held(Side.YOU) + state.held(Side.THEM), "deal $number, flip $flips")
                assertTrue(flips < MAX_FLIPS, "deal $number never ends")
            }
            if (state.isWon) wins++
        }
        assertTrue(wins in DEALS / 4..DEALS * 3 / 4, "won $wins of $DEALS")
    }

    @Test
    fun itPlaysItselfOutAndScoresTheCardsYouHold() {
        var game = GameType.War.start(5)
        assertEquals(26, game.score)
        assertTrue(game.canAutoComplete)
        assertEquals(WarMove.Flip, game.hint())
        while (true) game = game.play(game.nextAutoMove() ?: break)
        assertFalse(game.canAutoComplete)
        assertNull(game.hint())
        assertEquals(if (game.isWon) 52 else 0, game.score)
    }

    @Test
    fun aGameIsSavedAndLoadedAsWar() {
        val game =
            GameType.War
                .start(9)
                .play(WarMove.Flip)
                .play(WarMove.Flip)
        val loaded = assertNotNull(GameType.War.load(game.toJson()))
        assertEquals(game.record, loaded.record)
        assertEquals(game.state, WarRules.replay(loaded.record))
        assertTrue(game.toJson().contains("\"flip\""))
        assertEquals(GameType.War, GameType.fromKey("war"))
        assertNull(WarRules.moveOrNull("not a move"))
    }

    private companion object {
        const val DEALS = 200L
        const val MAX_FLIPS = 20_000
        val DEAL_ONE_YOURS = listOf("7H")
    }
}
