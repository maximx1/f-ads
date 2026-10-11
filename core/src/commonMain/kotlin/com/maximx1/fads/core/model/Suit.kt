package com.maximx1.fads.core.model

enum class SuitColor { BLACK, RED }

enum class Suit(
    val color: SuitColor,
    val symbol: Char,
) {
    SPADES(SuitColor.BLACK, '♠'),
    HEARTS(SuitColor.RED, '♥'),
    DIAMONDS(SuitColor.RED, '♦'),
    CLUBS(SuitColor.BLACK, '♣'),
}
