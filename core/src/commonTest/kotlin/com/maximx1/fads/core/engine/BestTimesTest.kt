package com.maximx1.fads.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class BestTimesTest {
    @Test
    fun theFirstWinIsTheBestOfAllAndTheBestOnItsDeal() {
        val win = Win(dealNumber = 617, millis = 200_000, moves = 120)
        val times = BestTimes().with(win)
        assertEquals(win, times.fastest)
        assertEquals(200_000, times.bestOn(617))
        assertNull(times.bestOn(1))
        assertNull(BestTimes().fastest)
    }

    @Test
    fun aQuickerWinTakesOverAndASlowerOneChangesNothing() {
        val first = BestTimes().with(Win(617, 200_000, 120))
        assertSame(first, first.with(Win(617, 200_000, 90)))
        assertSame(first, first.with(Win(617, 250_000, 90)))

        val quicker = Win(617, 150_000, 140)
        assertEquals(BestTimes(quicker, mapOf(617L to 150_000L)), first.with(quicker))
    }

    @Test
    fun aSlowWinOnANewDealIsThatDealsBestButNotTheBestOfAll() {
        val first = Win(617, 200_000, 120)
        val times = BestTimes().with(first).with(Win(5, 300_000, 100))
        assertEquals(first, times.fastest)
        assertEquals(300_000, times.bestOn(5))
        assertEquals(200_000, times.bestOn(617))
    }

    @Test
    fun recordsAreSavedAndReadBack() {
        val times = BestTimes().with(Win(617, 200_000, 120)).with(Win(5, 300_000, 100))
        assertEquals(times, BestTimes.fromJson(times.toJson()))
        assertEquals(BestTimes(), BestTimes.fromJson("{}"))
        assertNull(BestTimes.fromJson("not records"))
    }
}
