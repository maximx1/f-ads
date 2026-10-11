package com.maximx1.fads.core.model

enum class SuitColor { BLACK, RED }

/**
 * The order of the suits is part of the deal algorithm (see [Card.deck]).
 * Reordering them changes every numbered deal.
 */
enum class Suit(
    val color: SuitColor,
    val symbol: Char,
    val letter: Char,
) {
    SPADES(SuitColor.BLACK, '♠', 'S'),
    HEARTS(SuitColor.RED, '♥', 'H'),
    DIAMONDS(SuitColor.RED, '♦', 'D'),
    CLUBS(SuitColor.BLACK, '♣', 'C'),
}
