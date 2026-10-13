package com.maximx1.fads.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import com.maximx1.fads.MainActivity
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.ui.theme.TableTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Starts the whole app in each theme, paints it, and plays a move. The picture of each theme is left in
 * `build/reports/themes`, to be looked at: nothing here can judge whether a theme looks right.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeTest(
    private val theme: TableTheme,
) {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val saveFile = File(app.filesDir, "game.json")
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    @Test
    fun itPaintsTheTableAndTheGameStillPlays() {
        Settings(FileStorage(app.filesDir)).theme = theme
        saveFile.writeText("""{"dealNumber":1,"moves":[{"type":"cc","from":0,"to":4}],"elapsedMillis":83000}""")
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasTestTag(ELAPSED_TAG)).fetchSemanticsNodes().isNotEmpty() }
        // Long enough for the backroom's first cloud of smoke to be well on its way across.
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(SMOKE_MILLIS)

        val picture = compose.onRoot().captureToImage().asAndroidBitmap()
        val corner = picture.getPixel(0, picture.height - 1)
        assertNotEquals("Nothing was painted", 0, corner)
        File("build/reports/themes").apply { mkdirs() }.resolve("${theme.name.lowercase()}.png").outputStream().use {
            picture.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it)
        }

        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("2C").performTouchInput {
            down(topLeft + grip)
            up()
        }
        compose.waitUntil(TIMEOUT) { Game.fromJson(saveFile.readText())?.moveCount == 2 }
        assertEquals(theme, Settings(FileStorage(app.filesDir)).theme)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<TableTheme> = TableTheme.entries

        private const val TIMEOUT = 10_000L
        private const val SMOKE_MILLIS = 9_000L
        private const val PNG_QUALITY = 100
        private val grip = Offset(30f, 20f)
    }
}
