package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.SicBo
import com.maximx1.fads.core.den.SicBoBet
import com.maximx1.fads.core.den.SicBoKind
import com.maximx1.fads.core.den.sicBo
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings

/**
 * The sic bo table in [room], dressed in the den's theme: three dice, and a layout of small and big, odd and even,
 * the totals, and bets on each face showing once, twice or three times. A chip is chosen, and a tap on a box puts
 * it down there.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SicBoScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val table = den.sicBo(room)
    val theme = denTheme(den.theme)
    val measurer = rememberTextMeasurer()
    val chips = chipsFor(room)
    var chosen by rememberSaveable { mutableIntStateOf(chips.lastIndex) }
    var landed by rememberSaveable { mutableLongStateOf(table.throws) }
    val rolling = landed != table.throws
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.SIC_BO, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) { drawEmblem(theme, measurer) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TumblingDice(table.dice, DICE, table.throws, theme, onLanded = { landed = it }, size = 48.dp)
                Text(
                    if (rolling || table.dice == null) "" else Strings.diceResult(table.dice.orEmpty(), table.paid),
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp).testTag(SIC_BO_RESULT_TAG),
                )
            }
        }
        Layout(
            table,
            theme,
            onBet = { actions.betSicBo(it, chips[chosen].worth, room) },
            modifier = Modifier.weight(1f),
        )
        BetControls(
            chips,
            chosen,
            ready = table.bets.isNotEmpty() && !rolling,
            act = Strings.ROLL,
            onChoose = { chosen = it },
            onClear = { actions.clearSicBo(room) },
            onAct = { actions.rollSicBo(room) },
        )
    }
}

/** The layout, row by row, scrolling if the screen is short. */
@Composable
private fun Layout(
    table: SicBo,
    theme: DenTheme,
    onBet: (SicBoBet) -> Unit,
    modifier: Modifier,
) {
    val row = @Composable { bets: List<SicBoBet> ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (bet in bets) {
                LayoutBox(
                    Strings.sicBoBet(bet),
                    Strings.sicBoOdds(bet),
                    table.bets[bet] ?: 0,
                    open = true,
                    theme = theme,
                    onTap = { onBet(bet) },
                    modifier = Modifier.weight(1f).testTag(sicBoTag(bet)),
                )
            }
        }
    }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        row(listOf(SicBoBet(SicBoKind.SMALL), SicBoBet(SicBoKind.ANY_TRIPLE), SicBoBet(SicBoKind.BIG)))
        row(listOf(SicBoBet(SicBoKind.ODD), SicBoBet(SicBoKind.EVEN)))
        row((FIRST_TOTAL..MIDDLE_TOTAL).map { SicBoBet(SicBoKind.TOTAL, it) })
        row((MIDDLE_TOTAL + 1..LAST_TOTAL).map { SicBoBet(SicBoKind.TOTAL, it) })
        for (kind in listOf(
            SicBoKind.SINGLE,
            SicBoKind.DOUBLE,
            SicBoKind.TRIPLE,
        )) {
            row((1..FACES).map { SicBoBet(kind, it) })
        }
    }
}

/** Tags the box of the layout for [bet]. */
fun sicBoTag(bet: SicBoBet) = "sicBo-${bet.kind.name.lowercase()}-${bet.value}"

/** Tags what the last throw was and paid. */
const val SIC_BO_RESULT_TAG = "sicBoResult"

private const val DICE = 3
private const val FACES = 6
private const val FIRST_TOTAL = 4
private const val MIDDLE_TOTAL = 10
private const val LAST_TOTAL = 17
