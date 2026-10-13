package com.maximx1.fads.game

import com.maximx1.fads.core.engine.BestTimes
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.Win
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
            assertNull(GameStore(FileStorage(folder.root)).load())
        }

    @Test
    fun aSavedGameLoadsBackTheSameEvenFromAFreshStore() =
        runBlocking {
            val game = Game.start(1).play(Move.ColumnToColumn(0, 4)).play(Move.ColumnToFreeCell(0, 0))
            GameStore(FileStorage(folder.root)).save(game)

            val loaded = GameStore(FileStorage(folder.root)).load()
            assertEquals(game.record, loaded?.record)
            assertEquals(game.state, loaded?.state)
        }

    @Test
    fun savingAgainReplacesTheOldGame() =
        runBlocking {
            val store = GameStore(FileStorage(folder.root))
            store.save(Game.start(1).play(Move.ColumnToColumn(0, 4)))
            store.save(Game.start(617))
            assertEquals(Game.start(617).record, store.load()?.record)
        }

    @Test
    fun aDamagedFileLoadsAsNull() =
        runBlocking {
            file.writeText("""{"dealNumber":1,"moves":[{"type":"cc","fr""")
            assertNull(GameStore(FileStorage(folder.root)).load())
        }

    @Test
    fun bestTimesAreKeptBesideTheGameAndStartOutEmpty() =
        runBlocking {
            val store = GameStore(FileStorage(folder.root))
            assertEquals(BestTimes(), store.loadBestTimes())

            val times = BestTimes().with(Win(dealNumber = 617, millis = 75_000, moves = 131))
            store.save(times)
            store.save(Game.start(617))
            assertEquals(times, GameStore(FileStorage(folder.root)).loadBestTimes())
            assertEquals(Game.start(617).record, store.load()?.record)

            File(folder.root, GameStore.BEST_TIMES).writeText("nonsense")
            assertEquals(BestTimes(), store.loadBestTimes())
        }

    @Test
    fun storageThatCannotWriteLosesTheSaveButDoesNotThrow() =
        runBlocking {
            // A directory cannot be made inside something that is a file.
            val blocker = File(folder.root, "blocker").apply { writeText("in the way") }
            val store = GameStore(FileStorage(File(blocker, "saves")))
            store.save(Game.start(1))
            assertNull(store.load())
        }
}
