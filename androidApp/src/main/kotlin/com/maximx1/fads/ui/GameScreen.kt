package com.maximx1.fads.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maximx1.fads.R
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.game.GameViewModel

private enum class Prompt { NONE, NEW_GAME, RESTART }

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    var prompt by rememberSaveable { mutableStateOf(Prompt.NONE) }

    LifecycleStartEffect(viewModel) {
        viewModel.onScreen(true)
        onStopOrDispose { viewModel.onScreen(false) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colorResource(R.color.felt))
            // System bars and the camera cutout, but not the keyboard: typing a deal number should not
            // squeeze the board behind the dialog.
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
    ) {
        val current = game ?: return@Column
        TopBar(
            game = current,
            elapsedSeconds = elapsedSeconds,
            onUndo = viewModel::undo,
            onRestart = { prompt = Prompt.RESTART },
            onNewGame = { prompt = Prompt.NEW_GAME },
        )
        Board(state = current.state, onMove = viewModel::play, modifier = Modifier.weight(1f).fillMaxWidth())
    }

    val current = game ?: return
    when (prompt) {
        Prompt.NEW_GAME -> {
            NewGameDialog(
                onDeal = {
                    viewModel.newGame(it)
                    prompt = Prompt.NONE
                },
                onDismiss = { prompt = Prompt.NONE },
            )
        }

        Prompt.RESTART -> {
            RestartDialog(
                dealNumber = current.dealNumber,
                onRestart = {
                    viewModel.restart()
                    prompt = Prompt.NONE
                },
                onDismiss = { prompt = Prompt.NONE },
            )
        }

        Prompt.NONE -> {
            WinDialog(game = current, onNewGame = { viewModel.newGame(GameViewModel.randomDealNumber()) })
        }
    }
}

@Composable
private fun TopBar(
    game: Game,
    elapsedSeconds: Long,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onNewGame: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.game_number, game.dealNumber),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = DateUtils.formatElapsedTime(elapsedSeconds),
                color = Color.White.copy(alpha = DETAIL_ALPHA),
                // Digits of equal width, so the time does not shuffle sideways as it counts.
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.testTag(ELAPSED_TAG),
            )
        }
        TextButton(onClick = onUndo, enabled = game.canUndo) { Text(stringResource(R.string.undo)) }
        TextButton(onClick = onRestart, enabled = game.canUndo) { Text(stringResource(R.string.restart)) }
        TextButton(onClick = onNewGame) { Text(stringResource(R.string.new_game)) }
    }
}

/** Offers a random deal number, which the player can replace with any number they like. */
@Composable
private fun NewGameDialog(
    onDeal: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(GameViewModel.randomDealNumber().toString()) }
    val number = text.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_game_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { typed -> text = typed.filter { it.isDigit() }.take(MAX_DEAL_DIGITS) },
                label = { Text(stringResource(R.string.deal_number_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { number?.let(onDeal) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { number?.let(onDeal) }, enabled = number != null) {
                Text(stringResource(R.string.deal))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RestartDialog(
    dealNumber: Long,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restart_title)) },
        text = { Text(stringResource(R.string.restart_text, dealNumber)) },
        confirmButton = { TextButton(onClick = onRestart) { Text(stringResource(R.string.restart)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Shown once the game is won, until it is closed or the position changes. */
@Composable
private fun WinDialog(
    game: Game,
    onNewGame: () -> Unit,
) {
    var closed by rememberSaveable(game.dealNumber, game.moveCount) { mutableStateOf(false) }
    if (!game.state.isWon || closed) return
    AlertDialog(
        onDismissRequest = { closed = true },
        title = { Text(stringResource(R.string.win_title)) },
        text = {
            val time = DateUtils.formatElapsedTime(game.elapsedMillis / DateUtils.SECOND_IN_MILLIS)
            Text(stringResource(R.string.win_text, game.dealNumber, game.moveCount, time))
        },
        confirmButton = { TextButton(onClick = onNewGame) { Text(stringResource(R.string.win_new_game)) } },
        dismissButton = { TextButton(onClick = { closed = true }) { Text(stringResource(R.string.close)) } },
    )
}

// 18 digits always fits in a Long.
private const val MAX_DEAL_DIGITS = 18
private const val DETAIL_ALPHA = 0.7f

internal const val ELAPSED_TAG = "elapsed"
