package com.maximx1.fads.core.den

import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank

/** The kinds of poker hand, weakest first. */
enum class HandCategory {
    HIGH_CARD,
    ONE_PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
}

/**
 * How strong a five-card poker hand is: its [category], and then its [kickers], the values that settle a tie
 * between two of the same category, most telling first. Aces count 14, or 5 at the top of a straight from the ace
 * to the five. Hands compare by category and then by kickers, so the stronger hand is the greater.
 */
data class HandRank(
    val category: HandCategory,
    val kickers: List<Int>,
) : Comparable<HandRank> {
    override fun compareTo(other: HandRank): Int =
        listOf(category.compareTo(other.category))
            .plus(kickers.zip(other.kickers) { mine, theirs -> mine.compareTo(theirs) })
            .firstOrNull { it != 0 } ?: 0

    companion object {
        const val HAND_SIZE = 5

        /** The rank of exactly five [cards]. */
        fun of(cards: List<Card>): HandRank {
            require(cards.size == HAND_SIZE) { "A poker hand is five cards, not ${cards.size}" }
            // The values, most of a kind first and the highest first among kinds of the same size.
            val groups =
                cards
                    .groupingBy { it.pokerValue }
                    .eachCount()
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
            val counts = groups.map { it.value }
            val values = groups.map { it.key }
            val flush = cards.all { it.suit == cards.first().suit }
            val straight = straightHigh(values)
            val category =
                when {
                    straight != null && flush -> HandCategory.STRAIGHT_FLUSH
                    counts.first() == FOUR -> HandCategory.FOUR_OF_A_KIND
                    counts == listOf(THREE, 2) -> HandCategory.FULL_HOUSE
                    flush -> HandCategory.FLUSH
                    straight != null -> HandCategory.STRAIGHT
                    counts.first() == THREE -> HandCategory.THREE_OF_A_KIND
                    counts == listOf(2, 2, 1) -> HandCategory.TWO_PAIR
                    counts.first() == 2 -> HandCategory.ONE_PAIR
                    else -> HandCategory.HIGH_CARD
                }
            return HandRank(category, if (straight != null) listOf(straight) else values)
        }

        /** The rank of the best five of [cards], which may be five to seven. */
        fun best(cards: List<Card>): HandRank = fives(cards).maxOf { of(it) }

        // The top of the straight five different [values] make, or `null` if they make none.
        private fun straightHigh(values: List<Int>): Int? {
            if (values.size != HAND_SIZE) return null
            val sorted = values.sortedDescending()
            return when {
                sorted.first() - sorted.last() == HAND_SIZE - 1 -> sorted.first()
                sorted == WHEEL -> WHEEL_HIGH
                else -> null
            }
        }

        private val WHEEL = listOf(ACE_HIGH, 5, 4, 3, 2)
        private const val WHEEL_HIGH = 5
        private const val FOUR = 4
        private const val THREE = 3
    }
}

/** A card's value in poker: 2 to 10, then 11 for the jack up to 14 for the ace. */
val Card.pokerValue: Int get() = if (rank == Rank.ACE) ACE_HIGH else rank.value

/** Every way of picking five of [cards], in the order they come. */
internal fun fives(cards: List<Card>): List<List<Card>> {
    require(cards.size >= HandRank.HAND_SIZE) { "Five cards are needed, not ${cards.size}" }
    val picks = mutableListOf<List<Card>>()

    fun pick(
        from: Int,
        chosen: List<Card>,
    ) {
        if (chosen.size == HandRank.HAND_SIZE) {
            picks += chosen
            return
        }
        for (index in from..cards.size - (HandRank.HAND_SIZE - chosen.size)) pick(index + 1, chosen + cards[index])
    }
    pick(0, emptyList())
    return picks
}

private const val ACE_HIGH = 14
