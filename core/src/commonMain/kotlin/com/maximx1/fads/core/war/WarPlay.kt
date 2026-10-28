package com.maximx1.fads.core.war

import com.maximx1.fads.core.deal.SplitMix64
import com.maximx1.fads.core.deal.shuffledFor
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.core.model.Rank

/** War deal [number]: the deck shuffled for the number (see [shuffledFor]) and dealt one at a time, you first. */
fun warDeal(number: Long): WarState {
    val deck = Card.deck.shuffledFor(number)
    return WarState(
        dealNumber = number,
        yours = deck.filterIndexed { index, _ -> index % 2 == 0 },
        theirs = deck.filterIndexed { index, _ -> index % 2 == 1 },
    )
}

fun WarState.canMove(move: WarMove): Boolean =
    when (move) {
        WarMove.Flip -> !isOver
    }

/**
 * Returns the position after [move]: the last battle goes under its winner's pile, and the next one is fought.
 * Throws [IllegalArgumentException] if the game is over.
 */
fun WarState.play(move: WarMove): WarState {
    require(canMove(move)) { "Illegal move: $move" }
    return when (move) {
        WarMove.Flip -> gathered().fight()
    }
}

/**
 * The cards of [WarState.battle] under the piles of whoever took them, in an order drawn from the deal number and
 * the battle. Always gathering them in the same order can leave a game going round for ever.
 */
private fun WarState.gathered(): WarState {
    val last = battle ?: return this
    val random = battleRandom()

    fun shuffled(cards: List<Card>) = cards.shuffledBy(random)
    return when (last.winner) {
        Side.YOU -> copy(yours = yours + shuffled(last.cards), battle = null)
        Side.THEM -> copy(theirs = theirs + shuffled(last.cards), battle = null)
    }
}

/**
 * Both turn over their top card and the higher takes both, aces high. A tie is a war: each lays three face down
 * and another face up, and the higher of those takes the lot, going to war again on another tie. A side that runs
 * short lays what it has, its last card face up; one with no card left to lay loses the war. If both run out
 * together, a toss drawn from the deal number and the battle settles it, or two lone sevens would tie for ever.
 */
private fun WarState.fight(): WarState {
    val mine = yours.toMutableList()
    val its = theirs.toMutableList()
    val laidMine = ArrayList<Card>()
    val laidIts = ArrayList<Card>()
    while (true) {
        laidMine += mine.removeAt(0)
        laidIts += its.removeAt(0)
        val difference = laidMine.last().rank.compareAces(laidIts.last().rank)
        if (difference != 0 || mine.isEmpty() || its.isEmpty()) {
            val winner =
                when {
                    difference > 0 -> Side.YOU
                    difference < 0 -> Side.THEM
                    mine.isNotEmpty() -> Side.YOU
                    its.isNotEmpty() -> Side.THEM
                    battleRandom().nextBelow(2) == 0 -> Side.YOU
                    else -> Side.THEM
                }
            return WarState(dealNumber, mine, its, Battle(laidMine, laidIts, winner), battles + 1)
        }
        repeat(minOf(FACE_DOWN, mine.size - 1)) { laidMine += mine.removeAt(0) }
        repeat(minOf(FACE_DOWN, its.size - 1)) { laidIts += its.removeAt(0) }
    }
}

/** A random source for the battle being fought or gathered in, the same every time the game is replayed. */
private fun WarState.battleRandom() = SplitMix64(dealNumber xor (battles.toLong() * GATHER_SEED))

/** Positive if this rank beats [other], with aces high. */
private fun Rank.compareAces(other: Rank): Int = aceHigh() - other.aceHigh()

private fun Rank.aceHigh(): Int = if (this == Rank.ACE) Rank.entries.size else ordinal

private fun <T> List<T>.shuffledBy(random: SplitMix64): List<T> {
    val items = toMutableList()
    for (i in items.lastIndex downTo 1) {
        val j = random.nextBelow(i + 1)
        items[i] = items[j].also { items[j] = items[i] }
    }
    return items
}

private const val FACE_DOWN = 3

// Spreads the battles' seeds apart from one another and from the deal's own shuffle.
private const val GATHER_SEED = 0x5741525F47415448L
