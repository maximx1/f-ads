package com.maximx1.fads.ui

import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules

/** The table for the game [rules] play. Each type's layout is written for its own rules, so the cast holds. */
@Suppress("UNCHECKED_CAST")
internal fun <S : Any, M : Any> tableLayoutFor(rules: Rules<S, M>): TableLayout<S, M> =
    when (rules.type) {
        GameType.FreeCell -> FreeCellLayout
        is GameType.Klondike -> KlondikeLayout
        is GameType.Spider -> SpiderLayout
    } as TableLayout<S, M>
