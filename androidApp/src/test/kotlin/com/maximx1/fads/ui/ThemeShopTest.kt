package com.maximx1.fads.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.SHOP_TAG
import com.maximx1.fads.ui.den.themeTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The shop of den themes: buying one, being too short to, and putting one up again. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ThemeShopTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private lateinit var tables: DenViewModel

    private fun openTheShop(den: Den) {
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(den.toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        tables = DenViewModel(GameStore(storage), patience.history)
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasTestTag(SHOP_TAG)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Theme: House").assertExists()
        compose.onNodeWithTag(SHOP_TAG).performClick()
    }

    private fun tapTheme(key: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(themeTag(key)))
        compose.onNodeWithTag(themeTag(key)).performClick()
    }

    private fun inDialog(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    @Test
    fun aThemeIsBoughtAndPutUpAndOneTooDearIsNot() {
        openTheShop(Den(purse = Purse(1_500)))
        tapTheme("pirate")
        inDialog("Buy Pirate Ship for 1,000 bronzes?").assertExists()
        compose.onNode(hasText("Buy") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertEquals(Purse(500), tables.den.value?.purse)
        assertEquals("pirate", tables.den.value?.theme)

        tapTheme("samurai")
        inDialog("You need 500 bronzes more.").assertExists()
        compose.onNode(hasText("Buy") and hasClickAction() and hasAnyAncestor(isDialog())).assertIsNotEnabled()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()

        // The house theme is everyone's, and goes back up with a tap.
        tapTheme(Den.HOUSE_THEME)
        compose.waitForIdle()
        assertEquals(Den.HOUSE_THEME, tables.den.value?.theme)
        assertEquals(setOf("pirate"), tables.den.value?.themes)
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Theme: House").assertExists()
    }

    @Test
    fun everyThemeHasAKeyOfItsOwnAndThereAreFiftyToBuy() {
        val keys =
            com.maximx1.fads.ui.den.DenThemes
                .map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(Den.HOUSE_THEME, keys.first())
        assertEquals(50, keys.size - 1)
        assertEquals(
            "House",
            com.maximx1.fads.ui.den
                .denTheme("no such theme")
                .name,
        )
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
