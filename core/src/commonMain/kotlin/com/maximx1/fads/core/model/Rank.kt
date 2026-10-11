package com.maximx1.fads.core.model

/**
 * The order of the ranks is part of the deal algorithm (see [Card.deck]).
 * Reordering them changes every numbered deal.
 */
enum class Rank(
    val label: String,
) {
    ACE("A"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10"),
    JACK("J"),
    QUEEN("Q"),
    KING("K"),
    ;

    /** 1 for the ace up to 13 for the king. */
    val value: Int get() = ordinal + 1
}
