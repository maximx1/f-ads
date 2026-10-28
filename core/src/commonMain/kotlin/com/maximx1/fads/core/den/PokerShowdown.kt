package com.maximx1.fads.core.den

import kotlin.math.min

/** The best hand [seat] makes from its own cards and the board. */
fun PokerHand.rankOf(seat: Int): HandRank = HandRank.best(hole[seat] + board)

/**
 * The end of the hand: the pot goes out and the stacks take what they won. With only one player left in, it all
 * goes to them and nobody shows. Otherwise the players still in show, and the pot is split into a main pot and a
 * side pot for every player who went all in for less than the others: each goes to the best hand among those who
 * put in enough for it, shared between tied hands, an odd bronze going to the first of them after the button.
 */
internal fun PokerHand.finish(): PokerHand {
    val won = MutableList(seats.size) { 0L }
    val showdown = live.size > 1
    val ranks = if (showdown) live.associateWith { rankOf(it) } else emptyMap()
    var below = 0L
    for (level in paid.filter { it > 0 }.distinct().sorted()) {
        val slice = paid.sumOf { min(it, level) - min(it, below) }
        // Those still in who put in this much; were there none, the pot falls to whoever is left.
        val eligible = live.filter { paid[it] >= level }.ifEmpty { live }
        val best = if (showdown) eligible.maxOf { ranks.getValue(it) } else null
        val winners =
            eligible
                .filter {
                    best == null || ranks.getValue(it) == best
                }.sortedBy { (it - button - 1).mod(seats.size) }
        winners.forEachIndexed { index, seat ->
            won[seat] +=
                slice / winners.size + if (index < slice % winners.size) 1 else 0
        }
        below = level
    }
    return copy(
        seats = seats.mapIndexed { seat, player -> player.copy(stack = player.stack + won[seat]) },
        bets = bets.map { 0L },
        street = Street.SHOWDOWN,
        toAct = null,
        outcome = Outcome(won, ranks.keys, ranks.mapValues { it.value.category }),
    )
}
