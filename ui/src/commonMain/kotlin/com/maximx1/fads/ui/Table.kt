package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Suit

/**
 * How one game is laid out on the screen. It turns a position into an [Arrangement] for an area of a given size,
 * and says which cards to dim.
 */
interface TableLayout<S : Any, M : Any> {
    fun arrange(
        state: S,
        width: Float,
        height: Float,
        density: Density,
    ): Arrangement<M>

    /** The cards that no legal move would move right now, by [PlacedCard.id]. None, unless a game says so. */
    fun stuck(state: S): Set<Int> = emptySet()
}

/**
 * One position laid out at one size, in pixels: where every card and empty place is, and which move a tap, a
 * pick-up and a drop make.
 */
interface Arrangement<M : Any> {
    val cardSize: Size

    val cards: List<PlacedCard>

    val slots: List<Slot<M>>

    /** The row along the top of the table, if the game has one, for a theme to set apart. */
    val header: Header? get() = null

    /** The move a tap on card [id] plays, or `null` if it plays none. */
    fun tap(id: Int): M?

    /** The cards that go with card [id] when it is picked up, lowest first. Empty if it cannot be picked up. */
    fun pickUp(id: Int): List<Int>

    /** The move that dropping [held] with its first card's centre at [centre] plays, or `null` if none. */
    fun drop(
        held: List<Int>,
        centre: Offset,
    ): M?

    /** The cards [move] would pick up, so a hint can mark them. The move must be legal. */
    fun movedBy(move: M): List<Int>

    /** Where the first card of [move] would come to rest. The move must be legal. */
    fun landing(move: M): Offset
}

/**
 * A card at its place: [topLeft] in pixels, and [height] in its pile, which decides what it is drawn over. [id]
 * tells it apart from every other card, even an identical one from a second deck, and [tag] names it to tests.
 */
class PlacedCard(
    val id: Int,
    val card: Card,
    val faceUp: Boolean,
    val topLeft: Offset,
    val height: Float,
    val tag: String = card.code,
)

/** An empty place's outline, a [mark] to say what goes there, and the move a tap on it plays, if any. */
class Slot<M : Any>(
    val topLeft: Offset,
    val mark: SlotMark? = null,
    val tap: M? = null,
)

sealed interface SlotMark {
    /** The foundation for [suit]. */
    data class OfSuit(
        val suit: Suit,
    ) : SlotMark

    /** An empty stock that turns the waste back over when tapped. */
    data object Recycle : SlotMark
}

/** The row along the top: where it ends, and the places across it where one group of slots gives way to another. */
class Header(
    val bottom: Float,
    val dividers: List<Float> = emptyList(),
)
