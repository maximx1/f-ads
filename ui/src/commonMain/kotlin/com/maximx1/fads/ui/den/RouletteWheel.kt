package com.maximx1.fads.ui.den

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.maximx1.fads.core.den.PocketColor
import com.maximx1.fads.core.den.Wheel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The wheel, with the ball in the pocket the last spin landed in, at the top under the marker. When [spins] goes
 * up, the wheel spins one way and the ball the other until it drops into [pocket], and then [onSettled] is
 * called. Before the first spin there is no ball.
 */
@Composable
internal fun RouletteWheel(
    pocket: Int?,
    spins: Long,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val settled by rememberUpdatedState(onSettled)
    // The spin shown, so one that happened before this was on the screen is not spun again.
    var shown by remember { mutableLongStateOf(spins) }
    val turn = remember { Animatable(restingTurn(pocket)) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(spins) {
        if (spins == shown || pocket == null) return@LaunchedEffect
        shown = spins
        val from = turn.value
        // Whole turns and then on to where the pocket comes to the top.
        val to = from - SPIN_TURNS * FULL_TURN - (from - restingTurn(pocket)).mod(FULL_TURN)
        progress.snapTo(0f)
        val spin = tween<Float>(SPIN_MILLIS, easing = FastOutSlowInEasing)
        val wheel = launch { turn.animateTo(to, spin) }
        progress.animateTo(1f, spin)
        wheel.join()
        settled()
    }
    Canvas(modifier) {
        drawWheel(turn.value, measurer)
        if (pocket != null) drawBall(progress.value)
    }
}

// The turn of the wheel, in degrees clockwise, that brings [pocket] to the top.
private fun restingTurn(pocket: Int?): Float = if (pocket == null) 0f else -Wheel.pockets.indexOf(pocket) * STEP

private fun DrawScope.drawWheel(
    turn: Float,
    measurer: TextMeasurer,
) {
    val radius = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(RimLight, RimDark), center, radius), radius)
    val track = radius * TRACK
    val pockets = radius * POCKETS
    val inner = radius * INNER
    rotate(turn, center) {
        Wheel.pockets.forEachIndexed { index, number ->
            drawArc(
                colorOf(number),
                startAngle = index * STEP - QUARTER - STEP / 2,
                sweepAngle = STEP,
                useCenter = true,
                topLeft = center - Offset(pockets, pockets),
                size = Size(pockets * 2, pockets * 2),
            )
        }
        Wheel.pockets.forEachIndexed { index, number ->
            rotate(index * STEP, center) {
                val text =
                    measurer.measure(
                        number.toString(),
                        Numbers.copy(fontSize = (radius * NUMBER_SIZE / density).sp),
                    )
                drawText(
                    text,
                    topLeft = Offset(center.x - text.size.width / 2, center.y - pockets + radius * NUMBER_INSET),
                )
            }
        }
        drawCircle(Brush.radialGradient(listOf(HubLight, HubDark), center, inner), inner)
        for (spoke in 0 until SPOKES) {
            val angle = radians(spoke * FULL_TURN / SPOKES)
            val end = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * inner * SPOKE_REACH
            drawLine(Brass, center, end, inner * SPOKE_WIDTH)
        }
        drawCircle(Brass, inner * BOSS)
    }
    drawCircle(RimDark, track, style = Stroke(radius * TRACK_LINE))
    // The marker at the top that the winning pocket stops under.
    drawCircle(Brass, radius * MARKER, Offset(center.x, center.y - radius + radius * MARKER))
}

// The ball circles the track against the wheel and drops into the pocket at the top as [progress] reaches 1.
private fun DrawScope.drawBall(progress: Float) {
    val radius = size.minDimension / 2
    val angle = radians(-BALL_TURNS * FULL_TURN * (1 - progress) - QUARTER)
    val drop = ((progress - DROP_FROM) / (1 - DROP_FROM)).coerceIn(0f, 1f)
    val from = radius * TRACK_BALL
    val to = radius * (POCKETS - BALL_SINK)
    val distance = from + (to - from) * drop
    val at = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * distance
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White, BallShade),
            at - Offset(1f, 1f) * (radius * BALL / 2),
            radius * BALL * 2,
        ),
        radius * BALL,
        at,
    )
}

private fun radians(degrees: Float): Double = degrees * PI / HALF_TURN

private fun colorOf(number: Int): Color =
    when (Wheel.colorOf(number)) {
        PocketColor.GREEN -> PocketGreen
        PocketColor.RED -> PocketRed
        PocketColor.BLACK -> PocketBlack
    }

private val Numbers = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
private val PocketGreen = Color(0xFF1B7A3A)
private val PocketRed = Color(0xFFB3202A)
private val PocketBlack = Color(0xFF1A1A1A)
private val RimLight = Color(0xFF8A5A2B)
private val RimDark = Color(0xFF3B2210)
private val HubLight = Color(0xFF9C6B3C)
private val HubDark = Color(0xFF4A2C14)
private val Brass = Color(0xFFD9B45A)
private val BallShade = Color(0xFFB8BEC4)

private const val FULL_TURN = 360f
private const val HALF_TURN = 180
private const val QUARTER = 90f
private val STEP = FULL_TURN / Wheel.pockets.size
private const val SPIN_TURNS = 3
private const val BALL_TURNS = 4
private const val SPIN_MILLIS = 4_000
private const val DROP_FROM = 0.75f

// Sizes as shares of the wheel's radius.
private const val TRACK = 0.94f
private const val TRACK_LINE = 0.02f
private const val TRACK_BALL = 0.88f
private const val POCKETS = 0.8f
private const val INNER = 0.5f
private const val NUMBER_SIZE = 0.075f
private const val NUMBER_INSET = 0.02f
private const val BALL = 0.045f
private const val BALL_SINK = 0.2f
private const val MARKER = 0.035f
private const val SPOKES = 4
private const val SPOKE_REACH = 0.8f
private const val SPOKE_WIDTH = 0.06f
private const val BOSS = 0.16f
