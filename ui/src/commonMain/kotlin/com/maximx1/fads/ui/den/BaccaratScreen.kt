package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.BaccaratSide
import com.maximx1.fads.core.den.Coup
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.baccarat
import com.maximx1.fads.core.den.baccaratTotal
import com.maximx1.fads.core.model.Card
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.Strings
import com.maximx1.fads.ui.rememberPlaceholderCardStyle

/**
 * The baccarat table in [room], dressed in the den's theme: the last coup's player and banker hands, and boxes for
 * bets on the player, a tie and the banker, edged in the accent. A chip is chosen, and a tap on a box puts it down.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BaccaratScreen(
    den: Den,
    room: Room,
    actions: DenActions,
    onClose: () -> Unit,
) {
    val table = den.baccarat(room)
    val theme = denTheme(den.theme)
    val faces = rememberPlaceholderCardStyle()
    val style = remember(faces, theme) { theme.cards(faces) }
    val measurer = rememberTextMeasurer()
    val chips = chipsFor(room)
    var chosen by rememberSaveable { mutableIntStateOf(chips.lastIndex) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.tableName(Strings.BACCARAT, room),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(Modifier.fillMaxSize()) { drawEmblem(theme, measurer) }
            val width = minOf(maxWidth / CARDS_ACROSS, CARD_MOST)
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Side(Strings.baccaratSide(BaccaratSide.PLAYER), table.last?.player, style, width)
                Text(
                    table.last?.let {
                        Strings.coupResult(
                            it.won,
                            baccaratTotal(it.player),
                            baccaratTotal(it.banker),
                            it.paid,
                        )
                    }
                        ?: "",
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(BACCARAT_RESULT_TAG),
                )
                Side(Strings.baccaratSide(BaccaratSide.BANKER), table.last?.banker, style, width)
            }
        }
        Boxes(table.bets, theme, onBet = { actions.betBaccarat(it, chips[chosen].worth, room) })
        BetControls(
            chips,
            chosen,
            ready = table.bets.isNotEmpty(),
            act = Strings.DEAL,
            onChoose = { chosen = it },
            onClear = { actions.clearBaccarat(room) },
            onAct = { actions.playBaccarat(room) },
        )
    }
}

/** One side's hand from the last coup, under its name and total. */
@Composable
private fun Side(
    name: String,
    cards: List<Card>?,
    style: CardStyle,
    width: Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (cards == null) name else Strings.sideTotal(name, baccaratTotal(cards)), fontWeight = FontWeight.Bold)
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (cards ?: listOf(null, null)).forEach { PlayingCard(it, style, width) }
        }
    }
}

/** The boxes to bet in: player, tie and banker, each with its odds and what is on it. */
@Composable
private fun Boxes(
    bets: Map<BaccaratSide, Long>,
    theme: DenTheme,
    onBet: (BaccaratSide) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (side in listOf(BaccaratSide.PLAYER, BaccaratSide.TIE, BaccaratSide.BANKER)) {
            Column(
                Modifier
                    .weight(1f)
                    .background(theme.feltDeep, RoundedCornerShape(10.dp))
                    .border(2.dp, theme.accent, RoundedCornerShape(10.dp))
                    .clickable { onBet(side) }
                    .padding(8.dp)
                    .testTag(sideTag(side)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Strings.baccaratSide(side), fontWeight = FontWeight.Bold)
                Text(Strings.baccaratOdds(side), color = theme.accent, style = MaterialTheme.typography.labelMedium)
                Text(Strings.stack(bets[side] ?: 0), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Tags the box for bets on [side]. */
fun sideTag(side: BaccaratSide) = "baccarat-${side.name.lowercase()}"

/** Tags how the last coup went. */
const val BACCARAT_RESULT_TAG = "baccaratResult"

private val CARD_MOST = 72.dp

private const val CARDS_ACROSS = 5f
