package com.maximx1.fads.game

import com.maximx1.fads.core.engine.BestTimes
import com.maximx1.fads.core.engine.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Keeps the game in progress and the player's best times, so they survive the app being closed or killed. */
class GameStore(
    private val storage: Storage,
) {
    /** The saved game, or `null` if there is none or it cannot be read. */
    suspend fun load(): Game? = withContext(Dispatchers.IO) { storage.read(GAME)?.let(Game::fromJson) }

    suspend fun save(game: Game) = withContext(Dispatchers.IO) { storage.write(GAME, game.toJson()) }

    /** The saved best times. None at all if nothing was saved or it cannot be read. */
    suspend fun loadBestTimes(): BestTimes =
        withContext(Dispatchers.IO) { storage.read(BEST_TIMES)?.let(BestTimes::fromJson) ?: BestTimes() }

    suspend fun save(times: BestTimes) = withContext(Dispatchers.IO) { storage.write(BEST_TIMES, times.toJson()) }

    companion object {
        const val GAME = "game.json"
        const val BEST_TIMES = "best-times.json"
    }
}
