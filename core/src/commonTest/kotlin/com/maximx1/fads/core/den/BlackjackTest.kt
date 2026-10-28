package com.maximx1.fads.core.den

import com.maximx1.fads.core.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlackjackTest {
    private fun cards(codes: String) = codes.split(' ').map(Card::fromCode)

    /** A shoe dealt in order: the player's first card, the dealer's, the player's second, the dealer's, then hits. */
    private fun shoe(codes: String) = cards(codes) + Card.deck.filter { it !in cards(codes) }

    private fun deal(
        bet: Long,
        codes: String,
        purse: Long = 100,
    ) = Den(purse = Purse(purse)).dealBlackjackFrom(bet, shoe(codes))

    private fun Den.round() = blackjack().round!!

    @Test
    fun totalsCountAcesHighWhenTheyCanAndCourtCardsTen() {
        assertEquals(Total(21, soft = true), totalOf(cards("AS KH")))
        assertEquals(Total(13, soft = false), totalOf(cards("AS KH 2D")))
        assertEquals(Total(12, soft = true), totalOf(cards("AS AH")))
        assertEquals(Total(20, soft = false), totalOf(cards("QS JD")))
        assertTrue(isBlackjack(cards("AS 10H")))
        assertFalse(isBlackjack(cards("7S 7H 7D")))
    }

    @Test
    fun aNaturalPaysThreeToTwoAtOnceAndADealersNaturalTakesTheBet() {
        val natural = deal(10, "AS 9D KH 7C")
        assertTrue(natural.round().over)
        assertEquals(listOf(HandResult.BLACKJACK), natural.round().results)
        assertEquals(Purse(100 - 10 + 25), natural.purse)

        val dealers = deal(10, "9S AD 7H KC")
        assertEquals(listOf(HandResult.LOSE), dealers.round().results)
        assertEquals(Purse(90), dealers.purse)

        val both = deal(10, "AS AD KH KC")
        assertEquals(listOf(HandResult.PUSH), both.round().results)
        assertEquals(Purse(100), both.purse)
    }

    @Test
    fun theDealerDrawsToSeventeenAndBustsOrStands() {
        // The player stands on 20; the dealer, on 6 with a 10 then a 4 to come, makes 20 too.
        val push = deal(10, "KS 6D QH 10C 4S").playBlackjack(BlackjackMove.STAND)
        assertEquals(listOf(HandResult.PUSH), push.round().results)
        assertEquals(cards("6D 10C 4S"), push.round().dealer)
        assertEquals(Purse(100), push.purse)

        // On 16 the dealer must draw, and the queen busts them.
        val bust = deal(10, "KS 6D 8H 10C QS").playBlackjack(BlackjackMove.STAND)
        assertEquals(listOf(HandResult.WIN), bust.round().results)
        assertEquals(Purse(110), bust.purse)
        assertFailsWith<IllegalArgumentException>("the round is over") { bust.playBlackjack(BlackjackMove.HIT) }
    }

    @Test
    fun hittingPastTwentyOneLosesWithoutTheDealerDrawing() {
        val den = deal(10, "KS 6D 5H 10C 9S").playBlackjack(BlackjackMove.HIT)
        assertEquals(listOf(HandResult.LOSE), den.round().results)
        assertEquals(cards("6D 10C"), den.round().dealer, "no need to draw against a bust hand")
        assertEquals(Purse(90), den.purse)
    }

    @Test
    fun doublingTakesOneMoreCardForTwiceTheBet() {
        val den = deal(10, "6S 9D 5H 8C KS").playBlackjack(BlackjackMove.DOUBLE)
        val hand = den.round().hands.single()
        assertTrue(hand.doubled)
        assertEquals(20, hand.bet)
        assertEquals(cards("6S 5H KS"), hand.cards)
        // The dealer's 17 stands, and 21 beats it.
        assertEquals(listOf(HandResult.WIN), den.round().results)
        assertEquals(Purse(100 - 20 + 40), den.purse)
        assertFalse(deal(10, "6S 9D 5H 8C", purse = 15).canPlayBlackjack(BlackjackMove.DOUBLE), "not enough to double")
    }

    @Test
    fun aPairSplitsIntoTwoHandsPlayedOneAfterTheOther() {
        var den = deal(10, "8S 9D 8H 8C 3S KD")
        assertTrue(den.canPlayBlackjack(BlackjackMove.SPLIT))
        den = den.playBlackjack(BlackjackMove.SPLIT)
        assertEquals(Purse(80), den.purse)
        assertEquals(listOf(cards("8S 3S"), cards("8H KD")), den.round().hands.map { it.cards })
        assertFalse(den.canPlayBlackjack(BlackjackMove.SPLIT), "once only")
        den = den.playBlackjack(BlackjackMove.STAND)
        assertEquals(1, den.round().active)
        den = den.playBlackjack(BlackjackMove.STAND)
        assertTrue(den.round().over)
        // The dealer's 17 beats 11 and loses to 18.
        assertEquals(listOf(HandResult.LOSE, HandResult.WIN), den.round().results)
        assertEquals(Purse(100), den.purse)
        assertEquals(den, Den.fromJson(den.toJson()))
    }

    @Test
    fun splitAcesTakeOneCardEachAndTwentyOneThereIsNoBlackjack() {
        val den = deal(10, "AS 9D AH 8C KS 5D").playBlackjack(BlackjackMove.SPLIT)
        assertTrue(den.round().over)
        assertEquals(listOf(HandResult.WIN, HandResult.LOSE), den.round().results, "21 pays even money, 16 loses")
        assertEquals(Purse(80 + 20), den.purse)
    }

    @Test
    fun aRoundNeedsABetTheRoomTakesAndNoRoundBeingPlayed() {
        assertFailsWith<IllegalArgumentException> { Den(purse = Purse(5_000)).dealBlackjack(999, 1, Room.HIGH_ROLLERS) }
        assertFailsWith<IllegalArgumentException> { Den(purse = Purse(5)).dealBlackjack(10, 1) }
        val playing = deal(10, "KS 6D 5H 10C")
        assertFailsWith<IllegalArgumentException> { playing.dealBlackjack(10, 2) }
        assertEquals(1, Den(purse = Purse(50)).dealBlackjack(5, 3).blackjack().rounds)
    }
}
