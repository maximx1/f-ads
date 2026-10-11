package com.maximx1.fads.core.deal

import com.maximx1.fads.core.columnCodes
import com.maximx1.fads.core.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * These layouts are the contract for numbered deals. They were produced by a separate implementation of the
 * algorithm described on [Deal]. If one of these tests fails, every player's deal numbers have changed.
 */
class DealTest {
    private val fixtures =
        mapOf(
            0L to
                listOf(
                    "8C 3H 5D 5H 3C 9C 6H",
                    "JD AS 2S 8D 4H 7H 4D",
                    "9S JS 3D 8H 2D JC KS",
                    "2C 7S 5S AH 9H 6D 10D",
                    "2H 9D AD QD 3S KC",
                    "QH 8S 6S 10C KH 4S",
                    "6C QC 4C QS AC 7D",
                    "10S KD 10H JH 7C 5C",
                ),
            1L to
                listOf(
                    "7H 9C 7D 10C AC 9H 3D",
                    "9S JS 4D JC 5S 3H 2C",
                    "4H 5H 2S 10H AD 3C 9D",
                    "8D KH AH 8C KC AS 7C",
                    "8H 3S 6C QS 6D 4S",
                    "5C 2H KS 4C QC 2D",
                    "JH JD KD 6S QD 8S",
                    "6H 5D 10D 7S QH 10S",
                ),
            2L to
                listOf(
                    "QD AS 10S AH AC 9S 3H",
                    "3C 6S 9C QH 5S 10D 2S",
                    "4S 7C JH 6C JS 5H 8H",
                    "5D 3S 8S 8D AD 10C 9D",
                    "7D QS 4H 3D KS QC",
                    "2H 7H 5C 9H 2D KC",
                    "KD 6D 2C 6H 7S 8C",
                    "10H JD 4D JC 4C KH",
                ),
            1_000_000L to
                listOf(
                    "3H 3S 7S 2S 4S 8S 7D",
                    "3D 5D JS 6C KH 2C 8C",
                    "6D 5H 10H QD AC AD 7C",
                    "KC 6H 2H 9D 10D 5C 7H",
                    "QS AS 8D 9S 10C 3C",
                    "10S 9H KS 4C 6S AH",
                    "JC 8H 4H 9C 5S KD",
                    "JH QC 2D 4D QH JD",
                ),
            Int.MAX_VALUE.toLong() to
                listOf(
                    "8C 5C KD 2S QC 8S 4D",
                    "10C 9D AD 9S 6C JD 6H",
                    "4H 9C 10H 7C 5D 9H KS",
                    "KH 4S 2H 10S JC AS 6D",
                    "JH QS 8D 3C 5S 5H",
                    "QD KC 7D 8H AC QH",
                    "2D 6S 10D 3S 3D AH",
                    "3H JS 7S 2C 7H 4C",
                ),
            Long.MAX_VALUE to
                listOf(
                    "5S 8C 2H KS 6S AS 4S",
                    "JS QD 7H JD 2C 7C QC",
                    "8D 3D 7D 7S 4C 8S 5D",
                    "AD 3S JC 5H 9S KH KC",
                    "10H 10C KD 6H 5C 3C",
                    "9D QS 3H 9H 6D 4D",
                    "2S 8H 9C AH 10D JH",
                    "10S AC QH 6C 4H 2D",
                ),
        )

    @Test
    fun knownDealNumbersGiveTheirKnownLayouts() {
        for ((number, layout) in fixtures) {
            assertEquals(layout, Deal.fromNumber(number).columnCodes(), "deal $number")
        }
    }

    @Test
    fun everyDealHasAllFiftyTwoCardsSevenInTheFirstFourColumnsAndSixInTheRest() {
        for (number in 0L..500L) {
            val state = Deal.fromNumber(number)
            assertEquals(Card.deck.toSet(), state.allCards().toSet(), "deal $number")
            assertEquals(listOf(7, 7, 7, 7, 6, 6, 6, 6), state.columns.map { it.size }, "deal $number")
            assertTrue(state.freeCells.all { it == null })
            assertTrue(state.foundations.isEmpty())
        }
    }

    @Test
    fun sameNumberSameDealAndDifferentNumbersDiffer() {
        assertEquals(Deal.fromNumber(617), Deal.fromNumber(617))
        assertEquals(501, (0L..500L).map { Deal.fromNumber(it) }.toSet().size)
        assertNotEquals(Deal.fromNumber(0), Deal.fromNumber(1))
    }

    @Test
    fun negativeNumbersAreRejected() {
        assertFailsWith<IllegalArgumentException> { Deal.fromNumber(-1) }
    }
}
