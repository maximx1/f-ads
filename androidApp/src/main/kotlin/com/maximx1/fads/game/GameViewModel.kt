package com.maximx1.fads.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.rules.Move
import com.maximx1.fads.core.rules.canMove
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class GameViewModel(
    private val store: GameStore,
) : ViewModel() {
    private val current = MutableStateFlow<Game?>(null)

    /** The game on the table. `null` only for the moment it takes to read the saved game at start-up. */
    val game: StateFlow<Game?> = current.asStateFlow()

    // Conflated: if moves come faster than the disk, only the newest game is written.
    private val saves = Channel<Game>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            current.value = store.load() ?: Game.start(randomDealNumber()).also { saves.send(it) }
            for (game in saves) store.save(game)
        }
    }

    fun play(move: Move) = change { if (it.state.canMove(move)) it.play(move) else it }

    fun undo() = change { it.undo() }

    fun restart() = change { it.restart() }

    fun newGame(dealNumber: Long) = change { Game.start(dealNumber) }

    private fun change(transform: (Game) -> Game) {
        val before = current.value ?: return
        val after = transform(before)
        if (after !== before) {
            current.value = after
            saves.trySend(after)
        }
    }

    companion object {
        const val MAX_RANDOM_DEAL = 1_000_000L

        fun randomDealNumber(): Long = Random.nextLong(1, MAX_RANDOM_DEAL + 1)

        val Factory =
            viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    GameViewModel(GameStore(File(app.filesDir, "game.json")))
                }
            }
    }
}
