package com.maximx1.fads.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.maximx1.fads.core.mahjong.MahjongMove
import com.maximx1.fads.core.mahjong.MahjongState
import com.maximx1.fads.core.mahjong.Spot
import com.maximx1.fads.core.mahjong.Tile
import com.maximx1.fads.core.mahjong.Turtle
import com.maximx1.fads.core.mahjong.canMove
import kotlin.math.min

/**
 * The tiles of [state], stacked on the turtle and lit from the top left, so each layer stands a little up and to
 * the left of the one below. Tap a free tile to pick it, then a free tile that matches to call [onMove] with the
 * pair; tapping the picked tile again lets it go, and tapping another free tile picks that one instead. Tiles fade
 * as they leave the table, and back in if they come back to it. [marks] dims tiles and points out a hint's pair.
 */
@Composable
@Suppress("LongParameterList") // The same as a CardTable's.
internal fun MahjongTable(
    state: MahjongState,
    onMove: (MahjongMove) -> Unit,
    modifier: Modifier = Modifier,
    paint: BoardPaint = rememberPlainBoardPaint(),
    marks: TableMarks<MahjongMove> = TableMarks(),
) {
    val style = rememberTileStyle()
    // Every tile seen on this table, so the ones taken off are still there to fade out, and to fade back in.
    val seen = remember { HashMap<Int, Tile>() }
    val tiles =
        remember(state) {
            state.tiles
                .associateByTo(seen) { it.id }
                .values
                .sortedBy { it.id }
        }
    val hinted = (marks.hint as? MahjongMove.Match)?.let { setOf(it.first, it.second) }.orEmpty()
    var picked by remember(state) { mutableStateOf<Int?>(null) }
    val tap by rememberUpdatedState<(Int) -> Unit>({ id ->
        val first = picked
        val pair = first?.let { MahjongMove.Match(it, id) }
        if (id in state.free) {
            when {
                first == id -> picked = null
                pair != null && state.canMove(pair) -> onMove(pair)
                else -> picked = id
            }
        }
    })

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val at = remember(width, height, density) { TileGeometry(width, height, with(density) { Gap.toPx() }) }
        Canvas(Modifier.fillMaxSize()) { paint.pane?.invoke(this, null) }

        for (tile in tiles) {
            key(tile.id) {
                val mark =
                    when (tile.id) {
                        picked -> TileMark.PICKED
                        in hinted -> TileMark.HINTED
                        else -> TileMark.NONE
                    }
                val dim = tile.id in marks.dimmed && mark == TileMark.NONE
                PlacedTile(tile, state.tile(tile.id) != null, at, TileLook(style, mark, dim, paint.hint), tap)
            }
        }
    }
}

/** How one tile is drawn: in [style], marked out with [mark] in [markColor], and darker if [dim]. */
private class TileLook(
    val style: TileStyle,
    val mark: TileMark,
    val dim: Boolean,
    val markColor: Color,
)

/** One tile at its place, faded out if it is not [onTable], and calling [onTap] with its id when tapped. */
@Composable
private fun PlacedTile(
    tile: Tile,
    onTable: Boolean,
    at: TileGeometry,
    look: TileLook,
    onTap: (Int) -> Unit,
) {
    val shown by animateFloatAsState(if (onTable) 1f else 0f, tween(FADE_MILLIS))
    val density = LocalDensity.current
    val block = with(density) { DpSize(at.block.width.toDp(), at.block.height.toDp()) }
    val tap by rememberUpdatedState(onTap)
    val tappable = if (onTable) Modifier.pointerInput(tile.id) { detectTapGestures { tap(tile.id) } } else Modifier
    Canvas(
        Modifier
            .zIndex(at.order(tile.spot))
            .offset { at.topLeft(tile.spot).round() }
            .size(block)
            .graphicsLayer {
                alpha = shown
                scaleX = GONE_SCALE + (1 - GONE_SCALE) * shown
                scaleY = scaleX
            }.then(if (look.dim) Dimmed else Modifier)
            .testTag(MahjongTiles.tagOf(tile))
            .then(tappable),
    ) {
        with(look.style) { drawTile(tile.face, at.face, at.depth, look.mark, look.markColor) }
    }
}

/** Where the turtle's tiles go, in pixels, to fit it as large as it goes in [width] by [height], centred. */
private class TileGeometry(
    width: Float,
    height: Float,
    gap: Float,
) {
    private val spots = Turtle.spots
    private val left = spots.minOf { x(it) }
    private val top = spots.minOf { y(it) }
    private val across = spots.maxOf { x(it) } + 1 + DEPTH - left
    private val down = spots.maxOf { y(it) } + ASPECT + DEPTH - top

    /** A tile's width, which every other size here is a fraction of. */
    private val unit = min((width - gap * 2) / across, (height - gap * 2) / down).coerceAtLeast(1f)
    private val origin = Offset((width - across * unit) / 2 - left * unit, (height - down * unit) / 2 - top * unit)

    val face = Size(unit, unit * ASPECT)
    val depth = unit * DEPTH

    /** The face and the thickness below it. */
    val block = Size(face.width + depth, face.height + depth)

    fun topLeft(spot: Spot) = origin + Offset(x(spot), y(spot)) * unit

    /**
     * The order tiles are drawn in: layer by layer from the bottom, and on each from the top left, so every tile
     * covers the thickness of its neighbours to the left and above.
     */
    fun order(spot: Spot): Float =
        (spot.layer * LAYER_ORDER + (spot.col + spot.row) * DIAGONAL_ORDER + spot.col).toFloat()

    // Where a spot's face starts, in tile widths.
    private fun x(spot: Spot) = spot.col * HALF - spot.layer * DEPTH

    private fun y(spot: Spot) = spot.row * HALF * ASPECT - spot.layer * DEPTH
}

private val Gap = 4.dp

private const val ASPECT = 1.3f
private const val DEPTH = 0.17f
private const val HALF = 0.5f
private const val LAYER_ORDER = 2000
private const val DIAGONAL_ORDER = 32
private const val FADE_MILLIS = 180
private const val GONE_SCALE = 0.8f
