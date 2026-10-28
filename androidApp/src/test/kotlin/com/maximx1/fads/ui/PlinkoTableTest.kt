package com.maximx1.fads.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PlinkoBoard
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.game.DenViewModel
import com.maximx1.fads.game.FileStorage
import com.maximx1.fads.game.GameStore
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.den.Coin
import com.maximx1.fads.ui.den.DenGame
import com.maximx1.fads.ui.den.PLINKO_RESULT_TAG
import com.maximx1.fads.ui.den.STAKE_TAG
import com.maximx1.fads.ui.den.chipTag
import com.maximx1.fads.ui.den.tableTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** The plinko board: building up a stake with chips, dropping a ball, and being paid where it lands. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PlinkoTableTest {
    @get:Rule
    val compose = createComposeRule()

    private val files = RuntimeEnvironment.getApplication().filesDir

    @Test
    fun chipsMakeTheStakeAndTheBallPaysWhatItsBucketDoes() {
        val number = 7L
        val storage = FileStorage(files)
        File(files, GameStore.DEN).writeText(Den(purse = Purse(100)).toJson())
        val patience = GameViewModel(GameStore(storage)) { 0L }
        val tables = DenViewModel(GameStore(storage), patience.history) { number }
        compose.setContent { FadsTheme { GameScreen(patience, Settings(storage), tables) } }
        compose.onNodeWithText("Enter the Gambler's Den").performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodes(hasTestTag(tableTag(DenGame.PLINKO))).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(tableTag(DenGame.PLINKO)).performScrollTo().performClick()

        compose.onNodeWithText("Drop").assertIsNotEnabled()
        compose.onNodeWithTag(chipTag(Coin.SILVER)).performClick()
        compose.onNodeWithTag(chipTag(Coin.BRONZE)).performClick()
        compose.onNodeWithTag(STAKE_TAG).assertTextEquals("Stake: 11 bronzes")
        compose.onNodeWithText("Drop").performClick()
        compose.waitForIdle()

        val bucket = PlinkoBoard.path(number).count { it }
        val won = PlinkoBoard.payout(11, bucket)
        val tenths = PlinkoBoard.tenths[bucket]
        val multiplier = if (tenths % 10 == 0) "${tenths / 10}×" else "${tenths / 10}.${tenths % 10}×"
        val result = if (won > 0) "won $won bronzes" else "no win"
        compose.onNodeWithTag(PLINKO_RESULT_TAG).assertTextEquals("Landed in $multiplier: $result")
        assertEquals(Purse(100 - 11 + won), tables.den.value?.purse)
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
