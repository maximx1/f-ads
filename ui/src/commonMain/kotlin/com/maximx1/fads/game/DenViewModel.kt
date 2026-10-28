package com.maximx1.fads.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.engine.History
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds the player's den and keeps it saved. Points are cashed in against [history], the games played at
 * patience, which earn them.
 */
class DenViewModel(
    private val store: GameStore,
    private val history: StateFlow<History>,
) : ViewModel() {
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

    /** Trades every whole hundred points won for bronzes. */
    fun cashIn() = change { it.cashIn(history.value) }

    private fun change(how: (Den) -> Den) {
        val before = current.value ?: return
        val after = how(before)
        if (after == before) return
        current.value = after
        saves.trySend(after)
    }
}
