package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.score
import com.maximx1.fads.core.rules.canAutoComplete
import com.maximx1.fads.core.rules.stuckCards
import com.maximx1.fads.core.state.GameState
import com.maximx1.fads.game.GameViewModel
import com.maximx1.fads.ui.theme.ClockLook
import com.maximx1.fads.ui.theme.look
import kotlinx.coroutines.delay

private enum class Prompt { NONE, PAUSED, NEW_GAME, RESTART }

/** The whole game: the table, and whichever dialog is up over it. */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settings: Settings,
) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    var prompt by rememberSaveable { mutableStateOf(Prompt.NONE) }

    // The clock stops behind the pause dialog just as it does when the app goes out of sight.
    val paused = prompt == Prompt.PAUSED
    LifecycleStartEffect(viewModel, paused) {
        viewModel.onScreen(!paused)
        onStopOrDispose { viewModel.onScreen(false) }
    }

    Table(game, elapsedSeconds, settings, viewModel, onPrompt = { prompt = it })

    val current = game ?: return
    // The dialogs keep their dark panels in every theme and take only the colour of their buttons from it.
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = settings.theme.look.accent)) {
        when (prompt) {
            Prompt.PAUSED -> {
                PauseDialog(current, elapsedSeconds, records, settings, onResume = { prompt = Prompt.NONE })
            }

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
                WinDialog(current, records, onNewGame = { viewModel.newGame(GameViewModel.randomDealNumber()) })
            }
        }
    }
}

/** The whole screen but for the dialogs: the theme's table, the game on it, and whatever the theme hangs over it. */
@Composable
private fun Table(
    game: Game?,
    elapsedSeconds: Long,
    settings: Settings,
    viewModel: GameViewModel,
    onPrompt: (Prompt) -> Unit,
) {
    val look = settings.theme.look
    Box(Modifier.fillMaxSize()) {
        // A layer of its own, so the table is not painted again every time a card moves.
        Spacer(Modifier.fillMaxSize().graphicsLayer().drawBehind { with(look) { drawTable() } })
        if (game != null) PlayArea(game, elapsedSeconds, settings, viewModel, onPrompt)
        look.Overlay(Modifier.fillMaxSize())
    }
}

/** The clock and move count at the top, the board, and the split button at the bottom. */
@Composable
private fun PlayArea(
    game: Game,
    elapsedSeconds: Long,
    settings: Settings,
    viewModel: GameViewModel,
    onPrompt: (Prompt) -> Unit,
) {
    val look = settings.theme.look
    val state = game.state
    val suggestion = remember(state, game.moveCount) { game.hint() }
    val finishable = remember(state) { state.canAutoComplete }
    // The position a hint was asked for. The hint shows only while that is still the position on the table.
    var hintFor by remember { mutableStateOf<GameState?>(null) }
    LaunchedEffect(hintFor) {
        if (hintFor != null) {
            delay(HINT_MILLIS)
            hintFor = null
        }
    }
    val dim = settings.dimsStuckCards
    val stuck = remember(state, dim) { if (dim) state.stuckCards() else emptySet() }
    val hint = suggestion.takeIf { hintFor === state }
    val cards = rememberCardStyle(look::cards)

    Column(
        // System bars and the camera cutout, but not the keyboard: typing a deal number should not squeeze the
        // board behind the dialog.
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopRow(elapsedSeconds, game.score, game.movesMade, look.clock)
        Board(
            state = state,
            onMove = viewModel::play,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(look.boardPadding),
            paint = remember(cards, look) { BoardPaint(cards, look.slotColor, look.accent, look.pane) },
            marks = remember(stuck, hint) { BoardMarks(stuck, hint) },
        )
        SplitButton(
            segments =
                listOf(
                    Segment(Strings.UNDO, Glyph.UNDO, game.canUndo, viewModel::undo),
                    Segment(Strings.AUTO_COMPLETE, Glyph.AUTO_COMPLETE, finishable, viewModel::autoComplete),
                    Segment(Strings.RESTART, Glyph.RESTART, game.canUndo) { onPrompt(Prompt.RESTART) },
                    Segment(Strings.NEW_GAME, Glyph.NEW_GAME) { onPrompt(Prompt.NEW_GAME) },
                    Segment(Strings.HINT, Glyph.HINT, suggestion != null) {
                        // Asking again while the hint is still showing costs nothing.
                        if (hintFor !== state) viewModel.takeHint()
                        hintFor = state
                    },
                    Segment(Strings.PAUSE, Glyph.PAUSE) { onPrompt(Prompt.PAUSED) },
                ),
            look = look.buttons,
            modifier = Modifier.padding(BarPadding).widthIn(max = MaxBarWidth).fillMaxWidth(),
        )
    }
}

/** The score on the left, the clock in the middle and the move count on the right. */
@Composable
private fun TopRow(
    elapsedSeconds: Long,
    score: Int,
    movesMade: Int,
    look: ClockLook,
) {
    // Digits of equal width, so neither number shuffles sideways as it counts.
    val steady = look.text.copy(fontFeatureSettings = "tnum")
    Box(Modifier.fillMaxWidth()) {
        Text(
            text = Strings.score(score),
            style = MaterialTheme.typography.bodyMedium.merge(steady),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = BarPadding).testTag(SCORE_TAG),
        )
        Text(
            text = clockText(elapsedSeconds),
            style = MaterialTheme.typography.titleMedium.merge(steady),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(vertical = ClockGap)
                    .drawBehind { look.plate?.invoke(this) }
                    .padding(horizontal = PlatePadding, vertical = 2.dp)
                    .testTag(ELAPSED_TAG),
        )
        Text(
            text = Strings.moveCount(movesMade),
            style = MaterialTheme.typography.bodyMedium.merge(steady),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = BarPadding).testTag(MOVES_TAG),
        )
    }
}

private val ClockGap = 4.dp
private val PlatePadding = 14.dp
private val BarPadding = 12.dp
private val MaxBarWidth = 520.dp
private const val HINT_MILLIS = 4_000L

const val ELAPSED_TAG = "elapsed"
const val MOVES_TAG = "moves"
const val SCORE_TAG = "score"
