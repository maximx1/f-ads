package com.maximx1.fads.core.engine

import com.maximx1.fads.core.deal.Deal
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit
import com.maximx1.fads.core.position
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.state.GameState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun cardsAreWrittenAsTheirCodes() {
        assertEquals("\"10H\"", Json.encodeToString(Card.fromCode("10H")))
        assertEquals(Card.fromCode("AS"), Json.decodeFromString<Card>("\"AS\""))
    }

    @Test
    fun aPositionRoundTrips() {
        val state = position("KS QH", "", "5D", free = "- 9C", foundations = mapOf(Suit.HEARTS to 2))
        assertEquals(state, Json.decodeFromString<GameState>(Json.encodeToString(state)))

        val fresh = Deal.fromNumber(617)
        assertEquals(fresh, Json.decodeFromString<GameState>(Json.encodeToString(fresh)))
    }

    @Test
    fun aGameRecordRoundTripsAndReplaysToTheSamePosition() {
        val record =
            GameRecord(
                dealNumber = 1,
                moves =
                    listOf(
                        Move.ColumnToColumn(0, 4),
                        Move.ColumnToFreeCell(0, 0),
                        Move.ColumnToFoundation(0),
                        Move.ColumnToFoundation(1),
                    ),
            )
        val restored = Json.decodeFromString<GameRecord>(Json.encodeToString(record))
        assertEquals(record, restored)
        assertEquals(record.replay(), restored.replay())
    }

    @Test
    fun savedGameFormatIsStable() {
        val record =
            GameRecord(
                dealNumber = 7,
                moves =
                    listOf(
                        Move.ColumnToColumn(0, 1, 2),
                        Move.ColumnToFreeCell(2, 3),
                        Move.ColumnToFoundation(4),
                        Move.FreeCellToColumn(3, 5),
                        Move.FreeCellToFoundation(0),
                    ),
            )
        val json =
            """{"dealNumber":7,"moves":[{"type":"cc","from":0,"to":1,"count":2},{"type":"cf","from":2,"cell":3},""" +
                """{"type":"ch","from":4},{"type":"fc","cell":3,"to":5},{"type":"fh","cell":0}]}"""
        assertEquals(json, Json.encodeToString(record))
        assertEquals(record, Json.decodeFromString<GameRecord>(json))
    }

    @Test
    fun anEmptyRecordIsJustTheDeal() {
        assertEquals(Deal.fromNumber(42), GameRecord(42).replay())
    }
}
