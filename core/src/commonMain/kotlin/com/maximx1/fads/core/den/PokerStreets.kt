package com.maximx1.fads.core.den

import com.maximx1.fads.core.den.Said.Word

// How a hand moves on: from player to player, from street to street, and on to the showdown.

// On from [seat]'s move: the next player's turn, the next street, or the end of the hand.
internal fun PokerHand.next(seat: Int): PokerHand {
    // Those still to act: in the draw, whoever has not drawn; in the betting, whoever has not answered the bet.
    val waiting =
        if (street == Street.DRAW) {
            live.filter { it !in acted }
        } else {
            actors
                .filter { it !in acted || bets[it] < topBet }
                .takeUnless {
                    actors.size == 1 &&
                        bets[actors.first()] >= topBet
                }.orEmpty()
        }
    return when {
        live.size == 1 -> finish()
        waiting.isEmpty() -> nextStreet()
        else -> copy(toAct = after(seat, waiting))
    }
}

// Starts a street's betting, or its drawing, with the first player after [from] who can, or moves on if none can.
internal fun PokerHand.openRound(from: Int): PokerHand {
    val drawing = street == Street.DRAW
    val players = if (drawing) live else actors
    val waiting = if (drawing) players else players.filter { bets[it] < topBet || it !in acted }
    val nobodyToBet = !drawing && (actors.size < 2 && actors.all { bets[it] >= topBet })
    return if (waiting.isEmpty() ||
        nobodyToBet
    ) {
        nextStreet()
    } else {
        copy(toAct = after(from, waiting), acted = emptySet())
    }
}

// The bets go into the pot and the next street is dealt, or the hand goes to the showdown.
private fun PokerHand.nextStreet(): PokerHand {
    val following = nextStreetOf(street)
    if (following == Street.SHOWDOWN) return finish()
    // What was said on the street before is done with, but a fold or an all in still stands.
    val standing = said.map { it?.takeIf { said -> said.word == Word.FOLD || said.word == Word.ALL_IN } }
    val cleared =
        copy(
            street = following,
            bets = bets.map { 0L },
            acted = emptySet(),
            raiseBy = stake * 2,
            toAct = null,
            said = standing,
        )
    val cards = boardCardsFor(following)
    val dealtBoard = cleared.copy(board = board + deck.subList(dealt, dealt + cards), dealt = dealt + cards)
    return dealtBoard.openRound(from = button)
}

private fun nextStreetOf(street: Street): Street =
    when (street) {
        Street.BEFORE_DRAW -> Street.DRAW
        Street.DRAW -> Street.AFTER_DRAW
        Street.PREFLOP -> Street.FLOP
        Street.FLOP -> Street.TURN
        Street.TURN -> Street.RIVER
        else -> Street.SHOWDOWN
    }

private fun boardCardsFor(street: Street): Int =
    when (street) {
        Street.FLOP -> FLOP
        Street.TURN, Street.RIVER -> 1
        else -> 0
    }

private const val FLOP = 3
