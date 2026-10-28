package com.maximx1.fads.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.maximx1.fads.MainActivity
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.History
import com.maximx1.fads.core.engine.Played
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.POINTS_TAG
import com.maximx1.fads.ui.den.coinTag
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The gambler's den, in the whole app: the ways in and out, and cashing in points for coins. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class DenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun touchScreen() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @After
    fun close() {
        scenario?.close()
    }

    private fun played(
        score: Int,
        won: Boolean,
    ) = Played(GameType.FreeCell, 1, endedAt = 0, millis = 0, moves = 1, score = score, won = won, game = "")

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(ELAPSED_TAG) or hasText("Play")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun savedDen(): Den? = File(files, GameStore.DEN).takeIf { it.exists() }?.readText()?.let(Den::fromJson)

    private fun coins(coin: Coin) = compose.onNodeWithTag(coinTag(coin))

    @Test
    fun wonPointsAreCashedInForCoinsAndTheRestWaits() {
        val history = History(listOf(played(2_550, won = true), played(900, won = false), played(1_034, won = true)))
        File(files, GameStore.HISTORY).writeText(history.toJson())
        launch()

        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasText("Points to cash in: 3,584")).fetchSemanticsNodes().isNotEmpty()
        }
        coins(Coin.GOLD).assertTextEquals("× 0")

        compose.onNodeWithText("Cash in").performClick()
        compose.onNode(hasText("Trade 3,500 points for 35 bronzes?") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNode(hasText("Cash in") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()

        coins(Coin.GOLD).assertTextEquals("× 1")
        coins(Coin.SILVER).assertTextEquals("× 1")
        coins(Coin.BRONZE).assertTextEquals("× 0")
        compose.onNodeWithTag(POINTS_TAG).assertTextEquals("Points to cash in: 84")
        compose.onNodeWithText("Cash in").assertIsNotEnabled()
        compose.waitUntil(TIMEOUT) { savedDen() == Den(cashedIn = 3_500, purse = Purse(35)) }

        // Out the way it came in, to the choice of game.
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Pick a game").assertExists()
    }

    @Test
    fun theDenOpensFromThePauseMenuAndGoesBackToIt() {
        File(files, GameStore.GAME).writeText("""{"dealNumber":1}""")
        File(files, GameStore.DEN).writeText(Den(purse = Purse(Purse.GOLD * 4 + 3)).toJson())
        launch()

        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNode(hasText("Gambler's Den") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(
            TIMEOUT,
        ) { compose.onAllNodes(hasTestTag(coinTag(Coin.GOLD))).fetchSemanticsNodes().isNotEmpty() }
        coins(Coin.GOLD).assertTextEquals("× 4")
        coins(Coin.BRONZE).assertTextEquals("× 3")
        compose.onNodeWithTag(POINTS_TAG).assertTextEquals("Points to cash in: 0")

        compose.onNodeWithText("Back").performClick()
        compose.onNode(hasText("Paused") and hasAnyAncestor(isDialog())).assertExists()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
