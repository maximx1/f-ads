// The gradient builders take their stops only as varargs, and these arrays are a handful of entries each.
@file:Suppress("SpreadOperator")

package com.maximx1.fads.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

/** The colours of a gradient and how far along it each one sits, from 0 to 1. */
internal typealias Stops = Array<Pair<Float, Color>>

/** Top to bottom of whatever is being filled, or over [height] if one is given, [tileMode] deciding what follows. */
internal fun Stops.downward(
    height: Float = Float.POSITIVE_INFINITY,
    tileMode: TileMode = TileMode.Clamp,
): Brush = Brush.verticalGradient(*this, endY = height, tileMode = tileMode)

internal fun Stops.between(
    from: Offset,
    to: Offset,
): Brush = Brush.linearGradient(*this, start = from, end = to)

internal fun Stops.around(
    centre: Offset,
    radius: Float,
): Brush = Brush.radialGradient(*this, center = centre, radius = radius)
