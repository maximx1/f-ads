package com.maximx1.fads.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = CardCodeSerializer::class)
data class Card(
    val rank: Rank,
    val suit: Suit,
) {
    val color: SuitColor get() = suit.color

    /** Plain-text form used in saved games and tests: rank label then suit letter, e.g. `AS`, `10H`, `KD`. */
    val code: String get() = rank.label + suit.letter

    override fun toString(): String = rank.label + suit.symbol

    companion object {
        /**
         * The canonical deck: spades ace to king, then hearts, diamonds and clubs.
         * Numbered deals shuffle this list, so its order must never change.
         */
        val deck: List<Card> = Suit.entries.flatMap { suit -> Rank.entries.map { rank -> Card(rank, suit) } }

        private val byCode: Map<String, Card> = deck.associateBy { it.code }

        fun fromCode(code: String): Card = requireNotNull(byCode[code]) { "Not a card code: '$code'" }
    }
}

internal object CardCodeSerializer : KSerializer<Card> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.maximx1.fads.core.model.Card", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: Card,
    ) = encoder.encodeString(value.code)

    override fun deserialize(decoder: Decoder): Card = Card.fromCode(decoder.decodeString())
}
