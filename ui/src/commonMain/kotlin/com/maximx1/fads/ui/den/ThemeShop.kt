package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Den
import com.maximx1.fads.game.DenActions
import com.maximx1.fads.ui.Strings

/** Every theme the den can wear, to buy or to put up. Tapping one the player owns puts it up. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ThemeShop(
    den: Den,
    actions: DenActions,
    onClose: () -> Unit,
) {
    var asking by remember { mutableStateOf<DenTheme?>(null) }
    val measurer = rememberTextMeasurer()
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.THEMES, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text(Strings.BACK) }
        }
        PurseCoins(den.purse, coinSize = 20.dp)
        LazyVerticalGrid(
            GridCells.Adaptive(SwatchWidth),
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(DenThemes, key = { it.key }) { theme ->
                val worn = theme.key == den.theme
                Column(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (worn) Modifier.border(3.dp, theme.accent, RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { if (den.owns(theme.key)) actions.chooseTheme(theme.key) else asking = theme }
                        .padding(6.dp)
                        .testTag(themeTag(theme.key)),
                ) {
                    Canvas(Modifier.fillMaxWidth().aspectRatio(SWATCH_ASPECT)) { drawSwatch(theme, measurer) }
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        when {
                            worn -> Strings.IN_USE
                            den.owns(theme.key) -> Strings.OWNED
                            else -> Strings.bronzes(Den.THEME_PRICE)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = DIM),
                    )
                }
            }
        }
    }
    asking?.let { theme -> BuyDialog(den, theme, actions, onDone = { asking = null }) }
}

/** Asks before [theme] is bought, or says how far short the purse is. */
@Composable
private fun BuyDialog(
    den: Den,
    theme: DenTheme,
    actions: DenActions,
    onDone: () -> Unit,
) {
    val affordable = den.purse.holds(Den.THEME_PRICE)
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(theme.name) },
        text = {
            Text(
                if (affordable) {
                    Strings.buyText(theme.name, Den.THEME_PRICE)
                } else {
                    Strings.shortBy(Den.THEME_PRICE - den.purse.total)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                actions.buyTheme(theme.key)
                onDone()
            }, enabled = affordable) { Text(Strings.BUY) }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(Strings.CANCEL) } },
    )
}

/** Tags the swatch of the theme saved as [key]. */
fun themeTag(key: String) = "theme-$key"

private val SwatchWidth = 150.dp

private const val SWATCH_ASPECT = 1.5f
private const val DIM = 0.75f
