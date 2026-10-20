package com.maximx1.fads.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Fireworks for a win, in big square pixels: rockets climb from the bottom of the screen and burst into sparks that
 * spread, fall and flicker out, a new one every moment or so until the show is over. It takes no touches.
 */
@Composable
internal fun Fireworks(modifier: Modifier = Modifier) {
    val show = remember { Show.random() }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(show) {
        clock.animateTo(show.seconds, tween((show.seconds * MILLIS_PER_SECOND).toInt(), easing = LinearEasing))
    }
    // A layer of its own, so the sparks do not make the table under them paint itself again.
    Canvas(modifier.graphicsLayer()) { drawShow(show, clock.value) }
}

/** Every rocket of one show, and how long the whole of it lasts. */
private class Show(
    val shells: List<Shell>,
) {
    val seconds = shells.maxOf { it.launchAt + it.flight + it.burst.life }

    companion object {
        fun random(): Show {
            val shells = mutableListOf<Shell>()
            var at = FIRST_LAUNCH
            while (at < LAUNCHING_SECONDS) {
                shells += Shell.random(at)
                // Now and then two go up together.
                if (Random.nextFloat() < PAIRED) shells += Shell.random(at + Random.nextFloat() * PAIR_GAP)
                at += SHORTEST_GAP + Random.nextFloat() * GAP_RANGE
            }
            return Show(shells)
        }
    }
}

/**
 * One rocket: when it goes up, from how far across the screen, how high it bursts as a fraction of the screen's
 * height from the top, and the [burst] it ends in.
 */
private class Shell(
    val launchAt: Float,
    val across: Float,
    val peak: Float,
    val flight: Float,
    val burst: Burst,
) {
    companion object {
        fun random(launchAt: Float) =
            Shell(
                launchAt = launchAt,
                across = EDGE + Random.nextFloat() * (1 - 2 * EDGE),
                peak = HIGHEST_PEAK + Random.nextFloat() * PEAK_RANGE,
                flight = SHORTEST_FLIGHT + Random.nextFloat() * FLIGHT_RANGE,
                burst = Burst.random(),
            )
    }
}

/** How wide a rocket bursts, as a fraction of the screen's shorter side, how long it burns, and its sparks. */
private class Burst(
    val radius: Float,
    val life: Float,
    val colors: Pair<Color, Color>,
    val sparks: List<Spark>,
) {
    companion object {
        fun random(): Burst {
            // A ring spreads evenly to one size; the rest fill the circle, thicker towards the middle.
            val ring = Random.nextFloat() < RINGS
            val count = Random.nextInt(FEWEST_SPARKS, MOST_SPARKS)
            val sparks =
                List(count) { i ->
                    Spark(
                        angle = TURN * if (ring) (i + Random.nextFloat() * HALF) / count else Random.nextFloat(),
                        speed = if (ring) RING_SPEED + Random.nextFloat() * RING_SPREAD else sqrt(Random.nextFloat()),
                        second = i % 2 == 1,
                        flicker = Random.nextInt(FLICKER_EVERY),
                    )
                }
            return Burst(
                radius = SMALLEST_BURST + Random.nextFloat() * BURST_RANGE,
                life = SHORTEST_LIFE + Random.nextFloat() * LIFE_RANGE,
                colors = Palette.random() to Palette.random(),
                sparks = sparks,
            )
        }
    }
}

/** One spark of a burst: which way it flies, how fast as a fraction of the burst's width, and its colour. */
private class Spark(
    val angle: Float,
    val speed: Float,
    val second: Boolean,
    val flicker: Int,
)

/** Draws [show] as it is [seconds] in. */
private fun DrawScope.drawShow(
    show: Show,
    seconds: Float,
) {
    val cell = floor(PIXEL.toPx()).coerceAtLeast(1f)
    // Sparks blink on and off at a steady rate as they burn out.
    val blink = (seconds * BLINKS_PER_SECOND).toInt()
    for (shell in show.shells) {
        val since = seconds - shell.launchAt
        if (since in 0f..<shell.flight) drawRocket(shell, since, cell)
        val burning = since - shell.flight
        if (burning in 0f..<shell.burst.life) {
            drawBurst(shell.burst, rocketAt(shell, shell.flight), burning, blink, cell)
        }
    }
}

