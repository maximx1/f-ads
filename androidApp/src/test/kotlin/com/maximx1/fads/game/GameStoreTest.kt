package com.maximx1.fads.game

import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.rules.Move
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GameStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "game.json") }

    @Test
    fun nothingSavedYetLoadsAsNull() =
        runBlocking {
            assertNull(GameStore(file).load())
        }

    @Test
    fun aSavedGameLoadsBackTheSameEvenFromAFreshStore() =
        runBlocking {
            val game = Game.start(1).play(Move.ColumnToColumn(0, 4)).play(Move.ColumnToFreeCell(0, 0))
            GameStore(file).save(game)

            val loaded = GameStore(file).load()
            assertEquals(game.record, loaded?.record)
            assertEquals(game.state, loaded?.state)
        }

    @Test
    fun savingAgainReplacesTheOldGame() =
        runBlocking {
            val store = GameStore(file)
            store.save(Game.start(1).play(Move.ColumnToColumn(0, 4)))
            store.save(Game.start(617))
            assertEquals(Game.start(617).record, store.load()?.record)
        }

    @Test
    fun aDamagedFileLoadsAsNull() =
        runBlocking {
            file.writeText("""{"dealNumber":1,"moves":[{"type":"cc","fr""")
            assertNull(GameStore(file).load())
        }
}
