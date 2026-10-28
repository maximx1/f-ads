package com.maximx1.fads.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.maximx1.fads.core.den.Bet
import com.maximx1.fads.core.den.BetKind
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.roulette
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.HIGH_ROLLERS_TAG
import com.maximx1.fads.ui.den.LAYOUT_TAG
import com.maximx1.fads.ui.den.RouletteLayout
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The high rollers' room: who gets in, and the size of the bets there. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class HighRollersTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir
    private lateinit var tables: DenViewModel

    private fun enterTheDen(bronzes: Long) {
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(bronzes)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        tables = DenViewModel(GameStore(storage), patience.history)
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(
            TIMEOUT,
        ) { compose.onAllNodes(hasTestTag(HIGH_ROLLERS_TAG)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aPurseUnderTwentyFiveThousandIsNotLetIn() {
        enterTheDen(24_999)
        compose.onNodeWithTag(HIGH_ROLLERS_TAG).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun insideEveryBetIsAThousandAtLeastAndBackLeadsOutARoomAtATime() {
        enterTheDen(30_000)
        compose
            .onNodeWithTag(HIGH_ROLLERS_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithText("The High Rollers' Room").assertExists()
        compose.onNodeWithText("Every bet starts at 1,000 bronzes.").assertExists()

        compose.onNodeWithTag(tableTag(DenGame.ROULETTE)).performClick()
        compose.onNodeWithText("High Rollers' Roulette").assertExists()
        val (across, down) = RouletteLayout.centreOf(Bet(BetKind.RED))
        compose.onNodeWithTag(LAYOUT_TAG).performTouchInput {
            click(
                Offset(
                    (across / RouletteLayout.WIDTH * width).toFloat(),
                    (down / RouletteLayout.HEIGHT * height).toFloat(),
                ),
            )
        }
        compose.onNodeWithText("On the table: 1,000 bronzes").assertExists()
        assertEquals(
            1_000L,
            tables.den.value
                ?.roulette(Room.HIGH_ROLLERS)
                ?.staked,
        )
        assertEquals(
            0L,
            tables.den.value
                ?.roulette
                ?.staked,
        )

        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("The High Rollers' Room").assertExists()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("The Gambler's Den").assertExists()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
