package com.maximx1.fads.ui

/** One part of a [SplitButton]. It shows its [glyph]; the [label] is for screen readers. */
internal class Segment(
    val label: String,
    val glyph: Glyph,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)
