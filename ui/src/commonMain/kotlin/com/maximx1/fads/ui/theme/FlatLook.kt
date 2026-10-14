package com.maximx1.fads.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.maximx1.fads.ui.CardStyle
import com.maximx1.fads.ui.PlaceholderCardStyle

/** The default: plain colours, no gradients, no shadows. */
internal object FlatLook : Look {
    override val slotColor = Color(0x40FFFFFF)
    override val accent = Color(0xFFFFE082)
    override val clock = ClockLook(TextStyle(color = Color.White))
    override val buttons =
        ButtonLook(
            shape = RoundedCornerShape(8.dp),
            ink = Color.White,
            divider = Color(0x33FFFFFF),
            fill = { drawRect(ButtonFace) },
        )

    override fun DrawScope.drawTable() = drawRect(Felt)

    override fun cards(measurer: TextMeasurer): CardStyle = PlaceholderCardStyle(measurer)
}

private val Felt = Color(0xFF1B5E20)
private val ButtonFace = Color(0x26FFFFFF)
