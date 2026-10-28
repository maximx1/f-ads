package com.maximx1.fads.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.PokerMove
import com.maximx1.fads.core.den.botsTurn
import com.maximx1.fads.core.den.canMove
import com.maximx1.fads.core.den.canRebet
import com.maximx1.fads.core.den.clearBets
import com.maximx1.fads.core.den.dealHand
import com.maximx1.fads.core.den.leaveTable
import com.maximx1.fads.core.den.placeBet
import com.maximx1.fads.core.den.playBot
import com.maximx1.fads.core.den.playPoker
import com.maximx1.fads.core.den.playersTurn
import com.maximx1.fads.core.den.rebet
import com.maximx1.fads.core.den.sitDown
import com.maximx1.fads.core.den.spin
import com.maximx1.fads.core.den.table
import com.maximx1.fads.core.engine.History
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Everything the player can do in the den. Anything the purse cannot pay for does nothing. */
@Suppress("TooManyFunctions") // One for each thing the player can do in the den.
interface DenActions {
    /** Trades every whole hundred points won for bronzes. */
    fun cashIn()

    /** Puts [amount] bronzes on [bet] at the roulette table. */
    fun placeBet(
        bet: Bet,
        amount: Long,
    )

    /** Takes every bet back off the roulette layout. */
    fun clearBets()

    /** Puts the last spin's bets down again. */
    fun rebet()

    /** Spins the wheel, if anything is bet. */
    fun spin()

    /** Takes a seat at [game]'s table. */
    fun sitDown(game: PokerGame)

    /** Deals the next hand at [game]'s table, if none is being played and the purse holds something. */
    fun deal(game: PokerGame)

    /** Makes [move] at [game]'s table, if it is the player's turn and they can. */
    fun playPoker(
        game: PokerGame,
        move: PokerMove,
    )

    /** Gets up from [game]'s table, folding a hand still being played. */
    fun leaveTable(game: PokerGame)

    /** Buys the theme saved as [key] and puts it up, if the purse holds its price. */
    fun buyTheme(key: String)

    /** Puts up the theme saved as [key], if the player owns it. */
    fun chooseTheme(key: String)
}

/**
 * Holds the player's den and keeps it saved, and plays the den's players at the poker tables, a move every
 * [botMillis]. Points are cashed in against [history], the games played at patience, which earn them. [luck] picks
 * the number of each spin of the roulette wheel and of each hand dealt, which say how they go.
 */
@Suppress("TooManyFunctions") // One for each thing the player can do in the den, and the den's players' turns.
class DenViewModel(
    private val store: GameStore,
    private val history: StateFlow<History>,
    private val botMillis: Long = BOT_MILLIS,
    private val luck: () -> Long = { Random.nextLong(0, Long.MAX_VALUE) },
) : ViewModel(),
    DenActions {
    private var bots: Job? = null

    private val current = MutableStateFlow<Den?>(null)

    /** The player's den. `null` only for the moment it takes to read it at start-up. */
    val den: StateFlow<Den?> = current.asStateFlow()

    // Conflated: if changes come faster than the disk, only the newest den is written.
    private val saves = Channel<Den>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            current.value = store.loadDen()
            for (den in saves) store.save(den)
        }
    }

    override fun cashIn() = change { it.cashIn(history.value) }

    override fun placeBet(
        bet: Bet,
        amount: Long,
    ) = change { if (amount > 0 && it.purse.holds(amount)) it.placeBet(bet, amount) else it }

    override fun clearBets() = change { it.clearBets() }

    override fun rebet() = change { if (it.canRebet) it.rebet() else it }

    override fun spin() = change { if (it.roulette.bets.isEmpty()) it else it.spin(luck()) }

    override fun sitDown(game: PokerGame) {
        change { it.sitDown(game, luck()) }
        playBots(game)
    }

    override fun deal(game: PokerGame) {
        change { if (it.table(game)?.playing == false && it.purse.total > 0) it.dealHand(game, luck()) else it }
        playBots(game)
    }

    override fun playPoker(
        game: PokerGame,
        move: PokerMove,
    ) {
        change {
            if (it.playersTurn(game) &&
                it.table(game)?.hand?.canMove(move) == true
            ) {
                it.playPoker(game, move)
            } else {
                it
            }
        }
        playBots(game)
    }

    override fun leaveTable(game: PokerGame) {
        bots?.cancel()
        // The den's players play out a hand the player folds, which takes a moment.
        bots =
            viewModelScope.launch {
                val before = current.value ?: return@launch
                val after = withContext(Dispatchers.Default) { before.leaveTable(game) }
                if (current.value === before) change { after }
            }
    }

    override fun buyTheme(key: String) =
        change { if (!it.owns(key) && it.purse.holds(Den.THEME_PRICE)) it.buyTheme(key, Den.THEME_PRICE) else it }

    override fun chooseTheme(key: String) = change { if (it.owns(key)) it.chooseTheme(key) else it }

    // The den's players take their turns one after another until it is the player's, or the hand is over. They
    // think away from the screen's thread, since weighing up a hand takes a moment.
    private fun playBots(game: PokerGame) {
        if (bots?.isActive == true) return
        bots =
            viewModelScope.launch {
                while (current.value?.botsTurn(game) == true) {
                    delay(botMillis)
                    val before = current.value ?: break
                    val after = withContext(Dispatchers.Default) { before.playBot(game) }
                    if (current.value === before) change { after }
                }
            }
    }

    private fun change(how: (Den) -> Den) {
        val before = current.value ?: return
        val after = how(before)
        if (after == before) return
        current.value = after
        saves.trySend(after)
    }

    private companion object {
        const val BOT_MILLIS = 700L
    }
}
