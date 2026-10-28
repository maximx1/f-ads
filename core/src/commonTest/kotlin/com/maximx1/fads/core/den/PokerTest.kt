package com.maximx1.fads.core.den

import com.maximx1.fads.core.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PokerTest {
    private fun cards(codes: String) = codes.split(' ').map(Card::fromCode)

    private fun rank(codes: String) = HandRank.best(cards(codes))

    private fun seats(vararg stacks: Long) = stacks.mapIndexed { seat, stack -> Seat("P$seat", stack, bot = seat != 0) }

    /** [hand] with these cards in each seat's hand, and on the board. */
    private fun PokerHand.holding(
        vararg hands: String,
        board: String = "",
    ) = copy(hole = hands.map(::cards), board = if (board.isEmpty()) emptyList() else cards(board))

    @Test
    fun handsRankFromHighCardToStraightFlush() {
        val ladder =
            listOf(
                "KS JD 8C 5H 2S",
                "8S 8D KC 5H 2S",
                "8S 8D 5C 5H 2S",
                "8S 8D 8C 5H 2S",
                "6S 7D 8C 9H 10S",
                "KH JH 8H 5H 2H",
                "8S 8D 8C 5H 5S",
                "8S 8D 8C 8H 2S",
                "9H 10H JH QH KH",
            )
        assertEquals(HandCategory.entries, ladder.map { rank(it).category })
        assertEquals(ladder.map(::rank), ladder.map(::rank).sorted())
        // The ace-to-five straight is the lowest, and an ace can top one too.
        assertEquals(HandRank(HandCategory.STRAIGHT, listOf(5)), rank("AS 2D 3C 4H 5S"))
        assertTrue(rank("AS 2D 3C 4H 5S") < rank("2S 3D 4C 5H 6S"))
        assertTrue(rank("10S JD QC KH AS") > rank("9S 10D JC QH KS"))
        assertEquals(HandCategory.HIGH_CARD, rank("QS KD AC 2H 3S").category, "no wrapping round the ace")
        assertFailsWith<IllegalArgumentException> { HandRank.of(cards("AS KS")) }
    }

    @Test
    fun tiesGoToTheKickersAndSuitsNeverCount() {
        assertTrue(rank("AS AD KC 5H 2S") > rank("AH AC QC JH 10S"))
        assertTrue(rank("8S 8D 5C 5H AS") > rank("8H 8C 5S 5D KS"))
        assertEquals(0, rank("AS KD 9C 5H 2S").compareTo(rank("AH KC 9D 5S 2H")))
        // The best five of seven: here a flush beats the pair on the board.
        assertEquals(HandCategory.FLUSH, rank("2H 7H 9H JH QD QS 4H").category)
        assertEquals(21, fives(cards("2H 7H 9H JH QD QS 4H")).size)
    }

    @Test
    fun aDrawPlayerKeepsWhatItHasAndDrawsToWhatItCould() {
        assertEquals(emptySet(), botDiscards(cards("6S 7D 8C 9H 10S")))
        assertEquals(setOf(2, 3, 4), botDiscards(cards("8S 8D KC 5H 2S")))
        assertEquals(setOf(4), botDiscards(cards("8S 8D 5C 5H 2S")))
        assertEquals(setOf(1), botDiscards(cards("KH JS 8H 5H 2H")), "four to a flush")
        assertEquals(setOf(4), botDiscards(cards("6S 7D 8C 9H KS")), "four to a straight")
        assertEquals(setOf(2, 3, 4), botDiscards(cards("AS QD 8C 5H 2S")))
    }

    @Test
    fun drawAntesDealsFiveEachAndStartsAfterTheButton() {
        val hand = dealPoker(PokerGame.DRAW, 7, seats(50, 50, 50, 0), button = 1, stake = 1)
        assertEquals(listOf(5, 5, 5, 0), hand.hole.map { it.size })
        assertEquals(setOf(3), hand.folded, "the seat with nothing sits out")
        assertEquals(listOf(1L, 1L, 1L, 0L), hand.paid)
        assertEquals(listOf(0L, 0L, 0L, 0L), hand.bets)
        assertEquals(2, hand.toAct)
        assertEquals(15, hand.dealt)
        assertEquals(
            15,
            hand.hole
                .flatten()
                .toSet()
                .size,
        )
        assertEquals(hand, dealPoker(PokerGame.DRAW, 7, seats(50, 50, 50, 0), button = 1, stake = 1))
        assertFailsWith<IllegalArgumentException> { dealPoker(PokerGame.DRAW, 7, seats(50, 0), 0, 1) }
    }

    @Test
    fun aDrawHandGoesRoundTwiceWithTheDrawBetweenAndTheBestHandTakesThePot() {
        var hand =
            dealPoker(PokerGame.DRAW, 3, seats(20, 20, 20), button = 0, stake = 1)
                .holding("AS AD KC 5H 2S", "8S 8D 5C 5H 3S", "KH JS 9H 4C 2H")
        assertEquals(1, hand.toAct)
        assertFalse(hand.canMove(PokerMove.Call), "nothing to call")
        assertFalse(hand.canMove(PokerMove.Draw(emptySet())), "not the draw yet")
        hand = hand.play(PokerMove.Raise(4))
        assertEquals(Said(Said.Word.BET, 4), hand.said[1])
        assertFalse(hand.canMove(PokerMove.Raise(5)), "a raise adds at least the bet before it")
        hand = hand.play(PokerMove.Fold).play(PokerMove.Call)
        assertEquals(Street.DRAW, hand.street)
        assertEquals(1, hand.toAct)

        assertFalse(hand.canMove(PokerMove.Draw(setOf(0, 1, 2, 3))), "four only when keeping an ace")
        hand = hand.play(PokerMove.Draw(setOf(4)))
        assertEquals(Said(Said.Word.DRAW, 1), hand.said[1])
        assertTrue(hand.canMove(PokerMove.Draw(setOf(1, 2, 3, 4))), "keeping the ace")
        hand = hand.play(PokerMove.Draw(setOf(2, 3, 4)))
        assertEquals(Street.AFTER_DRAW, hand.street)
        assertEquals(19, hand.dealt)

        hand = hand.play(PokerMove.Check).play(PokerMove.Check)
        assertTrue(hand.isOver)
        val outcome = assertNotNull(hand.outcome)
        assertEquals(11, outcome.won.sum(), "three antes and two bets of four")
        assertEquals(setOf(0, 1), outcome.shown)
        val winner = outcome.winners.single()
        assertEquals(listOf(0, 1).maxBy { hand.rankOf(it) }, winner)
        assertEquals(60, hand.seats.sumOf { it.stack }, "no bronze lost or made")
        assertNull(hand.toAct)
        assertFailsWith<IllegalArgumentException> { hand.play(PokerMove.Check) }
    }

    @Test
    fun holdemPostsBlindsAndTheBigBlindHasTheLastWordBeforeTheFlop() {
        var hand = dealPoker(PokerGame.HOLDEM, 11, seats(40, 40, 40, 40), button = 0, stake = 1)
        assertEquals(listOf(0L, 1L, 2L, 0L), hand.bets)
        assertEquals(listOf(2, 2, 2, 2), hand.hole.map { it.size })
        assertEquals(3, hand.toAct, "after the big blind")
        hand = hand.play(PokerMove.Call).play(PokerMove.Call).play(PokerMove.Call)
        assertEquals(2, hand.toAct, "the big blind may still raise")
        assertTrue(hand.canMove(PokerMove.Check))
        hand = hand.play(PokerMove.Check)
        assertEquals(Street.FLOP, hand.street)
        assertEquals(3, hand.board.size)
        assertEquals(1, hand.toAct, "after the button")
        repeat(4) { hand = hand.play(PokerMove.Check) }
        assertEquals(Street.TURN, hand.street)
        repeat(4) { hand = hand.play(PokerMove.Check) }
        assertEquals(5, hand.board.size)
        repeat(4) { hand = hand.play(PokerMove.Check) }
        assertTrue(hand.isOver)
        assertEquals(8, assertNotNull(hand.outcome).won.sum())
    }

    @Test
    fun withTwoPlayersTheButtonPostsTheSmallBlindAndFoldingRoundToOneWinsUnseen() {
        var hand = dealPoker(PokerGame.HOLDEM, 2, seats(30, 30), button = 1, stake = 1)
        assertEquals(listOf(2L, 1L), hand.bets)
        assertEquals(1, hand.toAct)
        hand = hand.play(PokerMove.Raise(6)).play(PokerMove.Fold)
        val outcome = assertNotNull(hand.outcome)
        assertEquals(listOf(0L, 8L), outcome.won)
        assertTrue(outcome.shown.isEmpty())
        assertEquals(listOf(28L, 32L), hand.seats.map { it.stack })
    }

    @Test
    fun allInsForDifferentAmountsMakeSidePots() {
        var hand =
            dealPoker(PokerGame.HOLDEM, 5, seats(100, 10, 30), button = 0, stake = 1)
                .holding("7C 2D", "AS AH", "KS KH")
        hand = hand.play(PokerMove.Raise(100))
        assertEquals(Said(Said.Word.ALL_IN, 100), hand.said[0])
        hand = hand.play(PokerMove.Call).play(PokerMove.Call)
        // Nobody can bet any more, so the board is dealt out to the end.
        assertTrue(hand.isOver)
        assertEquals(5, hand.board.size)
        // The main pot of 30 to the best of all three; the side pot of 40 to the better of the two bigger stacks;
        // and the 70 nobody could match back to the big stack.
        val expected = MutableList(3) { 0L }
        expected[(0..2).maxBy { hand.rankOf(it) }] += 30
        expected[listOf(0, 2).maxBy { hand.rankOf(it) }] += 40
        expected[0] += 70
        assertEquals(expected, assertNotNull(hand.outcome).won)
        assertEquals(140, hand.seats.sumOf { it.stack })
    }

    @Test
    fun aTiedPotIsSharedAndTheOddBronzeGoesFirstAfterTheButton() {
        var hand =
            dealPoker(PokerGame.HOLDEM, 9, seats(20, 20, 20), button = 0, stake = 1)
                .holding("2C 3D", "2D 3C", "4S 7H")
        hand = hand.play(PokerMove.Call).play(PokerMove.Call).play(PokerMove.Check)
        repeat(6) { hand = hand.play(PokerMove.Check) }
        assertEquals(Street.RIVER, hand.street)
        // A board nobody's cards improve on, so every hand plays it and the pot of 6 is split three ways.
        hand = hand.copy(board = cards("AS KS QH JD 9C"))
        repeat(3) { hand = hand.play(PokerMove.Check) }
        assertEquals(listOf(2L, 2L, 2L), assertNotNull(hand.outcome).won)

        // Seven between two goes four to the one first after the button.
        val odd = hand.copy(outcome = null, folded = setOf(2), paid = listOf(3L, 3L, 1L), button = 1).finish()
        assertEquals(listOf(4L, 3L, 0L), assertNotNull(odd.outcome).won)
    }

    @Test
    fun theDensPlayersAlwaysMakeAMoveTheyCanAndFinishEveryHand() {
        for (number in 0L until 12L) {
            for (game in PokerGame.entries) {
                var hand = dealPoker(game, number, seats(60, 60, 60, 60).map { it.copy(bot = true) }, 0, 1)
                var turns = 0
                while (!hand.isOver) {
                    val move = hand.botMove()
                    assertTrue(hand.canMove(move), "$move in hand $number of $game")
                    hand = hand.play(move)
                    assertTrue(++turns < 200, "hand $number of $game never ends")
                }
                assertEquals(240, hand.seats.sumOf { it.stack })
                assertEquals(
                    hand,
                    generateSequence(dealPoker(game, number, seats(60, 60, 60, 60).map { it.copy(bot = true) }, 0, 1)) {
                        if (it.isOver) null else it.play(it.botMove())
                    }.last(),
                    "the same every time",
                )
            }
        }
    }

    @Test
    fun aStrongHandIsWorthMoreThanAWeakOne() {
        val hand =
            dealPoker(PokerGame.HOLDEM, 1, seats(50, 50), button = 0, stake = 1)
                .holding("AS AH", "7C 2D")
        val random =
            com.maximx1.fads.core.deal
                .SplitMix64(1)
        // Against cards it cannot see, a pair of aces wins most of the time and a seven and a two a third of it.
        val aces = hand.equity(0, random, 300)
        val sevenTwo = hand.equity(1, random, 300)
        assertTrue(aces > 0.75, "aces $aces")
        assertTrue(sevenTwo in 0.2..0.45, "seven-two $sevenTwo")
    }

    @Test
    fun theDenSeatsThreePlayersAndThePlayerPlaysWithTheirPurse() {
        var den = Den(purse = Purse(200)).sitDown(PokerGame.DRAW, 4)
        val table = assertNotNull(den.table(PokerGame.DRAW))
        assertEquals(4, table.seats.size)
        assertTrue(table.seats.drop(1).all { it.bot && it.stack in 100..300 })
        assertEquals(listOf("Lucky Lou", "Duchess", "Big Sal"), table.seats.drop(1).map { it.name })
        assertNull(den.table(PokerGame.HOLDEM))
        assertEquals(den, den.sitDown(PokerGame.DRAW, 99), "already sitting")

        den = den.dealHand(PokerGame.DRAW, 8)
        assertEquals(Purse(), den.purse, "the purse is on the table")
        assertTrue(assertNotNull(den.table(PokerGame.DRAW)).playing)
        assertFailsWith<IllegalArgumentException> { den.dealHand(PokerGame.DRAW, 9) }
        val before = den.table(PokerGame.DRAW)!!.seats.sumOf { it.stack } + den.table(PokerGame.DRAW)!!.hand!!.pot

        // The player folds as soon as it is their turn, and the den's players play out the rest.
        var turns = 0
        while (den.table(PokerGame.DRAW)!!.playing) {
            den =
                if (den.playersTurn(
                        PokerGame.DRAW,
                    )
                ) {
                    den.playPoker(PokerGame.DRAW, PokerMove.Fold)
                } else {
                    den.playBot(PokerGame.DRAW)
                }
            assertTrue(++turns < 100)
        }
        val after = den.table(PokerGame.DRAW)!!
        assertEquals(200 - 1, den.purse.total, "the ante is all the player lost")
        assertEquals(before, after.seats.drop(1).sumOf { it.stack } + den.purse.total)
        assertEquals(1, after.button)
        assertFalse(den.botsTurn(PokerGame.DRAW))
        assertFailsWith<IllegalArgumentException> { den.playBot(PokerGame.DRAW) }
        assertFailsWith<IllegalArgumentException> { den.playPoker(PokerGame.DRAW, PokerMove.Check) }
        assertEquals(den, Den.fromJson(den.toJson()))
    }

    @Test
    fun leavingInTheMiddleOfAHandFoldsItAndBrokePlayersAreReplaced() {
        var den = Den(purse = Purse(50)).sitDown(PokerGame.HOLDEM, 1).dealHand(PokerGame.HOLDEM, 3)
        while (den.botsTurn(PokerGame.HOLDEM)) den = den.playBot(PokerGame.HOLDEM)
        den = den.leaveTable(PokerGame.HOLDEM)
        val table = assertNotNull(den.table(PokerGame.HOLDEM))
        assertFalse(table.playing)
        assertTrue(den.purse.total in 48..50, "at most the big blind lost")
        assertEquals(den, den.leaveTable(PokerGame.HOLDEM), "nothing to leave")

        // A player of the den's with nothing left gets up for a new one before the next deal.
        val broke =
            table.copy(
                seats =
                    table.seats.mapIndexed { seat, player ->
                        if (seat ==
                            2
                        ) {
                            player.copy(stack = 0)
                        } else {
                            player
                        }
                    },
            )
        val dealt = den.copy(holdem = broke).dealHand(PokerGame.HOLDEM, 4).table(PokerGame.HOLDEM)!!
        assertEquals("Ivan", dealt.seats[2].name)
        assertTrue(dealt.seats[2].stack + dealt.hand!!.paid[2] >= PokerTable.LEAST_BOT_STACK / 2)
        assertFailsWith<IllegalArgumentException> { Den().sitDown(PokerGame.DRAW, 1).dealHand(PokerGame.DRAW, 1) }
    }
}
