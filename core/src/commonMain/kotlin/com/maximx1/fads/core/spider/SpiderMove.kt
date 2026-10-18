package com.maximx1.fads.core.spider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One Spider action. Piles are zero-based indexes. */
@Serializable
sealed interface SpiderMove {
    /** Move the top [count] cards of pile [from], a run of one suit, onto pile [to]. */
    @Serializable
    @SerialName("pp")
    data class PileToPile(
        val from: Int,
        val to: Int,
        val count: Int = 1,
    ) : SpiderMove

    /** Deal one card from the stock face up onto every pile. */
    @Serializable
    @SerialName("deal")
    data object DealRow : SpiderMove
}
