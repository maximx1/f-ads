package com.maximx1.fads.core.rules

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
}
