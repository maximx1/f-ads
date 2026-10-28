package com.maximx1.fads.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.canRebet
import com.maximx1.fads.core.den.clearBets
import com.maximx1.fads.core.den.placeBet
import com.maximx1.fads.core.den.rebet
import com.maximx1.fads.core.den.spin
import com.maximx1.fads.core.engine.History
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Everything the player can do in the den. Anything the purse cannot pay for does nothing. */
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
}

/**
 * Holds the player's den and keeps it saved. Points are cashed in against [history], the games played at
 * patience, which earn them. [spinNumber] picks the number of each spin of the roulette wheel, which says where
 * it lands.
 */
class DenViewModel(
    private val store: GameStore,
    private val history: StateFlow<History>,
    private val spinNumber: () -> Long = { Random.nextLong(0, Long.MAX_VALUE) },
) : ViewModel(),
    DenActions {
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

    override fun spin() = change { if (it.roulette.bets.isEmpty()) it else it.spin(spinNumber()) }

    private fun change(how: (Den) -> Den) {
        val before = current.value ?: return
        val after = how(before)
        if (after == before) return
        current.value = after
        saves.trySend(after)
    }
}
