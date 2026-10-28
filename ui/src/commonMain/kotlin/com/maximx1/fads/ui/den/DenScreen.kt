package com.maximx1.fads.ui.den

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.core.den.PokerGame
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.core.engine.History
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings

/**
 * The gambler's den: the player's purse, the points won at patience waiting to be cashed in for coins, and the
 * tables to play at, which open over it. [den] is `null` while it is read. [history] holds the games that earned
 * the points.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DenScreen(
    den: Den?,
    history: History,
    actions: DenActions,
    onClose: () -> Unit,
) {
    var table by rememberSaveable { mutableStateOf<DenGame?>(null) }
    val sitting = table
    if (sitting != null && den != null) {
        when (sitting) {
            DenGame.ROULETTE -> RouletteScreen(den, actions, onClose = { table = null })
            DenGame.FIVE_CARD_DRAW -> PokerScreen(PokerGame.DRAW, den, actions, onClose = { table = null })
            DenGame.TEXAS_HOLDEM -> PokerScreen(PokerGame.HOLDEM, den, actions, onClose = { table = null })
        }
        return
    }

    // The system back button or gesture goes where the Back button does.
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().drawBehind { drawRect(Felt) }) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Menu(den, history, actions, onClose) {
                table =
                    it
            }
        }
    }
}

/** The den's own screen: the purse and the points to cash in, and the tables. [onTable] sits down at one. */
@Composable
private fun Menu(
    den: Den?,
    history: History,
    actions: DenActions,
    onClose: () -> Unit,
    onTable: (DenGame) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = PanelWidth)) {
            Text(
                Strings.DEN_TITLE,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        Panel { PurseAndPoints(den, history, actions::cashIn) }
        Panel {
            Text(Strings.TABLES, style = MaterialTheme.typography.titleMedium)
            for (game in DenGame.entries) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(game.label, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onTable(game) }, modifier = Modifier.testTag(tableTag(game))) {
                        Text(Strings.PLAY)
                    }
                }
            }
        }
    }
}

/** The coins in [den]'s purse, and the points won in [history] waiting to be cashed in for more. */
@Composable
private fun ColumnScope.PurseAndPoints(
    den: Den?,
    history: History,
    onCashIn: () -> Unit,
) {
    var asking by rememberSaveable { mutableStateOf(false) }
    val points = den?.pointsLeft(history) ?: 0
    val bronzes = den?.bronzesToCashIn(history) ?: 0
    PurseCoins(den?.purse ?: Purse())
    Text(Strings.pointsToCashIn(points), modifier = Modifier.padding(top = Gutter).testTag(POINTS_TAG))
    if (bronzes == 0L) Text(Strings.NOTHING_TO_CASH_IN, style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = { asking = true },
        enabled = bronzes > 0,
        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
    ) { Text(Strings.CASH_IN) }

    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(Strings.CASH_IN) },
            text = { Text(Strings.cashInText(bronzes * Den.POINTS_PER_BRONZE, bronzes)) },
            confirmButton = {
                TextButton(onClick = {
                    onCashIn()
                    asking = false
                }) { Text(Strings.CASH_IN) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(Strings.CANCEL) } },
        )
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        modifier = Modifier.padding(top = Gutter).widthIn(max = PanelWidth).fillMaxWidth(),
    ) {
        Column(Modifier.padding(Gutter), content = content)
    }
}

/** Tags the button that opens [game]'s table. */
fun tableTag(game: DenGame) = "table-${game.name.lowercase()}"

/** Tags the line that says how many points are waiting to be cashed in. */
const val POINTS_TAG = "denPoints"

private val Felt = Brush.radialGradient(listOf(Color(0xFF22603A), Color(0xFF0A2214)))
private val Gutter = 20.dp
private val PanelWidth = 480.dp
