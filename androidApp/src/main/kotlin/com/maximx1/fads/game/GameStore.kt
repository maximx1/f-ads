package com.maximx1.fads.game

import android.util.AtomicFile
import android.util.Log
import com.maximx1.fads.core.engine.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Keeps the game in progress in one small file, so it survives the app being closed or killed. */
class GameStore(
    file: File,
) {
    private val file = AtomicFile(file)

    /** The saved game, or `null` if there is none or it cannot be read. */
    suspend fun load(): Game? =
        withContext(Dispatchers.IO) {
            try {
                Game.fromJson(file.readFully().decodeToString())
            } catch (ignored: IOException) {
                null
            }
        }

    suspend fun save(game: Game) =
        withContext(Dispatchers.IO) {
            try {
                val stream = file.startWrite()
                try {
                    stream.write(game.toJson().encodeToByteArray())
                    file.finishWrite(stream)
                } catch (e: IOException) {
                    file.failWrite(stream)
                    throw e
                }
            } catch (e: IOException) {
                Log.w(TAG, "Could not save the game", e)
            }
        }

    private companion object {
        const val TAG = "GameStore"
    }
}
