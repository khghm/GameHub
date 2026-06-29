package com.gamehub.shared.engines.card

import kotlinx.serialization.Serializable

@Serializable
data class Hand(
    val cards: List<Card>
) {
    fun add(card: Card): Hand = copy(cards = cards + card)

    fun remove(card: Card): Hand = copy(cards = cards - card)

    fun contains(card: Card): Boolean = card in cards

    val size: Int get() = cards.size
}