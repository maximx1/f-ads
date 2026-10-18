package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.engine.GameType

/** Keeps a chosen game type through rotation and process death, by its key. */
internal val GameTypeSaver = Saver<GameType, String>(save = { it.key }, restore = GameType::fromKey)

/** The game's name, the same whichever option it is played with. */
internal fun gameName(type: GameType): String =
    when (type) {
        GameType.FreeCell -> Strings.FREECELL
        is GameType.Klondike -> Strings.SOLITAIRE
    }

/** The option [type] is played with, or `null` for a game that has none. */
internal fun optionName(type: GameType): String? =
    when (type) {
        GameType.FreeCell -> null
        is GameType.Klondike -> Strings.draw(type.draw)
    }

/** The game and its option, as one line: "Spider, 2 suits". */
internal fun fullName(type: GameType): String = listOfNotNull(gameName(type), optionName(type)).joinToString(", ")

/**
 * One radio button for each game and, under the chosen one, a chip for each option it can be played with.
 * Picking a game picks its first option.
 */
@Composable
internal fun GameChoice(
    chosen: GameType,
    onChoose: (GameType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val games = GameType.all.groupBy(::gameName)
    Column(modifier.selectableGroup()) {
        for ((name, options) in games) {
            val picked = gameName(chosen) == name
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = picked,
                        role = Role.RadioButton,
                        onClick = { if (!picked) onChoose(options.first()) },
                    ).padding(vertical = ChoicePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = picked, onClick = null)
                Text(name, Modifier.padding(start = ChoicePadding))
            }
            if (picked && options.size > 1) {
                Row(
                    Modifier.padding(start = OptionIndent),
                    horizontalArrangement = Arrangement.spacedBy(ChoicePadding),
                ) {
                    for (option in options) {
                        FilterChip(
                            selected = option == chosen,
                            onClick = { onChoose(option) },
                            label = { Text(optionName(option).orEmpty()) },
                        )
                    }
                }
            }
        }
    }
}

private val ChoicePadding = 10.dp
private val OptionIndent = 34.dp
