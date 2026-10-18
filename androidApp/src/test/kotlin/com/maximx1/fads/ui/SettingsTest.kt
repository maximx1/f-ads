package com.maximx1.fads.ui

import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.ui.theme.TableTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun settings() = Settings(FileStorage(folder.root))

    @Test
    fun withNothingSavedTheThemeIsFlatAndStuckCardsAreDimmed() {
        assertEquals(TableTheme.FLAT, settings().theme)
        assertTrue(settings().dimsStuckCards)
    }

    @Test
    fun choicesAreStillThereTheNextTime() {
        settings().theme = TableTheme.BACKROOM
        assertEquals(TableTheme.BACKROOM, settings().theme)
        assertTrue(settings().dimsStuckCards)

        settings().dimsStuckCards = false
        assertFalse(settings().dimsStuckCards)
        assertEquals(TableTheme.BACKROOM, settings().theme)
    }

    @Test
    fun aSavedThemeThatNoLongerExistsFallsBackToFlat() {
        File(folder.root, "settings.txt").writeText("theme=LAVA_LAMP\nnonsense\ndimStuckCards=false\n")
        assertEquals(TableTheme.FLAT, settings().theme)
        assertFalse(settings().dimsStuckCards)
    }

    @Test
    fun noGameIsChosenUntilThePlayerPicksOneAndThenItIsKept() {
        assertNull(settings().gameType)
        settings().gameType = GameType.FreeCell
        assertEquals(GameType.FreeCell, settings().gameType)
    }

    @Test
    fun someoneWithAFreeCellGameSavedFromBeforeIsAFreeCellPlayer() {
        File(folder.root, GameStore.GAME).writeText("""{"dealNumber":1}""")
        assertEquals(GameType.FreeCell, settings().gameType)
    }
}
