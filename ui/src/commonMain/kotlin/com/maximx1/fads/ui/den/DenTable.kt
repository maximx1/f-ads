package com.maximx1.fads.ui.den

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.ui.Strings

// Pieces the den's dice and layout tables share: the dice that tumble and land, and the boxes bets go down in.

/**
 * Dice showing [faces], or blank before the first throw, their pips in the colour of [theme]'s card backs. When
 * [throws] goes up they tumble, the faces flicking round, then land; [onLanded] says which throw they came to rest
 * on.
 */
@Composable
@Suppress("LongParameterList") // The dice, how they are dressed and sized, and the throw they show.
internal fun TumblingDice(
    faces: List<Int>?,
    count: Int,
    throws: Long,
    theme: DenTheme,
    onLanded: (Long) -> Unit,
    size: Dp = 56.dp,
) {
    val landed by rememberUpdatedState(onLanded)
    val tumble = remember { Animatable(1f) }
    var shown by rememberSaveable { mutableLongStateOf(throws) }
    LaunchedEffect(throws) {
        if (throws == shown) return@LaunchedEffect
        shown = throws
        tumble.snapTo(0f)
        tumble.animateTo(1f, tween(TUMBLE_MILLIS, easing = FastOutSlowInEasing))
        landed(throws)
    }
    val settled = tumble.value >= 1f
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        for (index in 0 until count) {
            val face = faces?.getOrNull(index)
            val showing =
                if (settled ||
                    face == null
                ) {
                    face
                } else {
                    ((tumble.value * FLICKS).toInt() + index * 2) % FACES + 1
                }
            val turn = (1 - tumble.value) * TURNS * (if (index % 2 == 0) 1 else -1)
            Canvas(Modifier.size(size).rotate(turn)) { drawDie(showing, theme.back) }
        }
    }
}

/** A die showing [face] in [pips], or blank. */
internal fun DrawScope.drawDie(
    face: Int?,
    pips: Color = Color.Black,
) {
    drawRoundRect(Color.White, cornerRadius = CornerRadius(size.width * DIE_CORNER))
    for (at in face?.let { Pips.getOrNull(it - 1) }.orEmpty()) {
        drawCircle(pips, size.width * PIP, Offset(at.x * size.width, at.y * size.width))
    }
}

/**
 * A box of a betting layout on the theme's deep felt, edged in its accent: its [label] and [odds], the [stake] on it,
 * and a [puck] if one sits on it, edged more boldly when [lit]. It is dimmed, and takes no taps, unless [open].
 */
@Composable
@Suppress("LongParameterList") // The box, how it stands, how it is dressed, and what a tap does.
internal fun LayoutBox(
    label: String,
    odds: String,
    stake: Long,
    open: Boolean,
    theme: DenTheme,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    lit: Boolean = false,
    puck: String? = null,
) {
    Column(
        modifier
            .alpha(if (open) 1f else CLOSED)
            .background(theme.feltDeep, RoundedCornerShape(8.dp))
            .border(if (lit) 3.dp else 1.dp, theme.accent, RoundedCornerShape(8.dp))
            .then(if (open) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        // Lifted towards white, so a dark accent still reads on the deep felt.
        if (odds.isNotEmpty()) {
            Text(
                odds,
                color = lerp(theme.accent, Color.White, ODDS_LIFT),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (puck != null) {
            Text(
                puck,
                modifier = Modifier.background(theme.accent, CircleShape).padding(horizontal = 6.dp),
                color = Color.Black,
            )
        }
        if (stake > 0) Text(Strings.stack(stake), style = MaterialTheme.typography.labelLarge)
    }
}

// Where the pips of each face sit, one to six, as shares of the die's side.
private const val NEAR = 0.27f
private const val MID = 0.5f
private const val FAR = 0.73f
private val Pips: List<List<Offset>> =
    listOf(
        listOf(Offset(MID, MID)),
        listOf(Offset(NEAR, NEAR), Offset(FAR, FAR)),
        listOf(Offset(NEAR, NEAR), Offset(MID, MID), Offset(FAR, FAR)),
        listOf(Offset(NEAR, NEAR), Offset(FAR, NEAR), Offset(NEAR, FAR), Offset(FAR, FAR)),
        listOf(Offset(NEAR, NEAR), Offset(FAR, NEAR), Offset(MID, MID), Offset(NEAR, FAR), Offset(FAR, FAR)),
        listOf(NEAR, MID, FAR).flatMap { y -> listOf(Offset(NEAR, y), Offset(FAR, y)) },
    )

private const val TUMBLE_MILLIS = 800
private const val FLICKS = 12
private const val FACES = 6
private const val TURNS = 540f
private const val DIE_CORNER = 0.18f
private const val PIP = 0.09f
private const val CLOSED = 0.4f
private const val ODDS_LIFT = 0.45f
