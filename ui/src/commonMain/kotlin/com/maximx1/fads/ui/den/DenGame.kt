package com.maximx1.fads.ui.den

import com.maximx1.fads.ui.Strings

/** The games played in the den. */
enum class DenGame(
    val label: String,
) {
    FIVE_CARD_DRAW(Strings.FIVE_CARD_DRAW),
    TEXAS_HOLDEM(Strings.TEXAS_HOLDEM),
    ROULETTE(Strings.ROULETTE),
    PLINKO(Strings.PLINKO),
    SLOTS(Strings.SLOTS),
    VIDEO_POKER(Strings.VIDEO_POKER),
    BLACKJACK(Strings.BLACKJACK),
    BACCARAT(Strings.BACCARAT),
}
