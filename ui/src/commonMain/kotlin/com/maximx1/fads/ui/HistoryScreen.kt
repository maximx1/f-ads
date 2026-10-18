package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.History
import com.maximx1.fads.core.engine.Played
import kotlin.time.Instant

/**
 * Every game played, newest first, to search by deal number and narrow to one game or to wins. Each can be
 * played again from its first card, or watched move by move as it was played.
 */
@OptIn(ExperimentalComposeUiApi::class) // For the back handler, which is still marked experimental.
@Composable
internal fun HistoryScreen(
    history: History,
    now: Long,
    onPlayAgain: (Played) -> Unit,
    onWatch: (Played) -> Unit,
    onClose: () -> Unit,
) {
    var number by rememberSaveable { mutableStateOf("") }
    var game by rememberSaveable { mutableStateOf<String?>(null) }
    var wonOnly by rememberSaveable { mutableStateOf(false) }
    val games = remember { GameType.all.map(::gameName).distinct() }
    val found =
        remember(history, number, game, wonOnly) {
            history.search(number, type = { game == null || gameName(it) == game }, wonOnly = wonOnly)
        }

    // The system back button or gesture goes where the Back button does.
    BackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Gutter)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.HISTORY, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text(Strings.BACK) }
            }
            OutlinedTextField(
                value = number,
                onValueChange = { typed -> number = typed.filter { it.isDigit() } },
                label = { Text(Strings.SEARCH_DEALS) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(HISTORY_SEARCH_TAG),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
                modifier = Modifier.padding(vertical = ChipGap),
            ) {
                item { Chip(Strings.ALL_GAMES, selected = game == null) { game = null } }
                items(games) { name -> Chip(name, selected = game == name) { game = name } }
                item { Chip(Strings.WON_ONLY, selected = wonOnly) { wonOnly = !wonOnly } }
            }
            if (found.isEmpty()) {
                Text(
                    if (history.games.isEmpty()) Strings.NO_GAMES_YET else Strings.NOTHING_FOUND,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = Gutter),
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                items(found) { played ->
                    PlayedRow(played, now, onPlayAgain, onWatch)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) = FilterChip(selected = selected, onClick = onClick, label = { Text(label) })

@Composable
private fun PlayedRow(
    played: Played,
    now: Long,
    onPlayAgain: (Played) -> Unit,
    onWatch: (Played) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = RowPadding).testTag(HISTORY_ROW_TAG)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Strings.dealOf(fullName(played.type), played.dealNumber),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (played.won) Strings.WON else Strings.NOT_WON,
                style = MaterialTheme.typography.labelLarge,
                color = with(MaterialTheme.colorScheme) { if (played.won) primary else onSurfaceVariant },
            )
        }
        val ended = whenText(played.endedAt, now)
        Text(
            Strings.playedLine(ended, clockTextOfMillis(played.millis), played.moves, played.score),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row {
            TextButton(onClick = { onPlayAgain(played) }) { Text(Strings.PLAY_AGAIN) }
            TextButton(onClick = { onWatch(played) }) { Text(Strings.WATCH) }
        }
    }
}

/** When [then] was, seen from [now]: a while ago for the last week, and the date (UTC) before that. */
internal fun whenText(
    then: Long,
    now: Long,
): String {
    val minutes = (now - then).coerceAtLeast(0) / MILLIS_IN_MINUTE
    return when {
        minutes < 1 -> Strings.JUST_NOW
        minutes < MINUTES_IN_HOUR -> Strings.minutesAgo(minutes)
        minutes < MINUTES_IN_DAY -> Strings.hoursAgo(minutes / MINUTES_IN_HOUR)
        minutes < MINUTES_IN_WEEK -> Strings.daysAgo(minutes / MINUTES_IN_DAY)
        else -> Instant.fromEpochMilliseconds(then).toString().take(DATE_LENGTH)
    }
}

const val HISTORY_SEARCH_TAG = "historySearch"
const val HISTORY_ROW_TAG = "historyRow"

private val Gutter = 16.dp
private val ChipGap = 8.dp
private val RowPadding = 8.dp
private const val MILLIS_IN_MINUTE = 60_000L
private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_DAY = 24 * 60L
private const val MINUTES_IN_WEEK = 7 * 24 * 60L

// "2026-10-18" from an ISO instant.
private const val DATE_LENGTH = 10
