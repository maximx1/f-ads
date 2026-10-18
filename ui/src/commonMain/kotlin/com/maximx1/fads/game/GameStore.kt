package com.maximx1.fads.game

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Records
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Keeps a game in progress and the player's records for each type of game, so they survive the app being closed
 * or killed.
 */
class GameStore(
    private val storage: Storage,
) {
    /** The saved game of [type], or `null` if there is none or it cannot be read. */
    suspend fun load(type: GameType = GameType.FreeCell): Game<*, *>? =
        withContext(Dispatchers.IO) { storage.read(gameFile(type))?.let(type::load) }

    suspend fun save(game: Game<*, *>) =
        withContext(Dispatchers.IO) { storage.write(gameFile(game.type), game.toJson()) }

    /** The saved records for [type]. None at all if nothing was saved or it cannot be read. */
    suspend fun loadRecords(type: GameType = GameType.FreeCell): Records =
        withContext(Dispatchers.IO) { storage.read(recordsFile(type))?.let(Records::fromJson) ?: Records() }

    suspend fun save(
        records: Records,
        type: GameType = GameType.FreeCell,
    ) = withContext(Dispatchers.IO) { storage.write(recordsFile(type), records.toJson()) }

    companion object {
        const val GAME = "game.json"

        // Named from when it held only FreeCell's best times, and kept so that those still load.
        const val RECORDS = "best-times.json"

        // FreeCell keeps the names it had before there were other games.
        fun gameFile(type: GameType) = if (type == GameType.FreeCell) GAME else "game-${type.key}.json"

        fun recordsFile(type: GameType) = if (type == GameType.FreeCell) RECORDS else "records-${type.key}.json"
    }
}
