package com.maximx1.fads.ui.den

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maximx1.fads.core.den.Purse
import com.maximx1.fads.ui.Strings
import com.maximx1.fads.ui.grouped

/** The den's coins, each worth [worth] bronzes, and the metal each is struck in. */
enum class Coin(
    val worth: Long,
    val label: String,
    val face: Color,
    val edge: Color,
    val shine: Color,
) {
    GOLD(Purse.GOLD, Strings.GOLDS, GoldFace, GoldEdge, GoldShine),
    SILVER(Purse.SILVER, Strings.SILVERS, SilverFace, SilverEdge, Color.White),
    BRONZE(1, Strings.BRONZES, BronzeFace, BronzeEdge, BronzeShine),
}

/** A coin, lit from the top left, at [centre] and [radius]: unless told otherwise, filling the draw area. */
internal fun DrawScope.drawCoin(
    coin: Coin,
    centre: Offset = center,
    radius: Float = size.minDimension / 2,
) {
    val shine = centre - Offset(radius * SHINE_OFFSET, radius * SHINE_OFFSET)
    drawCircle(coin.edge, radius, centre)
    drawCircle(
        Brush.radialGradient(listOf(coin.shine, coin.face), shine, radius * SHINE_REACH),
        radius * FACE,
        centre,
    )
    drawCircle(coin.edge.copy(alpha = RING_ALPHA), radius * RING, centre, style = Stroke(radius * RING_LINE))
}

/** How many of each coin [purse] counts out to, as a coin and its count for each. */
@Composable
internal fun PurseCoins(
    purse: Purse,
    modifier: Modifier = Modifier,
    coinSize: Dp = 28.dp,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CoinCount(Coin.GOLD, purse.golds, coinSize)
        CoinCount(Coin.SILVER, purse.silvers, coinSize)
        CoinCount(Coin.BRONZE, purse.bronzes, coinSize)
    }
}

@Composable
private fun CoinCount(
    coin: Coin,
    count: Long,
    coinSize: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .semantics(
                    mergeDescendants = true,
                ) { contentDescription = coin.label }
                .testTag(coinTag(coin)),
    ) {
        Canvas(Modifier.size(coinSize)) { drawCoin(coin) }
        Text(
            "× ${grouped(count)}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Tags the count of [coin] in the purse. */
fun coinTag(coin: Coin) = "purse-${coin.name.lowercase()}"

private val GoldFace = Color(0xFFF2C14E)
private val GoldEdge = Color(0xFF8E5F0B)
private val GoldShine = Color(0xFFFFF3C4)
private val SilverFace = Color(0xFFC3CCD5)
private val SilverEdge = Color(0xFF5F6B76)
private val BronzeFace = Color(0xFFC27C45)
private val BronzeEdge = Color(0xFF5E3417)
private val BronzeShine = Color(0xFFF3C79C)

private const val SHINE_OFFSET = 0.35f
private const val SHINE_REACH = 1.3f
private const val FACE = 0.86f
private const val RING = 0.62f
private const val RING_LINE = 0.07f
private const val RING_ALPHA = 0.55f
