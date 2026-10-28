package com.maximx1.fads.core.den

import com.maximx1.fads.core.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoPokerTest {
    private fun cards(codes: String) = codes.split(' ').map(Card::fromCode)

    @Test
    fun handsPayFromJacksOrBetterToARoyalFlush() {
        assertEquals(250, JacksOrBetter.pays(cards("10H JH QH KH AH")))
        assertEquals(50, JacksOrBetter.pays(cards("9H 10H JH QH KH")))
        assertEquals(25, JacksOrBetter.pays(cards("5S 5H 5D 5C 2S")))
        assertEquals(9, JacksOrBetter.pays(cards("5S 5H 5D 2C 2S")))
        assertEquals(6, JacksOrBetter.pays(cards("2H 7H 9H JH KH")))
        assertEquals(4, JacksOrBetter.pays(cards("AS 2H 3D 4C 5S")))
        assertEquals(3, JacksOrBetter.pays(cards("5S 5H 5D KC 2S")))
        assertEquals(2, JacksOrBetter.pays(cards("5S 5H 2D 2C KS")))
        assertEquals(1, JacksOrBetter.pays(cards("JS JH 2D 7C KS")))
        assertEquals(0, JacksOrBetter.pays(cards("10S 10H 2D 7C KS")), "tens are not enough")
        assertEquals(0, JacksOrBetter.pays(cards("AS 3H 6D 9C QS")))
        assertNull(JacksOrBetter.line(cards("AS 3H 6D 9C QS")))
        assertEquals(PayLine.JACKS_OR_BETTER, JacksOrBetter.line(cards("AS AH 2D 7C KS")))
        assertEquals(listOf(250, 50, 25, 9, 6, 4, 3, 2, 1), PayLine.entries.map { it.pays })
    }

    @Test
    fun aHandIsDealtForTheBetThenTheCardsNotHeldAreDrawnAgainAndPaid() {
        var den = Den(purse = Purse(50)).dealVideoPoker(5, 3)
        assertEquals(Purse(45), den.purse)
        val dealt = den.videoPoker().hand!!
        assertEquals(5, dealt.cards.size)
        assertTrue(den.videoPoker().dealt)
        assertFailsWith<IllegalArgumentException>("draw first") { den.dealVideoPoker(5, 4) }

        den = den.drawVideoPoker(setOf(0, 2))
        val drawn = den.videoPoker().hand!!
        assertEquals(dealt.cards[0], drawn.cards[0])
        assertEquals(dealt.cards[2], drawn.cards[2])
        assertEquals(5, drawn.cards.toSet().size)
        assertTrue((drawn.cards - dealt.cards.toSet()).size == 3, "three fresh cards")
        assertEquals(5L * JacksOrBetter.pays(drawn.cards), drawn.won)
        assertEquals(Purse(45 + drawn.won), den.purse)
        assertFalse(den.videoPoker().dealt)
        assertFailsWith<IllegalArgumentException>("deal first") { den.drawVideoPoker(emptySet()) }
        assertEquals(den, Den.fromJson(den.toJson()))
    }

    @Test
    fun holdingEverythingKeepsTheHandAndTheRoomsLeastBetStands() {
        val den = Den(purse = Purse(10)).dealVideoPoker(1, 8)
        val held = den.drawVideoPoker(setOf(0, 1, 2, 3, 4))
        assertEquals(den.videoPoker().hand!!.cards, held.videoPoker().hand!!.cards)
        assertFailsWith<IllegalArgumentException> {
            Den(
                purse = Purse(5_000),
            ).dealVideoPoker(999, 1, Room.HIGH_ROLLERS)
        }
        assertFailsWith<IllegalArgumentException> { Den(purse = Purse(4)).dealVideoPoker(5, 1) }
    }
}
