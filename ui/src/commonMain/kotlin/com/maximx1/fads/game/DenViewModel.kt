package com.maximx1.fads.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.PokerMove
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.SlotMachine
import com.maximx1.fads.core.den.botsTurn
import com.maximx1.fads.core.den.canMove
import com.maximx1.fads.core.den.canRebet
import com.maximx1.fads.core.den.clearBets
import com.maximx1.fads.core.den.dealHand
import com.maximx1.fads.core.den.dealVideoPoker
import com.maximx1.fads.core.den.drawVideoPoker
import com.maximx1.fads.core.den.dropBall
import com.maximx1.fads.core.den.leaveTable
import com.maximx1.fads.core.den.placeBet
import com.maximx1.fads.core.den.playBot
import com.maximx1.fads.core.den.playPoker
import com.maximx1.fads.core.den.playersTurn
import com.maximx1.fads.core.den.pullLever
import com.maximx1.fads.core.den.rebet
import com.maximx1.fads.core.den.roulette
import com.maximx1.fads.core.den.sitDown
import com.maximx1.fads.core.den.spin
import com.maximx1.fads.core.den.table
import com.maximx1.fads.core.den.videoPoker
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

    /** Puts [amount] bronzes on [bet] at [room]'s roulette table. */
    fun placeBet(
        bet: Bet,
        amount: Long,
        room: Room,
    )

    /** Takes every bet back off [room]'s roulette layout. */
    fun clearBets(room: Room)

    /** Puts the last spin's bets down again at [room]'s roulette table. */
    fun rebet(room: Room)

    /** Spins [room]'s wheel, if anything is bet. */
    fun spin(room: Room)

    /** Takes a seat at [game]'s table in [room]. */
    fun sitDown(
        game: PokerGame,
        room: Room,
    )

    /** Deals the next hand at [game]'s table in [room], if none is being played and the purse holds enough. */
    fun deal(
        game: PokerGame,
        room: Room,
    )

    /** Makes [move] at [game]'s table in [room], if it is the player's turn and they can. */
    fun playPoker(
        game: PokerGame,
        move: PokerMove,
        room: Room,
    )

    /** Gets up from [game]'s table in [room], folding a hand still being played. */
    fun leaveTable(
        game: PokerGame,
        room: Room,
    )

    /** Drops a ball at [room]'s plinko table with [stake] bronzes on it, if the purse holds that much. */
    fun dropBall(
        stake: Long,
        room: Room,
    )

    /** Pulls the lever of [room]'s slot machine with [lineBet] bronzes on each line, if the purse holds them all. */
    fun pullLever(
        lineBet: Long,
        room: Room,
    )

    /** Deals a hand at [room]'s video poker machine for [bet] bronzes, if none waits for its draw. */
    fun dealVideoPoker(
        bet: Long,
        room: Room,
    )

    /** Draws at [room]'s video poker machine, keeping the cards [held]. */
    fun drawVideoPoker(
        held: Set<Int>,
        room: Room,
    )

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
        room: Room,
    ) = change { if (amount >= room.leastBet && it.purse.holds(amount)) it.placeBet(bet, amount, room) else it }

    override fun clearBets(room: Room) = change { it.clearBets(room) }

    override fun rebet(room: Room) = change { if (it.canRebet(room)) it.rebet(room) else it }

    override fun spin(room: Room) = change { if (it.roulette(room).bets.isEmpty()) it else it.spin(luck(), room) }

    override fun sitDown(
        game: PokerGame,
        room: Room,
    ) {
        change { it.sitDown(game, luck(), room) }
        playBots(game, room)
    }

    override fun deal(
        game: PokerGame,
        room: Room,
    ) {
        change {
            val ready = it.table(game, room)?.playing == false && it.purse.total > 0 && it.purse.holds(room.leastBet)
            if (ready) it.dealHand(game, luck(), room) else it
        }
        playBots(game, room)
    }

    override fun playPoker(
        game: PokerGame,
        move: PokerMove,
        room: Room,
    ) {
        change {
            val legal = it.playersTurn(game, room) && it.table(game, room)?.hand?.canMove(move) == true
            if (legal) it.playPoker(game, move, room) else it
        }
        playBots(game, room)
    }

    override fun leaveTable(
        game: PokerGame,
        room: Room,
    ) {
        bots?.cancel()
        // The den's players play out a hand the player folds, which takes a moment.
        bots =
            viewModelScope.launch {
                val before = current.value ?: return@launch
                val after = withContext(Dispatchers.Default) { before.leaveTable(game, room) }
                if (current.value === before) change { after }
            }
    }

    override fun dropBall(
        stake: Long,
        room: Room,
    ) = change { if (stake >= room.leastBet && it.purse.holds(stake)) it.dropBall(stake, luck(), room) else it }

    override fun pullLever(
        lineBet: Long,
        room: Room,
    ) = change {
        val enough = lineBet >= room.leastBet && it.purse.holds(lineBet * SlotMachine.lines.size)
        if (enough) it.pullLever(lineBet, luck(), room) else it
    }

    override fun dealVideoPoker(
        bet: Long,
        room: Room,
    ) = change {
        val ready = !it.videoPoker(room).dealt && bet >= room.leastBet && it.purse.holds(bet)
        if (ready) it.dealVideoPoker(bet, luck(), room) else it
    }

    override fun drawVideoPoker(
        held: Set<Int>,
        room: Room,
    ) = change { if (it.videoPoker(room).dealt) it.drawVideoPoker(held, room) else it }

    override fun buyTheme(key: String) =
        change { if (!it.owns(key) && it.purse.holds(Den.THEME_PRICE)) it.buyTheme(key, Den.THEME_PRICE) else it }

    override fun chooseTheme(key: String) = change { if (it.owns(key)) it.chooseTheme(key) else it }

    // The den's players take their turns one after another until it is the player's, or the hand is over. They
    // think away from the screen's thread, since weighing up a hand takes a moment.
    private fun playBots(
        game: PokerGame,
        room: Room,
    ) {
        if (bots?.isActive == true) return
        bots =
            viewModelScope.launch {
                while (current.value?.botsTurn(game, room) == true) {
                    delay(botMillis)
                    val before = current.value ?: break
                    val after = withContext(Dispatchers.Default) { before.playBot(game, room) }
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
