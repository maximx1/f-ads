package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Records
import com.maximx1.fads.core.engine.Win
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.theme.TableTheme

/**
 * Up while the player has stopped the clock. It shows the game on the table and the records for it, and is
 * where the theme and the other settings are changed.
 */
@Composable
internal fun PauseDialog(
    game: Game<*, *>,
    records: Records,
    settings: Settings,
    onResume: () -> Unit,
    onHistory: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text(Strings.PAUSED) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Heading(Strings.THIS_GAME, first = true)
                Text(fullName(game.type), style = MaterialTheme.typography.titleMedium)
                Text(Strings.gameNumber(game.dealNumber))
                // The clock has stopped, so the game holds all the time spent on it.
                Text(Strings.time(clockTextOfMillis(game.elapsedMillis)))
                Text(Strings.moveCount(game.movesMade))
                Text(Strings.score(game.score))

                val bests =
                    listOfNotNull(
                        records.fastest?.let { Strings.bestTime(clockTextOfMillis(it.millis)) },
                        records.bestOn(game.dealNumber)?.let { Strings.bestOnDeal(clockTextOfMillis(it)) },
                        records.highest?.let { Strings.highScore(it.score) },
                        records.highestOn(game.dealNumber)?.let { Strings.highestOnDeal(it) },
                    )
                if (bests.isNotEmpty()) {
                    Heading(Strings.RECORDS)
                    for (line in bests) Text(line)
                }

                Heading(Strings.SETTINGS)
                ThemeChoice(settings.theme, onChoose = { settings.theme = it })
                DimChoice(settings.dimsStuckCards, onChoose = { settings.dimsStuckCards = it })
            }
        },
        confirmButton = { TextButton(onClick = onResume) { Text(Strings.RESUME) } },
        dismissButton = { TextButton(onClick = onHistory) { Text(Strings.HISTORY) } },
    )
}

/** The name of a section of a dialog, with room above it unless it is the [first]. */
@Composable
private fun Heading(
    text: String,
    first: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = if (first) 0.dp else SectionGap, bottom = HeadingGap),
    )
}

/**
 * A dropdown of the themes: a button showing the one on the table, which opens a menu of all of them. Picking one
 * puts it on the table straight away, behind the dialog. (Material's own exposed dropdown never settles inside a
 * scrolling dialog, so this is a plain menu.)
 */
@Composable
private fun ThemeChoice(
    chosen: TableTheme,
    onChoose: (TableTheme) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Text(Strings.THEME, style = MaterialTheme.typography.labelMedium)
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(chosen.label, Modifier.weight(1f))
            Text(DROPDOWN_ARROW)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (theme in TableTheme.entries) {
                DropdownMenuItem(
                    text = { Text(theme.label) },
                    onClick = {
                        onChoose(theme)
                        open = false
                    },
                )
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

/**
 * Deals a random game, or the one whose number the player types, of whichever game the player picks. It starts on
 * the game being [playing].
 */
@Composable
internal fun NewGameDialog(
    playing: GameType,
    onDeal: (GameType, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var chosen by rememberSaveable(stateSaver = GameTypeSaver) { mutableStateOf(playing) }
    val number = text.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.NEW_GAME_TITLE) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GameChoice(chosen, onChoose = { chosen = it })
                OutlinedTextField(
                    value = text,
                    onValueChange = { typed -> text = typed.filter { it.isDigit() }.take(MAX_DEAL_DIGITS) },
                    label = { Text(Strings.DEAL_NUMBER) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { number?.let { onDeal(chosen, it) } }),
                    modifier = Modifier.padding(top = SectionGap),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { number?.let { onDeal(chosen, it) } },
                enabled = number != null,
            ) { Text(Strings.DEAL) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.CANCEL) }
            TextButton(onClick = { onDeal(chosen, GameViewModel.randomDealNumber()) }) { Text(Strings.RANDOM_DEAL) }
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

/**
 * Shown once the game is won, until it is closed or the position changes. New game deals a random game of whichever
 * game is picked in it, which starts on the one just won.
 */
@Composable
internal fun WinDialog(
    game: Game<*, *>,
    records: Records,
    onNewGame: (GameType) -> Unit,
) {
    var closed by rememberSaveable(game.dealNumber, game.moveCount) { mutableStateOf(false) }
    var next by rememberSaveable(game.type, stateSaver = GameTypeSaver) { mutableStateOf(game.type) }
    if (!game.isWon || closed) return
    // Over the table and under the dialog, which has a window of its own.
    Fireworks(Modifier.fillMaxSize())
    AlertDialog(
        onDismissRequest = { closed = true },
        title = { Text(Strings.WIN_TITLE) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(Strings.winText(game.dealNumber, game.movesMade, clockTextOfMillis(game.elapsedMillis)))
                Text(Strings.score(game.score))
                val win = Win(game.dealNumber, game.elapsedMillis, game.movesMade, game.score)
                val fastest = records.fastest
                when {
                    fastest == null -> Unit
                    fastest == win -> Text(Strings.NEW_BEST_TIME)
                    else -> Text(Strings.bestTime(clockTextOfMillis(fastest.millis)))
                }
                val highest = records.highest
                when {
                    highest == null -> Unit
                    highest == win -> Text(Strings.NEW_HIGH_SCORE)
                    else -> Text(Strings.highScore(highest.score))
                }
                Heading(Strings.NEXT_GAME)
                GameChoice(next, onChoose = { next = it })
            }
        },
        confirmButton = { TextButton(onClick = { onNewGame(next) }) { Text(Strings.WIN_NEW_GAME) } },
        dismissButton = { TextButton(onClick = { closed = true }) { Text(Strings.CLOSE) } },
    )
}

// 18 digits always fits in a Long.
private const val MAX_DEAL_DIGITS = 18
private val SectionGap = 16.dp
private val HeadingGap = 4.dp
private const val DROPDOWN_ARROW = "▾"
