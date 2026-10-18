package com.maximx1.fads.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RecordsTest {
    @Test
    fun theFirstWinIsTheBestOfAllAndTheBestOnItsDeal() {
        val win = Win(dealNumber = 617, millis = 200_000, moves = 120)
        val times = Records().with(win)
        assertEquals(win, times.fastest)
        assertEquals(200_000, times.bestOn(617))
        assertNull(times.bestOn(1))
        assertNull(Records().fastest)
    }

    @Test
    fun aQuickerWinTakesOverAndASlowerOneChangesNothing() {
        val first = Records().with(Win(617, 200_000, 120))
        assertSame(first, first.with(Win(617, 200_000, 90)))
        assertSame(first, first.with(Win(617, 250_000, 90)))

        val quicker = Win(617, 150_000, 140)
        val records = first.with(quicker)
        assertEquals(quicker, records.fastest)
        assertEquals(150_000, records.bestOn(617))
    }

    @Test
    fun aHigherScoreIsKeptEvenFromASlowerWin() {
        val first = Win(617, 200_000, 120, score = 4000)
        val higher = Win(617, 250_000, 90, score = 4500)
        val records = Records().with(first).with(higher)
        assertEquals(first, records.fastest)
        assertEquals(200_000, records.bestOn(617))
        assertEquals(higher, records.highest)
        assertEquals(4500, records.highestOn(617))

        assertSame(records, records.with(Win(617, 300_000, 90, score = 4500)))
    }

    @Test
    fun aLowScoringWinOnANewDealIsThatDealsHighScoreButNotTheHighestOfAll() {
        val first = Win(617, 200_000, 120, score = 4000)
        val records = Records().with(first).with(Win(5, 300_000, 100, score = 3000))
        assertEquals(first, records.highest)
        assertEquals(3000, records.highestOn(5))
        assertNull(records.highestOn(1))
    }

    @Test
    fun recordsSavedBeforeThereWereScoresStillLoad() {
        val old =
            Records.fromJson(
                """{"fastest":{"dealNumber":617,"millis":200000,"moves":120},"deals":{"617":200000}}""",
            )
        assertEquals(Win(617, 200_000, 120, score = 0), old?.fastest)
        assertNull(old?.highest)
        assertNull(old?.highestOn(617))
    }

    @Test
    fun aSlowWinOnANewDealIsThatDealsBestButNotTheBestOfAll() {
        val first = Win(617, 200_000, 120)
        val times = Records().with(first).with(Win(5, 300_000, 100))
        assertEquals(first, times.fastest)
        assertEquals(300_000, times.bestOn(5))
        assertEquals(200_000, times.bestOn(617))
    }

    @Test
    fun recordsAreSavedAndReadBack() {
        val times = Records().with(Win(617, 200_000, 120, 4000)).with(Win(5, 300_000, 100, 3000))
        assertEquals(times, Records.fromJson(times.toJson()))
        assertEquals(Records(), Records.fromJson("{}"))
        assertNull(Records.fromJson("not records"))
    }
}
