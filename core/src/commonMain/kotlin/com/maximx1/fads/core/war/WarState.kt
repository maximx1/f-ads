package com.maximx1.fads.core.war

import com.maximx1.fads.core.model.Card
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One War action. There is only one: both players turn over their top card. */
@Serializable
sealed interface WarMove {
    /** Gather in the last battle, if there was one, then fight the next. */
    @Serializable
    @SerialName("flip")
    data object Flip : WarMove
}

/** The two players. */
enum class Side { YOU, THEM }

/**
 * The cards each side laid in one battle, in the order they were laid, and who took them. The first card of each
 * is face up; in a war each side lays three face down and a fourth face up, or, short of cards, all but its last
 * face down and that last one face up. [winner] takes them all.
 */
data class Battle(
    val yours: List<Card>,
    val theirs: List<Card>,
    val winner: Side,
) {
    /** True if the battle went to war: the first cards were a tie. */
    val wasWar: Boolean get() = yours.size > 1 || theirs.size > 1

    /** Every card in the battle. */
    val cards: List<Card> get() = yours + theirs

    /** How many cards [side] holds once this battle is gathered in. */
    fun takenBy(side: Side): Int = if (winner == side) yours.size + theirs.size else 0

    fun laidBy(side: Side): List<Card> = if (side == Side.YOU) yours else theirs

    companion object {
        /** True if the card at [index] of a side's [laid] cards lies face up. */
        fun isFaceUp(
            index: Int,
            laid: List<Card>,
        ): Boolean = index % CARDS_PER_WAR == 0 || index == laid.lastIndex

        /** Cards each side lays in a war: three face down and one face up. */
        const val CARDS_PER_WAR = 4
    }
}

/**
 * One immutable position of a game of War against the computer.
 *
 * @property dealNumber the deal, which also decides the order the cards of each battle go under the winner's pile.
 * @property yours your pile, face down, the top card first.
 * @property theirs the computer's pile, the same way.
 * @property battle the last battle, still on the table until the next flip gathers it in.
 * @property battles how many battles have been fought.
 */
data class WarState(
    val dealNumber: Long,
    val yours: List<Card>,
    val theirs: List<Card>,
    val battle: Battle? = null,
    val battles: Int = 0,
) {
    /** Every card [side] holds: its pile and its share of the battle on the table. */
    fun held(side: Side): Int = pile(side).size + (battle?.takenBy(side) ?: 0)

    fun pile(side: Side): List<Card> = if (side == Side.YOU) yours else theirs

    /** You hold every card. */
    val isWon: Boolean get() = held(Side.THEM) == 0

    /** The computer holds every card. */
    val isLost: Boolean get() = held(Side.YOU) == 0

    val isOver: Boolean get() = isWon || isLost
}
