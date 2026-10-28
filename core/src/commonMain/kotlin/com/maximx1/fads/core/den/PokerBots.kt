package com.maximx1.fads.core.den

import com.maximx1.fads.core.deal.SplitMix64
import com.maximx1.fads.core.model.Card

/**
 * The move the den makes for the player whose turn it is. It draws to what it holds, and bets by how often its
 * hand would win: it raises a strong hand, calls when the pot pays enough for its chances, and folds the rest,
 * with the odd bluff. The same hand at the same point always gets the same move.
 */
fun PokerHand.botMove(): PokerMove {
    val seat = toAct ?: error("Nobody is to act")
    val random = SplitMix64(number * SEED_STEP + moves)
    if (street == Street.DRAW) return PokerMove.Draw(botDiscards(hole[seat]))

    val equity = equity(seat, random, SIMULATIONS)
    val owed = owed(seat)
    // How the hand does against an even share of the pot among those still in.
    val strength = equity * live.size
    val roll = random.nextBelow(PERCENT)
    val bluffing = roll < BLUFF_PERCENT
    val potOdds = owed.toDouble() / (pot + owed)
    val move =
        when {
            strength >= RAISE_STRENGTH || (bluffing && owed * 2 <= pot) -> raiseBy(seat, roll)
            owed == 0L && strength >= BET_STRENGTH -> raiseBy(seat, roll)
            owed == 0L -> PokerMove.Check
            equity >= potOdds -> PokerMove.Call
            else -> PokerMove.Fold
        }
    return move.takeIf { canMove(it) } ?: if (owed == 0L) PokerMove.Check else PokerMove.Call
}

// A bet or raise of between half the pot and the whole of it, or all in if that is more than is left.
private fun PokerHand.raiseBy(
    seat: Int,
    roll: Int,
): PokerMove {
    val size = maxOf(raiseBy, pot * (HALF_POT + roll % HALF_POT) / PERCENT)
    val most = bets[seat] + seats[seat].stack
    val to = minOf(topBet + size, most)
    return if (to <= topBet) PokerMove.Call else PokerMove.Raise(to)
}

/**
 * How often [seat]'s hand wins, from 0 to 1, a tie counting half: [rounds] deals of the cards it cannot see, to
 * finish the board and to give each other player still in a hand. Before the draw it draws as [botDiscards] says.
 */
internal fun PokerHand.equity(
    seat: Int,
    random: SplitMix64,
    rounds: Int,
): Double {
    val mine = hole[seat]
    val unseen = Card.deck - (mine + board).toSet()
    val others = live - seat
    val discards = if (street == Street.BEFORE_DRAW) botDiscards(mine) else emptySet()
    var wins = 0.0
    repeat(rounds) {
        val cards = unseen.shuffledWith(random).iterator()

        fun take(count: Int) = List(count) { cards.next() }
        val fullBoard = board + take(if (game == PokerGame.HOLDEM) BOARD - board.size else 0)
        val drawn = mine.mapIndexed { index, card -> if (index in discards) cards.next() else card }
        val own = HandRank.best(drawn + fullBoard)
        val theirs = others.maxOf { HandRank.best(take(PokerHand.cardsEach(game)) + fullBoard) }
        wins +=
            own.compareTo(theirs).let {
                if (it > 0) {
                    1.0
                } else if (it == 0) {
                    HALF
                } else {
                    0.0
                }
            }
    }
    return wins / rounds
}

/**
 * The cards a player of five-card draw throws away, by place in the hand: none from a straight or better, the
 * odd card from four to a flush or a straight, the cards that pair nothing from a pair or more, and otherwise all
 * but the two highest.
 */
fun botDiscards(cards: List<Card>): Set<Int> {
    val category = HandRank.of(cards).category
    val values = cards.map { it.pokerValue }
    val counts = values.groupingBy { it }.eachCount()
    val flushSuit =
        cards
            .groupingBy { it.suit }
            .eachCount()
            .entries
            .firstOrNull { it.value == FOUR }
            ?.key
    val run = fourInARow(values.toSet())
    return when {
        category >= HandCategory.STRAIGHT -> {
            emptySet()
        }

        category >= HandCategory.ONE_PAIR -> {
            cards.indices.filter { counts.getValue(values[it]) == 1 }.toSet()
        }

        flushSuit != null -> {
            cards.indices.filter { cards[it].suit != flushSuit }.toSet()
        }

        run != null -> {
            cards.indices.filter { values[it] !in run }.toSet()
        }

        else -> {
            cards.indices
                .sortedByDescending { values[it] }
                .drop(2)
                .toSet()
        }
    }
}

// Four values running one after another, if [values] has them.
private fun fourInARow(values: Set<Int>): IntRange? =
    values.map { it..it + FOUR - 1 }.firstOrNull { run -> run.all { it in values } }

// A fresh shuffle of these cards, driven by [random].
private fun List<Card>.shuffledWith(random: SplitMix64): List<Card> {
    val cards = toMutableList()
    for (i in cards.lastIndex downTo 1) {
        val j = random.nextBelow(i + 1)
        cards[i] = cards[j].also { cards[j] = cards[i] }
    }
    return cards
}

private const val SIMULATIONS = 150
private const val SEED_STEP = 1_009L
private const val PERCENT = 100
private const val BLUFF_PERCENT = 8
private const val RAISE_STRENGTH = 1.6
private const val BET_STRENGTH = 1.25
private const val HALF_POT = 50
private const val HALF = 0.5
private const val BOARD = 5
private const val FOUR = 4