/** Where [shell] is, [since] seconds after it went up: fast off the ground, slowing to a stop at its peak. */
private fun DrawScope.rocketAt(
    shell: Shell,
    since: Float,
): Offset {
    val left = 1 - (since / shell.flight).coerceIn(0f, 1f)
    val climb = 1 - left * left
    return Offset(size.width * shell.across, size.height * (1 - (1 - shell.peak) * climb))
}

private fun DrawScope.drawRocket(
    shell: Shell,
    since: Float,
    cell: Float,
) {
    for (back in TRAIL downTo 1) {
        val fade = 1 - back.toFloat() / (TRAIL + 1)
        pixel(rocketAt(shell, since - back * TRAIL_STEP), Ember.copy(alpha = fade), cell)
    }
    pixel(rocketAt(shell, since), White, cell)
}

/** [burst], [since] seconds after it went off at [centre]. */
private fun DrawScope.drawBurst(
    burst: Burst,
    centre: Offset,
    since: Float,
    blink: Int,
    cell: Float,
) {
    if (since < FLASH_SECONDS) {
        for (dx in -1..1) for (dy in -1..1) pixel(centre + Offset(dx * cell, dy * cell), White, cell)
    }
    val age = since / burst.life
    // Full strength for most of its life, then quickly out.
    val alpha = 1 - age * age * age
    val reach = min(size.width, size.height)
    val radius = reach * burst.radius
    val fall = reach * GRAVITY
    for (spark in burst.sparks) {
        // Late in their life, each spark is lit on only some blinks.
        if (age > FLICKER_FROM && (blink + spark.flicker) % FLICKER_EVERY == 0) continue
        val color =
            if (since < WHITE_SECONDS) {
                White
            } else if (spark.second) {
                burst.colors.second
            } else {
                burst.colors.first
            }
        val direction = Offset(cos(spark.angle), sin(spark.angle)) * (radius * spark.speed)

        fun at(t: Float) = centre + direction * (1 - exp(-DRAG * t)) + Offset(0f, fall * t * t)
        pixel(at((since - TAIL_SECONDS).coerceAtLeast(0f)), color.copy(alpha = alpha * TAIL_ALPHA), cell)
        pixel(at(since), color.copy(alpha = alpha), cell)
    }
}

/** One square of the grid, the one [at] falls in. */
private fun DrawScope.pixel(
    at: Offset,
    color: Color,
    cell: Float,
) {
    drawRect(color, Offset(floor(at.x / cell) * cell, floor(at.y / cell) * cell), Size(cell, cell))
}

private val PIXEL = 4.dp
private val White = Color(0xFFFFFBE6)
private val Ember = Color(0xFFFFB347)
private val Palette =
    listOf(
        Color(0xFFFF5252),
        Color(0xFFFFD740),
        Color(0xFF69F0AE),
        Color(0xFF40C4FF),
        Color(0xFFB388FF),
        Color(0xFFFF80AB),
        Color(0xFFFFAB40),
    )

private const val MILLIS_PER_SECOND = 1_000
private const val TURN = 2 * PI.toFloat()
private const val HALF = 0.5f

private const val FIRST_LAUNCH = 0.2f
private const val LAUNCHING_SECONDS = 9f
private const val SHORTEST_GAP = 0.35f
private const val GAP_RANGE = 0.6f
private const val PAIRED = 0.25f
private const val PAIR_GAP = 0.25f

private const val EDGE = 0.12f
private const val HIGHEST_PEAK = 0.12f
private const val PEAK_RANGE = 0.35f
private const val SHORTEST_FLIGHT = 0.8f
private const val FLIGHT_RANGE = 0.5f
private const val TRAIL = 6
private const val TRAIL_STEP = 0.018f

private const val SMALLEST_BURST = 0.14f
private const val BURST_RANGE = 0.12f
private const val SHORTEST_LIFE = 1.3f
private const val LIFE_RANGE = 0.7f
private const val RINGS = 0.4f
private const val RING_SPEED = 0.9f
private const val RING_SPREAD = 0.1f
private const val FEWEST_SPARKS = 40
private const val MOST_SPARKS = 64
private const val DRAG = 3.5f
private const val GRAVITY = 0.12f
private const val FLASH_SECONDS = 0.06f
private const val WHITE_SECONDS = 0.1f
private const val TAIL_SECONDS = 0.06f
private const val TAIL_ALPHA = 0.45f
private const val FLICKER_FROM = 0.55f
private const val FLICKER_EVERY = 3
private const val BLINKS_PER_SECOND = 20
