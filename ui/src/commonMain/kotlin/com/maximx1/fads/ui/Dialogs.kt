package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.engine.BestTimes
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.Win
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.theme.TableTheme

/**
 * Up while the player has stopped the clock. It names the deal on the table, shows the best times, and is where
 * the theme and the other settings are changed.
 */
@Composable
internal fun PauseDialog(
    game: Game,
    elapsedSeconds: Long,
    bestTimes: BestTimes,
    settings: Settings,
    onResume: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text(Strings.PAUSED) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(Strings.gameNumber(game.dealNumber), style = MaterialTheme.typography.titleMedium)
                Text(Strings.time(clockText(elapsedSeconds)))
                bestTimes.fastest?.let { Text(Strings.bestTime(clockTextOfMillis(it.millis))) }
                bestTimes.bestOn(game.dealNumber)?.let { Text(Strings.bestOnDeal(clockTextOfMillis(it))) }
                Text(
                    text = Strings.THEME,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = SectionGap),
                )
                ThemeChoice(settings.theme, onChoose = { settings.theme = it })
                DimChoice(settings.dimsStuckCards, onChoose = { settings.dimsStuckCards = it })
            }
        },
        confirmButton = { TextButton(onClick = onResume) { Text(Strings.RESUME) } },
    )
}

/** One radio button for each theme. Picking one puts it on the table straight away, behind the dialog. */
@Composable
private fun ThemeChoice(
    chosen: TableTheme,
    onChoose: (TableTheme) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        for (theme in TableTheme.entries) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = theme == chosen, role = Role.RadioButton, onClick = { onChoose(theme) })
                    .padding(vertical = ChoicePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = theme == chosen, onClick = null)
                Text(theme.label, Modifier.padding(start = ChoicePadding))
            }
        }
    }
}

/** The switch for drawing cards that cannot move darker than the rest. */
@Composable
private fun DimChoice(
    dims: Boolean,
    onChoose: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = dims, role = Role.Switch, onValueChange = onChoose)
            .padding(top = SectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(Strings.DIM_STUCK_CARDS, Modifier.weight(1f))
        Switch(checked = dims, onCheckedChange = null)
    }
}

/** Deals a random game, or the one whose number the player types. */
@Composable
internal fun NewGameDialog(
    onDeal: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val number = text.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.NEW_GAME_TITLE) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { typed -> text = typed.filter { it.isDigit() }.take(MAX_DEAL_DIGITS) },
                label = { Text(Strings.DEAL_NUMBER) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { number?.let(onDeal) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { number?.let(onDeal) }, enabled = number != null) { Text(Strings.DEAL) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.CANCEL) }
            TextButton(onClick = { onDeal(GameViewModel.randomDealNumber()) }) { Text(Strings.RANDOM_DEAL) }
        },
    )
}

@Composable
internal fun RestartDialog(
    dealNumber: Long,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.RESTART_TITLE) },
        text = { Text(Strings.restartText(dealNumber)) },
        confirmButton = { TextButton(onClick = onRestart) { Text(Strings.RESTART) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.CANCEL) } },
    )
}

/** Shown once the game is won, until it is closed or the position changes. */
@Composable
internal fun WinDialog(
    game: Game,
    bestTimes: BestTimes,
    onNewGame: () -> Unit,
) {
    var closed by rememberSaveable(game.dealNumber, game.moveCount) { mutableStateOf(false) }
    if (!game.state.isWon || closed) return
    AlertDialog(
        onDismissRequest = { closed = true },
        title = { Text(Strings.WIN_TITLE) },
        text = {
            Column {
                Text(Strings.winText(game.dealNumber, game.movesMade, clockTextOfMillis(game.elapsedMillis)))
                val fastest = bestTimes.fastest
                when {
                    fastest == null -> Unit
                    fastest == Win(game.dealNumber, game.elapsedMillis, game.movesMade) -> Text(Strings.NEW_BEST_TIME)
                    else -> Text(Strings.bestTime(clockTextOfMillis(fastest.millis)))
                }
            }
        },
        confirmButton = { TextButton(onClick = onNewGame) { Text(Strings.WIN_NEW_GAME) } },
        dismissButton = { TextButton(onClick = { closed = true }) { Text(Strings.CLOSE) } },
    )
}

// 18 digits always fits in a Long.
private const val MAX_DEAL_DIGITS = 18
private val SectionGap = 16.dp
private val ChoicePadding = 10.dp
