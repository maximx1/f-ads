package com.maximx1.fads.ui

import com.maximx1.fads.core.den.BaccaratSide
import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.core.den.BlackjackMove
import com.maximx1.fads.core.den.CrapsBet
import com.maximx1.fads.core.den.HandCategory
import com.maximx1.fads.core.den.HandResult
import com.maximx1.fads.core.den.Outcome
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.Said
import com.maximx1.fads.core.den.SicBoBet
import com.maximx1.fads.core.den.SicBoKind
import com.maximx1.fads.core.den.SymbolDiceGame

/** Every piece of text the game shows, in one place so that it can be translated one day. */
@Suppress("TooManyFunctions") // One for each piece of text with a number or a name in it.
internal object Strings {
    const val APP_NAME = "Fuck Ads"
    const val PICK_A_GAME = "Pick a game"
    const val PLAY = "Play"
    const val FREECELL = "FreeCell"
    const val SOLITAIRE = "Solitaire"
    const val SPIDER = "Spider"
    const val MAHJONG = "Mahjong"

    const val UNDO = "Undo"
    const val AUTO_COMPLETE = "Auto-complete"
    const val RESTART = "Restart"
    const val NEW_GAME = "New"
    const val HINT = "Hint"
    const val PAUSE = "Pause"

    const val PAUSED = "Paused"
    const val RESUME = "Resume"
    const val THEME = "Theme"
    const val THIS_GAME = "This game"
    const val RECORDS = "Records"
    const val SETTINGS = "Settings"
    const val DIM_STUCK_CARDS = "Dim cards that can't move"
    const val NEW_BEST_TIME = "A new best time!"
    const val NEW_HIGH_SCORE = "A new high score!"

    const val NEW_GAME_TITLE = "New game"
    const val DEAL_NUMBER = "Deal number"
    const val DEAL = "Deal"
    const val RANDOM_DEAL = "Random deal"
    const val CANCEL = "Cancel"

    const val RESTART_TITLE = "Restart this deal?"

    const val WIN_TITLE = "You won!"
    const val WIN_NEW_GAME = "New game"
    const val NEXT_GAME = "Next game"
    const val CLOSE = "Close"

    const val HISTORY = "History"
    const val BACK = "Back"
    const val SEARCH_DEALS = "Search deal numbers"
    const val ALL_GAMES = "All"
    const val WON_ONLY = "Won only"
    const val NO_GAMES_YET = "No games finished yet."
    const val NOTHING_FOUND = "No games match."
    const val WON = "Won"
    const val NOT_WON = "Not won"
    const val PLAY_AGAIN = "Play again"
    const val WATCH = "Watch"
    const val JUST_NOW = "Just now"
    const val CANNOT_WATCH = "This game can't be played back."
    const val TO_START = "Back to the start"
    const val STEP_BACK = "Step back"
    const val STEP_FORWARD = "Step forward"
    const val SPEED = "Speed"

    const val DEN = "Gambler's Den"
    const val ENTER_DEN = "Enter the Gambler's Den"
    const val DEN_TITLE = "The Gambler's Den"
    const val CASH_IN = "Cash in"
    const val NOTHING_TO_CASH_IN = "Win games of patience to earn points. Every 100 points make a bronze."
    const val TABLES = "Tables"
    const val FIVE_CARD_DRAW = "Five-card draw"
    const val TEXAS_HOLDEM = "Texas Hold'em"
    const val ROULETTE = "Roulette"
    const val PLINKO = "Plinko"
    const val VIDEO_POKER = "Video poker"
    const val BLACKJACK = "Blackjack"
    const val BACCARAT = "Baccarat"
    const val CRAPS = "Craps"
    const val SIC_BO = "Sic bo"
    const val CHUCK_A_LUCK = "Chuck-a-luck"
    const val CROWN_AND_ANCHOR = "Crown and anchor"
    const val SYMBOL_ODDS = "1 to 1 a die"
    const val ROLL = "Roll"
    const val ON = "ON"
    const val POINT_OFF = "No point yet: bet the line and roll"
    const val DEALER = "Dealer"
    const val BLACKJACK_RULES = "Blackjack pays 3 to 2 · Dealer stands on 17"
    const val HELD = "HELD"
    const val DRAW = "Draw"
    const val SLOTS = "Slot machine"
    const val DROP = "Drop"
    const val SPIN = "Spin"
    const val CLEAR = "Clear"
    const val REBET = "Rebet"
    const val COLUMN_BET = "2:1"
    const val RED = "Red"
    const val BLACK = "Black"
    const val GREEN = "Green"
    const val HIGH_ROLLERS = "The High Rollers' Room"
    const val ENTER = "Enter"
    const val THEMES = "Themes"
    const val SHOP = "Shop"
    const val BUY = "Buy"
    const val IN_USE = "In use"
    const val OWNED = "Owned"
    const val YOU = "You"
    const val FOLD = "Fold"
    const val CHECK = "Check"
    const val STAND_PAT = "Stand pat"
    const val ALL_IN = "All in"
    const val EMPTY_PURSE = "Your purse is empty. Win some patience and cash in."
    const val GOLDS = "Golds"
    const val SILVERS = "Silvers"
    const val BRONZES = "Bronzes"

