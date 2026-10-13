package com.maximx1.fads.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maximx1.fads.ui.theme.ButtonLook

/** A single button split into equal [segments], each of which is pressed on its own. */
@Composable
internal fun SplitButton(
    segments: List<Segment>,
    look: ButtonLook,
    modifier: Modifier = Modifier,
) {
    val outline = look.border?.let { Modifier.border(it, look.shape) } ?: Modifier
    Row(
        modifier
            .height(Height)
            .clip(look.shape)
            .drawBehind(look.fill)
            .then(outline),
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) Box(Modifier.width(DividerWidth).fillMaxHeight().background(look.divider))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = segment.enabled, role = Role.Button, onClick = segment.onClick)
                    .semantics { contentDescription = segment.label },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(GlyphSize).alpha(if (segment.enabled) 1f else DISABLED_ALPHA)) {
                    look.etch?.let { translate(top = EtchDrop.toPx()) { drawGlyph(segment.glyph, it) } }
                    drawGlyph(segment.glyph, look.ink)
                }
            }
        }
    }
}

private val Height = 44.dp
private val DividerWidth = 1.dp
private val GlyphSize = 28.dp
private val EtchDrop = 1.dp
private const val DISABLED_ALPHA = 0.38f
