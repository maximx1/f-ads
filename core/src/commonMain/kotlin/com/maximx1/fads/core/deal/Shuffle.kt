package com.maximx1.fads.core.deal

/**
 * This list in the order numbered deal [number] puts it in: [SplitMix64] seeded with the number drives a
 * Fisher-Yates shuffle, swapping item `i` with item `nextBelow(i + 1)` for `i` from the last down to 1. Every
 * game's numbered deals depend on this, so it must never change.
 */
internal fun <T> List<T>.shuffledFor(number: Long): List<T> {
    require(number >= 0) { "Deal numbers start at 0, got $number" }
    val items = toMutableList()
    val random = SplitMix64(number)
    for (i in items.lastIndex downTo 1) {
        val j = random.nextBelow(i + 1)
        items[i] = items[j].also { items[j] = items[i] }
    }
    return items
}