    fun draw(cards: Int) = "Draw $cards"

    fun suits(count: Int) = if (count == 1) "1 suit" else "$count suits"

    fun gameNumber(deal: Long) = "Game #$deal"

    fun moveCount(moves: Int) = "Moves: $moves"

    fun score(points: Int) = "Score: $points"

    fun highScore(points: Int) = "High score: $points"

    fun highestOnDeal(points: Int) = "High score on this deal: $points"

    fun time(clock: String) = "Time: $clock"

    fun bestTime(clock: String) = "Best time: $clock"

    fun bestOnDeal(clock: String) = "Best on this deal: $clock"

    fun dealOf(
        game: String,
        deal: Long,
    ) = "$game #$deal"

    fun playedLine(
        ended: String,
        clock: String,
        moves: Int,
        score: Int,
    ) = "$ended · $clock · $moves moves · $score points"

    fun minutesAgo(minutes: Long) = "$minutes min ago"

    fun hoursAgo(hours: Long) = "$hours h ago"

    fun daysAgo(days: Long) = if (days == 1L) "Yesterday" else "$days days ago"

    fun moveOf(
        step: Int,
        last: Int,
    ) = "Move $step of $last"

    fun speed(times: Int) = "$times×"

    fun pointsToCashIn(points: Long) = "Points to cash in: ${grouped(points)}"

    fun bronzes(count: Long) = if (count == 1L) "1 bronze" else "${grouped(count)} bronzes"

    fun cashInText(
        points: Long,
        bronzes: Long,
    ) = "Trade ${grouped(points)} points for ${bronzes(bronzes)}?"

    fun staked(bronzes: Long) = "On the table: ${bronzes(bronzes)}"

    fun betIn(coins: String) = "Bet in ${coins.lowercase()}"

    fun landed(
        pocket: Int,
        colour: String,
        won: Long,
    ) = "$pocket $colour: " + if (won > 0) "won ${bronzes(won)}" else "no win"

    fun lineBet(
        each: Long,
        total: Long,
    ) = "On each of 5 lines: ${grouped(each)}, ${bronzes(total)} in all"

    fun slotResult(won: Long) = if (won > 0) "Won ${bronzes(won)}!" else "No win this time"

    fun symbolDiceName(game: SymbolDiceGame) =
        if (game ==
            SymbolDiceGame.CHUCK_A_LUCK
        ) {
            CHUCK_A_LUCK
        } else {
            CROWN_AND_ANCHOR
        }

    /** The name of [face] at [game]: its number at chuck-a-luck, its picture's at crown and anchor. */
    fun faceName(
        game: SymbolDiceGame,
        face: Int,
    ) = if (game ==
        SymbolDiceGame.CHUCK_A_LUCK
    ) {
        "$face"
    } else {
        listOf("Crown", "Anchor", "Heart", "Spade", "Diamond", "Club")[
            face -
                1,
        ]
    }

    fun symbolResult(paid: Long) = if (paid > 0) "Paid ${bronzes(paid)}" else "Nothing back"

    fun sicBoBet(bet: SicBoBet) =
        when (bet.kind) {
            SicBoKind.SMALL -> "Small 4–10"
            SicBoKind.BIG -> "Big 11–17"
            SicBoKind.ODD -> "Odd"
            SicBoKind.EVEN -> "Even"
            SicBoKind.ANY_TRIPLE -> "Any triple"
            SicBoKind.TOTAL -> "${bet.value}"
            SicBoKind.SINGLE -> "One ${bet.value}"
            SicBoKind.DOUBLE -> "${bet.value} ${bet.value}"
            SicBoKind.TRIPLE -> "${bet.value} ${bet.value} ${bet.value}"
        }

    fun sicBoOdds(bet: SicBoBet) =
        when (bet.kind) {
            SicBoKind.SMALL, SicBoKind.BIG, SicBoKind.ODD, SicBoKind.EVEN -> "1 to 1"
            SicBoKind.ANY_TRIPLE -> "30 to 1"
            SicBoKind.TOTAL -> "${SicBoBet.TOTAL_PAYS.getValue(bet.value)} to 1"
            SicBoKind.SINGLE -> "1–3 to 1"
            SicBoKind.DOUBLE -> "10 to 1"
            SicBoKind.TRIPLE -> "180 to 1"
        }

