package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.engine.Game
import com.maximx1.fads.core.engine.Played
import com.maximx1.fads.core.engine.positions
import com.maximx1.fads.ui.theme.Look
import kotlinx.coroutines.delay

/**
 * A finished game played back move by move on its own table, which takes no touches. The player steps back and
 * forward, or lets it play at one of a few speeds.
 */
@OptIn(ExperimentalComposeUiApi::class) // For the back handler, which is still marked experimental.
@Composable
internal fun WatchScreen(
    played: Played,
    look: Look,
    onClose: () -> Unit,
) {
    val game = remember(played) { played.load() }
    // The system back button or gesture goes where the Back button does.
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize()) {
        Spacer(Modifier.fillMaxSize().graphicsLayer().drawBehind { with(look) { drawTable() } })
        if (game == null) {
            Surface(Modifier.align(Alignment.Center).padding(Gutter)) {
                Column(Modifier.padding(Gutter)) {
                    Text(Strings.CANNOT_WATCH)
                    TextButton(onClick = onClose) { Text(Strings.BACK) }
                }
            }
        } else {
            Replay(game, played, look, onClose)
        }
    }
}

/** Where a playback stands: which of the positions up to [last] is showing, and whether and how fast it plays. */
@Stable
private class Playback(
    val last: Int,
) {
    var step by mutableIntStateOf(0)
    var playing by mutableStateOf(false)
    var speed by mutableIntStateOf(1)

    fun go(to: Int) {
        playing = false
        step = to.coerceIn(0, last)
    }

    fun faster() {
        speed = if (speed >= MAX_SPEED) 1 else speed * 2
    }
}

@Composable
private fun <S : Any, M : Any> Replay(
    game: Game<S, M>,
    played: Played,
    look: Look,
    onClose: () -> Unit,
) {
    val positions = remember(game) { game.positions() }
    val playback = remember(played) { Playback(positions.lastIndex) }
    LaunchedEffect(playback.playing, playback.speed, playback.step) {
        if (!playback.playing) return@LaunchedEffect
        if (playback.step >= playback.last) {
            playback.playing = false
        } else {
            delay(STEP_MILLIS / playback.speed)
            playback.step += 1
        }
    }
    val layout = remember(game.rules) { tableLayoutFor(game.rules) }
    val cards = rememberCardStyle(look::cards)

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            Strings.dealOf(fullName(played.type), played.dealNumber),
            style = MaterialTheme.typography.titleMedium.merge(look.clock.text),
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(look.boardPadding)) {
            CardTable(
                state = positions[playback.step],
                layout = layout,
                onMove = {},
                modifier = Modifier.fillMaxSize(),
                paint = remember(cards, look) { BoardPaint(cards, look.slotColor, look.accent, look.pane) },
            )
            // Over the cards, so they cannot be picked up: this is to watch, not to play.
            Box(
                Modifier.fillMaxSize().pointerInput(
                    Unit,
                ) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            )
        }
        Text(
            Strings.moveOf(playback.step, playback.last),
            style = MaterialTheme.typography.bodyMedium.merge(look.clock.text),
            modifier = Modifier.testTag(WATCH_STEP_TAG),
        )
        Controls(playback, onClose)
    }
}

/** Back to the start, a step back, play or pause, a step forward, the speed, and back to the history. */
@Composable
private fun Controls(
    playback: Playback,
    onClose: () -> Unit,
) {
    val step = playback.step
    Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.padding(Gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Control(Strings.TO_START, "⏮", enabled = step > 0) { playback.go(0) }
            Control(Strings.STEP_BACK, "◀", enabled = step > 0) { playback.go(step - 1) }
            if (playback.playing) {
                Control(Strings.PAUSE, "⏸") { playback.playing = false }
            } else {
                Control(Strings.PLAY, "▶", enabled = step < playback.last) { playback.playing = true }
            }
            Control(Strings.STEP_FORWARD, "▶|", enabled = step < playback.last) { playback.go(step + 1) }
            Control(Strings.SPEED, Strings.speed(playback.speed), onClick = playback::faster)
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
    }
}

@Composable
private fun Control(
    label: String,
    symbol: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = label }) {
        Text(symbol)
    }
}

const val WATCH_STEP_TAG = "watchStep"

private val Gutter = 16.dp
private const val STEP_MILLIS = 600L
private const val MAX_SPEED = 4
