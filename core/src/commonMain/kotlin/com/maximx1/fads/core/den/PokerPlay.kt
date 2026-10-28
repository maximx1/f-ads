package com.maximx1.fads.core.den

import com.maximx1.fads.core.den.Said.Word
import com.maximx1.fads.core.model.Card
import kotlin.math.min

/**
 * Deals hand [number] of [game] to [seats], with the button at [button]. Seats with nothing in front of them sit it
 * out. In draw each player antes [stake]; in Hold'em the two after the button post blinds of [stake] and twice it,
 * the button itself posting the small one when only two play. Bets start at twice [stake].
 */
fun dealPoker(
    game: PokerGame,
    number: Long,
    seats: List<Seat>,
    button: Int,
    stake: Long,
): PokerHand {
    val playing = seats.indices.filter { seats[it].stack > 0 }
    require(playing.size >= 2) { "Poker needs two players with something to bet" }
    require(button in playing) { "The button must be at a seat that plays" }
    var hand =
        PokerHand(
            game = game,
            number = number,
            stake = stake,
            seats = seats,
            button = button,
            hole = seats.map { emptyList() },
            dealt = 0,
            street = if (game == PokerGame.DRAW) Street.BEFORE_DRAW else Street.PREFLOP,
            bets = seats.map { 0L },
            paid = seats.map { 0L },
            toAct = null,
            raiseBy = stake * 2,
            folded = seats.indices.toSet() - playing.toSet(),
        )
    // Round the table from the seat after the button, a card at a time.
    val order = generateSequence(hand.after(button, playing)) { hand.after(it, playing) }.take(playing.size).toList()
    val deck = hand.deck
    val each = PokerHand.cardsEach(game)
    val hole = seats.indices.map { mutableListOf<Card>() }
    repeat(each) { round -> order.forEachIndexed { at, seat -> hole[seat] += deck[round * order.size + at] } }
    hand = hand.copy(hole = hole, dealt = each * order.size)

    if (game == PokerGame.DRAW) {
        // Draw opens with the seat after the button.
        return playing
            .fold(
                hand,
            ) { posted, seat -> posted.put(seat, stake, Word.ANTE, intoBets = false) }
            .openRound(button)
    }
    // Hold'em opens with the seat after the big blind, who has the last word before the flop.
    val small = if (playing.size == 2) button else order.first()
    val big = hand.after(small, playing) ?: error("Two seats play")
    return hand.put(small, stake, Word.BLIND).put(big, stake * 2, Word.BLIND).openRound(big)
}

/** True if it is someone's turn and [move] is one they can make. */
fun PokerHand.canMove(move: PokerMove): Boolean {
    val seat = toAct ?: return false
    val stack = seats[seat].stack
    return when (move) {
        PokerMove.Fold -> {
            street != Street.DRAW
        }

        PokerMove.Check -> {
            street != Street.DRAW && owed(seat) == 0L
        }

        PokerMove.Call -> {
            street != Street.DRAW && owed(seat) > 0
        }

        is PokerMove.Raise -> {
            val adds = move.to - bets[seat]
            street != Street.DRAW && move.to > topBet && adds <= stack && (move.to >= leastRaise || adds == stack)
        }

        is PokerMove.Draw -> {
            street == Street.DRAW && canDraw(seat, move.discards)
        }
    }
}

/** The hand after the player whose turn it is makes [move]. Throws [IllegalArgumentException] if they cannot. */
fun PokerHand.play(move: PokerMove): PokerHand {
    require(canMove(move)) { "$move cannot be played now" }
    val seat = toAct ?: error("Somebody acts")
    val played =
        when (move) {
            PokerMove.Fold -> copy(folded = folded + seat).say(seat, Said(Word.FOLD))
            PokerMove.Check -> say(seat, Said(Word.CHECK))
            PokerMove.Call -> put(seat, min(owed(seat), seats[seat].stack), Word.CALL)
            is PokerMove.Raise -> raise(seat, move.to)
            is PokerMove.Draw -> draw(seat, move.discards)
        }
    return played.copy(acted = played.acted + seat, moves = moves + 1).next(seat)
}

/**
 * This hand with [seat] out of it, whether or not it is their turn, as when they get up from the table. If they
 * were the last but one, it ends.
 */
internal fun PokerHand.foldOut(seat: Int): PokerHand {
    val out = copy(folded = folded + seat).say(seat, Said(Word.FOLD))
    return when {
        out.live.size == 1 -> out.finish()
        toAct == seat -> out.copy(acted = acted + seat).next(seat)
        else -> out
    }
}

// True if [seat] can throw away the cards at [discards]: up to three, or four if the one kept is an ace.
private fun PokerHand.canDraw(
    seat: Int,
    discards: Set<Int>,
): Boolean {
    val cards = hole[seat]
    if (discards.any { it !in cards.indices }) return false
    val keepsAnAce = (cards.indices - discards).any { cards[it].pokerValue == ACE }
    return discards.size <= MOST_DRAWN || (discards.size == MOST_DRAWN + 1 && keepsAnAce)
}

private fun PokerHand.draw(
    seat: Int,
    discards: Set<Int>,
): PokerHand {
    val fresh = deck.drop(dealt).iterator()
    val cards = hole[seat].mapIndexed { index, card -> if (index in discards) fresh.next() else card }
    return copy(hole = hole.toMutableList().also { it[seat] = cards }, dealt = dealt + discards.size)
        .say(seat, Said(Word.DRAW, discards.size.toLong()))
}

private fun PokerHand.raise(
    seat: Int,
    to: Long,
): PokerHand {
    val raised = to - topBet
    val word = if (topBet == 0L) Word.BET else Word.RAISE
    val put = put(seat, to - bets[seat], word)
    // A full raise reopens the betting and sets the size of the next one; everyone else must answer it.
    return put.copy(raiseBy = maxOf(raiseBy, raised), acted = emptySet())
}

/** This hand with [amount] more from [seat]'s stack, as far as it goes, and what they said doing it. */
internal fun PokerHand.put(
    seat: Int,
    amount: Long,
    word: Word,
    intoBets: Boolean = true,
): PokerHand {
    val taken = min(amount, seats[seat].stack)
    val left = seats[seat].stack - taken
    val shown = if (left == 0L && word != Word.ANTE && word != Word.BLIND) Word.ALL_IN else word
    return copy(
        seats = seats.toMutableList().also { it[seat] = seats[seat].copy(stack = left) },
        bets = if (intoBets) bets.toMutableList().also { it[seat] += taken } else bets,
        paid = paid.toMutableList().also { it[seat] += taken },
    ).say(seat, Said(shown, if (intoBets) bets[seat] + taken else taken))
}

internal fun PokerHand.say(
    seat: Int,
    said: Said,
): PokerHand = copy(said = this.said.toMutableList().also { it[seat] = said })

private const val ACE = 14
private const val MOST_DRAWN = 3
