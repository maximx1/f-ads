package com.maximx1.fads.core.mahjong

import com.maximx1.fads.core.deal.SplitMix64
import com.maximx1.fads.core.deal.shuffledFor

/**
 * Mahjong deal [number], which can always be won. The places of the [Turtle] are emptied two free ones at a time,
 * picked at random, until none are left (starting again in the rare case that fewer than two are free), and the
 * set's pairs, shuffled for the number (see [shuffledFor]), are laid on the places in the order they were emptied.
 * Taking the pairs off in that same order wins.
 */
fun mahjongDeal(number: Long): MahjongState = solvedDeal(number).first

/** Deal [number], and the moves that win it. */
internal fun solvedDeal(number: Long): Pair<MahjongState, List<MahjongMove.Match>> {
    val pairs = facePairs().shuffledFor(number)
    val random = SplitMix64(number xor ORDER_SEED)
    var order: List<Int>? = null
    while (order == null) order = emptyingOrder(Turtle.spots, random)
    val faces = HashMap<Int, TileFace>()
    for ((index, pair) in pairs.withIndex()) {
        faces[order[index * 2]] = pair.first
        faces[order[index * 2 + 1]] = pair.second
    }
    val tiles = Turtle.spots.mapIndexed { id, spot -> Tile(id, faces.getValue(id), spot) }
    val solution = order.chunked(2) { (first, second) -> MahjongMove.Match(first, second) }
    return MahjongState(tiles) to solution
}

/**
 * The set as 72 pairs that match, in a fixed order: each face of a suit twice over, but flowers and seasons, of
 * which there is one of each, paired off as they come.
 */
internal fun facePairs(): List<Pair<TileFace, TileFace>> =
    TileFace.set.chunked(2) { (first, second) -> first to second }

/**
 * The indexes of [spots] in an order they can be emptied in, two free ones at a time, or `null` if the random
 * picks leave fewer than two free before the end.
 */
private fun emptyingOrder(
    spots: List<Spot>,
    random: SplitMix64,
): List<Int>? {
    val left = spots.indices.toMutableList()
    val order = ArrayList<Int>(spots.size)
    while (left.isNotEmpty()) {
        val standing = left.map { spots[it] }
        val free = left.filter { standing.isFree(spots[it]) }.toMutableList()
        if (free.size < 2) return null
        val first = free.removeAt(random.nextBelow(free.size))
        val second = free.removeAt(random.nextBelow(free.size))
        order += first
        order += second
        left -= setOf(first, second)
    }
    return order
}

fun MahjongState.canMove(move: MahjongMove): Boolean =
    when (move) {
        is MahjongMove.Match -> {
            val first = tile(move.first)
            val second = tile(move.second)
            first != null &&
                second != null &&
                move.first != move.second &&
                move.first in free &&
                move.second in free &&
                first.face.matches(second.face)
        }
    }

/** Returns the position after [move]. Throws [IllegalArgumentException] if the move is not legal here. */
fun MahjongState.play(move: MahjongMove): MahjongState {
    require(canMove(move)) { "Illegal move: $move" }
    return when (move) {
        is MahjongMove.Match -> MahjongState(tiles.filter { it.id != move.first && it.id != move.second })
    }
}

/** Every pair of free tiles that match, each once, the lower id first. */
fun MahjongState.legalMoves(): List<MahjongMove.Match> {
    val loose = tiles.filter { it.id in free }
    return buildList {
        for ((index, first) in loose.withIndex()) {
            for (second in loose.drop(index + 1)) {
                if (first.face.matches(second.face)) add(MahjongMove.Match(first.id, second.id))
            }
        }
    }
}

/** True once the game can only play out to a win: every tile left is free, so every one has a free match. */
val MahjongState.canAutoComplete: Boolean get() = !isWon && free.size == tiles.size

/** The next pair of an automatic finish, or `null` if the game cannot finish itself. */
fun MahjongState.nextAutoMove(): MahjongMove? = if (canAutoComplete) legalMoves().firstOrNull() else null

// Keeps the order the places are emptied in apart from the shuffle of the faces, which starts from the number itself.
private const val ORDER_SEED = 0x6D6A6F6E67L