    fun diceResult(
        dice: List<Int>,
        paid: Long,
    ) = "Rolled ${dice.joinToString(" ")}: ${dice.sum()} · " + if (paid > 0) "paid ${bronzes(paid)}" else "nothing back"

    fun crapsBet(bet: CrapsBet) =
        when (bet) {
            CrapsBet.PASS -> "Pass line"
            CrapsBet.DONT_PASS -> "Don't pass"
            CrapsBet.FIELD -> "Field 2 3 4 9 10 11 12"
            CrapsBet.ANY_SEVEN -> "Any seven"
            CrapsBet.ANY_CRAPS -> "Any craps"
            else -> bet.number.toString()
        }

    fun crapsOdds(bet: CrapsBet) =
        when (bet) {
            CrapsBet.FIELD -> "1 to 1, 2 and 12 double"
            CrapsBet.PASS, CrapsBet.DONT_PASS -> "1 to 1"
            else -> "${bet.pays} to ${bet.per}"
        }

    fun pointIs(point: Int) = "The point is $point"

    fun rolled(
        first: Int,
        second: Int,
        paid: Long,
    ) = "Rolled $first and $second: ${first + second} · " + if (paid > 0) "paid ${bronzes(paid)}" else "nothing back"

    fun baccaratSide(side: BaccaratSide) =
        when (side) {
            BaccaratSide.PLAYER -> "Player"
            BaccaratSide.BANKER -> "Banker"
            BaccaratSide.TIE -> "Tie"
        }

    fun baccaratOdds(side: BaccaratSide) =
        when (side) {
            BaccaratSide.PLAYER -> "1 to 1"
            BaccaratSide.BANKER -> "0.95 to 1"
            BaccaratSide.TIE -> "8 to 1"
        }

    fun sideTotal(
        side: String,
        total: Int,
    ) = "$side: $total"

    fun coupResult(
        won: BaccaratSide,
        player: Int,
        banker: Int,
        paid: Long,
    ): String {
        val winner =
            when (won) {
                BaccaratSide.TIE -> "A tie at $player"
                BaccaratSide.PLAYER -> "Player wins $player to $banker"
                BaccaratSide.BANKER -> "Banker wins $banker to $player"
            }
        return "$winner · " + if (paid > 0) "paid ${bronzes(paid)}" else "nothing back"
    }

    fun dealerShows(points: Int) = "Dealer shows $points"

    fun dealerHas(points: Int) = if (points > TWENTY_ONE) "Dealer busts with $points" else "Dealer has $points"

    /** A hand's total and bet, and once the round is over how it came out. */
    fun blackjackHand(
        points: Int,
        bet: Long,
        result: HandResult?,
    ): String {
        val total = if (points > TWENTY_ONE) "Bust, $points" else "$points"
        val outcome =
            when (result) {
                null -> ""
                HandResult.BLACKJACK -> " · Blackjack!"
                HandResult.WIN -> " · Win"
                HandResult.PUSH -> " · Push"
                HandResult.LOSE -> " · Lose"
            }
        return "$total on ${grouped(bet)}$outcome"
    }

    fun blackjackMove(move: BlackjackMove) =
        when (move) {
            BlackjackMove.HIT -> "Hit"
            BlackjackMove.STAND -> "Stand"
            BlackjackMove.DOUBLE -> "Double"
            BlackjackMove.SPLIT -> "Split"
        }

    fun blackjackWon(won: Long) = if (won > 0) "Paid ${bronzes(won)}" else "The house wins this one"

    fun paidFor(
        hand: String,
        won: Long,
    ) = "$hand: won ${bronzes(won)}"

    fun stakeOf(bronzes: Long) = "Stake: ${bronzes(bronzes)}"

    fun landedIn(
        multiplier: String,
        won: Long,
    ) = "Landed in $multiplier: " + if (won > 0) "won ${bronzes(won)}" else "no win"

    fun dozen(index: Int) = listOf("1st 12", "2nd 12", "3rd 12")[index]

    /** What an even-money bet says on the layout, or `null` for red and black, which are shown by their colour. */
    fun evenMoney(kind: BetKind): String? =
        when (kind) {
            BetKind.LOW -> "1–18"
            BetKind.HIGH -> "19–36"
            BetKind.EVEN -> "Even"
            BetKind.ODD -> "Odd"
            else -> null
        }

