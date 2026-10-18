package com.maximx1.fads.core.engine

import com.maximx1.fads.core.klondike.KlondikeRules
import com.maximx1.fads.core.klondike.KlondikeState
import com.maximx1.fads.core.rules.FreeCellRules
import com.maximx1.fads.core.spider.SpiderRules
import com.maximx1.fads.core.spider.SpiderState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The games there are, each with whatever option changes how it plays. */
@Serializable
sealed interface GameType {
    /** A name for the game and its option that is safe in a file name, and is what gets saved. */
    val key: String

    val rules: Rules<*, *>

    /** A fresh game of this type on deal [dealNumber]. */
    fun start(dealNumber: Long): Game<*, *>

    /** A saved game of this type, or `null` if the text is not one or its moves do not replay. */
    fun load(text: String): Game<*, *>?

    @Serializable
    @SerialName("freecell")
    data object FreeCell : GameType {
        override val key get() = "freecell"
        override val rules get() = FreeCellRules

        override fun start(dealNumber: Long) = Game.start(FreeCellRules, dealNumber)

        override fun load(text: String) = Game.fromJson(FreeCellRules, text)
    }

    /** Klondike, turning [draw] cards from the stock at a time: 1 or 3. */
    @Serializable
    @SerialName("klondike")
    data class Klondike(
        val draw: Int,
    ) : GameType {
        init {
            require(draw == 1 || draw == KlondikeState.DRAW_THREE) { "Draw 1 or 3, not $draw" }
        }

        override val key get() = "klondike-$draw"
        override val rules get() = KlondikeRules.forDraw(draw)

        override fun start(dealNumber: Long) = Game.start(rules, dealNumber)

        override fun load(text: String) = Game.fromJson(rules, text)
    }

    /** Spider, played with [suits] suits: 1 (all spades), 2 (spades and hearts) or 4. */
    @Serializable
    @SerialName("spider")
    data class Spider(
        val suits: Int,
    ) : GameType {
        init {
            require(suits in SpiderState.SUIT_CHOICES) { "Spider is played with 1, 2 or 4 suits, not $suits" }
        }

        override val key get() = "spider-$suits"
        override val rules get() = SpiderRules.forSuits(suits)

        override fun start(dealNumber: Long) = Game.start(rules, dealNumber)

        override fun load(text: String) = Game.fromJson(rules, text)
    }

    companion object {
        val all: List<GameType>
            get() =
                listOf(FreeCell, Klondike(1), Klondike(KlondikeState.DRAW_THREE)) +
                    SpiderState.SUIT_CHOICES.map(::Spider)

        fun fromKey(key: String): GameType? = all.find { it.key == key }
    }
}
