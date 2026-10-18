package com.maximx1.fads.ui

/** Every piece of text the game shows, in one place so that it can be translated one day. */
@Suppress("TooManyFunctions") // One for each piece of text with a number or a name in it.
internal object Strings {
    const val APP_NAME = "Fuck Ads"
    const val PICK_A_GAME = "Pick a game"
    const val PLAY = "Play"
    const val FREECELL = "FreeCell"
    const val SOLITAIRE = "Solitaire"
    const val SPIDER = "Spider"

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

    fun restartText(deal: Long) = "Your moves on deal #$deal will be cleared."

    fun winText(
        deal: Long,
        moves: Int,
        clock: String,
    ) = "Deal #$deal solved. Moves: $moves. Time: $clock."
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
private const val SECONDS_IN_MINUTE = 60
private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_HOUR = 3600
