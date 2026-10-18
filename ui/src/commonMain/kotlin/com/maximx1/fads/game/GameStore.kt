package com.maximx1.fads.game

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.Records
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Keeps the game in progress and the player's records, so they survive the app being closed or killed. */
class GameStore(
    private val storage: Storage,
) {
    /** The saved game, or `null` if there is none or it cannot be read. */
    suspend fun load(): Game? = withContext(Dispatchers.IO) { storage.read(GAME)?.let(Game::fromJson) }

    suspend fun save(game: Game) = withContext(Dispatchers.IO) { storage.write(GAME, game.toJson()) }

    /** The saved records. None at all if nothing was saved or it cannot be read. */
    suspend fun loadRecords(): Records =
        withContext(Dispatchers.IO) { storage.read(RECORDS)?.let(Records::fromJson) ?: Records() }

    suspend fun save(records: Records) = withContext(Dispatchers.IO) { storage.write(RECORDS, records.toJson()) }

    companion object {
        const val GAME = "game.json"

        // Named from when it held only best times, and kept so that those still load.
        const val RECORDS = "best-times.json"
    }
}
