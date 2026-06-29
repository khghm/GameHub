package com.gamehub.shared.engines.card

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val cards: List<Card>
) {
    fun shuffle(): Deck = copy(cards = cards.shuffled())

    fun draw(): Pair<Card, Deck> {
        require(cards.isNotEmpty())
        return cards.first() to copy(cards = cards.drop(1))
    }

    val size: Int get() = cards.size
}