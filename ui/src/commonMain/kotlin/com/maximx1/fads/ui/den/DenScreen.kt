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
import com.maximx1.fads.core.den.Room
import com.maximx1.fads.core.den.canEnter
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
    var shopping by rememberSaveable { mutableStateOf(false) }
    var room by rememberSaveable { mutableStateOf(Room.FLOOR) }
    val theme = denTheme(den?.theme ?: Den.HOUSE_THEME)
    val ways =
        DenWays(
            // Out of the high rollers' room onto the floor, and off the floor out of the den.
            onClose = if (room == Room.FLOOR) onClose else ({ room = Room.FLOOR }),
            onTable = { table = it },
            onShop = { shopping = true },
            onRoom = { room = it },
        )
    val getUp = { table = null }
    // Everything in the den is dressed in its theme: its felt, and the colour of its buttons.
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = theme.accent)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize().drawBehind { drawFelt(theme) }) {
                val sitting = if (den == null) null else table
                when {
                    den == null || (sitting == null && !shopping) -> Menu(den, history, actions, room, ways)
                    sitting == DenGame.ROULETTE -> RouletteScreen(den, room, actions, getUp)
                    sitting == DenGame.PLINKO -> PlinkoScreen(den, room, actions, getUp)
                    sitting == DenGame.SLOTS -> SlotsScreen(den, room, actions, getUp)
                    sitting == DenGame.VIDEO_POKER -> VideoPokerScreen(den, room, actions, getUp)
                    sitting == DenGame.FIVE_CARD_DRAW -> PokerScreen(PokerGame.DRAW, room, den, actions, getUp)
                    sitting == DenGame.TEXAS_HOLDEM -> PokerScreen(PokerGame.HOLDEM, room, den, actions, getUp)
                    else -> ThemeShop(den, actions, onClose = { shopping = false })
                }
            }
        }
    }
}

/** Where the den's buttons go: out, to a table, to the shop of themes, or into a room. */
private class DenWays(
    val onClose: () -> Unit,
    val onTable: (DenGame) -> Unit,
    val onShop: () -> Unit,
    val onRoom: (Room) -> Unit,
)

/**
 * The den's own screen in [room]: the purse and the points to cash in, and the tables. On the floor, the way into
 * the high rollers' room and the theme too.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Menu(
    den: Den?,
    history: History,
    actions: DenActions,
    room: Room,
    ways: DenWays,
) {
    // The system back button or gesture goes where the Back button does.
    BackHandler(onBack = ways.onClose)
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
                if (room == Room.FLOOR) Strings.DEN_TITLE else Strings.HIGH_ROLLERS,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = ways.onClose) { Text(Strings.BACK) }
        }
        Panel { PurseAndPoints(den, history, actions::cashIn) }
        Panel { Tables(room, ways.onTable) }
        if (room == Room.FLOOR) {
            Panel { HighRollers(den, onEnter = { ways.onRoom(Room.HIGH_ROLLERS) }) }
            Panel { Theme(den, ways.onShop) }
        }
    }
}

/** The tables in [room], each with a button to sit down at it. */
@Composable
private fun ColumnScope.Tables(
    room: Room,
    onTable: (DenGame) -> Unit,
) {
    Text(Strings.TABLES, style = MaterialTheme.typography.titleMedium)
    if (room != Room.FLOOR) Text(Strings.leastBet(room.leastBet), style = MaterialTheme.typography.bodySmall)
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

/** The way into the high rollers' room, open only to a big enough purse. */
@Composable
private fun ColumnScope.HighRollers(
    den: Den?,
    onEnter: () -> Unit,
) {
    val room = Room.HIGH_ROLLERS
    Text(Strings.HIGH_ROLLERS, style = MaterialTheme.typography.titleMedium)
    Text(Strings.highRollersText(room.entry, room.leastBet), style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = onEnter,
        enabled = den?.canEnter(room) == true,
        modifier = Modifier.align(Alignment.End).padding(top = 8.dp).testTag(HIGH_ROLLERS_TAG),
    ) { Text(Strings.ENTER) }
}

/** The theme the den is dressed in, and the way to the shop of them. */
@Composable
private fun Theme(
    den: Den?,
    onShop: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            Strings.themeIs(denTheme(den?.theme ?: Den.HOUSE_THEME).name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onShop, modifier = Modifier.testTag(SHOP_TAG)) { Text(Strings.SHOP) }
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

/** Tags the button that goes into the high rollers' room. */
const val HIGH_ROLLERS_TAG = "highRollers"

/** Tags the button that opens the shop of themes. */
const val SHOP_TAG = "shop"

/** Tags the line that says how many points are waiting to be cashed in. */
const val POINTS_TAG = "denPoints"

private val Gutter = 20.dp
private val PanelWidth = 480.dp
