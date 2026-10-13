package com.maximx1.fads.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cigarette smoke from somebody just out of sight. Every so often a thin cloud of it drifts across, rising and
 * spreading as it goes, and then the air is clear again for a while. Nothing is drawn between clouds.
 */
@Composable
internal fun Smoke(modifier: Modifier = Modifier) {
    val drift = remember { Animatable(0f) }
    var cloud by remember { mutableStateOf<Cloud?>(null) }
    LaunchedEffect(Unit) {
        var wait = FIRST_WAIT_MILLIS
        while (true) {
            delay(wait)
            val next = Cloud.random()
            cloud = next
            drift.snapTo(0f)
            drift.animateTo(1f, tween(next.millis, easing = LinearEasing))
            cloud = null
            wait = Random.nextLong(SHORTEST_WAIT_MILLIS, LONGEST_WAIT_MILLIS)
        }
    }
    // A layer of its own, so a moving cloud does not make the cards under it paint themselves again.
    Canvas(modifier.graphicsLayer()) { cloud?.let { drawCloud(it, drift.value) } }
}

/** One crossing of the screen. Sizes and places are fractions of the screen's width, heights of its height. */
private class Cloud(
    val fromLeft: Boolean,
    val height: Float,
    val millis: Int,
    val wisps: List<Wisp>,
) {
    companion object {
        fun random() =
            Cloud(
                fromLeft = Random.nextBoolean(),
                height = HIGHEST + Random.nextFloat() * HEIGHT_RANGE,
                millis = Random.nextInt(QUICKEST_MILLIS, SLOWEST_MILLIS),
                wisps =
                    List(WISPS) {
                        Wisp(
                            behind = Random.nextFloat() * TRAIL,
                            below = (Random.nextFloat() - HALF) * THICKNESS,
                            size = SMALLEST_WISP + Random.nextFloat() * WISP_RANGE,
                            phase = Random.nextFloat() * TURN,
                        )
                    },
            )
    }
}

/** One puff in a cloud: how far it trails [behind] the front, how far [below] the middle it sits, and its sway. */
private class Wisp(
    val behind: Float,
    val below: Float,
    val size: Float,
    val phase: Float,
)

/** Draws [cloud] with [progress] of its crossing done, from 0 to 1. */
private fun DrawScope.drawCloud(
    cloud: Cloud,
    progress: Float,
) {
    val width = size.width
    val front = -OFFSTAGE + (1 + 2 * OFFSTAGE + TRAIL) * progress
    val middle = size.height * cloud.height - width * RISE * progress
    // Thin at both ends of the crossing, so the cloud gathers and disperses instead of sliding on and off.
    val color = Tint.copy(alpha = THICKEST * sin(PI.toFloat() * progress))
    for (wisp in cloud.wisps) {
        val along = front - wisp.behind
        val sway = sin(progress * SWAYS * TURN + wisp.phase) * SWAY
        drawGlow(
            centre = Offset(width * if (cloud.fromLeft) along else 1 - along, middle + width * (wisp.below + sway)),
            radius = width * wisp.size * (1 + SPREAD * progress),
            color = color,
            stretch = STRETCH,
        )
    }
}

private val Tint = Color(0xFFE9E1D0)
private const val THICKEST = 0.1f
private const val WISPS = 8
private const val TURN = 2 * PI.toFloat()
private const val HALF = 0.5f

private const val FIRST_WAIT_MILLIS = 1_500L
private const val SHORTEST_WAIT_MILLIS = 6_000L
private const val LONGEST_WAIT_MILLIS = 24_000L
private const val QUICKEST_MILLIS = 16_000
private const val SLOWEST_MILLIS = 26_000

private const val HIGHEST = 0.2f
private const val HEIGHT_RANGE = 0.55f
private const val RISE = 0.3f
private const val OFFSTAGE = 0.7f
private const val TRAIL = 0.9f
private const val THICKNESS = 0.4f
private const val SMALLEST_WISP = 0.16f
private const val WISP_RANGE = 0.2f
private const val STRETCH = 1.9f
private const val SPREAD = 0.6f
private const val SWAY = 0.05f
private const val SWAYS = 1.5f
