package com.maximx1.fads.core.klondike

import com.maximx1.fads.core.model.Suit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One Klondike action. Piles are zero-based indexes. A card left face down on top of a pile turns by itself. */
@Serializable
sealed interface KlondikeMove {
    /** Turn the next one or three cards of the stock onto the waste. */
    @Serializable
    @SerialName("draw")
    data object Draw : KlondikeMove

    /** Turn the whole waste back over to make a new stock, once the stock is empty. */
    @Serializable
    @SerialName("recycle")
    data object Recycle : KlondikeMove

    @Serializable
    @SerialName("wh")
    data object WasteToFoundation : KlondikeMove

    @Serializable
    @SerialName("wp")
    data class WasteToPile(
        val to: Int,
    ) : KlondikeMove

    @Serializable
    @SerialName("ph")
    data class PileToFoundation(
        val from: Int,
    ) : KlondikeMove

    /** Move the top [count] face-up cards of pile [from], as a run, onto pile [to]. */
    @Serializable
    @SerialName("pp")
    data class PileToPile(
        val from: Int,
        val to: Int,
        val count: Int = 1,
    ) : KlondikeMove

    /** Take the top card of [suit]'s foundation back, onto pile [to]. */
    @Serializable
    @SerialName("hp")
    data class FoundationToPile(
        val suit: Suit,
        val to: Int,
    ) : KlondikeMove
}
