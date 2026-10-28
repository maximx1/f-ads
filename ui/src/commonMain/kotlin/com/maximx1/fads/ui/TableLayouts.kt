package com.maximx1.fads.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.maximx1.fads.core.engine.GameType
import com.maximx1.fads.core.engine.Rules
import com.maximx1.fads.core.mahjong.MahjongMove
import com.maximx1.fads.core.mahjong.MahjongState

/**
 * The card table for the game [rules] play, or `null` for mahjong, which is played with tiles on a [MahjongTable].
 * Each type's layout is written for its own rules, so the cast holds.
 */
@Suppress("UNCHECKED_CAST")
internal fun <S : Any, M : Any> tableLayoutFor(rules: Rules<S, M>): TableLayout<S, M>? =
    when (rules.type) {
        GameType.FreeCell -> FreeCellLayout
        is GameType.Klondike -> KlondikeLayout
        is GameType.Spider -> SpiderLayout
        GameType.Mahjong -> null
    } as TableLayout<S, M>?

/**
 * Whatever game [rules] play, on its table: tiles for mahjong, and cards laid out by the game for the rest. The
 * table pinches to zoom. Unless [playable], it only zooms: nothing on it can be picked up or tapped.
 */
@Composable
@Suppress("UNCHECKED_CAST", "LongParameterList") // A mahjong game's state and moves are mahjong's.
fun <S : Any, M : Any> GameTable(
    rules: Rules<S, M>,
    state: S,
    onMove: (M) -> Unit,
    modifier: Modifier = Modifier,
    paint: BoardPaint = rememberPlainBoardPaint(),
    marks: TableMarks<M> = TableMarks(),
    playable: Boolean = true,
) {
    val layout = remember(rules) { tableLayoutFor(rules) }
    val view = remember(rules.type) { TableView() }
    ZoomableTable(view, modifier) {
        if (layout != null) {
            CardTable(state, layout, onMove, Modifier.fillMaxSize(), paint, marks)
        } else {
            MahjongTable(
                state as MahjongState,
                onMove as (MahjongMove) -> Unit,
                Modifier.fillMaxSize(),
                paint,
                marks as TableMarks<MahjongMove>,
            )
        }
        if (!playable) {
            // Over the cards, so they cannot be picked up. It takes the touches without using them up, so the
            // table still zooms.
            Box(
                Modifier.fillMaxSize().pointerInput(
                    Unit,
                ) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            )
        }
    }
}

/** The cards or tiles on [state]'s table that no legal move would move right now, by id. */
internal fun <S : Any, M : Any> stuckOn(
    rules: Rules<S, M>,
    state: S,
): Set<Int> = tableLayoutFor(rules)?.stuck(state) ?: MahjongTiles.stuck(state as MahjongState)
