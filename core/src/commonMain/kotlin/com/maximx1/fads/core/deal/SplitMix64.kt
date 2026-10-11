package com.maximx1.fads.core.deal

/**
 * The SplitMix64 generator, written out with explicit 64-bit maths so it gives the same numbers on every
 * platform and Kotlin version. Numbered deals depend on it: do not swap it for `kotlin.random.Random`.
 */
internal class SplitMix64(
    seed: Long,
) {
    private var state = seed

    fun nextLong(): Long {
        state += GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_A)) * MIX_A
        z = (z xor (z ushr SHIFT_B)) * MIX_B
        return z xor (z ushr SHIFT_C)
    }

    /** A number from 0 up to but not including [bound]: the next value, read as unsigned, modulo [bound]. */
    fun nextBelow(bound: Int): Int = (nextLong().toULong() % bound.toULong()).toInt()

    private companion object {
        const val GAMMA = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        const val MIX_A = -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        const val MIX_B = -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        const val SHIFT_A = 30
        const val SHIFT_B = 27
        const val SHIFT_C = 31
    }
}
