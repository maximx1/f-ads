package com.maximx1.fads.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.History
import com.maximx1.fads.core.engine.Played
import com.maximx1.fads.core.engine.Records
import com.maximx1.fads.core.engine.Win
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Holds the game on the table, runs its clock and keeps it saved.
 *
 * @param wallClock the time of day, in milliseconds since 1970 began, to say when each finished game ended.
 * @param now a clock in milliseconds that only ever goes forward. Only the difference between two readings is
 * used.
 */
@Suppress("TooManyFunctions") // One for each thing the player can do to the game, and a few to run the clock.
class GameViewModel(
    private val store: GameStore,
    private val wallClock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val now: () -> Long,
) : ViewModel() {
    private val current = MutableStateFlow<Game<*, *>?>(null)

    /** The game on the table. `null` only for the moment it takes to read the saved game at start-up. */
    val game: StateFlow<Game<*, *>?> = current.asStateFlow()

    private val best = MutableStateFlow(Records())

    /** The player's best times and scores. Empty for the moment it takes to read them at start-up. */
    val records: StateFlow<Records> = best.asStateFlow()

    private val seconds = MutableStateFlow(0L)

    /**
     * Whole seconds on the game's clock. The clock runs from the first move until the game is won, and only
     * while the game is [onScreen] and not paused.
     */
    val elapsedSeconds: StateFlow<Long> = seconds.asStateFlow()

    // Conflated: if moves come faster than the disk, only the newest game is written.
    private val saves = Channel<Game<*, *>>(Channel.CONFLATED)

    // The clock. The game holds the time counted up to [runningSince]; the stretch since then is added to it
    // whenever the clock stops, which it does around every change to the game.
    private var visible = false
    private var runningSince: Long? = null
    private var ticker: Job? = null

    // Plays the cards up one by one once the game can finish itself. The clock stands still while it does.
    private var finisher: Job? = null
    private var finishing = false

    // The type of game being fetched from the store, until it is on the table.
    private var opening: GameType? = null

    private val past = MutableStateFlow(History())

    /** Every game finished or given up on, newest first. Empty for the moment it takes to read at start-up. */
    val history: StateFlow<History> = past.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = store.loadHistory()
            // Anything that ended while it was read is newer.
            past.value = History(past.value.games + saved.games)
            for (game in saves) store.save(game)
        }
    }

    /**
     * Puts [type]'s saved game on the table, or a fresh random deal of it if none was saved. Does nothing if a
     * game of that type is already there.
     */
    fun open(type: GameType) {
        if (current.value?.type == type || opening == type) return
        opening = type
        viewModelScope.launch {
            val saved = store.load(type)
            // Dealing a new game while this one was read wins.
            if (opening != type) return@launch
            switchTo(saved ?: type.start(randomDealNumber()))
        }
    }

    /**
     * Tells the clock whether the player is at the game: it is in sight and not paused. Leaving also saves the
     * time so far.
     */
    fun onScreen(visible: Boolean) {
        this.visible = visible
        change { it }
    }

    /** Plays [move] if it is legal in the game on the table, and does nothing otherwise. */
    fun play(move: Any) = change { it.playIfLegal(move) }

    fun undo() {
        finisher?.cancel()
        change { it.undo() }
    }

    /**
     * Plays every card left up to its foundation, one at a time so the player sees each go, if the game can
     * finish itself. Each card counts as a move. The time stops when the button is pressed, not when the last
     * card lands.
     */
    fun autoComplete() {
        if (finishing || current.value?.canAutoComplete != true) return
        finishing = true
        change { it }
        finisher =
            viewModelScope.launch {
                try {
                    while (true) {
                        val move = current.value?.nextAutoMove() ?: break
                        change { it.playIfLegal(move) }
                        delay(AUTO_STEP)
                    }
                } finally {
                    finishing = false
                    finisher = null
                    // If it was stopped part way, the game goes on and so does its clock.
                    change { it }
                }
            }
    }

    /** Counts a hint against the score. Showing it is up to the screen. */
    fun takeHint() = change { it.takeHint() }

    fun restart() {
        finisher?.cancel()
        change {
            giveUp(it)
            it.restart()
        }
    }

    /** Deals [dealNumber] of the game on the table. */
    fun newGame(dealNumber: Long) {
        val type = current.value?.type ?: return
        newGame(type, dealNumber)
    }

    /** Deals [dealNumber] of [type], which becomes the game on the table whatever was there before. */
    fun newGame(
        type: GameType,
        dealNumber: Long,
    ) {
        finisher?.cancel()
        if (current.value?.type == type) {
            change {
                giveUp(it)
                type.start(dealNumber)
            }
        } else {
            switchTo(type.start(dealNumber))
        }
    }

    /**
     * Stops the clock and writes the game out, returning once it is safely saved. For a platform that is about
     * to shut the game down without warning, as a desktop does when its window is closed.
     */
    suspend fun close() {
        onScreen(false)
        current.value?.let { store.save(it) }
    }

    /** Puts [game], of another type than the one on the table, there in its place, which is saved as it stood. */
    private fun switchTo(game: Game<*, *>) {
        opening = null
        current.value?.let { left ->
            val stopped = stopClock(left)
            viewModelScope.launch { store.save(stopped) }
        }
        best.value = Records()
        show(game)
        saves.trySend(game)
        viewModelScope.launch {
            val records = store.loadRecords(game.type)
            if (current.value?.type == game.type) best.value = records
        }
    }

    private fun change(transform: (Game<*, *>) -> Game<*, *>) {
        val before = current.value ?: return
        val after = transform(stopClock(before))
        if (after.isWon && !before.isWon) record(after)
        show(after)
        if (after !== before) saves.trySend(after)
    }

    /**
     * Counts a win towards the records. A win with nothing on the clock is left out: it can only come from a
     * game saved before there was a clock, and no real game could ever beat it.
     */
    private fun record(won: Game<*, *>) {
        addToHistory(won)
        if (won.elapsedMillis <= 0) return
        val records = best.value.with(Win(won.dealNumber, won.elapsedMillis, won.movesMade, won.score))
        if (records === best.value) return
        best.value = records
        viewModelScope.launch { store.save(records, won.type) }
    }

    /** Puts [game], which is being left unfinished, in the history, if anything was played. */
    private fun giveUp(game: Game<*, *>) {
        if (!game.isWon && game.movesMade > 0) addToHistory(game)
    }

    /** Puts [game], which has ended, in the history. */
    private fun addToHistory(game: Game<*, *>) {
        val history = past.value.with(Played.of(game, wallClock()))
        past.value = history
        viewModelScope.launch { store.save(history) }
    }

    /** Stops the clock and returns [game] with the time it counted added on. */
    private fun stopClock(game: Game<*, *>): Game<*, *> {
        ticker?.cancel()
        ticker = null
        val since = runningSince ?: return game
        runningSince = null
        return game.withElapsed(game.elapsedMillis + now() - since)
    }

    /** Puts [game] on the table and, if its clock should be running, starts it. The clock must be stopped. */
    private fun show(game: Game<*, *>) {
        current.value = game
        seconds.value = game.elapsedMillis / SECOND
        if (!visible || !game.inPlay || finishing) return
        val since = now()
        runningSince = since
        ticker =
            viewModelScope.launch {
                while (true) {
                    // Wake as each second turns over, so the display never lags the clock.
                    delay(SECOND - (game.elapsedMillis + now() - since) % SECOND)
                    seconds.value = (game.elapsedMillis + now() - since) / SECOND
                }
            }
    }

    companion object {
        const val MAX_RANDOM_DEAL = 1_000_000L
        private const val SECOND = 1000L
        private const val AUTO_STEP = 80L

        fun randomDealNumber(): Long = Random.nextLong(1, MAX_RANDOM_DEAL + 1)
    }
}
