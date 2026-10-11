package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.maximx1.fads.core.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLayoutTest {
    private val gap = 12f
    private val freshDeal = listOf(7, 7, 7, 7, 6, 6, 6, 6)

    /** Phone upright, phone on its side, tablet upright, tablet on its side: width x height in pixels. */
    private val screens = listOf(1280f to 2400f, 2600f to 1000f, 1600f to 2300f, 2560f to 1400f)

    private fun BoardLayout.card(topLeft: Offset) = Rect(topLeft, Size(cardWidth, cardHeight))

    @Test
    fun everythingFitsOnEveryScreenEvenWithAVeryLongColumn() {
        for ((width, height) in screens) {
            for (sizes in listOf(freshDeal, listOf(19, 1, 0, 7, 6, 6, 6, 7))) {
                val layout = BoardLayout(width, height, gap, sizes)
                val screen = Rect(0f, 0f, width, height)
                val cards =
                    sizes.flatMapIndexed { column, size ->
                        List(size) { layout.card(layout.position(Spot.InColumn(column, it))) }
                    } + List(4) { layout.card(layout.freeCell(it)) } +
                        Suit.entries.map { layout.card(layout.foundation(it)) }
                for (card in cards) {
                    assertTrue(
                        "$card outside $screen",
                        screen.contains(card.topLeft) && screen.contains(card.bottomRight - Offset(1f, 1f)),
                    )
                }
            }
        }
    }

    @Test
    fun columnsSitBelowTheTopRowAndSideBySide() {
        for ((width, height) in screens) {
            val layout = BoardLayout(width, height, gap, freshDeal)
            val topRowBottom = layout.card(layout.freeCell(0)).bottom
            for (column in 0 until 8) {
                assertTrue(layout.columnBase(column).y > topRowBottom)
                if (column > 0) {
                    assertTrue(
                        layout.columnBase(column).x >= layout.card(layout.columnBase(column - 1)).right + gap - 0.01f,
                    )
                }
            }
            assertEquals(layout.columnBase(0).x, layout.freeCell(0).x)
            assertEquals(layout.columnBase(4).x, layout.foundation(Suit.SPADES).x)
            assertEquals(layout.columnBase(7).x, layout.foundation(Suit.DIAMONDS).x)
        }
    }

    @Test
    fun foundationsRunSpadesClubsHeartsDiamondsFromTheLeft() {
        val layout = BoardLayout(1280f, 2400f, gap, freshDeal)
        val leftToRight = Suit.entries.sortedBy { layout.foundation(it).x }
        assertEquals(listOf(Suit.SPADES, Suit.CLUBS, Suit.HEARTS, Suit.DIAMONDS), leftToRight)
        assertEquals(
            4,
            Suit.entries
                .map { layout.foundation(it).x }
                .toSet()
                .size,
        )
    }

    @Test
    fun cardsInAColumnStepDownEvenlyAndSqueezeWhenTheColumnIsLong() {
        val short = BoardLayout(1280f, 2400f, gap, freshDeal)
        val step = short.position(Spot.InColumn(0, 1)).y - short.position(Spot.InColumn(0, 0)).y
        assertEquals(step * 6, short.position(Spot.InColumn(0, 6)).y - short.position(Spot.InColumn(0, 0)).y, 0.01f)

        val long = BoardLayout(2600f, 1000f, gap, listOf(19, 7, 7, 7, 6, 6, 0, 0))
        val squeezed = long.position(Spot.InColumn(0, 1)).y - long.position(Spot.InColumn(0, 0)).y
        val normal = long.position(Spot.InColumn(1, 1)).y - long.position(Spot.InColumn(1, 0)).y
        assertTrue(squeezed < normal)
        assertTrue(squeezed > 0f)
    }

    @Test
    fun dropTargetsMatchWhatIsDrawnThere() {
        for ((width, height) in screens) {
            val layout = BoardLayout(width, height, gap, freshDeal)
            for (i in 0 until 4) {
                assertEquals(DropTarget.FreeCell(i), layout.targetAt(layout.freeCell(i) + layout.cardCentre))
            }
            for (suit in Suit.entries) {
                assertEquals(DropTarget.Foundation, layout.targetAt(layout.foundation(suit) + layout.cardCentre))
            }
            for (column in 0 until 8) {
                assertEquals(DropTarget.Column(column), layout.targetAt(layout.columnBase(column) + layout.cardCentre))
                val wayDown = Offset(layout.columnBase(column).x + layout.cardWidth / 2, height - 1f)
                assertEquals(DropTarget.Column(column), layout.targetAt(wayDown))
            }
            assertNull(layout.targetAt(Offset(-50f, height / 2)))
            assertNull(layout.targetAt(Offset(width + 50f, height / 2)))
        }
    }
}