    fun highRollersText(
        entry: Long,
        leastBet: Long,
    ) = "Open to purses of ${bronzes(entry)} or more. Every bet there starts at ${grouped(leastBet)}."

    fun leastBet(bronzes: Long) = "Every bet starts at ${bronzes(bronzes)}."

    /** A table's name, and the room it is in if that is not the den's floor. */
    fun tableName(
        name: String,
        room: Room,
    ) = if (room == Room.FLOOR) name else "High Rollers' $name"

    fun buyText(
        theme: String,
        price: Long,
    ) = "Buy $theme for ${bronzes(price)}?"

    fun shortBy(bronzes: Long) = "You need ${bronzes(bronzes)} more."

    fun themeIs(name: String) = "Theme: $name"

    fun pokerName(game: PokerGame) = if (game == PokerGame.DRAW) FIVE_CARD_DRAW else TEXAS_HOLDEM

    fun pot(bronzes: Long) = "Pot: ${grouped(bronzes)}"

    fun stack(bronzes: Long) = grouped(bronzes)

    fun call(bronzes: Long) = "Call ${grouped(bronzes)}"

    fun raiseTo(
        bronzes: Long,
        opening: Boolean,
    ) = if (opening) "Bet ${grouped(bronzes)}" else "Raise to ${grouped(bronzes)}"

    fun raiseWith(coins: String) = "Raise with ${coins.lowercase()}"

    fun drawCards(count: Int) = if (count == 1) "Draw 1 card" else "Draw $count cards"

    fun waitingFor(name: String) = "$name is thinking…"

    /** What a player last did at the poker table. */
    fun said(said: Said): String {
        val amount = grouped(said.amount)
        return when (said.word) {
            Said.Word.ANTE -> "Ante $amount"
            Said.Word.BLIND -> "Blind $amount"
            Said.Word.FOLD -> FOLD
            Said.Word.CHECK -> CHECK
            Said.Word.CALL -> "Call $amount"
            Said.Word.BET -> "Bet $amount"
            Said.Word.RAISE -> "Raise to $amount"
            Said.Word.ALL_IN -> "All in, $amount"
            Said.Word.DRAW -> if (said.amount == 0L) "Stood pat" else "Drew $amount"
        }
    }

    /** How a hand went for [seat]: what they won and with what, or what they showed, or `null` if neither. */
    fun outcomeOf(
        outcome: Outcome,
        seat: Int,
    ): String? {
        val category = outcome.hands[seat]?.let(::handName)
        val won = outcome.won[seat]
        return when {
            won > 0 && category != null -> "Won ${grouped(won)}, $category"
            won > 0 -> "Won ${grouped(won)}"
            else -> category
        }
    }

    fun handName(category: HandCategory) =
        when (category) {
            HandCategory.HIGH_CARD -> "high card"
            HandCategory.ONE_PAIR -> "a pair"
            HandCategory.TWO_PAIR -> "two pair"
            HandCategory.THREE_OF_A_KIND -> "three of a kind"
            HandCategory.STRAIGHT -> "a straight"
            HandCategory.FLUSH -> "a flush"
            HandCategory.FULL_HOUSE -> "a full house"
            HandCategory.FOUR_OF_A_KIND -> "four of a kind"
            HandCategory.STRAIGHT_FLUSH -> "a straight flush"
        }

    fun restartText(deal: Long) = "Your moves on deal #$deal will be cleared."

    fun winText(
        deal: Long,
        moves: Int,
        clock: String,
    ) = "Deal #$deal solved. Moves: $moves. Time: $clock."
}

/** [number] with its thousands set apart by commas, such as 12,345. */
internal fun grouped(number: Long): String {
    val digits = number.toString().removePrefix("-")
    val sign = if (number < 0) "-" else ""
    return sign +
        digits
            .reversed()
            .chunked(DIGITS_IN_GROUP)
            .joinToString(",")
            .reversed()
}

/** A time as a clock shows it: `05:07`, or `1:05:07` once it runs to hours. */
internal fun clockText(seconds: Long): String {
    val hours = seconds / SECONDS_IN_HOUR
    val minutes = (seconds / SECONDS_IN_MINUTE % MINUTES_IN_HOUR).toString().padStart(2, '0')
    val rest = (seconds % SECONDS_IN_MINUTE).toString().padStart(2, '0')
    return if (hours > 0) "$hours:$minutes:$rest" else "$minutes:$rest"
}

internal fun clockTextOfMillis(millis: Long): String = clockText(millis / MILLIS_IN_SECOND)

private const val MILLIS_IN_SECOND = 1000
private const val DIGITS_IN_GROUP = 3
private const val TWENTY_ONE = 21
private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_HOUR = 3600
