package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.engine.GameType

/** The first thing the player sees, once: which game to play. [onPlay] deals a random game of the one picked. */
@Composable
internal fun Landing(onPlay: (GameType) -> Unit) {
    var chosen by rememberSaveable(stateSaver = GameTypeSaver) { mutableStateOf<GameType>(GameType.FreeCell) }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(Gutter).widthIn(max = PanelWidth),
        ) {
            Column(Modifier.padding(Gutter)) {
                Text(Strings.APP_NAME, style = MaterialTheme.typography.headlineMedium)
                Text(
                    Strings.PICK_A_GAME,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = Gutter),
                )
                GameChoice(chosen, onChoose = { chosen = it })
                Button(
                    onClick = { onPlay(chosen) },
                    modifier = Modifier.align(Alignment.End).padding(top = Gutter),
                ) { Text(Strings.PLAY) }
            }
        }
    }
}

private val Gutter = 24.dp
private val PanelWidth = 420.dp
