package com.maximx1.fads.ui

import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.core.den.HandCategory
import com.maximx1.fads.core.den.Outcome
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.Said

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
    const val SPIN = "Spin"
    const val CLEAR = "Clear"
    const val REBET = "Rebet"
    const val COLUMN_BET = "2:1"
    const val RED = "Red"
    const val BLACK = "Black"
    const val GREEN = "Green"
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
private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_HOUR = 3600
