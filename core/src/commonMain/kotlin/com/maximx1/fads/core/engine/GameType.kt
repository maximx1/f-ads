package com.maximx1.fads.core.engine

import com.maximx1.fads.core.rules.FreeCellRules
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

    companion object {
        val all: List<GameType> get() = listOf(FreeCell)

        fun fromKey(key: String): GameType? = all.find { it.key == key }
    }
}
