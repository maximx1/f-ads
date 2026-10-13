package com.maximx1.fads.core.rules

import com.maximx1.fads.core.model.Suit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One player action. Columns and free cells are zero-based indexes. */
@Serializable
sealed interface Move {
    /** Move the top [count] cards of column [from], as a run, onto column [to]. */
    @Serializable
    @SerialName("cc")
    data class ColumnToColumn(
        val from: Int,
        val to: Int,
        val count: Int = 1,
    ) : Move

    @Serializable
    @SerialName("cf")
    data class ColumnToFreeCell(
        val from: Int,
        val cell: Int,
    ) : Move

    @Serializable
    @SerialName("ch")
    data class ColumnToFoundation(
        val from: Int,
    ) : Move

    @Serializable
    @SerialName("fc")
    data class FreeCellToColumn(
        val cell: Int,
        val to: Int,
    ) : Move

    @Serializable
    @SerialName("fh")
    data class FreeCellToFoundation(
        val cell: Int,
    ) : Move

    /** Take the top card of [suit]'s foundation back, onto column [to]. */
    @Serializable
    @SerialName("hc")
    data class FoundationToColumn(
        val suit: Suit,
        val to: Int,
    ) : Move

    /** Take the top card of [suit]'s foundation back, into free cell [cell]. */
    @Serializable
    @SerialName("hf")
    data class FoundationToFreeCell(
        val suit: Suit,
        val cell: Int,
    ) : Move
}
